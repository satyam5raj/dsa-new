/*
 * ============================================================================
 *  MEDIUM 05 : PERMUTATIONS II               LeetCode #47
 *  https://leetcode.com/problems/permutations-ii/
 *  Pattern: PERMUTATIONS + DUPLICATES -> sort, skip a value if its equal twin is UNUSED
 * ============================================================================
 *
 *  PROBLEM
 *    nums may contain duplicates; return all DISTINCT permutations.
 *      [1,1,2] -> [[1,1,2],[1,2,1],[2,1,1]]
 *
 *  ---------------------------------------------------------------------------
 *  Treat equal values as if they were labelled 1a, 1b. [1a,1b,2] and [1b,1a,2] are the
 *  same permutation. Fix: force equal values to be used IN ORDER (1a before 1b).
 *  After sorting:
 *       if (i > 0 && a[i] == a[i-1] && !used[i-1]) continue;
 *  "Don't use the second 1 unless the first 1 is already in the path." Then each
 *  arrangement of the equal values appears exactly once.
 *
 *  WHY "!used[i-1]" and not "used[i-1]"? Both versions produce correct output, but
 *  "!used[i-1]" prunes at the TOP of the tree (much less work); "used[i-1]" only
 *  discovers the duplicates deep down.
 *
 *  DRY RUN [1,1,2]
 *    first pick: 1(i=0) ok; 1(i=1) skipped (twin 0 unused); 2 ok
 *    -> [1, then 1(i=1) allowed now since 0 used, 2] ... -> 3 permutations total
 *
 *  COMPLEXITY   Time O(n * n!) worst case   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M05_PermutationsII {

    static List<List<Integer>> permuteUnique(int[] nums) {
        int[] a = nums.clone();
        Arrays.sort(a);
        List<List<Integer>> out = new ArrayList<>();
        bt(a, new boolean[a.length], new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] a, boolean[] used, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == a.length) { out.add(new ArrayList<>(path)); return; }
        for (int i = 0; i < a.length; i++) {
            if (used[i]) continue;
            if (i > 0 && a[i] == a[i - 1] && !used[i - 1]) continue;   // use equal values in order only
            used[i] = true;
            path.add(a[i]);
            bt(a, used, path, out);
            path.remove(path.size() - 1);
            used[i] = false;
        }
    }

    // referee: all permutations of indices, deduplicated with a Set
    static Set<List<Integer>> brute(int[] a) {
        Set<List<Integer>> s = new HashSet<>();
        perm(a, 0, s);
        return s;
    }

    static void perm(int[] a, int pos, Set<List<Integer>> s) {
        if (pos == a.length) { List<Integer> p = new ArrayList<>(); for (int x : a) p.add(x); s.add(p); return; }
        for (int i = pos; i < a.length; i++) {
            int t = a[pos]; a[pos] = a[i]; a[i] = t;
            perm(a, pos + 1, s);
            t = a[pos]; a[pos] = a[i]; a[i] = t;
        }
    }

    public static void main(String[] args) {
        check("[1,1,2]", "[[1, 1, 2], [1, 2, 1], [2, 1, 1]]", permuteUnique(new int[]{1, 1, 2}));
        check("[1,2,3]", 6, permuteUnique(new int[]{1, 2, 3}).size());
        check("[2,2,2]", "[[2, 2, 2]]", permuteUnique(new int[]{2, 2, 2}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(7), 0, 3).toArray();
            List<List<Integer>> got = permuteUnique(a);
            ok &= new HashSet<>(got).equals(brute(a.clone())) && new HashSet<>(got).size() == got.size();
        }
        check("300 random arrays vs deduplicated brute force", true, ok);
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
