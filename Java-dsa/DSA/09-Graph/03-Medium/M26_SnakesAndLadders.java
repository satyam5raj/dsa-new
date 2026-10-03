/*
 * ============================================================================
 *  MEDIUM 26 : SNAKES AND LADDERS            LeetCode #909
 *  https://leetcode.com/problems/snakes-and-ladders/
 *  Pattern: BFS over squares (each die roll = an edge) + the BOUSTROPHEDON index math
 * ============================================================================
 *
 *  PROBLEM
 *    n x n board, squares numbered 1..n^2 starting at the BOTTOM-LEFT and zig-zagging
 *    ("boustrophedon": left->right, then right->left on the next row up...).
 *    board[r][c] != -1 means a snake/ladder to that square. From square s you roll
 *    1..6 and move to s+1..s+6; if you land on a snake/ladder you MUST take it
 *    (only once - you don't chain). Fewest rolls to reach n^2, or -1.
 *
 *  ---------------------------------------------------------------------------
 *  GRAPH: square s -> up to 6 neighbours (after applying the snake/ladder).
 *  Every roll costs 1 -> BFS gives the minimum number of rolls.
 *
 *  THE TRICKY PART: square number -> (row, col)
 *      k = s - 1                         (0-based)
 *      rowFromBottom = k / n,  col = k % n
 *      row = n - 1 - rowFromBottom
 *      if rowFromBottom is ODD the row runs right-to-left: col = n - 1 - col
 *  e.g. n=6: square 1 -> (5,0), square 6 -> (5,5), square 7 -> (4,5), square 12 -> (4,0)
 *
 *  RULES TO GET RIGHT
 *    - mark visited on the DESTINATION after the snake/ladder (where you actually end up)
 *    - a ladder's end square is NOT re-checked for another ladder (no chaining)
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M26_SnakesAndLadders {

    static int snakesAndLadders(int[][] board) {
        int n = board.length, target = n * n;
        int[] dist = new int[target + 1];
        Arrays.fill(dist, -1);
        dist[1] = 0;
        Queue<Integer> q = new ArrayDeque<>(List.of(1));
        while (!q.isEmpty()) {
            int s = q.poll();
            if (s == target) return dist[s];
            for (int next = s + 1; next <= Math.min(s + 6, target); next++) {
                int[] rc = cell(next, n);
                int dest = board[rc[0]][rc[1]] == -1 ? next : board[rc[0]][rc[1]];   // take the snake/ladder
                if (dist[dest] == -1) {
                    dist[dest] = dist[s] + 1;
                    q.add(dest);
                }
            }
        }
        return -1;
    }

    // square number (1-based) -> {row, col} on the zig-zag board
    static int[] cell(int s, int n) {
        int k = s - 1, fromBottom = k / n, col = k % n;
        if (fromBottom % 2 == 1) col = n - 1 - col;          // odd rows run right-to-left
        return new int[]{n - 1 - fromBottom, col};
    }

    // referee: Bellman-Ford-style relaxation over squares
    static int relax(int[][] board) {
        int n = board.length, T = n * n, INF = 1_000_000;
        int[] d = new int[T + 1];
        Arrays.fill(d, INF);
        d[1] = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int s = 1; s < T; s++) {
                if (d[s] == INF) continue;
                for (int nx = s + 1; nx <= Math.min(s + 6, T); nx++) {
                    int[] rc = cell(nx, n);
                    int dest = board[rc[0]][rc[1]] == -1 ? nx : board[rc[0]][rc[1]];
                    if (d[s] + 1 < d[dest]) { d[dest] = d[s] + 1; changed = true; }
                }
            }
        }
        return d[T] == INF ? -1 : d[T];
    }

    public static void main(String[] args) {
        int[][] b = {
                {-1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1},
                {-1, 35, -1, -1, 13, -1},
                {-1, -1, -1, -1, -1, -1},
                {-1, 15, -1, -1, -1, -1}};
        check("example", 4, snakesAndLadders(b));
        check("[[-1,-1],[-1,3]]", 1, snakesAndLadders(new int[][]{{-1, -1}, {-1, 3}}));
        check("square 7 on 6x6 is (4,5)", "[4, 5]", cell(7, 6));
        check("square 12 on 6x6 is (4,0)", "[4, 0]", cell(12, 6));
        Random rnd = new Random(26);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 2 + rnd.nextInt(5);
            int[][] g = new int[n][n];
            for (int[] row : g) Arrays.fill(row, -1);
            for (int i = 0; i < n; i++) {                     // a few random snakes/ladders (not on squares 1 and n^2)
                int s = 2 + rnd.nextInt(n * n - 2);
                int[] rc = cell(s, n);
                g[rc[0]][rc[1]] = 1 + rnd.nextInt(n * n);
            }
            ok &= snakesAndLadders(g) == relax(g);
        }
        check("300 random boards vs relaxation", true, ok);
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
