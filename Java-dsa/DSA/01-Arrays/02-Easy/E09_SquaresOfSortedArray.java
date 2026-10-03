/*
 * ============================================================================
 *  EASY 09 : SQUARES OF A SORTED ARRAY       LeetCode #977
 *  https://leetcode.com/problems/squares-of-a-sorted-array/
 *  Pattern: TWO POINTERS (opposite ends), fill the result from the back
 * ============================================================================
 *
 *  PROBLEM
 *    nums is sorted (may contain negatives). Return the squares, also sorted.
 *      [-4,-1,0,3,10] -> [0,1,9,16,100]
 *
 *  ---------------------------------------------------------------------------
 *  SIMPLE: square everything, then sort -> O(n log n).
 *
 *  KEY INSIGHT: negatives break the order ((-4)^2 = 16 > 3^2 = 9), BUT the
 *  LARGEST square is always at one of the two ENDS (biggest absolute value is
 *  either the most negative or the most positive number).
 *  So compare the two ends, put the bigger square at the END of the result,
 *  and move that pointer inward. One pass.
 *
 *  DRY RUN [-4,-1,0,3,10]   result filled from index 4 down to 0
 *    left=-4 (16) right=10 (100) -> 100 at [4], right--
 *    left=-4 (16) right=3  (9)   ->  16 at [3], left++
 *    left=-1 (1)  right=3  (9)   ->   9 at [2], right--
 *    left=-1 (1)  right=0  (0)   ->   1 at [1], left++
 *    left=0 = right              ->   0 at [0]
 *    -> [0,1,9,16,100]
 *
 *  COMPLEXITY   Time O(n)   Space O(n) for the output (O(1) extra beyond it)
 * ============================================================================
 */

import java.util.*;

public class E09_SquaresOfSortedArray {

    static int[] sortedSquares(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        int left = 0, right = n - 1;
        for (int w = n - 1; w >= 0; w--) {                 // biggest square goes last
            int ls = nums[left] * nums[left];
            int rs = nums[right] * nums[right];
            if (ls > rs) {
                result[w] = ls;
                left++;
            } else {
                result[w] = rs;
                right--;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        check("[-4,-1,0,3,10]", new int[]{0, 1, 9, 16, 100}, sortedSquares(new int[]{-4, -1, 0, 3, 10}));
        check("[-7,-3,2,3,11]", new int[]{4, 9, 9, 49, 121}, sortedSquares(new int[]{-7, -3, 2, 3, 11}));
        check("[-5,-3,-1] (all negative)", new int[]{1, 9, 25}, sortedSquares(new int[]{-5, -3, -1}));
        check("[1,2,3] (all positive)", new int[]{1, 4, 9}, sortedSquares(new int[]{1, 2, 3}));
        check("[-2] (single)", new int[]{4}, sortedSquares(new int[]{-2}));
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
