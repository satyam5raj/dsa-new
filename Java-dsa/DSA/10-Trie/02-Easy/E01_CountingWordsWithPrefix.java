/*
 * ============================================================================
 *  EASY 01 : COUNTING WORDS WITH A GIVEN PREFIX  ⭐ bonus   LeetCode #2185
 *  https://leetcode.com/problems/counting-words-with-a-given-prefix/
 *  Pattern: PREFIX COUNTS stored in trie nodes (pays off with MANY queries)
 * ============================================================================
 *
 *  PROBLEM
 *    How many words in `words` start with `pref`?
 *      ["pay","attention","practice","attend"], "at" -> 2
 *
 *  ---------------------------------------------------------------------------
 *  ONE QUERY: just check each word with startsWith -> O(N * |pref|). That's the
 *  right answer for this exact problem; building a trie for one query is overkill.
 *
 *  MANY QUERIES (the real-world version: an autocomplete box asks this on every
 *  keystroke): build a trie ONCE, where every node stores passCount = number of
 *  words going through it. Then each query is O(|pref|), independent of N.
 *     build: O(total characters);  query: walk to the prefix node, return its count
 *
 *  DRY RUN prefix "at": a (count 2: attention, attend) -> t (count 2) -> 2
 *
 *  COMPLEXITY   scan: O(N * P) per query;  trie: O(P) per query after O(total) build
 * ============================================================================
 */

import java.util.*;

public class E01_CountingWordsWithPrefix {

    static int prefixCount(String[] words, String pref) {          // single query: just scan
        int count = 0;
        for (String w : words) if (w.startsWith(pref)) count++;
        return count;
    }

    static class PrefixCounter {                                   // many queries: trie with counts
        static class Node { Node[] next = new Node[26]; int pass; }
        final Node root = new Node();

        PrefixCounter(String[] words) {
            for (String w : words) {
                Node n = root;
                for (char c : w.toCharArray()) {
                    if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                    n = n.next[c - 'a'];
                    n.pass++;                                       // one more word passes through
                }
            }
        }

        int count(String pref) {
            Node n = root;
            for (char c : pref.toCharArray()) {
                n = n.next[c - 'a'];
                if (n == null) return 0;
            }
            return n.pass;
        }
    }

    public static void main(String[] args) {
        String[] words = {"pay", "attention", "practice", "attend"};
        check("'at'", 2, prefixCount(words, "at"));
        check("'code' none", 0, prefixCount(new String[]{"leetcode", "win", "loops", "success"}, "code"));
        check("trie 'at'", 2, new PrefixCounter(words).count("at"));
        check("trie 'p'", 2, new PrefixCounter(words).count("p"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            String[] ws = new String[1 + rnd.nextInt(30)];
            for (int i = 0; i < ws.length; i++) ws[i] = randomWord(rnd);
            PrefixCounter pc = new PrefixCounter(ws);
            for (int q = 0; q < 20; q++) { String p = randomWord(rnd); ok &= pc.count(p) == prefixCount(ws, p); }
        }
        check("6,000 random queries: trie == scan", true, ok);
        summary();
    }

    static String randomWord(Random rnd) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1 + rnd.nextInt(4); i++) sb.append((char) ('a' + rnd.nextInt(3)));
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
