/*
 * ============================================================================
 *  EASY 12 : FIND THE INDEX OF THE FIRST OCCURRENCE  ⭐ bonus   LeetCode #28
 *  https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/
 *  Pattern: STRING MATCHING (naive -> KMP)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the index of the first occurrence of needle in haystack, or -1.
 *    (This is String.indexOf. Interviewers want you to implement it.)
 *      "sadbutsad", "sad"   -> 0
 *      "leetcode", "leeto"  -> -1
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: NAIVE (sliding comparison)            O(n * m) worst, O(1) space
 *    For each start i in 0..n-m, compare needle with haystack[i..i+m-1].
 *    Usually fast because mismatches happen early. Worst case:
 *    haystack = "aaaaaaaaab", needle = "aaab" (almost-matches everywhere).
 *
 *  APPROACH 2: KMP                                   O(n + m) guaranteed, O(m) space
 *    Build the LPS array of the needle, then scan the haystack ONCE without ever
 *    moving backwards. Full walkthrough: 01-Basics/StringMatchingAlgorithms.java
 *
 *  INTERVIEW TIP: write the naive version cleanly first, then explain WHY KMP
 *  avoids the re-comparisons and what the LPS array stores. Being able to explain
 *  KMP matters more than memorising it.
 * ============================================================================
 */

import java.util.*;

public class E12_FindFirstOccurrence {

    static int strStrNaive(String haystack, String needle) {
        int n = haystack.length(), m = needle.length();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && haystack.charAt(i + j) == needle.charAt(j)) j++;
            if (j == m) return i;
        }
        return -1;
    }

    static int strStrKmp(String haystack, String needle) {
        int m = needle.length();
        if (m == 0) return 0;
        int[] lps = new int[m];
        for (int i = 1, len = 0; i < m; ) {                       // build LPS
            if (needle.charAt(i) == needle.charAt(len)) lps[i++] = ++len;
            else if (len > 0) len = lps[len - 1];
            else lps[i++] = 0;
        }
        for (int i = 0, j = 0; i < haystack.length(); i++) {     // search
            while (j > 0 && haystack.charAt(i) != needle.charAt(j)) j = lps[j - 1];
            if (haystack.charAt(i) == needle.charAt(j)) j++;
            if (j == m) return i - m + 1;
        }
        return -1;
    }

    public static void main(String[] args) {
        check("sadbutsad / sad", 0, strStrKmp("sadbutsad", "sad"));
        check("leetcode / leeto", -1, strStrKmp("leetcode", "leeto"));
        check("aaaaaaaaab / aaab (worst for naive)", 6, strStrKmp("aaaaaaaaab", "aaab"));
        check("mississippi / issip", 4, strStrKmp("mississippi", "issip"));
        check("naive agrees", 4, strStrNaive("mississippi", "issip"));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            String h = randomString(rnd, rnd.nextInt(20)), nd = randomString(rnd, 1 + rnd.nextInt(4));
            if (strStrKmp(h, nd) != h.indexOf(nd) || strStrNaive(h, nd) != h.indexOf(nd)) ok = false;
        }
        check("1000 random cases vs String.indexOf", true, ok);
        summary();
    }

    static String randomString(Random r, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(2)));
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
