/*
 * ============================================================================
 *  MEDIUM 05 : CONTINUOUS SUBARRAY SUM  ⭐ bonus   LeetCode #523
 *  https://leetcode.com/problems/continuous-subarray-sum/
 *  Pattern: PREFIX SUM MOD k + HASHMAP OF THE FIRST INDEX
 * ============================================================================
 *
 *  PROBLEM
 *    Is there a subarray of LENGTH >= 2 whose sum is a multiple of k?
 *      [23,2,4,6,7], k=6  -> true    ([2,4] sums to 6)
 *      [23,2,6,4,7], k=13 -> false
 *      [5,0,0,0], k=3     -> true    ([0,0] sums to 0, a multiple of 3)
 *
 *  ---------------------------------------------------------------------------
 *  Builds on Subarray Sum Equals K (Arrays/M07), with a modulo twist.
 *  sum(i+1 .. j) = P[j] - P[i]. It's a multiple of k  <=>  P[j] % k == P[i] % k.
 *  -> "have I seen this same REMAINDER before, at least 2 positions back?"
 *
 *  HASHMAP  remainder -> the FIRST index where it appeared
 *    Why the FIRST index? We want the LONGEST possible gap, to satisfy length >= 2.
 *    So never overwrite an entry.
 *    Seed: {0: -1} (the empty prefix before index 0 has remainder 0).
 *    for j:
 *      r = prefix % k
 *      if r was seen at index i and j - i >= 2 -> true
 *      if r is new -> map[r] = j
 *
 *  DRY RUN [23,2,4,6,7], k=6     map {0:-1}
 *    j0 prefix 23 -> r 5 -> new: {5:0}
 *    j1 prefix 25 -> r 1 -> new: {1:1}
 *    j2 prefix 29 -> r 5 -> seen at 0, gap 2 >= 2 -> true   (subarray [2,4])
 *
 *  SIBLING TECHNIQUE: "longest subarray with sum == k" stores the FIRST index of
 *  each prefix sum (see M07 Contiguous Array next).
 *  COMPLEXITY   Time O(n)   Space O(min(n, k))
 * ============================================================================
 */

import java.util.*;

public class M05_ContinuousSubarraySum {

    static boolean checkSubarraySum(int[] nums, int k) {
        Map<Integer, Integer> firstIndex = new HashMap<>();
        firstIndex.put(0, -1);
        int prefix = 0;
        for (int j = 0; j < nums.length; j++) {
            prefix = (prefix + nums[j]) % k;                 // keep it small: only the remainder matters
            Integer i = firstIndex.get(prefix);
            if (i != null) {
                if (j - i >= 2) return true;
            } else {
                firstIndex.put(prefix, j);                   // only the FIRST occurrence is stored
            }
        }
        return false;
    }

    static boolean brute(int[] a, int k) {
        for (int i = 0; i < a.length; i++) {
            long s = a[i];
            for (int j = i + 1; j < a.length; j++) {
                s += a[j];
                if (s % k == 0) return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        check("[23,2,4,6,7] k=6", true, checkSubarraySum(new int[]{23, 2, 4, 6, 7}, 6));
        check("[23,2,6,4,7] k=6 (whole array 42)", true, checkSubarraySum(new int[]{23, 2, 6, 4, 7}, 6));
        check("[23,2,6,4,7] k=13", false, checkSubarraySum(new int[]{23, 2, 6, 4, 7}, 13));
        check("[5,0,0,0] k=3", true, checkSubarraySum(new int[]{5, 0, 0, 0}, 3));
        check("[6] k=6 (length 1 doesn't count)", false, checkSubarraySum(new int[]{6}, 6));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(10), 0, 10).toArray();
            int k = 1 + rnd.nextInt(10);
            ok &= checkSubarraySum(a, k) == brute(a, k);
        }
        check("1,000 random cases vs brute force", true, ok);
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
