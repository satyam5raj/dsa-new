/*
 * ============================================================================
 *  EASY 10 : MISSING NUMBER                  LeetCode #268
 *  https://leetcode.com/problems/missing-number/
 *  Pattern: MATH (sum formula) or BIT MANIPULATION (XOR)
 * ============================================================================
 *
 *  PROBLEM
 *    nums has n DISTINCT numbers from the range [0, n]. Exactly one number of
 *    that range is missing. Find it.
 *      [3,0,1]             -> 2      (range 0..3)
 *      [9,6,4,2,3,5,7,0,1] -> 8      (range 0..9)
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: HASHSET - put all in a set, check 0..n         O(n) time, O(n) space
 *  APPROACH 2: SORT    - first index where nums[i] != i       O(n log n)
 *
 *  APPROACH 3: SUM FORMULA                                    O(n) time, O(1) space
 *    sum of 0..n = n(n+1)/2. Subtract the actual sum -> the missing number.
 *    [3,0,1]: expected 0+1+2+3 = 6, actual 4 -> missing 2.
 *    (Overflow note: n(n+1)/2 can overflow int for huge n. Use long, or XOR.)
 *
 *  APPROACH 4: XOR                                            O(n) time, O(1) space, no overflow
 *    XOR facts:  a ^ a = 0,   a ^ 0 = a,   order doesn't matter.
 *    XOR all INDICES 0..n together with all VALUES. Every present number appears
 *    twice (once as an index, once as a value) and cancels to 0. Only the missing one survives.
 *    [3,0,1]: (0^1^2^3) ^ (3^0^1) = 2
 *
 *  COMPLEXITY (sum/XOR)   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E10_MissingNumber {

    static int missingNumberSum(int[] nums) {
        long n = nums.length;
        long expected = n * (n + 1) / 2;           // long: avoids overflow
        long actual = 0;
        for (int x : nums) actual += x;
        return (int) (expected - actual);
    }

    static int missingNumberXor(int[] nums) {
        int xor = nums.length;                     // start with n (the index that has no array slot)
        for (int i = 0; i < nums.length; i++) {
            xor ^= i ^ nums[i];                    // pairs cancel out
        }
        return xor;
    }

    public static void main(String[] args) {
        check("[3,0,1] (sum)", 2, missingNumberSum(new int[]{3, 0, 1}));
        check("[0,1] (missing n)", 2, missingNumberSum(new int[]{0, 1}));
        check("[9,6,4,2,3,5,7,0,1]", 8, missingNumberSum(new int[]{9, 6, 4, 2, 3, 5, 7, 0, 1}));
        check("[1] (missing 0)", 0, missingNumberSum(new int[]{1}));
        check("[3,0,1] (xor)", 2, missingNumberXor(new int[]{3, 0, 1}));
        check("[9,6,4,2,3,5,7,0,1] (xor)", 8, missingNumberXor(new int[]{9, 6, 4, 2, 3, 5, 7, 0, 1}));
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
