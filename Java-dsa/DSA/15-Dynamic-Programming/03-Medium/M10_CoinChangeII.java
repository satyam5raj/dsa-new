/*
 * ============================================================================
 *  MEDIUM 10 : COIN CHANGE II                LeetCode #518
 *  https://leetcode.com/problems/coin-change-ii/
 *  Pattern: UNBOUNDED knapsack, COUNT COMBINATIONS (coins in the OUTER loop)
 * ============================================================================
 *
 *  PROBLEM
 *    Number of COMBINATIONS (order doesn't matter) of coins summing to amount.
 *      amount 5, [1,2,5] -> 4   (5, 2+2+1, 2+1+1+1, 1+1+1+1+1)
 *      amount 3, [2]     -> 0       amount 10, [10] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  THE TRAP: "ways[a] = sum over coins of ways[a - c]" counts 1+2 and 2+1 separately
 *  (that's Combination Sum IV, the PERMUTATION count: 9 for this example).
 *
 *  KEY INSIGHT: impose an ORDER on coins - decide coin type by coin type.
 *      STATE       ways[i][a] = combinations of the first i coin types summing to a
 *      TRANSITION  ways[i][a] = ways[i-1][a]            (use no more of coin i)
 *                             + ways[i][a - coin_i]     (use one more of coin i)
 *  As one row with coins in the OUTER loop and amounts going UP (unbounded):
 *      for coin: for a = coin..amount: ways[a] += ways[a - coin]
 *  Each multiset is then built in exactly one order (non-decreasing coin index).
 *  (01-Basics/KnapsackPatterns lists both versions side by side.)
 *
 *  DRY RUN [1,2,5], 5   ways = [1,0,0,0,0,0]
 *    coin 1: [1,1,1,1,1,1]
 *    coin 2: [1,1,2,2,3,3]
 *    coin 5: [1,1,2,2,3,4]  -> 4
 *
 *  COMPLEXITY   Time O(amount * coins)   Space O(amount)
 * ============================================================================
 */

import java.util.*;

public class M10_CoinChangeII {

    static int change(int amount, int[] coins) {
        int[] ways = new int[amount + 1];
        ways[0] = 1;                                      // one way to make 0: no coins
        for (int coin : coins)                            // coin types OUTSIDE -> combinations
            for (int a = coin; a <= amount; a++) ways[a] += ways[a - coin];
        return ways[amount];
    }

    static int permutationsWrong(int amount, int[] coins) {   // amount outside -> counts orders
        int[] ways = new int[amount + 1];
        ways[0] = 1;
        for (int a = 1; a <= amount; a++) for (int c : coins) if (c <= a) ways[a] += ways[a - c];
        return ways[amount];
    }

    static int brute(int[] coins, int i, int rem) {     // recursion: use coin i again, or move on
        if (rem == 0) return 1;
        if (i == coins.length) return 0;
        return (coins[i] <= rem ? brute(coins, i, rem - coins[i]) : 0) + brute(coins, i + 1, rem);
    }

    public static void main(String[] args) {
        check("5, [1,2,5]", 4, change(5, new int[]{1, 2, 5}));
        check("3, [2]", 0, change(3, new int[]{2}));
        check("10, [10]", 1, change(10, new int[]{10}));
        check("0, [7]", 1, change(0, new int[]{7}));
        check("amount-outer loop counts orders", 9, permutationsWrong(5, new int[]{1, 2, 5}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            int[] c = new int[1 + rnd.nextInt(4)];
            Set<Integer> used = new HashSet<>();
            for (int i = 0; i < c.length; i++) { do c[i] = 1 + rnd.nextInt(15); while (!used.add(c[i])); }   // distinct coins
            int amount = rnd.nextInt(60);
            ok &= change(amount, c) == brute(c, 0, amount);
        }
        check("400 random vs recursion", true, ok);
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
