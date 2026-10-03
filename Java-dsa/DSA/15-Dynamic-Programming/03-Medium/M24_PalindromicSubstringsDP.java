/*
 * ============================================================================
 *  MEDIUM 24 : PALINDROMES THE DP WAY        LeetCode #5 and #647
 *  https://leetcode.com/problems/longest-palindromic-substring/
 *  https://leetcode.com/problems/palindromic-substrings/
 *  Pattern: INTERVAL DP - pal[i][j] from the inside pal[i+1][j-1]
 * ============================================================================
 *
 *  Both problems were solved with EXPAND AROUND CENTER in Strings M10 / M11
 *  (O(n^2) time, O(1) space - still the one to use in interviews). This file is the
 *  DP view, because the palindrome table is the simplest INTERVAL DP and it's reused
 *  by harder problems (Palindrome Partitioning in Backtracking M10, min cuts, ...).
 *
 *  PROBLEMS
 *    #5   longest palindromic substring     "babad" -> "aba" (or "bab")    "cbbd" -> "bb"
 *    #647 count palindromic substrings      "abc" -> 3           "aaa" -> 6
 *
 *  ---------------------------------------------------------------------------
 *      STATE       pal[i][j] = s[i..j] is a palindrome
 *      TRANSITION  pal[i][j] = s[i] == s[j] && (j - i < 2 || pal[i+1][j-1])
 *                  "ends match AND the inside is a palindrome" (length 1-2: no inside)
 *      ORDER       pal[i][j] reads pal[i+1][j-1] - a SHORTER interval starting LATER.
 *                  So loop i from n-1 DOWN to 0 and j from i UP to n-1
 *                  (or loop by increasing length - the general interval-DP order).
 *  Every true cell is one palindromic substring (#647 = count of trues); the widest
 *  true cell is #5's answer.
 *
 *  BONUS (also interval DP): LONGEST PALINDROMIC SUBSEQUENCE (#516)
 *      lps[i][j] = s[i] == s[j] ? lps[i+1][j-1] + 2 : max(lps[i+1][j], lps[i][j-1])
 *  ...which equals LCS(s, reverse(s)) - checked below.
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M24_PalindromicSubstringsDP {

    static String longestPalindrome(String s) {
        int n = s.length(), bestL = 0, bestR = 0;
        boolean[][] pal = new boolean[n][n];
        for (int i = n - 1; i >= 0; i--)
            for (int j = i; j < n; j++) {
                pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
                if (pal[i][j] && j - i > bestR - bestL) { bestL = i; bestR = j; }
            }
        return s.substring(bestL, bestR + 1);
    }

    static int countSubstrings(String s) {
        int n = s.length(), count = 0;
        boolean[][] pal = new boolean[n][n];
        for (int i = n - 1; i >= 0; i--)
            for (int j = i; j < n; j++) {
                pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
                if (pal[i][j]) count++;
            }
        return count;
    }

    static int longestPalindromeSubseq(String s) {
        int n = s.length();
        int[][] lps = new int[n][n];
        for (int i = n - 1; i >= 0; i--) {
            lps[i][i] = 1;
            for (int j = i + 1; j < n; j++)
                lps[i][j] = s.charAt(i) == s.charAt(j) ? lps[i + 1][j - 1] + 2 : Math.max(lps[i + 1][j], lps[i][j - 1]);
        }
        return n == 0 ? 0 : lps[0][n - 1];
    }

    static int lcs(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[a.length()][b.length()];
    }

    static boolean isPal(String s, int i, int j) { while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false; return true; }

    public static void main(String[] args) {
        check("#5 babad (aba or bab valid)", "aba", longestPalindrome("babad"));
        check("#5 cbbd", "bb", longestPalindrome("cbbd"));
        check("#647 abc", 3, countSubstrings("abc"));
        check("#647 aaa", 6, countSubstrings("aaa"));
        check("#516 bbbab", 4, longestPalindromeSubseq("bbbab"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1 + rnd.nextInt(14); i > 0; i--) sb.append((char) ('a' + rnd.nextInt(3)));
            String s = sb.toString();
            int cnt = 0, best = 0;
            for (int i = 0; i < s.length(); i++) for (int j = i; j < s.length(); j++) if (isPal(s, i, j)) { cnt++; best = Math.max(best, j - i + 1); }
            String lp = longestPalindrome(s);
            ok &= countSubstrings(s) == cnt && lp.length() == best && isPal(lp, 0, lp.length() - 1)
                  && longestPalindromeSubseq(s) == lcs(s, sb.reverse().toString());
        }
        check("500 random vs checking every substring", true, ok);
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
