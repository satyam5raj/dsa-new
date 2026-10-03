/*
 * ============================================================================
 *  MEDIUM 09 : CHEAPEST FLIGHTS WITHIN K STOPS   LeetCode #787
 *  https://leetcode.com/problems/cheapest-flights-within-k-stops/
 *  Pattern: BELLMAN-FORD limited to k+1 rounds (edges), relaxing from a COPY
 * ============================================================================
 *
 *  PROBLEM
 *    flights [from, to, price]. Cheapest price from src to dst using AT MOST k
 *    stops (= at most k+1 flights). -1 if impossible.
 *      n=4, [[0,1,100],[1,2,100],[2,0,100],[1,3,600],[2,3,200]], 0 -> 3, k=1 -> 700
 *        0->1->2->3 costs 400 but has 2 stops; 0->1->3 = 700 with 1 stop
 *
 *  ---------------------------------------------------------------------------
 *  WHY PLAIN DIJKSTRA FAILS: it keeps only the cheapest way to reach each city.
 *  But a pricier route with FEWER stops may be the only one that can still
 *  continue within the limit. (City 2 is cheapest via 0->1->2, but that's already
 *  2 flights.)
 *
 *  BELLMAN-FORD WITH A ROUND LIMIT
 *    After round i, cost[v] = cheapest way to reach v using at most i flights.
 *    Run exactly k+1 rounds. The catch: within one round, relax from the PREVIOUS
 *    round's values (a copy), otherwise an edge relaxed earlier in the same round
 *    could be chained -> two flights counted as one round.
 *        for round in 1..k+1:
 *           next = copy of cost
 *           for each flight (u, v, p): if cost[u] finite: next[v] = min(next[v], cost[u] + p)
 *           cost = next
 *
 *  DRY RUN k=1 (2 rounds), src 0
 *    start  cost [0, inf, inf, inf]
 *    round 1 (from start): 1 = 100                    -> [0,100,inf,inf]
 *    round 2 (from round 1): 2 = 200, 3 = 700, 0 stays -> [0,100,200,700]
 *    answer 700 (the 400 route would need round 3)
 *
 *  ALTERNATIVE: BFS/Dijkstra over states (city, flights used) also works.
 *  COMPLEXITY   Time O(k * E)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M09_CheapestFlightsWithinKStops {

    static int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        int INF = Integer.MAX_VALUE;
        int[] cost = new int[n];
        Arrays.fill(cost, INF);
        cost[src] = 0;
        for (int round = 0; round <= k; round++) {             // k stops = k + 1 flights
            int[] next = cost.clone();                         // relax from the PREVIOUS round only
            for (int[] f : flights) {
                if (cost[f[0]] != INF && cost[f[0]] + f[2] < next[f[1]]) next[f[1]] = cost[f[0]] + f[2];
            }
            cost = next;
        }
        return cost[dst] == INF ? -1 : cost[dst];
    }

    // brute force: DFS over all routes with at most k+1 flights
    static int brute(int n, int[][] flights, int src, int dst, int k) {
        int[] best = {Integer.MAX_VALUE};
        dfs(flights, src, dst, k + 1, 0, best);
        return best[0] == Integer.MAX_VALUE ? -1 : best[0];
    }

    static void dfs(int[][] flights, int u, int dst, int flightsLeft, int spent, int[] best) {
        if (u == dst) best[0] = Math.min(best[0], spent);
        if (flightsLeft == 0) return;
        for (int[] f : flights) if (f[0] == u) dfs(flights, f[1], dst, flightsLeft - 1, spent + f[2], best);
    }

    public static void main(String[] args) {
        int[][] f = {{0, 1, 100}, {1, 2, 100}, {2, 0, 100}, {1, 3, 600}, {2, 3, 200}};
        check("k=1", 700, findCheapestPrice(4, f, 0, 3, 1));
        check("k=2 (the 400 route is allowed)", 400, findCheapestPrice(4, f, 0, 3, 2));
        int[][] g = {{0, 1, 100}, {1, 2, 100}, {0, 2, 500}};
        check("k=1", 200, findCheapestPrice(3, g, 0, 2, 1));
        check("k=0 (direct only)", 500, findCheapestPrice(3, g, 0, 2, 0));
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 2 + rnd.nextInt(5);
            int[][] fl = new int[rnd.nextInt(10)][];
            for (int i = 0; i < fl.length; i++) fl[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(20)};
            int s = rnd.nextInt(n), d = rnd.nextInt(n), k = rnd.nextInt(4);
            ok &= findCheapestPrice(n, fl, s, d, k) == brute(n, fl, s, d, k);
        }
        check("1,000 random cases vs all routes", true, ok);
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
