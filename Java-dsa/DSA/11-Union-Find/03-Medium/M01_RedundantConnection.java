/*
 * ============================================================================
 *  MEDIUM 01 : REDUNDANT CONNECTION          LeetCode #684
 *  https://leetcode.com/problems/redundant-connection/
 *  Pattern: add edges one by one - the first edge whose ends are ALREADY connected closes a cycle
 * ============================================================================
 *
 *  PROBLEM
 *    A tree of n nodes (1..n) plus ONE extra edge. Return an edge whose removal
 *    leaves a tree; if several qualify, the one appearing LAST in the input.
 *      [[1,2],[1,3],[2,3]]            -> [2,3]
 *      [[1,2],[2,3],[3,4],[1,4],[1,5]] -> [1,4]
 *
 *  ---------------------------------------------------------------------------
 *  The extra edge creates exactly one cycle; any edge ON that cycle can be removed.
 *  Which one appears last? Process edges in input order with Union-Find:
 *     union(a, b) returns false  <=>  a and b were already connected
 *                                <=>  this edge closes the cycle
 *  Every earlier cycle edge was needed to make a and b connected, so the edge
 *  that closes the cycle IS the last cycle edge in the input. Return it.
 *
 *  BRUTE FORCE (referee): try removing edges from last to first; the first removal
 *  that leaves a connected, acyclic graph (a tree) is the answer.  O(E * (V + E))
 *
 *  DRY RUN [[1,2],[2,3],[3,4],[1,4],[1,5]]
 *    1-2 ok, 2-3 ok, 3-4 ok, 1-4: find(1) == find(4) -> [1,4]
 *
 *  Directed version (a node can get two parents) -> Redundant Connection II (H02).
 *  COMPLEXITY   Time O(n * alpha(n))   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M01_RedundantConnection {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static int[] findRedundantConnection(int[][] edges) {
        parent = new int[edges.length + 1];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (int[] e : edges) {
            int a = find(e[0]), b = find(e[1]);
            if (a == b) return e;                      // already connected: this edge closes the cycle
            parent[a] = b;
        }
        return new int[0];
    }

    static int[] brute(int[][] edges) {
        int n = edges.length;
        for (int skip = n - 1; skip >= 0; skip--) {
            int[] p = new int[n + 1];
            for (int i = 0; i <= n; i++) p[i] = i;
            int merges = 0;
            boolean cycle = false;
            for (int i = 0; i < n; i++) {
                if (i == skip) continue;
                int a = root(p, edges[i][0]), b = root(p, edges[i][1]);
                if (a == b) { cycle = true; break; }
                p[a] = b;
                merges++;
            }
            if (!cycle && merges == n - 1) return edges[skip];      // n nodes, n-1 edges, no cycle = a tree
        }
        return new int[0];
    }

    static int root(int[] p, int x) { while (p[x] != x) x = p[x]; return x; }

    public static void main(String[] args) {
        check("[[1,2],[1,3],[2,3]]", "[2, 3]", findRedundantConnection(new int[][]{{1, 2}, {1, 3}, {2, 3}}));
        check("[[1,2],[2,3],[3,4],[1,4],[1,5]]", "[1, 4]", findRedundantConnection(new int[][]{{1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}}));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 3 + rnd.nextInt(8);
            List<int[]> es = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int i = 2; i <= n; i++) { int p = 1 + rnd.nextInt(i - 1); es.add(new int[]{p, i}); seen.add(p + "," + i); }   // a random tree
            int a, b;
            do { a = 1 + rnd.nextInt(n); b = 1 + rnd.nextInt(n); } while (a >= b || seen.contains(a + "," + b));
            es.add(new int[]{a, b});                                                                      // the extra edge
            Collections.shuffle(es, rnd);
            int[][] arr = es.toArray(new int[0][]);
            ok &= Arrays.equals(findRedundantConnection(arr), brute(arr));
        }
        check("1,000 random trees + 1 edge vs brute force", true, ok);
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
