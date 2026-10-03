/*
 * ============================================================================
 *  MEDIUM 08 : UGLY NUMBER II                LeetCode #264
 *  https://leetcode.com/problems/ugly-number-ii/
 *  Pattern: GENERATE IN ORDER with a min-heap (+ a set against duplicates)
 *           -> then the 3-POINTER merge (a k-way merge without a heap)
 * ============================================================================
 *
 *  PROBLEM
 *    Ugly numbers have no prime factors other than 2, 3, 5: 1, 2, 3, 4, 5, 6, 8, 9, 10, 12...
 *    Return the n-th one (1 is the first).      n=10 -> 12
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: test 1, 2, 3, ... for ugliness. The 1690th is 2,123,366,400:
 *  far too many numbers to test.
 *
 *  KEY INSIGHT: every ugly number (except 1) is 2x, 3x or 5x of a SMALLER ugly
 *  number. So GENERATE them instead of testing.
 *
 *  APPROACH 1: MIN-HEAP
 *    heap = {1}; repeat n times: x = poll; push 2x, 3x, 5x (skip ones already seen:
 *    6 = 2*3 = 3*2 would appear twice). The i-th poll is the i-th ugly number.
 *    O(n log n). Use long: 5x overflows int near the end.
 *
 *  APPROACH 2: THREE POINTERS, O(n)
 *    ugly[0] = 1. Three "streams": 2*ugly[i2], 3*ugly[i3], 5*ugly[i5].
 *    Each is sorted, so this is a 3-WAY MERGE. Take the smallest head:
 *        next = min(2*ugly[i2], 3*ugly[i3], 5*ugly[i5])
 *    and advance EVERY pointer that produced `next` (that's how duplicates like
 *    6 = 2*3 = 3*2 are skipped: both pointers move).
 *
 *  DRY RUN pointers (ugly = [1])
 *    candidates 2,3,5 -> 2 (i2++)      ugly [1,2]
 *    4,3,5 -> 3 (i3++)                 [1,2,3]
 *    4,6,5 -> 4 (i2++)                 [1,2,3,4]
 *    6,6,5 -> 5 (i5++)                 [1,2,3,4,5]
 *    6,6,10 -> 6 (i2++ AND i3++)       [1,2,3,4,5,6]
 *
 *  COMPLEXITY   heap O(n log n) / pointers O(n);  Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M08_UglyNumberII {

    static int nthUglyNumberHeap(int n) {
        PriorityQueue<Long> heap = new PriorityQueue<>();
        Set<Long> seen = new HashSet<>();
        heap.add(1L);
        seen.add(1L);
        long x = 1;
        for (int i = 0; i < n; i++) {
            x = heap.poll();                                      // the i-th ugly number
            for (long f : new long[]{2, 3, 5}) if (seen.add(x * f)) heap.add(x * f);
        }
        return (int) x;
    }

    static int nthUglyNumber(int n) {
        int[] ugly = new int[n];
        ugly[0] = 1;
        int i2 = 0, i3 = 0, i5 = 0;
        for (int i = 1; i < n; i++) {
            int a = 2 * ugly[i2], b = 3 * ugly[i3], c = 5 * ugly[i5];
            ugly[i] = Math.min(a, Math.min(b, c));
            if (ugly[i] == a) i2++;                               // advance ALL that match
            if (ugly[i] == b) i3++;
            if (ugly[i] == c) i5++;
        }
        return ugly[n - 1];
    }

    static boolean isUgly(long x) {
        for (int p : new int[]{2, 3, 5}) while (x % p == 0) x /= p;
        return x == 1;
    }

    public static void main(String[] args) {
        check("n=10", 12, nthUglyNumber(10));
        check("n=1", 1, nthUglyNumber(1));
        check("n=1690 (the max)", 2123366400, nthUglyNumber(1690));
        check("heap version n=1690", 2123366400, nthUglyNumberHeap(1690));
        boolean ok = true;
        int found = 0;
        for (long x = 1; found < 300; x++) {
            if (isUgly(x)) {
                found++;
                ok &= nthUglyNumber(found) == x && nthUglyNumberHeap(found) == x;
            }
        }
        check("first 300 vs testing every number", true, ok);
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
