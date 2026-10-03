/*
 * ============================================================================
 *  EASY 03 : POWER OF TWO  ⭐ bonus            LeetCode #231 (also #326 Power of Three)
 *  https://leetcode.com/problems/power-of-two/
 *  Pattern: recursion that DIVIDES the input (logarithmic depth) - and the O(1) bit trick
 * ============================================================================
 *
 *  PROBLEM
 *    Is n a power of two?   1 true, 16 true, 3 false, 0 false, -16 false
 *
 *  ---------------------------------------------------------------------------
 *  RECURSIVE DEFINITION
 *    n == 1             -> true   (2^0)
 *    n <= 0 or n odd    -> false
 *    otherwise          -> isPowerOfTwo(n / 2)
 *  Each call halves n -> depth O(log n): no risk of stack overflow.
 *  The same shape works for any base (Power of Three: divide by 3).
 *
 *  BIT TRICK (O(1)): a power of two has exactly ONE bit set (1000...0).
 *  n - 1 flips that bit and sets all lower bits (0111...1), so
 *      n > 0 && (n & (n - 1)) == 0
 *
 *  DRY RUN 16 -> 8 -> 4 -> 2 -> 1 true;   12 -> 6 -> 3 (odd) false
 *
 *  COMPLEXITY   recursive O(log n) time and stack;  bit trick O(1)
 * ============================================================================
 */

import java.util.*;

public class E03_PowerOfTwo {

    static boolean isPowerOfTwo(int n) {
        if (n == 1) return true;
        if (n <= 0 || n % 2 != 0) return false;
        return isPowerOfTwo(n / 2);
    }

    static boolean isPowerOfThree(int n) {
        if (n == 1) return true;
        if (n <= 0 || n % 3 != 0) return false;
        return isPowerOfThree(n / 3);
    }

    static boolean bitTrick(int n) { return n > 0 && (n & (n - 1)) == 0; }

    public static void main(String[] args) {
        check("1", true, isPowerOfTwo(1));
        check("16", true, isPowerOfTwo(16));
        check("3", false, isPowerOfTwo(3));
        check("0", false, isPowerOfTwo(0));
        check("-16", false, isPowerOfTwo(-16));
        check("2^30", true, isPowerOfTwo(1 << 30));
        check("power of three 27", true, isPowerOfThree(27));
        check("power of three 45", false, isPowerOfThree(45));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 100_000; t++) {
            int n = rnd.nextInt(3) == 0 ? 1 << rnd.nextInt(31) : rnd.nextInt();
            ok &= isPowerOfTwo(n) == bitTrick(n);
        }
        check("100,000 random ints: recursion == bit trick", true, ok);
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
