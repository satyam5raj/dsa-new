/*
 * ============================================================================
 *  MEDIUM 01 : LRU CACHE                     LeetCode #146
 *  https://leetcode.com/problems/lru-cache/
 *  Pattern: HashMap<key, Node> + DOUBLY linked list with dummy head/tail
 * ============================================================================
 *
 *  PROBLEM
 *    LRUCache(capacity); get(key) -> value or -1; put(key, value). When full, evict
 *    the LEAST RECENTLY USED key. Both O(1).
 *      cap 2: put(1,1) put(2,2) get(1)=1 put(3,3)[evicts 2] get(2)=-1
 *             put(4,4)[evicts 1] get(1)=-1 get(3)=3 get(4)=4
 *
 *  ---------------------------------------------------------------------------
 *  (Full explanation in 01-Basics/LRUCacheBasics. Interview-sized version here.)
 *
 *    map : key -> node       (find any entry in O(1))
 *    list: head <-> most recent ... least recent <-> tail
 *    get : node = map[key]; move it to the front; return value
 *    put : existing -> update + move to front
 *          new      -> if full: remove tail.prev from the list AND the map;
 *                      add the new node at the front
 *
 *  CHECKLIST OF BUGS INTERVIEWERS WATCH FOR
 *    - forgetting to move to front on get (then it's FIFO, not LRU)
 *    - forgetting to remove the evicted key from the MAP (memory leak + wrong get)
 *    - updating an existing key counts as a USE (move it to the front)
 *    - node must store its key, or eviction can't find the map entry
 *
 *  COMPLEXITY   O(1) per operation   Space O(capacity)
 * ============================================================================
 */

import java.util.*;

public class M01_LRUCache {

    static class LRUCache {
        private static class Node {
            int key, value;
            Node prev, next;
            Node(int k, int v) { key = k; value = v; }
        }

        private final int capacity;
        private final Map<Integer, Node> map = new HashMap<>();
        private final Node head = new Node(0, 0), tail = new Node(0, 0);

        public LRUCache(int capacity) {
            this.capacity = capacity;
            head.next = tail;
            tail.prev = head;
        }

        public int get(int key) {
            Node n = map.get(key);
            if (n == null) return -1;
            remove(n);
            addFirst(n);
            return n.value;
        }

        public void put(int key, int value) {
            Node n = map.get(key);
            if (n != null) {
                n.value = value;
                remove(n);
                addFirst(n);
                return;
            }
            if (map.size() == capacity) {
                Node lru = tail.prev;
                remove(lru);
                map.remove(lru.key);
            }
            n = new Node(key, value);
            map.put(key, n);
            addFirst(n);
        }

        private void remove(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }

        private void addFirst(Node n) {
            n.next = head.next;
            n.prev = head;
            head.next.prev = n;
            head.next = n;
        }
    }

    // referee: LinkedHashMap in access order
    static class Ref extends LinkedHashMap<Integer, Integer> {
        final int cap;
        Ref(int cap) { super(16, 0.75f, true); this.cap = cap; }
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Integer> e) { return size() > cap; }
    }

    public static void main(String[] args) {
        LRUCache c = new LRUCache(2);
        StringBuilder out = new StringBuilder();
        c.put(1, 1);
        c.put(2, 2);
        out.append(c.get(1)).append(' ');
        c.put(3, 3);
        out.append(c.get(2)).append(' ');
        c.put(4, 4);
        out.append(c.get(1)).append(' ').append(c.get(3)).append(' ').append(c.get(4));
        check("example", "1 -1 -1 3 4", out.toString());
        LRUCache one = new LRUCache(1);
        one.put(2, 1);
        one.put(2, 2);                                   // update, not insert
        check("cap 1 update", 2, one.get(2));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 100; t++) {
            int cap = 1 + rnd.nextInt(8);
            LRUCache mine = new LRUCache(cap);
            Ref ref = new Ref(cap);
            for (int i = 0; i < 2000; i++) {
                int k = rnd.nextInt(15);
                if (rnd.nextBoolean()) { mine.put(k, i); ref.put(k, i); }
                else ok &= mine.get(k) == ref.getOrDefault(k, -1);
            }
        }
        check("200,000 random ops vs LinkedHashMap", true, ok);
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
