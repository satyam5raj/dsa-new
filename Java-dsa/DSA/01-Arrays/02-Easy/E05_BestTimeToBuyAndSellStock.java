/*
 * ============================================================================
 *  EASY 05 : BEST TIME TO BUY AND SELL STOCK  LeetCode #121
 *  https://leetcode.com/problems/best-time-to-buy-and-sell-stock/
 *  Pattern: TRACK THE MINIMUM SO FAR (one pass, greedy)
 * ============================================================================
 *
 *  PROBLEM
 *    prices[i] = stock price on day i. Buy on ONE day and sell on a LATER day.
 *    Return the maximum profit, or 0 if no profit is possible.
 *      [7,1,5,3,6,4] -> 5    buy at 1 (day 1), sell at 6 (day 4)
 *      [7,6,4,3,1]   -> 0    prices only fall
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every (buy, sell) pair with buy < sell -> O(n^2)
 *
 *  KEY INSIGHT: if I SELL on day i, the best I could have done is to have bought
 *  at the LOWEST price BEFORE day i. So walk left to right keeping:
 *    minPrice  = cheapest price seen so far (best buy day up to now)
 *    maxProfit = best of (price - minPrice) seen so far
 *  One pass, O(1) memory.
 *
 *  DRY RUN [7,1,5,3,6,4]
 *    price  minPrice  profit if sold today  maxProfit
 *      7       7            0                  0
 *      1       1            0                  0     <- new cheapest
 *      5       1            4                  4
 *      3       1            2                  4
 *      6       1            5                  5     <- best
 *      4       1            3                  5
 *
 *  WHY NOT JUST max - min? The max might come BEFORE the min ([5,1] -> 0, not 4).
 *  Order matters: you must buy before you sell.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E05_BestTimeToBuyAndSellStock {

    static int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);               // best day to have bought, so far
            maxProfit = Math.max(maxProfit, price - minPrice);  // sell today?
        }
        return maxProfit;
    }

    public static void main(String[] args) {
        check("[7,1,5,3,6,4]", 5, maxProfit(new int[]{7, 1, 5, 3, 6, 4}));
        check("[7,6,4,3,1] (falling)", 0, maxProfit(new int[]{7, 6, 4, 3, 1}));
        check("[2,4,1] (max before min)", 2, maxProfit(new int[]{2, 4, 1}));
        check("[1,2,3,4,5] (rising)", 4, maxProfit(new int[]{1, 2, 3, 4, 5}));
        check("[5] (single day)", 0, maxProfit(new int[]{5}));
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
