/*
 * ============================================================================
 *  HARD 02 : SHORTEST PALINDROME  ⭐ bonus (KMP in action)   LeetCode #214
 *  https://leetcode.com/problems/shortest-palindrome/
 *  Pattern: KMP's LPS ARRAY on  s + '#' + reverse(s)
 * ============================================================================
 *
 *  PROBLEM
 *    Add characters ONLY AT THE FRONT of s to make it a palindrome. Return the
 *    shortest possible result.
 *      "aacecaaa" -> "aaacecaaa"     (add one 'a')
 *      "abcd"     -> "dcbabcd"
 *
 *  ---------------------------------------------------------------------------
 *  KEY OBSERVATION: find the LONGEST PALINDROMIC PREFIX of s. Everything after
 *  it must be mirrored in front:
 *      s = [ palindrome prefix ][ rest ]
 *      answer = reverse(rest) + s
 *    "aacecaaa": longest palindromic prefix "aacecaa", rest "a" -> "a" + s
 *    "abcd":     longest palindromic prefix "a",       rest "bcd" -> "dcb" + s
 *
 *  BRUTE FORCE: try each prefix length from n down, check if it's a palindrome -> O(n^2)
 *
 *  KMP TRICK -> O(n)
 *    A prefix P of s is a palindrome  <=>  P equals the corresponding SUFFIX of reverse(s).
 *    So we want the longest PREFIX of s that is also a SUFFIX of reverse(s).
 *    "Longest prefix that is also a suffix" is EXACTLY what KMP's LPS array computes!
 *    Build  combined = s + "#" + reverse(s)  and take LPS[last].
 *    (The '#' separator stops a match from running across the two halves.)
 *
 *  DRY RUN s = "abab"
 *    combined = "abab#baba"
 *    LPS of the last position = 3 ("aba" is a prefix of combined AND a suffix of it)
 *    -> the longest palindromic prefix is "aba", rest = "b" -> answer "b" + "abab" = "babab"
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H02_ShortestPalindrome {

    static String shortestPalindrome(String s) {
        String rev = new StringBuilder(s).reverse().toString();
        String combined = s + "#" + rev;
        int[] lps = buildLps(combined);
        int palPrefixLen = lps[combined.length() - 1];          // longest palindromic prefix of s
        String rest = s.substring(palPrefixLen);
        return new StringBuilder(rest).reverse() + s;
    }

    static int[] buildLps(String p) {
        int[] lps = new int[p.length()];
        for (int i = 1, len = 0; i < p.length(); ) {
            if (p.charAt(i) == p.charAt(len)) lps[i++] = ++len;
            else if (len > 0) len = lps[len - 1];
            else lps[i++] = 0;
        }
        return lps;
    }

    static String shortestPalindromeBrute(String s) {
        for (int len = s.length(); len >= 0; len--) {
            if (isPal(s, len)) return new StringBuilder(s.substring(len)).reverse() + s;
        }
        return s;
    }

    static boolean isPal(String s, int len) {
        for (int i = 0, j = len - 1; i < j; i++, j--) if (s.charAt(i) != s.charAt(j)) return false;
        return true;
    }

    public static void main(String[] args) {
        check("aacecaaa", "aaacecaaa", shortestPalindrome("aacecaaa"));
        check("abcd", "dcbabcd", shortestPalindrome("abcd"));
        check("\"\" (empty)", "", shortestPalindrome(""));
        check("racecar (already)", "racecar", shortestPalindrome("racecar"));
        check("abab", "babab", shortestPalindrome("abab"));
        Random rnd = new Random(12);
        boolean ok = true;
        for (int k = 0; k < 500; k++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = rnd.nextInt(12); i < n; i++) sb.append((char) ('a' + rnd.nextInt(2)));
            if (!shortestPalindrome(sb.toString()).equals(shortestPalindromeBrute(sb.toString()))) ok = false;
        }
        check("500 random strings vs brute force", true, ok);
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
