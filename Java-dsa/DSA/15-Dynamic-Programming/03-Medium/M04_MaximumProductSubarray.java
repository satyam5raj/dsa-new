/*
 * ============================================================================
 *  MEDIUM 04 : MAXIMUM PRODUCT SUBARRAY      LeetCode #152
 *  https://leetcode.com/problems/maximum-product-subarray/
 *  Pattern: Kadane with TWO states - track the max AND the min ending here
 * ============================================================================
 *
 *  PROBLEM
 *    Largest product of a contiguous non-empty subarray.
 *      [2,3,-2,4]     -> 6   ([2,3])
 *      [-2,0,-1]      -> 0
 *      [-2,3,-4]      -> 24  (the whole array: two negatives cancel)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every subarray, running product -> O(n^2)
 *
 *  WHY PLAIN KADANE FAILS (Arrays M01 Maximum Subarray): with sums, a bad prefix is just
 *  dropped. With products a NEGATIVE number flips signs: the SMALLEST (most negative)
 *  product so far can become the LARGEST after multiplying by a negative.
 *
 *  KEY INSIGHT: keep two DP states for subarrays ENDING at i:
 *      hi[i] = max product ending at i,   lo[i] = min product ending at i
 *      candidates: x (start fresh), hi[i-1]*x, lo[i-1]*x
 *      hi[i] = max of the three,  lo[i] = min of the three
 *  Starting fresh also handles 0 (it "resets" both states).
 *
 *  DRY RUN [-2,3,-4]
 *    -2: hi=-2 lo=-2 best=-2
 *     3: cands 3, -6, -6       -> hi=3  lo=-6    best=3
 *    -4: cands -4, -12, 24     -> hi=24 lo=-12   best=24
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M04_MaximumProductSubarray {

    static int maxProduct(int[] nums) {
        int hi = nums[0], lo = nums[0], best = nums[0];
        for (int i = 1; i < nums.length; i++) {
            int x = nums[i];
            int a = hi * x, b = lo * x;                        // compute from the OLD hi/lo before overwriting
            hi = Math.max(x, Math.max(a, b));
            lo = Math.min(x, Math.min(a, b));
            best = Math.max(best, hi);
        }
        return best;
    }

    static int brute(int[] a) {
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            int p = 1;
            for (int j = i; j < a.length; j++) { p *= a[j]; best = Math.max(best, p); }
        }
        return best;
    }

    public static void main(String[] args) {
        check("[2,3,-2,4]", 6, maxProduct(new int[]{2, 3, -2, 4}));
        check("[-2,0,-1]", 0, maxProduct(new int[]{-2, 0, -1}));
        check("[-2,3,-4]", 24, maxProduct(new int[]{-2, 3, -4}));
        check("[-2]", -2, maxProduct(new int[]{-2}));
        check("[0,2]", 2, maxProduct(new int[]{0, 2}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = new int[1 + rnd.nextInt(12)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(9) - 4;
            ok &= maxProduct(a) == brute(a);
        }
        check("1000 random (with 0s, negatives)", true, ok);
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
