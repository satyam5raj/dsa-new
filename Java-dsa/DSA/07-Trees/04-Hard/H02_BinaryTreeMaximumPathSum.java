/*
 * ============================================================================
 *  HARD 02 : BINARY TREE MAXIMUM PATH SUM    LeetCode #124
 *  https://leetcode.com/problems/binary-tree-maximum-path-sum/
 *  Pattern: "RETURN ONE THING, TRACK ANOTHER" + drop negative branches
 * ============================================================================
 *
 *  PROBLEM
 *    A path = any sequence of connected nodes, each used at most once (it can
 *    start and end anywhere, it need not pass through the root). Values can be
 *    NEGATIVE. Return the maximum path sum (at least one node).
 *            -10
 *            /  \
 *           9    20          -> 42   (15 -> 20 -> 7)
 *               /  \
 *              15   7
 *
 *  ---------------------------------------------------------------------------
 *  This is Diameter (E05) with values instead of edge counts, plus negatives.
 *
 *  Every path has a HIGHEST node where it bends. The best path bending at node n:
 *       n.val + (best downward chain from n.left, or 0 if that's negative)
 *             + (best downward chain from n.right, or 0 if that's negative)
 *
 *  So gain(n) RETURNS the best DOWNWARD chain starting at n (one arm only - a
 *  parent can only extend a straight chain, not a "V" shape):
 *       left  = max(0, gain(n.left))      <- a negative arm: just don't take it
 *       right = max(0, gain(n.right))
 *       best  = max(best, n.val + left + right)     <- RECORD the V through n
 *       return n.val + max(left, right)             <- RETURN one arm
 *
 *  DRY RUN
 *    gain(9) = 9 (best 9);  gain(15) = 15, gain(7) = 7
 *    gain(20): l 15, r 7 -> best = 20+15+7 = 42, return 35
 *    gain(-10): l 9, r 35 -> candidate -10+9+35 = 34 (best stays 42), return 25
 *
 *  TRAPS
 *    - all values negative: [-3] -> -3. Start best at MIN_VALUE, not 0.
 *    - never clamp the NODE itself to 0; only the arms are optional.
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class H02_BinaryTreeMaximumPathSum {

    static int best;

    static int maxPathSum(TreeNode root) {
        best = Integer.MIN_VALUE;
        gain(root);
        return best;
    }

    static int gain(TreeNode n) {
        if (n == null) return 0;
        int left = Math.max(0, gain(n.left));            // skip a branch that only loses
        int right = Math.max(0, gain(n.right));
        best = Math.max(best, n.val + left + right);    // the path bending at n
        return n.val + Math.max(left, right);           // a parent can use only one arm
    }

    // brute force: sum the path between EVERY pair of nodes (through their LCA)
    static int brute(TreeNode root) {
        List<TreeNode> nodes = new ArrayList<>();
        Map<TreeNode, TreeNode> parent = new HashMap<>();
        collect(root, null, nodes, parent);
        int bestSum = Integer.MIN_VALUE;
        for (TreeNode a : nodes) {
            for (TreeNode b : nodes) {
                // walk a up to the root, remembering the running sums; then walk b up until we hit a's path
                Map<TreeNode, Integer> sumUpFromA = new HashMap<>();
                int s = 0;
                for (TreeNode x = a; x != null; x = parent.get(x)) { s += x.val; sumUpFromA.put(x, s); }
                int t = 0;
                for (TreeNode y = b; y != null; y = parent.get(y)) {
                    if (sumUpFromA.containsKey(y)) { bestSum = Math.max(bestSum, sumUpFromA.get(y) + t); break; }
                    t += y.val;
                }
            }
        }
        return bestSum;
    }

    static void collect(TreeNode n, TreeNode p, List<TreeNode> nodes, Map<TreeNode, TreeNode> parent) {
        if (n == null) return;
        nodes.add(n);
        parent.put(n, p);
        collect(n.left, n, nodes, parent);
        collect(n.right, n, nodes, parent);
    }

    public static void main(String[] args) {
        check("[1,2,3]", 6, maxPathSum(build(1, 2, 3)));
        check("[-10,9,20,null,null,15,7]", 42, maxPathSum(build(-10, 9, 20, null, null, 15, 7)));
        check("[-3] (all negative)", -3, maxPathSum(build(-3)));
        check("[-2,-1] (best is a single node)", -1, maxPathSum(build(-2, -1)));
        check("[2,-1] (drop the negative arm)", 2, maxPathSum(build(2, -1)));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            TreeNode r = randomTree(rnd, 1 + rnd.nextInt(20), -10, 10);
            ok &= maxPathSum(r) == brute(r);
        }
        check("1,000 random trees vs all pairs", true, ok);
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
