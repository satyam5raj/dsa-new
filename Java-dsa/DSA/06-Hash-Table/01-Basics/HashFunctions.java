/*
 * ============================================================================
 *  HASH TABLE 1.1 : HOW HASHING WORKS - hash functions, buckets, collisions
 * ============================================================================
 *
 *  Run:  java HashFunctions.java
 *
 *  THE GOAL: O(1) average insert / lookup / delete by KEY.
 *  An array gives O(1) access by INDEX. Hashing turns ANY key into an index:
 *
 *      key "apple" --hash function--> 93029210 --compress (mod size)--> bucket 2
 *
 *       bucket 0: []
 *       bucket 1: [("kiwi", 4)]
 *       bucket 2: [("apple", 7)] -> [("mango", 1)]     <- a COLLISION: 2 keys, same bucket
 *       bucket 3: []
 *
 *  THE 3 INGREDIENTS
 *    1. HASH FUNCTION  key -> integer. Must be DETERMINISTIC (same key -> same hash)
 *       and should SPREAD keys evenly.
 *    2. COMPRESSION    integer -> bucket index:  hash mod capacity
 *                      (or  hash & (capacity - 1)  when capacity is a power of 2)
 *    3. COLLISION HANDLING  different keys WILL land in the same bucket
 *       (pigeonhole principle: infinitely many keys, finitely many buckets)
 *         - SEPARATE CHAINING : each bucket holds a list   (HashTableChaining.java)
 *         - OPEN ADDRESSING   : probe for another empty slot (HashTableOpenAddressing.java)
 *
 *  LOAD FACTOR = entries / buckets. As it grows, collisions grow. Keep it bounded
 *  (Java's HashMap: 0.75) by RESIZING: double the buckets and REHASH every entry.
 *  This keeps operations O(1) on average, and amortized over the resizes.
 *
 *  WORST CASE O(n): if every key collides (a bad hash, or an attacker choosing
 *  keys), a bucket becomes a long list. Java 8's HashMap turns long buckets
 *  into red-black trees -> O(log n) worst case.
 */

import java.util.*;

public class HashFunctions {

    public static void main(String[] args) {
        stringHashCode();
        compression();
        goodVsBadHash();
        equalsHashCodeContract();
        hashSetVsTreeSet();
    }

    // =================================================================
    // 1. Java's String.hashCode: a POLYNOMIAL hash
    //      h = s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
    //    computed with Horner's rule: h = 31*h + c for each character.
    //    Why 31? An odd prime: few patterns collide, and 31*h == (h << 5) - h is cheap.
    //    The order of characters matters: "ab" and "ba" get different hashes.
    // =================================================================
    static int polynomialHash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);   // int overflow is fine: it just wraps
        return h;
    }

    static void stringHashCode() {
        System.out.println("===== 1. String.hashCode() is a polynomial hash =====");
        for (String s : new String[]{"a", "ab", "ba", "apple"}) {
            System.out.printf("   %-6s our hash = %11d   String.hashCode = %11d%n", s, polynomialHash(s), s.hashCode());
        }
        // A famous collision: different strings, same hash. Totally legal!
        System.out.println("   \"Aa\".hashCode() = " + "Aa".hashCode() + ", \"BB\".hashCode() = " + "BB".hashCode() + "  <- collision");
    }

    // =================================================================
    // 2. Compression: hash -> bucket index
    //    hash % n can be NEGATIVE in Java (hashes can be negative) -> use Math.floorMod,
    //    or mask off the sign bit:  (h & 0x7fffffff) % n
    //    Power-of-2 capacity: index = h & (n - 1) (keeps the low bits; very fast).
    //    Java's HashMap first mixes the high bits into the low ones: h ^ (h >>> 16).
    // =================================================================
    static void compression() {
        System.out.println("\n===== 2. COMPRESSION (hash -> bucket) =====");
        int h = "java hashing".hashCode();
        System.out.println("   hash = " + h + " (negative!)");
        System.out.println("   h % 16              = " + (h % 16) + "  <- a negative index: crash");
        System.out.println("   Math.floorMod(h,16) = " + Math.floorMod(h, 16));
        System.out.println("   h & 15              = " + (h & 15) + "  (power-of-2 capacity)");
        System.out.println("   (h ^ h>>>16) & 15   = " + ((h ^ (h >>> 16)) & 15) + "  (what HashMap does)");
    }

    // =================================================================
    // 3. A good hash spreads keys; a bad one piles them into a few buckets.
    //    Count how many keys land in the fullest bucket.
    // =================================================================
    static void goodVsBadHash() {
        System.out.println("\n===== 3. GOOD vs BAD HASH FUNCTIONS (10,000 words, 1,024 buckets) =====");
        List<String> words = new ArrayList<>();
        Random rnd = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0, n = 3 + rnd.nextInt(6); j < n; j++) sb.append((char) ('a' + rnd.nextInt(26)));
            words.add(sb.toString());
        }
        report("length of the word", words, String::length);
        report("sum of the characters", words, s -> s.chars().sum());
        report("polynomial (31)", words, HashFunctions::polynomialHash);
        System.out.println("   ideal: 10,000 / 1,024 ~ 10 per bucket. The bad hashes make huge chains -> O(n) lookups.");
        System.out.println("   (sum of chars also gives every ANAGRAM the same hash: \"listen\" and \"silent\" collide)");
    }

    static void report(String name, List<String> words, java.util.function.ToIntFunction<String> hash) {
        int[] buckets = new int[1024];
        for (String w : words) buckets[Math.floorMod(hash.applyAsInt(w), 1024)]++;
        int max = 0, used = 0;
        for (int b : buckets) { max = Math.max(max, b); if (b > 0) used++; }
        System.out.printf("   %-24s buckets used: %4d / 1024   biggest bucket: %5d keys%n", name, used, max);
    }

    // =================================================================
    // 4. The contract for keys (see OOP/07): equal objects MUST have equal hash codes.
    //    Break it and the map looks in the wrong bucket.
    // =================================================================
    record GoodKey(int x, int y) {}                                 // records generate equals + hashCode

    static class BadKey {                                           // equals only, no hashCode
        final int x, y;
        BadKey(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) { return o instanceof BadKey b && b.x == x && b.y == y; }
    }

    static void equalsHashCodeContract() {
        System.out.println("\n===== 4. KEYS NEED equals() AND hashCode() =====");
        Map<Object, String> m = new HashMap<>();
        m.put(new GoodKey(1, 2), "found");
        m.put(new BadKey(1, 2), "found");
        System.out.println("   lookup with an equal GoodKey -> " + m.get(new GoodKey(1, 2)));
        System.out.println("   lookup with an equal BadKey  -> " + m.get(new BadKey(1, 2)) + "  (wrong bucket!)");
        System.out.println("   tip: for grid problems, encode (r, c) as r * cols + c, or as a long, instead of a key object");
    }

    // =================================================================
    // 5. Hash-based vs tree-based sets/maps
    // =================================================================
    static void hashSetVsTreeSet() {
        System.out.println("\n===== 5. HashSet vs TreeSet =====");
        System.out.println("""
                   HashSet / HashMap     O(1) average   no order     needs hashCode/equals
                   LinkedHashSet / Map   O(1) average   insertion order (or access order -> LRU)
                   TreeSet / TreeMap     O(log n)       SORTED        needs compareTo; floor/ceiling/range queries
                   int[26] / boolean[]   O(1) always    when keys are small integers: the fastest "hash table" of all""");
    }
}
