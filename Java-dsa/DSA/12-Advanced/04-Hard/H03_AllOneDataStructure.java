/*
 * ============================================================================
 *  HARD 03 : ALL O`ONE DATA STRUCTURE  ⭐ bonus   LeetCode #432
 *  https://leetcode.com/problems/all-oone-data-structure/
 *  Pattern: doubly linked list of COUNT BUCKETS (sorted by count) + key -> bucket map
 * ============================================================================
 *
 *  PROBLEM
 *    inc(key): count + 1 (new keys start at 1); dec(key): count - 1 (remove at 0);
 *    getMaxKey() / getMinKey(): ANY key with the max / min count ("" if empty).
 *    ALL operations O(1).
 *
 *  ---------------------------------------------------------------------------
 *  A heap gives max OR min in O(1) but updates are O(log n). A TreeMap is O(log n).
 *
 *  BUCKET LIST (the same shape as LFU's frequency buckets, but as a linked list):
 *    head <-> [count 1: {a, b}] <-> [count 3: {c}] <-> [count 7: {d}] <-> tail
 *    buckets are sorted by count and only exist when non-empty
 *    map: key -> its bucket
 *  inc(key):
 *    the key moves from bucket c to bucket c+1 - which is either the NEXT bucket
 *    (if its count is c+1) or a NEW bucket inserted right after bucket c. O(1).
 *    remove the key from bucket c; delete bucket c if it became empty.
 *  dec(key): mirror image (bucket c-1 is the previous bucket or a new one before it;
 *    count 0 means drop the key entirely).
 *  getMaxKey = any key of tail.prev; getMinKey = any key of head.next.
 *  Counts only ever change by 1, which is why the neighbouring bucket is always
 *  the right place - no searching.
 *
 *  COMPLEXITY   O(1) per operation   Space O(number of keys)
 * ============================================================================
 */

import java.util.*;

public class H03_AllOneDataStructure {

    static class AllOne {
        private static class Bucket {
            int count;
            Set<String> keys = new LinkedHashSet<>();
            Bucket prev, next;
            Bucket(int count) { this.count = count; }
        }

        private final Bucket head = new Bucket(0), tail = new Bucket(Integer.MAX_VALUE);
        private final Map<String, Bucket> where = new HashMap<>();

        public AllOne() { head.next = tail; tail.prev = head; }

        private Bucket insertAfter(Bucket b, int count) {
            Bucket n = new Bucket(count);
            n.prev = b; n.next = b.next;
            b.next.prev = n; b.next = n;
            return n;
        }

        private void removeIfEmpty(Bucket b) {
            if (b != head && b != tail && b.keys.isEmpty()) { b.prev.next = b.next; b.next.prev = b.prev; }
        }

        public void inc(String key) {
            Bucket cur = where.getOrDefault(key, head);                  // head acts as "count 0"
            Bucket target = cur.next.count == cur.count + 1 ? cur.next : insertAfter(cur, cur.count + 1);
            target.keys.add(key);
            where.put(key, target);
            if (cur != head) { cur.keys.remove(key); removeIfEmpty(cur); }
        }

        public void dec(String key) {
            Bucket cur = where.get(key);
            if (cur == null) return;
            if (cur.count == 1) {
                where.remove(key);
            } else {
                Bucket target = cur.prev.count == cur.count - 1 ? cur.prev : insertAfter(cur.prev, cur.count - 1);
                target.keys.add(key);
                where.put(key, target);
            }
            cur.keys.remove(key);
            removeIfEmpty(cur);
        }

        public String getMaxKey() { return tail.prev == head ? "" : tail.prev.keys.iterator().next(); }
        public String getMinKey() { return head.next == tail ? "" : head.next.keys.iterator().next(); }
    }

    public static void main(String[] args) {
        AllOne a = new AllOne();
        a.inc("hello");
        a.inc("hello");
        check("max", "hello", a.getMaxKey());
        check("min", "hello", a.getMinKey());
        a.inc("leet");
        check("max after leet", "hello", a.getMaxKey());
        check("min after leet", "leet", a.getMinKey());
        a.dec("hello"); a.dec("hello");
        check("hello removed: max", "leet", a.getMaxKey());
        AllOne empty = new AllOne();
        check("empty max", "", empty.getMaxKey());

        Random rnd = new Random(3);
        AllOne mine = new AllOne();
        Map<String, Integer> ref = new HashMap<>();
        boolean ok = true;
        for (int i = 0; i < 100_000; i++) {
            String k = "k" + rnd.nextInt(15);
            if (rnd.nextInt(3) > 0) { mine.inc(k); ref.merge(k, 1, Integer::sum); }
            else if (ref.containsKey(k)) { mine.dec(k); if (ref.merge(k, -1, Integer::sum) == 0) ref.remove(k); }
            if (ref.isEmpty()) { ok &= mine.getMaxKey().isEmpty() && mine.getMinKey().isEmpty(); continue; }
            int max = Collections.max(ref.values()), min = Collections.min(ref.values());
            ok &= ref.get(mine.getMaxKey()) == max && ref.get(mine.getMinKey()) == min;   // ANY key with that count is fine
        }
        check("100,000 random ops vs a count map", true, ok);
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
