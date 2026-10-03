/*
 * ============================================================================
 *  GRAPH BASICS 1: vocabulary and the 3 ways to store a graph
 * ============================================================================
 *
 *  A GRAPH = VERTICES (nodes) + EDGES (connections between pairs of vertices).
 *  Trees, linked lists and grids are all special graphs. Unlike a tree, a graph
 *  can have CYCLES, several components, and no root.
 *
 *  VOCABULARY
 *    undirected  : an edge u-v works both ways (friendship)
 *    directed    : an edge u->v is one-way (follows, prerequisites, links)
 *    weighted    : edges carry a cost/distance/time
 *    degree      : number of edges at a vertex (directed: in-degree / out-degree)
 *    path        : a sequence of vertices joined by edges;  CYCLE: a path back to its start
 *    connected   : (undirected) every vertex reaches every other
 *    component   : a maximal connected piece
 *    DAG         : Directed Acyclic Graph - prerequisites, build systems (topological sort)
 *    tree        : connected + acyclic = exactly V - 1 edges
 *    dense/sparse: E close to V^2 / E close to V
 *
 *  ---------------------------------------------------------------------------
 *  REPRESENTATIONS (V vertices, E edges)
 *
 *    0 --- 1
 *    |   / |          edges: 0-1, 0-2, 1-2, 1-3
 *    |  /  |
 *    2     3
 *
 *  1. EDGE LIST   [[0,1],[0,2],[1,2],[1,3]]           space O(E)
 *     How problems usually GIVE you the graph. Good for Kruskal (sort edges),
 *     Bellman-Ford. Bad for "who are u's neighbours?" (O(E) scan).
 *
 *  2. ADJACENCY MATRIX  m[u][v] = 1 (or the weight)    space O(V^2)
 *          0 1 2 3
 *       0 [0 1 1 0]
 *       1 [1 0 1 1]       edge check O(1); neighbours O(V);
 *       2 [1 1 0 0]       good for DENSE graphs and Floyd-Warshall.
 *       3 [0 1 0 0]       10^5 vertices -> 10^10 cells: impossible.
 *
 *  3. ADJACENCY LIST  adj[u] = list of neighbours      space O(V + E)
 *       0: [1, 2]   1: [0, 2, 3]   2: [0, 1]   3: [1]
 *     neighbours in O(degree). THE default for interviews (BFS, DFS, Dijkstra).
 *     In Java: List<List<Integer>> (or List<int[]> per vertex for {neighbour, weight}),
 *     or Map<String, List<String>> when vertices are names.
 *
 *  4. IMPLICIT GRAPHS: never built at all. A GRID: each cell is a vertex, its 4
 *     neighbours are (r±1, c) and (r, c±1). Or "words that differ by one letter"
 *     (Word Ladder). You generate neighbours on the fly.
 *
 *  Run:  java GraphRepresentation.java
 * ============================================================================
 */

import java.util.*;

public class GraphRepresentation {

    // Edge list -> adjacency list. For an UNDIRECTED graph add BOTH directions.
    static List<List<Integer>> buildAdjList(int n, int[][] edges, boolean directed) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            if (!directed) adj.get(e[1]).add(e[0]);
        }
        return adj;
    }

    // Weighted: store {neighbour, weight} pairs
    static List<List<int[]>> buildWeighted(int n, int[][] edges, boolean directed) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(new int[]{e[1], e[2]});
            if (!directed) adj.get(e[1]).add(new int[]{e[0], e[2]});
        }
        return adj;
    }

    static int[][] buildMatrix(int n, int[][] edges, boolean directed) {
        int[][] m = new int[n][n];
        for (int[] e : edges) {
            m[e[0]][e[1]] = 1;
            if (!directed) m[e[1]][e[0]] = 1;
        }
        return m;
    }

    // Vertices with NAMES: a map is the adjacency list
    static Map<String, List<String>> buildNamed(String[][] edges) {
        Map<String, List<String>> g = new TreeMap<>();
        for (String[] e : edges) {
            g.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            g.computeIfAbsent(e[1], k -> new ArrayList<>());          // make sure sinks exist too
        }
        return g;
    }

    // Grid as an implicit graph: the 4 neighbours of (r, c) that are inside the grid
    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static List<int[]> gridNeighbours(int rows, int cols, int r, int c) {
        List<int[]> out = new ArrayList<>();
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) out.add(new int[]{nr, nc});
        }
        return out;
    }

    public static void main(String[] args) {
        int[][] edges = {{0, 1}, {0, 2}, {1, 2}, {1, 3}};

        System.out.println("1. Adjacency list (undirected)");
        List<List<Integer>> adj = buildAdjList(4, edges, false);
        check("adj", "[[1, 2], [0, 2, 3], [0, 1], [1]]", adj);
        check("degree of 1", 3, adj.get(1).size());
        check("sum of degrees = 2E", 8, adj.stream().mapToInt(List::size).sum());

        System.out.println("2. Directed: in-degree and out-degree");
        List<List<Integer>> dir = buildAdjList(4, edges, true);
        int[] indeg = new int[4];
        for (List<Integer> l : dir) for (int v : l) indeg[v]++;
        check("out-degrees", "[2, 2, 0, 0]", dir.stream().mapToInt(List::size).toArray());
        check("in-degrees", "[0, 1, 2, 1]", indeg);

        System.out.println("3. Adjacency matrix");
        int[][] m = buildMatrix(4, edges, false);
        check("matrix", "[[0, 1, 1, 0], [1, 0, 1, 1], [1, 1, 0, 0], [0, 1, 0, 0]]", m);
        check("edge 2-3? O(1)", 0, m[2][3]);

        System.out.println("4. Weighted adjacency list");
        List<List<int[]>> w = buildWeighted(3, new int[][]{{0, 1, 5}, {1, 2, 3}}, false);
        check("neighbours of 1 as {v, w}", "1:[0, 5], [2, 3]", "1:" + Arrays.toString(w.get(1).get(0)) + ", " + Arrays.toString(w.get(1).get(1)));

        System.out.println("5. Named vertices -> Map<String, List<String>>");
        check("named graph content", "{ATL=[], JFK=[SFO, ATL], SFO=[ATL]}",
                buildNamed(new String[][]{{"JFK", "SFO"}, {"JFK", "ATL"}, {"SFO", "ATL"}}));

        System.out.println("6. Grid as an implicit graph");
        check("neighbours of corner (0,0) in 3x3", 2, gridNeighbours(3, 3, 0, 0).size());
        check("neighbours of centre (1,1)", 4, gridNeighbours(3, 3, 1, 1).size());

        System.out.println("7. Memory: matrix vs list for a sparse graph (V = 20,000, E = 60,000)");
        long matrixBytes = 20_000L * 20_000L * 4;
        long listEntries = 2L * 60_000;
        System.out.printf("   matrix: %,d MB      list: ~%,d entries (a few MB)%n", matrixBytes / 1_000_000, listEntries);
        check("matrix would need 1.6 GB", 1600, (int) (matrixBytes / 1_000_000));
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
