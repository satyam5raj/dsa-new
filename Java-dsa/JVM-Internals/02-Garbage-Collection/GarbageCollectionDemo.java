/*
 * ============================================================================
 *  TOPIC 2 : GARBAGE COLLECTION - reachability, generations, algorithms,
 *            reference types, GC logs
 * ============================================================================
 *
 *  Run:  java GarbageCollectionDemo.java
 *  See GC logs:      java -Xlog:gc GarbageCollectionDemo.java
 *  Compare GCs:      ./compare-gcs.sh   (same workload on Serial/Parallel/G1/ZGC)
 *
 *  WHAT IS GARBAGE?
 *    An object is garbage when it is UNREACHABLE from any GC ROOT.
 *    (Java does NOT use reference counting, so cycles like A <-> B are
 *     collected fine once nothing reachable points to them.)
 *    GC ROOTS: local variables on thread stacks, static fields, active
 *    threads, JNI references, monitors held by synchronized.
 *
 *  BASIC PHASES
 *    MARK    : walk the object graph from the roots, mark everything reachable
 *    SWEEP   : free unmarked memory (leaves FRAGMENTATION)
 *    COMPACT : slide live objects together (no fragmentation, costs time)
 *    COPY    : copy live objects to an empty space (young gen). Cost is
 *              proportional to LIVE objects, not garbage. Very cheap when most die.
 *
 *  WEAK GENERATIONAL HYPOTHESIS: most objects die young (request DTOs,
 *  iterators, temp strings). So: collect the small YOUNG gen often and cheaply
 *  (minor GC), and the big OLD gen rarely (major/full GC).
 *  Old -> young references are tracked with a CARD TABLE, so a minor GC
 *  doesn't have to scan the whole old gen.
 *
 *  STOP-THE-WORLD (STW): all application threads pause during (parts of) a GC.
 *  Two goals pull against each other:
 *    THROUGHPUT : % of time spent running the app (not GC)
 *    LATENCY    : length of the pauses
 *    (plus FOOTPRINT: how much memory is used)
 *
 *  THE COLLECTORS (HotSpot)
 *  -----------------------------------------------------------------------------------
 *   GC           Flag                  Style                         Best for
 *  -----------------------------------------------------------------------------------
 *   Serial       -XX:+UseSerialGC      1 thread, STW                 tiny heaps, 1 CPU, containers <2 cores
 *   Parallel     -XX:+UseParallelGC    many threads, STW             batch jobs: max THROUGHPUT
 *   G1 (default) -XX:+UseG1GC          regions, mostly concurrent    balanced; pause target
 *                                      marking, STW evacuation       -XX:MaxGCPauseMillis=200
 *   ZGC          -XX:+UseZGC           concurrent compaction         huge heaps, ultra-low latency
 *                                      (colored pointers + load      (< 1 ms pauses, TB heaps)
 *                                      barriers); generational in 21+
 *   Shenandoah   -XX:+UseShenandoahGC  concurrent compaction         low latency (Red Hat builds)
 *   Epsilon      -XX:+UseEpsilonGC     NO-OP: never collects         perf testing, short-lived jobs
 *  -----------------------------------------------------------------------------------
 *   CMS was deprecated in 9 and REMOVED in 14.
 *
 *  G1 IN ONE PARAGRAPH: the heap is split into ~2048 equal regions (1-32 MB).
 *  Young GCs evacuate eden/survivor regions (STW, parallel). When the heap is
 *  ~45% full (InitiatingHeapOccupancyPercent), a CONCURRENT marking cycle
 *  finds which old regions are mostly garbage. Then "mixed" GCs collect young
 *  regions plus the regions with the MOST garbage first (hence "Garbage-First"),
 *  sized to meet the pause target. Objects >= half a region are "humongous"
 *  and go straight to the old gen.
 *
 *  WHEN GC PAUSES MATTER: latency-sensitive APIs (p99 SLAs), trading, games.
 *  Symptoms: p99 spikes, timeouts. Causes: too small heap, high allocation
 *  rate, humongous allocations, a leak (old gen keeps growing -> full GCs).
 */

import java.lang.management.*;
import java.lang.ref.*;
import java.util.*;

public class GarbageCollectionDemo {

    public static void main(String[] args) throws Exception {
        whichGcAmIUsing();
        reachabilityAndCycles();
        youngGcFromShortLivedObjects();
        referenceTypes();
        cleanerInsteadOfFinalize();
        gcIsOnlyAHint();
    }

    // =================================================================
    // 1. Inspect the collectors in use
    // =================================================================
    static void whichGcAmIUsing() {
        System.out.println("===== 1. COLLECTORS IN THIS JVM =====");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("   %-25s collections=%d time=%dms pools=%s%n",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime(), Arrays.toString(gc.getMemoryPoolNames()));
        }
        System.out.println("   (G1 Young Generation = minor GCs, G1 Concurrent GC = marking cycles, G1 Old = full GCs)");
    }

    // =================================================================
    // 2. Reachability: cycles are collected; a WeakReference shows us when
    // =================================================================
    static class Node {
        Node next;
        byte[] payload = new byte[1024];
    }

    static void reachabilityAndCycles() throws InterruptedException {
        System.out.println("\n===== 2. REACHABILITY & CYCLES =====");
        Node a = new Node(), b = new Node();
        a.next = b;
        b.next = a;                                      // a cycle: reference counting would leak this
        WeakReference<Node> probe = new WeakReference<>(a);

        System.gc();
        System.out.println("   still referenced by local 'a' -> collected? " + (probe.get() == null));

        a = null;                                        // drop the roots: the cycle is now unreachable
        b = null;
        waitForGc(probe);
        System.out.println("   after a = b = null           -> collected? " + (probe.get() == null)
                + " (cycle collected: Java uses reachability, not ref counting)");
    }

    // =================================================================
    // 3. Lots of short-lived garbage -> many cheap YOUNG collections
    // =================================================================
    static void youngGcFromShortLivedObjects() {
        System.out.println("\n===== 3. YOUNG GCs FROM SHORT-LIVED OBJECTS =====");
        Map<String, Long> before = gcCounts();
        long start = System.nanoTime();
        long checksum = 0;
        for (int i = 0; i < 3_000_000; i++) {
            String temp = "order-" + i;                  // garbage right after this iteration
            byte[] buf = new byte[256];                  // garbage too
            checksum += temp.length() + buf.length;
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        Map<String, Long> after = gcCounts();
        System.out.printf("   allocated ~%,d MB of temp objects in %d ms (checksum %d)%n", 3_000_000L * 300 / (1024 * 1024), ms, checksum);
        after.forEach((name, count) -> System.out.println("   " + name + ": +" + (count - before.getOrDefault(name, 0L)) + " collections"));
        System.out.println("   -> dead young objects are never copied, so each minor GC is fast");
    }

    // =================================================================
    // 4. Reference types (java.lang.ref)
    //    STRONG  : normal reference. Never collected while reachable.
    //    SOFT    : collected only when memory is LOW (before an OOM). Memory-sensitive caches.
    //    WEAK    : collected at the NEXT GC once only weakly reachable.
    //              Canonicalizing maps, WeakHashMap (keys are weak), listeners.
    //    PHANTOM : get() always returns null. Enqueued AFTER the object is
    //              finalized, for post-mortem cleanup (this is what Cleaner uses).
    //    A ReferenceQueue tells you WHEN a referent was collected.
    // =================================================================
    static void referenceTypes() throws InterruptedException {
        System.out.println("\n===== 4. REFERENCE TYPES =====");
        ReferenceQueue<Object> queue = new ReferenceQueue<>();

        Object strongObj = new byte[1024];
        WeakReference<Object> weak = new WeakReference<>(new byte[1024], queue);
        SoftReference<Object> soft = new SoftReference<>(new byte[1024]);
        PhantomReference<Object> phantom = new PhantomReference<>(new byte[1024], queue);

        waitForGc(weak);
        System.out.println("   strong  alive? " + (strongObj != null));
        System.out.println("   weak    alive? " + (weak.get() != null) + "   (collected at the first GC)");
        System.out.println("   soft    alive? " + (soft.get() != null) + "    (kept: memory isn't low)");
        System.out.println("   phantom get()  = " + phantom.get() + "    (always null by design)");
        int enqueued = 0;
        while (queue.remove(200) != null) enqueued++;
        System.out.println("   references enqueued after collection: " + enqueued + " (weak + phantom)");

        // WeakHashMap: an entry disappears once its KEY is no longer strongly reachable
        WeakHashMap<Object, String> sessionMeta = new WeakHashMap<>();
        Object session = new Object();
        sessionMeta.put(session, "metadata");
        session = null;
        for (int i = 0; i < 20 && !sessionMeta.isEmpty(); i++) { System.gc(); Thread.sleep(20); }
        System.out.println("   WeakHashMap size after key dropped: " + sessionMeta.size());
    }

    // =================================================================
    // 5. finalize() is deprecated (unpredictable, slow, can resurrect objects).
    //    Use try-with-resources for deterministic cleanup, and Cleaner as a
    //    SAFETY NET in case close() is forgotten.
    // =================================================================
    static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();

    static class NativeBuffer implements AutoCloseable {
        // The cleanup state must NOT reference the NativeBuffer itself (or it never becomes unreachable)
        private record State(String name) implements Runnable {
            public void run() { System.out.println("   cleaner released native memory for " + name); }
        }
        private final java.lang.ref.Cleaner.Cleanable cleanable;

        NativeBuffer(String name) { cleanable = CLEANER.register(this, new State(name)); }

        @Override
        public void close() { cleanable.clean(); }   // runs the action at most once
    }

    static void cleanerInsteadOfFinalize() throws InterruptedException {
        System.out.println("\n===== 5. CLEANER (replacement for finalize) =====");
        try (NativeBuffer b = new NativeBuffer("buffer-1")) {
            System.out.println("   using buffer-1");
        }                                                    // deterministic cleanup
        new NativeBuffer("buffer-2-forgotten");              // never closed ON PURPOSE (IDE warns: correct!)...
        for (int i = 0; i < 10; i++) { System.gc(); Thread.sleep(30); }   // ...the GC + Cleaner rescue it (eventually)
    }

    // =================================================================
    // 6. System.gc() is just a REQUEST (ignored with -XX:+DisableExplicitGC).
    //    Don't call it in production code. A full STW GC can hurt latency.
    // =================================================================
    static void gcIsOnlyAHint() {
        System.out.println("\n===== 6. System.gc() IS A HINT =====");
        System.out.println("   used to trigger collections for this DEMO only; never rely on it in real code");
    }

    // ---------- helpers ----------
    static Map<String, Long> gcCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) m.put(gc.getName(), gc.getCollectionCount());
        return m;
    }

    static void waitForGc(Reference<?> ref) throws InterruptedException {
        for (int i = 0; i < 20 && ref.get() != null; i++) { System.gc(); Thread.sleep(20); }
    }
}
