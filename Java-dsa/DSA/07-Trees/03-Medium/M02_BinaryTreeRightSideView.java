/*
 * ============================================================================
 *  MEDIUM 02 : BINARY TREE RIGHT SIDE VIEW   LeetCode #199
 *  https://leetcode.com/problems/binary-tree-right-side-view/
 *  Pattern: LAST node of each BFS level  (or DFS right-first, first visit per depth)
 * ============================================================================
 *
 *  PROBLEM
 *    Standing on the right of the tree, which values do you see, top to bottom?
 *              1            <- see 1
 *             / \
 *            2   3          <- see 3
 *             \
 *              5            <- see 5  (nothing on the right at this depth hides it!)
 *    -> [1, 3, 5]
 *
 *  ---------------------------------------------------------------------------
 *  THE TRAP: "just follow the right children" gives [1, 3]. A node deep on the
 *  LEFT side is visible when the right side is shorter.
 *  What you see = the RIGHTMOST node of EACH LEVEL.
 *
 *  BFS: the level-order template (M01); record the last node of each level.
 *
 *  DFS: visit node, then RIGHT, then left, passing the depth. The FIRST node you
 *  reach at each depth is the rightmost one -> record it when depth == result.size().
 *
 *  DRY RUN DFS (picture)
 *    1 (depth 0, new) -> [1];  3 (depth 1, new) -> [1,3];  2 (depth 1, seen);
 *    5 (depth 2, new) -> [1,3,5]
 *
 *  COMPLEXITY   Time O(n)   Space O(h) DFS / O(width) BFS
 * ============================================================================
 */

import java.util.*;

public class M02_BinaryTreeRightSideView {

    static List<Integer> rightSideView(TreeNode root) {
        List<Integer> view = new ArrayList<>();
        if (root == null) return view;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                if (i == size - 1) view.add(n.val);         // the last one on this level
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
        }
        return view;
    }

    static List<Integer> rightSideViewDfs(TreeNode root) {
        List<Integer> view = new ArrayList<>();
        dfs(root, 0, view);
        return view;
    }

    static void dfs(TreeNode n, int depth, List<Integer> view) {
        if (n == null) return;
        if (depth == view.size()) view.add(n.val);         // first node seen at this depth
        dfs(n.right, depth + 1, view);                      // RIGHT first
        dfs(n.left, depth + 1, view);
    }

    public static void main(String[] args) {
        check("[1,2,3,null,5,null,4]", "[1, 3, 4]", rightSideView(build(1, 2, 3, null, 5, null, 4)));
        check("[1,2,3,null,5] (left node visible)", "[1, 3, 5]", rightSideView(build(1, 2, 3, null, 5)));
        check("[1,null,3]", "[1, 3]", rightSideView(build(1, null, 3)));
        check("[]", "[]", rightSideView(null));
        check("DFS version", "[1, 3, 5]", rightSideViewDfs(build(1, 2, 3, null, 5)));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 99);
            ok &= rightSideView(r).equals(rightSideViewDfs(r));
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
