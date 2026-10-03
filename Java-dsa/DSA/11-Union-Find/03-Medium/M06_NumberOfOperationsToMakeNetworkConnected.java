/*
 * ============================================================================
 *  MEDIUM 06 : NUMBER OF OPERATIONS TO MAKE NETWORK CONNECTED   LeetCode #1319
 *  https://leetcode.com/problems/number-of-operations-to-make-network-connected/
 *  Pattern: count COMPONENTS and REDUNDANT cables with one Union-Find pass
 * ============================================================================
 *
 *  PROBLEM
 *    n computers, connections[i] = a cable. One operation = unplug any cable and
 *    plug it between two other computers. Minimum operations to connect everything,
 *    or -1 if impossible.
 *      n=4, [[0,1],[0,2],[1,2]]               -> 1  (move 1-2 to connect 3)
 *      n=6, [[0,1],[0,2],[0,3],[1,2]]         -> -1 (only 4 cables, need 5)
 *
 *  ---------------------------------------------------------------------------
 *  TWO FACTS
 *    1. Connecting n computers needs at least n - 1 cables. Fewer -> -1 right away.
 *    2. With enough cables, joining c components takes exactly c - 1 moves, and
 *       there are always enough spare cables: every cable that closes a cycle
 *       (union returns false) is redundant and can be moved.
 *  So: answer = components - 1  (after the n - 1 check).
 *
 *  Union-Find: start with n components, decrement on every successful union.
 *
 *  DRY RUN n=4: 0-1 (3 comps), 0-2 (2 comps), 1-2 redundant -> comps 2 -> 1 move
 *
 *  COMPLEXITY   Time O(E * alpha)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M06_NumberOfOperationsToMakeNetworkConnected {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static int makeConnected(int n, int[][] connections) {
        if (connections.length < n - 1) return -1;           // not enough cables in total
        parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int components = n;
        for (int[] c : connections) {
            int a = find(c[0]), b = find(c[1]);
            if (a != b) { parent[a] = b; components--; }       // else: a spare cable
        }
        return components - 1;
    }

    // referee: count components with DFS, apply the same reasoning independently
    static int brute(int n, int[][] cs) {
        if (cs.length < n - 1) return -1;
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] c : cs) { adj.get(c[0]).add(c[1]); adj.get(c[1]).add(c[0]); }
        boolean[] seen = new boolean[n];
        int comps = 0;
        for (int s = 0; s < n; s++) {
            if (seen[s]) continue;
            comps++;
            Deque<Integer> st = new ArrayDeque<>(List.of(s));
            seen[s] = true;
            while (!st.isEmpty()) for (int v : adj.get(st.pop())) if (!seen[v]) { seen[v] = true; st.push(v); }
        }
        return comps - 1;
    }

    public static void main(String[] args) {
        check("n=4", 1, makeConnected(4, new int[][]{{0, 1}, {0, 2}, {1, 2}}));
        check("n=6, two moves", 2, makeConnected(6, new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}}));
        check("n=6, not enough cables", -1, makeConnected(6, new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 2}}));
        check("already connected", 0, makeConnected(3, new int[][]{{0, 1}, {1, 2}}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(10);
            Set<String> seen = new HashSet<>();
            List<int[]> cs = new ArrayList<>();
            for (int i = 0; i < rnd.nextInt(2 * n + 1); i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b && seen.add(Math.min(a, b) + "," + Math.max(a, b))) cs.add(new int[]{a, b});
            }
            int[][] arr = cs.toArray(new int[0][]);
            ok &= makeConnected(n, arr) == brute(n, arr);
        }
        check("1,000 random networks vs DFS components", true, ok);
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
