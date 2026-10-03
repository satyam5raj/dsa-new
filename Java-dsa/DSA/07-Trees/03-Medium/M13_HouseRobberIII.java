/*
 * ============================================================================
 *  MEDIUM 13 : HOUSE ROBBER III              LeetCode #337
 *  https://leetcode.com/problems/house-robber-iii/
 *  Pattern: TREE DP - every node returns a PAIR {rob me, skip me}
 * ============================================================================
 *
 *  PROBLEM
 *    Houses form a binary tree. You can't rob two DIRECTLY connected houses
 *    (a parent and its child). Maximize the money.
 *              3*
 *             / \
 *            2   3          -> 7   (rob 3 + 3 + 1: the root and both grandchildren)
 *             \   \
 *              3*  1*
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY "rob every other level" FAILS: e.g. [4,1,null,2,null,3] - best is 4 + 3
 *  (levels 0 and 3), which no alternating-level rule picks.
 *
 *  NAIVE RECURSION: rob(node) = max(
 *       node.val + rob(4 grandchildren),      // take node -> children are off-limits
 *       rob(left) + rob(right) )              // skip node
 *    Grandchildren get recomputed many times -> exponential. A memo map fixes it.
 *
 *  CLEAN TREE DP: return TWO numbers per node (postorder):
 *       withNode    = node.val + left.without + right.without
 *       withoutNode = max(left.with, left.without) + max(right.with, right.without)
 *    (If we skip a node, each child is free to be robbed or not - take the better.)
 *    Answer: max(root.with, root.without). Each node computed once.
 *
 *  DRY RUN
 *    leaf 3 -> {3, 0}; leaf 1 -> {1, 0}
 *    node 2 (right child 3): with = 2 + 0 = 2, without = max(3,0) = 3 -> {2, 3}
 *    node 3 (right child 1): with = 3 + 0 = 3, without = 1           -> {3, 1}
 *    root 3: with = 3 + 3 + 1 = 7, without = max(2,3) + max(3,1) = 6 -> 7
 *
 *  This "return {take, skip}" is how you do DP on trees in general
 *  (max independent set, Binary Tree Cameras H04 uses 3 states).
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M13_HouseRobberIII {

    static int rob(TreeNode root) {
        int[] r = dp(root);
        return Math.max(r[0], r[1]);
    }

    // returns {best if this node IS robbed, best if it is NOT}
    static int[] dp(TreeNode n) {
        if (n == null) return new int[]{0, 0};
        int[] l = dp(n.left), r = dp(n.right);
        int with = n.val + l[1] + r[1];                                  // children must be skipped
        int without = Math.max(l[0], l[1]) + Math.max(r[0], r[1]);       // children free
        return new int[]{with, without};
    }

    // brute force: try every subset of nodes with no parent-child pair (small trees)
    static int brute(TreeNode root) {
        List<TreeNode> nodes = new ArrayList<>();
        Map<TreeNode, TreeNode> parent = new HashMap<>();
        collect(root, null, nodes, parent);
        int best = 0;
        for (int mask = 0; mask < (1 << nodes.size()); mask++) {
            Set<TreeNode> chosen = new HashSet<>();
            int sum = 0;
            for (int i = 0; i < nodes.size(); i++) if ((mask >> i & 1) == 1) { chosen.add(nodes.get(i)); sum += nodes.get(i).val; }
            boolean ok = true;
            for (TreeNode c : chosen) if (parent.get(c) != null && chosen.contains(parent.get(c))) { ok = false; break; }
            if (ok) best = Math.max(best, sum);
        }
        return best;
    }

    static void collect(TreeNode n, TreeNode p, List<TreeNode> nodes, Map<TreeNode, TreeNode> parent) {
        if (n == null) return;
        nodes.add(n);
        parent.put(n, p);
        collect(n.left, n, nodes, parent);
        collect(n.right, n, nodes, parent);
    }

    public static void main(String[] args) {
        check("[3,2,3,null,3,null,1]", 7, rob(build(3, 2, 3, null, 3, null, 1)));
        check("[3,4,5,1,3,null,1]", 9, rob(build(3, 4, 5, 1, 3, null, 1)));
        check("[4,1,null,2,null,3] (not alternating)", 7, rob(build(4, 1, null, 2, null, 3)));
        check("[]", 0, rob(null));
        Random rnd = new Random(13);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(12), 0, 20);
            ok &= rob(r) == brute(r);
        }
        check("500 random trees vs all subsets", true, ok);
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
