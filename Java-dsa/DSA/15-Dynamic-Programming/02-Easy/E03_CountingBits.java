/*
 * ============================================================================
 *  EASY 03 : COUNTING BITS  ⭐                LeetCode #338
 *  https://leetcode.com/problems/counting-bits/
 *  Pattern: DP where the "smaller subproblem" is a smaller NUMBER (i >> 1, i & (i-1))
 * ============================================================================
 *
 *  PROBLEM
 *    For every i in 0..n return the number of 1 bits in i.
 *      n=2 -> [0,1,1]        n=5 -> [0,1,1,2,1,2]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: popcount each i separately -> O(n log n)
 *
 *  KEY INSIGHT: every i is a SMALLER number plus at most one bit, and the smaller
 *  number's answer is already in the table.
 *      shift:   i = (i >> 1) followed by its last bit  -> dp[i] = dp[i >> 1] + (i & 1)
 *      lowbit:  i & (i - 1) clears the lowest 1 bit    -> dp[i] = dp[i & (i - 1)] + 1
 *  Both read a strictly smaller index, so a forward loop works.
 *
 *  DRY RUN (shift)  i=5 (101): dp[2] + 1 = 1 + 1 = 2     i=6 (110): dp[3] + 0 = 2
 *
 *  COMPLEXITY   Time O(n)   Space O(1) besides the output
 * ============================================================================
 */

import java.util.*;

public class E03_CountingBits {

    static int[] countBits(int n) {
        int[] dp = new int[n + 1];
        for (int i = 1; i <= n; i++) dp[i] = dp[i >> 1] + (i & 1);     // drop the last bit, add it back
        return dp;
    }

    static int[] countBitsLowbit(int n) {
        int[] dp = new int[n + 1];
        for (int i = 1; i <= n; i++) dp[i] = dp[i & (i - 1)] + 1;      // remove the lowest set bit
        return dp;
    }

    public static void main(String[] args) {
        check("n=2", "[0, 1, 1]", countBits(2));
        check("n=5", "[0, 1, 1, 2, 1, 2]", countBits(5));
        check("n=0", "[0]", countBits(0));
        int[] a = countBits(100_000), b = countBitsLowbit(100_000);
        boolean ok = true;
        for (int i = 0; i <= 100_000; i++) ok &= a[i] == Integer.bitCount(i) && b[i] == a[i];
        check("0..100000 vs Integer.bitCount", true, ok);
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
