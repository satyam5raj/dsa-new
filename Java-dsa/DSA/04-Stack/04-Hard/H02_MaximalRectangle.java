/*
 * ============================================================================
 *  HARD 02 : MAXIMAL RECTANGLE  ⭐ bonus (builds on Largest Rectangle in Histogram)   LeetCode #85
 *  https://leetcode.com/problems/maximal-rectangle/
 *  Pattern: TURN EACH ROW INTO A HISTOGRAM + MONOTONIC STACK
 * ============================================================================
 *
 *  FIRST: Largest Rectangle in Histogram (LeetCode #84, listed under Stack in
 *  leetcode.md) is fully explained in DSA/01-Arrays/04-Hard/H03. It's reused here
 *  as a helper. Read that file first.
 *
 *  PROBLEM
 *    A matrix of '0'/'1'. Find the area of the largest rectangle containing only 1s.
 *      1 0 1 0 0
 *      1 0 1 1 1
 *      1 1 1 1 1      -> 6   (rows 1-2, columns 2-4)
 *      1 0 0 1 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every top-left and bottom-right corner -> O((mn)^2) or worse.
 *
 *  REDUCTION TO HISTOGRAMS
 *    Look at the matrix row by row, treating the CURRENT row as the "ground".
 *    heights[j] = how many consecutive 1s stand on top of each other in column j,
 *                 ending at the current row (reset to 0 when the cell is '0').
 *      after row 0: [1,0,1,0,0]
 *      after row 1: [2,0,2,1,1]
 *      after row 2: [3,1,3,2,2]   -> the largest rectangle in THIS histogram is 6
 *      after row 3: [4,0,0,3,0]
 *    Any all-1 rectangle has a BOTTOM row; in that row's histogram it shows up as a
 *    rectangle under the bars. So: max over all rows of largestRectangle(heights).
 *
 *  COMPLEXITY   Time O(m * n) (an O(n) histogram pass per row)   Space O(n)
 *  THE LESSON: a hard 2D problem = a known 1D problem applied once per row.
 * ============================================================================
 */

import java.util.*;

public class H02_MaximalRectangle {

    static int maximalRectangle(char[][] matrix) {
        if (matrix.length == 0) return 0;
        int[] heights = new int[matrix[0].length];
        int best = 0;
        for (char[] row : matrix) {
            for (int j = 0; j < row.length; j++) {
                heights[j] = row[j] == '1' ? heights[j] + 1 : 0;    // grow the bar, or reset it
            }
            best = Math.max(best, largestRectangleArea(heights));
        }
        return best;
    }

    // Largest Rectangle in Histogram (explained in Arrays/H03): increasing stack of indices
    static int largestRectangleArea(int[] h) {
        Deque<Integer> stack = new ArrayDeque<>();
        int best = 0;
        for (int i = 0; i <= h.length; i++) {
            int cur = i == h.length ? 0 : h[i];
            while (!stack.isEmpty() && h[stack.peek()] > cur) {
                int height = h[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                best = Math.max(best, height * (i - left - 1));
            }
            stack.push(i);
        }
        return best;
    }

    static int brute(char[][] m) {
        int R = m.length, C = m[0].length, best = 0;
        for (int r1 = 0; r1 < R; r1++)
            for (int c1 = 0; c1 < C; c1++)
                for (int r2 = r1; r2 < R; r2++)
                    for (int c2 = c1; c2 < C; c2++) {
                        boolean all = true;
                        for (int r = r1; r <= r2 && all; r++)
                            for (int c = c1; c <= c2 && all; c++) all = m[r][c] == '1';
                        if (all) best = Math.max(best, (r2 - r1 + 1) * (c2 - c1 + 1));
                    }
        return best;
    }

    public static void main(String[] args) {
        char[][] m = {
                "10100".toCharArray(),
                "10111".toCharArray(),
                "11111".toCharArray(),
                "10010".toCharArray()};
        check("LeetCode example", 6, maximalRectangle(m));
        check("[[0]]", 0, maximalRectangle(new char[][]{{'0'}}));
        check("[[1]]", 1, maximalRectangle(new char[][]{{'1'}}));
        check("all ones 3x4", 12, maximalRectangle(new char[][]{"1111".toCharArray(), "1111".toCharArray(), "1111".toCharArray()}));
        Random rnd = new Random(85);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int R = 1 + rnd.nextInt(5), C = 1 + rnd.nextInt(5);
            char[][] g = new char[R][C];
            for (char[] row : g) for (int j = 0; j < C; j++) row[j] = rnd.nextInt(3) == 0 ? '0' : '1';
            ok &= maximalRectangle(g) == brute(g);
        }
        check("200 random grids vs brute force", true, ok);
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
