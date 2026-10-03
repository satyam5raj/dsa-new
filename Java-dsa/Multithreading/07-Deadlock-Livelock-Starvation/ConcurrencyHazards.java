/*
 * ============================================================================
 *  TOPIC 7 : DEADLOCK, LIVELOCK, STARVATION (and race conditions recap)
 *            causes, detection, prevention
 * ============================================================================
 *
 *  Run:  java ConcurrencyHazards.java
 *  (Deadlocked demo threads are daemons, so the program still exits.)
 *
 *  RACE CONDITION : the result depends on thread timing (see 02-Synchronization).
 *                   Fix: synchronize, use atomics, or avoid shared mutable state.
 *
 *  DEADLOCK : two or more threads each wait FOREVER for a lock the other holds.
 *      Thread 1: holds A, wants B
 *      Thread 2: holds B, wants A          -> both stuck, CPU usage 0%
 *
 *    The 4 COFFMAN CONDITIONS (ALL must hold for a deadlock to occur):
 *      1. Mutual exclusion : a resource can be held by only one thread
 *      2. Hold and wait    : a thread holds one resource while waiting for another
 *      3. No preemption    : a resource can't be forcibly taken away
 *      4. Circular wait    : T1 -> T2 -> ... -> T1 wait cycle
 *    PREVENTION = break at least ONE condition:
 *      - LOCK ORDERING (breaks circular wait): always acquire locks in a global
 *        order (e.g. by account id). THE most common fix.
 *      - tryLock with timeout (breaks no-preemption/hold-and-wait): back off and retry
 *      - Acquire all locks at once / use one coarser lock
 *      - Avoid nested locks; don't call unknown ("alien") code while holding a lock
 *    DETECTION: thread dump (jstack <pid>, or kill -3) prints "Found one
 *      Java-level deadlock". Programmatically: ThreadMXBean.findDeadlockedThreads().
 *
 *  LIVELOCK : threads are NOT blocked. They keep REACTING to each other and
 *      changing state, but no one makes progress. (Two people in a hallway
 *      both stepping aside the same way, forever.) CPU usage is high.
 *      Fix: add RANDOMNESS (random back-off) or a priority/tie-breaker.
 *
 *  STARVATION : a thread never gets the CPU or a lock because others keep
 *      getting it first (greedy threads, unfair locks, priority abuse, a long
 *      synchronized section). Fix: fair locks (new ReentrantLock(true)),
 *      shorter critical sections, bounded queues.
 */

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ConcurrencyHazards {

    public static void main(String[] args) throws Exception {
        deadlockAndDetection();
        fixWithLockOrdering();
        fixWithTryLock();
        livelock();
        starvationAndFairness();
    }

    static class Account {
        final int id;
        int balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }

    // =================================================================
    // 1. Create a real deadlock, then DETECT it
    // =================================================================
    static void transferDeadlockProne(Account from, Account to, int amount) {
        synchronized (from) {                          // lock 1
            sleep(50);                                 // make the bad interleaving very likely
            synchronized (to) {                        // lock 2 -> opposite order in the other thread
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    static void deadlockAndDetection() throws InterruptedException {
        System.out.println("===== 1. DEADLOCK + DETECTION =====");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> transferDeadlockProne(a, b, 100), "transfer-A-to-B");
        Thread t2 = new Thread(() -> transferDeadlockProne(b, a, 100), "transfer-B-to-A");
        t1.setDaemon(true);                            // so the JVM can exit despite the deadlock
        t2.setDaemon(true);
        t1.start(); t2.start();
        Thread.sleep(300);

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids != null) {
            for (ThreadInfo info : mx.getThreadInfo(ids)) {
                System.out.println("   DEADLOCKED: " + info.getThreadName() + " (" + info.getThreadState()
                        + ") waiting for lock held by " + info.getLockOwnerName());
            }
        } else {
            System.out.println("   no deadlock this run (timing-dependent)");
        }
    }

    // =================================================================
    // 2. FIX 1: global lock ordering (always lock the lower id first)
    // =================================================================
    static void transferOrdered(Account from, Account to, int amount) {
        Account first = from.id < to.id ? from : to;   // same order for every thread
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            sleep(10);
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    static void fixWithLockOrdering() throws InterruptedException {
        System.out.println("\n===== 2. FIX: LOCK ORDERING =====");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> { for (int i = 0; i < 20; i++) transferOrdered(a, b, 10); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 20; i++) transferOrdered(b, a, 5); });
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("   finished; a=" + a.balance + " b=" + b.balance + " total=" + (a.balance + b.balance) + " (conserved)");
    }

    // =================================================================
    // 3. FIX 2: tryLock with timeout + random back-off
    // =================================================================
    static boolean transferWithTryLock(Account from, Account to, int amount) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            from.balance -= amount;
                            to.balance += amount;
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();                // release what we hold -> breaks hold-and-wait
                }
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));   // random back-off avoids livelock
        }
        return false;                                  // gave up: report it instead of hanging
    }

    static void fixWithTryLock() throws InterruptedException {
        System.out.println("\n===== 3. FIX: tryLock + BACK-OFF =====");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        int[] ok = new int[2];
        Thread t1 = new Thread(() -> { for (int i = 0; i < 50; i++) try { if (transferWithTryLock(a, b, 10)) ok[0]++; } catch (InterruptedException ignored) {} });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 50; i++) try { if (transferWithTryLock(b, a, 10)) ok[1]++; } catch (InterruptedException ignored) {} });
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("   successful transfers: " + ok[0] + " + " + ok[1] + ", total money = " + (a.balance + b.balance));
    }

    // =================================================================
    // 4. LIVELOCK: two polite diners keep handing the spoon to each other
    //    (bounded rounds so the demo ends)
    // =================================================================
    static class Spoon {
        volatile Diner owner;
        Spoon(Diner d) { owner = d; }
    }

    static class Diner {
        final String name;
        volatile boolean hungry = true;
        Diner(String name) { this.name = name; }

        int eatWith(Spoon spoon, Diner partner, boolean randomBackoff, int maxRounds) {
            int rounds = 0;                             // counts only turns where we HOLD the spoon
            long deadline = System.currentTimeMillis() + 1_000;   // safety net so the demo always ends
            while (hungry && rounds < maxRounds && System.currentTimeMillis() < deadline) {
                if (spoon.owner != this) { Thread.onSpinWait(); continue; }   // waiting doesn't count
                rounds++;
                // Politeness: if the partner is hungry, give them the spoon...
                if (partner.hungry && !(randomBackoff && ThreadLocalRandom.current().nextBoolean())) {
                    spoon.owner = partner;              // ...and they do the same back -> livelock
                    sleep(1);
                    continue;
                }
                hungry = false;                         // finally eat
                spoon.owner = partner;
            }
            return rounds;
        }
    }

    static void livelock() throws InterruptedException {
        System.out.println("\n===== 4. LIVELOCK =====");
        for (boolean random : new boolean[]{false, true}) {
            Diner husband = new Diner("husband"), wife = new Diner("wife");
            Spoon spoon = new Spoon(husband);
            int[] rounds = new int[2];
            Thread h = new Thread(() -> rounds[0] = husband.eatWith(spoon, wife, random, 300));
            Thread w = new Thread(() -> rounds[1] = wife.eatWith(spoon, husband, random, 300));
            h.start(); w.start(); h.join(); w.join();
            System.out.printf("   %-15s husband ate=%-5s wife ate=%-5s (rounds %d/%d)%n",
                    random ? "random back-off" : "always polite", !husband.hungry, !wife.hungry, rounds[0], rounds[1]);
        }
        System.out.println("   always polite: both hit the round limit without eating -> LIVELOCK");
    }

    // =================================================================
    // 5. STARVATION and fair locks
    //    A greedy thread re-acquires an UNFAIR lock right after releasing it,
    //    before the waiting threads wake up. A FAIR lock hands it over in FIFO order.
    // =================================================================
    static void starvationAndFairness() throws InterruptedException {
        System.out.println("\n===== 5. STARVATION & FAIR LOCKS =====");
        for (boolean fair : new boolean[]{false, true}) {
            ReentrantLock lock = new ReentrantLock(fair);
            Map<String, Integer> acquired = new ConcurrentHashMap<>();
            long end = System.currentTimeMillis() + 300;
            Runnable worker = () -> {
                while (System.currentTimeMillis() < end) {
                    lock.lock();
                    try {
                        acquired.merge(Thread.currentThread().getName(), 1, Integer::sum);
                    } finally {
                        lock.unlock();
                    }
                }
            };
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < 4; i++) ts.add(new Thread(worker, "T" + i));
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join();
            int min = Collections.min(acquired.values()), max = Collections.max(acquired.values());
            System.out.printf("   %-6s lock: acquisitions per thread %s (max/min ratio %.1f)%n",
                    fair ? "fair" : "unfair", new TreeMap<>(acquired), (double) max / Math.max(1, min));
        }
        System.out.println("   fair = more even but slower (fewer total acquisitions); unfair = faster but can starve");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
