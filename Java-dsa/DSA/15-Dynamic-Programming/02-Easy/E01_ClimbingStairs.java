/*
 * ============================================================================
 *  EASY 01 : CLIMBING STAIRS                 LeetCode #70
 *  https://leetcode.com/problems/climbing-stairs/
 *  Pattern: 1-D DP, the "hello world" of DP (it's Fibonacci)
 * ============================================================================
 *
 *  PROBLEM
 *    n steps; each move climbs 1 or 2. How many distinct ways to reach the top?
 *      n=2 -> 2 (1+1, 2)      n=3 -> 3 (1+1+1, 1+2, 2+1)      n=45 -> 1836311903
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: recursion ways(n) = ways(n-1) + ways(n-2)  -> O(2^n), recomputes everything
 *
 *  KEY INSIGHT: the LAST move was either 1 step (from n-1) or 2 steps (from n-2).
 *  Those two groups don't overlap and cover everything, so
 *      STATE       dp[i] = number of ways to reach step i
 *      TRANSITION  dp[i] = dp[i-1] + dp[i-2]
 *      BASE        dp[0] = 1 (stand still), dp[1] = 1
 *      ANSWER      dp[n]
 *  dp[i] only reads the previous two -> two variables.
 *
 *  DRY RUN n=5:  1, 1, 2, 3, 5, 8  -> 8
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  (Recursion E01 Fibonacci shows the O(log n) matrix-power version.)
 * ============================================================================
 */

import java.util.*;

public class E01_ClimbingStairs {

    static int climbStairs(int n) {
        int prev2 = 1, prev1 = 1;                         // dp[i-2], dp[i-1], starting at dp[0], dp[1]
        for (int i = 2; i <= n; i++) {
            int cur = prev1 + prev2;
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static int brute(int n) { return n <= 1 ? 1 : brute(n - 1) + brute(n - 2); }

    public static void main(String[] args) {
        check("n=1", 1, climbStairs(1));
        check("n=2", 2, climbStairs(2));
        check("n=3", 3, climbStairs(3));
        check("n=5", 8, climbStairs(5));
        check("n=45 (max)", 1836311903, climbStairs(45));
        boolean ok = true;
        for (int n = 1; n <= 30; n++) ok &= climbStairs(n) == brute(n);
        check("n = 1..30 vs plain recursion", true, ok);
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
