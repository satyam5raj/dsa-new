/*
 * ============================================================================
 *  MEDIUM 04 : SEARCH SUGGESTIONS SYSTEM  ⭐ bonus   LeetCode #1268
 *  https://leetcode.com/problems/search-suggestions-system/
 *  Pattern: AUTOCOMPLETE - store the top-3 suggestions IN each trie node
 * ============================================================================
 *
 *  PROBLEM
 *    After each character typed of searchWord, suggest up to 3 products that start
 *    with the typed prefix, lexicographically smallest first.
 *      products ["mobile","mouse","moneypot","monitor","mousepad"], "mouse"
 *      m     -> [mobile, moneypot, monitor]
 *      mo    -> [mobile, moneypot, monitor]
 *      mou   -> [mouse, mousepad]
 *      mous  -> [mouse, mousepad]
 *      mouse -> [mouse, mousepad]
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: TRIE, suggestions cached in nodes
 *    Sort the products FIRST, then insert them in order. While inserting, add the
 *    product to every node on its path IF that node holds fewer than 3. Because
 *    products arrive sorted, each node keeps exactly its 3 smallest.
 *    Query: walk the search word; at each step output the node's list (once we
 *    fall off the trie, every later answer is empty).
 *    This is how real autocomplete works: precompute the top-k per prefix.
 *
 *  APPROACH 2: SORT + BINARY SEARCH (no trie, also great)
 *    In the sorted array, words with a given prefix form a CONTIGUOUS block.
 *    For each prefix, binary-search the first word >= prefix, then take up to 3
 *    following words that still start with the prefix.
 *
 *  COMPLEXITY   trie: O(total chars * 3) build, O(|searchWord|) query
 *               binary search: O(n log n + L * log n)
 * ============================================================================
 */

import java.util.*;

public class M04_SearchSuggestionsSystem {

    static class Node {
        Node[] next = new Node[26];
        List<String> top3 = new ArrayList<>(3);
    }

    static List<List<String>> suggestedProducts(String[] products, String searchWord) {
        String[] sorted = products.clone();
        Arrays.sort(sorted);                                     // insert in lexicographic order
        Node root = new Node();
        for (String p : sorted) {
            Node n = root;
            for (char c : p.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
                if (n.top3.size() < 3) n.top3.add(p);            // the first 3 to arrive are the smallest
            }
        }
        List<List<String>> result = new ArrayList<>();
        Node n = root;
        for (char c : searchWord.toCharArray()) {
            n = n == null ? null : n.next[c - 'a'];
            result.add(n == null ? List.of() : n.top3);
        }
        return result;
    }

    static List<List<String>> binarySearch(String[] products, String searchWord) {
        String[] s = products.clone();
        Arrays.sort(s);
        List<List<String>> result = new ArrayList<>();
        StringBuilder prefix = new StringBuilder();
        for (char c : searchWord.toCharArray()) {
            prefix.append(c);
            String p = prefix.toString();
            int lo = 0, hi = s.length;
            while (lo < hi) { int mid = (lo + hi) / 2; if (s[mid].compareTo(p) < 0) lo = mid + 1; else hi = mid; }   // first >= p
            List<String> sug = new ArrayList<>();
            for (int i = lo; i < s.length && sug.size() < 3 && s[i].startsWith(p); i++) sug.add(s[i]);
            result.add(sug);
        }
        return result;
    }

    public static void main(String[] args) {
        String[] products = {"mobile", "mouse", "moneypot", "monitor", "mousepad"};
        check("mouse", "[[mobile, moneypot, monitor], [mobile, moneypot, monitor], [mouse, mousepad], [mouse, mousepad], [mouse, mousepad]]",
                suggestedProducts(products, "mouse"));
        check("havana", "[[havana], [havana], [havana], [havana], [havana], [havana]]", suggestedProducts(new String[]{"havana"}, "havana"));
        check("falls off the trie at 'x'", "[[baggage, bags, banner], [baggage, bags, banner], [baggage, bags], []]",
                suggestedProducts(new String[]{"bags", "baggage", "banner", "box", "cloths"}, "bagx"));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            String[] ps = new String[1 + rnd.nextInt(15)];
            for (int i = 0; i < ps.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(5); j++) sb.append((char) ('a' + rnd.nextInt(3)));
                ps[i] = sb.toString();
            }
            String q = ps[rnd.nextInt(ps.length)] + (rnd.nextBoolean() ? "a" : "");
            ok &= suggestedProducts(ps, q).equals(binarySearch(ps, q));
        }
        check("500 random catalogs: trie == binary search", true, ok);
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
