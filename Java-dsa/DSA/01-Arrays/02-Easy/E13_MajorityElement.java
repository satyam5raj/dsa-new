/*
 * ============================================================================
 *  EASY 13 : MAJORITY ELEMENT                LeetCode #169
 *  https://leetcode.com/problems/majority-element/
 *  Pattern: BOYER-MOORE VOTING (a famous O(1)-space trick)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the element that appears MORE THAN n/2 times (it's guaranteed to exist).
 *      [3,2,3] -> 3            [2,2,1,1,1,2,2] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: COUNT MAP - count each value, return the one with count > n/2
 *              O(n) time, O(n) space
 *  APPROACH 2: SORT - the majority element MUST sit at index n/2
 *              (it fills more than half of the array, so it covers the middle)
 *              O(n log n)
 *
 *  APPROACH 3: BOYER-MOORE VOTING  (optimal: O(n) time, O(1) space)
 *    Think of it as a battle: each majority element "cancels" one different element.
 *    Since the majority has MORE than half, it can't be fully cancelled.
 *      candidate, count = 0
 *      for x:
 *        if count == 0: candidate = x      (start a new "group")
 *        count += (x == candidate) ? +1 : -1
 *      the surviving candidate is the majority
 *
 *  DRY RUN [2,2,1,1,1,2,2]
 *    x  candidate  count
 *    2     2         1
 *    2     2         2
 *    1     2         1
 *    1     2         0
 *    1     1         1     <- count was 0, new candidate 1
 *    2     1         0
 *    2     2         1     <- new candidate 2  -> answer 2
 *
 *  CAUTION: if a majority is NOT guaranteed, do a second pass to verify the
 *  candidate's real count is > n/2.
 *  Follow-up (Majority Element II, > n/3): keep TWO candidates (see Medium).
 * ============================================================================
 */

import java.util.*;

public class E13_MajorityElement {

    static int majorityElementMap(int[] nums) {
        Map<Integer, Integer> count = new HashMap<>();
        for (int x : nums) {
            if (count.merge(x, 1, Integer::sum) > nums.length / 2) return x;
        }
        throw new IllegalArgumentException("no majority");
    }

    static int majorityElement(int[] nums) {
        int candidate = 0, count = 0;
        for (int x : nums) {
            if (count == 0) candidate = x;
            count += (x == candidate) ? 1 : -1;
        }
        return candidate;
    }

    public static void main(String[] args) {
        check("[3,2,3]", 3, majorityElement(new int[]{3, 2, 3}));
        check("[2,2,1,1,1,2,2]", 2, majorityElement(new int[]{2, 2, 1, 1, 1, 2, 2}));
        check("[1] (single)", 1, majorityElement(new int[]{1}));
        check("[6,5,5] ", 5, majorityElement(new int[]{6, 5, 5}));
        check("map approach agrees", 2, majorityElementMap(new int[]{2, 2, 1, 1, 1, 2, 2}));
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
