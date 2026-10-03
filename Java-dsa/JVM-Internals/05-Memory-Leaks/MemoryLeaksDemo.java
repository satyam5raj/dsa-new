/*
 * ============================================================================
 *  TOPIC 5 : MEMORY LEAKS IN JAVA - causes, symptoms, diagnosis
 * ============================================================================
 *
 *  Run:  java MemoryLeaksDemo.java
 *
 *  "Java has a GC, so how can it leak?"
 *  The GC frees UNREACHABLE objects. A Java memory leak is an object that is
 *  still REACHABLE but will NEVER be used again. Some forgotten reference
 *  (a static map, a listener list, a ThreadLocal) keeps it alive.
 *
 *  SYMPTOMS
 *    - Old gen usage after each full GC keeps climbing (a "sawtooth with a
 *      rising floor")
 *    - More and longer GC pauses, CPU busy with GC, then
 *    - java.lang.OutOfMemoryError: Java heap space
 *
 *  KINDS OF OutOfMemoryError
 *    Java heap space                 : heap full of live objects (a leak, or the heap is too small)
 *    GC overhead limit exceeded      : >98% of time in GC recovering <2% of heap (Parallel GC)
 *    Metaspace                       : too many classes, e.g. classloader leaks on redeploy
 *    Requested array size exceeds VM limit
 *    unable to create native thread  : too many threads or the OS limit (ulimit -u)
 *    Direct buffer memory            : off-heap ByteBuffers (Netty), -XX:MaxDirectMemorySize
 *
 *  THE USUAL SUSPECTS (each demonstrated below)
 *    1. Static collections that only grow (caches with no eviction)
 *    2. Listeners/callbacks registered and never removed
 *    3. ThreadLocal values on POOLED threads (threads never die -> values never die)
 *    4. Non-static inner classes / lambdas capturing a big outer object
 *    5. HashMap keys without proper equals/hashCode (a "duplicate" entry on every put)
 *    6. Unclosed resources (streams, connections, ResultSets): native memory and file descriptors
 *    7. ClassLoader leaks: a reference from a long-lived class into a webapp
 *       class pins the whole webapp's classloader and all its classes (Metaspace)
 *    8. Long-lived objects holding references to big short-lived ones
 *       (e.g. substring/subList views, sessions holding entire request graphs)
 *
 *  HOW TO DIAGNOSE (the production playbook)
 *    1. CONFIRM: monitor heap-after-GC (GC logs -Xlog:gc*, JFR, Prometheus/Grafana
 *       via Micrometer, VisualVM). A growing floor = a leak. A spiky but flat floor = OK.
 *    2. CLASS HISTOGRAM (cheap, quick):  jcmd <pid> GC.class_histogram
 *       Take two, minutes apart. Whatever keeps growing is your suspect.
 *    3. HEAP DUMP:  jcmd <pid> GC.heap_dump /tmp/heap.hprof
 *                   (or jmap -dump:live,format=b,file=heap.hprof <pid>)
 *       Always set in production:
 *         -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps
 *    4. ANALYZE in Eclipse MAT / VisualVM / IntelliJ:
 *       "Leak Suspects" report, Dominator Tree (what RETAINS the most memory),
 *       "Path to GC Roots" (WHO is holding it).
 *    5. JFR (Java Flight Recorder) with the OldObjectSample event:
 *       low overhead, safe for production.
 */

import java.io.*;
import java.lang.management.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class MemoryLeaksDemo {

    public static void main(String[] args) throws Exception {
        staticCollectionLeak();
        listenerLeak();
        threadLocalLeak();
        innerClassLeak();
        badKeyLeak();
        classHistogram();
        heapDump();
        outOfMemoryKinds();
    }

    // =================================================================
    // 1. Static collection that only grows
    // =================================================================
    static final Map<Long, byte[]> REQUEST_CACHE = new HashMap<>();   // no eviction!

    static void handleRequest(long id) {
        REQUEST_CACHE.put(id, new byte[10 * 1024]);                    // "cache" each response (10 KB)
    }

    static void staticCollectionLeak() {
        System.out.println("===== 1. STATIC COLLECTION LEAK =====");
        for (int batch = 1; batch <= 4; batch++) {
            for (int i = 0; i < 2_000; i++) handleRequest(batch * 10_000L + i);
            System.out.printf("   after batch %d: heap used after GC = %,4d MB  <- floor keeps rising%n", batch, usedAfterGcMb());
        }
        REQUEST_CACHE.clear();
        System.out.printf("   after clearing the map: %,d MB%n", usedAfterGcMb());
        System.out.println("   FIX: bounded cache with eviction (LinkedHashMap LRU, Caffeine with maximumSize / expireAfterWrite)");
    }

    // =================================================================
    // 2. Listener registered but never removed
    // =================================================================
    static class EventBus {
        private final List<Runnable> listeners = new ArrayList<>();
        void register(Runnable r) { listeners.add(r); }
        void unregister(Runnable r) { listeners.remove(r); }
        int size() { return listeners.size(); }
    }

    static class Screen {
        private final byte[] bitmap = new byte[200 * 1024];          // 200 KB of state
        void onEvent() { /* redraw using bitmap: */ int unused = bitmap.length; }
    }

    static void listenerLeak() {
        System.out.println("\n===== 2. LISTENER LEAK =====");
        EventBus bus = new EventBus();                                // long-lived (app-wide)
        long before = usedAfterGcMb();
        for (int i = 0; i < 100; i++) {
            Screen s = new Screen();                                  // short-lived screen...
            bus.register(s::onEvent);                                 // ...but the bus holds a reference to it
            // user navigates away; we forget bus.unregister(...)
        }
        System.out.printf("   100 closed screens still retained: +%d MB, listeners=%d%n", usedAfterGcMb() - before, bus.size());
        System.out.println("   FIX: unregister in close()/onDestroy (try-with-resources), or use weak listeners");
    }

    // =================================================================
    // 3. ThreadLocal on pooled threads
    //    Pool threads live forever, and so do their ThreadLocal values,
    //    unless you remove() them. In app servers this also pins the webapp's
    //    classloader -> Metaspace leak on every redeploy.
    // =================================================================
    static final ThreadLocal<byte[]> REQUEST_BUFFER = new ThreadLocal<>();

    static void threadLocalLeak() throws Exception {
        System.out.println("\n===== 3. ThreadLocal LEAK ON A THREAD POOL =====");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        long before = usedAfterGcMb();
        runOnAllThreads(pool, () -> REQUEST_BUFFER.set(new byte[5 * 1024 * 1024]));   // 5 MB per thread, never removed
        System.out.printf("   requests finished, but +%d MB still held by 8 pool threads%n", usedAfterGcMb() - before);

        runOnAllThreads(pool, () -> {
            try {
                // ... use the buffer ...
            } finally {
                REQUEST_BUFFER.remove();                              // FIX: ALWAYS remove() in finally
            }
        });
        System.out.printf("   after remove() in finally: +%d MB%n", usedAfterGcMb() - before);
        pool.shutdown();
    }

    static void runOnAllThreads(ExecutorService pool, Runnable r) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(8);                 // makes sure all 8 threads take one task
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 8; i++) fs.add(pool.submit(() -> { r.run(); barrier.await(); return null; }));
        for (Future<?> f : fs) f.get();
    }

    // =================================================================
    // 4. Inner class / lambda capturing a huge outer object
    // =================================================================
    static class Report {
        private final byte[] rawData = new byte[20 * 1024 * 1024];    // 20 MB
        private final String title;                                   // NOT a compile-time constant

        Report(String title) { this.title = title; }

        // Non-static inner class: has a hidden reference "Report.this" -> keeps rawData alive.
        // (Since JDK 18, javac OMITS that hidden field when the inner class never touches
        //  the outer instance. Here text() reads Report.this.title, so the reference is kept.
        //  Had 'title' been a constant like = "Q3", it would be inlined and there would be no leak!)
        class TitleView { String text() { return title; } }

        // Static nested class that copies only what it needs
        static class SafeTitleView {
            private final String text;
            SafeTitleView(String text) { this.text = text; }
        }
    }

    // A long-lived holder (think: a UI component registry, a session, a cache).
    // NOTE: we use a static field, not a local variable. The JIT and even the
    // interpreter's liveness analysis treat a local that is never read again
    // as DEAD, so the GC could collect it anyway. Real leaks come from
    // long-lived references like this one.
    static Object longLivedHolder;

    static void innerClassLeak() {
        System.out.println("\n===== 4. INNER CLASS LEAK =====");
        long before = usedAfterGcMb();
        longLivedHolder = new Report("Q3 report").new TitleView();               // we only wanted the title...
        System.out.printf("   holding an inner-class TitleView retains: +%d MB (the whole Report)%n", usedAfterGcMb() - before);
        longLivedHolder = null;

        before = usedAfterGcMb();
        longLivedHolder = new Report.SafeTitleView(new Report("Q3 report").title);
        System.out.printf("   holding a static SafeTitleView retains : +%d MB%n", usedAfterGcMb() - before);
        longLivedHolder = null;
        // Same with lambdas: a lambda that uses an instance field captures 'this'.
    }

    // =================================================================
    // 5. Keys without equals/hashCode: the map grows on every "update"
    // =================================================================
    static class SessionKey {                                         // no equals/hashCode!
        final String userId;
        SessionKey(String userId) { this.userId = userId; }
    }

    static void badKeyLeak() {
        System.out.println("\n===== 5. BAD HASHMAP KEYS =====");
        Map<SessionKey, String> sessions = new HashMap<>();
        for (int i = 0; i < 10_000; i++) sessions.put(new SessionKey("user-1"), "session data");   // meant to overwrite
        System.out.println("   map size after 10,000 'updates' of ONE user: " + sessions.size());
        System.out.println("   FIX: implement equals/hashCode (or use a record as the key)");
    }

    // =================================================================
    // 6. Class histogram of THIS process via jcmd (what you'd do in prod)
    // =================================================================
    static final List<byte[]> SUSPECT = new ArrayList<>();

    static void classHistogram() throws Exception {
        System.out.println("\n===== 6. CLASS HISTOGRAM (jcmd <pid> GC.class_histogram) =====");
        for (int i = 0; i < 3_000; i++) SUSPECT.add(new byte[4096]);  // plant a "leak"
        long pid = ProcessHandle.current().pid();
        Path jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd");
        Process p = new ProcessBuilder(jcmd.toString(), String.valueOf(pid), "GC.class_histogram")
                .redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            r.lines().limit(8).forEach(line -> System.out.println("   " + line));
        }
        p.waitFor();
        System.out.println("   -> [B = byte[] dominates; take 2 histograms over time and compare the growth");
    }

    // =================================================================
    // 7. Programmatic heap dump (same as jcmd <pid> GC.heap_dump)
    // =================================================================
    static void heapDump() throws Exception {
        System.out.println("\n===== 7. HEAP DUMP =====");
        Path dir = Files.createTempDirectory("heapdump");
        Path file = dir.resolve("demo.hprof");
        var bean = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
        bean.dumpHeap(file.toString(), true);                         // true = only live objects (forces a GC)
        System.out.printf("   wrote %s (%,d MB)%n", file.getFileName(), Files.size(file) / (1024 * 1024));
        System.out.println("   open .hprof files in Eclipse MAT / VisualVM -> Dominator Tree -> Path to GC Roots");
        System.out.println("   HeapDumpOnOutOfMemoryError currently = "
                + bean.getVMOption("HeapDumpOnOutOfMemoryError").getValue() + " (turn it ON in production)");
        Files.delete(file);
        Files.delete(dir);
        SUSPECT.clear();
    }

    // =================================================================
    // 8. A safe-to-trigger OutOfMemoryError
    // =================================================================
    static void outOfMemoryKinds() {
        System.out.println("\n===== 8. OutOfMemoryError =====");
        try {
            long[] huge = new long[Integer.MAX_VALUE];               // ~16 GB: beyond limits, fails instantly
            System.out.println(huge.length);
        } catch (OutOfMemoryError e) {
            System.out.println("   caught: OutOfMemoryError: " + e.getMessage());
        }
        System.out.println("   (catching OOM is only safe in demos; in real apps the JVM state is unreliable afterwards.");
        System.out.println("    Prefer -XX:+ExitOnOutOfMemoryError so the orchestrator restarts a clean instance.)");
    }

    // ---------- helper: force a GC and measure what's still live ----------
    static long usedAfterGcMb() {
        for (int i = 0; i < 3; i++) System.gc();
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}
