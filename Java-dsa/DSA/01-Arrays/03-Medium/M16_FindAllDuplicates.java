/*
 * ============================================================================
 *  MEDIUM 16 : FIND ALL DUPLICATES IN AN ARRAY   LeetCode #442
 *  https://leetcode.com/problems/find-all-duplicates-in-an-array/
 *  Pattern: INDEX MARKING (sign flip) - O(1) extra space
 * ============================================================================
 *
 *  PROBLEM
 *    n integers, each in [1, n]. Each appears ONCE or TWICE. Return the ones
 *    that appear twice. O(n) time, O(1) extra space.
 *      [4,3,2,7,8,2,3,1] -> [2,3]
 *
 *  ---------------------------------------------------------------------------
 *  Same family as Easy E16 (Disappeared Numbers): value v "lives" at index v-1.
 *    for each x:
 *      idx = |x| - 1
 *      if nums[idx] < 0 -> we've ALREADY flipped it: x is a DUPLICATE
 *      else             -> flip it negative (first visit)
 *  The first visit marks the slot; the second visit finds it already marked.
 *
 *  DRY RUN [4,3,2,7,8,2,3,1]
 *    4 -> idx 3 positive  -> flip   [4,3,2,-7,8,2,3,1]
 *    3 -> idx 2 positive  -> flip   [4,3,-2,-7,8,2,3,1]
 *    2 -> idx 1 positive  -> flip   [4,-3,-2,-7,8,2,3,1]
 *    7 -> idx 6 positive  -> flip   [4,-3,-2,-7,8,2,-3,1]
 *    8 -> idx 7 positive  -> flip   [4,-3,-2,-7,8,2,-3,-1]
 *    2 -> idx 1 NEGATIVE  -> duplicate 2
 *    3 -> idx 2 NEGATIVE  -> duplicate 3
 *    1 -> idx 0 positive  -> flip
 *
 *  ALTERNATIVE (also O(1) space): CYCLIC SORT - swap each value to its home
 *  index v-1, then any i with nums[i] != i+1 holds a duplicate. Good to know:
 *  cyclic sort solves "First Missing Positive" (a Hard classic).
 *
 *  COMPLEXITY   Time O(n)   Space O(1) extra
 * ============================================================================
 */

import java.util.*;

public class M16_FindAllDuplicates {

    static List<Integer> findDuplicates(int[] nums) {
        List<Integer> dups = new ArrayList<>();
        for (int x : nums) {
            int idx = Math.abs(x) - 1;
            if (nums[idx] < 0) dups.add(Math.abs(x));   // second visit
            else nums[idx] = -nums[idx];                // first visit: mark
        }
        return dups;
    }

    public static void main(String[] args) {
        check("[4,3,2,7,8,2,3,1]", "[2, 3]", findDuplicates(new int[]{4, 3, 2, 7, 8, 2, 3, 1}));
        check("[1,1,2]", "[1]", findDuplicates(new int[]{1, 1, 2}));
        check("[1] (none)", "[]", findDuplicates(new int[]{1}));
        check("[2,2,1,1]", "[2, 1]", findDuplicates(new int[]{2, 2, 1, 1}));
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
