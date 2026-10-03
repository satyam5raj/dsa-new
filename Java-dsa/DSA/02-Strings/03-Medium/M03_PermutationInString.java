/*
 * ============================================================================
 *  MEDIUM 03 : PERMUTATION IN STRING         LeetCode #567
 *  https://leetcode.com/problems/permutation-in-string/
 *  Pattern: FIXED-SIZE SLIDING WINDOW + FREQUENCY COMPARISON
 * ============================================================================
 *
 *  PROBLEM
 *    Does s2 contain a PERMUTATION of s1 as a substring? (an anagram of s1)
 *      s1 "ab", s2 "eidbaooo" -> true   ("ba")
 *      s1 "ab", s2 "eidboaoo" -> false
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: generate all permutations of s1 (m! of them!) -> hopeless.
 *
 *  REFRAME: a permutation of s1 = a substring with EXACTLY s1's letter counts.
 *  Any such substring has length m = |s1|.
 *  -> slide a window of size m over s2 and compare letter counts.
 *
 *  NAIVE COMPARE: compare two int[26] arrays per step -> O(26 n). Fine already!
 *
 *  O(n) TRICK: track 'matches' = how many of the 26 letters currently have
 *  EQUAL counts in the window and in s1. Each slide changes only 2 letters, so
 *  update 'matches' only for those two. When matches == 26 -> found.
 *
 *  DRY RUN s1 "ab", s2 "eidbaooo"   (window size 2)
 *    "ei" -> no;  "id" -> no;  "db" -> no;  "ba" -> counts equal s1's -> true
 *
 *  COMPLEXITY   Time O(n + m)   Space O(1)
 *  NEXT: Find All Anagrams (M05) is the same, but collects EVERY start index.
 * ============================================================================
 */

import java.util.*;

public class M03_PermutationInString {

    // Simple and clear: compare the count arrays at every window position (O(26 * n))
    static boolean checkInclusion(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        if (m > n) return false;
        int[] need = new int[26], window = new int[26];
        for (int i = 0; i < m; i++) {
            need[s1.charAt(i) - 'a']++;
            window[s2.charAt(i) - 'a']++;
        }
        if (Arrays.equals(need, window)) return true;
        for (int i = m; i < n; i++) {
            window[s2.charAt(i) - 'a']++;          // char entering on the right
            window[s2.charAt(i - m) - 'a']--;      // char leaving on the left
            if (Arrays.equals(need, window)) return true;
        }
        return false;
    }

    // Optimized: maintain the number of letters whose counts match (O(n))
    static boolean checkInclusionMatches(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        if (m > n) return false;
        int[] diff = new int[26];                  // diff = window count - needed count
        for (int i = 0; i < m; i++) {
            diff[s1.charAt(i) - 'a']--;
            diff[s2.charAt(i) - 'a']++;
        }
        int matches = 0;
        for (int d : diff) if (d == 0) matches++;
        for (int i = m; ; i++) {
            if (matches == 26) return true;
            if (i == n) return false;
            int in = s2.charAt(i) - 'a', out = s2.charAt(i - m) - 'a';
            if (diff[in] == 0) matches--;          // was balanced, about to change
            if (++diff[in] == 0) matches++;
            if (diff[out] == 0) matches--;
            if (--diff[out] == 0) matches++;
        }
    }

    public static void main(String[] args) {
        check("ab in eidbaooo", true, checkInclusion("ab", "eidbaooo"));
        check("ab in eidboaoo", false, checkInclusion("ab", "eidboaoo"));
        check("adc in dcda", true, checkInclusion("adc", "dcda"));
        check("abc in ab (s1 longer)", false, checkInclusion("abc", "ab"));
        check("matches version: ab in eidbaooo", true, checkInclusionMatches("ab", "eidbaooo"));
        check("matches version: ab in eidboaoo", false, checkInclusionMatches("ab", "eidboaoo"));
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
