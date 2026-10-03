/*
 * ============================================================================
 *  EASY 02 : FLOOD FILL  ⭐ bonus              LeetCode #733
 *  https://leetcode.com/problems/flood-fill/
 *  Pattern: GRID = IMPLICIT GRAPH; DFS over 4-directional neighbours
 * ============================================================================
 *
 *  PROBLEM
 *    Paint-bucket tool: starting at (sr, sc), recolour that pixel and every pixel
 *    connected to it (4 directions) that has the SAME original colour.
 *      image [[1,1,1],[1,1,0],[1,0,1]], (1,1), color 2 -> [[2,2,2],[2,2,0],[2,0,1]]
 *        (the bottom-right 1 is NOT connected: only diagonally)
 *
 *  ---------------------------------------------------------------------------
 *  THE GRID TEMPLATE (Number of Islands, Rotting Oranges, Surrounded Regions...)
 *    dfs(r, c):
 *      if out of bounds OR not the colour we're replacing -> return
 *      paint it               <- painting doubles as "visited" (no extra array)
 *      dfs on the 4 neighbours
 *
 *  THE TRAP: if newColor == the original colour, painting doesn't change anything,
 *  so "visited" never gets marked and the DFS recurses forever. Return early.
 *
 *  DEEP RECURSION: a 1000x1000 single-colour image means a 10^6-deep recursion ->
 *  StackOverflowError. An explicit stack (or BFS queue) avoids it - shown below.
 *
 *  COMPLEXITY   Time O(rows * cols)   Space O(rows * cols) worst case (stack)
 * ============================================================================
 */

import java.util.*;

public class E02_FloodFill {

    static int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int original = image[sr][sc];
        if (original == color) return image;                 // nothing to do (and avoids infinite recursion)
        dfs(image, sr, sc, original, color);
        return image;
    }

    static void dfs(int[][] img, int r, int c, int from, int to) {
        if (r < 0 || r >= img.length || c < 0 || c >= img[0].length || img[r][c] != from) return;
        img[r][c] = to;                                      // paint = mark visited
        dfs(img, r + 1, c, from, to);
        dfs(img, r - 1, c, from, to);
        dfs(img, r, c + 1, from, to);
        dfs(img, r, c - 1, from, to);
    }

    static int[][] floodFillIterative(int[][] image, int sr, int sc, int color) {
        int original = image[sr][sc];
        if (original == color) return image;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});
        image[sr][sc] = color;
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            for (int[] d : dirs) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r >= 0 && r < image.length && c >= 0 && c < image[0].length && image[r][c] == original) {
                    image[r][c] = color;
                    stack.push(new int[]{r, c});
                }
            }
        }
        return image;
    }

    public static void main(String[] args) {
        check("example", "[[2, 2, 2], [2, 2, 0], [2, 0, 1]]", floodFill(new int[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 1}}, 1, 1, 2));
        check("same colour (no-op)", "[[0, 0, 0], [0, 0, 0]]", floodFill(new int[][]{{0, 0, 0}, {0, 0, 0}}, 0, 0, 0));
        check("iterative", "[[2, 2, 2], [2, 2, 0], [2, 0, 1]]", floodFillIterative(new int[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 1}}, 1, 1, 2));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int R = 1 + rnd.nextInt(8), C = 1 + rnd.nextInt(8);
            int[][] a = new int[R][C];
            for (int[] row : a) for (int c = 0; c < C; c++) row[c] = rnd.nextInt(3);
            int[][] b = Arrays.stream(a).map(int[]::clone).toArray(int[][]::new);
            int sr = rnd.nextInt(R), sc = rnd.nextInt(C), col = rnd.nextInt(3);
            ok &= Arrays.deepEquals(floodFill(a, sr, sc, col), floodFillIterative(b, sr, sc, col));
        }
        check("500 random images: recursive == iterative", true, ok);
        int[][] big = new int[1000][1000];
        floodFillIterative(big, 0, 0, 7);
        check("1000x1000 iterative (no stack overflow)", 7, big[999][999]);
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
