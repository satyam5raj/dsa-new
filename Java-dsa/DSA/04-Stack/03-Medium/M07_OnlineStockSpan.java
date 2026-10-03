/*
 * ============================================================================
 *  MEDIUM 07 : ONLINE STOCK SPAN             LeetCode #901
 *  https://leetcode.com/problems/online-stock-span/
 *  Pattern: MONOTONIC STACK that MERGES spans ("previous greater" in a stream)
 * ============================================================================
 *
 *  PROBLEM
 *    Prices arrive one per day. span(today) = the number of consecutive days
 *    ending TODAY (going backwards) with price <= today's price.
 *      prices 100, 80, 60, 70, 60, 75, 85 -> spans 1, 1, 1, 2, 1, 4, 6
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: walk back each day until a higher price -> O(n) per call, O(n^2) total.
 *
 *  KEY INSIGHT: once today's price p "absorbs" a previous day (that day's price <= p),
 *  that day can NEVER be the stopping point for a future day:
 *    - if a future price is >= p, it walks past today anyway (and so past that day too)
 *    - if a future price is <  p, it stops at today first
 *  So keep a stack of (price, span) with DECREASING prices. When a new price
 *  comes, pop every entry with price <= it and ADD their spans to today's span.
 *
 *  DRY RUN
 *    100 -> push (100,1)                     span 1
 *     80 -> push (80,1)                      span 1
 *     60 -> push (60,1)                      span 1
 *     70 -> pop (60,1): span 1+1=2; push (70,2)            span 2
 *     60 -> push (60,1)                      span 1
 *     75 -> pop (60,1), (70,2): 1+1+2=4; push (75,4)       span 4
 *     85 -> pop (75,4), (80,1): 1+4+1=6; push (85,6)       span 6
 *
 *  This is "distance to the previous GREATER element" for a stream.
 *  COMPLEXITY   amortized O(1) per call (each day pushed and popped once)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M07_OnlineStockSpan {

    static class StockSpanner {
        private final Deque<int[]> stack = new ArrayDeque<>();   // [price, span], prices decreasing

        public int next(int price) {
            int span = 1;
            while (!stack.isEmpty() && stack.peek()[0] <= price) {
                span += stack.pop()[1];                          // absorb the smaller day's whole span
            }
            stack.push(new int[]{price, span});
            return span;
        }
    }

    public static void main(String[] args) {
        StockSpanner s = new StockSpanner();
        int[] prices = {100, 80, 60, 70, 60, 75, 85};
        int[] got = new int[prices.length];
        for (int i = 0; i < prices.length; i++) got[i] = s.next(prices[i]);
        check("100,80,60,70,60,75,85", new int[]{1, 1, 1, 2, 1, 4, 6}, got);

        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            StockSpanner sp = new StockSpanner();
            int[] p = rnd.ints(1 + rnd.nextInt(40), 1, 10).toArray();
            for (int i = 0; i < p.length; i++) {
                int brute = 0;
                for (int j = i; j >= 0 && p[j] <= p[i]; j--) brute++;
                ok &= sp.next(p[i]) == brute;
            }
        }
        check("200 random price streams vs brute force", true, ok);
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
