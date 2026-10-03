/*
 * ============================================================================
 *  HARD 01 : WORD SEARCH II                  LeetCode #212
 *  https://leetcode.com/problems/word-search-ii/
 *  Pattern: TRIE + GRID BACKTRACKING - search ALL words in ONE pass, prune by prefix
 * ============================================================================
 *
 *  PROBLEM
 *    Board of letters + a list of words. Return every word that can be traced in
 *    the board (adjacent cells, each cell used once per word).
 *      board  o a a n        words [oath, pea, eat, rain] -> [eat, oath]
 *             e t a e
 *             i h k r
 *             i f l v
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: run Word Search (M08) once per word -> O(W * R*C * 3^L). With 3 * 10^4
 *  words that's hopeless: the same board paths are explored again and again.
 *
 *  TRIE OF ALL WORDS: DFS the board ONCE from every cell, walking the trie in
 *  parallel. The trie node tells you which words are still possible:
 *    - the next letter isn't a child of the current node -> NO word continues this
 *      way -> stop immediately (this pruning is the whole speed-up)
 *    - a node that ends a word -> record it
 *
 *  OPTIMIZATIONS (these turn TLE into AC)
 *    1. store the full word in the end node (no StringBuilder needed)
 *    2. after finding a word, set node.word = null - each word reported once
 *    3. PRUNE the trie: after exploring a child, if it has no children and no
 *       word left, remove it - later searches don't walk dead branches
 *    4. mark visited cells in place ('#') and restore after
 *
 *  COMPLEXITY   Time O(R*C * 4 * 3^(L-1)) worst case, far less with pruning
 *               Space O(total chars of words)
 * ============================================================================
 */

import java.util.*;

public class H01_WordSearchII {

    static class Node {
        Node[] next = new Node[26];
        String word;                                         // non-null: a word ends here
    }

    static List<String> findWords(char[][] board, String[] words) {
        Node root = new Node();
        for (String w : words) {
            Node n = root;
            for (char c : w.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
            }
            n.word = w;
        }
        List<String> found = new ArrayList<>();
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board[0].length; c++)
                dfs(board, r, c, root, found);
        return found;
    }

    static void dfs(char[][] b, int r, int c, Node parent, List<String> found) {
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length) return;
        char ch = b[r][c];
        if (ch == '#') return;                               // already on the current path
        Node n = parent.next[ch - 'a'];
        if (n == null) return;                               // no word continues this way: prune
        if (n.word != null) {
            found.add(n.word);
            n.word = null;                                   // report each word once
        }
        b[r][c] = '#';
        dfs(b, r + 1, c, n, found);
        dfs(b, r - 1, c, n, found);
        dfs(b, r, c + 1, n, found);
        dfs(b, r, c - 1, n, found);
        b[r][c] = ch;
        if (n.word == null && isLeaf(n)) parent.next[ch - 'a'] = null;   // prune exhausted branches
    }

    static boolean isLeaf(Node n) {
        for (Node x : n.next) if (x != null) return false;
        return true;
    }

    // referee: M08's single-word search, once per word
    static List<String> perWord(char[][] b, String[] words) {
        List<String> out = new ArrayList<>();
        for (String w : new LinkedHashSet<>(Arrays.asList(words))) {
            boolean f = false;
            for (int r = 0; r < b.length && !f; r++) for (int c = 0; c < b[0].length && !f; c++) f = one(b, w, r, c, 0);
            if (f) out.add(w);
        }
        return out;
    }

    static boolean one(char[][] b, String w, int r, int c, int i) {
        if (i == w.length()) return true;
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != w.charAt(i)) return false;
        char s = b[r][c];
        b[r][c] = '#';
        boolean f = one(b, w, r + 1, c, i + 1) || one(b, w, r - 1, c, i + 1) || one(b, w, r, c + 1, i + 1) || one(b, w, r, c - 1, i + 1);
        b[r][c] = s;
        return f;
    }

    static char[][] board(String... rows) { return Arrays.stream(rows).map(String::toCharArray).toArray(char[][]::new); }

    static List<String> sorted(List<String> l) { List<String> c = new ArrayList<>(l); Collections.sort(c); return c; }

    public static void main(String[] args) {
        char[][] b = board("oaan", "etae", "ihkr", "iflv");
        check("example", "[eat, oath]", sorted(findWords(b, new String[]{"oath", "pea", "eat", "rain"})));
        check("[[a,b],[c,d]] abcb", "[]", findWords(board("ab", "cd"), new String[]{"abcb"}));
        check("duplicate word reported once", "[a]", findWords(board("aa"), new String[]{"a", "a"}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int R = 1 + rnd.nextInt(4), C = 1 + rnd.nextInt(4);
            char[][] g = new char[R][C];
            for (char[] row : g) for (int c = 0; c < C; c++) row[c] = (char) ('a' + rnd.nextInt(3));
            String[] ws = new String[1 + rnd.nextInt(10)];
            for (int i = 0; i < ws.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(5); j++) sb.append((char) ('a' + rnd.nextInt(3)));
                ws[i] = sb.toString();
            }
            ok &= sorted(findWords(g, ws)).equals(sorted(perWord(g, ws)));
        }
        check("500 random boards vs one search per word", true, ok);
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
