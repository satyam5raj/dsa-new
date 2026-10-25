/*
 * ============================================================================
 *  ADVANCE 08 : SHORTEST PATH IN A GRID WITH OBSTACLES ELIMINATION  ⭐ bonus   LeetCode #1293
 *  https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/
 *  Pattern: BFS over STATES (row, col, obstacles left), not just cells
 * ============================================================================
 *
 *  PROBLEM
 *    m x n grid, 0 = empty, 1 = obstacle. Move up/down/left/right. You may walk through
 *    at most k obstacles (eliminating them). Return the minimum number of steps from the
 *    top-left to the bottom-right, or -1.
 *      grid [[0,0,0],[1,1,0],[0,0,0],[0,1,1],[0,0,0]], k = 1  ->  6
 *
 *  ---------------------------------------------------------------------------
 *  WHY PLAIN BFS FAILS: "visited[r][c]" says a cell is done after the first visit. But
 *  reaching a cell with 0 eliminations left is NOT the same as reaching it with 3 left -
 *  the second one can still go through walls. The STATE must include what's left:
 *        state = (r, c, remaining)       at most m * n * (k + 1) states
 *  BFS over states still finds the fewest steps, because every move costs 1.
 *
 *  APPROACH 1: seen[r][c][remaining] booleans.
 *  APPROACH 2 (main): best[r][c] = the MOST eliminations left we've ever arrived with.
 *    BFS goes level by level, so an earlier arrival with MORE left dominates a later one
 *    with the same or fewer left -> skip it. One int per cell instead of k+1 booleans.
 *  SHORTCUT: if k >= m + n - 3 we can bulldoze straight along the edge: answer m + n - 2.
 *    (the path has m+n-2 steps through m+n-3 cells between the corners)
 *
 *  DRY RUN (small) grid [[0,1],[1,0]], k = 1          shortcut: k=1 >= 2+2-3=1 -> 2
 *    without the shortcut: level 0 (0,0,1); level 1 (0,1,0) and (1,0,0);
 *    level 2 (1,1,0) reached -> 2
 *
 *  COMPLEXITY   O(m * n * k) time and space (approach 1);  approach 2 uses O(m * n) memory
 * ============================================================================
 */

import java.util.*;

public class A08_ShortestPathWithObstaclesElimination {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // ---------------- Referee: try every simple path (tiny grids only) ----------------
    static int shortestPathBrute(int[][] grid, int k) {
        int m = grid.length, n = grid[0].length;
        boolean[][] onPath = new boolean[m][n];
        onPath[0][0] = true;
        int best = dfs(grid, 0, 0, k, 0, onPath);
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int dfs(int[][] grid, int r, int c, int left, int steps, boolean[][] onPath) {
        int m = grid.length, n = grid[0].length;
        if (r == m - 1 && c == n - 1) return steps;
        int best = Integer.MAX_VALUE;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= m || nc < 0 || nc >= n || onPath[nr][nc]) continue;
            int nl = left - grid[nr][nc];
            if (nl < 0) continue;
            onPath[nr][nc] = true;
            best = Math.min(best, dfs(grid, nr, nc, nl, steps + 1, onPath));
            onPath[nr][nc] = false;
        }
        return best;
    }

    // ---------------- Approach 1: BFS with a 3-D visited array ----------------
    static int shortestPath3D(int[][] grid, int k) {
        int m = grid.length, n = grid[0].length;
        boolean[][][] seen = new boolean[m][n][k + 1];
        Deque<int[]> queue = new ArrayDeque<>();                         // {r, c, remaining}
        queue.add(new int[]{0, 0, k});
        seen[0][0][k] = true;
        for (int steps = 0; !queue.isEmpty(); steps++) {
            for (int size = queue.size(); size > 0; size--) {
                int[] cur = queue.poll();
                if (cur[0] == m - 1 && cur[1] == n - 1) return steps;
                for (int[] d : DIRS) {
                    int nr = cur[0] + d[0], nc = cur[1] + d[1];
                    if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                    int left = cur[2] - grid[nr][nc];
                    if (left < 0 || seen[nr][nc][left]) continue;
                    seen[nr][nc][left] = true;
                    queue.add(new int[]{nr, nc, left});
                }
            }
        }
        return -1;
    }

    // ---------------- Approach 2: BFS keeping the best "remaining" per cell (main solution) ----------------
    static int shortestPath(int[][] grid, int k) {
        int m = grid.length, n = grid[0].length;
        if (k >= m + n - 3) return m + n - 2;                            // enough to bulldoze any straight route
        int[][] best = new int[m][n];
        for (int[] row : best) Arrays.fill(row, -1);
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, 0, k});
        best[0][0] = k;
        for (int steps = 0; !queue.isEmpty(); steps++) {
            for (int size = queue.size(); size > 0; size--) {
                int[] cur = queue.poll();
                if (cur[0] == m - 1 && cur[1] == n - 1) return steps;
                for (int[] d : DIRS) {
                    int nr = cur[0] + d[0], nc = cur[1] + d[1];
                    if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                    int left = cur[2] - grid[nr][nc];
                    if (left <= best[nr][nc]) continue;                  // been here sooner with at least as many left
                    best[nr][nc] = left;
                    queue.add(new int[]{nr, nc, left});
                }
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[][] g1 = {{0, 0, 0}, {1, 1, 0}, {0, 0, 0}, {0, 1, 1}, {0, 0, 0}};
        check("example 1 (k=1)", 6, shortestPath(g1, 1));
        check("example 1 with k=0 (walk around)", 10, shortestPath(g1, 0));
        check("example 2 (blocked)", -1, shortestPath(new int[][]{{0, 1, 1}, {1, 1, 1}, {1, 0, 0}}, 1));
        check("single cell", 0, shortestPath(new int[][]{{0}}, 0));
        check("shortcut: enough eliminations", 2, shortestPath(new int[][]{{0, 1}, {1, 0}}, 1));

        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            int m = 1 + rnd.nextInt(4), n = 1 + rnd.nextInt(4), k = rnd.nextInt(4);
            int[][] g = new int[m][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(10) < 4 ? 1 : 0;
            g[0][0] = 0; g[m - 1][n - 1] = 0;
            int want = shortestPathBrute(g, k);
            ok &= shortestPath(g, k) == want && shortestPath3D(g, k) == want;
        }
        check("1,500 random grids: both BFS == every path", true, ok);
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
