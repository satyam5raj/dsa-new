/*
 * ============================================================================
 *  MEDIUM 02 : K CLOSEST POINTS TO ORIGIN    LeetCode #973
 *  https://leetcode.com/problems/k-closest-points-to-origin/
 *  Pattern: TOP-K smallest -> a MAX-heap of size k (compare SQUARED distances)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the k points closest to (0, 0) (any order).
 *      [[1,3],[-2,2]], k=1 -> [[-2,2]]      (dist^2: 10 vs 8)
 *
 *  ---------------------------------------------------------------------------
 *  DISTANCE: sqrt(x^2 + y^2). Never compute the sqrt: it's monotonic, so comparing
 *  x^2 + y^2 gives the same order, exactly, with no floating point.
 *
 *  "k SMALLEST" -> keep a MAX-heap of size k (the mirror of "k largest" -> min-heap):
 *    the root is the FARTHEST of the k kept; a new point closer than the root
 *    replaces it.
 *      for each point: offer; if size > k: poll (drops the farthest)
 *  O(n log k). Quickselect on distances (M01) gives O(n) average if needed.
 *
 *  DRY RUN [[3,3],[5,-1],[-2,4]], k=2   (dist^2 18, 26, 20)
 *    offer 18 -> {18};  offer 26 -> {26,18};  offer 20 -> {26,20,18} -> poll 26
 *    -> [[3,3],[-2,4]]
 *
 *  OVERFLOW NOTE: coordinates up to 10^4 -> x^2 + y^2 up to 2 * 10^8 fits in int.
 *  For bigger coordinates use long.
 *
 *  COMPLEXITY   Time O(n log k)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class M02_KClosestPointsToOrigin {

    static int dist(int[] p) { return p[0] * p[0] + p[1] * p[1]; }

    static int[][] kClosest(int[][] points, int k) {
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(dist(b), dist(a)));   // max-heap by distance
        for (int[] p : points) {
            heap.offer(p);
            if (heap.size() > k) heap.poll();             // drop the farthest
        }
        return heap.toArray(new int[0][]);
    }

    static String sorted(int[][] pts) {
        int[][] c = pts.clone();
        Arrays.sort(c, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        return Arrays.deepToString(c);
    }

    public static void main(String[] args) {
        check("[[1,3],[-2,2]] k=1", "[[-2, 2]]", sorted(kClosest(new int[][]{{1, 3}, {-2, 2}}, 1)));
        check("[[3,3],[5,-1],[-2,4]] k=2", "[[-2, 4], [3, 3]]", sorted(kClosest(new int[][]{{3, 3}, {5, -1}, {-2, 4}}, 2)));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            // distinct distances so the answer is unique: points on the x-axis at distinct |x|
            List<Integer> xs = new ArrayList<>();
            for (int x = 1; x <= 30; x++) xs.add(rnd.nextBoolean() ? x : -x);
            Collections.shuffle(xs, rnd);
            int n = 1 + rnd.nextInt(30), k = 1 + rnd.nextInt(n);
            int[][] pts = new int[n][];
            for (int i = 0; i < n; i++) pts[i] = new int[]{xs.get(i), 0};
            int[][] byDist = pts.clone();
            Arrays.sort(byDist, Comparator.comparingInt(M02_KClosestPointsToOrigin::dist));
            ok &= sorted(kClosest(pts, k)).equals(sorted(Arrays.copyOf(byDist, k)));
        }
        check("500 random cases vs sorting", true, ok);
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
