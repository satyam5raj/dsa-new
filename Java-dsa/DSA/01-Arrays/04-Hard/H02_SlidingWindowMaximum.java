/*
 * ============================================================================
 *  HARD 02 : SLIDING WINDOW MAXIMUM          LeetCode #239
 *  https://leetcode.com/problems/sliding-window-maximum/
 *  Pattern: MONOTONIC DEQUE (decreasing)
 * ============================================================================
 *
 *  PROBLEM
 *    A window of size k slides from left to right. Return the MAX of each window.
 *      nums = [1,3,-1,-3,5,3,6,7], k = 3  ->  [3,3,5,5,6,7]
 *        [1 3 -1] -3 5 3 6 7    max 3
 *        1 [3 -1 -3] 5 3 6 7    max 3
 *        1 3 [-1 -3 5] 3 6 7    max 5
 *        ...
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: scan each window -> O(n * k). With n = 10^5, k = 5*10^4: too slow.
 *  A running SUM was easy to slide (add one, subtract one). A running MAX is
 *  NOT: when the maximum leaves the window, what's the next maximum?
 *
 *  OPTION: a max-heap with lazy deletion -> O(n log n). Good, but we can do O(n).
 *
 *  MONOTONIC DEQUE (stores INDICES, their values DECREASING from front to back)
 *    The front is ALWAYS the index of the current window's maximum.
 *    For each new index i:
 *      1. pop from the FRONT if that index has left the window (index <= i - k)
 *      2. pop from the BACK while nums[back] <= nums[i]
 *         WHY? Those elements are smaller AND older than nums[i]. While nums[i]
 *         is in the window, they can NEVER be the maximum, and they'll leave
 *         before it does. They're useless forever.
 *      3. push i at the back
 *      4. once i >= k-1 (the first full window), record nums[front]
 *
 *  DRY RUN nums=[1,3,-1,-3,5,3,6,7], k=3   (deque shows VALUES for readability)
 *    i=0  1         [1]
 *    i=1  3         pop 1 (<=3)             [3]
 *    i=2  -1                                [3,-1]        -> max 3
 *    i=3  -3                                [3,-1,-3]     -> max 3
 *    i=4  5         front 3 (idx 1) expired; pop -3,-1   [5]   -> max 5
 *    i=5  3                                 [5,3]         -> max 5
 *    i=6  6         pop 3, 5                [6]           -> max 6
 *    i=7  7         pop 6                   [7]           -> max 7
 *
 *  WHY O(n)? Each index is pushed once and popped at most once. The inner while
 *  loops are "paid for" by earlier pushes (amortized analysis).
 *  COMPLEXITY   Time O(n)   Space O(k)
 *  USED ALSO IN: H04 (shortest subarray with sum >= k), "max of all subarrays of size k",
 *  some DP optimizations ("sliding window maximum DP").
 * ============================================================================
 */

import java.util.*;

public class H02_SlidingWindowMaximum {

    static int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> dq = new ArrayDeque<>();              // indices, values decreasing

        for (int i = 0; i < n; i++) {
            if (!dq.isEmpty() && dq.peekFirst() <= i - k) {
                dq.pollFirst();                              // 1. out of the window
            }
            while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) {
                dq.pollLast();                               // 2. useless forever
            }
            dq.offerLast(i);                                 // 3.
            if (i >= k - 1) {
                result[i - k + 1] = nums[dq.peekFirst()];    // 4. the front holds the max
            }
        }
        return result;
    }

    static int[] maxSlidingWindowBrute(int[] nums, int k) {
        int[] r = new int[nums.length - k + 1];
        for (int i = 0; i + k <= nums.length; i++) {
            int m = Integer.MIN_VALUE;
            for (int j = i; j < i + k; j++) m = Math.max(m, nums[j]);
            r[i] = m;
        }
        return r;
    }

    public static void main(String[] args) {
        check("[1,3,-1,-3,5,3,6,7] k=3", new int[]{3, 3, 5, 5, 6, 7}, maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3));
        check("[1] k=1", new int[]{1}, maxSlidingWindow(new int[]{1}, 1));
        check("[9,8,7,6] k=2 (decreasing)", new int[]{9, 8, 7}, maxSlidingWindow(new int[]{9, 8, 7, 6}, 2));
        check("[1,2,3,4] k=4 (one window)", new int[]{4}, maxSlidingWindow(new int[]{1, 2, 3, 4}, 4));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(25);
            int[] a = rnd.ints(n, -10, 10).toArray();
            int k = 1 + rnd.nextInt(n);
            if (!Arrays.equals(maxSlidingWindow(a, k), maxSlidingWindowBrute(a, k))) ok = false;
        }
        check("300 random arrays vs brute force", true, ok);
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
