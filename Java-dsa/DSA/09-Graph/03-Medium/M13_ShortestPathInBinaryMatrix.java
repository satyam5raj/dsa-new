/*
 * ============================================================================
 *  MEDIUM 13 : SHORTEST PATH IN BINARY MATRIX   LeetCode #1091
 *  https://leetcode.com/problems/shortest-path-in-binary-matrix/
 *  Pattern: BFS on a grid with 8 directions (unweighted shortest path)
 * ============================================================================
 *
 *  PROBLEM
 *    n x n grid of 0 (open) / 1 (blocked). Length (number of CELLS) of the shortest
 *    path from top-left to bottom-right moving in 8 directions; -1 if none.
 *      [[0,1],[1,0]]                -> 2
 *      [[0,0,0],[1,1,0],[1,1,0]]    -> 4
 *
 *  ---------------------------------------------------------------------------
 *  Every move costs the same -> BFS finds the shortest path (the first time BFS
 *  reaches a cell, it's via the fewest steps). DFS would NOT: it finds A path,
 *  not the shortest, and exploring all paths is exponential.
 *
 *  DETAILS
 *    - 8 neighbours: all (dr, dc) in {-1,0,1}^2 except (0,0)
 *    - if the start or the end is blocked -> -1 immediately
 *    - mark visited WHEN ENQUEUEING (write 1 into the grid, or a seen[][])
 *    - the answer counts CELLS, so the start has distance 1
 *
 *  (A* search with a heuristic like Chebyshev distance can be faster in practice;
 *   BFS is what interviewers expect.)
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M13_ShortestPathInBinaryMatrix {

    static int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) return -1;
        boolean[][] seen = new boolean[n][n];
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{0, 0});
        seen[0][0] = true;
        int dist = 1;                                          // counting cells
        while (!q.isEmpty()) {
            for (int i = q.size(); i > 0; i--) {
                int[] p = q.poll();
                if (p[0] == n - 1 && p[1] == n - 1) return dist;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int r = p[0] + dr, c = p[1] + dc;
                        if (r >= 0 && r < n && c >= 0 && c < n && grid[r][c] == 0 && !seen[r][c]) {
                            seen[r][c] = true;
                            q.add(new int[]{r, c});
                        }
                    }
                }
            }
            dist++;
        }
        return -1;
    }

    // referee: Bellman-Ford-style relaxation until nothing changes
    static int relax(int[][] g) {
        int n = g.length, INF = 1_000_000;
        if (g[0][0] == 1) return -1;
        int[][] d = new int[n][n];
        for (int[] row : d) Arrays.fill(row, INF);
        d[0][0] = 1;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
                if (g[r][c] == 1 || d[r][c] == INF) continue;
                for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr, nc = c + dc;
                    if (nr >= 0 && nr < n && nc >= 0 && nc < n && g[nr][nc] == 0 && d[r][c] + 1 < d[nr][nc]) { d[nr][nc] = d[r][c] + 1; changed = true; }
                }
            }
        }
        return d[n - 1][n - 1] == INF ? -1 : d[n - 1][n - 1];
    }

    public static void main(String[] args) {
        check("[[0,1],[1,0]]", 2, shortestPathBinaryMatrix(new int[][]{{0, 1}, {1, 0}}));
        check("[[0,0,0],[1,1,0],[1,1,0]]", 4, shortestPathBinaryMatrix(new int[][]{{0, 0, 0}, {1, 1, 0}, {1, 1, 0}}));
        check("blocked start", -1, shortestPathBinaryMatrix(new int[][]{{1, 0, 0}, {1, 1, 0}, {1, 1, 0}}));
        check("[[0]]", 1, shortestPathBinaryMatrix(new int[][]{{0}}));
        Random rnd = new Random(13);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(7);
            int[][] g = new int[n][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(10) < 3 ? 1 : 0;
            ok &= shortestPathBinaryMatrix(g) == relax(g);
        }
        check("500 random grids vs relaxation", true, ok);
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
