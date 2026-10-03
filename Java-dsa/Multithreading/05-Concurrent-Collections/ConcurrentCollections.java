/*
 * ============================================================================
 *  TOPIC 5 : CONCURRENT COLLECTIONS
 *            ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue family,
 *            ConcurrentLinkedQueue, ConcurrentSkipListMap
 * ============================================================================
 *
 *  Run:  java ConcurrentCollections.java
 *
 *  THREE GENERATIONS OF THREAD-SAFE COLLECTIONS
 *   1. Legacy (Java 1.0)     : Vector, Hashtable. Every method synchronized.
 *   2. Synchronized wrappers : Collections.synchronizedList/Map(...). One lock
 *                              for everything. Iteration still needs MANUAL locking:
 *                                synchronized (list) { for (x : list) ... }
 *   3. Concurrent (Java 5+)  : java.util.concurrent, designed for concurrency:
 *                              fine-grained locking or lock-free algorithms,
 *                              atomic compound operations, weakly consistent iterators.
 *
 *  -----------------------------------------------------------------------------
 *   Need                                Use
 *  -----------------------------------------------------------------------------
 *   Map, many readers & writers         ConcurrentHashMap
 *   Sorted concurrent map/set           ConcurrentSkipListMap / ConcurrentSkipListSet
 *   List, MANY reads, RARE writes       CopyOnWriteArrayList (listeners, configs)
 *   Producer-consumer handoff           BlockingQueue (ArrayBlockingQueue, LinkedBlockingQueue)
 *   Non-blocking queue                  ConcurrentLinkedQueue / ConcurrentLinkedDeque
 *   Priority tasks across threads       PriorityBlockingQueue
 *   Delayed / scheduled items           DelayQueue
 *   Direct handoff (no storage)         SynchronousQueue
 *  -----------------------------------------------------------------------------
 *
 *  ITERATORS
 *    Fail-fast (ArrayList, HashMap)  : throw ConcurrentModificationException
 *                                       if modified during iteration
 *    Weakly consistent (CHM, CLQ)    : never throw; may or may not show updates
 *                                       made after the iterator was created
 *    Snapshot (CopyOnWrite*)         : iterate over the array as it was at creation
 *
 *  BlockingQueue METHOD TABLE
 *  -----------------------------------------------------------------
 *              Throws       Special value   BLOCKS     Times out
 *   Insert     add(e)       offer(e)        put(e)     offer(e, t, unit)
 *   Remove     remove()     poll()          take()     poll(t, unit)
 *   Examine    element()    peek()          -          -
 *  -----------------------------------------------------------------
 *  ArrayBlockingQueue  : array, BOUNDED, one lock for put and take
 *  LinkedBlockingQueue : linked nodes, optionally bounded (default MAX_INT!),
 *                        SEPARATE put/take locks -> higher throughput
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class ConcurrentCollections {

    public static void main(String[] args) throws Exception {
        synchronizedWrapperPitfall();
        concurrentHashMapAtomicOps();
        concurrentHashMapBulkOps();
        copyOnWriteArrayList();
        blockingQueueProducerConsumer();
        otherQueues();
        skipListMap();
    }

    // =================================================================
    // 1. Synchronized wrappers still need manual locking for iteration
    // =================================================================
    static void synchronizedWrapperPitfall() throws InterruptedException {
        System.out.println("===== 1. SYNCHRONIZED WRAPPER PITFALL =====");
        List<Integer> list = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 1000; i++) list.add(i);

        Thread writer = new Thread(() -> { for (int i = 0; i < 1000; i++) list.add(i); });
        writer.start();
        try {
            long sum = 0;
            for (int x : list) sum += x;             // iteration is NOT protected by the wrapper
            System.out.println("   iteration happened to succeed this time");
        } catch (ConcurrentModificationException e) {
            System.out.println("   ConcurrentModificationException while iterating a synchronizedList");
        }
        writer.join();

        synchronized (list) {                        // correct: hold the wrapper's lock while iterating
            long sum = 0;
            for (int x : list) sum += x;
            System.out.println("   with synchronized(list) iteration is safe, size=" + list.size());
        }
    }

    // =================================================================
    // 2. ConcurrentHashMap: use the ATOMIC compound operations
    // =================================================================
    static void concurrentHashMapAtomicOps() throws InterruptedException {
        System.out.println("\n===== 2. ConcurrentHashMap ATOMIC OPS =====");
        ConcurrentHashMap<String, Integer> wrong = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> right = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, LongAdder> best = new ConcurrentHashMap<>();

        Runnable task = () -> {
            for (int i = 0; i < 10_000; i++) {
                // WRONG: get + put are each thread-safe, but together they're a race
                Integer old = wrong.get("hits");
                wrong.put("hits", old == null ? 1 : old + 1);
                // RIGHT: merge is atomic for that key
                right.merge("hits", 1, Integer::sum);
                // BEST under high contention: one LongAdder per key
                best.computeIfAbsent("hits", k -> new LongAdder()).increment();
            }
        };
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) (ts[i] = new Thread(task)).start();
        for (Thread t : ts) t.join();
        System.out.println("   get+put   = " + wrong.get("hits") + " (lost updates)");
        System.out.println("   merge     = " + right.get("hits"));
        System.out.println("   LongAdder = " + best.get("hits").sum());

        // computeIfAbsent = thread-safe lazy initialization ("create once")
        ConcurrentHashMap<String, List<String>> groups = new ConcurrentHashMap<>();
        groups.computeIfAbsent("admins", k -> new CopyOnWriteArrayList<>()).add("satyam");
        System.out.println("   computeIfAbsent group = " + groups);
        // CAUTION: keep compute functions short and NEVER modify the same map
        // inside them (that can deadlock or throw IllegalStateException).
    }

    // =================================================================
    // 3. Bulk parallel operations (Java 8): forEach, search, reduce
    //    The first arg is the parallelism threshold: run in parallel once size >= threshold.
    // =================================================================
    static void concurrentHashMapBulkOps() {
        System.out.println("\n===== 3. ConcurrentHashMap BULK OPS =====");
        ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();
        for (int i = 1; i <= 100; i++) stock.put("item" + i, i);

        long total = stock.reduceValuesToLong(10, Integer::longValue, 0L, Long::sum);
        String firstLow = stock.search(10, (k, v) -> v < 3 ? k : null);   // returns the first non-null
        System.out.println("   total stock = " + total + ", some low-stock item = " + firstLow);
        System.out.println("   mappingCount = " + stock.mappingCount() + " (long version of size())");

        // Concurrent Set backed by CHM
        Set<String> online = ConcurrentHashMap.newKeySet();
        online.add("user1");
        System.out.println("   ConcurrentHashMap.newKeySet = " + online);
    }

    // =================================================================
    // 4. CopyOnWriteArrayList: every WRITE copies the whole array.
    //    Reads/iteration take NO lock and see a stable snapshot.
    //    Perfect for listener lists. Terrible for frequent writes (O(n) per add).
    // =================================================================
    static void copyOnWriteArrayList() {
        System.out.println("\n===== 4. CopyOnWriteArrayList =====");
        List<String> listeners = new CopyOnWriteArrayList<>(List.of("audit", "email"));
        for (String l : listeners) {
            listeners.add("added-during-iteration");    // no exception
            System.out.println("   iterating snapshot: " + l);
        }
        System.out.println("   after loop: " + listeners);

        List<String> normal = new ArrayList<>(List.of("audit", "email"));
        try {
            for (String l : normal) normal.add("x");
        } catch (ConcurrentModificationException e) {
            System.out.println("   ArrayList in the same loop -> ConcurrentModificationException");
        }
    }

    // =================================================================
    // 5. BlockingQueue: producer-consumer without wait/notify
    //    Multiple producers and consumers, plus a poison pill per consumer.
    // =================================================================
    record Order(int id, String producer) {}
    static final Order POISON = new Order(-1, "none");

    static void blockingQueueProducerConsumer() throws InterruptedException {
        System.out.println("\n===== 5. BlockingQueue PRODUCER-CONSUMER =====");
        BlockingQueue<Order> queue = new ArrayBlockingQueue<>(5);       // bounded = backpressure
        int producers = 2, consumers = 3, perProducer = 6;
        ConcurrentHashMap<String, LongAdder> handled = new ConcurrentHashMap<>();

        ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
        CountDownLatch producersDone = new CountDownLatch(producers);
        for (int p = 0; p < producers; p++) {
            String name = "P" + p;
            pool.submit(() -> {
                for (int i = 0; i < perProducer; i++) queue.put(new Order(i, name));   // blocks when full
                producersDone.countDown();
                return null;
            });
        }
        for (int c = 0; c < consumers; c++) {
            pool.submit(() -> {
                while (true) {
                    Order o = queue.take();                     // blocks when empty
                    if (o == POISON) break;
                    handled.computeIfAbsent(Thread.currentThread().getName(), k -> new LongAdder()).increment();
                    Thread.sleep(10);
                }
                return null;
            });
        }
        producersDone.await();                                  // all orders produced
        for (int c = 0; c < consumers; c++) queue.put(POISON);  // one pill per consumer
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        long total = handled.values().stream().mapToLong(LongAdder::sum).sum();
        System.out.println("   orders handled = " + total + " (expected " + producers * perProducer + ")");
        handled.forEach((t, n) -> System.out.println("     " + t + " handled " + n));
    }

    // =================================================================
    // 6. Other queues
    // =================================================================
    record DelayedTask(String name, long runAt) implements Delayed {
        public long getDelay(TimeUnit unit) { return unit.convert(runAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS); }
        public int compareTo(Delayed o) { return Long.compare(runAt, ((DelayedTask) o).runAt); }
    }

    static void otherQueues() throws InterruptedException {
        System.out.println("\n===== 6. OTHER QUEUES =====");
        // PriorityBlockingQueue: unbounded heap, thread-safe
        PriorityBlockingQueue<Integer> pq = new PriorityBlockingQueue<>(List.of(5, 1, 3));
        System.out.println("   PriorityBlockingQueue take = " + pq.take());

        // DelayQueue: take() only returns an element once its delay has EXPIRED
        DelayQueue<DelayedTask> dq = new DelayQueue<>();
        long now = System.currentTimeMillis();
        dq.put(new DelayedTask("retry-payment", now + 150));
        dq.put(new DelayedTask("send-reminder", now + 50));
        System.out.println("   DelayQueue: " + dq.take().name() + " then " + dq.take().name()
                + " after " + (System.currentTimeMillis() - now) + "ms");

        // SynchronousQueue: capacity ZERO. put() waits until another thread take()s.
        SynchronousQueue<String> handoff = new SynchronousQueue<>();
        Thread receiver = new Thread(() -> {
            try { System.out.println("   SynchronousQueue received: " + handoff.take()); }
            catch (InterruptedException ignored) {}
        });
        receiver.start();
        handoff.put("baton");                                   // blocks until the receiver takes it
        receiver.join();

        // ConcurrentLinkedQueue: lock-free (CAS), non-blocking, unbounded
        Queue<String> clq = new ConcurrentLinkedQueue<>(List.of("a", "b"));
        System.out.println("   ConcurrentLinkedQueue poll = " + clq.poll() + ", empty poll returns null instead of blocking");
    }

    // =================================================================
    // 7. ConcurrentSkipListMap: a SORTED concurrent map (a thread-safe TreeMap)
    //    Skip list = layered linked lists, O(log n), lock-free.
    //    Use case: an order book, a leaderboard, time-ordered events.
    // =================================================================
    static void skipListMap() {
        System.out.println("\n===== 7. ConcurrentSkipListMap =====");
        ConcurrentSkipListMap<Double, String> orderBook = new ConcurrentSkipListMap<>();
        orderBook.put(101.5, "sell 10");
        orderBook.put(99.0, "sell 5");
        orderBook.put(100.25, "sell 7");
        System.out.println("   best ask        = " + orderBook.firstEntry());
        System.out.println("   asks <= 100.5   = " + orderBook.headMap(100.5, true));
    }
}
