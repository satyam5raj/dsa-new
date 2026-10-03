/*
 * ============================================================================
 *  MEDIUM 10 : LONGEST PALINDROMIC SUBSTRING   LeetCode #5
 *  https://leetcode.com/problems/longest-palindromic-substring/
 *  Pattern: EXPAND AROUND CENTER  (also: DP, and Manacher's O(n) for the curious)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the longest substring that is a palindrome.
 *      "babad" -> "bab" ("aba" is also valid)        "cbbd" -> "bb"
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every substring (n^2), check it (n) -> O(n^3)
 *
 *  APPROACH 1: DYNAMIC PROGRAMMING                       O(n^2) time, O(n^2) space
 *    pal[i][j] = (s[i] == s[j]) && (j - i < 2 || pal[i+1][j-1])
 *    "a substring is a palindrome if its ends match AND its inside is a palindrome"
 *    Fill by increasing length. (Covered properly in the DP topic.)
 *
 *  APPROACH 2: EXPAND AROUND CENTER                      O(n^2) time, O(1) space  <- use this
 *    Every palindrome mirrors around a CENTER:
 *      odd length  "aba"  -> the center is a character
 *      even length "abba" -> the center is the GAP between two characters
 *    So there are 2n-1 centers. From each, expand while s[l] == s[r].
 *    Track the longest found.
 *
 *  DRY RUN "cbbd"
 *    center 'c'     -> "c" (1)
 *    gap c|b        -> c != b -> 0
 *    center 'b'(1)  -> "b"
 *    gap b|b        -> b == b -> "bb"; expand: c vs d -> stop -> length 2  <- best
 *    ...
 *
 *  BONUS: Manacher's algorithm finds it in O(n) by reusing mirror information
 *  (similar in spirit to the Z-algorithm). Rarely expected in interviews.
 * ============================================================================
 */

import java.util.*;

public class M10_LongestPalindromicSubstring {

    static String longestPalindrome(String s) {
        if (s.isEmpty()) return "";
        int bestStart = 0, bestLen = 1;
        for (int center = 0; center < s.length(); center++) {
            int odd = expand(s, center, center);          // "aba"
            int even = expand(s, center, center + 1);     // "abba"
            int len = Math.max(odd, even);
            if (len > bestLen) {
                bestLen = len;
                bestStart = center - (len - 1) / 2;       // works for both odd and even lengths
            }
        }
        return s.substring(bestStart, bestStart + bestLen);
    }

    // returns the length of the longest palindrome centered between l and r
    static int expand(String s, int l, int r) {
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
            l--;
            r++;
        }
        return r - l - 1;                                 // the loop overshot by one on each side
    }

    static String longestPalindromeDp(String s) {
        int n = s.length(), start = 0, best = n == 0 ? 0 : 1;
        boolean[][] pal = new boolean[n][n];
        for (int len = 1; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                pal[i][j] = s.charAt(i) == s.charAt(j) && (len <= 2 || pal[i + 1][j - 1]);
                if (pal[i][j] && len > best) { best = len; start = i; }
            }
        }
        return s.substring(start, start + best);
    }

    public static void main(String[] args) {
        String r = longestPalindrome("babad");
        check("babad -> bab or aba", true, r.equals("bab") || r.equals("aba"));
        check("cbbd", "bb", longestPalindrome("cbbd"));
        check("a", "a", longestPalindrome("a"));
        check("forgeeksskeegfor", "geeksskeeg", longestPalindrome("forgeeksskeegfor"));
        check("racecar (whole string)", "racecar", longestPalindrome("racecar"));
        check("DP approach agrees", "geeksskeeg", longestPalindromeDp("forgeeksskeegfor"));
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
