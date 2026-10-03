/*
 * ============================================================================
 *  EASY 10 : MINIMUM DEPTH OF BINARY TREE    LeetCode #111
 *  https://leetcode.com/problems/minimum-depth-of-binary-tree/
 *  Pattern: BFS stops at the FIRST leaf  (and a classic DFS trap)
 * ============================================================================
 *
 *  PROBLEM
 *    The number of nodes on the SHORTEST root-to-LEAF path.
 *      [3,9,20,null,null,15,7] -> 2   (3 -> 9)
 *      [2,null,3,null,4]       -> 3   (a chain: the only leaf is 4)
 *
 *  ---------------------------------------------------------------------------
 *  THE TRAP: copying max depth with min:
 *      1 + min(minDepth(left), minDepth(right))       <- WRONG
 *  For node 2 in [2,null,3], left is null -> 0 -> answer 1. But node 2 is NOT a
 *  leaf, so "2" alone isn't a root-to-leaf path. Fix: if one child is missing,
 *  you MUST go down the other one:
 *      left == null  -> 1 + minDepth(right)
 *      right == null -> 1 + minDepth(left)
 *      both present  -> 1 + min(both)
 *
 *  BETTER: BFS. It explores level by level, so the FIRST leaf it meets is the
 *  shallowest one -> return immediately. On a big, lopsided tree, BFS may touch
 *  only a few nodes, while DFS always explores everything.
 *
 *  DRY RUN BFS [3,9,20,null,null,15,7]
 *    level 1: 3 (not a leaf)
 *    level 2: 9 -> leaf! -> return 2  (15 and 7 are never looked at)
 *
 *  COMPLEXITY   Time O(n) worst case   Space O(width) for BFS
 * ============================================================================
 */

import java.util.*;

public class E10_MinimumDepthOfBinaryTree {

    static int minDepth(TreeNode root) {
        if (root == null) return 0;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        int depth = 1;
        while (!q.isEmpty()) {
            for (int i = q.size(); i > 0; i--) {
                TreeNode n = q.poll();
                if (n.left == null && n.right == null) return depth;   // first leaf = shallowest
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            depth++;
        }
        return depth;   // unreachable: a non-empty tree always has a leaf
    }

    static int minDepthDfs(TreeNode n) {
        if (n == null) return 0;
        if (n.left == null) return 1 + minDepthDfs(n.right);     // must go right
        if (n.right == null) return 1 + minDepthDfs(n.left);     // must go left
        return 1 + Math.min(minDepthDfs(n.left), minDepthDfs(n.right));
    }

    static int wrongDfs(TreeNode n) {                             // the trap, for comparison
        if (n == null) return 0;
        return 1 + Math.min(wrongDfs(n.left), wrongDfs(n.right));
    }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", 2, minDepth(build(3, 9, 20, null, null, 15, 7)));
        check("[2,null,3,null,4,null,5,null,6]", 5, minDepth(build(2, null, 3, null, 4, null, 5, null, 6)));
        check("[]", 0, minDepth(null));
        check("DFS (correct version)", 5, minDepthDfs(build(2, null, 3, null, 4, null, 5, null, 6)));
        check("the WRONG version says", 1, wrongDfs(build(2, null, 3, null, 4, null, 5, null, 6)));
        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(30), 0, 9);
            ok &= minDepth(r) == minDepthDfs(r);
        }
        check("500 random trees: BFS == DFS", true, ok);
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
