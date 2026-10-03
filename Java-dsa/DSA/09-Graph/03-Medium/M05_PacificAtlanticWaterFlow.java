/*
 * ============================================================================
 *  MEDIUM 05 : PACIFIC ATLANTIC WATER FLOW   LeetCode #417
 *  https://leetcode.com/problems/pacific-atlantic-water-flow/
 *  Pattern: REVERSE the direction - search INWARD from the oceans (multi-source)
 * ============================================================================
 *
 *  PROBLEM
 *    heights grid. Pacific touches the top and left edges, Atlantic the bottom and
 *    right edges. Water flows from a cell to a neighbour with height <= its own.
 *    Return the cells from which water can reach BOTH oceans.
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: from every cell, DFS downhill and see which oceans you hit.
 *    O((R*C)^2) - each cell may explore the whole grid.
 *
 *  REVERSE THINKING: instead of "where can water from this cell go?", ask "which
 *  cells can reach the Pacific?". Start at ALL Pacific border cells at once and
 *  walk UPHILL (to neighbours with height >= current): every cell you reach can
 *  drain to the Pacific. Do the same for the Atlantic. Answer = cells in BOTH sets.
 *    Two traversals, O(R*C) total.
 *
 *  This "start from the targets, go backwards" trick is everywhere:
 *  Surrounded Regions (M06, from the border), Rotting Oranges (from all rotten),
 *  01 Matrix (from all zeros), Walls and Gates.
 *
 *  COMPLEXITY   Time O(R * C)   Space O(R * C)
 * ============================================================================
 */

import java.util.*;

public class M05_PacificAtlanticWaterFlow {

    static List<List<Integer>> pacificAtlantic(int[][] h) {
        int R = h.length, C = h[0].length;
        boolean[][] pac = new boolean[R][C], atl = new boolean[R][C];
        for (int r = 0; r < R; r++) { climb(h, r, 0, pac); climb(h, r, C - 1, atl); }   // left / right edges
        for (int c = 0; c < C; c++) { climb(h, 0, c, pac); climb(h, R - 1, c, atl); }   // top / bottom edges
        List<List<Integer>> result = new ArrayList<>();
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) if (pac[r][c] && atl[r][c]) result.add(List.of(r, c));
        return result;
    }

    // mark every cell reachable by walking UPHILL (or level) from (r, c)
    static void climb(int[][] h, int r, int c, boolean[][] reach) {
        if (reach[r][c]) return;
        reach[r][c] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < h.length && nc >= 0 && nc < h[0].length && !reach[nr][nc] && h[nr][nc] >= h[r][c]) {
                climb(h, nr, nc, reach);
            }
        }
    }

    // brute force: from each cell, flow DOWNHILL and see which oceans are reached
    static List<List<Integer>> brute(int[][] h) {
        int R = h.length, C = h[0].length;
        List<List<Integer>> out = new ArrayList<>();
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                boolean[][] seen = new boolean[R][C];
                boolean[] oceans = new boolean[2];
                flow(h, r, c, seen, oceans);
                if (oceans[0] && oceans[1]) out.add(List.of(r, c));
            }
        }
        return out;
    }

    static void flow(int[][] h, int r, int c, boolean[][] seen, boolean[] oceans) {
        seen[r][c] = true;
        if (r == 0 || c == 0) oceans[0] = true;
        if (r == h.length - 1 || c == h[0].length - 1) oceans[1] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < h.length && nc >= 0 && nc < h[0].length && !seen[nr][nc] && h[nr][nc] <= h[r][c]) flow(h, nr, nc, seen, oceans);
        }
    }

    public static void main(String[] args) {
        int[][] h = {{1, 2, 2, 3, 5}, {3, 2, 3, 4, 4}, {2, 4, 5, 3, 1}, {6, 7, 1, 4, 5}, {5, 1, 1, 2, 4}};
        check("example", "[[0, 4], [1, 3], [1, 4], [2, 2], [3, 0], [3, 1], [4, 0]]", pacificAtlantic(h));
        check("[[1]]", "[[0, 0]]", pacificAtlantic(new int[][]{{1}}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int R = 1 + rnd.nextInt(7), C = 1 + rnd.nextInt(7);
            int[][] g = new int[R][C];
            for (int[] row : g) for (int c = 0; c < C; c++) row[c] = rnd.nextInt(5);
            ok &= pacificAtlantic(g).equals(brute(g));
        }
        check("300 random grids vs per-cell search", true, ok);
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
