/*
 * ============================================================================
 *  EASY 01 : VALID ANAGRAM                   LeetCode #242
 *  https://leetcode.com/problems/valid-anagram/
 *  Pattern: FREQUENCY ARRAY
 * ============================================================================
 *
 *  PROBLEM
 *    Is t an anagram of s (same letters, same counts, any order)? Lowercase letters only.
 *      "anagram", "nagaram" -> true          "rat", "car" -> false
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: SORT both and compare            O(n log n) time, O(n) space (char arrays)
 *    sorted("anagram") = "aaagmnr" = sorted("nagaram")
 *
 *  APPROACH 2: COUNT                             O(n) time, O(1) space (26 counters)
 *    count[c]++ for each char of s, count[c]-- for each char of t.
 *    Anagram <=> every counter ends at 0.
 *    Quick reject: different lengths can't be anagrams.
 *
 *  DRY RUN "rat" vs "car"
 *    +r +a +t  then  -c -a -r   -> t: +1, c: -1  -> not all zero -> false
 *
 *  FOLLOW-UP (asked often): "What if the input has Unicode characters?"
 *    -> a fixed int[26] no longer works. Use a HashMap<Character, Integer>
 *       (or int[65536] for chars). Implemented below as isAnagramUnicode.
 * ============================================================================
 */

import java.util.*;

public class E01_ValidAnagram {

    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
            count[t.charAt(i) - 'a']--;
        }
        for (int c : count) {
            if (c != 0) return false;
        }
        return true;
    }

    static boolean isAnagramSort(String s, String t) {
        char[] a = s.toCharArray(), b = t.toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    static boolean isAnagramUnicode(String s, String t) {
        if (s.length() != t.length()) return false;
        Map<Character, Integer> count = new HashMap<>();
        for (char c : s.toCharArray()) count.merge(c, 1, Integer::sum);
        for (char c : t.toCharArray()) {
            if (count.merge(c, -1, Integer::sum) < 0) return false;   // t has more of c than s
        }
        return true;
    }

    public static void main(String[] args) {
        check("anagram / nagaram", true, isAnagram("anagram", "nagaram"));
        check("rat / car", false, isAnagram("rat", "car"));
        check("a / ab (lengths differ)", false, isAnagram("a", "ab"));
        check("aacc / ccac (counts differ)", false, isAnagram("aacc", "ccac"));
        check("sort approach", true, isAnagramSort("listen", "silent"));
        check("unicode follow-up", true, isAnagramUnicode("héllo", "olléh"));
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
