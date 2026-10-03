/*
 * ============================================================================
 *  MEDIUM 06 : MAP SUM PAIRS  ⭐ bonus        LeetCode #677
 *  https://leetcode.com/problems/map-sum-pairs/
 *  Pattern: trie nodes store an AGGREGATE (prefix sum); updates push a DELTA
 * ============================================================================
 *
 *  PROBLEM
 *    insert(key, val) (overwrites an existing key's value); sum(prefix) = sum of
 *    values of all keys starting with prefix.
 *      insert("apple", 3); sum("ap") -> 3
 *      insert("app", 2);   sum("ap") -> 5
 *      insert("apple", 5); sum("ap") -> 7   (apple's 3 is REPLACED by 5)
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: a HashMap of keys; sum(prefix) scans all keys -> O(N * L) per query.
 *
 *  TRIE WITH SUMS: node.sum = total value of all keys passing through the node.
 *  Then sum(prefix) = the sum at the prefix's node: O(L).
 *
 *  THE OVERWRITE TRAP: inserting "apple" again must not ADD 5 on top of 3.
 *  Keep a HashMap key -> current value; compute delta = val - old (0 if new),
 *  and add the DELTA along the path. (Same idea as Fenwick tree updates.)
 *
 *  DRY RUN
 *    insert apple 3: path a,p,p,l,e each +3
 *    insert app 2:   path a,p,p each +2      -> node "ap" = 5
 *    insert apple 5: delta = 2 -> a,p,p,l,e each +2 -> "ap" = 7
 *
 *  COMPLEXITY   insert O(L), sum O(L)
 * ============================================================================
 */

import java.util.*;

public class M06_MapSumPairs {

    static class MapSum {
        private static class Node { Node[] next = new Node[26]; int sum; }
        private final Node root = new Node();
        private final Map<String, Integer> values = new HashMap<>();

        public void insert(String key, int val) {
            int delta = val - values.getOrDefault(key, 0);    // replace, don't add
            values.put(key, val);
            Node n = root;
            for (char c : key.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
                n.sum += delta;
            }
        }

        public int sum(String prefix) {
            Node n = root;
            for (char c : prefix.toCharArray()) {
                n = n.next[c - 'a'];
                if (n == null) return 0;
            }
            return n.sum;
        }
    }

    public static void main(String[] args) {
        MapSum m = new MapSum();
        m.insert("apple", 3);
        check("sum ap", 3, m.sum("ap"));
        m.insert("app", 2);
        check("sum ap", 5, m.sum("ap"));
        m.insert("apple", 5);
        check("sum ap after overwrite", 7, m.sum("ap"));
        check("sum b (none)", 0, m.sum("b"));
        Random rnd = new Random(6);
        MapSum mine = new MapSum();
        Map<String, Integer> ref = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 10_000; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 1 + rnd.nextInt(4); j++) sb.append((char) ('a' + rnd.nextInt(3)));
            String k = sb.toString();
            if (rnd.nextBoolean()) { int v = rnd.nextInt(100); mine.insert(k, v); ref.put(k, v); }
            else ok &= mine.sum(k) == ref.entrySet().stream().filter(e -> e.getKey().startsWith(k)).mapToInt(Map.Entry::getValue).sum();
        }
        check("10,000 random ops vs scanning a map", true, ok);
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
