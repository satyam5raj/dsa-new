/*
 * ============================================================================
 *  MEDIUM 07 : LONGEST CONTINUOUS SUBARRAY WITH ABSOLUTE DIFF <= LIMIT   LeetCode #1438
 *  https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/
 *  Pattern: SLIDING WINDOW + TWO MONOTONIC DEQUES (window max AND window min)
 * ============================================================================
 *
 *  PROBLEM
 *    Longest subarray in which |a - b| <= limit for EVERY pair of its elements.
 *      [8,2,4,7], limit 4         -> 2   ([2,4] or [4,7])
 *      [10,1,2,4,7,2], limit 5    -> 4   ([2,4,7,2])
 *      [4,2,2,2,4,4,2,2], limit 0 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  REFRAME: "every pair differs by at most limit"  <=>  max(window) - min(window) <= limit
 *  So: the longest window whose (max - min) <= limit, a variable sliding window
 *  (Arrays/M12 template). The hard part is knowing the window's max and min
 *  quickly while elements leave from the LEFT.
 *
 *  TWO MONOTONIC DEQUES (store values)
 *    maxDq: values DECREASING -> its front is the window max
 *    minDq: values INCREASING -> its front is the window min
 *    expand with x:  pop from maxDq's back while back < x;  push x
 *                    pop from minDq's back while back > x;  push x
 *    while (maxDq.front - minDq.front > limit):   shrink
 *        the leaving element nums[left]: if it equals a front, poll that front
 *        left++
 *    best = max(best, right - left + 1)
 *  (Keep EQUAL values in the deques (pop only strictly smaller/larger), so that
 *   when one copy leaves, its duplicates are still counted.)
 *
 *  ALTERNATIVE: a TreeMap<value, count> as a multiset gives max/min in O(log n) -> O(n log n).
 *
 *  DRY RUN [10,1,2,4,7,2], limit 5
 *    [10,1]: 10-1 = 9 > 5 -> drop 10            window [1]
 *    [1,2,4]: 4-1 = 3 ok                        length 3
 *    [1,2,4,7]: 7-1 = 6 > 5 -> drop 1           [2,4,7] length 3
 *    [2,4,7,2]: 7-2 = 5 ok                      length 4  <- best
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M07_LongestSubarrayAbsDiffLimit {

    static int longestSubarray(int[] nums, int limit) {
        Deque<Integer> maxDq = new ArrayDeque<>(), minDq = new ArrayDeque<>();
        int left = 0, best = 0;
        for (int right = 0; right < nums.length; right++) {
            int x = nums[right];
            while (!maxDq.isEmpty() && maxDq.peekLast() < x) maxDq.pollLast();
            maxDq.offerLast(x);
            while (!minDq.isEmpty() && minDq.peekLast() > x) minDq.pollLast();
            minDq.offerLast(x);

            while (maxDq.peekFirst() - minDq.peekFirst() > limit) {
                if (nums[left] == maxDq.peekFirst()) maxDq.pollFirst();
                if (nums[left] == minDq.peekFirst()) minDq.pollFirst();
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static int brute(int[] a, int limit) {
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            int mx = a[i], mn = a[i];
            for (int j = i; j < a.length; j++) {
                mx = Math.max(mx, a[j]);
                mn = Math.min(mn, a[j]);
                if (mx - mn <= limit) best = Math.max(best, j - i + 1);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("[8,2,4,7] limit 4", 2, longestSubarray(new int[]{8, 2, 4, 7}, 4));
        check("[10,1,2,4,7,2] limit 5", 4, longestSubarray(new int[]{10, 1, 2, 4, 7, 2}, 5));
        check("[4,2,2,2,4,4,2,2] limit 0", 3, longestSubarray(new int[]{4, 2, 2, 2, 4, 4, 2, 2}, 0));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), 0, 10).toArray();     // many duplicates on purpose
            int lim = rnd.nextInt(6);
            ok &= longestSubarray(a, lim) == brute(a, lim);
        }
        check("500 random arrays (duplicates) vs brute force", true, ok);
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
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
