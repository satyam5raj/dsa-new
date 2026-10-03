/*
 * ============================================================================
 *  MEDIUM 09 : SORT CHARACTERS BY FREQUENCY  LeetCode #451
 *  https://leetcode.com/problems/sort-characters-by-frequency/
 *  Pattern: COUNT + BUCKET SORT (by frequency)
 * ============================================================================
 *
 *  PROBLEM
 *    Sort the characters by DECREASING frequency. Ties in any order.
 *      "tree"   -> "eert" (or "eetr")
 *      "cccaaa" -> "cccaaa" (or "aaaccc")
 *      "Aabb"   -> "bbAa" (or "bbaA")   ('A' and 'a' are different characters)
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1: count every character (int[128])
 *
 *  STEP 2, OPTION A: sort the distinct characters by count       O(n + k log k)
 *    (k = number of distinct characters, at most 128 -> effectively O(n))
 *  STEP 2, OPTION B: BUCKET SORT by frequency                    O(n)
 *    A frequency is between 1 and n. Make buckets[freq] = list of chars with that
 *    frequency. Walk the buckets from n down to 1 and append each char freq times.
 *  (Bucket sort = "the values are small integers -> use them as indices".
 *   The same trick solves "Top K Frequent Elements" in O(n).)
 *
 *  DRY RUN "tree"
 *    counts: t1 r1 e2
 *    buckets: [2] = [e], [1] = [t, r]
 *    walk from 4 down: freq 2 -> "ee", freq 1 -> "t","r" -> "eetr"
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M09_SortCharactersByFrequency {

    static String frequencySort(String s) {
        int[] count = new int[128];
        for (char c : s.toCharArray()) count[c]++;

        List<List<Character>> buckets = new ArrayList<>();
        for (int i = 0; i <= s.length(); i++) buckets.add(new ArrayList<>());
        for (int c = 0; c < 128; c++) {
            if (count[c] > 0) buckets.get(count[c]).add((char) c);
        }

        StringBuilder sb = new StringBuilder();
        for (int freq = s.length(); freq >= 1; freq--) {          // highest frequency first
            for (char c : buckets.get(freq)) {
                sb.append(String.valueOf(c).repeat(freq));
            }
        }
        return sb.toString();
    }

    // Ties may come out in any order, so check the PROPERTY: same characters,
    // equal characters grouped together, and group frequencies never increasing.
    static boolean valid(String in, String out) {
        if (in.length() != out.length()) return false;
        char[] a = in.toCharArray(), b = out.toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        if (!Arrays.equals(a, b)) return false;
        Set<Character> seen = new HashSet<>();
        int prevRun = Integer.MAX_VALUE;
        for (int i = 0; i < out.length(); ) {
            int j = i;
            while (j < out.length() && out.charAt(j) == out.charAt(i)) j++;
            if (!seen.add(out.charAt(i)) || j - i > prevRun) return false;
            prevRun = j - i;
            i = j;
        }
        return true;
    }

    public static void main(String[] args) {
        for (String s : new String[]{"tree", "cccaaa", "Aabb", "", "x", "loveleetcode"}) {
            String out = frequencySort(s);
            check("\"" + s + "\" -> \"" + out + "\"", true, valid(s, out));
        }
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
