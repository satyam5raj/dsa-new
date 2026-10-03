/*
 * ============================================================================
 *  MEDIUM 17 : TRIANGLE                      LeetCode #120
 *  https://leetcode.com/problems/triangle/
 *  Pattern: grid DP filled BOTTOM-UP (the direction choice removes edge cases)
 * ============================================================================
 *
 *  PROBLEM
 *    Minimum path sum from top to bottom; from index i you may go to i or i+1 on the
 *    next row.
 *      [[2],[3,4],[6,5,7],[4,1,8,3]] -> 11   (2 + 3 + 5 + 1)
 *
 *  ---------------------------------------------------------------------------
 *  TOP-DOWN DIRECTION: dp[r][i] = t[r][i] + min(dp[r-1][i-1], dp[r-1][i]) needs care at
 *  both edges (i = 0 and i = r), and the answer is min of the whole last row.
 *
 *  BOTTOM-UP DIRECTION is cleaner:
 *      STATE       dp[i] = min path sum from (r, i) DOWN to the bottom
 *      TRANSITION  dp[i] = t[r][i] + min(dp[i], dp[i + 1])      (the row below)
 *      BASE        dp = the last row
 *      ANSWER      dp[0]  - a single apex, no final min, no edge cases
 *  Updating dp[i] left to right is safe: dp[i+1] still holds the row below when read.
 *
 *  DRY RUN   start [4,1,8,3]
 *    row [6,5,7] -> [7,6,10]      row [3,4] -> [9,10]      row [2] -> [11]
 *
 *  COMPLEXITY   Time O(n^2) (number of cells)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M17_Triangle {

    static int minimumTotal(List<List<Integer>> triangle) {
        int n = triangle.size();
        int[] dp = new int[n + 1];                        // one extra 0 so the last row needs no special case
        for (int r = n - 1; r >= 0; r--)
            for (int i = 0; i <= r; i++) dp[i] = triangle.get(r).get(i) + Math.min(dp[i], dp[i + 1]);
        return dp[0];
    }

    static int brute(List<List<Integer>> t, int r, int i) {
        if (r == t.size()) return 0;
        return t.get(r).get(i) + Math.min(brute(t, r + 1, i), brute(t, r + 1, i + 1));
    }

    public static void main(String[] args) {
        check("4-row example", 11, minimumTotal(List.of(List.of(2), List.of(3, 4), List.of(6, 5, 7), List.of(4, 1, 8, 3))));
        check("[[-10]]", -10, minimumTotal(List.of(List.of(-10))));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            List<List<Integer>> tri = new ArrayList<>();
            for (int r = 0; r < 1 + rnd.nextInt(10); r++) {
                List<Integer> row = new ArrayList<>();
                for (int i = 0; i <= r; i++) row.add(rnd.nextInt(21) - 10);
                tri.add(row);
            }
            ok &= minimumTotal(tri) == brute(tri, 0, 0);
        }
        check("500 random triangles vs recursion", true, ok);
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
