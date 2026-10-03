/*
 * ============================================================================
 *  HARD 04 : SHORTEST SUBARRAY WITH SUM AT LEAST K   LeetCode #862
 *  https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/
 *  Pattern: PREFIX SUMS + MONOTONIC DEQUE
 * ============================================================================
 *
 *  PROBLEM
 *    Return the length of the shortest non-empty subarray with sum >= k, or -1.
 *    Values CAN BE NEGATIVE.
 *      [1], k=1        -> 1
 *      [1,2], k=4      -> -1
 *      [2,-1,2], k=3   -> 3
 *      [84,-37,32,40,95], k=167 -> 3   ([32,40,95])
 *
 *  ---------------------------------------------------------------------------
 *  WHY M11's SLIDING WINDOW FAILS: with negative numbers, shrinking the window
 *  can INCREASE the sum (dropping a -37). The window's validity is no longer
 *  monotonic, so the shrink logic breaks.
 *
 *  REFRAME WITH PREFIX SUMS
 *    P[0] = 0, P[j] = sum of the first j elements.
 *    sum(i..j-1) = P[j] - P[i].  We want the minimum (j - i) with P[j] - P[i] >= k.
 *
 *  MONOTONIC DEQUE of candidate START indices i, with P INCREASING front -> back.
 *    For each j (as a possible END):
 *      1. while P[j] - P[front] >= k:
 *            record j - front, then pop the front.
 *         WHY POP? Any later j' > j would give a LONGER subarray with this same
 *         start, so this start can't do better than now.
 *      2. while P[back] >= P[j]: pop the back.
 *         WHY? j is a later start with a smaller-or-equal prefix: a BETTER start
 *         in every way (gives a bigger sum AND a shorter subarray). The old one is useless.
 *      3. push j.
 *
 *  DRY RUN [2,-1,2], k=3     P = [0,2,1,3]
 *    j=0 P=0  push                       dq=[0]
 *    j=1 P=2  2-0=2 <3; push             dq=[0,1]
 *    j=2 P=1  1-0 <3; pop back 1 (P=2 >= 1); push   dq=[0,2]
 *    j=3 P=3  3-P[0]=3 >= 3 -> length 3, pop 0;  3-P[2]=2 <3; push   dq=[2,3]
 *    answer 3
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 *  Use long for prefix sums (they can overflow int).
 * ============================================================================
 */

import java.util.*;

public class H04_ShortestSubarrayWithSumAtLeastK {

    static int shortestSubarray(int[] nums, int k) {
        int n = nums.length;
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];

        Deque<Integer> dq = new ArrayDeque<>();           // candidate starts, prefix increasing
        int best = Integer.MAX_VALUE;
        for (int j = 0; j <= n; j++) {
            while (!dq.isEmpty() && prefix[j] - prefix[dq.peekFirst()] >= k) {
                best = Math.min(best, j - dq.pollFirst());           // 1. found; this start is done
            }
            while (!dq.isEmpty() && prefix[dq.peekLast()] >= prefix[j]) {
                dq.pollLast();                                       // 2. j dominates it
            }
            dq.offerLast(j);                                         // 3.
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    static int shortestBrute(int[] a, int k) {
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) {
            long s = 0;
            for (int j = i; j < a.length; j++) {
                s += a[j];
                if (s >= k) { best = Math.min(best, j - i + 1); break; }
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    public static void main(String[] args) {
        check("[1] k=1", 1, shortestSubarray(new int[]{1}, 1));
        check("[1,2] k=4 (impossible)", -1, shortestSubarray(new int[]{1, 2}, 4));
        check("[2,-1,2] k=3 (negatives)", 3, shortestSubarray(new int[]{2, -1, 2}, 3));
        check("[84,-37,32,40,95] k=167", 3, shortestSubarray(new int[]{84, -37, 32, 40, 95}, 167));
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), -10, 15).toArray();
            int k = 1 + rnd.nextInt(30);
            if (shortestSubarray(a, k) != shortestBrute(a, k)) ok = false;
        }
        check("500 random arrays vs brute force", true, ok);
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
