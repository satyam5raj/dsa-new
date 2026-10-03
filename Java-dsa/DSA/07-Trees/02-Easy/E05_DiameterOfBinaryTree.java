/*
 * ============================================================================
 *  EASY 05 : DIAMETER OF BINARY TREE         LeetCode #543
 *  https://leetcode.com/problems/diameter-of-binary-tree/
 *  Pattern: "RETURN ONE THING, TRACK ANOTHER" (return height, record the best path)
 * ============================================================================
 *
 *  PROBLEM
 *    The LENGTH (in edges) of the longest path between ANY two nodes.
 *    The path does NOT have to pass through the root.
 *            1
 *           / \
 *          2   3        -> 3   (4 -> 2 -> 1 -> 3, or 5 -> 2 -> 1 -> 3)
 *         / \
 *        4   5
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for every node, compute height(left) + height(right), each height
 *  computation being O(n) -> O(n^2) total.
 *
 *  KEY INSIGHT: every path has one HIGHEST node where it "bends". The longest path
 *  bending at node n has length  height(n.left) + height(n.right)  (in nodes = edges here).
 *  We compute heights bottom-up anyway, so check every bend while we're at it:
 *    depth(n) RETURNS: the height in nodes (what the PARENT needs: one arm)
 *    and RECORDS:      best = max(best, leftDepth + rightDepth)  (both arms)
 *  This is THE pattern for: Max Path Sum (H02), Longest Univalue Path, Cameras (H04).
 *
 *  DRY RUN (picture above)
 *    depth(4) = depth(5) = 1  (best stays 0)
 *    depth(2): l=1, r=1 -> best = 2, return 2
 *    depth(3) = 1
 *    depth(1): l=2, r=1 -> best = 3, return 3     -> answer 3
 *
 *  A path not through the root:   root's left child has two deep branches,
 *  root's right side is empty -> the best bend is at the child. Tested below.
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E05_DiameterOfBinaryTree {

    static int best;

    static int diameterOfBinaryTree(TreeNode root) {
        best = 0;
        depth(root);
        return best;
    }

    // height in NODES of the subtree; updates 'best' with the path bending here
    static int depth(TreeNode n) {
        if (n == null) return 0;
        int l = depth(n.left), r = depth(n.right);
        best = Math.max(best, l + r);
        return 1 + Math.max(l, r);
    }

    // O(n^2) brute force: try every node as the bend
    static int brute(TreeNode n) {
        if (n == null) return 0;
        int through = height(n.left) + height(n.right);
        return Math.max(through, Math.max(brute(n.left), brute(n.right)));
    }

    static int height(TreeNode n) { return n == null ? 0 : 1 + Math.max(height(n.left), height(n.right)); }

    public static void main(String[] args) {
        check("[1,2,3,4,5]", 3, diameterOfBinaryTree(build(1, 2, 3, 4, 5)));
        check("[1,2]", 1, diameterOfBinaryTree(build(1, 2)));
        check("[1] (single node)", 0, diameterOfBinaryTree(build(1)));
        //        1
        //       /
        //      2          best path 5-4-3-2-6-7-8 bends at 2, not the root
        //     / \
        //    3   6
        //   /     \
        //  4       7
        // /         \
        //5           8
        check("bend below the root", 6, diameterOfBinaryTree(build(1, 2, null, 3, 6, 4, null, null, 7, 5, null, null, 8)));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 9);
            ok &= diameterOfBinaryTree(r) == brute(r);
        }
        check("500 random trees vs O(n^2)", true, ok);
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
