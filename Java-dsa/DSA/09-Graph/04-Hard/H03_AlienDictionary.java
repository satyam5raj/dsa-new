/*
 * ============================================================================
 *  HARD 03 : ALIEN DICTIONARY                LeetCode #269 (premium)
 *  https://leetcode.com/problems/alien-dictionary/
 *  Pattern: derive EDGES from adjacent words, then TOPOLOGICAL SORT the letters
 * ============================================================================
 *
 *  PROBLEM
 *    words are sorted in an unknown alphabet. Return any letter order consistent
 *    with them, or "" if none exists.
 *      ["wrt","wrf","er","ett","rftt"] -> "wertf"
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1 - EDGES. Only ADJACENT words give information (the order is transitive),
 *  and only at their FIRST differing position:
 *     "wrt" vs "wrf": first difference t / f   ->  t comes before f   (edge t -> f)
 *     "wrf" vs "er" :                w / e     ->  w -> e
 *     "er"  vs "ett":                r / t     ->  r -> t
 *     "ett" vs "rftt":               e / r     ->  e -> r
 *  Letters AFTER the first difference tell us nothing.
 *
 *  THE INVALID-PREFIX TRAP: ["abc", "ab"] is impossible - a word can't come after
 *  its own prefix. If the shorter word is a prefix of the earlier longer word -> "".
 *
 *  STEP 2 - KAHN over all letters that APPEAR (letters with no edges still count):
 *    w -> e -> r -> t -> f   gives "wertf".
 *    If not every letter gets output, there's a cycle -> "".
 *
 *  COMPLEXITY   Time O(total characters)   Space O(26^2)
 * ============================================================================
 */

import java.util.*;

public class H03_AlienDictionary {

    static String alienOrder(String[] words) {
        Map<Character, Set<Character>> adj = new TreeMap<>();
        Map<Character, Integer> indeg = new TreeMap<>();
        for (String w : words) for (char c : w.toCharArray()) { adj.putIfAbsent(c, new TreeSet<>()); indeg.putIfAbsent(c, 0); }

        for (int i = 0; i + 1 < words.length; i++) {
            String a = words[i], b = words[i + 1];
            int len = Math.min(a.length(), b.length()), j = 0;
            while (j < len && a.charAt(j) == b.charAt(j)) j++;
            if (j == len) {
                if (a.length() > b.length()) return "";            // "abc" before "ab": impossible
                continue;                                           // a is a prefix of b: no information
            }
            char from = a.charAt(j), to = b.charAt(j);
            if (adj.get(from).add(to)) indeg.merge(to, 1, Integer::sum);   // count each edge once
        }

        Queue<Character> q = new ArrayDeque<>();
        for (Map.Entry<Character, Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        StringBuilder order = new StringBuilder();
        while (!q.isEmpty()) {
            char c = q.poll();
            order.append(c);
            for (char nxt : adj.get(c)) if (indeg.merge(nxt, -1, Integer::sum) == 0) q.add(nxt);
        }
        return order.length() == indeg.size() ? order.toString() : "";   // leftover letters = cycle
    }

    // is the word list sorted under this alphabet?
    static boolean sortedUnder(String[] words, String alphabet) {
        int[] rank = new int[26];
        for (int i = 0; i < alphabet.length(); i++) rank[alphabet.charAt(i) - 'a'] = i;
        for (int i = 0; i + 1 < words.length; i++) {
            String a = words[i], b = words[i + 1];
            int j = 0;
            while (j < a.length() && j < b.length() && a.charAt(j) == b.charAt(j)) j++;
            if (j == Math.min(a.length(), b.length())) { if (a.length() > b.length()) return false; continue; }
            if (rank[a.charAt(j) - 'a'] > rank[b.charAt(j) - 'a']) return false;
        }
        return true;
    }

    // brute force: does ANY permutation of the letters work?
    static boolean anyOrderWorks(String[] words, List<Character> letters, int k) {
        if (k == letters.size()) {
            StringBuilder sb = new StringBuilder();
            for (char c : letters) sb.append(c);
            return sortedUnder(words, sb.toString());
        }
        for (int i = k; i < letters.size(); i++) {
            Collections.swap(letters, k, i);
            if (anyOrderWorks(words, letters, k + 1)) { Collections.swap(letters, k, i); return true; }
            Collections.swap(letters, k, i);
        }
        return false;
    }

    public static void main(String[] args) {
        check("example", "wertf", alienOrder(new String[]{"wrt", "wrf", "er", "ett", "rftt"}));
        check("[z,x]", "zx", alienOrder(new String[]{"z", "x"}));
        check("cycle [z,x,z]", "", alienOrder(new String[]{"z", "x", "z"}));
        check("prefix trap [abc,ab]", "", alienOrder(new String[]{"abc", "ab"}));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            String[] ws = new String[1 + rnd.nextInt(5)];
            for (int i = 0; i < ws.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 1 + rnd.nextInt(3); j++) sb.append((char) ('a' + rnd.nextInt(4)));
                ws[i] = sb.toString();
            }
            String got = alienOrder(ws);
            Set<Character> ls = new TreeSet<>();
            for (String w : ws) for (char c : w.toCharArray()) ls.add(c);
            boolean exists = anyOrderWorks(ws, new ArrayList<>(ls), 0);
            ok &= got.isEmpty() ? !exists : (got.length() == ls.size() && sortedUnder(ws, got));
        }
        check("1,000 random lists vs all permutations", true, ok);
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
