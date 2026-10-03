/*
 * ============================================================================
 *  EASY 17 : THIRD MAXIMUM NUMBER            LeetCode #414
 *  https://leetcode.com/problems/third-maximum-number/
 *  Pattern: TRACK TOP-K IN VARIABLES (one pass)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the third DISTINCT maximum. If it doesn't exist, return the maximum.
 *      [3,2,1]   -> 1
 *      [1,2]     -> 2       (no third distinct value -> the max)
 *      [2,2,3,1] -> 1       (distinct values: 3, 2, 1)
 *
 *  ---------------------------------------------------------------------------
 *  SIMPLE: sort descending and walk, counting distinct values -> O(n log n).
 *  OR: a TreeSet keeping only the 3 largest -> O(n log 3) = O(n).
 *
 *  ONE PASS WITH 3 VARIABLES  first > second > third
 *    for each x:
 *      skip x if it equals first, second or third   (DISTINCT only)
 *      x > first  -> shift down: third = second, second = first, first = x
 *      x > second -> third = second, second = x
 *      x > third  -> third = x
 *
 *  TRAP: values can be Integer.MIN_VALUE itself, so we can't use MIN_VALUE as
 *  "empty". Use 'Long' (or nullable Integer) with null meaning "not set yet".
 *
 *  DRY RUN [2,2,3,1]
 *    2 -> first=2
 *    2 -> duplicate, skip
 *    3 -> first=3, second=2
 *    1 -> third=1          -> third exists -> answer 1
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E17_ThirdMaximumNumber {

    static int thirdMax(int[] nums) {
        Long first = null, second = null, third = null;
        for (int v : nums) {
            long x = v;
            if ((first != null && x == first) || (second != null && x == second) || (third != null && x == third)) {
                continue;                                  // only DISTINCT values count
            }
            if (first == null || x > first) {
                third = second;
                second = first;
                first = x;
            } else if (second == null || x > second) {
                third = second;
                second = x;
            } else if (third == null || x > third) {
                third = x;
            }
        }
        return (int) (third != null ? (long) third : (long) first);
    }

    public static void main(String[] args) {
        check("[3,2,1]", 1, thirdMax(new int[]{3, 2, 1}));
        check("[1,2] (no third)", 2, thirdMax(new int[]{1, 2}));
        check("[2,2,3,1] (duplicates)", 1, thirdMax(new int[]{2, 2, 3, 1}));
        check("[1,2,MIN_VALUE] (MIN is valid)", Integer.MIN_VALUE, thirdMax(new int[]{1, 2, Integer.MIN_VALUE}));
        check("[5,5,5]", 5, thirdMax(new int[]{5, 5, 5}));
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
