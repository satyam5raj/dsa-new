/*
 * ============================================================================
 *  TOPIC 4 : EXECUTOR FRAMEWORK - ExecutorService, thread pools,
 *            ThreadPoolExecutor tuning, scheduling, virtual threads
 * ============================================================================
 *
 *  Run:  java ExecutorFramework.java
 *
 *  WHY NOT new Thread() FOR EVERY TASK?
 *    - Creating/destroying OS threads is EXPENSIVE (~1MB stack, a syscall).
 *    - No upper limit: 10,000 requests = 10,000 threads -> OutOfMemoryError
 *      or thrashing from context switches.
 *    - No result handling, no lifecycle management.
 *  A THREAD POOL reuses a fixed set of worker threads that pull tasks from a QUEUE.
 *  The Executor framework separates TASK SUBMISSION (what) from EXECUTION (how/when).
 *
 *  HIERARCHY
 *    Executor                   execute(Runnable)
 *      ExecutorService          submit(), invokeAll(), invokeAny(), shutdown()...
 *        ScheduledExecutorService   schedule(), scheduleAtFixedRate()...
 *    Implementations: ThreadPoolExecutor, ScheduledThreadPoolExecutor, ForkJoinPool
 *    Factory: Executors.newXxx()
 *
 *  Executors FACTORY METHODS
 *  ---------------------------------------------------------------------------------
 *   Method                          core/max        queue                    Danger
 *  ---------------------------------------------------------------------------------
 *   newFixedThreadPool(n)           n / n           UNBOUNDED LinkedBlocking  queue grows -> OOM
 *   newCachedThreadPool()           0 / MAX_INT     SynchronousQueue          unlimited threads -> OOM
 *   newSingleThreadExecutor()       1 / 1           unbounded                 same as fixed
 *   newScheduledThreadPool(n)       n / MAX_INT     DelayedWorkQueue
 *   newWorkStealingPool()           ForkJoinPool (parallelism = #cores)
 *   newVirtualThreadPerTaskExecutor() (21) one virtual thread per task, no pool
 *  ---------------------------------------------------------------------------------
 *  In PRODUCTION, prefer constructing ThreadPoolExecutor yourself, with a
 *  BOUNDED queue, a named ThreadFactory and an explicit rejection policy.
 *
 *  ThreadPoolExecutor - HOW A TASK IS HANDLED (interview favorite)
 *    1. Fewer than corePoolSize threads running?  -> start a NEW thread
 *    2. Otherwise, try to put it on the QUEUE
 *    3. Queue full and threads < maximumPoolSize? -> start a NEW (non-core) thread
 *    4. Queue full and threads == max?            -> REJECT (RejectedExecutionHandler)
 *    Non-core threads idle longer than keepAliveTime are terminated.
 *    NOTE: with an UNBOUNDED queue, step 3 never happens, so maxPoolSize is ignored!
 *
 *  REJECTION POLICIES
 *    AbortPolicy (default) : throw RejectedExecutionException
 *    CallerRunsPolicy      : the submitting thread runs the task itself -> natural BACKPRESSURE
 *    DiscardPolicy         : silently drop the task
 *    DiscardOldestPolicy   : drop the oldest queued task, retry this one
 *
 *  SIZING (rule of thumb)
 *    CPU-bound : threads ~= number of cores (+1)
 *    I/O-bound : threads ~= cores * (1 + waitTime / computeTime)
 *    Or, Java 21+: use virtual threads for blocking I/O and stop sizing pools.
 *
 *  SHUTDOWN (always do it, or non-daemon workers keep the JVM alive)
 *    shutdown()           : no new tasks; already-queued tasks still run
 *    shutdownNow()        : interrupts running tasks; returns the queued ones
 *    awaitTermination(t)  : block until finished or timeout
 *    Java 19+: ExecutorService is AutoCloseable -> try-with-resources calls close()
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorFramework {

    public static void main(String[] args) throws Exception {
        executeVsSubmit();
        invokeAllAndAny();
        customThreadPoolExecutor();
        rejectionPolicies();
        scheduledExecutor();
        shutdownProperly();
        virtualThreads();
    }

    // =================================================================
    // 1. execute() vs submit()
    //    execute(Runnable)        : fire-and-forget. An exception kills the
    //                               worker and goes to the uncaught handler.
    //    submit(Runnable/Callable): returns a Future. An exception is CAPTURED
    //                               and rethrown from future.get(). If you never
    //                               call get(), the exception is SILENTLY LOST!
    // =================================================================
    static void executeVsSubmit() throws Exception {
        System.out.println("===== 1. execute vs submit =====");
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            pool.execute(() -> System.out.println("   execute: ran on " + Thread.currentThread().getName()));

            Future<Integer> f = pool.submit(() -> 6 * 7);                 // Callable
            System.out.println("   submit Callable result = " + f.get());

            Future<?> failing = pool.submit(() -> { throw new IllegalStateException("task failed"); });
            try {
                failing.get();
            } catch (ExecutionException e) {
                System.out.println("   exception surfaced by get(): " + e.getCause().getMessage());
            }
        }   // close() = shutdown + await
    }

    // =================================================================
    // 2. invokeAll (wait for ALL) and invokeAny (first SUCCESSFUL wins)
    // =================================================================
    static void invokeAllAndAny() throws Exception {
        System.out.println("\n===== 2. invokeAll / invokeAny =====");
        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            List<Callable<String>> calls = List.of(
                    () -> { Thread.sleep(300); return "price from service A"; },
                    () -> { Thread.sleep(100); return "price from service B"; },
                    () -> { Thread.sleep(200); return "price from service C"; });

            long start = System.currentTimeMillis();
            List<Future<String>> all = pool.invokeAll(calls);
            for (Future<String> f : all) System.out.println("   invokeAll -> " + f.get());
            System.out.println("   took ~" + (System.currentTimeMillis() - start) + "ms (the slowest one, not the sum)");

            String fastest = pool.invokeAny(calls);         // the others get cancelled
            System.out.println("   invokeAny -> " + fastest);
        }
    }

    // =================================================================
    // 3. A production-style ThreadPoolExecutor
    // =================================================================
    static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.getAndIncrement());   // names show up in thread dumps!
            t.setUncaughtExceptionHandler((th, e) -> System.out.println("   [" + th.getName() + "] crashed: " + e));
            return t;
        };
    }

    static void customThreadPoolExecutor() throws InterruptedException {
        System.out.println("\n===== 3. ThreadPoolExecutor STEP BY STEP =====");
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                                   // corePoolSize
                4,                                   // maximumPoolSize
                30, TimeUnit.SECONDS,                // keepAliveTime for non-core threads
                new ArrayBlockingQueue<>(2),         // BOUNDED queue of 2
                named("order-worker"),
                new ThreadPoolExecutor.AbortPolicy());

        // Submit 7 slow tasks and watch where each one goes
        for (int i = 1; i <= 7; i++) {
            int id = i;
            try {
                pool.execute(() -> sleep(300));
                System.out.printf("   task %d accepted -> threads=%d, queued=%d%n", id, pool.getPoolSize(), pool.getQueue().size());
            } catch (RejectedExecutionException e) {
                System.out.printf("   task %d REJECTED (queue full and max threads reached)%n", id);
            }
        }
        // tasks 1-2: core threads | 3-4: queue | 5-6: extra threads up to max | 7: rejected
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   completed tasks = " + pool.getCompletedTaskCount());
    }

    // =================================================================
    // 4. CallerRunsPolicy: backpressure instead of errors
    // =================================================================
    static void rejectionPolicies() throws InterruptedException {
        System.out.println("\n===== 4. CallerRunsPolicy (backpressure) =====");
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1), named("w"), new ThreadPoolExecutor.CallerRunsPolicy());
        Set<String> ranOn = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 4; i++) {
            pool.execute(() -> { ranOn.add(Thread.currentThread().getName()); sleep(100); });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   tasks ran on: " + ranOn + "  <- 'main' ran overflow tasks, slowing the producer down");
    }

    // =================================================================
    // 5. Scheduling
    //    scheduleAtFixedRate    : start every N ms, regardless of how long a run takes
    //                             (if a run is late, the next starts right after it)
    //    scheduleWithFixedDelay : wait N ms AFTER the previous run ENDS
    //    An exception in a periodic task SILENTLY cancels all future runs. Catch inside!
    // =================================================================
    static void scheduledExecutor() throws Exception {
        System.out.println("\n===== 5. ScheduledExecutorService =====");
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, named("scheduler"));
        long start = System.currentTimeMillis();

        ScheduledFuture<String> delayed = ses.schedule(() -> "ran after ~200ms", 200, TimeUnit.MILLISECONDS);
        AtomicInteger beats = new AtomicInteger();
        ScheduledFuture<?> heartbeat = ses.scheduleAtFixedRate(() ->
                System.out.println("   heartbeat #" + beats.incrementAndGet() + " at " + (System.currentTimeMillis() - start) + "ms"),
                0, 100, TimeUnit.MILLISECONDS);

        System.out.println("   " + delayed.get());
        Thread.sleep(250);
        heartbeat.cancel(false);
        ses.shutdown();
    }

    // =================================================================
    // 6. Graceful shutdown pattern (from the ExecutorService Javadoc)
    // =================================================================
    static void shutdownProperly() {
        System.out.println("\n===== 6. GRACEFUL SHUTDOWN =====");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> sleep(100));
        pool.submit(() -> {
            try { Thread.sleep(60_000); }            // a stuck task
            catch (InterruptedException e) { System.out.println("   long task interrupted by shutdownNow()"); }
        });

        pool.shutdown();                                         // 1. stop accepting
        try {
            if (!pool.awaitTermination(300, TimeUnit.MILLISECONDS)) {   // 2. wait a bit
                List<Runnable> neverRan = pool.shutdownNow();    // 3. force: interrupt workers
                System.out.println("   forced shutdown, " + neverRan.size() + " queued tasks never ran");
                pool.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("   terminated = " + pool.isTerminated());
    }

    // =================================================================
    // 7. Virtual threads (Java 21)
    //    Platform thread = 1:1 with an OS thread (expensive, ~thousands max)
    //    Virtual thread  = managed by the JVM, MOUNTED on a few "carrier"
    //                      platform threads. When it blocks on I/O, sleep or a
    //                      lock, it UNMOUNTS and frees the carrier. Millions are possible.
    //    Great for: thread-per-request servers with blocking I/O.
    //    Not for:   CPU-bound work (still limited by cores).
    //    Pitfalls:  don't POOL virtual threads (create one per task); avoid
    //               long synchronized blocks around blocking I/O ("pinning"
    //               the carrier, improved in Java 24); ThreadLocal per
    //               virtual thread can use lots of memory.
    // =================================================================
    static void virtualThreads() throws Exception {
        System.out.println("\n===== 7. VIRTUAL THREADS =====");
        int tasks = 10_000;
        long start = System.currentTimeMillis();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) exec.submit(() -> sleep(200));   // simulated blocking I/O
        }
        System.out.println("   " + tasks + " blocking tasks (200ms each) on virtual threads: "
                + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        try (ExecutorService exec = Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < 1_000; i++) exec.submit(() -> sleep(200));
        }
        System.out.println("   1000 same tasks on a 100-thread platform pool: "
                + (System.currentTimeMillis() - start) + " ms (10 waves x 200ms)");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
