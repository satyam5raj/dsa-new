/*
 * ============================================================================
 *  EASY 09 : RANSOM NOTE                     LeetCode #383
 *  https://leetcode.com/problems/ransom-note/
 *  Pattern: FREQUENCY ARRAY (supply vs demand)
 * ============================================================================
 *
 *  PROBLEM
 *    Can ransomNote be built from the letters of magazine? Each magazine letter
 *    can be used ONCE.
 *      "a", "b"    -> false
 *      "aa", "ab"  -> false   (only one 'a' available)
 *      "aa", "aab" -> true
 *
 *  ---------------------------------------------------------------------------
 *  THINK "SUPPLY AND DEMAND"
 *    1. count the SUPPLY: letters available in the magazine
 *    2. for each letter the note DEMANDS: use one from the supply;
 *       if the supply drops below 0 -> impossible
 *  Early exit: a note longer than the magazine can never work.
 *
 *  DRY RUN note "aa", magazine "ab"
 *    supply: a1 b1
 *    'a' -> a0
 *    'a' -> a-1 -> false
 *
 *  Difference from Valid Anagram: anagram needs EQUAL counts; here the magazine
 *  just needs AT LEAST as many of each letter.
 *  COMPLEXITY   Time O(m + n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E09_RansomNote {

    static boolean canConstruct(String ransomNote, String magazine) {
        if (ransomNote.length() > magazine.length()) return false;
        int[] supply = new int[26];
        for (int i = 0; i < magazine.length(); i++) supply[magazine.charAt(i) - 'a']++;
        for (int i = 0; i < ransomNote.length(); i++) {
            if (--supply[ransomNote.charAt(i) - 'a'] < 0) return false;   // demand exceeds supply
        }
        return true;
    }

    public static void main(String[] args) {
        check("a / b", false, canConstruct("a", "b"));
        check("aa / ab", false, canConstruct("aa", "ab"));
        check("aa / aab", true, canConstruct("aa", "aab"));
        check("\"\" / abc (empty note)", true, canConstruct("", "abc"));
        check("abcd / abc (note longer)", false, canConstruct("abcd", "abc"));
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
