/*
 * ============================================================================
 *  HARD 05 : MINIMUM COST TO HIRE K WORKERS   LeetCode #857
 *  https://leetcode.com/problems/minimum-cost-to-hire-k-workers/
 *  Pattern: SORT by ratio + MAX-HEAP of qualities (keep the k smallest sum)
 * ============================================================================
 *
 *  PROBLEM
 *    Worker i has quality[i] and minimum wage[i]. Hire exactly k workers so that:
 *      - pay is PROPORTIONAL to quality within the group
 *      - everyone gets at least their minimum wage
 *    Minimum total cost?
 *      quality [10,20,5], wage [70,50,30], k=2 -> 105.0  (workers 0 and 2: 70 + 35)
 *
 *  ---------------------------------------------------------------------------
 *  THE MATH
 *    Proportional pay means one RATE r (money per unit of quality) for the group.
 *    Worker i is satisfied iff r * quality[i] >= wage[i]  <=>  r >= wage[i] / quality[i].
 *    So r must be >= the largest "ratio" in the group, and paying more is pointless:
 *        r = max ratio in the group,   cost = r * (sum of qualities in the group)
 *
 *  SEARCH: sort workers by ratio. Fix the "captain" = the worker with the max ratio
 *  in the group; everyone else must come from EARLIER in the sorted order (smaller
 *  ratio). For that captain, the cost is ratio_captain * (quality sum), so pick the
 *  k workers (captain included) with the SMALLEST qualities among those seen so far.
 *    -> sweep in ratio order, keep the k smallest qualities in a MAX-heap of size k
 *       (top-k smallest, like K Closest Points), with their running sum.
 *       Whenever the heap holds k: cost = ratio_current * sum.
 *  (When the current worker gets popped right away, the cost computed with its
 *   ratio is just an overestimate of a group already considered - harmless.)
 *
 *  DRY RUN quality [10,20,5], wage [70,50,30], k=2
 *    ratios: w0 7.0, w1 2.5, w2 6.0 -> order w1, w2, w0
 *    w1: heap {20} sum 20
 *    w2: heap {20,5} sum 25 -> cost 6.0 * 25 = 150
 *    w0: push 10 -> {20,10,5} -> pop 20 -> sum 15 -> cost 7.0 * 15 = 105  <- best
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H05_MinimumCostToHireKWorkers {

    static double mincostToHireWorkers(int[] quality, int[] wage, int k) {
        int n = quality.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> (double) wage[i] / quality[i]));   // by ratio
        PriorityQueue<Integer> maxQ = new PriorityQueue<>(Comparator.reverseOrder());       // qualities kept
        long sum = 0;
        double best = Double.MAX_VALUE;
        for (int i : order) {
            maxQ.offer(quality[i]);
            sum += quality[i];
            if (maxQ.size() > k) sum -= maxQ.poll();          // drop the largest quality
            if (maxQ.size() == k) best = Math.min(best, sum * ((double) wage[i] / quality[i]));
        }
        return best;
    }

    // brute force: every group of k workers
    static double brute(int[] q, int[] w, int k) {
        int n = q.length;
        double best = Double.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) != k) continue;
            double rate = 0;
            long sum = 0;
            for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) { rate = Math.max(rate, (double) w[i] / q[i]); sum += q[i]; }
            best = Math.min(best, rate * sum);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[10,20,5] [70,50,30] k=2", "105.00000", String.format("%.5f", mincostToHireWorkers(new int[]{10, 20, 5}, new int[]{70, 50, 30}, 2)));
        check("[3,1,10,10,1] [4,8,2,2,7] k=3", "30.66667", String.format("%.5f", mincostToHireWorkers(new int[]{3, 1, 10, 10, 1}, new int[]{4, 8, 2, 2, 7}, 3)));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(9), k = 1 + rnd.nextInt(n);
            int[] q = rnd.ints(n, 1, 20).toArray(), w = rnd.ints(n, 1, 50).toArray();
            ok &= Math.abs(mincostToHireWorkers(q, w, k) - brute(q, w, k)) < 1e-6;
        }
        check("1,000 random cases vs all groups", true, ok);
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
