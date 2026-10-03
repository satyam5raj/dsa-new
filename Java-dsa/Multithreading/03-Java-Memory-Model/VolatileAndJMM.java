/*
 * ============================================================================
 *  TOPIC 3 : JAVA MEMORY MODEL (JMM) - visibility, reordering, happens-before,
 *            volatile, atomics & CAS
 * ============================================================================
 *
 *  Run:  java VolatileAndJMM.java
 *
 *  WHY A MEMORY MODEL?
 *  Modern hardware and compilers cheat to go fast:
 *    1. CPU CACHES: each core has its own L1/L2 cache. A write by core 1 may
 *       sit in its cache/store buffer, invisible to core 2 for a while.
 *    2. REORDERING: the compiler, the JIT and the CPU may execute
 *       instructions out of order, as long as a SINGLE thread can't tell.
 *       Another thread CAN tell.
 *    3. JIT HOISTING: while (!stop) {} may become
 *         if (!stop) while (true) {}    // reads 'stop' once, loops forever
 *  The JMM is the CONTRACT that says when a write by one thread is
 *  GUARANTEED to be visible to a read by another.
 *
 *  THREE PROBLEMS
 *    Atomicity  : is the operation indivisible? (count++ is NOT)
 *    Visibility : will other threads SEE my write?
 *    Ordering   : will other threads see my writes in the order I made them?
 *
 *  HAPPENS-BEFORE (HB): if action A happens-before B, then everything A did
 *  (and everything before A) is visible to B. The main HB rules:
 *    1. Program order    : each action in a thread HB the next action in that thread
 *    2. Monitor lock     : unlock of M HB every later lock of M
 *    3. volatile         : a write to volatile v HB every later read of v
 *    4. Thread start     : t.start() HB every action in t
 *    5. Thread join      : every action in t HB another thread's return from t.join()
 *    6. Transitivity     : A HB B and B HB C  =>  A HB C
 *    + final fields: correctly published once the constructor finishes
 *    + java.util.concurrent: putting into a concurrent collection, submitting
 *      to an executor, Future.get(), latch countDown/await... all create HB edges
 *
 *  volatile
 *    - Every read goes to main memory and every write is flushed (VISIBILITY)
 *    - Prevents reordering around it (ORDERING): writes BEFORE a volatile
 *      write can't move after it, and reads AFTER a volatile read can't move
 *      before it. So a volatile flag can "publish" other data safely.
 *    - Does NOT make compound actions atomic: volatile count; count++ is STILL broken.
 *    USE FOR: status flags (running/stop), safe publication of an immutable
 *             object reference, double-checked locking.
 *
 *  ATOMICS (java.util.concurrent.atomic) - lock-free thread safety via CAS
 *    CAS = Compare-And-Swap, a single CPU instruction (cmpxchg on x86):
 *      "if the value is still EXPECTED, set it to NEW, else fail". Retry on failure.
 *    AtomicInteger, AtomicLong, AtomicBoolean, AtomicReference,
 *    LongAdder (better under HIGH contention: striped counters, summed on read)
 *    ABA problem: the value goes A -> B -> A, and CAS can't tell it changed.
 *    Fix: AtomicStampedReference (adds a version number).
 *
 *  synchronized vs volatile vs Atomic
 *    synchronized : atomicity + visibility, for MANY variables / complex logic (blocking)
 *    volatile     : visibility + ordering only, for ONE variable written by one thread
 *    Atomic*      : atomicity + visibility for ONE variable (non-blocking)
 */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class VolatileAndJMM {

    public static void main(String[] args) throws Exception {
        visibilityProblem();
        volatileFixesVisibility();
        volatileIsNotAtomic();
        safePublication();
        atomicsAndCas();
        longAdderVsAtomic();
        abaProblem();
    }

    // =================================================================
    // 1. Visibility: the reader may NEVER see the write
    // =================================================================
    static boolean stopPlain = false;              // NOT volatile

    static void visibilityProblem() throws InterruptedException {
        System.out.println("===== 1. VISIBILITY PROBLEM =====");
        Thread worker = new Thread(() -> {
            long spins = 0;
            while (!stopPlain) spins++;            // the JIT may hoist the read out of the loop
        });
        worker.setDaemon(true);                    // so a stuck worker doesn't keep the JVM alive
        worker.start();
        Thread.sleep(300);                         // let the JIT compile the hot loop
        stopPlain = true;                          // main writes...
        worker.join(1000);                         // ...wait up to 1s for the worker to notice
        System.out.println("   worker still running after stop=true? " + worker.isAlive()
                + (worker.isAlive() ? "  <- it NEVER saw the write" : "  (got lucky this run; not guaranteed)"));
    }

    // =================================================================
    // 2. volatile fixes it
    // =================================================================
    static volatile boolean stopVolatile = false;

    static void volatileFixesVisibility() throws InterruptedException {
        System.out.println("\n===== 2. volatile FIXES VISIBILITY =====");
        Thread worker = new Thread(() -> {
            long spins = 0;
            while (!stopVolatile) spins++;         // re-read from memory every iteration
            System.out.println("   worker saw stop=true and exited");
        });
        worker.start();
        Thread.sleep(300);
        stopVolatile = true;
        worker.join(1000);
        System.out.println("   worker still running? " + worker.isAlive());
    }

    // =================================================================
    // 3. volatile does NOT make ++ atomic
    // =================================================================
    static volatile int volatileCount = 0;

    static void volatileIsNotAtomic() throws InterruptedException {
        System.out.println("\n===== 3. volatile IS NOT ATOMIC =====");
        Runnable r = () -> { for (int i = 0; i < 100_000; i++) volatileCount++; };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("   expected 200000, got " + volatileCount + " (read-modify-write still races)");
    }

    // =================================================================
    // 4. Safe publication with a volatile reference
    //    Writer: fill the object, THEN assign it to the volatile field.
    //    Reader: read the volatile field. If non-null, it sees a FULLY built object
    //    (the volatile write/read pair creates happens-before for all earlier writes).
    //    Without volatile, a reader could see the reference but stale fields.
    // =================================================================
    static class Config {
        int timeout;           // plain fields
        String url;
    }

    static volatile Config published;              // the "publication point"

    static void safePublication() throws InterruptedException {
        System.out.println("\n===== 4. SAFE PUBLICATION =====");
        Thread reader = new Thread(() -> {
            Config c;
            while ((c = published) == null) Thread.onSpinWait();   // spin until published
            System.out.println("   reader sees timeout=" + c.timeout + ", url=" + c.url + " (never 0/null)");
        });
        reader.start();
        Config c = new Config();
        c.timeout = 30;                            // these writes...
        c.url = "https://api";
        published = c;                             // ...happen-before this volatile write
        reader.join();
        // Other safe ways: final fields + no 'this' escape, static initializers,
        // putting into a ConcurrentHashMap, passing through a BlockingQueue.
    }

    // =================================================================
    // 5. Atomics & CAS
    // =================================================================
    static void atomicsAndCas() throws InterruptedException {
        System.out.println("\n===== 5. ATOMICS & CAS =====");
        AtomicInteger count = new AtomicInteger();
        Runnable r = () -> { for (int i = 0; i < 100_000; i++) count.incrementAndGet(); };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("   AtomicInteger total = " + count.get());

        // The CAS loop that incrementAndGet uses internally:
        AtomicInteger max = new AtomicInteger(10);
        int candidate = 42;
        while (true) {
            int current = max.get();                         // 1. read
            if (candidate <= current) break;                 // nothing to do
            if (max.compareAndSet(current, candidate)) break; // 2. swap only if unchanged, else retry
        }
        System.out.println("   CAS-based max = " + max.get());

        // Helpers that hide the loop
        max.updateAndGet(v -> v * 2);
        max.accumulateAndGet(100, Math::max);
        System.out.println("   updateAndGet/accumulateAndGet -> " + max.get());

        AtomicReference<String> leader = new AtomicReference<>("node-1");
        boolean won = leader.compareAndSet("node-1", "node-2");     // e.g. a leader-election flip
        System.out.println("   AtomicReference CAS won=" + won + ", leader=" + leader.get());
    }

    // =================================================================
    // 6. LongAdder: much faster than AtomicLong under heavy contention
    //    AtomicLong: every thread CASes ONE memory location -> constant retries.
    //    LongAdder : each thread mostly updates its own cell; sum() adds them up.
    //    Use LongAdder for statistics/metrics counters; AtomicLong when you
    //    need an exact value on every update (e.g. sequence ids).
    // =================================================================
    static void longAdderVsAtomic() throws Exception {
        System.out.println("\n===== 6. LongAdder vs AtomicLong =====");
        int threads = 8, perThread = 2_000_000;
        AtomicLong atomic = new AtomicLong();
        LongAdder adder = new LongAdder();
        long t1 = timeIt(threads, () -> { for (int i = 0; i < perThread; i++) atomic.incrementAndGet(); });
        long t2 = timeIt(threads, () -> { for (int i = 0; i < perThread; i++) adder.increment(); });
        System.out.println("   AtomicLong " + atomic.get() + " in " + t1 + " ms");
        System.out.println("   LongAdder  " + adder.sum() + " in " + t2 + " ms");
    }

    // =================================================================
    // 7. ABA problem and AtomicStampedReference
    // =================================================================
    static void abaProblem() {
        System.out.println("\n===== 7. ABA PROBLEM =====");
        AtomicReference<String> ref = new AtomicReference<>("A");
        String seen = ref.get();                     // thread 1 reads A
        ref.set("B");                                // thread 2: A -> B
        ref.set("A");                                // thread 2: B -> A
        System.out.println("   plain CAS succeeds despite changes: " + ref.compareAndSet(seen, "C"));

        AtomicStampedReference<String> stamped = new AtomicStampedReference<>("A", 0);
        int stamp = stamped.getStamp();              // thread 1 reads (A, v0)
        stamped.compareAndSet("A", "B", 0, 1);       // thread 2: (B, v1)
        stamped.compareAndSet("B", "A", 1, 2);       // thread 2: (A, v2)
        System.out.println("   stamped CAS detects it:             " + stamped.compareAndSet("A", "C", stamp, stamp + 1));
    }

    static long timeIt(int threads, Runnable r) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) pool.submit(r);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        return System.currentTimeMillis() - start;
    }
}
