/*
 * ============================================================================
 *  MEDIUM 11 : PALINDROMIC SUBSTRINGS        LeetCode #647
 *  https://leetcode.com/problems/palindromic-substrings/
 *  Pattern: EXPAND AROUND CENTER (count instead of max)
 * ============================================================================
 *
 *  PROBLEM
 *    COUNT the palindromic substrings. Substrings at different positions count
 *    separately, even if they look the same.
 *      "abc" -> 3   ("a","b","c")
 *      "aaa" -> 6   ("a","a","a","aa","aa","aaa")
 *
 *  ---------------------------------------------------------------------------
 *  SAME ENGINE AS M10: from each of the 2n-1 centers, expand outward.
 *  EVERY successful expansion step is ONE more palindrome:
 *    center at the 2nd 'a' of "aaa":  "a" (+1), then "aaa" (+1)
 *  So instead of tracking the maximum length, add 1 per successful expansion.
 *
 *  DRY RUN "aaa"
 *    odd centers:  i=0: "a"                 -> 1
 *                  i=1: "a", "aaa"          -> 2
 *                  i=2: "a"                 -> 1
 *    even centers: 0|1: "aa"                -> 1
 *                  1|2: "aa"                -> 1
 *    total 6
 *
 *  COMPLEXITY   Time O(n^2) (worst case "aaaa..."; there really are ~n^2/2 palindromes)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M11_PalindromicSubstrings {

    static int countSubstrings(String s) {
        int count = 0;
        for (int center = 0; center < s.length(); center++) {
            count += expandCount(s, center, center);       // odd-length palindromes
            count += expandCount(s, center, center + 1);   // even-length palindromes
        }
        return count;
    }

    static int expandCount(String s, int l, int r) {
        int found = 0;
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
            found++;                                       // s[l..r] is a palindrome
            l--;
            r++;
        }
        return found;
    }

    public static void main(String[] args) {
        check("abc", 3, countSubstrings("abc"));
        check("aaa", 6, countSubstrings("aaa"));
        check("abba", 6, countSubstrings("abba"));   // a, b, b, a, bb, abba
        check("a", 1, countSubstrings("a"));
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
