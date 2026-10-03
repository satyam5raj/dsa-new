/*
 * ============================================================================
 *  EASY 04 : LENGTH OF LAST WORD             LeetCode #58
 *  https://leetcode.com/problems/length-of-last-word/
 *  Pattern: SCAN FROM THE END
 * ============================================================================
 *
 *  PROBLEM
 *    s has words and spaces. Return the length of the LAST word.
 *      "Hello World"                  -> 5
 *      "   fly me   to   the moon  "  -> 4      (trailing spaces!)
 *
 *  ---------------------------------------------------------------------------
 *  EASY WAY: s.trim().split(" +") and take the last element. O(n) time, but it
 *  creates an array of ALL words just to read one.
 *
 *  SCAN FROM THE END (O(1) space): the answer lives at the end, so start there.
 *    1. skip trailing spaces
 *    2. count characters until the next space (or the start of the string)
 *  We never look at the earlier words at all.
 *
 *  DRY RUN "   fly me   to   the moon  "
 *    skip 2 spaces -> at 'n'
 *    count n,o,o,m -> 4 -> hit a space -> stop
 *
 *  COMPLEXITY   Time O(length of trailing spaces + last word)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E04_LengthOfLastWord {

    static int lengthOfLastWord(String s) {
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == ' ') i--;          // 1. skip trailing spaces
        int length = 0;
        while (i >= 0 && s.charAt(i) != ' ') {             // 2. count the last word
            length++;
            i--;
        }
        return length;
    }

    public static void main(String[] args) {
        check("Hello World", 5, lengthOfLastWord("Hello World"));
        check("   fly me   to   the moon  ", 4, lengthOfLastWord("   fly me   to   the moon  "));
        check("luffy is still joyboy", 6, lengthOfLastWord("luffy is still joyboy"));
        check("a (single letter)", 1, lengthOfLastWord("a"));
        check("word   (only trailing spaces)", 4, lengthOfLastWord("word   "));
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
