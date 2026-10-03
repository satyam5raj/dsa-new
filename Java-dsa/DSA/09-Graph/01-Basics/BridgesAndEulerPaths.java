/*
 * ============================================================================
 *  GRAPH BASICS 6 (advanced): BRIDGES (Tarjan) and EULER PATHS (Hierholzer)
 * ============================================================================
 *
 *  Two classic DFS-based algorithms that show up in the hard problems
 *  (Critical Connections H05, Reconstruct Itinerary H04).
 *
 *  ---------------------------------------------------------------------------
 *  1. BRIDGES: an edge whose removal DISCONNECTS the graph ("critical connection").
 *
 *     0 --- 1          edges in the triangle 0-1-2 are NOT bridges
 *      \   /           (remove one, the other two still connect them)
 *        2 --- 3       2-3 IS a bridge: it's the only way to reach 3
 *
 *     Brute force: remove each edge, check connectivity -> O(E * (V + E)).
 *
 *     TARJAN, one DFS, O(V + E):
 *       disc[u] = the time u was discovered (a DFS counter)
 *       low[u]  = the EARLIEST discovery time reachable from u's subtree using
 *                 tree edges down and at most ONE back edge up
 *       For a tree edge u -> v:
 *          low[v] > disc[u]  =>  nothing under v can climb back to u or above
 *                            =>  u-v is a BRIDGE
 *       Computing low: low[u] = min(disc[u], disc[w] for back edges u-w (w not the parent),
 *                                   low[v] for children v)
 *     (Articulation POINTS - vertices whose removal disconnects - use low[v] >= disc[u].)
 *
 *  ---------------------------------------------------------------------------
 *  2. EULER PATH: a path that uses EVERY EDGE exactly once ("draw it without lifting
 *     the pen"). Directed graph, it exists iff (ignoring isolated vertices) it's
 *     connected and either every vertex has in == out (an Euler CIRCUIT), or exactly
 *     one vertex has out = in + 1 (the START) and one has in = out + 1 (the END).
 *
 *     HIERHOLZER, O(E): DFS from the start, CONSUMING each edge as you use it;
 *     append a vertex to the answer when it has NO unused edges left (postorder);
 *     reverse at the end. Dead ends get placed at the END of the path automatically,
 *     and detours ("sub-cycles") get spliced in where they belong.
 *
 *  Run:  java BridgesAndEulerPaths.java
 * ============================================================================
 */

import java.util.*;

public class BridgesAndEulerPaths {

    // ---------------- Tarjan's bridges ----------------
    static int timer;
    static int[] disc, low;
    static List<List<Integer>> adj;
    static List<List<Integer>> bridges;

    static List<List<Integer>> findBridges(int n, int[][] edges) {
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        disc = new int[n];
        low = new int[n];
        Arrays.fill(disc, -1);
        timer = 0;
        bridges = new ArrayList<>();
        for (int u = 0; u < n; u++) if (disc[u] == -1) dfs(u, -1);
        return bridges;
    }

    static void dfs(int u, int parent) {
        disc[u] = low[u] = timer++;
        for (int v : adj.get(u)) {
            if (v == parent) continue;                      // don't use the edge we came in on
            if (disc[v] == -1) {                            // tree edge
                dfs(v, u);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u]) bridges.add(List.of(Math.min(u, v), Math.max(u, v)));
            } else {                                        // back edge to an ancestor
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    // brute force: remove each edge and count components
    static List<List<Integer>> bruteBridges(int n, int[][] edges) {
        List<List<Integer>> out = new ArrayList<>();
        int base = components(n, edges, -1);
        for (int i = 0; i < edges.length; i++) {
            if (components(n, edges, i) > base) out.add(List.of(Math.min(edges[i][0], edges[i][1]), Math.max(edges[i][0], edges[i][1])));
        }
        return out;
    }

    static int components(int n, int[][] edges, int skip) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        int c = n;
        for (int i = 0; i < edges.length; i++) {
            if (i == skip) continue;
            int a = root(p, edges[i][0]), b = root(p, edges[i][1]);
            if (a != b) { p[a] = b; c--; }
        }
        return c;
    }

    static int root(int[] p, int x) { while (p[x] != x) x = p[x]; return x; }

    // ---------------- Hierholzer's Euler path (directed) ----------------
    static List<Integer> eulerPath(int n, int[][] edges) {
        List<Deque<Integer>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new ArrayDeque<>());
        int[] in = new int[n], outDeg = new int[n];
        for (int[] e : edges) { out.get(e[0]).add(e[1]); outDeg[e[0]]++; in[e[1]]++; }
        int start = -1;
        for (int v = 0; v < n; v++) if (outDeg[v] - in[v] == 1) start = v;          // the unique start, if any
        if (start == -1) for (int v = 0; v < n; v++) if (outDeg[v] > 0) { start = v; break; }
        if (start == -1) return List.of();
        LinkedList<Integer> path = new LinkedList<>();
        Deque<Integer> stack = new ArrayDeque<>(List.of(start));                     // iterative DFS
        while (!stack.isEmpty()) {
            int u = stack.peek();
            if (!out.get(u).isEmpty()) stack.push(out.get(u).poll());                // consume an edge
            else path.addFirst(stack.pop());                                         // stuck: finalize u
        }
        return path.size() == edges.length + 1 ? path : List.of();                  // used every edge?
    }

    public static void main(String[] args) {
        System.out.println("1. Bridges");
        check("triangle + tail", "[[2, 3]]", findBridges(4, new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 3}}));
        check("a path: every edge is a bridge", "[[2, 3], [1, 2], [0, 1]]", findBridges(4, new int[][]{{0, 1}, {1, 2}, {2, 3}}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(9);
            Set<String> seen = new HashSet<>();
            List<int[]> es = new ArrayList<>();
            for (int i = 0; i < n + rnd.nextInt(n + 1); i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a == b || !seen.add(Math.min(a, b) + "," + Math.max(a, b))) continue;   // simple graph
                es.add(new int[]{a, b});
            }
            int[][] e = es.toArray(new int[0][]);
            Comparator<List<Integer>> cmp = Comparator.<List<Integer>>comparingInt(x -> x.get(0)).thenComparingInt(x -> x.get(1));
            List<List<Integer>> a = new ArrayList<>(findBridges(n, e)), b = new ArrayList<>(bruteBridges(n, e));
            a.sort(cmp);
            b.sort(cmp);
            ok &= a.equals(b);
        }
        check("500 random graphs vs remove-each-edge", true, ok);

        System.out.println("2. Euler path (Hierholzer)");
        // 0->1->2->0 is a cycle; 0->3->4 hangs off it. Start must be 0 (out - in = 1)
        check("cycle with a detour", "[0, 1, 2, 0, 3, 4]", eulerPath(5, new int[][]{{0, 1}, {1, 2}, {2, 0}, {0, 3}, {3, 4}}));
        check("no Euler path (two starts)", "[]", eulerPath(3, new int[][]{{0, 2}, {1, 2}}));
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
