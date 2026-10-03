/*
 * ============================================================================
 *  MEDIUM 15 : SUM OF SUBARRAY MINIMUMS  ⭐ bonus (contribution technique)   LeetCode #907
 *  https://leetcode.com/problems/sum-of-subarray-minimums/
 *  Pattern: MONOTONIC STACK -> "in how many subarrays is arr[i] the minimum?"
 * ============================================================================
 *
 *  PROBLEM
 *    Sum of min(subarray) over EVERY contiguous subarray (modulo 10^9+7).
 *      [3,1,2,4] -> 17
 *        subarrays: [3]3 [1]1 [2]2 [4]4 [3,1]1 [1,2]1 [2,4]2 [3,1,2]1 [1,2,4]1 [3,1,2,4]1 -> 17
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every subarray with a running min -> O(n^2)
 *
 *  FLIP THE QUESTION (the "contribution technique"):
 *    instead of "for each subarray, what's its min?" ask
 *    "for each element arr[i], in HOW MANY subarrays is it the minimum?"
 *    answer = sum of arr[i] * count(i)
 *
 *  COUNTING: arr[i] is the min of every subarray that
 *    - starts anywhere after the previous SMALLER element (on the left), and
 *    - ends anywhere before the next SMALLER element (on the right)
 *    left[i]  = i - (index of the previous smaller)     choices for the start
 *    right[i] = (index of the next smaller) - i         choices for the end
 *    count(i) = left[i] * right[i]
 *  Both "nearest smaller" arrays come from a monotonic stack in O(n).
 *
 *  DUPLICATES TRAP: with equal values, a subarray like [2,2] must be counted
 *  ONCE, not for both 2s. Fix: strict '<' on one side and '<=' on the other
 *  ("previous smaller" vs "next smaller-OR-EQUAL").
 *
 *  DRY RUN [3,1,2,4]
 *    i  val  prevSmaller  nextSmaller  left  right  contribution
 *    0   3      -1           1          1     1      3*1 = 3
 *    1   1      -1           4          2     3      1*6 = 6
 *    2   2       1           4          1     2      2*2 = 4
 *    3   4       2           4          1     1      4*1 = 4     -> 17
 *
 *  SAME TECHNIQUE: Largest Rectangle in Histogram, Sum of Subarray Ranges (#2104).
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M15_SumOfSubarrayMinimums {

    static final int MOD = 1_000_000_007;

    static int sumSubarrayMins(int[] arr) {
        int n = arr.length;
        int[] left = new int[n], right = new int[n];
        Deque<Integer> st = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {                               // previous STRICTLY smaller
            while (!st.isEmpty() && arr[st.peek()] >= arr[i]) st.pop();
            left[i] = i - (st.isEmpty() ? -1 : st.peek());
            st.push(i);
        }
        st.clear();
        for (int i = n - 1; i >= 0; i--) {                          // next smaller OR EQUAL
            while (!st.isEmpty() && arr[st.peek()] > arr[i]) st.pop();
            right[i] = (st.isEmpty() ? n : st.peek()) - i;
            st.push(i);
        }
        long sum = 0;
        for (int i = 0; i < n; i++) sum = (sum + (long) arr[i] * left[i] * right[i]) % MOD;
        return (int) sum;
    }

    static int brute(int[] a) {
        long s = 0;
        for (int i = 0; i < a.length; i++) {
            int m = Integer.MAX_VALUE;
            for (int j = i; j < a.length; j++) { m = Math.min(m, a[j]); s += m; }
        }
        return (int) (s % MOD);
    }

    public static void main(String[] args) {
        check("[3,1,2,4]", 17, sumSubarrayMins(new int[]{3, 1, 2, 4}));
        check("[11,81,94,43,3]", 444, sumSubarrayMins(new int[]{11, 81, 94, 43, 3}));
        check("[2,2] (duplicates counted once)", 6, sumSubarrayMins(new int[]{2, 2}));
        Random rnd = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(25), 1, 6).toArray();     // small range -> many duplicates
            ok &= sumSubarrayMins(a) == brute(a);
        }
        check("500 random arrays (many duplicates) vs brute force", true, ok);
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
