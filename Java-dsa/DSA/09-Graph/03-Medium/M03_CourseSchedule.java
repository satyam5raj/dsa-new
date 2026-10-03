/*
 * ============================================================================
 *  MEDIUM 03 : COURSE SCHEDULE               LeetCode #207
 *  https://leetcode.com/problems/course-schedule/
 *  Pattern: CYCLE DETECTION in a directed graph (Kahn's topological sort)
 * ============================================================================
 *
 *  PROBLEM
 *    numCourses, prerequisites[i] = [a, b] means "take b before a". Can you finish
 *    all courses?
 *      2, [[1,0]]        -> true   (0 then 1)
 *      2, [[1,0],[0,1]]  -> false  (each waits for the other)
 *
 *  ---------------------------------------------------------------------------
 *  MODEL: edge b -> a ("b unlocks a"). All courses can be finished  <=>  the graph
 *  has NO CYCLE  <=>  a topological order exists (01-Basics/TopologicalSort).
 *
 *  KAHN: start with courses that have no prerequisites (in-degree 0); "take" one,
 *  which lowers the in-degree of the courses it unlocks; any that reach 0 become
 *  takeable. Count how many we took. All of them -> true.
 *  Courses on a cycle never reach in-degree 0 -> the count falls short.
 *
 *  DFS ALTERNATIVE: 3 colours (white/gray/black); reaching a GRAY course = a cycle.
 *
 *  DIRECTION TRAP: [a, b] means b -> a. Getting it backwards still detects cycles
 *  correctly here (a reversed graph has the same cycles), but it matters in
 *  Course Schedule II where you must output the ORDER.
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M03_CourseSchedule {

    static boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> unlocks = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) unlocks.add(new ArrayList<>());
        int[] indeg = new int[numCourses];
        for (int[] p : prerequisites) {
            unlocks.get(p[1]).add(p[0]);         // p[1] must come before p[0]
            indeg[p[0]]++;
        }
        Queue<Integer> ready = new ArrayDeque<>();
        for (int c = 0; c < numCourses; c++) if (indeg[c] == 0) ready.add(c);
        int taken = 0;
        while (!ready.isEmpty()) {
            int c = ready.poll();
            taken++;
            for (int next : unlocks.get(c)) if (--indeg[next] == 0) ready.add(next);
        }
        return taken == numCourses;              // anything left is stuck on a cycle
    }

    static boolean canFinishDfs(int n, int[][] prereq) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] p : prereq) adj.get(p[1]).add(p[0]);
        int[] color = new int[n];
        for (int u = 0; u < n; u++) if (color[u] == 0 && hasCycle(adj, u, color)) return false;
        return true;
    }

    static boolean hasCycle(List<List<Integer>> adj, int u, int[] color) {
        color[u] = 1;                            // gray: on the current path
        for (int v : adj.get(u)) {
            if (color[v] == 1) return true;
            if (color[v] == 0 && hasCycle(adj, v, color)) return true;
        }
        color[u] = 2;                            // black: done
        return false;
    }

    public static void main(String[] args) {
        check("2, [[1,0]]", true, canFinish(2, new int[][]{{1, 0}}));
        check("2, [[1,0],[0,1]]", false, canFinish(2, new int[][]{{1, 0}, {0, 1}}));
        check("self-loop [[0,0]]", false, canFinish(1, new int[][]{{0, 0}}));
        check("diamond (no cycle)", true, canFinish(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}}));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] p = new int[rnd.nextInt(10)][];
            for (int i = 0; i < p.length; i++) p[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n)};
            ok &= canFinish(n, p) == canFinishDfs(n, p);
        }
        check("1,000 random: Kahn == DFS colours", true, ok);
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
