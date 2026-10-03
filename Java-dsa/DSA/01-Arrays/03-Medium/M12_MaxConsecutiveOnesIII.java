/*
 * ============================================================================
 *  MEDIUM 12 : MAX CONSECUTIVE ONES III      LeetCode #1004
 *  https://leetcode.com/problems/max-consecutive-ones-iii/
 *  Pattern: SLIDING WINDOW (longest window with at most k "bad" elements)
 * ============================================================================
 *
 *  PROBLEM
 *    Binary array. You may flip at most k zeros to ones. Return the longest run
 *    of 1s you can get.
 *      [1,1,1,0,0,0,1,1,1,1,0], k=2 -> 6
 *
 *  ---------------------------------------------------------------------------
 *  REFRAME (the key step!): "flip at most k zeros" ==
 *     "find the LONGEST window containing AT MOST k zeros"
 *  (Any such window can become all 1s by flipping its zeros.)
 *
 *  TEMPLATE (maximum-length version: shrink while INVALID)
 *    for right:
 *      if nums[right] == 0: zeros++               // expand
 *      while zeros > k:                           // invalid -> shrink
 *        if nums[left] == 0: zeros--
 *        left++
 *      best = max(best, right - left + 1)         // the window is valid here
 *
 *  Compare with M11: there we shrink while VALID (looking for the MINIMUM);
 *  here we shrink while INVALID (looking for the MAXIMUM). Know both forms.
 *
 *  DRY RUN [1,1,1,0,0,0,1,1,1,1,0], k=2 (key moments)
 *    r=4: window [0..4] has 2 zeros -> length 5
 *    r=5: 3 zeros -> shrink until left=4 (dropping the zero at 3) -> [4..5]
 *    r=9: window [4..9] = 0,0,1,1,1,1 -> 2 zeros -> length 6  <- best
 *    r=10: 3 zeros -> shrink to left=5 -> length 6
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  SAME SHAPE: "longest substring with at most k distinct characters",
 *              "longest repeating character replacement", Fruit Into Baskets (M17).
 * ============================================================================
 */

import java.util.*;

public class M12_MaxConsecutiveOnesIII {

    static int longestOnes(int[] nums, int k) {
        int left = 0, zeros = 0, best = 0;
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) zeros++;
            while (zeros > k) {
                if (nums[left] == 0) zeros--;
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[1,1,1,0,0,0,1,1,1,1,0] k=2", 6, longestOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}, 2));
        check("[0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1] k=3", 10,
                longestOnes(new int[]{0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1}, 3));
        check("k=0 (no flips)", 2, longestOnes(new int[]{1, 1, 0, 1}, 0));
        check("all zeros, k=2", 2, longestOnes(new int[]{0, 0, 0}, 2));
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
