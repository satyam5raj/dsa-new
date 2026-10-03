/*
 * ============================================================================
 *  HARD 03 : NUMBER OF DIGIT ONE  ⭐ bonus     LeetCode #233
 *  https://leetcode.com/problems/number-of-digit-one/
 *  Pattern: recursion on the LEADING DIGIT (split n into "full blocks" + a remainder)
 * ============================================================================
 *
 *  PROBLEM
 *    Count how many times the digit 1 appears in all numbers from 0 to n.
 *      n=13 -> 6   (1, 10, 11 (two), 12, 13)
 *    n up to 10^9 - counting number by number is too slow.
 *
 *  ---------------------------------------------------------------------------
 *  Write n = a * p + r, where p is the highest power of 10 <= n, a is the leading
 *  digit and r the remainder.   e.g. n = 3456: p = 1000, a = 3, r = 456
 *  Let f(n) = ones in 0..n. Split 0..n into:
 *    a full blocks [0, p), [p, 2p), ..., [(a-1)p, ap): the LOWER digits run through
 *       every value 0..p-1 in each block -> a * f(p - 1) ones
 *    the leading-digit 1s:
 *       if a > 1: the whole block [p, 2p) has a leading 1 -> p ones
 *       if a == 1: only [p, n] has one -> r + 1 ones
 *    the final partial block [ap, n]: its lower digits run 0..r -> f(r)
 *  So
 *     f(n) = a * f(p - 1) + (a == 1 ? r + 1 : p) + f(r)
 *     f(0) = 0
 *  f(p - 1) for p = 10^d has a closed form d * 10^(d-1) (each of d positions is 1 in a
 *  tenth of the numbers), but the recursion handles it too.
 *
 *  DRY RUN n = 13: p = 10, a = 1, r = 3 -> f(9) + (3 + 1) + f(3) = 1 + 4 + 1 = 6
 *
 *  (The other classic solution counts per digit position with the "high / cur / low"
 *   split - also O(log n).)
 *  COMPLEXITY   Time O(log^2 n) at worst   Space O(log n)
 * ============================================================================
 */

import java.util.*;

public class H03_NumberOfDigitOne {

    static int countDigitOne(int n) { return (int) f(n); }

    static long f(long n) {
        if (n <= 0) return 0;
        if (n < 10) return 1;                         // only the number 1
        long p = 1;
        while (p * 10 <= n) p *= 10;                  // highest power of 10 <= n
        long a = n / p, r = n % p;
        long leading = a == 1 ? r + 1 : p;            // numbers whose LEADING digit is 1
        return a * f(p - 1) + leading + f(r);
    }

    // per-position formula as a referee
    static long perPosition(long n) {
        long count = 0;
        for (long m = 1; m <= n; m *= 10) {
            long high = n / (m * 10), cur = n / m % 10, low = n % m;
            if (cur == 0) count += high * m;
            else if (cur == 1) count += high * m + low + 1;
            else count += (high + 1) * m;
        }
        return count;
    }

    static long brute(int n) {
        long c = 0;
        for (int i = 1; i <= n; i++) for (int x = i; x > 0; x /= 10) if (x % 10 == 1) c++;
        return c;
    }

    public static void main(String[] args) {
        check("n=13", 6, countDigitOne(13));
        check("n=0", 0, countDigitOne(0));
        check("n=100", 21, countDigitOne(100));
        check("n=10^9 (instant)", 900000001, countDigitOne(1_000_000_000));
        boolean ok = true;
        long running = 0;
        for (int n = 1; n <= 20_000; n++) {
            for (int x = n; x > 0; x /= 10) if (x % 10 == 1) running++;
            ok &= f(n) == running;
        }
        check("n = 1..20,000 vs counting digits", true, ok);
        Random rnd = new Random(1);
        ok = true;
        for (int t = 0; t < 10_000; t++) { int n = rnd.nextInt(Integer.MAX_VALUE); ok &= f(n) == perPosition(n); }
        check("10,000 big random n vs per-position formula", true, ok);
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
