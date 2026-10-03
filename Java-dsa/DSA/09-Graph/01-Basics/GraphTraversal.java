/*
 * ============================================================================
 *  GRAPH BASICS 2: BFS, DFS, components, cycles, bipartite
 * ============================================================================
 *
 *  THE ONE DIFFERENCE FROM TREES: graphs have CYCLES, so you MUST remember which
 *  vertices you've VISITED, or you'll loop forever. Mark a vertex visited when
 *  you PUSH it (BFS) / ENTER it (DFS), not when you pop it - otherwise the same
 *  vertex can be queued many times.
 *
 *  BFS (queue): explores in rings of increasing distance.
 *    -> SHORTEST PATH in an UNWEIGHTED graph (number of edges). Keep parent[] to
 *       rebuild the path.  O(V + E)
 *  DFS (recursion or a stack): goes deep first.
 *    -> reachability, components, cycle detection, topological sort, bridges,
 *       backtracking over paths.  O(V + E)
 *
 *  CONNECTED COMPONENTS (undirected): loop over all vertices; each unvisited one
 *    starts a new BFS/DFS = a new component.
 *
 *  CYCLE DETECTION
 *    UNDIRECTED: during DFS, reaching an already-visited vertex that is NOT the one
 *      we came from (the parent) -> cycle.
 *    DIRECTED: "visited" isn't enough (two paths can reach the same vertex without
 *      a cycle). Use 3 COLOURS:
 *        WHITE = not visited, GRAY = on the current DFS path, BLACK = finished
 *      Reaching a GRAY vertex = a BACK EDGE = a cycle.
 *
 *  BIPARTITE (2-colourable): colour the start 0, neighbours 1, their neighbours 0...
 *    If an edge ever joins two vertices of the SAME colour -> not bipartite.
 *    (Equivalent: no ODD-length cycle.)
 *
 *  Run:  java GraphTraversal.java
 * ============================================================================
 */

import java.util.*;

public class GraphTraversal {

    static List<List<Integer>> graph(int n, int[][] edges, boolean directed) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            if (!directed) adj.get(e[1]).add(e[0]);
        }
        return adj;
    }

    // ---------- BFS: shortest path (in edges) from src to dst, with the actual path ----------
    static List<Integer> shortestPath(List<List<Integer>> adj, int src, int dst) {
        int n = adj.size();
        int[] parent = new int[n];
        Arrays.fill(parent, -2);                      // -2 = not visited
        parent[src] = -1;
        Queue<Integer> q = new ArrayDeque<>();
        q.add(src);
        while (!q.isEmpty()) {
            int u = q.poll();
            if (u == dst) break;
            for (int v : adj.get(u)) {
                if (parent[v] == -2) {                // mark when PUSHING
                    parent[v] = u;
                    q.add(v);
                }
            }
        }
        if (parent[dst] == -2) return List.of();      // unreachable
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = dst; v != -1; v = parent[v]) path.addFirst(v);   // walk the parents back
        return path;
    }

    // BFS distances from src to everyone (-1 = unreachable)
    static int[] bfsDistances(List<List<Integer>> adj, int src) {
        int[] dist = new int[adj.size()];
        Arrays.fill(dist, -1);
        dist[src] = 0;
        Queue<Integer> q = new ArrayDeque<>(List.of(src));
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adj.get(u)) if (dist[v] == -1) { dist[v] = dist[u] + 1; q.add(v); }
        }
        return dist;
    }

    // ---------- DFS: recursive and iterative ----------
    static void dfs(List<List<Integer>> adj, int u, boolean[] seen, List<Integer> order) {
        seen[u] = true;
        order.add(u);
        for (int v : adj.get(u)) if (!seen[v]) dfs(adj, v, seen, order);
    }

    static List<Integer> dfsIterative(List<List<Integer>> adj, int src) {
        List<Integer> order = new ArrayList<>();
        boolean[] seen = new boolean[adj.size()];
        Deque<Integer> stack = new ArrayDeque<>(List.of(src));
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (seen[u]) continue;                    // may have been pushed twice
            seen[u] = true;
            order.add(u);
            List<Integer> nb = adj.get(u);
            for (int i = nb.size() - 1; i >= 0; i--) if (!seen[nb.get(i)]) stack.push(nb.get(i));   // reverse -> same order as recursion
        }
        return order;
    }

    // ---------- Connected components ----------
    static int countComponents(List<List<Integer>> adj) {
        boolean[] seen = new boolean[adj.size()];
        int count = 0;
        for (int u = 0; u < adj.size(); u++) {
            if (!seen[u]) {
                count++;                              // u starts a brand-new component
                dfs(adj, u, seen, new ArrayList<>());
            }
        }
        return count;
    }

    // ---------- Cycle detection ----------
    static boolean hasCycleUndirected(List<List<Integer>> adj) {
        boolean[] seen = new boolean[adj.size()];
        for (int u = 0; u < adj.size(); u++) if (!seen[u] && cycleFrom(adj, u, -1, seen)) return true;
        return false;
    }

    static boolean cycleFrom(List<List<Integer>> adj, int u, int parent, boolean[] seen) {
        seen[u] = true;
        for (int v : adj.get(u)) {
            if (!seen[v]) { if (cycleFrom(adj, v, u, seen)) return true; }
            else if (v != parent) return true;        // visited and not where we came from
        }
        return false;
    }

    static final int WHITE = 0, GRAY = 1, BLACK = 2;

    static boolean hasCycleDirected(List<List<Integer>> adj) {
        int[] color = new int[adj.size()];
        for (int u = 0; u < adj.size(); u++) if (color[u] == WHITE && backEdgeFrom(adj, u, color)) return true;
        return false;
    }

    static boolean backEdgeFrom(List<List<Integer>> adj, int u, int[] color) {
        color[u] = GRAY;                              // on the current path
        for (int v : adj.get(u)) {
            if (color[v] == GRAY) return true;        // back edge -> cycle
            if (color[v] == WHITE && backEdgeFrom(adj, v, color)) return true;
        }
        color[u] = BLACK;                             // fully explored, no cycle through u
        return false;
    }

    // ---------- Bipartite via BFS colouring ----------
    static boolean isBipartite(List<List<Integer>> adj) {
        int[] color = new int[adj.size()];
        Arrays.fill(color, -1);
        for (int s = 0; s < adj.size(); s++) {
            if (color[s] != -1) continue;
            color[s] = 0;
            Queue<Integer> q = new ArrayDeque<>(List.of(s));
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) {
                    if (color[v] == -1) { color[v] = 1 - color[u]; q.add(v); }
                    else if (color[v] == color[u]) return false;   // same colour on both ends
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        //  0 - 1 - 3        5 - 6
        //  |   |
        //  2 --+            4 (alone)
        List<List<Integer>> g = graph(7, new int[][]{{0, 1}, {0, 2}, {1, 2}, {1, 3}, {5, 6}}, false);

        System.out.println("1. BFS shortest path");
        check("path 2 -> 3", "[2, 1, 3]", shortestPath(g, 2, 3));
        check("distances from 0", "[0, 1, 1, 2, -1, -1, -1]", bfsDistances(g, 0));
        check("unreachable 0 -> 5", "[]", shortestPath(g, 0, 5));

        System.out.println("2. DFS orders");
        List<Integer> rec = new ArrayList<>();
        dfs(g, 0, new boolean[7], rec);
        check("recursive DFS from 0", "[0, 1, 2, 3]", rec);
        check("iterative DFS from 0 (same order)", "[0, 1, 2, 3]", dfsIterative(g, 0));

        System.out.println("3. Components");
        check("components", 3, countComponents(g));

        System.out.println("4. Cycles");
        check("undirected with triangle 0-1-2", true, hasCycleUndirected(g));
        check("undirected tree", false, hasCycleUndirected(graph(4, new int[][]{{0, 1}, {1, 2}, {1, 3}}, false)));
        check("directed 0->1->2->0", true, hasCycleDirected(graph(3, new int[][]{{0, 1}, {1, 2}, {2, 0}}, true)));
        // diamond: 0->1->3 and 0->2->3. 3 is reached twice but there's NO cycle
        check("directed diamond (no cycle)", false, hasCycleDirected(graph(4, new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}}, true)));

        System.out.println("5. Bipartite");
        check("square 0-1-2-3-0 (even cycle)", true, isBipartite(graph(4, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}}, false)));
        check("triangle (odd cycle)", false, isBipartite(graph(3, new int[][]{{0, 1}, {1, 2}, {2, 0}}, false)));

        System.out.println("6. Random check: BFS distances vs Floyd-Warshall-style relaxation");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int n = 1 + rnd.nextInt(12);
            List<int[]> es = new ArrayList<>();
            for (int i = 0; i < n * 2; i++) es.add(new int[]{rnd.nextInt(n), rnd.nextInt(n)});
            List<List<Integer>> adj = graph(n, es.toArray(new int[0][]), false);
            int[][] d = new int[n][n];
            for (int[] row : d) Arrays.fill(row, 1_000);
            for (int i = 0; i < n; i++) d[i][i] = 0;
            for (int[] e : es) if (e[0] != e[1]) { d[e[0]][e[1]] = 1; d[e[1]][e[0]] = 1; }
            for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);
            int[] bfs = bfsDistances(adj, 0);
            for (int v = 0; v < n; v++) ok &= bfs[v] == (d[0][v] >= 1_000 ? -1 : d[0][v]);
        }
        check("200 random graphs", true, ok);
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
