/*
 * ============================================================================
 *  HLD 01 : SCALABILITY + LOAD BALANCING  (simulated, measured)
 * ============================================================================
 *  Run:  java LoadBalancing.java          (concepts: README.md)
 *
 *     [algorithms]  RoundRobin, WeightedRoundRobin (nginx "smooth" variant), Random,
 *                   LeastConnections, PowerOfTwoChoices, ConsistentHash (affinity)
 *     [health]      HealthChecker: a backend failing N checks is taken out of rotation
 *     [simulation]  discrete-event queueing sim: backends with DIFFERENT speeds, requests with
 *                   random service times -> mean / p99 latency per algorithm
 *     [state]       why services must be STATELESS: in-memory sessions die with the server,
 *                   an external session store survives
 *
 *  WHAT THE NUMBERS SHOW
 *    - Round robin ignores load: with one slow box, its queue grows and p99 explodes.
 *    - Least-connections and power-of-two-choices adapt to slow servers automatically.
 *    - P2C ("pick 2 at random, take the less loaded") is almost as good as full least-conn
 *      with O(1) work and no global view - what Envoy/NGINX/Finagle use at scale.
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class LoadBalancing {

    // =====================================================================
    // [algorithms]
    // =====================================================================

    static final class Backend {
        final String id;
        final double speed;             // 1.0 = normal; 0.4 = a slow/old box
        final int weight;               // what an operator configured (for weighted RR)
        boolean healthy = true;
        int inFlight;                   // outstanding requests (for least-connections)
        double freeAt;                  // single FIFO server: when its queue drains
        Backend(String id, double speed, int weight) { this.id = id; this.speed = speed; this.weight = weight; }
    }

    interface Balancer { Backend pick(List<Backend> healthy, String key); }

    static class RoundRobin implements Balancer {
        private int i;
        public Backend pick(List<Backend> b, String k) { return b.get(Math.floorMod(i++, b.size())); }
    }

    static class RandomPick implements Balancer {
        private final Random r; RandomPick(Random r) { this.r = r; }
        public Backend pick(List<Backend> b, String k) { return b.get(r.nextInt(b.size())); }
    }

    /**
     * nginx's SMOOTH weighted round robin: each pick, every backend's current += weight;
     * choose the max; subtract the total from the chosen one. Weights 5,1,1 give
     * a a b a c a a (spread out) instead of a a a a a b c (bursty).
     */
    static class SmoothWeightedRoundRobin implements Balancer {
        private final Map<String, Integer> current = new HashMap<>();
        public Backend pick(List<Backend> b, String k) {
            int total = 0;
            Backend best = null;
            for (Backend x : b) {
                int c = current.merge(x.id, x.weight, Integer::sum);
                total += x.weight;
                if (best == null || c > current.get(best.id)) best = x;
            }
            current.merge(best.id, -total, Integer::sum);
            return best;
        }
    }

    static class LeastConnections implements Balancer {
        public Backend pick(List<Backend> b, String k) { return b.stream().min(Comparator.comparingInt((Backend x) -> x.inFlight).thenComparing(x -> x.id)).get(); }
    }

    /** Pick two at random, send to the less loaded. Exponentially better than random, O(1). */
    static class PowerOfTwoChoices implements Balancer {
        private final Random r; PowerOfTwoChoices(Random r) { this.r = r; }
        public Backend pick(List<Backend> b, String k) {
            Backend x = b.get(r.nextInt(b.size())), y = b.get(r.nextInt(b.size()));
            return x.inFlight <= y.inFlight ? x : y;
        }
    }

    /**
     * Consistent hashing on a request key (user id / session id): the same key goes to the
     * same backend -> local caches stay warm ("affinity"). When a backend leaves, only ITS
     * keys move. (Deep dive with virtual nodes: 02-Databases / 11-Distributed-Cache.)
     */
    static class ConsistentHash implements Balancer {
        private final int vnodes;
        ConsistentHash(int vnodes) { this.vnodes = vnodes; }
        public Backend pick(List<Backend> b, String key) {
            TreeMap<Integer, Backend> ring = new TreeMap<>();               // rebuilt per call for clarity; cache it in real code
            for (Backend x : b) for (int v = 0; v < vnodes; v++) ring.put(hash(x.id + "#" + v), x);
            Map.Entry<Integer, Backend> e = ring.ceilingEntry(hash(key));
            return (e != null ? e : ring.firstEntry()).getValue();
        }
        static int hash(String s) { int h = s.hashCode() * 0x9E3779B9; return h ^ (h >>> 15); }
    }

    // =====================================================================
    // [health]
    // =====================================================================

    /** Active health checks: N consecutive failures -> out; M consecutive successes -> back in. */
    static class HealthChecker {
        private final int failThreshold, passThreshold;
        private final Map<String, int[]> streaks = new HashMap<>();        // id -> {fails, passes}
        HealthChecker(int fail, int pass) { failThreshold = fail; passThreshold = pass; }
        void report(Backend b, boolean ok) {
            int[] s = streaks.computeIfAbsent(b.id, k -> new int[2]);
            if (ok) { s[1]++; s[0] = 0; if (!b.healthy && s[1] >= passThreshold) b.healthy = true; }
            else { s[0]++; s[1] = 0; if (b.healthy && s[0] >= failThreshold) b.healthy = false; }
        }
    }

    // =====================================================================
    // [simulation]
    // =====================================================================

    record Stats(double mean, double p99, Map<String, Integer> share) {}

    /**
     * Requests arrive (Poisson); each needs `work` units; a backend with speed s takes work/s.
     * Each backend is a FIFO queue: start = max(arrival, freeAt). We track in-flight counts by
     * replaying completions in time order before each arrival.
     */
    static Stats simulate(List<Backend> backends, Balancer lb, int requests, double arrivalRate, long seed) {
        Random r = new Random(seed);
        for (Backend b : backends) { b.inFlight = 0; b.freeAt = 0; }
        PriorityQueue<double[]> completions = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));   // {time, backendIndex}
        double t = 0;
        double[] lat = new double[requests];
        Map<String, Integer> share = new TreeMap<>();
        for (int i = 0; i < requests; i++) {
            t += -Math.log(1 - r.nextDouble()) / arrivalRate;               // exponential inter-arrival
            while (!completions.isEmpty() && completions.peek()[0] <= t) backends.get((int) completions.poll()[1]).inFlight--;
            List<Backend> healthy = backends.stream().filter(x -> x.healthy).toList();
            Backend b = lb.pick(healthy, "user" + r.nextInt(10_000));
            double work = -Math.log(1 - r.nextDouble());                    // mean 1 unit
            double start = Math.max(t, b.freeAt), end = start + work / b.speed;
            b.freeAt = end;
            b.inFlight++;
            completions.add(new double[]{end, backends.indexOf(b)});
            lat[i] = end - t;
            share.merge(b.id, 1, Integer::sum);
        }
        Arrays.sort(lat);
        return new Stats(Arrays.stream(lat).average().orElse(0), lat[(int) (requests * 0.99)], share);
    }

    static List<Backend> fleet() {
        return List.of(new Backend("a", 1.0, 5), new Backend("b", 1.0, 5), new Backend("c", 1.0, 5), new Backend("slow", 0.4, 2));
    }

    // =====================================================================
    // [state]
    // =====================================================================

    /** A server that keeps sessions in its own memory: sticky sessions are mandatory, and a crash logs everyone out. */
    static class StatefulServer {
        final Map<String, String> sessions = new HashMap<>();
        boolean alive = true;
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Algorithms on 3 normal servers + 1 slow server (total capacity 3.4, load 2.8 req/unit)");
        Map<String, Stats> results = new LinkedHashMap<>();
        results.put("round-robin", simulate(fleet(), new RoundRobin(), 200_000, 2.8, 1));
        results.put("random", simulate(fleet(), new RandomPick(new Random(2)), 200_000, 2.8, 1));
        results.put("weighted-rr", simulate(fleet(), new SmoothWeightedRoundRobin(), 200_000, 2.8, 1));
        results.put("least-conn", simulate(fleet(), new LeastConnections(), 200_000, 2.8, 1));
        results.put("p2c", simulate(fleet(), new PowerOfTwoChoices(new Random(3)), 200_000, 2.8, 1));
        results.forEach((k, s) -> System.out.printf("   %-12s mean %7.2f   p99 %8.2f   share %s%n", k, s.mean(), s.p99(), s.share()));
        check("round robin: slow box overloaded -> worst p99", true, results.get("round-robin").p99() > 5 * results.get("least-conn").p99());
        check("least-conn sends the slow box less traffic", true, results.get("least-conn").share().get("slow") < results.get("round-robin").share().get("slow"));
        check("p2c within 2x of least-conn p99", true, results.get("p2c").p99() < 2 * results.get("least-conn").p99());

        System.out.println("2. Smooth weighted round robin spreads picks out");
        SmoothWeightedRoundRobin swrr = new SmoothWeightedRoundRobin();
        List<Backend> w = List.of(new Backend("a", 1, 5), new Backend("b", 1, 1), new Backend("c", 1, 1));
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < 7; i++) seq.append(swrr.pick(w, "").id);
        check("weights 5:1:1", "aabacaa", seq.toString());

        System.out.println("3. Health checks take a dead backend out of rotation");
        List<Backend> pool = new ArrayList<>(fleet());
        HealthChecker hc = new HealthChecker(3, 2);
        Backend dead = pool.get(0);
        for (int i = 0; i < 2; i++) hc.report(dead, false);
        check("2 failures: still in (avoid flapping)", true, dead.healthy);
        hc.report(dead, false);
        check("3 failures: removed", false, dead.healthy);
        Stats s = simulate(pool, new LeastConnections(), 50_000, 2.0, 4);
        check("no traffic to the dead backend", null, s.share().get("a"));
        hc.report(dead, true); hc.report(dead, true);
        check("2 passes: back in rotation", true, dead.healthy);

        System.out.println("4. Consistent hashing: affinity, and minimal movement when a backend dies");
        ConsistentHash ch = new ConsistentHash(100);
        List<Backend> four = fleet(), three = four.subList(0, 3);           // "slow" removed
        int moved = 0, total = 10_000, wasOnSlow = 0;
        for (int i = 0; i < total; i++) {
            String key = "user" + i;
            String before = ch.pick(four, key).id, after = ch.pick(three, key).id;
            if (before.equals("slow")) wasOnSlow++;
            else if (!before.equals(after)) moved++;
        }
        check("same key -> same backend (session affinity)", ch.pick(four, "user42").id, ch.pick(four, "user42").id);
        check("only keys of the removed backend moved", 0, moved);
        System.out.printf("   %d of %d keys (%.0f%%) were on the removed backend and moved; modulo hashing would move ~75%%%n", wasOnSlow, total, 100.0 * wasOnSlow / total);

        System.out.println("5. Stateless vs stateful services");
        StatefulServer s1 = new StatefulServer(), s2 = new StatefulServer();
        s1.sessions.put("asha", "cart=3 items");
        s1.alive = false;                                                     // deploy/crash
        check("in-memory session lost with the server", null, s2.sessions.get("asha"));
        Map<String, String> redis = new HashMap<>();                         // external session store
        redis.put("asha", "cart=3 items");
        check("external store: any server can serve asha", "cart=3 items", redis.get("asha"));
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
