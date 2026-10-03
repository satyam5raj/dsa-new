/*
 * ============================================================================
 *  MEDIUM 09 : SUPER UGLY NUMBER             LeetCode #313
 *  https://leetcode.com/problems/super-ugly-number/
 *  Pattern: Ugly Number II with k primes -> K-WAY MERGE with a HEAP of the k pointers
 * ============================================================================
 *
 *  PROBLEM
 *    Super ugly numbers only have prime factors from `primes` (sorted, up to 100 primes).
 *    Return the n-th (1 is the first).
 *      n=12, primes [2,7,13,19] -> 32
 *      sequence: 1,2,4,7,8,13,14,16,19,26,28,32
 *
 *  ---------------------------------------------------------------------------
 *  M08 had 3 pointers and took min of 3 candidates each step. With k primes:
 *
 *  APPROACH 1: k pointers, scan all k every step         O(n * k)
 *     next = min over j of primes[j] * ugly[idx[j]]; advance every j that matches.
 *
 *  APPROACH 2: HEAP of the k streams                     O(n log k)
 *     heap entries {value, prime, idx}: stream j's current candidate.
 *     Each step: the heap's min is the next ugly number (if it's a DUPLICATE of
 *     the last one written, just skip it). Then advance THAT stream:
 *       push {prime * ugly[idx + 1], prime, idx + 1}
 *     This is exactly the k-way merge of Merge k Sorted Lists, where "list j" is
 *     primes[j] * (the ugly sequence itself).
 *
 *  DRY RUN primes [2,7], first steps (ugly = [1])
 *    heap {2(2,0), 7(7,0)} -> 2: ugly [1,2], push 2*ugly[1]=4
 *    heap {4, 7} -> 4: [1,2,4], push 2*ugly[2]=8
 *    heap {7, 8} -> 7: [1,2,4,7], push 7*ugly[1]=14 ...
 *
 *  Use long for the candidates: prime * ugly can exceed int even though the
 *  answer fits.
 *  COMPLEXITY   Time O(n log k)   Space O(n + k)
 * ============================================================================
 */

import java.util.*;

public class M09_SuperUglyNumber {

    static int nthSuperUglyNumber(int n, int[] primes) {
        long[] ugly = new long[n];
        ugly[0] = 1;
        PriorityQueue<long[]> heap = new PriorityQueue<>(Comparator.comparingLong(e -> e[0]));   // {value, prime, idx}
        for (int p : primes) heap.offer(new long[]{p, p, 0});
        int i = 1;
        while (i < n) {
            long[] e = heap.poll();
            if (e[0] != ugly[i - 1]) {                                    // skip duplicates
                ugly[i++] = e[0];
                if (i == n) break;                                        // done: don't advance past the end
            }
            int idx = (int) e[2] + 1;                                     // < i: always already computed
            heap.offer(new long[]{e[1] * ugly[idx], e[1], idx});         // advance this stream
        }
        return (int) ugly[n - 1];
    }

    static int pointers(int n, int[] primes) {          // O(n * k) version
        long[] ugly = new long[n];
        ugly[0] = 1;
        int[] idx = new int[primes.length];
        for (int i = 1; i < n; i++) {
            long next = Long.MAX_VALUE;
            for (int j = 0; j < primes.length; j++) next = Math.min(next, primes[j] * ugly[idx[j]]);
            ugly[i] = next;
            for (int j = 0; j < primes.length; j++) if (primes[j] * ugly[idx[j]] == next) idx[j]++;
        }
        return (int) ugly[n - 1];
    }

    public static void main(String[] args) {
        check("n=12 [2,7,13,19]", 32, nthSuperUglyNumber(12, new int[]{2, 7, 13, 19}));
        check("n=1 [2,3,5]", 1, nthSuperUglyNumber(1, new int[]{2, 3, 5}));
        check("n=10 [2,3,5] = Ugly II", 12, nthSuperUglyNumber(10, new int[]{2, 3, 5}));
        int[] allPrimes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53};
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            // always include 2, 3, 5 so the n-th number stays within int (as LeetCode guarantees);
            // with only big primes like {53}, 53^1999 would overflow even a long
            List<Integer> ps = new ArrayList<>(List.of(2, 3, 5));
            for (int p : allPrimes) if (p > 5 && rnd.nextInt(3) == 0) ps.add(p);
            int[] primes = ps.stream().mapToInt(Integer::intValue).toArray();
            int n = 1 + rnd.nextInt(1500);
            ok &= nthSuperUglyNumber(n, primes) == pointers(n, primes);
        }
        check("200 random prime sets: heap == pointers", true, ok);
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
