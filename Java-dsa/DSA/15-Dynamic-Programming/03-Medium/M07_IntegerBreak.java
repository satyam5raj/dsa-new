/*
 * ============================================================================
 *  MEDIUM 07 : INTEGER BREAK                 LeetCode #343
 *  https://leetcode.com/problems/integer-break/
 *  Pattern: 1-D DP over "the first piece"  (+ the math shortcut: use 3s)
 * ============================================================================
 *
 *  PROBLEM
 *    Split n (2..58) into at least TWO positive integers to maximise their product.
 *      n=2 -> 1 (1+1)      n=10 -> 36 (3+3+4)
 *
 *  ---------------------------------------------------------------------------
 *  DP:
 *      STATE       dp[i] = best product from splitting i into >= 2 parts
 *      TRANSITION  cut off a first piece j (1..i-1); the rest i-j is either
 *                  kept whole (j * (i-j)) or split further (j * dp[i-j]):
 *                  dp[i] = max over j of  j * max(i - j, dp[i - j])
 *      The "max(i-j, dp[i-j])" is the subtle part: dp requires >= 2 parts, but the
 *      remainder may stay in one piece (dp[2] = 1, yet keeping 2 whole gives 2).
 *
 *  MATH (O(1)): use as many 3s as possible; never a 1; if remainder is 1, turn a
 *  3+1 into 2+2. Why 3? For a fixed sum, e^(1/e) peaks near e = 2.718; and any piece
 *  >= 5 is better split (2*(k-2) > k). So pieces are only 2s and 3s, and 3*3 > 2*2*2.
 *
 *  DRY RUN n=10 (math): 10 = 3+3+3+1 -> 3+3+4 -> 36
 *
 *  COMPLEXITY   DP: Time O(n^2) Space O(n)       Math: O(1) (pow is O(log n))
 * ============================================================================
 */

import java.util.*;

public class M07_IntegerBreak {

    static int integerBreak(int n) {
        int[] dp = new int[n + 1];
        dp[1] = 1;
        for (int i = 2; i <= n; i++)
            for (int j = 1; j < i; j++) dp[i] = Math.max(dp[i], j * Math.max(i - j, dp[i - j]));
        return dp[n];
    }

    static int integerBreakMath(int n) {
        if (n <= 3) return n - 1;                        // must split: 2 -> 1*1, 3 -> 1*2
        int threes = n / 3, rem = n % 3;
        if (rem == 0) return (int) Math.pow(3, threes);
        if (rem == 1) return (int) Math.pow(3, threes - 1) * 4;     // 3 + 1 -> 2 + 2
        return (int) Math.pow(3, threes) * 2;
    }

    static long brute(int n, int maxPart, boolean mustSplit) {     // every partition of n
        if (n == 0) return 1;
        long best = 0;
        for (int p = Math.min(n - (mustSplit ? 1 : 0), maxPart); p >= 1; p--) best = Math.max(best, p * brute(n - p, p, false));
        return best;
    }

    public static void main(String[] args) {
        check("n=2", 1, integerBreak(2));
        check("n=10", 36, integerBreak(10));
        check("n=58", 1549681956, integerBreak(58));
        boolean ok = true;
        for (int n = 2; n <= 58; n++) ok &= integerBreak(n) == integerBreakMath(n);
        check("n = 2..58: DP == math", true, ok);
        ok = true;
        for (int n = 2; n <= 30; n++) ok &= integerBreak(n) == brute(n, n, true);
        check("n = 2..30 vs every partition", true, ok);
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
