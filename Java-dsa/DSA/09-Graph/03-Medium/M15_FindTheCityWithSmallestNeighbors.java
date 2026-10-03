/*
 * ============================================================================
 *  MEDIUM 15 : FIND THE CITY WITH THE SMALLEST NUMBER OF NEIGHBORS AT A THRESHOLD DISTANCE
 *  LeetCode #1334
 *  https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/
 *  Pattern: ALL-PAIRS shortest paths -> FLOYD-WARSHALL (n <= 100)
 * ============================================================================
 *
 *  PROBLEM
 *    n cities, weighted undirected roads. For each city, count the other cities
 *    reachable with total distance <= distanceThreshold. Return the city with the
 *    SMALLEST count; on a tie, the one with the LARGEST index.
 *      n=4, [[0,1,3],[1,2,1],[1,3,4],[2,3,1]], threshold 4 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  We need the distance between EVERY pair -> all-pairs shortest paths.
 *    FLOYD-WARSHALL O(n^3): 10^6 steps for n = 100 - simplest code
 *      for k: for i: for j: d[i][j] = min(d[i][j], d[i][k] + d[k][j])
 *      (k OUTERMOST: after step k, d[i][j] is the best path using only cities
 *       0..k as intermediate stops. Putting k inside is a classic bug.)
 *    Or n x Dijkstra: O(n * E log n) - better for big sparse graphs.
 *  (leetcode.md files it under Bellman-Ford; n x Bellman-Ford also works,
 *   O(n^2 * E) - Floyd is the natural choice here.)
 *
 *  THEN: count per city, pick min count, ties -> larger index (iterate with <=).
 *
 *  DRY RUN distances from each city within 4:
 *    0: {1,2}  1: {0,2,3}  2: {0,1,3}  3: {1,2}   -> min count 2: cities 0 and 3 -> 3
 *
 *  COMPLEXITY   Time O(n^3)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M15_FindTheCityWithSmallestNeighbors {

    static int findTheCity(int n, int[][] edges, int distanceThreshold) {
        int INF = 1_000_000_000;
        int[][] d = new int[n][n];
        for (int[] row : d) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) d[i][i] = 0;
        for (int[] e : edges) {
            d[e[0]][e[1]] = Math.min(d[e[0]][e[1]], e[2]);
            d[e[1]][e[0]] = Math.min(d[e[1]][e[0]], e[2]);
        }
        for (int k = 0; k < n; k++)                          // k = allowed intermediate city
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
        int best = -1, bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int count = 0;
            for (int j = 0; j < n; j++) if (i != j && d[i][j] <= distanceThreshold) count++;
            if (count <= bestCount) { bestCount = count; best = i; }   // <= : ties go to the larger index
        }
        return best;
    }

    // referee: Dijkstra from every city
    static int withDijkstra(int n, int[][] edges, int th) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(new int[]{e[1], e[2]}); adj.get(e[1]).add(new int[]{e[0], e[2]}); }
        int best = -1, bestCount = Integer.MAX_VALUE;
        for (int s = 0; s < n; s++) {
            int[] dist = new int[n];
            Arrays.fill(dist, Integer.MAX_VALUE);
            dist[s] = 0;
            PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
            pq.add(new int[]{0, s});
            while (!pq.isEmpty()) {
                int[] t = pq.poll();
                if (t[0] > dist[t[1]]) continue;
                for (int[] e : adj.get(t[1])) if (t[0] + e[1] < dist[e[0]]) { dist[e[0]] = t[0] + e[1]; pq.add(new int[]{dist[e[0]], e[0]}); }
            }
            int c = 0;
            for (int j = 0; j < n; j++) if (j != s && dist[j] <= th) c++;
            if (c <= bestCount) { bestCount = c; best = s; }
        }
        return best;
    }

    public static void main(String[] args) {
        check("example 1", 3, findTheCity(4, new int[][]{{0, 1, 3}, {1, 2, 1}, {1, 3, 4}, {2, 3, 1}}, 4));
        check("example 2", 0, findTheCity(5, new int[][]{{0, 1, 2}, {0, 4, 8}, {1, 2, 3}, {1, 4, 2}, {2, 3, 1}, {3, 4, 1}}, 2));
        Random rnd = new Random(15);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 2 + rnd.nextInt(8);
            int[][] es = new int[rnd.nextInt(15)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(10)};
            int th = rnd.nextInt(20);
            ok &= findTheCity(n, es, th) == withDijkstra(n, es, th);
        }
        check("500 random maps: Floyd == n x Dijkstra", true, ok);
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
