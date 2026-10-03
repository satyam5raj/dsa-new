/*
 * ============================================================================
 *  EASY 06 : BALANCED BINARY TREE            LeetCode #110
 *  https://leetcode.com/problems/balanced-binary-tree/
 *  Pattern: BOTTOM-UP with a SENTINEL (-1 = "already unbalanced, stop")
 * ============================================================================
 *
 *  PROBLEM
 *    Height-balanced = for EVERY node, |height(left) - height(right)| <= 1.
 *      [3,9,20,null,null,15,7] -> true
 *      [1,2,2,3,3,null,null,4,4] -> false
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE (top-down)
 *    balanced(n) = |height(l) - height(r)| <= 1 && balanced(l) && balanced(r)
 *    height() is O(subtree), called again at every level -> O(n^2) on a chain,
 *    O(n log n) when balanced.
 *
 *  OPTIMAL (bottom-up, one pass)
 *    Compute the height and the balance check together. Return:
 *       the height if the subtree is balanced
 *       -1 if it is NOT  (heights are never negative, so -1 is a safe signal)
 *    At a node: if either child returned -1, or the heights differ by > 1 -> return -1.
 *    Each node is visited once -> O(n). The -1 also lets us stop early.
 *
 *  DRY RUN [1,2,2,3,3,null,null,4,4]
 *              1
 *             / \
 *            2   2
 *           / \
 *          3   3
 *         / \
 *        4   4
 *    h(4)=1, h(4)=1 -> h(3)=2;  h(3 right)=1;  node 2: |2-1| ok -> 3
 *    right 2 -> 1;  root: |3-1| = 2 > 1 -> -1 -> false
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E06_BalancedBinaryTree {

    static boolean isBalanced(TreeNode root) {
        return check(root) != -1;
    }

    // height in nodes, or -1 if the subtree is unbalanced somewhere
    static int check(TreeNode n) {
        if (n == null) return 0;
        int l = check(n.left);
        if (l == -1) return -1;                     // stop early: no need to look at the right side
        int r = check(n.right);
        if (r == -1 || Math.abs(l - r) > 1) return -1;
        return 1 + Math.max(l, r);
    }

    static boolean brute(TreeNode n) {
        if (n == null) return true;
        return Math.abs(height(n.left) - height(n.right)) <= 1 && brute(n.left) && brute(n.right);
    }

    static int height(TreeNode n) { return n == null ? 0 : 1 + Math.max(height(n.left), height(n.right)); }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", true, isBalanced(build(3, 9, 20, null, null, 15, 7)));
        check("[1,2,2,3,3,null,null,4,4]", false, isBalanced(build(1, 2, 2, 3, 3, null, null, 4, 4)));
        check("[]", true, isBalanced(null));
        // The root looks balanced (both sides height 2), but a child is not:
        check("[1,2,2,3,null,null,3,4,null,null,4]", false, isBalanced(build(1, 2, 2, 3, null, null, 3, 4, null, null, 4)));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(15), 0, 9);
            ok &= isBalanced(r) == brute(r);
        }
        check("1,000 random trees vs O(n^2)", true, ok);
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
