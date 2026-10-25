/*
 * ============================================================================
 *  ADVANCE 13 : REVERSE PAIRS  ⭐ bonus   LeetCode #493
 *  https://leetcode.com/problems/reverse-pairs/
 *  Pattern: MERGE SORT counting  or  FENWICK TREE with coordinate compression
 * ============================================================================
 *
 *  PROBLEM
 *    Count pairs i < j with nums[i] > 2 * nums[j].
 *      [1,3,2,3,1] -> 2      (3 > 2*1 twice: (1,4) and (3,4))
 *      [2,4,3,5,1] -> 3      (4>2, 3>2, 5>2 against the last 1)
 *
 *  ---------------------------------------------------------------------------
 *  This is "count inversions" with a twist. The same two tools as A12 work:
 *
 *  (A) MERGE SORT: pairs inside each half are counted recursively. For pairs with i in
 *      the left half and j in the right half, both halves can be SORTED (positions inside
 *      a half no longer matter, only "left of" vs "right of"). For each left value x
 *      (ascending), count right values y with x > 2y: a pointer that only moves right.
 *      IMPORTANT: count BEFORE merging, with its own pointer - the merge compares x vs y,
 *      the count compares x vs 2y.
 *
 *  (B) FENWICK TREE: walk j left to right, count earlier values > 2*nums[j]
 *      = (how many seen) - (how many seen <= 2*nums[j]). Compress the values to ranks.
 *
 *  2 * nums[j] overflows int when nums[j] is near 2^31 -> use 2L * nums[j].
 *
 *  DRY RUN (A) [2,4,3,5,1]
 *    split [2,4] [3,5,1]  ->  [2,4]: 2 > 8? no -> 0 ;  [3,5,1] -> [3] [5,1]: 5 > 2 -> 1,
 *                             then [3] vs [1,5]: 3 > 2 -> 1   => right half total 2
 *    top level: left [2,4] vs right sorted [1,3,5]: 2 > 2? no; 4 > 2 -> 1   => 1
 *    total 0 + 2 + 1 = 3
 *
 *  COMPLEXITY   O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A13_ReversePairs {

    // ---------------- Approach 1: every pair (the referee) ----------------
    static int reversePairsBrute(int[] nums) {
        int count = 0;
        for (int i = 0; i < nums.length; i++)
            for (int j = i + 1; j < nums.length; j++)
                if ((long) nums[i] > 2L * nums[j]) count++;
        return count;
    }

    // ---------------- Approach 2: merge sort (main solution) ----------------
    static int reversePairs(int[] nums) {
        int[] a = nums.clone();
        return sortCount(a, new int[a.length], 0, a.length);
    }

    private static int sortCount(int[] a, int[] tmp, int lo, int hi) {      // range [lo, hi)
        if (hi - lo <= 1) return 0;
        int mid = (lo + hi) >>> 1;
        int count = sortCount(a, tmp, lo, mid) + sortCount(a, tmp, mid, hi);
        int j = mid;
        for (int i = lo; i < mid; i++) {                                  // both halves are sorted now
            while (j < hi && a[i] > 2L * a[j]) j++;
            count += j - mid;                                             // a[mid..j) are all < a[i] / 2
        }
        int p = lo, q = mid, k = lo;                                      // standard merge
        while (p < mid || q < hi) tmp[k++] = (q >= hi || (p < mid && a[p] <= a[q])) ? a[p++] : a[q++];
        System.arraycopy(tmp, lo, a, lo, hi - lo);
        return count;
    }

    // ---------------- Approach 3: Fenwick tree + coordinate compression ----------------
    static int reversePairsFenwick(int[] nums) {
        long[] sorted = Arrays.stream(nums).asLongStream().sorted().distinct().toArray();
        int[] tree = new int[sorted.length + 1];
        int count = 0;
        for (int j = 0; j < nums.length; j++) {
            int atMost = upperBound(sorted, 2L * nums[j]);                // ranks whose value <= 2*nums[j]
            count += j - prefix(tree, atMost);                            // seen so far minus those small enough
            add(tree, upperBound(sorted, nums[j]));                       // rank of nums[j], 1-based
        }
        return count;
    }

    static int upperBound(long[] a, long x) {                             // number of values <= x
        int lo = 0, hi = a.length;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] <= x) lo = mid + 1; else hi = mid; }
        return lo;
    }

    static void add(int[] tree, int i) { for (; i < tree.length; i += i & -i) tree[i]++; }

    static int prefix(int[] tree, int i) {
        int s = 0;
        for (; i > 0; i -= i & -i) s += tree[i];
        return s;
    }

    public static void main(String[] args) {
        check("example 1", 2, reversePairs(new int[]{1, 3, 2, 3, 1}));
        check("example 2", 3, reversePairs(new int[]{2, 4, 3, 5, 1}));
        check("example 2 (Fenwick)", 3, reversePairsFenwick(new int[]{2, 4, 3, 5, 1}));
        check("2 * MAX must not overflow", 0, reversePairs(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}));
        check("negatives: -1 > 2*(-5)", 1, reversePairs(new int[]{-1, -5}));

        Random rnd = new Random(13);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = rnd.nextInt(30);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(5) == 0 ? (rnd.nextBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE) : rnd.nextInt(41) - 20;
            int want = reversePairsBrute(a);
            ok &= reversePairs(a) == want && reversePairsFenwick(a) == want;
        }
        check("2,000 random arrays: merge == Fenwick == brute", true, ok);
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
