/*
 * ============================================================================
 *  NETWORKING 09 : LOAD BALANCING, RESILIENCE AND DISTRIBUTED-SYSTEMS NETWORKING
 * ============================================================================
 *  Run:  java Resilience.java          (explanations: README.md in this folder)
 *
 *    1. balancing algorithms   round robin, smooth weighted RR, least connections, power of two choices
 *    2. consistent hashing     only ~1/N keys move when a node joins (vs ~all with hash mod N)
 *    3. service discovery      registry + heartbeats + TTL expiry; client-side balancing
 *    4. retries + backoff      exponential backoff with and without jitter (thundering herd)
 *    5. retry storms           amplification across layers; retry budgets
 *    6. circuit breaker        closed -> open -> half-open -> closed
 *    7. bulkheads              one slow dependency must not eat every thread
 *    8. deadlines              propagate the remaining budget; stop wasted work
 *    9. backpressure           bounded queue + rejection vs an unbounded queue (Little's law)
 *   10. idempotency            idempotency keys make a timed-out payment safe to retry
 *   11. pool exhaustion        pool size vs throughput x latency
 *   12. tail latency           fan-out amplifies P99
 *   13. partitions             CP vs AP during a network partition; eventual consistency
 *   14. RPC encoding           protobuf-style varints vs JSON
 * ============================================================================
 */

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

public class Resilience {

    // ---------------------------------------------------------------------
    // 1. balancing algorithms on heterogeneous backends (discrete-event-ish simulation)
    // ---------------------------------------------------------------------
    static double simulate(String algo, long seed) {
        double[] serviceMs = {10, 10, 40};                       // backend 3 is slow (noisy neighbour, GC, older hardware)
        int[] weights = {4, 4, 1};
        int[] currentWeight = new int[3];
        List<Deque<Double>> inFlight = List.of(new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>());   // completion times
        double[] busyUntil = new double[3];
        Random r = new Random(seed);
        double total = 0;
        int n = 20000, rr = 0;
        for (int i = 0; i < n; i++) {
            double now = i * 6.0;                                 // ~167 req/s offered; total capacity ~225 req/s
            for (int b = 0; b < 3; b++) { Deque<Double> q = inFlight.get(b); while (!q.isEmpty() && q.peekFirst() <= now) q.pollFirst(); }
            int pick = switch (algo) {
                case "round robin" -> rr++ % 3;
                case "weighted RR" -> {                           // nginx "smooth weighted round robin"
                    int best = 0, sum = 0;
                    for (int b = 0; b < 3; b++) { currentWeight[b] += weights[b]; sum += weights[b]; if (currentWeight[b] > currentWeight[best]) best = b; }
                    currentWeight[best] -= sum;
                    yield best;
                }
                case "least connections" -> {
                    int best = 0;
                    for (int b = 1; b < 3; b++) if (inFlight.get(b).size() < inFlight.get(best).size()) best = b;
                    yield best;
                }
                default -> {                                     // power of two random choices
                    int a = r.nextInt(3), b = r.nextInt(3);
                    yield inFlight.get(a).size() <= inFlight.get(b).size() ? a : b;
                }
            };
            double start = Math.max(now, busyUntil[pick]);       // single-worker FIFO queue per backend
            double svc = serviceMs[pick] * (0.5 + r.nextDouble());
            busyUntil[pick] = start + svc;
            inFlight.get(pick).addLast(busyUntil[pick]);
            total += busyUntil[pick] - now;
        }
        return total / n;
    }

    static void balancing() {
        section("1. Load-balancing algorithms (one backend is 4x slower)");
        Map<String, Double> res = new LinkedHashMap<>();
        for (String a : List.of("round robin", "weighted RR", "least connections", "power of two")) res.put(a, simulate(a, 1));
        res.forEach((k, v) -> show(String.format("%-18s mean latency %8.1f ms", k, v)));
        check("plain round robin overloads the slow backend (its queue grows without bound)", true, res.get("round robin") > 1000);
        check("load-aware algorithms keep latency ~1000x lower than round robin", true, res.get("least connections") < 20 && res.get("power of two") < 50);
        check("weighted RR helps when you KNOW the capacities", true, res.get("weighted RR") < res.get("round robin") / 10);
    }

    // ---------------------------------------------------------------------
    // 2. consistent hashing
    // ---------------------------------------------------------------------
    static int hash(String s) {                                   // MD5-based, like ketama: similar strings ("n1#3", "n1#4") must not cluster
        try {
            byte[] d = java.security.MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            return (d[0] & 0xff) << 24 | (d[1] & 0xff) << 16 | (d[2] & 0xff) << 8 | (d[3] & 0xff);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static String ringOwner(TreeMap<Integer, String> ring, String key) {
        Map.Entry<Integer, String> e = ring.ceilingEntry(hash(key));
        return (e == null ? ring.firstEntry() : e).getValue();
    }

    static TreeMap<Integer, String> ring(List<String> nodes, int vnodes) {
        TreeMap<Integer, String> r = new TreeMap<>();
        for (String n : nodes) for (int v = 0; v < vnodes; v++) r.put(hash(n + "#" + v), n);
        return r;
    }

    static void consistentHashing() {
        section("2. Consistent hashing");
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 20000; i++) keys.add("user:" + i);
        List<String> four = List.of("n1", "n2", "n3", "n4"), five = List.of("n1", "n2", "n3", "n4", "n5");
        long movedMod = keys.stream().filter(k -> Math.floorMod(hash(k), 4) != Math.floorMod(hash(k), 5)).count();
        TreeMap<Integer, String> r4 = ring(four, 150), r5 = ring(five, 150);
        long movedRing = keys.stream().filter(k -> !ringOwner(r4, k).equals(ringOwner(r5, k))).count();
        show(String.format("adding a 5th node: hash mod N moves %.1f%% of keys, consistent hashing moves %.1f%%", 100.0 * movedMod / keys.size(), 100.0 * movedRing / keys.size()));
        check("mod-N remaps ~80% (cache wipe-out / mass data movement)", true, movedMod > 0.75 * keys.size());
        check("consistent hashing remaps ~1/5 (only what the new node takes over)", true, Math.abs(movedRing / (double) keys.size() - 0.2) < 0.04);
        Map<String, Integer> load = new TreeMap<>();
        for (String k : keys) load.merge(ringOwner(r5, k), 1, Integer::sum);
        check("150 virtual nodes per server keep the load even (max/min < 1.4)", true, Collections.max(load.values()) / (double) Collections.min(load.values()) < 1.4);
    }

    // ---------------------------------------------------------------------
    // 3. service discovery
    // ---------------------------------------------------------------------
    static void discovery() {
        section("3. Service discovery with heartbeats and TTL");
        Map<String, Long> registry = new HashMap<>();                     // instance -> last heartbeat time
        long ttl = 30;
        for (String i : List.of("10.0.1.5:8080", "10.0.2.7:8080", "10.0.3.9:8080")) registry.put(i, 0L);
        for (long t = 10; t <= 60; t += 10) { registry.put("10.0.1.5:8080", t); registry.put("10.0.2.7:8080", t); }   // the 3rd instance crashed at t=0
        long now = 60;
        List<String> live = registry.entrySet().stream().filter(e -> now - e.getValue() <= ttl).map(Map.Entry::getKey).sorted().toList();
        check("an instance that stops heartbeating expires after the TTL", "[10.0.1.5:8080, 10.0.2.7:8080]", live.toString());
        int[] rr = {0};
        Supplier<String> clientSideLb = () -> live.get(rr[0]++ % live.size());   // client-side balancing over the discovered list
        check("client-side load balancing picks directly from the discovered instances", "10.0.1.5:8080 10.0.2.7:8080 10.0.1.5:8080", clientSideLb.get() + " " + clientSideLb.get() + " " + clientSideLb.get());
    }

    // ---------------------------------------------------------------------
    // 4. backoff and jitter
    // ---------------------------------------------------------------------
    static void backoff() {
        section("4. Retries with exponential backoff and jitter");
        int clients = 1000;
        Random r = new Random(4);
        Map<Long, Integer> noJitter = new TreeMap<>(), fullJitter = new TreeMap<>();
        for (int c = 0; c < clients; c++) {
            for (int attempt = 1; attempt <= 4; attempt++) {
                long cap = 100L << attempt;                               // 200, 400, 800, 1600 ms
                noJitter.merge(cap, 1, Integer::sum);                     // everyone retries at exactly the same instant
                fullJitter.merge((long) (r.nextDouble() * cap) / 10 * 10, 1, Integer::sum);   // random in [0, cap), 10 ms buckets
            }
        }
        int peakNo = Collections.max(noJitter.values()), peakJ = Collections.max(fullJitter.values());
        show(String.format("1000 clients fail at once and retry 4 times: peak requests in one 10 ms window -> no jitter %d, full jitter %d", peakNo, peakJ));
        check("without jitter, retries arrive in synchronized waves (thundering herd)", 1000, peakNo);
        check("full jitter spreads them out (peak ~9x lower)", true, peakJ * 5 < peakNo);
    }

    // ---------------------------------------------------------------------
    // 5. retry storms
    // ---------------------------------------------------------------------
    static void retryStorm() {
        section("5. Retry storms: amplification across layers");
        int layers = 3, attemptsPerLayer = 4;                              // 1 try + 3 retries at each of 3 layers
        int worst = (int) Math.pow(attemptsPerLayer, layers);
        check("a failing DB sees up to 4^3 = 64 attempts per user request", 64, worst);
        double baseRps = 1000, failureRate = 1.0;
        double withBudget = baseRps * (1 + 0.1);                           // retry budget: retries <= 10% of requests
        show(String.format("during an outage: naive retries -> %,.0f req/s at the bottom layer; a 10%% retry budget -> %,.0f req/s", baseRps * worst * failureRate, withBudget));
        check("a retry budget caps the extra load at +10%", true, withBudget / baseRps < 1.11);
        show("rules: retry at ONE layer (usually the edge/client), only idempotent calls, with backoff + jitter + budget");
    }

    // ---------------------------------------------------------------------
    // 6. circuit breaker
    // ---------------------------------------------------------------------
    static class CircuitBreaker {
        String state = "CLOSED";
        int failures = 0;
        long openedAt;
        final int threshold;
        final long coolDown;
        CircuitBreaker(int threshold, long coolDown) { this.threshold = threshold; this.coolDown = coolDown; }

        String call(long now, BooleanSupplier dependencyOk) {
            if (state.equals("OPEN")) {
                if (now - openedAt < coolDown) return "fast-fail";        // don't even try: protect the dependency and ourselves
                state = "HALF_OPEN";                                       // let ONE trial request through
            }
            boolean ok = dependencyOk.getAsBoolean();
            if (ok) { failures = 0; state = "CLOSED"; return "ok"; }
            failures++;
            if (state.equals("HALF_OPEN") || failures >= threshold) { state = "OPEN"; openedAt = now; }
            return "error";
        }
    }

    static void circuitBreaker() {
        section("6. Circuit breaker");
        CircuitBreaker cb = new CircuitBreaker(5, 30);
        boolean[] depUp = {false};
        List<String> results = new ArrayList<>();
        for (long t = 0; t < 10; t++) results.add(cb.call(t, () -> depUp[0]));
        show("dependency down, 10 calls: " + results);
        check("after 5 failures the breaker opens and fails fast", "[error, error, error, error, error, fast-fail, fast-fail, fast-fail, fast-fail, fast-fail]", results.toString());
        check("open: callers get instant errors instead of waiting for timeouts", "OPEN", cb.state);
        String probeWhileDown = cb.call(40, () -> depUp[0]);
        check("after the cool-down one probe goes through; it fails -> open again", "error OPEN", probeWhileDown + " " + cb.state);
        depUp[0] = true;
        String probeUp = cb.call(80, () -> depUp[0]);
        check("a successful probe in HALF_OPEN closes the breaker", "ok CLOSED", probeUp + " " + cb.state);
    }

    // ---------------------------------------------------------------------
    // 7. bulkheads
    // ---------------------------------------------------------------------
    static void bulkheads() {
        section("7. Bulkheads: isolate dependencies");
        int threads = 200;
        double slowLatencySec = 30, slowRps = 10, fastLatencySec = 0.05, fastRps = 500;
        double slowNeeds = slowRps * slowLatencySec;                        // Little's law: concurrency = rate x latency
        double fastNeeds = fastRps * fastLatencySec;
        boolean sharedPoolStarved = slowNeeds >= threads;
        check("Little's law: a 30 s-hanging dependency at 10 req/s needs 300 threads", 300.0, slowNeeds);
        check("shared 200-thread pool: the slow dependency takes them all -> healthy endpoints fail too", true, sharedPoolStarved);
        int slowPool = 20, fastPool = 180;
        check("with bulkheads (20 for the slow dep, 180 for the rest) the fast path still has room", true, fastNeeds < fastPool);
        show("plus a 2 s timeout on the slow dependency: it needs only 10 x 2 = 20 threads in the first place");
    }

    // ---------------------------------------------------------------------
    // 8. deadlines
    // ---------------------------------------------------------------------
    static void deadlines() {
        section("8. Timeouts and deadline propagation");
        double clientTimeout = 1000;
        double[] hopCost = {200, 300, 900};                                 // gateway -> service A -> service B (B is slow)
        double wastedNoDeadline = 0, elapsed = 0;
        for (double c : hopCost) { elapsed += c; if (elapsed > clientTimeout) wastedNoDeadline += Math.min(c, elapsed - clientTimeout); }
        check("without deadlines B keeps working 400 ms after the client already gave up", 400.0, wastedNoDeadline);
        double remaining = clientTimeout;
        List<String> decisions = new ArrayList<>();
        for (int i = 0; i < hopCost.length; i++) {
            if (hopCost[i] > remaining) { decisions.add("hop" + (i + 1) + ": only " + (int) remaining + " ms left -> fail fast"); break; }
            remaining -= hopCost[i];
            decisions.add("hop" + (i + 1) + ": ok, " + (int) remaining + " ms left");
        }
        show("with a propagated deadline (gRPC deadline / header): " + decisions);
        check("the last hop sees it can't finish and stops immediately", true, decisions.get(decisions.size() - 1).contains("fail fast"));
    }

    // ---------------------------------------------------------------------
    // 9. backpressure
    // ---------------------------------------------------------------------
    static void backpressure() {
        section("9. Backpressure: bounded queues and load shedding");
        double capacity = 100, arrival = 120;                               // req/s; overloaded by 20%
        int seconds = 60;
        double unboundedQueue = (arrival - capacity) * seconds;
        double unboundedWait = unboundedQueue / capacity;
        check("unbounded queue after 60 s of 20% overload: 1200 queued, every request waits 12 s", "1200 12.0", (int) unboundedQueue + " " + unboundedWait);
        int bound = 50;
        double boundedWait = bound / capacity, shed = (arrival - capacity) / arrival;
        show(String.format("bounded queue of %d: max wait %.1f s; %.0f%% of requests rejected fast (429/503) instead", bound, boundedWait, shed * 100));
        check("bounded queue: latency stays bounded; the excess is shed (and clients back off)", true, boundedWait < 1);
    }

    // ---------------------------------------------------------------------
    // 10. idempotency keys
    // ---------------------------------------------------------------------
    static void idempotency() {
        section("10. Idempotency keys");
        Map<String, String> processed = new HashMap<>();
        List<String> charges = new ArrayList<>();
        BiFunction<String, Integer, String> charge = (key, amount) -> processed.computeIfAbsent(key, k -> {
            charges.add("charge " + amount);                                // the side effect happens once per key
            return "receipt-" + charges.size();
        });
        String first = charge.apply("order-77-attempt", 500);                // response lost: client times out...
        String retry = charge.apply("order-77-attempt", 500);                // ...and retries with the SAME key
        check("the retry returns the original result", first, retry);
        check("the customer is charged exactly once", 1, charges.size());
        String noKey1 = "charge#" + UUID.randomUUID(), noKey2 = "charge#" + UUID.randomUUID();
        charge.apply(noKey1, 500);
        charge.apply(noKey2, 500);
        check("without a stable key, a retry is a second charge", 3, charges.size());
    }

    // ---------------------------------------------------------------------
    // 11. connection pool exhaustion
    // ---------------------------------------------------------------------
    static void poolExhaustion() {
        section("11. Connection pool exhaustion");
        int pool = 20;
        double rps = 400;
        double normalMs = 20, slowMs = 200;
        double needNormal = rps * normalMs / 1000, needSlow = rps * slowMs / 1000;
        check("normal: 400 req/s x 20 ms = 8 connections busy (pool 20 is fine)", 8.0, needNormal);
        check("DB slows to 200 ms: 80 needed > 20 in the pool -> requests queue for a connection", 80.0, needSlow);
        double maxThroughput = pool / (slowMs / 1000);
        show(String.format("max throughput with 20 connections at 200 ms = %.0f req/s < 400 offered: queue grows, P99 explodes", maxThroughput));
        check("the pool, not the CPU, becomes the bottleneck", true, maxThroughput < rps);
    }

    // ---------------------------------------------------------------------
    // 12. tail latency fan-out
    // ---------------------------------------------------------------------
    static void tailLatency() {
        section("12. Tail latency amplification (fan-out)");
        for (int n : List.of(1, 10, 100)) show(String.format("a request that fans out to %3d backends hits at least one P99-slow backend %.1f%% of the time", n, 100 * (1 - Math.pow(0.99, n))));
        check("fan-out 100: 63% of requests see a backend's P99", "63.4", String.format("%.1f", 100 * (1 - Math.pow(0.99, 100))));
        double pSlow = 0.01;
        double hedged = pSlow * pSlow;                                      // hedged request: send a 2nd copy after ~P95, take the first reply
        check("hedging (2 copies) turns a 1% slow rate into ~0.01%", "0.0100", String.format("%.4f", hedged * 100));
    }

    // ---------------------------------------------------------------------
    // 13. network partitions
    // ---------------------------------------------------------------------
    static void partitions() {
        section("13. Network partitions: CP vs AP");
        int[] a = {100}, b = {100};                                         // replicated stock count on two nodes
        boolean partitioned = true;
        String cpWrite = partitioned ? "rejected (can't reach a majority)" : "ok";
        check("CP system during a partition: refuses writes it can't replicate (consistent, less available)", "rejected (can't reach a majority)", cpWrite);
        a[0] -= 3;                                                          // AP: both sides keep accepting writes
        b[0] -= 5;
        check("AP system: both sides accept writes and DIVERGE", "97 vs 95", a[0] + " vs " + b[0]);
        int merged = 100 - (100 - a[0]) - (100 - b[0]);                     // reconcile by merging the decrements (CRDT-style counter)
        check("after healing, merging operations converges (eventual consistency)", 92, merged);
        show("last-write-wins would have kept 95 or 97 and silently LOST the other side's sales");
    }

    // ---------------------------------------------------------------------
    // 14. RPC encoding
    // ---------------------------------------------------------------------
    static byte[] varint(long v) {
        List<Byte> out = new ArrayList<>();
        while ((v & ~0x7FL) != 0) { out.add((byte) ((v & 0x7F) | 0x80)); v >>>= 7; }
        out.add((byte) v);
        byte[] b = new byte[out.size()];
        for (int i = 0; i < b.length; i++) b[i] = out.get(i);
        return b;
    }

    static void rpcEncoding() {
        section("14. RPC vs REST: binary encoding");
        String json = "{\"user_id\":150,\"quantity\":3,\"price_cents\":129900}";
        int proto = (1 + varint(150).length) + (1 + varint(3).length) + (1 + varint(129900).length);   // tag byte + varint per field
        check("varint(150) = 2 bytes (0x96 0x01), as in the protobuf docs", "[-106, 1]", Arrays.toString(varint(150)));
        show(String.format("same message: JSON %d bytes, protobuf-style %d bytes", json.getBytes(StandardCharsets.UTF_8).length, proto));
        check("binary encoding is several times smaller (and faster to parse)", true, json.length() > 4 * proto);
    }

    public static void main(String[] args) {
        balancing();
        consistentHashing();
        discovery();
        backoff();
        retryStorm();
        circuitBreaker();
        bulkheads();
        deadlines();
        backpressure();
        idempotency();
        poolExhaustion();
        tailLatency();
        partitions();
        rpcEncoding();
        summary();
    }

    // =====================================================================
    static int passed = 0, total = 0;
    static void section(String s) { System.out.println(s); }
    static void show(String s) { System.out.println("      " + s); }

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
