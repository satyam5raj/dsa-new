/*
 * ============================================================================
 *  ADVANCE 25 : MAX SUM OF RECTANGLE NO LARGER THAN K  ⭐ bonus   LeetCode #363
 *  https://leetcode.com/problems/max-sum-of-rectangle-no-larger-than-k/
 *  Pattern: fix two rows -> 1-D array; PREFIX SUMS + TreeSet.ceiling for "best sum <= k"
 * ============================================================================
 *
 *  PROBLEM
 *    Return the largest sum of any rectangle in the matrix that is NO LARGER THAN k.
 *    (A rectangle with sum <= k always exists.)
 *      [[1, 0, 1],
 *       [0,-2, 3]], k = 2   ->  2      the rectangle [[0,1],[-2,3]] sums to 2
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1 - 2-D TO 1-D: pick a top row and a bottom row. Add up each column between
 *  them -> one array colSum[]. Every rectangle spanning exactly those rows is a
 *  SUBARRAY of colSum. So: for all O(m^2) row pairs, solve the 1-D problem.
 *  (Adding one row at a time to colSum makes each pair O(n) to build.)
 *
 *  STEP 2 - 1-D: max subarray sum <= k. Kadane doesn't work (it can't respect "<= k").
 *  With prefix sums, a subarray sum is  cur - p  for an earlier prefix p. We want
 *      cur - p <= k   ->   p >= cur - k
 *  and to MAXIMISE cur - p we want the SMALLEST such p:  TreeSet.ceiling(cur - k).
 *  Insert each prefix after using it. O(n log n).
 *
 *  BRUTE FORCE: a 2-D prefix sum gives any rectangle in O(1); try all O(m^2 n^2).
 *
 *  DRY RUN rows 0..1 of the example: colSum = [1, -2, 4], k = 2
 *    prefixes {0}
 *    cur 1:  ceiling(1-2 = -1) = 0 -> sum 1        add 1   {0, 1}
 *    cur -1: ceiling(-3) = 0      -> sum -1        add -1  {-1, 0, 1}
 *    cur 3:  ceiling(1) = 1       -> sum 2  <- best (cols 1..2)
 *
 *  COMPLEXITY   O(m^2 * n log n)  (put the SMALLER dimension in the m^2 loop)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A25_MaxSumOfRectangleNoLargerThanK {

    // ---------------- Referee: every rectangle with a 2-D prefix sum ----------------
    static int maxSumSubmatrixBrute(int[][] matrix, int k) {
        int m = matrix.length, n = matrix[0].length;
        int[][] P = new int[m + 1][n + 1];                                // P[r][c] = sum of matrix[0..r-1][0..c-1]
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) P[r + 1][c + 1] = matrix[r][c] + P[r][c + 1] + P[r + 1][c] - P[r][c];
        int best = Integer.MIN_VALUE;
        for (int r1 = 0; r1 < m; r1++)
            for (int r2 = r1; r2 < m; r2++)
                for (int c1 = 0; c1 < n; c1++)
                    for (int c2 = c1; c2 < n; c2++) {
                        int s = P[r2 + 1][c2 + 1] - P[r1][c2 + 1] - P[r2 + 1][c1] + P[r1][c1];
                        if (s <= k) best = Math.max(best, s);
                    }
        return best;
    }

    // ---------------- Main solution: row pairs + prefix sums + TreeSet ----------------
    static int maxSumSubmatrix(int[][] matrix, int k) {
        int m = matrix.length, n = matrix[0].length, best = Integer.MIN_VALUE;
        for (int top = 0; top < m; top++) {
            int[] colSum = new int[n];
            for (int bottom = top; bottom < m; bottom++) {
                for (int c = 0; c < n; c++) colSum[c] += matrix[bottom][c];   // grow the strip by one row
                best = Math.max(best, maxSubarrayAtMost(colSum, k));
                if (best == k) return k;                                  // can't do better than k itself
            }
        }
        return best;
    }

    /** Largest subarray sum that is <= k (Integer.MIN_VALUE if none). */
    static int maxSubarrayAtMost(int[] a, int k) {
        TreeSet<Integer> prefixes = new TreeSet<>();
        prefixes.add(0);                                                  // the empty prefix: subarrays starting at 0
        int cur = 0, best = Integer.MIN_VALUE;
        for (int x : a) {
            cur += x;
            Integer p = prefixes.ceiling(cur - k);                       // smallest earlier prefix with cur - p <= k
            if (p != null) best = Math.max(best, cur - p);
            prefixes.add(cur);
        }
        return best;
    }

    public static void main(String[] args) {
        check("example 1", 2, maxSumSubmatrix(new int[][]{{1, 0, 1}, {0, -2, 3}}, 2));
        check("example 2", 3, maxSumSubmatrix(new int[][]{{2, 2, -1}}, 3));
        check("all negative", -1, maxSumSubmatrix(new int[][]{{-5, -1}, {-3, -4}}, 0));
        check("k smaller than any positive sum", -2, maxSumSubmatrix(new int[][]{{5, -2}, {7, 9}}, 1));
        check("1-D helper [1,-2,4], k=2", 2, maxSubarrayAtMost(new int[]{1, -2, 4}, 2));

        Random rnd = new Random(25);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int m = 1 + rnd.nextInt(5), n = 1 + rnd.nextInt(5);
            int[][] g = new int[m][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(21) - 10;
            int k = rnd.nextInt(41) - 10;
            int want = maxSumSubmatrixBrute(g, k);
            if (want == Integer.MIN_VALUE) continue;                      // LeetCode guarantees an answer exists
            ok &= maxSumSubmatrix(g, k) == want;
        }
        check("2,000 random matrices vs every rectangle", true, ok);
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
