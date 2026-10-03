/*
 * ============================================================================
 *  BLOOM FILTER: "definitely NOT in the set" or "PROBABLY in the set"
 * ============================================================================
 *
 *  A space-efficient PROBABILISTIC set:
 *    add(x)
 *    mightContain(x) -> false  : x was DEFINITELY never added   (no false negatives)
 *                    -> true   : x was PROBABLY added           (false positives possible)
 *  It stores NO elements - only bits. You can't list items or (in the basic version)
 *  delete them.
 *
 *  HOW
 *    a bit array of m bits, all 0, and k hash functions h1..hk mapping x to [0, m)
 *    add(x):          set bits h1(x), ..., hk(x) to 1
 *    mightContain(x): true iff ALL k of those bits are 1
 *  A false positive happens when other elements happened to set all k bits of x.
 *
 *      m = 16 bits, k = 3
 *      add "cat": bits 2, 7, 13      add "dog": bits 5, 7, 11
 *      query "cow": bits 2, 11, 13 -> all 1 -> "probably" - but it was never added!
 *
 *  THE MATH (n = items added)
 *    false positive rate  p ~ (1 - e^(-k n / m))^k
 *    best k = (m / n) * ln 2
 *    bits needed for a target p:  m = -n ln p / (ln 2)^2   ~ 9.6 bits per item for 1%
 *  Compare: a HashSet<String> of URLs uses hundreds of bits per item.
 *
 *  DOUBLE HASHING: k hash functions from TWO base hashes: g_i(x) = h1(x) + i * h2(x)
 *  (Kirsch & Mitzenmacher) - as good as k independent hashes in practice.
 *
 *  USED BY: databases (Cassandra, RocksDB, HBase: "is this key maybe in this file?"
 *  before an expensive disk read), web crawlers (seen this URL?), CDNs, Chrome's
 *  old malicious-URL check, spell checkers.
 *  COUNTING BLOOM FILTER: small counters instead of bits -> supports delete.
 *
 *  Run:  java BloomFilterBasics.java
 * ============================================================================
 */

import java.util.*;

public class BloomFilterBasics {

    static class BloomFilter {
        private final long[] bits;          // a bit array packed into longs
        private final int m, k;

        BloomFilter(int expectedItems, double falsePositiveRate) {
            m = (int) Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
            k = Math.max(1, (int) Math.round((double) m / expectedItems * Math.log(2)));
            bits = new long[(m + 63) / 64];
        }

        int bitCount() { return m; }
        int hashCount() { return k; }

        // two independent-ish base hashes from the string
        private static int h1(String s) { return s.hashCode(); }

        private static int h2(String s) {
            int h = 0x9747b28c;
            for (int i = 0; i < s.length(); i++) { h ^= s.charAt(i); h *= 0x5bd1e995; h ^= h >>> 15; }
            return h | 1;                   // odd, so the probe sequence doesn't collapse
        }

        private int index(String s, int i) {
            return Math.floorMod(h1(s) + i * h2(s), m);         // double hashing
        }

        void add(String s) {
            for (int i = 0; i < k; i++) { int b = index(s, i); bits[b >>> 6] |= 1L << (b & 63); }
        }

        boolean mightContain(String s) {
            for (int i = 0; i < k; i++) {
                int b = index(s, i);
                if ((bits[b >>> 6] & (1L << (b & 63))) == 0) return false;   // one 0 bit = definitely absent
            }
            return true;
        }
    }

    // Counting variant: 4-bit-ish counters (here a byte each) -> supports remove
    static class CountingBloomFilter {
        private final byte[] counters;
        private final int k;
        CountingBloomFilter(int m, int k) { counters = new byte[m]; this.k = k; }
        private int index(String s, int i) { return Math.floorMod(s.hashCode() + i * BloomFilter.h2(s), counters.length); }
        void add(String s) { for (int i = 0; i < k; i++) counters[index(s, i)]++; }
        void remove(String s) { if (mightContain(s)) for (int i = 0; i < k; i++) counters[index(s, i)]--; }
        boolean mightContain(String s) { for (int i = 0; i < k; i++) if (counters[index(s, i)] == 0) return false; return true; }
    }

    public static void main(String[] args) {
        System.out.println("1. Sizing for 100,000 items at 1% false positives");
        BloomFilter bf = new BloomFilter(100_000, 0.01);
        System.out.printf("   m = %,d bits (%.1f bits per item = %,d KB), k = %d hash functions%n",
                bf.bitCount(), bf.bitCount() / 100_000.0, bf.bitCount() / 8 / 1024, bf.hashCount());
        check("about 9.6 bits per item", true, Math.abs(bf.bitCount() / 100_000.0 - 9.6) < 0.1);
        check("k = 7", 7, bf.hashCount());

        System.out.println("2. No false negatives, ever");
        for (int i = 0; i < 100_000; i++) bf.add("user-" + i);
        boolean allFound = true;
        for (int i = 0; i < 100_000; i++) allFound &= bf.mightContain("user-" + i);
        check("every added item is reported present", true, allFound);

        System.out.println("3. Measured false positive rate");
        int fp = 0, trials = 200_000;
        for (int i = 0; i < trials; i++) if (bf.mightContain("stranger-" + i)) fp++;
        double rate = fp / (double) trials;
        System.out.printf("   %,d of %,d never-added items said \"maybe\" -> %.3f%% (target 1%%)%n", fp, trials, rate * 100);
        check("false positive rate close to 1%", true, rate > 0.005 && rate < 0.02);

        System.out.println("4. Overfilling breaks the guarantee");
        BloomFilter small = new BloomFilter(1_000, 0.01);
        for (int i = 0; i < 20_000; i++) small.add("x" + i);                // 20x more than designed for
        int fp2 = 0;
        for (int i = 0; i < 10_000; i++) if (small.mightContain("y" + i)) fp2++;
        System.out.printf("   designed for 1,000 items, given 20,000: false positive rate %.1f%%%n", fp2 / 100.0);
        check("rate explodes when overfilled", true, fp2 > 5_000);

        System.out.println("5. Counting Bloom filter supports delete");
        CountingBloomFilter cbf = new CountingBloomFilter(10_000, 5);
        cbf.add("apple");
        cbf.add("banana");
        check("apple maybe present", true, cbf.mightContain("apple"));
        cbf.remove("apple");
        check("apple gone after remove", false, cbf.mightContain("apple"));
        check("banana unaffected", true, cbf.mightContain("banana"));
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
