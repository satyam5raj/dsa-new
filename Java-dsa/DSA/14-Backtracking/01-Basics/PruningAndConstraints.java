/*
 * ============================================================================
 *  PRUNING AND CONSTRAINT CHECKS: what makes backtracking fast enough
 * ============================================================================
 *
 *  Backtracking explores a decision tree. PRUNING = refusing to enter a branch as
 *  soon as you KNOW it can't lead to a valid answer. A branch cut near the root
 *  removes an exponential number of leaves.
 *
 *  KINDS OF PRUNING
 *  1. CONSTRAINT CHECK BEFORE CHOOSING: is this choice legal right now?
 *       N-Queens: is this column / diagonal attacked?   Sudoku: is the digit used?
 *  2. BOUNDS: the partial answer already exceeds a limit.
 *       Combination Sum: sorted candidates -> once a candidate > remaining, BREAK
 *       (every later candidate is bigger too).
 *  3. FEASIBILITY: even the best completion can't work.
 *       "need k more numbers but fewer than k remain" (Combinations).
 *  4. ORDERING: try the most constrained choice first (fewest options), or the largest
 *       item first (Matchsticks: big sticks fail fast).
 *  5. SYMMETRY: skip choices equivalent to ones already tried (duplicate values,
 *       empty buckets in Partition K Subsets).
 *
 *  O(1) CONSTRAINT CHECKS WITH ARRAYS / BITMASKS
 *    N-Queens: cols[c], diag1[r + c], diag2[r - c + n - 1] instead of scanning the board.
 *    Sudoku:   rows[r], cols[c], boxes[b] as 9-bit masks of used digits.
 *
 *  This file measures N-Queens three ways to show the effect:
 *    A. place 8 queens anywhere, check at the end    (64 choose 8 ~ 4.4 billion: way too slow - not run)
 *    B. one queen per ROW, check the full board at the end      (8^8 = 16.7 million leaves)
 *    C. one queen per row, prune attacked squares immediately   (~2,000 nodes)
 *  Run:  java PruningAndConstraints.java
 * ============================================================================
 */

import java.util.*;

public class PruningAndConstraints {

    static long nodes;

    // B: no pruning - fill every row, validate only complete boards
    static int noPruning(int n, int row, int[] colOf) {
        nodes++;
        if (row == n) return valid(colOf) ? 1 : 0;
        int count = 0;
        for (int c = 0; c < n; c++) { colOf[row] = c; count += noPruning(n, row + 1, colOf); }
        return count;
    }

    static boolean valid(int[] colOf) {
        for (int i = 0; i < colOf.length; i++)
            for (int j = i + 1; j < colOf.length; j++)
                if (colOf[i] == colOf[j] || Math.abs(colOf[i] - colOf[j]) == j - i) return false;
        return true;
    }

    // C: prune immediately with O(1) attack checks
    static int pruned(int n, int row, boolean[] cols, boolean[] d1, boolean[] d2) {
        nodes++;
        if (row == n) return 1;
        int count = 0;
        for (int c = 0; c < n; c++) {
            if (cols[c] || d1[row + c] || d2[row - c + n - 1]) continue;   // attacked: don't even enter
            cols[c] = d1[row + c] = d2[row - c + n - 1] = true;
            count += pruned(n, row + 1, cols, d1, d2);
            cols[c] = d1[row + c] = d2[row - c + n - 1] = false;
        }
        return count;
    }

    // BOUND pruning: combination sum with sorted candidates and break
    static long csNodes;                                 // loop iterations

    static int combinationSumCount(int[] sorted, int start, int remaining, boolean useBreak) {
        if (remaining == 0) return 1;
        int count = 0;
        for (int i = start; i < sorted.length; i++) {
            csNodes++;                                   // candidates EXAMINED (what break saves)
            if (sorted[i] > remaining) { if (useBreak) break; else continue; }
            count += combinationSumCount(sorted, i, remaining - sorted[i], useBreak);
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println("1. N-Queens n=8: no pruning vs pruning");
        nodes = 0;
        int a = noPruning(8, 0, new int[8]);
        long noPruneNodes = nodes;
        nodes = 0;
        int b = pruned(8, 0, new boolean[8], new boolean[15], new boolean[15]);
        long pruneNodes = nodes;
        System.out.printf("   without pruning: %,d nodes    with pruning: %,d nodes%n", noPruneNodes, pruneNodes);
        check("both find 92 solutions", "92 92", a + " " + b);
        check("pruning visits > 1000x fewer nodes", true, noPruneNodes > 1000 * pruneNodes);

        System.out.println("2. Solutions for n = 1..12 (pruned)");
        int[] known = {1, 0, 0, 2, 10, 4, 40, 92, 352, 724, 2680, 14200};
        boolean ok = true;
        for (int n = 1; n <= 12; n++) ok &= pruned(n, 0, new boolean[n], new boolean[2 * n], new boolean[2 * n]) == known[n - 1];
        check("matches the known sequence", true, ok);

        System.out.println("3. Bound pruning: sorted candidates + break");
        int[] cand = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29};
        csNodes = 0;
        int x = combinationSumCount(cand, 0, 60, false);
        long noBreak = csNodes;
        csNodes = 0;
        int y = combinationSumCount(cand, 0, 60, true);
        System.out.printf("   target 60: %,d candidates examined with 'continue', %,d with 'break' (same %,d answers)%n", noBreak, csNodes, y);
        check("same count either way", x, y);
        check("break examines fewer candidates", true, csNodes < noBreak);
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
