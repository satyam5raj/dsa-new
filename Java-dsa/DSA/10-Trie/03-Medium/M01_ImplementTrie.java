/*
 * ============================================================================
 *  MEDIUM 01 : IMPLEMENT TRIE (PREFIX TREE)   LeetCode #208
 *  https://leetcode.com/problems/implement-trie-prefix-tree/
 *  Pattern: the trie itself - children[26] + an isEnd flag
 * ============================================================================
 *
 *  PROBLEM
 *    Implement insert(word), search(word), startsWith(prefix). Lowercase letters.
 *      insert("apple"); search("apple") -> true; search("app") -> false;
 *      startsWith("app") -> true; insert("app"); search("app") -> true
 *
 *  ---------------------------------------------------------------------------
 *  (01-Basics/TrieBasics explains the structure in detail; this is the
 *   interview-sized version you should be able to write in 5 minutes.)
 *
 *    Node: Node[26] children, boolean isEnd
 *    insert    : walk, creating missing nodes; set isEnd at the last node
 *    search    : walk; false if a node is missing; at the end return isEnd
 *    startsWith: walk; false if a node is missing; at the end return true
 *  search and startsWith share the walk -> one helper `find(s)` returning the
 *  last node or null. The ONLY difference between them is the isEnd check.
 *
 *  COMMON BUGS
 *    - forgetting isEnd: search("app") returns true after only inserting "apple"
 *    - using `ch - 'A'` / not handling the index offset
 *
 *  COMPLEXITY   each operation O(L);  Space O(total chars * 26) worst case
 * ============================================================================
 */

import java.util.*;

public class M01_ImplementTrie {

    static class Trie {
        private static class Node {
            Node[] children = new Node[26];
            boolean isEnd;
        }

        private final Node root = new Node();

        public void insert(String word) {
            Node n = root;
            for (char c : word.toCharArray()) {
                int i = c - 'a';
                if (n.children[i] == null) n.children[i] = new Node();
                n = n.children[i];
            }
            n.isEnd = true;
        }

        public boolean search(String word) {
            Node n = find(word);
            return n != null && n.isEnd;
        }

        public boolean startsWith(String prefix) {
            return find(prefix) != null;
        }

        private Node find(String s) {
            Node n = root;
            for (char c : s.toCharArray()) {
                n = n.children[c - 'a'];
                if (n == null) return null;
            }
            return n;
        }
    }

    public static void main(String[] args) {
        Trie t = new Trie();
        t.insert("apple");
        check("search apple", true, t.search("apple"));
        check("search app", false, t.search("app"));
        check("startsWith app", true, t.startsWith("app"));
        t.insert("app");
        check("search app after insert", true, t.search("app"));
        check("startsWith '' (empty prefix)", true, t.startsWith(""));
        Random rnd = new Random(1);
        Trie mine = new Trie();
        Set<String> words = new HashSet<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 1 + rnd.nextInt(5); j++) sb.append((char) ('a' + rnd.nextInt(3)));
            String w = sb.toString();
            switch (rnd.nextInt(3)) {
                case 0 -> { mine.insert(w); words.add(w); }
                case 1 -> ok &= mine.search(w) == words.contains(w);
                default -> ok &= mine.startsWith(w) == words.stream().anyMatch(x -> x.startsWith(w));
            }
        }
        check("20,000 random ops vs HashSet scan", true, ok);
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
