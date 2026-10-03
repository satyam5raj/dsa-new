/*
 * ============================================================================
 *  MEDIUM 01 : SUBSETS                       LeetCode #78
 *  https://leetcode.com/problems/subsets/
 *  Pattern: SUBSETS family - record at every node, loop from `start`
 * ============================================================================
 *
 *  PROBLEM
 *    Distinct integers -> all 2^n subsets (any order).
 *      [1,2,3] -> [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
 *
 *  ---------------------------------------------------------------------------
 *  THREE WAYS, all O(n * 2^n):
 *  1. BACKTRACKING (start index): every node is a subset; children add a LATER element.
 *       bt(start): record path; for i in start..n-1: add a[i]; bt(i+1); remove
 *     Looping from `start` (not 0) is what prevents [2,1] after [1,2].
 *  2. PICK / NOT PICK: decide for element i: include or skip; record at depth n.
 *  3. BITMASK: subset number `mask` contains a[i] iff bit i is set. mask = 0..2^n-1.
 *  4. ITERATIVE: start with [[]]; for each x, copy every existing subset and add x.
 *
 *  DRY RUN backtracking [1,2,3]:
 *    [] -> [1] -> [1,2] -> [1,2,3];  back to [1] -> [1,3];  [] -> [2] -> [2,3];  [3]
 *
 *  COMPLEXITY   Time O(n * 2^n)   Space O(n) recursion (+ output)
 * ============================================================================
 */

import java.util.*;

public class M01_Subsets {

    static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> out = new ArrayList<>();
        bt(nums, 0, new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(new ArrayList<>(path));
        for (int i = start; i < a.length; i++) {
            path.add(a[i]);
            bt(a, i + 1, path, out);
            path.remove(path.size() - 1);
        }
    }

    static List<List<Integer>> bitmask(int[] a) {
        List<List<Integer>> out = new ArrayList<>();
        for (int mask = 0; mask < (1 << a.length); mask++) {
            List<Integer> s = new ArrayList<>();
            for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s.add(a[i]);
            out.add(s);
        }
        return out;
    }

    static List<List<Integer>> iterative(int[] a) {
        List<List<Integer>> out = new ArrayList<>();
        out.add(new ArrayList<>());
        for (int x : a) {
            int size = out.size();
            for (int i = 0; i < size; i++) { List<Integer> s = new ArrayList<>(out.get(i)); s.add(x); out.add(s); }
        }
        return out;
    }

    static Set<List<Integer>> norm(List<List<Integer>> l) {
        Set<List<Integer>> s = new HashSet<>();
        for (List<Integer> x : l) { List<Integer> c = new ArrayList<>(x); Collections.sort(c); s.add(c); }
        return s;
    }

    public static void main(String[] args) {
        check("[1,2,3]", "[[], [1], [1, 2], [1, 2, 3], [1, 3], [2], [2, 3], [3]]", subsets(new int[]{1, 2, 3}));
        check("[0]", "[[], [0]]", subsets(new int[]{0}));
        boolean ok = true;
        for (int n = 0; n <= 10; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = i * 3 - 7;
            List<List<Integer>> b = subsets(a);
            ok &= b.size() == (1 << n) && norm(b).size() == b.size() && norm(b).equals(norm(bitmask(a))) && norm(b).equals(norm(iterative(a)));
        }
        check("n = 0..10: backtracking == bitmask == iterative", true, ok);
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
