/*
 * ============================================================================
 *  HARD 07 : BUS ROUTES                      LeetCode #815
 *  https://leetcode.com/problems/bus-routes/
 *  Pattern: BFS where the "vertices" are ROUTES (buses), not stops
 * ============================================================================
 *
 *  PROBLEM
 *    routes[i] = the stops bus i loops through. Fewest BUSES to get from source
 *    stop to target stop? (-1 if impossible, 0 if source == target)
 *      routes [[1,2,7],[3,6,7]], 1 -> 6 = 2   (bus 0 to stop 7, change to bus 1)
 *
 *  ---------------------------------------------------------------------------
 *  WHAT DO WE COUNT? Buses, not stops. Riding bus 0 from stop 1 to stop 7 costs 1,
 *  however many stops it passes.
 *
 *  NAIVE GRAPH ON STOPS: connect every pair of stops on the same route -> a route
 *  with k stops creates k^2 edges. With 10^5 stops per route: way too many.
 *
 *  BFS OVER ROUTES:
 *    stopToRoutes: stop -> the routes passing through it (a HashMap)
 *    level 1 = all routes through the source stop
 *    for each route in the current level:
 *       if it contains the target -> answer = level
 *       for each stop on it: every unvisited route through that stop -> next level
 *    Mark ROUTES as visited (and stops too, so a busy stop isn't re-scanned).
 *  Each route and each stop is processed once -> O(total stops on all routes).
 *
 *  DRY RUN: routes through stop 1: {0} (level 1). Route 0 has 1,2,7 - no 6.
 *    Stop 7 -> route 1 (level 2). Route 1 contains 6 -> 2.
 *
 *  COMPLEXITY   Time O(sum of route lengths)   Space O(same)
 * ============================================================================
 */

import java.util.*;

public class H07_BusRoutes {

    static int numBusesToDestination(int[][] routes, int source, int target) {
        if (source == target) return 0;
        Map<Integer, List<Integer>> stopToRoutes = new HashMap<>();
        for (int r = 0; r < routes.length; r++) for (int s : routes[r]) stopToRoutes.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
        if (!stopToRoutes.containsKey(source) || !stopToRoutes.containsKey(target)) return -1;

        boolean[] routeSeen = new boolean[routes.length];
        Set<Integer> stopSeen = new HashSet<>(List.of(source));
        Queue<Integer> q = new ArrayDeque<>();
        for (int r : stopToRoutes.get(source)) { routeSeen[r] = true; q.add(r); }
        int buses = 1;
        while (!q.isEmpty()) {
            for (int i = q.size(); i > 0; i--) {
                int r = q.poll();
                for (int s : routes[r]) {
                    if (s == target) return buses;
                    if (!stopSeen.add(s)) continue;               // this stop's routes were already queued
                    for (int nr : stopToRoutes.get(s)) {
                        if (!routeSeen[nr]) { routeSeen[nr] = true; q.add(nr); }
                    }
                }
            }
            buses++;
        }
        return -1;
    }

    // referee: BFS on stops; from a stop, every stop sharing a route costs one bus
    static int stopBfs(int[][] routes, int source, int target) {
        if (source == target) return 0;
        Map<Integer, Integer> dist = new HashMap<>(Map.of(source, 0));
        Queue<Integer> q = new ArrayDeque<>(List.of(source));
        while (!q.isEmpty()) {
            int s = q.poll();
            for (int[] route : routes) {
                boolean has = false;
                for (int x : route) if (x == s) { has = true; break; }
                if (!has) continue;
                for (int x : route) if (!dist.containsKey(x)) { dist.put(x, dist.get(s) + 1); q.add(x); }
            }
        }
        return dist.getOrDefault(target, -1);
    }

    public static void main(String[] args) {
        check("example 1", 2, numBusesToDestination(new int[][]{{1, 2, 7}, {3, 6, 7}}, 1, 6));
        check("example 2 (impossible)", -1, numBusesToDestination(new int[][]{{7, 12}, {4, 5, 15}, {6}, {15, 19}, {9, 12, 13}}, 15, 12));
        check("source == target", 0, numBusesToDestination(new int[][]{{1, 2}}, 3, 3));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[][] routes = new int[1 + rnd.nextInt(5)][];
            for (int i = 0; i < routes.length; i++) routes[i] = rnd.ints(1 + rnd.nextInt(4), 0, 10).distinct().toArray();
            int s = rnd.nextInt(10), e = rnd.nextInt(10);
            ok &= numBusesToDestination(routes, s, e) == stopBfs(routes, s, e);
        }
        check("1,000 random networks vs stop-level BFS", true, ok);
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
