/*
 * ============================================================================
 *  EASY 02 : REVERSE STRING  ⭐ bonus          LeetCode #344
 *  https://leetcode.com/problems/reverse-string/
 *  Pattern: recursion on TWO INDICES (shrink the range from both ends)
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse a char[] in place.   ['h','e','l','l','o'] -> ['o','l','l','e','h']
 *
 *  ---------------------------------------------------------------------------
 *  RECURSIVE THINKING: "reverse s[lo..hi]" =
 *      swap the two ends, then reverse s[lo+1..hi-1]
 *  Base case: lo >= hi (0 or 1 characters left: already reversed).
 *
 *  The same "two indices moving inward" shape as the two-pointer loop (Arrays) -
 *  every two-pointer loop can be written as recursion and vice versa. The loop is
 *  better in practice: O(1) space instead of O(n/2) stack frames.
 *
 *  DRY RUN "hello": swap(0,4) "oellh" -> swap(1,3) "olleh" -> lo == hi = 2: stop
 *
 *  COMPLEXITY   Time O(n)   Space O(n) recursion stack (O(1) with the loop)
 * ============================================================================
 */

import java.util.*;

public class E02_ReverseString {

    static void reverseString(char[] s) { reverse(s, 0, s.length - 1); }

    static void reverse(char[] s, int lo, int hi) {
        if (lo >= hi) return;                         // base case
        char t = s[lo]; s[lo] = s[hi]; s[hi] = t;
        reverse(s, lo + 1, hi - 1);                   // the smaller problem
    }

    public static void main(String[] args) {
        char[] a = "hello".toCharArray();
        reverseString(a);
        check("hello", "olleh", new String(a));
        char[] b = "Hannah".toCharArray();
        reverseString(b);
        check("Hannah", "hannaH", new String(b));
        char[] c = "x".toCharArray();
        reverseString(c);
        check("single char", "x", new String(c));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rnd.nextInt(40); i++) sb.append((char) ('a' + rnd.nextInt(26)));
            char[] s = sb.toString().toCharArray();
            reverseString(s);
            ok &= new String(s).equals(sb.reverse().toString());
        }
        check("500 random strings vs StringBuilder.reverse", true, ok);
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
