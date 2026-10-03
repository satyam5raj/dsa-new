/*
 * ============================================================================
 *  MEDIUM 08 : WORD SEARCH                   LeetCode #79
 *  https://leetcode.com/problems/word-search/
 *  Pattern: GRID BACKTRACKING (mark, recurse 4 ways, unmark) - the base of Word Search II
 * ============================================================================
 *
 *  PROBLEM
 *    Does `word` exist in the board as a path of horizontally/vertically adjacent
 *    cells, using each cell at most ONCE?
 *      A B C E
 *      S F C S      "ABCCED" -> true,  "SEE" -> true,  "ABCB" -> false
 *      A D E E                                          (would reuse the B)
 *
 *  ---------------------------------------------------------------------------
 *  BACKTRACKING from every cell:
 *    dfs(r, c, i):   // can word[i..] be matched starting at (r, c)?
 *      if i == len: true
 *      if out of bounds or board[r][c] != word[i]: false
 *      MARK the cell (e.g. board[r][c] = '#') so this path can't reuse it
 *      found = dfs on the 4 neighbours with i + 1
 *      UNMARK (restore the letter) - other paths may use this cell
 *      return found
 *  Unlike flood fill, we UNMARK: "visited" only applies to the CURRENT path.
 *
 *  PRUNING (big speed-ups in practice)
 *    - if the word has more of some letter than the board -> false immediately
 *    - if the last letter is rarer than the first, search the REVERSED word
 *      (fewer starting points)
 *
 *  Word Search II (H01) searches MANY words: running this once per word is too
 *  slow - a trie lets one DFS find all words at once.
 *
 *  COMPLEXITY   Time O(R * C * 3^L)  (3: we never step straight back)   Space O(L)
 * ============================================================================
 */

import java.util.*;

public class M08_WordSearch {

    static boolean exist(char[][] board, String word) {
        int[] boardCount = new int[128], wordCount = new int[128];
        for (char[] row : board) for (char c : row) boardCount[c]++;
        for (char c : word.toCharArray()) if (++wordCount[c] > boardCount[c]) return false;   // impossible: prune
        if (boardCount[word.charAt(0)] > boardCount[word.charAt(word.length() - 1)]) {
            word = new StringBuilder(word).reverse().toString();                               // start from the rarer end
        }
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board[0].length; c++)
                if (dfs(board, word, r, c, 0)) return true;
        return false;
    }

    static boolean dfs(char[][] b, String w, int r, int c, int i) {
        if (i == w.length()) return true;
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != w.charAt(i)) return false;
        char saved = b[r][c];
        b[r][c] = '#';                                    // mark: used on this path
        boolean found = dfs(b, w, r + 1, c, i + 1) || dfs(b, w, r - 1, c, i + 1)
                     || dfs(b, w, r, c + 1, i + 1) || dfs(b, w, r, c - 1, i + 1);
        b[r][c] = saved;                                  // unmark: other paths may use it
        return found;
    }

    // referee: plain backtracking with a visited array, no pruning
    static boolean plain(char[][] b, String w) {
        boolean[][] used = new boolean[b.length][b[0].length];
        for (int r = 0; r < b.length; r++) for (int c = 0; c < b[0].length; c++) if (go(b, w, r, c, 0, used)) return true;
        return false;
    }

    static boolean go(char[][] b, String w, int r, int c, int i, boolean[][] used) {
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || used[r][c] || b[r][c] != w.charAt(i)) return false;
        if (i == w.length() - 1) return true;
        used[r][c] = true;
        boolean f = go(b, w, r + 1, c, i + 1, used) || go(b, w, r - 1, c, i + 1, used) || go(b, w, r, c + 1, i + 1, used) || go(b, w, r, c - 1, i + 1, used);
        used[r][c] = false;
        return f;
    }

    static char[][] board(String... rows) { return Arrays.stream(rows).map(String::toCharArray).toArray(char[][]::new); }

    public static void main(String[] args) {
        char[][] b = board("ABCE", "SFCS", "ADEE");
        check("ABCCED", true, exist(b, "ABCCED"));
        check("SEE", true, exist(b, "SEE"));
        check("ABCB (reuse)", false, exist(b, "ABCB"));
        check("board restored", "[[A, B, C, E], [S, F, C, S], [A, D, E, E]]", b);
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int R = 1 + rnd.nextInt(4), C = 1 + rnd.nextInt(4);
            char[][] g = new char[R][C];
            for (char[] row : g) for (int c = 0; c < C; c++) row[c] = (char) ('A' + rnd.nextInt(3));
            StringBuilder w = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(6); i++) w.append((char) ('A' + rnd.nextInt(3)));
            ok &= exist(g, w.toString()) == plain(g, w.toString());
        }
        check("1,000 random boards vs plain backtracking", true, ok);
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
