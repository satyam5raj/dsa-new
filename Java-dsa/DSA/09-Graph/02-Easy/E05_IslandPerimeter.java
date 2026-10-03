/*
 * ============================================================================
 *  EASY 05 : ISLAND PERIMETER  ⭐ bonus        LeetCode #463
 *  https://leetcode.com/problems/island-perimeter/
 *  Pattern: GRID counting - each land cell adds 4, each shared edge removes 2
 * ============================================================================
 *
 *  PROBLEM
 *    Grid of 1 (land) / 0 (water) with exactly one island (no lakes inside).
 *    Return the island's perimeter.
 *      [[0,1,0,0],
 *       [1,1,1,0],
 *       [0,1,0,0],
 *       [1,1,0,0]] -> 16
 *
 *  ---------------------------------------------------------------------------
 *  COUNTING: every land cell has 4 sides. Each pair of ADJACENT land cells hides
 *  2 sides (one from each). So:
 *       perimeter = 4 * landCells - 2 * adjacentPairs
 *  Count each pair once by looking only UP and LEFT from each land cell.
 *
 *  DFS VERSION (the grid-graph way): walk the island; every step that leaves the
 *  grid or lands in water crosses one edge of the perimeter -> count it.
 *  Both are O(rows * cols); the counting version needs no recursion.
 *
 *  DRY RUN the example: 7 land cells -> 28; adjacent pairs: 6 -> 28 - 12 = 16
 *
 *  COMPLEXITY   Time O(rows * cols)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E05_IslandPerimeter {

    static int islandPerimeter(int[][] grid) {
        int land = 0, shared = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] != 1) continue;
                land++;
                if (r > 0 && grid[r - 1][c] == 1) shared++;      // neighbour above
                if (c > 0 && grid[r][c - 1] == 1) shared++;      // neighbour to the left
            }
        }
        return 4 * land - 2 * shared;
    }

    static int dfsPerimeter(int[][] grid) {
        boolean[][] seen = new boolean[grid.length][grid[0].length];
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[0].length; c++)
                if (grid[r][c] == 1) return dfs(grid, r, c, seen);
        return 0;
    }

    static int dfs(int[][] g, int r, int c, boolean[][] seen) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] == 0) return 1;   // crossed an edge
        if (seen[r][c]) return 0;
        seen[r][c] = true;
        return dfs(g, r + 1, c, seen) + dfs(g, r - 1, c, seen) + dfs(g, r, c + 1, seen) + dfs(g, r, c - 1, seen);
    }

    public static void main(String[] args) {
        int[][] g = {{0, 1, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 0}, {1, 1, 0, 0}};
        check("example", 16, islandPerimeter(g));
        check("[[1]]", 4, islandPerimeter(new int[][]{{1}}));
        check("[[1,0]]", 4, islandPerimeter(new int[][]{{1, 0}}));
        check("DFS version", 16, dfsPerimeter(g));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            // grow one connected island by random walk
            int R = 1 + rnd.nextInt(8), C = 1 + rnd.nextInt(8);
            int[][] a = new int[R][C];
            int r = rnd.nextInt(R), c = rnd.nextInt(C);
            for (int s = 0; s < R * C; s++) {
                a[r][c] = 1;
                int d = rnd.nextInt(4);
                int nr = r + (d == 0 ? 1 : d == 1 ? -1 : 0), nc = c + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (nr >= 0 && nr < R && nc >= 0 && nc < C) { r = nr; c = nc; }
            }
            ok &= islandPerimeter(a) == dfsPerimeter(a);
        }
        check("500 random islands: counting == DFS", true, ok);
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
