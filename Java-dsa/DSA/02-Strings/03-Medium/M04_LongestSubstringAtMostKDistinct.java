/*
 * ============================================================================
 *  MEDIUM 04 : LONGEST SUBSTRING WITH AT MOST K DISTINCT CHARACTERS   LeetCode #340
 *  https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/
 *  (LeetCode Premium; the same as #159 for k = 2)
 *  Pattern: SLIDING WINDOW + COUNTS + "distinct" counter
 * ============================================================================
 *
 *  PROBLEM
 *    Length of the longest substring with AT MOST k distinct characters.
 *      "eceba", k=2 -> 3   ("ece")
 *      "aa",    k=1 -> 2
 *
 *  ---------------------------------------------------------------------------
 *  This is Arrays/M17 (Fruit Into Baskets) on characters. Instead of a HashMap
 *  and map.size(), use an int[128] plus a 'distinct' counter:
 *    adding c:   if count[c] was 0 -> distinct++
 *    removing c: if count[c] becomes 0 -> distinct--
 *  This avoids HashMap overhead and the "forgot to remove the zero key" bug.
 *
 *  TEMPLATE
 *    for right: add s[right]
 *      while distinct > k: remove s[left], left++
 *      best = max(best, right - left + 1)
 *
 *  DRY RUN "eceba", k=2
 *    e       distinct 1  len 1
 *    ec      distinct 2  len 2
 *    ece     distinct 2  len 3   <- best
 *    eceb    distinct 3 -> drop e,c -> "eb"   len 2
 *    eba     distinct 3 -> drop e -> "ba"     len 2
 *
 *  EDGE: k = 0 -> 0 (no character is allowed).
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M04_LongestSubstringAtMostKDistinct {

    static int lengthOfLongestSubstringKDistinct(String s, int k) {
        int[] count = new int[128];
        int left = 0, distinct = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            if (count[s.charAt(right)]++ == 0) distinct++;       // a new character entered
            while (distinct > k) {
                if (--count[s.charAt(left)] == 0) distinct--;    // a character fully left
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        check("eceba k=2", 3, lengthOfLongestSubstringKDistinct("eceba", 2));
        check("aa k=1", 2, lengthOfLongestSubstringKDistinct("aa", 1));
        check("abcadcacacaca k=3", 11, lengthOfLongestSubstringKDistinct("abcadcacacaca", 3));
        check("abc k=0", 0, lengthOfLongestSubstringKDistinct("abc", 0));
        check("abc k=5 (k > distinct)", 3, lengthOfLongestSubstringKDistinct("abc", 5));
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
