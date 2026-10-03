/*
 * ============================================================================
 *  TOPIC 1 : THREAD BASICS - lifecycle, creation, join, sleep, interrupt, daemon
 * ============================================================================
 *
 *  Run:  java ThreadBasics.java
 *
 *  PROCESS vs THREAD
 *    Process : a running program with its OWN memory space (heap, code).
 *              Processes are isolated from each other.
 *    Thread  : a path of execution INSIDE a process. Threads of one process
 *              SHARE the heap (objects, static fields), but each thread has
 *              its OWN stack (local variables, call frames) and program counter.
 *    Sharing the heap makes communication cheap, and it is also the source
 *    of every concurrency bug.
 *
 *  CONCURRENCY vs PARALLELISM
 *    Concurrency : dealing with many tasks at once (interleaving; possible on 1 core)
 *    Parallelism : doing many tasks at the SAME instant (needs multiple cores)
 *
 *  THREAD LIFECYCLE (Thread.State enum)
 *
 *        NEW --start()--> RUNNABLE <-------------------------------+
 *                          |   ^  (RUNNABLE = ready OR running;     |
 *                          |   |   the OS scheduler decides)        |
 *          synchronized    |   |  lock acquired                     |
 *          lock is busy    v   |                                    |
 *                        BLOCKED                                    |
 *                                                                   |
 *          wait(), join(), LockSupport.park()  -> WAITING ----------+ notify / unpark / joined thread ends
 *          sleep(ms), wait(ms), join(ms)       -> TIMED_WAITING ----+ timeout / notify
 *
 *          run() finishes or throws  -> TERMINATED (can't be restarted)
 *
 *  WAYS TO CREATE A THREAD / TASK
 *    1. extends Thread, override run()             - couples the task to the thread (avoid)
 *    2. implements Runnable  (void run())          - task separate from thread (preferred)
 *    3. implements Callable<V> (V call() throws)   - RETURNS a value, can throw checked
 *                                                    exceptions. Used with ExecutorService -> Future
 *    4. Lambda: new Thread(() -> ...)              - Runnable is a functional interface
 *    5. Thread.ofVirtual() / ofPlatform() (Java 21) - builder API, virtual threads
 *  In real code you rarely create threads by hand: use an ExecutorService.
 *
 *  start() vs run()  (classic interview question)
 *    start() : creates a NEW thread, which then calls run()
 *    run()   : just a normal method call on the CURRENT thread. No new thread!
 */

import java.util.concurrent.*;

public class ThreadBasics {

    public static void main(String[] args) throws Exception {
        creationWays();
        startVsRun();
        lifecycleStates();
        joinDemo();
        interruptDemo();
        daemonThreads();
        threadPropertiesAndExceptions();
    }

    // =================================================================
    // 1. Creating threads
    // =================================================================
    // Way 1: extend Thread
    static class MyThread extends Thread {
        MyThread(String name) { super(name); }
        @Override
        public void run() { System.out.println("   [1 extends Thread] running in " + getName()); }
    }

    // Way 2: implement Runnable (task logic only)
    static class PrintTask implements Runnable {
        @Override
        public void run() { System.out.println("   [2 Runnable]       running in " + Thread.currentThread().getName()); }
    }

    // Way 3: implement Callable (returns a result)
    static class SumTask implements Callable<Integer> {
        private final int n;
        SumTask(int n) { this.n = n; }
        @Override
        public Integer call() {
            int sum = 0;
            for (int i = 1; i <= n; i++) sum += i;
            return sum;
        }
    }

    static void creationWays() throws Exception {
        System.out.println("===== 1. CREATING THREADS =====");
        Thread t1 = new MyThread("worker-A");
        Thread t2 = new Thread(new PrintTask(), "worker-B");
        Thread t3 = new Thread(() -> System.out.println("   [4 lambda]         running in "
                + Thread.currentThread().getName()), "worker-C");
        t1.start(); t1.join();     // join() just to keep the output in order
        t2.start(); t2.join();
        t3.start(); t3.join();

        // Callable needs something to run it and hand back the result: a FutureTask or an executor
        FutureTask<Integer> future = new FutureTask<>(new SumTask(100));
        new Thread(future, "worker-D").start();
        System.out.println("   [3 Callable]       sum 1..100 = " + future.get());   // get() blocks until done

        // Java 21 builder API
        Thread platform = Thread.ofPlatform().name("platform-1").start(() ->
                System.out.println("   [5 ofPlatform]     " + Thread.currentThread()));
        platform.join();
        Thread virtual = Thread.ofVirtual().name("virtual-1").start(() ->
                System.out.println("   [5 ofVirtual]      " + Thread.currentThread()));
        virtual.join();
    }

    // =================================================================
    // 2. start() vs run()
    // =================================================================
    static void startVsRun() throws InterruptedException {
        System.out.println("\n===== 2. start() vs run() =====");
        Runnable task = () -> System.out.println("   executing on: " + Thread.currentThread().getName());
        Thread t = new Thread(task, "new-thread");
        t.run();     // WRONG: runs on main, like any method call
        t.start();   // RIGHT: runs on "new-thread"
        t.join();
        try {
            t.start();   // a thread can be started only ONCE
        } catch (IllegalThreadStateException e) {
            System.out.println("   start() twice -> IllegalThreadStateException");
        }
    }

    // =================================================================
    // 3. Observing every lifecycle state
    // =================================================================
    static void lifecycleStates() throws Exception {
        System.out.println("\n===== 3. LIFECYCLE STATES =====");
        Object lock = new Object();

        Thread sleeper = new Thread(() -> sleepQuietly(300));
        System.out.println("   after new        : " + sleeper.getState());          // NEW
        sleeper.start();
        Thread.sleep(50);
        System.out.println("   while sleeping   : " + sleeper.getState());          // TIMED_WAITING

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try { lock.wait(); } catch (InterruptedException ignored) {}       // WAITING until notify
            }
        });
        waiter.start();
        Thread.sleep(50);
        System.out.println("   while wait()-ing : " + waiter.getState());          // WAITING

        Thread blocked;
        synchronized (lock) {                                   // main holds the lock...
            blocked = new Thread(() -> { synchronized (lock) { } });
            blocked.start();
            Thread.sleep(50);
            System.out.println("   waiting for lock : " + blocked.getState());     // BLOCKED
            lock.notifyAll();                                  // wake the waiter
        }
        Thread busy = new Thread(() -> { long x = 0; for (long i = 0; i < 300_000_000L; i++) x += i; });
        busy.start();
        System.out.println("   computing        : " + busy.getState());            // RUNNABLE
        sleeper.join(); waiter.join(); blocked.join(); busy.join();
        System.out.println("   after finishing  : " + sleeper.getState());         // TERMINATED
    }

    // =================================================================
    // 4. join(): wait for another thread to finish
    // =================================================================
    static void joinDemo() throws InterruptedException {
        System.out.println("\n===== 4. join() =====");
        int[] partial = new int[2];
        Thread left = new Thread(() -> { for (int i = 1; i <= 500; i++) partial[0] += i; });
        Thread right = new Thread(() -> { for (int i = 501; i <= 1000; i++) partial[1] += i; });
        left.start();
        right.start();
        left.join();      // main WAITS here until 'left' finishes
        right.join();     // join also guarantees VISIBILITY: main sees partial[] writes (happens-before)
        System.out.println("   sum 1..1000 split across 2 threads = " + (partial[0] + partial[1]));
    }

    // =================================================================
    // 5. Interruption: the COOPERATIVE way to stop a thread
    //    Thread.stop() is deprecated/removed (it killed threads mid-update,
    //    leaving objects corrupted). Instead:
    //      t.interrupt() sets a flag. The target thread must CHECK it:
    //        - blocking calls (sleep, wait, join, BlockingQueue.take) throw
    //          InterruptedException and CLEAR the flag
    //        - loops check Thread.currentThread().isInterrupted()
    //    RULE: never swallow InterruptedException. Either rethrow it, or
    //    restore the flag with Thread.currentThread().interrupt().
    // =================================================================
    static void interruptDemo() throws InterruptedException {
        System.out.println("\n===== 5. INTERRUPTION =====");
        Thread downloader = new Thread(() -> {
            int chunk = 0;
            while (!Thread.currentThread().isInterrupted()) {           // check the flag
                chunk++;
                try {
                    Thread.sleep(40);                                    // blocking call
                } catch (InterruptedException e) {
                    System.out.println("   interrupted during sleep after chunk " + chunk + ", cleaning up");
                    Thread.currentThread().interrupt();                  // restore the flag so the loop exits
                }
            }
            System.out.println("   downloader exited cleanly");
        });
        downloader.start();
        Thread.sleep(150);
        downloader.interrupt();                                          // politely ask it to stop
        downloader.join();
    }

    // =================================================================
    // 6. Daemon threads
    //    The JVM exits when only DAEMON threads are left. Use them for
    //    background helpers (GC, cache cleanup, heartbeats). Never for work
    //    that must complete (writing files), because they're killed abruptly
    //    and finally blocks may not run.
    // =================================================================
    static void daemonThreads() {
        System.out.println("\n===== 6. DAEMON THREADS =====");
        Thread heartbeat = new Thread(() -> {
            while (true) sleepQuietly(1_000);            // infinite loop, but it won't stop JVM exit
        }, "heartbeat");
        heartbeat.setDaemon(true);                       // must be set BEFORE start()
        heartbeat.start();
        System.out.println("   heartbeat daemon? " + heartbeat.isDaemon() + " (JVM can still exit)");
    }

    // =================================================================
    // 7. Name, priority, uncaught exceptions
    //    Priority (1-10) is only a HINT to the OS scheduler. Don't rely on it.
    //    An exception in a thread kills ONLY that thread. Main doesn't see
    //    it unless you install an UncaughtExceptionHandler (or use Futures).
    // =================================================================
    static void threadPropertiesAndExceptions() throws InterruptedException {
        System.out.println("\n===== 7. PROPERTIES & UNCAUGHT EXCEPTIONS =====");
        Thread main = Thread.currentThread();
        System.out.println("   main thread: name=" + main.getName() + " priority=" + main.getPriority()
                + " cores=" + Runtime.getRuntime().availableProcessors());

        Thread failing = new Thread(() -> { throw new IllegalStateException("boom in worker"); }, "failing-worker");
        failing.setUncaughtExceptionHandler((t, e) ->
                System.out.println("   handler caught '" + e.getMessage() + "' from " + t.getName()));
        failing.start();
        failing.join();
        System.out.println("   main is still alive");
    }

    static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
