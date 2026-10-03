/*
 * ============================================================================
 *  MEDIUM 07 : COUNT GOOD NUMBERS  ⭐ bonus    LeetCode #1922
 *  https://leetcode.com/problems/count-good-numbers/
 *  Pattern: counting -> a product of powers -> FAST MODULAR EXPONENTIATION (recursive)
 * ============================================================================
 *
 *  PROBLEM
 *    A digit string is "good" if digits at EVEN indices are even (0,2,4,6,8: 5 choices)
 *    and digits at ODD indices are prime (2,3,5,7: 4 choices). Count good strings of
 *    length n, modulo 1e9+7. n can be 10^15.
 *      n=1 -> 5,  n=4 -> 400 (5*4*5*4),  n=50 -> 564908303
 *
 *  ---------------------------------------------------------------------------
 *  COUNTING: positions are independent -> multiply the choices.
 *     even positions: ceil(n/2) of them, odd positions: floor(n/2)
 *     answer = 5^ceil(n/2) * 4^floor(n/2)   (mod 1e9+7)
 *
 *  THE PROBLEM: exponents up to 5 * 10^14 - multiplying one by one is impossible.
 *  FAST POWER (M01) with a modulus:
 *     pow(b, e) = pow(b, e/2)^2 (* b if e is odd), every multiplication taken mod M
 *  Products of two numbers < 1e9+7 fit in a long (< 1.0e18 < 9.2e18) - but take
 *  the modulus after EVERY multiplication, or it overflows.
 *
 *  This modular fast power shows up all over: hashing, combinatorics, cryptography (RSA).
 *
 *  COMPLEXITY   Time O(log n)   Space O(log n) recursion
 * ============================================================================
 */

import java.util.*;

public class M07_CountGoodNumbers {

    static final long MOD = 1_000_000_007L;

    static int countGoodNumbers(long n) {
        long evens = (n + 1) / 2, odds = n / 2;
        return (int) (modPow(5, evens) * modPow(4, odds) % MOD);
    }

    static long modPow(long base, long exp) {
        if (exp == 0) return 1;
        long half = modPow(base, exp / 2);
        long sq = half * half % MOD;                       // reduce after EVERY multiply
        return exp % 2 == 0 ? sq : sq * (base % MOD) % MOD;
    }

    // referee for small n: enumerate all digit strings
    static int brute(int n) {
        int count = 0;
        int[] digits = new int[n];
        while (true) {
            boolean good = true;
            for (int i = 0; i < n && good; i++) {
                int d = digits[i];
                good = i % 2 == 0 ? d % 2 == 0 : (d == 2 || d == 3 || d == 5 || d == 7);
            }
            if (good) count++;
            int i = n - 1;
            while (i >= 0 && digits[i] == 9) digits[i--] = 0;
            if (i < 0) break;
            digits[i]++;
        }
        return count;
    }

    public static void main(String[] args) {
        check("n=1", 5, countGoodNumbers(1));
        check("n=4", 400, countGoodNumbers(4));
        check("n=50", 564908303, countGoodNumbers(50));
        check("n=10^15 (instant)", true, countGoodNumbers(1_000_000_000_000_000L) >= 0);
        boolean ok = true;
        for (int n = 1; n <= 6; n++) ok &= countGoodNumbers(n) == brute(n);
        check("n = 1..6 vs enumerating all strings", true, ok);
        Random rnd = new Random(1);
        ok = true;
        for (int t = 0; t < 1000; t++) {
            long b = rnd.nextInt(1000), e = rnd.nextInt(60);
            ok &= modPow(b, e) == java.math.BigInteger.valueOf(b).modPow(java.math.BigInteger.valueOf(e), java.math.BigInteger.valueOf(MOD)).longValue();
        }
        check("1,000 random modPow vs BigInteger", true, ok);
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
