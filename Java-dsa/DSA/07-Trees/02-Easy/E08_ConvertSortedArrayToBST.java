/*
 * ============================================================================
 *  EASY 08 : CONVERT SORTED ARRAY TO BINARY SEARCH TREE   LeetCode #108
 *  https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/
 *  Pattern: DIVIDE AND CONQUER (the middle element is the root)
 * ============================================================================
 *
 *  PROBLEM
 *    A sorted array -> a HEIGHT-BALANCED BST.
 *      [-10,-3,0,5,9] -> [0,-3,9,-10,null,5]  (one valid answer)
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: in a BST, everything left of the root is smaller, everything right
 *  is larger. In a SORTED array, that's exactly "left of an index / right of it".
 *  Pick the MIDDLE element as the root: both halves get (almost) the same size,
 *  which makes the tree balanced. Then do the same for each half.
 *
 *    build(lo, hi):
 *      lo > hi -> null
 *      mid = (lo + hi) / 2
 *      node(nums[mid]), left = build(lo, mid-1), right = build(mid+1, hi)
 *
 *  DRY RUN [-10,-3,0,5,9]
 *    build(0,4): mid 2 -> 0
 *      build(0,1): mid 0 -> -10, right = build(1,1) -> -3
 *      build(3,4): mid 3 -> 5,   right = build(4,4) -> 9
 *    ->        0
 *            /   \
 *         -10     5
 *            \     \
 *            -3     9
 *  (LeetCode accepts any balanced answer; choosing mid = (lo+hi+1)/2 gives another.)
 *
 *  This is exactly how you'd rebuild a degenerate BST into a balanced one:
 *  inorder -> sorted list -> this function. (#1382 Balance a BST)
 *  COMPLEXITY   Time O(n)   Space O(log n) recursion
 * ============================================================================
 */

import java.util.*;

public class E08_ConvertSortedArrayToBST {

    static TreeNode sortedArrayToBST(int[] nums) {
        return build(nums, 0, nums.length - 1);
    }

    static TreeNode build(int[] nums, int lo, int hi) {
        if (lo > hi) return null;
        int mid = lo + (hi - lo) / 2;
        TreeNode node = new TreeNode(nums[mid]);
        node.left = build(nums, lo, mid - 1);
        node.right = build(nums, mid + 1, hi);
        return node;
    }

    static void inorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        inorder(n.left, out);
        out.add(n.val);
        inorder(n.right, out);
    }

    static int heightIfBalanced(TreeNode n) {        // -1 = unbalanced (E06)
        if (n == null) return 0;
        int l = heightIfBalanced(n.left), r = heightIfBalanced(n.right);
        if (l < 0 || r < 0 || Math.abs(l - r) > 1) return -1;
        return 1 + Math.max(l, r);
    }

    public static void main(String[] args) {
        check("[-10,-3,0,5,9]", "[0, -10, 5, null, -3, null, 9]", sortedArrayToBST(new int[]{-10, -3, 0, 5, 9}));
        check("[1,3]", "[1, null, 3]", sortedArrayToBST(new int[]{1, 3}));
        check("[] -> empty", null, sortedArrayToBST(new int[]{}));
        boolean ok = true;
        for (int n = 0; n <= 300; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = 3 * i - 50;          // sorted, distinct
            TreeNode r = sortedArrayToBST(a);
            List<Integer> in = new ArrayList<>();
            inorder(r, in);
            ok &= in.equals(Arrays.stream(a).boxed().toList());     // valid BST with the same keys
            ok &= heightIfBalanced(r) >= 0;                        // height-balanced
        }
        check("n = 0..300: BST order + balanced", true, ok);
        summary();
    }

    // ---------------- TreeNode + helpers (same in every tree file) ----------------
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) { this.val = val; this.left = left; this.right = right; }

        // LeetCode's level-order format, e.g. [1, 2, 3, null, 4]
        @Override public String toString() {
            List<String> out = new ArrayList<>();
            Queue<TreeNode> q = new LinkedList<>();          // LinkedList accepts null elements (ArrayDeque doesn't)
            q.add(this);
            while (!q.isEmpty()) {
                TreeNode n = q.poll();
                if (n == null) { out.add("null"); continue; }
                out.add(String.valueOf(n.val));
                q.add(n.left);
                q.add(n.right);
            }
            while (out.get(out.size() - 1).equals("null")) out.remove(out.size() - 1);
            return "[" + String.join(", ", out) + "]";
        }
    }

    // Build a tree from LeetCode's level-order format: build(3, 9, 20, null, null, 15, 7)
    static TreeNode build(Integer... vals) {
        if (vals.length == 0 || vals[0] == null) return null;
        TreeNode root = new TreeNode(vals[0]);
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        int i = 1;
        while (i < vals.length) {
            TreeNode n = q.poll();
            if (vals[i] != null) { n.left = new TreeNode(vals[i]); q.add(n.left); }
            i++;
            if (i < vals.length && vals[i] != null) { n.right = new TreeNode(vals[i]); q.add(n.right); }
            i++;
        }
        return root;
    }

    // A random tree shape with 'size' nodes and values in [lo, hi] (for brute-force cross-checks)
    static TreeNode randomTree(Random rnd, int size, int lo, int hi) {
        if (size == 0) return null;
        int leftSize = rnd.nextInt(size);
        TreeNode n = new TreeNode(lo + rnd.nextInt(hi - lo + 1));
        n.left = randomTree(rnd, leftSize, lo, hi);
        n.right = randomTree(rnd, size - 1 - leftSize, lo, hi);
        return n;
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
