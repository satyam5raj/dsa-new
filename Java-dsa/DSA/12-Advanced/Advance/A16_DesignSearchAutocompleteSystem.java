/*
 * ============================================================================
 *  ADVANCE 16 : DESIGN SEARCH AUTOCOMPLETE SYSTEM  ⭐ bonus   LeetCode #642
 *  https://leetcode.com/problems/design-search-autocomplete-system/
 *  Pattern: TRIE (follow the typed prefix one char at a time) + size-3 HEAP for the top 3
 * ============================================================================
 *
 *  PROBLEM
 *    Built from past sentences and how many times each was typed ("hot degree").
 *    input(c): the user types one character.
 *      c == '#'  -> the typed sentence is finished: record it (+1 hot degree), reset,
 *                   return []
 *      otherwise -> return the top 3 past sentences starting with everything typed so far,
 *                   by hot degree (high first), ties by ASCII order (' ' < 'a')
 *    sentences ["i love you","island","iroman","i love leetcode"], times [5,3,2,2]
 *      input 'i' -> ["i love you", "island", "i love leetcode"]
 *      input ' ' -> ["i love you", "i love leetcode"]
 *      input 'a' -> []
 *      input '#' -> []          ("i a" is now saved with degree 1)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE (Approach 1): a HashMap sentence -> count. On every keystroke filter all
 *  sentences by startsWith(prefix) and sort. O(N * L) per keystroke.
 *
 *  TRIE (Approach 2, main):
 *    - every sentence is a path of characters; the node at its end stores the sentence
 *      and its count
 *    - keep a pointer 'cur' to the node of the prefix typed so far. Each new character is
 *      ONE step down (no re-walking from the root). If the step doesn't exist, cur = null
 *      and every later keystroke returns [] immediately until '#'.
 *    - to answer: visit only cur's subtree (the sentences with this prefix) and keep the
 *      best 3 in a min-heap of size 3, where the WORST of the three is on top
 *
 *  DRY RUN input 'i' -> cur = node "i"; subtree sentences:
 *    i love you(5) island(3) iroman(2) i love leetcode(2)
 *    heap keeps the best 3: drop iroman ("i love leetcode" < "iroman" in ASCII since ' ' < 'r')
 *    -> [i love you, island, i love leetcode]
 *
 *  COMPLEXITY   a keystroke O(1) step + O(S log 3) for S sentences under the prefix
 *               '#' O(L) to insert   Space O(total characters)
 * ============================================================================
 */

import java.util.*;

public class A16_DesignSearchAutocompleteSystem {

    static final Comparator<Map.Entry<String, Integer>> RANK =             // better first
            (a, b) -> !a.getValue().equals(b.getValue()) ? Integer.compare(b.getValue(), a.getValue()) : a.getKey().compareTo(b.getKey());

    // ---------------- Approach 1: HashMap + filter + sort (the referee) ----------------
    static class MapAutocomplete {
        private final Map<String, Integer> count = new HashMap<>();
        private final StringBuilder typed = new StringBuilder();

        MapAutocomplete(String[] sentences, int[] times) { for (int i = 0; i < sentences.length; i++) count.merge(sentences[i], times[i], Integer::sum); }

        public List<String> input(char c) {
            if (c == '#') { count.merge(typed.toString(), 1, Integer::sum); typed.setLength(0); return List.of(); }
            typed.append(c);
            String prefix = typed.toString();
            List<Map.Entry<String, Integer>> matches = new ArrayList<>();
            for (Map.Entry<String, Integer> e : count.entrySet()) if (e.getKey().startsWith(prefix)) matches.add(e);
            matches.sort(RANK);
            List<String> out = new ArrayList<>();
            for (int i = 0; i < Math.min(3, matches.size()); i++) out.add(matches.get(i).getKey());
            return out;
        }
    }

    // ---------------- Approach 2: trie + size-3 heap (main solution) ----------------
    static class AutocompleteSystem {
        private static class Node {
            final Map<Character, Node> children = new HashMap<>();
            String sentence;                                             // non-null at the end of a sentence
            int count;
        }

        private final Node root = new Node();
        private Node cur = root;                                         // node of the prefix typed so far (null = no match)
        private final StringBuilder typed = new StringBuilder();

        AutocompleteSystem(String[] sentences, int[] times) { for (int i = 0; i < sentences.length; i++) insert(sentences[i], times[i]); }

        private void insert(String s, int times) {
            Node node = root;
            for (char ch : s.toCharArray()) node = node.children.computeIfAbsent(ch, k -> new Node());
            node.sentence = s;
            node.count += times;
        }

        public List<String> input(char c) {
            if (c == '#') {
                insert(typed.toString(), 1);
                typed.setLength(0);
                cur = root;
                return List.of();
            }
            typed.append(c);
            if (cur != null) cur = cur.children.get(c);                  // one step down the trie
            if (cur == null) return List.of();

            PriorityQueue<Map.Entry<String, Integer>> best = new PriorityQueue<>(RANK.reversed());   // worst of the 3 on top
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(cur);
            while (!stack.isEmpty()) {                                   // every sentence under this prefix
                Node node = stack.pop();
                if (node.sentence != null) {
                    best.add(Map.entry(node.sentence, node.count));
                    if (best.size() > 3) best.poll();                    // drop the worst
                }
                for (Node child : node.children.values()) stack.push(child);
            }
            List<Map.Entry<String, Integer>> top = new ArrayList<>(best);
            top.sort(RANK);
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, Integer> e : top) out.add(e.getKey());
            return out;
        }
    }

    public static void main(String[] args) {
        String[] s = {"i love you", "island", "iroman", "i love leetcode"};
        int[] times = {5, 3, 2, 2};
        AutocompleteSystem ac = new AutocompleteSystem(s, times);
        check("input 'i'", "[i love you, island, i love leetcode]", ac.input('i'));
        check("input ' '", "[i love you, i love leetcode]", ac.input(' '));
        check("input 'a'", "[]", ac.input('a'));
        check("input '#'", "[]", ac.input('#'));
        check("'i a' is now remembered", "[i love you, island, i love leetcode]", ac.input('i'));
        check("...and suggested", "[i love you, i love leetcode, i a]", ac.input(' '));

        Random rnd = new Random(16);
        char[] alphabet = {'a', 'b', ' '};
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = rnd.nextInt(8);
            String[] sent = new String[n];
            int[] tm = new int[n];
            for (int i = 0; i < n; i++) {
                StringBuilder sb = new StringBuilder("a");
                for (int l = rnd.nextInt(4); l > 0; l--) sb.append(alphabet[rnd.nextInt(3)]);
                sent[i] = sb.toString();
                tm[i] = 1 + rnd.nextInt(3);
            }
            AutocompleteSystem main = new AutocompleteSystem(sent, tm);
            MapAutocomplete ref = new MapAutocomplete(sent, tm);
            for (int i = 0; i < 80; i++) {
                char c = rnd.nextInt(5) == 0 ? '#' : alphabet[rnd.nextInt(3)];
                ok &= main.input(c).equals(ref.input(c));
            }
        }
        check("24,000 random keystrokes: trie == map", true, ok);
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
