/*
 * ============================================================================
 *  HARD 04 : REMOVE MAX NUMBER OF EDGES TO KEEP GRAPH FULLY TRAVERSABLE  ⭐ bonus
 *  LeetCode #1579
 *  https://leetcode.com/problems/remove-max-number-of-edges-to-keep-graph-fully-traversable/
 *  Pattern: TWO Union-Finds (Alice's and Bob's view) + greedy: shared edges FIRST
 * ============================================================================
 *
 *  PROBLEM
 *    Edge types: 1 = only Alice can use it, 2 = only Bob, 3 = both. Remove as many
 *    edges as possible so that BOTH Alice and Bob can still reach every node.
 *    Return the number removed, or -1 if one of them can't traverse everything
 *    even with all edges.
 *      n=4, [[3,1,2],[3,2,3],[1,1,3],[1,2,4],[1,1,2],[2,3,4]] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  EACH PERSON needs a spanning tree of their usable edges (n - 1 edges). We want to
 *  KEEP as few edges as possible in total.
 *
 *  GREEDY: a type-3 edge serves BOTH people at once, so it's worth twice as much.
 *  Take type-3 edges FIRST:
 *    alice, bob = two separate DSUs
 *    for type-3 edges: if it joins two components (in either DSU, which are the same
 *                      at this point), union in BOTH and keep it; otherwise it's removable
 *    then type-1 edges into alice's DSU only, type-2 into bob's DSU only:
 *                      keep only edges that join two components
 *    if either DSU still has more than 1 component -> -1
 *    answer = total edges - kept edges
 *  Why shared-first is optimal: replacing a kept type-3 edge would need one type-1
 *  AND one type-2 edge - never cheaper.
 *
 *  This is Kruskal's MST idea with all weights equal and a priority by type.
 *  COMPLEXITY   Time O(E * alpha)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H04_RemoveMaxEdgesKeepGraphTraversable {

    static class DSU {
        final int[] p;
        int components;
        DSU(int n) { p = new int[n + 1]; for (int i = 0; i <= n; i++) p[i] = i; components = n; }
        int find(int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            p[ra] = rb;
            components--;
            return true;
        }
    }

    static int maxNumEdgesToRemove(int n, int[][] edges) {
        DSU alice = new DSU(n), bob = new DSU(n);
        int kept = 0;
        for (int[] e : edges) {                                         // type 3 first: they help both
            if (e[0] == 3 && alice.union(e[1], e[2])) {
                bob.union(e[1], e[2]);                                  // identical so far: also joins in bob's view
                kept++;
            }
        }
        for (int[] e : edges) {
            if (e[0] == 1 && alice.union(e[1], e[2])) kept++;
            if (e[0] == 2 && bob.union(e[1], e[2])) kept++;
        }
        if (alice.components > 1 || bob.components > 1) return -1;
        return edges.length - kept;
    }

    // brute force: try every subset of edges to keep; smallest keep-set that works
    static int brute(int n, int[][] edges) {
        int m = edges.length, bestKeep = Integer.MAX_VALUE;
        for (int mask = 0; mask < (1 << m); mask++) {
            int keep = Integer.bitCount(mask);
            if (keep >= bestKeep) continue;
            DSU a = new DSU(n), b = new DSU(n);
            for (int i = 0; i < m; i++) {
                if ((mask >> i & 1) == 0) continue;
                if (edges[i][0] != 2) a.union(edges[i][1], edges[i][2]);
                if (edges[i][0] != 1) b.union(edges[i][1], edges[i][2]);
            }
            if (a.components == 1 && b.components == 1) bestKeep = keep;
        }
        return bestKeep == Integer.MAX_VALUE ? -1 : m - bestKeep;
    }

    public static void main(String[] args) {
        check("example 1", 2, maxNumEdgesToRemove(4, new int[][]{{3, 1, 2}, {3, 2, 3}, {1, 1, 3}, {1, 2, 4}, {1, 1, 2}, {2, 3, 4}}));
        check("example 2", 0, maxNumEdgesToRemove(4, new int[][]{{3, 1, 2}, {3, 2, 3}, {1, 1, 4}, {2, 1, 4}}));
        check("example 3 (impossible)", -1, maxNumEdgesToRemove(4, new int[][]{{3, 2, 3}, {1, 1, 2}, {2, 3, 4}}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(5);
            int[][] es = new int[rnd.nextInt(11)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{1 + rnd.nextInt(3), 1 + rnd.nextInt(n), 1 + rnd.nextInt(n)};
            ok &= maxNumEdgesToRemove(n, es) == brute(n, es);
        }
        check("500 random graphs vs all subsets", true, ok);
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
