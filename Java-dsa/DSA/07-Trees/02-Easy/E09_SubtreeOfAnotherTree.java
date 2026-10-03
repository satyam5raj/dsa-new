/*
 * ============================================================================
 *  EASY 09 : SUBTREE OF ANOTHER TREE         LeetCode #572
 *  https://leetcode.com/problems/subtree-of-another-tree/
 *  Pattern: "SAME TREE" at every node  (or: serialize + string search)
 * ============================================================================
 *
 *  PROBLEM
 *    Is subRoot a subtree of root? A subtree = some node of root plus ALL of its
 *    descendants (you can't cut off part of the bottom).
 *        root            subRoot
 *          3               4
 *         / \             / \          -> true
 *        4   5           1   2
 *       / \
 *      1   2
 *    If root had an extra 0 under the 2, the answer would be FALSE: the subtree
 *    at 4 would then include the 0.
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: for every node of root, ask isSameTree(node, subRoot)  (E02)
 *    isSubtree(r, s) = same(r, s) || isSubtree(r.left, s) || isSubtree(r.right, s)
 *    Time O(n * m) worst case, usually much faster (mismatches stop early).
 *
 *  APPROACH 2: SERIALIZE both trees with explicit NULL markers, then check whether
 *  one string contains the other.  O(n + m) with KMP (Strings/01-Basics).
 *    Two traps:
 *    - nulls MUST be written: without them, different shapes give the same string
 *    - each value needs a separator IN FRONT: without it, root [12] serializes to
 *      "12,#,#" and subRoot [2] to "2,#,#", which is found inside "12,#,#" -> a false
 *      match. With a leading comma: ",12,#,#" does NOT contain ",2,#,#".
 *
 *  COMPLEXITY   Approach 1: O(n*m), Approach 2: O(n + m)
 * ============================================================================
 */

import java.util.*;

public class E09_SubtreeOfAnotherTree {

    static boolean isSubtree(TreeNode root, TreeNode subRoot) {
        if (root == null) return subRoot == null;
        return isSame(root, subRoot) || isSubtree(root.left, subRoot) || isSubtree(root.right, subRoot);
    }

    static boolean isSame(TreeNode a, TreeNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.val == b.val && isSame(a.left, b.left) && isSame(a.right, b.right);
    }

    // Approach 2: preorder with null markers, then a substring search
    static boolean isSubtreeBySerialization(TreeNode root, TreeNode subRoot) {
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        serialize(root, a);
        serialize(subRoot, b);
        return a.indexOf(b.toString()) >= 0;      // swap in KMP for a guaranteed O(n + m)
    }

    static void serialize(TreeNode n, StringBuilder sb) {
        if (n == null) { sb.append(",#"); return; }
        sb.append(',').append(n.val);             // the leading comma separates "12" from "2"
        serialize(n.left, sb);
        serialize(n.right, sb);
    }

    public static void main(String[] args) {
        check("example 1", true, isSubtree(build(3, 4, 5, 1, 2), build(4, 1, 2)));
        check("extra node below (not a subtree)", false, isSubtree(build(3, 4, 5, 1, 2, null, null, null, null, 0), build(4, 1, 2)));
        check("[12] vs [2] (serialization trap)", false, isSubtreeBySerialization(build(12), build(2)));
        check("serialization, example 1", true, isSubtreeBySerialization(build(3, 4, 5, 1, 2), build(4, 1, 2)));
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(12), 0, 2), s = randomTree(rnd, 1 + rnd.nextInt(3), 0, 2);
            ok &= isSubtree(r, s) == isSubtreeBySerialization(r, s);
        }
        check("2,000 random pairs: both approaches agree", true, ok);
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
