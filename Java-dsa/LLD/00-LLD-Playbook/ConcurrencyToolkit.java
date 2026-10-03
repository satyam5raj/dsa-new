/*
 * ============================================================================
 *  CONCURRENCY TOOLKIT FOR LLD: the 6 building blocks almost every design reuses
 * ============================================================================
 *
 *  Run:  java ConcurrencyToolkit.java
 *
 *  Every LLD interview has the moment: "what if two users do this at the same time?"
 *  These are the answers, each one runnable:
 *
 *   1. CHECK-THEN-ACT RACE  ->  make check + act atomic with a PER-KEY lock
 *        "if seat is free -> mark it booked" done by 2 threads = double booking.
 *   2. OPTIMISTIC LOCKING   ->  version + compare-and-set, retry on conflict
 *        no lock held while "thinking"; great when conflicts are rare.
 *   3. LOCK ORDERING        ->  transfer(A, B) and transfer(B, A) at once deadlock
 *        unless both always lock the smaller id first.
 *   4. IDEMPOTENCY KEYS     ->  a retried request must not charge twice
 *        store key -> result (a Future, so concurrent duplicates WAIT for the first).
 *   5. HOLD WITH EXPIRY     ->  temporary reservations that free themselves (TTL),
 *        using an INJECTED clock so tests control time.
 *   6. PRODUCER / CONSUMER  ->  BlockingQueue + worker threads (back-pressure).
 *
 *  Each section runs the scenario with many threads started at the same instant
 *  (CountDownLatch "starting gun") to maximise the chance of a real race.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class ConcurrencyToolkit {

    // =====================================================================
    // 1. CHECK-THEN-ACT  (the double-booking bug)
    // =====================================================================

    enum SeatStatus { AVAILABLE, BOOKED }

    /** BROKEN: the check and the act are two separate steps. */
    static class UnsafeSeatMap {
        final Map<String, SeatStatus> seats = new ConcurrentHashMap<>();   // a thread-safe MAP is NOT enough!

        boolean book(String seat) {
            if (seats.get(seat) == SeatStatus.AVAILABLE) {                // CHECK
                sleepQuietly(1);                                          // widen the window (a real app does DB/IO here)
                seats.put(seat, SeatStatus.BOOKED);                       // ACT  - another thread may have passed the check too
                return true;
            }
            return false;
        }
    }

    /**
     * FIXED with a PER-KEY lock: threads booking DIFFERENT seats never block each other,
     * threads booking the SAME seat are serialised. (A single global lock would also be
     * correct, but every booking in the whole system would queue behind one lock.)
     */
    static class KeyedLocks {
        private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

        <T> T withLock(String key, Supplier<T> action) {
            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());   // one lock object per key
            lock.lock();
            try { return action.get(); }
            finally { lock.unlock(); }                                    // ALWAYS unlock in finally
        }
    }

    static class SafeSeatMap {
        final Map<String, SeatStatus> seats = new ConcurrentHashMap<>();
        final KeyedLocks locks = new KeyedLocks();

        boolean book(String seat) {
            return locks.withLock(seat, () -> {
                if (seats.get(seat) != SeatStatus.AVAILABLE) return false;   // check ...
                sleepQuietly(1);
                seats.put(seat, SeatStatus.BOOKED);                         // ... and act, under the same lock
                return true;
            });
        }
    }

    // Another correct option without explicit locks: an atomic map operation.
    // ConcurrentHashMap.replace(key, expected, newValue) is a CAS on one entry.
    static boolean bookWithReplace(Map<String, SeatStatus> seats, String seat) {
        return seats.replace(seat, SeatStatus.AVAILABLE, SeatStatus.BOOKED);
    }

    // =====================================================================
    // 2. OPTIMISTIC LOCKING (version + CAS)
    // =====================================================================

    /** Immutable snapshot: a balance together with its version. */
    record Versioned(long balance, long version) {}

    static class OptimisticAccount {
        final AtomicReference<Versioned> state = new AtomicReference<>(new Versioned(0, 0));
        final AtomicLong conflicts = new AtomicLong();

        void deposit(long amount) {
            while (true) {
                Versioned cur = state.get();                              // read (no lock)
                Versioned next = new Versioned(cur.balance() + amount, cur.version() + 1);
                if (state.compareAndSet(cur, next)) return;               // write only if nobody changed it meanwhile
                conflicts.incrementAndGet();                              // someone won the race -> retry with fresh data
            }
        }
        // DB equivalent: UPDATE account SET balance=?, version=version+1 WHERE id=? AND version=?
        //                -> 0 rows updated means conflict -> reload and retry
    }

    // =====================================================================
    // 3. LOCK ORDERING (deadlock-free transfer)
    // =====================================================================

    static class Account {
        final long id;
        long balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(long id, long balance) { this.id = id; this.balance = balance; }
    }

    /**
     * Thread 1: transfer(A, B) locks A then B.  Thread 2: transfer(B, A) locks B then A.
     * Each holds one lock and waits for the other -> DEADLOCK.
     * FIX: always acquire locks in a GLOBAL ORDER (smaller id first), whatever the direction.
     */
    static boolean transfer(Account from, Account to, long amount) {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (from.balance < amount) return false;                  // invariant: no negative balance
                from.balance -= amount;
                to.balance += amount;
                return true;
            } finally { second.lock.unlock(); }
        } finally { first.lock.unlock(); }
    }

    // =====================================================================
    // 4. IDEMPOTENCY KEYS
    // =====================================================================

    static class IdempotentPaymentService {
        // key -> the FUTURE result. putIfAbsent of a future means: the first request runs the
        // work; concurrent duplicates find the same future and simply wait for its result.
        private final ConcurrentHashMap<String, CompletableFuture<String>> results = new ConcurrentHashMap<>();
        final AtomicInteger actualCharges = new AtomicInteger();

        String charge(String idempotencyKey, long amount) {
            CompletableFuture<String> mine = new CompletableFuture<>();
            CompletableFuture<String> existing = results.putIfAbsent(idempotencyKey, mine);
            if (existing != null) return existing.join();                 // duplicate: replay the first result
            try {
                actualCharges.incrementAndGet();                          // the real side effect, exactly once
                sleepQuietly(5);
                String receipt = "receipt-" + idempotencyKey + "-" + amount;
                mine.complete(receipt);
                return receipt;
            } catch (RuntimeException e) {
                results.remove(idempotencyKey);                           // failed attempts may be retried
                mine.completeExceptionally(e);
                throw e;
            }
        }
        // Production: key stored in a DB table with a UNIQUE constraint, plus the response,
        // with a TTL (e.g. 24h). Stripe's API works exactly like this.
    }

    // =====================================================================
    // 5. HOLD WITH EXPIRY (TTL) + INJECTED CLOCK
    // =====================================================================

    /** A tiny controllable clock: production passes System::currentTimeMillis. */
    static class FakeClock implements LongSupplier {
        private long now;
        public long getAsLong() { return now; }
        void advance(long ms) { now += ms; }
    }

    static class SeatHolds {
        record Hold(String user, long expiresAt) {}
        private final ConcurrentHashMap<String, Hold> holds = new ConcurrentHashMap<>();
        private final LongSupplier clock;
        private final long ttlMs;
        SeatHolds(LongSupplier clock, long ttlMs) { this.clock = clock; this.ttlMs = ttlMs; }

        /** Atomically: grant the hold if the seat is free OR the previous hold expired. */
        boolean hold(String seat, String user) {
            long now = clock.getAsLong();
            Hold result = holds.compute(seat, (k, cur) ->
                    cur == null || cur.expiresAt() <= now ? new Hold(user, now + ttlMs) : cur);
            return result.user().equals(user) && result.expiresAt() == now + ttlMs;
        }

        boolean confirm(String seat, String user) {                      // only the current, unexpired holder
            Hold h = holds.get(seat);
            return h != null && h.user().equals(user) && h.expiresAt() > clock.getAsLong();
        }
    }

    // =====================================================================
    // 6. PRODUCER / CONSUMER
    // =====================================================================

    static int producerConsumer(int items, int workers) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);       // BOUNDED -> producers block when full (back-pressure)
        AtomicInteger processed = new AtomicInteger();
        int POISON = -1;                                                  // "poison pill" tells a worker to stop
        List<Thread> ts = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        int x = queue.take();                             // blocks while empty
                        if (x == POISON) return;
                        processed.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            t.start();
            ts.add(t);
        }
        for (int i = 0; i < items; i++) queue.put(i);                     // blocks while full
        for (int w = 0; w < workers; w++) queue.put(POISON);
        for (Thread t : ts) t.join();
        return processed.get();
    }

    // =====================================================================
    // helpers + main
    // =====================================================================

    /** Runs `task` on n threads released at the same instant; returns how many returned true. */
    static int race(int n, Supplier<Boolean> task) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { return; }
                if (task.get()) wins.incrementAndGet();
            });
            t.start();
            ts.add(t);
        }
        start.countDown();                                                // starting gun
        for (Thread t : ts) t.join();
        return wins.get();
    }

    static void sleepQuietly(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    public static void main(String[] args) throws Exception {
        System.out.println("1. Check-then-act: 20 users book seat A1 at the same moment");
        UnsafeSeatMap unsafe = new UnsafeSeatMap();
        unsafe.seats.put("A1", SeatStatus.AVAILABLE);
        int unsafeWins = race(20, () -> unsafe.book("A1"));
        System.out.println("   unsafe (ConcurrentHashMap, no atomicity): " + unsafeWins + " users 'booked' the same seat");
        SafeSeatMap safe = new SafeSeatMap();
        safe.seats.put("A1", SeatStatus.AVAILABLE);
        check("per-key lock: exactly one winner", 1, race(20, () -> safe.book("A1")));
        Map<String, SeatStatus> m = new ConcurrentHashMap<>(Map.of("A1", SeatStatus.AVAILABLE));
        check("CHM.replace (CAS): exactly one winner", 1, race(20, () -> bookWithReplace(m, "A1")));

        System.out.println("2. Optimistic locking: 8 threads x 1000 deposits of 1");
        OptimisticAccount acc = new OptimisticAccount();
        race(8, () -> { for (int i = 0; i < 1000; i++) acc.deposit(1); return true; });
        check("no lost updates", 8000L, acc.state.get().balance());
        check("version counts every write", 8000L, acc.state.get().version());
        System.out.println("   CAS conflicts that were retried: " + acc.conflicts.get());

        System.out.println("3. Lock ordering: A->B and B->A transfers in parallel");
        Account a = new Account(1, 1_000_000), b = new Account(2, 1_000_000);
        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) transfer(a, b, 1); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) transfer(b, a, 1); });
        t1.start(); t2.start();
        t1.join(5000); t2.join(5000);
        check("finished (no deadlock)", false, t1.isAlive() || t2.isAlive());
        check("money conserved", 2_000_000L, a.balance + b.balance);

        System.out.println("4. Idempotency: the client retries the same payment 10 times concurrently");
        IdempotentPaymentService pay = new IdempotentPaymentService();
        Set<String> receipts = ConcurrentHashMap.newKeySet();
        race(10, () -> receipts.add(pay.charge("order-42", 500)));
        check("charged exactly once", 1, pay.actualCharges.get());
        check("every retry got the same receipt", 1, receipts.size());

        System.out.println("5. Seat hold with TTL (fake clock)");
        FakeClock clock = new FakeClock();
        SeatHolds holds = new SeatHolds(clock, 10 * 60_000);              // 10-minute hold
        check("alice holds A1", true, holds.hold("A1", "alice"));
        check("bob can't while it's held", false, holds.hold("A1", "bob"));
        clock.advance(11 * 60_000);                                        // alice abandons checkout
        check("alice's confirm fails after expiry", false, holds.confirm("A1", "alice"));
        check("bob gets it after expiry", true, holds.hold("A1", "bob"));

        System.out.println("6. Producer / consumer with a bounded queue");
        check("all 10,000 items processed by 4 workers", 10_000, producerConsumer(10_000, 4));
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
