/*
 * ============================================================================
 *  KNAPSACK PATTERNS: 0/1 vs unbounded, loop direction, combinations vs permutations
 * ============================================================================
 *
 *  0/1 KNAPSACK   n items (weight w[i], value v[i]), capacity W, each item AT MOST ONCE.
 *    STATE       dp[i][c] = best value using the first i items with capacity c
 *    TRANSITION  dp[i][c] = max( dp[i-1][c],                     skip item i
 *                                dp[i-1][c - w[i]] + v[i] )      take it (if it fits)
 *    BASE        dp[0][c] = 0
 *    ANSWER      dp[n][W]                         Time O(n*W)  Space O(n*W)
 *
 *  1-D VERSION: row i only reads row i-1, so keep ONE row and overwrite it in place.
 *      for item: for c = W DOWN TO w:  dp[c] = max(dp[c], dp[c - w] + v)
 *    WHY BACKWARDS? dp[c - w] must still hold the OLD row (item not yet used).
 *    Going forwards, dp[c - w] was already updated in this pass -> the item gets used
 *    again -> that's the UNBOUNDED knapsack instead. Part 2 demonstrates the bug.
 *
 *  UNBOUNDED KNAPSACK (each item any number of times)
 *      for item: for c = w UP TO W:  dp[c] = max(dp[c], dp[c - w] + v)
 *    Forward order is now exactly what we want: dp[c - w] may already include this item.
 *
 *  COUNTING VERSIONS (Coin Change II, Combination Sum IV)
 *    COMBINATIONS (order doesn't matter, {1,2} == {2,1}):
 *        for coin:  for amount:  ways[a] += ways[a - coin]
 *      coins in the OUTER loop -> each multiset is built in one fixed coin order.
 *    PERMUTATIONS (order matters, 1+2 != 2+1):
 *        for amount:  for coin:  ways[a] += ways[a - coin]
 *      the amount on the outside -> every coin can be the LAST one used.
 *
 *  WHAT IS "THE KNAPSACK" IN DISGUISE?
 *    Partition Equal Subset Sum  0/1, boolean: can a subset reach sum/2?
 *    Target Sum                  0/1, counting: subsets summing to (total+target)/2
 *    Coin Change                 unbounded, min count
 *    Coin Change II              unbounded, count combinations
 *    Perfect Squares             unbounded, min count, items = 1, 4, 9, ...
 *    Ones and Zeroes             0/1 with TWO capacities (2-D dp, both loops backwards)
 *
 *  Run:  java KnapsackPatterns.java
 * ============================================================================
 */

import java.util.*;

public class KnapsackPatterns {

    // 0/1, full 2-D table
    static int knapsack2D(int[] w, int[] v, int W) {
        int n = w.length;
        int[][] dp = new int[n + 1][W + 1];
        for (int i = 1; i <= n; i++)
            for (int c = 0; c <= W; c++) {
                dp[i][c] = dp[i - 1][c];                                               // skip
                if (w[i - 1] <= c) dp[i][c] = Math.max(dp[i][c], dp[i - 1][c - w[i - 1]] + v[i - 1]);   // take
            }
        return dp[n][W];
    }

    // 0/1, one row, capacity loop BACKWARDS
    static int knapsack1D(int[] w, int[] v, int W) {
        int[] dp = new int[W + 1];
        for (int i = 0; i < w.length; i++)
            for (int c = W; c >= w[i]; c--) dp[c] = Math.max(dp[c], dp[c - w[i]] + v[i]);
        return dp[W];
    }

    // the classic bug: one row, but FORWARDS -> items get reused (it's the unbounded knapsack)
    static int knapsackForwardBug(int[] w, int[] v, int W) {
        int[] dp = new int[W + 1];
        for (int i = 0; i < w.length; i++)
            for (int c = w[i]; c <= W; c++) dp[c] = Math.max(dp[c], dp[c - w[i]] + v[i]);
        return dp[W];
    }

    static int unbounded(int[] w, int[] v, int W) { return knapsackForwardBug(w, v, W); }   // same code, intended this time

    // brute force 0/1: every subset
    static int brute01(int[] w, int[] v, int W) {
        int best = 0;
        for (int mask = 0; mask < 1 << w.length; mask++) {
            int tw = 0, tv = 0;
            for (int i = 0; i < w.length; i++) if ((mask >> i & 1) == 1) { tw += w[i]; tv += v[i]; }
            if (tw <= W) best = Math.max(best, tv);
        }
        return best;
    }

    // brute force unbounded: recursion over "which item next"
    static int bruteUnbounded(int[] w, int[] v, int W) {
        int best = 0;
        for (int i = 0; i < w.length; i++) if (w[i] <= W) best = Math.max(best, v[i] + bruteUnbounded(w, v, W - w[i]));
        return best;
    }

    // counting: combinations vs permutations
    static long combinations(int[] coins, int amount) {
        long[] ways = new long[amount + 1];
        ways[0] = 1;
        for (int coin : coins) for (int a = coin; a <= amount; a++) ways[a] += ways[a - coin];
        return ways[amount];
    }

    static long permutations(int[] coins, int amount) {
        long[] ways = new long[amount + 1];
        ways[0] = 1;
        for (int a = 1; a <= amount; a++) for (int coin : coins) if (coin <= a) ways[a] += ways[a - coin];
        return ways[amount];
    }

    // list them, to SEE the difference
    static void listCombos(int[] coins, int start, int rem, Deque<Integer> path, List<String> out) {
        if (rem == 0) { out.add(path.toString()); return; }
        for (int i = start; i < coins.length; i++)
            if (coins[i] <= rem) { path.addLast(coins[i]); listCombos(coins, i, rem - coins[i], path, out); path.removeLast(); }
    }

    static void listPerms(int[] coins, int rem, Deque<Integer> path, List<String> out) {
        if (rem == 0) { out.add(path.toString()); return; }
        for (int c : coins) if (c <= rem) { path.addLast(c); listPerms(coins, rem - c, path, out); path.removeLast(); }
    }

    // reconstruction for 0/1: walk the 2-D table backwards
    static List<Integer> chosenItems(int[] w, int[] v, int W) {
        int n = w.length;
        int[][] dp = new int[n + 1][W + 1];
        for (int i = 1; i <= n; i++)
            for (int c = 0; c <= W; c++) {
                dp[i][c] = dp[i - 1][c];
                if (w[i - 1] <= c) dp[i][c] = Math.max(dp[i][c], dp[i - 1][c - w[i - 1]] + v[i - 1]);
            }
        List<Integer> items = new ArrayList<>();
        for (int i = n, c = W; i > 0; i--)
            if (dp[i][c] != dp[i - 1][c]) { items.add(i - 1); c -= w[i - 1]; }   // value changed -> item i was taken
        Collections.reverse(items);
        return items;
    }

    public static void main(String[] args) {
        int[] w = {1, 3, 4, 5}, v = {1, 4, 5, 7};
        System.out.println("1. 0/1 knapsack  weights " + Arrays.toString(w) + " values " + Arrays.toString(v) + " W=7");
        check("2-D table", 9, knapsack2D(w, v, 7));
        check("1-D, backwards", 9, knapsack1D(w, v, 7));
        check("items taken (3+4 -> 4+5)", "[1, 2]", chosenItems(w, v, 7));

        System.out.println("2. The forward-loop bug = unbounded knapsack");
        check("1-D forwards (item reused!)", 9, knapsackForwardBug(new int[]{3}, new int[]{3}, 9));
        check("0/1 answer for one item", 3, knapsack1D(new int[]{3}, new int[]{3}, 9));

        Random rnd = new Random(1);
        boolean ok01 = true, okUnb = true;
        for (int t = 0; t < 400; t++) {
            int n = 1 + rnd.nextInt(8), W = rnd.nextInt(25);
            int[] ww = new int[n], vv = new int[n];
            for (int i = 0; i < n; i++) { ww[i] = 1 + rnd.nextInt(8); vv[i] = rnd.nextInt(15); }
            int b = brute01(ww, vv, W);
            ok01 &= knapsack2D(ww, vv, W) == b && knapsack1D(ww, vv, W) == b;
            okUnb &= unbounded(ww, vv, W) == bruteUnbounded(ww, vv, W);
        }
        check("400 random 0/1 vs all subsets", true, ok01);
        check("400 random unbounded vs recursion", true, okUnb);

        System.out.println("3. Counting: combinations vs permutations (coins 1,2,5 -> 5)");
        int[] coins = {1, 2, 5};
        List<String> combos = new ArrayList<>(), perms = new ArrayList<>();
        listCombos(coins, 0, 5, new ArrayDeque<>(), combos);
        listPerms(coins, 5, new ArrayDeque<>(), perms);
        System.out.println("   combinations: " + combos);
        System.out.println("   permutations: " + perms);
        check("coins outer loop = combinations", 4L, combinations(coins, 5));
        check("amount outer loop = permutations", 9L, permutations(coins, 5));
        check("matches the listed counts", true, combos.size() == 4 && perms.size() == 9);
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
