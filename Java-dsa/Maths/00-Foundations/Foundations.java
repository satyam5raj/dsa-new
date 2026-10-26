/*
 * ============================================================================
 *  MATHS 00 : FOUNDATIONS
 * ============================================================================
 *  Run:  java Foundations.java          (explanations: README.md in this folder)
 *
 *  Every worked example in the README is computed and checked here:
 *    1. functions          composition, inverse
 *    2. exponents & logs   log rules, log2 = "how many halvings", bits of a number
 *    3. sums               arithmetic, geometric, harmonic (~ ln n), double sums
 *    4. floor / ceil / mod  and Java's surprising % on negative numbers
 *    5. logic              truth tables, De Morgan, implication, contrapositive
 *    6. proofs             induction checked numerically, binary-search loop invariant
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class Foundations {

    // =====================================================================
    // 1. functions
    // =====================================================================

    static void functions() {
        System.out.println("1. Functions");
        Function<Integer, Integer> f = x -> 2 * x + 3;          // f(x) = 2x + 3
        Function<Integer, Integer> g = x -> x * x;              // g(x) = x^2
        check("f(4)", 11, f.apply(4));
        check("g(f(4)) = (2*4+3)^2", 121, f.andThen(g).apply(4));   // first f, then g
        check("f(g(4)) = 2*16+3", 35, g.andThen(f).apply(4));       // order matters!
        Function<Double, Double> fInv = y -> (y - 3) / 2;        // undo f: y = 2x+3 -> x = (y-3)/2
        check("f^-1(f(4)) = 4", 4.0, fInv.apply((double) f.apply(4)));
    }

    // =====================================================================
    // 2. exponents and logarithms
    // =====================================================================

    static double log2(double x) { return Math.log(x) / Math.log(2); }     // change of base

    static int halvingsUntilOne(long n) { int steps = 0; while (n > 1) { n /= 2; steps++; } return steps; }

    static void exponentsAndLogs() {
        System.out.println("2. Exponents and logarithms");
        check("2^3 * 2^4 = 2^7", 128.0, Math.pow(2, 3) * Math.pow(2, 4));
        check("(2^3)^2 = 2^6", 64.0, Math.pow(Math.pow(2, 3), 2));
        check("2^-3 = 1/8", 0.125, Math.pow(2, -3));
        check("log2(1024)", 10.0, round(log2(1024), 9));
        check("log(a*b) = log a + log b", true, close(log2(8 * 32), log2(8) + log2(32)));
        check("log(a/b) = log a - log b", true, close(log2(64.0 / 4), log2(64) - log2(4)));
        check("log(a^k) = k log a", true, close(log2(Math.pow(5, 3)), 3 * log2(5)));
        check("halvings of 1,000,000 until 1", 19, halvingsUntilOne(1_000_000));
        check("floor(log2(1,000,000))", 19, (int) Math.floor(log2(1_000_000)));
        check("bits to write 1,000,000", 20, Long.toBinaryString(1_000_000).length());
        check("bits = floor(log2 n) + 1", 20, 64 - Long.numberOfLeadingZeros(1_000_000L));
        check("ln(e) = 1", 1.0, Math.log(Math.E));
        check("e^(ln 7) = 7", 7.0, round(Math.exp(Math.log(7)), 9));
        // product of many small probabilities underflows; sum of logs does not
        double product = 1, logSum = 0;
        for (int i = 0; i < 400; i++) { product *= 0.1; logSum += Math.log10(0.1); }
        check("0.1^400 underflows to 0.0", 0.0, product);
        check("but log10 of it is fine: -400", -400.0, round(logSum, 6));
    }

    // =====================================================================
    // 3. sums
    // =====================================================================

    static void sums() {
        System.out.println("3. Sums (sigma notation)");
        long s = 0;
        for (int i = 1; i <= 100; i++) s += i;
        check("1+2+...+100 (loop)", 5050L, s);
        check("n(n+1)/2 with n = 100", 5050L, 100L * 101 / 2);
        long sq = 0;
        for (int i = 1; i <= 10; i++) sq += (long) i * i;
        check("1^2+...+10^2 = n(n+1)(2n+1)/6", 385L, sq);
        check("formula", 385L, 10L * 11 * 21 / 6);
        long geo = 0;
        for (int i = 0; i < 10; i++) geo += 1L << i;
        check("1+2+4+...+2^9 = 2^10 - 1", 1023L, geo);
        double half = 0;
        for (int i = 1; i <= 50; i++) half += Math.pow(0.5, i);
        check("1/2 + 1/4 + 1/8 + ... -> 1", 1.0, round(half, 9));
        double h = 0;
        for (int i = 1; i <= 1_000_000; i++) h += 1.0 / i;
        check("harmonic H(10^6)", 14.392727, round(h, 6));
        check("ln(10^6) + 0.5772 (close)", true, Math.abs(h - (Math.log(1e6) + 0.5772156649)) < 1e-5);
        long pairs = 0;
        int n = 50;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) pairs++;     // nested loop "double sum"
        check("pairs i<j for n=50 = n(n-1)/2", 1225L, pairs);
    }

    // =====================================================================
    // 4. floor, ceil, mod
    // =====================================================================

    static void floorCeilMod() {
        System.out.println("4. Floor, ceiling, mod");
        check("floor(3.7)", 3.0, Math.floor(3.7));
        check("ceil(3.2)", 4.0, Math.ceil(3.2));
        check("floor(-3.7) = -4 (not -3!)", -4.0, Math.floor(-3.7));
        check("ceil(7/2) with integers: (7+2-1)/2", 4, (7 + 2 - 1) / 2);
        check("17 mod 5", 2, 17 % 5);
        check("Java: -7 % 3 = -1 (sign follows the dividend)", -1, -7 % 3);
        check("Math.floorMod(-7, 3) = 2 (the maths answer)", 2, Math.floorMod(-7, 3));
        check("Java: -7 / 3 = -2 (rounds toward zero)", -2, -7 / 3);
        check("Math.floorDiv(-7, 3) = -3", -3, Math.floorDiv(-7, 3));
        check("a = q*b + r always: -7 = -3*3 + 2", -7, Math.floorDiv(-7, 3) * 3 + Math.floorMod(-7, 3));
    }

    // =====================================================================
    // 5. logic
    // =====================================================================

    static boolean implies(boolean p, boolean q) { return !p || q; }

    static void logic() {
        System.out.println("5. Logic");
        StringBuilder table = new StringBuilder();
        for (boolean p : new boolean[]{true, false})
            for (boolean q : new boolean[]{true, false}) table.append(implies(p, q) ? 'T' : 'F');
        check("p -> q for TT, TF, FT, FF", "TFTT", table.toString());
        boolean deMorgan = true, contra = true, converseSame = true;
        for (boolean p : new boolean[]{true, false})
            for (boolean q : new boolean[]{true, false}) {
                deMorgan &= (!(p && q)) == (!p || !q) && (!(p || q)) == (!p && !q);
                contra &= implies(p, q) == implies(!q, !p);             // contrapositive: always equal
                converseSame &= implies(p, q) == implies(q, p);         // converse: NOT always equal
            }
        check("De Morgan holds for all 4 cases", true, deMorgan);
        check("p->q == (not q -> not p)", true, contra);
        check("p->q == q->p ? (converse)", false, converseSame);
        check("XOR: 5 ^ 3 = 6 (101 ^ 011 = 110)", 6, 5 ^ 3);
        // quantifiers over a finite set
        int[] xs = {2, 4, 6, 7};
        check("for all x: x even ?", false, Arrays.stream(xs).allMatch(x -> x % 2 == 0));
        check("exists x: x odd ?", true, Arrays.stream(xs).anyMatch(x -> x % 2 != 0));
        check("not(all even) == exists odd", true,
                !Arrays.stream(xs).allMatch(x -> x % 2 == 0) == Arrays.stream(xs).anyMatch(x -> x % 2 != 0));
    }

    // =====================================================================
    // 6. proofs (we can't prove with code, but we can test claims and invariants)
    // =====================================================================

    static int binarySearch(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            // INVARIANT: if target is in a, it is inside a[lo..hi]
            int mid = (lo + hi) >>> 1;
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1; else hi = mid - 1;
        }
        return -1;
    }

    static void proofs() {
        System.out.println("6. Proofs and invariants");
        boolean sumFormula = true, powerBound = true;
        long running = 0;
        for (int n = 1; n <= 10_000; n++) {
            running += n;
            sumFormula &= running == (long) n * (n + 1) / 2;
            if (n <= 60) powerBound &= (1L << n) > n;                  // 2^n > n
        }
        check("sum formula true for n = 1..10,000", true, sumFormula);
        check("2^n > n for n = 1..60", true, powerBound);
        // a famous claim that LOOKS true for small n but isn't: n^2 + n + 41 is prime
        int firstFail = -1;
        for (int n = 0; n < 100 && firstFail < 0; n++) if (!isPrime(n * n + n + 41)) firstFail = n;
        check("n^2+n+41 prime for n=0..39, fails at n =", 40, firstFail);
        int[] sorted = new int[1000];
        for (int i = 0; i < 1000; i++) sorted[i] = 3 * i;
        boolean ok = true;
        for (int t = -5; t < 3005; t++) ok &= (binarySearch(sorted, t) >= 0) == (t >= 0 && t % 3 == 0 && t < 3000);
        check("binary search correct on 3,010 targets", true, ok);
        check("sqrt(2) is not a fraction p/q with q <= 10,000", true, noFractionForSqrt2(10_000));
    }

    static boolean isPrime(int x) { if (x < 2) return false; for (int d = 2; (long) d * d <= x; d++) if (x % d == 0) return false; return true; }

    static boolean noFractionForSqrt2(int maxQ) {
        for (long q = 1; q <= maxQ; q++) { long p = Math.round(Math.sqrt(2) * q); if (p * p == 2 * q * q) return false; }
        return true;
    }

    public static void main(String[] args) {
        functions();
        exponentsAndLogs();
        sums();
        floorCeilMod();
        logic();
        proofs();
        summary();
    }

    static boolean close(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static double round(double x, int places) { double p = Math.pow(10, places); return Math.round(x * p) / p; }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-44s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
