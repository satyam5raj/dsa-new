/*
 * ============================================================================
 *  EASY 02 : CHECK IF A WORD OCCURS AS A PREFIX OF ANY WORD IN A SENTENCE  ⭐ bonus
 *  LeetCode #1455
 *  https://leetcode.com/problems/check-if-a-word-occurs-as-a-prefix-of-any-word-in-a-sentence/
 *  Pattern: prefix test per word (and when a trie would be the better tool)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the 1-based index of the FIRST word in `sentence` that starts with
 *    searchWord, or -1.
 *      "i love eating burger", "burg" -> 4
 *      "this problem is an easy problem", "pro" -> 2  (first match wins)
 *
 *  ---------------------------------------------------------------------------
 *  ONE searchWord: split and test startsWith - O(length of sentence). Done.
 *
 *  WHEN A TRIE HELPS: if the sentence is fixed and MANY searchWords arrive,
 *  build a trie of the sentence's words where each node remembers the SMALLEST
 *  word index passing through it. Each query is then O(|searchWord|).
 *  (Implemented below; this "store the answer in the node" idea is used again in
 *   Search Suggestions System, M04.)
 *
 *  COMPLEXITY   scan O(n); trie O(|query|) per query after O(n) build
 * ============================================================================
 */

import java.util.*;

public class E02_IsPrefixOfWordInSentence {

    static int isPrefixOfWord(String sentence, String searchWord) {
        String[] words = sentence.split(" ");
        for (int i = 0; i < words.length; i++) if (words[i].startsWith(searchWord)) return i + 1;
        return -1;
    }

    static class SentenceIndex {
        static class Node { Node[] next = new Node[26]; int firstIndex = Integer.MAX_VALUE; }
        final Node root = new Node();

        SentenceIndex(String sentence) {
            String[] words = sentence.split(" ");
            for (int i = 0; i < words.length; i++) {
                Node n = root;
                for (char c : words[i].toCharArray()) {
                    if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                    n = n.next[c - 'a'];
                    n.firstIndex = Math.min(n.firstIndex, i + 1);   // the answer, stored in the node
                }
            }
        }

        int query(String w) {
            Node n = root;
            for (char c : w.toCharArray()) {
                n = n.next[c - 'a'];
                if (n == null) return -1;
            }
            return n.firstIndex;
        }
    }

    public static void main(String[] args) {
        check("burg", 4, isPrefixOfWord("i love eating burger", "burg"));
        check("pro (first match)", 2, isPrefixOfWord("this problem is an easy problem", "pro"));
        check("you (none)", -1, isPrefixOfWord("i am tired", "you"));
        SentenceIndex idx = new SentenceIndex("this problem is an easy problem");
        check("trie: pro", 2, idx.query("pro"));
        check("trie: e", 5, idx.query("e"));
        check("trie: x", -1, idx.query("x"));
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
