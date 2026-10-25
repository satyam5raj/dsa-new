/*
 * ============================================================================
 *  ADVANCE 04 : PROCESS TASKS USING SERVERS  ⭐ bonus   LeetCode #1882
 *  https://leetcode.com/problems/process-tasks-using-servers/
 *  Pattern: TWO HEAPS - free servers (by weight, index) + busy servers (by free time)
 * ============================================================================
 *
 *  PROBLEM
 *    servers[i] = weight of server i. tasks[j] = how long task j takes; task j arrives at
 *    second j. Tasks are assigned IN ORDER. A task goes to the free server with the
 *    smallest weight (ties: smallest index). If no server is free, the task waits until
 *    one frees up (and tasks still go out in order). Return ans[j] = server of task j.
 *      servers [3,3,2], tasks [1,2,3,2,1,2]  ->  [2,2,0,2,1,2]
 *
 *  ---------------------------------------------------------------------------
 *  TWO DIFFERENT "SMALLEST" QUESTIONS -> TWO HEAPS
 *    free: which free server is best?        min-heap by (weight, index)
 *    busy: which busy server frees first?    min-heap by (freeTime, ...)
 *  For task j at time = max(time, j):
 *    1. move every busy server with freeTime <= time back to free
 *    2. if free is still empty, jump time to busy.peek().freeTime and move again
 *    3. take the best free server; it becomes busy until time + tasks[j]
 *
 *  DRY RUN servers [3,3,2], tasks [1,2,3,2,1,2]
 *    j0 t0: free {s2(2), s0(3), s1(3)} -> s2, busy until 1
 *    j1 t1: s2 frees -> s2, busy until 3
 *    j2 t2: free {s0, s1} -> s0, busy until 5
 *    j3 t3: s2 frees -> s2, busy until 5
 *    j4 t4: free {s1} -> s1, busy until 5
 *    j5 t5: s0, s1, s2 all free at 5 -> s2 (lightest)
 *    answer [2,2,0,2,1,2]
 *
 *  COMPLEXITY   O((n + m) log n)   (n servers, m tasks)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A04_ProcessTasksUsingServers {

    // ---------------- Approach 1: scan every server for every task (the referee) ----------------
    static int[] assignTasksBrute(int[] servers, int[] tasks) {
        long[] freeAt = new long[servers.length];
        int[] ans = new int[tasks.length];
        long time = 0;
        for (int j = 0; j < tasks.length; j++) {
            time = Math.max(time, j);
            long earliest = Long.MAX_VALUE;
            for (long f : freeAt) earliest = Math.min(earliest, f);
            time = Math.max(time, earliest);                                 // wait if nobody is free
            int best = -1;
            for (int i = 0; i < servers.length; i++)
                if (freeAt[i] <= time && (best == -1 || servers[i] < servers[best])) best = i;
            ans[j] = best;
            freeAt[best] = time + tasks[j];
        }
        return ans;
    }

    // ---------------- Approach 2: two heaps (main solution) ----------------
    static int[] assignTasks(int[] servers, int[] tasks) {
        PriorityQueue<Integer> free = new PriorityQueue<>((a, b) ->
                servers[a] != servers[b] ? Integer.compare(servers[a], servers[b]) : Integer.compare(a, b));
        PriorityQueue<long[]> busy = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {freeTime, server}
        for (int i = 0; i < servers.length; i++) free.add(i);

        int[] ans = new int[tasks.length];
        long time = 0;
        for (int j = 0; j < tasks.length; j++) {
            time = Math.max(time, j);
            while (!busy.isEmpty() && busy.peek()[0] <= time) free.add((int) busy.poll()[1]);
            if (free.isEmpty()) {                                            // everyone busy: wait for the first one
                time = busy.peek()[0];
                while (!busy.isEmpty() && busy.peek()[0] <= time) free.add((int) busy.poll()[1]);
            }
            int s = free.poll();
            ans[j] = s;
            busy.add(new long[]{time + tasks[j], s});
        }
        return ans;
    }

    public static void main(String[] args) {
        check("example 1", "[2, 2, 0, 2, 1, 2]", assignTasks(new int[]{3, 3, 2}, new int[]{1, 2, 3, 2, 1, 2}));
        check("example 2", "[1, 4, 1, 4, 1, 3, 2]", assignTasks(new int[]{5, 1, 4, 3, 2}, new int[]{2, 1, 2, 4, 5, 2, 1}));
        check("one server, tasks queue up", "[0, 0, 0]", assignTasks(new int[]{7}, new int[]{5, 5, 5}));

        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 3000; t++) {
            int n = 1 + rnd.nextInt(5), m = 1 + rnd.nextInt(15);
            int[] servers = new int[n], tasks = new int[m];
            for (int i = 0; i < n; i++) servers[i] = 1 + rnd.nextInt(4);
            for (int j = 0; j < m; j++) tasks[j] = 1 + rnd.nextInt(6);
            ok &= Arrays.equals(assignTasks(servers, tasks), assignTasksBrute(servers, tasks));
        }
        check("3,000 random cases vs scanning servers", true, ok);
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
