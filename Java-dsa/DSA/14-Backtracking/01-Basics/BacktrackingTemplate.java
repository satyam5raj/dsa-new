/*
 * ============================================================================
 *  BACKTRACKING: the template and the 3 families (subsets, combinations, permutations)
 * ============================================================================
 *
 *  BACKTRACKING = recursion that builds a solution ONE CHOICE AT A TIME, and UNDOES
 *  the choice after exploring it, so the next option starts from a clean state.
 *  It explores a DECISION TREE depth-first:
 *
 *                       []                       choose 1? 2? 3?
 *               /        |        \
 *            [1]        [2]       [3]            each level = one more decision
 *           /   \        |
 *       [1,2]  [1,3]   [2,3]                     leaves (or every node) = candidates
 *         |
 *      [1,2,3]
 *
 *  THE TEMPLATE (memorize this shape)
 *    void backtrack(state, path):
 *      if path is a complete/valid answer: record a COPY of path   (maybe return)
 *      for each choice available from this state:
 *        if choice is not allowed: continue                        (PRUNING)
 *        make the choice       (path.add, mark used, update sums)  CHOOSE
 *        backtrack(next state, path)                               EXPLORE
 *        undo the choice       (path.remove, unmark)               UN-CHOOSE
 *
 *  Forgetting the COPY (result.add(new ArrayList<>(path))) is the #1 bug: all
 *  recorded answers would be the same list object, empty at the end.
 *
 *  ---------------------------------------------------------------------------
 *  THE 3 FAMILIES (most problems are one of these + a constraint)
 *    SUBSETS       : every node of the tree is an answer; loop i from `start` (never go
 *                    back) -> 2^n results.               (Subsets, Subsets II)
 *    COMBINATIONS  : subsets of a fixed size k / with a target sum; loop from `start`.
 *                    C(n, k) results.                     (Combinations, Combination Sum)
 *    PERMUTATIONS  : order matters; loop over ALL elements, skip the ones `used[]`.
 *                    n! results.                          (Permutations I/II)
 *
 *  DUPLICATES IN THE INPUT (e.g. [1,2,2]): SORT first, then at the same tree level
 *  skip a value equal to the previous one:
 *      subsets/combinations: if (i > start && a[i] == a[i-1]) continue;
 *      permutations:         if (i > 0 && a[i] == a[i-1] && !used[i-1]) continue;
 *  -> each distinct answer is generated exactly once (no Set needed).
 *
 *  COST: (number of nodes in the tree) x (work per node, often O(n) to copy a path).
 *  Exponential by nature; PRUNING (cutting branches that can't succeed) is what makes
 *  it practical - see PruningAndConstraints.java.
 *
 *  Run:  java BacktrackingTemplate.java
 * ============================================================================
 */

import java.util.*;

public class BacktrackingTemplate {

    // SUBSETS: record at EVERY node
    static void subsets(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(new ArrayList<>(path));                 // every path is a subset
        for (int i = start; i < a.length; i++) {
            path.add(a[i]);                              // choose
            subsets(a, i + 1, path, out);                // explore (only later elements)
            path.remove(path.size() - 1);                // un-choose
        }
    }

    // COMBINATIONS of size k from 1..n, with pruning: stop when too few numbers remain
    static void combine(int n, int k, int start, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == k) { out.add(new ArrayList<>(path)); return; }
        for (int i = start; i <= n - (k - path.size()) + 1; i++) {   // need k - size more numbers
            path.add(i);
            combine(n, k, i + 1, path, out);
            path.remove(path.size() - 1);
        }
    }

    // PERMUTATIONS: any unused element can go next
    static void permute(int[] a, boolean[] used, List<Integer> path, List<List<Integer>> out) {
        if (path.size() == a.length) { out.add(new ArrayList<>(path)); return; }
        for (int i = 0; i < a.length; i++) {
            if (used[i]) continue;
            used[i] = true; path.add(a[i]);
            permute(a, used, path, out);
            used[i] = false; path.remove(path.size() - 1);
        }
    }

    // DUPLICATES: subsets of a SORTED array with repeated values, each subset once
    static void subsetsWithDup(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(new ArrayList<>(path));
        for (int i = start; i < a.length; i++) {
            if (i > start && a[i] == a[i - 1]) continue;  // same value at the same level: skip
            path.add(a[i]);
            subsetsWithDup(a, i + 1, path, out);
            path.remove(path.size() - 1);
        }
    }

    // THE #1 BUG, demonstrated: recording the path itself instead of a copy
    static void buggySubsets(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
        out.add(path);                                   // no copy!
        for (int i = start; i < a.length; i++) {
            path.add(a[i]);
            buggySubsets(a, i + 1, path, out);
            path.remove(path.size() - 1);
        }
    }

    static long factorial(int n) { return n <= 1 ? 1 : n * factorial(n - 1); }

    static long choose(int n, int k) { long r = 1; for (int i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; }

    public static void main(String[] args) {
        System.out.println("1. Subsets");
        List<List<Integer>> s = new ArrayList<>();
        subsets(new int[]{1, 2, 3}, 0, new ArrayList<>(), s);
        check("subsets of [1,2,3]", "[[], [1], [1, 2], [1, 2, 3], [1, 3], [2], [2, 3], [3]]", s);

        System.out.println("2. Combinations");
        List<List<Integer>> c = new ArrayList<>();
        combine(4, 2, 1, new ArrayList<>(), c);
        check("C(4,2)", "[[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]", c);

        System.out.println("3. Permutations");
        List<List<Integer>> p = new ArrayList<>();
        permute(new int[]{1, 2, 3}, new boolean[3], new ArrayList<>(), p);
        check("perms of [1,2,3]", "[[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]", p);

        System.out.println("4. Duplicates handled by sort + skip");
        List<List<Integer>> d = new ArrayList<>();
        subsetsWithDup(new int[]{1, 2, 2}, 0, new ArrayList<>(), d);
        check("subsets of [1,2,2]", "[[], [1], [1, 2], [1, 2, 2], [2], [2, 2]]", d);

        System.out.println("5. The missing-copy bug");
        List<List<Integer>> bug = new ArrayList<>();
        buggySubsets(new int[]{1, 2, 3}, 0, new ArrayList<>(), bug);
        check("every 'answer' is the same empty list", "[[], [], [], [], [], [], [], []]", bug);

        System.out.println("6. Counts match the formulas");
        boolean ok = true;
        for (int n = 0; n <= 7; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = i;
            List<List<Integer>> ss = new ArrayList<>(), pp = new ArrayList<>();
            subsets(a, 0, new ArrayList<>(), ss);
            permute(a, new boolean[n], new ArrayList<>(), pp);
            ok &= ss.size() == (1 << n) && pp.size() == factorial(n) && new HashSet<>(pp).size() == pp.size();
            for (int k = 0; k <= n; k++) {
                List<List<Integer>> cc = new ArrayList<>();
                combine(n, k, 1, new ArrayList<>(), cc);
                ok &= cc.size() == choose(n, k);
            }
        }
        check("2^n subsets, n! perms, C(n,k) combos", true, ok);
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
