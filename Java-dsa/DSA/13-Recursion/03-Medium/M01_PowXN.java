/*
 * ============================================================================
 *  MEDIUM 01 : POW(X, N)  ⭐ bonus             LeetCode #50
 *  https://leetcode.com/problems/powx-n/
 *  Pattern: DIVIDE AND CONQUER - halve the exponent (fast exponentiation)
 * ============================================================================
 *
 *  PROBLEM
 *    x^n for a double x and an int n (possibly negative).
 *      2.0^10 = 1024.0     2.1^3 = 9.261     2.0^-2 = 0.25
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: multiply n times -> O(n). n = 2^31 - 1 -> two billion multiplications.
 *
 *  KEY IDENTITY:  x^n = (x^(n/2))^2           if n is even
 *                 x^n = (x^(n/2))^2 * x       if n is odd
 *  Compute x^(n/2) ONCE and square it -> the exponent halves each call -> O(log n).
 *  (Calling pow(x, n/2) TWICE instead of storing it would be T(n) = 2T(n/2) -> O(n)
 *   again - a classic mistake.)
 *
 *  NEGATIVE n:  x^-n = 1 / x^n.
 *  THE OVERFLOW TRAP: n = Integer.MIN_VALUE -> -n overflows (still negative!).
 *  Convert to long before negating.
 *
 *  DRY RUN 2^10: 2^5 -> 2^2 -> 2^1 -> 2^0 = 1;  2^1 = 1*1*2 = 2;  2^2 = 4;
 *          2^5 = 4*4*2 = 32;  2^10 = 32*32 = 1024     (4 levels instead of 10 multiplications)
 *
 *  COMPLEXITY   Time O(log n)   Space O(log n) recursion
 * ============================================================================
 */

import java.util.*;

public class M01_PowXN {

    static double myPow(double x, int n) {
        long N = n;                                   // long: -Integer.MIN_VALUE fits
        if (N < 0) { x = 1 / x; N = -N; }
        return pow(x, N);
    }

    static double pow(double x, long n) {
        if (n == 0) return 1.0;
        double half = pow(x, n / 2);                  // computed ONCE
        return n % 2 == 0 ? half * half : half * half * x;
    }

    public static void main(String[] args) {
        check("2^10", 1024.0, myPow(2.0, 10));
        check("2.1^3", "9.26100", String.format("%.5f", myPow(2.1, 3)));
        check("2^-2", 0.25, myPow(2.0, -2));
        check("1^MIN_VALUE (overflow trap)", 1.0, myPow(1.0, Integer.MIN_VALUE));
        check("2^MIN_VALUE -> 0", 0.0, myPow(2.0, Integer.MIN_VALUE));
        check("-2^3", -8.0, myPow(-2.0, 3));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 10_000; t++) {
            double x = rnd.nextDouble() * 4 - 2;
            int n = rnd.nextInt(41) - 20;
            double exp = Math.pow(x, n), got = myPow(x, n);
            ok &= Math.abs(got - exp) <= 1e-9 * Math.max(1, Math.abs(exp));
        }
        check("10,000 random cases vs Math.pow", true, ok);
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
