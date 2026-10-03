/*
 * ============================================================================
 *  TOPIC 8 : SYNCHRONIZERS - CountDownLatch, CyclicBarrier, Semaphore,
 *            Phaser, Exchanger
 * ============================================================================
 *
 *  Run:  java Synchronizers.java
 *
 *  Higher-level tools to COORDINATE threads (instead of raw wait/notify).
 *
 *  -------------------------------------------------------------------------------
 *   Tool            Idea                                   Reusable?  Typical use
 *  -------------------------------------------------------------------------------
 *   CountDownLatch  N events must happen; waiters block    NO         wait for services
 *                   until the count reaches 0                         to start; a "start gun"
 *   CyclicBarrier   N threads wait for EACH OTHER at a     YES        parallel simulation
 *                   point, then all continue together                 in rounds/phases
 *   Semaphore       N permits; acquire() blocks when 0     YES        limit concurrency:
 *                                                                     connection pools, rate limiting
 *   Phaser          flexible barrier; parties can          YES        multi-phase work with
 *                   register/deregister dynamically                   a changing number of threads
 *   Exchanger       two threads swap objects at a          YES        double buffering
 *                   meeting point
 *  -------------------------------------------------------------------------------
 *
 *  CountDownLatch vs CyclicBarrier (classic interview question)
 *    Latch  : some threads WAIT for OTHER threads to count down. One-shot.
 *             The counting threads don't block.
 *    Barrier: the SAME threads wait for EACH OTHER. Resets after each trip.
 *             Optional barrier action runs once per trip. If one thread
 *             fails or times out, the barrier BREAKS (BrokenBarrierException).
 *
 *  Semaphore vs Lock
 *    A lock has 1 owner and must be released by the owner. A semaphore has N
 *    permits and ANY thread can release (no ownership). A binary semaphore
 *    (1 permit) is like a non-reentrant mutex.
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Synchronizers {

    public static void main(String[] args) throws Exception {
        countDownLatchStartup();
        countDownLatchStartingGun();
        cyclicBarrier();
        semaphore();
        phaser();
        exchanger();
    }

    // =================================================================
    // 1. CountDownLatch: main waits for 3 services to be ready
    // =================================================================
    static void countDownLatchStartup() throws InterruptedException {
        System.out.println("===== 1. CountDownLatch: WAIT FOR STARTUP =====");
        List<String> services = List.of("database", "cache", "message-queue");
        CountDownLatch ready = new CountDownLatch(services.size());
        ExecutorService pool = Executors.newFixedThreadPool(3);

        for (String s : services) {
            pool.submit(() -> {
                sleep(ThreadLocalRandom.current().nextInt(50, 300));     // init time
                System.out.println("   " + s + " ready");
                ready.countDown();                                       // count - 1 (doesn't block)
            });
        }
        boolean allUp = ready.await(2, TimeUnit.SECONDS);               // blocks until count == 0
        System.out.println("   all services up? " + allUp + " -> accepting traffic");
        pool.shutdown();
    }

    // =================================================================
    // 2. CountDownLatch(1) as a "starting gun": release all threads at once
    //    (great for load tests and for reproducing race conditions)
    // =================================================================
    static void countDownLatchStartingGun() throws InterruptedException {
        System.out.println("\n===== 2. CountDownLatch: STARTING GUN =====");
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(4);
        long[] startTimes = new long[4];
        for (int i = 0; i < 4; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    startGun.await();                                    // everyone waits here
                    startTimes[id] = System.nanoTime();
                } catch (InterruptedException ignored) {
                } finally {
                    finished.countDown();
                }
            }).start();
        }
        sleep(100);                                                      // all threads are now parked
        startGun.countDown();                                            // GO!
        finished.await();
        long spread = (Arrays.stream(startTimes).max().getAsLong() - Arrays.stream(startTimes).min().getAsLong()) / 1000;
        System.out.println("   4 runners started within " + spread + " us of each other");
    }

    // =================================================================
    // 3. CyclicBarrier: workers process a matrix in rounds; no one starts
    //    round N+1 until everyone has finished round N
    // =================================================================
    static void cyclicBarrier() throws InterruptedException {
        System.out.println("\n===== 3. CyclicBarrier: PHASED COMPUTATION =====");
        int workers = 3, rounds = 3;
        AtomicInteger round = new AtomicInteger(1);
        // The barrier action runs ONCE per trip, by the last thread to arrive
        CyclicBarrier barrier = new CyclicBarrier(workers, () ->
                System.out.println("   --- all " + workers + " workers finished round " + round.getAndIncrement() + ", merging results ---"));

        List<Thread> ts = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            int id = w;
            Thread t = new Thread(() -> {
                try {
                    for (int r = 1; r <= rounds; r++) {
                        sleep(ThreadLocalRandom.current().nextInt(20, 80));
                        System.out.println("   worker " + id + " done with round " + r);
                        barrier.await();                                 // wait for the others; the barrier then RESETS
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    System.out.println("   barrier broken: " + e);
                }
            });
            ts.add(t);
            t.start();
        }
        for (Thread t : ts) t.join();
    }

    // =================================================================
    // 4. Semaphore: at most 3 concurrent calls to a rate-limited API
    // =================================================================
    static void semaphore() throws InterruptedException {
        System.out.println("\n===== 4. Semaphore: LIMIT CONCURRENCY TO 3 =====");
        Semaphore permits = new Semaphore(3);          // new Semaphore(3, true) for FIFO fairness
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxSeen = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            pool.submit(() -> {
                try {
                    permits.acquire();                 // blocks if 3 are already in use
                    try {
                        int now = inFlight.incrementAndGet();
                        maxSeen.accumulateAndGet(now, Math::max);
                        sleep(100);                    // the external API call
                        inFlight.decrementAndGet();
                    } finally {
                        permits.release();             // ALWAYS release in finally
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   10 tasks, max concurrently inside = " + maxSeen.get());

        // tryAcquire: fail fast instead of waiting (load shedding)
        Semaphore one = new Semaphore(1);
        one.acquire();
        System.out.println("   tryAcquire when exhausted = " + one.tryAcquire() + " -> respond 429 Too Many Requests");
    }

    // =================================================================
    // 5. Phaser: a reusable barrier where parties can join or leave
    // =================================================================
    static void phaser() throws InterruptedException {
        System.out.println("\n===== 5. Phaser =====");
        Phaser phaser = new Phaser(1);                 // register main as a party
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int id = i;
            phaser.register();                         // a new party joins dynamically
            Thread t = new Thread(() -> {
                System.out.println("   task " + id + " phase " + phaser.getPhase() + ": download");
                phaser.arriveAndAwaitAdvance();        // wait for everyone
                System.out.println("   task " + id + " phase " + phaser.getPhase() + ": process");
                if (id == 2) {
                    phaser.arriveAndDeregister();      // task 2 leaves early
                    return;
                }
                phaser.arriveAndAwaitAdvance();
                System.out.println("   task " + id + " phase " + phaser.getPhase() + ": upload");
                phaser.arriveAndDeregister();
            });
            ts.add(t);
            t.start();
        }
        phaser.arriveAndAwaitAdvance();                // main: end of phase 0
        phaser.arriveAndAwaitAdvance();                // main: end of phase 1
        phaser.arriveAndDeregister();
        for (Thread t : ts) t.join();
        System.out.println("   phaser terminated = " + phaser.isTerminated());
    }

    // =================================================================
    // 6. Exchanger: a producer fills buffer A while the consumer drains
    //    buffer B, then they SWAP (double buffering)
    // =================================================================
    static void exchanger() throws InterruptedException {
        System.out.println("\n===== 6. Exchanger: DOUBLE BUFFERING =====");
        Exchanger<List<Integer>> exchanger = new Exchanger<>();
        Thread producer = new Thread(() -> {
            List<Integer> buffer = new ArrayList<>();
            try {
                for (int batch = 0; batch < 2; batch++) {
                    for (int i = 0; i < 3; i++) buffer.add(batch * 10 + i);
                    buffer = exchanger.exchange(buffer);   // hand over the full buffer, get an empty one back
                }
            } catch (InterruptedException ignored) {}
        });
        Thread consumer = new Thread(() -> {
            List<Integer> buffer = new ArrayList<>();
            try {
                for (int batch = 0; batch < 2; batch++) {
                    buffer = exchanger.exchange(buffer);   // give the empty one, get a full one
                    System.out.println("   consumer got batch " + buffer);
                    buffer.clear();
                }
            } catch (InterruptedException ignored) {}
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
