/*
 * ============================================================================
 *  MEDIUM 05 : LONGEST WORD IN DICTIONARY  ⭐ bonus   LeetCode #720
 *  https://leetcode.com/problems/longest-word-in-dictionary/
 *  Pattern: DFS the trie, only stepping into nodes that END a word
 * ============================================================================
 *
 *  PROBLEM
 *    Find the longest word that can be built ONE CHARACTER AT A TIME by other
 *    words in the list (every prefix must itself be a word). Ties -> the
 *    lexicographically smallest.
 *      ["w","wo","wor","worl","world"]                       -> "world"
 *      ["a","banana","app","appl","ap","apply","apple"]      -> "apple"
 *        (apply and apple both work; apple < apply)
 *
 *  ---------------------------------------------------------------------------
 *  TRIE INSIGHT: "every prefix is a word" = every node on the path has isEnd.
 *  Insert all words, then DFS from the root, but ONLY descend into children whose
 *  isEnd is true. The deepest node reached is the answer. Visiting children in
 *  a..z order and replacing the best only on STRICTLY longer words gives the
 *  smallest word among ties automatically.
 *
 *  ALTERNATIVE: sort the words (by length, then alphabetically); keep a set of
 *  "buildable" words; a word is buildable if its prefix minus the last char is
 *  in the set (or its length is 1). O(n log n + total chars).
 *
 *  COMPLEXITY   Time O(total chars)   Space O(total chars)
 * ============================================================================
 */

import java.util.*;

public class M05_LongestWordInDictionary {

    static class Node { Node[] next = new Node[26]; String word; }   // word != null means "ends here"

    static String longestWord(String[] words) {
        Node root = new Node();
        for (String w : words) {
            Node n = root;
            for (char c : w.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
            }
            n.word = w;
        }
        String[] best = {""};
        dfs(root, best);
        return best[0];
    }

    static void dfs(Node n, String[] best) {
        for (Node child : n.next) {                                       // a..z order
            if (child == null || child.word == null) continue;            // only through complete words
            if (child.word.length() > best[0].length()) best[0] = child.word;   // strictly longer only
            dfs(child, best);
        }
    }

    static String sortVersion(String[] words) {
        String[] s = words.clone();
        Arrays.sort(s);                                                   // alphabetical: prefixes come first
        Set<String> built = new HashSet<>();
        String best = "";
        for (String w : s) {
            if (w.length() == 1 || built.contains(w.substring(0, w.length() - 1))) {
                built.add(w);
                if (w.length() > best.length()) best = w;                 // alphabetical order -> smallest among ties
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("world", "world", longestWord(new String[]{"w", "wo", "wor", "worl", "world"}));
        check("apple vs apply", "apple", longestWord(new String[]{"a", "banana", "app", "appl", "ap", "apply", "apple"}));
        check("nothing buildable", "", longestWord(new String[]{"ab", "bc"}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            Set<String> ws = new HashSet<>();
            for (int i = 0; i < 1 + rnd.nextInt(15); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(4); j++) sb.append((char) ('a' + rnd.nextInt(3)));
                ws.add(sb.toString());
            }
            String[] arr = ws.toArray(new String[0]);
            ok &= longestWord(arr).equals(sortVersion(arr));
        }
        check("1,000 random lists: trie == sort", true, ok);
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
