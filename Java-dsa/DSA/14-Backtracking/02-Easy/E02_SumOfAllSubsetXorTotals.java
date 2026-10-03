/*
 * ============================================================================
 *  EASY 02 : SUM OF ALL SUBSET XOR TOTALS  ⭐ bonus   LeetCode #1863
 *  https://leetcode.com/problems/sum-of-all-subset-xor-totals/
 *  Pattern: PICK / NOT PICK over every element, carrying a running value
 * ============================================================================
 *
 *  PROBLEM
 *    Sum of (XOR of the subset) over ALL subsets (the empty subset XORs to 0).
 *      [1,3] -> 6        subsets: [] 0, [1] 1, [3] 3, [1,3] 2  -> 6
 *
 *  ---------------------------------------------------------------------------
 *  BACKTRACKING (pick / not pick, from 13-Recursion/RecursionPatterns):
 *     dfs(i, xorSoFar):
 *       if i == n: return xorSoFar                       (one complete subset)
 *       return dfs(i+1, xorSoFar ^ a[i])  +  dfs(i+1, xorSoFar)
 *  No list needed: we only carry the XOR - the "path" is just a number, so there's
 *  nothing to undo (passing a new value down IS the undo).
 *  2^n leaves -> fine for n <= 12.
 *
 *  O(n) TRICK: a bit set in ANY element is set in exactly half of all subset XORs.
 *     answer = (OR of all elements) * 2^(n-1)
 *
 *  COMPLEXITY   O(2^n) backtracking  /  O(n) with the trick
 * ============================================================================
 */

import java.util.*;

public class E02_SumOfAllSubsetXorTotals {

    static int subsetXORSum(int[] nums) { return dfs(nums, 0, 0); }

    static int dfs(int[] a, int i, int xor) {
        if (i == a.length) return xor;
        return dfs(a, i + 1, xor ^ a[i]) + dfs(a, i + 1, xor);   // pick + not pick
    }

    static int trick(int[] a) {
        int or = 0;
        for (int x : a) or |= x;
        return or << (a.length - 1);
    }

    public static void main(String[] args) {
        check("[1,3]", 6, subsetXORSum(new int[]{1, 3}));
        check("[5,1,6]", 28, subsetXORSum(new int[]{5, 1, 6}));
        check("[3,4,5,6,7,8]", 480, subsetXORSum(new int[]{3, 4, 5, 6, 7, 8}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(12), 1, 21).toArray();
            int brute = 0;
            for (int mask = 0; mask < (1 << a.length); mask++) { int x = 0; for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) x ^= a[i]; brute += x; }
            ok &= subsetXORSum(a) == brute && trick(a) == brute;
        }
        check("500 random arrays: dfs == bitmask == trick", true, ok);
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
