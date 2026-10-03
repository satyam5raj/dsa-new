/*
 * ============================================================================
 *  EASY 12 : PLUS ONE                        LeetCode #66
 *  https://leetcode.com/problems/plus-one/
 *  Pattern: SIMULATION (carry from the right, like adding by hand)
 * ============================================================================
 *
 *  PROBLEM
 *    A large integer is stored as an array of digits (most significant first).
 *    Add one and return the new digits.
 *      [1,2,3] -> [1,2,4]       [4,3,2,9] -> [4,3,3,0]       [9,9] -> [1,0,0]
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT CONVERT TO A NUMBER? The array can have 100 digits: that overflows
 *  long (max ~19 digits). Work digit by digit, like on paper.
 *
 *  LOGIC (from the last digit backwards)
 *    - digit < 9  -> just increment it and RETURN (no carry, we're done)
 *    - digit == 9 -> it becomes 0, the carry moves to the next digit on the left
 *    - if we fall off the front, every digit was 9 (999 -> 1000):
 *      return a NEW array of length n+1 = [1, 0, 0, ..., 0]
 *
 *  DRY RUN [4,3,2,9]
 *    i=3: 9 -> 0, carry       [4,3,2,0]
 *    i=2: 2 < 9 -> 3, return  [4,3,3,0]
 *
 *  COMPLEXITY   Time O(n) worst case (all 9s), usually O(1)   Space O(1), or O(n) for the all-9s case
 * ============================================================================
 */

import java.util.*;

public class E12_PlusOne {

    static int[] plusOne(int[] digits) {
        for (int i = digits.length - 1; i >= 0; i--) {
            if (digits[i] < 9) {
                digits[i]++;
                return digits;                  // no carry left -> done early
            }
            digits[i] = 0;                      // 9 + 1 = 10 -> write 0, carry 1 to the left
        }
        int[] result = new int[digits.length + 1];  // all digits were 9
        result[0] = 1;                               // Java arrays start as zeros: [1,0,0,...]
        return result;
    }

    public static void main(String[] args) {
        check("[1,2,3]", new int[]{1, 2, 4}, plusOne(new int[]{1, 2, 3}));
        check("[4,3,2,9] (one carry)", new int[]{4, 3, 3, 0}, plusOne(new int[]{4, 3, 2, 9}));
        check("[9,9] (grows)", new int[]{1, 0, 0}, plusOne(new int[]{9, 9}));
        check("[0]", new int[]{1}, plusOne(new int[]{0}));
        check("[1,9,9] (two carries)", new int[]{2, 0, 0}, plusOne(new int[]{1, 9, 9}));
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
