/*
 * ============================================================================
 *  TRIE (PREFIX TREE) FROM SCRATCH
 * ============================================================================
 *
 *  A TRIE stores strings character by character along paths from the root.
 *  Words sharing a prefix SHARE the nodes of that prefix.
 *
 *    words: car, cart, care, cat, dog
 *
 *            (root)
 *            /    \
 *           c      d
 *           |      |
 *           a      o
 *          / \     |
 *         r*  t*   g*        * = "a word ends here" (isEnd flag)
 *        / \
 *       t*  e*
 *
 *  Note "car" ends at an INTERNAL node - that's why we need the isEnd flag:
 *  reaching a node only proves the PREFIX exists.
 *
 *  OPERATIONS (L = length of the word / prefix)
 *    insert(word)        O(L)   walk down, creating missing children; mark the end
 *    search(word)        O(L)   walk down; true if every char exists AND isEnd
 *    startsWith(prefix)  O(L)   walk down; true if every char exists
 *    countPrefix(p)      O(L)   store a counter in each node (words passing through)
 *    delete(word)        O(L)   unmark the end, decrement counters, prune empty nodes
 *  None of these depend on HOW MANY words are stored!
 *
 *  TRIE vs HashSet<String>
 *    exact lookup  : both ~O(L) (a hash must read the whole string too)
 *    prefix query  : HashSet must scan ALL words O(N * L); trie O(L) + output
 *    sorted output : a trie gives words in alphabetical order by DFS (children a..z)
 *    memory        : a trie with 26-slot arrays can be HEAVIER for few/unrelated words;
 *                    it wins when many words share prefixes
 *
 *  CHILDREN STORAGE
 *    TrieNode[26]       fast, simple, fixed alphabet (lowercase letters) - interview default
 *    HashMap<Character> any alphabet (Unicode, digits), less memory when sparse
 *
 *  USED FOR: autocomplete, spell checkers, IP routing (longest prefix match),
 *  word games (Boggle / Word Search II), T9 keyboards, max-XOR (binary tries).
 *
 *  Run:  java TrieBasics.java
 * ============================================================================
 */

import java.util.*;

public class TrieBasics {

    // ---------------- 1. Array-based trie with prefix counts ----------------
    static class Trie {
        static class Node {
            Node[] children = new Node[26];
            boolean isEnd;
            int passCount;              // how many words pass THROUGH this node (prefix count)
        }

        final Node root = new Node();
        int size;

        void insert(String word) {
            if (search(word)) return;                      // keep counts correct for duplicates
            Node n = root;
            for (char ch : word.toCharArray()) {
                int i = ch - 'a';
                if (n.children[i] == null) n.children[i] = new Node();   // create the missing edge
                n = n.children[i];
                n.passCount++;
            }
            n.isEnd = true;
            size++;
        }

        // walk down the path of s; null if it falls off the trie
        private Node walk(String s) {
            Node n = root;
            for (char ch : s.toCharArray()) {
                n = n.children[ch - 'a'];
                if (n == null) return null;
            }
            return n;
        }

        boolean search(String word) {
            Node n = walk(word);
            return n != null && n.isEnd;                   // the path exists AND a word ends here
        }

        boolean startsWith(String prefix) { return walk(prefix) != null; }

        int countPrefix(String prefix) {
            if (prefix.isEmpty()) return size;
            Node n = walk(prefix);
            return n == null ? 0 : n.passCount;
        }

        boolean delete(String word) {
            if (!search(word)) return false;
            Node n = root;
            for (char ch : word.toCharArray()) {
                Node child = n.children[ch - 'a'];
                if (--child.passCount == 0) {              // nobody else uses this branch: prune it
                    n.children[ch - 'a'] = null;
                    size--;
                    return true;
                }
                n = child;
            }
            n.isEnd = false;                               // the word was a prefix of others: just unmark
            size--;
            return true;
        }

        // AUTOCOMPLETE: every word with this prefix, in alphabetical order (DFS a..z)
        List<String> wordsWithPrefix(String prefix) {
            List<String> out = new ArrayList<>();
            Node n = walk(prefix);
            if (n != null) collect(n, new StringBuilder(prefix), out);
            return out;
        }

        private void collect(Node n, StringBuilder sb, List<String> out) {
            if (n.isEnd) out.add(sb.toString());
            for (int i = 0; i < 26; i++) {
                if (n.children[i] == null) continue;
                sb.append((char) ('a' + i));
                collect(n.children[i], sb, out);
                sb.deleteCharAt(sb.length() - 1);          // backtrack
            }
        }

        // Longest common prefix of ALL words: walk while there's exactly one child and no word ends
        String longestCommonPrefix() {
            StringBuilder sb = new StringBuilder();
            Node n = root;
            while (n != null && !n.isEnd) {
                int only = -1, count = 0;
                for (int i = 0; i < 26; i++) if (n.children[i] != null) { only = i; count++; }
                if (count != 1) break;
                sb.append((char) ('a' + only));
                n = n.children[only];
            }
            return sb.toString();
        }

        int nodeCount() { return countNodes(root); }

        private int countNodes(Node n) {
            int c = 1;
            for (Node ch : n.children) if (ch != null) c += countNodes(ch);
            return c;
        }
    }

    // ---------------- 2. HashMap-based trie (any characters) ----------------
    static class MapTrie {
        static class Node {
            Map<Character, Node> children = new HashMap<>();
            boolean isEnd;
        }

        final Node root = new Node();

        void insert(String w) {
            Node n = root;
            for (char c : w.toCharArray()) n = n.children.computeIfAbsent(c, k -> new Node());
            n.isEnd = true;
        }

        boolean search(String w) {
            Node n = root;
            for (char c : w.toCharArray()) {
                n = n.children.get(c);
                if (n == null) return false;
            }
            return n.isEnd;
        }
    }

    public static void main(String[] args) {
        Trie t = new Trie();
        for (String w : List.of("car", "cart", "care", "cat", "dog")) t.insert(w);

        System.out.println("1. search vs startsWith");
        check("search car (ends at an internal node)", true, t.search("car"));
        check("search ca (prefix only)", false, t.search("ca"));
        check("startsWith ca", true, t.startsWith("ca"));
        check("startsWith cow", false, t.startsWith("cow"));

        System.out.println("2. Prefix counts and autocomplete");
        check("countPrefix car", 3, t.countPrefix("car"));
        check("countPrefix c", 4, t.countPrefix("c"));
        check("autocomplete ca (sorted)", "[car, care, cart, cat]", t.wordsWithPrefix("ca"));
        check("nodes used (shared prefixes)", 10, t.nodeCount());

        System.out.println("3. Delete");
        t.delete("car");
        check("car gone", false, t.search("car"));
        check("cart still there", true, t.search("cart"));
        t.delete("dog");
        check("dog branch pruned", false, t.startsWith("d"));
        check("size", 3, t.size);

        System.out.println("4. Longest common prefix");
        Trie lcp = new Trie();
        for (String w : List.of("flower", "flow", "flight")) lcp.insert(w);
        check("flower, flow, flight", "fl", lcp.longestCommonPrefix());

        System.out.println("5. HashMap trie (any alphabet)");
        MapTrie mt = new MapTrie();
        mt.insert("Hello, World!");
        mt.insert("नमस्ते");
        check("search unicode", true, mt.search("नमस्ते"));
        check("search Hello", false, mt.search("Hello"));

        System.out.println("6. 20,000 random ops vs a TreeSet (prefix queries by scanning)");
        Random rnd = new Random(1);
        Trie mine = new Trie();
        TreeSet<String> ref = new TreeSet<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 1 + rnd.nextInt(5); j++) sb.append((char) ('a' + rnd.nextInt(3)));
            String w = sb.toString();
            switch (rnd.nextInt(4)) {
                case 0, 1 -> { mine.insert(w); ref.add(w); }
                case 2 -> ok &= mine.delete(w) == ref.remove(w);
                default -> {
                    String p = w.substring(0, 1 + rnd.nextInt(w.length()));
                    List<String> exp = new ArrayList<>();
                    for (String s : ref) if (s.startsWith(p)) exp.add(s);
                    ok &= mine.wordsWithPrefix(p).equals(exp) && mine.countPrefix(p) == exp.size() && mine.search(w) == ref.contains(w);
                }
            }
        }
        check("trie == TreeSet", true, ok);
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
