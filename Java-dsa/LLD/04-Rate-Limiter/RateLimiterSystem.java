/*
 * ============================================================================
 *  LLD 04 : RATE LIMITER
 * ============================================================================
 *  Run:  java RateLimiterSystem.java          (design notes: README.md)
 *
 *     [api]        RateLimiter (Strategy interface), Decision (allowed, remaining, retryAfter)
 *     [algorithms] FixedWindow, SlidingWindowLog, SlidingWindowCounter, TokenBucket, LeakyBucket
 *     [config]     Rule + RateLimiterFactory (Factory), TieredRateLimiter (per-plan rules),
 *                  CompositeRateLimiter (per-user AND global limits)
 *     [demo]       main(): each algorithm's behaviour with a fake clock, the fixed-window
 *                  boundary burst, and a concurrency test (exactly N allowed)
 *
 *  THE 5 ALGORITHMS IN ONE LINE EACH
 *    Fixed window          count per [00:00, 01:00) bucket. Simple, O(1). Allows 2x at a boundary.
 *    Sliding window log    keep every timestamp, drop older than the window. Exact. O(limit) memory.
 *    Sliding window counter current count + previous count * overlap fraction. ~Exact, O(1).
 *    Token bucket          tokens refill at r/sec up to capacity b; each request takes one.
 *                          Allows BURSTS up to b, average r.   (AWS API Gateway, Stripe)
 *    Leaky bucket          a queue that drains at a constant rate; smooths traffic out.
 *
 *  DESIGN POINTS
 *   - State is PER KEY (user id / API key / IP): ConcurrentHashMap<key, state>. Each state
 *     object synchronises on ITSELF, so different users never contend.
 *   - NO background refill threads: token buckets refill LAZILY from the elapsed time on
 *     each call. Millions of keys cost nothing while idle.
 *   - Time is injected (LongSupplier of millis) so tests are deterministic.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class RateLimiterSystem {

    // =====================================================================
    // [api]
    // =====================================================================

    /** What an HTTP filter needs to answer 200 or 429 + X-RateLimit-Remaining + Retry-After. */
    record Decision(boolean allowed, long remaining, long retryAfterMs) {
        static Decision allow(long remaining) { return new Decision(true, remaining, 0); }
        static Decision deny(long retryAfterMs) { return new Decision(false, 0, Math.max(1, retryAfterMs)); }
    }

    /** STRATEGY interface: every algorithm is interchangeable behind this one method. */
    interface RateLimiter {
        Decision tryAcquire(String key);
    }

    /**
     * Template for "per-key state + lazily created": subclasses only define the state type
     * and the per-state decision. (Template Method: the skeleton lives here.)
     */
    static abstract class PerKeyLimiter<S> implements RateLimiter {
        protected final LongSupplier clock;
        private final ConcurrentHashMap<String, S> states = new ConcurrentHashMap<>();
        PerKeyLimiter(LongSupplier clock) { this.clock = clock; }

        public final Decision tryAcquire(String key) {
            S state = states.computeIfAbsent(key, k -> newState());
            synchronized (state) {                                   // lock ONE user's state, not the limiter
                return decide(state, clock.getAsLong());
            }
        }
        protected abstract S newState();
        protected abstract Decision decide(S state, long nowMs);
        int trackedKeys() { return states.size(); }
    }

    // =====================================================================
    // [algorithms]
    // =====================================================================

    /** 1. FIXED WINDOW: count requests in the current window [start, start + window). */
    static class FixedWindowLimiter extends PerKeyLimiter<FixedWindowLimiter.State> {
        static class State { long windowStart = -1; int count; }
        private final int limit; private final long windowMs;
        FixedWindowLimiter(int limit, long windowMs, LongSupplier clock) { super(clock); this.limit = limit; this.windowMs = windowMs; }
        protected State newState() { return new State(); }
        protected Decision decide(State s, long now) {
            long window = now - now % windowMs;                       // align to wall-clock windows
            if (window != s.windowStart) { s.windowStart = window; s.count = 0; }   // new window: reset
            if (s.count < limit) { s.count++; return Decision.allow(limit - s.count); }
            return Decision.deny(window + windowMs - now);
        }
    }

    /** 2. SLIDING WINDOW LOG: exact; stores one timestamp per accepted request. */
    static class SlidingLogLimiter extends PerKeyLimiter<ArrayDeque<Long>> {
        private final int limit; private final long windowMs;
        SlidingLogLimiter(int limit, long windowMs, LongSupplier clock) { super(clock); this.limit = limit; this.windowMs = windowMs; }
        protected ArrayDeque<Long> newState() { return new ArrayDeque<>(); }
        protected Decision decide(ArrayDeque<Long> log, long now) {
            while (!log.isEmpty() && log.peekFirst() <= now - windowMs) log.pollFirst();   // drop expired
            if (log.size() < limit) { log.addLast(now); return Decision.allow(limit - log.size()); }
            return Decision.deny(log.peekFirst() + windowMs - now);   // when the oldest one expires
        }
    }

    /**
     * 3. SLIDING WINDOW COUNTER: two counters, previous and current window.
     *    estimate = current + previous * (fraction of the previous window still inside the sliding window)
     *    O(1) memory per key, error is small in practice (assumes uniform spread). Used by Cloudflare.
     */
    static class SlidingCounterLimiter extends PerKeyLimiter<SlidingCounterLimiter.State> {
        static class State { long windowStart = 0; int current, previous; }
        private final int limit; private final long windowMs;
        SlidingCounterLimiter(int limit, long windowMs, LongSupplier clock) { super(clock); this.limit = limit; this.windowMs = windowMs; }
        protected State newState() { return new State(); }
        protected Decision decide(State s, long now) {
            long window = now - now % windowMs;
            if (window != s.windowStart) {
                s.previous = window - s.windowStart == windowMs ? s.current : 0;   // gap of 2+ windows -> 0
                s.current = 0;
                s.windowStart = window;
            }
            double overlap = 1.0 - (double) (now - window) / windowMs;
            double estimate = s.current + s.previous * overlap;
            if (estimate + 1 <= limit) { s.current++; return Decision.allow((long) (limit - estimate - 1)); }
            return Decision.deny(Math.max(1, (long) (windowMs * 0.1)));
        }
    }

    /**
     * 4. TOKEN BUCKET: capacity b, refill r tokens/second. Each request costs 1 token.
     *    LAZY REFILL: tokens += elapsed * rate, computed when a request arrives.
     *    Allows a burst of b, then a steady r/sec.
     */
    static class TokenBucketLimiter extends PerKeyLimiter<TokenBucketLimiter.State> {
        static class State { double tokens; long last; State(double t, long l) { tokens = t; last = l; } }
        private final long capacity; private final double perMs;
        TokenBucketLimiter(long capacity, double refillPerSecond, LongSupplier clock) {
            super(clock); this.capacity = capacity; this.perMs = refillPerSecond / 1000.0;
        }
        protected State newState() { return new State(capacity, clock.getAsLong()); }   // start full
        protected Decision decide(State s, long now) {
            s.tokens = Math.min(capacity, s.tokens + (now - s.last) * perMs);   // refill for the elapsed time
            s.last = now;
            if (s.tokens >= 1) { s.tokens -= 1; return Decision.allow((long) s.tokens); }
            return Decision.deny((long) Math.ceil((1 - s.tokens) / perMs));      // time until one full token
        }
    }

    /**
     * 5. LEAKY BUCKET (as a meter): the bucket holds at most `capacity` requests and leaks at a
     *    constant rate. Accept if there's room. Output rate is smooth - no bursts downstream.
     *    (The queue version actually DELAYS requests; this "meter" version just accepts/rejects.)
     */
    static class LeakyBucketLimiter extends PerKeyLimiter<LeakyBucketLimiter.State> {
        static class State { double water; long last; State(long l) { last = l; } }
        private final long capacity; private final double leakPerMs;
        LeakyBucketLimiter(long capacity, double leakPerSecond, LongSupplier clock) { super(clock); this.capacity = capacity; this.leakPerMs = leakPerSecond / 1000.0; }
        protected State newState() { return new State(clock.getAsLong()); }
        protected Decision decide(State s, long now) {
            s.water = Math.max(0, s.water - (now - s.last) * leakPerMs);       // leaked since last time
            s.last = now;
            if (s.water + 1 <= capacity) { s.water += 1; return Decision.allow((long) (capacity - s.water)); }
            return Decision.deny((long) Math.ceil((s.water + 1 - capacity) / leakPerMs));
        }
    }

    // =====================================================================
    // [config] factory, tiers, composition
    // =====================================================================

    enum Algorithm { FIXED_WINDOW, SLIDING_LOG, SLIDING_COUNTER, TOKEN_BUCKET, LEAKY_BUCKET }

    /** A rule from configuration: "100 requests per 60s using a token bucket". */
    record Rule(Algorithm algorithm, int limit, long windowMs) {}

    /** FACTORY: config -> limiter. Callers never write `new TokenBucketLimiter(...)`. */
    static class RateLimiterFactory {
        static RateLimiter create(Rule r, LongSupplier clock) {
            return switch (r.algorithm()) {
                case FIXED_WINDOW -> new FixedWindowLimiter(r.limit(), r.windowMs(), clock);
                case SLIDING_LOG -> new SlidingLogLimiter(r.limit(), r.windowMs(), clock);
                case SLIDING_COUNTER -> new SlidingCounterLimiter(r.limit(), r.windowMs(), clock);
                case TOKEN_BUCKET -> new TokenBucketLimiter(r.limit(), r.limit() * 1000.0 / r.windowMs(), clock);
                case LEAKY_BUCKET -> new LeakyBucketLimiter(r.limit(), r.limit() * 1000.0 / r.windowMs(), clock);
            };
        }
    }

    enum Plan { FREE, PRO, ENTERPRISE }

    /** Different limits per customer plan; the plan lookup is injected (e.g. from an API-key store). */
    static class TieredRateLimiter implements RateLimiter {
        private final Map<Plan, RateLimiter> byPlan = new EnumMap<>(Plan.class);
        private final Function<String, Plan> planOf;
        TieredRateLimiter(Map<Plan, Rule> rules, Function<String, Plan> planOf, LongSupplier clock) {
            rules.forEach((p, r) -> byPlan.put(p, RateLimiterFactory.create(r, clock)));
            this.planOf = planOf;
        }
        public Decision tryAcquire(String key) { return byPlan.get(planOf.apply(key)).tryAcquire(key); }
    }

    /**
     * COMPOSITE: a request must pass EVERY limiter, e.g. 10/s per user AND 1000/s for the
     * whole service (protects the backend even when many users are each under their limit).
     * The key mapper lets the global limiter use one shared key.
     * Caveat (say it!): if limiter 1 accepts and limiter 2 rejects, limiter 1 already counted
     * the request. Acceptable for protection; a strict version would need check-then-commit.
     */
    static class CompositeRateLimiter implements RateLimiter {
        record Layer(RateLimiter limiter, UnaryOperator<String> keyMapper) {}
        private final List<Layer> layers;
        CompositeRateLimiter(List<Layer> layers) { this.layers = List.copyOf(layers); }
        public Decision tryAcquire(String key) {
            long remaining = Long.MAX_VALUE;
            for (Layer l : layers) {
                Decision d = l.limiter().tryAcquire(l.keyMapper().apply(key));
                if (!d.allowed()) return d;                              // the first rejection wins
                remaining = Math.min(remaining, d.remaining());
            }
            return Decision.allow(remaining);
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static class FakeClock implements LongSupplier {
        long now = 1_000_000;                                            // aligned to a window boundary for readability
        public long getAsLong() { return now; }
        void advance(long ms) { now += ms; }
        void set(long ms) { now = ms; }
    }

    static int burst(RateLimiter rl, String key, int n) {
        int ok = 0;
        for (int i = 0; i < n; i++) if (rl.tryAcquire(key).allowed()) ok++;
        return ok;
    }

    public static void main(String[] args) throws Exception {
        FakeClock clock = new FakeClock();

        System.out.println("1. Fixed window: simple, but 2x burst at the boundary");
        RateLimiter fixed = new FixedWindowLimiter(5, 1000, clock);
        check("5 of 8 in one window", 5, burst(fixed, "u", 8));
        Decision d = fixed.tryAcquire("u");
        check("retry-after = rest of the window", 1000L, d.retryAfterMs());
        check("other users unaffected", 5, burst(fixed, "v", 5));
        clock.set(2_000_999);                                            // last ms of a window
        int edge = burst(fixed, "w", 5);
        clock.advance(1);                                                // first ms of the next window
        edge += burst(fixed, "w", 5);
        check("10 requests within 2 ms (the flaw)", 10, edge);

        System.out.println("2. Sliding window log: exact, no boundary burst");
        RateLimiter log = new SlidingLogLimiter(5, 1000, clock);
        clock.set(3_000_999);
        int s = burst(log, "w", 5);
        clock.advance(1);
        s += burst(log, "w", 5);
        check("only 5 within any 1s span", 5, s);
        clock.advance(1000);
        check("after the window passes", 5, burst(log, "w", 5));

        System.out.println("3. Sliding window counter: O(1) approximation");
        RateLimiter counter = new SlidingCounterLimiter(10, 1000, clock);
        clock.set(5_000_000);
        check("10 in the first window", 10, burst(counter, "x", 12));
        clock.advance(1500);                                             // halfway into the next window
        check("~half the previous still counts -> 5", 5, burst(counter, "x", 10));

        System.out.println("4. Token bucket: burst up to capacity, then the refill rate");
        RateLimiter tb = new TokenBucketLimiter(10, 5, clock);           // burst 10, 5/sec
        clock.set(7_000_000);
        check("initial burst of 10", 10, burst(tb, "t", 15));
        Decision denied = tb.tryAcquire("t");
        check("retry-after = 200ms (1 token at 5/s)", 200L, denied.retryAfterMs());
        clock.advance(1000);
        check("1s later: 5 tokens", 5, burst(tb, "t", 10));
        clock.advance(60_000);
        check("idle a minute: capped at capacity", 10, burst(tb, "t", 20));

        System.out.println("5. Leaky bucket: smooth output");
        RateLimiter lb = new LeakyBucketLimiter(3, 1, clock);            // holds 3, leaks 1/sec
        clock.set(9_000_000);
        check("bucket holds 3", 3, burst(lb, "l", 10));
        clock.advance(1000);
        check("1s later exactly 1 more", 1, burst(lb, "l", 10));

        System.out.println("6. Factory + tiers + composite (per-user AND global)");
        Map<String, Plan> plans = Map.of("free-user", Plan.FREE, "pro-user", Plan.PRO);
        RateLimiter tiered = new TieredRateLimiter(Map.of(
                Plan.FREE, new Rule(Algorithm.TOKEN_BUCKET, 3, 1000),
                Plan.PRO, new Rule(Algorithm.TOKEN_BUCKET, 20, 1000),
                Plan.ENTERPRISE, new Rule(Algorithm.SLIDING_LOG, 1000, 1000)), plans::get, clock);
        check("free plan", 3, burst(tiered, "free-user", 50));
        check("pro plan", 20, burst(tiered, "pro-user", 50));
        RateLimiter global = RateLimiterFactory.create(new Rule(Algorithm.FIXED_WINDOW, 25, 1000), clock);
        RateLimiter perUser = RateLimiterFactory.create(new Rule(Algorithm.FIXED_WINDOW, 10, 1000), clock);
        RateLimiter both = new CompositeRateLimiter(List.of(
                new CompositeRateLimiter.Layer(perUser, k -> k),
                new CompositeRateLimiter.Layer(global, k -> "GLOBAL")));
        clock.set(20_000_000);
        int total = 0;
        for (String u : List.of("a", "b", "c", "d")) total += burst(both, u, 10);
        check("4 users x 10 capped globally at 25", 25, total);

        System.out.println("7. Concurrency: 16 threads x 1000 calls on one key, limit 100");
        for (Algorithm a : Algorithm.values()) {
            RateLimiter rl = RateLimiterFactory.create(new Rule(a, 100, 1_000_000), clock);   // huge window: no refill during the test
            clock.set(100_000_000);
            AtomicInteger ok = new AtomicInteger();
            CountDownLatch go = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(16);
            for (int t = 0; t < 16; t++) pool.submit(() -> { go.await(); for (int i = 0; i < 1000; i++) if (rl.tryAcquire("hot").allowed()) ok.incrementAndGet(); return null; });
            go.countDown();
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            check(a + ": exactly 100 allowed", 100, ok.get());
        }
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
