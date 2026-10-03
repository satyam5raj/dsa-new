/*
 * ============================================================================
 *  MEDIUM 06 : GRAY CODE                     LeetCode #89
 *  https://leetcode.com/problems/gray-code/
 *  Pattern: build the answer for n from the answer for n-1 ("REFLECT and prefix")
 * ============================================================================
 *
 *  PROBLEM
 *    An n-bit Gray code: all 2^n numbers, starting at 0, where consecutive numbers
 *    (and the last and first) differ in exactly ONE bit.
 *      n=2 -> [0,1,3,2]   (00, 01, 11, 10)
 *
 *  ---------------------------------------------------------------------------
 *  RECURSIVE CONSTRUCTION (reflected binary code):
 *    G(0) = [0]
 *    G(n) = G(n-1)  followed by  REVERSE(G(n-1)) with bit (n-1) set
 *       n=1: [0] + [1]                      -> [0, 1]
 *       n=2: [00, 01] + [11, 10]            -> [0, 1, 3, 2]
 *       n=3: [000,001,011,010] + [110,111,101,100]
 *  WHY it works:
 *    - inside each half, neighbours differ by one bit (G(n-1) already does)
 *    - at the seam, the last of the first half and the first of the reversed half are
 *      the SAME (n-1)-bit code, differing only in the new top bit
 *    - the very last (top bit + G(n-1)'s first = 100..0) differs from 0 in one bit
 *
 *  FORMULA (O(1) each): the i-th Gray code is i ^ (i >> 1).
 *
 *  (leetcode.md lists this under Backtracking; DFS over bit flips works too, but the
 *   recursive reflection is the cleanest.)
 *
 *  COMPLEXITY   Time O(2^n)   Space O(2^n) for the output
 * ============================================================================
 */

import java.util.*;

public class M06_GrayCode {

    static List<Integer> grayCode(int n) {
        if (n == 0) return new ArrayList<>(List.of(0));
        List<Integer> prev = grayCode(n - 1);
        List<Integer> result = new ArrayList<>(prev);
        for (int i = prev.size() - 1; i >= 0; i--) result.add(prev.get(i) | (1 << (n - 1)));   // reflect + set top bit
        return result;
    }

    static boolean valid(int n, List<Integer> g) {
        if (g.size() != 1 << n || g.get(0) != 0 || new HashSet<>(g).size() != g.size()) return false;
        for (int i = 0; i < g.size(); i++) {
            int a = g.get(i), b = g.get((i + 1) % g.size());
            if (a < 0 || a >= (1 << n)) return false;
            if (g.size() > 1 && Integer.bitCount(a ^ b) != 1) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        check("n=2", "[0, 1, 3, 2]", grayCode(2));
        check("n=1", "[0, 1]", grayCode(1));
        check("n=3", "[0, 1, 3, 2, 6, 7, 5, 4]", grayCode(3));
        boolean ok = true;
        for (int n = 1; n <= 16; n++) {
            List<Integer> g = grayCode(n);
            ok &= valid(n, g);
            for (int i = 0; i < g.size(); i++) ok &= g.get(i) == (i ^ (i >> 1));   // matches the formula
        }
        check("n = 1..16 valid and == i ^ (i >> 1)", true, ok);
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
