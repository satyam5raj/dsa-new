/*
 * ============================================================================
 *  EASY 04 : DESIGN HASHMAP                  LeetCode #706
 *  https://leetcode.com/problems/design-hashmap/
 *  Pattern: BUCKETS OF [key, value] PAIRS (chaining) - E03 with values
 * ============================================================================
 *
 *  PROBLEM
 *    put(key, value) (insert or UPDATE), get(key) (-1 if missing), remove(key).
 *    Keys and values are ints in [0, 10^6]. No built-in hash tables.
 *
 *  ---------------------------------------------------------------------------
 *  Same structure as Design HashSet, but each chain node stores (key, value):
 *    put:    walk the bucket; if the key exists -> UPDATE its value, else add a new node
 *    get:    walk the bucket; return the value, or -1
 *    remove: walk the bucket with a 'prev' pointer and unlink the node
 *
 *  Here the chain is a hand-written singly linked list with a DUMMY head per
 *  bucket (the Linked Lists topic's trick), so 'remove' needs no special case
 *  for the first node.
 *
 *  THE "UPDATE, DON'T DUPLICATE" BUG: forgetting to check for an existing key in
 *  put() creates two nodes for one key, and get() returns whichever comes first.
 *  The random test below would catch it.
 *
 *  For the full-featured version (generic keys, resizing, load factor), see
 *  01-Basics/HashTableChaining.java.
 *  COMPLEXITY   O(1) average per operation
 * ============================================================================
 */

import java.util.*;

public class E04_DesignHashMap {

    static class MyHashMap {
        private static class Node {
            int key, value;
            Node next;
            Node(int key, int value, Node next) { this.key = key; this.value = value; this.next = next; }
        }

        private static final int BUCKETS = 2069;                  // prime
        private final Node[] buckets = new Node[BUCKETS];          // each slot: a dummy head (created lazily)

        private Node dummy(int key) {
            int i = key % BUCKETS;
            if (buckets[i] == null) buckets[i] = new Node(-1, -1, null);
            return buckets[i];
        }

        // returns the node BEFORE the one holding key (or the last node if absent)
        private Node findPrev(Node dummy, int key) {
            Node prev = dummy;
            while (prev.next != null && prev.next.key != key) prev = prev.next;
            return prev;
        }

        public void put(int key, int value) {
            Node prev = findPrev(dummy(key), key);
            if (prev.next != null) prev.next.value = value;         // UPDATE an existing key
            else prev.next = new Node(key, value, null);            // insert at the end
        }

        public int get(int key) {
            Node prev = findPrev(dummy(key), key);
            return prev.next == null ? -1 : prev.next.value;
        }

        public void remove(int key) {
            Node prev = findPrev(dummy(key), key);
            if (prev.next != null) prev.next = prev.next.next;      // unlink
        }
    }

    public static void main(String[] args) {
        MyHashMap m = new MyHashMap();
        m.put(1, 1);
        m.put(2, 2);
        check("get(1)", 1, m.get(1));
        check("get(3) missing", -1, m.get(3));
        m.put(2, 1);
        check("get(2) after update", 1, m.get(2));
        m.remove(2);
        check("get(2) after remove", -1, m.get(2));

        Random rnd = new Random(4);
        MyHashMap mine = new MyHashMap();
        Map<Integer, Integer> ref = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 50_000; i++) {
            int k = rnd.nextInt(10_000), v = rnd.nextInt(1000);
            switch (rnd.nextInt(3)) {
                case 0 -> { mine.put(k, v); ref.put(k, v); }
                case 1 -> { mine.remove(k); ref.remove(k); }
                default -> ok &= mine.get(k) == ref.getOrDefault(k, -1);
            }
        }
        check("50,000 random ops vs java.util.HashMap", true, ok);
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
