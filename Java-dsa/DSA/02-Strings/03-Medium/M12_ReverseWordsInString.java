/*
 * ============================================================================
 *  MEDIUM 12 : REVERSE WORDS IN A STRING  ⭐ bonus (classic)   LeetCode #151
 *  https://leetcode.com/problems/reverse-words-in-a-string/
 *  Pattern: SPLIT + REVERSE  /  REVERSE ALL, THEN EACH WORD (in-place on char[])
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse the ORDER of the words. Output: words separated by single spaces,
 *    with no leading/trailing spaces.
 *      "the sky is blue"     -> "blue is sky the"
 *      "  hello world  "     -> "world hello"
 *      "a good   example"    -> "example good a"
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: SPLIT + JOIN BACKWARDS                    O(n) time, O(n) space
 *    words = s.trim().split("\\s+"); append them from last to first.
 *    Clear, and perfectly fine in an interview if you mention it.
 *
 *  APPROACH 2: THE DOUBLE REVERSE (works in-place in languages with mutable strings)
 *    1. reverse the WHOLE string        "the sky"  -> "yks eht"
 *    2. reverse EACH WORD               "yks eht"  -> "sky the"
 *    3. clean up the extra spaces (a slow/fast pass: copy chars, keep one space between words)
 *    Same idea as rotating an array with three reversals (Arrays/M02).
 *
 *  DRY RUN approach 2, "  hello world  "
 *    reverse all  -> "  dlrow olleh  "
 *    reverse each -> "  world hello  "
 *    clean spaces -> "world hello"
 *
 *  COMPLEXITY   both O(n) time. Approach 2 is O(1) extra on a char[] (Java needs one copy).
 * ============================================================================
 */

import java.util.*;

public class M12_ReverseWordsInString {

    static String reverseWords(String s) {
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    static String reverseWordsInPlace(String s) {
        char[] a = s.toCharArray();
        int n = a.length;
        reverse(a, 0, n - 1);                                   // 1. whole string
        for (int i = 0; i < n; ) {                              // 2. each word
            while (i < n && a[i] == ' ') i++;
            int start = i;
            while (i < n && a[i] != ' ') i++;
            reverse(a, start, i - 1);
        }
        int w = 0;                                              // 3. squeeze spaces (slow/fast)
        for (int i = 0; i < n; i++) {
            if (a[i] != ' ') {
                a[w++] = a[i];
            } else if (w > 0 && a[w - 1] != ' ') {
                a[w++] = ' ';                                   // keep a single space after a word
            }
        }
        if (w > 0 && a[w - 1] == ' ') w--;                      // drop the trailing space
        return new String(a, 0, w);
    }

    static void reverse(char[] a, int i, int j) {
        while (i < j) { char t = a[i]; a[i++] = a[j]; a[j--] = t; }
    }

    public static void main(String[] args) {
        check("the sky is blue", "blue is sky the", reverseWords("the sky is blue"));
        check("  hello world  ", "world hello", reverseWords("  hello world  "));
        check("a good   example", "example good a", reverseWords("a good   example"));
        check("in-place: the sky is blue", "blue is sky the", reverseWordsInPlace("the sky is blue"));
        check("in-place: a good   example", "example good a", reverseWordsInPlace("  a good   example "));
        check("in-place: single word", "hi", reverseWordsInPlace("   hi "));
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
