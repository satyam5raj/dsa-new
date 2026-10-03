/*
 * ============================================================================
 *  MEDIUM 11 : MINIMUM SIZE SUBARRAY SUM     LeetCode #209
 *  https://leetcode.com/problems/minimum-size-subarray-sum/
 *  Pattern: SLIDING WINDOW (variable size, shrink while valid)
 * ============================================================================
 *
 *  PROBLEM
 *    All numbers are POSITIVE. Return the minimal length of a contiguous
 *    subarray whose sum is >= target, or 0 if none exists.
 *      target=7, [2,3,1,2,4,3] -> 2   ([4,3])
 *      target=4, [1,4,4]       -> 1
 *      target=11, [1,1,1,1,1]  -> 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every start, extend until sum >= target -> O(n^2)
 *
 *  WHY A SLIDING WINDOW WORKS HERE: all numbers are POSITIVE, so
 *    - adding an element on the right ALWAYS increases the sum
 *    - removing an element on the left ALWAYS decreases it
 *  The sum is "monotonic" in the window size: we always know which way to move.
 *  (With negatives this breaks. Then use prefix sums + a deque, see Hard H04.)
 *
 *  TEMPLATE (minimum-length version)
 *    for right in 0..n-1:
 *      sum += nums[right]                          // EXPAND
 *      while sum >= target:                        // the window is VALID
 *        best = min(best, right - left + 1)        //   record it
 *        sum -= nums[left++]                       //   SHRINK to look for a shorter one
 *
 *  DRY RUN target=7, [2,3,1,2,4,3]
 *    r=0 [2]          sum 2
 *    r=1 [2,3]        sum 5
 *    r=2 [2,3,1]      sum 6
 *    r=3 [2,3,1,2]    sum 8 >= 7 -> best 4; drop 2 -> [3,1,2] sum 6
 *    r=4 [3,1,2,4]    sum 10    -> best 4; drop 3 -> [1,2,4] sum 7 -> best 3; drop 1 -> [2,4] sum 6
 *    r=5 [2,4,3]      sum 9     -> best 3; drop 2 -> [4,3] sum 7 -> best 2; drop 4 -> [3] sum 3
 *    answer 2
 *
 *  COMPLEXITY   Time O(n): each element is added once and removed at most once   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M11_MinimumSizeSubarraySum {

    static int minSubArrayLen(int target, int[] nums) {
        int left = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= nums[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public static void main(String[] args) {
        check("t=7 [2,3,1,2,4,3]", 2, minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));
        check("t=4 [1,4,4]", 1, minSubArrayLen(4, new int[]{1, 4, 4}));
        check("t=11 [1,1,1,1,1] (impossible)", 0, minSubArrayLen(11, new int[]{1, 1, 1, 1, 1}));
        check("t=15 [1,2,3,4,5] (whole array)", 5, minSubArrayLen(15, new int[]{1, 2, 3, 4, 5}));
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
