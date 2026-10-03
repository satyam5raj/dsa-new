/*
 * ============================================================================
 *  MEDIUM 16 : MINIMUM PATH SUM              LeetCode #64
 *  https://leetcode.com/problems/minimum-path-sum/
 *  Pattern: grid DP (optimization) + reconstructing the path
 * ============================================================================
 *
 *  PROBLEM
 *    Non-negative grid; move right or down from top-left to bottom-right minimising
 *    the sum of visited cells.
 *      [[1,3,1],[1,5,1],[4,2,1]] -> 7   (1 -> 3 -> 1 -> 1 -> 1)
 *      [[1,2,3],[4,5,6]]         -> 12
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY FAILS: always stepping to the cheaper neighbour can walk into an expensive
 *  region. DP considers both arrivals for every cell.
 *      STATE       dp[r][c] = min sum of a path from (0,0) to (r,c), including both
 *      TRANSITION  dp[r][c] = grid[r][c] + min(dp[r-1][c], dp[r][c-1])
 *      BASE        dp[0][0] = grid[0][0]; first row/column have one predecessor
 *  Rolling one row: dp[c] = grid[r][c] + min(dp[c] (above), dp[c-1] (left)).
 *
 *  RECONSTRUCTION: keep the full table, start at the end and step to whichever
 *  predecessor holds dp[cur] - grid[cur].
 *
 *  DRY RUN [[1,3,1],[1,5,1],[4,2,1]]
 *    row0: 1 4 5     row1: 2 7 6     row2: 6 8 7   -> 7
 *
 *  COMPLEXITY   Time O(m*n)   Space O(n)  (O(m*n) if you need the path)
 * ============================================================================
 */

import java.util.*;

public class M16_MinimumPathSum {

    static int minPathSum(int[][] grid) {
        int n = grid[0].length;
        int[] dp = new int[n];
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < n; c++) {
                if (r == 0 && c == 0) dp[c] = grid[0][0];
                else if (r == 0) dp[c] = dp[c - 1] + grid[r][c];             // only from the left
                else if (c == 0) dp[c] = dp[c] + grid[r][c];                 // only from above
                else dp[c] = Math.min(dp[c], dp[c - 1]) + grid[r][c];
            }
        return dp[n - 1];
    }

    static List<String> path(int[][] g) {
        int m = g.length, n = g[0].length;
        int[][] dp = new int[m][n];
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) {
                int best = r == 0 && c == 0 ? 0 : Integer.MAX_VALUE;
                if (r > 0) best = Math.min(best, dp[r - 1][c]);
                if (c > 0) best = Math.min(best, dp[r][c - 1]);
                dp[r][c] = best + g[r][c];
            }
        LinkedList<String> out = new LinkedList<>();
        int r = m - 1, c = n - 1;
        while (true) {
            out.addFirst("(" + r + "," + c + ")");
            if (r == 0 && c == 0) break;
            int need = dp[r][c] - g[r][c];
            if (r > 0 && dp[r - 1][c] == need) r--; else c--;                   // the predecessor that produced it
        }
        return out;
    }

    static int brute(int[][] g, int r, int c) {
        if (r == g.length - 1 && c == g[0].length - 1) return g[r][c];
        int best = Integer.MAX_VALUE;
        if (r + 1 < g.length) best = Math.min(best, brute(g, r + 1, c));
        if (c + 1 < g[0].length) best = Math.min(best, brute(g, r, c + 1));
        return g[r][c] + best;
    }

    public static void main(String[] args) {
        int[][] g = {{1, 3, 1}, {1, 5, 1}, {4, 2, 1}};
        check("3x3 example", 7, minPathSum(g));
        check("the path", "[(0,0), (0,1), (0,2), (1,2), (2,2)]", path(g));
        check("[[1,2,3],[4,5,6]]", 12, minPathSum(new int[][]{{1, 2, 3}, {4, 5, 6}}));
        check("[[5]]", 5, minPathSum(new int[][]{{5}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[][] a = new int[1 + rnd.nextInt(7)][1 + rnd.nextInt(7)];
            for (int[] row : a) for (int c = 0; c < row.length; c++) row[c] = rnd.nextInt(10);
            ok &= minPathSum(a) == brute(a, 0, 0);
        }
        check("500 random grids vs recursion", true, ok);
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
