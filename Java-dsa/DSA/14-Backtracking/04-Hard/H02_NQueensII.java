/*
 * ============================================================================
 *  HARD 02 : N-QUEENS II                     LeetCode #52
 *  https://leetcode.com/problems/n-queens-ii/
 *  Pattern: count only -> BITMASK backtracking (attacks shift with each row)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the NUMBER of N-Queens solutions.   n=4 -> 2,  n=8 -> 92
 *
 *  ---------------------------------------------------------------------------
 *  H01 with a counter instead of boards works. The classic speed-up: keep the attacked
 *  columns of the CURRENT row as three bitmasks:
 *     cols  : columns already used
 *     diag  : squares attacked by "/" diagonals in this row
 *     anti  : squares attacked by "\" diagonals in this row
 *  Moving to the next row, each diagonal attack moves one column:
 *     diag -> (diag | bit) << 1        anti -> (anti | bit) >> 1
 *  Free squares in this row:  free = ~(cols | diag | anti) & ((1 << n) - 1)
 *  Pick them one at a time with the lowest-set-bit trick:  bit = free & -free
 *  Everything is a few machine instructions - no arrays, no loops over occupied columns.
 *
 *  DRY RUN n=4, row 0 picks bit 0010 (col 1):
 *    row 1: cols 0010, diag 0100, anti 0001 -> free = 1000 -> col 3 ...
 *
 *  COMPLEXITY   Time O(n!) roughly, tiny constant   Space O(n) recursion
 * ============================================================================
 */

import java.util.*;

public class H02_NQueensII {

    static int totalNQueens(int n) { return count(n, 0, 0, 0, 0); }

    static int count(int n, int row, int cols, int diag, int anti) {
        if (row == n) return 1;
        int all = (1 << n) - 1;
        int free = ~(cols | diag | anti) & all;          // squares not attacked in this row
        int total = 0;
        while (free != 0) {
            int bit = free & -free;                       // lowest free square
            free -= bit;
            total += count(n, row + 1, cols | bit, ((diag | bit) << 1) & all, (anti | bit) >> 1);
        }
        return total;
    }

    // referee: the array version from H01, counting only
    static int arrays(int n, int r, boolean[] c, boolean[] d, boolean[] a) {
        if (r == n) return 1;
        int t = 0;
        for (int col = 0; col < n; col++) {
            if (c[col] || d[r + col] || a[r - col + n - 1]) continue;
            c[col] = d[r + col] = a[r - col + n - 1] = true;
            t += arrays(n, r + 1, c, d, a);
            c[col] = d[r + col] = a[r - col + n - 1] = false;
        }
        return t;
    }

    public static void main(String[] args) {
        check("n=4", 2, totalNQueens(4));
        check("n=8", 92, totalNQueens(8));
        boolean ok = true;
        for (int n = 1; n <= 11; n++) ok &= totalNQueens(n) == arrays(n, 0, new boolean[n], new boolean[2 * n], new boolean[2 * n]);
        check("n = 1..11: bitmask == arrays", true, ok);
        long t0 = System.nanoTime();
        int r = totalNQueens(13);
        long bits = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        arrays(13, 0, new boolean[13], new boolean[26], new boolean[26]);
        long arr = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("   n=13 (%,d solutions): bitmask %d ms, arrays %d ms%n", r, bits, arr);
        check("n=13", 73712, r);
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
