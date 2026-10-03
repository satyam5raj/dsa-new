/*
 * ============================================================================
 *  MEDIUM 15 : MAJORITY ELEMENT II           LeetCode #229
 *  https://leetcode.com/problems/majority-element-ii/
 *  Pattern: EXTENDED BOYER-MOORE VOTING (two candidates)
 * ============================================================================
 *
 *  PROBLEM
 *    Return ALL elements appearing MORE than n/3 times. O(n) time, O(1) space.
 *      [3,2,3] -> [3]        [1,2] -> [1,2]        [1] -> [1]
 *
 *  ---------------------------------------------------------------------------
 *  HOW MANY ANSWERS CAN THERE BE? At most TWO. Three values each with more
 *  than n/3 occurrences would need more than n elements.
 *  -> track TWO candidates with two counters (Easy E13 used one for n/2).
 *
 *  VOTING RULES for each x
 *    x == cand1        -> count1++
 *    x == cand2        -> count2++
 *    count1 == 0       -> cand1 = x, count1 = 1
 *    count2 == 0       -> cand2 = x, count2 = 1
 *    otherwise         -> count1--, count2--      (x cancels one vote from EACH candidate)
 *  The ORDER of these checks matters: first test "is it one of my candidates?"
 *  before reusing an empty slot. Otherwise both slots can end up holding the same value.
 *
 *  SECOND PASS IS REQUIRED: the candidates are only POSSIBLE answers. [1,2,3]
 *  leaves candidates, but nothing appears > n/3. Count them again for real.
 *
 *  INTUITION: each "otherwise" step removes 3 DIFFERENT values at once. A value
 *  with > n/3 copies can't be fully cancelled by such triple-removals.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  GENERAL: elements > n/k -> keep k-1 candidates.
 * ============================================================================
 */

import java.util.*;

public class M15_MajorityElementII {

    static List<Integer> majorityElement(int[] nums) {
        int cand1 = 0, cand2 = 1, count1 = 0, count2 = 0;    // different initial candidates
        for (int x : nums) {
            if (x == cand1) count1++;
            else if (x == cand2) count2++;
            else if (count1 == 0) { cand1 = x; count1 = 1; }
            else if (count2 == 0) { cand2 = x; count2 = 1; }
            else { count1--; count2--; }
        }
        // verify the candidates
        count1 = 0;
        count2 = 0;
        for (int x : nums) {
            if (x == cand1) count1++;
            else if (x == cand2) count2++;
        }
        List<Integer> result = new ArrayList<>();
        if (count1 > nums.length / 3) result.add(cand1);
        if (count2 > nums.length / 3) result.add(cand2);
        Collections.sort(result);                             // only for predictable test output
        return result;
    }

    public static void main(String[] args) {
        check("[3,2,3]", "[3]", majorityElement(new int[]{3, 2, 3}));
        check("[1,2]", "[1, 2]", majorityElement(new int[]{1, 2}));
        check("[1]", "[1]", majorityElement(new int[]{1}));
        check("[1,2,3] (none)", "[]", majorityElement(new int[]{1, 2, 3}));
        check("[2,2,1,1,1,2,2]", "[1, 2]", majorityElement(new int[]{2, 2, 1, 1, 1, 2, 2}));
        check("[0,0,0] (zeros, initial cand1=0)", "[0]", majorityElement(new int[]{0, 0, 0}));
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
