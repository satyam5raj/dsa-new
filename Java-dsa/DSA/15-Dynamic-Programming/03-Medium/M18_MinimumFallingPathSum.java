/*
 * ============================================================================
 *  MEDIUM 18 : MINIMUM FALLING PATH SUM      LeetCode #931
 *  https://leetcode.com/problems/minimum-falling-path-sum/
 *  Pattern: row-by-row grid DP with three predecessors
 * ============================================================================
 *
 *  PROBLEM
 *    n x n matrix. Start anywhere in row 0; from (r, c) go to (r+1, c-1), (r+1, c) or
 *    (r+1, c+1). Minimum sum of a falling path?
 *      [[2,1,3],[6,5,4],[7,8,9]] -> 13   (1 -> 5 -> 7  or  1 -> 4 -> 8)
 *      [[-19,57],[-40,-5]]       -> -59
 *
 *  ---------------------------------------------------------------------------
 *  STATE       dp[r][c] = min falling path sum ending at (r, c)
 *  TRANSITION  dp[r][c] = a[r][c] + min(dp[r-1][c-1], dp[r-1][c], dp[r-1][c+1])  (in range)
 *  BASE        dp[0] = row 0;          ANSWER min of the last row
 *  Two rows are enough: build `next` from `prev` (dp[c-1] would be overwritten if we
 *  updated in place, unlike the 2-predecessor grids).
 *
 *  DRY RUN [[2,1,3],[6,5,4],[7,8,9]]
 *    row0: 2 1 3     row1: 6+1=7 5+1=6 4+1=5     row2: 7+6=13 8+5=13 9+5=14  -> 13
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M18_MinimumFallingPathSum {

    static int minFallingPathSum(int[][] a) {
        int n = a.length;
        int[] prev = a[0].clone();
        for (int r = 1; r < n; r++) {
            int[] next = new int[n];
            for (int c = 0; c < n; c++) {
                int best = prev[c];
                if (c > 0) best = Math.min(best, prev[c - 1]);
                if (c < n - 1) best = Math.min(best, prev[c + 1]);
                next[c] = a[r][c] + best;
            }
            prev = next;
        }
        return Arrays.stream(prev).min().getAsInt();
    }

    static int brute(int[][] a, int r, int c) {
        if (c < 0 || c >= a.length) return Integer.MAX_VALUE;
        if (r == a.length - 1) return a[r][c];
        int best = Math.min(brute(a, r + 1, c), Math.min(brute(a, r + 1, c - 1), brute(a, r + 1, c + 1)));
        return a[r][c] + best;
    }

    public static void main(String[] args) {
        check("3x3 example", 13, minFallingPathSum(new int[][]{{2, 1, 3}, {6, 5, 4}, {7, 8, 9}}));
        check("[[-19,57],[-40,-5]]", -59, minFallingPathSum(new int[][]{{-19, 57}, {-40, -5}}));
        check("[[7]]", 7, minFallingPathSum(new int[][]{{7}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(6);
            int[][] a = new int[n][n];
            for (int[] row : a) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(41) - 20;
            int b = Integer.MAX_VALUE;
            for (int c = 0; c < n; c++) b = Math.min(b, brute(a, 0, c));
            ok &= minFallingPathSum(a) == b;
        }
        check("300 random matrices vs recursion", true, ok);
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
