/*
 * ============================================================================
 *  MEDIUM 04 : PERMUTATIONS                  LeetCode #46
 *  https://leetcode.com/problems/permutations/
 *  Pattern: PERMUTATIONS family - loop over ALL elements, skip the `used` ones
 * ============================================================================
 *
 *  PROBLEM
 *    Distinct integers -> all n! orderings.
 *      [1,2,3] -> [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
 *
 *  ---------------------------------------------------------------------------
 *  The difference from subsets: ORDER matters, so after picking 2, picking 1 is still
 *  allowed. Loop from 0 every time and skip elements already in the path:
 *     bt(): if path.size() == n: record
 *           for i in 0..n-1: if used[i] continue; mark, add, bt(), unmark, remove
 *
 *  SWAP VERSION (no used[] array, no extra path): fix position `pos` by swapping each
 *  candidate a[i] (i >= pos) into it, recurse on pos+1, swap back. Same n! leaves.
 *
 *  DRY RUN [1,2,3]: 1 -> {2 -> 3, 3 -> 2}, 2 -> {1 -> 3, 3 -> 1}, 3 -> {1 -> 2, 2 -> 1}
 *
 *  COMPLEXITY   Time O(n * n!)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M04_Permutations {

    static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> out = new ArrayList<>();
        bt(nums, new boolean[nums.length], new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] a, boolean[] used, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == a.length) { out.add(new ArrayList<>(path)); return; }
        for (int i = 0; i < a.length; i++) {
            if (used[i]) continue;
            used[i] = true;
            path.add(a[i]);
            bt(a, used, path, out);
            path.remove(path.size() - 1);
            used[i] = false;
        }
    }

    static List<List<Integer>> permuteSwap(int[] nums) {
        List<List<Integer>> out = new ArrayList<>();
        swapBt(nums.clone(), 0, out);
        return out;
    }

    static void swapBt(int[] a, int pos, List<List<Integer>> out) {
        if (pos == a.length) { List<Integer> p = new ArrayList<>(); for (int x : a) p.add(x); out.add(p); return; }
        for (int i = pos; i < a.length; i++) {
            swap(a, pos, i);                                    // put a[i] at position pos
            swapBt(a, pos + 1, out);
            swap(a, pos, i);                                    // undo
        }
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void main(String[] args) {
        check("[1,2,3]", "[[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]", permute(new int[]{1, 2, 3}));
        check("[0,1]", "[[0, 1], [1, 0]]", permute(new int[]{0, 1}));
        boolean ok = true;
        long fact = 1;
        for (int n = 1; n <= 8; n++) {
            fact *= n;
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = 10 - i;
            List<List<Integer>> p1 = permute(a), p2 = permuteSwap(a);
            ok &= p1.size() == fact && new HashSet<>(p1).size() == fact && new HashSet<>(p1).equals(new HashSet<>(p2));
        }
        check("n = 1..8: n! distinct, both versions agree", true, ok);
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
