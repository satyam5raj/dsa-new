/*
 * ============================================================================
 *  GRAPH BASICS 4: SHORTEST PATHS - which algorithm when?
 * ============================================================================
 *
 *   edge weights          algorithm              time              notes
 *   all equal (1)         BFS                    O(V + E)          GraphTraversal
 *   only 0 or 1           0-1 BFS (deque)        O(V + E)          0-edges to the FRONT
 *   non-negative          DIJKSTRA (heap)        O(E log V)        the workhorse
 *   negative allowed      BELLMAN-FORD           O(V * E)          detects negative cycles
 *   all pairs, small V    FLOYD-WARSHALL         O(V^3)            V <= ~400
 *   DAG (any weights)     topological order + relax   O(V + E)
 *
 *  ---------------------------------------------------------------------------
 *  DIJKSTRA: "BFS with a priority queue"
 *    dist[src] = 0, everything else infinity; heap of {dist, vertex}
 *    pop the closest unfinished vertex u (if the popped dist is STALE - bigger
 *    than dist[u] - skip it: lazy deletion), then RELAX its edges:
 *        if dist[u] + w < dist[v]:  dist[v] = dist[u] + w; push {dist[v], v}
 *    WHY IT WORKS: with non-negative weights, the closest unfinished vertex can't
 *    be improved later (any other route goes through something at least as far).
 *    WHY NEGATIVE EDGES BREAK IT: a later negative edge CAN improve a finished vertex.
 *
 *  BELLMAN-FORD: relax EVERY edge, V-1 times. A shortest path has at most V-1
 *    edges, and round i fixes all paths of i edges. If round V still improves
 *    something -> a NEGATIVE CYCLE is reachable (no shortest path exists).
 *    "At most k edges" = stop after k rounds (Cheapest Flights Within K Stops) -
 *    but relax from a COPY of the previous round's distances.
 *
 *  FLOYD-WARSHALL: dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j]) for every
 *    k as an allowed intermediate vertex. Three nested loops, k OUTERMOST.
 *
 *  0-1 BFS: with weights 0 and 1 only, a deque keeps it sorted: push a 0-edge
 *    target to the FRONT (same distance), a 1-edge target to the BACK.
 *
 *  This file implements all of them and checks they agree on random graphs.
 *  Run:  java ShortestPaths.java
 * ============================================================================
 */

import java.util.*;

public class ShortestPaths {

    static final long INF = Long.MAX_VALUE / 4;

    static List<List<int[]>> graph(int n, int[][] edges) {        // directed, {to, w}
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(new int[]{e[1], e[2]});
        return adj;
    }

    static long[] dijkstra(int n, int[][] edges, int src) {
        List<List<int[]>> adj = graph(n, edges);
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;
        PriorityQueue<long[]> heap = new PriorityQueue<>(Comparator.comparingLong(x -> x[0]));
        heap.add(new long[]{0, src});
        while (!heap.isEmpty()) {
            long[] top = heap.poll();
            int u = (int) top[1];
            if (top[0] > dist[u]) continue;                       // stale entry: skip
            for (int[] e : adj.get(u)) {
                long nd = dist[u] + e[1];
                if (nd < dist[e[0]]) {                            // relax
                    dist[e[0]] = nd;
                    heap.add(new long[]{nd, e[0]});
                }
            }
        }
        return dist;
    }

    // Textbook Dijkstra: once a vertex is popped it is FINAL and never revisited.
    // Correct for non-negative weights; shown here only to see it fail on a negative edge.
    // (The lazy version above re-pushes improved vertices, so it often survives a negative
    //  edge - but can take exponential time. Use Bellman-Ford when weights can be negative.)
    static long[] dijkstraFinalized(int n, int[][] edges, int src) {
        List<List<int[]>> adj = graph(n, edges);
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;
        boolean[] done = new boolean[n];
        PriorityQueue<long[]> heap = new PriorityQueue<>(Comparator.comparingLong(x -> x[0]));
        heap.add(new long[]{0, src});
        while (!heap.isEmpty()) {
            int u = (int) heap.poll()[1];
            if (done[u]) continue;
            done[u] = true;
            for (int[] e : adj.get(u)) {
                if (!done[e[0]] && dist[u] + e[1] < dist[e[0]]) {
                    dist[e[0]] = dist[u] + e[1];
                    heap.add(new long[]{dist[e[0]], e[0]});
                }
            }
        }
        return dist;
    }

    // Returns null if a negative cycle is reachable from src.
    static long[] bellmanFord(int n, int[][] edges, int src) {
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;
        for (int round = 0; round < n - 1; round++) {
            boolean changed = false;
            for (int[] e : edges) {
                if (dist[e[0]] < INF && dist[e[0]] + e[2] < dist[e[1]]) { dist[e[1]] = dist[e[0]] + e[2]; changed = true; }
            }
            if (!changed) break;                                  // early exit: already optimal
        }
        for (int[] e : edges) if (dist[e[0]] < INF && dist[e[0]] + e[2] < dist[e[1]]) return null;   // round V improves -> negative cycle
        return dist;
    }

    static long[][] floydWarshall(int n, int[][] edges) {
        long[][] d = new long[n][n];
        for (long[] row : d) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) d[i][i] = 0;
        for (int[] e : edges) d[e[0]][e[1]] = Math.min(d[e[0]][e[1]], e[2]);
        for (int k = 0; k < n; k++)                               // k = the newly allowed middle vertex
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (d[i][k] < INF && d[k][j] < INF) d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);
        return d;
    }

    static long[] zeroOneBfs(int n, int[][] edges, int src) {     // weights must be 0 or 1
        List<List<int[]>> adj = graph(n, edges);
        long[] dist = new long[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;
        Deque<Integer> dq = new ArrayDeque<>(List.of(src));
        while (!dq.isEmpty()) {
            int u = dq.pollFirst();
            for (int[] e : adj.get(u)) {
                if (dist[u] + e[1] < dist[e[0]]) {
                    dist[e[0]] = dist[u] + e[1];
                    if (e[1] == 0) dq.addFirst(e[0]); else dq.addLast(e[0]);
                }
            }
        }
        return dist;
    }

    static String show(long[] d) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < d.length; i++) sb.append(i > 0 ? ", " : "").append(d[i] >= INF ? "inf" : String.valueOf(d[i]));
        return sb.append("]").toString();
    }

    public static void main(String[] args) {
        //   0 --4--> 1 --1--> 3
        //   |        ^
        //   1        2
        //   v        |
        //   2 -------+   2 --5--> 3
        int[][] edges = {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}};
        System.out.println("1. Dijkstra");
        check("from 0 (0->2->1->3 = 4)", "[0, 3, 1, 4]", show(dijkstra(4, edges, 0)));

        System.out.println("2. Bellman-Ford (negative edges OK)");
        int[][] neg = {{0, 1, 4}, {0, 2, 5}, {2, 1, -3}};
        check("with a -3 edge", "[0, 2, 5]", show(bellmanFord(3, neg, 0)));
        check("classic Dijkstra is WRONG here", "[0, 4, 5]", show(dijkstraFinalized(3, neg, 0)));
        check("negative cycle -> null", null, bellmanFord(3, new int[][]{{0, 1, 1}, {1, 2, -2}, {2, 1, 1}}, 0));

        System.out.println("3. Floyd-Warshall (all pairs)");
        long[][] fw = floydWarshall(4, edges);
        check("row 0", "[0, 3, 1, 4]", show(fw[0]));
        check("row 2", "[inf, 2, 0, 3]", show(fw[2]));

        System.out.println("4. 0-1 BFS");
        check("0/1 weights", "[0, 0, 1, 1]", show(zeroOneBfs(4, new int[][]{{0, 1, 0}, {1, 2, 1}, {0, 2, 1}, {2, 3, 0}}, 0)));

        System.out.println("5. 300 random graphs: all algorithms agree");
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(12), m = rnd.nextInt(40);
            int[][] es = new int[m][];
            for (int i = 0; i < m; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), rnd.nextInt(2) == 0 ? rnd.nextInt(2) : rnd.nextInt(20)};
            long[] d = dijkstra(n, es, 0), b = bellmanFord(n, es, 0);
            long[][] f = floydWarshall(n, es);
            ok &= Arrays.equals(d, b) && Arrays.equals(d, f[0]);
            int[][] zo = new int[m][];
            for (int i = 0; i < m; i++) zo[i] = new int[]{es[i][0], es[i][1], es[i][2] % 2};
            ok &= Arrays.equals(zeroOneBfs(n, zo, 0), dijkstra(n, zo, 0));
        }
        check("Dijkstra == Bellman-Ford == Floyd, 0-1 BFS == Dijkstra", true, ok);
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
