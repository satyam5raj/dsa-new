/*
 * ============================================================================
 *  MEDIUM 05 : LOWEST COMMON ANCESTOR OF A BINARY TREE   LeetCode #236
 *  https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/
 *  Pattern: BOTTOM-UP "what did you find down there?"
 * ============================================================================
 *
 *  PROBLEM
 *    The LCA of nodes p and q is the DEEPEST node that has both p and q as
 *    descendants (a node counts as a descendant of itself).
 *                3
 *              /   \
 *             5     1           LCA(5, 1) = 3
 *            / \   / \          LCA(5, 4) = 5   (5 is an ancestor of 4)
 *           6   2 0   8         LCA(6, 4) = 5
 *              / \
 *             7   4
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: find the root-to-p path and the root-to-q path (lists), then
 *  walk both from the root; the last node they share is the LCA. O(n), 2 passes.
 *
 *  ONE-PASS RECURSION: lca(node) returns
 *     p or q        if it finds one of them in this subtree (or "the answer" if found)
 *     null          if neither is in this subtree
 *  At a node:
 *     node is null, p or q  -> return node   (found something, or nothing)
 *     L = lca(left), R = lca(right)
 *     L and R both non-null -> p is on one side, q on the other -> THIS node is the LCA
 *     only one non-null     -> pass it up (it's p, q, or an LCA found deeper)
 *
 *  Why does "return node as soon as node == p" work when q is BELOW p?
 *  Then p is the LCA. Nothing else in the tree returns non-null, so p's value
 *  propagates all the way up. (LeetCode guarantees both p and q exist.)
 *
 *  DRY RUN LCA(6, 4)
 *    at 5: left(6) returns 6; right(2): left 7 -> null, right 4 -> 4 -> returns 4
 *    at 5: both non-null -> return 5;  at 3: right side (1) returns null -> pass up 5
 *
 *  FOR A BST (#235, simpler): walk from the root; if both values are smaller go
 *  left, both larger go right, otherwise the current node is the split point = LCA.
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M05_LowestCommonAncestor {

    static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null) return root;      // split point
        return left != null ? left : right;                  // pass up whatever was found
    }

    // #235: LCA in a BST using the ordering
    static TreeNode lcaBST(TreeNode root, int p, int q) {
        TreeNode n = root;
        while (n != null) {
            if (p < n.val && q < n.val) n = n.left;
            else if (p > n.val && q > n.val) n = n.right;
            else return n;                                    // they split here (or one IS n)
        }
        return null;
    }

    // brute force: compare root-to-node paths
    static TreeNode brute(TreeNode root, TreeNode p, TreeNode q) {
        List<TreeNode> a = new ArrayList<>(), b = new ArrayList<>();
        path(root, p, a);
        path(root, q, b);
        TreeNode lca = null;
        for (int i = 0; i < Math.min(a.size(), b.size()) && a.get(i) == b.get(i); i++) lca = a.get(i);
        return lca;
    }

    static boolean path(TreeNode n, TreeNode target, List<TreeNode> out) {
        if (n == null) return false;
        out.add(n);
        if (n == target || path(n.left, target, out) || path(n.right, target, out)) return true;
        out.remove(out.size() - 1);                           // backtrack
        return false;
    }

    static TreeNode find(TreeNode n, int v) {
        if (n == null || n.val == v) return n;
        TreeNode l = find(n.left, v);
        return l != null ? l : find(n.right, v);
    }

    static void allNodes(TreeNode n, List<TreeNode> out) {
        if (n == null) return;
        out.add(n);
        allNodes(n.left, out);
        allNodes(n.right, out);
    }

    public static void main(String[] args) {
        TreeNode t = build(3, 5, 1, 6, 2, 0, 8, null, null, 7, 4);
        check("LCA(5, 1)", 3, lowestCommonAncestor(t, find(t, 5), find(t, 1)).val);
        check("LCA(5, 4) (ancestor of itself)", 5, lowestCommonAncestor(t, find(t, 5), find(t, 4)).val);
        check("LCA(6, 4)", 5, lowestCommonAncestor(t, find(t, 6), find(t, 4)).val);
        check("LCA(7, 8)", 3, lowestCommonAncestor(t, find(t, 7), find(t, 8)).val);
        TreeNode bst = build(6, 2, 8, 0, 4, 7, 9, null, null, 3, 5);
        check("BST LCA(2, 8)", 6, lcaBST(bst, 2, 8).val);
        check("BST LCA(3, 5)", 4, lcaBST(bst, 3, 5).val);
        Random rnd = new Random(5);
        boolean ok = true;
        for (int i = 0; i < 500; i++) {
            TreeNode r = randomTree(rnd, 1 + rnd.nextInt(30), 0, 9);
            List<TreeNode> nodes = new ArrayList<>();
            allNodes(r, nodes);
            TreeNode p = nodes.get(rnd.nextInt(nodes.size())), q = nodes.get(rnd.nextInt(nodes.size()));
            ok &= lowestCommonAncestor(r, p, q) == brute(r, p, q);
        }
        check("500 random trees vs path comparison", true, ok);
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
