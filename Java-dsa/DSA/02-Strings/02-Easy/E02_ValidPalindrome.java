/*
 * ============================================================================
 *  EASY 02 : VALID PALINDROME                LeetCode #125
 *  https://leetcode.com/problems/valid-palindrome/
 *  Pattern: TWO POINTERS (skipping characters that don't count)
 * ============================================================================
 *
 *  PROBLEM
 *    After lowercasing and removing every non-alphanumeric character, does the
 *    string read the same forwards and backwards?
 *      "A man, a plan, a canal: Panama" -> true   ("amanaplanacanalpanama")
 *      "race a car"                     -> false
 *      " "                              -> true   (empty after cleaning)
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: build the cleaned string, then compare with its reverse
 *              O(n) time, O(n) extra space
 *
 *  APPROACH 2: TWO POINTERS ON THE ORIGINAL STRING   O(n) time, O(1) space
 *    i from the left, j from the right:
 *      skip i forward while s[i] is not a letter/digit
 *      skip j backward while s[j] is not a letter/digit
 *      compare lowercase(s[i]) and lowercase(s[j]); mismatch -> false
 *      move both inward
 *    The inner while loops also check i < j, so they can't run past each other.
 *
 *  DRY RUN "race a car"
 *    r=r, a=a, c=c, e vs a -> mismatch -> false
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E02_ValidPalindrome {

    static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;   // skip junk on the left
            while (i < j && !Character.isLetterOrDigit(s.charAt(j))) j--;   // skip junk on the right
            if (Character.toLowerCase(s.charAt(i)) != Character.toLowerCase(s.charAt(j))) return false;
            i++;
            j--;
        }
        return true;
    }

    public static void main(String[] args) {
        check("A man, a plan, a canal: Panama", true, isPalindrome("A man, a plan, a canal: Panama"));
        check("race a car", false, isPalindrome("race a car"));
        check("\" \" (only spaces)", true, isPalindrome(" "));
        check("0P (digit vs letter)", false, isPalindrome("0P"));
        check(".,a,. (punctuation around)", true, isPalindrome(".,a,."));
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
