/*
 * ============================================================================
 *  MEDIUM 10 : BINARY TREE ZIGZAG LEVEL ORDER TRAVERSAL   LeetCode #103
 *  https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/
 *  Pattern: level-order BFS + fill each level from alternating ENDS (a deque)
 * ============================================================================
 *
 *  PROBLEM
 *    Level order, but alternate direction: left->right, right->left, left->right...
 *              3
 *             / \
 *            9   20         -> [[3], [20, 9], [15, 7]]
 *               /  \
 *              15   7
 *
 *  ---------------------------------------------------------------------------
 *  SIMPLE: do the normal level order (M01) and reverse every odd level.
 *  Works, but it's an extra pass per level.
 *
 *  CLEANER: keep the BFS exactly the same (always add children left then right),
 *  but build each level's list in a Deque:
 *     leftToRight -> addLast(val)
 *     rightToLeft -> addFirst(val)   (later nodes end up in front = reversed)
 *  Don't try to change the ORDER OF CHILDREN in the queue - that gets confusing
 *  fast and is the usual source of bugs here.
 *
 *  DRY RUN
 *    level 0 (L->R): 3            -> [3]
 *    level 1 (R->L): 9 addFirst, 20 addFirst -> [20, 9]
 *    level 2 (L->R): 15, 7        -> [15, 7]
 *
 *  COMPLEXITY   Time O(n)   Space O(width)
 * ============================================================================
 */

import java.util.*;

public class M10_ZigzagLevelOrder {

    static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        boolean leftToRight = true;
        while (!q.isEmpty()) {
            Deque<Integer> level = new ArrayDeque<>();
            for (int i = q.size(); i > 0; i--) {
                TreeNode n = q.poll();
                if (leftToRight) level.addLast(n.val);
                else level.addFirst(n.val);
                if (n.left != null) q.add(n.left);          // children ALWAYS left then right
                if (n.right != null) q.add(n.right);
            }
            result.add(new ArrayList<>(level));
            leftToRight = !leftToRight;
        }
        return result;
    }

    // simple version: normal level order, reverse odd levels
    static List<List<Integer>> simple(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        Queue<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.add(root);
        while (!q.isEmpty()) {
            List<Integer> level = new ArrayList<>();
            for (int i = q.size(); i > 0; i--) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            if (result.size() % 2 == 1) Collections.reverse(level);
            result.add(level);
        }
        return result;
    }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", "[[3], [20, 9], [15, 7]]", zigzagLevelOrder(build(3, 9, 20, null, null, 15, 7)));
        check("[1,2,3,4,5,6,7]", "[[1], [3, 2], [4, 5, 6, 7]]", zigzagLevelOrder(build(1, 2, 3, 4, 5, 6, 7)));
        check("[1]", "[[1]]", zigzagLevelOrder(build(1)));
        check("[]", "[]", zigzagLevelOrder(null));
        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 99);
            ok &= zigzagLevelOrder(r).equals(simple(r));
        }
        check("500 random trees: deque == reverse odd levels", true, ok);
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
