/*
 * ============================================================================
 *  LRU CACHE: HashMap + Doubly Linked List (and why both are needed)
 * ============================================================================
 *
 *  A CACHE keeps a limited number of entries in fast memory. When it's full and a
 *  new entry arrives, something must be EVICTED. Policies:
 *    LRU  (Least Recently Used)   evict the entry untouched for the longest time
 *    LFU  (Least Frequently Used) evict the entry used the fewest times (H01)
 *    FIFO evict the oldest inserted;  RANDOM;  TTL (expire after a time)
 *  LRU is the classic: recently used data is likely to be used again soon
 *  (temporal locality). Used in CPU caches, databases, browsers, CDNs, Redis.
 *
 *  ---------------------------------------------------------------------------
 *  REQUIREMENTS: get(key) and put(key, value) both O(1).
 *
 *  WHY ONE STRUCTURE ISN'T ENOUGH
 *    HashMap alone       : O(1) lookup, but no idea which entry is least recent
 *    Linked list alone   : order of use is easy, but finding a key is O(n)
 *  COMBINE THEM:
 *    DOUBLY linked list ordered by recency:  head <-> [most recent] ... [least recent] <-> tail
 *    HashMap key -> the list NODE (not just the value)
 *    get(k): map lookup O(1) -> unlink the node O(1) -> move it to the front O(1)
 *    put(k): update/insert at the front; if over capacity, remove tail.prev O(1)
 *            and delete its key from the map (that's why nodes store their KEY)
 *  WHY DOUBLY linked: unlinking a node needs its PREVIOUS node; a singly linked
 *  list would have to search for it (O(n)).
 *  DUMMY head/tail sentinels remove every null check (Linked Lists 01-Basics).
 *
 *  JAVA SHORTCUT: LinkedHashMap(capacity, 0.75f, accessOrder = true) keeps entries
 *  in access order, and overriding removeEldestEntry evicts automatically. Know
 *  both: interviewers usually want the hand-built version.
 *
 *  Run:  java LRUCacheBasics.java
 * ============================================================================
 */

import java.util.*;

public class LRUCacheBasics {

    // ---------------- 1. From scratch ----------------
    static class LRUCache<K, V> {
        private class Node {
            K key; V value;
            Node prev, next;
            Node(K k, V v) { key = k; value = v; }
        }

        private final int capacity;
        private final Map<K, Node> map = new HashMap<>();
        private final Node head = new Node(null, null), tail = new Node(null, null);   // sentinels

        LRUCache(int capacity) {
            this.capacity = capacity;
            head.next = tail;
            tail.prev = head;
        }

        V get(K key) {
            Node n = map.get(key);
            if (n == null) return null;
            moveToFront(n);                               // touching it makes it the most recent
            return n.value;
        }

        void put(K key, V value) {
            Node n = map.get(key);
            if (n != null) {                              // update an existing key
                n.value = value;
                moveToFront(n);
                return;
            }
            if (map.size() == capacity) {                 // full: evict the least recent
                Node lru = tail.prev;
                unlink(lru);
                map.remove(lru.key);                      // the node knows its key -> O(1) map removal
            }
            n = new Node(key, value);
            map.put(key, n);
            addFront(n);
        }

        private void unlink(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }

        private void addFront(Node n) {
            n.next = head.next;
            n.prev = head;
            head.next.prev = n;
            head.next = n;
        }

        private void moveToFront(Node n) { unlink(n); addFront(n); }

        List<K> orderMostToLeastRecent() {
            List<K> out = new ArrayList<>();
            for (Node n = head.next; n != tail; n = n.next) out.add(n.key);
            return out;
        }
    }

    // ---------------- 2. The LinkedHashMap shortcut ----------------
    static class LinkedHashLRU<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;
        LinkedHashLRU(int capacity) {
            super(capacity, 0.75f, true);                 // true = ACCESS order (get moves to the end)
            this.capacity = capacity;
        }
        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;                     // called after every put
        }
    }

    public static void main(String[] args) {
        System.out.println("1. From-scratch LRU (capacity 2)");
        LRUCache<Integer, String> c = new LRUCache<>(2);
        c.put(1, "one");
        c.put(2, "two");
        check("get 1", "one", c.get(1));                  // order: 1, 2
        c.put(3, "three");                                // evicts 2 (least recent)
        check("get 2 (evicted)", null, c.get(2));
        c.put(4, "four");                                 // evicts 1
        check("get 1 (evicted)", null, c.get(1));
        check("get 3", "three", c.get(3));
        check("order most -> least recent", "[3, 4]", c.orderMostToLeastRecent());
        c.put(4, "FOUR");                                 // update moves 4 to the front
        check("after updating 4", "[4, 3]", c.orderMostToLeastRecent());

        System.out.println("2. LinkedHashMap version");
        LinkedHashLRU<Integer, String> l = new LinkedHashLRU<>(2);
        l.put(1, "one");
        l.put(2, "two");
        l.get(1);
        l.put(3, "three");
        check("keys after evicting 2", "[1, 3]", l.keySet());

        System.out.println("3. 100,000 random ops: from scratch == LinkedHashMap");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 50; t++) {
            int cap = 1 + rnd.nextInt(10);
            LRUCache<Integer, Integer> a = new LRUCache<>(cap);
            LinkedHashLRU<Integer, Integer> b = new LinkedHashLRU<>(cap);
            for (int i = 0; i < 2000; i++) {
                int k = rnd.nextInt(20);
                if (rnd.nextBoolean()) { a.put(k, i); b.put(k, i); }
                else ok &= Objects.equals(a.get(k), b.get(k));
            }
        }
        check("identical behaviour", true, ok);
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
