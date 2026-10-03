/*
 * ============================================================================
 *  MEDIUM 04 : KTH SMALLEST ELEMENT IN A BST  LeetCode #230
 *  https://leetcode.com/problems/kth-smallest-element-in-a-bst/
 *  Pattern: INORDER = SORTED; stop after k nodes (iterative inorder)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the k-th smallest value (1-indexed) in a BST.
 *              5
 *             / \
 *            3   6          k=3 -> 3   (sorted: 1 2 3 4 5 6)
 *           / \
 *          2   4
 *         /
 *        1
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: collect the full inorder list, return list[k-1]. O(n) time and space.
 *
 *  BETTER: inorder visits in sorted order, so just COUNT and STOP at the k-th
 *  visit. With the iterative inorder (01-Basics): push the left spine, pop,
 *  count, go right. Time O(h + k): we only walk down once and then visit k nodes.
 *
 *  DRY RUN k=3
 *    push 5,3,2,1 -> pop 1 (count 1) -> pop 2 (2) -> pop 3 (3 == k) -> return 3
 *
 *  FOLLOW-UP (asked often): "the BST is modified often and you query kth often"
 *    -> store the SUBTREE SIZE in every node (an "order-statistic tree").
 *       leftSize = size(node.left)
 *       k == leftSize + 1 -> this node
 *       k <= leftSize     -> go left
 *       else              -> go right with k - leftSize - 1
 *    That's O(h) per query; with a balanced tree, O(log n). Shown below too.
 *
 *  COMPLEXITY   Time O(h + k)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M04_KthSmallestElementInBST {

    static int kthSmallest(TreeNode root, int k) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (true) {
            while (cur != null) {             // go as far left as possible
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();                // the next smallest
            if (--k == 0) return cur.val;
            cur = cur.right;
        }
    }

    // ---- Follow-up: order-statistic idea using subtree sizes ----
    static Map<TreeNode, Integer> sizes = new HashMap<>();

    static int size(TreeNode n) {
        if (n == null) return 0;
        Integer s = sizes.get(n);
        if (s == null) { s = 1 + size(n.left) + size(n.right); sizes.put(n, s); }
        return s;
    }

    static int kthBySize(TreeNode n, int k) {
        while (true) {
            int leftSize = size(n.left);
            if (k == leftSize + 1) return n.val;
            if (k <= leftSize) n = n.left;
            else { k -= leftSize + 1; n = n.right; }
        }
    }

    static TreeNode insert(TreeNode n, int v) {
        if (n == null) return new TreeNode(v);
        if (v < n.val) n.left = insert(n.left, v);
        else if (v > n.val) n.right = insert(n.right, v);
        return n;
    }

    public static void main(String[] args) {
        TreeNode t = build(5, 3, 6, 2, 4, null, null, 1);
        check("k=3", 3, kthSmallest(t, 3));
        check("k=1 (min)", 1, kthSmallest(t, 1));
        check("k=6 (max)", 6, kthSmallest(t, 6));
        check("[3,1,4,null,2] k=1", 1, kthSmallest(build(3, 1, 4, null, 2), 1));
        check("subtree-size version k=4", 4, kthBySize(t, 4));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int i = 0; i < 300; i++) {
            TreeNode r = null;
            TreeSet<Integer> keys = new TreeSet<>();
            for (int j = 0, n = 1 + rnd.nextInt(40); j < n; j++) {
                int v = rnd.nextInt(200);
                keys.add(v);
                r = insert(r, v);
            }
            List<Integer> sorted = new ArrayList<>(keys);
            sizes.clear();
            for (int k = 1; k <= sorted.size(); k++) ok &= kthSmallest(r, k) == sorted.get(k - 1) && kthBySize(r, k) == sorted.get(k - 1);
        }
        check("300 random BSTs, every k", true, ok);
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
