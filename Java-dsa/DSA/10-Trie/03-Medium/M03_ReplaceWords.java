/*
 * ============================================================================
 *  MEDIUM 03 : REPLACE WORDS  ⭐ bonus        LeetCode #648
 *  https://leetcode.com/problems/replace-words/
 *  Pattern: walk each word through the trie, STOP at the first word end (shortest root)
 * ============================================================================
 *
 *  PROBLEM
 *    dictionary of ROOTS. In the sentence, replace every word that starts with a
 *    root by the SHORTEST such root.
 *      ["cat","bat","rat"], "the cattle was rattled by the battery"
 *      -> "the cat was rat by the bat"
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each word, try every root with startsWith -> O(words * roots * L).
 *
 *  HASHSET OF ROOTS: for each word, check prefixes of length 1, 2, 3... in the
 *  set; the first hit is the shortest root. O(L^2) per word (substring + hashing).
 *
 *  TRIE OF ROOTS: walk the word's characters down the trie:
 *     - hit a node with isEnd -> the prefix so far is the SHORTEST root: replace
 *     - fall off the trie      -> no root is a prefix: keep the word
 *  O(L) per word, and we stop early at the shortest root.
 *
 *  DRY RUN "cattle": c -> a -> t (isEnd: "cat") -> replace with "cat"
 *          "the": t has no child in the trie... (t isn't a root's start) -> keep
 *
 *  COMPLEXITY   Time O(total chars of roots + sentence)   Space O(roots' chars)
 * ============================================================================
 */

import java.util.*;

public class M03_ReplaceWords {

    static class Node { Node[] next = new Node[26]; boolean end; }

    static String replaceWords(List<String> dictionary, String sentence) {
        Node root = new Node();
        for (String r : dictionary) {
            Node n = root;
            for (char c : r.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
            }
            n.end = true;
        }
        StringBuilder out = new StringBuilder();
        for (String word : sentence.split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(shortestRoot(root, word));
        }
        return out.toString();
    }

    static String shortestRoot(Node root, String word) {
        Node n = root;
        for (int i = 0; i < word.length(); i++) {
            n = n.next[word.charAt(i) - 'a'];
            if (n == null) return word;                    // no root is a prefix of this word
            if (n.end) return word.substring(0, i + 1);   // the first (shortest) root
        }
        return word;                                       // the word is a prefix of a longer root only
    }

    static String brute(List<String> dict, String sentence) {
        StringBuilder out = new StringBuilder();
        for (String w : sentence.split(" ")) {
            String best = w;
            for (String r : dict) if (w.startsWith(r) && r.length() < best.length()) best = r;
            if (out.length() > 0) out.append(' ');
            out.append(best);
        }
        return out.toString();
    }

    public static void main(String[] args) {
        check("example 1", "the cat was rat by the bat", replaceWords(List.of("cat", "bat", "rat"), "the cattle was rattled by the battery"));
        check("example 2", "a a b c", replaceWords(List.of("a", "b", "c"), "aadsfasf absbs bbab cadsfafs"));
        check("shortest root wins", "a", replaceWords(List.of("abc", "a", "ab"), "abcd"));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            List<String> dict = new ArrayList<>();
            for (int i = 0; i < 1 + rnd.nextInt(5); i++) dict.add(word(rnd, 3));
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(6); i++) s.append(i > 0 ? " " : "").append(word(rnd, 5));
            ok &= replaceWords(dict, s.toString()).equals(brute(dict, s.toString()));
        }
        check("500 random cases vs brute force", true, ok);
        summary();
    }

    static String word(Random rnd, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1 + rnd.nextInt(max); i++) sb.append((char) ('a' + rnd.nextInt(3)));
        return sb.toString();
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
