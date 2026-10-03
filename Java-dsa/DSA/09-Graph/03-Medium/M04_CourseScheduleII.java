/*
 * ============================================================================
 *  MEDIUM 04 : COURSE SCHEDULE II            LeetCode #210
 *  https://leetcode.com/problems/course-schedule-ii/
 *  Pattern: TOPOLOGICAL ORDER (Kahn) - return the order itself, [] on a cycle
 * ============================================================================
 *
 *  PROBLEM
 *    Same as M03, but return ONE valid order to take all courses (or [] if impossible).
 *      4, [[1,0],[2,0],[3,1],[3,2]] -> [0,1,2,3] or [0,2,1,3]
 *
 *  ---------------------------------------------------------------------------
 *  Kahn's algorithm already produces the order: the sequence in which courses are
 *  polled from the "ready" queue. If fewer than numCourses got polled, there's a
 *  cycle -> return [].
 *
 *  Here the edge DIRECTION matters: [a, b] = b BEFORE a  ->  edge b -> a,
 *  indeg[a]++. Reverse it and you'd print the order backwards.
 *
 *  DFS VERSION: reversed postorder (01-Basics/TopologicalSort). Kahn is usually
 *  preferred in interviews: iterative, and cycle detection comes for free.
 *
 *  DRY RUN 4, [[1,0],[2,0],[3,1],[3,2]]
 *    indeg [0,1,1,2]; ready [0]
 *    take 0 -> indeg 1,2 drop to 0 -> ready [1,2]
 *    take 1 -> indeg[3] = 1;  take 2 -> indeg[3] = 0 -> ready [3];  take 3
 *    -> [0,1,2,3]
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M04_CourseScheduleII {

    static int[] findOrder(int numCourses, int[][] prerequisites) {
        List<List<Integer>> unlocks = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) unlocks.add(new ArrayList<>());
        int[] indeg = new int[numCourses];
        for (int[] p : prerequisites) { unlocks.get(p[1]).add(p[0]); indeg[p[0]]++; }
        Queue<Integer> ready = new ArrayDeque<>();
        for (int c = 0; c < numCourses; c++) if (indeg[c] == 0) ready.add(c);
        int[] order = new int[numCourses];
        int idx = 0;
        while (!ready.isEmpty()) {
            int c = ready.poll();
            order[idx++] = c;
            for (int next : unlocks.get(c)) if (--indeg[next] == 0) ready.add(next);
        }
        return idx == numCourses ? order : new int[0];
    }

    static boolean valid(int n, int[][] prereq, int[] order) {
        if (order.length != n) return false;
        int[] pos = new int[n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) { if (pos[order[i]] != -1) return false; pos[order[i]] = i; }
        for (int[] p : prereq) if (pos[p[1]] > pos[p[0]]) return false;
        return true;
    }

    public static void main(String[] args) {
        check("2, [[1,0]]", "[0, 1]", findOrder(2, new int[][]{{1, 0}}));
        check("diamond", "[0, 1, 2, 3]", findOrder(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}}));
        check("1, []", "[0]", findOrder(1, new int[][]{}));
        check("cycle", "[]", findOrder(2, new int[][]{{1, 0}, {0, 1}}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] p = new int[rnd.nextInt(10)][];
            for (int i = 0; i < p.length; i++) p[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n)};
            int[] o = findOrder(n, p);
            ok &= o.length == 0 ? !isAcyclic(n, p) : valid(n, p, o);
        }
        check("1,000 random: valid order or a real cycle", true, ok);
        summary();
    }

    // independent check: true if the prerequisite graph has NO cycle (DFS colours)
    static boolean isAcyclic(int n, int[][] p) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : p) adj.get(e[1]).add(e[0]);
        int[] color = new int[n];
        for (int u = 0; u < n; u++) if (color[u] == 0 && cyc(adj, u, color)) return false;
        return true;
    }

    static boolean cyc(List<List<Integer>> adj, int u, int[] color) {
        color[u] = 1;
        for (int v : adj.get(u)) if (color[v] == 1 || (color[v] == 0 && cyc(adj, v, color))) return true;
        color[u] = 2;
        return false;
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
