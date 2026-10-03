/*
 * ============================================================================
 *  HLD 05 : CAP, PACELC AND CONSISTENCY MODELS  (simulated)
 * ============================================================================
 *  Run:  java ConsistencyModels.java          (concepts: README.md)
 *
 *     [cap]      a 3-node cluster during a NETWORK PARTITION:
 *                  CP (majority quorum): the minority side REFUSES writes -> unavailable, consistent
 *                  AP (accept anywhere): both sides accept -> available, divergent
 *     [lww]      last-write-wins by timestamp + CLOCK SKEW = the later write silently lost
 *     [vclock]   vector clocks: tell "happened-before" from "concurrent"; keep SIBLINGS
 *     [crdt]     G-Counter, PN-Counter, OR-Set: replicas merge in ANY order and converge
 *     [pacelc]   Else (no partition) Latency vs Consistency: waiting for W of N replicas
 *                  makes the p99 grow with W
 * ============================================================================
 */

import java.util.*;

public class ConsistencyModels {

    // =====================================================================
    // [cap]
    // =====================================================================

    enum Mode { CP, AP }

    /** 3 replicas of one register. `side[i]` = which side of the partition node i is on. */
    static final class Cluster {
        final Mode mode;
        final String[] value = new String[3];
        final long[] version = new long[3];
        int[] side = {0, 0, 0};                                         // all connected
        Cluster(Mode m) { mode = m; }

        void partition(int... sides) { side = sides; }
        void heal() { side = new int[]{0, 0, 0}; }

        /** A client attached to node `via` writes. Returns false if the system refuses (unavailable). */
        boolean write(int via, String v, long ts) {
            List<Integer> reachable = new ArrayList<>();
            for (int i = 0; i < 3; i++) if (side[i] == side[via]) reachable.add(i);
            if (mode == Mode.CP && reachable.size() < 2) return false;  // no majority -> refuse (choose C over A)
            for (int i : reachable) { value[i] = v; version[i] = ts; }
            return true;
        }

        /** CP read needs a majority too; AP reads whatever the local node has. */
        Optional<String> read(int via) {
            int reachable = 0;
            for (int i = 0; i < 3; i++) if (side[i] == side[via]) reachable++;
            if (mode == Mode.CP && reachable < 2) return Optional.empty();
            return Optional.ofNullable(value[via]);
        }

        /** After healing, AP replicas reconcile - here by last-write-wins on the version/timestamp. */
        void antiEntropyLww() {
            int newest = 0;
            for (int i = 1; i < 3; i++) if (version[i] > version[newest]) newest = i;
            for (int i = 0; i < 3; i++) { value[i] = value[newest]; version[i] = version[newest]; }
        }
    }

    // =====================================================================
    // [vclock]
    // =====================================================================

    /** node -> counter. a < b if every entry of a <= b and at least one is smaller. */
    record VClock(Map<String, Integer> c) {
        static VClock empty() { return new VClock(Map.of()); }
        VClock tick(String node) { Map<String, Integer> m = new TreeMap<>(c); m.merge(node, 1, Integer::sum); return new VClock(m); }
        VClock merge(VClock o) { Map<String, Integer> m = new TreeMap<>(c); o.c.forEach((k, v) -> m.merge(k, v, Math::max)); return new VClock(m); }
        boolean happenedBefore(VClock o) {
            boolean strictly = false;
            Set<String> keys = new HashSet<>(c.keySet()); keys.addAll(o.c.keySet());
            for (String k : keys) {
                int a = c.getOrDefault(k, 0), b = o.c.getOrDefault(k, 0);
                if (a > b) return false;
                if (a < b) strictly = true;
            }
            return strictly;
        }
        boolean concurrentWith(VClock o) { return !happenedBefore(o) && !o.happenedBefore(this) && !c.equals(o.c); }
    }

    record VersionedValue(Set<String> items, VClock clock) {}

    /** Dynamo-style store: a write that's concurrent with an existing version keeps BOTH (siblings). */
    static List<VersionedValue> putWithSiblings(List<VersionedValue> existing, VersionedValue incoming) {
        List<VersionedValue> out = new ArrayList<>();
        for (VersionedValue v : existing) if (!v.clock().happenedBefore(incoming.clock())) out.add(v);   // drop what it supersedes
        out.add(incoming);
        return out;
    }

    // =====================================================================
    // [crdt]
    // =====================================================================

    /** Grow-only counter: one slot per replica; value = sum; merge = per-slot max. Commutative, idempotent. */
    static final class GCounter {
        final Map<String, Long> slots = new TreeMap<>();
        void inc(String replica, long n) { slots.merge(replica, n, Long::sum); }
        long value() { return slots.values().stream().mapToLong(Long::longValue).sum(); }
        void merge(GCounter o) { o.slots.forEach((k, v) -> slots.merge(k, v, Math::max)); }
        GCounter copy() { GCounter g = new GCounter(); g.slots.putAll(slots); return g; }
    }

    /** Increment AND decrement: two G-Counters. Likes/unlikes, stock reservations. */
    static final class PNCounter {
        final GCounter p = new GCounter(), n = new GCounter();
        void inc(String r) { p.inc(r, 1); }
        void dec(String r) { n.inc(r, 1); }
        long value() { return p.value() - n.value(); }
        void merge(PNCounter o) { p.merge(o.p); n.merge(o.n); }
    }

    /** Observed-remove set: each add gets a unique tag; remove deletes only the tags it has SEEN. Add-wins on concurrency. */
    static final class ORSet {
        final Map<String, Set<String>> adds = new TreeMap<>(), removes = new TreeMap<>();
        private static int tagSeq;
        void add(String e) { adds.computeIfAbsent(e, k -> new HashSet<>()).add("t" + (++tagSeq)); }
        void remove(String e) { removes.computeIfAbsent(e, k -> new HashSet<>()).addAll(adds.getOrDefault(e, Set.of())); }
        Set<String> value() {
            Set<String> out = new TreeSet<>();
            adds.forEach((e, tags) -> { Set<String> live = new HashSet<>(tags); live.removeAll(removes.getOrDefault(e, Set.of())); if (!live.isEmpty()) out.add(e); });
            return out;
        }
        void merge(ORSet o) {
            o.adds.forEach((e, t) -> adds.computeIfAbsent(e, k -> new HashSet<>()).addAll(t));
            o.removes.forEach((e, t) -> removes.computeIfAbsent(e, k -> new HashSet<>()).addAll(t));
        }
    }

    // =====================================================================
    // [pacelc]
    // =====================================================================

    /** p99 of "wait for the W-th fastest of N replica acks", replica latency ~ 1 ms + long tail. */
    static double p99QuorumLatency(int n, int w, int trials, Random rnd) {
        double[] lat = new double[trials];
        for (int t = 0; t < trials; t++) {
            double[] acks = new double[n];
            for (int i = 0; i < n; i++) acks[i] = 1 + (rnd.nextDouble() < 0.05 ? 20 + rnd.nextDouble() * 80 : rnd.nextDouble());   // 5% slow replies (GC, disk)
            Arrays.sort(acks);
            lat[t] = acks[w - 1];
        }
        Arrays.sort(lat);
        return lat[(int) (trials * 0.99)];
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. CAP during a partition: nodes {0,1} | {2}");
        Cluster cp = new Cluster(Mode.CP), ap = new Cluster(Mode.AP);
        for (Cluster c : List.of(cp, ap)) { c.write(0, "v1", 1); c.partition(0, 0, 1); }
        check("CP: majority side still writes", true, cp.write(0, "v2", 2));
        check("CP: minority side REFUSES the write (unavailable)", false, cp.write(2, "v3", 3));
        check("CP: minority side can't read either", true, cp.read(2).isEmpty());
        check("AP: both sides accept writes (available)", true, ap.write(0, "v2", 2) && ap.write(2, "v3", 3));
        check("AP: sides disagree during the partition", "v2 vs v3", ap.read(0).get() + " vs " + ap.read(2).get());
        ap.heal(); ap.antiEntropyLww();
        check("AP after heal + LWW: converged, but v2 was discarded", "v3", ap.read(0).get());

        System.out.println("2. Last-write-wins with clock skew");
        Cluster skew = new Cluster(Mode.AP);
        skew.partition(0, 0, 1);
        skew.write(2, "price=100 (set at 10:00:05 by a node whose clock runs 10s FAST -> ts 10:00:15)", 1015);
        skew.write(0, "price=120 (really set later, at 10:00:08, ts 10:00:08)", 1008);
        skew.heal(); skew.antiEntropyLww();
        check("the REAL last write (120) was lost", true, skew.read(0).get().startsWith("price=100"));

        System.out.println("3. Vector clocks detect concurrency (shopping cart)");
        VClock base = VClock.empty().tick("A");                                   // cart created on A
        VersionedValue v0 = new VersionedValue(Set.of("milk"), base);
        VersionedValue fromB = new VersionedValue(Set.of("milk", "eggs"), base.tick("B"));   // phone, via node B
        VersionedValue fromC = new VersionedValue(Set.of("milk", "bread"), base.tick("C"));  // laptop, via node C
        check("v0 happened before both edits", true, v0.clock().happenedBefore(fromB.clock()) && v0.clock().happenedBefore(fromC.clock()));
        check("the two edits are CONCURRENT", true, fromB.clock().concurrentWith(fromC.clock()));
        List<VersionedValue> stored = putWithSiblings(putWithSiblings(List.of(v0), fromB), fromC);
        check("store keeps 2 siblings, drops v0", 2, stored.size());
        Set<String> mergedItems = new TreeSet<>();
        VClock mergedClock = VClock.empty();
        for (VersionedValue s : stored) { mergedItems.addAll(s.items()); mergedClock = mergedClock.merge(s.clock()); }
        VersionedValue resolved = new VersionedValue(mergedItems, mergedClock.tick("A"));
        check("client merges siblings (union): nothing lost", "[bread, eggs, milk]", mergedItems);
        check("the resolved write supersedes both siblings", 1, putWithSiblings(stored, resolved).size());

        System.out.println("4. CRDTs: merge in any order, converge");
        GCounter a = new GCounter(), b = new GCounter(), c = new GCounter();
        a.inc("a", 5); b.inc("b", 3); c.inc("c", 7); a.inc("a", 1);
        GCounter x = a.copy(); x.merge(b); x.merge(c); x.merge(b);              // duplicates are harmless
        GCounter y = c.copy(); y.merge(a); y.merge(b);
        check("G-Counter: same value whatever the merge order", "16 = 16", x.value() + " = " + y.value());
        PNCounter likes1 = new PNCounter(), likes2 = new PNCounter();
        likes1.inc("r1"); likes1.inc("r1"); likes2.inc("r2"); likes2.dec("r2");
        likes1.merge(likes2); likes2.merge(likes1);
        check("PN-Counter converges: 2 + 1 - 1", "2/2", likes1.value() + "/" + likes2.value());
        ORSet s1 = new ORSet(), s2 = new ORSet();
        s1.add("apple"); s2.merge(s1);
        s2.remove("apple");                                                      // replica 2 removes the apple it saw
        s1.add("apple");                                                         // concurrently replica 1 re-adds it (new tag)
        s1.merge(s2); s2.merge(s1);
        check("OR-Set: concurrent add wins over remove, both replicas agree", "[apple]/[apple]", s1.value() + "/" + s2.value());

        System.out.println("5. PACELC: even WITHOUT a partition, consistency costs latency");
        Random rnd = new Random(1);
        double w1 = p99QuorumLatency(3, 1, 50_000, rnd), w2 = p99QuorumLatency(3, 2, 50_000, rnd), w3 = p99QuorumLatency(3, 3, 50_000, rnd);
        System.out.printf("   p99 write latency, N=3: W=1 %.1f ms, W=2 %.1f ms, W=3 %.1f ms%n", w1, w2, w3);
        check("waiting for all replicas hits the slow tail", true, w3 > 10 * w2);
        check("majority quorum barely worse than one ack", true, w2 < 3 * w1);
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
