/*
 * ============================================================================
 *  MEDIUM 26 : BEST TIME TO BUY AND SELL STOCK WITH TRANSACTION FEE   LeetCode #714
 *  https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/
 *  Pattern: STATE MACHINE DP (2 states: holding / free)
 * ============================================================================
 *
 *  PROBLEM
 *    Unlimited transactions, one share at a time, each completed transaction costs
 *    `fee`. Max profit?
 *      [1,3,2,8,4,9], fee 2   -> 8   (buy 1 sell 8: 5; buy 4 sell 9: 3)
 *      [1,3,7,5,10,3], fee 3  -> 6
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY "sum every rise" (Stock II) FAILS: with a fee, many small trades lose to one
 *  long trade: 1->3 + 2->8 as two trades nets 0 + 4 = 4, one trade 1->8 nets 5.
 *
 *  TWO STATES (end of day):
 *      hold = best profit while holding a share
 *      free = best profit with no share
 *      hold' = max(hold, free - p)              keep, or buy today
 *      free' = max(free, hold + p - fee)        keep waiting, or sell today (pay fee)
 *      BASE hold = -infinity, free = 0;   ANSWER free
 *  Charging the fee on the sell (or the buy - either works, once per round trip).
 *  Using the new `free` when computing `hold` on the same day is harmless here
 *  (selling and re-buying the same day only loses a fee), so no temp is needed.
 *
 *  DRY RUN [1,3,2,8,4,9] fee 2   (hold, free)
 *    1:(-1,0)  3:(-1,0)  2:(-1,0)  8:(-1,5)  4:(1,5)  9:(1,8)  -> 8
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M26_StockWithTransactionFee {

    static int maxProfit(int[] prices, int fee) {
        int hold = Integer.MIN_VALUE / 2, free = 0;
        for (int p : prices) {
            free = Math.max(free, hold + p - fee);         // sell
            hold = Math.max(hold, free - p);               // buy (same-day re-buy can't help)
        }
        return free;
    }

    static int greedyNoFeeAware(int[] p, int fee) {     // Stock II's "take every rise", paying the fee each time
        int s = 0;
        for (int i = 1; i < p.length; i++) if (p[i] > p[i - 1]) s += p[i] - p[i - 1] - fee;
        return s;
    }

    static int brute(int[] p, int fee, int day, boolean holding) {
        if (day == p.length) return 0;
        int skip = brute(p, fee, day + 1, holding);
        int act = holding ? p[day] - fee + brute(p, fee, day + 1, false) : -p[day] + brute(p, fee, day + 1, true);
        return Math.max(skip, act);
    }

    public static void main(String[] args) {
        check("[1,3,2,8,4,9], 2", 8, maxProfit(new int[]{1, 3, 2, 8, 4, 9}, 2));
        check("[1,3,7,5,10,3], 3", 6, maxProfit(new int[]{1, 3, 7, 5, 10, 3}, 3));
        check("naive per-rise trading", 7, greedyNoFeeAware(new int[]{1, 3, 2, 8, 4, 9}, 2));
        check("[9,8,7], 1", 0, maxProfit(new int[]{9, 8, 7}, 1));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] p = new int[1 + rnd.nextInt(14)];
            for (int i = 0; i < p.length; i++) p[i] = rnd.nextInt(20);
            int fee = rnd.nextInt(5);
            ok &= maxProfit(p, fee) == brute(p, fee, 0, false);
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
