/*
 * ============================================================================
 *  EASY 05 : BACKSPACE STRING COMPARE  ⭐ bonus   LeetCode #844
 *  https://leetcode.com/problems/backspace-string-compare/
 *  Pattern: STACK simulation -> follow-up: TWO POINTERS FROM THE BACK (O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    '#' means backspace. Are s and t equal after typing them into a text editor?
 *      "ab#c", "ad#c" -> true    (both "ac")
 *      "ab##", "c#d#" -> true    (both "")
 *      "a#c",  "b"    -> false
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: STACK (StringBuilder)                    O(n + m) time, O(n + m) space
 *    letter -> push;  '#' -> pop if not empty. Then compare the two results.
 *
 *  APPROACH 2: WALK BACKWARDS                            O(n + m) time, O(1) space
 *    Reading from the END, a '#' tells us to SKIP the next letter we meet.
 *    Keep a 'skip' counter per string:
 *      '#'              -> skip++
 *      letter, skip > 0 -> skip--, ignore the letter
 *      letter, skip = 0 -> this letter survives: compare it with the other string's survivor
 *
 *  DRY RUN approach 2 on "ab##" vs "c#d#"
 *    s from the end: #,# -> skip 2; b, a skipped -> nothing left
 *    t from the end: # -> skip 1; d skipped; # -> skip 1; c skipped -> nothing left
 *    both empty -> true
 *
 *  This "stack first, then optimize away the stack" path is a common interview flow.
 * ============================================================================
 */

import java.util.*;

public class E05_BackspaceStringCompare {

    static boolean backspaceCompareStack(String s, String t) {
        return type(s).equals(type(t));
    }

    static String type(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c != '#') sb.append(c);
            else if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    static boolean backspaceCompare(String s, String t) {
        int i = s.length() - 1, j = t.length() - 1;
        while (i >= 0 || j >= 0) {
            i = nextSurvivor(s, i);
            j = nextSurvivor(t, j);
            if (i < 0 && j < 0) return true;                       // both fully consumed
            if (i < 0 || j < 0 || s.charAt(i) != t.charAt(j)) return false;
            i--;
            j--;
        }
        return true;
    }

    // index of the next character (moving left from i) that survives the backspaces, or -1
    static int nextSurvivor(String s, int i) {
        int skip = 0;
        while (i >= 0) {
            if (s.charAt(i) == '#') skip++;
            else if (skip > 0) skip--;
            else break;                                            // a surviving letter
            i--;
        }
        return i;
    }

    public static void main(String[] args) {
        check("ab#c vs ad#c", true, backspaceCompare("ab#c", "ad#c"));
        check("ab## vs c#d#", true, backspaceCompare("ab##", "c#d#"));
        check("a#c vs b", false, backspaceCompare("a#c", "b"));
        check("a##c vs #a#c (extra backspaces)", true, backspaceCompare("a##c", "#a#c"));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int k = 0; k < 2000; k++) {
            String a = rand(rnd), b = rand(rnd);
            ok &= backspaceCompare(a, b) == backspaceCompareStack(a, b);
        }
        check("2,000 random pairs: O(1) == stack version", true, ok);
        summary();
    }

    static String rand(Random r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = r.nextInt(8); i < n; i++) sb.append("ab##".charAt(r.nextInt(4)));
        return sb.toString();
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
