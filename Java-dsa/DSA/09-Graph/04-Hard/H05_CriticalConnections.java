/*
 * ============================================================================
 *  HARD 05 : CRITICAL CONNECTIONS IN A NETWORK   LeetCode #1192
 *  https://leetcode.com/problems/critical-connections-in-a-network/
 *  Pattern: BRIDGES with TARJAN'S low-link values (one DFS)
 * ============================================================================
 *
 *  PROBLEM
 *    n servers, undirected connections. A connection is CRITICAL if removing it
 *    disconnects some servers. Return all critical connections.
 *      n=4, [[0,1],[1,2],[2,0],[1,3]] -> [[1,3]]
 *
 *  ---------------------------------------------------------------------------
 *  Critical connection = BRIDGE (01-Basics/BridgesAndEulerPaths has the theory).
 *
 *  BRUTE FORCE: remove each edge, test connectivity: O(E * (V + E)) - with
 *  10^5 edges, 10^10. Too slow.
 *
 *  TARJAN (one DFS, O(V + E))
 *    disc[u]: when u was first visited
 *    low[u] : the smallest disc reachable from u's DFS subtree using at most one
 *             BACK edge (an edge to an already-visited vertex, not the parent edge)
 *    After exploring child v of u:
 *       low[u] = min(low[u], low[v])
 *       if low[v] > disc[u] -> v's subtree has NO way back to u or above
 *                           -> u-v is the only link -> a BRIDGE
 *  Intuition: an edge is a bridge exactly when it's NOT on any cycle; a back edge
 *  from below creates a cycle that "protects" every tree edge it spans.
 *
 *  DRY RUN: DFS 0 -> 1 -> 2 (2 has a back edge to 0: low[2] = 0) ; 1 -> 3
 *    low[3] = disc[3] = 3 > disc[1] = 1 -> [1,3] is a bridge
 *    low[2] = 0, not > disc[1] -> 1-2 is protected by the cycle
 *
 *  Recursion depth can reach n (10^5) -> on the JVM, run with a bigger stack or
 *  convert to an iterative DFS for production code.
 *  COMPLEXITY   Time O(V + E)   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class H05_CriticalConnections {

    static int timer;

    static List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (List<Integer> c : connections) { adj.get(c.get(0)).add(c.get(1)); adj.get(c.get(1)).add(c.get(0)); }
        int[] disc = new int[n], low = new int[n];
        Arrays.fill(disc, -1);
        timer = 0;
        List<List<Integer>> bridges = new ArrayList<>();
        for (int u = 0; u < n; u++) if (disc[u] == -1) dfs(u, -1, adj, disc, low, bridges);
        return bridges;
    }

    static void dfs(int u, int parent, List<List<Integer>> adj, int[] disc, int[] low, List<List<Integer>> bridges) {
        disc[u] = low[u] = timer++;
        for (int v : adj.get(u)) {
            if (v == parent) continue;
            if (disc[v] == -1) {
                dfs(v, u, adj, disc, low, bridges);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u]) bridges.add(List.of(u, v));      // v's subtree can't climb past u
            } else {
                low[u] = Math.min(low[u], disc[v]);                    // back edge
            }
        }
    }

    // brute force: remove each connection and count components
    static List<List<Integer>> brute(int n, List<List<Integer>> cs) {
        List<List<Integer>> out = new ArrayList<>();
        int base = comps(n, cs, -1);
        for (int i = 0; i < cs.size(); i++) if (comps(n, cs, i) > base) out.add(cs.get(i));
        return out;
    }

    static int comps(int n, List<List<Integer>> cs, int skip) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        int c = n;
        for (int i = 0; i < cs.size(); i++) {
            if (i == skip) continue;
            int a = root(p, cs.get(i).get(0)), b = root(p, cs.get(i).get(1));
            if (a != b) { p[a] = b; c--; }
        }
        return c;
    }

    static int root(int[] p, int x) { while (p[x] != x) x = p[x]; return x; }

    static Set<String> norm(List<List<Integer>> es) {
        Set<String> s = new TreeSet<>();
        for (List<Integer> e : es) s.add(Math.min(e.get(0), e.get(1)) + "-" + Math.max(e.get(0), e.get(1)));
        return s;
    }

    public static void main(String[] args) {
        check("example", "[1-3]", norm(criticalConnections(4, List.of(List.of(0, 1), List.of(1, 2), List.of(2, 0), List.of(1, 3)))));
        check("[[0,1]]", "[0-1]", norm(criticalConnections(2, List.of(List.of(0, 1)))));
        check("a cycle has no bridges", "[]", norm(criticalConnections(4, List.of(List.of(0, 1), List.of(1, 2), List.of(2, 3), List.of(3, 0)))));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 2 + rnd.nextInt(9);
            Set<String> seen = new HashSet<>();
            List<List<Integer>> cs = new ArrayList<>();
            for (int i = 1; i < n; i++) { int p = rnd.nextInt(i); cs.add(List.of(p, i)); seen.add(p + "," + i); }   // connected
            for (int i = 0; i < rnd.nextInt(n); i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b && seen.add(Math.min(a, b) + "," + Math.max(a, b)) && seen.add(Math.max(a, b) + "," + Math.min(a, b))) cs.add(List.of(a, b));
            }
            ok &= norm(criticalConnections(n, cs)).equals(norm(brute(n, cs)));
        }
        check("500 random networks vs remove-each-edge", true, ok);
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
