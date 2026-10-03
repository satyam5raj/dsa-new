/*
 * ============================================================================
 *  MEDIUM 22 : TIME NEEDED TO INFORM ALL EMPLOYEES   LeetCode #1376
 *  https://leetcode.com/problems/time-needed-to-inform-all-employees/
 *  Pattern: a TREE given as PARENT ARRAY -> build children lists, DFS the MAX root-to-leaf time
 * ============================================================================
 *
 *  PROBLEM
 *    n employees, manager[i] = i's boss (headID has -1). Employee i needs
 *    informTime[i] minutes to tell ALL direct subordinates (simultaneously).
 *    Minutes until everyone knows the news?
 *      n=6, headID=2, manager [2,2,-1,2,2,2], informTime [0,0,1,0,0,0] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  The company is a TREE rooted at the head. News reaches employee e after the
 *  sum of informTime along the path head -> ... -> e's manager. Everyone knows
 *  when the SLOWEST path finishes:
 *       answer = max over root-to-leaf paths of (sum of informTime on the path)
 *
 *  STEPS
 *    1. the input points UP (child -> manager); build children lists (pointing DOWN)
 *    2. DFS: time(u) = informTime[u] + max over children c of time(c)   (bottom-up)
 *       (a leaf's informTime is 0 anyway)
 *  BFS with "arrival time" works too: arrival[c] = arrival[u] + informTime[u]; answer = max.
 *
 *  ALTERNATIVE without building children: for each employee, walk UP to the head
 *  summing manager informTimes, memoizing. Also O(n).
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M22_TimeNeededToInformAllEmployees {

    static int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        List<List<Integer>> children = new ArrayList<>();
        for (int i = 0; i < n; i++) children.add(new ArrayList<>());
        for (int i = 0; i < n; i++) if (manager[i] != -1) children.get(manager[i]).add(i);
        // iterative DFS carrying the arrival time (avoids deep recursion on a long chain)
        int best = 0;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{headID, 0});                    // {employee, time they learn the news}
        while (!stack.isEmpty()) {
            int[] top = stack.pop();
            int u = top[0], t = top[1];
            best = Math.max(best, t);
            for (int c : children.get(u)) stack.push(new int[]{c, t + informTime[u]});
        }
        return best;
    }

    // walk-up version with memoization
    static int walkUp(int n, int headID, int[] manager, int[] informTime) {
        int[] memo = new int[n];
        Arrays.fill(memo, -1);
        memo[headID] = 0;
        int best = 0;
        for (int i = 0; i < n; i++) best = Math.max(best, arrival(i, manager, informTime, memo));
        return best;
    }

    static int arrival(int i, int[] manager, int[] informTime, int[] memo) {
        if (memo[i] >= 0) return memo[i];
        return memo[i] = arrival(manager[i], manager, informTime, memo) + informTime[manager[i]];
    }

    public static void main(String[] args) {
        check("n=1", 0, numOfMinutes(1, 0, new int[]{-1}, new int[]{0}));
        check("star under head 2", 1, numOfMinutes(6, 2, new int[]{2, 2, -1, 2, 2, 2}, new int[]{0, 0, 1, 0, 0, 0}));
        check("chain 6->5->...->0", 21, numOfMinutes(7, 6, new int[]{1, 2, 3, 4, 5, 6, -1}, new int[]{0, 6, 5, 4, 3, 2, 1}));
        Random rnd = new Random(22);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(20);
            int[] perm = new int[n];
            for (int i = 0; i < n; i++) perm[i] = i;
            for (int i = n - 1; i > 0; i--) { int j = rnd.nextInt(i + 1); int x = perm[i]; perm[i] = perm[j]; perm[j] = x; }
            int[] mgr = new int[n], inf = new int[n];
            mgr[perm[0]] = -1;
            for (int i = 1; i < n; i++) mgr[perm[i]] = perm[rnd.nextInt(i)];      // random tree
            for (int i = 0; i < n; i++) inf[i] = rnd.nextInt(10);
            ok &= numOfMinutes(n, perm[0], mgr, inf) == walkUp(n, perm[0], mgr, inf);
        }
        check("500 random companies: DFS == walk-up", true, ok);
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
