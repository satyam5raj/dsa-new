/*
 * ============================================================================
 *  MEDIUM 07 : CONSTRUCT BINARY TREE FROM PREORDER AND INORDER   LeetCode #105
 *  https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/
 *  Pattern: DIVIDE AND CONQUER + HASHMAP of inorder positions
 * ============================================================================
 *
 *  PROBLEM
 *    Rebuild the tree (values unique) from its preorder and inorder traversals.
 *      preorder [3,9,20,15,7], inorder [9,3,15,20,7] -> [3,9,20,null,null,15,7]
 *
 *  ---------------------------------------------------------------------------
 *  TWO FACTS
 *    PREORDER = root, [left subtree...], [right subtree...]  -> the FIRST value is the root
 *    INORDER  = [left subtree...], root, [right subtree...]  -> the root SPLITS it in two
 *
 *    preorder: [3 | 9 | 20 15 7]          inorder: [9 | 3 | 15 20 7]
 *               ^root                                ^ root at index 1
 *    -> left subtree has 1 node (everything left of 3 in inorder): {9}
 *    -> right subtree has 3 nodes: {15, 20, 7}
 *    Recurse on each part. The next root in preorder is simply the next unused value.
 *
 *  ALGORITHM
 *    map: value -> its index in inorder   (O(1) lookup instead of an O(n) search)
 *    pre = 0  (a shared pointer into preorder)
 *    build(inLo, inHi):
 *      if inLo > inHi -> null
 *      root = preorder[pre++]
 *      mid = map[root]
 *      root.left  = build(inLo, mid - 1)     <- MUST build left first: preorder lists the
 *      root.right = build(mid + 1, inHi)        left subtree before the right one
 *
 *  WHY UNIQUE VALUES? With duplicates the root's position in inorder is ambiguous.
 *  Preorder + POSTORDER isn't enough either (it can't tell a left child from a right one);
 *  inorder is needed to split.
 *
 *  COMPLEXITY   Time O(n)   Space O(n) map + O(h) recursion
 * ============================================================================
 */

import java.util.*;

public class M07_ConstructFromPreorderInorder {

    static int pre;
    static Map<Integer, Integer> inIndex;

    static TreeNode buildTree(int[] preorder, int[] inorder) {
        pre = 0;
        inIndex = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) inIndex.put(inorder[i], i);
        return build(preorder, 0, inorder.length - 1);
    }

    static TreeNode build(int[] preorder, int inLo, int inHi) {
        if (inLo > inHi) return null;
        TreeNode root = new TreeNode(preorder[pre++]);         // next root from preorder
        int mid = inIndex.get(root.val);                       // where it splits inorder
        root.left = build(preorder, inLo, mid - 1);            // left first (preorder order)
        root.right = build(preorder, mid + 1, inHi);
        return root;
    }

    static void preorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        out.add(n.val); preorder(n.left, out); preorder(n.right, out);
    }

    static void inorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        inorder(n.left, out); out.add(n.val); inorder(n.right, out);
    }

    public static void main(String[] args) {
        check("example", "[3, 9, 20, null, null, 15, 7]", buildTree(new int[]{3, 9, 20, 15, 7}, new int[]{9, 3, 15, 20, 7}));
        check("single [-1]", "[-1]", buildTree(new int[]{-1}, new int[]{-1}));
        check("left chain", "[3, 2, null, 1]", buildTree(new int[]{3, 2, 1}, new int[]{1, 2, 3}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 0);
            int[] next = {0};
            relabel(r, next);                                   // unique values
            List<Integer> p = new ArrayList<>(), in = new ArrayList<>();
            preorder(r, p);
            inorder(r, in);
            TreeNode rebuilt = buildTree(p.stream().mapToInt(Integer::intValue).toArray(), in.stream().mapToInt(Integer::intValue).toArray());
            ok &= String.valueOf(rebuilt).equals(String.valueOf(r));
        }
        check("500 random trees rebuilt exactly", true, ok);
        summary();
    }

    static void relabel(TreeNode n, int[] next) {
        if (n == null) return;
        n.val = next[0]++ * 7 % 1000 - 300;     // unique, not in sorted order
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
