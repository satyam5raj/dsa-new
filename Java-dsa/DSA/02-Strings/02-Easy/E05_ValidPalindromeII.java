/*
 * ============================================================================
 *  EASY 05 : VALID PALINDROME II             LeetCode #680
 *  https://leetcode.com/problems/valid-palindrome-ii/
 *  Pattern: TWO POINTERS + ONE "SKIP" (branch once on the first mismatch)
 * ============================================================================
 *
 *  PROBLEM
 *    Can s become a palindrome by deleting AT MOST ONE character?
 *      "aba" -> true      "abca" -> true (delete 'b' or 'c')      "abc" -> false
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: try deleting each character, check for a palindrome -> O(n^2)
 *
 *  KEY INSIGHT: walk inward with two pointers. While the characters match,
 *  nothing needs deleting. At the FIRST mismatch (s[i] != s[j]), one of these
 *  two characters MUST be the one we delete (everything outside already matched).
 *  So check just two options:
 *     is s[i+1..j] a palindrome?   (delete s[i])
 *     is s[i..j-1] a palindrome?   (delete s[j])
 *  If either works -> true. No further deletions are allowed, so these checks are
 *  plain palindrome checks.
 *
 *  DRY RUN "abca"
 *    a == a -> i=1, j=2
 *    b != c -> check "c" (delete b) -> palindrome -> true
 *
 *  COMPLEXITY   Time O(n): the main scan plus at most 2 more scans   Space O(1)
 *  FOLLOW-UP: "at most k deletions" -> this branching explodes; use DP
 *  (longest palindromic subsequence >= n - k).
 * ============================================================================
 */

import java.util.*;

public class E05_ValidPalindromeII {

    static boolean validPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i) != s.charAt(j)) {
                return isPalindrome(s, i + 1, j) || isPalindrome(s, i, j - 1);   // our ONE deletion
            }
            i++;
            j--;
        }
        return true;                                      // no deletion needed
    }

    static boolean isPalindrome(String s, int i, int j) {
        while (i < j) {
            if (s.charAt(i++) != s.charAt(j--)) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        check("aba (already)", true, validPalindrome("aba"));
        check("abca (delete one)", true, validPalindrome("abca"));
        check("abc", false, validPalindrome("abc"));
        check("deeee (delete the d)", true, validPalindrome("deeee"));
        check("eeccccbebaeeabebccceea", false, validPalindrome("eeccccbebaeeabebccceea"));
        check("cupuufxoohdfpgjdmysgvhmvffcnqxjjxqncffvmhvgsymdjgpfdhooxfuupucu", true,
                validPalindrome("cupuufxoohdfpgjdmysgvhmvffcnqxjjxqncffvmhvgsymdjgpfdhooxfuupucu"));
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
