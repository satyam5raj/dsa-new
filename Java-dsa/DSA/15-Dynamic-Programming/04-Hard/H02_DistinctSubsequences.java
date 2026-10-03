/*
 * ============================================================================
 *  HARD 02 : DISTINCT SUBSEQUENCES           LeetCode #115
 *  https://leetcode.com/problems/distinct-subsequences/
 *  Pattern: two-sequence COUNTING DP (LCS's table, but sums instead of max)
 * ============================================================================
 *
 *  PROBLEM
 *    Count the distinct ways (sets of positions) to pick t as a subsequence of s.
 *      s="rabbbit", t="rabbit"  -> 3   (drop any one of the three b's)
 *      s="babgbag", t="bag"     -> 5
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: choose |t| positions of s -> C(n, m), exponential.
 *
 *  KEY INSIGHT: look at s's LAST character.
 *      STATE       dp[i][j] = ways to form t[0..j) from s[0..i)
 *      TRANSITION  dp[i][j] = dp[i-1][j]                         don't use s[i-1]
 *                           + (s[i-1] == t[j-1] ? dp[i-1][j-1] : 0)   use it as t's last char
 *      BASE        dp[i][0] = 1 (empty t: one way, pick nothing);   dp[0][j>0] = 0
 *  ONE ROW: iterate j DOWNWARD so dp[j-1] still holds row i-1 (same reason as 0/1
 *  knapsack - each s character is used at most once per way).
 *
 *  OVERFLOW: LeetCode promises the answer fits in 32 bits, but intermediate cells may
 *  not; use long (or note that LeetCode's tests let int wrap harmlessly).
 *
 *  DRY RUN "babgbag" / "bag"   dp over j = ["", b, a, g] starts [1,0,0,0]
 *    b:[1,1,0,0] a:[1,1,1,0] b:[1,2,1,0] g:[1,2,1,1] b:[1,3,1,1] a:[1,3,4,1] g:[1,3,4,5]
 *
 *  COMPLEXITY   Time O(n*m)   Space O(m)
 * ============================================================================
 */

import java.util.*;

public class H02_DistinctSubsequences {

    static int numDistinct(String s, String t) {
        int m = t.length();
        long[] dp = new long[m + 1];
        dp[0] = 1;
        for (int i = 1; i <= s.length(); i++)
            for (int j = Math.min(i, m); j >= 1; j--)               // downward: dp[j-1] is still row i-1
                if (s.charAt(i - 1) == t.charAt(j - 1)) dp[j] += dp[j - 1];
        return (int) dp[m];
    }

    static int brute(String s, String t) {                         // every subset of positions
        int count = 0;
        for (int mask = 0; mask < 1 << s.length(); mask++) {
            if (Integer.bitCount(mask) != t.length()) continue;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) if ((mask >> i & 1) == 1) sb.append(s.charAt(i));
            if (sb.toString().equals(t)) count++;
        }
        return count;
    }

    public static void main(String[] args) {
        check("rabbbit / rabbit", 3, numDistinct("rabbbit", "rabbit"));
        check("babgbag / bag", 5, numDistinct("babgbag", "bag"));
        check("abc / \"\"", 1, numDistinct("abc", ""));
        check("a / aa", 0, numDistinct("a", "aa"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            String s = rs(rnd, 13), tt = rs(rnd, 4);
            ok &= numDistinct(s, tt) == brute(s, tt);
        }
        check("500 random vs every position subset", true, ok);
        summary();
    }

    static String rs(Random r, int maxLen) { StringBuilder sb = new StringBuilder(); for (int i = r.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + r.nextInt(2))); return sb.toString(); }

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
