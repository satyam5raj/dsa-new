/*
 * ============================================================================
 *  EASY 11 : COUSINS IN BINARY TREE          LeetCode #993
 *  https://leetcode.com/problems/cousins-in-binary-tree/
 *  Pattern: record (DEPTH, PARENT) for the two targets
 * ============================================================================
 *
 *  PROBLEM
 *    Values are unique. x and y are COUSINS if they are at the SAME depth but
 *    have DIFFERENT parents.
 *              1
 *             / \
 *            2   3          x=5, y=4 -> true  (depth 2, parents 3 and 2)
 *             \   \         x=2, y=3 -> false (same parent: they're siblings)
 *              4   5
 *
 *  ---------------------------------------------------------------------------
 *  DFS: walk the tree passing (depth, parent) down (top-down). When you meet x
 *  or y, write down its depth and parent. At the end compare:
 *      depthX == depthY && parentX != parentY
 *
 *  BFS ALTERNATIVE: process one level at a time.
 *    - If both x and y appear on this level -> cousins, UNLESS they are children
 *      of the same node (check that when looking at each node's two children).
 *    - If only one appears -> different depths -> false.
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class E11_CousinsInBinaryTree {

    static int depthX, depthY;
    static TreeNode parentX, parentY;

    static boolean isCousins(TreeNode root, int x, int y) {
        depthX = depthY = -1;
        parentX = parentY = null;
        dfs(root, null, 0, x, y);
        return depthX == depthY && depthX != -1 && parentX != parentY;
    }

    static void dfs(TreeNode n, TreeNode parent, int depth, int x, int y) {
        if (n == null) return;
        if (n.val == x) { depthX = depth; parentX = parent; }
        if (n.val == y) { depthY = depth; parentY = parent; }
        dfs(n.left, n, depth + 1, x, y);
        dfs(n.right, n, depth + 1, x, y);
    }

    static boolean isCousinsBfs(TreeNode root, int x, int y) {
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            boolean seenX = false, seenY = false;
            for (int i = q.size(); i > 0; i--) {
                TreeNode n = q.poll();
                if (n.val == x) seenX = true;
                if (n.val == y) seenY = true;
                if (n.left != null && n.right != null) {          // siblings check
                    int a = n.left.val, b = n.right.val;
                    if ((a == x && b == y) || (a == y && b == x)) return false;
                }
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            if (seenX && seenY) return true;
            if (seenX || seenY) return false;                    // only one on this level
        }
        return false;
    }

    public static void main(String[] args) {
        TreeNode t = build(1, 2, 3, null, 4, null, 5);
        check("x=5, y=4", true, isCousins(t, 5, 4));
        check("x=2, y=3 (siblings)", false, isCousins(t, 2, 3));
        check("x=4, y=3 (different depths)", false, isCousins(build(1, 2, 3, 4), 4, 3));
        check("BFS x=5, y=4", true, isCousinsBfs(t, 5, 4));
        // random check: unique values 1..n with a random shape
        Random rnd = new Random(11);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            int n = 2 + rnd.nextInt(12);
            TreeNode r = randomTree(rnd, n, 0, 0);
            int[] next = {1};
            relabel(r, next);
            int x = 1 + rnd.nextInt(n), y = 1 + rnd.nextInt(n);
            if (x == y) continue;
            ok &= isCousins(r, x, y) == isCousinsBfs(r, x, y);
        }
        check("1,000 random trees: DFS == BFS", true, ok);
        summary();
    }

    static void relabel(TreeNode n, int[] next) {      // give every node a unique value
        if (n == null) return;
        n.val = next[0]++;
        relabel(n.left, next);
        relabel(n.right, next);
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
