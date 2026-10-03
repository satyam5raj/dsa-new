/*
 * ============================================================================
 *  MEDIUM 03 : TWO SUM II - INPUT ARRAY IS SORTED    LeetCode #167
 *  https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/
 *  Pattern: TWO POINTERS (opposite ends)
 * ============================================================================
 *
 *  PROBLEM
 *    numbers is sorted ascending. Return the 1-INDEXED positions [i, j] (i < j)
 *    of two numbers summing to target. Exactly one solution. Use O(1) extra space.
 *      [2,7,11,15], target 9 -> [1,2]
 *
 *  ---------------------------------------------------------------------------
 *  The HashMap solution from Two Sum works, but uses O(n) space. The SORTED
 *  order lets us do better.
 *
 *  TWO POINTERS
 *    left = 0 (smallest), right = n-1 (largest)
 *      sum == target -> found
 *      sum <  target -> we need a BIGGER sum   -> left++  (the only way to increase it)
 *      sum >  target -> we need a SMALLER sum  -> right-- (the only way to decrease it)
 *
 *  WHY CAN'T WE SKIP THE ANSWER? (the proof interviewers like)
 *    When sum < target we discard numbers[left]. Could numbers[left] pair with
 *    someone? Its biggest possible partner is numbers[right], and even that is
 *    too small. So numbers[left] pairs with nobody: it's safe to drop.
 *    Symmetric argument for right--.
 *
 *  DRY RUN [2,7,11,15], target 9
 *    2 + 15 = 17 > 9 -> right--
 *    2 + 11 = 13 > 9 -> right--
 *    2 + 7  =  9     -> [1, 2]  (1-indexed)
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  This exact loop is the inner engine of 3Sum (next-but-one problem).
 * ============================================================================
 */

import java.util.*;

public class M03_TwoSumII_SortedInput {

    static int[] twoSum(int[] numbers, int target) {
        int left = 0, right = numbers.length - 1;
        while (left < right) {
            int sum = numbers[left] + numbers[right];
            if (sum == target) return new int[]{left + 1, right + 1};   // 1-indexed
            if (sum < target) left++;
            else right--;
        }
        return new int[0];
    }

    public static void main(String[] args) {
        check("[2,7,11,15] t=9", new int[]{1, 2}, twoSum(new int[]{2, 7, 11, 15}, 9));
        check("[2,3,4] t=6", new int[]{1, 3}, twoSum(new int[]{2, 3, 4}, 6));
        check("[-1,0] t=-1 (negatives)", new int[]{1, 2}, twoSum(new int[]{-1, 0}, -1));
        check("[1,2,3,4,4,9,56,90] t=8", new int[]{4, 5}, twoSum(new int[]{1, 2, 3, 4, 4, 9, 56, 90}, 8));
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
