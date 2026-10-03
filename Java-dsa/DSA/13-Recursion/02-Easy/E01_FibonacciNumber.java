/*
 * ============================================================================
 *  EASY 01 : FIBONACCI NUMBER  ⭐ bonus       LeetCode #509
 *  https://leetcode.com/problems/fibonacci-number/
 *  Pattern: TREE RECURSION -> MEMOIZATION -> BOTTOM-UP (the road to DP)
 * ============================================================================
 *
 *  PROBLEM
 *    F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2). Return F(n).     F(10) = 55
 *
 *  ---------------------------------------------------------------------------
 *  FOUR VERSIONS - the same evolution you'll do for every DP problem (topic 15):
 *
 *  1. PLAIN RECURSION: direct translation of the definition.
 *     The call tree recomputes F(n-2) inside F(n-1), F(n-3) many times... O(phi^n).
 *                 F(5)
 *              /        \
 *           F(4)        F(3)        <- F(3) computed twice, F(2) three times...
 *          /    \       /   \
 *        F(3)  F(2)   F(2)  F(1)
 *
 *  2. MEMOIZATION (top-down): same recursion, but store each answer the first time
 *     and return it immediately afterwards. Every F(k) computed once -> O(n).
 *
 *  3. TABULATION (bottom-up): fill dp[0..n] in order. No recursion, no stack. O(n).
 *
 *  4. TWO VARIABLES: dp[i] only needs dp[i-1] and dp[i-2] -> O(1) space.
 *
 *  (Even faster: matrix exponentiation or the fast-doubling formulas, O(log n).)
 *
 *  COMPLEXITY   plain O(phi^n) / memo O(n) time + O(n) space / iterative O(n) time, O(1) space
 * ============================================================================
 */

import java.util.*;

public class E01_FibonacciNumber {

    static int fibPlain(int n) {
        if (n <= 1) return n;
        return fibPlain(n - 1) + fibPlain(n - 2);
    }

    static int fibMemo(int n) { return memo(n, new Integer[n + 1]); }

    static int memo(int n, Integer[] cache) {
        if (n <= 1) return n;
        if (cache[n] != null) return cache[n];                  // already solved
        return cache[n] = memo(n - 1, cache) + memo(n - 2, cache);
    }

    static int fibTable(int n) {
        if (n <= 1) return n;
        int[] dp = new int[n + 1];
        dp[1] = 1;
        for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
        return dp[n];
    }

    static int fib(int n) {
        int prev = 0, cur = 1;
        if (n == 0) return 0;
        for (int i = 2; i <= n; i++) { int next = prev + cur; prev = cur; cur = next; }
        return cur;
    }

    public static void main(String[] args) {
        check("F(2)", 1, fib(2));
        check("F(10)", 55, fib(10));
        check("F(30)", 832040, fib(30));
        check("F(0)", 0, fib(0));
        boolean ok = true;
        for (int n = 0; n <= 30; n++) ok &= fibPlain(n) == fibMemo(n) && fibMemo(n) == fibTable(n) && fibTable(n) == fib(n);
        check("n = 0..30: all four versions agree", true, ok);
        long t0 = System.nanoTime();
        fibPlain(35);
        long plain = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        fibMemo(35);
        long memo = (System.nanoTime() - t0) / 1000;
        System.out.printf("   F(35): plain recursion %d ms, memoized %d us%n", plain, memo);
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
