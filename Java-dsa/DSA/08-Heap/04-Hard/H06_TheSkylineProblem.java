/*
 * ============================================================================
 *  HARD 06 : THE SKYLINE PROBLEM             LeetCode #218
 *  https://leetcode.com/problems/the-skyline-problem/
 *  Pattern: SWEEP LINE over x + MAX-HEAP of "active" heights (with lazy deletion)
 * ============================================================================
 *
 *  PROBLEM
 *    Buildings [left, right, height]. Return the skyline's "key points": [x, y]
 *    wherever the outline's height CHANGES (left to right, no two consecutive with
 *    the same y; the last point has y = 0).
 *      [[2,9,10],[3,7,15],[5,12,12],[15,20,10],[19,24,8]]
 *      -> [[2,10],[3,15],[7,12],[12,0],[15,10],[20,8],[24,0]]
 *
 *  ---------------------------------------------------------------------------
 *  SWEEP LINE: move a vertical line from left to right. The outline's height at x
 *  is the MAX height among the buildings covering x. It can only change at a
 *  building's left or right edge -> only look at those x values ("events").
 *
 *  ALGORITHM
 *    collect every distinct edge x, sorted
 *    sort buildings by left; a pointer adds buildings as the sweep reaches them
 *    max-heap of {height, right} for the buildings started so far
 *    at each x:
 *      push every building with left == x
 *      LAZY DELETION: pop from the top while top.right <= x (that building ended)
 *         (buildings that ended but aren't on top don't matter until they surface)
 *      h = heap empty ? 0 : top.height
 *      if h != the previous height -> key point [x, h]
 *
 *  DRY RUN (first steps)
 *    x=2: push (10,9) -> h 10 -> [2,10]
 *    x=3: push (15,7) -> h 15 -> [3,15]
 *    x=5: push (12,12) -> h 15, unchanged
 *    x=7: top (15,7) ended -> pop -> h 12 -> [7,12]
 *    x=9: top (12,12) still alive -> h 12, unchanged ((10,9) is dead but buried)
 *    x=12: pop (12,12), pop (10,9) -> empty -> [12,0]
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H06_TheSkylineProblem {

    static List<List<Integer>> getSkyline(int[][] buildings) {
        TreeSet<Integer> xs = new TreeSet<>();
        for (int[] b : buildings) { xs.add(b[0]); xs.add(b[1]); }
        int[][] byLeft = buildings.clone();
        Arrays.sort(byLeft, Comparator.comparingInt(b -> b[0]));
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // {height, right}, max height
        List<List<Integer>> result = new ArrayList<>();
        int i = 0, prevHeight = 0;
        for (int x : xs) {
            while (i < byLeft.length && byLeft[i][0] == x) {           // buildings starting here
                heap.offer(new int[]{byLeft[i][2], byLeft[i][1]});
                i++;
            }
            while (!heap.isEmpty() && heap.peek()[1] <= x) heap.poll();  // lazily drop finished tallest
            int h = heap.isEmpty() ? 0 : heap.peek()[0];
            if (h != prevHeight) {
                result.add(List.of(x, h));
                prevHeight = h;
            }
        }
        return result;
    }

    // brute force: height at each event x = max over buildings covering [x, x+epsilon)
    static List<List<Integer>> brute(int[][] buildings) {
        TreeSet<Integer> xs = new TreeSet<>();
        for (int[] b : buildings) { xs.add(b[0]); xs.add(b[1]); }
        List<List<Integer>> out = new ArrayList<>();
        int prev = 0;
        for (int x : xs) {
            int h = 0;
            for (int[] b : buildings) if (b[0] <= x && x < b[1]) h = Math.max(h, b[2]);
            if (h != prev) { out.add(List.of(x, h)); prev = h; }
        }
        return out;
    }

    public static void main(String[] args) {
        int[][] b = {{2, 9, 10}, {3, 7, 15}, {5, 12, 12}, {15, 20, 10}, {19, 24, 8}};
        check("example", "[[2, 10], [3, 15], [7, 12], [12, 0], [15, 10], [20, 8], [24, 0]]", getSkyline(b));
        check("[[0,2,3],[2,5,3]] (merge equal heights)", "[[0, 3], [5, 0]]", getSkyline(new int[][]{{0, 2, 3}, {2, 5, 3}}));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] bs = new int[n][];
            for (int j = 0; j < n; j++) { int l = rnd.nextInt(20); bs[j] = new int[]{l, l + 1 + rnd.nextInt(8), 1 + rnd.nextInt(6)}; }
            ok &= getSkyline(bs).equals(brute(bs));
        }
        check("1,000 random cities vs brute force", true, ok);
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
