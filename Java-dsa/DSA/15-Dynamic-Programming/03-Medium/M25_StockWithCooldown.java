/*
 * ============================================================================
 *  MEDIUM 25 : BEST TIME TO BUY AND SELL STOCK WITH COOLDOWN   LeetCode #309
 *  https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/
 *  Pattern: STATE MACHINE DP (3 states per day)
 * ============================================================================
 *
 *  PROBLEM
 *    Unlimited transactions, at most one share at a time, and after SELLING you must
 *    wait one day before buying again. Max profit?
 *      [1,2,3,0,2] -> 3   (buy 1, sell 2, cooldown, buy 0, sell 2)
 *      [1]         -> 0
 *
 *  ---------------------------------------------------------------------------
 *  DRAW THE STATES (end of each day):
 *
 *        buy            sell
 *    REST ----> HOLD ----> SOLD
 *     ^ |        | ^         |
 *     | +--------+ +-- rest  |       rest = do nothing
 *     +----------------------+       (after SOLD you can only go to REST: the cooldown)
 *
 *      hold[i] = max(hold[i-1],            rest[i-1] - p[i])   keep / buy (only from REST)
 *      sold[i] = hold[i-1] + p[i]                              sell today
 *      rest[i] = max(rest[i-1], sold[i-1])                     idle / cooldown finished
 *      BASE  hold = -infinity (can't hold before buying), sold = -inf, rest = 0
 *      ANSWER max(sold, rest) on the last day
 *  Every arrow is exactly one term in a max(). That's the recipe for every stock
 *  variant: fee (M26), at most k transactions (H11), cooldown (here).
 *
 *  DRY RUN [1,2,3,0,2]   (hold, sold, rest)
 *    p=1: (-1, -inf, 0)   p=2: (-1, 1, 0)   p=3: (-1, 2, 1)
 *    p=0: (1, -1, 2)      p=2: (1, 3, 2)    -> max(3, 2) = 3
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M25_StockWithCooldown {

    static int maxProfit(int[] prices) {
        int NEG = Integer.MIN_VALUE / 2;
        int hold = NEG, sold = NEG, rest = 0;
        for (int p : prices) {
            int newHold = Math.max(hold, rest - p);        // buying is only allowed from REST
            int newSold = hold + p;
            int newRest = Math.max(rest, sold);
            hold = newHold; sold = newSold; rest = newRest;
        }
        return Math.max(sold, rest);
    }

    // referee: plain recursion over (day, holding, mustWait)
    static int brute(int[] p, int day, boolean holding) {
        if (day >= p.length) return 0;
        int skip = brute(p, day + 1, holding);
        if (holding) return Math.max(skip, p[day] + brute(p, day + 2, false));   // sell -> skip tomorrow
        return Math.max(skip, -p[day] + brute(p, day + 1, true));
    }

    public static void main(String[] args) {
        check("[1,2,3,0,2]", 3, maxProfit(new int[]{1, 2, 3, 0, 2}));
        check("[1]", 0, maxProfit(new int[]{1}));
        check("[5,4,3,2,1] (never trade)", 0, maxProfit(new int[]{5, 4, 3, 2, 1}));
        check("[1,4,2] (cooldown irrelevant)", 3, maxProfit(new int[]{1, 4, 2}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] p = new int[1 + rnd.nextInt(14)];
            for (int i = 0; i < p.length; i++) p[i] = rnd.nextInt(20);
            ok &= maxProfit(p) == brute(p, 0, false);
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
