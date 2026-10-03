/*
 * ============================================================================
 *  MEDIUM 01 : BINARY TREE LEVEL ORDER TRAVERSAL   LeetCode #102
 *  https://leetcode.com/problems/binary-tree-level-order-traversal/
 *  Pattern: BFS with the LEVEL-SIZE SNAPSHOT  (the template for every level problem)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the values level by level, left to right.
 *      [3,9,20,null,null,15,7] -> [[3],[9,20],[15,7]]
 *
 *  ---------------------------------------------------------------------------
 *  WHY A QUEUE: FIFO order means everything at depth d is dequeued before
 *  anything at depth d+1 (children are added behind the current level).
 *
 *  HOW TO KNOW WHERE A LEVEL ENDS: at the start of each round, the queue holds
 *  EXACTLY one full level. Snapshot size = q.size(), poll exactly that many
 *  nodes (adding their children behind them) -> that's one level.
 *
 *  DRY RUN [3,9,20,null,null,15,7]
 *    q=[3]        size 1 -> [3]          q=[9,20]
 *    q=[9,20]     size 2 -> [9,20]       q=[15,7]
 *    q=[15,7]     size 2 -> [15,7]       q=[]
 *
 *  DFS ALTERNATIVE: pass the depth down; append to result[depth] (create the list
 *  the first time a depth is reached). Preorder visits left before right, so each
 *  level still comes out left to right.
 *
 *  VARIANTS using the same template: right side view (M02), zigzag (M10),
 *  level averages, largest value per level, min depth (E10), level-order bottom-up.
 *
 *  COMPLEXITY   Time O(n)   Space O(width) - up to n/2 for the last level
 * ============================================================================
 */

import java.util.*;

public class M01_BinaryTreeLevelOrderTraversal {

    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.offer(root);
        while (!q.isEmpty()) {
            int size = q.size();                           // the whole current level
            List<Integer> level = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.offer(n.left);
                if (n.right != null) q.offer(n.right);
            }
            result.add(level);
        }
        return result;
    }

    static List<List<Integer>> levelOrderDfs(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        dfs(root, 0, result);
        return result;
    }

    static void dfs(TreeNode n, int depth, List<List<Integer>> result) {
        if (n == null) return;
        if (depth == result.size()) result.add(new ArrayList<>());   // first visit to this depth
        result.get(depth).add(n.val);
        dfs(n.left, depth + 1, result);
        dfs(n.right, depth + 1, result);
    }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", "[[3], [9, 20], [15, 7]]", levelOrder(build(3, 9, 20, null, null, 15, 7)));
        check("[1]", "[[1]]", levelOrder(build(1)));
        check("[]", "[]", levelOrder(null));
        check("DFS version", "[[3], [9, 20], [15, 7]]", levelOrderDfs(build(3, 9, 20, null, null, 15, 7)));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(40), 0, 99);
            ok &= levelOrder(r).equals(levelOrderDfs(r));
        }
        check("500 random trees: BFS == DFS", true, ok);
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
