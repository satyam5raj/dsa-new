/*
 * ============================================================================
 *  GRAPH BASICS 5: MINIMUM SPANNING TREE (Prim and Kruskal)
 * ============================================================================
 *
 *  A SPANNING TREE of a connected, undirected, weighted graph uses V - 1 edges to
 *  connect ALL vertices with no cycle. The MINIMUM spanning tree (MST) has the
 *  smallest total weight. Cheapest way to wire up houses, cities, network nodes.
 *  (Min Cost to Connect All Points is exactly this.)
 *
 *          1
 *     A ------- B           MST: A-B (1), B-C (2), C-D (3)  -> total 6
 *     |  \      |           A-C (4) and A-D (5) would only add cost / a cycle
 *    5|   4\    |2
 *     |      \  |
 *     D ------- C
 *          3
 *
 *  THE CUT PROPERTY (why both greedy algorithms work)
 *    Split the vertices into two groups in any way. The CHEAPEST edge crossing
 *    between the groups belongs to some MST. So greedily taking cheapest
 *    "connecting" edges is safe.
 *
 *  ---------------------------------------------------------------------------
 *  PRIM (grow ONE tree, like Dijkstra)
 *    start anywhere; min-heap of edges leaving the tree; repeatedly take the
 *    cheapest edge to a vertex NOT yet in the tree, add that vertex, push its edges.
 *    O(E log V). For a DENSE graph (every pair is an edge, e.g. points on a plane)
 *    the O(V^2) array version without a heap is better: keep minCost[v] to the tree.
 *
 *  KRUSKAL (merge FORESTS)
 *    sort ALL edges by weight; take each edge unless it joins two vertices that are
 *    ALREADY connected (it would make a cycle). "Already connected?" is answered in
 *    ~O(1) by UNION-FIND (Disjoint Set Union) - full treatment in 11-Union-Find;
 *    a minimal version is below. O(E log E) for the sort.
 *
 *  Run:  java MinimumSpanningTree.java
 * ============================================================================
 */

import java.util.*;

public class MinimumSpanningTree {

    // edges: {u, v, w}, undirected. Returns the MST weight, or -1 if disconnected.
    static long prim(int n, int[][] edges) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(new int[]{e[1], e[2]}); adj.get(e[1]).add(new int[]{e[0], e[2]}); }
        boolean[] inTree = new boolean[n];
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[1]));   // {vertex, cost to reach it}
        heap.add(new int[]{0, 0});
        long total = 0;
        int added = 0;
        while (!heap.isEmpty() && added < n) {
            int[] top = heap.poll();
            if (inTree[top[0]]) continue;                     // already reached more cheaply
            inTree[top[0]] = true;
            total += top[1];
            added++;
            for (int[] e : adj.get(top[0])) if (!inTree[e[0]]) heap.add(new int[]{e[0], e[1]});
        }
        return added == n ? total : -1;
    }

    // ---- minimal Union-Find (path compression + union by size); details in 11-Union-Find ----
    static int[] parent, size;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }   // path halving
        return x;
    }

    static boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;                           // already connected: would make a cycle
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
        return true;
    }

    static long kruskal(int n, int[][] edges) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
        int[][] sorted = edges.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[2]));
        long total = 0;
        int used = 0;
        for (int[] e : sorted) {
            if (union(e[0], e[1])) {                          // joins two different trees
                total += e[2];
                if (++used == n - 1) break;
            }
        }
        return used == n - 1 || n <= 1 ? total : -1;
    }

    // Prim O(V^2) for a COMPLETE graph given by a weight function (no edge list needed)
    static long primDense(int[][] pts) {
        int n = pts.length;
        long[] minCost = new long[n];
        Arrays.fill(minCost, Long.MAX_VALUE);
        minCost[0] = 0;
        boolean[] inTree = new boolean[n];
        long total = 0;
        for (int step = 0; step < n; step++) {
            int u = -1;
            for (int v = 0; v < n; v++) if (!inTree[v] && (u == -1 || minCost[v] < minCost[u])) u = v;   // cheapest to add
            inTree[u] = true;
            total += minCost[u];
            for (int v = 0; v < n; v++) {
                long d = Math.abs(pts[u][0] - pts[v][0]) + Math.abs(pts[u][1] - pts[v][1]);   // Manhattan distance
                if (!inTree[v] && d < minCost[v]) minCost[v] = d;
            }
        }
        return total;
    }

    // brute force for tiny graphs: try every subset of n-1 edges
    static long brute(int n, int[][] edges) {
        long best = -1;
        int m = edges.length;
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != n - 1) continue;
            parent = new int[n]; size = new int[n];
            for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
            long w = 0;
            boolean tree = true;
            for (int i = 0; i < m && tree; i++) if ((mask >> i & 1) == 1) { tree = union(edges[i][0], edges[i][1]); w += edges[i][2]; }
            if (tree && (best == -1 || w < best)) best = w;
        }
        return best;
    }

    public static void main(String[] args) {
        // A=0, B=1, C=2, D=3
        int[][] edges = {{0, 1, 1}, {1, 2, 2}, {2, 3, 3}, {0, 3, 5}, {0, 2, 4}};
        check("Prim", 6L, prim(4, edges));
        check("Kruskal", 6L, kruskal(4, edges));
        check("disconnected -> -1", -1L, prim(3, new int[][]{{0, 1, 1}}));
        check("dense Prim on points", 20L, primDense(new int[][]{{0, 0}, {2, 2}, {3, 10}, {5, 2}, {7, 0}}));

        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(6), m = rnd.nextInt(10);
            int[][] es = new int[m][];
            for (int i = 0; i < m; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), rnd.nextInt(20)};
            long p = prim(n, es), k = kruskal(n, es);
            long b = n == 1 ? 0 : brute(n, es);
            ok &= p == k && p == b;
        }
        check("300 random graphs: Prim == Kruskal == brute", true, ok);
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
