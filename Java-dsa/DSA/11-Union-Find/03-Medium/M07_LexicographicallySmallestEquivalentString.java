/*
 * ============================================================================
 *  MEDIUM 07 : LEXICOGRAPHICALLY SMALLEST EQUIVALENT STRING  ⭐ bonus   LeetCode #1061
 *  https://leetcode.com/problems/lexicographically-smallest-equivalent-string/
 *  Pattern: Union-Find where the ROOT is always the SMALLEST element of its set
 * ============================================================================
 *
 *  PROBLEM
 *    s1[i] and s2[i] are equivalent characters (equivalence is reflexive, symmetric,
 *    transitive). Rewrite baseStr replacing each char by the SMALLEST char in its
 *    equivalence class.
 *      s1 "parker", s2 "morris", base "parser" -> "makkek"
 *        classes: {m,p}, {a,o}, {k,r,s}, {e,i}  ->  p->m, a->a, r->k, s->k, e->e
 *
 *  ---------------------------------------------------------------------------
 *  Equivalence classes -> Union-Find over 26 letters.
 *  The twist: we need the smallest member of each class. Instead of union by size,
 *  ALWAYS make the SMALLER root the parent:
 *      union(a, b): ra = find(a), rb = find(b);  parent[max(ra, rb)] = min(ra, rb)
 *  Then find(x) IS the answer for x. (With only 26 elements, giving up union by
 *  size costs nothing.)
 *  This "choose the root deliberately so it carries the answer" idea is common:
 *  smallest/largest member, a representative with special meaning, etc.
 *
 *  COMPLEXITY   Time O((|s1| + |base|) * alpha)   Space O(26)
 * ============================================================================
 */

import java.util.*;

public class M07_LexicographicallySmallestEquivalentString {

    static int[] parent = new int[26];

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static String smallestEquivalentString(String s1, String s2, String baseStr) {
        for (int i = 0; i < 26; i++) parent[i] = i;
        for (int i = 0; i < s1.length(); i++) {
            int a = find(s1.charAt(i) - 'a'), b = find(s2.charAt(i) - 'a');
            if (a != b) parent[Math.max(a, b)] = Math.min(a, b);       // the smaller letter stays root
        }
        StringBuilder sb = new StringBuilder();
        for (char c : baseStr.toCharArray()) sb.append((char) ('a' + find(c - 'a')));
        return sb.toString();
    }

    // referee: transitive closure of the equivalence relation, then min per class
    static String brute(String s1, String s2, String base) {
        boolean[][] eq = new boolean[26][26];
        for (int i = 0; i < 26; i++) eq[i][i] = true;
        for (int i = 0; i < s1.length(); i++) { int a = s1.charAt(i) - 'a', b = s2.charAt(i) - 'a'; eq[a][b] = eq[b][a] = true; }
        for (int k = 0; k < 26; k++) for (int i = 0; i < 26; i++) for (int j = 0; j < 26; j++) eq[i][j] |= eq[i][k] && eq[k][j];
        StringBuilder sb = new StringBuilder();
        for (char c : base.toCharArray()) { int x = c - 'a', m = x; for (int j = 0; j < 26; j++) if (eq[x][j]) { m = j; break; } sb.append((char) ('a' + m)); }
        return sb.toString();
    }

    public static void main(String[] args) {
        check("parker/morris/parser", "makkek", smallestEquivalentString("parker", "morris", "parser"));
        check("hello/world/hold", "hdld", smallestEquivalentString("hello", "world", "hold"));
        check("leetcode/programs/sourcecode", "aauaaaaada", smallestEquivalentString("leetcode", "programs", "sourcecode"));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int len = 1 + rnd.nextInt(8);
            StringBuilder a = new StringBuilder(), b = new StringBuilder(), base = new StringBuilder();
            for (int i = 0; i < len; i++) { a.append((char) ('a' + rnd.nextInt(8))); b.append((char) ('a' + rnd.nextInt(8))); }
            for (int i = 0; i < 6; i++) base.append((char) ('a' + rnd.nextInt(8)));
            ok &= smallestEquivalentString(a.toString(), b.toString(), base.toString()).equals(brute(a.toString(), b.toString(), base.toString()));
        }
        check("1,000 random cases vs transitive closure", true, ok);
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
