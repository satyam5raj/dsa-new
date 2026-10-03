/*
 * ============================================================================
 *  EASY 10 : FIND THE DIFFERENCE             LeetCode #389
 *  https://leetcode.com/problems/find-the-difference/
 *  Pattern: XOR / CHARACTER SUM (cancel the common part)
 * ============================================================================
 *
 *  PROBLEM
 *    t = s shuffled, plus ONE extra letter at a random position. Find that letter.
 *      s "abcd", t "abcde" -> 'e'          s "", t "y" -> 'y'
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: COUNT  (+1 for t, -1 for s; the letter left at +1)   O(n), O(1)
 *
 *  APPROACH 2: SUM OF CHAR CODES                                  O(n), O(1)
 *    sum(t) - sum(s) = the code of the extra letter.
 *
 *  APPROACH 3: XOR (same trick as Single Number / Missing Number) O(n), O(1)
 *    XOR all characters of s AND t. Every common letter appears an even number
 *    of times and cancels (x ^ x = 0). Only the extra letter survives.
 *
 *  DRY RUN XOR, s "ab", t "bab"
 *    a ^ b ^ b ^ a ^ b = (a^a) ^ (b^b) ^ b = b
 *
 *  This "cancel what's common" idea is worth remembering: E10 in Arrays
 *  (Missing Number) and E14 (Single Number) use the same trick.
 * ============================================================================
 */

import java.util.*;

public class E10_FindTheDifference {

    static char findTheDifference(String s, String t) {
        char x = 0;
        for (int i = 0; i < s.length(); i++) x ^= s.charAt(i);
        for (int i = 0; i < t.length(); i++) x ^= t.charAt(i);
        return x;
    }

    static char findTheDifferenceSum(String s, String t) {
        int sum = 0;
        for (char c : t.toCharArray()) sum += c;
        for (char c : s.toCharArray()) sum -= c;
        return (char) sum;
    }

    public static void main(String[] args) {
        check("abcd / abcde", 'e', findTheDifference("abcd", "abcde"));
        check("\"\" / y", 'y', findTheDifference("", "y"));
        check("ab / bab (duplicate letter)", 'b', findTheDifference("ab", "bab"));
        check("sum approach", 'e', findTheDifferenceSum("abcd", "abcde"));
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
