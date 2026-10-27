/*
 * ============================================================================
 *  OS 12 : LINUX INTERNALS AND CONTAINERS  (reading the evidence)
 * ============================================================================
 *  Run:  java LinuxContainers.java          (explanations: README.md in this folder)
 *
 *    1. load average         runnable + uninterruptible tasks vs CPU count
 *    2. /proc/meminfo        "free" vs "available"; page cache is reclaimable
 *    3. /proc/<pid>/status   VmRSS vs VmSize, threads, context switches
 *    4. vmstat               r, b, si/so, cs, us/sy/wa/st -- what each column says
 *    5. cgroup CPU quota     why a container at 40% CPU still gets throttled (simulation)
 *    6. memory cgroup + OOM  memory.max, oom_score, what the OOM killer picks
 *    7. namespaces           what a container is: a process with private views
 *    8. exit codes + PID 1   137 / 143 / 139, signals to PID 1, graceful shutdown (real)
 *    9. dmesg / journalctl   finding the OOM kill in the kernel log
 *   10. ulimits              per-process limits (real)
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

public class LinuxContainers {

    // ---------------------------------------------------------------------
    // 1. load average
    // ---------------------------------------------------------------------
    static String interpretLoad(double load1, double load15, int cpus, double ioWaitPct) {
        double perCpu = load1 / cpus;
        String trend = load1 > load15 * 1.5 ? "rising" : load1 < load15 / 1.5 ? "falling" : "steady";
        if (perCpu > 1 && ioWaitPct > 20) return "overloaded (" + trend + "), many tasks in D state: I/O-bound";
        if (perCpu > 1) return "overloaded (" + trend + "): more runnable tasks than CPUs";
        return "ok (" + trend + ")";
    }

    static void loadAverage() {
        section("1. Load average");
        check("load 3.5 on 8 CPUs", "ok (steady)", interpretLoad(3.5, 3.2, 8, 1));
        check("load 24 on 8 CPUs, low iowait -> CPU saturation", "overloaded (rising): more runnable tasks than CPUs", interpretLoad(24, 6, 8, 2));
        check("load 24 on 8 CPUs, 60% iowait -> tasks stuck in disk/NFS waits", "overloaded (rising), many tasks in D state: I/O-bound", interpretLoad(24, 6, 8, 60));
        show("Linux load = exponentially damped average of tasks RUNNING + RUNNABLE + UNINTERRUPTIBLE (D) over 1, 5, 15 minutes");
    }

    // ---------------------------------------------------------------------
    // 2. /proc/meminfo
    // ---------------------------------------------------------------------
    static final String MEMINFO = """
        MemTotal:       16303340 kB
        MemFree:          412716 kB
        MemAvailable:    9873120 kB
        Buffers:          201332 kB
        Cached:          9120544 kB
        SwapTotal:       2097148 kB
        SwapFree:        2097148 kB
        Dirty:              1260 kB
        AnonPages:       5612780 kB
        """;

    static Map<String, Long> parseKb(String text) {
        Map<String, Long> m = new LinkedHashMap<>();
        Matcher mt = Pattern.compile("^(\\w+):\\s+(\\d+) kB$", Pattern.MULTILINE).matcher(text);
        while (mt.find()) m.put(mt.group(1), Long.parseLong(mt.group(2)));
        return m;
    }

    static void meminfo() {
        section("2. /proc/meminfo: 'free' is not 'available'");
        Map<String, Long> m = parseKb(MEMINFO);
        double total = m.get("MemTotal") / 1024.0 / 1024, free = m.get("MemFree") / 1024.0 / 1024, avail = m.get("MemAvailable") / 1024.0 / 1024, cache = m.get("Cached") / 1024.0 / 1024;
        show(String.format("total %.1f GB, free %.1f GB, available %.1f GB, page cache %.1f GB, swap used %d kB", total, free, avail, cache, m.get("SwapTotal") - m.get("SwapFree")));
        check("only 2.5% 'free' -- but ~60% 'available' (cache can be reclaimed)", "2.5% / 60.6%", String.format("%.1f%% / %.1f%%", 100 * free / total, 100 * avail / total));
        check("no swap in use: no memory pressure here", 0L, m.get("SwapTotal") - m.get("SwapFree"));
        show("alert on MemAvailable (or PSI memory pressure), never on MemFree -- Linux uses idle RAM as disk cache on purpose");
    }

    // ---------------------------------------------------------------------
    // 3. /proc/<pid>/status
    // ---------------------------------------------------------------------
    static final String STATUS = """
        Name:   java
        State:  S (sleeping)
        Pid:    4123
        PPid:   1
        Threads:        412
        VmPeak: 12482216 kB
        VmSize: 12481880 kB
        VmRSS:   2385344 kB
        voluntary_ctxt_switches:        1830233
        nonvoluntary_ctxt_switches:     928411
        """;

    static void procStatus() {
        section("3. /proc/<pid>/status");
        Map<String, Long> m = parseKb(STATUS);
        Matcher th = Pattern.compile("Threads:\\s+(\\d+)").matcher(STATUS);
        th.find();
        double vsz = m.get("VmSize") / 1024.0 / 1024, rss = m.get("VmRSS") / 1024.0 / 1024;
        show(String.format("VmSize %.1f GB (address space reserved), VmRSS %.2f GB (actually in RAM), threads %s", vsz, rss, th.group(1)));
        check("virtual size is ~5x RSS: reservations (heap max, thread stacks, mapped files) aren't memory USE", true, vsz / rss > 4);
        Matcher vol = Pattern.compile("^voluntary_ctxt_switches:\\s+(\\d+)", Pattern.MULTILINE).matcher(STATUS), inv = Pattern.compile("nonvoluntary_ctxt_switches:\\s+(\\d+)").matcher(STATUS);
        vol.find(); inv.find();
        double ratio = Long.parseLong(inv.group(1)) / (double) Long.parseLong(vol.group(1));
        check("involuntary/voluntary switches ~0.5: lots of PREEMPTION -> CPU contention or throttling", "0.51", String.format("%.2f", ratio));
        check("PPid 1: started by the container runtime / systemd (it IS the main process)", true, STATUS.contains("PPid:   1"));
    }

    // ---------------------------------------------------------------------
    // 4. vmstat
    // ---------------------------------------------------------------------
    static final String VMSTAT = """
        procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
         r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
        18  0      0 412716 201332 9120544  0    0     4    61 9012 48211 88 10  2  0  0
         2 14  81920  35120  10244  410512 812  930  9120  4410 2210  9811  4  6  5 85  0
        """;

    static String diagnoseVmstat(String line, int cpus) {
        String[] f = line.trim().split("\\s+");
        int r = Integer.parseInt(f[0]), b = Integer.parseInt(f[1]), si = Integer.parseInt(f[6]), so = Integer.parseInt(f[7]);
        int us = Integer.parseInt(f[12]), sy = Integer.parseInt(f[13]), wa = Integer.parseInt(f[15]);
        if (si + so > 0 && wa > 30) return "memory pressure: swapping, CPU waiting on I/O";
        if (r > 2 * cpus && us + sy > 90) return "CPU saturated: run queue " + r + " on " + cpus + " CPUs";
        if (b > cpus) return "many tasks blocked on I/O";
        return "healthy";
    }

    static void vmstat() {
        section("4. vmstat 1");
        String[] lines = VMSTAT.strip().split("\n");
        check("first sample: r=18 runnable on 8 CPUs, 98% busy", "CPU saturated: run queue 18 on 8 CPUs", diagnoseVmstat(lines[2], 8));
        check("second sample: swap in/out + 85% iowait", "memory pressure: swapping, CPU waiting on I/O", diagnoseVmstat(lines[3], 8));
        show("columns: r runnable, b blocked (D), si/so swap in/out per s, cs context switches/s, us/sy/id/wa/st user/system/idle/iowait/STEAL");
        show("st (steal) > 0 on a VM = the hypervisor gave your vCPU to someone else (noisy neighbour)");
    }

    // ---------------------------------------------------------------------
    // 5. cgroup CPU quota throttling
    // ---------------------------------------------------------------------
    static void cpuThrottling() {
        section("5. cgroup CPU quota: throttled at '40% average' usage");
        double quotaMs = 200, periodMs = 100;                           // limit = 2 CPUs (cpu.max = "200000 100000")
        int busyThreads = 8;                                            // e.g. GC threads + request burst on an 8-core node
        double burnRateMsPerMs = busyThreads;                           // 8 threads consume 8 ms of CPU per wall ms
        double runMs = quotaMs / burnRateMsPerMs;                       // quota gone after 25 ms
        double throttledMs = periodMs - runMs;
        show(String.format("limit 2 CPUs, burst of %d busy threads: quota used up after %.0f ms, then ALL threads frozen for %.0f ms of every 100 ms period", busyThreads, runMs, throttledMs));
        check("a 30 ms request that starts the burst takes > 100 ms of wall time", true, 30 > runMs);
        double avgUtil = 0.8 / 2;                                       // averaged over a minute the container uses 0.8 of its 2 CPUs
        check("dashboards show 40% average CPU, yet nr_throttled keeps climbing", "40%", String.format("%.0f%%", avgUtil * 100));
        show("check: cat /sys/fs/cgroup/cpu.stat (nr_throttled, throttled_usec); fixes: raise/remove CPU limits, cap thread pools to the limit,");
        show("        -XX:ActiveProcessorCount / container-aware runtimes, GOMAXPROCS = limit, use CPU requests (shares) for fairness instead");
    }

    // ---------------------------------------------------------------------
    // 6. memory cgroup + OOM killer
    // ---------------------------------------------------------------------
    record Proc(String name, long rssMb, int oomScoreAdj) { }

    static void oom() {
        section("6. Memory cgroup and the OOM killer");
        long limitMb = 2048;
        long heapMb = 1536, metaspace = 180, threads = 250, directBuffers = 200, codeCache = 60;
        long total = heapMb + metaspace + threads + directBuffers + codeCache;
        show(String.format("container limit %d MB; JVM: heap %d + metaspace %d + thread stacks %d + direct buffers %d + code cache %d = %d MB", limitMb, heapMb, metaspace, threads, directBuffers, codeCache, total));
        check("-Xmx1536m in a 2 GB container still exceeds the limit -> OOMKilled (exit 137)", true, total > limitMb);
        // oom_score ~ (share of memory used) x 1000 + oom_score_adj; -1000 = never kill
        java.util.function.Function<List<Proc>, String> victim = ps -> ps.stream()
            .max(Comparator.comparingLong(p -> p.oomScoreAdj() == -1000 ? Long.MIN_VALUE : p.rssMb() * 1000 / 4096 + p.oomScoreAdj())).get().name();
        List<Proc> plain = List.of(new Proc("sshd", 12, -1000), new Proc("app-java", 1900, 0), new Proc("log-agent", 300, 0), new Proc("sidecar", 120, 0));
        List<Proc> tuned = List.of(new Proc("sshd", 12, -1000), new Proc("app-java", 1900, 0), new Proc("log-agent", 300, 0), new Proc("sidecar", 120, 500));
        check("by default the biggest memory user is the victim (app-java: 1900/4096 -> score 463)", "app-java", victim.apply(plain));
        check("oom_score_adj +500 makes the small sidecar die FIRST (29 + 500 = 529 > 463)", "sidecar", victim.apply(tuned));
        List<Proc> onlySshd = List.of(new Proc("sshd", 3000, -1000), new Proc("tiny", 10, 0));
        check("oom_score_adj = -1000 makes a process immune, however big (sshd, kubelet)", "tiny", victim.apply(onlySshd));
    }

    // ---------------------------------------------------------------------
    // 7. namespaces
    // ---------------------------------------------------------------------
    static void namespaces() {
        section("7. Namespaces: a container is a process with private views");
        Map<String, String> ns = new LinkedHashMap<>();
        ns.put("pid", "own PID numbering: the app sees itself as PID 1");
        ns.put("net", "own interfaces, IPs, routes, ports, iptables");
        ns.put("mnt", "own mount table / root filesystem (the image)");
        ns.put("uts", "own hostname");
        ns.put("ipc", "own System V IPC / POSIX message queues");
        ns.put("user", "UID 0 inside can map to an unprivileged UID outside");
        ns.put("cgroup", "own view of the cgroup hierarchy");
        ns.forEach((k, v) -> show(String.format("%-7s %s", k, v)));
        check("7 namespace types (plus 'time' since Linux 5.6)", 7, ns.size());
        show("namespaces = what you can SEE; cgroups = how much you can USE; seccomp/capabilities/LSM = what you may DO");
        show("all containers share ONE kernel -> a kernel exploit escapes; VMs / gVisor / Firecracker add a stronger boundary");
    }

    // ---------------------------------------------------------------------
    // 8. exit codes and PID 1 (real)
    // ---------------------------------------------------------------------
    static String explainExit(int code) {
        if (code == 0) return "success";
        if (code == 137) return "SIGKILL: OOM-killed or killed after the grace period";
        if (code == 143) return "SIGTERM: asked to stop (deploy, scale-down, docker stop)";
        if (code == 139) return "SIGSEGV: native crash";
        if (code > 128) return "killed by signal " + (code - 128);
        return "application error " + code;
    }

    static void exitCodes() throws Exception {
        section("8. Exit codes and signals in containers (real child processes)");
        Process p = new ProcessBuilder("sh", "-c", "kill -KILL $$").start();
        int k = p.waitFor();
        check("137 = 128 + 9", "SIGKILL: OOM-killed or killed after the grace period", explainExit(k));
        check("143 = 128 + 15", "SIGTERM: asked to stop (deploy, scale-down, docker stop)", explainExit(143));
        check("1 = the app chose to exit with an error", "application error 1", explainExit(1));
        Process pid1 = new ProcessBuilder("sh", "-c", "trap 'exit 0' TERM; while true; do sleep 0.05; done").start();
        Thread.sleep(200);
        new ProcessBuilder("kill", "-TERM", String.valueOf(pid1.pid())).start().waitFor();
        boolean exited = pid1.waitFor(3, TimeUnit.SECONDS);
        check("an entrypoint that HANDLES SIGTERM shuts down promptly with exit 0", "true 0", exited + " " + (exited ? pid1.exitValue() : -1));
        show("PID 1 in a container gets NO default signal handlers: if it doesn't trap SIGTERM, 'docker stop' waits 10 s then SIGKILLs (137)");
        show("'sh -c java ...' as entrypoint: the shell is PID 1 and may not forward SIGTERM to java -> use exec, or tini/--init");
    }

    // ---------------------------------------------------------------------
    // 9. kernel log
    // ---------------------------------------------------------------------
    static final String DMESG = """
        [812345.112] Memory cgroup out of memory: Killed process 4123 (java) total-vm:12481880kB, anon-rss:2091520kB, file-rss:0kB, oom_score_adj:0
        [812345.115] oom_reaper: reaped process 4123 (java), now anon-rss:0kB
        """;

    static void kernelLog() {
        section("9. Finding the OOM kill: dmesg / journalctl -k");
        Matcher m = Pattern.compile("Killed process (\\d+) \\((\\w+)\\).*anon-rss:(\\d+)kB").matcher(DMESG);
        m.find();
        check("the kernel names the victim and its memory at death", "pid 4123 java 2042 MB", "pid " + m.group(1) + " " + m.group(2) + " " + Long.parseLong(m.group(3)) / 1024 + " MB");
        check("'Memory cgroup out of memory' = the CONTAINER limit, not the whole host", true, DMESG.contains("Memory cgroup out of memory"));
        show("Kubernetes: kubectl describe pod -> Last State: Terminated, Reason: OOMKilled, Exit Code: 137");
    }

    // ---------------------------------------------------------------------
    // 10. ulimits
    // ---------------------------------------------------------------------
    static void ulimits() throws Exception {
        section("10. Per-process limits (real)");
        Process p = new ProcessBuilder("sh", "-c", "ulimit -n; ulimit -u").start();
        String[] out = new String(p.getInputStream().readAllBytes()).trim().split("\\s+");
        p.waitFor();
        show("this machine: max open files (ulimit -n) = " + out[0] + ", max user processes (ulimit -u) = " + out[1]);
        check("limits are visible to every process", true, out.length == 2);
        show("servers raise nofile to 65k-1M (each connection = a descriptor); systemd: LimitNOFILE=, Docker: --ulimit nofile=");
    }

    public static void main(String[] args) throws Exception {
        loadAverage();
        meminfo();
        procStatus();
        vmstat();
        cpuThrottling();
        oom();
        namespaces();
        exitCodes();
        kernelLog();
        ulimits();
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
