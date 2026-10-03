/*
 * ============================================================================
 *  MEDIUM 01 : MAXIMUM SUBARRAY              LeetCode #53
 *  https://leetcode.com/problems/maximum-subarray/
 *  Pattern: KADANE'S ALGORITHM (dynamic programming in O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    Find the contiguous subarray (at least one element) with the LARGEST sum.
 *      [-2,1,-3,4,-1,2,1,-5,4] -> 6      ([4,-1,2,1])
 *      [5,4,-1,7,8]            -> 23     (the whole array)
 *      [-3,-1,-2]              -> -1     (all negative: the single largest element)
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: BRUTE FORCE - every (start, end), with a running sum    O(n^2)
 *
 *  APPROACH 2: KADANE                                                  O(n), O(1)
 *    Define  best_ending_here(i) = the max sum of a subarray that ENDS at index i.
 *    That subarray either:
 *      (a) is just nums[i] alone (start fresh), or
 *      (b) extends the best subarray ending at i-1:  best_ending_here(i-1) + nums[i]
 *    So:  cur = max(nums[i], cur + nums[i])
 *    In words: "if what I've accumulated so far is NEGATIVE, it only drags me
 *    down. Drop it and start over from here."
 *    The answer is the maximum 'cur' seen anywhere.
 *
 *  DRY RUN [-2,1,-3,4,-1,2,1,-5,4]
 *    x    cur = max(x, cur+x)    best
 *   -2         -2                 -2
 *    1    max(1, -1)  =  1         1     <- start fresh (old sum was negative)
 *   -3    max(-3, -2) = -2         1
 *    4    max(4, 2)   =  4         4     <- start fresh
 *   -1    3                        4
 *    2    5                        5
 *    1    6                        6     <- [4,-1,2,1]
 *   -5    1                        6
 *    4    5                        6
 *
 *  WHY INITIALISE WITH nums[0] AND NOT 0? With all-negative input the answer
 *  is negative ([-3,-1,-2] -> -1). Starting best at 0 would wrongly return 0.
 *
 *  BONUS: track WHERE the best subarray starts and ends (shown below), and the
 *  DIVIDE & CONQUER O(n log n) version is a classic follow-up question.
 * ============================================================================
 */

import java.util.*;

public class M01_MaximumSubarray {

    static int maxSubArray(int[] nums) {
        int cur = nums[0], best = nums[0];
        for (int i = 1; i < nums.length; i++) {
            cur = Math.max(nums[i], cur + nums[i]);   // extend or restart
            best = Math.max(best, cur);
        }
        return best;
    }

    // Same algorithm, but also returns the subarray boundaries [sum, start, end]
    static int[] maxSubArrayWithIndices(int[] nums) {
        int cur = nums[0], best = nums[0];
        int start = 0, bestStart = 0, bestEnd = 0;
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > cur + nums[i]) {             // restarting is better
                cur = nums[i];
                start = i;
            } else {
                cur += nums[i];
            }
            if (cur > best) {
                best = cur;
                bestStart = start;
                bestEnd = i;
            }
        }
        return new int[]{best, bestStart, bestEnd};
    }

    public static void main(String[] args) {
        check("[-2,1,-3,4,-1,2,1,-5,4]", 6, maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        check("[5,4,-1,7,8] (whole array)", 23, maxSubArray(new int[]{5, 4, -1, 7, 8}));
        check("[-3,-1,-2] (all negative)", -1, maxSubArray(new int[]{-3, -1, -2}));
        check("[1] (single)", 1, maxSubArray(new int[]{1}));
        check("with indices [sum,start,end]", new int[]{6, 3, 6}, maxSubArrayWithIndices(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
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
