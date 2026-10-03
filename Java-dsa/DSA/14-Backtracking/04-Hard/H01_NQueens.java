/*
 * ============================================================================
 *  HARD 01 : N-QUEENS                        LeetCode #51
 *  https://leetcode.com/problems/n-queens/
 *  Pattern: one queen per ROW + O(1) attack checks with column / diagonal arrays
 * ============================================================================
 *
 *  PROBLEM
 *    Place n queens on an n x n board so that no two attack each other (same row,
 *    column or diagonal). Return every board.
 *      n=4 -> [[".Q..","...Q","Q...","..Q."], ["..Q.","Q...","...Q",".Q.."]]
 *
 *  ---------------------------------------------------------------------------
 *  REDUCE THE SEARCH: two queens can't share a row, so each ROW has exactly one
 *  queen. The decision at depth r is "which column for the queen of row r?"
 *  -> at most n^n leaves instead of C(n^2, n).
 *
 *  O(1) CONSTRAINT CHECK: a square (r, c) is attacked if
 *      cols[c]                  a queen is in column c
 *      diag[r + c]              same "/" diagonal: r + c is constant along it
 *      anti[r - c + n - 1]      same "\" diagonal: r - c is constant (shift to >= 0)
 *  Mark the three when placing, unmark when backtracking.
 *  (01-Basics/PruningAndConstraints measured: 19 million nodes without pruning vs
 *   2,057 with, for n = 8.)
 *
 *  DRY RUN n=4: row 0 col 0 -> row 1 col 2 -> row 2: all attacked -> back;
 *               row 1 col 3 -> row 2 col 1 -> row 3: all attacked -> back ... ;
 *               row 0 col 1 -> 3 -> 0 -> 2 ✓
 *
 *  COMPLEXITY   Time O(n!) roughly (each row has fewer free columns)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H01_NQueens {

    static List<List<String>> solveNQueens(int n) {
        List<List<String>> out = new ArrayList<>();
        int[] queenCol = new int[n];
        bt(n, 0, queenCol, new boolean[n], new boolean[2 * n - 1], new boolean[2 * n - 1], out);
        return out;
    }

    static void bt(int n, int r, int[] queenCol, boolean[] cols, boolean[] diag, boolean[] anti, List<List<String>> out) {
        if (r == n) { out.add(draw(queenCol)); return; }
        for (int c = 0; c < n; c++) {
            if (cols[c] || diag[r + c] || anti[r - c + n - 1]) continue;   // attacked
            queenCol[r] = c;
            cols[c] = diag[r + c] = anti[r - c + n - 1] = true;
            bt(n, r + 1, queenCol, cols, diag, anti, out);
            cols[c] = diag[r + c] = anti[r - c + n - 1] = false;           // undo
        }
    }

    static List<String> draw(int[] queenCol) {
        List<String> board = new ArrayList<>();
        for (int c : queenCol) {
            char[] row = new char[queenCol.length];
            Arrays.fill(row, '.');
            row[c] = 'Q';
            board.add(new String(row));
        }
        return board;
    }

    static boolean valid(List<String> board) {
        int n = board.size();
        List<int[]> q = new ArrayList<>();
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (board.get(r).charAt(c) == 'Q') q.add(new int[]{r, c});
        if (q.size() != n) return false;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
            int[] a = q.get(i), b = q.get(j);
            if (a[0] == b[0] || a[1] == b[1] || Math.abs(a[0] - b[0]) == Math.abs(a[1] - b[1])) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        check("n=4", "[[.Q.., ...Q, Q..., ..Q.], [..Q., Q..., ...Q, .Q..]]", solveNQueens(4));
        check("n=1", "[[Q]]", solveNQueens(1));
        int[] known = {1, 0, 0, 2, 10, 4, 40, 92, 352};
        boolean ok = true;
        for (int n = 1; n <= 9; n++) {
            List<List<String>> s = solveNQueens(n);
            ok &= s.size() == known[n - 1] && new HashSet<>(s).size() == s.size();
            for (List<String> b : s) ok &= valid(b);
        }
        check("n = 1..9: known counts, every board valid", true, ok);
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
