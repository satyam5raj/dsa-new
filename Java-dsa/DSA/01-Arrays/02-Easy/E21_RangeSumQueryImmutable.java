/*
 * ============================================================================
 *  EASY 21 : RANGE SUM QUERY - IMMUTABLE     LeetCode #303
 *  https://leetcode.com/problems/range-sum-query-immutable/
 *  Pattern: PREFIX SUM (precompute once, answer many queries in O(1))
 * ============================================================================
 *
 *  PROBLEM
 *    Design a class: NumArray(nums) and sumRange(left, right) = the sum of
 *    nums[left..right] inclusive. sumRange is called MANY times.
 *      nums = [-2,0,3,-5,2,-1]
 *      sumRange(0,2) = 1     sumRange(2,5) = -1     sumRange(0,5) = -3
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: loop from left to right on every call -> O(n) per query.
 *         10^4 queries x 10^4 elements = 10^8 operations. Too slow.
 *
 *  PREFIX SUM: build once in O(n), answer each query in O(1).
 *    prefix[i] = sum of nums[0 .. i-1]   (length n+1, prefix[0] = 0)
 *    sumRange(l, r) = prefix[r+1] - prefix[l]
 *    Why the extra leading 0? It makes l = 0 work without a special case.
 *
 *    nums   =     [-2,  0,  3, -5,  2, -1]
 *    prefix = [0,  -2, -2,  1, -4, -2, -3]
 *    sumRange(2,5) = prefix[6] - prefix[2] = -3 - (-2) = -1   ✓
 *
 *  WHAT IF nums CAN CHANGE? (#307 Range Sum Query - Mutable) -> each update would
 *  rebuild the prefix array in O(n). Use a FENWICK TREE or SEGMENT TREE: O(log n)
 *  for both update and query (covered in the Trees topic).
 *
 *  COMPLEXITY   build O(n), query O(1), Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E21_RangeSumQueryImmutable {

    static class NumArray {
        private final int[] prefix;

        NumArray(int[] nums) {
            prefix = new int[nums.length + 1];
            for (int i = 0; i < nums.length; i++) {
                prefix[i + 1] = prefix[i] + nums[i];
            }
        }

        int sumRange(int left, int right) {
            return prefix[right + 1] - prefix[left];
        }
    }

    public static void main(String[] args) {
        NumArray na = new NumArray(new int[]{-2, 0, 3, -5, 2, -1});
        check("sumRange(0,2)", 1, na.sumRange(0, 2));
        check("sumRange(2,5)", -1, na.sumRange(2, 5));
        check("sumRange(0,5)", -3, na.sumRange(0, 5));
        check("sumRange(3,3) (single)", -5, na.sumRange(3, 3));
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
