/*
 * ============================================================================
 *  MEDIUM 04 : 4SUM II  ⭐ bonus   LeetCode #454
 *  https://leetcode.com/problems/4sum-ii/
 *  Pattern: MEET IN THE MIDDLE (hash the pair sums of one half)
 * ============================================================================
 *
 *  PROBLEM
 *    Four arrays A, B, C, D of length n. Count the tuples (i, j, k, l) with
 *    A[i] + B[j] + C[k] + D[l] == 0.
 *      A=[1,2], B=[-2,-1], C=[-1,2], D=[0,2] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: 4 nested loops -> O(n^4). n = 200 -> 1.6 billion. No.
 *
 *  MEET IN THE MIDDLE
 *    Split into two halves:  (A[i] + B[j])  +  (C[k] + D[l])  == 0
 *    1. count every sum a + b in a HashMap:  sum -> how many (i, j) pairs give it   O(n^2)
 *    2. for every c + d, the other half must be -(c + d): add map[-(c + d)]           O(n^2)
 *    O(n^4) -> O(n^2): this is Two Sum, where each "number" is a pair sum.
 *
 *  DRY RUN A=[1,2], B=[-2,-1], C=[-1,2], D=[0,2]
 *    A+B sums: 1-2=-1, 1-1=0, 2-2=0, 2-1=1 -> {-1:1, 0:2, 1:1}
 *    C+D: -1+0=-1 -> need 1 (count 1);  -1+2=1 -> need -1 (1);  2+0=2 -> need -2 (0);  2+2=4 -> need -4 (0)
 *    total 2
 *
 *  MEET IN THE MIDDLE is a powerful general idea: split the work in half, enumerate
 *  each half, and join the halves with a hash lookup (also used for subset-sum
 *  with n ~ 40: 2^20 + 2^20 instead of 2^40).
 *  COMPLEXITY   Time O(n^2)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M04_FourSumII {

    static int fourSumCount(int[] a, int[] b, int[] c, int[] d) {
        Map<Integer, Integer> pairSums = new HashMap<>();
        for (int x : a) for (int y : b) pairSums.merge(x + y, 1, Integer::sum);
        int count = 0;
        for (int x : c) for (int y : d) count += pairSums.getOrDefault(-(x + y), 0);
        return count;
    }

    static int brute(int[] a, int[] b, int[] c, int[] d) {
        int n = 0;
        for (int w : a) for (int x : b) for (int y : c) for (int z : d) if (w + x + y + z == 0) n++;
        return n;
    }

    public static void main(String[] args) {
        check("example", 2, fourSumCount(new int[]{1, 2}, new int[]{-2, -1}, new int[]{-1, 2}, new int[]{0, 2}));
        check("all zeros n=1", 1, fourSumCount(new int[]{0}, new int[]{0}, new int[]{0}, new int[]{0}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int n = 1 + rnd.nextInt(8);
            int[] a = rnd.ints(n, -5, 6).toArray(), b = rnd.ints(n, -5, 6).toArray(),
                    c = rnd.ints(n, -5, 6).toArray(), d = rnd.ints(n, -5, 6).toArray();
            ok &= fourSumCount(a, b, c, d) == brute(a, b, c, d);
        }
        check("200 random cases vs O(n^4)", true, ok);
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
