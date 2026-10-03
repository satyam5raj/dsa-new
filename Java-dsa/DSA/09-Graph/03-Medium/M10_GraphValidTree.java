/*
 * ============================================================================
 *  MEDIUM 10 : GRAPH VALID TREE              LeetCode #261 (premium)
 *  https://leetcode.com/problems/graph-valid-tree/
 *  Pattern: TREE  <=>  exactly n-1 edges AND connected
 * ============================================================================
 *
 *  PROBLEM
 *    n nodes (0..n-1) and undirected edges. Do they form a valid tree?
 *      n=5, [[0,1],[0,2],[0,3],[1,4]]        -> true
 *      n=5, [[0,1],[1,2],[2,3],[1,3],[1,4]]  -> false (cycle 1-2-3)
 *
 *  ---------------------------------------------------------------------------
 *  A TREE = connected + no cycle. Two equivalent shortcuts (graph theory):
 *    with exactly n - 1 edges:  connected  <=>  acyclic
 *  So:
 *    1. if edges.length != n - 1 -> false   (too few: disconnected; too many: a cycle)
 *    2. BFS/DFS from node 0; return whether all n nodes were reached
 *  No explicit cycle detection needed!
 *
 *  UNION-FIND VERSION: for each edge, if both ends are already in the same set ->
 *  a cycle -> false. (11-Union-Find)
 *
 *  DRY RUN n=5, 5 edges -> != 4 -> false immediately.
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M10_GraphValidTree {

    static boolean validTree(int n, int[][] edges) {
        if (edges.length != n - 1) return false;            // a tree has exactly n - 1 edges
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        boolean[] seen = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>(List.of(0));
        seen[0] = true;
        int reached = 1;
        while (!stack.isEmpty()) {
            for (int v : adj.get(stack.pop())) {
                if (!seen[v]) { seen[v] = true; reached++; stack.push(v); }
            }
        }
        return reached == n;                                // connected + n-1 edges = tree
    }

    // referee: explicit "connected AND no cycle" with DFS parent tracking
    static boolean referee(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int i = 0; i < edges.length; i++) { adj.get(edges[i][0]).add(i); adj.get(edges[i][1]).add(i); }
        boolean[] seen = new boolean[n];
        if (cycle(0, -1, adj, edges, seen)) return false;
        for (boolean s : seen) if (!s) return false;
        return true;
    }

    static boolean cycle(int u, int viaEdge, List<List<Integer>> adj, int[][] edges, boolean[] seen) {
        seen[u] = true;
        for (int ei : adj.get(u)) {
            if (ei == viaEdge) continue;                    // don't go back along the same edge
            int v = edges[ei][0] == u ? edges[ei][1] : edges[ei][0];
            if (seen[v] || cycle(v, ei, adj, edges, seen)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        check("n=5 tree", true, validTree(5, new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 4}}));
        check("n=5 with cycle", false, validTree(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {1, 3}, {1, 4}}));
        check("n=4, 3 edges but disconnected + cycle", false, validTree(4, new int[][]{{0, 1}, {1, 2}, {2, 0}}));
        check("n=1, no edges", true, validTree(1, new int[][]{}));
        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 1 + rnd.nextInt(7);
            int[][] es = new int[Math.max(0, n - 2 + rnd.nextInt(3))][];
            for (int i = 0; i < es.length; i++) { int a = rnd.nextInt(n), b; do b = rnd.nextInt(n); while (n > 1 && b == a); es[i] = new int[]{a, b}; }
            if (n == 1 && es.length > 0) continue;
            ok &= validTree(n, es) == referee(n, es);
        }
        check("2,000 random graphs vs explicit check", true, ok);
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
