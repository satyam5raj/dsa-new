/*
 * ============================================================================
 *  EASY 07 : ISOMORPHIC STRINGS              LeetCode #205
 *  https://leetcode.com/problems/isomorphic-strings/
 *  Pattern: CHARACTER MAPPING (bijection, two maps)
 * ============================================================================
 *
 *  PROBLEM
 *    s and t are isomorphic if s's characters can be REPLACED to get t, where
 *    each character maps to exactly one character and no two characters map to
 *    the same one (a character may map to itself).
 *      "egg", "add"     -> true    (e->a, g->d)
 *      "foo", "bar"     -> false   (o would need to map to both a and r)
 *      "badc", "baba"   -> false   (b->b and d->b: two chars map to the same 'b')
 *
 *  ---------------------------------------------------------------------------
 *  WHY TWO MAPS? One map s->t catches "o -> a and o -> r" (one char, two
 *  targets). It MISSES "b -> b and d -> b" (two chars, one target). The reverse
 *  map t->s catches that. Together they enforce a ONE-TO-ONE mapping (a bijection).
 *
 *  ALGORITHM
 *    for each position i, with a = s[i], b = t[i]:
 *      if a already maps to something other than b -> false
 *      if b is already mapped FROM something other than a -> false
 *      record a->b and b->a
 *
 *  ELEGANT ALTERNATIVE: compare "first seen index" patterns.
 *    "egg" -> [0,1,1], "add" -> [0,1,1] -> same structure -> isomorphic.
 *    (implemented below as isIsomorphicPattern)
 *
 *  COMPLEXITY   Time O(n)   Space O(1) (fixed-size arrays for ASCII)
 * ============================================================================
 */

import java.util.*;

public class E07_IsomorphicStrings {

    static boolean isIsomorphic(String s, String t) {
        int[] sToT = new int[256], tToS = new int[256];       // 0 = "unmapped"; store char + 1
        for (int i = 0; i < s.length(); i++) {
            int a = s.charAt(i), b = t.charAt(i);
            if (sToT[a] == 0 && tToS[b] == 0) {
                sToT[a] = b + 1;
                tToS[b] = a + 1;
            } else if (sToT[a] != b + 1 || tToS[b] != a + 1) {
                return false;
            }
        }
        return true;
    }

    // Same answer by comparing "where did I first see this char" for both strings
    static boolean isIsomorphicPattern(String s, String t) {
        int[] lastS = new int[256], lastT = new int[256];
        for (int i = 0; i < s.length(); i++) {
            if (lastS[s.charAt(i)] != lastT[t.charAt(i)]) return false;
            lastS[s.charAt(i)] = i + 1;
            lastT[t.charAt(i)] = i + 1;
        }
        return true;
    }

    public static void main(String[] args) {
        check("egg / add", true, isIsomorphic("egg", "add"));
        check("foo / bar", false, isIsomorphic("foo", "bar"));
        check("paper / title", true, isIsomorphic("paper", "title"));
        check("badc / baba (needs 2nd map)", false, isIsomorphic("badc", "baba"));
        check("pattern approach: badc/baba", false, isIsomorphicPattern("badc", "baba"));
        check("pattern approach: paper/title", true, isIsomorphicPattern("paper", "title"));
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
