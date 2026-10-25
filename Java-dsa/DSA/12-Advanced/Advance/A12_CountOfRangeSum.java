/*
 * ============================================================================
 *  ADVANCE 12 : COUNT OF RANGE SUM  ⭐ bonus   LeetCode #327
 *  https://leetcode.com/problems/count-of-range-sum/
 *  Pattern: PREFIX SUMS + (MERGE SORT counting  or  FENWICK TREE with coordinate compression)
 * ============================================================================
 *
 *  PROBLEM
 *    Count the subarrays nums[i..j] whose sum lies in [lower, upper].
 *      nums [-2,5,-1], lower -2, upper 2  ->  3     ([-2], [-1], [-2,5,-1])
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1 - PREFIX SUMS turn "subarray sum" into "difference of two numbers":
 *    pre[0] = 0, pre[k] = nums[0] + ... + nums[k-1];   sum(i..j) = pre[j+1] - pre[i]
 *    So we count pairs a < b with  lower <= pre[b] - pre[a] <= upper.
 *    Use long: the sums of 10^5 ints overflow int.
 *
 *  STEP 2 - count those pairs faster than O(n^2). Two classic ways:
 *
 *  (A) MERGE SORT: split pre into halves. Pairs inside a half are counted recursively.
 *      For pairs with a in the LEFT half and b in the RIGHT half, order inside each half
 *      no longer matters - so both halves can be SORTED. For each left value x, the right
 *      values y with  x + lower <= y <= x + upper  form a contiguous window; as x grows
 *      the window only slides right -> two pointers, O(n) per level. Then merge.
 *
 *  (B) FENWICK TREE: walk b from left to right; we need how many earlier pre[a] lie in
 *      [pre[b] - upper, pre[b] - lower]. Values can be huge/negative, so first sort the
 *      distinct prefix values and replace each by its RANK (coordinate compression); then
 *      a Fenwick tree over ranks counts "how many seen with rank in [l, r]" in O(log n).
 *
 *  DRY RUN (B) nums [-2,5,-1], [-2,2]     pre = [0,-2,3,2]   sorted distinct [-2,0,2,3]
 *    b=0 pre 0:  nothing seen yet                       insert 0
 *    b=1 pre -2: want [-4,0]: seen {0}        -> 1      insert -2
 *    b=2 pre 3:  want [1,5]:  seen {0,-2}     -> 0      insert 3
 *    b=3 pre 2:  want [0,4]:  seen {0,-2,3}   -> 2      total 3
 *
 *  COMPLEXITY   O(n log n) both ways   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A12_CountOfRangeSum {

    // ---------------- Approach 1: every pair of prefix sums (the referee) ----------------
    static int countRangeSumBrute(int[] nums, int lower, int upper) {
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            long sum = 0;
            for (int j = i; j < nums.length; j++) {
                sum += nums[j];
                if (lower <= sum && sum <= upper) count++;
            }
        }
        return count;
    }

    // ---------------- Approach 2: merge sort on prefix sums ----------------
    static int countRangeSumMerge(int[] nums, int lower, int upper) {
        long[] pre = new long[nums.length + 1];
        for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i];
        return (int) sortCount(pre, new long[pre.length], 0, pre.length, lower, upper);
    }

    private static long sortCount(long[] s, long[] tmp, int lo, int hi, int lower, int upper) {   // range [lo, hi)
        if (hi - lo <= 1) return 0;
        int mid = (lo + hi) >>> 1;
        long count = sortCount(s, tmp, lo, mid, lower, upper) + sortCount(s, tmp, mid, hi, lower, upper);
        int from = mid, to = mid;                                        // window [from, to) in the sorted right half
        for (int i = lo; i < mid; i++) {
            while (from < hi && s[from] - s[i] < lower) from++;
            while (to < hi && s[to] - s[i] <= upper) to++;
            count += to - from;
        }
        int a = lo, b = mid, k = lo;                                     // standard merge
        while (a < mid || b < hi) tmp[k++] = (b >= hi || (a < mid && s[a] <= s[b])) ? s[a++] : s[b++];
        System.arraycopy(tmp, lo, s, lo, hi - lo);
        return count;
    }

    // ---------------- Approach 3: Fenwick tree + coordinate compression (main solution) ----------------
    static int countRangeSum(int[] nums, int lower, int upper) {
        long[] pre = new long[nums.length + 1];
        for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i];
        long[] sorted = Arrays.stream(pre).sorted().distinct().toArray();
        int[] tree = new int[sorted.length + 1];                         // Fenwick tree, 1-indexed by rank

        long count = 0;
        for (long p : pre) {
            int l = lowerBound(sorted, p - upper);                       // first rank with value >= p - upper
            int r = lowerBound(sorted, p - lower + 1);                   // first rank with value >  p - lower
            count += prefix(tree, r) - prefix(tree, l);                  // seen values with rank in [l, r)
            add(tree, lowerBound(sorted, p) + 1);
        }
        return (int) count;
    }

    static int lowerBound(long[] a, long x) {                            // first index with a[i] >= x
        int lo = 0, hi = a.length;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] < x) lo = mid + 1; else hi = mid; }
        return lo;
    }

    static void add(int[] tree, int i) { for (; i < tree.length; i += i & -i) tree[i]++; }

    static int prefix(int[] tree, int i) {                               // how many seen with rank < i (0-based)
        int s = 0;
        for (; i > 0; i -= i & -i) s += tree[i];
        return s;
    }

    public static void main(String[] args) {
        check("example 1", 3, countRangeSum(new int[]{-2, 5, -1}, -2, 2));
        check("example 1 (merge sort)", 3, countRangeSumMerge(new int[]{-2, 5, -1}, -2, 2));
        check("single zero", 1, countRangeSum(new int[]{0}, 0, 0));
        check("MAX+1 must not wrap to MIN", 0, countRangeSum(new int[]{Integer.MAX_VALUE, 1}, Integer.MIN_VALUE, 0));

        Random rnd = new Random(12);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = rnd.nextInt(25);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(21) - 10;
            int lo = rnd.nextInt(21) - 10, hi = lo + rnd.nextInt(15);
            int want = countRangeSumBrute(a, lo, hi);
            ok &= countRangeSum(a, lo, hi) == want && countRangeSumMerge(a, lo, hi) == want;
        }
        check("2,000 random arrays: Fenwick == merge == brute", true, ok);
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
