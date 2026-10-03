/*
 * ============================================================================
 *  MEDIUM 09 : COIN CHANGE                   LeetCode #322
 *  https://leetcode.com/problems/coin-change/
 *  Pattern: UNBOUNDED knapsack, minimise the number of items
 * ============================================================================
 *
 *  PROBLEM
 *    Fewest coins (unlimited of each denomination) that make up amount; -1 if impossible.
 *      coins [1,2,5], 11 -> 3 (5+5+1)      [2], 3 -> -1      [1], 0 -> 0
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY FAILS for arbitrary coins: [1,3,4], 6 -> greedy 4+1+1 = 3 coins, best 3+3 = 2.
 *
 *  KEY INSIGHT: the LAST coin used is some c, and what's left (a - c) must itself be
 *  made optimally.
 *      STATE       dp[a] = fewest coins summing to a  (INF if impossible)
 *      TRANSITION  dp[a] = 1 + min over coins c <= a of dp[a - c]
 *      BASE        dp[0] = 0
 *      ANSWER      dp[amount], or -1 if still INF
 *  Use INF = amount + 1 (no answer can need more than `amount` coins) instead of
 *  Integer.MAX_VALUE, so "INF + 1" can't overflow.
 *
 *  DRY RUN [1,2,5], 6:  dp = 0,1,1,2,2,1,2   (dp6 = 1 + min(dp5, dp4, dp1) = 2)
 *
 *  COMPLEXITY   Time O(amount * coins)   Space O(amount)
 *  Alternative: BFS from 0 where each edge adds a coin (levels = coin counts).
 * ============================================================================
 */

import java.util.*;

public class M09_CoinChange {

    static int coinChange(int[] coins, int amount) {
        int INF = amount + 1;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, INF);
        dp[0] = 0;
        for (int a = 1; a <= amount; a++)
            for (int c : coins)
                if (c <= a) dp[a] = Math.min(dp[a], dp[a - c] + 1);
        return dp[amount] == INF ? -1 : dp[amount];
    }

    // referee: memoized recursion "try every coin as the last one"
    static int memo(int[] coins, int a, Integer[] cache) {
        if (a == 0) return 0;
        if (cache[a] != null) return cache[a];
        int best = Integer.MAX_VALUE;
        for (int c : coins)
            if (c <= a) { int r = memo(coins, a - c, cache); if (r != Integer.MAX_VALUE) best = Math.min(best, r + 1); }
        return cache[a] = best;
    }

    static int greedy(int[] coins, int amount) {
        int[] s = coins.clone(); Arrays.sort(s);
        int count = 0;
        for (int i = s.length - 1; i >= 0; i--) { count += amount / s[i]; amount %= s[i]; }
        return amount == 0 ? count : -1;
    }

    public static void main(String[] args) {
        check("[1,2,5], 11", 3, coinChange(new int[]{1, 2, 5}, 11));
        check("[2], 3", -1, coinChange(new int[]{2}, 3));
        check("[1], 0", 0, coinChange(new int[]{1}, 0));
        check("[1,3,4], 6 (DP)", 2, coinChange(new int[]{1, 3, 4}, 6));
        check("[1,3,4], 6 (greedy, wrong)", 3, greedy(new int[]{1, 3, 4}, 6));
        check("[186,419,83,408], 6249", 20, coinChange(new int[]{186, 419, 83, 408}, 6249));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] c = new int[1 + rnd.nextInt(4)];
            for (int i = 0; i < c.length; i++) c[i] = 1 + rnd.nextInt(20);
            int amount = rnd.nextInt(200);
            int m = memo(c, amount, new Integer[amount + 1]);
            ok &= coinChange(c, amount) == (m == Integer.MAX_VALUE ? -1 : m);
        }
        check("500 random vs memoized recursion", true, ok);
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
