/*
 * ============================================================================
 *  EASY 03 : INVERT BINARY TREE              LeetCode #226
 *  https://leetcode.com/problems/invert-binary-tree/
 *  Pattern: VISIT EVERY NODE, SWAP ITS CHILDREN (any traversal works)
 * ============================================================================
 *
 *  PROBLEM
 *    Mirror the tree: every left child becomes the right child and vice versa.
 *
 *           4                    4
 *         /   \                /   \
 *        2     7     ->       7     2
 *       / \   / \            / \   / \
 *      1   3 6   9          9   6 3   1
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: "mirroring the whole tree" = at EVERY node, swap left and right.
 *  The order of visiting doesn't matter, as long as every node gets its swap once.
 *    recursive : swap, then invert both children (preorder)
 *    iterative : BFS/stack, swap each popped node's children
 *
 *  A COMMON BUG (inorder): invert(left); swap; invert(left) <- the old right is
 *  now on the left, so you must call invert(left) TWICE, not invert(right).
 *  Preorder or postorder avoids that confusion.
 *
 *  (Famous because the author of Homebrew failed it in a Google interview.)
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E03_InvertBinaryTree {

    static TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        TreeNode tmp = root.left;          // swap the children of this node
        root.left = root.right;
        root.right = tmp;
        invertTree(root.left);             // then fix both subtrees
        invertTree(root.right);
        return root;
    }

    static TreeNode invertIterative(TreeNode root) {
        Queue<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            TreeNode tmp = n.left; n.left = n.right; n.right = tmp;
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        return root;
    }

    public static void main(String[] args) {
        check("[4,2,7,1,3,6,9]", "[4, 7, 2, 9, 6, 3, 1]", invertTree(build(4, 2, 7, 1, 3, 6, 9)));
        check("[2,1,3]", "[2, 3, 1]", invertTree(build(2, 1, 3)));
        check("[1,2] (shape changes)", "[1, null, 2]", invertTree(build(1, 2)));
        check("[]", null, invertTree(null));
        check("iterative", "[4, 7, 2, 9, 6, 3, 1]", invertIterative(build(4, 2, 7, 1, 3, 6, 9)));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            long seed = rnd.nextLong();
            TreeNode a = randomTree(new Random(seed), 20, 0, 99), b = randomTree(new Random(seed), 20, 0, 99);
            String original = String.valueOf(a);
            ok &= String.valueOf(invertTree(a)).equals(String.valueOf(invertIterative(b)));
            ok &= String.valueOf(invertTree(a)).equals(original);   // inverting twice = the original
        }
        check("300 random: recursive == iterative, twice = identity", true, ok);
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
