/*
 * ============================================================================
 *  HARD 06 : SWIM IN RISING WATER            LeetCode #778
 *  https://leetcode.com/problems/swim-in-rising-water/
 *  Pattern: MINIMAX PATH - Dijkstra on max(), like Path With Minimum Effort (M14)
 * ============================================================================
 *
 *  PROBLEM
 *    n x n grid of distinct elevations 0..n^2-1. At time t the water level is t; you
 *    can swim between adjacent cells if BOTH elevations are <= t (instantly).
 *    Least time to get from (0,0) to (n-1,n-1)?
 *      [[0,2],[1,3]] -> 3       (you must stand on 3 at the end)
 *
 *  ---------------------------------------------------------------------------
 *  REFRAME: a path is usable at time t iff EVERY cell on it is <= t. So the time a
 *  path needs = the MAX elevation along it, and we want the path minimizing that
 *  max. A "minimax path" - same as M14, but the weights sit on CELLS, not edges.
 *
 *  DIJKSTRA with cost(path) = max over cells: always expand the cell with the
 *  lowest "max so far"; the first time we pop the target, that's the answer.
 *
 *  OTHER VIEWS (all O(n^2 log n))
 *    - binary search on t + BFS "can I reach the end using cells <= t?"
 *    - Union-Find: add cells in increasing elevation, union with already-added
 *      neighbours; stop when (0,0) and the end are connected -> that elevation.
 *  (The first is the referee below.)
 *
 *  DRY RUN [[0,2],[1,3]]: start 0 -> neighbours 2, 1 -> pop 1 (max 1) -> 3 (max 3)
 *    -> pop 2 (max 2) -> 3 still max 3 -> pop end with 3
 *
 *  COMPLEXITY   Time O(n^2 log n)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class H06_SwimInRisingWater {

    static int swimInWater(int[][] grid) {
        int n = grid.length;
        boolean[][] seen = new boolean[n][n];
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));   // {time needed, r, c}
        heap.add(new int[]{grid[0][0], 0, 0});
        seen[0][0] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (true) {
            int[] top = heap.poll();
            if (top[1] == n - 1 && top[2] == n - 1) return top[0];
            for (int[] d : dirs) {
                int r = top[1] + d[0], c = top[2] + d[1];
                if (r >= 0 && r < n && c >= 0 && c < n && !seen[r][c]) {
                    seen[r][c] = true;                     // node weights: the first push is already the best
                    heap.add(new int[]{Math.max(top[0], grid[r][c]), r, c});
                }
            }
        }
    }

    static int binarySearch(int[][] g) {
        int n = g.length, lo = Math.max(g[0][0], g[n - 1][n - 1]), hi = n * n - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (reachable(g, mid)) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    static boolean reachable(int[][] g, int t) {
        int n = g.length;
        boolean[][] seen = new boolean[n][n];
        Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{0, 0}));
        seen[0][0] = true;
        while (!st.isEmpty()) {
            int[] p = st.pop();
            if (p[0] == n - 1 && p[1] == n - 1) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r >= 0 && r < n && c >= 0 && c < n && !seen[r][c] && g[r][c] <= t) { seen[r][c] = true; st.push(new int[]{r, c}); }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        check("[[0,2],[1,3]]", 3, swimInWater(new int[][]{{0, 2}, {1, 3}}));
        int[][] g = {{0, 1, 2, 3, 4}, {24, 23, 22, 21, 5}, {12, 13, 14, 15, 16}, {11, 17, 18, 19, 20}, {10, 9, 8, 7, 6}};
        check("spiral example", 16, swimInWater(g));
        check("[[0]]", 0, swimInWater(new int[][]{{0}}));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(7);
            List<Integer> vals = new ArrayList<>();
            for (int i = 0; i < n * n; i++) vals.add(i);
            Collections.shuffle(vals, rnd);
            int[][] a = new int[n][n];
            for (int i = 0; i < n * n; i++) a[i / n][i % n] = vals.get(i);
            ok &= swimInWater(a) == binarySearch(a);
        }
        check("300 random grids: Dijkstra == binary search", true, ok);
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
