/*
 * ============================================================================
 *  TOPIC 2.1 : RACE CONDITIONS, synchronized, wait/notify
 * ============================================================================
 *
 *  Run:  java SynchronizedAndWaitNotify.java
 *
 *  RACE CONDITION: the result depends on the unpredictable TIMING of threads.
 *  count++ looks atomic but is THREE steps:
 *      1. READ count from memory   (count = 5)
 *      2. ADD 1                     (6)
 *      3. WRITE back                (count = 6)
 *  Two threads both read 5, both write 6 -> one increment is LOST.
 *  The code that touches shared mutable state is the CRITICAL SECTION.
 *
 *  TWO TYPICAL RACE SHAPES
 *    read-modify-write : count++, balance -= x
 *    check-then-act    : if (!map.containsKey(k)) map.put(k, v);
 *                        if (instance == null) instance = new X();
 *
 *  synchronized - Java's built-in lock ("intrinsic lock" / "monitor")
 *    - Every object has a monitor. Only ONE thread can hold it at a time.
 *    - synchronized method        -> locks 'this'
 *    - static synchronized method -> locks the Class object (ClassName.class)
 *    - synchronized (obj) { }     -> locks obj (a block: smaller scope = better)
 *    Guarantees BOTH:
 *      MUTUAL EXCLUSION : one thread at a time in the critical section
 *      VISIBILITY       : changes made before unlock are visible to the
 *                         next thread that locks the same monitor (happens-before)
 *    - REENTRANT: a thread holding a lock can enter it again (no self-deadlock)
 *    - Released automatically on exit, even if an exception is thrown
 *    - Can't time out, can't be interrupted while waiting (ReentrantLock can)
 *
 *  wait / notify / notifyAll - thread COORDINATION on a monitor
 *    - Must be called while HOLDING that object's lock (else IllegalMonitorStateException)
 *    - wait()      : RELEASES the lock and sleeps until notified
 *    - notify()    : wakes ONE waiting thread (an arbitrary one)
 *    - notifyAll() : wakes ALL waiting threads (safer default)
 *    - ALWAYS wait in a WHILE loop, never an if: (1) spurious wakeups can
 *      happen, and (2) another thread may grab the condition first.
 *    Today, prefer higher-level tools (BlockingQueue, Condition, latches),
 *    but interviewers love asking for producer-consumer with wait/notify.
 *
 *  sleep() vs wait()
 *    sleep: Thread method, KEEPS any locks held, wakes after a time
 *    wait : Object method, RELEASES the lock, wakes on notify/timeout
 */

import java.util.*;

public class SynchronizedAndWaitNotify {

    public static void main(String[] args) throws Exception {
        raceCondition();
        synchronizedFixes();
        checkThenAct();
        reentrancy();
        producerConsumer();
        oddEvenPrinter();
    }

    // =================================================================
    // 1. Lost updates
    // =================================================================
    static int unsafeCount = 0;

    static void raceCondition() throws InterruptedException {
        System.out.println("===== 1. RACE CONDITION =====");
        Runnable inc = () -> { for (int i = 0; i < 100_000; i++) unsafeCount++; };
        runInThreads(inc, 4);
        System.out.println("   expected 400000, got " + unsafeCount + "  <- lost updates");
    }

    // =================================================================
    // 2. Three ways to lock
    // =================================================================
    static class Counter {
        private int count = 0;
        private static int instances = 0;
        private final Object lock = new Object();    // private lock object: outsiders can't grab it

        synchronized void incMethod() { count++; }                 // locks 'this'

        void incBlock() {
            // ... work that needs no lock could go here (keep critical sections SMALL) ...
            synchronized (lock) { count++; }                       // locks a private object
        }

        static synchronized void newInstance() { instances++; }   // locks Counter.class

        synchronized int get() { return count; }                  // reads also need the lock for VISIBILITY
    }

    static void synchronizedFixes() throws InterruptedException {
        System.out.println("\n===== 2. synchronized FIXES =====");
        Counter c1 = new Counter();
        runInThreads(() -> { for (int i = 0; i < 100_000; i++) c1.incMethod(); }, 4);
        System.out.println("   synchronized method : " + c1.get());

        Counter c2 = new Counter();
        runInThreads(() -> { for (int i = 0; i < 100_000; i++) c2.incBlock(); }, 4);
        System.out.println("   synchronized block  : " + c2.get());

        runInThreads(() -> { for (int i = 0; i < 1000; i++) Counter.newInstance(); }, 4);
        System.out.println("   static synchronized : " + Counter.instances);
        // Tip: a private final lock object is better than 'this'. With 'this',
        // any outside code can do synchronized(yourObject) and interfere.
    }

    // =================================================================
    // 3. Check-then-act: each call is thread-safe, the COMBINATION isn't
    // =================================================================
    static void checkThenAct() throws InterruptedException {
        System.out.println("\n===== 3. CHECK-THEN-ACT =====");
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        int[] created = {0};
        Object createdLock = new Object();
        Runnable unsafe = () -> {
            if (!syncMap.containsKey("config")) {          // check   (thread-safe on its own)
                sleep(5);                                  // widen the gap
                syncMap.put("config", 1);                  // act     (thread-safe on its own)
                synchronized (createdLock) { created[0]++; }
            }
        };
        runInThreads(unsafe, 4);
        System.out.println("   'create once' ran " + created[0] + " times (should be 1)");
        // Fixes: synchronized(syncMap) { check + put }, or an atomic op:
        // concurrentHashMap.putIfAbsent / computeIfAbsent
    }

    // =================================================================
    // 4. Reentrancy: a synchronized method can call another one on the same lock
    // =================================================================
    static class Reentrant {
        synchronized void outer() {
            System.out.println("   in outer(), holds lock: " + Thread.holdsLock(this));
            inner();                                       // same lock again: allowed
        }
        synchronized void inner() { System.out.println("   in inner(), re-entered the same lock fine"); }
    }

    static void reentrancy() {
        System.out.println("\n===== 4. REENTRANCY =====");
        new Reentrant().outer();
    }

    // =================================================================
    // 5. Producer-Consumer with wait/notifyAll (bounded buffer)
    // =================================================================
    static class BoundedBuffer<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;

        BoundedBuffer(int capacity) { this.capacity = capacity; }

        synchronized void put(T item) throws InterruptedException {
            while (queue.size() == capacity) {   // WHILE, not if
                wait();                          // releases the lock; producer sleeps
            }
            queue.add(item);
            notifyAll();                         // wake consumers waiting on "empty"
        }

        synchronized T take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();                          // consumer sleeps
            }
            T item = queue.poll();
            notifyAll();                         // wake producers waiting on "full"
            return item;
        }
    }

    static void producerConsumer() throws InterruptedException {
        System.out.println("\n===== 5. PRODUCER-CONSUMER (wait/notify) =====");
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(2);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    buffer.put(i);
                    System.out.println("   produced " + i);
                }
                buffer.put(-1);                  // "poison pill": tells the consumer to stop
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int v = buffer.take();
                    if (v == -1) break;
                    consumed.add(v);
                    System.out.println("            consumed " + v);
                    sleep(20);                   // slow consumer -> producer blocks when the buffer is full
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("   all consumed in order: " + consumed);
    }

    // =================================================================
    // 6. Classic interview: two threads print 1..10 alternately (odd/even)
    // =================================================================
    static void oddEvenPrinter() throws InterruptedException {
        System.out.println("\n===== 6. ODD/EVEN PRINTER =====");
        Object lock = new Object();
        int max = 10;
        int[] next = {1};
        StringBuilder out = new StringBuilder();

        Runnable printer = () -> {
            boolean odd = Thread.currentThread().getName().equals("odd");
            synchronized (lock) {
                while (next[0] <= max) {
                    if ((next[0] % 2 == 1) == odd) {          // my turn
                        out.append(Thread.currentThread().getName()).append(":").append(next[0]).append(' ');
                        next[0]++;
                        lock.notifyAll();                     // let the other thread go
                    } else {
                        try { lock.wait(); } catch (InterruptedException e) { return; }
                    }
                }
                lock.notifyAll();                             // release the other thread at the end
            }
        };
        Thread t1 = new Thread(printer, "odd"), t2 = new Thread(printer, "even");
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("   " + out.toString().trim());
    }

    // ---------- helpers ----------
    static void runInThreads(Runnable r, int n) throws InterruptedException {
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) (ts[i] = new Thread(r)).start();
        for (Thread t : ts) t.join();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
