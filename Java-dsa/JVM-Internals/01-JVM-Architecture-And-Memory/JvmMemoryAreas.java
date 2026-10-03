/*
 * ============================================================================
 *  TOPIC 1 : JVM ARCHITECTURE & RUNTIME MEMORY AREAS
 * ============================================================================
 *
 *  Run:  java JvmMemoryAreas.java
 *  Try:  java -Xss256k JvmMemoryAreas.java     (smaller stacks -> shallower recursion)
 *        java -Xmx64m  JvmMemoryAreas.java     (smaller heap)
 *
 *  FROM SOURCE TO RUNNING CODE
 *     Hello.java --javac--> Hello.class (BYTECODE: platform-independent)
 *                --java---> JVM: ClassLoader -> Bytecode Verifier -> Execution Engine
 *  "Write once, run anywhere": the .class file is the same everywhere; each
 *  OS/CPU has its own JVM that runs it.
 *
 *  JVM ARCHITECTURE
 *  +-----------------------------------------------------------------------+
 *  |  CLASS LOADER SUBSYSTEM  (load -> link -> initialize)  [topic 3]      |
 *  +-----------------------------------------------------------------------+
 *  |  RUNTIME DATA AREAS                                                   |
 *  |   Shared by ALL threads:            Per THREAD:                       |
 *  |    - Heap (objects)                  - JVM Stack (frames)             |
 *  |    - Method Area / Metaspace         - PC Register                    |
 *  |      (class metadata, statics*)      - Native Method Stack            |
 *  |    - Code Cache (JIT-compiled code)                                   |
 *  +-----------------------------------------------------------------------+
 *  |  EXECUTION ENGINE: Interpreter + JIT compiler (C1/C2) [topic 4]       |
 *  |                    + Garbage Collector [topic 2]                      |
 *  +-----------------------------------------------------------------------+
 *  |  JNI (Java Native Interface) <-> native libraries                     |
 *  +-----------------------------------------------------------------------+
 *
 *  1) HEAP  (-Xms initial, -Xmx max)
 *     All objects and arrays. Shared, so it needs thread-safety. Managed by the GC.
 *     Classic GENERATIONAL layout:
 *       YOUNG GEN  = Eden + Survivor S0 + Survivor S1
 *         new objects are born in Eden. A "minor GC" copies survivors to S0/S1;
 *         each survival increments the object's AGE.
 *       OLD GEN (Tenured)
 *         objects older than the tenuring threshold (default up to 15) are
 *         PROMOTED here. Collected by a "major/mixed/full GC" (more expensive).
 *     (G1 splits the heap into equal REGIONS, each labelled eden/survivor/old/
 *      humongous. ZGC is region-based too, and generational since Java 21.)
 *     Since Java 7, the STRING POOL and STATIC fields live in the heap.
 *
 *  2) METASPACE (Java 8+, replaced PermGen)
 *     Class metadata: class structures, method bytecode, constant pools.
 *     Lives in NATIVE memory (outside the heap), grows automatically.
 *     Cap it with -XX:MaxMetaspaceSize. Leaks: classloader leaks (redeploys,
 *     dynamic proxies) -> "OutOfMemoryError: Metaspace".
 *     The "Method Area" is the JVM spec name; Metaspace is HotSpot's implementation.
 *
 *  3) JVM STACK (per thread, -Xss, default ~512KB-1MB)
 *     One FRAME per method call, holding: local variables (primitives and
 *     object REFERENCES), the operand stack, and the return address.
 *     Frame popped on return -> no GC needed. Too deep -> StackOverflowError.
 *
 *  4) PC REGISTER (per thread): address of the current bytecode instruction.
 *  5) NATIVE METHOD STACK (per thread): for native (C/C++) methods.
 *  6) CODE CACHE: machine code produced by the JIT (-XX:ReservedCodeCacheSize).
 *  7) DIRECT MEMORY: off-heap buffers (ByteBuffer.allocateDirect, Netty).
 *     Capped by -XX:MaxDirectMemorySize.
 *
 *  TOTAL PROCESS MEMORY = heap + metaspace + thread stacks x N + code cache
 *                         + direct buffers + GC overhead + ...
 *  => in a container, -Xmx must be well BELOW the container limit.
 */

import java.lang.management.*;
import java.util.*;

public class JvmMemoryAreas {

    static int depth = 0;
    static final List<String> STATIC_LIST = new ArrayList<>();   // static field: the list object lives in the heap

    public static void main(String[] args) {
        heapSummary();
        memoryPools();
        stackFrames();
        stackOverflow();
        whereThingsLive();
        objectSizeIntuition();
    }

    // =================================================================
    // 1. Heap numbers from the Runtime API
    // =================================================================
    static void heapSummary() {
        System.out.println("===== 1. HEAP SUMMARY =====");
        Runtime rt = Runtime.getRuntime();
        System.out.printf("   max heap   (-Xmx)       : %,d MB%n", rt.maxMemory() / mb());
        System.out.printf("   committed  (reserved now): %,d MB%n", rt.totalMemory() / mb());
        System.out.printf("   used                     : %,d MB%n", (rt.totalMemory() - rt.freeMemory()) / mb());
        System.out.println("   (default max heap = 1/4 of physical RAM, per MaxRAMPercentage=25)");
    }

    // =================================================================
    // 2. Every memory pool the JVM manages (heap AND non-heap)
    // =================================================================
    static void memoryPools() {
        System.out.println("\n===== 2. MEMORY POOLS (this JVM, this GC) =====");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage u = pool.getUsage();
            long max = u.getMax();
            System.out.printf("   %-8s %-32s used=%7d KB  max=%s%n",
                    pool.getType() == MemoryType.HEAP ? "HEAP" : "NON-HEAP",
                    pool.getName(), u.getUsed() / 1024,
                    max < 0 ? "unbounded" : (max / 1024) + " KB");
        }
        System.out.println("   G1 Eden/Survivor/Old = young + old generations; Metaspace + CodeHeap = non-heap");
    }

    // =================================================================
    // 3. Stack frames: each call gets its own copy of the locals
    // =================================================================
    static void stackFrames() {
        System.out.println("\n===== 3. STACK FRAMES =====");
        System.out.println("   factorial(4) frames:");
        factorial(4, "   ");
    }

    static int factorial(int n, String indent) {
        int local = n;                                      // lives in THIS frame only
        System.out.println(indent + "push frame: n=" + n + " (local=" + local + ")");
        int result = (n <= 1) ? 1 : n * factorial(n - 1, indent + "  ");
        System.out.println(indent + "pop  frame: n=" + n + " returns " + result);
        return result;
    }

    // =================================================================
    // 4. StackOverflowError: unbounded recursion fills the thread's stack.
    //    It's an Error (not an Exception). Catching it is OK for a demo,
    //    rarely in real code. Fix the recursion (or convert it to iteration).
    // =================================================================
    static void recurse() {
        depth++;
        recurse();
    }

    static void stackOverflow() {
        System.out.println("\n===== 4. StackOverflowError =====");
        try {
            recurse();
        } catch (StackOverflowError e) {
            System.out.println("   overflowed after ~" + String.format("%,d", depth) + " frames on thread 'main'");
        }

        // A thread can be created with a specific stack size
        depth = 0;
        Thread big = new Thread(null, () -> {
            try { recurse(); } catch (StackOverflowError e) {
                System.out.println("   with a 64 MB stack: ~" + String.format("%,d", depth) + " frames");
            }
        }, "big-stack", 64L * 1024 * 1024);
        big.start();
        try { big.join(); } catch (InterruptedException ignored) {}
        System.out.println("   DSA tip: deep DFS on 10^5+ nodes can overflow -> use an explicit stack or a bigger -Xss");
    }

    // =================================================================
    // 5. Where does each thing live? (annotated)
    // =================================================================
    static void whereThingsLive() {
        System.out.println("\n===== 5. WHERE THINGS LIVE =====");
        int count = 5;                       // STACK: primitive local, stored in the frame
        String literal = "hello";            // STACK: reference -> HEAP: pooled String object
        int[] arr = new int[3];              // STACK: reference -> HEAP: array object
        StringBuilder sb = new StringBuilder();   // STACK: reference -> HEAP: object (+ its internal byte[])
        STATIC_LIST.add("x");                // the static FIELD lives with the Class's data; the List object is in the HEAP
        // The bytecode of THIS METHOD and the class structure -> METASPACE
        // The JIT-compiled machine code of hot methods        -> CODE CACHE

        System.out.println("   int count          -> stack frame of whereThingsLive()");
        System.out.println("   \"hello\" literal    -> heap (string pool), reference on the stack");
        System.out.println("   new int[3]         -> heap, reference on the stack");
        System.out.println("   static list object -> heap, reachable from the class (a GC root)");
        System.out.println("   class metadata     -> metaspace (native memory)");
        // ESCAPE ANALYSIS: if the JIT proves an object never escapes a method,
        // it can skip the heap allocation entirely ("scalar replacement").
        System.out.println("   (" + count + arr.length + sb.length() + literal.length() + ")");
    }

    // =================================================================
    // 6. How big is an object? (64-bit JVM, compressed oops)
    //    OBJECT HEADER = mark word (8 bytes: hash, lock state, GC age)
    //                  + class pointer (4 bytes compressed)        = 12 bytes
    //    + fields, then padded to a multiple of 8 bytes.
    //    new Object()      = 12 -> 16 bytes
    //    Integer           = 12 + 4 (int) = 16 bytes   (vs 4 bytes for an int!)
    //    int[10]           = 16 (header + length) + 40 = 56 bytes
    //    So List<Integer> with 1M elements is ~16 MB of Integer objects + ~4 MB of references,
    //    vs 4 MB for an int[1_000_000]. Prefer primitive arrays in hot code.
    // =================================================================
    static void objectSizeIntuition() {
        System.out.println("\n===== 6. OBJECT SIZE INTUITION =====");
        int n = 2_000_000;
        long before = usedHeap();
        int[] primitives = new int[n];
        long afterPrimitive = usedHeap();
        List<Integer> boxed = new ArrayList<>(n);
        for (int i = 0; i < n; i++) boxed.add(i + 1_000);       // +1000 avoids the Integer cache
        long afterBoxed = usedHeap();
        System.out.printf("   int[%,d]        ~ %,d MB%n", n, (afterPrimitive - before) / mb());
        System.out.printf("   List<Integer> %,d ~ %,d MB  (headers + references + boxing)%n", n, (afterBoxed - afterPrimitive) / mb());
        System.out.println("   (rough numbers: measured via used heap, GC may run in between) "
                + primitives.length + boxed.size() / n);
    }

    static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    static long mb() { return 1024 * 1024; }
}
