/*
 * ============================================================================
 *  EASY 04 : SYMMETRIC TREE                  LeetCode #101
 *  https://leetcode.com/problems/symmetric-tree/
 *  Pattern: TWO TREES IN LOCKSTEP (the left and right subtrees, crossed over)
 * ============================================================================
 *
 *  PROBLEM
 *    Is the tree a mirror image of itself (around its centre)?
 *           1                       1
 *         /   \                   /   \
 *        2     2      -> true    2     2     -> false
 *       / \   / \                 \     \
 *      3   4 4   3                 3     3
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: the tree is symmetric  <=>  root.left is a MIRROR of root.right.
 *  Two subtrees a and b are mirrors when:
 *    a.val == b.val
 *    a.left  mirrors b.right   (the OUTER pair)
 *    a.right mirrors b.left    (the INNER pair)
 *  Same as Same Tree (E02), except the children are compared CROSSWISE.
 *
 *  DRY RUN (left picture)
 *    mirror(2, 2): equal -> mirror(3, 3) outer ✓  and  mirror(4, 4) inner ✓ -> true
 *
 *  ITERATIVE: push PAIRS into a queue: (a.left, b.right) and (a.right, b.left).
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E04_SymmetricTree {

    static boolean isSymmetric(TreeNode root) {
        return root == null || mirror(root.left, root.right);
    }

    static boolean mirror(TreeNode a, TreeNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.val == b.val && mirror(a.left, b.right) && mirror(a.right, b.left);
    }

    static boolean isSymmetricIterative(TreeNode root) {
        if (root == null) return true;
        Queue<TreeNode> q = new LinkedList<>();          // LinkedList: we enqueue nulls on purpose
        q.add(root.left);
        q.add(root.right);
        while (!q.isEmpty()) {
            TreeNode a = q.poll(), b = q.poll();         // always taken as a pair
            if (a == null && b == null) continue;
            if (a == null || b == null || a.val != b.val) return false;
            q.add(a.left);  q.add(b.right);              // outer pair
            q.add(a.right); q.add(b.left);               // inner pair
        }
        return true;
    }

    public static void main(String[] args) {
        check("[1,2,2,3,4,4,3]", true, isSymmetric(build(1, 2, 2, 3, 4, 4, 3)));
        check("[1,2,2,null,3,null,3]", false, isSymmetric(build(1, 2, 2, null, 3, null, 3)));
        check("[1]", true, isSymmetric(build(1)));
        check("[1,2,3] (values differ)", false, isSymmetric(build(1, 2, 3)));
        check("iterative", true, isSymmetricIterative(build(1, 2, 2, 3, 4, 4, 3)));
        Random rnd = new Random(4);
        boolean ok = true;
        int symmetricCount = 0;
        for (int t = 0; t < 2000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(8), 0, 1);
            boolean s = isSymmetric(r);
            if (s) symmetricCount++;
            ok &= s == isSymmetricIterative(r);
            ok &= s == equalsItsMirror(r);
        }
        check("2,000 random: rec == iter == (tree == its mirror)", true, ok);
        System.out.println("   (" + symmetricCount + " of them were symmetric)");
        summary();
    }

    // Referee: a tree is symmetric exactly when it equals its own mirror image.
    static boolean equalsItsMirror(TreeNode r) {
        return String.valueOf(r).equals(String.valueOf(mirrorCopy(r)));
    }

    static TreeNode mirrorCopy(TreeNode n) {
        return n == null ? null : new TreeNode(n.val, mirrorCopy(n.right), mirrorCopy(n.left));
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
