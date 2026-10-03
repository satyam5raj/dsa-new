/*
 * ============================================================================
 *  EASY 02 : CONTAINS DUPLICATE              LeetCode #217
 *  https://leetcode.com/problems/contains-duplicate/
 *  Pattern: HASHING (seen-before set)
 * ============================================================================
 *
 *  PROBLEM
 *    Return true if any value appears AT LEAST TWICE in the array, false if
 *    every element is distinct.
 *      [1,2,3,1] -> true        [1,2,3,4] -> false
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: BRUTE FORCE - compare every pair          O(n^2) time, O(1) space
 *
 *  APPROACH 2: SORT, then compare NEIGHBOURS             O(n log n) time, O(1)* space
 *    After sorting, equal values sit next to each other: [1,1,2,3].
 *    (*O(log n) for the sort's recursion; also MODIFIES the input.)
 *
 *  APPROACH 3: HASHSET  (optimal time)                   O(n) time, O(n) space
 *    Walk once; for each x ask "have I seen x?". set.add(x) returns FALSE if x
 *    was already present: that's the duplicate.
 *
 *  DRY RUN [1,2,3,1]
 *    add 1 -> true  {1}
 *    add 2 -> true  {1,2}
 *    add 3 -> true  {1,2,3}
 *    add 1 -> FALSE -> duplicate found -> return true
 *
 *  WHICH ONE IN AN INTERVIEW? Mention all three. HashSet is fastest. Sorting
 *  wins if memory is tight. This "time vs space" discussion is what they want.
 * ============================================================================
 */

import java.util.*;

public class E02_ContainsDuplicate {

    static boolean containsDuplicateSort(int[] nums) {
        int[] a = nums.clone();                 // don't destroy the caller's array
        Arrays.sort(a);
        for (int i = 1; i < a.length; i++) {
            if (a[i] == a[i - 1]) return true;  // equal neighbours
        }
        return false;
    }

    static boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int x : nums) {
            if (!seen.add(x)) return true;      // add() == false -> already there
        }
        return false;
    }

    public static void main(String[] args) {
        check("[1,2,3,1]", true, containsDuplicate(new int[]{1, 2, 3, 1}));
        check("[1,2,3,4]", false, containsDuplicate(new int[]{1, 2, 3, 4}));
        check("[1,1,1,3,3,4,3,2,4,2]", true, containsDuplicate(new int[]{1, 1, 1, 3, 3, 4, 3, 2, 4, 2}));
        check("[] (empty)", false, containsDuplicate(new int[]{}));
        check("[7] (single)", false, containsDuplicate(new int[]{7}));
        check("sorting approach agrees", true, containsDuplicateSort(new int[]{1, 2, 3, 1}));
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
