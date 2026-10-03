/*
 * ============================================================================
 *  EASY 07 : PATH SUM                        LeetCode #112
 *  https://leetcode.com/problems/path-sum/
 *  Pattern: TOP-DOWN (pass the REMAINING target down)
 * ============================================================================
 *
 *  PROBLEM
 *    Is there a ROOT-TO-LEAF path whose values add up to targetSum?
 *              5
 *             / \
 *            4   8
 *           /   / \           target 22 -> true  (5 + 4 + 11 + 2)
 *          11  13  4
 *         /  \      \
 *        7    2      1
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: carry "how much is still needed" down the tree.
 *    hasPath(node, remaining):
 *      null           -> false (no path here)
 *      remaining -= node.val
 *      LEAF           -> remaining == 0
 *      otherwise      -> hasPath(left, remaining) || hasPath(right, remaining)
 *
 *  TRAPS
 *    - The path must END AT A LEAF. [1,2], target 1 -> false: node 1 alone is not
 *      a leaf (it has child 2). That's why the check is at the LEAF, not at null.
 *      (Checking "null && remaining == 0" would wrongly accept node 1 via its empty right side.)
 *    - Values can be negative, so you can't stop early when remaining < 0.
 *    - Empty tree -> false, even for target 0.
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E07_PathSum {

    static boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) return false;
        int remaining = targetSum - root.val;
        if (root.left == null && root.right == null) return remaining == 0;   // at a leaf: done?
        return hasPathSum(root.left, remaining) || hasPathSum(root.right, remaining);
    }

    // referee: collect every root-to-leaf sum
    static void sums(TreeNode n, int acc, Set<Integer> out) {
        if (n == null) return;
        acc += n.val;
        if (n.left == null && n.right == null) out.add(acc);
        sums(n.left, acc, out);
        sums(n.right, acc, out);
    }

    public static void main(String[] args) {
        TreeNode t = build(5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1);
        check("example, target 22", true, hasPathSum(t, 22));
        check("example, target 26 (5+8+13)", true, hasPathSum(t, 26));
        check("example, target 9 (not to a leaf)", false, hasPathSum(t, 9));
        check("[1,2] target 1 (1 is not a leaf)", false, hasPathSum(build(1, 2), 1));
        check("[] target 0", false, hasPathSum(null, 0));
        check("[-2,null,-3] target -5", true, hasPathSum(build(-2, null, -3), -5));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(12), -5, 5);
            Set<Integer> s = new HashSet<>();
            sums(r, 0, s);
            int target = rnd.nextInt(21) - 10;
            ok &= hasPathSum(r, target) == s.contains(target);
        }
        check("1,000 random trees vs all leaf sums", true, ok);
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
