/*
 * ============================================================================
 *  MEDIUM 20 : PARALLEL COURSES              LeetCode #1136 (premium)
 *  https://leetcode.com/problems/parallel-courses/
 *  Pattern: KAHN'S ALGORITHM, LEVEL BY LEVEL (each level = one semester)
 * ============================================================================
 *
 *  PROBLEM
 *    n courses (1..n), relations [prev, next]: prev before next. In one semester you
 *    may take ANY number of courses whose prerequisites were all done in EARLIER
 *    semesters. Minimum semesters to take all? -1 if there's a cycle.
 *      n=3, [[1,3],[2,3]] -> 2   (1 and 2, then 3)
 *      n=3, [[1,2],[2,3],[3,1]] -> -1
 *
 *  ---------------------------------------------------------------------------
 *  Course Schedule II (M04) + the BFS "level" idea:
 *    semester 1 = every course with in-degree 0
 *    taking a whole semester's courses lowers in-degrees; the courses that drop to
 *    0 form the NEXT semester. Count the levels.
 *  The answer equals the number of vertices on the LONGEST path in the DAG
 *  (the critical path - a chain that must be done one after another).
 *
 *  DRY RUN [[1,3],[2,3]]: level 1 {1,2} -> 3's in-degree 0 -> level 2 {3} -> 2
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M20_ParallelCourses {

    static int minimumSemesters(int n, int[][] relations) {
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i <= n; i++) next.add(new ArrayList<>());
        int[] indeg = new int[n + 1];
        for (int[] r : relations) { next.get(r[0]).add(r[1]); indeg[r[1]]++; }
        Queue<Integer> q = new ArrayDeque<>();
        for (int c = 1; c <= n; c++) if (indeg[c] == 0) q.add(c);
        int semesters = 0, taken = 0;
        while (!q.isEmpty()) {
            semesters++;
            for (int i = q.size(); i > 0; i--) {            // everything ready this semester
                int c = q.poll();
                taken++;
                for (int d : next.get(c)) if (--indeg[d] == 0) q.add(d);
            }
        }
        return taken == n ? semesters : -1;
    }

    // referee: longest path (in vertices) via memoized DFS; -1 on a cycle
    static int longestPath(int n, int[][] rel) {
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i <= n; i++) next.add(new ArrayList<>());
        for (int[] r : rel) next.get(r[0]).add(r[1]);
        int[] memo = new int[n + 1], state = new int[n + 1];
        int best = 0;
        for (int c = 1; c <= n; c++) {
            int l = dfs(c, next, memo, state);
            if (l < 0) return -1;
            best = Math.max(best, l);
        }
        return best;
    }

    static int dfs(int u, List<List<Integer>> next, int[] memo, int[] state) {
        if (state[u] == 1) return -1;                        // on the stack: cycle
        if (state[u] == 2) return memo[u];
        state[u] = 1;
        int best = 1;
        for (int v : next.get(u)) {
            int l = dfs(v, next, memo, state);
            if (l < 0) return -1;
            best = Math.max(best, 1 + l);
        }
        state[u] = 2;
        return memo[u] = best;
    }

    public static void main(String[] args) {
        check("n=3 [[1,3],[2,3]]", 2, minimumSemesters(3, new int[][]{{1, 3}, {2, 3}}));
        check("cycle", -1, minimumSemesters(3, new int[][]{{1, 2}, {2, 3}, {3, 1}}));
        check("chain of 4", 4, minimumSemesters(4, new int[][]{{1, 2}, {2, 3}, {3, 4}}));
        Random rnd = new Random(20);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] rel = new int[rnd.nextInt(10)][];
            for (int i = 0; i < rel.length; i++) { int a = 1 + rnd.nextInt(n), b = 1 + rnd.nextInt(n); rel[i] = new int[]{a, b}; }
            ok &= minimumSemesters(n, rel) == longestPath(n, rel);
        }
        check("1,000 random: levels == longest path", true, ok);
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
