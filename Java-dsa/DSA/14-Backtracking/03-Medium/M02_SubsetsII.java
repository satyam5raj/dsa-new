/*
 * ============================================================================
 *  MEDIUM 02 : SUBSETS II                    LeetCode #90
 *  https://leetcode.com/problems/subsets-ii/
 *  Pattern: SUBSETS + DUPLICATES -> sort, then skip equal values at the same level
 * ============================================================================
 *
 *  PROBLEM
 *    nums may contain duplicates; return all DISTINCT subsets.
 *      [1,2,2] -> [[],[1],[1,2],[1,2,2],[2],[2,2]]
 *
 *  ---------------------------------------------------------------------------
 *  LAZY FIX: generate all 2^n subsets, sort each, put them in a Set. Works but wastes
 *  time generating duplicates (and hashing lists).
 *
 *  SORT + SKIP: after sorting, equal values sit together. At one level of the tree
 *  (one `for` loop), choosing the first 2 or the second 2 as the NEXT element leads to
 *  identical subtrees. So at the same level, use only the FIRST of equal values:
 *       if (i > start && nums[i] == nums[i-1]) continue;
 *  `i > start` matters: the second 2 is still allowed DEEPER (after the first 2 was
 *  picked at the level above) - that's how [2,2] is formed.
 *
 *  DRY RUN [1,2,2]
 *    level 0: pick 1 -> {level 1: pick 2 (i=1) -> [1,2] -> [1,2,2];  i=2 skipped}
 *             pick 2 (i=1) -> [2] -> [2,2];   i=2 skipped at level 0
 *
 *  COMPLEXITY   Time O(n * 2^n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M02_SubsetsII {

    static List<List<Integer>> subsetsWithDup(int[] nums) {
        int[] a = nums.clone();
        Arrays.sort(a);                                          // equal values adjacent
        List<List<Integer>> out = new ArrayList<>();
        bt(a, 0, new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(new ArrayList<>(path));
        for (int i = start; i < a.length; i++) {
            if (i > start && a[i] == a[i - 1]) continue;         // same value, same level: duplicate subtree
            path.add(a[i]);
            bt(a, i + 1, path, out);
            path.remove(path.size() - 1);
        }
    }

    static Set<List<Integer>> brute(int[] a) {
        Set<List<Integer>> s = new HashSet<>();
        for (int mask = 0; mask < (1 << a.length); mask++) {
            List<Integer> x = new ArrayList<>();
            for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) x.add(a[i]);
            Collections.sort(x);
            s.add(x);
        }
        return s;
    }

    public static void main(String[] args) {
        check("[1,2,2]", "[[], [1], [1, 2], [1, 2, 2], [2], [2, 2]]", subsetsWithDup(new int[]{1, 2, 2}));
        check("[0]", "[[], [0]]", subsetsWithDup(new int[]{0}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(rnd.nextInt(10), 0, 4).toArray();
            List<List<Integer>> got = subsetsWithDup(a);
            ok &= new HashSet<>(got).equals(brute(a)) && new HashSet<>(got).size() == got.size();   // no duplicates generated
        }
        check("500 random arrays vs set of all subsets", true, ok);
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
