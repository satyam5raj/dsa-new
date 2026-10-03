/*
 * ============================================================================
 *  MEDIUM 08 : ROTATE IMAGE                  LeetCode #48
 *  https://leetcode.com/problems/rotate-image/
 *  Pattern: MATRIX = TRANSPOSE + REVERSE ROWS (in-place)
 * ============================================================================
 *
 *  PROBLEM
 *    Rotate an n x n matrix 90 degrees CLOCKWISE, IN-PLACE (no second matrix).
 *      1 2 3        7 4 1
 *      4 5 6   ->   8 5 2
 *      7 8 9        9 6 3
 *
 *  ---------------------------------------------------------------------------
 *  With a second matrix it's easy: rotated[j][n-1-i] = m[i][j]. But we must do it in-place.
 *
 *  THE TRICK: clockwise rotation = TRANSPOSE, then REVERSE EACH ROW
 *    1. transpose (swap m[i][j] with m[j][i]: rows become columns)
 *         1 4 7
 *         2 5 8
 *         3 6 9
 *    2. reverse every row
 *         7 4 1
 *         8 5 2
 *         9 6 3     done
 *  Each step is easy to do in-place. Transpose: only swap ABOVE the diagonal
 *  (j > i), otherwise every pair gets swapped twice and nothing changes!
 *
 *  OTHER ROTATIONS
 *    counter-clockwise 90 = transpose, then reverse each COLUMN (or reverse rows first, then transpose)
 *    180                  = reverse rows' order + reverse each row
 *
 *  COMPLEXITY   Time O(n^2) (every cell is touched)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M08_RotateImage {

    static void rotate(int[][] m) {
        int n = m.length;
        // 1. transpose
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {         // j > i: upper triangle only
                int t = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = t;
            }
        }
        // 2. reverse each row
        for (int[] row : m) {
            for (int l = 0, r = n - 1; l < r; l++, r--) {
                int t = row[l];
                row[l] = row[r];
                row[r] = t;
            }
        }
    }

    static int[][] run(int[][] m) {
        rotate(m);
        return m;
    }

    public static void main(String[] args) {
        check("3x3", new int[][]{{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}, run(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));
        check("4x4", new int[][]{{15, 13, 2, 5}, {14, 3, 4, 1}, {12, 6, 8, 9}, {16, 7, 10, 11}},
                run(new int[][]{{5, 1, 9, 11}, {2, 4, 8, 10}, {13, 3, 6, 7}, {15, 14, 12, 16}}));
        check("1x1", new int[][]{{1}}, run(new int[][]{{1}}));
        int[][] m = {{1, 2}, {3, 4}};
        for (int i = 0; i < 4; i++) rotate(m);
        check("4 rotations = original", new int[][]{{1, 2}, {3, 4}}, m);
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
