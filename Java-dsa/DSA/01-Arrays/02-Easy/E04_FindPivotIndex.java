/*
 * ============================================================================
 *  EASY 04 : FIND PIVOT INDEX                LeetCode #724
 *  https://leetcode.com/problems/find-pivot-index/
 *  Pattern: PREFIX SUM (left sum vs right sum)
 * ============================================================================
 *
 *  PROBLEM
 *    The pivot index is where the sum of everything STRICTLY LEFT equals the sum
 *    of everything STRICTLY RIGHT. Return the LEFTMOST pivot, or -1.
 *      [1,7,3,6,5,6] -> 3    left = 1+7+3 = 11, right = 5+6 = 11
 *      [1,2,3]       -> -1
 *      [2,1,-1]      -> 0    left = 0 (nothing), right = 1 + (-1) = 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each i, sum the left part and the right part -> O(n^2)
 *
 *  KEY INSIGHT: we don't need to recompute the right side.
 *    total     = sum of all elements (computed once)
 *    leftSum   = running sum of elements BEFORE i
 *    rightSum  = total - leftSum - nums[i]
 *    pivot when leftSum == rightSum
 *    -> two passes, O(n), O(1) space.
 *
 *  DRY RUN [1,7,3,6,5,6], total = 28
 *    i  nums[i]  leftSum  rightSum = 28 - left - nums[i]
 *    0    1        0        27
 *    1    7        1        20
 *    2    3        8        17
 *    3    6       11        11    <- equal -> return 3
 *
 *  COMMON BUG: updating leftSum BEFORE comparing (that would include nums[i] on the left).
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E04_FindPivotIndex {

    static int pivotIndex(int[] nums) {
        int total = 0;
        for (int x : nums) total += x;

        int leftSum = 0;
        for (int i = 0; i < nums.length; i++) {
            int rightSum = total - leftSum - nums[i];
            if (leftSum == rightSum) return i;     // compare FIRST...
            leftSum += nums[i];                    // ...then move nums[i] to the left side
        }
        return -1;
    }

    public static void main(String[] args) {
        check("[1,7,3,6,5,6]", 3, pivotIndex(new int[]{1, 7, 3, 6, 5, 6}));
        check("[1,2,3] (no pivot)", -1, pivotIndex(new int[]{1, 2, 3}));
        check("[2,1,-1] (pivot at 0)", 0, pivotIndex(new int[]{2, 1, -1}));
        check("[-1,-1,0,1,1,0] (leftmost)", 5, pivotIndex(new int[]{-1, -1, 0, 1, 1, 0}));
        check("[0] (single)", 0, pivotIndex(new int[]{0}));
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
