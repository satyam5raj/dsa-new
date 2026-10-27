/*
 * ============================================================================
 *  OS 06 : MEMORY ALLOCATION AND GARBAGE COLLECTION
 * ============================================================================
 *  Run:  java GarbageCollection.java          (explanations: README.md in this folder)
 *
 *    1. a toy heap            objects, references, GC roots
 *    2. reference counting    frees instantly -- and leaks cycles
 *    3. mark-and-sweep        trace from roots; free the unreachable; fragmentation remains
 *    4. mark-compact          slide live objects together: no fragmentation
 *    5. copying collector     copy live objects to to-space; cost ~ live data, not heap size
 *    6. generational GC       most objects die young: minor GCs are cheap
 *    7. real JVM: weak refs   a WeakReference is cleared once the object is only weakly reachable
 *    8. real JVM: GC stats    collector names, counts and accumulated pause time (MXBeans)
 *    9. real JVM: a leak      a static cache that only grows; retained heap after full GCs
 *   10. allocated vs RSS      why the process can hold more memory than live objects need
 * ============================================================================
 */

import java.lang.management.*;
import java.lang.ref.WeakReference;
import java.util.*;

public class GarbageCollection {

    // ---------------------------------------------------------------------
    // 1. a toy heap
    // ---------------------------------------------------------------------
    static class Obj {
        final String name;
        final int size;
        final List<Obj> refs = new ArrayList<>();
        int refCount;
        boolean marked;
        int address;
        int age;
        Obj(String name, int size) { this.name = name; this.size = size; }
    }

    static class Heap {
        final List<Obj> objects = new ArrayList<>();
        final List<Obj> roots = new ArrayList<>();                     // stack variables, static fields, registers, JNI handles

        Obj alloc(String n, int size) { Obj o = new Obj(n, size); objects.add(o); return o; }

        void link(Obj from, Obj to) { from.refs.add(to); to.refCount++; }

        Set<String> reachable() {
            Set<String> seen = new TreeSet<>();
            Deque<Obj> stack = new ArrayDeque<>(roots);
            while (!stack.isEmpty()) { Obj o = stack.pop(); if (seen.add(o.name)) stack.addAll(o.refs); }
            return seen;
        }
    }

    static Heap sampleHeap() {
        Heap h = new Heap();
        Obj a = h.alloc("A", 10), b = h.alloc("B", 20), c = h.alloc("C", 10), d = h.alloc("D", 30), e = h.alloc("E", 10), f = h.alloc("F", 20);
        h.roots.add(a);
        h.link(a, b);
        h.link(b, c);
        h.link(d, e);                                                  // D, E: garbage
        h.link(e, d);                                                  // ...and a CYCLE (D <-> E)
        // F: garbage, no references at all
        return h;
    }

    // ---------------------------------------------------------------------
    // 2. reference counting
    // ---------------------------------------------------------------------
    static void referenceCounting() {
        section("1-2. Reference counting");
        Heap h = sampleHeap();
        Set<String> freedByRc = new TreeSet<>();
        for (Obj o : h.objects) if (o.refCount == 0 && !h.roots.contains(o)) freedByRc.add(o.name);
        check("objects with refcount 0 are freed immediately", "[F]", freedByRc.toString());
        check("...but the D <-> E cycle keeps both counts at 1 forever: a LEAK", "1/1", h.objects.get(3).refCount + "/" + h.objects.get(4).refCount);
        check("tracing from the roots shows only A, B, C are live", "[A, B, C]", h.reachable().toString());
        show("used by: CPython (plus a cycle detector), Swift/ObjC ARC, C++ shared_ptr (use weak_ptr to break cycles)");
    }

    // ---------------------------------------------------------------------
    // 3-4. mark-sweep and mark-compact
    // ---------------------------------------------------------------------
    static void markSweepCompact() {
        section("3-4. Mark-and-sweep and mark-compact");
        Heap h = sampleHeap();
        int addr = 0;
        for (Obj o : h.objects) { o.address = addr; addr += o.size; }     // A@0 B@10 C@30 D@40 E@70 F@80 (heap = 100)
        Set<String> live = h.reachable();
        for (Obj o : h.objects) o.marked = live.contains(o.name);          // MARK
        List<int[]> free = new ArrayList<>();
        for (Obj o : h.objects) if (!o.marked) free.add(new int[]{o.address, o.size});   // SWEEP: unmarked -> free list
        int totalFree = free.stream().mapToInt(x -> x[1]).sum();
        int[] merged = {0, 0};
        for (int[] x : free) if (merged[0] + merged[1] == x[0] || merged[1] == 0) { if (merged[1] == 0) merged[0] = x[0]; merged[1] += x[1]; }
        check("sweep frees D, E (the cycle!) and F: 60 bytes", 60, totalFree);
        check("here they happen to be adjacent (40..100) -> one 60-byte hole", "40+60", merged[0] + "+" + merged[1]);
        show("in general freed objects are scattered -> fragmentation; a 50-byte object may not fit although 60 are free");

        Heap g = sampleHeap();
        int a2 = 0;
        for (Obj o : g.objects) { o.address = a2; a2 += o.size; }
        g.objects.get(1).refs.clear();                                     // drop B -> C, so C (at 30) becomes garbage between live objects
        g.link(g.objects.get(0), g.objects.get(3));                        // and make D (at 40) live via A
        Set<String> live2 = g.reachable();
        int next = 0;
        StringBuilder layout = new StringBuilder();
        for (Obj o : g.objects) if (live2.contains(o.name)) { o.address = next; next += o.size; layout.append(o.name).append('@').append(o.address).append(' '); }   // COMPACT: slide down
        check("live set after the change", "[A, B, D, E]", live2.toString());
        check("compaction slides live objects together", "A@0 B@10 D@30 E@60", layout.toString().trim());
        check("...leaving ONE contiguous free region (bump-pointer allocation again)", "70..100", next + "..100");
    }

    // ---------------------------------------------------------------------
    // 5. copying collector
    // ---------------------------------------------------------------------
    static void copying() {
        section("5. Copying collector (semi-space)");
        Heap h = sampleHeap();
        Set<String> live = h.reachable();
        int copied = h.objects.stream().filter(o -> live.contains(o.name)).mapToInt(o -> o.size).sum();
        int heapUsed = h.objects.stream().mapToInt(o -> o.size).sum();
        check("only live objects are copied to to-space (A, B, C = 40 bytes)", 40, copied);
        show(String.format("work ~ live data (%d) not heap size (%d); garbage costs NOTHING; but half the space is reserved", copied, heapUsed));
        check("result is compact by construction", true, copied < heapUsed);
    }

    // ---------------------------------------------------------------------
    // 6. generational
    // ---------------------------------------------------------------------
    static void generational() {
        section("6. Generational GC: most objects die young");
        Random r = new Random(3);
        int allocated = 100_000, survivedYoung = 0;
        for (int i = 0; i < allocated; i++) if (r.nextDouble() < 0.05) survivedYoung++;    // 95% of objects are garbage by the next minor GC
        check("weak generational hypothesis: ~5% survive a minor GC", true, Math.abs(survivedYoung / (double) allocated - 0.05) < 0.005);
        double youngGcWork = survivedYoung, fullHeapWork = allocated;
        show(String.format("a minor GC copies only the %,d survivors instead of tracing %,d objects (%.0fx less work)", survivedYoung, allocated, fullHeapWork / youngGcWork));
        check("young collections are cheap and frequent; promoted survivors are collected rarely", true, fullHeapWork / youngGcWork > 15);
        show("needs a WRITE BARRIER + card table: old->young pointers must be found without scanning the old generation");
    }

    // ---------------------------------------------------------------------
    // 7-9. the real JVM
    // ---------------------------------------------------------------------
    static List<byte[]> CACHE = new ArrayList<>();                      // a classic leak: static, unbounded

    static long usedAfterGc() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(50); } catch (InterruptedException ignored) { } }
        MemoryUsage u = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return u.getUsed();
    }

    static void realJvm() throws Exception {
        section("7-9. The real JVM: weak references, GC statistics, a leak");
        Object strong = new byte[1024];
        WeakReference<Object> weak = new WeakReference<>(new byte[1024]);   // only weakly reachable from the start
        WeakReference<Object> held = new WeakReference<>(strong);
        for (int i = 0; i < 5 && weak.get() != null; i++) { System.gc(); Thread.sleep(20); }
        check("an object reachable only through a WeakReference is collected", true, weak.get() == null);
        check("one that is also strongly reachable survives", true, held.get() != null);
        if (strong.hashCode() == 42) System.out.print("");

        List<String> gcs = new ArrayList<>();
        long count = 0, timeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) { gcs.add(gc.getName()); count += gc.getCollectionCount(); timeMs += gc.getCollectionTime(); }
        show("collectors: " + gcs + ", collections so far " + count + ", total GC time " + timeMs + " ms");
        check("the JVM runs a generational collector (young + old/concurrent spaces)", true, gcs.size() >= 2);

        long before = usedAfterGc();
        for (int i = 0; i < 50; i++) CACHE.add(new byte[1024 * 1024]);    // 50 MB "cached" and never evicted
        long after = usedAfterGc();
        show(String.format("heap used after full GC: before %d MB, after filling the cache %d MB", before >> 20, after >> 20));
        check("retained heap grows by ~50 MB and SURVIVES full GCs: reachable garbage = a leak", true, (after - before) > 45L * 1024 * 1024);
        CACHE = null;
        long released = usedAfterGc();
        check("dropping the reference lets the GC reclaim it", true, released < after - 40L * 1024 * 1024);
    }

    // ---------------------------------------------------------------------
    // 10. allocated vs resident
    // ---------------------------------------------------------------------
    static void rss() {
        section("10. Live objects vs heap committed vs process RSS");
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        show(String.format("heap: used %d MB, committed %d MB, max %d MB; non-heap (metaspace, code cache) committed %d MB",
             heap.getUsed() >> 20, heap.getCommitted() >> 20, heap.getMax() >> 20, nonHeap.getCommitted() >> 20));
        check("committed >= used: the JVM keeps freed heap instead of returning it to the OS", true, heap.getCommitted() >= heap.getUsed());
        show("RSS = heap committed (touched) + metaspace + code cache + thread stacks + direct buffers + malloc arenas + mapped files");
        show("so RSS stays high after a GC; container limits must cover ALL of it, not just -Xmx");
    }

    public static void main(String[] args) throws Exception {
        referenceCounting();
        markSweepCompact();
        copying();
        generational();
        realJvm();
        rss();
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
    }
}
