/*
 * ============================================================================
 *  MEDIUM 02 : COUNT PRIMES                  LeetCode #204
 *  https://leetcode.com/problems/count-primes/
 *  Pattern: SIEVE OF ERATOSTHENES (a boolean array used as a lookup table)
 * ============================================================================
 *
 *  PROBLEM
 *    Count the primes STRICTLY LESS than n.
 *      n = 10 -> 4   (2, 3, 5, 7)        n = 0 or 1 -> 0
 *
 *  ---------------------------------------------------------------------------
 *  (It's in leetcode.md's Hashing list because the sieve is a "direct address
 *   table": a boolean[] indexed by the number itself, the simplest hash table.)
 *
 *  BRUTE FORCE: test each number with trial division up to sqrt(x)
 *    -> O(n * sqrt(n)): about 3 * 10^9 steps for n = 5 * 10^6. Too slow.
 *
 *  SIEVE: instead of TESTING each number, CROSS OUT the multiples of each prime.
 *    composite = new boolean[n]
 *    for p = 2 while p*p < n:
 *      if p is not crossed out (so it's prime):
 *        cross out p*p, p*p + p, p*p + 2p, ...  (start at p*p: smaller multiples
 *                                                were already crossed out by smaller primes)
 *    count what's left (from 2 to n-1)
 *  Why stop at sqrt(n)? Every composite c < n has a prime factor <= sqrt(c),
 *  so it was already crossed out by then.
 *
 *  DRY RUN n = 20
 *    p=2: cross 4,6,8,10,12,14,16,18
 *    p=3: cross 9,15 (12 and 18 were already crossed)
 *    p=4 crossed, 5*5 = 25 >= 20 -> stop
 *    left: 2,3,5,7,11,13,17,19 -> 8
 *
 *  COMPLEXITY   Time O(n log log n) (practically linear)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M02_CountPrimes {

    static int countPrimes(int n) {
        if (n < 3) return 0;
        boolean[] composite = new boolean[n];
        for (int p = 2; (long) p * p < n; p++) {
            if (!composite[p]) {
                for (int m = p * p; m < n; m += p) composite[m] = true;
            }
        }
        int count = 0;
        for (int i = 2; i < n; i++) if (!composite[i]) count++;
        return count;
    }

    static boolean isPrime(int x) {
        if (x < 2) return false;
        for (int d = 2; (long) d * d <= x; d++) if (x % d == 0) return false;
        return true;
    }

    public static void main(String[] args) {
        check("n=10", 4, countPrimes(10));
        check("n=0", 0, countPrimes(0));
        check("n=1", 0, countPrimes(1));
        check("n=2 (strictly less)", 0, countPrimes(2));
        check("n=3", 1, countPrimes(3));
        boolean ok = true;
        int brute = 0;
        for (int n = 0; n <= 3000; n++) {
            if (n > 0 && isPrime(n - 1)) brute++;
            ok &= countPrimes(n) == brute;
        }
        check("n = 0..3000 vs trial division", true, ok);
        long t0 = System.nanoTime();
        int big = countPrimes(5_000_000);
        System.out.printf("   primes below 5,000,000: %,d in %d ms%n", big, (System.nanoTime() - t0) / 1_000_000);
        check("primes below 5,000,000", 348513, big);
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
