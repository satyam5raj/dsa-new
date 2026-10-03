/*
 * ============================================================================
 *  MEDIUM 19 : LONGEST INCREASING SUBSEQUENCE   LeetCode #300
 *  https://leetcode.com/problems/longest-increasing-subsequence/
 *  Pattern: LIS - O(n^2) "ending at i" DP, then O(n log n) patience sorting
 * ============================================================================
 *
 *  PROBLEM
 *    Length of the longest STRICTLY increasing subsequence (not necessarily contiguous).
 *      [10,9,2,5,3,7,101,18] -> 4   ([2,3,7,18])
 *      [0,1,0,3,2,3]         -> 4       [7,7,7,7] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: O(n^2)
 *      STATE       dp[i] = length of the longest increasing subsequence ENDING at i
 *      TRANSITION  dp[i] = 1 + max(dp[j] for j < i with a[j] < a[i])   (or 1)
 *      ANSWER      max over all dp[i]     <- NOT dp[n-1]: the best may end anywhere
 *  "Ending at i" is the trick: it pins down the last value, so extending is checkable.
 *
 *  APPROACH 2: O(n log n) - "tails"
 *    tails[k] = the SMALLEST possible tail of an increasing subsequence of length k+1.
 *    tails is always sorted. For each x:
 *      binary-search the first tails[k] >= x and replace it with x  (a better, lower tail)
 *      if x is bigger than all tails, append  (a longer subsequence exists)
 *    The answer is tails.length. NOTE: tails is NOT itself a valid subsequence; only its
 *    length is meaningful. (Reconstruction needs parent pointers - shown below.)
 *
 *  DRY RUN tails on [10,9,2,5,3,7,101,18]
 *    10:[10]  9:[9]  2:[2]  5:[2,5]  3:[2,3]  7:[2,3,7]  101:[2,3,7,101]  18:[2,3,7,18] -> 4
 *
 *  COMPLEXITY   O(n^2) / O(n)    or    O(n log n) / O(n)
 *  Used by: Russian Doll Envelopes (H03), Largest Divisible Subset (M20).
 * ============================================================================
 */

import java.util.*;

public class M19_LongestIncreasingSubsequence {

    static int lengthOfLIS(int[] nums) {
        int[] tails = new int[nums.length];
        int len = 0;
        for (int x : nums) {
            int lo = 0, hi = len;                         // first index with tails[idx] >= x
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < x) lo = mid + 1; else hi = mid;
            }
            tails[lo] = x;
            if (lo == len) len++;
        }
        return len;
    }

    static int lisQuadratic(int[] a) {
        int[] dp = new int[a.length];
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    // reconstruct one actual LIS with parent pointers (O(n^2) version)
    static List<Integer> oneLIS(int[] a) {
        int n = a.length, end = 0;
        int[] dp = new int[n], parent = new int[n];
        for (int i = 0; i < n; i++) {
            dp[i] = 1; parent[i] = -1;
            for (int j = 0; j < i; j++) if (a[j] < a[i] && dp[j] + 1 > dp[i]) { dp[i] = dp[j] + 1; parent[i] = j; }
            if (dp[i] > dp[end]) end = i;
        }
        LinkedList<Integer> out = new LinkedList<>();
        for (int i = end; i != -1; i = parent[i]) out.addFirst(a[i]);
        return out;
    }

    static boolean increasing(List<Integer> l) { for (int i = 1; i < l.size(); i++) if (l.get(i) <= l.get(i - 1)) return false; return true; }

    public static void main(String[] args) {
        check("[10,9,2,5,3,7,101,18]", 4, lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
        check("[0,1,0,3,2,3]", 4, lengthOfLIS(new int[]{0, 1, 0, 3, 2, 3}));
        check("[7,7,7,7] (strict)", 1, lengthOfLIS(new int[]{7, 7, 7, 7}));
        check("one LIS", "[2, 5, 7, 101]", oneLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = new int[1 + rnd.nextInt(20)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(15);
            List<Integer> s = oneLIS(a);
            ok &= lengthOfLIS(a) == lisQuadratic(a) && s.size() == lisQuadratic(a) && increasing(s);
        }
        check("1000 random: tails == O(n^2) == path", true, ok);
        int[] big = new int[200_000];
        for (int i = 0; i < big.length; i++) big[i] = rnd.nextInt(1_000_000);
        long t0 = System.nanoTime();
        int len = lengthOfLIS(big);
        System.out.printf("   n = 200,000 with tails: LIS %d in %d ms (O(n^2) would be 4*10^10 steps)%n", len, (System.nanoTime() - t0) / 1_000_000);
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
