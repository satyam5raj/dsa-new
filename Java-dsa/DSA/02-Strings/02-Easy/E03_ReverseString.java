/*
 * ============================================================================
 *  EASY 03 : REVERSE STRING                  LeetCode #344
 *  https://leetcode.com/problems/reverse-string/
 *  Pattern: TWO POINTERS (swap from both ends)
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse a char array IN-PLACE with O(1) extra memory.
 *      ['h','e','l','l','o'] -> ['o','l','l','e','h']
 *
 *  ---------------------------------------------------------------------------
 *  TWO POINTERS: swap s[left] and s[right], move both inward, stop when they meet.
 *  Only n/2 swaps; the middle element of an odd-length array stays put.
 *
 *  DRY RUN [h,e,l,l,o]
 *    swap(0,4) -> [o,e,l,l,h]
 *    swap(1,3) -> [o,l,l,e,h]
 *    left=2, right=2 -> stop
 *
 *  RECURSIVE VERSION (shown below): same swaps, but uses O(n) STACK space.
 *  A good example of "recursion isn't free" (see the complexity folder).
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E03_ReverseString {

    static void reverseString(char[] s) {
        int left = 0, right = s.length - 1;
        while (left < right) {
            char t = s[left];
            s[left++] = s[right];
            s[right--] = t;
        }
    }

    static void reverseRecursive(char[] s, int left, int right) {
        if (left >= right) return;                     // base case: crossed or met
        char t = s[left]; s[left] = s[right]; s[right] = t;
        reverseRecursive(s, left + 1, right - 1);
    }

    static char[] run(String s) {
        char[] c = s.toCharArray();
        reverseString(c);
        return c;
    }

    public static void main(String[] args) {
        check("hello", "olleh".toCharArray(), run("hello"));
        check("Hannah", "hannaH".toCharArray(), run("Hannah"));
        check("a (single)", "a".toCharArray(), run("a"));
        check("ab (even length)", "ba".toCharArray(), run("ab"));
        char[] r = "abcde".toCharArray();
        reverseRecursive(r, 0, r.length - 1);
        check("recursive version", "edcba".toCharArray(), r);
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
