/*
 * ============================================================================
 *  MEDIUM 03 : TASK SCHEDULER                LeetCode #621
 *  https://leetcode.com/problems/task-scheduler/
 *  Pattern: GREEDY "most frequent first" (max-heap + cooldown queue) -> then a FORMULA
 * ============================================================================
 *
 *  PROBLEM
 *    Tasks A-Z, each takes 1 unit. Two SAME tasks must be at least n units apart
 *    (idle is allowed). Minimum total time?
 *      ["A","A","A","B","B","B"], n=2 -> 8    A B idle A B idle A B
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY: at each time unit, run the AVAILABLE task with the MOST remaining copies.
 *  Why most frequent? It's the bottleneck - it needs the most cooldown gaps, so
 *  starting it early leaves the most room to fill gaps with other tasks.
 *
 *  SIMULATION (heap + cooldown queue)
 *    heap  : remaining counts of tasks ready to run (max-heap)
 *    queue : {remaining count, time it becomes ready again}
 *    each tick: move ready tasks from queue to heap; run the top of the heap
 *    (count-1, back into the queue with readyAt = time + n + 1); else idle.
 *    O(total time * log 26).
 *
 *  FORMULA (O(n), the "aha"):
 *    Let maxFreq = the highest count, and countMax = how many tasks have it.
 *    Picture the most frequent task as the frame:
 *        A _ _ | A _ _ | A          (maxFreq - 1) blocks of size (n + 1), plus a last row
 *    time = (maxFreq - 1) * (n + 1) + countMax
 *    If there are MORE tasks than idle slots, nothing ever idles: time = tasks.length.
 *    -> answer = max(tasks.length, (maxFreq - 1) * (n + 1) + countMax)
 *
 *  DRY RUN AAABBB, n=2: maxFreq 3, countMax 2 -> (3-1)*3 + 2 = 8;  max(6, 8) = 8
 *
 *  COMPLEXITY   formula O(len), simulation O(time * log 26)
 * ============================================================================
 */

import java.util.*;

public class M03_TaskScheduler {

    static int leastInterval(char[] tasks, int n) {
        int[] freq = new int[26];
        for (char t : tasks) freq[t - 'A']++;
        int maxFreq = Arrays.stream(freq).max().getAsInt();
        int countMax = 0;
        for (int f : freq) if (f == maxFreq) countMax++;
        return Math.max(tasks.length, (maxFreq - 1) * (n + 1) + countMax);
    }

    static int simulate(char[] tasks, int n) {
        int[] freq = new int[26];
        for (char t : tasks) freq[t - 'A']++;
        PriorityQueue<Integer> ready = new PriorityQueue<>(Comparator.reverseOrder());
        for (int f : freq) if (f > 0) ready.offer(f);
        Queue<int[]> cooling = new ArrayDeque<>();                // {remaining, readyAt}
        int time = 0;
        while (!ready.isEmpty() || !cooling.isEmpty()) {
            if (!cooling.isEmpty() && cooling.peek()[1] <= time) ready.offer(cooling.poll()[0]);
            if (!ready.isEmpty()) {
                int remaining = ready.poll() - 1;                 // run the most frequent task
                if (remaining > 0) cooling.offer(new int[]{remaining, time + n + 1});
            }                                                     // else: idle this tick
            time++;
        }
        return time;
    }

    public static void main(String[] args) {
        check("AAABBB n=2", 8, leastInterval("AAABBB".toCharArray(), 2));
        check("ACABDB n=1 (no idle)", 6, leastInterval("ACABDB".toCharArray(), 1));
        check("AAABBB n=3", 10, leastInterval("AAABBB".toCharArray(), 3));
        check("AAABBB n=0", 6, leastInterval("AAABBB".toCharArray(), 0));
        check("simulation AAABBB n=2", 8, simulate("AAABBB".toCharArray(), 2));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            char[] tasks = new char[1 + rnd.nextInt(30)];
            int letters = 1 + rnd.nextInt(6);
            for (int i = 0; i < tasks.length; i++) tasks[i] = (char) ('A' + rnd.nextInt(letters));
            int n = rnd.nextInt(6);
            ok &= leastInterval(tasks, n) == simulate(tasks, n);
        }
        check("2,000 random: formula == greedy simulation", true, ok);
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
