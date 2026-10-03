/*
 * ============================================================================
 *  HARD 04 : RECONSTRUCT ITINERARY           LeetCode #332
 *  https://leetcode.com/problems/reconstruct-itinerary/
 *  Pattern: EULER PATH (use every ticket = every edge once) via HIERHOLZER
 * ============================================================================
 *
 *  PROBLEM
 *    tickets [from, to]. Use ALL tickets exactly once, starting at "JFK". If several
 *    itineraries work, return the lexicographically SMALLEST.
 *      [["JFK","SFO"],["JFK","ATL"],["SFO","ATL"],["ATL","JFK"],["ATL","SFO"]]
 *      -> [JFK, ATL, JFK, SFO, ATL, SFO]
 *
 *  ---------------------------------------------------------------------------
 *  "Use every EDGE exactly once" = an EULER PATH (01-Basics/BridgesAndEulerPaths).
 *
 *  NAIVE GREEDY FAILS: always flying to the smallest airport can lead into a dead
 *  end before all tickets are used:  JFK->KUL (dead end), JFK->NRT->JFK.
 *  Greedy takes KUL first and gets stuck. Backtracking fixes it but can be exponential.
 *
 *  HIERHOLZER (O(E log E)):
 *    keep each airport's destinations in a MIN-HEAP (smallest first)
 *    dfs(a): while a has unused tickets: dfs(poll the smallest destination)
 *            then PREPEND a to the route  (postorder)
 *  A dead end finishes FIRST, so it lands at the END of the route; the remaining
 *  loop gets spliced in before it. Greedy-smallest + postorder = the smallest valid
 *  itinerary.
 *
 *  DRY RUN JFK->KUL, JFK->NRT, NRT->JFK
 *    dfs(JFK): take KUL -> dfs(KUL): no tickets -> route [KUL]
 *              take NRT -> dfs(NRT): take JFK -> dfs(JFK): none left -> [JFK, KUL]
 *                          NRT done -> [NRT, JFK, KUL]
 *              JFK done -> [JFK, NRT, JFK, KUL]
 *
 *  COMPLEXITY   Time O(E log E)   Space O(E)
 * ============================================================================
 */

import java.util.*;

public class H04_ReconstructItinerary {

    static List<String> findItinerary(List<List<String>> tickets) {
        Map<String, PriorityQueue<String>> flights = new HashMap<>();
        for (List<String> t : tickets) flights.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).add(t.get(1));
        LinkedList<String> route = new LinkedList<>();
        visit("JFK", flights, route);
        return route;
    }

    static void visit(String airport, Map<String, PriorityQueue<String>> flights, LinkedList<String> route) {
        PriorityQueue<String> dests = flights.get(airport);
        while (dests != null && !dests.isEmpty()) visit(dests.poll(), flights, route);   // use each ticket once
        route.addFirst(airport);                                                         // postorder, prepended
    }

    // referee: backtracking, trying destinations in sorted order (the first full route is the smallest)
    static List<String> backtracking(List<List<String>> tickets) {
        List<List<String>> sorted = new ArrayList<>(tickets);
        sorted.sort(Comparator.comparing((List<String> t) -> t.get(1)));
        List<String> route = new ArrayList<>(List.of("JFK"));
        return bt(sorted, new boolean[sorted.size()], route) ? route : null;
    }

    static boolean bt(List<List<String>> t, boolean[] used, List<String> route) {
        if (route.size() == t.size() + 1) return true;
        String at = route.get(route.size() - 1);
        for (int i = 0; i < t.size(); i++) {
            if (used[i] || !t.get(i).get(0).equals(at)) continue;
            if (i > 0 && !used[i - 1] && t.get(i).equals(t.get(i - 1))) continue;   // skip duplicate tickets
            used[i] = true;
            route.add(t.get(i).get(1));
            if (bt(t, used, route)) return true;
            route.remove(route.size() - 1);
            used[i] = false;
        }
        return false;
    }

    static List<List<String>> tickets(String... s) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < s.length; i += 2) out.add(List.of(s[i], s[i + 1]));
        return out;
    }

    public static void main(String[] args) {
        check("example 1", "[JFK, MUC, LHR, SFO, SJC]", findItinerary(tickets("MUC", "LHR", "JFK", "MUC", "SFO", "SJC", "LHR", "SFO")));
        check("example 2", "[JFK, ATL, JFK, SFO, ATL, SFO]", findItinerary(tickets("JFK", "SFO", "JFK", "ATL", "SFO", "ATL", "ATL", "JFK", "ATL", "SFO")));
        check("greedy dead-end trap", "[JFK, NRT, JFK, KUL]", findItinerary(tickets("JFK", "KUL", "JFK", "NRT", "NRT", "JFK")));
        String[] airports = {"JFK", "AAA", "BBB", "CCC"};
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            List<List<String>> ts = new ArrayList<>();
            String at = "JFK";
            for (int i = 0; i < 1 + rnd.nextInt(7); i++) {                 // a random walk = a valid itinerary exists
                String to = airports[rnd.nextInt(airports.length)];
                ts.add(List.of(at, to));
                at = to;
            }
            Collections.shuffle(ts, rnd);
            ok &= findItinerary(ts).equals(backtracking(ts));
        }
        check("500 random ticket sets vs backtracking", true, ok);
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
