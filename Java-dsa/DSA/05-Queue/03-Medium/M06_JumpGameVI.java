/*
 * ============================================================================
 *  MEDIUM 06 : JUMP GAME VI  ⭐ bonus (DP + monotonic deque)   LeetCode #1696
 *  https://leetcode.com/problems/jump-game-vi/
 *  Pattern: DP WHERE EACH STATE NEEDS THE MAX OF THE PREVIOUS k STATES
 * ============================================================================
 *
 *  PROBLEM
 *    Start at index 0; each move jumps 1..k steps forward. Your score is the sum
 *    of nums at every index you land on. Maximize the score on reaching the last index.
 *      nums = [1,-1,-2,4,-7,3], k = 2 -> 7     (path 0 -> 1 -> 3 -> 5: 1 - 1 + 4 + 3)
 *
 *  ---------------------------------------------------------------------------
 *  DP DEFINITION
 *    dp[i] = the best score of a path that ENDS at index i
 *    dp[i] = nums[i] + max(dp[i-k], ..., dp[i-1])   (we came from one of the k previous indices)
 *  Computing that max naively is O(k) per index -> O(n * k): too slow for n = k = 10^5.
 *
 *  "MAX OF THE LAST k VALUES, as the window slides" = Sliding Window Maximum (Arrays/H02)!
 *  Keep a MONOTONIC DEQUE of indices whose dp values are DECREASING:
 *    for each i:
 *      drop the front if it's out of reach (index < i - k)
 *      dp[i] = nums[i] + dp[front]            (the front is the best reachable dp)
 *      drop from the BACK every index with dp <= dp[i] (they'll never be better than i)
 *      push i
 *
 *  DRY RUN nums = [1,-1,-2,4,-7,3], k = 2
 *    dp[0] = 1                      dq [0]
 *    dp[1] = -1 + 1 = 0             dq [0,1]
 *    dp[2] = -2 + 1 = -1            dq [0,1,2] (0 is still reachable: 2 - 2 = 0)
 *    dp[3] = 4 + 0 = 4              dq [3]     (0 expires; pops 2 and 1: smaller than 4)
 *    dp[4] = -7 + 4 = -3            dq [3,4]
 *    dp[5] = 3 + 4 = 7              -> 7
 *
 *  THE GENERAL LESSON: when a DP transition takes the max/min over a SLIDING
 *  range of previous states, a monotonic deque makes it O(1) amortized.
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M06_JumpGameVI {

    static int maxResult(int[] nums, int k) {
        int n = nums.length;
        int[] dp = new int[n];
        dp[0] = nums[0];
        Deque<Integer> dq = new ArrayDeque<>();                  // indices, dp values decreasing
        dq.offer(0);
        for (int i = 1; i < n; i++) {
            if (dq.peekFirst() < i - k) dq.pollFirst();          // out of jumping range
            dp[i] = nums[i] + dp[dq.peekFirst()];
            while (!dq.isEmpty() && dp[dq.peekLast()] <= dp[i]) dq.pollLast();
            dq.offerLast(i);
        }
        return dp[n - 1];
    }

    static int brute(int[] nums, int k) {                        // O(n*k) DP
        int[] dp = new int[nums.length];
        dp[0] = nums[0];
        for (int i = 1; i < nums.length; i++) {
            int best = Integer.MIN_VALUE;
            for (int j = Math.max(0, i - k); j < i; j++) best = Math.max(best, dp[j]);
            dp[i] = nums[i] + best;
        }
        return dp[nums.length - 1];
    }

    public static void main(String[] args) {
        check("[1,-1,-2,4,-7,3] k=2", 7, maxResult(new int[]{1, -1, -2, 4, -7, 3}, 2));
        check("[10,-5,-2,4,0,3] k=3", 17, maxResult(new int[]{10, -5, -2, 4, 0, 3}, 3));
        check("[1,-5,-20,4,-1,3,-6,-3] k=2", 0, maxResult(new int[]{1, -5, -20, 4, -1, 3, -6, -3}, 2));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), -10, 10).toArray();
            int k = 1 + rnd.nextInt(5);
            ok &= maxResult(a, k) == brute(a, k);
        }
        check("500 random cases vs O(n*k) DP", true, ok);
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
