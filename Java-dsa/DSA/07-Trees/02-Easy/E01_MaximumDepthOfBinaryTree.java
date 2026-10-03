/*
 * ============================================================================
 *  EASY 01 : MAXIMUM DEPTH OF BINARY TREE    LeetCode #104
 *  https://leetcode.com/problems/maximum-depth-of-binary-tree/
 *  Pattern: BOTTOM-UP recursion (the "hello world" of trees)
 * ============================================================================
 *
 *  PROBLEM
 *    The number of NODES on the longest root-to-leaf path.
 *      [3,9,20,null,null,15,7] -> 3        [] -> 0
 *
 *  ---------------------------------------------------------------------------
 *  THINK RECURSIVELY (the checklist from 01-Basics/TreeRecursionPatterns)
 *    1. depth(node) = the max depth of the subtree rooted at node
 *    2. depth(null) = 0
 *    3. ASSUME depth(left) and depth(right) are correct. Then:
 *         depth(node) = 1 + max(depth(left), depth(right))
 *    That's the whole solution. Don't trace the whole recursion in your head:
 *    trust that it works for the children (the "leap of faith").
 *
 *  DRY RUN        3          depth(9) = 1 + max(0,0) = 1
 *               /   \        depth(15) = depth(7) = 1
 *              9     20      depth(20) = 1 + max(1,1) = 2
 *                   /  \     depth(3)  = 1 + max(1,2) = 3
 *                  15   7
 *
 *  ALTERNATIVE: BFS and count the levels (no recursion -> no stack overflow on
 *  very deep trees).
 *
 *  COMPLEXITY   Time O(n)   Space O(h) recursion (h = n in the worst case)
 * ============================================================================
 */

import java.util.*;

public class E01_MaximumDepthOfBinaryTree {

    static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    static int maxDepthBfs(TreeNode root) {
        if (root == null) return 0;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        int levels = 0;
        while (!q.isEmpty()) {
            for (int i = q.size(); i > 0; i--) {          // one full level per outer loop
                TreeNode n = q.poll();
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            levels++;
        }
        return levels;
    }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", 3, maxDepth(build(3, 9, 20, null, null, 15, 7)));
        check("[1,null,2]", 2, maxDepth(build(1, null, 2)));
        check("[] (empty)", 0, maxDepth(null));
        check("BFS version", 3, maxDepthBfs(build(3, 9, 20, null, null, 15, 7)));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 9);
            ok &= maxDepth(r) == maxDepthBfs(r);
        }
        check("300 random trees: DFS == BFS", true, ok);
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
