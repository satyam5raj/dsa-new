/*
 * ============================================================================
 *  MEDIUM 06 : SURROUNDED REGIONS            LeetCode #130
 *  https://leetcode.com/problems/surrounded-regions/
 *  Pattern: protect what touches the BORDER first, then flip everything else
 * ============================================================================
 *
 *  PROBLEM
 *    Board of 'X' and 'O'. Capture every region of 'O' that is completely
 *    surrounded by 'X' (flip it to 'X'). Regions touching the border survive.
 *      X X X X        X X X X
 *      X O O X   ->   X X X X
 *      X X O X        X X X X
 *      X O X X        X O X X     (bottom O touches the border: safe)
 *
 *  ---------------------------------------------------------------------------
 *  DIRECT APPROACH (hard): for each region, check whether it reaches the border,
 *  then flip it. Needs a two-pass per region.
 *
 *  REVERSE (like Pacific Atlantic): an 'O' survives  <=>  it's connected to a
 *  BORDER 'O'. So:
 *    1. DFS from every border 'O', marking its whole region as SAFE ('S')
 *    2. sweep the board: remaining 'O' -> 'X' (captured), 'S' -> 'O' (restore)
 *  Using the board itself for the 'S' marks: no extra memory.
 *
 *  COMPLEXITY   Time O(R * C)   Space O(R * C) recursion worst case
 * ============================================================================
 */

import java.util.*;

public class M06_SurroundedRegions {

    static void solve(char[][] board) {
        int R = board.length, C = board[0].length;
        for (int r = 0; r < R; r++) { markSafe(board, r, 0); markSafe(board, r, C - 1); }
        for (int c = 0; c < C; c++) { markSafe(board, 0, c); markSafe(board, R - 1, c); }
        for (char[] row : board) {
            for (int c = 0; c < C; c++) {
                if (row[c] == 'O') row[c] = 'X';         // never reached from the border: captured
                else if (row[c] == 'S') row[c] = 'O';    // safe: restore
            }
        }
    }

    static void markSafe(char[][] b, int r, int c) {
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != 'O') return;
        b[r][c] = 'S';
        markSafe(b, r + 1, c);
        markSafe(b, r - 1, c);
        markSafe(b, r, c + 1);
        markSafe(b, r, c - 1);
    }

    // brute force: for each 'O' region, BFS it; flip it if no cell is on the border
    static void brute(char[][] b) {
        int R = b.length, C = b[0].length;
        boolean[][] seen = new boolean[R][C];
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) {
            if (b[r][c] != 'O' || seen[r][c]) continue;
            List<int[]> region = new ArrayList<>();
            Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, c}));
            seen[r][c] = true;
            boolean border = false;
            while (!st.isEmpty()) {
                int[] p = st.pop();
                region.add(p);
                if (p[0] == 0 || p[1] == 0 || p[0] == R - 1 || p[1] == C - 1) border = true;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nr = p[0] + d[0], nc = p[1] + d[1];
                    if (nr >= 0 && nr < R && nc >= 0 && nc < C && b[nr][nc] == 'O' && !seen[nr][nc]) { seen[nr][nc] = true; st.push(new int[]{nr, nc}); }
                }
            }
            if (!border) for (int[] p : region) b[p[0]][p[1]] = 'X';
        }
    }

    static char[][] board(String... rows) { return Arrays.stream(rows).map(String::toCharArray).toArray(char[][]::new); }

    public static void main(String[] args) {
        char[][] b = board("XXXX", "XOOX", "XXOX", "XOXX");
        solve(b);
        check("example", "[[X, X, X, X], [X, X, X, X], [X, X, X, X], [X, O, X, X]]", b);
        char[][] b2 = board("OOO", "OXO", "OOO");
        solve(b2);
        check("all on the border: nothing flips", "[[O, O, O], [O, X, O], [O, O, O]]", b2);
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int R = 1 + rnd.nextInt(8), C = 1 + rnd.nextInt(8);
            char[][] x = new char[R][C];
            for (char[] row : x) for (int c = 0; c < C; c++) row[c] = rnd.nextBoolean() ? 'O' : 'X';
            char[][] y = Arrays.stream(x).map(char[]::clone).toArray(char[][]::new);
            solve(x);
            brute(y);
            ok &= Arrays.deepEquals(x, y);
        }
        check("500 random boards vs region-by-region", true, ok);
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
