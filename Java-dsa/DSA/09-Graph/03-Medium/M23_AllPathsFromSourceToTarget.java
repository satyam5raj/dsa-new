/*
 * ============================================================================
 *  MEDIUM 23 : ALL PATHS FROM SOURCE TO TARGET   LeetCode #797
 *  https://leetcode.com/problems/all-paths-from-source-to-target/
 *  Pattern: DFS + BACKTRACKING on a DAG (no visited set needed - no cycles)
 * ============================================================================
 *
 *  PROBLEM
 *    A DAG with nodes 0..n-1. Return EVERY path from 0 to n-1.
 *      [[1,2],[3],[3],[]] -> [[0,1,3],[0,2,3]]
 *
 *  ---------------------------------------------------------------------------
 *  "All paths" means we must ENUMERATE, not just find one: backtracking.
 *    dfs(u, path):
 *      if u == target: record a COPY of path
 *      for each v in graph[u]: path.add(v); dfs(v); path.remove(last)
 *  (The add/recurse/remove shape from Path Sum II in Trees.)
 *
 *  WHY NO VISITED SET? It's a DAG: no path can return to a node, so it always
 *  terminates. And a node MUST be visitable again by a DIFFERENT path - a global
 *  visited set would wrongly cut off the second path through node 3.
 *  (On a graph with cycles you'd use an "on the current path" set instead.)
 *
 *  OUTPUT SIZE: the number of paths can be exponential (2^(n-2) in the worst case),
 *  so no algorithm can be polynomial here.
 *
 *  COMPLEXITY   Time O(2^n * n)   Space O(n) recursion (+ output)
 * ============================================================================
 */

import java.util.*;

public class M23_AllPathsFromSourceToTarget {

    static List<List<Integer>> allPathsSourceTarget(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> path = new ArrayList<>(List.of(0));
        dfs(graph, 0, path, result);
        return result;
    }

    static void dfs(int[][] g, int u, List<Integer> path, List<List<Integer>> result) {
        if (u == g.length - 1) {
            result.add(new ArrayList<>(path));               // copy!
            return;
        }
        for (int v : g[u]) {
            path.add(v);                                     // choose
            dfs(g, v, path, result);
            path.remove(path.size() - 1);                    // un-choose
        }
    }

    // count paths with DP (memo) - must equal the number of enumerated paths
    static long countPaths(int[][] g, int u, Long[] memo) {
        if (u == g.length - 1) return 1;
        if (memo[u] != null) return memo[u];
        long c = 0;
        for (int v : g[u]) c += countPaths(g, v, memo);
        return memo[u] = c;
    }

    public static void main(String[] args) {
        check("[[1,2],[3],[3],[]]", "[[0, 1, 3], [0, 2, 3]]", allPathsSourceTarget(new int[][]{{1, 2}, {3}, {3}, {}}));
        check("example 2", "[[0, 4], [0, 3, 4], [0, 1, 3, 4], [0, 1, 2, 3, 4], [0, 1, 4]]",
                allPathsSourceTarget(new int[][]{{4, 3, 1}, {3, 2, 4}, {3}, {4}, {}}));
        Random rnd = new Random(23);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 2 + rnd.nextInt(10);
            int[][] g = new int[n][];
            for (int u = 0; u < n; u++) {
                List<Integer> out = new ArrayList<>();
                for (int v = u + 1; v < n; v++) if (rnd.nextInt(3) == 0) out.add(v);   // only forward edges: a DAG
                g[u] = out.stream().mapToInt(Integer::intValue).toArray();
            }
            List<List<Integer>> paths = allPathsSourceTarget(g);
            ok &= paths.size() == countPaths(g, 0, new Long[n]);
            for (List<Integer> p : paths) ok &= p.get(0) == 0 && p.get(p.size() - 1) == n - 1;
            ok &= new HashSet<>(paths).size() == paths.size();                          // no duplicates
        }
        check("300 random DAGs: count == DP count", true, ok);
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
