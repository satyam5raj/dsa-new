/*
 * ============================================================================
 *  MEDIUM 01 : NUMBER OF ISLANDS             LeetCode #200
 *  https://leetcode.com/problems/number-of-islands/
 *  Pattern: COUNT CONNECTED COMPONENTS in a grid (each unvisited '1' starts one)
 * ============================================================================
 *
 *  PROBLEM
 *    '1' = land, '0' = water. An island = land cells connected horizontally or
 *    vertically. Count the islands.
 *      11000
 *      11000      -> 3
 *      00100
 *      00011
 *
 *  ---------------------------------------------------------------------------
 *  THIS IS "COUNT COMPONENTS" (GraphTraversal) on an implicit grid graph:
 *    for every cell:
 *      if it's unvisited land: count++, then flood the WHOLE island (DFS/BFS)
 *      so none of its cells start another count
 *  "Sinking" the island (overwrite '1' with '0') doubles as the visited marker.
 *  (Ask whether you may modify the input; if not, use a boolean[][] visited.)
 *
 *  THREE WAYS, same O(R*C): DFS (shortest code), BFS (no deep recursion),
 *  Union-Find (union adjacent land cells; count = number of roots - 11-Union-Find).
 *
 *  DRY RUN: (0,0) is land -> count 1, sink the 2x2 block; (2,2) -> count 2;
 *  (3,3) -> count 3, sinks (3,4) too.
 *
 *  COMPLEXITY   Time O(R * C)   Space O(R * C) worst case (recursion / queue)
 * ============================================================================
 */

import java.util.*;

public class M01_NumberOfIslands {

    static int numIslands(char[][] grid) {
        int count = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == '1') {
                    count++;                     // a new island
                    sink(grid, r, c);            // erase it so we don't count it again
                }
            }
        }
        return count;
    }

    static void sink(char[][] g, int r, int c) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != '1') return;
        g[r][c] = '0';
        sink(g, r + 1, c);
        sink(g, r - 1, c);
        sink(g, r, c + 1);
        sink(g, r, c - 1);
    }

    static int numIslandsBfs(char[][] grid) {
        int R = grid.length, C = grid[0].length, count = 0;
        boolean[][] seen = new boolean[R][C];
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                if (grid[r][c] != '1' || seen[r][c]) continue;
                count++;
                Queue<int[]> q = new ArrayDeque<>();
                q.add(new int[]{r, c});
                seen[r][c] = true;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    for (int[] d : dirs) {
                        int nr = p[0] + d[0], nc = p[1] + d[1];
                        if (nr >= 0 && nr < R && nc >= 0 && nc < C && grid[nr][nc] == '1' && !seen[nr][nc]) {
                            seen[nr][nc] = true;
                            q.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        return count;
    }

    static char[][] grid(String... rows) { return Arrays.stream(rows).map(String::toCharArray).toArray(char[][]::new); }

    public static void main(String[] args) {
        check("example (3 islands)", 3, numIslands(grid("11000", "11000", "00100", "00011")));
        check("one big island", 1, numIslands(grid("11110", "11010", "11000", "00000")));
        check("diagonals don't connect", 2, numIslands(grid("10", "01")));
        check("BFS version", 3, numIslandsBfs(grid("11000", "11000", "00100", "00011")));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int R = 1 + rnd.nextInt(10), C = 1 + rnd.nextInt(10);
            char[][] a = new char[R][C];
            for (char[] row : a) for (int c = 0; c < C; c++) row[c] = rnd.nextInt(5) < 2 ? '1' : '0';
            char[][] b = Arrays.stream(a).map(char[]::clone).toArray(char[][]::new);
            ok &= numIslandsBfs(b) == numIslands(a);
        }
        check("500 random grids: DFS == BFS", true, ok);
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
