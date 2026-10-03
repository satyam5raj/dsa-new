/*
 * ============================================================================
 *  HLD 07 : URL SHORTENER AT SCALE  (simulation of the parts that matter)
 * ============================================================================
 *  Run:  java UrlShortenerHld.java          (the full design answer: README.md)
 *
 *     [estimate]  QPS / storage / cache from the requirements
 *     [keygen]    Key Generation Service with RANGE leases: N app servers, crashes included ->
 *                 zero duplicates, bounded gaps, coordination once per 10,000 ids
 *     [storage]   codes spread over shards by hash: even load, single-shard lookups
 *     [tiers]     redirect traffic through CDN -> Redis -> DB under a Zipf workload:
 *                 how much each tier absorbs (why the DB barely notices 100K QPS)
 *     [redirect]  301 vs 302: what the analytics pipeline can still see
 *     [analytics] click events aggregated per (code, minute) from a stream
 *  (Class-level design - base62, collisions, aliases, expiry: LLD/05-URL-Shortener.)
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.atomic.*;

public class UrlShortenerHld {

    // =====================================================================
    // [keygen]
    // =====================================================================

    /** The coordinator (a DB row / ZooKeeper node): hands out disjoint blocks. */
    static final class KeyRangeService {
        private final AtomicLong next = new AtomicLong(1);
        final AtomicInteger leases = new AtomicInteger();
        long[] lease(int size) { leases.incrementAndGet(); long s = next.getAndAdd(size); return new long[]{s, s + size}; }
    }

    static final class AppServer {
        private final KeyRangeService krs; private final int block;
        private long cur, end;
        AppServer(KeyRangeService k, int block) { krs = k; this.block = block; }
        long nextId() { if (cur >= end) { long[] r = krs.lease(block); cur = r[0]; end = r[1]; } return cur++; }
        void crash() { cur = end = 0; }                                    // unused part of its block is simply lost (a gap)
    }

    static String base62(long n) {
        String a = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        do { sb.append(a.charAt((int) (n % 62))); n /= 62; } while (n > 0);
        return sb.reverse().toString();
    }

    // =====================================================================
    // [tiers]
    // =====================================================================

    static final class Zipf {
        private final double[] cdf; private final Random rnd;
        Zipf(int n, double s, Random rnd) {
            this.rnd = rnd; cdf = new double[n]; double sum = 0;
            for (int i = 1; i <= n; i++) { sum += 1 / Math.pow(i, s); cdf[i - 1] = sum; }
            for (int i = 0; i < n; i++) cdf[i] /= sum;
        }
        int next() { int i = Arrays.binarySearch(cdf, rnd.nextDouble()); return i >= 0 ? i : -i - 1; }
    }

    static <K> LinkedHashMap<K, Boolean> lru(int cap) {
        return new LinkedHashMap<>(16, 0.75f, true) { protected boolean removeEldestEntry(Map.Entry<K, Boolean> e) { return size() > cap; } };
    }

    /** Returns how many requests each tier served: {cdn, redis, db}. CDN only caches 302s briefly; here: per-edge LRU. */
    static long[] tiers(int urls, int requests, int edges, int cdnPerEdge, int redisSize, long seed) {
        Random rnd = new Random(seed);
        Zipf z = new Zipf(urls, 1.0, rnd);
        List<LinkedHashMap<Integer, Boolean>> cdn = new ArrayList<>();
        for (int e = 0; e < edges; e++) cdn.add(lru(cdnPerEdge));
        LinkedHashMap<Integer, Boolean> redis = lru(redisSize);
        long[] served = new long[3];
        for (int i = 0; i < requests; i++) {
            int code = z.next();
            LinkedHashMap<Integer, Boolean> edge = cdn.get(rnd.nextInt(edges));
            if (edge.get(code) != null) { served[0]++; continue; }
            edge.put(code, true);
            if (redis.get(code) != null) { served[1]++; continue; }
            redis.put(code, true);
            served[2]++;
        }
        return served;
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Estimation (assumptions in README section 2)");
        double writeQps = 100e6 / 86_400, readQps = writeQps * 100;
        System.out.printf("   writes ~%.0f/s, reads ~%.0f/s avg, ~%.0f/s peak%n", writeQps, readQps, readQps * 3);
        check("10 years of ids fit in 7 base62 chars", true, Math.pow(62, 7) > 100e6 * 365 * 10);

        System.out.println("2. Key generation: 20 app servers, random crashes, 1,000,000 ids");
        KeyRangeService krs = new KeyRangeService();
        List<AppServer> servers = new ArrayList<>();
        for (int i = 0; i < 20; i++) servers.add(new AppServer(krs, 10_000));
        Random rnd = new Random(1);
        Set<Long> ids = new HashSet<>();
        long maxId = 0;
        for (int i = 0; i < 1_000_000; i++) {
            AppServer s = servers.get(rnd.nextInt(20));
            if (rnd.nextDouble() < 0.0001) s.crash();                       // ~100 crashes
            long id = s.nextId();
            ids.add(id);
            maxId = Math.max(maxId, id);
        }
        check("no duplicate ids, ever", 1_000_000, ids.size());
        System.out.printf("   %d range leases for 1M ids; %.1f%% of the id space skipped by crashes (harmless)%n", krs.leases.get(), 100.0 * (maxId - ids.size()) / maxId);
        check("coordination is rare (< 300 leases)", true, krs.leases.get() < 300);
        check("codes are short", "4c92", base62(1_000_000));

        System.out.println("3. Storage sharding by hash(code): 16 shards");
        int[] perShard = new int[16];
        for (long id : ids) perShard[Math.floorMod(base62(id).hashCode() * 0x9E3779B9, 16)]++;
        int max = Arrays.stream(perShard).max().getAsInt(), min = Arrays.stream(perShard).min().getAsInt();
        check("even spread (max/min < 1.1)", true, (double) max / min < 1.1);
        System.out.println("   a redirect = ONE key lookup on ONE shard; no cross-shard queries needed");

        System.out.println("4. Read tiers under Zipf traffic: 1M urls, 2M redirects, 20 edges");
        long[] served = tiers(1_000_000, 2_000_000, 20, 5_000, 100_000, 2);
        long total = Arrays.stream(served).sum();
        System.out.printf("   CDN %.1f%%, Redis %.1f%%, DB %.1f%%%n", 100.0 * served[0] / total, 100.0 * served[1] / total, 100.0 * served[2] / total);
        check("caches absorb the majority of reads", true, served[2] < total * 0.5);
        System.out.printf("   with this SMALL cold cache the DB would still see ~%.0fK of 347K peak QPS -> size Redis for the hot set%n", 347.0 * served[2] / total);
        long[] bigger = tiers(1_000_000, 2_000_000, 20, 5_000, 300_000, 2);
        check("3x bigger Redis cuts DB traffic", true, bigger[2] < served[2]);
        System.out.printf("   Redis 300K entries -> DB %.1f%%%n", 100.0 * bigger[2] / Arrays.stream(bigger).sum());

        System.out.println("5. 301 vs 302 and analytics");
        int clicksBySameUsers = 10;
        int seen301 = 1, seen302 = clicksBySameUsers;                       // a 301 is cached by the browser after the first hit
        check("301: the service sees only the first click", 1, seen301);
        check("302: every click reaches us -> analytics + expiry work", 10, seen302);

        System.out.println("6. Click analytics: aggregate a stream per (code, minute)");
        Map<String, Integer> perMinute = new TreeMap<>();
        long t0 = 1_700_000_040_000L;                                     // aligned to a minute boundary
        Random r2 = new Random(3);
        for (int i = 0; i < 10_000; i++) {
            String code = "c" + r2.nextInt(3);
            long ts = t0 + r2.nextInt(180_000);                              // 3 minutes of events, arriving in any order
            perMinute.merge(code + "@" + (ts / 60_000), 1, Integer::sum);
        }
        check("9 (code, minute) buckets", 9, perMinute.size());
        check("no click lost in aggregation", 10_000, perMinute.values().stream().mapToInt(Integer::intValue).sum());
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
