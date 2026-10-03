/*
 * ============================================================================
 *  HARD 01 : LFU CACHE  ⭐ bonus              LeetCode #460
 *  https://leetcode.com/problems/lfu-cache/
 *  Pattern: frequency -> LinkedHashSet of keys (LRU order inside each bucket) + minFreq
 * ============================================================================
 *
 *  PROBLEM
 *    get(key) and put(key, value) in O(1). When full, evict the key with the LOWEST
 *    use count; ties -> the LEAST RECENTLY used among them. (get and put both count
 *    as a use; a new key starts at count 1.)
 *      cap 2: put(1,1) put(2,2) get(1)=1 put(3,3)[evicts 2: count 1 vs key1's 2]
 *             get(2)=-1 get(3)=3 put(4,4)[1 and 3 both count 2 -> evict 1, used longer ago]
 *             get(1)=-1 get(3)=3 get(4)=4
 *
 *  ---------------------------------------------------------------------------
 *  A heap ordered by (count, lastUsed) would be O(log n). For O(1):
 *    values : key -> value
 *    counts : key -> its use count
 *    buckets: count -> LinkedHashSet of keys with that count, in USE order
 *             (insertion order of a LinkedHashSet = least recent first)
 *    minFreq: the smallest count that currently has keys
 *
 *  touch(key) (on every get/put of an existing key):
 *    c = counts[key]; remove key from buckets[c]; add it to buckets[c + 1]
 *    if buckets[c] is now empty and c == minFreq: minFreq++
 *  evict: the FIRST key of buckets[minFreq] (lowest count, least recent)
 *  insert new key: count 1, add to buckets[1], minFreq = 1  (a new key is always the minimum)
 *
 *  Why is "minFreq++" enough after a touch? The touched key moved from c to c+1,
 *  so if bucket c emptied, c+1 is now non-empty and is the new minimum.
 *
 *  COMPLEXITY   O(1) per operation   Space O(capacity)
 * ============================================================================
 */

import java.util.*;

public class H01_LFUCache {

    static class LFUCache {
        private final int capacity;
        private final Map<Integer, Integer> values = new HashMap<>(), counts = new HashMap<>();
        private final Map<Integer, LinkedHashSet<Integer>> buckets = new HashMap<>();
        private int minFreq = 0;

        public LFUCache(int capacity) { this.capacity = capacity; }

        public int get(int key) {
            if (!values.containsKey(key)) return -1;
            touch(key);
            return values.get(key);
        }

        public void put(int key, int value) {
            if (capacity == 0) return;
            if (values.containsKey(key)) {
                values.put(key, value);
                touch(key);
                return;
            }
            if (values.size() == capacity) {
                int victim = buckets.get(minFreq).iterator().next();      // lowest count, least recent
                buckets.get(minFreq).remove(victim);
                values.remove(victim);
                counts.remove(victim);
            }
            values.put(key, value);
            counts.put(key, 1);
            buckets.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
            minFreq = 1;
        }

        private void touch(int key) {
            int c = counts.get(key);
            counts.put(key, c + 1);
            buckets.get(c).remove(key);
            if (c == minFreq && buckets.get(c).isEmpty()) minFreq++;
            buckets.computeIfAbsent(c + 1, k -> new LinkedHashSet<>()).add(key);   // most recent in its new bucket
        }
    }

    // referee: O(n) eviction by scanning (count, lastUsed)
    static class Brute {
        final int cap;
        final Map<Integer, int[]> m = new HashMap<>();      // key -> {value, count, lastUsed}
        int clock = 0;
        Brute(int cap) { this.cap = cap; }
        int get(int k) { int[] e = m.get(k); if (e == null) return -1; e[1]++; e[2] = clock++; return e[0]; }
        void put(int k, int v) {
            if (cap == 0) return;
            int[] e = m.get(k);
            if (e != null) { e[0] = v; e[1]++; e[2] = clock++; return; }
            if (m.size() == cap) {
                int victim = -1;
                for (Map.Entry<Integer, int[]> x : m.entrySet()) {
                    if (victim == -1) { victim = x.getKey(); continue; }
                    int[] a = x.getValue(), b = m.get(victim);
                    if (a[1] < b[1] || (a[1] == b[1] && a[2] < b[2])) victim = x.getKey();
                }
                m.remove(victim);
            }
            m.put(k, new int[]{v, 1, clock++});
        }
    }

    public static void main(String[] args) {
        LFUCache c = new LFUCache(2);
        StringBuilder out = new StringBuilder();
        c.put(1, 1);
        c.put(2, 2);
        out.append(c.get(1)).append(' ');
        c.put(3, 3);
        out.append(c.get(2)).append(' ').append(c.get(3)).append(' ');
        c.put(4, 4);
        out.append(c.get(1)).append(' ').append(c.get(3)).append(' ').append(c.get(4));
        check("example", "1 -1 3 -1 3 4", out.toString());
        LFUCache zero = new LFUCache(0);
        zero.put(0, 0);
        check("capacity 0", -1, zero.get(0));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 100; t++) {
            int cap = rnd.nextInt(6);
            LFUCache mine = new LFUCache(cap);
            Brute ref = new Brute(cap);
            for (int i = 0; i < 2000; i++) {
                int k = rnd.nextInt(10);
                if (rnd.nextBoolean()) { mine.put(k, i); ref.put(k, i); }
                else ok &= mine.get(k) == ref.get(k);
            }
        }
        check("200,000 random ops vs scanning eviction", true, ok);
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
