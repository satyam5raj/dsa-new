/*
 * ============================================================================
 *  TOPIC 6 : JVM TUNING FLAGS FOR PRODUCTION
 * ============================================================================
 *
 *  Run:  java JvmFlagsDemo.java
 *  Run with a production-like flag set:   ./run-with-prod-flags.sh
 *  See ALL flags and their final values:  java -XX:+PrintFlagsFinal -version
 *  Flags of a running process:            jcmd <pid> VM.flags   /   jcmd <pid> VM.command_line
 *
 *  FLAG SYNTAX
 *    -Xmx / -Xms / -Xss           : "standard-ish" shortcuts
 *    -XX:+Flag / -XX:-Flag        : turn a boolean on / off
 *    -XX:Flag=value               : set a value
 *    -Xlog:...                    : unified logging (Java 9+)
 *
 *  ------------------------------------------------------------------------------------
 *   HEAP & MEMORY
 *  ------------------------------------------------------------------------------------
 *   -Xms2g -Xmx2g                 initial / max heap. Setting them EQUAL avoids resize
 *                                 pauses and makes memory use predictable (common for servers)
 *   -XX:MaxRAMPercentage=75       CONTAINERS: heap as a % of the container memory limit.
 *                                 Better than a hard -Xmx in Kubernetes. Leave ~25% for
 *                                 metaspace, thread stacks, direct buffers, code cache
 *   -XX:InitialRAMPercentage=50   same for the initial heap
 *   -Xss512k                      thread stack size (more threads vs deeper recursion)
 *   -XX:MaxMetaspaceSize=256m     cap class metadata (catches classloader leaks early)
 *   -XX:MaxDirectMemorySize=512m  cap off-heap NIO buffers
 *   -XX:+UseContainerSupport      ON by default (JDK 10+): respects cgroup CPU/memory limits
 *
 *  ------------------------------------------------------------------------------------
 *   GARBAGE COLLECTOR
 *  ------------------------------------------------------------------------------------
 *   -XX:+UseG1GC                  default on "server-class" machines
 *   -XX:MaxGCPauseMillis=200      G1 pause TARGET (a goal, not a guarantee)
 *   -XX:+UseZGC                   (Java 21: add -XX:+ZGenerational; default from 23)
 *   -XX:+UseParallelGC            throughput (batch jobs)
 *   -XX:+UseSerialGC              tiny containers (< 2 CPUs or < ~1.7 GB, chosen automatically there)
 *   -XX:ParallelGCThreads=N       STW GC worker threads
 *   -XX:ConcGCThreads=N           concurrent GC threads
 *   -XX:+UseStringDeduplication   G1/ZGC: share identical String byte[] arrays
 *   -XX:+DisableExplicitGC        ignore System.gc() calls from libraries
 *
 *  ------------------------------------------------------------------------------------
 *   OBSERVABILITY & FAILURE HANDLING  (turn these on in EVERY production service)
 *  ------------------------------------------------------------------------------------
 *   -Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
 *                                 rotating GC logs (near-zero overhead, invaluable afterwards)
 *   -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps
 *                                 heap dump at the moment of OOM, for post-mortem analysis
 *   -XX:+ExitOnOutOfMemoryError   die fast on OOM so Kubernetes restarts a clean pod
 *                                 (a half-dead JVM that keeps serving errors is worse)
 *   -XX:+CrashOnOutOfMemoryError  alternative: also writes an hs_err crash file
 *   -XX:StartFlightRecording=duration=0,filename=rec.jfr,settings=default
 *                                 continuous Java Flight Recorder (~1% overhead)
 *   -XX:ErrorFile=/logs/hs_err_%p.log   location of the JVM crash log
 *
 *  ------------------------------------------------------------------------------------
 *   JIT / STARTUP
 *  ------------------------------------------------------------------------------------
 *   -XX:ReservedCodeCacheSize=256m  bigger code cache for huge apps ("CodeCache is full" warning)
 *   -XX:TieredStopAtLevel=1         C1 only: faster startup, slower peak (dev tools, CLIs)
 *   -XX:SharedArchiveFile=app.jsa   AppCDS: faster startup by sharing pre-parsed classes
 *   -XX:+AlwaysPreTouch             touch all heap pages at startup (no page faults later)
 *
 *  TUNING METHOD (what to say in an interview)
 *    1. Define the GOAL first: throughput? p99 latency? memory footprint?
 *    2. MEASURE a baseline: GC logs, JFR, latency percentiles. Don't guess.
 *    3. Right-size the heap first (most "GC problems" are a heap that's too small,
 *       or a leak), then pick the collector that fits the goal.
 *    4. Change ONE thing at a time; re-measure under realistic load.
 *    5. Often the best GC tuning is ALLOCATING LESS (fewer temp objects,
 *       primitives instead of boxed values, streaming instead of loading everything).
 */

import java.lang.management.*;
import java.util.*;

public class JvmFlagsDemo {

    public static void main(String[] args) {
        runtimeInfo();
        effectiveFlags();
        containerAndCpu();
        recommendedFlags();
    }

    // =================================================================
    // 1. What JVM am I, and what arguments was I started with?
    // =================================================================
    static void runtimeInfo() {
        System.out.println("===== 1. RUNTIME INFO =====");
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        System.out.println("   JVM        : " + rt.getVmName() + " " + rt.getVmVersion());
        System.out.println("   vendor     : " + rt.getVmVendor());
        System.out.println("   pid        : " + ProcessHandle.current().pid());
        System.out.println("   uptime     : " + rt.getUptime() + " ms");
        System.out.println("   JVM args   : " + (rt.getInputArguments().isEmpty() ? "(none: all defaults)" : rt.getInputArguments()));
        String collectors = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName).reduce((a, b) -> a + ", " + b).orElse("?");
        System.out.println("   collectors : " + collectors);
    }

    // =================================================================
    // 2. Read the EFFECTIVE value of important flags (defaults + ergonomics + overrides)
    //    Same information as: java -XX:+PrintFlagsFinal -version | grep <Flag>
    // =================================================================
    static void effectiveFlags() {
        System.out.println("\n===== 2. EFFECTIVE FLAG VALUES =====");
        var hs = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
        List<String> flags = List.of(
                "InitialHeapSize", "MaxHeapSize", "MaxRAMPercentage", "UseContainerSupport",
                "UseG1GC", "UseZGC", "UseParallelGC", "UseSerialGC", "MaxGCPauseMillis",
                "ParallelGCThreads", "ConcGCThreads", "ThreadStackSize", "MaxMetaspaceSize",
                "ReservedCodeCacheSize", "TieredCompilation", "CICompilerCount",
                "HeapDumpOnOutOfMemoryError", "ExitOnOutOfMemoryError", "UseStringDeduplication");
        for (String name : flags) {
            try {
                var opt = hs.getVMOption(name);
                String value = opt.getValue();
                if (name.endsWith("HeapSize") || name.equals("ReservedCodeCacheSize") || name.equals("MaxMetaspaceSize")) {
                    long bytes = Long.parseLong(value);
                    value = bytes < 0 ? "unlimited" : human(bytes);   // -1 means "no limit"
                }
                System.out.printf("   %-28s = %-18s (origin: %s)%n", name, value, opt.getOrigin());
            } catch (IllegalArgumentException e) {
                System.out.printf("   %-28s = (not on this JVM/OS; e.g. container flags exist only on Linux)%n", name);
            }
        }
        System.out.println("   origin ERGONOMIC = the JVM picked it from your hardware; VM_CREATION/OTHER = set by you");
        System.out.println("   ThreadStackSize is in KB");
    }

    // =================================================================
    // 3. What hardware does the JVM THINK it has? (matters in containers)
    // =================================================================
    static void containerAndCpu() {
        System.out.println("\n===== 3. CPU & MEMORY AS SEEN BY THE JVM =====");
        System.out.println("   availableProcessors = " + Runtime.getRuntime().availableProcessors()
                + "  (in a container: derived from the CPU limit/quota)");
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            System.out.println("   total memory        = " + human(os.getTotalMemorySize())
                    + "  (in a container: the cgroup memory limit)");
        }
        System.out.println("   max heap            = " + human(Runtime.getRuntime().maxMemory())
                + "  (= MaxRAMPercentage of the above unless -Xmx is set)");
        System.out.println("   Thread pools, GC threads and JIT threads are all sized from these numbers,");
        System.out.println("   so an old JVM that ignores cgroups would oversize everything inside Docker.");
    }

    // =================================================================
    // 4. A sensible starting point for a containerized Spring Boot service
    // =================================================================
    static void recommendedFlags() {
        System.out.println("\n===== 4. A SENSIBLE PRODUCTION BASELINE (container, Java 21) =====");
        String flags = """
                java \\
                  -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=75 \\
                  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \\
                  -Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \\
                  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps \\
                  -XX:+ExitOnOutOfMemoryError \\
                  -XX:StartFlightRecording=disk=true,maxsize=256m,dumponexit=true,filename=/logs/app.jfr \\
                  -jar app.jar""";
        flags.lines().forEach(l -> System.out.println("   " + l));
        System.out.println("   Then MEASURE and adjust one flag at a time.");
    }

    static String human(long bytes) {
        if (bytes >= 1L << 30) return String.format("%.1f GB", bytes / (double) (1L << 30));
        if (bytes >= 1L << 20) return String.format("%.0f MB", bytes / (double) (1L << 20));
        if (bytes >= 1L << 10) return String.format("%.0f KB", bytes / (double) (1L << 10));
        return bytes + " B";
    }
}
