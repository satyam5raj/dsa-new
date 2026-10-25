/*
 * ============================================================================
 *  ADVANCE 03 : SINGLE-THREADED CPU  ⭐ bonus   LeetCode #1834
 *  https://leetcode.com/problems/single-threaded-cpu/
 *  Pattern: SORT by arrival + MIN-HEAP of available tasks + a moving clock
 * ============================================================================
 *
 *  PROBLEM
 *    tasks[i] = [enqueueTime, processingTime]. One CPU. Whenever it is idle it picks,
 *    among tasks that have ARRIVED, the one with the shortest processing time (ties:
 *    smallest index), and runs it to completion. If nothing has arrived, it waits.
 *    Return the order in which tasks are processed.
 *      [[1,2],[2,4],[3,2],[4,1]]  ->  [0,2,3,1]
 *
 *  ---------------------------------------------------------------------------
 *  TWO QUESTIONS EVERY STEP:
 *    1. "Which tasks have arrived by now?"  -> tasks SORTED by enqueue time + a pointer:
 *       move the pointer forward while the next task's enqueueTime <= time.
 *    2. "Among those, which is shortest?"   -> a MIN-HEAP ordered by (processingTime, index).
 *  If the heap is empty the CPU is idle: jump the clock to the next arrival.
 *
 *  DRY RUN [[1,2],[2,4],[3,2],[4,1]]   (sorted by arrival: 0,1,2,3)
 *    time 0: heap empty -> jump to 1; add task0          run 0 (2)  -> time 3
 *    time 3: add task1 (2<=3), task2 (3<=3); heap {2:(2), 1:(4)}   run 2 -> time 5
 *    time 5: add task3 (4<=5); heap {3:(1), 1:(4)}                 run 3 -> time 6
 *    time 6: heap {1}                                              run 1 -> time 10
 *    order [0,2,3,1]
 *
 *  The clock is a long: up to 10^5 tasks x 10^9 processing time overflows an int.
 *
 *  COMPLEXITY   O(n log n) (sort + heap)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A03_SingleThreadedCPU {

    // ---------------- Approach 1: rescan all tasks at every step (the referee) ----------------
    static int[] getOrderBrute(int[][] tasks) {
        int n = tasks.length;
        boolean[] done = new boolean[n];
        int[] order = new int[n];
        long time = 0;
        for (int k = 0; k < n; k++) {
            int best = -1;
            for (int i = 0; i < n; i++)
                if (!done[i] && tasks[i][0] <= time && (best == -1 || tasks[i][1] < tasks[best][1])) best = i;
            if (best == -1) {                                                // idle: jump to the earliest arrival
                long next = Long.MAX_VALUE;
                for (int i = 0; i < n; i++) if (!done[i]) next = Math.min(next, tasks[i][0]);
                time = next;
                k--;                                                         // retry this slot at the new time
                continue;
            }
            done[best] = true;
            order[k] = best;
            time += tasks[best][1];
        }
        return order;
    }

    // ---------------- Approach 2: sort + min-heap (main solution) ----------------
    static int[] getOrder(int[][] tasks) {
        int n = tasks.length;
        Integer[] byArrival = new Integer[n];
        for (int i = 0; i < n; i++) byArrival[i] = i;
        Arrays.sort(byArrival, Comparator.comparingInt(i -> tasks[i][0]));

        PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) ->
                tasks[a][1] != tasks[b][1] ? Integer.compare(tasks[a][1], tasks[b][1]) : Integer.compare(a, b));

        int[] order = new int[n];
        int k = 0, next = 0;
        long time = 0;
        while (k < n) {
            if (ready.isEmpty() && time < tasks[byArrival[next]][0]) time = tasks[byArrival[next]][0];   // idle: jump
            while (next < n && tasks[byArrival[next]][0] <= time) ready.add(byArrival[next++]);
            int t = ready.poll();
            order[k++] = t;
            time += tasks[t][1];
        }
        return order;
    }

    public static void main(String[] args) {
        check("example 1", "[0, 2, 3, 1]", getOrder(new int[][]{{1, 2}, {2, 4}, {3, 2}, {4, 1}}));
        check("all arrive together", "[4, 3, 2, 0, 1]", getOrder(new int[][]{{7, 10}, {7, 12}, {7, 5}, {7, 4}, {7, 2}}));
        check("idle gap in the middle", "[0, 1]", getOrder(new int[][]{{1, 1}, {100, 1}}));
        check("huge times (long clock)", "[0, 1, 2]", getOrder(new int[][]{{1, 1_000_000_000}, {2, 1_000_000_000}, {3, 1_000_000_000}}));

        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 1 + rnd.nextInt(12);
            int[][] tasks = new int[n][2];
            for (int[] task : tasks) { task[0] = 1 + rnd.nextInt(20); task[1] = 1 + rnd.nextInt(6); }
            ok &= Arrays.equals(getOrder(tasks), getOrderBrute(tasks));
        }
        check("2,000 random task sets vs rescanning", true, ok);
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
