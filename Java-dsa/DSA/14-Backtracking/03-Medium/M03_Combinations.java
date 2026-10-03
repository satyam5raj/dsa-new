/*
 * ============================================================================
 *  MEDIUM 03 : COMBINATIONS  ⭐ bonus          LeetCode #77
 *  https://leetcode.com/problems/combinations/
 *  Pattern: COMBINATIONS family - fixed size k, loop from `start`, FEASIBILITY pruning
 * ============================================================================
 *
 *  PROBLEM
 *    All combinations of k numbers chosen from 1..n.
 *      n=4, k=2 -> [[1,2],[1,3],[1,4],[2,3],[2,4],[3,4]]
 *
 *  ---------------------------------------------------------------------------
 *  Subsets (M01) that stop at size k:
 *     bt(start): if path.size() == k: record, return
 *                for i in start..n: add i; bt(i + 1); remove
 *
 *  PRUNING: if we still need `need = k - path.size()` numbers, starting at i leaves
 *  n - i + 1 numbers. If that's < need, no completion exists. So the loop only runs
 *  while i <= n - need + 1. For n=20, k=10 this cuts the nodes visited a lot
 *  (measured below).
 *
 *  COMPLEXITY   Time O(k * C(n, k))   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class M03_Combinations {

    static long nodes;

    static List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> out = new ArrayList<>();
        bt(n, k, 1, new ArrayList<>(), out, true);
        return out;
    }

    static void bt(int n, int k, int start, List<Integer> path, List<List<Integer>> out, boolean prune) {
        nodes++;
        if (path.size() == k) { out.add(new ArrayList<>(path)); return; }
        int last = prune ? n - (k - path.size()) + 1 : n;     // enough numbers left to finish?
        for (int i = start; i <= last; i++) {
            path.add(i);
            bt(n, k, i + 1, path, out, prune);
            path.remove(path.size() - 1);
        }
    }

    static long choose(int n, int k) { long r = 1; for (int i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; }

    public static void main(String[] args) {
        check("n=4 k=2", "[[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]", combine(4, 2));
        check("n=1 k=1", "[[1]]", combine(1, 1));
        boolean ok = true;
        for (int n = 1; n <= 12; n++) for (int k = 1; k <= n; k++) {
            List<List<Integer>> c = combine(n, k);
            ok &= c.size() == choose(n, k) && new HashSet<>(c).size() == c.size();
        }
        check("n <= 12: count == C(n,k), all distinct", true, ok);
        List<List<Integer>> tmp = new ArrayList<>();
        nodes = 0;
        bt(20, 10, 1, new ArrayList<>(), tmp, false);
        long without = nodes;
        tmp.clear();
        nodes = 0;
        bt(20, 10, 1, new ArrayList<>(), tmp, true);
        System.out.printf("   n=20, k=10: %,d nodes without pruning, %,d with%n", without, nodes);
        check("pruning visits fewer nodes", true, nodes < without);
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
