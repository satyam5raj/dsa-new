/*
 * ============================================================================
 *  LLD 06 : CACHE WITH PLUGGABLE EVICTION (LRU / LFU / FIFO), TTL, LOADER
 * ============================================================================
 *  Run:  java CacheSystem.java          (design notes: README.md)
 *
 *     [api]       Cache<K,V>, CacheStats, RemovalListener + RemovalCause
 *     [eviction]  EvictionPolicy<K> (Strategy): LruPolicy, LfuPolicy, FifoPolicy - each O(1)
 *     [core]      BoundedCache<K,V>: storage + policy + TTL + stats, guarded by one lock
 *     [scale]     SegmentedCache: N independent segments (lock striping) for concurrency
 *     [loading]   getOrLoad(): read-through with SINGLE-FLIGHT (no cache stampede)
 *     [builder]   CacheBuilder: capacity, policy, ttl, listener, segments
 *     [demo]      policy behaviour, TTL, stats, listeners, stampede test, concurrency test
 *
 *  The pure data-structure versions (LeetCode 146 / 460) are in
 *  DSA/12-Advanced (M01_LRUCache, H01_LFUCache). This file is the DESIGN around them:
 *  the cache does not know HOW victims are chosen - it asks its EvictionPolicy.
 *
 *  WHY ONE LOCK PER SEGMENT (and not a ReadWriteLock)?
 *    In LRU and LFU, a GET MUTATES the policy (moves the key to the front / bumps its
 *    frequency). So every get is a write -> a read lock would be wrong. Instead we split the
 *    cache into segments by key hash: threads on different segments run in parallel.
 *    (Caffeine goes further: it records reads in a lock-free buffer and replays them later.)
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class CacheSystem {

    // =====================================================================
    // [api]
    // =====================================================================

    interface Cache<K, V> {
        Optional<V> get(K key);
        void put(K key, V value);
        void remove(K key);
        V getOrLoad(K key, Function<K, V> loader);
        int size();
        CacheStats stats();
    }

    record CacheStats(long hits, long misses, long evictions, long loads) {
        double hitRate() { return hits + misses == 0 ? 0 : (double) hits / (hits + misses); }
    }

    enum RemovalCause { EVICTED, EXPIRED, EXPLICIT, REPLACED }

    @FunctionalInterface
    interface RemovalListener<K, V> { void onRemoval(K key, V value, RemovalCause cause); }

    // =====================================================================
    // [eviction] strategies - each O(1) per operation
    // =====================================================================

    /**
     * The policy only tracks KEYS and picks a victim. It never sees values.
     * All methods are called while the owning cache segment holds its lock, so policies
     * themselves need no synchronisation.
     */
    interface EvictionPolicy<K> {
        void onInsert(K key);
        void onAccess(K key);
        void onRemove(K key);
        K victim();                                                    // which key to evict next
    }

    /** LRU: a LinkedHashMap in ACCESS order is a doubly-linked list + hash map in one object. */
    static class LruPolicy<K> implements EvictionPolicy<K> {
        private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>(16, 0.75f, true);   // true = access order
        public void onInsert(K k) { order.put(k, Boolean.TRUE); }
        public void onAccess(K k) { order.get(k); }                    // get() moves it to the MRU end
        public void onRemove(K k) { order.remove(k); }
        public K victim() { return order.keySet().iterator().next(); } // eldest = least recently used
    }

    /** FIFO: insertion order; reads don't matter. Simplest, surprisingly decent for some workloads. */
    static class FifoPolicy<K> implements EvictionPolicy<K> {
        private final LinkedHashSet<K> order = new LinkedHashSet<>();
        public void onInsert(K k) { order.add(k); }
        public void onAccess(K k) { }
        public void onRemove(K k) { order.remove(k); }
        public K victim() { return order.iterator().next(); }
    }

    /**
     * LFU in O(1): key -> frequency, and frequency -> keys in LRU order (for ties).
     * minFreq points at the bucket to evict from. (Same idea as LeetCode 460.)
     */
    static class LfuPolicy<K> implements EvictionPolicy<K> {
        private final Map<K, Integer> freq = new HashMap<>();
        private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
        private int minFreq = 0;

        public void onInsert(K k) { freq.put(k, 1); bucket(1).add(k); minFreq = 1; }
        public void onAccess(K k) {
            int f = freq.get(k);
            buckets.get(f).remove(k);
            if (f == minFreq && buckets.get(f).isEmpty()) minFreq = f + 1;
            freq.put(k, f + 1);
            bucket(f + 1).add(k);
        }
        public void onRemove(K k) {
            Integer f = freq.remove(k);
            if (f != null) buckets.get(f).remove(k);
        }
        public K victim() {
            while (buckets.getOrDefault(minFreq, new LinkedHashSet<>()).isEmpty()) minFreq++;   // after explicit removes
            return buckets.get(minFreq).iterator().next();             // least frequent, oldest among ties
        }
        private LinkedHashSet<K> bucket(int f) { return buckets.computeIfAbsent(f, x -> new LinkedHashSet<>()); }
    }

    // =====================================================================
    // [core] one bounded cache segment
    // =====================================================================

    static class BoundedCache<K, V> implements Cache<K, V> {
        private record Entry<V>(V value, long expiresAt) {}

        private final int capacity;
        private final long ttlMs;                                      // 0 = no expiry
        private final EvictionPolicy<K> policy;
        private final RemovalListener<K, V> listener;
        private final LongSupplier clock;
        private final Map<K, Entry<V>> data = new HashMap<>();
        private final ReentrantLock lock = new ReentrantLock();
        private final LongAdder hits = new LongAdder(), misses = new LongAdder(), evictions = new LongAdder(), loads = new LongAdder();
        // single-flight: concurrent misses for the same key share ONE load
        private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

        BoundedCache(int capacity, long ttlMs, EvictionPolicy<K> policy, RemovalListener<K, V> listener, LongSupplier clock) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
            this.capacity = capacity; this.ttlMs = ttlMs; this.policy = policy; this.listener = listener; this.clock = clock;
        }

        public Optional<V> get(K key) {
            List<Runnable> notifications = new ArrayList<>();
            Optional<V> result;
            lock.lock();
            try {
                Entry<V> e = data.get(key);
                if (e != null && isExpired(e)) {                       // lazy expiry on read
                    removeLocked(key, RemovalCause.EXPIRED, notifications);
                    e = null;
                }
                if (e == null) { misses.increment(); result = Optional.empty(); }
                else { hits.increment(); policy.onAccess(key); result = Optional.of(e.value()); }
            } finally { lock.unlock(); }
            notifications.forEach(Runnable::run);                      // call listeners OUTSIDE the lock
            return result;
        }

        public void put(K key, V value) {
            Objects.requireNonNull(key); Objects.requireNonNull(value);
            List<Runnable> notifications = new ArrayList<>();
            lock.lock();
            try {
                long expiresAt = ttlMs > 0 ? clock.getAsLong() + ttlMs : Long.MAX_VALUE;
                Entry<V> old = data.get(key);
                if (old != null) {                                     // update: counts as an access
                    data.put(key, new Entry<>(value, expiresAt));
                    policy.onAccess(key);
                    V ov = old.value();
                    notifications.add(() -> listener.onRemoval(key, ov, RemovalCause.REPLACED));
                } else {
                    // EVICT FIRST, THEN INSERT. If we inserted first, LFU would always pick the
                    // brand-new key (frequency 1) as the victim and the cache could never admit
                    // anything new. (A real bug this file had - see README "Common mistakes".)
                    while (data.size() >= capacity) {
                        K victim = policy.victim();
                        evictions.increment();
                        removeLocked(victim, RemovalCause.EVICTED, notifications);
                    }
                    data.put(key, new Entry<>(value, expiresAt));
                    policy.onInsert(key);
                }
            } finally { lock.unlock(); }
            notifications.forEach(Runnable::run);
        }

        public void remove(K key) {
            List<Runnable> notifications = new ArrayList<>();
            lock.lock();
            try { if (data.containsKey(key)) removeLocked(key, RemovalCause.EXPLICIT, notifications); }
            finally { lock.unlock(); }
            notifications.forEach(Runnable::run);
        }

        /**
         * READ-THROUGH with SINGLE-FLIGHT. 100 threads miss on the same hot key at once ->
         * without this, 100 identical DB queries (a "cache stampede" / "thundering herd").
         * With it, the first thread loads; the others wait on the same future.
         */
        public V getOrLoad(K key, Function<K, V> loader) {
            Optional<V> hit = get(key);
            if (hit.isPresent()) return hit.get();
            CompletableFuture<V> mine = new CompletableFuture<>();
            CompletableFuture<V> existing = inFlight.putIfAbsent(key, mine);
            if (existing != null) return existing.join();              // someone is already loading it
            try {
                Optional<V> again = get(key);                          // loaded between our miss and our putIfAbsent?
                V v = again.isPresent() ? again.get() : loader.apply(key);
                if (again.isEmpty()) { loads.increment(); put(key, v); }
                mine.complete(v);
                return v;
            } catch (RuntimeException ex) {
                mine.completeExceptionally(ex);                        // waiters see the failure too
                throw ex;
            } finally {
                inFlight.remove(key, mine);
            }
        }

        private boolean isExpired(Entry<V> e) { return clock.getAsLong() >= e.expiresAt(); }

        private void removeLocked(K key, RemovalCause cause, List<Runnable> notifications) {
            Entry<V> e = data.remove(key);
            policy.onRemove(key);
            if (e != null) notifications.add(() -> listener.onRemoval(key, e.value(), cause));
        }

        /** Optional proactive cleanup (a scheduled job could call this); reads expire lazily anyway. */
        int purgeExpired() {
            List<Runnable> notifications = new ArrayList<>();
            lock.lock();
            try {
                for (K k : new ArrayList<>(data.keySet())) if (isExpired(data.get(k))) removeLocked(k, RemovalCause.EXPIRED, notifications);
            } finally { lock.unlock(); }
            notifications.forEach(Runnable::run);
            return notifications.size();
        }

        public int size() { lock.lock(); try { return data.size(); } finally { lock.unlock(); } }
        public CacheStats stats() { return new CacheStats(hits.sum(), misses.sum(), evictions.sum(), loads.sum()); }
    }

    // =====================================================================
    // [scale] segmented cache = lock striping
    // =====================================================================

    /**
     * N independent BoundedCaches, chosen by key hash. Total capacity is split evenly.
     * Trade-off: eviction is LRU *per segment*, i.e. approximately global LRU. That's what
     * ConcurrentHashMap (Java 7) and Guava's cache did.
     */
    static class SegmentedCache<K, V> implements Cache<K, V> {
        private final List<BoundedCache<K, V>> segments = new ArrayList<>();
        SegmentedCache(int segmentsCount, Supplier<BoundedCache<K, V>> factory) {
            for (int i = 0; i < segmentsCount; i++) segments.add(factory.get());
        }
        private BoundedCache<K, V> seg(K key) {
            int h = key.hashCode();
            h ^= (h >>> 16);                                              // spread high bits like HashMap does
            return segments.get(Math.floorMod(h, segments.size()));
        }
        public Optional<V> get(K k) { return seg(k).get(k); }
        public void put(K k, V v) { seg(k).put(k, v); }
        public void remove(K k) { seg(k).remove(k); }
        public V getOrLoad(K k, Function<K, V> loader) { return seg(k).getOrLoad(k, loader); }
        public int size() { return segments.stream().mapToInt(BoundedCache::size).sum(); }
        public CacheStats stats() {
            long h = 0, m = 0, e = 0, l = 0;
            for (BoundedCache<K, V> s : segments) { CacheStats st = s.stats(); h += st.hits(); m += st.misses(); e += st.evictions(); l += st.loads(); }
            return new CacheStats(h, m, e, l);
        }
    }

    // =====================================================================
    // [builder]
    // =====================================================================

    enum Policy { LRU, LFU, FIFO }

    static class CacheBuilder<K, V> {
        private int capacity = 100, segments = 1;
        private long ttlMs = 0;
        private Policy policy = Policy.LRU;
        private RemovalListener<K, V> listener = (k, v, c) -> {};
        private LongSupplier clock = System::currentTimeMillis;

        CacheBuilder<K, V> capacity(int c) { capacity = c; return this; }
        CacheBuilder<K, V> policy(Policy p) { policy = p; return this; }
        CacheBuilder<K, V> ttlMs(long t) { ttlMs = t; return this; }
        CacheBuilder<K, V> segments(int s) { segments = s; return this; }
        CacheBuilder<K, V> listener(RemovalListener<K, V> l) { listener = l; return this; }
        CacheBuilder<K, V> clock(LongSupplier c) { clock = c; return this; }

        private EvictionPolicy<K> newPolicy() {                      // FACTORY for the strategy
            return switch (policy) { case LRU -> new LruPolicy<>(); case LFU -> new LfuPolicy<>(); case FIFO -> new FifoPolicy<>(); };
        }

        Cache<K, V> build() {
            if (segments == 1) return new BoundedCache<>(capacity, ttlMs, newPolicy(), listener, clock);
            int per = Math.max(1, capacity / segments);
            return new SegmentedCache<>(segments, () -> new BoundedCache<>(per, ttlMs, newPolicy(), listener, clock));
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static <V> String keys(Cache<String, V> c, String... all) {
        StringBuilder sb = new StringBuilder();
        for (String k : all) if (c.get(k).isPresent()) sb.append(k);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("1. Eviction policies on the same access pattern (capacity 3)");
        for (Policy p : Policy.values()) {
            Cache<String, Integer> c = new CacheBuilder<String, Integer>().capacity(3).policy(p).build();
            c.put("A", 1); c.put("B", 2); c.put("C", 3);
            c.get("A"); c.get("A"); c.get("B");                          // A used twice, B once, C never
            c.put("D", 4);                                               // someone must go
            String expected = switch (p) { case LRU -> "ABD"; case LFU -> "ABD"; case FIFO -> "BCD"; };
            check(p + " keeps", expected, keys(c, "A", "B", "C", "D"));
        }
        Cache<String, Integer> lru = new CacheBuilder<String, Integer>().capacity(2).policy(Policy.LRU).build();
        Cache<String, Integer> lfu = new CacheBuilder<String, Integer>().capacity(2).policy(Policy.LFU).build();
        for (Cache<String, Integer> c : List.of(lru, lfu)) {
            c.put("hot", 1); for (int i = 0; i < 5; i++) c.get("hot");
            c.put("x", 2); c.get("x");
            c.put("y", 3);                                               // LRU evicts hot (oldest access); LFU evicts x (freq 2 < 6)
        }
        check("LRU evicts the hot key after a scan", "xy", keys(lru, "hot", "x", "y"));
        check("LFU protects the hot key", "hoty", keys(lfu, "hot", "x", "y"));

        System.out.println("2. TTL (lazy expiry) + removal listener with causes");
        AtomicLong now = new AtomicLong();
        List<String> removed = new CopyOnWriteArrayList<>();
        Cache<String, String> ttl = new CacheBuilder<String, String>().capacity(2).ttlMs(1000).clock(now::get)
                .listener((k, v, cause) -> removed.add(k + ":" + cause)).build();
        ttl.put("s1", "a");
        now.set(999);
        check("alive at 999ms", "a", ttl.get("s1").orElse(null));
        now.set(1000);
        check("expired at 1000ms", true, ttl.get("s1").isEmpty());
        ttl.put("k1", "1"); ttl.put("k1", "2"); ttl.put("k2", "x"); ttl.put("k3", "y"); ttl.remove("k3");
        check("causes", "[s1:EXPIRED, k1:REPLACED, k1:EVICTED, k3:EXPLICIT]", removed);

        System.out.println("3. Stats");
        Cache<Integer, Integer> st = new CacheBuilder<Integer, Integer>().capacity(10).build();
        for (int i = 0; i < 20; i++) st.getOrLoad(i % 5, k -> k * k);
        check("5 loads, 15 hits on repeated keys", "CacheStats[hits=15, misses=10, evictions=0, loads=5]", st.stats());

        System.out.println("4. Cache stampede: 64 threads miss the same key at once");
        Cache<String, String> sf = new CacheBuilder<String, String>().capacity(10).build();
        AtomicInteger dbCalls = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(64);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 0; i < 64; i++) fs.add(pool.submit(() -> { go.await(); return sf.getOrLoad("config", k -> { dbCalls.incrementAndGet(); sleep(50); return "value"; }); }));
        go.countDown();
        boolean allSame = true;
        for (Future<String> f : fs) allSame &= f.get().equals("value");
        check("loader ran once (single-flight)", 1, dbCalls.get());
        check("all 64 got the value", true, allSame);

        System.out.println("5. Concurrency: 16 threads, 200k random ops on a segmented LRU (cap 1000, 8 segments)");
        Cache<Integer, Integer> seg = new CacheBuilder<Integer, Integer>().capacity(1000).segments(8).policy(Policy.LRU).build();
        AtomicBoolean wrongValue = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> work = new ArrayList<>();
        for (int t = 0; t < 16; t++) work.add(pool.submit(() -> {
            start.await();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 12_500; i++) {
                int k = r.nextInt(5000);
                if (r.nextBoolean()) seg.put(k, k * 10);
                else seg.get(k).ifPresent(v -> { if (v != k * 10) wrongValue.set(true); });
            }
            return null;
        }));
        start.countDown();
        for (Future<?> f : work) f.get();
        pool.shutdown();
        check("size never above capacity", true, seg.size() <= 1000);
        check("no corrupted values", false, wrongValue.get());
        System.out.println("   final size " + seg.size() + ", " + seg.stats());
        summary();
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

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
