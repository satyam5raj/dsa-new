/*
 * ============================================================================
 *  MEDIUM 18 : FIND EVENTUAL SAFE STATES     LeetCode #802
 *  https://leetcode.com/problems/find-eventual-safe-states/
 *  Pattern: 3-COLOUR DFS ("on a cycle or leads to one" = unsafe)  /  Kahn on the REVERSED graph
 * ============================================================================
 *
 *  PROBLEM
 *    Directed graph. A TERMINAL node has no outgoing edges. A node is SAFE if
 *    EVERY path from it ends at a terminal node (it can never get stuck in a cycle).
 *    Return all safe nodes, sorted.
 *      [[1,2],[2,3],[5],[0],[5],[],[]] -> [2,4,5,6]
 *        (0 -> 1 -> 3 -> 0 is a cycle, so 0, 1, 3 are unsafe)
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: DFS with 3 colours (the directed cycle detector)
 *    GRAY  = on the current path (or found to be unsafe - we leave it gray)
 *    BLACK = proven safe
 *    safe(u): if GRAY -> false (a cycle); if BLACK -> true
 *             colour u GRAY; if any neighbour is unsafe -> return false (stay GRAY)
 *             otherwise colour u BLACK -> true
 *    Leaving unsafe nodes GRAY is the trick: anyone who later reaches them also
 *    sees "gray" and becomes unsafe - which is correct (they lead to a cycle).
 *
 *  APPROACH 2: REVERSE + KAHN
 *    Terminal nodes are safe. A node is safe once ALL its out-neighbours are safe.
 *    Reverse every edge; then out-degree plays the role of in-degree: start with
 *    out-degree-0 nodes, and "peel" backwards. Whatever gets peeled is safe.
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V)
 * ============================================================================
 */

import java.util.*;

public class M18_FindEventualSafeStates {

    static List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        int[] color = new int[n];                    // 0 white, 1 gray (on path / unsafe), 2 black (safe)
        List<Integer> safe = new ArrayList<>();
        for (int u = 0; u < n; u++) if (isSafe(graph, u, color)) safe.add(u);
        return safe;
    }

    static boolean isSafe(int[][] g, int u, int[] color) {
        if (color[u] != 0) return color[u] == 2;
        color[u] = 1;
        for (int v : g[u]) if (!isSafe(g, v, color)) return false;   // stays gray = unsafe
        color[u] = 2;
        return true;
    }

    static List<Integer> kahnReversed(int[][] graph) {
        int n = graph.length;
        List<List<Integer>> rev = new ArrayList<>();
        for (int i = 0; i < n; i++) rev.add(new ArrayList<>());
        int[] outdeg = new int[n];
        for (int u = 0; u < n; u++) { outdeg[u] = graph[u].length; for (int v : graph[u]) rev.get(v).add(u); }
        Queue<Integer> q = new ArrayDeque<>();
        for (int u = 0; u < n; u++) if (outdeg[u] == 0) q.add(u);     // terminals
        boolean[] safe = new boolean[n];
        while (!q.isEmpty()) {
            int v = q.poll();
            safe[v] = true;
            for (int u : rev.get(v)) if (--outdeg[u] == 0) q.add(u);   // all of u's exits are safe now
        }
        List<Integer> out = new ArrayList<>();
        for (int u = 0; u < n; u++) if (safe[u]) out.add(u);
        return out;
    }

    public static void main(String[] args) {
        int[][] g = {{1, 2}, {2, 3}, {5}, {0}, {5}, {}, {}};
        check("example 1", "[2, 4, 5, 6]", eventualSafeNodes(g));
        check("example 2", "[4]", eventualSafeNodes(new int[][]{{1, 2, 3, 4}, {1, 2}, {3, 4}, {0, 4}, {}}));
        check("Kahn on reversed graph", "[2, 4, 5, 6]", kahnReversed(g));
        Random rnd = new Random(18);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(10);
            int[][] gr = new int[n][];
            for (int u = 0; u < n; u++) {
                TreeSet<Integer> s = new TreeSet<>();
                for (int i = 0; i < rnd.nextInt(3); i++) s.add(rnd.nextInt(n));
                gr[u] = s.stream().mapToInt(Integer::intValue).toArray();
            }
            ok &= eventualSafeNodes(gr).equals(kahnReversed(gr));
        }
        check("1,000 random graphs: DFS == reversed Kahn", true, ok);
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
