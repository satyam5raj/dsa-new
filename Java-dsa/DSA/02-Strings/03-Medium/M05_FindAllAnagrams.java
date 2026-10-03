/*
 * ============================================================================
 *  MEDIUM 05 : FIND ALL ANAGRAMS IN A STRING   LeetCode #438
 *  https://leetcode.com/problems/find-all-anagrams-in-a-string/
 *  Pattern: FIXED-SIZE SLIDING WINDOW + FREQUENCY (M03 collecting every match)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the start indices of every anagram of p inside s.
 *      s "cbaebabacd", p "abc" -> [0, 6]     ("cba" at 0, "bac" at 6)
 *      s "abab",       p "ab"  -> [0, 1, 2]  (windows may OVERLAP)
 *
 *  ---------------------------------------------------------------------------
 *  Same as Permutation in String (M03), but don't stop at the first match:
 *  record the window's start index every time the counts match.
 *
 *  This version uses a single 'need' array plus a counter of how many
 *  characters are still missing:
 *    need[c] = how many more c's the window needs (starts as p's counts)
 *    missing = total characters still missing (starts as |p|)
 *    char enters: if need[c] > 0 -> missing--;  need[c]--
 *    char leaves: need[c]++;  if need[c] > 0 -> missing++
 *    missing == 0 with window size |p|  ->  an anagram
 *  (need[c] can go NEGATIVE: the window has extra c's. That's fine.)
 *
 *  DRY RUN s "abab", p "ab"   need {a:1,b:1}, missing 2
 *    r=0 a: missing 1
 *    r=1 b: missing 0 -> window [0..1] -> record 0
 *    r=2 a enters (need a = 0 -> -1, no change); a leaves (need a -> 0, not > 0) -> missing 0 -> record 1
 *    r=3 b enters / b leaves similarly -> record 2
 *
 *  COMPLEXITY   Time O(n + m)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M05_FindAllAnagrams {

    static List<Integer> findAnagrams(String s, String p) {
        List<Integer> result = new ArrayList<>();
        int m = p.length();
        if (m > s.length()) return result;
        int[] need = new int[26];
        for (char c : p.toCharArray()) need[c - 'a']++;
        int missing = m;

        for (int right = 0; right < s.length(); right++) {
            int in = s.charAt(right) - 'a';
            if (need[in] > 0) missing--;               // this char was actually needed
            need[in]--;

            if (right >= m) {                          // window too big: drop the left char
                int out = s.charAt(right - m) - 'a';
                need[out]++;
                if (need[out] > 0) missing++;          // we lost a needed char
            }
            if (missing == 0) result.add(right - m + 1);
        }
        return result;
    }

    public static void main(String[] args) {
        check("cbaebabacd / abc", "[0, 6]", findAnagrams("cbaebabacd", "abc"));
        check("abab / ab (overlapping)", "[0, 1, 2]", findAnagrams("abab", "ab"));
        check("aaaa / aa", "[0, 1, 2]", findAnagrams("aaaa", "aa"));
        check("abc / abcd (p longer)", "[]", findAnagrams("abc", "abcd"));
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
