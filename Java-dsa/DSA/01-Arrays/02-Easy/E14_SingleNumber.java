/*
 * ============================================================================
 *  EASY 14 : SINGLE NUMBER                   LeetCode #136
 *  https://leetcode.com/problems/single-number/
 *  Pattern: BIT MANIPULATION (XOR cancels pairs)
 * ============================================================================
 *
 *  PROBLEM
 *    Every element appears TWICE except one. Find that one.
 *    Required: O(n) time and O(1) extra space.
 *      [2,2,1] -> 1          [4,1,2,1,2] -> 4
 *
 *  ---------------------------------------------------------------------------
 *  HASHSET idea: add if absent, remove if present; the leftover is the answer.
 *  O(n) time but O(n) space -> fails the O(1) space requirement.
 *
 *  XOR IDEA  (^ = exclusive OR, bit by bit: 1 if the bits DIFFER)
 *    x ^ x = 0         a number cancels itself
 *    x ^ 0 = x         0 changes nothing
 *    XOR is commutative and associative: the order doesn't matter
 *  So XOR-ing everything: all pairs cancel to 0 and only the single number remains.
 *
 *  DRY RUN [4,1,2,1,2]
 *    0 ^ 4 = 4
 *    4 ^ 1 = 5
 *    5 ^ 2 = 7
 *    7 ^ 1 = 6
 *    6 ^ 2 = 4   -> 4   (equivalently: 4 ^ (1^1) ^ (2^2) = 4 ^ 0 ^ 0)
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E14_SingleNumber {

    static int singleNumber(int[] nums) {
        int result = 0;
        for (int x : nums) result ^= x;
        return result;
    }

    public static void main(String[] args) {
        check("[2,2,1]", 1, singleNumber(new int[]{2, 2, 1}));
        check("[4,1,2,1,2]", 4, singleNumber(new int[]{4, 1, 2, 1, 2}));
        check("[1] (single)", 1, singleNumber(new int[]{1}));
        check("[-3,7,7] (negative)", -3, singleNumber(new int[]{-3, 7, 7}));
        check("[0,5,5]", 0, singleNumber(new int[]{0, 5, 5}));
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
