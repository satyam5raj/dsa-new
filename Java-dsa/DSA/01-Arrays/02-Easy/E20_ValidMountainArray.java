/*
 * ============================================================================
 *  EASY 20 : VALID MOUNTAIN ARRAY            LeetCode #941
 *  https://leetcode.com/problems/valid-mountain-array/
 *  Pattern: WALK UP, THEN WALK DOWN (simulation with one pointer)
 * ============================================================================
 *
 *  PROBLEM
 *    A mountain: length >= 3, STRICTLY increases to a peak, then STRICTLY decreases.
 *    The peak can't be the first or the last element.
 *      [0,3,2,1] -> true     [3,5,5] -> false (flat)     [0,1,2,3] -> false (no way down)
 *
 *  ---------------------------------------------------------------------------
 *  LOGIC
 *    1. i = 0; climb while a[i] < a[i+1]          (walk up)
 *    2. the peak must not be at either end: i == 0 or i == n-1 -> false
 *    3. descend while a[i] > a[i+1]               (walk down)
 *    4. valid only if we reached the LAST index   (i == n-1)
 *  Plateaus (equal neighbours) stop both walks early, so they fail check 4.
 *
 *  DRY RUN [0,3,2,1]
 *    climb: 0<3 -> i=1; 3<2? no -> peak at 1 (not an end, OK)
 *    descend: 3>2 -> i=2; 2>1 -> i=3 = n-1 -> true
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E20_ValidMountainArray {

    static boolean validMountainArray(int[] arr) {
        int n = arr.length;
        if (n < 3) return false;
        int i = 0;
        while (i + 1 < n && arr[i] < arr[i + 1]) i++;    // walk up
        if (i == 0 || i == n - 1) return false;          // never went up, or never came down
        while (i + 1 < n && arr[i] > arr[i + 1]) i++;    // walk down
        return i == n - 1;
    }

    public static void main(String[] args) {
        check("[0,3,2,1]", true, validMountainArray(new int[]{0, 3, 2, 1}));
        check("[3,5,5] (plateau)", false, validMountainArray(new int[]{3, 5, 5}));
        check("[0,1,2,3] (only up)", false, validMountainArray(new int[]{0, 1, 2, 3}));
        check("[3,2,1] (only down)", false, validMountainArray(new int[]{3, 2, 1}));
        check("[2,1] (too short)", false, validMountainArray(new int[]{2, 1}));
        check("[1,3,2,2] (flat at the bottom)", false, validMountainArray(new int[]{1, 3, 2, 2}));
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
