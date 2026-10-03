/*
 * ============================================================================
 *  HARD 02 : STREAM OF CHARACTERS  ⭐ bonus   LeetCode #1032
 *  https://leetcode.com/problems/stream-of-characters/
 *  Pattern: REVERSED TRIE - match SUFFIXES by walking the stream backwards
 * ============================================================================
 *
 *  PROBLEM
 *    StreamChecker(words); query(letter) returns true if some word is a SUFFIX of
 *    all letters queried so far.
 *      words [cd, f, kl]: query a,b,c -> false;  d -> true ("cd");  ... f -> true
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: after each letter, check every word with endsWith -> O(W * L) per query.
 *
 *  A FORWARD trie answers "which words START here?". We need "which words END at
 *  the newest character?" Flip everything:
 *    - insert every word REVERSED into the trie ("cd" -> d, c)
 *    - keep the stream in a StringBuilder; on each query, walk the trie starting
 *      from the NEWEST character going backwards
 *    - hit an end-of-word node -> true; fall off the trie -> false
 *  Only the last (max word length) characters can ever matter, so the walk is
 *  at most maxLen steps, and the buffer can be trimmed to that length.
 *
 *  DRY RUN words [cd, f, kl] -> reversed trie: d-c*, f*, l-k*
 *    stream "abcd": walk d -> c (end) -> true
 *
 *  ALTERNATIVE: Aho-Corasick (a forward trie with failure links) processes each
 *  character in amortized O(1). More complex; the reversed trie is interview-sized.
 *
 *  COMPLEXITY   query O(maxLen)   Space O(total chars)
 * ============================================================================
 */

import java.util.*;

public class H02_StreamOfCharacters {

    static class StreamChecker {
        private static class Node { Node[] next = new Node[26]; boolean end; }
        private final Node root = new Node();
        private final StringBuilder stream = new StringBuilder();
        private final int maxLen;

        StreamChecker(String[] words) {
            int m = 0;
            for (String w : words) {
                m = Math.max(m, w.length());
                Node n = root;
                for (int i = w.length() - 1; i >= 0; i--) {          // insert REVERSED
                    int c = w.charAt(i) - 'a';
                    if (n.next[c] == null) n.next[c] = new Node();
                    n = n.next[c];
                }
                n.end = true;
            }
            maxLen = m;
        }

        public boolean query(char letter) {
            stream.append(letter);
            if (stream.length() > 2 * maxLen) stream.delete(0, stream.length() - maxLen);   // only the tail matters
            Node n = root;
            for (int i = stream.length() - 1, steps = 0; i >= 0 && steps < maxLen; i--, steps++) {   // newest first
                n = n.next[stream.charAt(i) - 'a'];
                if (n == null) return false;
                if (n.end) return true;                              // a word ends exactly at the newest char
            }
            return false;
        }
    }

    public static void main(String[] args) {
        StreamChecker sc = new StreamChecker(new String[]{"cd", "f", "kl"});
        StringBuilder out = new StringBuilder();
        for (char c : "abcdefghijkl".toCharArray()) out.append(sc.query(c) ? 'T' : '.');
        check("abcdefghijkl", "...T.T.....T", out.toString());
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            String[] ws = new String[1 + rnd.nextInt(5)];
            for (int i = 0; i < ws.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(4); j++) sb.append((char) ('a' + rnd.nextInt(3)));
                ws[i] = sb.toString();
            }
            StreamChecker mine = new StreamChecker(ws);
            StringBuilder s = new StringBuilder();
            for (int q = 0; q < 60; q++) {
                char c = (char) ('a' + rnd.nextInt(3));
                s.append(c);
                boolean exp = false;
                for (String w : ws) exp |= s.toString().endsWith(w);
                ok &= mine.query(c) == exp;
            }
        }
        check("18,000 random queries vs endsWith", true, ok);
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
