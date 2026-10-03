/*
 * ============================================================================
 *  EASY 07 : REMOVE DUPLICATES FROM SORTED ARRAY   LeetCode #26
 *  https://leetcode.com/problems/remove-duplicates-from-sorted-array/
 *  Pattern: TWO POINTERS (slow/fast) + "sorted means duplicates are adjacent"
 * ============================================================================
 *
 *  PROBLEM
 *    nums is sorted ascending. Remove duplicates IN-PLACE so each value appears
 *    once. Return k = the number of unique values. The first k slots must hold
 *    them in order (whatever is after k doesn't matter).
 *      [1,1,2]             -> k=2, nums starts [1,2,...]
 *      [0,0,1,1,1,2,2,3,3,4] -> k=5, nums starts [0,1,2,3,4,...]
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: the array is SORTED -> duplicates are NEIGHBOURS. A value is
 *  "new" exactly when it differs from the LAST UNIQUE value we kept.
 *
 *  TWO POINTERS
 *    k = length of the unique prefix (nums[0..k-1] are unique). Starts at 1 (the first element is always kept).
 *    i scans from 1. If nums[i] != nums[k-1] (a new value): write it at nums[k], k++.
 *
 *  DRY RUN [0,0,1,1,1,2,2,3,3,4]
 *    i=1  0 == nums[0]=0  skip
 *    i=2  1 != 0  -> nums[1]=1  k=2
 *    i=3,4  1 == 1  skip
 *    i=5  2 != 1  -> nums[2]=2  k=3
 *    i=6  skip
 *    i=7  3 != 2  -> nums[3]=3  k=4
 *    i=8  skip
 *    i=9  4 != 3  -> nums[4]=4  k=5      -> [0,1,2,3,4, ...]
 *
 *  FOLLOW-UP (LeetCode #80): allow each value at most TWICE.
 *    Same idea, compare with nums[k-2] instead of nums[k-1]. (Implemented below.)
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E07_RemoveDuplicatesFromSortedArray {

    static int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;
        int k = 1;
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] != nums[k - 1]) {      // a value we haven't kept yet
                nums[k++] = nums[i];
            }
        }
        return k;
    }

    // Follow-up #80: keep at most 2 copies of each value
    static int removeDuplicatesAtMostTwice(int[] nums) {
        int k = 0;
        for (int x : nums) {
            if (k < 2 || x != nums[k - 2]) nums[k++] = x;   // allowed if not equal to the one TWO back
        }
        return k;
    }

    static int[] uniquePrefix(int[] nums) {
        return Arrays.copyOf(nums, removeDuplicates(nums));
    }

    public static void main(String[] args) {
        check("[1,1,2]", new int[]{1, 2}, uniquePrefix(new int[]{1, 1, 2}));
        check("[0,0,1,1,1,2,2,3,3,4]", new int[]{0, 1, 2, 3, 4}, uniquePrefix(new int[]{0, 0, 1, 1, 1, 2, 2, 3, 3, 4}));
        check("[1,2,3] (no dups)", new int[]{1, 2, 3}, uniquePrefix(new int[]{1, 2, 3}));
        check("[5,5,5] (all same)", new int[]{5}, uniquePrefix(new int[]{5, 5, 5}));
        check("k for [] (empty)", 0, removeDuplicates(new int[]{}));
        int[] a = {1, 1, 1, 2, 2, 3};
        check("#80 at most twice", new int[]{1, 1, 2, 2, 3}, Arrays.copyOf(a, removeDuplicatesAtMostTwice(a)));
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
