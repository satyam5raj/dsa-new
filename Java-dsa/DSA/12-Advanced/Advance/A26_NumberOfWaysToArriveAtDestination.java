/*
 * ============================================================================
 *  ADVANCE 26 : NUMBER OF WAYS TO ARRIVE AT DESTINATION  ⭐ bonus   LeetCode #1976
 *  https://leetcode.com/problems/number-of-ways-to-arrive-at-destination/
 *  Pattern: DIJKSTRA (min-heap) + a COUNTING DP carried along with the distances
 * ============================================================================
 *
 *  PROBLEM
 *    n intersections, roads[i] = [u, v, time] (undirected, time >= 1). How many
 *    different SHORTEST routes are there from 0 to n-1? Answer modulo 10^9 + 7.
 *      n = 7, roads [[0,6,7],[0,1,2],[1,2,3],[1,3,3],[6,3,3],[3,5,1],[6,5,1],[2,5,1],
 *                    [0,4,5],[4,6,2]]   ->  4   (all four routes take 7 minutes)
 *
 *  ---------------------------------------------------------------------------
 *  TWO NUMBERS PER NODE
 *    dist[v] = shortest time from 0 to v        (Dijkstra)
 *    ways[v] = how many routes achieve dist[v]  (counting DP)
 *  When Dijkstra relaxes edge u -> v with nd = dist[u] + w:
 *    nd <  dist[v]  a strictly better route: dist[v] = nd, ways[v] = ways[u]  (RESET)
 *    nd == dist[v]  another equally short way:           ways[v] += ways[u]  (ADD)
 *  Why ways[u] is final when u is popped: every shortest route into u comes from a node
 *  with SMALLER distance (times are >= 1), and Dijkstra pops nodes in increasing
 *  distance - so all of them were already popped and added their ways.
 *
 *  THE BUG TO AVOID: a heap can hold STALE entries for u (pushed before a shorter route
 *  was found). If you process a stale entry you add ways[u] to neighbours TWICE.
 *  Skip entries whose distance is bigger than dist[u].
 *  Distances use long: up to 200 nodes x 10^9 per road.
 *
 *  DRY RUN (small) n=4, roads 0-1 (1), 0-2 (1), 1-3 (1), 2-3 (1)
 *    pop 0: dist1 = 1 ways1 = 1;  dist2 = 1 ways2 = 1
 *    pop 1: dist3 = 2 ways3 = 1
 *    pop 2: 1 + 1 == dist3 -> ways3 = 2
 *    answer 2   (0-1-3 and 0-2-3)
 *
 *  COMPLEXITY   O(E log V)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class A26_NumberOfWaysToArriveAtDestination {

    static final int MOD = 1_000_000_007;

    // ---------------- Referee: Bellman-Ford distances, then count over nodes sorted by distance ----------------
    static int countPathsBrute(int n, int[][] roads) {
        long[] dist = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[0] = 0;
        for (int round = 0; round < n - 1; round++)                       // relax every edge n-1 times
            for (int[] r : roads) {
                if (dist[r[0]] != Long.MAX_VALUE) dist[r[1]] = Math.min(dist[r[1]], dist[r[0]] + r[2]);
                if (dist[r[1]] != Long.MAX_VALUE) dist[r[0]] = Math.min(dist[r[0]], dist[r[1]] + r[2]);
            }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingLong(i -> dist[i]));
        long[] ways = new long[n];
        ways[0] = 1;
        for (int v : order)                                               // predecessors always come earlier
            for (int[] r : roads) {
                if (r[1] == v && dist[r[0]] + r[2] == dist[v]) ways[v] = (ways[v] + ways[r[0]]) % MOD;
                if (r[0] == v && dist[r[1]] + r[2] == dist[v]) ways[v] = (ways[v] + ways[r[1]]) % MOD;
            }
        return (int) ways[n - 1];
    }

    // ---------------- Main solution: Dijkstra + ways ----------------
    static int countPaths(int n, int[][] roads) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] r : roads) { adj.get(r[0]).add(new int[]{r[1], r[2]}); adj.get(r[1]).add(new int[]{r[0], r[2]}); }

        long[] dist = new long[n];
        long[] ways = new long[n];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[0] = 0;
        ways[0] = 1;
        PriorityQueue<long[]> heap = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {distance, node}
        heap.add(new long[]{0, 0});
        while (!heap.isEmpty()) {
            long[] top = heap.poll();
            int u = (int) top[1];
            if (top[0] > dist[u]) continue;                               // stale entry: u was improved later
            for (int[] e : adj.get(u)) {
                int v = e[0];
                long nd = dist[u] + e[1];
                if (nd < dist[v]) {                                       // strictly shorter: reset the count
                    dist[v] = nd;
                    ways[v] = ways[u];
                    heap.add(new long[]{nd, v});
                } else if (nd == dist[v]) {                               // equally short: add the routes through u
                    ways[v] = (ways[v] + ways[u]) % MOD;
                }
            }
        }
        return (int) ways[n - 1];
    }

    public static void main(String[] args) {
        int[][] ex1 = {{0, 6, 7}, {0, 1, 2}, {1, 2, 3}, {1, 3, 3}, {6, 3, 3}, {3, 5, 1}, {6, 5, 1}, {2, 5, 1}, {0, 4, 5}, {4, 6, 2}};
        check("example 1", 4, countPaths(7, ex1));
        check("example 2", 1, countPaths(2, new int[][]{{1, 0, 10}}));
        check("a square: two routes", 2, countPaths(4, new int[][]{{0, 1, 1}, {0, 2, 1}, {1, 3, 1}, {2, 3, 1}}));
        List<int[]> ladder = new ArrayList<>();                           // 2^60 shortest routes -> needs the modulo
        for (int i = 0; i < 60; i++) { int a = 2 * i; ladder.add(new int[]{a, a + 1, 1}); ladder.add(new int[]{a, a + 2, 2}); ladder.add(new int[]{a + 1, a + 2, 1}); }
        check("2^60 routes, modulo 1e9+7", (int) modPow(2, 60), countPaths(121, ladder.toArray(new int[0][])));

        Random rnd = new Random(26);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            int n = 2 + rnd.nextInt(7);
            Set<Long> used = new HashSet<>();
            List<int[]> roads = new ArrayList<>();
            for (int i = 1; i < n; i++) { int p = rnd.nextInt(i); roads.add(new int[]{i, p, 1 + rnd.nextInt(3)}); used.add((long) Math.min(i, p) * 100 + Math.max(i, p)); }
            for (int e = rnd.nextInt(10); e > 0; e--) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b && used.add((long) Math.min(a, b) * 100 + Math.max(a, b))) roads.add(new int[]{a, b, 1 + rnd.nextInt(3)});
            }
            int[][] r = roads.toArray(new int[0][]);
            ok &= countPaths(n, r) == countPathsBrute(n, r);
        }
        check("1,500 random graphs vs Bellman-Ford + counting", true, ok);
        summary();
    }

    static long modPow(long b, int e) { long r = 1; for (int i = 0; i < e; i++) r = r * b % MOD; return r; }

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
