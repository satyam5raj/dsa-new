/*
 * ============================================================================
 *  EASY 22 : MAX CONSECUTIVE ONES            LeetCode #485
 *  https://leetcode.com/problems/max-consecutive-ones/
 *  Pattern: RUNNING COUNTER (reset on a break)
 * ============================================================================
 *
 *  PROBLEM
 *    In a binary array, return the length of the longest run of consecutive 1s.
 *      [1,1,0,1,1,1] -> 3
 *
 *  ---------------------------------------------------------------------------
 *  LOGIC
 *    current = length of the run ending at this position
 *      x == 1 -> current++, best = max(best, current)
 *      x == 0 -> current = 0   (the run is broken)
 *
 *  DRY RUN [1,1,0,1,1,1]
 *    1 -> cur 1 best 1
 *    1 -> cur 2 best 2
 *    0 -> cur 0
 *    1 -> cur 1
 *    1 -> cur 2
 *    1 -> cur 3 best 3
 *
 *  NEXT LEVEL: "Max Consecutive Ones III" (flip at most k zeros) needs a
 *  SLIDING WINDOW, see the Medium folder.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E22_MaxConsecutiveOnes {

    static int findMaxConsecutiveOnes(int[] nums) {
        int best = 0, current = 0;
        for (int x : nums) {
            if (x == 1) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("[1,1,0,1,1,1]", 3, findMaxConsecutiveOnes(new int[]{1, 1, 0, 1, 1, 1}));
        check("[1,0,1,1,0,1]", 2, findMaxConsecutiveOnes(new int[]{1, 0, 1, 1, 0, 1}));
        check("[0,0] (no ones)", 0, findMaxConsecutiveOnes(new int[]{0, 0}));
        check("[1,1,1] (all ones)", 3, findMaxConsecutiveOnes(new int[]{1, 1, 1}));
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
