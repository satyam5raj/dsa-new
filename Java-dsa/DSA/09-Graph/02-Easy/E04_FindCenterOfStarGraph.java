/*
 * ============================================================================
 *  EASY 04 : FIND CENTER OF STAR GRAPH  ⭐ bonus   LeetCode #1791
 *  https://leetcode.com/problems/find-center-of-star-graph/
 *  Pattern: use the GRAPH'S STRUCTURE - an O(1) observation beats a traversal
 * ============================================================================
 *
 *  PROBLEM
 *    A star graph: one center connected to every other vertex, and nothing else.
 *    Given its edges, return the center.
 *      [[1,2],[2,3],[4,2]] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  GENERAL APPROACH: count degrees; the center has degree n - 1.  O(n)
 *
 *  OBSERVATION: the center is in EVERY edge. So it's the vertex the first two
 *  edges have in common. O(1)!
 *     edges[0] = [a, b], edges[1] = [c, d]  ->  a if a == c or a == d, else b
 *
 *  The lesson: before writing BFS, ask what the problem's structure already
 *  guarantees. (Same spirit: a tree has exactly V - 1 edges - Graph Valid Tree.)
 *
 *  COMPLEXITY   Time O(1)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E04_FindCenterOfStarGraph {

    static int findCenter(int[][] edges) {
        int a = edges[0][0], b = edges[0][1];
        return (a == edges[1][0] || a == edges[1][1]) ? a : b;
    }

    static int byDegree(int[][] edges) {
        int n = edges.length + 1;
        int[] deg = new int[n + 1];
        for (int[] e : edges) { deg[e[0]]++; deg[e[1]]++; }
        for (int v = 1; v <= n; v++) if (deg[v] == n - 1) return v;
        return -1;
    }

    public static void main(String[] args) {
        check("[[1,2],[2,3],[4,2]]", 2, findCenter(new int[][]{{1, 2}, {2, 3}, {4, 2}}));
        check("[[1,2],[5,1],[1,3],[1,4]]", 1, findCenter(new int[][]{{1, 2}, {5, 1}, {1, 3}, {1, 4}}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 3 + rnd.nextInt(10), center = 1 + rnd.nextInt(n);
            List<int[]> es = new ArrayList<>();
            for (int v = 1; v <= n; v++) if (v != center) es.add(rnd.nextBoolean() ? new int[]{v, center} : new int[]{center, v});
            Collections.shuffle(es, rnd);
            int[][] e = es.toArray(new int[0][]);
            ok &= findCenter(e) == center && byDegree(e) == center;
        }
        check("500 random stars", true, ok);
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
