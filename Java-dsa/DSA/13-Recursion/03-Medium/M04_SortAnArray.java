/*
 * ============================================================================
 *  MEDIUM 04 : SORT AN ARRAY  ⭐ bonus         LeetCode #912
 *  https://leetcode.com/problems/sort-an-array/
 *  Pattern: DIVIDE AND CONQUER - MERGE SORT and QUICK SORT
 * ============================================================================
 *
 *  PROBLEM
 *    Sort without built-in sort functions, in O(n log n).   [5,2,3,1] -> [1,2,3,5]
 *
 *  ---------------------------------------------------------------------------
 *  MERGE SORT ("easy split, hard combine")
 *    sort(lo, hi): split at mid, sort both halves RECURSIVELY, MERGE them
 *    merge two sorted halves with two pointers (Merge Sorted Array from Arrays)
 *    T(n) = 2T(n/2) + O(n) = O(n log n) ALWAYS. STABLE (equal keys keep their order).
 *    Needs an O(n) buffer. Java uses a merge-sort variant (TimSort) for objects.
 *
 *         [5 2 3 1]
 *        /         \
 *     [5 2]       [3 1]        split down to single elements
 *     /   \       /   \
 *   [5]  [2]    [3]  [1]
 *     \   /       \   /
 *     [2 5]       [1 3]        merge on the way back up
 *        \         /
 *        [1 2 3 5]
 *
 *  QUICK SORT ("hard split, easy combine")
 *    pick a PIVOT, PARTITION: smaller left, bigger right; recurse on both sides
 *    nothing to combine - after partitioning, everything is already in place
 *    average O(n log n), WORST O(n^2) when the pivot is always the min/max
 *    (sorted input + first-element pivot) -> use a RANDOM pivot.
 *    Many duplicates also hurt a 2-way partition -> 3-WAY partition (< = >).
 *    In place, O(log n) stack on average, not stable. Java uses dual-pivot quicksort
 *    for primitive arrays.
 *
 *  COMPLEXITY   merge: O(n log n) time, O(n) space;  quick: O(n log n) avg, O(log n) stack
 * ============================================================================
 */

import java.util.*;

public class M04_SortAnArray {

    // ---------------- merge sort ----------------
    static int[] mergeSort(int[] nums) {
        int[] a = nums.clone(), buf = new int[a.length];
        mergeSort(a, buf, 0, a.length - 1);
        return a;
    }

    static void mergeSort(int[] a, int[] buf, int lo, int hi) {
        if (lo >= hi) return;                                   // 0 or 1 element: sorted
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid);                             // sort the left half
        mergeSort(a, buf, mid + 1, hi);                         // sort the right half
        if (a[mid] <= a[mid + 1]) return;                       // already in order: skip the merge
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) buf[k++] = a[i] <= a[j] ? a[i++] : a[j++];   // <= keeps it stable
        while (i <= mid) buf[k++] = a[i++];
        while (j <= hi) buf[k++] = a[j++];
        System.arraycopy(buf, lo, a, lo, hi - lo + 1);
    }

    // ---------------- quick sort (random pivot, 3-way partition) ----------------
    static final Random RND = new Random(42);

    static int[] quickSort(int[] nums) {
        int[] a = nums.clone();
        quickSort(a, 0, a.length - 1);
        return a;
    }

    static void quickSort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int pivot = a[lo + RND.nextInt(hi - lo + 1)];
        int lt = lo, i = lo, gt = hi;                            // [lo..lt-1] < p, [lt..i-1] == p, [gt+1..hi] > p
        while (i <= gt) {
            if (a[i] < pivot) swap(a, lt++, i++);
            else if (a[i] > pivot) swap(a, i, gt--);
            else i++;
        }
        quickSort(a, lo, lt - 1);                                // the "equal" block is final
        quickSort(a, gt + 1, hi);
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    // naive quick sort (first element pivot) - to show the worst case
    static int naiveDepth;

    static void naiveQuick(int[] a, int lo, int hi, int depth) {
        if (lo >= hi) return;
        naiveDepth = Math.max(naiveDepth, depth);
        int p = a[lo], i = lo;
        for (int j = lo + 1; j <= hi; j++) if (a[j] < p) swap(a, ++i, j);
        swap(a, lo, i);
        naiveQuick(a, lo, i - 1, depth + 1);
        naiveQuick(a, i + 1, hi, depth + 1);
    }

    public static void main(String[] args) {
        check("merge sort [5,2,3,1]", "[1, 2, 3, 5]", mergeSort(new int[]{5, 2, 3, 1}));
        check("quick sort [5,1,1,2,0,0]", "[0, 0, 1, 1, 2, 5]", quickSort(new int[]{5, 1, 1, 2, 0, 0}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int[] a = rnd.ints(rnd.nextInt(60), -20, 20).toArray(), s = a.clone();
            Arrays.sort(s);
            ok &= Arrays.equals(mergeSort(a), s) && Arrays.equals(quickSort(a), s);
        }
        check("2,000 random arrays vs Arrays.sort", true, ok);

        int[] sorted = new int[3000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        naiveDepth = 0;
        naiveQuick(sorted.clone(), 0, sorted.length - 1, 1);
        System.out.printf("   already-sorted input, first-element pivot: recursion depth %,d (= n -> O(n^2))%n", naiveDepth);
        check("naive pivot degenerates", true, naiveDepth >= 2999);

        int[] big = rnd.ints(2_000_000).toArray();
        long t0 = System.nanoTime();
        int[] m = mergeSort(big);
        long tm = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        int[] q = quickSort(big);
        long tq = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("   2,000,000 random ints: merge sort %d ms, quick sort %d ms%n", tm, tq);
        check("both sort the big array identically", true, Arrays.equals(m, q));
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
