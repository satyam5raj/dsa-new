/*
 * ============================================================================
 *  TOPIC 3.3 : MAP - HashMap (internals!), LinkedHashMap, TreeMap, ConcurrentHashMap
 * ============================================================================
 *
 *  Run:  java MapDemo.java
 *
 *  MAP = key -> value pairs. Keys are unique; values can repeat.
 *
 *  ------------------------------------------------------------------------------
 *   Impl               Order              null key?   Thread-safe?  get/put
 *  ------------------------------------------------------------------------------
 *   HashMap            none               1 allowed   No            O(1) avg
 *   LinkedHashMap      insertion/access   1 allowed   No            O(1) avg
 *   TreeMap            sorted by key      No          No            O(log n)
 *   ConcurrentHashMap  none               No          Yes           O(1) avg
 *   Hashtable(legacy)  none               No          Yes (slow)    O(1) avg
 *  ------------------------------------------------------------------------------
 *
 *  =========================  HASHMAP INTERNALS  =========================
 *  (the #1 Core Java interview topic)
 *
 *  STRUCTURE: an array called 'table' of "buckets". Each bucket holds a
 *  linked list of Node{hash, key, value, next}  (or a red-black tree, see below).
 *
 *     table:  [0] -> null
 *             [1] -> (k1,v1) -> (k9,v9) -> null      <- collision chain
 *             [2] -> null
 *             [3] -> (k3,v3) -> null
 *             ...
 *
 *  PUT(key, value) step by step:
 *   1. h = key.hashCode()
 *   2. "spread" the bits: hash = h ^ (h >>> 16)
 *        (mixes high bits into low bits, because only low bits pick the bucket)
 *   3. index = hash & (capacity - 1)
 *        (same as hash % capacity, but faster; works because capacity
 *         is ALWAYS a power of 2)
 *   4. bucket empty?  -> put a new node there
 *      bucket not empty? -> walk the chain:
 *           same hash && key.equals(existing) -> REPLACE value
 *           reached the end -> append new node (collision)
 *   5. size++ ; if size > threshold -> RESIZE
 *
 *  GET(key): same steps 1-3, then walk the bucket comparing with equals().
 *
 *  KEY NUMBERS
 *   - Default initial capacity : 16
 *   - Load factor              : 0.75
 *   - Threshold                : capacity * loadFactor  (16 * 0.75 = 12)
 *     -> when the 13th entry is added, the table DOUBLES to 32.
 *   - Why 0.75? A balance: lower = fewer collisions but wasted memory,
 *     higher = less memory but longer chains.
 *
 *  RESIZE (rehash)
 *   - New table of 2x size; every node is moved.
 *   - Clever trick: since capacity doubles, each node either stays at
 *     index i or moves to i + oldCapacity (decided by one extra hash bit).
 *   - Resizing is O(n) -> if you know the size, pre-size the map:
 *       new HashMap<>(expectedSize / 0.75f + 1)
 *
 *  TREEIFICATION (Java 8+)
 *   - If ONE bucket's chain reaches 8 nodes (TREEIFY_THRESHOLD) AND the
 *     table capacity is >= 64 (MIN_TREEIFY_CAPACITY), that bucket becomes
 *     a RED-BLACK TREE -> worst case per bucket goes O(n) -> O(log n).
 *   - If capacity < 64, it RESIZES instead of treeifying.
 *   - Shrinks back to a list when the tree drops to 6 nodes (UNTREEIFY_THRESHOLD).
 *   - Protects against bad hashCodes and hash-flooding attacks.
 *
 *  WHY KEYS SHOULD BE IMMUTABLE
 *   - If a key's fields change after insertion, its hashCode changes,
 *     and get() looks in the WRONG bucket -> entry "lost".
 *   - String, Integer, records are ideal keys.
 *
 *  NOT THREAD-SAFE: concurrent puts can lose updates / corrupt the table.
 *  Use ConcurrentHashMap.
 */

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapDemo {

    public static void main(String[] args) {
        hashMapBasics();
        java8MapMethods();
        iteratingMaps();
        linkedHashMapAndLRU();
        treeMap();
        concurrentHashMap();
        mutableKeyProblem();
        customHashMapDemo();
    }

    // =================================================================
    // 1. HashMap basics
    // =================================================================
    static void hashMapBasics() {
        System.out.println("===== 1. HASHMAP BASICS =====");
        Map<String, Integer> ages = new HashMap<>();

        ages.put("Ravi", 25);
        ages.put("Asha", 30);
        Integer old = ages.put("Ravi", 26);   // same key -> REPLACES, returns old value
        System.out.println("put returned old value = " + old);
        System.out.println("map = " + ages);

        System.out.println("get(Ravi)        = " + ages.get("Ravi"));
        System.out.println("get(Unknown)     = " + ages.get("Unknown"));     // null if absent
        System.out.println("getOrDefault     = " + ages.getOrDefault("Unknown", 0));
        System.out.println("containsKey Asha = " + ages.containsKey("Asha"));  // O(1)
        System.out.println("containsValue 30 = " + ages.containsValue(30));    // O(n)!
        ages.remove("Asha");
        System.out.println("after remove     = " + ages + ", size = " + ages.size());

        ages.put(null, 0);                          // HashMap allows ONE null key (bucket 0)
        System.out.println("null key value   = " + ages.get(null));
    }

    // =================================================================
    // 2. Java 8 map methods - make counting/grouping one-liners
    // =================================================================
    static void java8MapMethods() {
        System.out.println("\n===== 2. JAVA 8 MAP METHODS =====");
        String text = "the cat and the hat and the bat";

        // FREQUENCY COUNT - the most common map pattern in DSA
        Map<String, Integer> freq = new HashMap<>();
        for (String w : text.split(" ")) {
            // merge: if absent put 1, else apply (old, 1) -> old + 1
            freq.merge(w, 1, Integer::sum);
        }
        System.out.println("merge freq       = " + freq);

        // Same thing, older style
        Map<String, Integer> freq2 = new HashMap<>();
        for (String w : text.split(" ")) {
            freq2.put(w, freq2.getOrDefault(w, 0) + 1);
        }
        System.out.println("getOrDefault freq= " + freq2);

        // GROUPING: computeIfAbsent creates the list only if the key is missing
        Map<Integer, List<String>> byLength = new HashMap<>();
        for (String w : text.split(" ")) {
            byLength.computeIfAbsent(w.length(), k -> new ArrayList<>()).add(w);
        }
        System.out.println("group by length  = " + byLength);

        Map<String, Integer> m = new HashMap<>(Map.of("a", 1));
        m.putIfAbsent("a", 100);                        // ignored, "a" exists
        m.putIfAbsent("b", 2);                          // added
        m.computeIfPresent("a", (k, v) -> v * 10);     // a -> 10
        m.compute("c", (k, v) -> v == null ? 1 : v + 1); // works whether present or not
        m.replaceAll((k, v) -> v + 1);                  // update every value
        System.out.println("misc ops         = " + m);
    }

    // =================================================================
    // 3. Iterating
    // =================================================================
    static void iteratingMaps() {
        System.out.println("\n===== 3. ITERATING =====");
        Map<String, Integer> scores = new HashMap<>(Map.of("A", 90, "B", 80, "C", 70));

        // entrySet: best when you need both key and value (one lookup)
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            System.out.print(e.getKey() + "=" + e.getValue() + " ");
        }
        System.out.println(" <- entrySet");

        for (String k : scores.keySet()) System.out.print(k + " ");
        System.out.println(" <- keySet");

        for (int v : scores.values()) System.out.print(v + " ");
        System.out.println(" <- values");

        scores.forEach((k, v) -> System.out.print(k + ":" + v + " "));
        System.out.println(" <- forEach");

        // Sort entries by value descending (e.g. "top K frequent")
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        System.out.println("sorted by value desc = " + entries);
    }

    // =================================================================
    // 4. LinkedHashMap - keeps a doubly linked list through all entries
    //    accessOrder=false (default): iteration in INSERTION order
    //    accessOrder=true           : iteration in ACCESS order (LRU first)
    //    Override removeEldestEntry -> instant LRU cache!
    // =================================================================
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        LRUCache(int capacity) {
            super(16, 0.75f, true);    // true = access order
            this.capacity = capacity;
        }

        // Called after every put; return true to evict the eldest (least recently used)
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    static void linkedHashMapAndLRU() {
        System.out.println("\n===== 4. LINKEDHASHMAP & LRU =====");
        Map<String, Integer> lhm = new LinkedHashMap<>();
        lhm.put("zebra", 1);
        lhm.put("apple", 2);
        lhm.put("mango", 3);
        System.out.println("insertion order = " + lhm);

        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);                 // 1 becomes most recently used
        cache.put(3, "three");        // evicts 2 (least recently used)
        System.out.println("LRU cache       = " + cache);
    }

    // =================================================================
    // 5. TreeMap - sorted by key (Red-Black tree), navigation methods
    // =================================================================
    static void treeMap() {
        System.out.println("\n===== 5. TREEMAP =====");
        TreeMap<Integer, String> tm = new TreeMap<>();
        tm.put(30, "thirty");
        tm.put(10, "ten");
        tm.put(20, "twenty");
        tm.put(40, "forty");
        System.out.println("sorted map      = " + tm);
        System.out.println("firstKey/lastKey= " + tm.firstKey() + " / " + tm.lastKey());
        System.out.println("floorKey(25)    = " + tm.floorKey(25));      // 20
        System.out.println("ceilingEntry(25)= " + tm.ceilingEntry(25));  // 30=thirty
        System.out.println("headMap(30)     = " + tm.headMap(30));       // keys < 30
        System.out.println("tailMap(30)     = " + tm.tailMap(30));       // keys >= 30
        System.out.println("descendingMap   = " + tm.descendingMap());

        // Real use: map a score to a grade with floorEntry
        TreeMap<Integer, Character> grades = new TreeMap<>(Map.of(0, 'F', 50, 'C', 70, 'B', 90, 'A'));
        System.out.println("grade for 76    = " + grades.floorEntry(76).getValue());
    }

    // =================================================================
    // 6. ConcurrentHashMap
    //    Java 7: split into 16 "segments", each with its own lock.
    //    Java 8+: no segments. Uses CAS (compare-and-swap) to insert into an
    //             EMPTY bucket, and 'synchronized' on the bucket's FIRST NODE
    //             otherwise -> locking per bucket, so different buckets can be
    //             written in parallel. Reads are mostly lock-free (volatile).
    //    - No null keys or values (null would be ambiguous: "absent" or "null value"?)
    //    - Iterators are WEAKLY CONSISTENT: never throw
    //      ConcurrentModificationException; may or may not see recent updates.
    //    - Use the ATOMIC methods (merge, compute, putIfAbsent). A
    //      "get then put" sequence is NOT atomic even on a concurrent map.
    // =================================================================
    static void concurrentHashMap() {
        System.out.println("\n===== 6. CONCURRENTHASHMAP =====");
        Map<String, Integer> plain = new HashMap<>();
        Map<String, Integer> concurrent = new ConcurrentHashMap<>();

        Runnable task = () -> {
            for (int i = 0; i < 10_000; i++) {
                try {
                    plain.merge("count", 1, Integer::sum);   // unsafe on HashMap
                } catch (ConcurrentModificationException ignored) {
                    // HashMap may even detect the race and throw - another sign it's unsafe
                }
                concurrent.merge("count", 1, Integer::sum);  // atomic on ConcurrentHashMap
            }
        };

        Thread t1 = new Thread(task), t2 = new Thread(task);
        t1.start();
        t2.start();
        try {
            t1.join();   // wait for both threads to finish
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("HashMap count           = " + plain.get("count") + " (expected 20000, often less)");
        System.out.println("ConcurrentHashMap count = " + concurrent.get("count"));

        try {
            concurrent.put("x", null);
        } catch (NullPointerException e) {
            System.out.println("ConcurrentHashMap rejects null values");
        }
    }

    // =================================================================
    // 7. The mutable key problem
    // =================================================================
    static void mutableKeyProblem() {
        System.out.println("\n===== 7. MUTABLE KEY PROBLEM =====");
        List<Integer> key = new ArrayList<>(List.of(1, 2));   // List's hashCode depends on contents
        Map<List<Integer>, String> map = new HashMap<>();
        map.put(key, "value");
        System.out.println("before mutation get = " + map.get(key));
        key.add(3);                                            // hashCode changes!
        System.out.println("after mutation get  = " + map.get(key) + "  (entry is lost)");
    }

    // =================================================================
    // 8. Build a mini HashMap yourself - the best way to understand it.
    //    Separate chaining, power-of-2 capacity, load factor 0.75, resize.
    // =================================================================
    static class MyHashMap<K, V> {

        // One entry in a bucket's linked list
        private static class Node<K, V> {
            final int hash;
            final K key;
            V value;
            Node<K, V> next;

            Node(int hash, K key, V value, Node<K, V> next) {
                this.hash = hash;
                this.key = key;
                this.value = value;
                this.next = next;
            }
        }

        private Node<K, V>[] table;
        private int size;
        private static final float LOAD_FACTOR = 0.75f;

        @SuppressWarnings("unchecked")
        MyHashMap() {
            table = (Node<K, V>[]) new Node[16];   // generic array creation needs a cast
        }

        // Same spreading as java.util.HashMap
        private int hash(Object key) {
            if (key == null) return 0;
            int h = key.hashCode();
            return h ^ (h >>> 16);
        }

        private int indexFor(int hash, int capacity) {
            return hash & (capacity - 1);           // capacity is a power of 2
        }

        public V put(K key, V value) {
            int h = hash(key);
            int i = indexFor(h, table.length);

            // Walk the chain: if key exists, replace its value
            for (Node<K, V> n = table[i]; n != null; n = n.next) {
                if (n.hash == h && Objects.equals(n.key, key)) {
                    V old = n.value;
                    n.value = value;
                    return old;
                }
            }
            // Not found: insert at the head of the chain (simple, O(1))
            table[i] = new Node<>(h, key, value, table[i]);
            if (++size > table.length * LOAD_FACTOR) resize();
            return null;
        }

        public V get(K key) {
            int h = hash(key);
            for (Node<K, V> n = table[indexFor(h, table.length)]; n != null; n = n.next) {
                if (n.hash == h && Objects.equals(n.key, key)) return n.value;
            }
            return null;
        }

        public V remove(K key) {
            int h = hash(key);
            int i = indexFor(h, table.length);
            Node<K, V> prev = null;
            for (Node<K, V> n = table[i]; n != null; prev = n, n = n.next) {
                if (n.hash == h && Objects.equals(n.key, key)) {
                    if (prev == null) table[i] = n.next;   // removing the head
                    else prev.next = n.next;               // unlink from the middle
                    size--;
                    return n.value;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private void resize() {
            Node<K, V>[] old = table;
            table = (Node<K, V>[]) new Node[old.length * 2];
            // Rehash every node into the new, bigger table
            for (Node<K, V> head : old) {
                for (Node<K, V> n = head; n != null; ) {
                    Node<K, V> next = n.next;              // save before relinking
                    int i = indexFor(n.hash, table.length);
                    n.next = table[i];
                    table[i] = n;
                    n = next;
                }
            }
            System.out.println("   [MyHashMap resized " + old.length + " -> " + table.length + "]");
        }

        public int size() { return size; }
    }

    static void customHashMapDemo() {
        System.out.println("\n===== 8. CUSTOM HASHMAP =====");
        MyHashMap<String, Integer> map = new MyHashMap<>();
        for (int i = 1; i <= 13; i++) map.put("key" + i, i);   // 13th insert triggers resize (> 12)
        map.put("key1", 100);                                   // replace
        System.out.println("get(key1) = " + map.get("key1") + ", get(key13) = " + map.get("key13"));
        map.remove("key5");
        System.out.println("after remove key5: get = " + map.get("key5") + ", size = " + map.size());
    }
}
