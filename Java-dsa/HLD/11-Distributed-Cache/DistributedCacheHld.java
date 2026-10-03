/*
 * ============================================================================
 *  HLD 11 : DISTRIBUTED CACHE (Memcached / Redis Cluster)  - simulated
 * ============================================================================
 *  Run:  java DistributedCacheHld.java          (the full design answer: README.md)
 *
 *     [ring]       consistent hashing with virtual nodes; REPLICATION to the next R-1 distinct
 *                  nodes on the ring; a node failure -> replicas keep keys readable
 *     [scaling]    adding a node moves ~1/N of the keys (and only TO the new node)
 *     [slots]      Redis Cluster style: 16,384 hash slots -> nodes; migrating a slot;
 *                  clients follow MOVED redirects and refresh their slot map
 *     [hotkeys]    a Count-Min Sketch finds the heaviest keys in a stream with tiny memory,
 *                  so they can be replicated to every node / near caches
 *  (Single-node internals - eviction policies, TTL, single-flight: LLD/06 and LLD/17.)
 * ============================================================================
 */

import java.util.*;

public class DistributedCacheHld {

    static int hash(String s) { int h = s.hashCode() * 0x9E3779B9; h ^= h >>> 16; h *= 0x85EBCA6B; return h ^ (h >>> 13); }

    // =====================================================================
    // [ring]
    // =====================================================================

    static final class Ring {
        final TreeMap<Integer, String> ring = new TreeMap<>();
        final int vnodes;
        final Set<String> nodes = new TreeSet<>();
        Ring(int vnodes) { this.vnodes = vnodes; }
        void add(String node) { nodes.add(node); for (int v = 0; v < vnodes; v++) ring.put(hash(node + "#" + v), node); }
        void remove(String node) { nodes.remove(node); ring.values().removeIf(n -> n.equals(node)); }

        /** The first R DISTINCT nodes clockwise from the key: primary + replicas. */
        List<String> owners(String key, int r) {
            List<String> out = new ArrayList<>();
            for (var e : ring.tailMap(hash(key)).entrySet()) { if (!out.contains(e.getValue())) out.add(e.getValue()); if (out.size() == r) return out; }
            for (var e : ring.entrySet()) { if (!out.contains(e.getValue())) out.add(e.getValue()); if (out.size() == r) return out; }
            return out;
        }
    }

    static final class CacheCluster {
        final Ring ring;
        final int replicas;
        final Map<String, Map<String, String>> data = new HashMap<>();     // node -> its key/values
        final Set<String> down = new HashSet<>();
        CacheCluster(int replicas, int vnodes, String... nodes) { this.replicas = replicas; ring = new Ring(vnodes); for (String n : nodes) { ring.add(n); data.put(n, new HashMap<>()); } }

        void put(String k, String v) { for (String n : ring.owners(k, replicas)) data.get(n).put(k, v); }

        /** Try owners in order; skip dead nodes (client-side failover). */
        Optional<String> get(String k) {
            for (String n : ring.owners(k, replicas)) {
                if (down.contains(n)) continue;
                String v = data.get(n).get(k);
                if (v != null) return Optional.of(v);
            }
            return Optional.empty();
        }
    }

    // =====================================================================
    // [slots]
    // =====================================================================

    static final int SLOTS = 16_384;
    static int slotOf(String key) { return Math.floorMod(hash(key), SLOTS); }   // Redis uses CRC16(key) mod 16384

    static final class SlotServer {
        final String id;
        final Set<Integer> ownedSlots = new HashSet<>();
        final Map<String, String> kv = new HashMap<>();
        SlotServer(String id) { this.id = id; }
    }

    record Reply(String value, String movedTo) {}

    static final class SlotCluster {
        final Map<String, SlotServer> servers = new LinkedHashMap<>();
        SlotCluster(String... ids) {
            for (String id : ids) servers.put(id, new SlotServer(id));
            List<SlotServer> list = new ArrayList<>(servers.values());
            for (int s = 0; s < SLOTS; s++) list.get(s * list.size() / SLOTS).ownedSlots.add(s);   // contiguous ranges
        }
        String ownerOf(int slot) { return servers.values().stream().filter(x -> x.ownedSlots.contains(slot)).findFirst().get().id; }

        /** A server answers for its slots, or redirects: "MOVED <slot> <node>". */
        Reply ask(String serverId, String key, String setValue) {
            SlotServer s = servers.get(serverId);
            int slot = slotOf(key);
            if (!s.ownedSlots.contains(slot)) return new Reply(null, ownerOf(slot));
            if (setValue != null) s.kv.put(key, setValue);
            return new Reply(s.kv.get(key), null);
        }

        /** Move one slot (and its keys) from one node to another (resharding). */
        void migrate(int slot, String from, String to) {
            SlotServer a = servers.get(from), b = servers.get(to);
            a.kv.entrySet().removeIf(e -> { if (slotOf(e.getKey()) == slot) { b.kv.put(e.getKey(), e.getValue()); return true; } return false; });
            a.ownedSlots.remove(slot); b.ownedSlots.add(slot);
        }
    }

    /** A "smart client": caches the slot map, follows MOVED, then updates its map. */
    static final class SmartClient {
        final Map<Integer, String> slotMap = new HashMap<>();
        int redirects;
        SmartClient(SlotCluster c) { for (int s = 0; s < SLOTS; s++) slotMap.put(s, c.ownerOf(s)); }
        String get(SlotCluster c, String key) {
            String node = slotMap.get(slotOf(key));
            Reply r = c.ask(node, key, null);
            if (r.movedTo() != null) { redirects++; slotMap.put(slotOf(key), r.movedTo()); r = c.ask(r.movedTo(), key, null); }
            return r.value();
        }
    }

    // =====================================================================
    // [hotkeys]
    // =====================================================================

    /** Count-Min Sketch: d rows x w counters. Estimates never UNDER-count; memory is fixed. */
    static final class CountMinSketch {
        final long[][] t; final int d, w;
        CountMinSketch(int d, int w) { this.d = d; this.w = w; t = new long[d][w]; }
        private int idx(String k, int row) { return Math.floorMod(hash(k + "@" + row), w); }
        void add(String k) { for (int r = 0; r < d; r++) t[r][idx(k, r)]++; }
        long estimate(String k) { long m = Long.MAX_VALUE; for (int r = 0; r < d; r++) m = Math.min(m, t[r][idx(k, r)]); return m; }
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Replication on the ring: a node dies");
        CacheCluster noRep = new CacheCluster(1, 150, "n1", "n2", "n3", "n4", "n5");
        CacheCluster rep2 = new CacheCluster(2, 150, "n1", "n2", "n3", "n4", "n5");
        for (int i = 0; i < 50_000; i++) { noRep.put("k" + i, "v" + i); rep2.put("k" + i, "v" + i); }
        noRep.down.add("n3"); rep2.down.add("n3");
        long hitsNoRep = 0, hitsRep = 0;
        for (int i = 0; i < 50_000; i++) { if (noRep.get("k" + i).isPresent()) hitsNoRep++; if (rep2.get("k" + i).isPresent()) hitsRep++; }
        System.out.printf("   readable after n3 died: R=1 %.1f%%, R=2 %.1f%%%n", hitsNoRep / 500.0, hitsRep / 500.0);
        check("R=1 loses ~1/5 of the keys (a DB stampede for those)", true, hitsNoRep < 45_000);
        check("R=2 keeps every key readable", 50_000L, hitsRep);
        check("replicas are on DISTINCT nodes", 2L, rep2.ring.owners("k42", 2).stream().distinct().count());

        System.out.println("2. Scaling out 5 -> 6 nodes");
        Ring r5 = new Ring(150), r6 = new Ring(150);
        for (String n : List.of("n1", "n2", "n3", "n4", "n5")) { r5.add(n); r6.add(n); }
        r6.add("n6");
        int moved = 0, movedNotToNew = 0;
        for (int i = 0; i < 100_000; i++) {
            String a = r5.owners("k" + i, 1).get(0), b = r6.owners("k" + i, 1).get(0);
            if (!a.equals(b)) { moved++; if (!b.equals("n6")) movedNotToNew++; }
        }
        System.out.printf("   %.1f%% of keys moved (ideal 1/6 = 16.7%%)%n", moved / 1000.0);
        check("~1/N of keys move", true, moved > 14_000 && moved < 20_000);
        check("every moved key moved TO the new node", 0, movedNotToNew);

        System.out.println("3. Redis-Cluster hash slots: migration + MOVED redirects");
        SlotCluster cluster = new SlotCluster("A", "B", "C");
        SmartClient client = new SmartClient(cluster);
        for (int i = 0; i < 3_000; i++) { String k = "user:" + i; cluster.ask(client.slotMap.get(slotOf(k)), k, "profile" + i); }
        int slot = slotOf("user:7");
        String from = cluster.ownerOf(slot), to = from.equals("C") ? "A" : "C";
        cluster.migrate(slot, from, to);
        check("old owner now answers MOVED", to, cluster.ask(from, "user:7", null).movedTo());
        check("client follows the redirect and gets the value", "profile7", client.get(cluster, "user:7"));
        check("...and updates its map: no redirect next time", "1 then 1", client.redirects + " then " + (client.get(cluster, "user:7") != null ? client.redirects : -1));
        int lost = 0;
        for (int i = 0; i < 3_000; i++) if (!("profile" + i).equals(client.get(cluster, "user:" + i))) lost++;
        check("no key lost during migration", 0, lost);

        System.out.println("4. Hot-key detection with a Count-Min Sketch (1M requests, 4 x 2,048 counters)");
        CountMinSketch cms = new CountMinSketch(4, 2_048);
        Map<String, Integer> exact = new HashMap<>();
        Random rnd = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            String k = rnd.nextDouble() < 0.2 ? "hot" + rnd.nextInt(5) : "key" + rnd.nextInt(200_000);   // 5 keys get 20% of traffic
            cms.add(k);
            exact.merge(k, 1, Integer::sum);
        }
        boolean neverUnder = true;
        double worstOver = 0;
        for (int i = 0; i < 5; i++) {
            long est = cms.estimate("hot" + i), real = exact.get("hot" + i);
            neverUnder &= est >= real;
            worstOver = Math.max(worstOver, (est - real) / (double) real);
        }
        long coldEst = cms.estimate("key123");
        System.out.printf("   hot keys estimated within %.1f%% (8,192 counters vs %,d distinct keys); a cold key estimates %d%n", worstOver * 100, exact.size(), coldEst);
        check("never under-counts", true, neverUnder);
        check("hot keys clearly separated from the cold tail", true, cms.estimate("hot0") > 20 * coldEst);
        System.out.println("   -> replicate detected hot keys to all nodes / near caches (see 03-Caching)");
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
