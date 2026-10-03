/*
 * ============================================================================
 *  LLD 17 : IN-MEMORY KEY-VALUE STORE  (a mini Redis)
 * ============================================================================
 *  Run:  java KeyValueStore.java          (design notes: README.md)
 *
 *     [entry]      Entry (immutable): value + version + expiresAt
 *     [store]      KVStore: N lock STRIPES, each a HashMap guarded by its own lock
 *                  get / set (+TTL, NX) / del / incr / cas(version) / expire / ttl / scan(prefix)
 *     [expiry]     lazy (on access) + ACTIVE sampling cycle (Redis' algorithm)
 *     [eviction]   EvictionPolicy (Strategy) when maxKeys is reached:
 *                  NoEviction (reject writes), SampledLru (Redis-style approximated LRU)
 *     [txn]        MULTI/EXEC-style transactions with optimistic WATCH: lock all involved
 *                  stripes in ascending order (no deadlock), verify versions, apply all or none
 *     [aof]        Append-only log of write COMMANDS (Command pattern) -> replay to rebuild
 *     [demo]       TTLs, NX, CAS, eviction, active expiry, transactions, AOF replay, and
 *                  concurrent INCR / CAS / transfers
 *
 *  DESIGN CHOICE vs REDIS: Redis is SINGLE-THREADED - one event loop executes commands one by
 *  one, so it needs no locks at all (like Node.js). In Java we serve many threads, so we
 *  partition the keyspace into stripes: operations on different stripes run in parallel,
 *  operations on one key are serialised by its stripe's lock.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class KeyValueStore {

    // =====================================================================
    // [entry]
    // =====================================================================

    /** Immutable -> safe to hand out; every write creates a new Entry with version + 1. */
    record Entry(String value, long version, long expiresAt, long lastAccess) {
        boolean expired(long now) { return expiresAt > 0 && now >= expiresAt; }
    }

    static class KVException extends RuntimeException { KVException(String m) { super(m); } }

    // =====================================================================
    // [eviction]
    // =====================================================================

    /** Called (under the stripe lock) when a NEW key would exceed maxKeys. Returns the key to evict, or null = reject. */
    interface EvictionPolicy { String chooseVictim(Map<String, Entry> stripe, Random rnd); }

    static class NoEviction implements EvictionPolicy {
        public String chooseVictim(Map<String, Entry> s, Random r) { return null; }     // "OOM command not allowed"
    }

    /**
     * Redis "allkeys-lru" doesn't keep a full LRU list (too much memory per key). It SAMPLES
     * a few random keys and evicts the least recently used among them. With 5 samples it's
     * remarkably close to true LRU.
     */
    static class SampledLru implements EvictionPolicy {
        private final int samples;
        SampledLru(int samples) { this.samples = samples; }
        public String chooseVictim(Map<String, Entry> s, Random r) {
            if (s.isEmpty()) return null;
            List<String> keys = new ArrayList<>(s.keySet());
            String best = null;
            for (int i = 0; i < samples; i++) {
                String k = keys.get(r.nextInt(keys.size()));
                if (best == null || s.get(k).lastAccess() < s.get(best).lastAccess()) best = k;
            }
            return best;
        }
    }

    // =====================================================================
    // [aof] commands
    // =====================================================================

    /** A write command, recorded in order. Replaying the log rebuilds the dataset (Command pattern). */
    sealed interface Command permits SetCmd, DelCmd, ExpireCmd {}
    record SetCmd(String key, String value, long expiresAt) implements Command {}
    record DelCmd(String key) implements Command {}
    record ExpireCmd(String key, long expiresAt) implements Command {}

    // =====================================================================
    // [store]
    // =====================================================================

    static class KVStore {
        private final int stripes;
        private final List<Map<String, Entry>> maps = new ArrayList<>();
        private final List<ReentrantLock> locks = new ArrayList<>();
        private final int maxKeysPerStripe;
        private final EvictionPolicy eviction;
        private final LongSupplier clock;
        private final Random rnd;
        private final AtomicLong versions = new AtomicLong();
        final List<Command> aof = Collections.synchronizedList(new ArrayList<>());
        final LongAdder evictions = new LongAdder(), expiredRemoved = new LongAdder();

        KVStore(int stripes, int maxKeys, EvictionPolicy eviction, LongSupplier clock, Random rnd) {
            this.stripes = stripes; this.maxKeysPerStripe = Math.max(1, maxKeys / stripes); this.eviction = eviction; this.clock = clock; this.rnd = rnd;
            for (int i = 0; i < stripes; i++) { maps.add(new HashMap<>()); locks.add(new ReentrantLock()); }
        }

        int stripeOf(String key) { int h = key.hashCode(); return Math.floorMod(h ^ (h >>> 16), stripes); }

        private <T> T locked(String key, Function<Map<String, Entry>, T> op) {
            int s = stripeOf(key);
            locks.get(s).lock();
            try { return op.apply(maps.get(s)); } finally { locks.get(s).unlock(); }
        }

        /** Returns the live entry or null; deletes it if expired (LAZY expiry). Caller holds the lock. */
        private Entry live(Map<String, Entry> m, String key) {
            Entry e = m.get(key);
            if (e != null && e.expired(clock.getAsLong())) { m.remove(key); expiredRemoved.increment(); return null; }
            return e;
        }

        // ---------------- reads ----------------

        Optional<String> get(String key) {
            return locked(key, m -> {
                Entry e = live(m, key);
                if (e == null) return Optional.<String>empty();
                m.put(key, new Entry(e.value(), e.version(), e.expiresAt(), clock.getAsLong()));   // touch for LRU (same version)
                return Optional.of(e.value());
            });
        }

        /** The version is what clients use for optimistic concurrency (like an ETag). */
        OptionalLong version(String key) { return locked(key, m -> { Entry e = live(m, key); return e == null ? OptionalLong.empty() : OptionalLong.of(e.version()); }); }

        /** Remaining TTL in ms: -2 = no key, -1 = no expiry (Redis semantics). */
        long ttl(String key) {
            return locked(key, m -> { Entry e = live(m, key); return e == null ? -2L : e.expiresAt() == 0 ? -1L : e.expiresAt() - clock.getAsLong(); });
        }

        // ---------------- writes ----------------

        /** SET key value [PX ttl] [NX]. Returns false if NX and the key exists. */
        boolean set(String key, String value, long ttlMs, boolean onlyIfAbsent) {
            return locked(key, m -> {
                Entry cur = live(m, key);
                if (onlyIfAbsent && cur != null) return false;
                if (cur == null) makeRoom(m);
                long exp = ttlMs > 0 ? clock.getAsLong() + ttlMs : 0;
                m.put(key, new Entry(value, versions.incrementAndGet(), exp, clock.getAsLong()));
                aof.add(new SetCmd(key, value, exp));
                return true;
            });
        }
        void set(String key, String value) { set(key, value, 0, false); }

        private void makeRoom(Map<String, Entry> m) {
            if (m.size() < maxKeysPerStripe) return;
            String victim = eviction.chooseVictim(m, rnd);
            if (victim == null) throw new KVException("OOM: maxmemory reached and policy is noeviction");
            m.remove(victim);
            evictions.increment();
            aof.add(new DelCmd(victim));
        }

        boolean del(String key) { return locked(key, m -> { boolean had = live(m, key) != null; m.remove(key); if (had) aof.add(new DelCmd(key)); return had; }); }

        /** Atomic increment (read-modify-write under the stripe lock). Keeps the key's TTL. */
        long incrBy(String key, long delta) {
            return locked(key, m -> {
                Entry e = live(m, key);
                long cur;
                try { cur = e == null ? 0 : Long.parseLong(e.value()); }
                catch (NumberFormatException ex) { throw new KVException("value is not an integer"); }
                if (e == null) makeRoom(m);
                long next = Math.addExact(cur, delta);
                m.put(key, new Entry(Long.toString(next), versions.incrementAndGet(), e == null ? 0 : e.expiresAt(), clock.getAsLong()));
                aof.add(new SetCmd(key, Long.toString(next), e == null ? 0 : e.expiresAt()));
                return next;
            });
        }

        /** Compare-and-set on the VERSION: succeed only if nobody wrote since you read. */
        boolean cas(String key, long expectedVersion, String newValue) {
            return locked(key, m -> {
                Entry e = live(m, key);
                if (e == null || e.version() != expectedVersion) return false;
                m.put(key, new Entry(newValue, versions.incrementAndGet(), e.expiresAt(), clock.getAsLong()));
                aof.add(new SetCmd(key, newValue, e.expiresAt()));
                return true;
            });
        }

        boolean expire(String key, long ttlMs) {
            return locked(key, m -> {
                Entry e = live(m, key);
                if (e == null) return false;
                long exp = clock.getAsLong() + ttlMs;
                m.put(key, new Entry(e.value(), e.version(), exp, e.lastAccess()));
                aof.add(new ExpireCmd(key, exp));
                return true;
            });
        }

        /** Keys with a prefix, stripe by stripe (each stripe consistent, the whole scan not a snapshot - like Redis SCAN). */
        List<String> scan(String prefix) {
            List<String> out = new ArrayList<>();
            for (int s = 0; s < stripes; s++) {
                locks.get(s).lock();
                try { for (String k : new ArrayList<>(maps.get(s).keySet())) if (k.startsWith(prefix) && live(maps.get(s), k) != null) out.add(k); }
                finally { locks.get(s).unlock(); }
            }
            Collections.sort(out);
            return out;
        }

        // ---------------- active expiry ----------------

        /**
         * Redis' active expire cycle: per stripe, sample up to 20 keys that HAVE a TTL, delete the
         * expired ones, and repeat while more than 25% of the sample was expired. Bounded work per
         * call, yet it keeps memory from filling with dead keys nobody reads.
         */
        int activeExpireCycle() {
            int removed = 0;
            long now = clock.getAsLong();
            for (int s = 0; s < stripes; s++) {
                locks.get(s).lock();
                try {
                    Map<String, Entry> m = maps.get(s);
                    while (true) {
                        List<String> withTtl = m.entrySet().stream().filter(e -> e.getValue().expiresAt() > 0).map(Map.Entry::getKey).toList();
                        if (withTtl.isEmpty()) break;
                        int sample = Math.min(20, withTtl.size()), expired = 0;
                        for (int i = 0; i < sample; i++) {
                            String k = withTtl.get(rnd.nextInt(withTtl.size()));
                            Entry e = m.get(k);
                            if (e != null && e.expired(now)) { m.remove(k); expired++; removed++; expiredRemoved.increment(); }
                        }
                        if (expired * 4 <= sample) break;               // <= 25% expired: good enough, stop
                    }
                } finally { locks.get(s).unlock(); }
            }
            return removed;
        }

        int size() { int n = 0; for (int s = 0; s < stripes; s++) { locks.get(s).lock(); try { n += maps.get(s).size(); } finally { locks.get(s).unlock(); } } return n; }

        // ---------------- transactions ----------------

        /** MULTI ... EXEC with WATCH. Build with ops, then exec(). */
        Transaction multi() { return new Transaction(this); }

        static class Transaction {
            private final KVStore store;
            private final Map<String, Long> watched = new LinkedHashMap<>();       // key -> version seen (-1 = absent)
            private final List<BiConsumer<Map<String, Map<String, Entry>>, List<Command>>> ops = new ArrayList<>();
            private final Set<String> keys = new TreeSet<>();
            Transaction(KVStore s) { store = s; }

            /** WATCH: remember the current version; EXEC aborts if it changed (optimistic locking). */
            Transaction watch(String key) { watched.put(key, store.version(key).orElse(-1)); keys.add(key); return this; }

            Transaction set(String key, String value) {
                keys.add(key);
                ops.add((maps, log) -> { Map<String, Entry> m = maps.get(key); m.put(key, new Entry(value, store.versions.incrementAndGet(), 0, store.clock.getAsLong())); log.add(new SetCmd(key, value, 0)); });
                return this;
            }

            Transaction incrBy(String key, long delta) {
                keys.add(key);
                ops.add((maps, log) -> {
                    Map<String, Entry> m = maps.get(key);
                    Entry e = store.live(m, key);
                    long next = (e == null ? 0 : Long.parseLong(e.value())) + delta;
                    m.put(key, new Entry(Long.toString(next), store.versions.incrementAndGet(), e == null ? 0 : e.expiresAt(), store.clock.getAsLong()));
                    log.add(new SetCmd(key, Long.toString(next), e == null ? 0 : e.expiresAt()));
                });
                return this;
            }

            /**
             * Lock every involved stripe in ASCENDING index order (global order -> no deadlock
             * between two transactions touching the same stripes), check WATCHed versions,
             * apply all ops. Returns false (nothing applied) if a watched key changed.
             */
            boolean exec() {
                TreeSet<Integer> stripeIds = new TreeSet<>();
                for (String k : keys) stripeIds.add(store.stripeOf(k));
                for (int s : stripeIds) store.locks.get(s).lock();
                try {
                    for (var w : watched.entrySet()) {
                        Entry e = store.live(store.maps.get(store.stripeOf(w.getKey())), w.getKey());
                        if ((e == null ? -1 : e.version()) != w.getValue()) return false;   // someone wrote: abort
                    }
                    Map<String, Map<String, Entry>> byKey = new HashMap<>();
                    for (String k : keys) byKey.put(k, store.maps.get(store.stripeOf(k)));
                    List<Command> log = new ArrayList<>();
                    for (var op : ops) op.accept(byKey, log);
                    store.aof.addAll(log);                                 // logged together (atomic in the AOF)
                    return true;
                } finally {
                    for (int s : stripeIds.descendingSet()) store.locks.get(s).unlock();
                }
            }
        }

        // ---------------- persistence ----------------

        /** Rebuild a store from an AOF (what Redis does on restart). Expired entries vanish naturally. */
        static KVStore replay(List<Command> log, int stripes, LongSupplier clock) {
            KVStore s = new KVStore(stripes, Integer.MAX_VALUE, new NoEviction(), clock, new Random(0));
            for (Command c : log) switch (c) {
                case SetCmd sc -> s.locked(sc.key(), m -> m.put(sc.key(), new Entry(sc.value(), s.versions.incrementAndGet(), sc.expiresAt(), clock.getAsLong())));
                case DelCmd dc -> s.locked(dc.key(), m -> m.remove(dc.key()));
                case ExpireCmd ec -> s.locked(ec.key(), m -> { Entry e = m.get(ec.key()); return e == null ? null : m.put(ec.key(), new Entry(e.value(), e.version(), ec.expiresAt(), e.lastAccess())); });
            }
            return s;
        }

        Map<String, String> dump() {
            Map<String, String> out = new TreeMap<>();
            for (String k : scan("")) get(k).ifPresent(v -> out.put(k, v));
            return out;
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong(1_000);
        KVStore kv = new KVStore(8, 10_000, new SampledLru(5), clock::get, new Random(1));

        System.out.println("1. Basic ops, TTL, NX");
        kv.set("user:1", "asha");
        check("get", "asha", kv.get("user:1").orElse(null));
        check("ttl of a key without expiry", -1L, kv.ttl("user:1"));
        check("ttl of a missing key", -2L, kv.ttl("nope"));
        kv.set("session:9", "tok", 5_000, false);
        check("ttl counts down", 5_000L, kv.ttl("session:9"));
        clock.addAndGet(5_000);
        check("expired exactly at the deadline (lazy)", true, kv.get("session:9").isEmpty());
        check("SET NX on an existing key fails", false, kv.set("user:1", "x", 0, true));
        check("SET NX on a new key works (a simple lock)", true, kv.set("lock:job", "worker-1", 30_000, true));
        check("del", true, kv.del("user:1"));
        check("scan by prefix", "[lock:job]", kv.scan("lock:"));

        System.out.println("2. INCR and CAS (optimistic concurrency)");
        check("incr from nothing", 1L, kv.incrBy("views", 1));
        check("incr by 10", 11L, kv.incrBy("views", 10));
        kv.set("name", "bob");
        expect("incr on a non-number", () -> kv.incrBy("name", 1));
        long v = kv.version("name").getAsLong();
        check("cas with the right version", true, kv.cas("name", v, "bobby"));
        check("cas with a stale version fails", false, kv.cas("name", v, "robert"));

        System.out.println("3. Eviction policy when full");
        KVStore tiny = new KVStore(1, 3, new NoEviction(), clock::get, new Random(1));
        tiny.set("a", "1"); tiny.set("b", "2"); tiny.set("c", "3");
        expect("noeviction: writes rejected when full", () -> tiny.set("d", "4"));
        KVStore lru = new KVStore(1, 100, new SampledLru(10), clock::get, new Random(2));
        for (int i = 0; i < 100; i++) { clock.incrementAndGet(); lru.set("k" + i, "v"); }
        for (int i = 50; i < 100; i++) { clock.incrementAndGet(); lru.get("k" + i); }      // the newer half is hot
        for (int i = 0; i < 40; i++) { clock.incrementAndGet(); lru.set("new" + i, "v"); }
        long hotSurvived = 0;
        for (int i = 50; i < 100; i++) if (lru.get("k" + i).isPresent()) hotSurvived++;
        check("40 evictions, size stays 100", "40/100", lru.evictions.sum() + "/" + lru.size());
        check("sampled LRU mostly evicts cold keys (>= 45 of 50 hot survive)", true, hotSurvived >= 45);
        System.out.println("   hot keys surviving: " + hotSurvived + "/50");

        System.out.println("4. Active expiry reclaims keys nobody reads");
        KVStore ex = new KVStore(4, 100_000, new NoEviction(), clock::get, new Random(3));
        for (int i = 0; i < 5000; i++) ex.set("tmp" + i, "x", 1_000, false);
        for (int i = 0; i < 1000; i++) ex.set("keep" + i, "x");
        clock.addAndGet(2_000);
        int removed = 0;
        for (int round = 0; round < 50 && ex.size() > 1000; round++) removed += ex.activeExpireCycle();
        check("expired keys reclaimed without being read", 1000, ex.size());
        System.out.println("   removed " + removed + " keys by sampling");

        System.out.println("5. Transactions: WATCH + MULTI/EXEC");
        kv.set("acct:a", "100"); kv.set("acct:b", "0");
        KVStore.Transaction tx = kv.multi().watch("acct:a").incrBy("acct:a", -30).incrBy("acct:b", 30);
        check("exec applies all", true, tx.exec());
        check("balances", "70/30", kv.get("acct:a").get() + "/" + kv.get("acct:b").get());
        KVStore.Transaction stale = kv.multi().watch("acct:a").incrBy("acct:a", -70).incrBy("acct:b", 70);
        kv.incrBy("acct:a", 5);                                            // someone else writes between WATCH and EXEC
        check("watched key changed -> aborted", false, stale.exec());
        check("nothing applied", "75/30", kv.get("acct:a").get() + "/" + kv.get("acct:b").get());

        System.out.println("6. AOF replay rebuilds the same data");
        Map<String, String> before = kv.dump();
        KVStore restored = KVStore.replay(List.copyOf(kv.aof), 8, clock::get);
        check("restored == original", before, restored.dump());

        System.out.println("7. Concurrency");
        KVStore c = new KVStore(16, 100_000, new NoEviction(), clock::get, new Random(4));
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < 16; t++) fs.add(pool.submit(() -> { for (int i = 0; i < 1000; i++) c.incrBy("counter", 1); }));
        for (Future<?> f : fs) f.get();
        check("16 x 1000 INCR = 16000 (no lost updates)", "16000", c.get("counter").get());

        c.set("cas", "0");
        fs.clear();
        for (int t = 0; t < 8; t++) fs.add(pool.submit(() -> {
            for (int i = 0; i < 500; i++)
                while (true) { long ver = c.version("cas").getAsLong(); long val = Long.parseLong(c.get("cas").get()); if (c.cas("cas", ver, Long.toString(val + 1))) break; }
        }));
        for (Future<?> f : fs) f.get();
        check("CAS retry loops: 8 x 500 = 4000", "4000", c.get("cas").get());

        for (int i = 0; i < 20; i++) c.set("bal:" + i, "1000");
        fs.clear();
        for (int t = 0; t < 16; t++) fs.add(pool.submit(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 500; i++) {
                String a = "bal:" + r.nextInt(20), b = "bal:" + r.nextInt(20);
                if (a.equals(b)) continue;
                c.multi().incrBy(a, -7).incrBy(b, 7).exec();               // stripes locked in order: no deadlock
            }
        }));
        for (Future<?> f : fs) f.get(20, TimeUnit.SECONDS);
        pool.shutdown();
        long sum = 0;
        for (int i = 0; i < 20; i++) sum += Long.parseLong(c.get("bal:" + i).get());
        check("~8,000 concurrent transfer transactions: total conserved", 20_000L, sum);
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (KVException e) { check(label, "exception", "exception"); }
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
