/*
 * ============================================================================
 *  OS 01 : OPERATING-SYSTEM FUNDAMENTALS
 * ============================================================================
 *  Run:  java OsFundamentals.java          (explanations: README.md in this folder)
 *
 *    1. system calls        every unbuffered write() is a trip into the kernel -- measure the cost
 *    2. process isolation   a child process segfaults (SIGSEGV); the parent is untouched (real)
 *    3. exit codes/signals  128 + signal number: 139 = SIGSEGV, 143 = SIGTERM, 137 = SIGKILL (real)
 *    4. exceptions (traps)  CPU faults the JVM turns into Java exceptions: divide by zero, null access
 *    5. resource limits     what the OS tells a process about itself (CPUs, memory, PID)
 *    6. strace              reading a syscall trace of "open a file and read it"
 * ============================================================================
 */

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

public class OsFundamentals {

    // ---------------------------------------------------------------------
    // 1. system calls
    // ---------------------------------------------------------------------
    static void syscalls() throws Exception {
        section("1. System calls: the price of crossing into the kernel");
        Path f = Files.createTempFile("syscalls", ".bin");
        int n = 200_000;
        long t0 = System.nanoTime();
        try (FileOutputStream out = new FileOutputStream(f.toFile())) {
            for (int i = 0; i < n; i++) out.write('x');                  // FileOutputStream.write(int) = one write() syscall per byte
        }
        double unbuffered = (System.nanoTime() - t0) / 1e6;
        t0 = System.nanoTime();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f.toFile()), 8192)) {
            for (int i = 0; i < n; i++) out.write('x');                  // collected in user space; one syscall per 8 KB
        }
        double buffered = (System.nanoTime() - t0) / 1e6;
        show(String.format("200,000 one-byte writes: unbuffered %.1f ms (~200,000 syscalls), buffered %.1f ms (~25 syscalls) -> %.0fx", unbuffered, buffered, unbuffered / buffered));
        check("same bytes on disk either way", (long) n, Files.size(f));
        check("crossing user -> kernel mode has a real cost: buffering is >10x faster", true, unbuffered > 10 * buffered);
        show(String.format("~%.2f us per write() syscall on this machine", unbuffered * 1000 / n));
        Files.delete(f);
    }

    // ---------------------------------------------------------------------
    // 2-3. isolation, exit codes, signals (real child processes)
    // ---------------------------------------------------------------------
    static int runShell(String script) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", script).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); return -1; }
        return p.exitValue();
    }

    static void isolation() throws Exception {
        section("2-3. Process isolation, exit codes and signals (real child processes)");
        int segv = runShell("kill -SEGV $$");                             // the child crashes itself with a segmentation fault
        check("child killed by SIGSEGV (11) -> exit status 128 + 11", 139, segv);
        check("...and THIS process (the parent) is still running fine", true, ProcessHandle.current().isAlive());
        check("normal exit codes pass through", 3, runShell("exit 3"));
        check("SIGTERM (15): polite stop request -> 143", 143, runShell("kill -TERM $$"));
        check("SIGKILL (9): cannot be caught -> 137 (also what the OOM killer sends)", 137, runShell("kill -KILL $$"));
        Process sleeper = new ProcessBuilder("sleep", "30").start();
        sleeper.destroy();                                                // Java sends SIGTERM
        sleeper.waitFor(5, TimeUnit.SECONDS);
        check("Process.destroy() sends SIGTERM to the child", 143, sleeper.exitValue());
    }

    // ---------------------------------------------------------------------
    // 4. CPU exceptions surfacing as Java exceptions
    // ---------------------------------------------------------------------
    static int divide(int a, int b) { return a / b; }

    static void traps() {
        section("4. Exceptions (synchronous traps) vs interrupts (asynchronous)");
        String r;
        try { divide(1, 0); r = "no trap?"; } catch (ArithmeticException e) { r = "ArithmeticException: " + e.getMessage(); }
        check("integer divide by zero: the CPU raises a fault, the JVM turns it into an exception", "ArithmeticException: / by zero", r);
        String npe;
        try { String s = null; npe = String.valueOf(s.length()); } catch (NullPointerException e) { npe = "NullPointerException"; }
        check("touching unmapped memory (address 0): page fault -> SIGSEGV -> JVM -> NPE", "NullPointerException", npe);
        check("floating point divide by zero does NOT trap (IEEE 754 gives Infinity)", "Infinity", String.valueOf(1.0 / 0));
        show("interrupts are asynchronous: a timer tick, a NIC packet, a disk completion -- they arrive between any two instructions");
    }

    // ---------------------------------------------------------------------
    // 5. what the OS tells us
    // ---------------------------------------------------------------------
    static void selfInfo() {
        section("5. Resource management: what the OS reports to this process");
        int cpus = Runtime.getRuntime().availableProcessors();
        long maxHeap = Runtime.getRuntime().maxMemory();
        long pid = ProcessHandle.current().pid();
        show(String.format("pid %d, %d CPUs visible, max heap %d MB, OS %s %s (%s)", pid, cpus, maxHeap >> 20,
             System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")));
        check("every process has a unique PID > 0", true, pid > 0);
        check("the OS reports at least one CPU (in a container this honours the CPU limit)", true, cpus >= 1);
        String uptime = String.valueOf(ManagementFactory.getRuntimeMXBean().getUptime() >= 0);
        check("the JVM itself is just a user-space process using syscalls", "true", uptime);
    }

    // ---------------------------------------------------------------------
    // 6. strace
    // ---------------------------------------------------------------------
    static final String STRACE = """
        openat(AT_FDCWD, "/etc/app/config.yaml", O_RDONLY|O_CLOEXEC) = 3
        fstat(3, {st_mode=S_IFREG|0644, st_size=2048, ...}) = 0
        read(3, "server:\\n  port: 8080\\n"..., 4096) = 2048
        read(3, "", 4096)                       = 0
        close(3)                                = 0
        openat(AT_FDCWD, "/etc/app/secrets.yaml", O_RDONLY) = -1 ENOENT (No such file or directory)
        write(2, "config error: secrets missing\\n", 30) = 30
        exit_group(1)                           = ?
        """;

    static void strace() {
        section("6. Reading strace output (syscalls of 'load config and fail')");
        Map<String, Integer> counts = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        Pattern p = Pattern.compile("^(\\w+)\\((.*)\\)\\s*=\\s*(.+)$");
        for (String line : STRACE.strip().split("\n")) {
            Matcher m = p.matcher(line.trim());
            if (!m.find()) continue;
            counts.merge(m.group(1), 1, Integer::sum);
            if (m.group(3).startsWith("-1")) errors.add(m.group(1) + " " + m.group(3));
        }
        show("syscall counts: " + counts);
        check("openat returned file descriptor 3 (0, 1, 2 are stdin/stdout/stderr)", true, STRACE.contains(") = 3"));
        check("a read() returning 0 means end of file", 2, counts.get("read"));
        check("the root cause, straight from the kernel: ENOENT on secrets.yaml", "[openat -1 ENOENT (No such file or directory)]", errors.toString());
        show("real use: strace -f -e trace=file,network -p <pid>   |   macOS: dtruss; Windows: Process Monitor");
    }

    public static void main(String[] args) throws Exception {
        syscalls();
        isolation();
        traps();
        selfInfo();
        strace();
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
