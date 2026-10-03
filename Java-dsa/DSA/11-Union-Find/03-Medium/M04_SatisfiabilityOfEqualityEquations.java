/*
 * ============================================================================
 *  MEDIUM 04 : SATISFIABILITY OF EQUALITY EQUATIONS   LeetCode #990
 *  https://leetcode.com/problems/satisfiability-of-equality-equations/
 *  Pattern: TWO PASSES - first union all "==", then check every "!="
 * ============================================================================
 *
 *  PROBLEM
 *    Equations like "a==b" or "b!=c" over single-letter variables. Can integers be
 *    assigned so all hold?
 *      ["a==b","b!=a"]          -> false
 *      ["a==b","b==c","a==c"]   -> true
 *      ["a==b","b!=c","c==a"]   -> false   (a=b, c=a  =>  b=c, contradicting b!=c)
 *
 *  ---------------------------------------------------------------------------
 *  Equality is an EQUIVALENCE relation (reflexive, symmetric, transitive) - exactly
 *  what Union-Find groups model. Inequalities are just constraints to check.
 *
 *  ORDER MATTERS: process ALL "==" first, then all "!=". If you checked "b!=c" before
 *  seeing "c==a", it would wrongly pass (b and c weren't connected YET).
 *    pass 1: for "x==y": union(x, y)
 *    pass 2: for "x!=y": if find(x) == find(y) -> false
 *  Note "a!=a" is always false (same root) - handled automatically.
 *
 *  Only 26 variables -> a 26-element parent array.
 *  COMPLEXITY   Time O(n)   Space O(26)
 * ============================================================================
 */

import java.util.*;

public class M04_SatisfiabilityOfEqualityEquations {

    static int[] parent = new int[26];

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static boolean equationsPossible(String[] equations) {
        for (int i = 0; i < 26; i++) parent[i] = i;
        for (String e : equations)                                   // pass 1: all equalities
            if (e.charAt(1) == '=') parent[find(e.charAt(0) - 'a')] = find(e.charAt(3) - 'a');
        for (String e : equations)                                   // pass 2: check inequalities
            if (e.charAt(1) == '!' && find(e.charAt(0) - 'a') == find(e.charAt(3) - 'a')) return false;
        return true;
    }

    // brute force: try all assignments of values 0..k-1 to the variables used
    static boolean brute(String[] eqs) {
        List<Character> vars = new ArrayList<>();
        for (String e : eqs) for (char c : new char[]{e.charAt(0), e.charAt(3)}) if (!vars.contains(c)) vars.add(c);
        int k = vars.size();
        int[] val = new int[k];
        return assign(eqs, vars, val, 0, k);
    }

    static boolean assign(String[] eqs, List<Character> vars, int[] val, int i, int k) {
        if (i == k) {
            for (String e : eqs) {
                int a = val[vars.indexOf(e.charAt(0))], b = val[vars.indexOf(e.charAt(3))];
                if ((e.charAt(1) == '=') != (a == b)) return false;
            }
            return true;
        }
        for (int v = 0; v < k; v++) { val[i] = v; if (assign(eqs, vars, val, i + 1, k)) return true; }
        return false;
    }

    public static void main(String[] args) {
        check("a==b, b!=a", false, equationsPossible(new String[]{"a==b", "b!=a"}));
        check("a==b, b==c, a==c", true, equationsPossible(new String[]{"a==b", "b==c", "a==c"}));
        check("a==b, b!=c, c==a (order trap)", false, equationsPossible(new String[]{"a==b", "b!=c", "c==a"}));
        check("a!=a", false, equationsPossible(new String[]{"a!=a"}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            String[] eqs = new String[1 + rnd.nextInt(6)];
            for (int i = 0; i < eqs.length; i++)
                eqs[i] = "" + (char) ('a' + rnd.nextInt(4)) + (rnd.nextInt(3) == 0 ? "!=" : "==") + (char) ('a' + rnd.nextInt(4));
            ok &= equationsPossible(eqs) == brute(eqs);
        }
        check("1,000 random systems vs all assignments", true, ok);
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
