/*
 * ============================================================================
 *  MEDIUM 01 : KTH LARGEST ELEMENT IN AN ARRAY   LeetCode #215
 *  https://leetcode.com/problems/kth-largest-element-in-an-array/
 *  Pattern: size-k MIN-heap  vs  QUICKSELECT (average O(n))
 * ============================================================================
 *
 *  PROBLEM
 *    Return the k-th largest element (in sorted order, NOT the k-th distinct).
 *      [3,2,1,5,6,4], k=2 -> 5        [3,2,3,1,2,4,5,5,6], k=4 -> 4
 *
 *  ---------------------------------------------------------------------------
 *  1. SORT: O(n log n). Fine, but the interviewer wants better.
 *
 *  2. MIN-HEAP OF SIZE k: O(n log k), O(k) space. (E01 on a fixed array.)
 *     Great when k is small or the data is a stream.
 *
 *  3. QUICKSELECT: average O(n), O(1) space.
 *     Quicksort's PARTITION puts a pivot at its final sorted position p, with
 *     smaller elements left and larger right. The k-th largest is at sorted index
 *     target = n - k. Then:
 *        p == target -> done
 *        p <  target -> the answer is in the RIGHT part only
 *        p >  target -> the LEFT part only
 *     Unlike quicksort, recurse into ONE side: n + n/2 + n/4 + ... = 2n on average.
 *     WORST CASE O(n^2) with bad pivots (e.g. always the max) -> pick a RANDOM pivot.
 *     ALSO: arrays with many duplicates make a 2-way partition slow. Use a 3-WAY
 *     partition (less / equal / greater, the Dutch flag from Arrays) - done below.
 *
 *  DRY RUN quickselect [3,2,1,5,6,4], k=2 -> target index 4
 *    pivot 4: [3,2,1] [4] [5,6] -> equal block at index 3 < 4 -> go right: [5,6]
 *    pivot 5: [5] [6] -> index 4 holds 5 -> answer 5
 *
 *  COMPLEXITY   heap O(n log k) / quickselect O(n) average, O(n^2) worst (rare with random pivot)
 * ============================================================================
 */

import java.util.*;

public class M01_KthLargestElementInArray {

    static int findKthLargestHeap(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int x : nums) {
            heap.offer(x);
            if (heap.size() > k) heap.poll();
        }
        return heap.peek();
    }

    static final Random RND = new Random(42);

    static int findKthLargest(int[] nums, int k) {
        int[] a = nums.clone();
        int target = a.length - k;                       // index in ASCENDING order
        int lo = 0, hi = a.length - 1;
        while (true) {
            int pivot = a[lo + RND.nextInt(hi - lo + 1)];
            // 3-way partition of a[lo..hi]: [ < pivot | == pivot | > pivot ]
            int lt = lo, i = lo, gt = hi;
            while (i <= gt) {
                if (a[i] < pivot) swap(a, lt++, i++);
                else if (a[i] > pivot) swap(a, i, gt--);
                else i++;
            }
            if (target < lt) hi = lt - 1;                // answer among the smaller ones
            else if (target > gt) lo = gt + 1;           // among the larger ones
            else return pivot;                           // inside the "equal" block
        }
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void main(String[] args) {
        check("[3,2,1,5,6,4] k=2", 5, findKthLargest(new int[]{3, 2, 1, 5, 6, 4}, 2));
        check("[3,2,3,1,2,4,5,5,6] k=4", 4, findKthLargest(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4));
        check("heap version", 4, findKthLargestHeap(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4));
        check("[1] k=1", 1, findKthLargest(new int[]{1}, 1));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(50), -10, 10).toArray();
            int k = 1 + rnd.nextInt(a.length);
            int[] s = a.clone();
            Arrays.sort(s);
            ok &= findKthLargest(a, k) == s[a.length - k] && findKthLargestHeap(a, k) == s[a.length - k];
        }
        check("1,000 random arrays vs sorting", true, ok);

        int[] big = new int[2_000_000];
        Arrays.fill(big, 7);                             // all duplicates: 2-way partition's nightmare
        for (int i = 0; i < 1000; i++) big[rnd.nextInt(big.length)] = rnd.nextInt(100);
        long t0 = System.nanoTime();
        int q = findKthLargest(big, 1_000_000);
        long qs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        int h = findKthLargestHeap(big, 1_000_000);
        long hp = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("   n = 2,000,000 (mostly duplicates), k = 1,000,000: quickselect %d ms, heap %d ms%n", qs, hp);
        check("both agree on the big input", true, q == h);
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
