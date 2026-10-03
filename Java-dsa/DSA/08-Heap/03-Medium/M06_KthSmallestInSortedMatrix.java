/*
 * ============================================================================
 *  MEDIUM 06 : KTH SMALLEST ELEMENT IN A SORTED MATRIX   LeetCode #378
 *  https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/
 *  Pattern: K-WAY MERGE of the rows  -or-  BINARY SEARCH ON THE VALUE
 * ============================================================================
 *
 *  PROBLEM
 *    n x n matrix, every row AND every column sorted ascending. k-th smallest overall.
 *      [[ 1, 5, 9],
 *       [10,11,13],
 *       [12,13,15]], k=8 -> 13     (1 5 9 10 11 12 13 [13] 15)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE: flatten + sort, O(n^2 log n).
 *
 *  APPROACH 1: K-WAY MERGE (HeapPatterns #2)
 *    Each row is a sorted list. Put the first element of every row in a min-heap
 *    {value, row, col}. Poll k times; each poll pushes the next element of that row.
 *    The k-th poll is the answer.   O(k log n)  (only min(n, k) rows are needed)
 *
 *  APPROACH 2: BINARY SEARCH ON THE ANSWER VALUE   O(n log(max - min))
 *    For a value x, count how many entries are <= x - in O(n) with a staircase walk:
 *      start at the bottom-left; if a[r][c] <= x, the whole column above is <= x
 *      (count += r + 1, move right), else move up.
 *    count(x) grows with x. The answer is the SMALLEST x with count(x) >= k.
 *    (That x is always a value in the matrix: the smallest one reaching k.)
 *
 *  DRY RUN merge, k=8
 *    heap {1,10,12} -> polls: 1, 5, 9, 10, 11, 12, 13, 13 -> the 8th is 13
 *
 *  COMPLEXITY   merge O(k log n);  binary search O(n log(range)), O(1) space
 * ============================================================================
 */

import java.util.*;

public class M06_KthSmallestInSortedMatrix {

    static int kthSmallestHeap(int[][] m, int k) {
        int n = m.length;
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
        for (int r = 0; r < Math.min(n, k); r++) heap.offer(new int[]{m[r][0], r, 0});
        int[] e = null;
        for (int i = 0; i < k; i++) {
            e = heap.poll();
            if (e[2] + 1 < n) heap.offer(new int[]{m[e[1]][e[2] + 1], e[1], e[2] + 1});   // next in that row
        }
        return e[0];
    }

    static int kthSmallest(int[][] m, int k) {
        int lo = m[0][0], hi = m[m.length - 1][m.length - 1];
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (countAtMost(m, mid) >= k) hi = mid;     // mid could be the answer, look lower
            else lo = mid + 1;
        }
        return lo;
    }

    // how many entries are <= x, walking a staircase from the bottom-left
    static int countAtMost(int[][] m, int x) {
        int n = m.length, r = n - 1, c = 0, count = 0;
        while (r >= 0 && c < n) {
            if (m[r][c] <= x) { count += r + 1; c++; }  // this column's rows 0..r are all <= x
            else r--;
        }
        return count;
    }

    public static void main(String[] args) {
        int[][] m = {{1, 5, 9}, {10, 11, 13}, {12, 13, 15}};
        check("k=8 (binary search)", 13, kthSmallest(m, 8));
        check("k=8 (heap merge)", 13, kthSmallestHeap(m, 8));
        check("[[-5]] k=1", -5, kthSmallest(new int[][]{{-5}}, 1));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] a = new int[n][n];
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++) {
                    int base = Math.max(r > 0 ? a[r - 1][c] : -20, c > 0 ? a[r][c - 1] : -20);
                    a[r][c] = base + rnd.nextInt(4);        // keeps rows and columns sorted
                }
            int[] flat = Arrays.stream(a).flatMapToInt(Arrays::stream).sorted().toArray();
            int k = 1 + rnd.nextInt(n * n);
            ok &= kthSmallest(a, k) == flat[k - 1] && kthSmallestHeap(a, k) == flat[k - 1];
        }
        check("300 random matrices vs flatten+sort", true, ok);
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
