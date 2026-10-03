/*
 * ============================================================================
 *  MEDIUM 07 : COMBINATION SUM II            LeetCode #40
 *  https://leetcode.com/problems/combination-sum-ii/
 *  Pattern: each element ONCE (i + 1) + DUPLICATE skipping + break on overshoot
 * ============================================================================
 *
 *  PROBLEM
 *    candidates may contain duplicates; each element may be used at most ONCE.
 *    Return unique combinations summing to target.
 *      [10,1,2,7,6,1,5], 8 -> [[1,1,6],[1,2,5],[1,7],[2,6]]
 *
 *  ---------------------------------------------------------------------------
 *  Combination Sum (M06) with two changes - the combination of Subsets II's trick:
 *    1. recurse with i + 1  (each element once)
 *    2. sort, and at the same level skip equal values:
 *         if (i > start && c[i] == c[i-1]) continue;
 *       Without it, [1(a),7] and [1(b),7] would both appear.
 *    plus the same `break` once c[i] > remaining.
 *
 *  This is the template behind most "unique combinations with duplicates" problems.
 *
 *  DRY RUN sorted [1,1,2,5,6,7,10], 8
 *    1 -> 1 -> 6 ✓ ; 1 -> 2 -> 5 ✓ ; 1 -> 7 ✓ ; (second 1 at level 0 skipped) ;
 *    2 -> 6 ✓ ; 5 -> (needs 3: none) ; ...
 *
 *  COMPLEXITY   Time O(2^n * n) worst case   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M07_CombinationSumII {

    static List<List<Integer>> combinationSum2(int[] candidates, int target) {
        int[] c = candidates.clone();
        Arrays.sort(c);
        List<List<Integer>> out = new ArrayList<>();
        bt(c, 0, target, new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] c, int start, int remaining, List<Integer> path, List<List<Integer>> out) {
        if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
        for (int i = start; i < c.length; i++) {
            if (i > start && c[i] == c[i - 1]) continue;       // duplicate at this level
            if (c[i] > remaining) break;
            path.add(c[i]);
            bt(c, i + 1, remaining - c[i], path, out);         // each element once
            path.remove(path.size() - 1);
        }
    }

    static Set<List<Integer>> brute(int[] c, int target) {
        Set<List<Integer>> s = new HashSet<>();
        for (int mask = 0; mask < (1 << c.length); mask++) {
            List<Integer> x = new ArrayList<>();
            int sum = 0;
            for (int i = 0; i < c.length; i++) if ((mask >> i & 1) == 1) { x.add(c[i]); sum += c[i]; }
            if (sum == target) { Collections.sort(x); s.add(x); }
        }
        return s;
    }

    public static void main(String[] args) {
        check("[10,1,2,7,6,1,5] 8", "[[1, 1, 6], [1, 2, 5], [1, 7], [2, 6]]", combinationSum2(new int[]{10, 1, 2, 7, 6, 1, 5}, 8));
        check("[2,5,2,1,2] 5", "[[1, 2, 2], [5]]", combinationSum2(new int[]{2, 5, 2, 1, 2}, 5));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] c = rnd.ints(1 + rnd.nextInt(12), 1, 8).toArray();
            int target = 1 + rnd.nextInt(20);
            List<List<Integer>> got = combinationSum2(c, target);
            ok &= new HashSet<>(got).equals(brute(c, target)) && new HashSet<>(got).size() == got.size();
        }
        check("500 random cases vs all subsets", true, ok);
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
