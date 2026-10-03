/*
 * ============================================================================
 *  MEDIUM 11 : FLATTEN BINARY TREE TO LINKED LIST   LeetCode #114
 *  https://leetcode.com/problems/flatten-binary-tree-to-linked-list/
 *  Pattern: rewire IN PLACE in PREORDER order  (Morris-style O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    Flatten the tree in place into a "linked list" that uses the RIGHT pointers,
 *    in PREORDER order, with every left pointer set to null.
 *            1
 *           / \                 1 -> 2 -> 3 -> 4 -> 5 -> 6
 *          2   5        ->      (all via .right)
 *         / \   \
 *        3   4   6
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: collect the preorder in a list, then relink. O(n) extra space.
 *
 *  APPROACH 2: reverse postorder (right, left, node) with a "prev" pointer.
 *    Visiting in reverse preorder means each node is linked to the one that should
 *    come AFTER it, which we've just processed:  node.right = prev; node.left = null; prev = node.
 *    O(h) recursion space.
 *
 *  APPROACH 3 (O(1) space, Morris-like): walk down the right spine with cur.
 *    If cur has a left subtree:
 *       find the RIGHTMOST node of that left subtree (the last one in its preorder)
 *       hang cur's right subtree there:     rightmost.right = cur.right
 *       move the left subtree to the right: cur.right = cur.left;  cur.left = null
 *    Step to cur.right and repeat.
 *
 *  DRY RUN approach 3
 *    cur=1: left subtree 2(3,4), its rightmost is 4 -> 4.right = 5(...)
 *           1.right = 2, 1.left = null       ->  1 - 2(3, 4 - 5 - 6)
 *    cur=2: left 3, rightmost 3 -> 3.right = 4 ... -> 1-2-3-4-5-6
 *
 *  COMPLEXITY   Approach 3: Time O(n) (each node is on some "rightmost" walk at most once)
 *               Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M11_FlattenBinaryTreeToLinkedList {

    // Approach 3: O(1) extra space
    static void flatten(TreeNode root) {
        TreeNode cur = root;
        while (cur != null) {
            if (cur.left != null) {
                TreeNode rightmost = cur.left;
                while (rightmost.right != null) rightmost = rightmost.right;
                rightmost.right = cur.right;     // hang the right subtree after the left part
                cur.right = cur.left;            // the left part comes first in preorder
                cur.left = null;
            }
            cur = cur.right;
        }
    }

    // Approach 2: reverse preorder with a prev pointer
    static TreeNode prev;

    static void flattenRecursive(TreeNode root) {
        prev = null;
        reversePre(root);
    }

    static void reversePre(TreeNode n) {
        if (n == null) return;
        reversePre(n.right);
        reversePre(n.left);
        n.right = prev;            // link to the node that comes next in preorder
        n.left = null;
        prev = n;
    }

    static String asList(TreeNode root) {
        StringBuilder sb = new StringBuilder("[");
        for (TreeNode n = root; n != null; n = n.right) {
            if (n.left != null) return "left pointer not null!";
            sb.append(sb.length() > 1 ? ", " : "").append(n.val);
        }
        return sb.append("]").toString();
    }

    static void preorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        out.add(n.val); preorder(n.left, out); preorder(n.right, out);
    }

    public static void main(String[] args) {
        TreeNode t = build(1, 2, 5, 3, 4, null, 6);
        flatten(t);
        check("[1,2,5,3,4,null,6]", "[1, 2, 3, 4, 5, 6]", asList(t));
        TreeNode t2 = build(1, 2, 5, 3, 4, null, 6);
        flattenRecursive(t2);
        check("recursive version", "[1, 2, 3, 4, 5, 6]", asList(t2));
        TreeNode empty = null;
        flatten(empty);
        check("[] stays empty", "[]", asList(empty));
        Random rnd = new Random(11);
        boolean ok = true;
        for (int i = 0; i < 500; i++) {
            long seed = rnd.nextLong();
            TreeNode a = randomTree(new Random(seed), 30, 0, 99), b = randomTree(new Random(seed), 30, 0, 99);
            List<Integer> pre = new ArrayList<>();
            preorder(a, pre);
            flatten(a);
            flattenRecursive(b);
            ok &= asList(a).equals(pre.toString()) && asList(b).equals(pre.toString());
        }
        check("500 random trees == preorder", true, ok);
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
