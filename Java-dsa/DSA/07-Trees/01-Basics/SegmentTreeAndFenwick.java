/*
 * ============================================================================
 *  SEGMENT TREE & FENWICK TREE (Binary Indexed Tree): fast RANGE queries
 * ============================================================================
 *
 *  THE PROBLEM: an array that CHANGES, plus questions about RANGES.
 *    update(i, value)      and      sum(l..r)  (or min, max, gcd ...)
 *
 *    approach               update      range query
 *    plain array            O(1)        O(n)    (loop over the range)
 *    prefix sums            O(n)        O(1)    (rebuild after every change)
 *    SEGMENT / FENWICK      O(log n)    O(log n)   <- both fast
 *
 *  ---------------------------------------------------------------------------
 *  SEGMENT TREE
 *    A binary tree where each node stores the answer for a SEGMENT of the array.
 *    The root covers [0, n-1]; a node covering [l, r] has children [l, mid] and [mid+1, r].
 *    Leaves are single elements. Stored in an ARRAY like a heap: children of
 *    node i are 2i and 2i+1 (root at 1). Size 4n is always enough.
 *
 *      arr = [2, 1, 5, 3]            [0..3] = 11
 *                                   /           \
 *                            [0..1] = 3        [2..3] = 8
 *                            /     \           /     \
 *                         [0]=2  [1]=1      [2]=5   [3]=3
 *
 *    QUERY(l, r): start at the root. For a node's segment:
 *       completely OUTSIDE [l, r] -> contributes 0
 *       completely INSIDE         -> return the stored value (don't go deeper)
 *       partial overlap           -> ask both children, combine
 *       At most ~4 nodes per level are visited -> O(log n).
 *    UPDATE(i, v): change the leaf, then recompute every ancestor -> O(log n).
 *    Works for any ASSOCIATIVE operation: sum, min, max, gcd, xor, matrix product...
 *
 *  LAZY PROPAGATION (range UPDATES, e.g. "add 5 to every element in [l, r]")
 *    Updating each element is O(n). Instead, when a node's segment is completely
 *    inside the update range, update the node's value and leave a NOTE ("lazy[i] += 5:
 *    my children still owe this") and stop. Push the note down to the children only
 *    when a later operation needs to go below that node. -> O(log n) range updates.
 *
 *  ---------------------------------------------------------------------------
 *  FENWICK TREE (Binary Indexed Tree) - smaller, simpler, but only for
 *  "invertible" operations like SUM (because range = prefix(r) - prefix(l-1))
 *    1-indexed array tree[1..n]. tree[i] stores the sum of the LAST lowbit(i)
 *    elements ending at i, where lowbit(i) = i & -i = the lowest set bit of i.
 *       i = 6 = 110b -> lowbit 2 -> tree[6] = a[5] + a[6]
 *       i = 8 = 1000b -> lowbit 8 -> tree[8] = a[1..8]
 *    prefix(i):  sum tree[i], then i -= lowbit(i)   (strip the lowest bit)  -> O(log n)
 *    add(i, d):  tree[i] += d, then i += lowbit(i)  (climb to the parents)  -> O(log n)
 *    The whole thing is about 10 lines. Use it for counting problems (H05).
 *
 *  Run:  java SegmentTreeAndFenwick.java
 * ============================================================================
 */

import java.util.*;

public class SegmentTreeAndFenwick {

    // ---------------- 1. Segment tree for range SUM, point UPDATE ----------------
    static class SegmentTree {
        final int n;
        final long[] tree;

        SegmentTree(int[] arr) {
            n = arr.length;
            tree = new long[4 * Math.max(n, 1)];
            if (n > 0) build(arr, 1, 0, n - 1);
        }

        private void build(int[] arr, int node, int l, int r) {
            if (l == r) { tree[node] = arr[l]; return; }          // leaf
            int mid = (l + r) / 2;
            build(arr, 2 * node, l, mid);
            build(arr, 2 * node + 1, mid + 1, r);
            tree[node] = tree[2 * node] + tree[2 * node + 1];     // combine children
        }

        void update(int i, int value) { update(1, 0, n - 1, i, value); }

        private void update(int node, int l, int r, int i, int value) {
            if (l == r) { tree[node] = value; return; }
            int mid = (l + r) / 2;
            if (i <= mid) update(2 * node, l, mid, i, value);    // go toward the leaf
            else update(2 * node + 1, mid + 1, r, i, value);
            tree[node] = tree[2 * node] + tree[2 * node + 1];     // recompute on the way back up
        }

        long sum(int ql, int qr) { return sum(1, 0, n - 1, ql, qr); }

        private long sum(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return 0;                        // completely outside
            if (ql <= l && r <= qr) return tree[node];             // completely inside
            int mid = (l + r) / 2;                                 // partial: ask both children
            return sum(2 * node, l, mid, ql, qr) + sum(2 * node + 1, mid + 1, r, ql, qr);
        }
    }

    // ---------------- 2. Segment tree for range MIN (same shape, different combine) ----------------
    static class MinSegmentTree {
        final int n;
        final int[] tree;

        MinSegmentTree(int[] arr) {
            n = arr.length;
            tree = new int[4 * n];
            build(arr, 1, 0, n - 1);
        }

        private void build(int[] arr, int node, int l, int r) {
            if (l == r) { tree[node] = arr[l]; return; }
            int mid = (l + r) / 2;
            build(arr, 2 * node, l, mid);
            build(arr, 2 * node + 1, mid + 1, r);
            tree[node] = Math.min(tree[2 * node], tree[2 * node + 1]);
        }

        int min(int ql, int qr) { return min(1, 0, n - 1, ql, qr); }

        private int min(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return Integer.MAX_VALUE;        // the "identity" for min
            if (ql <= l && r <= qr) return tree[node];
            int mid = (l + r) / 2;
            return Math.min(min(2 * node, l, mid, ql, qr), min(2 * node + 1, mid + 1, r, ql, qr));
        }
    }

    // ---------------- 3. Lazy segment tree: range ADD, range SUM ----------------
    static class LazySegmentTree {
        final int n;
        final long[] tree, lazy;        // lazy[node] = an add still owed to node's CHILDREN

        LazySegmentTree(int n) {
            this.n = n;
            tree = new long[4 * n];
            lazy = new long[4 * n];
        }

        // Apply "add v to every element" to a whole node: its sum grows by v * length.
        private void apply(int node, int l, int r, long v) {
            tree[node] += v * (r - l + 1);
            lazy[node] += v;
        }

        // Before visiting the children, hand them the pending note.
        private void push(int node, int l, int r) {
            if (lazy[node] != 0) {
                int mid = (l + r) / 2;
                apply(2 * node, l, mid, lazy[node]);
                apply(2 * node + 1, mid + 1, r, lazy[node]);
                lazy[node] = 0;
            }
        }

        void add(int ql, int qr, long v) { add(1, 0, n - 1, ql, qr, v); }

        private void add(int node, int l, int r, int ql, int qr, long v) {
            if (qr < l || r < ql) return;
            if (ql <= l && r <= qr) { apply(node, l, r, v); return; }   // fully covered: just leave a note
            push(node, l, r);
            int mid = (l + r) / 2;
            add(2 * node, l, mid, ql, qr, v);
            add(2 * node + 1, mid + 1, r, ql, qr, v);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }

        long sum(int ql, int qr) { return sum(1, 0, n - 1, ql, qr); }

        private long sum(int node, int l, int r, int ql, int qr) {
            if (qr < l || r < ql) return 0;
            if (ql <= l && r <= qr) return tree[node];
            push(node, l, r);
            int mid = (l + r) / 2;
            return sum(2 * node, l, mid, ql, qr) + sum(2 * node + 1, mid + 1, r, ql, qr);
        }
    }

    // ---------------- 4. Fenwick tree (BIT): point add, prefix/range sum ----------------
    static class Fenwick {
        final long[] tree;               // 1-indexed: tree[0] unused

        Fenwick(int n) { tree = new long[n + 1]; }

        // add d to element i (0-indexed from the outside)
        void add(int i, long d) {
            for (int x = i + 1; x < tree.length; x += x & -x) tree[x] += d;   // climb: add the low bit
        }

        // sum of elements 0..i
        long prefix(int i) {
            long s = 0;
            for (int x = i + 1; x > 0; x -= x & -x) s += tree[x];            // descend: strip the low bit
            return s;
        }

        long sum(int l, int r) { return prefix(r) - (l > 0 ? prefix(l - 1) : 0); }
    }

    public static void main(String[] args) {
        int[] arr = {2, 1, 5, 3, 4, 7, 6};

        System.out.println("1. Segment tree (sum)");
        SegmentTree st = new SegmentTree(arr);
        check("sum(0..3) = 2+1+5+3", 11L, st.sum(0, 3));
        check("sum(2..5) = 5+3+4+7", 19L, st.sum(2, 5));
        st.update(3, 10);                                  // arr[3]: 3 -> 10
        check("after arr[3]=10, sum(2..5)", 26L, st.sum(2, 5));

        System.out.println("2. Segment tree (min)");
        MinSegmentTree mst = new MinSegmentTree(arr);
        check("min(2..6)", 3, mst.min(2, 6));
        check("min(4..6)", 4, mst.min(4, 6));

        System.out.println("3. Lazy segment tree (range add)");
        LazySegmentTree lz = new LazySegmentTree(8);
        lz.add(0, 7, 1);                                   // all ones
        lz.add(2, 5, 10);                                  // [1,1,11,11,11,11,1,1]
        check("sum(0..7)", 48L, lz.sum(0, 7));
        check("sum(4..6)", 23L, lz.sum(4, 6));

        System.out.println("4. Fenwick tree");
        Fenwick fw = new Fenwick(arr.length);
        for (int i = 0; i < arr.length; i++) fw.add(i, arr[i]);
        check("prefix(3)", 11L, fw.prefix(3));
        check("sum(2..5)", 19L, fw.sum(2, 5));
        System.out.println("   lowbit walk for prefix(6) (1-indexed 7 = 111b): tree[7] + tree[6] + tree[4]");

        System.out.println("5. 20,000 random ops: all structures vs a plain array");
        Random rnd = new Random(5);
        int n = 300;
        int[] a = new int[n];
        long[] b = new long[n];                            // for the lazy tree
        SegmentTree s = new SegmentTree(a);
        Fenwick f = new Fenwick(n);
        LazySegmentTree z = new LazySegmentTree(n);
        boolean ok = true;
        for (int t = 0; t < 20_000; t++) {
            int l = rnd.nextInt(n), r = l + rnd.nextInt(n - l);
            switch (rnd.nextInt(3)) {
                case 0 -> {                                 // point set
                    int v = rnd.nextInt(201) - 100;
                    f.add(l, v - a[l]);                     // Fenwick stores deltas
                    a[l] = v;
                    s.update(l, v);
                }
                case 1 -> {                                 // range add on the lazy tree
                    int v = rnd.nextInt(21) - 10;
                    z.add(l, r, v);
                    for (int i = l; i <= r; i++) b[i] += v;
                }
                default -> {
                    long exp = 0, expB = 0;
                    for (int i = l; i <= r; i++) { exp += a[i]; expB += b[i]; }
                    ok &= s.sum(l, r) == exp && f.sum(l, r) == exp && z.sum(l, r) == expB;
                }
            }
        }
        check("segment, Fenwick, lazy all correct", true, ok);

        System.out.println("6. Speed: 200,000 updates + 200,000 range sums on n = 200,000");
        n = 200_000;
        SegmentTree big = new SegmentTree(new int[n]);
        Fenwick bigF = new Fenwick(n);
        long t0 = System.nanoTime(), acc = 0;
        for (int t = 0; t < 200_000; t++) {
            int i = rnd.nextInt(n);
            big.update(i, t);
            acc += big.sum(rnd.nextInt(i + 1), i);
        }
        long segMs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        for (int t = 0; t < 200_000; t++) {
            int i = rnd.nextInt(n);
            bigF.add(i, t);
            acc += bigF.sum(rnd.nextInt(i + 1), i);
        }
        long fenMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("   segment tree %d ms, Fenwick %d ms  (a plain array would need ~2*10^10 steps)%n", segMs, fenMs);
        check("both finished", true, acc != 42);
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
