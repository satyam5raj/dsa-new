/*
 * ============================================================================
 *  MEDIUM 14 : UNIQUE PATHS                  LeetCode #62
 *  https://leetcode.com/problems/unique-paths/
 *  Pattern: GRID DP (counting) + rolling one row  (+ the combinatorics shortcut)
 * ============================================================================
 *
 *  PROBLEM
 *    A robot at the top-left of an m x n grid moves only RIGHT or DOWN. How many
 *    paths reach the bottom-right?
 *      m=3, n=7 -> 28        m=3, n=2 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: the last move into (r, c) came from above or from the left.
 *      STATE       dp[r][c] = number of paths to (r, c)
 *      TRANSITION  dp[r][c] = dp[r-1][c] + dp[r][c-1]
 *      BASE        first row and first column = 1 (only one straight path)
 *      ANSWER      dp[m-1][n-1]
 *
 *  SPACE: row r only needs row r-1 -> ONE array, updated left to right:
 *      dp[c] += dp[c - 1]      (before the update dp[c] = "above", dp[c-1] = "left")
 *
 *  MATH: every path is (m-1) downs and (n-1) rights in some order:
 *      C(m + n - 2, m - 1)  - computed incrementally to avoid overflow.
 *
 *  DRY RUN m=3, n=3:   1 1 1  /  1 2 3  /  1 3 6   -> 6
 *
 *  COMPLEXITY   Time O(m*n)   Space O(n)      Math: O(min(m, n))
 * ============================================================================
 */

import java.util.*;

public class M14_UniquePaths {

    static int uniquePaths(int m, int n) {
        int[] dp = new int[n];
        Arrays.fill(dp, 1);                               // row 0: all ones
        for (int r = 1; r < m; r++)
            for (int c = 1; c < n; c++) dp[c] += dp[c - 1];   // above (old dp[c]) + left (new dp[c-1])
        return dp[n - 1];
    }

    static long uniquePathsMath(int m, int n) {
        long res = 1;
        int k = Math.min(m, n) - 1, total = m + n - 2;
        for (int i = 1; i <= k; i++) res = res * (total - k + i) / i;   // stays an integer at every step
        return res;
    }

    static int brute(int r, int c) { return r == 0 || c == 0 ? 1 : brute(r - 1, c) + brute(r, c - 1); }

    public static void main(String[] args) {
        check("3 x 7", 28, uniquePaths(3, 7));
        check("3 x 2", 3, uniquePaths(3, 2));
        check("1 x 1", 1, uniquePaths(1, 1));
        check("23 x 12", 193536720, uniquePaths(23, 12));
        boolean ok = true;
        for (int m = 1; m <= 12; m++) for (int n = 1; n <= 12; n++) ok &= uniquePaths(m, n) == brute(m - 1, n - 1) && uniquePaths(m, n) == uniquePathsMath(m, n);
        check("all 12x12 sizes: DP == recursion == C(.,.)", true, ok);
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
