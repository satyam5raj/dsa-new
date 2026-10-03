/*
 * ============================================================================
 *  TREE RECURSION PATTERNS: how to THINK about any tree problem
 * ============================================================================
 *
 *  Almost every tree problem is one of these 5 shapes. Recognize the shape
 *  and the code nearly writes itself.
 *
 *  ---------------------------------------------------------------------------
 *  PATTERN 1: BOTTOM-UP ("ask the children, then combine")  = postorder
 *    f(node) = combine( f(node.left), f(node.right), node.val )
 *    Trust the recursion: ASSUME f works on the children; just write the combine.
 *    Examples: max depth, size, sum, is balanced, diameter, max path sum.
 *
 *  PATTERN 2: TOP-DOWN ("pass information down")  = preorder
 *    f(node, infoFromAbove) -> each child gets updated info
 *    Examples: path sum (remaining target), good nodes (max so far),
 *    sum root-to-leaf numbers (number so far), validate BST (allowed range).
 *
 *  PATTERN 3: "RETURN ONE THING, TRACK ANOTHER"  (the "global answer" trick)
 *    The value you RETURN to the parent is not the answer itself.
 *    e.g. Diameter: return the HEIGHT (what the parent needs), but at each node
 *    update a global best with leftHeight + rightHeight (a path THROUGH this node).
 *    Examples: diameter, max path sum, longest univalue path, cameras.
 *
 *  PATTERN 4: TWO TREES AT ONCE
 *    f(a, b) compares nodes in lockstep: same tree, symmetric tree, subtree, merge.
 *    Base cases: both null -> true; one null -> false.
 *
 *  PATTERN 5: LEVEL BY LEVEL (BFS with a queue, size snapshot)
 *    Level averages, right side view, zigzag, min depth, cousins.
 *
 *  ---------------------------------------------------------------------------
 *  HOW TO WRITE A RECURSIVE TREE FUNCTION (the checklist)
 *    1. What does f(node) RETURN? Say it in one sentence.
 *    2. Base case: what is f(null)? (0? -1? true? an empty list?)
 *    3. Assume f(node.left) and f(node.right) already work. Combine them.
 *    4. Do I need info from ABOVE? -> add a parameter (top-down).
 *    5. Is the answer different from what the parent needs? -> global / field.
 *
 *  Complexity of all of these: O(n) time (each node once), O(h) stack space.
 *
 *  Run:  java TreeRecursionPatterns.java
 * ============================================================================
 */

import java.util.*;

public class TreeRecursionPatterns {

    // PATTERN 1 (bottom-up): max depth in NODES. f(null) = 0.
    static int maxDepth(TreeNode n) {
        if (n == null) return 0;
        return 1 + Math.max(maxDepth(n.left), maxDepth(n.right));
    }

    // PATTERN 1: the maximum value in the tree.
    static int maxValue(TreeNode n) {
        if (n == null) return Integer.MIN_VALUE;                 // identity for max
        return Math.max(n.val, Math.max(maxValue(n.left), maxValue(n.right)));
    }

    // PATTERN 2 (top-down): print every root-to-leaf path. The path so far comes from above.
    static void allPaths(TreeNode n, String soFar, List<String> out) {
        if (n == null) return;
        String path = soFar.isEmpty() ? String.valueOf(n.val) : soFar + "->" + n.val;
        if (n.left == null && n.right == null) { out.add(path); return; }   // leaf: a path is complete
        allPaths(n.left, path, out);
        allPaths(n.right, path, out);
    }

    // PATTERN 2: depth of every node = parent's depth + 1 (information flows DOWN).
    static void depths(TreeNode n, int depth, Map<Integer, Integer> out) {
        if (n == null) return;
        out.put(n.val, depth);
        depths(n.left, depth + 1, out);
        depths(n.right, depth + 1, out);
    }

    // PATTERN 3 (return one thing, track another): diameter = the longest path in EDGES
    // between any two nodes. Return the HEIGHT to the parent; record left + right as a candidate.
    static int best;

    static int diameter(TreeNode root) {
        best = 0;
        heightNodes(root);
        return best;
    }

    static int heightNodes(TreeNode n) {
        if (n == null) return 0;
        int l = heightNodes(n.left), r = heightNodes(n.right);
        best = Math.max(best, l + r);              // the best path that BENDS at n
        return 1 + Math.max(l, r);                 // what the parent can extend: one arm only
    }

    // PATTERN 4 (two trees): are the two trees mirror images?
    static boolean isMirror(TreeNode a, TreeNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.val == b.val && isMirror(a.left, b.right) && isMirror(a.right, b.left);
    }

    // PATTERN 5 (BFS): the average of each level.
    static List<Double> levelAverages(TreeNode root) {
        List<Double> out = new ArrayList<>();
        if (root == null) return out;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            int size = q.size();
            double sum = 0;
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                sum += n.val;
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            out.add(sum / size);
        }
        return out;
    }

    public static void main(String[] args) {
        //         3
        //       /   \
        //      9     20
        //     /     /  \
        //    1     15   7
        TreeNode root = build(3, 9, 20, 1, null, 15, 7);

        System.out.println("PATTERN 1: bottom-up");
        check("max depth", 3, maxDepth(root));
        check("max value", 20, maxValue(root));

        System.out.println("PATTERN 2: top-down");
        List<String> paths = new ArrayList<>();
        allPaths(root, "", paths);
        check("root-to-leaf paths", "[3->9->1, 3->20->15, 3->20->7]", paths);
        Map<Integer, Integer> d = new TreeMap<>();
        depths(root, 0, d);
        check("depth of each value", "{1=2, 3=0, 7=2, 9=1, 15=2, 20=1}", d);

        System.out.println("PATTERN 3: return height, track the diameter");
        check("diameter (1->9->3->20->15)", 4, diameter(root));

        System.out.println("PATTERN 4: two trees in lockstep");
        check("symmetric [1,2,2,3,4,4,3]", true, isMirror(build(1, 2, 2, 3, 4, 4, 3), build(1, 2, 2, 3, 4, 4, 3)));
        check("not symmetric", false, isMirror(root, root));

        System.out.println("PATTERN 5: BFS level by level");
        check("level averages", "[3.0, 14.5, 7.666666666666667]", levelAverages(root));
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
