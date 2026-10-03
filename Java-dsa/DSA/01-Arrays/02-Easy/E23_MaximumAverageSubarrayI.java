/*
 * ============================================================================
 *  EASY 23 : MAXIMUM AVERAGE SUBARRAY I      LeetCode #643
 *  https://leetcode.com/problems/maximum-average-subarray-i/
 *  Pattern: SLIDING WINDOW (fixed size k)
 * ============================================================================
 *
 *  PROBLEM
 *    Find the contiguous subarray of length EXACTLY k with the maximum average.
 *      nums = [1,12,-5,-6,50,3], k = 4  ->  12.75     ([12,-5,-6,50] -> 51/4)
 *
 *  ---------------------------------------------------------------------------
 *  OBSERVATION: max average of size k == max SUM of size k, divided by k.
 *  (k is fixed, so compare sums and divide once at the end. Avoids float work in the loop.)
 *
 *  BRUTE FORCE: sum each of the n-k+1 windows from scratch -> O(n * k)
 *
 *  SLIDING WINDOW: consecutive windows share k-1 elements!
 *    next window sum = previous sum + (element entering) - (element leaving)
 *
 *    nums = [1, 12, -5, -6, 50, 3], k = 4
 *    window [1,12,-5,-6]   sum = 2
 *    slide: +50 -1         sum = 51    <- max
 *    slide: +3  -12        sum = 42
 *    answer = 51 / 4 = 12.75
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E23_MaximumAverageSubarrayI {

    static double findMaxAverage(int[] nums, int k) {
        int sum = 0;
        for (int i = 0; i < k; i++) sum += nums[i];          // the first window
        int best = sum;
        for (int i = k; i < nums.length; i++) {
            sum += nums[i] - nums[i - k];                    // add the new one, drop the old one
            best = Math.max(best, sum);
        }
        return (double) best / k;                            // cast BEFORE dividing (integer division trap)
    }

    public static void main(String[] args) {
        check("[1,12,-5,-6,50,3] k=4", 12.75, findMaxAverage(new int[]{1, 12, -5, -6, 50, 3}, 4));
        check("[5] k=1", 5.0, findMaxAverage(new int[]{5}, 1));
        check("[-1,-2,-3] k=2 (negatives)", -1.5, findMaxAverage(new int[]{-1, -2, -3}, 2));
        check("[0,4,0,3,2] k=1", 4.0, findMaxAverage(new int[]{0, 4, 0, 3, 2}, 1));
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
