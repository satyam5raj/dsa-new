/*
 * ============================================================================
 *  TOPIC 2.2 : EXPLICIT LOCKS - ReentrantLock, Condition, ReadWriteLock, StampedLock
 * ============================================================================
 *
 *  Run:  java ExplicitLocks.java
 *
 *  java.util.concurrent.locks gives MORE CONTROL than synchronized:
 *
 *  ---------------------------------------------------------------------------
 *   Feature                    synchronized        ReentrantLock
 *  ---------------------------------------------------------------------------
 *   Try without blocking       no                  tryLock()
 *   Timeout                    no                  tryLock(1, SECONDS)
 *   Interruptible wait         no                  lockInterruptibly()
 *   Fairness (FIFO)            no                  new ReentrantLock(true)
 *   Multiple wait-sets         one per object      many Conditions per lock
 *   Lock in one method,        no (block-scoped)   yes
 *     unlock in another
 *   Auto-release               YES                 NO -> ALWAYS unlock in finally!
 *   Diagnostics                little              isLocked, getQueueLength, isHeldByCurrentThread
 *  ---------------------------------------------------------------------------
 *  RULE: use synchronized for simple cases (less error-prone). Use
 *  ReentrantLock when you need one of its extra features.
 *
 *  THE IDIOM (memorize):
 *      lock.lock();
 *      try {
 *          // critical section
 *      } finally {
 *          lock.unlock();
 *      }
 *
 *  ReadWriteLock (ReentrantReadWriteLock)
 *    - MANY readers at once OR ONE writer (exclusive).
 *    - Great when reads vastly outnumber writes (caches, configs).
 *    - A read lock can't be upgraded to a write lock (that deadlocks).
 *      Downgrading (write -> read) is allowed.
 *
 *  StampedLock (Java 8)
 *    - Adds an OPTIMISTIC READ: read without locking, then validate(stamp).
 *      If a write happened in between, retry with a real read lock.
 *    - Very fast for read-heavy workloads. NOT reentrant, no Conditions.
 *
 *  FAIRNESS: a fair lock hands the lock to the longest-waiting thread
 *  (no starvation), but throughput is lower. Unfair (the default) lets a
 *  newly arriving thread "barge" in, which is faster.
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ExplicitLocks {

    public static void main(String[] args) throws Exception {
        basicReentrantLock();
        tryLockWithTimeout();
        interruptibleLock();
        conditions();
        readWriteLock();
        stampedLock();
    }

    // =================================================================
    // 1. Basic lock/unlock with try/finally
    // =================================================================
    static class BankAccount {
        private final Lock lock = new ReentrantLock();
        private double balance = 1000;

        void withdraw(double amount) {
            lock.lock();
            try {
                if (balance >= amount) {
                    sleep(1);                       // simulate work; another thread would interleave here without the lock
                    balance -= amount;
                }
            } finally {
                lock.unlock();                      // ALWAYS in finally
            }
        }

        double balance() {
            lock.lock();
            try { return balance; } finally { lock.unlock(); }
        }
    }

    static void basicReentrantLock() throws InterruptedException {
        System.out.println("===== 1. ReentrantLock =====");
        BankAccount acc = new BankAccount();
        Thread[] ts = new Thread[10];
        for (int i = 0; i < ts.length; i++) (ts[i] = new Thread(() -> acc.withdraw(200))).start();
        for (Thread t : ts) t.join();
        System.out.println("   10 threads try to withdraw 200 from 1000 -> balance = " + acc.balance() + " (never negative)");
    }

    // =================================================================
    // 2. tryLock: don't wait forever (useful to AVOID DEADLOCKS)
    // =================================================================
    static void tryLockWithTimeout() throws InterruptedException {
        System.out.println("\n===== 2. tryLock with timeout =====");
        ReentrantLock lock = new ReentrantLock();
        Thread holder = new Thread(() -> {
            lock.lock();
            try { sleep(500); } finally { lock.unlock(); }
        });
        holder.start();
        sleep(50);

        System.out.println("   tryLock() immediately    -> " + lock.tryLock());
        boolean got = lock.tryLock(100, TimeUnit.MILLISECONDS);
        System.out.println("   tryLock(100ms)           -> " + got + "  (do something else instead of blocking)");
        System.out.println("   threads queued on lock   -> " + lock.getQueueLength() + ", isLocked=" + lock.isLocked());
        boolean later = lock.tryLock(1, TimeUnit.SECONDS);
        System.out.println("   tryLock(1s)              -> " + later);
        if (later) lock.unlock();
        holder.join();
    }

    // =================================================================
    // 3. lockInterruptibly: a waiting thread can be cancelled
    // =================================================================
    static void interruptibleLock() throws InterruptedException {
        System.out.println("\n===== 3. lockInterruptibly =====");
        ReentrantLock lock = new ReentrantLock();
        lock.lock();                                         // main holds it
        Thread waiter = new Thread(() -> {
            try {
                lock.lockInterruptibly();                    // waits, but responds to interrupt
                lock.unlock();
            } catch (InterruptedException e) {
                System.out.println("   waiter was interrupted while waiting for the lock -> gave up cleanly");
            }
        });
        waiter.start();
        sleep(100);
        waiter.interrupt();                                  // with synchronized this would wait forever
        waiter.join();
        lock.unlock();
    }

    // =================================================================
    // 4. Conditions: SEPARATE wait-sets for "notFull" and "notEmpty"
    //    With wait/notify there's one wait-set, so notifyAll wakes producers
    //    AND consumers. Conditions wake exactly the right group.
    //    await() = wait(), signal() = notify(), signalAll() = notifyAll()
    // =================================================================
    static class BoundedQueue<T> {
        private final Deque<T> items = new ArrayDeque<>();
        private final int capacity;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        BoundedQueue(int capacity) { this.capacity = capacity; }

        void put(T t) throws InterruptedException {
            lock.lock();
            try {
                while (items.size() == capacity) notFull.await();   // wait until there's space
                items.addLast(t);
                notEmpty.signal();                                  // wake ONE consumer only
            } finally {
                lock.unlock();
            }
        }

        T take() throws InterruptedException {
            lock.lock();
            try {
                while (items.isEmpty()) notEmpty.await();
                T t = items.removeFirst();
                notFull.signal();                                   // wake ONE producer only
                return t;
            } finally {
                lock.unlock();
            }
        }
    }

    static void conditions() throws InterruptedException {
        System.out.println("\n===== 4. CONDITIONS =====");
        BoundedQueue<String> q = new BoundedQueue<>(2);
        Thread producer = new Thread(() -> {
            try {
                for (String s : List.of("a", "b", "c", "d")) { q.put(s); System.out.println("   put  " + s); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++) { sleep(30); System.out.println("        take " + q.take()); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

    // =================================================================
    // 5. ReadWriteLock: concurrent readers, exclusive writer
    // =================================================================
    static class ConfigCache {
        private final Map<String, String> data = new HashMap<>();
        private final ReadWriteLock rw = new ReentrantReadWriteLock();
        private final Lock readLock = rw.readLock();
        private final Lock writeLock = rw.writeLock();

        String get(String key) {
            readLock.lock();                 // many threads can hold the read lock together
            try {
                sleep(50);                   // slow read
                return data.get(key);
            } finally {
                readLock.unlock();
            }
        }

        void put(String key, String value) {
            writeLock.lock();                // waits until NO readers and no writer
            try {
                data.put(key, value);
            } finally {
                writeLock.unlock();
            }
        }
    }

    static void readWriteLock() throws InterruptedException {
        System.out.println("\n===== 5. ReadWriteLock =====");
        ConfigCache cache = new ConfigCache();
        cache.put("timeout", "30s");
        long start = System.currentTimeMillis();
        Thread[] readers = new Thread[5];
        for (int i = 0; i < 5; i++) (readers[i] = new Thread(() -> cache.get("timeout"))).start();
        for (Thread t : readers) t.join();
        long ms = System.currentTimeMillis() - start;
        System.out.println("   5 reads of 50ms each took ~" + ms + " ms (parallel; an exclusive lock would take ~250ms)");
    }

    // =================================================================
    // 6. StampedLock with optimistic reads
    // =================================================================
    static class Point {
        private double x, y;
        private final StampedLock sl = new StampedLock();

        void move(double dx, double dy) {
            long stamp = sl.writeLock();
            try { x += dx; y += dy; } finally { sl.unlockWrite(stamp); }
        }

        double distanceFromOrigin() {
            long stamp = sl.tryOptimisticRead();    // NO lock taken, just a version stamp
            double cx = x, cy = y;                  // read into locals
            if (!sl.validate(stamp)) {              // did a write sneak in?
                stamp = sl.readLock();              // fall back to a real read lock
                try { cx = x; cy = y; } finally { sl.unlockRead(stamp); }
            }
            return Math.hypot(cx, cy);
        }
    }

    static void stampedLock() {
        System.out.println("\n===== 6. StampedLock =====");
        Point p = new Point();
        p.move(3, 4);
        System.out.println("   distance (optimistic read) = " + p.distanceFromOrigin());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
