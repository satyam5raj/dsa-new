/*
 * ============================================================================
 *  HLD 03 : CACHING PATTERNS, INVALIDATION AND FAILURE MODES  (simulated)
 * ============================================================================
 *  Run:  java CachingPatterns.java          (concepts: README.md)
 *
 *     [sizing]       hit ratio vs cache size under a ZIPF (power-law) workload
 *     [aside]        cache-aside read path; the classic STALE-CACHE RACE, step by step,
 *                    and two fixes (versioned set, delete-after-delay)
 *     [write]        write-through vs write-behind: DB writes saved vs data lost on crash
 *     [stampede]     expiry stampede: N concurrent misses -> N DB calls; single-flight -> 1;
 *                    TTL JITTER spreads synchronized expirations
 *     [penetration]  requests for keys that DON'T exist bypass the cache every time ->
 *                    negative caching + a Bloom filter
 *     [hotkey]       one celebrity key overloads one cache shard -> local near-cache
 *
 *  The "database" and "cache" are maps with call counters, so every effect is a number.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class CachingPatterns {

    // =====================================================================
    // [sizing] Zipf workload + LRU cache
    // =====================================================================

    /** Zipf(s): item i is requested with probability ~ 1 / i^s. Real traffic looks like this. */
    static final class Zipf {
        private final double[] cdf;
        private final Random rnd;
        Zipf(int n, double s, Random rnd) {
            this.rnd = rnd;
            cdf = new double[n];
            double sum = 0;
            for (int i = 1; i <= n; i++) { sum += 1 / Math.pow(i, s); cdf[i - 1] = sum; }
            for (int i = 0; i < n; i++) cdf[i] /= sum;
        }
        int next() { int i = Arrays.binarySearch(cdf, rnd.nextDouble()); return i >= 0 ? i : -i - 1; }
    }

    static double hitRatio(int items, int cacheSize, int requests, long seed) {
        LinkedHashMap<Integer, Boolean> lru = new LinkedHashMap<>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> e) { return size() > cacheSize; }
        };
        Zipf z = new Zipf(items, 1.0, new Random(seed));
        int hits = 0;
        for (int i = 0; i < requests; i++) { int k = z.next(); if (lru.get(k) != null) hits++; else lru.put(k, true); }
        return (double) hits / requests;
    }

    // =====================================================================
    // [aside] the stale-cache race
    // =====================================================================

    record Versioned(String value, long version) {}

    /**
     * Cache-aside with "update DB, then DELETE cache" (the recommended order). Still racy:
     *   t1 Reader: cache miss
     *   t2 Reader: reads DB -> OLD value
     *   t3 Writer: updates DB -> NEW value
     *   t4 Writer: deletes cache key (nothing there)
     *   t5 Reader: puts OLD value into cache   <- stale until TTL!
     * We replay that exact interleaving.
     */
    static String staleRace(boolean versionedSet) {
        Map<String, Versioned> db = new HashMap<>(Map.of("price", new Versioned("100", 1)));
        Map<String, Versioned> cache = new HashMap<>();
        Versioned readerSaw;
        // t1, t2
        readerSaw = cache.get("price") == null ? db.get("price") : null;
        // t3, t4
        db.put("price", new Versioned("120", 2));
        cache.remove("price");
        // t5: the reader populates the cache with what it read
        if (versionedSet) {
            // FIX: only set if newer than what the DB currently has (or use SET-IF-VERSION in Redis via Lua / a CAS).
            // Simplest robust variant: re-check the source version before caching.
            if (readerSaw.version() >= db.get("price").version()) cache.put("price", readerSaw);
        } else cache.put("price", readerSaw);
        Versioned c = cache.get("price");
        return c == null ? "miss(next read loads 120)" : c.value();
    }

    // =====================================================================
    // [write] write-through vs write-behind
    // =====================================================================

    record WriteResult(int dbWrites, int lostOnCrash) {}

    static WriteResult writeThrough(List<String[]> writes) {
        Map<String, String> db = new HashMap<>(), cache = new HashMap<>();
        int dbWrites = 0;
        for (String[] w : writes) { db.put(w[0], w[1]); dbWrites++; cache.put(w[0], w[1]); }   // both, synchronously
        return new WriteResult(dbWrites, 0);
    }

    /** Writes go to the cache; a flusher writes dirty keys every `flushEvery` writes (coalescing repeated keys). */
    static WriteResult writeBehind(List<String[]> writes, int flushEvery, int crashAfter) {
        Map<String, String> db = new HashMap<>(), cache = new HashMap<>();
        Map<String, String> dirty = new LinkedHashMap<>();
        int dbWrites = 0;
        for (int i = 0; i < writes.size(); i++) {
            if (i == crashAfter) return new WriteResult(dbWrites, dirty.size());   // cache node dies: dirty data gone
            cache.put(writes.get(i)[0], writes.get(i)[1]);
            dirty.put(writes.get(i)[0], writes.get(i)[1]);
            if ((i + 1) % flushEvery == 0) { dbWrites += dirty.size(); db.putAll(dirty); dirty.clear(); }
        }
        dbWrites += dirty.size();
        return new WriteResult(dbWrites, 0);
    }

    // =====================================================================
    // [stampede]
    // =====================================================================

    static int stampede(boolean singleFlight, int clients) throws Exception {
        AtomicInteger dbCalls = new AtomicInteger();
        ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
        Supplier<String> loadFromDb = () -> { dbCalls.incrementAndGet(); sleep(30); return "homepage-html"; };
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 0; i < clients; i++) fs.add(pool.submit(() -> {
            go.await();
            String v = cache.get("home");
            if (v != null) return v;
            if (!singleFlight) { v = loadFromDb.get(); cache.put("home", v); return v; }
            CompletableFuture<String> mine = new CompletableFuture<>(), prev = inFlight.putIfAbsent("home", mine);
            if (prev != null) return prev.join();                          // wait for the leader's load
            v = loadFromDb.get(); cache.put("home", v); mine.complete(v); inFlight.remove("home");
            return v;
        }));
        go.countDown();
        for (Future<String> f : fs) f.get();
        pool.shutdown();
        return dbCalls.get();
    }

    /** How many of 10,000 keys cached at the same moment expire in the busiest single second. */
    static int worstSecondOfExpiries(boolean jitter, Random rnd) {
        Map<Long, Integer> perSecond = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            long ttl = 3600 + (jitter ? rnd.nextInt(600) : 0);               // 1h, +0..10 min of jitter
            perSecond.merge(ttl, 1, Integer::sum);
        }
        return Collections.max(perSecond.values());
    }

    // =====================================================================
    // [penetration]
    // =====================================================================

    /** A tiny Bloom filter: "definitely not present" or "maybe present". (Full version: DSA/12-Advanced.) */
    static final class Bloom {
        final BitSet bits; final int m, k;
        Bloom(int m, int k) { this.m = m; this.k = k; bits = new BitSet(m); }
        int h(String s, int i) { int a = s.hashCode(), b = Integer.rotateLeft(a * 0x9E3779B9, 16) | 1; return Math.floorMod(a + i * b, m); }
        void add(String s) { for (int i = 0; i < k; i++) bits.set(h(s, i)); }
        boolean mightContain(String s) { for (int i = 0; i < k; i++) if (!bits.get(h(s, i))) return false; return true; }
    }

    static int dbCallsForMissingKeys(boolean negativeCache, Bloom bloom, int requests, Random rnd) {
        Map<String, Optional<String>> cache = new HashMap<>();
        int dbCalls = 0;
        for (int i = 0; i < requests; i++) {
            String key = "user:" + (1_000_000 + rnd.nextInt(200));          // attacker / bug: ids that don't exist
            if (bloom != null && !bloom.mightContain(key)) continue;          // rejected without touching cache or DB
            if (cache.containsKey(key)) continue;
            dbCalls++;                                                        // DB says: not found
            if (negativeCache) cache.put(key, Optional.empty());              // remember "not found" (short TTL in real life)
        }
        return dbCalls;
    }

    // =====================================================================
    // [hotkey]
    // =====================================================================

    /** Requests per cache shard when one key gets 30% of all traffic; with/without a per-app-server near cache. */
    static int[] shardLoad(boolean nearCache, int appServers, int shards, int requests, Random rnd) {
        int[] load = new int[shards];
        List<Set<String>> local = new ArrayList<>();
        for (int i = 0; i < appServers; i++) local.add(new HashSet<>());
        for (int i = 0; i < requests; i++) {
            String key = rnd.nextDouble() < 0.3 ? "celebrity" : "k" + rnd.nextInt(100_000);
            int server = rnd.nextInt(appServers);
            if (nearCache && key.equals("celebrity") && local.get(server).contains(key)) continue;   // served from local memory
            load[Math.floorMod(key.hashCode(), shards)]++;
            if (nearCache && key.equals("celebrity")) local.get(server).add(key);   // short TTL in production
        }
        return load;
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(1);

        System.out.println("1. Cache sizing under a Zipf workload (100,000 items)");
        double h1 = hitRatio(100_000, 1_000, 300_000, 1), h10 = hitRatio(100_000, 10_000, 300_000, 1), h20 = hitRatio(100_000, 20_000, 300_000, 1);
        System.out.printf("   cache 1%% of items -> %.0f%% hits, 10%% -> %.0f%%, 20%% -> %.0f%%%n", h1 * 100, h10 * 100, h20 * 100);
        check("1% of the data already serves > 40% of reads", true, h1 > 0.40);
        check("diminishing returns: 10% -> 20% adds < 10 points", true, h20 - h10 < 0.10);

        System.out.println("2. Cache-aside: the update/delete race");
        check("plain cache-aside caches the OLD price", "100", staleRace(false));
        check("version check refuses the stale set", "miss(next read loads 120)", staleRace(true));

        System.out.println("3. Write-through vs write-behind (1,000 writes to 50 hot keys)");
        List<String[]> writes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) writes.add(new String[]{"cart:" + rnd.nextInt(50), "v" + i});
        WriteResult wt = writeThrough(writes), wb = writeBehind(writes, 100, Integer.MAX_VALUE), crash = writeBehind(writes, 100, 950);
        System.out.printf("   write-through %d DB writes; write-behind %d DB writes (coalesced)%n", wt.dbWrites(), wb.dbWrites());
        check("write-behind saves DB writes", true, wb.dbWrites() < wt.dbWrites() / 2);
        check("...but loses dirty data if the cache dies", true, crash.lostOnCrash() > 0);
        System.out.println("   crash before a flush lost " + crash.lostOnCrash() + " keys' latest values");

        System.out.println("4. Stampede on a hot key's expiry (64 concurrent requests)");
        int naive = stampede(false, 64), sf = stampede(true, 64);
        System.out.printf("   without protection %d DB calls, with single-flight %d%n", naive, sf);
        check("single-flight -> 1 DB call", 1, sf);
        check("naive -> many", true, naive > 10);
        int noJitter = worstSecondOfExpiries(false, rnd), withJitter = worstSecondOfExpiries(true, rnd);
        System.out.printf("   10,000 keys cached together: worst second has %d expiries without jitter, %d with%n", noJitter, withJitter);
        check("jitter spreads expirations", true, withJitter < noJitter / 100);

        System.out.println("5. Cache penetration (requests for ids that don't exist)");
        Bloom bloom = new Bloom(1 << 20, 5);
        for (int i = 0; i < 100_000; i++) bloom.add("user:" + i);           // all REAL user ids
        int none = dbCallsForMissingKeys(false, null, 10_000, new Random(2));
        int neg = dbCallsForMissingKeys(true, null, 10_000, new Random(2));
        int bl = dbCallsForMissingKeys(false, bloom, 10_000, new Random(2));
        int both = dbCallsForMissingKeys(true, bloom, 10_000, new Random(2));
        System.out.printf("   DB calls: no protection %d, negative cache %d, bloom filter %d, bloom + negative cache %d%n", none, neg, bl, both);
        check("negative caching: one DB call per missing id", 200, neg);
        check("bloom alone: only false positives get through", true, bl < none / 100);
        check("bloom + negative cache: a false positive costs ONE call", true, both <= 3);

        System.out.println("6. Hot key: one cache shard melts; a near cache fixes it");
        int[] noNear = shardLoad(false, 50, 8, 200_000, new Random(3)), near = shardLoad(true, 50, 8, 200_000, new Random(3));
        System.out.println("   shard load without near cache " + Arrays.toString(noNear));
        System.out.println("   shard load with near cache    " + Arrays.toString(near));
        double skewBefore = Arrays.stream(noNear).max().getAsInt() / Arrays.stream(noNear).average().getAsDouble();
        double skewAfter = Arrays.stream(near).max().getAsInt() / Arrays.stream(near).average().getAsDouble();
        check("hottest shard drops to ~average", true, skewBefore > 2.5 && skewAfter < 1.2);
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
