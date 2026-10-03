/*
 * ============================================================================
 *  MEDIUM 09 : SET MATRIX ZEROES             LeetCode #73
 *  https://leetcode.com/problems/set-matrix-zeroes/
 *  Pattern: USE THE FIRST ROW/COLUMN AS MARKERS (O(1) extra space)
 * ============================================================================
 *
 *  PROBLEM
 *    If a cell is 0, set its ENTIRE row and column to 0. In-place.
 *      1 1 1        1 0 1
 *      1 0 1   ->   0 0 0
 *      1 1 1        1 0 1
 *
 *  ---------------------------------------------------------------------------
 *  TRAP: zeroing rows/columns WHILE scanning creates new zeros that then wipe
 *  out even more rows. We must first RECORD which rows/cols to clear, then clear.
 *
 *  APPROACH 1: two boolean arrays zeroRow[m], zeroCol[n]         O(m + n) space
 *
 *  APPROACH 2: O(1) SPACE - store those flags INSIDE the matrix
 *    Use row 0 as zeroCol[] and column 0 as zeroRow[]:
 *      matrix[i][0] = 0  means "clear row i"
 *      matrix[0][j] = 0  means "clear column j"
 *    PROBLEM: row 0 and column 0 are ALSO real data. Was row 0 itself supposed
 *    to be cleared? Save that in ONE boolean (firstRowZero) BEFORE using row 0 as markers.
 *    (matrix[0][0] covers column 0's own flag.)
 *
 *    STEPS
 *      1. firstRowZero = does row 0 contain a 0?
 *         firstColZero = does col 0 contain a 0?
 *      2. for cells (i>=1, j>=1): if 0 -> mark matrix[i][0] = 0 and matrix[0][j] = 0
 *      3. for cells (i>=1, j>=1): if matrix[i][0]==0 or matrix[0][j]==0 -> set 0
 *      4. finally clear row 0 / col 0 if their flags say so (LAST, so the markers survive step 3)
 *
 *  COMPLEXITY   Time O(m * n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M09_SetMatrixZeroes {

    static void setZeroes(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        boolean firstRowZero = false, firstColZero = false;
        for (int j = 0; j < n; j++) if (matrix[0][j] == 0) firstRowZero = true;
        for (int i = 0; i < m; i++) if (matrix[i][0] == 0) firstColZero = true;

        // record markers in row 0 / column 0
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][j] == 0) {
                    matrix[i][0] = 0;
                    matrix[0][j] = 0;
                }
            }
        }
        // clear the inner cells using the markers
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (matrix[i][0] == 0 || matrix[0][j] == 0) matrix[i][j] = 0;
            }
        }
        // finally the first row / column themselves
        if (firstRowZero) for (int j = 0; j < n; j++) matrix[0][j] = 0;
        if (firstColZero) for (int i = 0; i < m; i++) matrix[i][0] = 0;
    }

    static int[][] run(int[][] m) {
        setZeroes(m);
        return m;
    }

    public static void main(String[] args) {
        check("center zero", new int[][]{{1, 0, 1}, {0, 0, 0}, {1, 0, 1}}, run(new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 1, 1}}));
        check("zeros in first row", new int[][]{{0, 0, 0, 0}, {0, 4, 5, 0}, {0, 3, 1, 0}},
                run(new int[][]{{0, 1, 2, 0}, {3, 4, 5, 2}, {1, 3, 1, 5}}));
        check("no zeros", new int[][]{{1, 2}, {3, 4}}, run(new int[][]{{1, 2}, {3, 4}}));
        check("single row", new int[][]{{0, 0, 0}}, run(new int[][]{{1, 0, 3}}));
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
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
