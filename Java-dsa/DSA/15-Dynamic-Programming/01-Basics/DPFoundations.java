/*
 * ============================================================================
 *  DP FOUNDATIONS: recursion -> memoization -> tabulation -> space optimization
 * ============================================================================
 *
 *  WHAT DP IS
 *    Dynamic Programming = recursion where the SAME subproblem shows up many times,
 *    so you solve each one ONCE and store the answer. It needs two properties:
 *      1. OPTIMAL SUBSTRUCTURE      the answer is built from answers to smaller
 *                                   versions of the same problem
 *      2. OVERLAPPING SUBPROBLEMS   the recursion tree repeats subproblems
 *                                   (if it never repeats -> divide & conquer, no table)
 *
 *  THE 5-STEP RECIPE (say this out loud in interviews)
 *    1. STATE       what does dp[i] (or dp[i][j]) MEAN, in one English sentence?
 *                   "dp[i] = number of ways to reach step i"
 *    2. TRANSITION  how is a state built from smaller states?
 *                   dp[i] = dp[i-1] + dp[i-2]
 *    3. BASE CASES  the smallest states you know directly: dp[0] = 1, dp[1] = 1
 *    4. ORDER       fill so every state's inputs are ready first (i increasing)
 *    5. ANSWER      which cell holds the answer? dp[n]
 *
 *  THE 4 VERSIONS OF THE SAME SOLUTION (this file runs all of them)
 *    A. plain recursion         O(2^n)  - the "brute force" you derive the state from
 *    B. memoization (top-down)  O(n)    - same recursion + a cache. Easiest to write:
 *                                         just add "if cached return; ... store"
 *    C. tabulation (bottom-up)  O(n)    - a loop filling the table in dependency order;
 *                                         no recursion -> no stack overflow
 *    D. space optimized         O(1)    - keep only the states the transition reads
 *                                         (here: the last two)
 *
 *  TOP-DOWN vs BOTTOM-UP
 *    top-down  + only visits the states it actually needs (good for sparse states)
 *              + mirrors the recurrence, so it's hard to get wrong
 *              - recursion depth (StackOverflowError around depth ~10^4..10^5)
 *              - call + hash-map overhead
 *    bottom-up + fast, no stack, and it enables space optimization
 *              - you must work out the fill ORDER yourself
 *
 *  MEMO PITFALLS
 *    - a sentinel that is also a valid answer: memo filled with 0, and 0 is a real
 *      answer, means it recomputes forever. Use -1, or Integer[] with null = unknown.
 *    - non-int / multi-part states: use a long key like i * W + j, or a HashMap.
 *
 *  RECONSTRUCTION: the table gives the best VALUE. To get the CHOICES, either store a
 *  "parent" pointer per state or walk the table backwards, re-checking which
 *  transition produced each value (shown in part 4).
 *
 *  Run:  java DPFoundations.java
 * ============================================================================
 */

import java.util.*;

public class DPFoundations {

    // ---------- the example problem: ways to climb n stairs taking 1, 2 or 3 steps ----------
    static long calls;

    // A. plain recursion: ways(n) = ways(n-1) + ways(n-2) + ways(n-3)
    static long plain(int n) {
        calls++;
        if (n < 0) return 0;
        if (n == 0) return 1;                            // one way to stand still: do nothing
        return plain(n - 1) + plain(n - 2) + plain(n - 3);
    }

    // B. memoization: identical, plus a cache (null = not computed yet)
    static long memo(int n, Long[] cache) {
        calls++;
        if (n < 0) return 0;
        if (n == 0) return 1;
        if (cache[n] != null) return cache[n];
        return cache[n] = memo(n - 1, cache) + memo(n - 2, cache) + memo(n - 3, cache);
    }

    // C. tabulation: dp[i] = ways to reach step i, filled from small i upward
    static long table(int n) {
        long[] dp = new long[n + 1];
        dp[0] = 1;
        for (int i = 1; i <= n; i++) {
            dp[i] = dp[i - 1];
            if (i >= 2) dp[i] += dp[i - 2];
            if (i >= 3) dp[i] += dp[i - 3];
        }
        return dp[n];
    }

    // D. space optimized: dp[i] reads only the previous 3 values -> keep 3 variables
    static long rolling(int n) {
        long a = 0, b = 0, c = 1;                        // dp[i-3], dp[i-2], dp[i-1]; start: dp[0] = 1
        for (int i = 1; i <= n; i++) {
            long next = a + b + c;
            a = b; b = c; c = next;
        }
        return c;
    }

    // ---------- part 3: why a wrong sentinel hurts ----------
    // Ways to write n as an ordered sum of 2s and 4s. For ODD n the answer is 0, and so
    // is every state it reaches -> with 0 as "unknown" nothing is ever treated as cached.
    static long sentinelCalls;

    static long badSentinel(int n, long[] memo) {        // memo filled with 0 = "unknown" - but 0 is a real answer!
        sentinelCalls++;
        if (n == 0) return 1;
        if (n < 0) return 0;
        if (memo[n] != 0) return memo[n];
        return memo[n] = badSentinel(n - 2, memo) + badSentinel(n - 4, memo);
    }

    static long goodSentinel(int n, long[] memo) {       // filled with -1 = unknown
        sentinelCalls++;
        if (n == 0) return 1;
        if (n < 0) return 0;
        if (memo[n] != -1) return memo[n];
        return memo[n] = goodSentinel(n - 2, memo) + goodSentinel(n - 4, memo);
    }

    // ---------- part 4: reconstruction - min-cost stairs, and WHICH steps were used ----------
    // cost[i] is paid when you stand on step i; start at 0 or 1, reach the top (index n).
    static List<Integer> cheapestPath(int[] cost) {
        int n = cost.length;
        int[] dp = new int[n + 1];                       // dp[i] = min cost to stand at i (top = n is free)
        int[] parent = new int[n + 1];
        Arrays.fill(parent, -1);
        for (int i = 2; i <= n; i++) {
            int fromOne = dp[i - 1] + cost[i - 1], fromTwo = dp[i - 2] + cost[i - 2];
            if (fromOne <= fromTwo) { dp[i] = fromOne; parent[i] = i - 1; }
            else { dp[i] = fromTwo; parent[i] = i - 2; }
        }
        LinkedList<Integer> path = new LinkedList<>();
        for (int at = parent[n]; at != -1; at = parent[at]) path.addFirst(at);   // follow parents back
        return path;
    }

    static int pathCost(int[] cost, List<Integer> path) { int s = 0; for (int i : path) s += cost[i]; return s; }

    // brute force min cost: try every jump sequence
    static int bruteMin(int[] cost, int i) {
        if (i >= cost.length) return 0;
        return cost[i] + Math.min(bruteMin(cost, i + 1), bruteMin(cost, i + 2));
    }

    public static void main(String[] args) {
        System.out.println("1. Same answer, four ways (n = 25)");
        calls = 0;
        long a = plain(25);
        long plainCalls = calls;
        calls = 0;
        long b = memo(25, new Long[26]);
        long memoCalls = calls;
        System.out.printf("   plain recursion: %,d calls     memoized: %,d calls%n", plainCalls, memoCalls);
        check("plain == memo", a, b);
        check("memo == table == rolling", true, b == table(25) && b == rolling(25));
        check("memo makes < 100 calls", true, memoCalls < 100);
        boolean ok = true;
        for (int n = 0; n <= 20; n++) ok &= plain(n) == table(n) && table(n) == rolling(n);
        check("n = 0..20 all versions agree", true, ok);

        System.out.println("2. Top-down recursion depth limit vs bottom-up");
        boolean overflowed = false;
        try { memo(200_000, new Long[200_001]); } catch (StackOverflowError e) { overflowed = true; }
        System.out.println("   memo(200,000) -> " + (overflowed ? "StackOverflowError" : "ok") + "    table(200,000) fine (overflowing long, but no crash)");
        check("deep memo recursion overflows the stack", true, overflowed);
        table(200_000);

        System.out.println("3. Memo sentinel pitfall (ways to write 61 with 2s and 4s = 0)");
        long[] bad = new long[62], good = new long[62];
        Arrays.fill(good, -1);
        sentinelCalls = 0;
        long x = badSentinel(61, bad);
        long badCalls = sentinelCalls;
        sentinelCalls = 0;
        long y = goodSentinel(61, good);
        System.out.printf("   0 as 'unknown': %,d calls     -1 as 'unknown': %,d calls%n", badCalls, sentinelCalls);
        check("same answer", x, y);
        check("answer is 0", 0L, y);
        check("bad sentinel: exponential recomputation", true, badCalls > 1000 * sentinelCalls);

        System.out.println("4. Reconstruction with parent pointers (min cost climbing stairs)");
        int[] cost = {1, 100, 1, 1, 1, 100, 1, 1, 100, 1};
        List<Integer> path = cheapestPath(cost);
        check("steps used", "[0, 2, 4, 6, 7, 9]", path);
        check("their cost = the minimum", 6, pathCost(cost, path));
        Random rnd = new Random(1);
        ok = true;
        for (int t = 0; t < 300; t++) {
            int[] c = new int[2 + rnd.nextInt(12)];
            for (int i = 0; i < c.length; i++) c[i] = rnd.nextInt(20);
            ok &= pathCost(c, cheapestPath(c)) == Math.min(bruteMin(c, 0), bruteMin(c, 1));
        }
        check("300 random: path cost == brute minimum", true, ok);
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
