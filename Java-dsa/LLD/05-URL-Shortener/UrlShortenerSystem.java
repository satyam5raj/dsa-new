/*
 * ============================================================================
 *  LLD 05 : URL SHORTENER (the LLD half; the HLD half is capacity, sharding, caching)
 * ============================================================================
 *  Run:  java UrlShortenerSystem.java          (design notes: README.md)
 *
 *     [codec]      Base62: number <-> short string
 *     [keygen]     KeyGenerationStrategy:
 *                    CounterKeyGenerator   - global counter -> base62 (unique by construction)
 *                    RangeKeyGenerator     - each server leases a BLOCK of ids (no per-request coordination)
 *                    HashKeyGenerator      - hash(url) -> first 7 chars, collisions resolved by re-hashing with a salt
 *                    RandomKeyGenerator    - random 7 chars, retry on collision
 *     [storage]    UrlRepository (interface) + InMemoryUrlRepository (putIfAbsent = atomic insert)
 *     [service]    UrlShortenerService: shorten (custom alias, TTL, dedupe), resolve (redirect),
 *                  analytics via an observer
 *     [demo]       base62 round-trips, collisions forced with a weak hash, expiry, alias race,
 *                  16 threads creating 20,000 links with zero duplicates
 *
 *  CORE IDEA: a short code is just a NUMBER written in base 62 [0-9a-zA-Z].
 *     62^7 = 3.5 trillion codes in 7 characters. The design question is HOW TO PICK THE
 *     NUMBER so it's unique without a bottleneck:
 *       counter -> unique, but a single point of contention and PREDICTABLE (enumerable)
 *       ranges  -> counter without contention: lease 1,000 ids at a time per server
 *       hash    -> same URL -> same code (natural dedupe), but COLLISIONS must be handled
 *       random  -> unpredictable, collisions handled by retry (rare until the space fills up)
 *  Whatever the generator, the FINAL safety net is an atomic "insert if absent" in storage.
 * ============================================================================
 */

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class UrlShortenerSystem {

    // =====================================================================
    // [codec] Base62
    // =====================================================================

    static final class Base62 {
        private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        static String encode(long n) {
            if (n < 0) throw new IllegalArgumentException("negative");
            if (n == 0) return "0";
            StringBuilder sb = new StringBuilder();
            while (n > 0) { sb.append(ALPHABET.charAt((int) (n % 62))); n /= 62; }   // least significant digit first
            return sb.reverse().toString();
        }

        static long decode(String s) {
            long n = 0;
            for (char c : s.toCharArray()) {
                int d = ALPHABET.indexOf(c);
                if (d < 0) throw new IllegalArgumentException("bad char " + c);
                n = n * 62 + d;
            }
            return n;
        }

        static String padded(long n, int len) {                   // fixed-length codes look nicer: 0000a3F
            String s = encode(n);
            return s.length() >= len ? s : "0".repeat(len - s.length()) + s;
        }
    }

    // =====================================================================
    // [keygen] strategies
    // =====================================================================

    /** STRATEGY: how to produce a candidate short code. `attempt` lets hash/random strategies vary on retry. */
    interface KeyGenerationStrategy {
        String generate(String longUrl, int attempt);
        default boolean mayCollide() { return true; }
    }

    /** Global counter: unique by construction, no retries. Contention on one AtomicLong / DB sequence. */
    static class CounterKeyGenerator implements KeyGenerationStrategy {
        private final AtomicLong counter;
        CounterKeyGenerator(long start) { counter = new AtomicLong(start); }
        public String generate(String url, int attempt) { return Base62.padded(counter.getAndIncrement(), 7); }
        public boolean mayCollide() { return false; }
    }

    /**
     * RANGE ALLOCATION (a "ticket server" / ZooKeeper / DB sequence with step 1000):
     * each app server leases a block [start, start + size) ONCE, then hands out ids locally.
     * Coordination happens once per 1,000 links instead of on every request.
     * If a server dies, its unused ids are simply skipped (gaps are fine).
     */
    static class RangeCoordinator {                                // stands in for ZooKeeper / a DB row
        private final AtomicLong nextBlockStart = new AtomicLong(1_000_000);
        final AtomicInteger leases = new AtomicInteger();
        long lease(int size) { leases.incrementAndGet(); return nextBlockStart.getAndAdd(size); }
    }

    static class RangeKeyGenerator implements KeyGenerationStrategy {
        private final RangeCoordinator coordinator;
        private final int blockSize;
        private long next, end;                                     // guarded by `this`
        RangeKeyGenerator(RangeCoordinator c, int blockSize) { this.coordinator = c; this.blockSize = blockSize; }
        public synchronized String generate(String url, int attempt) {
            if (next >= end) { next = coordinator.lease(blockSize); end = next + blockSize; }
            return Base62.padded(next++, 7);
        }
        public boolean mayCollide() { return false; }
    }

    /**
     * Hash the URL (SHA-256), take the first 42 bits -> 7 base62 chars.
     * Same URL -> same code (natural dedupe). On a COLLISION (a different URL already owns the
     * code) re-hash with the attempt number as a salt.
     * The hash function is injectable so the demo can force collisions with a weak hash.
     */
    static class HashKeyGenerator implements KeyGenerationStrategy {
        private final ToLongFunction<String> hash;
        HashKeyGenerator(ToLongFunction<String> hash) { this.hash = hash; }
        HashKeyGenerator() { this(HashKeyGenerator::sha256Prefix); }
        public String generate(String url, int attempt) {
            String input = attempt == 0 ? url : url + "#" + attempt;    // salt on retry
            long h = hash.applyAsLong(input) & ((1L << 42) - 1);        // 42 bits < 62^7
            return Base62.padded(h % 3_521_614_606_208L, 7);           // 62^7
        }
        static long sha256Prefix(String s) {
            try {
                byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
                long v = 0;
                for (int i = 0; i < 8; i++) v = (v << 8) | (d[i] & 0xff);
                return v;
            } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        }
    }

    /** Random 7 chars: unguessable, retry on collision. Collision odds stay tiny until billions of links. */
    static class RandomKeyGenerator implements KeyGenerationStrategy {
        private final Random rnd;
        RandomKeyGenerator(Random rnd) { this.rnd = rnd; }
        public String generate(String url, int attempt) {
            return Base62.padded(Math.floorMod(rnd.nextLong(), 3_521_614_606_208L), 7);
        }
    }

    // =====================================================================
    // [storage]
    // =====================================================================

    record ShortUrl(String code, String longUrl, String owner, long createdAt, long expiresAt) {
        boolean expired(long now) { return expiresAt > 0 && now >= expiresAt; }
    }

    /** Behind an interface so the in-memory map can become DynamoDB/Cassandra/MySQL. */
    interface UrlRepository {
        /** Atomic insert: returns false if the code already exists. THE uniqueness guarantee. */
        boolean insertIfAbsent(ShortUrl u);
        Optional<ShortUrl> find(String code);
        void delete(String code);
    }

    static class InMemoryUrlRepository implements UrlRepository {
        private final ConcurrentHashMap<String, ShortUrl> byCode = new ConcurrentHashMap<>();
        public boolean insertIfAbsent(ShortUrl u) { return byCode.putIfAbsent(u.code(), u) == null; }
        public Optional<ShortUrl> find(String code) { return Optional.ofNullable(byCode.get(code)); }
        public void delete(String code) { byCode.remove(code); }
        int size() { return byCode.size(); }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    interface ClickListener { void onClick(String code, long at); }

    /** Analytics as an observer: LongAdder is built for many threads incrementing one counter. */
    static class ClickCounter implements ClickListener {
        final ConcurrentHashMap<String, LongAdder> clicks = new ConcurrentHashMap<>();
        public void onClick(String code, long at) { clicks.computeIfAbsent(code, k -> new LongAdder()).increment(); }
        long count(String code) { LongAdder a = clicks.get(code); return a == null ? 0 : a.sum(); }
    }

    static class UrlShortenerService {
        static final int MAX_ATTEMPTS = 8;
        private static final Set<String> RESERVED = Set.of("api", "admin", "login", "health");

        private final UrlRepository repo;
        private final KeyGenerationStrategy keygen;
        private final LongSupplier clock;
        private final List<ClickListener> listeners = new CopyOnWriteArrayList<>();
        private final ConcurrentHashMap<String, String> dedupe = new ConcurrentHashMap<>();   // owner|url -> code
        final AtomicInteger collisions = new AtomicInteger();

        UrlShortenerService(UrlRepository repo, KeyGenerationStrategy keygen, LongSupplier clock) {
            this.repo = repo; this.keygen = keygen; this.clock = clock;
        }

        void addListener(ClickListener l) { listeners.add(l); }

        String shorten(String longUrl, String owner) { return shorten(longUrl, owner, null, 0); }

        /**
         * @param alias custom code (optional)   @param ttlMs 0 = never expires
         * Idempotent per (owner, url) for non-custom links: shortening the same URL twice
         * returns the same code instead of burning a new one.
         */
        String shorten(String longUrl, String owner, String alias, long ttlMs) {
            validateUrl(longUrl);
            long now = clock.getAsLong();
            long expiresAt = ttlMs > 0 ? now + ttlMs : 0;
            if (alias != null) {
                validateAlias(alias);
                if (!repo.insertIfAbsent(new ShortUrl(alias, longUrl, owner, now, expiresAt)))
                    throw new IllegalStateException("Alias '" + alias + "' is taken");
                return alias;
            }
            String dedupeKey = owner + "|" + longUrl;
            String existing = dedupe.get(dedupeKey);
            if (existing != null && repo.find(existing).filter(u -> !u.expired(now)).isPresent()) return existing;

            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                String code = keygen.generate(longUrl, attempt);
                if (repo.insertIfAbsent(new ShortUrl(code, longUrl, owner, now, expiresAt))) {   // atomic
                    dedupe.put(dedupeKey, code);
                    return code;
                }
                // The code exists. For the hash strategy it may be OUR url (same url, other owner):
                // still a collision for this owner -> retry with a salt.
                collisions.incrementAndGet();
                if (!keygen.mayCollide()) throw new IllegalStateException("Generator produced a duplicate - bug");
            }
            throw new IllegalStateException("Could not allocate a code after " + MAX_ATTEMPTS + " attempts");
        }

        /** The redirect path: must be FAST (it's 100x more frequent than shorten). */
        Optional<String> resolve(String code) {
            long now = clock.getAsLong();
            Optional<ShortUrl> u = repo.find(code);
            if (u.isEmpty()) return Optional.empty();
            if (u.get().expired(now)) { repo.delete(code); return Optional.empty(); }    // lazy expiry
            listeners.forEach(l -> l.onClick(code, now));                 // async in production (Kafka)
            return Optional.of(u.get().longUrl());
        }

        void delete(String code, String requester) {
            ShortUrl u = repo.find(code).orElseThrow(() -> new IllegalArgumentException("No such code"));
            if (!u.owner().equals(requester)) throw new SecurityException("Not the owner");
            repo.delete(code);
        }

        private static void validateUrl(String url) {
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://")) || url.length() > 2048)
                throw new IllegalArgumentException("Invalid URL: " + url);
        }

        private static void validateAlias(String a) {
            if (!a.matches("[0-9a-zA-Z_-]{3,30}")) throw new IllegalArgumentException("Bad alias " + a);
            if (RESERVED.contains(a.toLowerCase())) throw new IllegalArgumentException("Reserved alias " + a);
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong(0);

        System.out.println("1. Base62");
        check("encode 125", "21", Base62.encode(125));
        check("62^7 - 1 fits 7 chars", "ZZZZZZZ", Base62.encode(3_521_614_606_207L));
        boolean ok = true;
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) { long n = Math.floorMod(r.nextLong(), Long.MAX_VALUE); ok &= Base62.decode(Base62.encode(n)) == n; }
        check("10,000 random round-trips", true, ok);

        System.out.println("2. Counter strategy: shorten, resolve, dedupe, analytics");
        UrlShortenerService svc = new UrlShortenerService(new InMemoryUrlRepository(), new CounterKeyGenerator(100_000), clock::get);
        ClickCounter clicks = new ClickCounter();
        svc.addListener(clicks);
        String c1 = svc.shorten("https://example.com/a/very/long/path", "alice");
        check("7-char code", 7, c1.length());
        check("resolve", "https://example.com/a/very/long/path", svc.resolve(c1).orElse(null));
        check("same url + owner -> same code", c1, svc.shorten("https://example.com/a/very/long/path", "alice"));
        check("another owner gets a new code", false, c1.equals(svc.shorten("https://example.com/a/very/long/path", "bob")));
        svc.resolve(c1); svc.resolve(c1);
        check("3 clicks counted", 3L, clicks.count(c1));
        check("unknown code -> 404", true, svc.resolve("nope123").isEmpty());

        System.out.println("3. Custom alias, validation, expiry, delete");
        check("alias", "launch", svc.shorten("https://example.com/launch", "alice", "launch", 0));
        expect("alias taken", () -> svc.shorten("https://other.com", "bob", "launch", 0));
        expect("reserved word", () -> svc.shorten("https://other.com", "bob", "admin", 0));
        expect("not a URL", () -> svc.shorten("javascript:alert(1)", "bob"));
        String temp = svc.shorten("https://example.com/sale", "alice", null, 60_000);
        clock.addAndGet(59_999);
        check("before expiry", true, svc.resolve(temp).isPresent());
        clock.addAndGet(1);
        check("after expiry -> gone", true, svc.resolve(temp).isEmpty());
        expect("only the owner can delete", () -> svc.delete("launch", "bob"));
        svc.delete("launch", "alice");
        check("deleted", true, svc.resolve("launch").isEmpty());

        System.out.println("4. Hash strategy: deterministic codes + collision handling");
        UrlShortenerService hashed = new UrlShortenerService(new InMemoryUrlRepository(), new HashKeyGenerator(), clock::get);
        String h1 = hashed.shorten("https://example.com/x", "a");
        check("same url hashes to the same code", h1, new HashKeyGenerator().generate("https://example.com/x", 0));
        // A deliberately terrible hash with only 4 possible values forces collisions:
        UrlShortenerService weak = new UrlShortenerService(new InMemoryUrlRepository(), new HashKeyGenerator(s -> Math.floorMod(s.hashCode(), 4)), clock::get);
        Set<String> codes = new HashSet<>();
        for (String suffix : List.of("a", "e", "i", "m"))                       // chars 4 apart -> same hash mod 4
            codes.add(weak.shorten("https://site.com/" + suffix, "a"));
        check("4 urls in a 4-code space all fit", 4, codes.size());
        check("collisions happened and were salted away", true, weak.collisions.get() > 0);
        System.out.println("   collisions resolved by salting: " + weak.collisions.get());
        expect("space exhausted -> clear error", () -> weak.shorten("https://site.com/99", "a"));

        System.out.println("5. Range allocation: 2 servers, coordination once per 1,000 ids");
        RangeCoordinator zk = new RangeCoordinator();
        InMemoryUrlRepository shared = new InMemoryUrlRepository();
        UrlShortenerService s1 = new UrlShortenerService(shared, new RangeKeyGenerator(zk, 1000), clock::get);
        UrlShortenerService s2 = new UrlShortenerService(shared, new RangeKeyGenerator(zk, 1000), clock::get);
        for (int i = 0; i < 2500; i++) { s1.shorten("https://a.com/" + i, "u"); s2.shorten("https://b.com/" + i, "u"); }
        check("5,000 links, all unique", 5000, shared.size());
        check("only 6 range leases", 6, zk.leases.get());

        System.out.println("6. Concurrency: 16 threads create 20,000 links; 10 threads race for one alias");
        for (KeyGenerationStrategy k : List.of(new CounterKeyGenerator(0), new RandomKeyGenerator(new Random(5)), new HashKeyGenerator())) {
            InMemoryUrlRepository repo = new InMemoryUrlRepository();
            UrlShortenerService s = new UrlShortenerService(repo, k, clock::get);
            ExecutorService pool = Executors.newFixedThreadPool(16);
            Set<String> out = ConcurrentHashMap.newKeySet();
            for (int t = 0; t < 16; t++) { int tt = t; pool.submit(() -> { for (int i = 0; i < 1250; i++) out.add(s.shorten("https://x.com/" + tt + "/" + i, "u")); }); }
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
            check(k.getClass().getSimpleName() + ": 20,000 unique", 20_000, out.size() == repo.size() ? out.size() : -1);
        }
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int n = i;
            Thread t = new Thread(() -> { try { go.await(); svc.shorten("https://t.com/" + n, "user" + n, "promo", 0); winners.incrementAndGet(); } catch (Exception ignored) {} });
            t.start(); ts.add(t);
        }
        go.countDown();
        for (Thread t : ts) t.join();
        check("exactly one gets the alias", 1, winners.get());
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); System.out.println("         (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")"); }
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
