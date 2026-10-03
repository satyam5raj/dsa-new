/*
 * ============================================================================
 *  MEDIUM 11 : NUMBER OF CONNECTED COMPONENTS IN AN UNDIRECTED GRAPH   LeetCode #323 (premium)
 *  https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/
 *  Pattern: count how many times a DFS has to START  (or Union-Find: n - successful unions)
 * ============================================================================
 *
 *  PROBLEM
 *      n=5, [[0,1],[1,2],[3,4]] -> 2      n=5, [[0,1],[1,2],[2,3],[3,4]] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  DFS: for v in 0..n-1: if not visited -> components++, DFS(v) marks its whole
 *  component. (Number of Islands is the grid version of this.)
 *
 *  UNION-FIND: start with n components. Every edge that joins two DIFFERENT sets
 *  merges them -> components--. Edges inside one set change nothing.
 *  Handy when edges arrive ONE AT A TIME ("how many components after each edge?"),
 *  where re-running DFS would be too slow. (11-Union-Find)
 *
 *  COMPLEXITY   DFS O(V + E);  Union-Find ~O(E * alpha(n)) ≈ O(E)
 * ============================================================================
 */

import java.util.*;

public class M11_NumberOfConnectedComponents {

    static int countComponents(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        boolean[] seen = new boolean[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if (seen[v]) continue;
            count++;                                          // v starts a new component
            Deque<Integer> stack = new ArrayDeque<>(List.of(v));
            seen[v] = true;
            while (!stack.isEmpty()) for (int w : adj.get(stack.pop())) if (!seen[w]) { seen[w] = true; stack.push(w); }
        }
        return count;
    }

    static int unionFind(int n, int[][] edges) {
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int count = n;
        for (int[] e : edges) {
            int a = find(parent, e[0]), b = find(parent, e[1]);
            if (a != b) { parent[a] = b; count--; }          // merged two components
        }
        return count;
    }

    static int find(int[] p, int x) {
        while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
        return x;
    }

    public static void main(String[] args) {
        check("[[0,1],[1,2],[3,4]]", 2, countComponents(5, new int[][]{{0, 1}, {1, 2}, {3, 4}}));
        check("a chain", 1, countComponents(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}));
        check("no edges", 4, countComponents(4, new int[][]{}));
        check("union-find version", 2, unionFind(5, new int[][]{{0, 1}, {1, 2}, {3, 4}}));
        Random rnd = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(12);
            int[][] es = new int[rnd.nextInt(12)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n)};
            ok &= countComponents(n, es) == unionFind(n, es);
        }
        check("1,000 random graphs: DFS == Union-Find", true, ok);
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
