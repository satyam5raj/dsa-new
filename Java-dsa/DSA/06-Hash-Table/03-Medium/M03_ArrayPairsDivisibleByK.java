/*
 * ============================================================================
 *  MEDIUM 03 : CHECK IF ARRAY PAIRS ARE DIVISIBLE BY k   LeetCode #1497
 *  https://leetcode.com/problems/check-if-array-pairs-are-divisible-by-k/
 *  Pattern: COUNT REMAINDERS (hash by value mod k), then match complements
 * ============================================================================
 *
 *  PROBLEM
 *    arr has even length n. Can it be split into n/2 pairs where each pair's
 *    SUM is divisible by k?
 *      [1,2,3,4,5,10,6,7,8,9], k=5 -> true    ((1,9) (2,8) (3,7) (4,6) (5,10))
 *      [1,2,3,4,5,6], k=7          -> true    ((1,6) (2,5) (3,4))
 *      [1,2,3,4,5,6], k=10         -> false
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: only the REMAINDERS matter. (a + b) % k == 0  <=>  ra + rb is 0 or k,
 *  where ra = a % k. So a remainder r must pair with remainder k - r.
 *  (This is Two Sum's "complement" idea, applied to remainders.)
 *
 *  ALGORITHM
 *    count[r] = how many elements have remainder r
 *    NEGATIVE NUMBERS: in Java -3 % 5 == -3. Normalize with ((x % k) + k) % k.
 *    check:
 *      count[0] must be EVEN            (remainder-0 elements pair with each other)
 *      for r = 1 .. k-1: count[r] == count[k - r]
 *         (when k is even, r = k/2 pairs with itself, so it must be even: the
 *          condition count[r] == count[k-r] would be trivially true, so check it explicitly)
 *
 *  DRY RUN [1,2,3,4,5,10,6,7,8,9], k=5
 *    remainders: 1,2,3,4,0,0,1,2,3,4 -> count = [2,2,2,2,2]
 *    count[0] = 2 even; count[1] = count[4]; count[2] = count[3] -> true
 *
 *  COMPLEXITY   Time O(n + k)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class M03_ArrayPairsDivisibleByK {

    static boolean canArrange(int[] arr, int k) {
        int[] count = new int[k];
        for (int x : arr) count[((x % k) + k) % k]++;          // normalize negative remainders
        if (count[0] % 2 != 0) return false;
        for (int r = 1; r < k; r++) {
            if (2 * r == k) {
                if (count[r] % 2 != 0) return false;           // r pairs with itself
            } else if (count[r] != count[k - r]) {
                return false;
            }
        }
        return true;
    }

    // brute force: try to pair greedily with backtracking (small inputs)
    static boolean brute(int[] a, int k) {
        return pair(a, new boolean[a.length], k);
    }

    static boolean pair(int[] a, boolean[] used, int k) {
        int i = 0;
        while (i < a.length && used[i]) i++;
        if (i == a.length) return true;
        used[i] = true;
        for (int j = i + 1; j < a.length; j++) {
            if (!used[j] && Math.floorMod(a[i] + a[j], k) == 0) {
                used[j] = true;
                if (pair(a, used, k)) { used[i] = used[j] = false; return true; }
                used[j] = false;
            }
        }
        used[i] = false;
        return false;
    }

    public static void main(String[] args) {
        check("[1..10 mixed] k=5", true, canArrange(new int[]{1, 2, 3, 4, 5, 10, 6, 7, 8, 9}, 5));
        check("[1..6] k=7", true, canArrange(new int[]{1, 2, 3, 4, 5, 6}, 7));
        check("[1..6] k=10", false, canArrange(new int[]{1, 2, 3, 4, 5, 6}, 10));
        check("[-1,1,-2,2,-3,3,-4,4] k=3 (negatives)", true, canArrange(new int[]{-1, 1, -2, 2, -3, 3, -4, 4}, 3));
        check("[2,2] k=4 (r = k/2 pairs itself)", true, canArrange(new int[]{2, 2}, 4));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 2 * (1 + rnd.nextInt(4));
            int[] a = rnd.ints(n, -10, 11).toArray();
            int k = 1 + rnd.nextInt(6);
            ok &= canArrange(a, k) == brute(a, k);
        }
        check("500 random arrays vs backtracking", true, ok);
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
