/*
 * ============================================================================
 *  EASY 16 : FIND ALL NUMBERS DISAPPEARED IN AN ARRAY   LeetCode #448
 *  https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/
 *  Pattern: INDEX MARKING (use the array itself as a hash set)
 * ============================================================================
 *
 *  PROBLEM
 *    nums has n integers, each in [1, n]. Some appear twice, others not at all.
 *    Return all numbers in [1, n] that do NOT appear. Follow-up: O(1) extra space.
 *      [4,3,2,7,8,2,3,1] -> [5,6]
 *
 *  ---------------------------------------------------------------------------
 *  EASY WAY: a boolean[] seen of size n+1 -> O(n) space.
 *
 *  KEY INSIGHT: values are 1..n and the array has n slots (indices 0..n-1).
 *  So every value v has a "home" index v-1. Use the SIGN of nums[v-1] as a
 *  "seen" flag: make it negative when v is present.
 *    1. for each x: idx = |x| - 1; make nums[idx] negative (if not already)
 *       (|x| because x itself may already have been flipped negative)
 *    2. any index i still POSITIVE means the value i+1 never appeared
 *
 *  DRY RUN [4,3,2,7,8,2,3,1]
 *    x=4 -> mark idx 3    [4,3,2,-7,8,2,3,1]
 *    x=3 -> mark idx 2    [4,3,-2,-7,8,2,3,1]
 *    x=2 -> mark idx 1    [4,-3,-2,-7,8,2,3,1]
 *    x=7 -> mark idx 6    [4,-3,-2,-7,8,2,-3,1]
 *    x=8 -> mark idx 7    [4,-3,-2,-7,8,2,-3,-1]
 *    x=2 -> idx 1 already negative
 *    x=3 -> idx 2 already negative
 *    x=1 -> mark idx 0    [-4,-3,-2,-7,8,2,-3,-1]
 *    positive at idx 4 and 5 -> missing 5 and 6
 *
 *  COMPLEXITY   Time O(n)   Space O(1) extra (the output list doesn't count)
 *  NOTE: this modifies the input. Mention it, and restore the signs afterwards if needed.
 * ============================================================================
 */

import java.util.*;

public class E16_FindAllNumbersDisappeared {

    static List<Integer> findDisappearedNumbers(int[] nums) {
        for (int x : nums) {
            int idx = Math.abs(x) - 1;
            if (nums[idx] > 0) nums[idx] = -nums[idx];     // mark value idx+1 as seen
        }
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) missing.add(i + 1);
        }
        return missing;
    }

    public static void main(String[] args) {
        check("[4,3,2,7,8,2,3,1]", "[5, 6]", findDisappearedNumbers(new int[]{4, 3, 2, 7, 8, 2, 3, 1}));
        check("[1,1]", "[2]", findDisappearedNumbers(new int[]{1, 1}));
        check("[1,2,3] (none missing)", "[]", findDisappearedNumbers(new int[]{1, 2, 3}));
        check("[2,2,2] ", "[1, 3]", findDisappearedNumbers(new int[]{2, 2, 2}));
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
