/*
 * ============================================================================
 *  MEDIUM 17 : IS GRAPH BIPARTITE?           LeetCode #785
 *  https://leetcode.com/problems/is-graph-bipartite/
 *  Pattern: 2-COLOURING with BFS (every component!)
 * ============================================================================
 *
 *  PROBLEM
 *    graph[u] = neighbours of u (undirected, possibly DISCONNECTED). Can the
 *    vertices be split into two sets so every edge goes BETWEEN the sets?
 *      [[1,2,3],[0,2],[0,1,3],[0,2]] -> false (triangle 0-1-2)
 *      [[1,3],[0,2],[1,3],[0,2]]     -> true  (square: {0,2} vs {1,3})
 *
 *  ---------------------------------------------------------------------------
 *  2-COLOURING: give a start vertex colour 0; every neighbour must get the OTHER
 *  colour; continue with BFS. If an edge ever connects two vertices of the SAME
 *  colour -> not bipartite. (A graph is bipartite <=> it has no ODD cycle.)
 *
 *  TRAP: the graph may be DISCONNECTED. Start a new BFS from every uncoloured
 *  vertex, or you'll only check the first component.
 *
 *  DRY RUN triangle: 0 -> colour 0; 1, 2 -> colour 1; edge 1-2: both 1 -> false
 *
 *  Possible Bipartition (M25) is this problem with "dislike" edges.
 *  COMPLEXITY   Time O(V + E)   Space O(V)
 * ============================================================================
 */

import java.util.*;

public class M17_IsGraphBipartite {

    static boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] color = new int[n];
        Arrays.fill(color, -1);
        for (int start = 0; start < n; start++) {            // every component
            if (color[start] != -1) continue;
            color[start] = 0;
            Queue<Integer> q = new ArrayDeque<>(List.of(start));
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : graph[u]) {
                    if (color[v] == -1) { color[v] = 1 - color[u]; q.add(v); }
                    else if (color[v] == color[u]) return false;   // conflict: same colour on an edge
                }
            }
        }
        return true;
    }

    // brute force: try all 2^n colourings
    static boolean brute(int[][] g) {
        int n = g.length;
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean ok = true;
            for (int u = 0; u < n && ok; u++) for (int v : g[u]) if ((mask >> u & 1) == (mask >> v & 1)) { ok = false; break; }
            if (ok) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        check("triangle inside", false, isBipartite(new int[][]{{1, 2, 3}, {0, 2}, {0, 1, 3}, {0, 2}}));
        check("square", true, isBipartite(new int[][]{{1, 3}, {0, 2}, {1, 3}, {0, 2}}));
        // component 1: 0-1 (fine), component 2: triangle 2-3-4
        check("odd cycle in 2nd component", false, isBipartite(new int[][]{{1}, {0}, {3, 4}, {2, 4}, {2, 3}}));
        Random rnd = new Random(17);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            List<Set<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < n; i++) adj.add(new TreeSet<>());
            for (int i = 0; i < rnd.nextInt(n + 3); i++) { int a = rnd.nextInt(n), b = rnd.nextInt(n); if (a != b) { adj.get(a).add(b); adj.get(b).add(a); } }
            int[][] g = adj.stream().map(s -> s.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
            ok &= isBipartite(g) == brute(g);
        }
        check("1,000 random graphs vs all colourings", true, ok);
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
