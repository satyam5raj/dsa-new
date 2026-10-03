/*
 * ============================================================================
 *  MEDIUM 15 : RECOVER BINARY SEARCH TREE    LeetCode #99
 *  https://leetcode.com/problems/recover-binary-search-tree/
 *  Pattern: INORDER should be sorted -> find the 2 "inversions" -> swap values back
 * ============================================================================
 *
 *  PROBLEM
 *    Exactly TWO nodes of a BST had their values swapped by mistake. Fix the tree
 *    without changing its structure.
 *      [1,3,null,null,2] -> [3,1,null,null,2]
 *      [3,1,4,null,null,2] -> [2,1,4,null,null,3]
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: the inorder of a BST is sorted. Swapping two values creates
 *  "drops" (places where prev > cur) in that sequence:
 *
 *    NOT ADJACENT:  1 2 [6] 4 5 [3] 7        two drops: 6>4 and 5>3
 *                       ^first      ^second
 *       first  = the BIGGER value at the FIRST drop  (prev of drop 1) = 6
 *       second = the SMALLER value at the LAST drop  (cur of drop 2)  = 3
 *
 *    ADJACENT:      1 2 [4] [3] 5            only one drop: 4>3
 *       first = prev = 4, second = cur = 3
 *
 *  One rule covers both cases:
 *       on every drop: if first is null -> first = prev;   ALWAYS second = cur
 *  Then swap first.val and second.val.
 *
 *  SPACE
 *    Recursive or stack inorder: O(h).
 *    The follow-up asks for O(1) space: use MORRIS inorder (01-Basics) with the
 *    same drop-detection logic. Both versions are below.
 *
 *  COMPLEXITY   Time O(n)   Space O(h) or O(1) with Morris
 * ============================================================================
 */

import java.util.*;

public class M15_RecoverBinarySearchTree {

    static TreeNode first, second, prev;

    static void recoverTree(TreeNode root) {
        first = second = prev = null;
        inorder(root);
        int tmp = first.val; first.val = second.val; second.val = tmp;
    }

    static void inorder(TreeNode n) {
        if (n == null) return;
        inorder(n.left);
        visit(n);
        inorder(n.right);
    }

    static void visit(TreeNode n) {
        if (prev != null && prev.val > n.val) {     // a drop
            if (first == null) first = prev;        // first drop: the bigger one
            second = n;                             // last drop: the smaller one
        }
        prev = n;
    }

    // Same logic on top of Morris traversal: O(1) extra space
    static void recoverTreeMorris(TreeNode root) {
        first = second = prev = null;
        TreeNode cur = root;
        while (cur != null) {
            if (cur.left == null) {
                visit(cur);
                cur = cur.right;
            } else {
                TreeNode pred = cur.left;
                while (pred.right != null && pred.right != cur) pred = pred.right;
                if (pred.right == null) { pred.right = cur; cur = cur.left; }
                else { pred.right = null; visit(cur); cur = cur.right; }
            }
        }
        int tmp = first.val; first.val = second.val; second.val = tmp;
    }

    static TreeNode insert(TreeNode n, int v) {
        if (n == null) return new TreeNode(v);
        if (v < n.val) n.left = insert(n.left, v); else n.right = insert(n.right, v);
        return n;
    }

    static void nodes(TreeNode n, List<TreeNode> out) {
        if (n == null) return;
        nodes(n.left, out); out.add(n); nodes(n.right, out);
    }

    public static void main(String[] args) {
        TreeNode a = build(1, 3, null, null, 2);
        recoverTree(a);
        check("[1,3,null,null,2] (adjacent)", "[3, 1, null, null, 2]", a);
        TreeNode b = build(3, 1, 4, null, null, 2);
        recoverTree(b);
        check("[3,1,4,null,null,2]", "[2, 1, 4, null, null, 3]", b);
        TreeNode c = build(3, 1, 4, null, null, 2);
        recoverTreeMorris(c);
        check("Morris version", "[2, 1, 4, null, null, 3]", c);

        Random rnd = new Random(15);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            TreeNode r = null;
            List<Integer> keys = new ArrayList<>();
            for (int i = 0, n = 2 + rnd.nextInt(20); i < n; i++) keys.add(i * 3);
            Collections.shuffle(keys, rnd);
            for (int k : keys) r = insert(r, k);
            String correct = String.valueOf(r);
            List<TreeNode> ns = new ArrayList<>();
            nodes(r, ns);
            int i = rnd.nextInt(ns.size()), j = rnd.nextInt(ns.size());
            if (i == j) continue;
            int tmp = ns.get(i).val; ns.get(i).val = ns.get(j).val; ns.get(j).val = tmp;   // break it
            if (t % 2 == 0) recoverTree(r); else recoverTreeMorris(r);
            ok &= String.valueOf(r).equals(correct);
        }
        check("1,000 random swaps repaired", true, ok);
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
