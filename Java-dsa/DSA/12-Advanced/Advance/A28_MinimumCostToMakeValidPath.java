/*
 * ============================================================================
 *  ADVANCE 28 : MINIMUM COST TO MAKE AT LEAST ONE VALID PATH IN A GRID  ⭐ bonus   LeetCode #1368
 *  https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/
 *  Pattern: 0-1 BFS with a DEQUE (cost-0 edges to the front, cost-1 edges to the back)
 * ============================================================================
 *
 *  PROBLEM
 *    Each cell has a sign: 1 = go right, 2 = left, 3 = down, 4 = up. Starting at the
 *    top-left you follow the signs. Changing one cell's sign costs 1. Return the minimum
 *    total cost so that following the signs reaches the bottom-right.
 *      [[1,1,1,1],
 *       [2,2,2,2],     ->  3
 *       [1,1,1,1],
 *       [2,2,2,2]]
 *
 *  ---------------------------------------------------------------------------
 *  AS A GRAPH: from every cell there are 4 moves. Moving the way the sign points is
 *  FREE (cost 0); any other direction means changing the sign (cost 1). Find the
 *  cheapest path from the top-left to the bottom-right.
 *
 *  DIJKSTRA works (heap ordered by cost): O(mn log mn).
 *  0-1 BFS is faster when every edge costs 0 or 1: use a DEQUE instead of a heap.
 *    free move  -> addFirst (same cost: handle it before anything more expensive)
 *    cost-1 move -> addLast
 *  The deque then always holds costs d (at the front) and d+1 (at the back), exactly
 *  like a heap would order them - in O(1) per operation. O(mn) total.
 *
 *  DRY RUN [[1,2],[4,3]]   (0,0)=right  (0,1)=left  (1,0)=up  (1,1)=down
 *    pop (0,0) cost 0: right -> (0,1) cost 0 (front);  down -> (1,0) cost 1 (back)
 *    pop (0,1) cost 0: down -> (1,1) cost 1 (back)  [its sign says left]
 *    pop (1,0) cost 1: right -> (1,1) cost 2, not better
 *    pop (1,1) cost 1  -> answer 1
 *
 *  COMPLEXITY   O(m n)   Space O(m n)
 * ============================================================================
 */

import java.util.*;

public class A28_MinimumCostToMakeValidPath {

    static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};       // index + 1 = the sign: 1 right, 2 left, 3 down, 4 up

    // ---------------- Referee: relax every cell until nothing changes (Bellman-Ford style) ----------------
    static int minCostRelax(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] cost = new int[m][n];
        for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
        cost[0][0] = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r = 0; r < m; r++)
                for (int c = 0; c < n; c++) {
                    if (cost[r][c] == Integer.MAX_VALUE) continue;
                    for (int d = 0; d < 4; d++) {
                        int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
                        if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                        int nd = cost[r][c] + (grid[r][c] == d + 1 ? 0 : 1);
                        if (nd < cost[nr][nc]) { cost[nr][nc] = nd; changed = true; }
                    }
                }
        }
        return cost[m - 1][n - 1];
    }

    // ---------------- Approach 1: Dijkstra with a heap ----------------
    static int minCostDijkstra(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] cost = new int[m][n];
        for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
        cost[0][0] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));   // {cost, r, c}
        heap.add(new int[]{0, 0, 0});
        while (!heap.isEmpty()) {
            int[] cur = heap.poll();
            int r = cur[1], c = cur[2];
            if (cur[0] > cost[r][c]) continue;                            // stale
            for (int d = 0; d < 4; d++) {
                int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                int nd = cur[0] + (grid[r][c] == d + 1 ? 0 : 1);
                if (nd < cost[nr][nc]) { cost[nr][nc] = nd; heap.add(new int[]{nd, nr, nc}); }
            }
        }
        return cost[m - 1][n - 1];
    }

    // ---------------- Approach 2: 0-1 BFS with a deque (main solution) ----------------
    static int minCost(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] cost = new int[m][n];
        for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
        cost[0][0] = 0;
        Deque<int[]> deque = new ArrayDeque<>();                          // {cost, r, c}
        deque.add(new int[]{0, 0, 0});
        while (!deque.isEmpty()) {
            int[] cur = deque.pollFirst();
            int r = cur[1], c = cur[2];
            if (cur[0] > cost[r][c]) continue;                            // already reached more cheaply
            if (r == m - 1 && c == n - 1) return cur[0];                  // the front always has the smallest cost
            for (int d = 0; d < 4; d++) {
                int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                boolean free = grid[r][c] == d + 1;
                int nd = cur[0] + (free ? 0 : 1);
                if (nd < cost[nr][nc]) {
                    cost[nr][nc] = nd;
                    if (free) deque.addFirst(new int[]{nd, nr, nc});      // same cost: go first
                    else deque.addLast(new int[]{nd, nr, nc});            // one more: go last
                }
            }
        }
        return cost[m - 1][n - 1];
    }

    public static void main(String[] args) {
        int[][] ex1 = {{1, 1, 1, 1}, {2, 2, 2, 2}, {1, 1, 1, 1}, {2, 2, 2, 2}};
        check("example 1", 3, minCost(ex1));
        check("example 1 (Dijkstra)", 3, minCostDijkstra(ex1));
        check("example 2 (already valid)", 0, minCost(new int[][]{{1, 1, 3}, {3, 2, 2}, {1, 1, 4}}));
        check("example 3", 1, minCost(new int[][]{{1, 2}, {4, 3}}));
        check("single cell", 0, minCost(new int[][]{{4}}));

        Random rnd = new Random(28);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int m = 1 + rnd.nextInt(6), n = 1 + rnd.nextInt(6);
            int[][] g = new int[m][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = 1 + rnd.nextInt(4);
            int want = minCostRelax(g);
            ok &= minCost(g) == want && minCostDijkstra(g) == want;
        }
        check("2,000 random grids: 0-1 BFS == Dijkstra == relaxation", true, ok);
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
