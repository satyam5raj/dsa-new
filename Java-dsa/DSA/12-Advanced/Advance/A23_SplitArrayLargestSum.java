/*
 * ============================================================================
 *  ADVANCE 23 : SPLIT ARRAY LARGEST SUM  ⭐ bonus   LeetCode #410
 *  https://leetcode.com/problems/split-array-largest-sum/
 *  Pattern: BINARY SEARCH ON THE ANSWER + a GREEDY feasibility check  (or DP)
 * ============================================================================
 *
 *  PROBLEM
 *    Split nums into k non-empty CONTIGUOUS parts so that the largest part-sum is as
 *    small as possible. Return that smallest possible largest sum.
 *      [7,2,5,10,8], k = 2  ->  18      ([7,2,5] = 14 and [10,8] = 18)
 *
 *  ---------------------------------------------------------------------------
 *  FLIP THE QUESTION. Instead of "what's the best split?", ask an easier yes/no:
 *      "can we split into at most k parts if no part may exceed CAP?"
 *  GREEDY answers it in O(n): keep adding numbers to the current part; when the next
 *  one would push it over CAP, start a new part. Greedy uses the FEWEST parts possible
 *  (never closing a part early can only help).
 *
 *  MONOTONIC: if CAP works, any bigger CAP works too. So the answers look like
 *      CAP:   ... no no no no YES YES YES ...
 *  and we binary-search for the first YES between
 *      lo = max(nums)  (every part holds at least one number)
 *      hi = sum(nums)  (one part holds everything)
 *  "At most k" is enough: a split with fewer parts can always be cut further (k <= n).
 *
 *  DP ALTERNATIVE: dp[j][i] = best largest-sum splitting the first i numbers into j
 *  parts = min over p of max(dp[j-1][p], sum(p..i-1)).  O(k n^2) - correct but slower.
 *
 *  DRY RUN [7,2,5,10,8], k = 2     lo = 10, hi = 32
 *    mid 21: [7,2,5] [10,8]          2 parts <= 2  -> hi = 21
 *    mid 15: [7,2,5] [10] [8]        3 parts       -> lo = 16
 *    mid 18: [7,2,5] [10,8]          2             -> hi = 18
 *    mid 17: [7,2,5] [10] [8]        3             -> lo = 18      lo == hi -> 18
 *
 *  COMPLEXITY   O(n log(sum))   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class A23_SplitArrayLargestSum {

    // ---------------- Referee: try every way to place the cuts ----------------
    static int splitArrayBrute(int[] nums, int k) { return brute(nums, 0, k); }

    private static int brute(int[] nums, int start, int k) {
        if (k == 1) { int s = 0; for (int i = start; i < nums.length; i++) s += nums[i]; return s; }
        int best = Integer.MAX_VALUE, first = 0;
        for (int end = start; end <= nums.length - k; end++) {           // the first part is nums[start..end]
            first += nums[end];
            best = Math.min(best, Math.max(first, brute(nums, end + 1, k - 1)));
        }
        return best;
    }

    // ---------------- Approach 1: DP over (parts, prefix length) ----------------
    static int splitArrayDP(int[] nums, int k) {
        int n = nums.length;
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + nums[i];
        long[][] dp = new long[k + 1][n + 1];
        for (long[] row : dp) Arrays.fill(row, Long.MAX_VALUE);
        dp[0][0] = 0;
        for (int j = 1; j <= k; j++)
            for (int i = j; i <= n; i++)                                  // i numbers into j non-empty parts
                for (int p = j - 1; p < i; p++)                           // the last part is nums[p..i-1]
                    if (dp[j - 1][p] != Long.MAX_VALUE)
                        dp[j][i] = Math.min(dp[j][i], Math.max(dp[j - 1][p], pre[i] - pre[p]));
        return (int) dp[k][n];
    }

    // ---------------- Approach 2: binary search on the answer + greedy (main solution) ----------------
    static int splitArray(int[] nums, int k) {
        long lo = 0, hi = 0;
        for (int x : nums) { lo = Math.max(lo, x); hi += x; }
        while (lo < hi) {
            long mid = (lo + hi) / 2;
            if (partsNeeded(nums, mid) <= k) hi = mid;                    // mid works: try smaller
            else lo = mid + 1;                                            // too small: need a bigger cap
        }
        return (int) lo;
    }

    /** Fewest contiguous parts with every part-sum <= cap (greedy: fill each part as much as possible). */
    static int partsNeeded(int[] nums, long cap) {
        int parts = 1;
        long current = 0;
        for (int x : nums) {
            if (current + x > cap) { parts++; current = 0; }
            current += x;
        }
        return parts;
    }

    public static void main(String[] args) {
        check("example 1", 18, splitArray(new int[]{7, 2, 5, 10, 8}, 2));
        check("example 2", 9, splitArray(new int[]{1, 2, 3, 4, 5}, 2));
        check("k = n: the max element", 4, splitArray(new int[]{1, 4, 4}, 3));
        check("k = 1: the whole sum", 23, splitArray(new int[]{1, 4, 4, 14}, 1));
        check("example 1 (DP)", 18, splitArrayDP(new int[]{7, 2, 5, 10, 8}, 2));
        int[] big = new int[1000];
        Arrays.fill(big, 1_000_000);
        check("1,000 x 10^6: a sum of 10^9", 1_000_000_000, splitArray(big, 1));

        Random rnd = new Random(23);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 1 + rnd.nextInt(9), k = 1 + rnd.nextInt(n);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(20);
            int want = splitArrayBrute(a, k);
            ok &= splitArray(a, k) == want && splitArrayDP(a, k) == want;
        }
        check("2,000 random cases: binary search == DP == brute", true, ok);
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
