/*
 * ============================================================================
 *  MEDIUM 13 : BEAUTIFUL ARRANGEMENT         LeetCode #526
 *  https://leetcode.com/problems/beautiful-arrangement/
 *  Pattern: PERMUTATIONS with a constraint checked AS EACH POSITION IS FILLED
 * ============================================================================
 *
 *  PROBLEM
 *    Count permutations perm of 1..n where for every position i (1-based):
 *       perm[i] % i == 0  or  i % perm[i] == 0
 *      n=2 -> 2   ([1,2] and [2,1])
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: generate all n! permutations, check each. n=15: 1.3 * 10^12. No.
 *
 *  PRUNE WHILE BUILDING: fill position pos with an unused number v only if the
 *  divisibility rule holds for (v, pos). A bad prefix is cut immediately, so most of
 *  the n! tree is never entered.
 *     bt(pos): if pos > n: count++
 *              for v in 1..n: if !used[v] && (v % pos == 0 || pos % v == 0): use v, bt(pos+1), unuse
 *
 *  EXTRA TRICK: fill positions from n DOWN to 1 - large positions have the fewest
 *  valid numbers (most constrained first), so the tree is cut even earlier.
 *
 *  COMPLEXITY   O(k) where k = valid partial arrangements (far below n!)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M13_BeautifulArrangement {

    static int countArrangement(int n) { return bt(n, n, new boolean[n + 1]); }

    // fill positions n, n-1, ..., 1 (most constrained first)
    static int bt(int n, int pos, boolean[] used) {
        if (pos == 0) return 1;
        int count = 0;
        for (int v = 1; v <= n; v++) {
            if (used[v] || (v % pos != 0 && pos % v != 0)) continue;   // rule fails: prune now
            used[v] = true;
            count += bt(n, pos - 1, used);
            used[v] = false;
        }
        return count;
    }

    // referee: all permutations, checked at the end
    static int brute(int n) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i + 1;
        return perm(p, 0);
    }

    static int perm(int[] p, int k) {
        if (k == p.length) {
            for (int i = 0; i < p.length; i++) if (p[i] % (i + 1) != 0 && (i + 1) % p[i] != 0) return 0;
            return 1;
        }
        int c = 0;
        for (int i = k; i < p.length; i++) {
            int t = p[k]; p[k] = p[i]; p[i] = t;
            c += perm(p, k + 1);
            t = p[k]; p[k] = p[i]; p[i] = t;
        }
        return c;
    }

    public static void main(String[] args) {
        check("n=1", 1, countArrangement(1));
        check("n=2", 2, countArrangement(2));
        check("n=15", 24679, countArrangement(15));
        boolean ok = true;
        for (int n = 1; n <= 9; n++) ok &= countArrangement(n) == brute(n);
        check("n = 1..9 vs checking all n! permutations", true, ok);
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
