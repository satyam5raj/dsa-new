/*
 * ============================================================================
 *  MEDIUM 14 : SUBARRAY PRODUCT LESS THAN K  LeetCode #713
 *  https://leetcode.com/problems/subarray-product-less-than-k/
 *  Pattern: SLIDING WINDOW + "COUNT SUBARRAYS ENDING AT right"
 * ============================================================================
 *
 *  PROBLEM
 *    Positive integers. Count the contiguous subarrays whose product is
 *    STRICTLY less than k.
 *      [10,5,2,6], k=100 -> 8
 *      [10],[5],[2],[6],[10,5],[5,2],[2,6],[5,2,6]
 *
 *  ---------------------------------------------------------------------------
 *  SLIDING WINDOW (positive numbers -> the product grows when expanding, shrinks when shrinking)
 *    keep the window [left..right] with product < k
 *
 *  THE COUNTING TRICK (very reusable!)
 *    When [left..right] is valid, EVERY subarray that ENDS at right and starts
 *    at or after left is also valid (they're sub-windows with smaller products):
 *      [left..right], [left+1..right], ..., [right..right]
 *    That's exactly (right - left + 1) NEW subarrays. Add them up for every right.
 *    Each subarray is counted exactly once: by its END position.
 *
 *  DRY RUN [10,5,2,6], k=100
 *    r=0  window [10]        prod 10   +1  (total 1)
 *    r=1  window [10,5]      prod 50   +2  (total 3)   [5], [10,5]
 *    r=2  [10,5,2] = 100 >= k -> drop 10 -> [5,2] prod 10   +2  (total 5)
 *    r=3  [5,2,6]            prod 60   +3  (total 8)
 *
 *  EDGE CASE: k <= 1 -> no product of positive integers is < 1 -> 0.
 *  (Without this guard, the while loop would push left past right.)
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M14_SubarrayProductLessThanK {

    static int numSubarrayProductLessThanK(int[] nums, int k) {
        if (k <= 1) return 0;
        long product = 1;
        int left = 0, count = 0;
        for (int right = 0; right < nums.length; right++) {
            product *= nums[right];
            while (product >= k) {
                product /= nums[left++];        // shrink until valid again
            }
            count += right - left + 1;          // subarrays ending at 'right'
        }
        return count;
    }

    public static void main(String[] args) {
        check("[10,5,2,6] k=100", 8, numSubarrayProductLessThanK(new int[]{10, 5, 2, 6}, 100));
        check("[1,2,3] k=0", 0, numSubarrayProductLessThanK(new int[]{1, 2, 3}, 0));
        check("[1,1,1] k=2 (all valid)", 6, numSubarrayProductLessThanK(new int[]{1, 1, 1}, 2));
        check("[100] k=100 (strictly less)", 0, numSubarrayProductLessThanK(new int[]{100}, 100));
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
