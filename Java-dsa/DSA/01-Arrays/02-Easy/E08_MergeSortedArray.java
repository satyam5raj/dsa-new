/*
 * ============================================================================
 *  EASY 08 : MERGE SORTED ARRAY              LeetCode #88
 *  https://leetcode.com/problems/merge-sorted-array/
 *  Pattern: TWO POINTERS FROM THE BACK
 * ============================================================================
 *
 *  PROBLEM
 *    nums1 has length m + n: the first m elements are real, the last n are 0
 *    placeholders. nums2 has n elements. Both are sorted. Merge nums2 INTO nums1
 *    so nums1 becomes one sorted array.
 *      nums1 = [1,2,3,0,0,0], m=3, nums2 = [2,5,6], n=3  ->  [1,2,2,3,5,6]
 *
 *  ---------------------------------------------------------------------------
 *  SIMPLE: copy nums2 into the tail, then sort -> O((m+n) log(m+n)). Ignores that the input is sorted.
 *
 *  FRONT-TO-BACK MERGE PROBLEM: writing the smallest element at nums1[0]
 *  would OVERWRITE nums1's own unread values.
 *
 *  KEY INSIGHT: fill from the BACK. The empty space is at the END of nums1, so
 *  placing the LARGEST remaining element at the end never overwrites anything unread.
 *    i = m-1      (last real element of nums1)
 *    j = n-1      (last element of nums2)
 *    w = m+n-1    (where to write)
 *    put the bigger of nums1[i], nums2[j] at w, and move that pointer back.
 *    When nums2 is used up we're DONE: nums1's remaining elements are already in place.
 *
 *  DRY RUN nums1=[1,2,3,_,_,_], nums2=[2,5,6]
 *    3 vs 6 -> write 6 at 5   [1,2,3,_,_,6]
 *    3 vs 5 -> write 5 at 4   [1,2,3,_,5,6]
 *    3 vs 2 -> write 3 at 3   [1,2,3,3,5,6]
 *    2 vs 2 -> write 2 (nums2) at 2   [1,2,2,3,5,6]    (ties: either is fine)
 *    j < 0 -> stop. The 1,2 at the front are already correct.
 *
 *  COMPLEXITY   Time O(m + n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E08_MergeSortedArray {

    static void merge(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1, j = n - 1, w = m + n - 1;
        while (j >= 0) {                                   // only nums2 needs to be fully placed
            if (i >= 0 && nums1[i] > nums2[j]) {
                nums1[w--] = nums1[i--];
            } else {
                nums1[w--] = nums2[j--];
            }
        }
    }

    static int[] run(int[] nums1, int m, int[] nums2, int n) {
        merge(nums1, m, nums2, n);
        return nums1;
    }

    public static void main(String[] args) {
        check("[1,2,3,0,0,0] + [2,5,6]", new int[]{1, 2, 2, 3, 5, 6}, run(new int[]{1, 2, 3, 0, 0, 0}, 3, new int[]{2, 5, 6}, 3));
        check("[1] + []", new int[]{1}, run(new int[]{1}, 1, new int[]{}, 0));
        check("[0] + [1] (m=0)", new int[]{1}, run(new int[]{0}, 0, new int[]{1}, 1));
        check("[4,5,6,0,0,0] + [1,2,3]", new int[]{1, 2, 3, 4, 5, 6}, run(new int[]{4, 5, 6, 0, 0, 0}, 3, new int[]{1, 2, 3}, 3));
        check("[-1,0,0] + [-3] ", new int[]{-3, -1, 0}, run(new int[]{-1, 0, 0}, 2, new int[]{-3}, 1));
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
