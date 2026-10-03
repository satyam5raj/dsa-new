/*
 * ============================================================================
 *  MEDIUM 14 : PATH WITH MINIMUM EFFORT      LeetCode #1631
 *  https://leetcode.com/problems/path-with-minimum-effort/
 *  Pattern: DIJKSTRA where a path's cost is its MAX edge (a "minimax" path)
 * ============================================================================
 *
 *  PROBLEM
 *    heights grid. A route's EFFORT = the largest absolute height difference
 *    between two consecutive cells on it. Minimum effort from top-left to bottom-right?
 *      [[1,2,2],[3,8,2],[5,3,5]] -> 2   (1-3-5-3-5: steps 2,2,2,2)
 *
 *  ---------------------------------------------------------------------------
 *  It's a shortest-path problem, but a path's cost isn't a SUM, it's a MAX:
 *      cost(path + edge) = max(cost(path), edge weight)
 *  DIJKSTRA STILL WORKS, because extending a path never makes its cost smaller
 *  (max is monotonic, just like adding non-negative weights). Replace
 *  "dist[u] + w" with "max(dist[u], w)" and keep everything else.
 *
 *  ALTERNATIVES
 *    - BINARY SEARCH on the answer E: "can I get there using only steps <= E?"
 *      is a plain BFS; feasibility is monotonic in E. O(RC log(maxH))
 *    - UNION-FIND: add edges in increasing order of weight until start and end
 *      are connected; the last edge added is the answer (Kruskal-style).
 *
 *  Swim in Rising Water (H06) is the same problem with cell weights.
 *  COMPLEXITY   Time O(RC log(RC))   Space O(RC)
 * ============================================================================
 */

import java.util.*;

public class M14_PathWithMinimumEffort {

    static int minimumEffortPath(int[][] h) {
        int R = h.length, C = h[0].length;
        int[][] effort = new int[R][C];
        for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
        effort[0][0] = 0;
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));   // {effort, r, c}
        heap.add(new int[]{0, 0, 0});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int e = top[0], r = top[1], c = top[2];
            if (r == R - 1 && c == C - 1) return e;           // the first time we POP the target, it's optimal
            if (e > effort[r][c]) continue;                   // stale
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= R || nc < 0 || nc >= C) continue;
                int ne = Math.max(e, Math.abs(h[nr][nc] - h[r][c]));   // MAX instead of SUM
                if (ne < effort[nr][nc]) {
                    effort[nr][nc] = ne;
                    heap.add(new int[]{ne, nr, nc});
                }
            }
        }
        return 0;
    }

    // binary search on the answer + BFS feasibility
    static int binarySearch(int[][] h) {
        int lo = 0, hi = 1_000_000;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (canReach(h, mid)) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    static boolean canReach(int[][] h, int limit) {
        int R = h.length, C = h[0].length;
        boolean[][] seen = new boolean[R][C];
        Deque<int[]> q = new ArrayDeque<>(List.<int[]>of(new int[]{0, 0}));
        seen[0][0] = true;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            if (p[0] == R - 1 && p[1] == C - 1) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r >= 0 && r < R && c >= 0 && c < C && !seen[r][c] && Math.abs(h[r][c] - h[p[0]][p[1]]) <= limit) { seen[r][c] = true; q.add(new int[]{r, c}); }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        check("example 1", 2, minimumEffortPath(new int[][]{{1, 2, 2}, {3, 8, 2}, {5, 3, 5}}));
        check("example 2", 1, minimumEffortPath(new int[][]{{1, 2, 3}, {3, 8, 4}, {5, 3, 5}}));
        check("all equal path exists", 0, minimumEffortPath(new int[][]{{1, 2, 1, 1, 1}, {1, 2, 1, 2, 1}, {1, 2, 1, 2, 1}, {1, 2, 1, 2, 1}, {1, 1, 1, 2, 1}}));
        check("single cell", 0, minimumEffortPath(new int[][]{{7}}));
        Random rnd = new Random(14);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int R = 1 + rnd.nextInt(7), C = 1 + rnd.nextInt(7);
            int[][] h = new int[R][C];
            for (int[] row : h) for (int c = 0; c < C; c++) row[c] = 1 + rnd.nextInt(20);
            ok &= minimumEffortPath(h) == binarySearch(h);
        }
        check("300 random grids: Dijkstra == binary search", true, ok);
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
