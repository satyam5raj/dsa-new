/*
 * ============================================================================
 *  OS 04 : CONCURRENCY AND SYNCHRONIZATION  (real threads, real races, a real deadlock)
 * ============================================================================
 *  Run:  java Concurrency.java          (explanations: README.md in this folder)
 *
 *    1. race condition        count++ from 8 threads loses updates (real)
 *    2. fixes                 synchronized, AtomicInteger (CAS), LongAdder -- and their cost under contention
 *    3. CAS                   a compare-and-swap retry loop; a lock-free (Treiber) stack
 *    4. semaphore             cap concurrent access to a resource
 *    5. read-write lock       many readers OR one writer
 *    6. condition variable    wait until a condition is true (no busy waiting)
 *    7. barrier               phases: everyone waits for everyone
 *    8. visibility            volatile and happens-before
 *    9. deadlock              create a real one, detect it with ThreadMXBean, then avoid it (ordering, tryLock)
 * ============================================================================
 */

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class Concurrency {

    static final int THREADS = 8, PER_THREAD = 200_000;

    static void runAll(Runnable r) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) { Thread t = new Thread(() -> { try { go.await(); } catch (InterruptedException ignored) { } r.run(); }); ts.add(t); t.start(); }
        go.countDown();                                                  // release everyone at once: maximum contention
        for (Thread t : ts) t.join();
    }

    // ---------------------------------------------------------------------
    // 1-2. race condition and fixes
    // ---------------------------------------------------------------------
    static int unsafeCount = 0, syncCount = 0;

    static void races() throws Exception {
        section("1-2. Race condition: count++ from 8 threads x 200,000");
        runAll(() -> { for (int i = 0; i < PER_THREAD; i++) unsafeCount++; });            // read-modify-write, not atomic
        show(String.format("unsafe count = %,d (expected %,d) -> %,d updates LOST", unsafeCount, THREADS * PER_THREAD, THREADS * PER_THREAD - unsafeCount));
        check("count++ is NOT atomic: concurrent read-modify-write loses updates", true, unsafeCount < THREADS * PER_THREAD);

        Object lock = new Object();
        long t0 = System.nanoTime();
        runAll(() -> { for (int i = 0; i < PER_THREAD; i++) synchronized (lock) { syncCount++; } });
        double tSync = (System.nanoTime() - t0) / 1e6;
        AtomicInteger atomic = new AtomicInteger();
        t0 = System.nanoTime();
        runAll(() -> { for (int i = 0; i < PER_THREAD; i++) atomic.incrementAndGet(); });
        double tAtomic = (System.nanoTime() - t0) / 1e6;
        LongAdder adder = new LongAdder();
        t0 = System.nanoTime();
        runAll(() -> { for (int i = 0; i < PER_THREAD; i++) adder.increment(); });
        double tAdder = (System.nanoTime() - t0) / 1e6;
        check("synchronized (mutex) -> exact", THREADS * PER_THREAD, syncCount);
        check("AtomicInteger (hardware CAS) -> exact", THREADS * PER_THREAD, atomic.get());
        check("LongAdder (striped counters, summed on read) -> exact", (long) THREADS * PER_THREAD, adder.sum());
        show(String.format("time for 1.6M contended increments: synchronized %.0f ms, AtomicInteger %.0f ms, LongAdder %.0f ms", tSync, tAtomic, tAdder));
        show("LongAdder usually wins under heavy contention: threads update separate cells instead of one hot cache line");
    }

    // ---------------------------------------------------------------------
    // 3. CAS and a lock-free stack
    // ---------------------------------------------------------------------
    static class TreiberStack<T> {
        record Node<T>(T value, Node<T> next) { }
        final AtomicReference<Node<T>> head = new AtomicReference<>();
        final AtomicLong casFailures = new AtomicLong();

        void push(T v) {
            while (true) {
                Node<T> old = head.get();
                if (head.compareAndSet(old, new Node<>(v, old))) return;   // succeed only if nobody changed head meanwhile
                casFailures.incrementAndGet();                              // someone won the race: retry with the new head
            }
        }

        T pop() {
            while (true) {
                Node<T> old = head.get();
                if (old == null) return null;
                if (head.compareAndSet(old, old.next())) return old.value();
                casFailures.incrementAndGet();
            }
        }
    }

    static void cas() throws Exception {
        section("3. Compare-and-swap and a lock-free stack");
        AtomicInteger x = new AtomicInteger(5);
        check("CAS(expected 5 -> 6) succeeds when the value is 5", true, x.compareAndSet(5, 6));
        check("CAS(expected 5 -> 7) fails because the value is now 6", false, x.compareAndSet(5, 7));
        TreiberStack<Integer> stack = new TreiberStack<>();
        AtomicInteger id = new AtomicInteger();
        runAll(() -> { for (int i = 0; i < 20_000; i++) stack.push(id.incrementAndGet()); });
        Set<Integer> popped = new HashSet<>();
        Integer v;
        while ((v = stack.pop()) != null) popped.add(v);
        check("lock-free stack: 160,000 concurrent pushes, none lost or duplicated", THREADS * 20_000, popped.size());
        show(String.format("CAS retries under contention: %,d (threads never block, they just retry)", stack.casFailures.get()));
        check("lock-free = some thread always makes progress; individual threads may retry", true, stack.casFailures.get() > 0);
    }

    // ---------------------------------------------------------------------
    // 4. semaphore
    // ---------------------------------------------------------------------
    static void semaphore() throws Exception {
        section("4. Semaphore: at most 3 concurrent users of a resource");
        Semaphore permits = new Semaphore(3);
        AtomicInteger inside = new AtomicInteger(), maxInside = new AtomicInteger();
        ExecutorService ex = Executors.newFixedThreadPool(12);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 24; i++) fs.add(ex.submit(() -> {
            permits.acquireUninterruptibly();
            try {
                int now = inside.incrementAndGet();
                maxInside.accumulateAndGet(now, Math::max);
                Thread.sleep(10);
                inside.decrementAndGet();
            } catch (InterruptedException ignored) {
            } finally { permits.release(); }
            return null;
        }));
        for (Future<?> f : fs) f.get();
        ex.shutdown();
        check("12 threads competing, never more than 3 inside (like a connection-pool limit)", 3, maxInside.get());
    }

    // ---------------------------------------------------------------------
    // 5. read-write lock
    // ---------------------------------------------------------------------
    static void readWrite() throws Exception {
        section("5. Read-write lock");
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        AtomicInteger readers = new AtomicInteger(), maxReaders = new AtomicInteger();
        AtomicBoolean writerSawReader = new AtomicBoolean(false);
        ExecutorService ex = Executors.newFixedThreadPool(10);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            boolean writer = i % 10 == 0;
            fs.add(ex.submit(() -> {
                Lock l = writer ? rw.writeLock() : rw.readLock();
                l.lock();
                try {
                    if (writer) { if (readers.get() > 0) writerSawReader.set(true); }
                    else { maxReaders.accumulateAndGet(readers.incrementAndGet(), Math::max); }
                    Thread.sleep(5);
                    if (!writer) readers.decrementAndGet();
                } catch (InterruptedException ignored) {
                } finally { l.unlock(); }
                return null;
            }));
        }
        for (Future<?> f : fs) f.get();
        ex.shutdown();
        check("many readers hold the lock at the same time", true, maxReaders.get() > 1);
        check("a writer never overlaps a reader", false, writerSawReader.get());
    }

    // ---------------------------------------------------------------------
    // 6. condition variable
    // ---------------------------------------------------------------------
    static void condition() throws Exception {
        section("6. Condition variable: wait for 'data ready' without spinning");
        ReentrantLock lock = new ReentrantLock();
        Condition ready = lock.newCondition();
        String[] box = {null};
        String[] got = {null};
        Thread consumer = new Thread(() -> {
            lock.lock();
            try {
                while (box[0] == null) ready.awaitUninterruptibly();     // ALWAYS a while loop: spurious wakeups happen
                got[0] = box[0];
            } finally { lock.unlock(); }
        });
        consumer.start();
        Thread.sleep(50);
        check("the consumer is parked (WAITING), not burning CPU", Thread.State.WAITING, consumer.getState());
        lock.lock();
        try { box[0] = "payload"; ready.signal(); } finally { lock.unlock(); }
        consumer.join();
        check("signal() wakes it and it sees the data", "payload", got[0]);
    }

    // ---------------------------------------------------------------------
    // 7. barrier
    // ---------------------------------------------------------------------
    static void barrier() throws Exception {
        section("7. Barrier: phase-by-phase parallel work");
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        CyclicBarrier b = new CyclicBarrier(4, () -> log.add("|"));          // the action runs once all 4 arrive
        runParties(4, i -> { log.add("A"); await(b); log.add("B"); await(b); });
        String s = String.join("", log);
        check("no worker starts phase B until all finished phase A", "AAAA|BBBB|", s);
    }

    static void await(CyclicBarrier b) { try { b.await(); } catch (Exception e) { throw new RuntimeException(e); } }
    static void await(CountDownLatch l) { try { l.await(); } catch (InterruptedException e) { throw new RuntimeException(e); } }

    static void runParties(int n, java.util.function.IntConsumer body) throws Exception {
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) { int k = i; Thread t = new Thread(() -> body.accept(k)); ts.add(t); t.start(); }
        for (Thread t : ts) t.join();
    }

    // ---------------------------------------------------------------------
    // 8. visibility
    // ---------------------------------------------------------------------
    static volatile boolean stop = false;

    static void visibility() throws Exception {
        section("8. Memory visibility: volatile");
        AtomicLong spins = new AtomicLong();
        Thread worker = new Thread(() -> { long n = 0; while (!stop) n++; spins.set(n); });
        worker.start();
        Thread.sleep(50);
        stop = true;                                                     // volatile write: guaranteed visible to the reader
        worker.join(2000);
        check("the volatile flag is seen and the loop exits", false, worker.isAlive());
        show("without volatile the JIT may hoist the read out of the loop -> it spins forever (a real, intermittent bug)");
        show("happens-before edges: volatile write->read, unlock->lock, Thread.start, Thread.join, executor submit->run");
    }

    // ---------------------------------------------------------------------
    // 9. deadlock: create, detect, avoid
    // ---------------------------------------------------------------------
    static void deadlock() throws Exception {
        section("9. Deadlock: a real one, detected, then avoided");
        ReentrantLock a = new ReentrantLock(), b = new ReentrantLock();
        CountDownLatch bothHoldOne = new CountDownLatch(2);
        Thread t1 = new Thread(() -> { a.lock(); bothHoldOne.countDown(); await(bothHoldOne); b.lock(); }, "transfer-A-to-B");
        Thread t2 = new Thread(() -> { b.lock(); bothHoldOne.countDown(); await(bothHoldOne); a.lock(); }, "transfer-B-to-A");
        t1.setDaemon(true);
        t2.setDaemon(true);                                              // daemon: they won't keep the JVM alive
        t1.start();
        t2.start();
        Thread.sleep(300);
        long[] ids = ManagementFactory.getThreadMXBean().findDeadlockedThreads();   // what jstack's "Found one Java-level deadlock" uses
        List<String> names = new ArrayList<>();
        if (ids != null) for (ThreadInfo ti : ManagementFactory.getThreadMXBean().getThreadInfo(ids)) names.add(ti.getThreadName());
        Collections.sort(names);
        check("ThreadMXBean finds the cycle: each thread holds one lock and waits for the other", "[transfer-A-to-B, transfer-B-to-A]", names.toString());

        ReentrantLock c = new ReentrantLock(), d = new ReentrantLock();     // FIX 1: global lock ordering (always c then d)
        AtomicInteger done = new AtomicInteger();
        Runnable ordered = () -> { for (int i = 0; i < 10_000; i++) { c.lock(); d.lock(); done.incrementAndGet(); d.unlock(); c.unlock(); } };
        Thread o1 = new Thread(ordered), o2 = new Thread(ordered);
        o1.start(); o2.start(); o1.join(5000); o2.join(5000);
        check("with a global lock order, no circular wait is possible", 20_000, done.get());

        ReentrantLock e = new ReentrantLock(), f = new ReentrantLock();     // FIX 2: tryLock with timeout + back off
        AtomicInteger ok = new AtomicInteger();
        java.util.function.BiFunction<ReentrantLock, ReentrantLock, Runnable> careful = (first, second) -> () -> {
            for (int i = 0; i < 2000; i++) {
                while (true) {
                    try {
                        if (first.tryLock(10, TimeUnit.MILLISECONDS)) {
                            try { if (second.tryLock(10, TimeUnit.MILLISECONDS)) { try { ok.incrementAndGet(); break; } finally { second.unlock(); } } }
                            finally { first.unlock(); }
                        }
                        Thread.sleep(ThreadLocalRandom.current().nextInt(2));       // back off with jitter, avoid livelock
                    } catch (InterruptedException ex) { return; }
                }
            }
        };
        Thread p1 = new Thread(careful.apply(e, f)), p2 = new Thread(careful.apply(f, e));   // OPPOSITE orders, still safe
        p1.start(); p2.start(); p1.join(20000); p2.join(20000);
        check("tryLock + timeout + back-off breaks 'hold and wait' even with opposite orders", 4000, ok.get());
    }

    public static void main(String[] args) throws Exception {
        races();
        cas();
        semaphore();
        readWrite();
        condition();
        barrier();
        visibility();
        deadlock();
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
        System.exit(0);                                                  // the deadlocked demo threads are daemons, but exit explicitly anyway
    }
}
