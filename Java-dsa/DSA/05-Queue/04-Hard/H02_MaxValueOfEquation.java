/*
 * ============================================================================
 *  HARD 02 : MAX VALUE OF EQUATION  ⭐ bonus   LeetCode #1499
 *  https://leetcode.com/problems/max-value-of-equation/
 *  Pattern: REWRITE THE FORMULA -> sliding-window maximum with a monotonic deque
 * ============================================================================
 *
 *  PROBLEM
 *    points[i] = [x_i, y_i], sorted by x (strictly increasing). Find the maximum of
 *        y_i + y_j + |x_i - x_j|    over pairs i < j with  x_j - x_i <= k.
 *      [[1,3],[2,0],[5,10],[6,-10]], k=1 -> 4    (points 0,1: 3 + 0 + 1)
 *      [[0,0],[3,0],[9,2]],          k=3 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: all pairs within distance k -> O(n^2)
 *
 *  THE ALGEBRA TRICK (the real insight)
 *    x is sorted, so for i < j:  |x_i - x_j| = x_j - x_i. The expression becomes
 *        (y_i - x_i) + (y_j + x_j)
 *    For a FIXED j, the second part is a constant. We just need the MAX of
 *    (y_i - x_i) over earlier points with x_i >= x_j - k: a SLIDING WINDOW MAXIMUM!
 *
 *  MONOTONIC DEQUE of indices with DECREASING (y - x)
 *    for each j:
 *      drop from the front the points that are too far away (x_j - x_front > k)
 *      if the deque isn't empty: candidate = (y - x of the front) + y_j + x_j
 *      drop from the back every point with (y - x) <= y_j - x_j  (j is at least as good, and newer)
 *      push j
 *
 *  DRY RUN [[1,3],[2,0],[5,10],[6,-10]], k=1
 *    j0 (1,3):  deque empty;  push (y-x = 2)
 *    j1 (2,0):  front x=1, 2-1 <= 1 -> candidate 2 + 0 + 2 = 4;  y-x = -2 < 2 -> push
 *    j2 (5,10): fronts at x=1, x=2 are too far -> empty;  push (y-x = 5)
 *    j3 (6,-10): front x=5, 6-5 <= 1 -> candidate 5 + (-10) + 6 = 1
 *    answer 4
 *
 *  LESSON: when a formula mixes i and j, try to SEPARATE it into f(i) + g(j).
 *  Then "best pair" becomes "best f(i) in a window", for each j.
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H02_MaxValueOfEquation {

    static int findMaxValueOfEquation(int[][] points, int k) {
        Deque<Integer> dq = new ArrayDeque<>();                 // indices, (y - x) decreasing
        int best = Integer.MIN_VALUE;
        for (int j = 0; j < points.length; j++) {
            int xj = points[j][0], yj = points[j][1];
            while (!dq.isEmpty() && xj - points[dq.peekFirst()][0] > k) dq.pollFirst();   // too far
            if (!dq.isEmpty()) {
                int i = dq.peekFirst();
                best = Math.max(best, points[i][1] - points[i][0] + yj + xj);
            }
            while (!dq.isEmpty() && points[dq.peekLast()][1] - points[dq.peekLast()][0] <= yj - xj) dq.pollLast();
            dq.offerLast(j);
        }
        return best;
    }

    static int brute(int[][] p, int k) {
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < p.length; i++)
            for (int j = i + 1; j < p.length && p[j][0] - p[i][0] <= k; j++)
                best = Math.max(best, p[i][1] + p[j][1] + p[j][0] - p[i][0]);
        return best;
    }

    public static void main(String[] args) {
        check("[[1,3],[2,0],[5,10],[6,-10]] k=1", 4, findMaxValueOfEquation(new int[][]{{1, 3}, {2, 0}, {5, 10}, {6, -10}}, 1));
        check("[[0,0],[3,0],[9,2]] k=3", 3, findMaxValueOfEquation(new int[][]{{0, 0}, {3, 0}, {9, 2}}, 3));
        Random rnd = new Random(9);
        boolean ok = true;
        int tested = 0;
        for (int t = 0; t < 1000; t++) {
            int n = 2 + rnd.nextInt(15);
            int[][] p = new int[n][2];
            int x = rnd.nextInt(5);
            for (int i = 0; i < n; i++) { x += 1 + rnd.nextInt(3); p[i] = new int[]{x, rnd.nextInt(21) - 10}; }
            int k = 1 + rnd.nextInt(6);
            int b = brute(p, k);
            if (b == Integer.MIN_VALUE) continue;               // the problem guarantees at least one valid pair
            tested++;
            ok &= findMaxValueOfEquation(p, k) == b;
        }
        check(tested + " random cases vs brute force", true, ok);
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
