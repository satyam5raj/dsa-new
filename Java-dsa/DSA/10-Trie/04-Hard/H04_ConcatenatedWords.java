/*
 * ============================================================================
 *  HARD 04 : CONCATENATED WORDS  ⭐ bonus     LeetCode #472
 *  https://leetcode.com/problems/concatenated-words/
 *  Pattern: TRIE + DP / memoized DFS over split points ("word break" for every word)
 * ============================================================================
 *
 *  PROBLEM
 *    Return every word that is made of AT LEAST TWO shorter words from the list.
 *      ["cat","cats","catsdogcats","dog","dogcatsdog","hippopotamuses","rat","ratcatdogcat"]
 *      -> ["catsdogcats","dogcatsdog","ratcatdogcat"]
 *
 *  ---------------------------------------------------------------------------
 *  FOR ONE WORD this is WORD BREAK (a DP problem): can w be cut into dictionary words?
 *     ok[i] = "w[0..i) can be built from words";  ok[0] = true
 *     ok[j] = true if ok[i] and w[i..j) is a word, for some i < j
 *
 *  THE TRIE MAKES THE INNER LOOP CHEAP: from position i, walk the trie along
 *  w[i], w[i+1], ...; every word end you pass is a valid j. No substring/hashing,
 *  and you stop as soon as you fall off the trie.
 *
 *  "AT LEAST TWO words": process words SHORTEST FIRST and check each word against
 *  the trie BEFORE inserting it - then the word can't use itself, so any successful
 *  split uses >= 2 smaller words. Then insert it (it may help longer words).
 *  Empty strings are skipped (they'd make everything "concatenated").
 *
 *  DRY RUN "catsdogcats" (trie has cat, cats, dog):
 *    from 0: "cat" (end at 3), "cats" (end at 4) -> ok[3], ok[4]
 *    from 4: "dog" -> ok[7];  from 7: "cat" -> ok[10], "cats" -> ok[11] = the end -> yes
 *
 *  COMPLEXITY   Time O(sum of L^2)   Space O(total chars)
 * ============================================================================
 */

import java.util.*;

public class H04_ConcatenatedWords {

    static class Node { Node[] next = new Node[26]; boolean end; }

    static List<String> findAllConcatenatedWordsInADict(String[] words) {
        String[] sorted = words.clone();
        Arrays.sort(sorted, Comparator.comparingInt(String::length));      // shortest first
        Node root = new Node();
        List<String> result = new ArrayList<>();
        for (String w : sorted) {
            if (w.isEmpty()) continue;
            if (canBuild(root, w)) result.add(w);                          // check BEFORE inserting itself
            insert(root, w);
        }
        return result;
    }

    static boolean canBuild(Node root, String w) {
        int n = w.length();
        boolean[] ok = new boolean[n + 1];
        ok[0] = true;
        for (int i = 0; i < n; i++) {
            if (!ok[i]) continue;                                          // can't even reach position i
            Node node = root;
            for (int j = i; j < n; j++) {
                node = node.next[w.charAt(j) - 'a'];
                if (node == null) break;                                   // no word continues: stop early
                if (node.end) ok[j + 1] = true;                            // w[i..j] is a word
            }
            if (ok[n]) return true;
        }
        return ok[n];
    }

    static void insert(Node root, String w) {
        Node n = root;
        for (char c : w.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.end = true;
    }

    // referee: word break with a HashSet, excluding the word itself, counting pieces >= 2
    static Set<String> brute(String[] words) {
        Set<String> dict = new HashSet<>(Arrays.asList(words));
        dict.remove("");
        Set<String> out = new HashSet<>();
        for (String w : dict) if (pieces(w, 0, dict, w) >= 2) out.add(w);
        return out;
    }

    // max number of pieces w[i..] can be split into using words other than `self` as a whole; -1 if impossible
    static int pieces(String w, int i, Set<String> dict, String self) {
        if (i == w.length()) return 0;
        int best = -1;
        for (int j = i + 1; j <= w.length(); j++) {
            String part = w.substring(i, j);
            if (!dict.contains(part) || (i == 0 && j == w.length())) continue;   // not the whole word itself
            int rest = pieces(w, j, dict, self);
            if (rest >= 0) best = Math.max(best, rest + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        String[] ex = {"cat", "cats", "catsdogcats", "dog", "dogcatsdog", "hippopotamuses", "rat", "ratcatdogcat"};
        List<String> r = findAllConcatenatedWordsInADict(ex);
        Collections.sort(r);
        check("example", "[catsdogcats, dogcatsdog, ratcatdogcat]", r);
        check("[cat,dog,catdog]", "[catdog]", findAllConcatenatedWordsInADict(new String[]{"cat", "dog", "catdog"}));
        check("a word alone doesn't count", "[]", findAllConcatenatedWordsInADict(new String[]{"a"}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            Set<String> ws = new LinkedHashSet<>();
            for (int i = 0; i < 1 + rnd.nextInt(10); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(5); j++) sb.append((char) ('a' + rnd.nextInt(2)));
                ws.add(sb.toString());
            }
            String[] arr = ws.toArray(new String[0]);
            ok &= new HashSet<>(findAllConcatenatedWordsInADict(arr)).equals(brute(arr));
        }
        check("500 random dictionaries vs word break", true, ok);
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
