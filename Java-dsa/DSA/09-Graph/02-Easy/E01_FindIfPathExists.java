/*
 * ============================================================================
 *  EASY 01 : FIND IF PATH EXISTS IN GRAPH     LeetCode #1971
 *  https://leetcode.com/problems/find-if-path-exists-in-graph/
 *  Pattern: build the ADJACENCY LIST, then BFS/DFS with a visited array
 * ============================================================================
 *
 *  PROBLEM
 *    Undirected graph with n vertices and an edge list. Is there a path source -> destination?
 *      n=3, edges [[0,1],[1,2],[2,0]], 0 -> 2  -> true
 *      n=6, edges [[0,1],[0,2],[3,5],[5,4],[4,3]], 0 -> 5 -> false (different components)
 *
 *  ---------------------------------------------------------------------------
 *  THE GRAPH TEMPLATE (you'll write this in almost every problem):
 *    1. edge list -> adjacency list (both directions for undirected)
 *    2. BFS from source with visited[]; mark when PUSHING
 *    3. stop as soon as destination is reached
 *  Without visited[], the cycle 0-1-2-0 would make BFS loop forever.
 *
 *  (Union-Find also answers "are they connected?" - see 11-Union-Find.)
 *
 *  DRY RUN n=3, 0 -> 2
 *    q [0] -> visit 1, 2 -> 2 is the destination -> true
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class E01_FindIfPathExists {

    static boolean validPath(int n, int[][] edges, int source, int destination) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

        boolean[] visited = new boolean[n];
        Queue<Integer> q = new ArrayDeque<>();
        q.add(source);
        visited[source] = true;
        while (!q.isEmpty()) {
            int u = q.poll();
            if (u == destination) return true;
            for (int v : adj.get(u)) {
                if (!visited[v]) { visited[v] = true; q.add(v); }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        check("triangle 0 -> 2", true, validPath(3, new int[][]{{0, 1}, {1, 2}, {2, 0}}, 0, 2));
        check("two components 0 -> 5", false, validPath(6, new int[][]{{0, 1}, {0, 2}, {3, 5}, {5, 4}, {4, 3}}, 0, 5));
        check("source == destination", true, validPath(1, new int[][]{}, 0, 0));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(10);
            int[][] es = new int[rnd.nextInt(n + 2)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n)};
            // referee: transitive closure with repeated relaxation
            boolean[][] r = new boolean[n][n];
            for (int i = 0; i < n; i++) r[i][i] = true;
            for (int[] e : es) { r[e[0]][e[1]] = true; r[e[1]][e[0]] = true; }
            for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) r[i][j] |= r[i][k] && r[k][j];
            int s = rnd.nextInt(n), d = rnd.nextInt(n);
            ok &= validPath(n, es, s, d) == r[s][d];
        }
        check("500 random graphs vs transitive closure", true, ok);
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
