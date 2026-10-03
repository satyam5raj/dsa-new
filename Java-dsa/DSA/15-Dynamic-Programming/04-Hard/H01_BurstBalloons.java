/*
 * ============================================================================
 *  HARD 01 : BURST BALLOONS                  LeetCode #312
 *  https://leetcode.com/problems/burst-balloons/
 *  Pattern: INTERVAL DP where k is the LAST balloon burst in the interval
 * ============================================================================
 *
 *  PROBLEM
 *    Bursting balloon i earns nums[left] * nums[i] * nums[right], where left/right are
 *    its CURRENT neighbours (outside the array counts as 1). Max coins?
 *      [3,1,5,8] -> 167   (burst 1, 5, 3, 8: 3*1*5 + 3*5*8 + 1*3*8 + 1*8*1)
 *      [1,5]     -> 10
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every burst order -> O(n!).
 *
 *  WHY "FIRST BURST" DOESN'T SPLIT: bursting k first makes k-1 and k+1 neighbours,
 *  so the left and right parts still interact - not independent subproblems.
 *
 *  KEY INSIGHT: think of k as the LAST balloon burst in the open interval (l, r).
 *  While everything else in (l, r) is being burst, k is still standing and acts as a
 *  WALL: the left part (l, k) and right part (k, r) never see each other. When k
 *  finally goes, its neighbours are exactly l and r.
 *      pad the array: a = [1, nums..., 1]
 *      STATE       dp[l][r] = max coins from bursting every balloon strictly between l, r
 *      TRANSITION  dp[l][r] = max over l < k < r of  dp[l][k] + a[l]*a[k]*a[r] + dp[k][r]
 *      BASE        dp[l][l+1] = 0 (nothing in between)
 *      ORDER       by increasing gap r - l;      ANSWER dp[0][n+1]
 *  "Choose the LAST action so the rest splits cleanly" is the key move of interval DP.
 *
 *  DRY RUN [3,1,5,8] -> a = [1,3,1,5,8,1]; the best last balloon overall is 8 (k=4):
 *    dp[0][4] (burst 3,1,5 between 1 and 8) + 1*8*1 + dp[4][5] = 159 + 8 + 0 = 167
 *
 *  COMPLEXITY   Time O(n^3)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class H01_BurstBalloons {

    static int maxCoins(int[] nums) {
        int n = nums.length;
        int[] a = new int[n + 2];
        a[0] = a[n + 1] = 1;
        System.arraycopy(nums, 0, a, 1, n);
        int[][] dp = new int[n + 2][n + 2];
        for (int gap = 2; gap <= n + 1; gap++)                  // r - l
            for (int l = 0; l + gap <= n + 1; l++) {
                int r = l + gap;
                for (int k = l + 1; k < r; k++)                 // k = the LAST balloon burst in (l, r)
                    dp[l][r] = Math.max(dp[l][r], dp[l][k] + a[l] * a[k] * a[r] + dp[k][r]);
            }
        return dp[0][n + 1];
    }

    // referee: try every burst order
    static int brute(List<Integer> b) {
        if (b.isEmpty()) return 0;
        int best = 0;
        for (int i = 0; i < b.size(); i++) {
            int left = i > 0 ? b.get(i - 1) : 1, right = i < b.size() - 1 ? b.get(i + 1) : 1;
            int coins = left * b.get(i) * right;
            List<Integer> rest = new ArrayList<>(b);
            rest.remove(i);
            best = Math.max(best, coins + brute(rest));
        }
        return best;
    }

    public static void main(String[] args) {
        check("[3,1,5,8]", 167, maxCoins(new int[]{3, 1, 5, 8}));
        check("[1,5]", 10, maxCoins(new int[]{1, 5}));
        check("[7]", 7, maxCoins(new int[]{7}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int[] a = new int[1 + rnd.nextInt(7)];
            List<Integer> l = new ArrayList<>();
            for (int i = 0; i < a.length; i++) { a[i] = rnd.nextInt(10); l.add(a[i]); }
            ok &= maxCoins(a) == brute(l);
        }
        check("200 random vs every burst order (n!)", true, ok);
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
