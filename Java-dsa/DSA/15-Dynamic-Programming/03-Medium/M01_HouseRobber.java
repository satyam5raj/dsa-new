/*
 * ============================================================================
 *  MEDIUM 01 : HOUSE ROBBER                  LeetCode #198
 *  https://leetcode.com/problems/house-robber/
 *  Pattern: 1-D DP "take it or skip it"
 * ============================================================================
 *
 *  PROBLEM
 *    Houses in a row hold money; robbing two ADJACENT houses triggers the alarm.
 *    Maximum you can rob?
 *      [1,2,3,1]     -> 4   (1 + 3)
 *      [2,7,9,3,1]   -> 12  (2 + 9 + 1)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every non-adjacent subset -> O(2^n)
 *  GREEDY FAILS: "take the biggest" on [2,3,2] takes 3 -> 3, but 2+2 = 4.
 *
 *  KEY INSIGHT: decide house i. Either
 *      skip it -> best of the first i-1 houses              dp[i-1]
 *      rob it  -> house i-1 is forbidden: dp[i-2] + a[i]
 *      STATE       dp[i] = best using houses 0..i
 *      TRANSITION  dp[i] = max(dp[i-1], dp[i-2] + a[i])
 *      BASE        dp[-1] = dp[-2] = 0
 *  Only two previous values are read -> O(1) space.
 *
 *  DRY RUN [2,7,9,3,1]   (prev2, prev1) start at (0, 0)
 *    2 -> max(0, 0+2)=2      7 -> max(2, 0+7)=7      9 -> max(7, 2+9)=11
 *    3 -> max(11, 7+3)=11    1 -> max(11, 11+1)=12
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  Follow-ups: circle -> M02 House Robber II; tree -> Trees M13 House Robber III.
 * ============================================================================
 */

import java.util.*;

public class M01_HouseRobber {

    static int rob(int[] nums) {
        int prev2 = 0, prev1 = 0;                          // best up to i-2, best up to i-1
        for (int x : nums) {
            int cur = Math.max(prev1, prev2 + x);          // skip vs rob
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static int brute(int[] a) {                           // every subset with no two adjacent
        int best = 0;
        for (int mask = 0; mask < 1 << a.length; mask++) {
            if ((mask & (mask >> 1)) != 0) continue;       // two adjacent bits set
            int s = 0;
            for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s += a[i];
            best = Math.max(best, s);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[1,2,3,1]", 4, rob(new int[]{1, 2, 3, 1}));
        check("[2,7,9,3,1]", 12, rob(new int[]{2, 7, 9, 3, 1}));
        check("[2,3,2] (greedy fails)", 4, rob(new int[]{2, 3, 2}));
        check("[5]", 5, rob(new int[]{5}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = new int[1 + rnd.nextInt(15)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(50);
            ok &= rob(a) == brute(a);
        }
        check("500 random vs every valid subset", true, ok);
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
