/*
 * ============================================================================
 *  MEDIUM 16 : RANGE SUM QUERY - MUTABLE  ⭐ bonus   LeetCode #307
 *  https://leetcode.com/problems/range-sum-query-mutable/
 *  Pattern: SEGMENT TREE or FENWICK TREE (01-Basics/SegmentTreeAndFenwick)
 * ============================================================================
 *
 *  PROBLEM
 *    NumArray(nums), update(index, val), sumRange(left, right) - interleaved,
 *    up to 3 * 10^4 calls each.
 *      nums [1,3,5]: sumRange(0,2) = 9; update(1,2); sumRange(0,2) = 8
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT THE USUAL TOOLS
 *    loop over the range     : update O(1), query O(n)
 *    prefix sums (Arrays)    : query O(1), but update must rebuild them: O(n)
 *    -> with many of BOTH, either is O(n * q).
 *
 *  FENWICK (Binary Indexed Tree): both O(log n), ~10 lines
 *    update(i, val): add the DIFFERENCE (val - nums[i]) via the low-bit climb,
 *                    remember the new nums[i]
 *    sumRange(l, r) = prefix(r) - prefix(l - 1)
 *
 *  ITERATIVE SEGMENT TREE (bottom-up, size 2n) - also short, also O(log n):
 *    leaves at tree[n .. 2n-1], parent of i is i/2, tree[i] = tree[2i] + tree[2i+1]
 *    update: set the leaf, walk up with i /= 2
 *    query [l, r]: l += n, r += n + 1 (half-open), and while l < r:
 *        if l is a RIGHT child (odd): take tree[l++]    (its parent covers too much)
 *        if r is a RIGHT child (odd): take tree[--r]
 *        l /= 2, r /= 2
 *
 *  WHICH TO USE IN AN INTERVIEW? Fenwick for sums (shortest code). Segment tree
 *  when the operation isn't invertible (min/max) or you need range updates (lazy).
 *
 *  COMPLEXITY   construct O(n), update O(log n), sumRange O(log n)
 * ============================================================================
 */

import java.util.*;

public class M16_RangeSumQueryMutable {

    static class NumArray {                    // Fenwick tree version
        private final int[] nums;
        private final int[] tree;              // 1-indexed

        NumArray(int[] nums) {
            this.nums = nums.clone();
            tree = new int[nums.length + 1];
            for (int i = 0; i < nums.length; i++) add(i, nums[i]);
        }

        private void add(int i, int delta) {
            for (int x = i + 1; x < tree.length; x += x & -x) tree[x] += delta;
        }

        private int prefix(int i) {            // sum of nums[0..i]
            int s = 0;
            for (int x = i + 1; x > 0; x -= x & -x) s += tree[x];
            return s;
        }

        void update(int index, int val) {
            add(index, val - nums[index]);     // Fenwick stores sums: push the DIFFERENCE
            nums[index] = val;
        }

        int sumRange(int left, int right) {
            return prefix(right) - (left > 0 ? prefix(left - 1) : 0);
        }
    }

    static class NumArraySegment {             // iterative bottom-up segment tree
        private final int n;
        private final int[] tree;

        NumArraySegment(int[] nums) {
            n = nums.length;
            tree = new int[2 * n];
            for (int i = 0; i < n; i++) tree[n + i] = nums[i];           // leaves
            for (int i = n - 1; i > 0; i--) tree[i] = tree[2 * i] + tree[2 * i + 1];
        }

        void update(int index, int val) {
            int i = index + n;
            tree[i] = val;
            for (i /= 2; i > 0; i /= 2) tree[i] = tree[2 * i] + tree[2 * i + 1];
        }

        int sumRange(int left, int right) {
            int s = 0;
            for (int l = left + n, r = right + n + 1; l < r; l /= 2, r /= 2) {
                if ((l & 1) == 1) s += tree[l++];
                if ((r & 1) == 1) s += tree[--r];
            }
            return s;
        }
    }

    public static void main(String[] args) {
        NumArray na = new NumArray(new int[]{1, 3, 5});
        check("sumRange(0,2)", 9, na.sumRange(0, 2));
        na.update(1, 2);
        check("after update(1,2): sumRange(0,2)", 8, na.sumRange(0, 2));
        NumArraySegment ns = new NumArraySegment(new int[]{1, 3, 5});
        ns.update(1, 2);
        check("segment tree: same", 8, ns.sumRange(0, 2));

        Random rnd = new Random(16);
        boolean ok = true;
        for (int t = 0; t < 50; t++) {
            int n = 1 + rnd.nextInt(60);
            int[] a = rnd.ints(n, -100, 101).toArray();
            NumArray f = new NumArray(a);
            NumArraySegment s = new NumArraySegment(a);
            for (int op = 0; op < 400; op++) {
                if (rnd.nextBoolean()) {
                    int i = rnd.nextInt(n), v = rnd.nextInt(201) - 100;
                    a[i] = v; f.update(i, v); s.update(i, v);
                } else {
                    int l = rnd.nextInt(n), r = l + rnd.nextInt(n - l), exp = 0;
                    for (int i = l; i <= r; i++) exp += a[i];
                    ok &= f.sumRange(l, r) == exp && s.sumRange(l, r) == exp;
                }
            }
        }
        check("20,000 random ops (both, all sizes)", true, ok);
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
