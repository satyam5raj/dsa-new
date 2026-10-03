/*
 * ============================================================================
 *  EASY 03 : RUNNING SUM OF 1D ARRAY         LeetCode #1480
 *  https://leetcode.com/problems/running-sum-of-1d-array/
 *  Pattern: PREFIX SUM (the simplest form)
 * ============================================================================
 *
 *  PROBLEM
 *    runningSum[i] = nums[0] + nums[1] + ... + nums[i]
 *      [1,2,3,4] -> [1,3,6,10]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each i, add up nums[0..i] from scratch -> O(n^2)
 *    We recompute the same partial sums again and again.
 *
 *  KEY INSIGHT: each answer = the PREVIOUS answer + the current element
 *    runningSum[i] = runningSum[i-1] + nums[i]
 *  So one pass is enough -> O(n). We can even write the results INTO nums
 *  (in-place, O(1) extra space).
 *
 *  DRY RUN [1,2,3,4]
 *    i=1: nums[1] = 2 + 1 = 3     -> [1,3,3,4]
 *    i=2: nums[2] = 3 + 3 = 6     -> [1,3,6,4]
 *    i=3: nums[3] = 4 + 6 = 10    -> [1,3,6,10]
 *
 *  WHY IT MATTERS: this "prefix sum" array is the foundation of Pivot Index,
 *  Range Sum Query and Subarray Sum Equals K (all coming up).
 *
 *  COMPLEXITY   Time O(n)   Space O(1) extra (in-place)
 * ============================================================================
 */

import java.util.*;

public class E03_RunningSum {

    static int[] runningSum(int[] nums) {
        for (int i = 1; i < nums.length; i++) {
            nums[i] += nums[i - 1];        // nums[i-1] already holds the sum up to i-1
        }
        return nums;
    }

    public static void main(String[] args) {
        check("[1,2,3,4]", new int[]{1, 3, 6, 10}, runningSum(new int[]{1, 2, 3, 4}));
        check("[1,1,1,1,1]", new int[]{1, 2, 3, 4, 5}, runningSum(new int[]{1, 1, 1, 1, 1}));
        check("[3,1,2,10,1]", new int[]{3, 4, 6, 16, 17}, runningSum(new int[]{3, 1, 2, 10, 1}));
        check("[-1,2,-3] (negatives)", new int[]{-1, 1, -2}, runningSum(new int[]{-1, 2, -3}));
        check("[5] (single)", new int[]{5}, runningSum(new int[]{5}));
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
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
