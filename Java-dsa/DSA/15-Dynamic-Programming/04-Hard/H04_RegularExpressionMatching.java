/*
 * ============================================================================
 *  HARD 04 : REGULAR EXPRESSION MATCHING     LeetCode #10
 *  https://leetcode.com/problems/regular-expression-matching/
 *  Pattern: two-sequence BOOLEAN DP where '*' means "zero or more of the previous"
 * ============================================================================
 *
 *  PROBLEM
 *    '.' matches any single char; 'x*' matches zero or more x. The match must cover
 *    the WHOLE string.
 *      "aa", "a"      -> false        "aa", "a*"    -> true
 *      "ab", ".*"     -> true         "aab", "c*a*b" -> true
 *      "mississippi", "mis*is*p*." -> false
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: treat "x*" as ONE unit. Look at the end of the pattern.
 *      STATE   dp[i][j] = s[0..i) matches p[0..j)
 *      p[j-1] is a normal char or '.':
 *              dp[i][j] = i > 0 && matches(s[i-1], p[j-1]) && dp[i-1][j-1]
 *      p[j-1] == '*' (unit = p[j-2] followed by *):
 *              ZERO copies:   dp[i][j-2]                          (drop "x*")
 *              ONE MORE copy: i > 0 && matches(s[i-1], p[j-2]) && dp[i-1][j]
 *                             (eat one char of s, KEEP "x*" for more)
 *      BASE    dp[0][0] = true;  dp[0][j] = p[j-1] == '*' && dp[0][j-2]  ("a*b*" matches "")
 *  The "dp[i-1][j]" (stay on the same pattern column) is what makes * repeat.
 *
 *  DRY RUN "aab", "c*a*b"
 *    "" matches "c*", "c*a*"  (zero copies)
 *    "a" matches "c*a*" (one a), "aa" matches "c*a*" (dp[1][4] -> one more)
 *    "aab" matches "c*a*b" (b == b and dp[2][4])  -> true
 *
 *  COMPLEXITY   Time O(n*m)   Space O(n*m)  (O(m) with two rows)
 * ============================================================================
 */

import java.util.*;
import java.util.regex.Pattern;

public class H04_RegularExpressionMatching {

    static boolean isMatch(String s, String p) {
        int n = s.length(), m = p.length();
        boolean[][] dp = new boolean[n + 1][m + 1];
        dp[0][0] = true;
        for (int j = 2; j <= m; j++) dp[0][j] = p.charAt(j - 1) == '*' && dp[0][j - 2];
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    dp[i][j] = dp[i][j - 2]                                           // zero copies of p[j-2]
                            || (matches(s.charAt(i - 1), p.charAt(j - 2)) && dp[i - 1][j]);   // one more copy
                } else {
                    dp[i][j] = matches(s.charAt(i - 1), pc) && dp[i - 1][j - 1];
                }
            }
        return dp[n][m];
    }

    static boolean matches(char sc, char pc) { return pc == '.' || pc == sc; }

    public static void main(String[] args) {
        check("aa / a", false, isMatch("aa", "a"));
        check("aa / a*", true, isMatch("aa", "a*"));
        check("ab / .*", true, isMatch("ab", ".*"));
        check("aab / c*a*b", true, isMatch("aab", "c*a*b"));
        check("mississippi / mis*is*p*.", false, isMatch("mississippi", "mis*is*p*."));
        check("\"\" / a*b*", true, isMatch("", "a*b*"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 3000; t++) {
            StringBuilder s = new StringBuilder(), p = new StringBuilder();
            for (int i = rnd.nextInt(7); i > 0; i--) s.append((char) ('a' + rnd.nextInt(2)));
            for (int i = rnd.nextInt(6); i > 0; i--) {
                int k = rnd.nextInt(3);
                p.append(k == 0 ? '.' : (char) ('a' + rnd.nextInt(2)));
                if (rnd.nextInt(3) == 0) p.append('*');                         // '*' always follows a char
            }
            ok &= isMatch(s.toString(), p.toString()) == Pattern.matches(p.toString(), s.toString());
        }
        check("3000 random vs java.util.regex", true, ok);
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
