/*
 * ============================================================================
 *  EASY 26 : RELATIVE SORT ARRAY             LeetCode #1122
 *  https://leetcode.com/problems/relative-sort-array/
 *  Pattern: COUNTING SORT (values in a small range)
 * ============================================================================
 *
 *  PROBLEM
 *    Sort arr1 so that elements follow the ORDER they appear in arr2. Elements
 *    not in arr2 go at the end in ascending order. All of arr2's elements are
 *    distinct and present in arr1. Values are in 0..1000.
 *      arr1 = [2,3,1,3,2,4,6,7,9,2,19], arr2 = [2,1,4,3,9,6]
 *      ->     [2,2,2,1,4,3,3,9,6,7,19]
 *
 *  ---------------------------------------------------------------------------
 *  KEY CLUE: values are small (0..1000) -> COUNTING SORT beats comparison sorting.
 *    1. count[v] = how many times v appears in arr1
 *    2. for each v in arr2 (in order): write v count[v] times, set count[v] = 0
 *    3. for v = 0..1000 (ascending): write the leftovers count[v] times
 *
 *  DRY RUN (counts of the example)
 *    count: 1->1, 2->3, 3->2, 4->1, 6->1, 7->1, 9->1, 19->1
 *    arr2 order: 2,2,2 | 1 | 4 | 3,3 | 9 | 6
 *    leftovers ascending: 7, 19
 *
 *  COUNTING SORT IN GENERAL: O(n + R) time and O(R) space, where R is the value
 *  range. Great when R is small, impossible when values go up to 10^9.
 *
 *  COMPLEXITY   Time O(n + m + R)   Space O(R), R = 1001
 * ============================================================================
 */

import java.util.*;

public class E26_RelativeSortArray {

    static int[] relativeSortArray(int[] arr1, int[] arr2) {
        int[] count = new int[1001];
        for (int x : arr1) count[x]++;

        int[] result = new int[arr1.length];
        int w = 0;
        for (int v : arr2) {                    // 1) in arr2's order
            while (count[v]-- > 0) result[w++] = v;
        }
        for (int v = 0; v < count.length; v++) { // 2) leftovers in ascending order
            while (count[v]-- > 0) result[w++] = v;
        }
        return result;
    }

    public static void main(String[] args) {
        check("example 1", new int[]{2, 2, 2, 1, 4, 3, 3, 9, 6, 7, 19},
                relativeSortArray(new int[]{2, 3, 1, 3, 2, 4, 6, 7, 9, 2, 19}, new int[]{2, 1, 4, 3, 9, 6}));
        check("example 2", new int[]{22, 28, 8, 6, 17, 44},
                relativeSortArray(new int[]{28, 6, 22, 8, 44, 17}, new int[]{22, 28, 8, 6}));
        check("arr2 empty -> plain sort", new int[]{1, 2, 3}, relativeSortArray(new int[]{3, 1, 2}, new int[]{}));
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
