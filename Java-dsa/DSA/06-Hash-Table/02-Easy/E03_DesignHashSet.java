/*
 * ============================================================================
 *  EASY 03 : DESIGN HASHSET                  LeetCode #705
 *  https://leetcode.com/problems/design-hashset/
 *  Pattern: BUCKETS + CHAINING (no built-in hash tables allowed)
 * ============================================================================
 *
 *  PROBLEM
 *    Implement add(key), remove(key), contains(key) for int keys in [0, 10^6],
 *    without java.util hash collections.
 *
 *  ---------------------------------------------------------------------------
 *  OPTION A: boolean[1_000_001]  (a "direct address table")
 *    O(1) everything, ~1 MB of memory. Perfectly valid when keys are small
 *    integers! But it's not really a hash set, and it doesn't work for big or
 *    arbitrary keys.
 *
 *  OPTION B: A REAL HASH SET (what the interviewer wants to see)
 *    - an array of BUCKETS, each a small list (chaining)
 *    - bucket index = hash(key) % BUCKETS. Pick a PRIME bucket count so keys
 *      with patterns (all even, all multiples of 10...) still spread out
 *    - add: if the key isn't in its bucket's list -> append
 *    - remove / contains: search that one bucket's list
 *    With ~10^4 operations and 769 buckets, the chains stay short.
 *
 *  FOLLOW-UP QUESTIONS to mention: the load factor and resizing (01-Basics
 *  HashTableChaining), open addressing (01-Basics), keeping a bucket as a BST for
 *  a better worst case (what Java 8 does).
 *  COMPLEXITY   O(1) average (O(chain length)) per operation
 * ============================================================================
 */

import java.util.*;

public class E03_DesignHashSet {

    static class MyHashSet {
        private static final int BUCKETS = 769;                // a prime
        private final List<Integer>[] buckets;

        @SuppressWarnings("unchecked")
        MyHashSet() {
            buckets = new List[BUCKETS];
            for (int i = 0; i < BUCKETS; i++) buckets[i] = new LinkedList<>();
        }

        private List<Integer> bucket(int key) { return buckets[key % BUCKETS]; }

        public void add(int key) {
            List<Integer> b = bucket(key);
            if (!b.contains(key)) b.add(key);
        }

        public void remove(int key) {
            bucket(key).remove(Integer.valueOf(key));          // remove by VALUE, not by index!
        }

        public boolean contains(int key) {
            return bucket(key).contains(key);
        }
    }

    public static void main(String[] args) {
        MyHashSet s = new MyHashSet();
        s.add(1);
        s.add(2);
        check("contains(1)", true, s.contains(1));
        check("contains(3)", false, s.contains(3));
        s.add(2);
        check("contains(2) after a duplicate add", true, s.contains(2));
        s.remove(2);
        check("contains(2) after remove", false, s.contains(2));

        Random rnd = new Random(3);
        MyHashSet mine = new MyHashSet();
        Set<Integer> ref = new HashSet<>();
        boolean ok = true;
        for (int i = 0; i < 50_000; i++) {
            int k = rnd.nextInt(5000);
            switch (rnd.nextInt(3)) {
                case 0 -> { mine.add(k); ref.add(k); }
                case 1 -> { mine.remove(k); ref.remove(k); }
                default -> ok &= mine.contains(k) == ref.contains(k);
            }
        }
        check("50,000 random ops vs java.util.HashSet", true, ok);
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
