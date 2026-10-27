/*
 * ============================================================================
 *  OS 02 : PROCESSES AND THREADS
 * ============================================================================
 *  Run:  java ProcessesThreads.java          (explanations: README.md in this folder)
 *
 *    1. fork + exec + wait    start a real child process, read its output through a pipe, collect its exit code
 *    2. process tree          parent / child PIDs
 *    3. zombies and orphans   a child that exited but was not reaped; children of a dead parent (real)
 *    4. isolation             a child's memory is its own: changing it doesn't touch ours
 *    5. threads share memory  threads see each other's writes (and need synchronization -- chapter 04)
 *    6. thread failure        an uncaught exception kills the thread, not the process; System.exit kills all
 *    7. creation cost         new thread per task vs a thread pool vs virtual threads (real timings)
 *    8. stacks                each thread has its own stack; deep recursion -> StackOverflowError
 *    9. thread-local storage  per-thread variables
 * ============================================================================
 */

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ProcessesThreads {

    // ---------------------------------------------------------------------
    // 1-2. fork/exec/wait
    // ---------------------------------------------------------------------
    static void forkExec() throws Exception {
        section("1-2. Creating a process: fork + exec, pipes, wait");
        long myPid = ProcessHandle.current().pid();
        Process child = new ProcessBuilder("sh", "-c", "echo \"child pid=$$ parent=$PPID\"; exit 7").start();   // the JVM uses posix_spawn/fork+exec
        String out = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();   // read the child's stdout through a pipe
        int code = child.waitFor();                                        // like waitpid(): block until it exits, collect the status
        show("child said: " + out);
        check("the child's parent is us", "parent=" + myPid, out.substring(out.indexOf("parent=")));
        check("the child has a different PID", true, !out.contains("pid=" + myPid + " "));
        check("waitpid() collected the child's exit status", 7, code);
        Process running = new ProcessBuilder("sleep", "5").start();
        check("a running child appears in our process tree (ProcessHandle.children)", true, ProcessHandle.current().children().anyMatch(h -> h.pid() == running.pid()));
        running.destroy();
        running.waitFor();
    }

    // ---------------------------------------------------------------------
    // 3. zombies and orphans
    // ---------------------------------------------------------------------
    static void zombiesOrphans() throws Exception {
        section("3. Zombies and orphans (real)");
        // the shell starts 'sleep 0' in the background, then replaces itself with 'sleep 2' (which never calls wait) -> zombie
        Process parent = new ProcessBuilder("sh", "-c", "sleep 0 & echo $!; exec sleep 2").start();   // exec: the parent BECOMES 'sleep', which never wait()s
        BufferedReader r = new BufferedReader(new InputStreamReader(parent.getInputStream()));
        String childPid = r.readLine().trim();
        Thread.sleep(500);                                                 // the child has exited by now
        Process ps = new ProcessBuilder("ps", "-o", "stat=", "-p", childPid).start();
        String stat = new String(ps.getInputStream().readAllBytes()).trim();
        ps.waitFor();
        show("ps -o stat -p " + childPid + " -> '" + stat + "'");
        check("a child that exited but was not waited for is a ZOMBIE (state Z)", true, stat.startsWith("Z"));
        parent.waitFor();
        Thread.sleep(200);
        Process ps2 = new ProcessBuilder("ps", "-o", "stat=", "-p", childPid).start();
        String gone = new String(ps2.getInputStream().readAllBytes()).trim();
        ps2.waitFor();
        check("when the parent exits, init/launchd adopts and reaps it: the zombie disappears", "", gone);
        show("orphan = a child whose parent died first; it is re-parented to PID 1 (or a subreaper) which reaps it");
        show("in containers YOUR app may be PID 1: it must reap children (use tini / --init) or zombies accumulate");
    }

    // ---------------------------------------------------------------------
    // 4. process isolation
    // ---------------------------------------------------------------------
    static int counter = 100;

    static void isolation() throws Exception {
        section("4. Process isolation: separate address spaces");
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        File src = File.createTempFile("Child", ".java");
        try (Writer w = new FileWriter(src)) {
            w.write("public class Child { static int counter = 100; public static void main(String[] a) { counter = 999; System.out.print(counter); } }");
        }
        Process p = new ProcessBuilder(javaBin, src.getAbsolutePath()).start();
        String childSaw = new String(p.getInputStream().readAllBytes()).trim();
        p.waitFor();
        check("the child set ITS counter to 999", "999", childSaw);
        check("our counter (same name, same program text) is untouched", 100, counter);
        src.delete();
    }

    // ---------------------------------------------------------------------
    // 5-6. threads share memory; thread failure
    // ---------------------------------------------------------------------
    static void threadsShare() throws Exception {
        section("5-6. Threads share their process's memory; a thread's crash");
        int[] shared = {0};
        Thread t = new Thread(() -> shared[0] = 42);
        t.start();
        t.join();                                                          // join() also guarantees we SEE the write (happens-before)
        check("another thread's write to shared memory is visible after join()", 42, shared[0]);
        AtomicReference<String> died = new AtomicReference<>();
        Thread crasher = new Thread(() -> { throw new IllegalStateException("boom"); });
        crasher.setUncaughtExceptionHandler((th, ex) -> died.set(ex.getMessage()));
        crasher.start();
        crasher.join();
        check("an uncaught exception ends THAT thread (handler saw it)", "boom", died.get());
        check("the process and other threads keep running", true, Thread.currentThread().isAlive());
        show("but a native crash (SIGSEGV in JNI), OOM, or System.exit() in any thread takes down the WHOLE process");
    }

    // ---------------------------------------------------------------------
    // 7. creation cost: threads vs pool vs virtual threads
    // ---------------------------------------------------------------------
    static void creationCost() throws Exception {
        section("7. Thread creation overhead (real timings)");
        int tasks = 2000;
        Runnable work = () -> { long x = 0; for (int i = 0; i < 1000; i++) x += i; if (x < 0) System.out.print(""); };
        long t0 = System.nanoTime();
        for (int i = 0; i < tasks; i++) { Thread t = new Thread(work); t.start(); t.join(); }
        double perThread = (System.nanoTime() - t0) / 1e6;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        t0 = System.nanoTime();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < tasks; i++) fs.add(pool.submit(work));
        for (Future<?> f : fs) f.get();
        double pooled = (System.nanoTime() - t0) / 1e6;
        pool.shutdown();
        show(String.format("%d tiny tasks: new OS thread each %.1f ms, 4-thread pool %.1f ms", tasks, perThread, pooled));
        check("reusing threads from a pool beats creating one per task", true, pooled < perThread);

        int many = 10_000;
        AtomicInteger done = new AtomicInteger();
        t0 = System.nanoTime();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < many; i++) vt.submit(() -> { try { Thread.sleep(100); } catch (InterruptedException ignored) { } done.incrementAndGet(); });
        }
        double vtMs = (System.nanoTime() - t0) / 1e6;
        show(String.format("%,d virtual threads each sleeping 100 ms finished in %.0f ms total", many, vtMs));
        check("10,000 concurrent blocking tasks on a few carrier OS threads (JDK 21 virtual threads)", many, done.get());
        check("they ran concurrently (not 10,000 x 100 ms = 1000 s)", true, vtMs < 5000);
    }

    // ---------------------------------------------------------------------
    // 8. stacks
    // ---------------------------------------------------------------------
    static int depth;
    static void recurse() { depth++; recurse(); }

    static int maxDepth(long stackBytes) throws Exception {
        int[] result = new int[1];
        Thread t = new Thread(null, () -> { depth = 0; try { recurse(); } catch (StackOverflowError e) { result[0] = depth; } }, "deep", stackBytes);
        t.start();
        t.join();
        return result[0];
    }

    static void stacks() throws Exception {
        section("8. Each thread has its own stack");
        int small = maxDepth(256 * 1024), big = maxDepth(8 * 1024 * 1024);
        show(String.format("max recursion depth: 256 KB stack %,d frames, 8 MB stack %,d frames", small, big));
        check("infinite recursion -> StackOverflowError (the guard page below the stack is hit)", true, small > 0);
        check("a bigger stack allows deeper recursion", true, big > 4 * small);
        show("1,000 platform threads x 1 MB default stack = 1 GB of reserved virtual memory -> why thousands of OS threads hurt");
    }

    // ---------------------------------------------------------------------
    // 9. thread-local storage
    // ---------------------------------------------------------------------
    static final ThreadLocal<String> REQUEST_ID = ThreadLocal.withInitial(() -> "none");

    static void threadLocal() throws Exception {
        section("9. Thread-local storage");
        Map<String, String> seen = new ConcurrentHashMap<>();
        Thread a = new Thread(() -> { REQUEST_ID.set("req-A"); seen.put("A", REQUEST_ID.get()); }, "A");
        Thread b = new Thread(() -> { REQUEST_ID.set("req-B"); seen.put("B", REQUEST_ID.get()); }, "B");
        a.start(); b.start(); a.join(); b.join();
        check("each thread sees only its own value", "{A=req-A, B=req-B}", new TreeMap<>(seen).toString());
        check("the main thread never set it", "none", REQUEST_ID.get());
        show("pitfall: thread POOLS reuse threads -> always remove() thread-locals or the next request sees stale data");
    }

    public static void main(String[] args) throws Exception {
        forkExec();
        zombiesOrphans();
        isolation();
        threadsShare();
        creationCost();
        stacks();
        threadLocal();
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
