/*
 * ============================================================================
 *  HLD 02 : DATABASES - INDEXING, SHARDING, REPLICATION, QUORUMS, MIGRATIONS
 * ============================================================================
 *  Run:  java DatabaseScaling.java          (concepts: README.md)
 *
 *     [index]        full scan vs B-tree-like index (TreeMap), composite index LEFTMOST-PREFIX rule
 *     [sharding]     hash % N vs CONSISTENT HASHING: keys moved when adding a shard;
 *                    virtual nodes -> balance; RANGE sharding hot spot with sequential keys
 *     [replication]  async leader-follower: replication LAG breaks read-your-writes; fixes;
 *                    failover with unreplicated writes = data loss (RPO > 0)
 *     [quorum]       N replicas, W write acks, R read replicas: R + W > N => reads see the latest
 *     [migration]    dual writes diverge on partial failure; backfill + CDC + verify does not
 *
 *  Everything is an in-memory model, so the effects are measurable and repeatable.
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class DatabaseScaling {

    // =====================================================================
    // [index]
    // =====================================================================

    record Row(int id, String country, int age, String name) {}

    static long comparisons;

    static List<Row> fullScan(List<Row> table, Predicate<Row> p) {
        List<Row> out = new ArrayList<>();
        for (Row r : table) { comparisons++; if (p.test(r)) out.add(r); }
        return out;
    }

    /**
     * A composite index on (country, age) = a sorted map keyed by "country|age(padded)".
     * Like a B-tree, it can answer queries on a LEFTMOST PREFIX of its columns:
     *    country = ?                  -> yes (prefix range)
     *    country = ? AND age BETWEEN  -> yes (tighter range)
     *    age = ?                      -> NO: ages are scattered across all countries
     */
    static class CompositeIndex {
        final TreeMap<String, List<Row>> tree = new TreeMap<>();
        CompositeIndex(List<Row> table) { for (Row r : table) tree.computeIfAbsent(key(r.country(), r.age()), k -> new ArrayList<>()).add(r); }
        static String key(String c, int age) { return c + "|" + String.format("%03d", age); }

        List<Row> byCountry(String c) { return range(c + "|", c + "|~"); }
        List<Row> byCountryAndAge(String c, int lo, int hi) { return range(key(c, lo), key(c, hi) + "~"); }
        private List<Row> range(String from, String to) {
            List<Row> out = new ArrayList<>();
            for (var e : tree.subMap(from, true, to, true).entrySet()) { comparisons++; out.addAll(e.getValue()); }
            comparisons += (long) (Math.log(tree.size()) / Math.log(2));     // the descent to the first leaf
            return out;
        }
    }

    // =====================================================================
    // [sharding]
    // =====================================================================

    static int hash(String s) { int h = s.hashCode() * 0x9E3779B9; h ^= h >>> 16; h *= 0x85EBCA6B; return h ^ (h >>> 13); }

    interface Sharder { String shardFor(String key); }

    static Sharder modulo(List<String> shards) { return k -> shards.get(Math.floorMod(hash(k), shards.size())); }

    /** Each physical shard owns `vnodes` points on a ring; a key goes to the next point clockwise. */
    static class ConsistentRing implements Sharder {
        final TreeMap<Integer, String> ring = new TreeMap<>();
        ConsistentRing(List<String> shards, int vnodes) { for (String s : shards) for (int v = 0; v < vnodes; v++) ring.put(hash(s + "#vn" + v), s); }
        public String shardFor(String k) { var e = ring.ceilingEntry(hash(k)); return (e != null ? e : ring.firstEntry()).getValue(); }
    }

    static double movedFraction(Sharder before, Sharder after, int keys) {
        int moved = 0;
        for (int i = 0; i < keys; i++) if (!before.shardFor("k" + i).equals(after.shardFor("k" + i))) moved++;
        return (double) moved / keys;
    }

    /** max load / average load: 1.0 = perfect balance. */
    static double imbalance(Sharder s, List<String> shards, int keys) {
        Map<String, Integer> load = new HashMap<>();
        for (int i = 0; i < keys; i++) load.merge(s.shardFor("k" + i), 1, Integer::sum);
        return load.values().stream().mapToInt(x -> x).max().getAsInt() / ((double) keys / shards.size());
    }

    // =====================================================================
    // [replication]
    // =====================================================================

    /** Leader applies writes immediately; followers apply them `lag` ticks later (async replication). */
    static class ReplicatedStore {
        final Map<String, String> leader = new HashMap<>();
        final List<Map<String, String>> followers = new ArrayList<>();
        final Deque<long[]> pending = new ArrayDeque<>();                   // {applyAtTick, logIndex}
        final List<String[]> log = new ArrayList<>();                       // replication log: {key, value}
        long tick;
        final int lag;
        ReplicatedStore(int followers, int lag) { for (int i = 0; i < followers; i++) this.followers.add(new HashMap<>()); this.lag = lag; }

        long write(String k, String v) { leader.put(k, v); log.add(new String[]{k, v}); pending.add(new long[]{tick + lag, log.size() - 1}); return log.size(); }   // returns the LSN
        void advance(int ticks) {
            tick += ticks;
            while (!pending.isEmpty() && pending.peek()[0] <= tick) {
                String[] e = log.get((int) pending.poll()[1]);
                for (Map<String, String> f : followers) f.put(e[0], e[1]);
            }
        }
        long followerLsn() { return log.size() - pending.size(); }
        String readFollower(String k) { return followers.get(0).get(k); }
        /** Read-your-writes: if the follower hasn't reached MY last LSN, read from the leader. */
        String readYourWrites(String k, long myLastLsn) { return followerLsn() >= myLastLsn ? readFollower(k) : leader.get(k); }
    }

    // =====================================================================
    // [quorum]
    // =====================================================================

    /** N replicas with versions; a write reaches W random replicas (others later); a read asks R and takes the newest. */
    static double staleReadRate(int n, int w, int r, int trials, Random rnd) {
        int stale = 0;
        for (int t = 0; t < trials; t++) {
            long[] version = new long[n];                                    // all replicas start at version 1
            Arrays.fill(version, 1);
            List<Integer> idx = new ArrayList<>(); for (int i = 0; i < n; i++) idx.add(i);
            Collections.shuffle(idx, rnd);
            for (int i = 0; i < w; i++) version[idx.get(i)] = 2;              // write acknowledged by W replicas
            Collections.shuffle(idx, rnd);
            long newest = 0;
            for (int i = 0; i < r; i++) newest = Math.max(newest, version[idx.get(i)]);
            if (newest < 2) stale++;
        }
        return (double) stale / trials;
    }

    // =====================================================================
    // [migration]
    // =====================================================================

    /**
     * Moving data from OLD to NEW store while live.
     * NAIVE DUAL WRITE: app writes old, then new. If the second write fails (timeout, deploy,
     * crash), the stores silently diverge - and nothing tells you.
     * SAFER: write ONLY to the source of truth, stream its changes (CDC / outbox) to the new
     * store with retries, backfill history, then VERIFY (compare) before switching reads.
     */
    static Map<String, Integer> naiveDualWrite(Random rnd, double failRate, Map<String, Integer> oldDb) {
        Map<String, Integer> newDb = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            String k = "acct" + rnd.nextInt(500);
            int v = rnd.nextInt(1000);
            oldDb.put(k, v);
            if (rnd.nextDouble() >= failRate) newDb.put(k, v);               // second write sometimes lost
        }
        return newDb;
    }

    static Map<String, Integer> cdcMigration(Random rnd, double failRate, Map<String, Integer> oldDb) {
        List<Map.Entry<String, Integer>> changeLog = new ArrayList<>();       // the DB's own log (binlog/WAL)
        for (int i = 0; i < 10_000; i++) {
            String k = "acct" + rnd.nextInt(500);
            int v = rnd.nextInt(1000);
            oldDb.put(k, v);
            changeLog.add(Map.entry(k, v));                                   // committed => in the log, atomically
        }
        Map<String, Integer> newDb = new HashMap<>();
        for (var change : changeLog) {
            while (rnd.nextDouble() < failRate) { /* apply failed: CDC consumer retries from its offset */ }
            newDb.put(change.getKey(), change.getValue());                    // idempotent upsert, in log order
        }
        return newDb;
    }

    static int diff(Map<String, Integer> a, Map<String, Integer> b) {
        Set<String> keys = new HashSet<>(a.keySet()); keys.addAll(b.keySet());
        int d = 0;
        for (String k : keys) if (!Objects.equals(a.get(k), b.get(k))) d++;
        return d;
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        Random rnd = new Random(1);
        String[] countries = {"IN", "US", "DE", "JP", "BR", "UK", "FR", "SG"};
        List<Row> table = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) table.add(new Row(i, countries[rnd.nextInt(countries.length)], 18 + rnd.nextInt(60), "u" + i));

        System.out.println("1. Indexes: full scan vs composite index (country, age)");
        CompositeIndex idx = new CompositeIndex(table);
        comparisons = 0;
        int scanHits = fullScan(table, r -> r.country().equals("IN") && r.age() >= 30 && r.age() <= 32).size();
        long scanCost = comparisons;
        comparisons = 0;
        int idxHits = idx.byCountryAndAge("IN", 30, 32).size();
        long idxCost = comparisons;
        System.out.printf("   full scan touched %,d rows; index touched %,d entries%n", scanCost, idxCost);
        check("same answer", scanHits, idxHits);
        check("index is > 1000x cheaper", true, scanCost > 1000 * idxCost);
        check("leftmost prefix (country only) works", (int) table.stream().filter(r -> r.country().equals("JP")).count(), idx.byCountry("JP").size());
        System.out.println("   'WHERE age = 30' alone can't use (country, age): it would need its own index on age");

        System.out.println("2. Sharding: hash % N vs consistent hashing when going 4 -> 5 shards");
        List<String> four = List.of("s1", "s2", "s3", "s4"), five = List.of("s1", "s2", "s3", "s4", "s5");
        double modMoved = movedFraction(modulo(four), modulo(five), 100_000);
        double ringMoved = movedFraction(new ConsistentRing(four, 200), new ConsistentRing(five, 200), 100_000);
        System.out.printf("   modulo moved %.0f%% of keys, consistent hashing moved %.0f%% (ideal 1/5 = 20%%)%n", modMoved * 100, ringMoved * 100);
        check("modulo moves ~80%", true, modMoved > 0.75);
        check("ring moves ~20%", true, ringMoved > 0.15 && ringMoved < 0.25);
        double im1 = imbalance(new ConsistentRing(five, 1), five, 100_000), im200 = imbalance(new ConsistentRing(five, 200), five, 100_000);
        System.out.printf("   max/avg shard load: 1 vnode %.2f, 200 vnodes %.2f%n", im1, im200);
        check("virtual nodes even out the load", true, im200 < 1.15 && im200 < im1);

        System.out.println("3. Range sharding by time = hot spot");
        int[] rangeLoad = new int[4], hashLoad = new int[4];
        long base = 1_700_000_000L;
        for (int i = 0; i < 10_000; i++) {
            long ts = base + 3 * 86_400 + i;                                  // all of TODAY's writes
            rangeLoad[(int) Math.min(3, (ts - base) / 86_400)]++;             // shard = day bucket
            hashLoad[Math.floorMod(hash(Long.toString(ts)), 4)]++;
        }
        check("range by day: all writes hit the newest shard", "[0, 0, 0, 10000]", rangeLoad);
        check("hash: spread out (each shard 20-30%)", true, Arrays.stream(hashLoad).allMatch(x -> x > 2000 && x < 3000));

        System.out.println("4. Async replication: lag breaks read-your-writes");
        ReplicatedStore db = new ReplicatedStore(2, 5);
        long lsn = db.write("profile:asha", "new photo");
        check("immediately after writing, a follower read is STALE", null, db.readFollower("profile:asha"));
        check("read-your-writes via LSN -> served by the leader", "new photo", db.readYourWrites("profile:asha", lsn));
        db.advance(5);
        check("after the lag the follower catches up", "new photo", db.readFollower("profile:asha"));
        db.write("order:9", "PAID");
        System.out.println("   leader crashes now -> followers never got order:9 -> promoted follower lost it (RPO > 0)");
        check("unreplicated write lost on failover", null, db.followers.get(0).get("order:9"));

        System.out.println("5. Quorums: N=3");
        for (int[] q : new int[][]{{1, 1}, {2, 1}, {1, 3}, {2, 2}, {3, 1}}) {
            double s = staleReadRate(3, q[0], q[1], 20_000, rnd);
            System.out.printf("   W=%d R=%d  (R+W%s N)  stale reads %.1f%%%n", q[0], q[1], q[0] + q[1] > 3 ? ">" : "<=", s * 100);
            if (q[0] + q[1] > 3) check("W=" + q[0] + " R=" + q[1] + ": never stale", 0.0, s);
        }
        check("W=1 R=1 is often stale", true, staleReadRate(3, 1, 1, 20_000, rnd) > 0.5);

        System.out.println("6. Live migration: naive dual write vs CDC");
        Map<String, Integer> old1 = new HashMap<>(), old2 = new HashMap<>();
        Map<String, Integer> dual = naiveDualWrite(new Random(5), 0.01, old1);
        Map<String, Integer> cdc = cdcMigration(new Random(5), 0.01, old2);
        int dualDiff = diff(old1, dual);
        System.out.println("   dual write with 1% second-write failures: " + dualDiff + " of 500 accounts differ (silently!)");
        check("dual write diverges", true, dualDiff > 0);
        check("CDC (log + retries + idempotent upserts) converges exactly", 0, diff(old2, cdc));
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
