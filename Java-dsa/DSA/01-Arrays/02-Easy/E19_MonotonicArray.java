/*
 * ============================================================================
 *  EASY 19 : MONOTONIC ARRAY                 LeetCode #896
 *  https://leetcode.com/problems/monotonic-array/
 *  Pattern: SINGLE PASS WITH FLAGS
 * ============================================================================
 *
 *  PROBLEM
 *    An array is monotonic if it is entirely non-decreasing OR entirely non-increasing.
 *      [1,2,2,3] -> true     [6,5,4,4] -> true     [1,3,2] -> false
 *
 *  ---------------------------------------------------------------------------
 *  IDEA: assume it could be BOTH, and rule possibilities out as we go.
 *    increasing = true, decreasing = true
 *    for each neighbour pair:
 *      a[i] > a[i-1] -> it can't be non-increasing -> decreasing = false
 *      a[i] < a[i-1] -> it can't be non-decreasing -> increasing = false
 *    answer = increasing || decreasing
 *  Equal neighbours don't rule anything out (that's why "NON-decreasing").
 *
 *  DRY RUN [1,3,2]
 *    3 > 1 -> decreasing = false
 *    2 < 3 -> increasing = false   -> neither -> false
 *
 *  COMPLEXITY   Time O(n)   Space O(1)   (could stop early once both flags are false)
 * ============================================================================
 */

import java.util.*;

public class E19_MonotonicArray {

    static boolean isMonotonic(int[] nums) {
        boolean increasing = true, decreasing = true;
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > nums[i - 1]) decreasing = false;
            if (nums[i] < nums[i - 1]) increasing = false;
            if (!increasing && !decreasing) return false;    // early exit
        }
        return true;
    }

    public static void main(String[] args) {
        check("[1,2,2,3]", true, isMonotonic(new int[]{1, 2, 2, 3}));
        check("[6,5,4,4]", true, isMonotonic(new int[]{6, 5, 4, 4}));
        check("[1,3,2]", false, isMonotonic(new int[]{1, 3, 2}));
        check("[5,5,5] (all equal)", true, isMonotonic(new int[]{5, 5, 5}));
        check("[1] (single)", true, isMonotonic(new int[]{1}));
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
