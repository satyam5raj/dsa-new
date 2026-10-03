/*
 * ============================================================================
 *  EASY 02 : SAME TREE                       LeetCode #100
 *  https://leetcode.com/problems/same-tree/
 *  Pattern: TWO TREES IN LOCKSTEP
 * ============================================================================
 *
 *  PROBLEM
 *    Are two trees identical: same shape AND same values?
 *      [1,2,3] vs [1,2,3]    -> true
 *      [1,2]   vs [1,null,2] -> false   (2 is a left child in one, a right child in the other)
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT COMPARE TRAVERSALS? [1,2] and [1,null,2] have the SAME preorder (1 2).
 *  A single traversal loses the shape (unless you also record the nulls).
 *
 *  RECURSION on PAIRS of nodes (p, q):
 *    both null        -> true   (two empty trees are the same)
 *    exactly one null -> false  (shapes differ)
 *    values differ    -> false
 *    otherwise        -> same(p.left, q.left) && same(p.right, q.right)
 *  The three base cases above are the template for every "two trees" problem:
 *  Symmetric Tree (E04), Subtree of Another Tree (E09), Merge Two Trees...
 *
 *  DRY RUN [1,2] vs [1,null,2]
 *    (1,1) equal -> (2, null): one null -> false
 *
 *  COMPLEXITY   Time O(min(n, m))   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E02_SameTree {

    static boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null && q == null) return true;
        if (p == null || q == null) return false;
        return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }

    public static void main(String[] args) {
        check("[1,2,3] vs [1,2,3]", true, isSameTree(build(1, 2, 3), build(1, 2, 3)));
        check("[1,2] vs [1,null,2]", false, isSameTree(build(1, 2), build(1, null, 2)));
        check("[1,2,1] vs [1,1,2]", false, isSameTree(build(1, 2, 1), build(1, 1, 2)));
        check("both empty", true, isSameTree(null, null));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode a = randomTree(rnd, rnd.nextInt(6), 0, 1), b = randomTree(rnd, rnd.nextInt(6), 0, 1);
            // the level-order string (with nulls) describes a tree exactly, so it's a fair referee
            ok &= isSameTree(a, b) == String.valueOf(a).equals(String.valueOf(b));
        }
        check("500 random pairs vs serialization", true, ok);
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
