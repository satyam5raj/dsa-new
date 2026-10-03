/*
 * ============================================================================
 *  MEDIUM 02 : ROTATE ARRAY                  LeetCode #189
 *  https://leetcode.com/problems/rotate-array/
 *  Pattern: REVERSAL TRICK (in-place)
 * ============================================================================
 *
 *  PROBLEM
 *    Rotate the array to the RIGHT by k steps, in-place.
 *      [1,2,3,4,5,6,7], k=3 -> [5,6,7,1,2,3,4]
 *      [-1,-100,3,99],  k=2 -> [3,99,-1,-100]
 *
 *  ---------------------------------------------------------------------------
 *  FIRST: k can be bigger than n. Rotating by n changes nothing, so k = k % n.
 *
 *  APPROACH 1: rotate by one, k times                  O(n * k)  -> too slow for big k
 *  APPROACH 2: EXTRA ARRAY  result[(i + k) % n] = nums[i]   O(n) time, O(n) space
 *    (element at i moves to i+k, wrapping around)
 *
 *  APPROACH 3: TRIPLE REVERSE                          O(n) time, O(1) space
 *    Right-rotating by k = the last k elements move to the FRONT, in order.
 *      1. reverse ALL            [7,6,5,4,3,2,1]   last k are now first, but backwards
 *      2. reverse the first k    [5,6,7,4,3,2,1]   fix their order
 *      3. reverse the rest       [5,6,7,1,2,3,4]   fix the others' order
 *    WHY IT WORKS: reversing the whole array puts each block at its target
 *    position (reversed). Reversing each block restores its internal order.
 *
 *  LEFT rotation by k? Same trick, but reverse the first k, then the rest, then all.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M02_RotateArray {

    static void rotate(int[] nums, int k) {
        int n = nums.length;
        k %= n;
        if (k == 0) return;
        reverse(nums, 0, n - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, n - 1);
    }

    static void reverse(int[] a, int i, int j) {
        while (i < j) {
            int t = a[i]; a[i] = a[j]; a[j] = t;
            i++; j--;
        }
    }

    // Approach 2 for comparison
    static int[] rotateWithExtraArray(int[] nums, int k) {
        int n = nums.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[(i + k) % n] = nums[i];
        return out;
    }

    static int[] run(int[] nums, int k) {
        rotate(nums, k);
        return nums;
    }

    public static void main(String[] args) {
        check("[1..7] k=3", new int[]{5, 6, 7, 1, 2, 3, 4}, run(new int[]{1, 2, 3, 4, 5, 6, 7}, 3));
        check("[-1,-100,3,99] k=2", new int[]{3, 99, -1, -100}, run(new int[]{-1, -100, 3, 99}, 2));
        check("k > n (k=10, n=3)", new int[]{3, 1, 2}, run(new int[]{1, 2, 3}, 10));
        check("k = n (no change)", new int[]{1, 2, 3}, run(new int[]{1, 2, 3}, 3));
        check("extra-array approach agrees", new int[]{5, 6, 7, 1, 2, 3, 4}, rotateWithExtraArray(new int[]{1, 2, 3, 4, 5, 6, 7}, 3));
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
