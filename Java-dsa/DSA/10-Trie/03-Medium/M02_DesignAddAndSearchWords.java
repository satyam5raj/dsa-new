/*
 * ============================================================================
 *  MEDIUM 02 : DESIGN ADD AND SEARCH WORDS DATA STRUCTURE  ⭐ bonus   LeetCode #211
 *  https://leetcode.com/problems/design-add-and-search-words-data-structure/
 *  Pattern: trie + WILDCARD DFS ('.' = try every child)
 * ============================================================================
 *
 *  PROBLEM
 *    addWord(word); search(pattern) where '.' matches any ONE letter.
 *      add bad, dad, mad
 *      search("pad") -> false, ("bad") -> true, (".ad") -> true, ("b..") -> true
 *
 *  ---------------------------------------------------------------------------
 *  HASHSET + brute force: for every stored word of the same length, compare
 *  character by character -> O(N * L) per search.
 *
 *  TRIE: a normal letter follows exactly one child. A '.' must try ALL existing
 *  children - recursion with branching:
 *     dfs(node, i):
 *       if i == len: return node.isEnd
 *       c = pattern[i]
 *       if c == '.': return any child for which dfs(child, i + 1) is true
 *       else:        return child[c] exists && dfs(child[c], i + 1)
 *  Without dots it's O(L). With dots it can branch up to 26 ways per dot, but the
 *  trie only has children that actually exist, so in practice it's fast.
 *
 *  OPTIMIZATION: group words by LENGTH (a separate trie per length, or check the
 *  length first) - a pattern of length 3 never needs to look at longer words.
 *
 *  COMPLEXITY   addWord O(L);  search O(L) without dots, up to O(26^dots * L)
 * ============================================================================
 */

import java.util.*;

public class M02_DesignAddAndSearchWords {

    static class WordDictionary {
        private static class Node {
            Node[] next = new Node[26];
            boolean end;
        }

        private final Node root = new Node();

        public void addWord(String word) {
            Node n = root;
            for (char c : word.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
            }
            n.end = true;
        }

        public boolean search(String word) { return dfs(root, word, 0); }

        private boolean dfs(Node n, String w, int i) {
            if (i == w.length()) return n.end;
            char c = w.charAt(i);
            if (c == '.') {
                for (Node child : n.next) if (child != null && dfs(child, w, i + 1)) return true;   // try every branch
                return false;
            }
            Node child = n.next[c - 'a'];
            return child != null && dfs(child, w, i + 1);
        }
    }

    static boolean bruteMatch(Set<String> words, String p) {
        outer:
        for (String w : words) {
            if (w.length() != p.length()) continue;
            for (int i = 0; i < w.length(); i++) if (p.charAt(i) != '.' && p.charAt(i) != w.charAt(i)) continue outer;
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        WordDictionary d = new WordDictionary();
        d.addWord("bad");
        d.addWord("dad");
        d.addWord("mad");
        check("pad", false, d.search("pad"));
        check("bad", true, d.search("bad"));
        check(".ad", true, d.search(".ad"));
        check("b..", true, d.search("b.."));
        check("... ", true, d.search("..."));
        check(".... (too long)", false, d.search("...."));
        Random rnd = new Random(2);
        WordDictionary mine = new WordDictionary();
        Set<String> ref = new HashSet<>();
        boolean ok = true;
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder();
            int len = 1 + rnd.nextInt(4);
            if (rnd.nextBoolean()) {
                for (int j = 0; j < len; j++) sb.append((char) ('a' + rnd.nextInt(3)));
                mine.addWord(sb.toString());
                ref.add(sb.toString());
            } else {
                for (int j = 0; j < len; j++) sb.append(rnd.nextInt(3) == 0 ? '.' : (char) ('a' + rnd.nextInt(3)));
                ok &= mine.search(sb.toString()) == bruteMatch(ref, sb.toString());
            }
        }
        check("5,000 random ops vs brute matching", true, ok);
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
