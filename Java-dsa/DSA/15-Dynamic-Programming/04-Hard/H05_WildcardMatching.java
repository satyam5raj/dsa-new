/*
 * ============================================================================
 *  HARD 05 : WILDCARD MATCHING               LeetCode #44
 *  https://leetcode.com/problems/wildcard-matching/
 *  Pattern: two-sequence boolean DP ('*' = any sequence)  + greedy backtracking O(1) space
 * ============================================================================
 *
 *  PROBLEM
 *    '?' matches one char, '*' matches ANY sequence (including empty). Whole-string match.
 *      "aa", "a"     -> false     "aa", "*"    -> true
 *      "cb", "?a"    -> false     "adceb", "*a*b" -> true
 *      "acdcb", "a*c?b" -> false
 *
 *  ---------------------------------------------------------------------------
 *  DIFFERENCE FROM REGEX (H04): here '*' stands alone (not "previous char repeated").
 *      STATE   dp[i][j] = s[0..i) matches p[0..j)
 *      p[j-1] == '*':  dp[i][j] = dp[i][j-1]          '*' matches nothing
 *                              || dp[i-1][j]          '*' eats s[i-1] and stays
 *      otherwise:      dp[i][j] = (p[j-1] == '?' || p[j-1] == s[i-1]) && dp[i-1][j-1]
 *      BASE    dp[0][0] = true;  dp[0][j] = dp[0][j-1] && p[j-1] == '*'
 *
 *  GREEDY ALTERNATIVE (O(1) space, usually fast): walk both strings; on '*' remember
 *  (star position, s position). On a mismatch, backtrack: let the LAST star eat one
 *  more character. Only the last star matters, because any earlier star's extra
 *  characters could equally be eaten by the later one.
 *
 *  COMPLEXITY   DP: Time O(n*m), Space O(m)    Greedy: worst O(n*m), Space O(1)
 * ============================================================================
 */

import java.util.*;
import java.util.regex.Pattern;

public class H05_WildcardMatching {

    static boolean isMatch(String s, String p) {
        int m = p.length();
        boolean[] dp = new boolean[m + 1];                // row i-1, overwritten into row i
        dp[0] = true;
        for (int j = 1; j <= m; j++) dp[j] = dp[j - 1] && p.charAt(j - 1) == '*';
        for (int i = 1; i <= s.length(); i++) {
            boolean diag = dp[0];                         // dp[i-1][0]
            dp[0] = false;                                // non-empty s vs empty pattern
            for (int j = 1; j <= m; j++) {
                boolean up = dp[j];                       // dp[i-1][j]
                char pc = p.charAt(j - 1);
                if (pc == '*') dp[j] = dp[j - 1] || up;   // empty match || eat one char
                else dp[j] = (pc == '?' || pc == s.charAt(i - 1)) && diag;
                diag = up;
            }
        }
        return dp[m];
    }

    static boolean isMatchGreedy(String s, String p) {
        int i = 0, j = 0, star = -1, mark = 0;
        while (i < s.length()) {
            if (j < p.length() && (p.charAt(j) == '?' || p.charAt(j) == s.charAt(i))) { i++; j++; }
            else if (j < p.length() && p.charAt(j) == '*') { star = j++; mark = i; }      // star matches "" for now
            else if (star != -1) { j = star + 1; i = ++mark; }                             // last star eats one more
            else return false;
        }
        while (j < p.length() && p.charAt(j) == '*') j++;
        return j == p.length();
    }

    public static void main(String[] args) {
        check("aa / a", false, isMatch("aa", "a"));
        check("aa / *", true, isMatch("aa", "*"));
        check("cb / ?a", false, isMatch("cb", "?a"));
        check("adceb / *a*b", true, isMatch("adceb", "*a*b"));
        check("acdcb / a*c?b", false, isMatch("acdcb", "a*c?b"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 3000; t++) {
            StringBuilder s = new StringBuilder(), p = new StringBuilder();
            for (int i = rnd.nextInt(8); i > 0; i--) s.append((char) ('a' + rnd.nextInt(2)));
            for (int i = rnd.nextInt(7); i > 0; i--) { int k = rnd.nextInt(4); p.append(k == 0 ? '*' : k == 1 ? '?' : (char) ('a' + rnd.nextInt(2))); }
            String regex = p.toString().replace("?", ".").replace("*", ".*");             // translate to java.util.regex
            boolean exp = Pattern.matches(regex, s.toString());
            ok &= isMatch(s.toString(), p.toString()) == exp && isMatchGreedy(s.toString(), p.toString()) == exp;
        }
        check("3000 random: DP == greedy == regex", true, ok);
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
