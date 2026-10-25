/*
 * ============================================================================
 *  ADVANCE 10 : TRAPPING RAIN WATER II  ⭐ bonus   LeetCode #407
 *  https://leetcode.com/problems/trapping-rain-water-ii/
 *  Pattern: MIN-HEAP of boundary cells + BFS inward (Dijkstra-like flood from outside)
 * ============================================================================
 *
 *  PROBLEM
 *    heightMap is an m x n grid of bar heights. How much rain water is trapped?
 *      [[1,4,3,1,3,2],
 *       [3,2,1,3,2,4],
 *       [2,3,3,2,3,1]]   ->  4      (inner cells: 2 -> +1, 1 -> +2, 2 -> +1)
 *
 *  ---------------------------------------------------------------------------
 *  WHY THE 1-D TRICK DOESN'T WORK: in 1-D, water at i = min(maxLeft, maxRight) - h.
 *  In 2-D, water can leak out in ANY direction along a winding path, so it's not just
 *  about the row and column.
 *
 *  THE RIGHT QUESTION: a cell's water level = the LOWEST "highest wall" over all paths
 *  from that cell to the outside. Water escapes over the weakest point.
 *
 *  THE ALGORITHM (think of water rising from the outside in):
 *    1. Push every border cell into a min-heap (they can't hold water).
 *    2. Pop the LOWEST cell - it is the weakest point of the current wall.
 *    3. For each unvisited neighbour: if it's lower, it fills up to that level:
 *         water += level - neighbour height
 *       push it with height max(level, neighbour height) - it is now part of the wall.
 *  The heap always processes the lowest wall first, exactly like Dijkstra processes the
 *  closest node first, so every cell's level is final when it's reached.
 *
 *  DRY RUN (example 1; the inner cells are (1,1)..(1,4))
 *    pop (0,3) level 1 -> (1,3) h3: +0, push level 3
 *    pop the border 1s and 2s (no unvisited neighbours), then (2,1) level 3
 *                      -> (1,1) h2: +1, push level 3
 *    pop (1,3) level 3 -> (1,4) h2: +1 and (1,2) h1: +2
 *    the rest only meets visited cells                                   water = 4
 *
 *  COMPLEXITY   O(m n log(m n))   Space O(m n)
 * ============================================================================
 */

import java.util.*;

public class A10_TrappingRainWaterII {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // ---------------- Approach 1: relax water levels until nothing changes (the referee) ----------------
    static int trapRainWaterRelax(int[][] h) {
        int m = h.length, n = h[0].length;
        int[][] level = new int[m][n];
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                level[r][c] = (r == 0 || c == 0 || r == m - 1 || c == n - 1) ? h[r][c] : Integer.MAX_VALUE;
        boolean changed = true;
        while (changed) {                                                // like Bellman-Ford
            changed = false;
            for (int r = 1; r < m - 1; r++)
                for (int c = 1; c < n - 1; c++) {
                    int lowestNeighbour = Integer.MAX_VALUE;
                    for (int[] d : DIRS) lowestNeighbour = Math.min(lowestNeighbour, level[r + d[0]][c + d[1]]);
                    int want = Math.max(h[r][c], lowestNeighbour);
                    if (want < level[r][c]) { level[r][c] = want; changed = true; }
                }
        }
        int water = 0;
        for (int r = 0; r < m; r++) for (int c = 0; c < n; c++) water += level[r][c] - h[r][c];
        return water;
    }

    // ---------------- Approach 2: min-heap from the border inward (main solution) ----------------
    static int trapRainWater(int[][] heightMap) {
        int m = heightMap.length, n = heightMap[0].length;
        if (m < 3 || n < 3) return 0;
        boolean[][] visited = new boolean[m][n];
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[2], b[2]));   // {r, c, level}
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                if (r == 0 || c == 0 || r == m - 1 || c == n - 1) {
                    heap.add(new int[]{r, c, heightMap[r][c]});
                    visited[r][c] = true;
                }
        int water = 0;
        while (!heap.isEmpty()) {
            int[] cell = heap.poll();                                     // the weakest point of the wall
            for (int[] d : DIRS) {
                int nr = cell[0] + d[0], nc = cell[1] + d[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n || visited[nr][nc]) continue;
                visited[nr][nc] = true;
                water += Math.max(0, cell[2] - heightMap[nr][nc]);        // fills up to the wall's level
                heap.add(new int[]{nr, nc, Math.max(cell[2], heightMap[nr][nc])});
            }
        }
        return water;
    }

    public static void main(String[] args) {
        check("example 1", 4, trapRainWater(new int[][]{{1, 4, 3, 1, 3, 2}, {3, 2, 1, 3, 2, 4}, {2, 3, 3, 2, 3, 1}}));
        check("example 2 (a bowl)", 10, trapRainWater(new int[][]{{3, 3, 3, 3, 3}, {3, 2, 2, 2, 3}, {3, 2, 1, 2, 3}, {3, 2, 2, 2, 3}, {3, 3, 3, 3, 3}}));
        check("leak through a gap", 0, trapRainWater(new int[][]{{3, 3, 3}, {3, 1, 0}, {3, 3, 3}}));
        check("too thin to hold water", 0, trapRainWater(new int[][]{{5, 1, 5}, {5, 1, 5}}));

        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            int m = 1 + rnd.nextInt(7), n = 1 + rnd.nextInt(7);
            int[][] g = new int[m][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(8);
            ok &= trapRainWater(g) == trapRainWaterRelax(g);
        }
        check("1,500 random maps vs relaxation", true, ok);
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
