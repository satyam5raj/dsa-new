/*
 * ============================================================================
 *  MEDIUM 12 : MIN COST TO CONNECT ALL POINTS   LeetCode #1584
 *  https://leetcode.com/problems/min-cost-to-connect-all-points/
 *  Pattern: MINIMUM SPANNING TREE on a COMPLETE graph -> O(n^2) Prim (no heap)
 * ============================================================================
 *
 *  PROBLEM
 *    Points on a plane; connecting two costs their MANHATTAN distance
 *    |x1 - x2| + |y1 - y2|. Minimum total cost so every pair is connected (directly
 *    or through others).
 *      [[0,0],[2,2],[3,10],[5,2],[7,0]] -> 20
 *
 *  ---------------------------------------------------------------------------
 *  "Connect everything as cheaply as possible, no need for cycles" = MST
 *  (01-Basics/MinimumSpanningTree).
 *
 *  WHICH MST ALGORITHM? Every pair of points can be connected: E = n(n-1)/2.
 *    Kruskal: build and sort ~n^2/2 edges -> O(n^2 log n) time, O(n^2) memory
 *    Prim with a heap: O(n^2 log n)
 *    Prim with an ARRAY: O(n^2), O(n) memory  <- best for dense graphs
 *       minDist[v] = cheapest edge from the current tree to v
 *       repeat n times: pick the unvisited v with the smallest minDist (O(n) scan),
 *       add it, then update minDist of all others through v (O(n))
 *
 *  DRY RUN (first steps): start at (0,0), minDist = [0, 4, 13, 7, 7]
 *    add (2,2) cost 4 -> update: (5,2) 3, (3,10) 9 ...
 *    total 4 + 3 + 4 + 9 = 20
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M12_MinCostToConnectAllPoints {

    static int minCostConnectPoints(int[][] points) {
        int n = points.length;
        int[] minDist = new int[n];
        Arrays.fill(minDist, Integer.MAX_VALUE);
        minDist[0] = 0;
        boolean[] inTree = new boolean[n];
        int total = 0;
        for (int step = 0; step < n; step++) {
            int u = -1;
            for (int v = 0; v < n; v++) if (!inTree[v] && (u == -1 || minDist[v] < minDist[u])) u = v;
            inTree[u] = true;
            total += minDist[u];
            for (int v = 0; v < n; v++) {
                if (inTree[v]) continue;
                int d = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
                if (d < minDist[v]) minDist[v] = d;                // a cheaper way into the tree, via u
            }
        }
        return total;
    }

    // Kruskal with union-find as a referee
    static int kruskal(int[][] p) {
        int n = p.length;
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
            edges.add(new int[]{Math.abs(p[i][0] - p[j][0]) + Math.abs(p[i][1] - p[j][1]), i, j});
        edges.sort(Comparator.comparingInt(e -> e[0]));
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int total = 0;
        for (int[] e : edges) {
            int a = find(parent, e[1]), b = find(parent, e[2]);
            if (a != b) { parent[a] = b; total += e[0]; }
        }
        return total;
    }

    static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }

    public static void main(String[] args) {
        check("example", 20, minCostConnectPoints(new int[][]{{0, 0}, {2, 2}, {3, 10}, {5, 2}, {7, 0}}));
        check("[[3,12],[-2,5],[-4,1]]", 18, minCostConnectPoints(new int[][]{{3, 12}, {-2, 5}, {-4, 1}}));
        check("single point", 0, minCostConnectPoints(new int[][]{{0, 0}}));
        Random rnd = new Random(12);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[][] p = new int[1 + rnd.nextInt(30)][];
            for (int i = 0; i < p.length; i++) p[i] = new int[]{rnd.nextInt(41) - 20, rnd.nextInt(41) - 20};
            ok &= minCostConnectPoints(p) == kruskal(p);
        }
        check("300 random point sets: Prim == Kruskal", true, ok);
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
