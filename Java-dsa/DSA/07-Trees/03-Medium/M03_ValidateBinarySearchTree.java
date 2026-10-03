/*
 * ============================================================================
 *  MEDIUM 03 : VALIDATE BINARY SEARCH TREE   LeetCode #98
 *  https://leetcode.com/problems/validate-binary-search-tree/
 *  Pattern: TOP-DOWN with an allowed RANGE (low, high)  -or-  INORDER is increasing
 * ============================================================================
 *
 *  PROBLEM
 *    Is it a valid BST? (left subtree < node < right subtree, strictly, everywhere)
 *      [2,1,3]               -> true
 *      [5,1,4,null,null,3,6] -> false (4 is in 5's RIGHT subtree but 4 < 5)
 *
 *  ---------------------------------------------------------------------------
 *  THE CLASSIC WRONG ANSWER: only compare each node with its own children.
 *              5
 *             / \
 *            4   6         every parent/child pair looks fine,
 *               / \        but 3 is in the RIGHT subtree of 5 and 3 < 5 -> NOT a BST
 *              3   7
 *  The rule is about whole SUBTREES, not just children.
 *
 *  APPROACH 1: pass down the allowed RANGE (top-down)
 *    valid(node, low, high): low < node.val < high
 *      going LEFT : the node becomes the new upper bound -> valid(left, low, node.val)
 *      going RIGHT: the node becomes the new lower bound -> valid(right, node.val, high)
 *    Start with (-inf, +inf). Use long or null bounds: a node can hold Integer.MAX_VALUE.
 *
 *  APPROACH 2: INORDER must be STRICTLY increasing
 *    Do an inorder traversal remembering the previous value; any prev >= cur -> false.
 *
 *  DRY RUN approach 1 on the picture
 *    5 in (-inf, inf) ✓ -> 4 in (-inf, 5) ✓ -> 6 in (5, inf) ✓ -> 3 in (5, 6) ✗ -> false
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M03_ValidateBinarySearchTree {

    static boolean isValidBST(TreeNode root) {
        return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    static boolean valid(TreeNode n, long low, long high) {
        if (n == null) return true;
        if (n.val <= low || n.val >= high) return false;
        return valid(n.left, low, n.val) && valid(n.right, n.val, high);
    }

    static TreeNode prev;

    static boolean isValidBSTInorder(TreeNode root) {
        prev = null;
        return inorder(root);
    }

    static boolean inorder(TreeNode n) {
        if (n == null) return true;
        if (!inorder(n.left)) return false;
        if (prev != null && prev.val >= n.val) return false;     // must strictly increase
        prev = n;
        return inorder(n.right);
    }

    static boolean wrongChildrenOnly(TreeNode n) {               // the classic mistake
        if (n == null) return true;
        if (n.left != null && n.left.val >= n.val) return false;
        if (n.right != null && n.right.val <= n.val) return false;
        return wrongChildrenOnly(n.left) && wrongChildrenOnly(n.right);
    }

    // referee: collect inorder, check strictly increasing
    static boolean referee(TreeNode r) {
        List<Integer> in = new ArrayList<>();
        collect(r, in);
        for (int i = 1; i < in.size(); i++) if (in.get(i - 1) >= in.get(i)) return false;
        return true;
    }

    static void collect(TreeNode n, List<Integer> out) {
        if (n == null) return;
        collect(n.left, out);
        out.add(n.val);
        collect(n.right, out);
    }

    public static void main(String[] args) {
        check("[2,1,3]", true, isValidBST(build(2, 1, 3)));
        check("[5,1,4,null,null,3,6]", false, isValidBST(build(5, 1, 4, null, null, 3, 6)));
        TreeNode trap = build(5, 4, 6, null, null, 3, 7);
        check("[5,4,6,null,null,3,7] (deep violation)", false, isValidBST(trap));
        check("  ...the children-only check says", true, wrongChildrenOnly(trap));
        check("[2,2,2] (duplicates not allowed)", false, isValidBST(build(2, 2, 2)));
        check("[2147483647] (MAX_VALUE node)", true, isValidBST(build(Integer.MAX_VALUE)));
        check("inorder version", false, isValidBSTInorder(trap));
        Random rnd = new Random(3);
        boolean ok = true;
        int valid = 0;
        for (int t = 0; t < 3000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(7), 0, 9);
            boolean exp = referee(r);
            if (exp) valid++;
            ok &= isValidBST(r) == exp && isValidBSTInorder(r) == exp;
        }
        check("3,000 random trees: both approaches", true, ok);
        System.out.println("   (" + valid + " of them were valid BSTs)");
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
