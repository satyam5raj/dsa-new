/*
 * ============================================================================
 *  HARD 01 : CONSTRAINED SUBSEQUENCE SUM  ⭐ bonus   LeetCode #1425
 *  https://leetcode.com/problems/constrained-subsequence-sum/
 *  Pattern: DP + MONOTONIC DEQUE (Jump Game VI, with "start fresh" allowed)
 * ============================================================================
 *
 *  PROBLEM
 *    Choose a NON-EMPTY subsequence where each pair of CONSECUTIVE chosen indices
 *    is at most k apart (j - i <= k). Maximize its sum.
 *      [10,2,-10,5,20], k=2 -> 37    (10, 2, 5, 20)
 *      [-1,-2,-3], k=1      -> -1    (just pick -1)
 *      [10,-2,-10,-5,20], k=2 -> 23  (10, -2, -5, 20)
 *
 *  ---------------------------------------------------------------------------
 *  DP
 *    dp[i] = the best sum of a valid subsequence that ENDS by choosing index i
 *    dp[i] = nums[i] + max(0, max(dp[i-k .. i-1]))
 *  The max(0, ...) is the difference from Jump Game VI (M06): we may START a new
 *  subsequence at i if all reachable previous sums are negative (like Kadane!).
 *  answer = max over all dp[i]  (the subsequence may end anywhere)
 *
 *  The window max over dp[i-k .. i-1] -> MONOTONIC DEQUE of indices with decreasing dp.
 *  Optimization: only push i if dp[i] > 0. A non-positive dp would never be chosen
 *  as a predecessor (max(0, ...) beats it).
 *
 *  DRY RUN [10,2,-10,5,20], k=2
 *    dp0 = 10                          dq [0]
 *    dp1 = 2 + 10 = 12                 dq [1]       (pops 0: 10 <= 12)
 *    dp2 = -10 + 12 = 2                dq [1,2]
 *    dp3 = 5 + max(12, 2) = 17         dq [3]       (1 is still in range: 3-1 = 2)
 *    dp4 = 20 + 17 = 37                -> answer 37
 *
 *  This is Kadane's idea + a sliding-window maximum, two tools from the Arrays topic.
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H01_ConstrainedSubsequenceSum {

    static int constrainedSubsetSum(int[] nums, int k) {
        int n = nums.length;
        int[] dp = new int[n];
        Deque<Integer> dq = new ArrayDeque<>();                  // indices, dp decreasing (all > 0)
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            if (!dq.isEmpty() && dq.peekFirst() < i - k) dq.pollFirst();
            dp[i] = nums[i] + (dq.isEmpty() ? 0 : dp[dq.peekFirst()]);   // 0 = start fresh
            best = Math.max(best, dp[i]);
            while (!dq.isEmpty() && dp[dq.peekLast()] <= dp[i]) dq.pollLast();
            if (dp[i] > 0) dq.offerLast(i);                      // only useful as a predecessor if positive
        }
        return best;
    }

    static int brute(int[] nums, int k) {
        int n = nums.length, best = Integer.MIN_VALUE;
        int[] dp = new int[n];
        for (int i = 0; i < n; i++) {
            int prev = 0;
            for (int j = Math.max(0, i - k); j < i; j++) prev = Math.max(prev, dp[j]);
            dp[i] = nums[i] + prev;
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[10,2,-10,5,20] k=2", 37, constrainedSubsetSum(new int[]{10, 2, -10, 5, 20}, 2));
        check("[-1,-2,-3] k=1 (all negative)", -1, constrainedSubsetSum(new int[]{-1, -2, -3}, 1));
        check("[10,-2,-10,-5,20] k=2", 23, constrainedSubsetSum(new int[]{10, -2, -10, -5, 20}, 2));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), -10, 10).toArray();
            int k = 1 + rnd.nextInt(5);
            ok &= constrainedSubsetSum(a, k) == brute(a, k);
        }
        check("500 random cases vs O(n*k) DP", true, ok);
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
