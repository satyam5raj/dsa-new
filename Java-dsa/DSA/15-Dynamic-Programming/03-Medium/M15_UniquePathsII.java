/*
 * ============================================================================
 *  MEDIUM 15 : UNIQUE PATHS II               LeetCode #63
 *  https://leetcode.com/problems/unique-paths-ii/
 *  Pattern: grid DP with OBSTACLES (blocked cell = 0 ways)
 * ============================================================================
 *
 *  PROBLEM
 *    Same as Unique Paths, but grid[r][c] == 1 is an obstacle.
 *      [[0,0,0],[0,1,0],[0,0,0]] -> 2        [[0,1],[0,0]] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  Same recurrence, one extra rule: an obstacle cell has 0 paths.
 *      dp[r][c] = grid[r][c] == 1 ? 0 : dp[r-1][c] + dp[r][c-1]
 *  WHY THE MATH SHORTCUT DIES: obstacles break the "any order of moves" symmetry.
 *  COMMON BUG: initialising the first row/column to all 1s. An obstacle in row 0
 *  blocks EVERYTHING to its right. Using a single dp[] with dp[0] = 1 (start) and the
 *  general rule everywhere avoids special-casing.
 *
 *  DRY RUN 3x3 with the centre blocked (one row array):
 *    row0: 1 1 1     row1: 1 0 1     row2: 1 1 2   -> 2
 *
 *  COMPLEXITY   Time O(m*n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M15_UniquePathsII {

    static int uniquePathsWithObstacles(int[][] grid) {
        int n = grid[0].length;
        int[] dp = new int[n];
        dp[0] = 1;                                        // "one way to be at the start" (killed if it's blocked)
        for (int[] row : grid)
            for (int c = 0; c < n; c++) {
                if (row[c] == 1) dp[c] = 0;               // obstacle: no path ends here
                else if (c > 0) dp[c] += dp[c - 1];       // above + left
            }
        return dp[n - 1];
    }

    static int brute(int[][] g, int r, int c) {
        if (r >= g.length || c >= g[0].length || g[r][c] == 1) return 0;
        if (r == g.length - 1 && c == g[0].length - 1) return 1;
        return brute(g, r + 1, c) + brute(g, r, c + 1);
    }

    public static void main(String[] args) {
        check("centre blocked", 2, uniquePathsWithObstacles(new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 0}}));
        check("[[0,1],[0,0]]", 1, uniquePathsWithObstacles(new int[][]{{0, 1}, {0, 0}}));
        check("start blocked", 0, uniquePathsWithObstacles(new int[][]{{1}}));
        check("row 0 blocked mid-way", 1, uniquePathsWithObstacles(new int[][]{{0, 1, 0}, {0, 0, 0}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[][] g = new int[1 + rnd.nextInt(7)][1 + rnd.nextInt(7)];
            for (int[] row : g) for (int c = 0; c < row.length; c++) row[c] = rnd.nextInt(5) == 0 ? 1 : 0;
            ok &= uniquePathsWithObstacles(g) == brute(g, 0, 0);
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
