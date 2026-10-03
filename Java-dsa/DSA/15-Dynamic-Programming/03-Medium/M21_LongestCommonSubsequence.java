/*
 * ============================================================================
 *  MEDIUM 21 : LONGEST COMMON SUBSEQUENCE    LeetCode #1143
 *  https://leetcode.com/problems/longest-common-subsequence/
 *  Pattern: TWO-SEQUENCE DP - the template for Edit Distance, diff, regex, ...
 * ============================================================================
 *
 *  PROBLEM
 *    Length of the longest subsequence common to both strings.
 *      "abcde", "ace" -> 3        "abc", "abc" -> 3        "abc", "def" -> 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every subsequence of a (2^n), test against b.
 *
 *  KEY INSIGHT: look at the LAST characters of the two prefixes.
 *      STATE       dp[i][j] = LCS of a[0..i) and b[0..j)
 *      TRANSITION  a[i-1] == b[j-1]:  dp[i-1][j-1] + 1     (use the matching pair)
 *                  otherwise:         max(dp[i-1][j], dp[i][j-1])   (drop one of them)
 *      BASE        dp[0][*] = dp[*][0] = 0  (empty prefix)
 *      ANSWER      dp[n][m]
 *  Why is matching always safe? If the last chars are equal, some optimal LCS can
 *  use them as its last pair (any LCS can be rewired to end there).
 *
 *  The (n+1) x (m+1) table with an empty-prefix row/column is THE standard layout:
 *  it removes all "i-1 < 0" special cases.
 *
 *  SPACE: dp[i][*] needs row i-1 and the diagonal -> two rows, or one row + a saved
 *  "diagonal" variable.  RECONSTRUCTION walks back from (n, m) (shown below).
 *
 *  DRY RUN "abcde" / "ace" (rows = abcde, cols = ace)
 *         ""  a  c  e
 *     a    0  1  1  1
 *     b    0  1  1  1
 *     c    0  1  2  2
 *     d    0  1  2  2
 *     e    0  1  2  3
 *
 *  COMPLEXITY   Time O(n*m)   Space O(min(n, m)) for the length
 * ============================================================================
 */

import java.util.*;

public class M21_LongestCommonSubsequence {

    static int longestCommonSubsequence(String a, String b) {
        if (a.length() < b.length()) { String t = a; a = b; b = t; }   // row length = the shorter string
        int[] dp = new int[b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            int diag = 0;                                  // dp[i-1][j-1]
            for (int j = 1; j <= b.length(); j++) {
                int above = dp[j];                         // dp[i-1][j], about to be overwritten
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? diag + 1 : Math.max(above, dp[j - 1]);
                diag = above;
            }
        }
        return dp[b.length()];
    }

    static String oneLCS(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        StringBuilder sb = new StringBuilder();
        for (int i = n, j = m; i > 0 && j > 0; ) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        return sb.reverse().toString();
    }

    static int brute(String a, String b) {
        int best = 0;
        for (int mask = 0; mask < 1 << a.length(); mask++) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length(); i++) if ((mask >> i & 1) == 1) s.append(a.charAt(i));
            if (isSubseq(s.toString(), b)) best = Math.max(best, s.length());
        }
        return best;
    }

    static boolean isSubseq(String s, String t) {
        int i = 0;
        for (int j = 0; j < t.length() && i < s.length(); j++) if (s.charAt(i) == t.charAt(j)) i++;
        return i == s.length();
    }

    public static void main(String[] args) {
        check("abcde / ace", 3, longestCommonSubsequence("abcde", "ace"));
        check("abc / abc", 3, longestCommonSubsequence("abc", "abc"));
        check("abc / def", 0, longestCommonSubsequence("abc", "def"));
        check("the LCS itself", "ace", oneLCS("abcde", "ace"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            String a = rs(rnd, 10), b = rs(rnd, 10);
            String l = oneLCS(a, b);
            int b0 = brute(a, b);
            ok &= longestCommonSubsequence(a, b) == b0 && l.length() == b0 && isSubseq(l, a) && isSubseq(l, b);
        }
        check("500 random vs 2^n (+ path is common)", true, ok);
        summary();
    }

    static String rs(Random r, int maxLen) { StringBuilder sb = new StringBuilder(); for (int i = r.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + r.nextInt(4))); return sb.toString(); }

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
