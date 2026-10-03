/*
 * ============================================================================
 *  TOPIC 4 : JIT COMPILATION - interpreter vs compiled code, HotSpot,
 *            tiered compilation, inlining, escape analysis, deoptimization
 * ============================================================================
 *
 *  Run:  java JitDemo.java
 *  See what gets compiled:  java -XX:+PrintCompilation JitDemo.java | grep JitDemo
 *  See inlining decisions:  java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining JitDemo.java
 *  Interpreter only:        java -Xint ...   (very slow: shows how much the JIT does)
 *
 *  HOW JAVA CODE RUNS
 *    1. INTERPRETER: executes bytecode instruction by instruction. Starts
 *       instantly but is slow. While interpreting, it PROFILES: counts
 *       method calls and loop iterations, and records which types and
 *       branches actually occur.
 *    2. When a method gets HOT (enough invocations or loop back-edges), the
 *       JIT ("Just-In-Time") compiler turns it into optimized native machine
 *       code, stored in the CODE CACHE. Later calls jump straight to it.
 *    That's why it's called "HotSpot": it finds the hot spots and compiles those.
 *
 *  TIERED COMPILATION (default since Java 8)
 *    Level 0 : interpreter (with profiling)
 *    Level 1-3: C1 "client" compiler. Compiles FAST, does basic optimizations.
 *               Level 3 adds full profiling.
 *    Level 4 : C2 "server" compiler. Compiles SLOWLY, but produces highly
 *              optimized code using the collected profile.
 *    Typical path: 0 -> 3 -> 4. (GraalVM can replace C2; AOT/CDS/Leyden
 *    help with startup.)
 *    OSR (On-Stack Replacement): a long-running LOOP can switch to compiled
 *    code in the MIDDLE of its execution (shown as '%' in PrintCompilation).
 *
 *  KEY OPTIMIZATIONS
 *    - INLINING : copy a small callee's body into the caller (removes call
 *                 overhead, and ENABLES every other optimization). Small
 *                 getters/setters cost nothing.
 *    - ESCAPE ANALYSIS : if an object never escapes a method, the JIT can
 *                 avoid allocating it ("scalar replacement": fields become
 *                 locals) and remove its locks (lock elision).
 *    - DEAD CODE ELIMINATION, constant folding, loop unrolling, range-check
 *      elimination, VECTORIZATION (SIMD), intrinsics (Math, String,
 *      System.arraycopy mapped to special CPU instructions).
 *    - SPECULATIVE optimizations: "this call site has only ever seen
 *      ArrayList" -> inline ArrayList.get directly, behind a cheap type check.
 *
 *  DEOPTIMIZATION: if a speculation turns out WRONG (a new subtype appears,
 *  or a never-taken branch is taken), the JIT throws the compiled code away
 *  and goes back to the interpreter, then may recompile later ("made not entrant").
 *
 *  CALL SITE SHAPES:  monomorphic (1 type: inlined, fastest)
 *                     bimorphic   (2 types: still inlined, with a type check)
 *                     megamorphic (3+ types: virtual call through the vtable, no inlining)
 *
 *  WHY THIS MATTERS IN PRODUCTION
 *    - WARM-UP: the first requests after a deploy are slower. Use warm-up
 *      traffic, CDS (class data sharing), or CRaC/Leyden to cut it.
 *    - MICRO-BENCHMARKS LIE unless you warm up and prevent dead-code
 *      elimination. Use JMH (Java Microbenchmark Harness).
 */

import java.lang.management.*;

public class JitDemo {

    public static void main(String[] args) {
        warmUp();
        escapeAnalysis();
        callSiteShapes();
        benchmarkPitfall();
        jitInfo();
    }

    // =================================================================
    // 1. Warm-up: the same work gets much faster as the JIT kicks in
    // =================================================================
    static long work(int n) {                    // a hot method
        long acc = 0;
        for (int i = 0; i < n; i++) acc += (i * 31L) ^ (i >>> 3);
        return acc;
    }

    static void warmUp() {
        System.out.println("===== 1. WARM-UP (interpreter -> C1 -> C2) =====");
        long sink = 0;
        for (int round = 1; round <= 8; round++) {
            long start = System.nanoTime();
            for (int k = 0; k < 200; k++) sink += work(10_000);
            long us = (System.nanoTime() - start) / 1_000;
            System.out.printf("   round %d: %,7d us %s%n", round, us, round == 1 ? "<- mostly interpreted" : "");
        }
        System.out.println("   (sink=" + (sink & 0xF) + ") later rounds run JIT-compiled native code");
    }

    // =================================================================
    // 2. Escape analysis: a non-escaping object may never touch the heap
    //    We count young GCs while creating 100M tiny objects.
    // =================================================================
    record Point(int x, int y) {
        int lengthSquared() { return x * x + y * y; }
    }

    static long sumNonEscaping(int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) {
            Point p = new Point(i, i + 1);        // never leaves this method -> scalar-replaced
            sum += p.lengthSquared();
        }
        return sum;
    }

    static Point[] escapeHole = new Point[1024];

    static long sumEscaping(int n) {
        long sum = 0;
        for (int i = 0; i < n; i++) {
            Point p = new Point(i, i + 1);
            escapeHole[i & 1023] = p;             // stored in a static array -> ESCAPES -> real allocation
            sum += p.lengthSquared();
        }
        return sum;
    }

    static void escapeAnalysis() {
        System.out.println("\n===== 2. ESCAPE ANALYSIS =====");
        for (int i = 0; i < 20; i++) { sumNonEscaping(100_000); sumEscaping(100_000); }   // warm up both

        int n = 100_000_000;
        long gc0 = youngGcCount();
        long t0 = System.nanoTime();
        long a = sumNonEscaping(n);
        long t1 = System.nanoTime();
        long gc1 = youngGcCount();
        long b = sumEscaping(n);
        long t2 = System.nanoTime();
        long gc2 = youngGcCount();

        System.out.printf("   non-escaping: %4d ms, young GCs during run = %d  (objects never allocated on the heap)%n", (t1 - t0) / 1_000_000, gc1 - gc0);
        System.out.printf("   escaping    : %4d ms, young GCs during run = %d  (~%,d MB really allocated)%n", (t2 - t1) / 1_000_000, gc2 - gc1, (long) n * 16 / (1024 * 1024));
        System.out.println("   (check: " + (a == b) + ") Try -XX:-DoEscapeAnalysis to see the first case allocate too.");
    }

    // =================================================================
    // 3. Call-site shapes: monomorphic vs megamorphic
    // =================================================================
    interface Shape { double area(); }
    record Sq(double s) implements Shape { public double area() { return s * s; } }
    record Ci(double r) implements Shape { public double area() { return 3.14159 * r * r; } }
    record Tr(double b, double h) implements Shape { public double area() { return 0.5 * b * h; } }
    record Re(double w, double h) implements Shape { public double area() { return w * h; } }

    static double total(Shape[] shapes) {
        double sum = 0;
        for (Shape s : shapes) sum += s.area();  // THIS call site's profile decides inlining
        return sum;
    }

    static double timeTotal(Shape[] shapes) {
        double sink = 0;
        long start = System.nanoTime();
        for (int r = 0; r < 200; r++) sink += total(shapes);
        System.out.printf("%5d ms", (System.nanoTime() - start) / 1_000_000);
        return sink;
    }

    static void callSiteShapes() {
        System.out.println("\n===== 3. CALL-SITE SHAPES & DEOPTIMIZATION =====");
        int n = 200_000;
        Shape[] mono = new Shape[n], mega = new Shape[n];
        for (int i = 0; i < n; i++) {
            mono[i] = new Sq(i % 10);
            mega[i] = switch (i % 4) {
                case 0 -> new Sq(i % 10);
                case 1 -> new Ci(i % 10);
                case 2 -> new Tr(i % 10, 2);
                default -> new Re(i % 10, 3);
            };
        }
        double sink = 0;
        System.out.print("   monomorphic (only Sq)       : "); sink += timeTotal(mono); System.out.println("  <- inlined Sq.area()");
        System.out.print("   megamorphic (4 shape types) : "); sink += timeTotal(mega); System.out.println("  <- deopt, then virtual calls");
        System.out.print("   monomorphic again           : "); sink += timeTotal(mono); System.out.println("  <- profile now polluted: often a bit slower than the first run");
        System.out.println("   (sink " + ((long) sink & 1) + ") Run with -XX:+PrintCompilation to see 'made not entrant' = deoptimized");
    }

    // =================================================================
    // 4. Benchmark pitfall: dead code elimination
    //    If a result is never used, the JIT may delete the whole computation.
    // =================================================================
    static void benchmarkPitfall() {
        System.out.println("\n===== 4. BENCHMARK PITFALL: DEAD CODE ELIMINATION =====");
        for (int i = 0; i < 50; i++) { resultIgnored(); resultUsed(); }   // warm up

        long t0 = System.nanoTime();
        for (int i = 0; i < 1_000; i++) resultIgnored();
        long t1 = System.nanoTime();
        long sink = 0;
        for (int i = 0; i < 1_000; i++) sink += resultUsed();
        long t2 = System.nanoTime();
        System.out.printf("   result ignored : %,8d us  <- suspiciously fast: the work may have been optimized away%n", (t1 - t0) / 1000);
        System.out.printf("   result used    : %,8d us  (sink %d)%n", (t2 - t1) / 1000, sink & 1);
        System.out.println("   => use JMH: it handles warm-up, forks, and Blackhole.consume() to defeat DCE");
    }

    static void resultIgnored() {
        double x = 0;
        for (int i = 1; i < 10_000; i++) x += Math.sqrt(i);   // x is never used (the IDE warns too: intentional!)
    }

    static double resultUsed() {
        double x = 0;
        for (int i = 1; i < 10_000; i++) x += Math.sqrt(i);
        return x;
    }

    // =================================================================
    // 5. JIT info from the management API
    // =================================================================
    static void jitInfo() {
        System.out.println("\n===== 5. JIT INFO =====");
        CompilationMXBean jit = ManagementFactory.getCompilationMXBean();
        System.out.println("   compiler: " + jit.getName());
        System.out.println("   total time spent JIT-compiling so far: " + jit.getTotalCompilationTime() + " ms (on background compiler threads)");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().startsWith("CodeHeap")) {
                System.out.printf("   %-28s used %,d KB%n", pool.getName(), pool.getUsage().getUsed() / 1024);
            }
        }
    }

    static long youngGcCount() {
        long c = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans())
            if (gc.getName().contains("Young") || gc.getName().contains("Scavenge") || gc.getName().equals("Copy")) c += gc.getCollectionCount();
        return c;
    }
}
