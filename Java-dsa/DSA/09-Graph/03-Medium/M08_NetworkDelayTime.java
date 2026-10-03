/*
 * ============================================================================
 *  MEDIUM 08 : NETWORK DELAY TIME            LeetCode #743
 *  https://leetcode.com/problems/network-delay-time/
 *  Pattern: DIJKSTRA from one source; answer = the LARGEST shortest distance
 * ============================================================================
 *
 *  PROBLEM
 *    n nodes (1..n), times[i] = [u, v, w]: a signal takes w to go u -> v.
 *    A signal starts at k. How long until ALL nodes have it? -1 if some never do.
 *      times [[2,1,1],[2,3,1],[3,4,1]], n=4, k=2 -> 2
 *
 *  ---------------------------------------------------------------------------
 *  Every node receives the signal at its SHORTEST-path time from k (the signal
 *  travels all edges at once). Everyone has it once the LAST one does:
 *      answer = max over v of dist[v]   (or -1 if some dist is infinite)
 *  Non-negative weights -> DIJKSTRA (01-Basics/ShortestPaths).
 *
 *  DIJKSTRA RECAP
 *    dist[k] = 0; min-heap of {dist, node}
 *    pop the closest; skip if stale; relax each outgoing edge; push improvements
 *
 *  DRY RUN k=2
 *    pop 2 (0): 1 -> 1, 3 -> 1
 *    pop 1 (1): no edges;  pop 3 (1): 4 -> 2;  pop 4 (2)
 *    dist {1:1, 2:0, 3:1, 4:2} -> max 2
 *
 *  ALTERNATIVES: Bellman-Ford O(V*E) works too (and handles negative weights);
 *  for n <= 100 even Floyd-Warshall O(n^3) is fine. Used below as referees.
 *
 *  COMPLEXITY   Time O(E log V)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M08_NetworkDelayTime {

    static int networkDelayTime(int[][] times, int n, int k) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
        for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]});
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
        heap.add(new int[]{0, k});
        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int d = top[0], u = top[1];
            if (d > dist[u]) continue;                       // stale
            for (int[] e : adj.get(u)) {
                if (d + e[1] < dist[e[0]]) {
                    dist[e[0]] = d + e[1];
                    heap.add(new int[]{dist[e[0]], e[0]});
                }
            }
        }
        int max = 0;
        for (int v = 1; v <= n; v++) {
            if (dist[v] == Integer.MAX_VALUE) return -1;     // someone never hears it
            max = Math.max(max, dist[v]);
        }
        return max;
    }

    static int bellmanFord(int[][] times, int n, int k) {
        long[] d = new long[n + 1];
        Arrays.fill(d, Long.MAX_VALUE / 2);
        d[k] = 0;
        for (int i = 0; i < n - 1; i++) for (int[] t : times) d[t[1]] = Math.min(d[t[1]], d[t[0]] + t[2]);
        long max = 0;
        for (int v = 1; v <= n; v++) max = Math.max(max, d[v]);
        return max >= Long.MAX_VALUE / 2 ? -1 : (int) max;
    }

    public static void main(String[] args) {
        check("example", 2, networkDelayTime(new int[][]{{2, 1, 1}, {2, 3, 1}, {3, 4, 1}}, 4, 2));
        check("[[1,2,1]] n=2 k=1", 1, networkDelayTime(new int[][]{{1, 2, 1}}, 2, 1));
        check("[[1,2,1]] n=2 k=2 (unreachable)", -1, networkDelayTime(new int[][]{{1, 2, 1}}, 2, 2));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] times = new int[rnd.nextInt(20)][];
            for (int i = 0; i < times.length; i++) times[i] = new int[]{1 + rnd.nextInt(n), 1 + rnd.nextInt(n), rnd.nextInt(10)};
            int k = 1 + rnd.nextInt(n);
            ok &= networkDelayTime(times, n, k) == bellmanFord(times, n, k);
        }
        check("1,000 random networks vs Bellman-Ford", true, ok);
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
