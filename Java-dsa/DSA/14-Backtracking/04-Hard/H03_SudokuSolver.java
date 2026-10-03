/*
 * ============================================================================
 *  HARD 03 : SUDOKU SOLVER                   LeetCode #37
 *  https://leetcode.com/problems/sudoku-solver/
 *  Pattern: CONSTRAINT SATISFACTION - bitmask constraints + "most constrained cell first"
 * ============================================================================
 *
 *  PROBLEM
 *    Fill the empty cells ('.') of a 9x9 board so every row, column and 3x3 box holds
 *    the digits 1-9 exactly once. The puzzle has exactly one solution.
 *
 *  ---------------------------------------------------------------------------
 *  BASIC BACKTRACKING: find an empty cell, try digits 1-9 that don't clash, recurse;
 *  if nothing works, undo and return false (the caller tries its next digit).
 *  "return true as soon as solved" stops the search at the first solution.
 *
 *  O(1) CLASH CHECKS: three arrays of 9-bit masks
 *      rows[r], cols[c], boxes[b]   bit d set = digit d+1 already used there
 *      box index b = (r / 3) * 3 + c / 3
 *      allowed digits for (r, c) = ~(rows[r] | cols[c] | boxes[b]) & 0x1FF
 *
 *  MOST-CONSTRAINED-CELL-FIRST (the "MRV" heuristic): instead of the next empty cell
 *  in reading order, fill the empty cell with the FEWEST allowed digits. A cell with
 *  one option is forced; a cell with zero options means "backtrack NOW". This shrinks
 *  the tree enormously on hard puzzles (measured below).
 *
 *  COMPLEXITY   exponential in the worst case, fast in practice   Space O(81)
 * ============================================================================
 */

import java.util.*;

public class H03_SudokuSolver {

    static int[] rows, cols, boxes;
    static long nodes;

    static void solveSudoku(char[][] board) { solve(board, true); }

    static boolean solve(char[][] board, boolean mrv) {
        rows = new int[9]; cols = new int[9]; boxes = new int[9];
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) {
            if (board[r][c] == '.') { empty.add(new int[]{r, c}); continue; }
            int bit = 1 << (board[r][c] - '1');
            rows[r] |= bit; cols[c] |= bit; boxes[r / 3 * 3 + c / 3] |= bit;
        }
        nodes = 0;
        return bt(board, empty, empty.size(), mrv);
    }

    static boolean bt(char[][] board, List<int[]> empty, int remaining, boolean mrv) {
        nodes++;
        if (remaining == 0) return true;
        // choose the cell to fill: the most constrained (mrv) or simply the next one
        int pick = remaining - 1, best = 10;
        if (mrv) {
            for (int i = 0; i < remaining; i++) {
                int[] cell = empty.get(i);
                int options = Integer.bitCount(allowed(cell[0], cell[1]));
                if (options < best) { best = options; pick = i; if (options <= 1) break; }
            }
        }
        Collections.swap(empty, pick, remaining - 1);              // move the chosen cell to the end
        int[] cell = empty.get(remaining - 1);
        int r = cell[0], c = cell[1], b = r / 3 * 3 + c / 3;
        for (int mask = allowed(r, c); mask != 0; mask &= mask - 1) {
            int bit = mask & -mask;
            int d = Integer.numberOfTrailingZeros(bit);
            board[r][c] = (char) ('1' + d);
            rows[r] |= bit; cols[c] |= bit; boxes[b] |= bit;
            if (bt(board, empty, remaining - 1, mrv)) return true;   // solved: stop everything
            rows[r] ^= bit; cols[c] ^= bit; boxes[b] ^= bit;          // undo
        }
        board[r][c] = '.';
        Collections.swap(empty, pick, remaining - 1);
        return false;
    }

    static int allowed(int r, int c) { return ~(rows[r] | cols[c] | boxes[r / 3 * 3 + c / 3]) & 0x1FF; }

    static boolean validSolution(char[][] puzzle, char[][] solved) {
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) if (puzzle[r][c] != '.' && puzzle[r][c] != solved[r][c]) return false;
        for (int i = 0; i < 9; i++) {
            Set<Character> row = new HashSet<>(), col = new HashSet<>(), box = new HashSet<>();
            for (int j = 0; j < 9; j++) {
                row.add(solved[i][j]); col.add(solved[j][i]);
                box.add(solved[i / 3 * 3 + j / 3][i % 3 * 3 + j % 3]);
            }
            if (row.size() != 9 || col.size() != 9 || box.size() != 9 || row.contains('.')) return false;
        }
        return true;
    }

    static char[][] parse(String s) {
        char[][] b = new char[9][9];
        for (int i = 0; i < 81; i++) b[i / 9][i % 9] = s.charAt(i) == '0' ? '.' : s.charAt(i);
        return b;
    }

    static char[][] copy(char[][] b) { return Arrays.stream(b).map(char[]::clone).toArray(char[][]::new); }

    public static void main(String[] args) {
        char[][] ex = parse("530070000600195000098000060800060003400803001700020006060000280000419005000080079");
        char[][] solved = copy(ex);
        solveSudoku(solved);
        check("LeetCode example solved", true, validSolution(ex, solved));
        check("first row", "[5, 3, 4, 6, 7, 8, 9, 1, 2]", solved[0]);

        // a famously hard puzzle ("Arto Inkala")
        char[][] hard = parse("800000000003600000070090200050007000000045700000100030001000068008500010090000400");
        char[][] h1 = copy(hard), h2 = copy(hard);
        solve(h1, false);
        long plain = nodes;
        solve(h2, true);
        long mrv = nodes;
        System.out.printf("   hardest puzzle: next-empty-cell %,d nodes, most-constrained-cell %,d nodes%n", plain, mrv);
        check("both solve it identically", true, validSolution(hard, h2) && Arrays.deepEquals(h1, h2));
        check("MRV explores fewer nodes", true, mrv < plain);

        // random puzzles: blank cells of a known solution, solve, verify
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 50; t++) {
            char[][] p = copy(solved);
            for (int k = 0; k < 45 + rnd.nextInt(10); k++) p[rnd.nextInt(9)][rnd.nextInt(9)] = '.';
            char[][] s = copy(p);
            solveSudoku(s);
            ok &= validSolution(p, s);
        }
        check("50 random puzzles solved validly", true, ok);
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
