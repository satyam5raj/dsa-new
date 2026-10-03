/*
 * ============================================================================
 *  HARD 09 : MAXIMUM PROFIT IN JOB SCHEDULING   LeetCode #1235
 *  https://leetcode.com/problems/maximum-profit-in-job-scheduling/
 *  Pattern: WEIGHTED INTERVAL SCHEDULING - sort by end + DP + binary search
 * ============================================================================
 *
 *  PROBLEM
 *    Jobs (start, end, profit). Pick non-overlapping jobs (a job may start exactly when
 *    another ends) to maximise total profit.
 *      start [1,2,3,3], end [3,4,5,6], profit [50,10,40,70]            -> 120  (50 + 70)
 *      start [1,2,3,4,6], end [3,5,10,6,9], profit [20,20,100,70,60]    -> 150
 *      start [1,1,1], end [2,3,4], profit [5,6,4]                      -> 6
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY FAILS: "earliest end first" is right for COUNTING jobs (unweighted), not for
 *  profit; "highest profit first" fails too.
 *
 *  KEY INSIGHT: sort jobs by END time. For job i, either
 *      skip it       -> best of the first i-1 jobs                       dp[i-1]
 *      take it       -> profit[i] + best among jobs that END <= start[i]
 *  Because jobs are sorted by end, "jobs ending <= start[i]" is a PREFIX of the list:
 *  find its length p with a binary search (upper bound on end times).
 *      STATE       dp[i] = best profit using the first i jobs (sorted by end)
 *      TRANSITION  dp[i] = max(dp[i-1], profit[i-1] + dp[p])
 *      BASE        dp[0] = 0
 *  It's House Robber where "the previous compatible job" replaces "i-2".
 *
 *  DRY RUN example 1 (sorted by end: [1,3,50] [2,4,10] [3,5,40] [3,6,70])
 *    dp1 = 50   dp2 = max(50, 10 + 0) = 50   dp3 = max(50, 40 + dp1) = 90
 *    dp4 = max(90, 70 + dp1) = 120
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H09_MaximumProfitInJobScheduling {

    static int jobScheduling(int[] startTime, int[] endTime, int[] profit) {
        int n = startTime.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Integer.compare(endTime[a], endTime[b]));
        int[] ends = new int[n];
        for (int i = 0; i < n; i++) ends[i] = endTime[idx[i]];
        int[] dp = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            int job = idx[i - 1];
            int p = upperBound(ends, i - 1, startTime[job]);        // # of earlier jobs with end <= start
            dp[i] = Math.max(dp[i - 1], profit[job] + dp[p]);
        }
        return dp[n];
    }

    static int upperBound(int[] a, int len, int key) {           // first index in a[0..len) with a[idx] > key
        int lo = 0, hi = len;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] <= key) lo = mid + 1; else hi = mid; }
        return lo;
    }

    static int brute(int[] s, int[] e, int[] p) {
        int n = s.length, best = 0;
        for (int mask = 0; mask < 1 << n; mask++) {
            boolean ok = true;
            int sum = 0;
            for (int i = 0; i < n && ok; i++) {
                if ((mask >> i & 1) == 0) continue;
                sum += p[i];
                for (int j = i + 1; j < n && ok; j++)
                    if ((mask >> j & 1) == 1 && s[i] < e[j] && s[j] < e[i]) ok = false;   // overlap
            }
            if (ok) best = Math.max(best, sum);
        }
        return best;
    }

    public static void main(String[] args) {
        check("example 1", 120, jobScheduling(new int[]{1, 2, 3, 3}, new int[]{3, 4, 5, 6}, new int[]{50, 10, 40, 70}));
        check("example 2", 150, jobScheduling(new int[]{1, 2, 3, 4, 6}, new int[]{3, 5, 10, 6, 9}, new int[]{20, 20, 100, 70, 60}));
        check("example 3", 6, jobScheduling(new int[]{1, 1, 1}, new int[]{2, 3, 4}, new int[]{5, 6, 4}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(12);
            int[] s = new int[n], e = new int[n], p = new int[n];
            for (int i = 0; i < n; i++) { s[i] = rnd.nextInt(15); e[i] = s[i] + 1 + rnd.nextInt(6); p[i] = 1 + rnd.nextInt(30); }
            ok &= jobScheduling(s, e, p) == brute(s, e, p);
        }
        check("500 random vs every subset", true, ok);
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
