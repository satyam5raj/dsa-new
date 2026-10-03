/*
 * ============================================================================
 *  MEDIUM 08 : COUNT GOOD NODES IN BINARY TREE   LeetCode #1448
 *  https://leetcode.com/problems/count-good-nodes-in-binary-tree/
 *  Pattern: TOP-DOWN, pass the "max so far" on the path
 * ============================================================================
 *
 *  PROBLEM
 *    A node X is GOOD if no node on the path from the root to X has a value
 *    GREATER than X. Count the good nodes.
 *              3*
 *             / \
 *            1   4*         good: 3, 4, 3 (the leftmost leaf), 5 -> 4
 *           /   / \
 *          3*  1   5*
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: "good" only depends on the maximum value ABOVE the node on its path.
 *  Pass that maximum DOWN (top-down / preorder):
 *     good(node, maxAbove):
 *        isGood = node.val >= maxAbove
 *        newMax = max(maxAbove, node.val)
 *        return (isGood ? 1 : 0) + good(left, newMax) + good(right, newMax)
 *  Start with maxAbove = -infinity (the root is always good).
 *
 *  DRY RUN
 *    3 (max -inf) good -> max 3
 *      1 (3) not good -> max 3 -> 3 (3) good (>= counts)
 *      4 (3) good -> max 4 -> 1 (4) no, 5 (4) good
 *    total 4
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M08_CountGoodNodes {

    static int goodNodes(TreeNode root) {
        return count(root, Integer.MIN_VALUE);
    }

    static int count(TreeNode n, int maxAbove) {
        if (n == null) return 0;
        int good = n.val >= maxAbove ? 1 : 0;
        int newMax = Math.max(maxAbove, n.val);
        return good + count(n.left, newMax) + count(n.right, newMax);
    }

    // referee: carry the whole path and check it explicitly
    static int brute(TreeNode n, List<Integer> path) {
        if (n == null) return 0;
        boolean good = path.stream().allMatch(v -> v <= n.val);
        path.add(n.val);
        int c = (good ? 1 : 0) + brute(n.left, path) + brute(n.right, path);
        path.remove(path.size() - 1);
        return c;
    }

    public static void main(String[] args) {
        check("[3,1,4,3,null,1,5]", 4, goodNodes(build(3, 1, 4, 3, null, 1, 5)));
        check("[3,3,null,4,2]", 3, goodNodes(build(3, 3, null, 4, 2)));
        check("[1]", 1, goodNodes(build(1)));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(25), -5, 5);
            ok &= goodNodes(r) == brute(r, new ArrayList<>());
        }
        check("1,000 random trees vs full-path check", true, ok);
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
