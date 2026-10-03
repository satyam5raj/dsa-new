/*
 * ============================================================================
 *  EASY 02 : MIN COST CLIMBING STAIRS  ⭐     LeetCode #746
 *  https://leetcode.com/problems/min-cost-climbing-stairs/
 *  Pattern: 1-D DP with MIN instead of +  (counting -> optimization)
 * ============================================================================
 *
 *  PROBLEM
 *    cost[i] is paid when you step ON stair i. Start on stair 0 or 1; each move climbs
 *    1 or 2. The top is just past the last stair. Minimum total cost?
 *      [10,15,20]                         -> 15   (start at 1, jump 2)
 *      [1,100,1,1,1,100,1,1,100,1]        -> 6
 *
 *  ---------------------------------------------------------------------------
 *  Same shape as Climbing Stairs, but we MINIMISE instead of COUNT:
 *      STATE       dp[i] = min cost to ARRIVE at position i (not yet paying cost[i])
 *      TRANSITION  dp[i] = min(dp[i-1] + cost[i-1], dp[i-2] + cost[i-2])
 *      BASE        dp[0] = dp[1] = 0    (you may start on either for free)
 *      ANSWER      dp[n]                (the top)
 *  Choosing the state definition ("arrive at" vs "stand on and pay") is the main
 *  decision; either works if the base cases and answer cell match it.
 *
 *  DRY RUN [10,15,20]:  dp0=0 dp1=0 dp2=min(0+15, 0+10)=10 dp3=min(10+20, 0+15)=15
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E02_MinCostClimbingStairs {

    static int minCostClimbingStairs(int[] cost) {
        int prev2 = 0, prev1 = 0;                         // dp[i-2], dp[i-1]
        for (int i = 2; i <= cost.length; i++) {
            int cur = Math.min(prev1 + cost[i - 1], prev2 + cost[i - 2]);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static int brute(int[] cost, int i) {                // min cost from standing on i (paying it) to the top
        if (i >= cost.length) return 0;
        return cost[i] + Math.min(brute(cost, i + 1), brute(cost, i + 2));
    }

    public static void main(String[] args) {
        check("[10,15,20]", 15, minCostClimbingStairs(new int[]{10, 15, 20}));
        check("[1,100,1,1,1,100,1,1,100,1]", 6, minCostClimbingStairs(new int[]{1, 100, 1, 1, 1, 100, 1, 1, 100, 1}));
        check("[0,0]", 0, minCostClimbingStairs(new int[]{0, 0}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] c = new int[2 + rnd.nextInt(15)];
            for (int i = 0; i < c.length; i++) c[i] = rnd.nextInt(100);
            ok &= minCostClimbingStairs(c) == Math.min(brute(c, 0), brute(c, 1));
        }
        check("500 random vs recursion", true, ok);
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
