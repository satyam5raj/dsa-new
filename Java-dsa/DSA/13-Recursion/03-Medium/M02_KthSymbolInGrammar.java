/*
 * ============================================================================
 *  MEDIUM 02 : K-TH SYMBOL IN GRAMMAR  ⭐ bonus   LeetCode #779
 *  https://leetcode.com/problems/k-th-symbol-in-grammar/
 *  Pattern: recurse on the PARENT, never build the huge row
 * ============================================================================
 *
 *  PROBLEM
 *    Row 1 is "0". Each next row replaces 0 -> "01" and 1 -> "10".
 *      row 1: 0
 *      row 2: 01
 *      row 3: 0110
 *      row 4: 01101001
 *    Return the k-th symbol (1-indexed) of row n.   n=4, k=6 -> 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: build row n. It has 2^(n-1) symbols; n = 30 -> 500 million. No.
 *
 *  KEY INSIGHT: every symbol in row n is produced by ONE parent in row n-1:
 *    symbols k = 1,2 come from parent 1; k = 3,4 from parent 2; ...
 *       parent position = (k + 1) / 2
 *    a parent 0 makes "01", a parent 1 makes "10":
 *       odd k  (the left child)  = the SAME as the parent
 *       even k (the right child) = the OPPOSITE of the parent
 *  So:  f(n, k) = f(n-1, (k+1)/2)             if k is odd
 *               = 1 - f(n-1, (k+1)/2)         if k is even
 *       f(1, 1) = 0
 *  Like walking up a binary tree from a leaf to the root: n levels.
 *
 *  BIT VIEW (O(1)): the answer = parity of the number of 1 bits in (k - 1).
 *
 *  DRY RUN n=4, k=6: even -> 1 - f(3, 3); f(3,3): odd -> f(2, 2); f(2,2): even -> 1 - f(1,1) = 1
 *          f(3,3) = 1 -> f(4,6) = 0
 *
 *  COMPLEXITY   Time O(n)   Space O(n) recursion
 * ============================================================================
 */

import java.util.*;

public class M02_KthSymbolInGrammar {

    static int kthGrammar(int n, int k) {
        if (n == 1) return 0;
        int parent = kthGrammar(n - 1, (k + 1) / 2);
        return k % 2 == 1 ? parent : 1 - parent;
    }

    static int bitTrick(int n, int k) { return Integer.bitCount(k - 1) % 2; }

    static String buildRow(int n) {
        String row = "0";
        for (int i = 1; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            for (char c : row.toCharArray()) sb.append(c == '0' ? "01" : "10");
            row = sb.toString();
        }
        return row;
    }

    public static void main(String[] args) {
        check("n=1 k=1", 0, kthGrammar(1, 1));
        check("n=2 k=2", 1, kthGrammar(2, 2));
        check("n=4 k=6", 0, kthGrammar(4, 6));
        check("n=30 k=2^29 (no row built)", bitTrick(30, 1 << 29), kthGrammar(30, 1 << 29));
        boolean ok = true;
        for (int n = 1; n <= 14; n++) {
            String row = buildRow(n);
            for (int k = 1; k <= row.length(); k++) ok &= kthGrammar(n, k) == row.charAt(k - 1) - '0' && bitTrick(n, k) == kthGrammar(n, k);
        }
        check("rows 1..14 built and compared", true, ok);
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
