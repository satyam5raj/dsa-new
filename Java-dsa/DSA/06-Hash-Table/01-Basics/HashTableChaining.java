/*
 * ============================================================================
 *  HASH TABLE 1.2 : SEPARATE CHAINING - a generic HashMap from scratch
 * ============================================================================
 *
 *  Run:  java HashTableChaining.java
 *
 *  Each bucket holds a small LINKED LIST of entries whose keys hashed there.
 *
 *    buckets[0] -> null
 *    buckets[1] -> (k1,v1) -> (k9,v9) -> null       collision: both keys hashed to 1
 *    buckets[2] -> (k2,v2) -> null
 *
 *  OPERATIONS (all walk ONE bucket's chain)
 *    put(k, v):  find the bucket; if k is in its chain, REPLACE the value; else prepend a new entry
 *    get(k):     find the bucket; walk the chain comparing with equals()
 *    remove(k):  find the bucket; unlink the entry from the chain
 *  With load factor <= 0.75, the average chain length is < 1 -> O(1) average.
 *
 *  RESIZE: when size > capacity * 0.75, double the capacity and move EVERY entry to
 *  its new bucket (its index depends on the capacity!). O(n) per resize, but the
 *  doubling makes it O(1) amortized per put.
 *
 *  PROS of chaining: simple; deletion is easy; degrades gracefully even above load 1.
 *  CONS: a pointer per entry; scattered memory (poor cache use).
 *  java.util.HashMap uses chaining (+ treeified buckets when a chain gets long).
 *  A deeper look at java.util.HashMap's internals is in Core-Java/03-Collections/MapDemo.java.
 */

import java.util.*;

public class HashTableChaining<K, V> {

    private static class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next;
        Entry(K key, V value, Entry<K, V> next) { this.key = key; this.value = value; this.next = next; }
    }

    private Entry<K, V>[] buckets;
    private int size;
    private int resizes;
    private static final double MAX_LOAD = 0.75;

    @SuppressWarnings("unchecked")
    public HashTableChaining() {
        buckets = (Entry<K, V>[]) new Entry[8];
    }

    private int indexFor(Object key, int capacity) {
        int h = (key == null) ? 0 : key.hashCode();
        h ^= (h >>> 16);                          // mix the high bits into the low bits (like HashMap)
        return h & (capacity - 1);                // capacity is a power of 2
    }

    public V put(K key, V value) {
        int i = indexFor(key, buckets.length);
        for (Entry<K, V> e = buckets[i]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) {     // key exists -> replace
                V old = e.value;
                e.value = value;
                return old;
            }
        }
        buckets[i] = new Entry<>(key, value, buckets[i]);   // prepend: O(1)
        if (++size > buckets.length * MAX_LOAD) resize();
        return null;
    }

    public V get(K key) {
        for (Entry<K, V> e = buckets[indexFor(key, buckets.length)]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) return e.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        for (Entry<K, V> e = buckets[indexFor(key, buckets.length)]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) return true;
        }
        return false;
    }

    public V remove(K key) {
        int i = indexFor(key, buckets.length);
        Entry<K, V> prev = null;
        for (Entry<K, V> e = buckets[i]; e != null; prev = e, e = e.next) {
            if (Objects.equals(e.key, key)) {
                if (prev == null) buckets[i] = e.next;      // removing the chain's head
                else prev.next = e.next;
                size--;
                return e.value;
            }
        }
        return null;
    }

    public int size() { return size; }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] old = buckets;
        buckets = (Entry<K, V>[]) new Entry[old.length * 2];
        for (Entry<K, V> head : old) {
            for (Entry<K, V> e = head; e != null; ) {
                Entry<K, V> next = e.next;                  // save it before relinking
                int i = indexFor(e.key, buckets.length);    // the NEW index (the capacity changed)
                e.next = buckets[i];
                buckets[i] = e;
                e = next;
            }
        }
        resizes++;
    }

    // chain-length statistics, for the demo
    String chainStats() {
        int used = 0, longest = 0;
        for (Entry<K, V> head : buckets) {
            int len = 0;
            for (Entry<K, V> e = head; e != null; e = e.next) len++;
            if (len > 0) used++;
            longest = Math.max(longest, len);
        }
        return String.format("size=%d capacity=%d load=%.2f usedBuckets=%d longestChain=%d resizes=%d",
                size, buckets.length, (double) size / buckets.length, used, longest, resizes);
    }

    public static void main(String[] args) {
        System.out.println("===== BASIC OPERATIONS =====");
        HashTableChaining<String, Integer> ages = new HashTableChaining<>();
        ages.put("ravi", 30);
        ages.put("asha", 25);
        ages.put("ravi", 31);                               // replace
        ages.put(null, 0);                                  // null key allowed (bucket 0), like HashMap
        System.out.println("   get(ravi)=" + ages.get("ravi") + " get(asha)=" + ages.get("asha") + " get(null)=" + ages.get(null) + " get(zz)=" + ages.get("zz"));
        System.out.println("   remove(asha)=" + ages.remove("asha") + " containsKey(asha)=" + ages.containsKey("asha") + " size=" + ages.size());

        System.out.println("\n===== GROWTH: chains stay short thanks to resizing =====");
        HashTableChaining<Integer, Integer> t = new HashTableChaining<>();
        for (int i = 0; i < 100_000; i++) {
            t.put(i * 7919, i);
            if (i == 5 || i == 1000 || i == 99_999) System.out.println("   after " + (i + 1) + " puts: " + t.chainStats());
        }

        // self-check vs java.util.HashMap
        Random rnd = new Random(1);
        HashTableChaining<Integer, Integer> mine = new HashTableChaining<>();
        Map<Integer, Integer> ref = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 100_000; i++) {
            int k = rnd.nextInt(2000), v = rnd.nextInt();
            switch (rnd.nextInt(3)) {
                case 0 -> ok &= Objects.equals(mine.put(k, v), ref.put(k, v));
                case 1 -> ok &= Objects.equals(mine.get(k), ref.get(k));
                default -> ok &= Objects.equals(mine.remove(k), ref.remove(k));
            }
            ok &= mine.size() == ref.size();
        }
        System.out.println("\n===== 100,000 random ops vs java.util.HashMap: " + (ok ? "all match" : "MISMATCH") + " =====");
        if (!ok) System.exit(1);
    }
}
