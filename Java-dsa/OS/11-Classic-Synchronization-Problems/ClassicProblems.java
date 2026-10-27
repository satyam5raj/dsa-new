/*
 * ============================================================================
 *  OS 11 : CLASSIC SYNCHRONIZATION PROBLEMS  (real threads)
 * ============================================================================
 *  Run:  java ClassicProblems.java          (explanations: README.md in this folder)
 *
 *    1. bounded buffer (semaphores)   producer-consumer with empty/full counting semaphores + a mutex
 *    2. bounded buffer (monitor)      the same with a lock and two condition variables
 *    3. readers-writers               readers share, writers exclusive; reader preference starves writers
 *    4. dining philosophers           a real deadlock with naive fork grabbing; fixed by ordering and by a waiter
 *    5. Banker's algorithm            is the state safe? grant or delay a request (textbook instance)
 *    6. deadlock detection            cycle in a wait-for (resource allocation) graph
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class ClassicProblems {

    // ---------------------------------------------------------------------
    // 1. bounded buffer with semaphores
    // ---------------------------------------------------------------------
    static class SemaphoreBuffer {
        final int[] buf;
        int in, out;
        final Semaphore empty, full = new Semaphore(0), mutex = new Semaphore(1);
        int maxFill;
        SemaphoreBuffer(int n) { buf = new int[n]; empty = new Semaphore(n); }

        void put(int x) throws InterruptedException {
            empty.acquire();                          // wait for a free slot   (P(empty))
            mutex.acquire();                          // enter critical section
            buf[in] = x; in = (in + 1) % buf.length;
            maxFill = Math.max(maxFill, buf.length - empty.availablePermits());
            mutex.release();
            full.release();                           // one more item          (V(full))
        }

        int take() throws InterruptedException {
            full.acquire();
            mutex.acquire();
            int x = buf[out]; out = (out + 1) % buf.length;
            mutex.release();
            empty.release();
            return x;
        }
    }

    static void boundedBufferSemaphores() throws Exception {
        section("1. Producer-consumer (bounded buffer) with semaphores");
        SemaphoreBuffer b = new SemaphoreBuffer(8);
        int producers = 3, perProducer = 10_000;
        long expectedSum = 0;
        for (int p = 0; p < producers; p++) for (int i = 0; i < perProducer; i++) expectedSum += p * 100_000L + i;
        AtomicLong sum = new AtomicLong();
        AtomicInteger count = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int p = 0; p < producers; p++) { int id = p; ts.add(new Thread(() -> { try { for (int i = 0; i < perProducer; i++) b.put(id * 100_000 + i); } catch (InterruptedException ignored) { } })); }
        for (int c = 0; c < 2; c++) ts.add(new Thread(() -> { try { for (int i = 0; i < producers * perProducer / 2; i++) { sum.addAndGet(b.take()); count.incrementAndGet(); } } catch (InterruptedException ignored) { } }));
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        check("every item consumed exactly once (30,000 items, sums match)", expectedSum, sum.get());
        check("the buffer never held more than its capacity (8)", true, b.maxFill <= 8);
        show("order matters: acquire 'empty' BEFORE 'mutex' -- the other way round, a producer can sleep holding the mutex -> deadlock");
    }

    // ---------------------------------------------------------------------
    // 2. bounded buffer as a monitor
    // ---------------------------------------------------------------------
    static class MonitorBuffer<T> {
        final Deque<T> q = new ArrayDeque<>();
        final int cap;
        final ReentrantLock lock = new ReentrantLock();
        final Condition notFull = lock.newCondition(), notEmpty = lock.newCondition();
        MonitorBuffer(int cap) { this.cap = cap; }

        void put(T x) throws InterruptedException {
            lock.lock();
            try { while (q.size() == cap) notFull.await(); q.addLast(x); notEmpty.signal(); } finally { lock.unlock(); }
        }

        T take() throws InterruptedException {
            lock.lock();
            try { while (q.isEmpty()) notEmpty.await(); T x = q.pollFirst(); notFull.signal(); return x; } finally { lock.unlock(); }
        }
    }

    static void boundedBufferMonitor() throws Exception {
        section("2. Producer-consumer as a monitor (lock + conditions)");
        MonitorBuffer<Integer> b = new MonitorBuffer<>(4);
        List<Integer> got = Collections.synchronizedList(new ArrayList<>());
        Thread prod = new Thread(() -> { try { for (int i = 0; i < 1000; i++) b.put(i); } catch (InterruptedException ignored) { } });
        Thread cons = new Thread(() -> { try { for (int i = 0; i < 1000; i++) got.add(b.take()); } catch (InterruptedException ignored) { } });
        prod.start(); cons.start(); prod.join(); cons.join();
        boolean ordered = true;
        for (int i = 0; i < got.size(); i++) ordered &= got.get(i) == i;
        check("single producer + single consumer: all 1,000 items, in FIFO order", "1000 true", got.size() + " " + ordered);
        show("this is what java.util.concurrent.ArrayBlockingQueue does internally");
    }

    // ---------------------------------------------------------------------
    // 3. readers-writers
    // ---------------------------------------------------------------------
    static long writerWaitMs(boolean fair) throws Exception {
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock(fair);
        AtomicBoolean stop = new AtomicBoolean();
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {                                // a constant stream of overlapping readers
            Thread t = new Thread(() -> { while (!stop.get()) { rw.readLock().lock(); try { Thread.sleep(3); } catch (InterruptedException ignored) { } finally { rw.readLock().unlock(); } } });
            readers.add(t);
            t.start();
        }
        Thread.sleep(50);
        long t0 = System.nanoTime();
        boolean got = rw.writeLock().tryLock(1500, TimeUnit.MILLISECONDS);
        long waited = (System.nanoTime() - t0) / 1_000_000;
        if (got) rw.writeLock().unlock();
        stop.set(true);
        for (Thread t : readers) t.join();
        return got ? waited : -1;
    }

    static void readersWriters() throws Exception {
        section("3. Readers-writers");
        long fairWait = writerWaitMs(true);
        show(String.format("writer waiting against a continuous stream of readers: fair lock -> got it after %d ms", fairWait));
        check("a fair (FIFO) read-write lock lets the writer in promptly", true, fairWait >= 0 && fairWait < 200);
        show("reader-preference (classic first solution): new readers keep joining while any reader holds the lock -> WRITER STARVATION");
        show("writer-preference: block new readers once a writer waits -> readers can starve instead; fair queues trade throughput for fairness");
    }

    // ---------------------------------------------------------------------
    // 4. dining philosophers
    // ---------------------------------------------------------------------
    static boolean dine(String strategy, int meals) throws Exception {
        int n = 5;
        ReentrantLock[] forks = new ReentrantLock[n];
        for (int i = 0; i < n; i++) forks[i] = new ReentrantLock();
        Semaphore waiter = new Semaphore(n - 1);                     // at most 4 may try to eat at once
        CyclicBarrier together = new CyclicBarrier(n);
        AtomicInteger eaten = new AtomicInteger();
        List<Thread> ps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int left = i, right = (i + 1) % n;
            Thread t = new Thread(() -> {
                try {
                    for (int m = 0; m < meals; m++) {
                        int first = left, second = right;
                        if (strategy.equals("ordered")) { first = Math.min(left, right); second = Math.max(left, right); }   // global order: lower fork first
                        if (strategy.equals("waiter")) waiter.acquire();
                        if (strategy.equals("naive") && m == 0) together.await();   // everyone reaches for the left fork at the same moment
                        forks[first].lock();
                        if (strategy.equals("naive") && m == 0) Thread.sleep(50);
                        forks[second].lock();
                        eaten.incrementAndGet();
                        forks[second].unlock();
                        forks[first].unlock();
                        if (strategy.equals("waiter")) waiter.release();
                    }
                } catch (Exception ignored) { }
            });
            t.setDaemon(true);
            ps.add(t);
            t.start();
        }
        for (Thread t : ps) t.join(3000);
        return eaten.get() == n * meals;
    }

    static void philosophers() throws Exception {
        section("4. Dining philosophers");
        check("naive (everyone takes LEFT then RIGHT, simultaneously) -> circular wait: DEADLOCK", false, dine("naive", 1));
        check("resource ordering (always pick the lower-numbered fork first) breaks the cycle", true, dine("ordered", 2000));
        check("a 'waiter' semaphore (at most 4 of 5 at the table) also prevents deadlock", true, dine("waiter", 2000));
    }

    // ---------------------------------------------------------------------
    // 5. Banker's algorithm
    // ---------------------------------------------------------------------
    static List<Integer> safeSequence(int[] available, int[][] max, int[][] alloc) {
        int n = max.length, m = available.length;
        int[] work = available.clone();
        boolean[] finished = new boolean[n];
        List<Integer> seq = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < n; i++) {
                if (finished[i]) continue;
                boolean canFinish = true;
                for (int j = 0; j < m; j++) if (max[i][j] - alloc[i][j] > work[j]) canFinish = false;   // need <= work ?
                if (canFinish) { for (int j = 0; j < m; j++) work[j] += alloc[i][j]; finished[i] = true; seq.add(i); progress = true; }
            }
        }
        return seq.size() == n ? seq : null;                         // null = unsafe
    }

    static void bankers() {
        section("5. Banker's algorithm (deadlock avoidance; the classic textbook instance)");
        int[] available = {3, 3, 2};
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        int[][] alloc = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        List<Integer> seq = safeSequence(available, max, alloc);
        show("safe sequence: " + seq.stream().map(i -> "P" + i).toList());
        check("the state is SAFE (all can finish in some order)", "[1, 3, 4, 0, 2]", String.valueOf(seq));
        int[][] alloc2 = Arrays.stream(alloc).map(int[]::clone).toArray(int[][]::new);
        alloc2[1] = new int[]{3, 0, 2};                               // P1 requests (1,0,2): pretend to grant it
        check("P1 requests (1,0,2): the resulting state is still safe -> GRANT", true, safeSequence(new int[]{2, 3, 0}, max, alloc2) != null);
        int[][] alloc3 = Arrays.stream(alloc2).map(int[]::clone).toArray(int[][]::new);
        alloc3[0] = new int[]{0, 3, 0};                               // AFTER P1's grant, P0 requests (0,2,0): available would drop to (2,1,0)
        check("then P0 requests (0,2,0): resources exist, but the state would be UNSAFE -> P0 waits", null, safeSequence(new int[]{2, 1, 0}, max, alloc3));
        show("needs every process's MAXIMUM claim up front -> rarely used in general OSes; the idea lives on in admission control");
    }

    // ---------------------------------------------------------------------
    // 6. wait-for graph cycle detection
    // ---------------------------------------------------------------------
    static List<String> findCycle(Map<String, List<String>> waitsFor) {
        Map<String, Integer> state = new HashMap<>();                // 0 = new, 1 = on stack, 2 = done
        Deque<String> path = new ArrayDeque<>();
        for (String start : waitsFor.keySet()) {
            List<String> c = dfs(start, waitsFor, state, path);
            if (c != null) return c;
        }
        return List.of();
    }

    static List<String> dfs(String v, Map<String, List<String>> g, Map<String, Integer> state, Deque<String> path) {
        if (state.getOrDefault(v, 0) == 1) {                         // back edge: cycle
            List<String> cycle = new ArrayList<>();
            Iterator<String> it = path.iterator();                   // oldest -> newest: the cycle runs from v to the end of the path
            boolean in = false;
            while (it.hasNext()) { String x = it.next(); if (x.equals(v)) in = true; if (in) cycle.add(x); }
            cycle.add(v);
            return cycle;
        }
        if (state.getOrDefault(v, 0) == 2) return null;
        state.put(v, 1);
        path.addLast(v);
        for (String w : g.getOrDefault(v, List.of())) { List<String> c = dfs(w, g, state, path); if (c != null) return c; }
        path.removeLast();
        state.put(v, 2);
        return null;
    }

    static void detection() {
        section("6. Deadlock detection: cycles in the wait-for graph");
        Map<String, List<String>> g = new LinkedHashMap<>();
        g.put("T1", List.of("T2"));                                  // T1 waits for a lock held by T2 ...
        g.put("T2", List.of("T3"));
        g.put("T3", List.of("T1"));
        g.put("T4", List.of("T2"));                                  // T4 is blocked but not part of the cycle
        check("cycle found -> deadlock among T1, T2, T3", "[T1, T2, T3, T1]", findCycle(g).toString());
        g.put("T3", List.of());                                      // abort/roll back a victim (as databases do)
        check("after aborting the victim's wait, no cycle remains", "[]", findCycle(g).toString());
    }

    public static void main(String[] args) throws Exception {
        boundedBufferSemaphores();
        boundedBufferMonitor();
        readersWriters();
        philosophers();
        bankers();
        detection();
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
        System.exit(0);                                              // the deadlocked philosophers are daemon threads
    }
}
