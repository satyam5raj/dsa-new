/*
 * ============================================================================
 *  HARD 11 : BEST TIME TO BUY AND SELL STOCK IV  ⭐   LeetCode #188
 *  https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/
 *  Pattern: STATE MACHINE with a transaction counter: buy[j], sell[j] for j = 1..k
 * ============================================================================
 *
 *  PROBLEM
 *    At most k transactions (buy + sell), one share at a time. Max profit?
 *      k=2, [2,4,1]         -> 2
 *      k=2, [3,2,6,5,0,3]   -> 7   (2->6, 0->3)
 *
 *  Added because it GENERALISES the whole stock family:
 *      Stock I   (#121)  k = 1
 *      Stock II  (#122)  k = infinity      -> sum every rise
 *      Stock III (#123)  k = 2
 *      Cooldown (M25), Fee (M26): k = infinity + one extra rule
 *
 *  ---------------------------------------------------------------------------
 *  STATES per day, for j = 1..k:
 *      buy[j]  = best profit while HOLDING, having started the j-th transaction
 *      sell[j] = best profit NOT holding, having completed j transactions
 *  TRANSITIONS (today's price p):
 *      buy[j]  = max(buy[j],  sell[j-1] - p)     keep holding, or open transaction j
 *      sell[j] = max(sell[j], buy[j] + p)        keep, or close transaction j
 *      BASE sell[0] = 0 forever, buy[j] = -infinity, sell[j] = 0
 *      ANSWER sell[k]
 *  SHORTCUT: if k >= n/2 the limit can never bind (a transaction needs 2 days) ->
 *  Stock II greedy, which also avoids allocating a huge k-sized array.
 *
 *  DRY RUN k=2, [3,2,6,5,0,3]   (buy1, sell1, buy2, sell2)
 *    3:(-3,0,-3,0)  2:(-2,0,-2,0)  6:(-2,4,-2,4)  5:(-2,4,-1,4)  0:(0,4,4,4)  3:(0,4,4,7) -> 7
 *
 *  COMPLEXITY   Time O(n*k)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class H11_StockIV {

    static int maxProfit(int k, int[] prices) {
        int n = prices.length;
        if (k >= n / 2) {                                 // unlimited in practice: take every rise
            int s = 0;
            for (int i = 1; i < n; i++) s += Math.max(0, prices[i] - prices[i - 1]);
            return s;
        }
        int[] buy = new int[k + 1], sell = new int[k + 1];
        Arrays.fill(buy, Integer.MIN_VALUE / 2);
        for (int p : prices)
            for (int j = 1; j <= k; j++) {
                buy[j] = Math.max(buy[j], sell[j - 1] - p);
                sell[j] = Math.max(sell[j], buy[j] + p);
            }
        return sell[k];
    }

    static int brute(int[] p, int day, boolean holding, int left) {
        if (day == p.length) return 0;
        int skip = brute(p, day + 1, holding, left);
        if (holding) return Math.max(skip, p[day] + brute(p, day + 1, false, left));
        if (left == 0) return skip;
        return Math.max(skip, -p[day] + brute(p, day + 1, true, left - 1));      // buying opens a transaction
    }

    public static void main(String[] args) {
        check("k=2 [2,4,1]", 2, maxProfit(2, new int[]{2, 4, 1}));
        check("k=2 [3,2,6,5,0,3]", 7, maxProfit(2, new int[]{3, 2, 6, 5, 0, 3}));
        check("k=1 (Stock I) [7,1,5,3,6,4]", 5, maxProfit(1, new int[]{7, 1, 5, 3, 6, 4}));
        check("k=100 (Stock II) [7,1,5,3,6,4]", 7, maxProfit(100, new int[]{7, 1, 5, 3, 6, 4}));
        check("k=0", 0, maxProfit(0, new int[]{1, 5}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] p = new int[1 + rnd.nextInt(12)];
            for (int i = 0; i < p.length; i++) p[i] = rnd.nextInt(20);
            int k = rnd.nextInt(5);
            ok &= maxProfit(k, p) == brute(p, 0, false, k);
        }
        check("1000 random vs recursion", true, ok);
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
