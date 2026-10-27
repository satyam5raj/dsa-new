/*
 * ============================================================================
 *  OS 03 : CPU SCHEDULING  (a simulator for every classic algorithm)
 * ============================================================================
 *  Run:  java Scheduling.java          (explanations: README.md in this folder)
 *
 *    1. FCFS, SJF, SRTF     the textbook workload; waiting / turnaround / response times; convoy effect
 *    2. round robin         the quantum trade-off: responsiveness vs context switches
 *    3. priority            starvation, and aging as the fix
 *    4. MLFQ                interactive (I/O-bound) jobs float up, CPU hogs sink
 *    5. priority inversion  the Mars Pathfinder bug, and priority inheritance
 *    6. real oversubscription  more busy threads than cores -> each takes longer (real measurement)
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;

public class Scheduling {

    record Job(String id, int arrival, int burst, int priority) { }

    record Result(double avgWait, double avgTurnaround, double avgResponse, int switches, String gantt) { }

    /** Generic discrete-time simulator. 'pick' chooses the next job among ready ones; preemptive re-picks every tick. */
    static Result run(List<Job> jobs, java.util.function.BiFunction<List<Job>, Map<String, Integer>, Job> pick, boolean preemptive, int quantum) {
        Map<String, Integer> remaining = new HashMap<>(), finish = new HashMap<>(), firstRun = new HashMap<>();
        for (Job j : jobs) remaining.put(j.id(), j.burst());
        StringBuilder gantt = new StringBuilder();
        Deque<Job> rrQueue = new ArrayDeque<>();
        Set<String> enqueued = new HashSet<>();
        Job current = null;
        int sliceUsed = 0, switches = 0, t = 0;
        String last = null;
        while (finish.size() < jobs.size()) {
            for (Job j : jobs) if (j.arrival() == t && enqueued.add(j.id())) rrQueue.addLast(j);
            List<Job> ready = new ArrayList<>(rrQueue);
            if (current != null && remaining.get(current.id()) == 0) current = null;
            if (quantum > 0) {                                                   // round robin
                if (current != null && sliceUsed == quantum) { rrQueue.addLast(current); current = null; }
                if (current == null && !rrQueue.isEmpty()) { current = rrQueue.pollFirst(); sliceUsed = 0; }
            } else if (current == null || preemptive) {
                if (current != null) rrQueue.addLast(current);
                ready = new ArrayList<>(rrQueue);
                current = ready.isEmpty() ? null : pick.apply(ready, remaining);
                if (current != null) rrQueue.remove(current);
            }
            String now = current == null ? "-" : current.id();
            if (last != null && !now.equals(last) && !now.equals("-")) switches++;
            gantt.append(now);
            last = now;
            if (current != null) {
                firstRun.putIfAbsent(current.id(), t);
                remaining.merge(current.id(), -1, Integer::sum);
                sliceUsed++;
                if (remaining.get(current.id()) == 0) { finish.put(current.id(), t + 1); current = null; }
            }
            t++;
        }
        double w = 0, ta = 0, r = 0;
        for (Job j : jobs) {
            int turnaround = finish.get(j.id()) - j.arrival();
            ta += turnaround;
            w += turnaround - j.burst();
            r += firstRun.get(j.id()) - j.arrival();
        }
        int n = jobs.size();
        return new Result(w / n, ta / n, r / n, switches, gantt.toString());
    }

    static final List<Job> TEXTBOOK = List.of(new Job("1", 0, 8, 2), new Job("2", 1, 4, 1), new Job("3", 2, 9, 3), new Job("4", 3, 5, 2));

    static void classic() {
        section("1. FCFS, SJF, SRTF on the textbook workload (P1 0/8, P2 1/4, P3 2/9, P4 3/5)");
        Result fcfs = run(TEXTBOOK, (ready, rem) -> ready.stream().min(Comparator.comparingInt(Job::arrival)).get(), false, 0);
        Result sjf = run(TEXTBOOK, (ready, rem) -> ready.stream().min(Comparator.comparingInt(Job::burst)).get(), false, 0);
        Result srtf = run(TEXTBOOK, (ready, rem) -> ready.stream().min(Comparator.comparingInt(j -> rem.get(j.id()))).get(), true, 0);
        show("FCFS gantt " + fcfs.gantt());
        show("SJF  gantt " + sjf.gantt());
        show("SRTF gantt " + srtf.gantt());
        check("FCFS average waiting time (0+7+10+18)/4", "8.75", String.format("%.2f", fcfs.avgWait()));
        check("SJF (non-preemptive) average waiting time", "7.75", String.format("%.2f", sjf.avgWait()));
        check("SRTF (preemptive SJF) average waiting time: provably optimal", "6.50", String.format("%.2f", srtf.avgWait()));
        List<Job> convoy = List.of(new Job("L", 0, 100, 0), new Job("a", 0, 1, 0), new Job("b", 0, 1, 0), new Job("c", 0, 1, 0));   // all waiting, L first in line
        Result c1 = run(convoy, (ready, rem) -> ready.stream().min(Comparator.comparingInt(Job::arrival)).get(), false, 0);
        Result c2 = run(convoy, (ready, rem) -> ready.stream().min(Comparator.comparingInt(Job::burst)).get(), false, 0);
        show(String.format("convoy effect: one 100-unit job ahead of three 1-unit jobs: FCFS avg wait %.1f vs SJF %.1f", c1.avgWait(), c2.avgWait()));
        check("FCFS convoy: (0+100+101+102)/4 vs SJF (0+1+2+3)/4", "75.75 vs 1.50", String.format("%.2f vs %.2f", c1.avgWait(), c2.avgWait()));
    }

    static void roundRobin() {
        section("2. Round robin: the time-quantum trade-off");
        Map<Integer, Result> rr = new LinkedHashMap<>();
        for (int q : List.of(1, 4, 100)) rr.put(q, run(TEXTBOOK, null, true, q));
        rr.forEach((q, r) -> show(String.format("quantum %3d: avg response %.2f, avg wait %.2f, context switches %d   %s", q, r.avgResponse(), r.avgWait(), r.switches(), r.gantt())));
        check("small quantum: best response time (everyone starts quickly)", true, rr.get(1).avgResponse() < rr.get(100).avgResponse());
        check("...but far more context switches", true, rr.get(1).switches() > 3 * rr.get(4).switches());
        check("a huge quantum degenerates into FCFS", "8.75", String.format("%.2f", rr.get(100).avgWait()));
    }

    static void priorityAndAging() {
        section("3. Priority scheduling: starvation and aging");
        // a low-priority batch job vs a steady stream of high-priority jobs (lower number = higher priority)
        int lowWaitedNoAging = waitOfLowPriority(false), lowWaitedAging = waitOfLowPriority(true);
        show(String.format("low-priority job's wait over 200 ticks: without aging %s, with aging %d ticks", lowWaitedNoAging < 0 ? "never ran (starved)" : String.valueOf(lowWaitedNoAging), lowWaitedAging));
        check("without aging the low-priority job starves", -1, lowWaitedNoAging);
        check("aging (priority improves while waiting) guarantees it eventually runs", true, lowWaitedAging > 0 && lowWaitedAging < 200);
    }

    static int waitOfLowPriority(boolean aging) {
        double lowPrio = 10;
        int highBacklog = 0;
        for (int t = 0; t < 200; t++) {
            highBacklog += 1;                                                   // a new high-priority (prio 1) unit of work every tick
            double effective = aging ? lowPrio - 0.05 * t : lowPrio;             // aging: +1 priority level every 20 ticks of waiting
            if (highBacklog > 0 && 1 <= effective) highBacklog--;               // a high-priority job wins the CPU
            else return t;                                                      // the low-priority job finally runs
        }
        return -1;
    }

    static void mlfq() {
        section("4. Multilevel feedback queue: learn who is interactive");
        // Q0 (quantum 2) > Q1 (quantum 4) > Q2 (FCFS). Use the full quantum -> demoted. Block for I/O early -> stay high.
        int[] level = {0, 0};                                                   // [cpuHog, editor]
        String[] names = {"cpu-hog", "editor"};
        int[] quantum = {2, 4, Integer.MAX_VALUE};
        int[] burstBeforeIo = {Integer.MAX_VALUE, 1};                           // the editor runs 1 unit then waits for a keystroke
        for (int round = 0; round < 5; round++) {
            for (int p = 0; p < 2; p++) {
                int used = Math.min(quantum[level[p]], burstBeforeIo[p]);
                if (used == quantum[level[p]] && level[p] < 2) level[p]++;      // used the whole slice -> CPU-bound -> demote
            }
        }
        show("after 5 rounds: " + names[0] + " in queue " + level[0] + ", " + names[1] + " in queue " + level[1]);
        check("the CPU hog sinks to the lowest queue", 2, level[0]);
        check("the interactive editor stays in the top queue (snappy response)", 0, level[1]);
        show("periodic 'priority boost' moves everyone back to Q0 so sunk jobs can't starve forever");
    }

    static void priorityInversion() {
        section("5. Priority inversion (Mars Pathfinder, 1997)");
        int highFinishNoPI = invert(false), highFinishPI = invert(true);
        show(String.format("high-priority task finishes at t=%d without priority inheritance, t=%d with it", highFinishNoPI, highFinishPI));
        check("medium-priority work delays the HIGH task (inversion)", true, highFinishNoPI > highFinishPI + 5);
        check("with priority inheritance the lock holder runs at high priority and releases quickly", 6, highFinishPI);
    }

    /** L holds a lock H needs; M (medium) is CPU-bound. Returns the time H completes. */
    static int invert(boolean inheritance) {
        int lRemaining = 3, mRemaining = 10, hRemaining = 2;                   // L needs 3 more units in its critical section
        boolean lockHeldByL = true;
        int t = 0;
        // t=0: L running with the lock. t=1: H arrives and blocks on the lock. t=1: M arrives.
        t = 1;
        while (hRemaining > 0) {
            if (lockHeldByL) {
                boolean lRuns = inheritance || mRemaining == 0;                 // without inheritance M (higher than L) preempts L
                if (lRuns) { lRemaining--; if (lRemaining == 0) lockHeldByL = false; }
                else mRemaining--;
            } else hRemaining--;                                                // lock free: H runs
            t++;
        }
        return t;
    }

    static void oversubscription() throws Exception {
        section("6. Real measurement: more runnable threads than cores");
        int cores = Runtime.getRuntime().availableProcessors();
        double one = timeBusy(cores, 1), over = timeBusy(cores, 4);
        show(String.format("%d cores: %d busy threads -> %.0f ms each;  %d busy threads -> %.0f ms each", cores, cores, one, cores * 4, over));
        check("4x oversubscribed: each thread's work takes ~2-3x longer (time-slicing, not more CPU)", true, over > 1.5 * one);
    }

    static double timeBusy(int cores, int factor) throws Exception {
        int threads = cores * factor;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> fs = new ArrayList<>();
        for (int i = 0; i < threads; i++) fs.add(ex.submit(() -> {
            start.await();
            long t0 = System.nanoTime(), x = 0;
            for (long k = 0; k < 150_000_000L; k++) x += k ^ (k >>> 3);           // ~pure CPU work
            if (x == 42) System.out.print("");
            return System.nanoTime() - t0;
        }));
        start.countDown();
        double sum = 0;
        for (Future<Long> f : fs) sum += f.get() / 1e6;
        ex.shutdown();
        return sum / threads;
    }

    public static void main(String[] args) throws Exception {
        classic();
        roundRobin();
        priorityAndAging();
        mlfq();
        priorityInversion();
        oversubscription();
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
