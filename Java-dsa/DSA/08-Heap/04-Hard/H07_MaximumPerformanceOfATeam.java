/*
 * ============================================================================
 *  HARD 07 : MAXIMUM PERFORMANCE OF A TEAM   LeetCode #1383
 *  https://leetcode.com/problems/maximum-performance-of-a-team/
 *  Pattern: SORT by efficiency desc + MIN-HEAP of speeds (keep the k fastest)
 * ============================================================================
 *
 *  PROBLEM
 *    Engineer i has speed[i] and efficiency[i]. Pick AT MOST k engineers.
 *    performance = (sum of their speeds) * (MIN efficiency among them).
 *    Return the max performance modulo 1e9+7.
 *      n=6, speed [2,10,3,1,5,8], efficiency [5,4,3,9,7,2], k=2 -> 60
 *        engineers 1 and 4: (10 + 5) * min(4, 7) = 60
 *
 *  ---------------------------------------------------------------------------
 *  Same shape as Hire K Workers (H05): one factor is a MIN/MAX over the group,
 *  the other is a SUM. Fix the bottleneck, optimize the sum.
 *
 *  Fix the engineer with the MIN efficiency in the team (the "bottleneck"). All
 *  others must have efficiency >= it. Sort by efficiency DESCENDING; when we reach
 *  engineer i, everyone before it qualifies. Best team with i as bottleneck:
 *      i + the (k-1) FASTEST engineers seen before it
 *  -> keep the k largest speeds in a MIN-heap of size k (top-k), with a running sum.
 *     After adding i: if size > k, pop the slowest. Candidate = sum * efficiency[i].
 *  (If i itself gets popped, the candidate uses a too-low efficiency for that
 *   group - an underestimate, never a wrong maximum.)
 *
 *  MODULO TRAP: compare the REAL products (they fit in a long: 10^5 * 10^5 * 10^8
 *  = 10^18 < 9.2 * 10^18); take % only at the very end. Taking % early would make
 *  comparisons meaningless.
 *
 *  DRY RUN k=2, sorted by eff desc: (1,9) (5,7) (2,5) (10,4) (3,3) (8,2)
 *    (1,9): sum 1  -> 9
 *    (5,7): sum 6  -> 42
 *    (2,5): {1,5,2} pop 1 -> sum 7 -> 35
 *    (10,4): {2,5,10} pop 2 -> sum 15 -> 60  <- best
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H07_MaximumPerformanceOfATeam {

    static int maxPerformance(int n, int[] speed, int[] efficiency, int k) {
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(efficiency[b], efficiency[a]));   // efficiency desc
        PriorityQueue<Integer> speeds = new PriorityQueue<>();                          // min-heap: k fastest
        long sum = 0, best = 0;
        for (int i : order) {
            speeds.offer(speed[i]);
            sum += speed[i];
            if (speeds.size() > k) sum -= speeds.poll();       // drop the slowest
            best = Math.max(best, sum * efficiency[i]);        // i is the bottleneck
        }
        return (int) (best % 1_000_000_007L);                  // modulo only at the end
    }

    static long brute(int n, int[] s, int[] e, int k) {
        long best = 0;
        for (int mask = 1; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) > k) continue;
            long sum = 0;
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) { sum += s[i]; min = Math.min(min, e[i]); }
            best = Math.max(best, sum * min);
        }
        return best;
    }

    public static void main(String[] args) {
        int[] s = {2, 10, 3, 1, 5, 8}, e = {5, 4, 3, 9, 7, 2};
        check("k=2", 60, maxPerformance(6, s, e, 2));
        check("k=3", 68, maxPerformance(6, s, e, 3));
        check("k=4", 72, maxPerformance(6, s, e, 4));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(10), k = 1 + rnd.nextInt(n);
            int[] sp = rnd.ints(n, 1, 30).toArray(), ef = rnd.ints(n, 1, 30).toArray();
            ok &= maxPerformance(n, sp, ef, k) == brute(n, sp, ef, k);
        }
        check("1,000 random teams vs all subsets", true, ok);
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
