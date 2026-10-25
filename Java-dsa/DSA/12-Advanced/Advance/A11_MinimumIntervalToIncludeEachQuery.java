/*
 * ============================================================================
 *  ADVANCE 11 : MINIMUM INTERVAL TO INCLUDE EACH QUERY  ⭐ bonus   LeetCode #1851
 *  https://leetcode.com/problems/minimum-interval-to-include-each-query/
 *  Pattern: OFFLINE queries (sort them) + sorted intervals + MIN-HEAP by size
 * ============================================================================
 *
 *  PROBLEM
 *    intervals[i] = [left, right] (inclusive, size = right - left + 1). For each query q,
 *    return the size of the SMALLEST interval with left <= q <= right, or -1.
 *      intervals [[1,4],[2,4],[3,6],[4,4]], queries [2,3,4,5]  ->  [3,3,1,4]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each query scan every interval: O(n * q) - too slow for 10^5 each.
 *
 *  OFFLINE TRICK: we're allowed to answer queries in ANY order as long as we put each
 *  answer back in its original slot. Answer them from the smallest q to the largest:
 *    - intervals sorted by left: as q grows, more intervals START covering it -> push
 *      each one into the heap exactly once (a pointer, never goes back)
 *    - min-heap by (size, right): the smallest candidate is on top
 *    - an interval with right < q can never cover this or any LATER (bigger) query ->
 *      pop it for good
 *  After that, the heap's top (if any) is the answer.
 *
 *  DRY RUN   sorted intervals [1,4] [2,4] [3,6] [4,4]
 *    q=2: push [1,4](4) [2,4](3)           top size 3, right 4 >= 2      -> 3
 *    q=3: push [3,6](4)                    top size 3                    -> 3
 *    q=4: push [4,4](1)                    top size 1                    -> 1
 *    q=5: heap (size,right) = (1,4) (3,4) (4,4) (4,6)
 *         (1,4) right 4 < 5 pop;  (3,4) pop;  (4,4) pop;  top (4,6) covers 5 -> 4
 *
 *  COMPLEXITY   O((n + q) log n + q log q)   Space O(n + q)
 * ============================================================================
 */

import java.util.*;

public class A11_MinimumIntervalToIncludeEachQuery {

    // ---------------- Approach 1: scan all intervals per query (the referee) ----------------
    static int[] minIntervalBrute(int[][] intervals, int[] queries) {
        int[] ans = new int[queries.length];
        for (int j = 0; j < queries.length; j++) {
            int best = -1;
            for (int[] iv : intervals)
                if (iv[0] <= queries[j] && queries[j] <= iv[1]) {
                    int size = iv[1] - iv[0] + 1;
                    if (best == -1 || size < best) best = size;
                }
            ans[j] = best;
        }
        return ans;
    }

    // ---------------- Approach 2: offline sweep + min-heap (main solution) ----------------
    static int[] minInterval(int[][] intervals, int[] queries) {
        int[][] ivs = intervals.clone();
        Arrays.sort(ivs, Comparator.comparingInt(iv -> iv[0]));        // by left end
        Integer[] order = new Integer[queries.length];
        for (int j = 0; j < queries.length; j++) order[j] = j;
        Arrays.sort(order, Comparator.comparingInt(j -> queries[j]));   // query indices, smallest q first

        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));   // {size, right}
        int[] ans = new int[queries.length];
        int i = 0;
        for (int j : order) {
            int q = queries[j];
            while (i < ivs.length && ivs[i][0] <= q) {                   // intervals that have started
                heap.add(new int[]{ivs[i][1] - ivs[i][0] + 1, ivs[i][1]});
                i++;
            }
            while (!heap.isEmpty() && heap.peek()[1] < q) heap.poll();   // ended before q: useless forever
            ans[j] = heap.isEmpty() ? -1 : heap.peek()[0];
        }
        return ans;
    }

    public static void main(String[] args) {
        check("example 1", "[3, 3, 1, 4]", minInterval(new int[][]{{1, 4}, {2, 4}, {3, 6}, {4, 4}}, new int[]{2, 3, 4, 5}));
        check("example 2 (with a gap)", "[2, -1, 4, 6]", minInterval(new int[][]{{2, 3}, {2, 5}, {1, 8}, {20, 25}}, new int[]{2, 19, 5, 22}));
        check("duplicate queries keep their slots", "[1, -1, 1]", minInterval(new int[][]{{5, 5}}, new int[]{5, 9, 5}));

        Random rnd = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 1 + rnd.nextInt(10), q = 1 + rnd.nextInt(10);
            int[][] ivs = new int[n][2];
            for (int[] iv : ivs) { iv[0] = 1 + rnd.nextInt(20); iv[1] = iv[0] + rnd.nextInt(8); }
            int[] qs = new int[q];
            for (int j = 0; j < q; j++) qs[j] = 1 + rnd.nextInt(30);
            ok &= Arrays.equals(minInterval(ivs, qs), minIntervalBrute(ivs, qs));
        }
        check("2,000 random cases vs scanning", true, ok);
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
