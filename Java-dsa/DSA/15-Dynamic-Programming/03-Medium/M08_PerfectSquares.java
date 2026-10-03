/*
 * ============================================================================
 *  MEDIUM 08 : PERFECT SQUARES               LeetCode #279
 *  https://leetcode.com/problems/perfect-squares/
 *  Pattern: UNBOUNDED knapsack, min count (coins = 1, 4, 9, 16, ...)
 * ============================================================================
 *
 *  PROBLEM
 *    Least number of perfect squares that sum to n.
 *      n=12 -> 3 (4+4+4)       n=13 -> 2 (4+9)
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY FAILS: 12 -> 9 + 1 + 1 + 1 = 4 squares, but 4+4+4 = 3.
 *
 *  It's Coin Change with coins = all squares <= n:
 *      STATE       dp[i] = fewest squares summing to i
 *      TRANSITION  dp[i] = 1 + min over squares s <= i of dp[i - s]
 *      BASE        dp[0] = 0
 *
 *  ALTERNATIVES worth mentioning
 *    BFS: level k = numbers reachable with k squares; the first level containing n
 *         is the answer (same O(n sqrt n), often faster in practice).
 *    MATH (Lagrange + Legendre): every n is a sum of 4 squares; it needs 4 exactly when
 *         n = 4^a (8b + 7); check 1 and 2 directly; otherwise 3. O(sqrt n).
 *
 *  DRY RUN n=12:  dp[12] = 1 + min(dp[11], dp[8], dp[3]) = 1 + min(3, 2, 3) = 3
 *
 *  COMPLEXITY   Time O(n sqrt n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M08_PerfectSquares {

    static int numSquares(int n) {
        int[] dp = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            dp[i] = Integer.MAX_VALUE;
            for (int s = 1; s * s <= i; s++) dp[i] = Math.min(dp[i], dp[i - s * s] + 1);
        }
        return dp[n];
    }

    static int numSquaresMath(int n) {
        if (isSquare(n)) return 1;
        int m = n;
        while (m % 4 == 0) m /= 4;
        if (m % 8 == 7) return 4;                         // Legendre's three-square theorem
        for (int a = 1; a * a <= n; a++) if (isSquare(n - a * a)) return 2;
        return 3;
    }

    static boolean isSquare(int x) { int r = (int) Math.sqrt(x); return r * r == x; }

    static int greedy(int n) { int c = 0; while (n > 0) { int r = (int) Math.sqrt(n); n -= r * r; c++; } return c; }

    public static void main(String[] args) {
        check("n=12", 3, numSquares(12));
        check("n=13", 2, numSquares(13));
        check("n=1", 1, numSquares(1));
        check("greedy on 12 (wrong)", 4, greedy(12));
        boolean ok = true;
        for (int n = 1; n <= 3000; n++) ok &= numSquares(n) == numSquaresMath(n);
        check("n = 1..3000: DP == number theory", true, ok);
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
