/*
 * ============================================================================
 *  MEDIUM 01 : LONGEST SUBSTRING WITHOUT REPEATING CHARACTERS   LeetCode #3
 *  https://leetcode.com/problems/longest-substring-without-repeating-characters/
 *  Pattern: SLIDING WINDOW (variable) - two versions: counts, and "jump" with last index
 * ============================================================================
 *
 *  PROBLEM
 *    Length of the longest SUBSTRING (contiguous) with all-distinct characters.
 *      "abcabcbb" -> 3 ("abc")      "bbbbb" -> 1      "pwwkew" -> 3 ("wke"; "pwke" is not contiguous)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every substring, check whether it's unique -> O(n^3) (O(n^2) with a set)
 *
 *  VERSION 1: SLIDING WINDOW WITH COUNTS
 *    expand right; if s[right] is now repeated inside the window, shrink from the
 *    left until it isn't. The window [left..right] always has unique characters.
 *
 *  VERSION 2: JUMP LEFT DIRECTLY (one step instead of shrinking one by one)
 *    Remember lastIndex[c] = where c was last seen. When s[right] = c was seen
 *    INSIDE the window, move left to lastIndex[c] + 1 in one jump.
 *      left = max(left, lastIndex[c] + 1)
 *    The max() matters: if c's last position is BEFORE left (already outside the
 *    window), we must not move left backwards. Example "abba": at the final 'a',
 *    lastIndex[a] = 0 but left is already 2.
 *
 *  DRY RUN version 2, "pwwkew"
 *    r=0 p  left=0          len 1
 *    r=1 w  left=0          len 2   "pw"
 *    r=2 w  seen at 1 -> left=2     len 1   "w"
 *    r=3 k                  len 2   "wk"
 *    r=4 e                  len 3   "wke"  <- best
 *    r=5 w  seen at 2 -> left=3     len 3   "kew"
 *
 *  COMPLEXITY   Time O(n)   Space O(1) (128 / 256-size arrays)
 * ============================================================================
 */

import java.util.*;

public class M01_LongestSubstringWithoutRepeating {

    // Version 1: counts + shrink
    static int lengthOfLongestSubstringCounts(String s) {
        int[] count = new int[128];
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            count[c]++;
            while (count[c] > 1) count[s.charAt(left++)]--;   // shrink until c is unique again
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // Version 2: jump with last index
    static int lengthOfLongestSubstring(String s) {
        int[] lastIndex = new int[128];
        Arrays.fill(lastIndex, -1);
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            left = Math.max(left, lastIndex[c] + 1);           // never move left backwards
            lastIndex[c] = right;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        check("abcabcbb", 3, lengthOfLongestSubstring("abcabcbb"));
        check("bbbbb", 1, lengthOfLongestSubstring("bbbbb"));
        check("pwwkew", 3, lengthOfLongestSubstring("pwwkew"));
        check("\"\" (empty)", 0, lengthOfLongestSubstring(""));
        check("abba (why max() is needed)", 2, lengthOfLongestSubstring("abba"));
        check("\"a b c\" (spaces count)", 3, lengthOfLongestSubstring("a b c"));
        check("counts version agrees", 3, lengthOfLongestSubstringCounts("pwwkew"));
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
