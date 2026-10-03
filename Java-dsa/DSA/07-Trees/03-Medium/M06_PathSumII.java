/*
 * ============================================================================
 *  MEDIUM 06 : PATH SUM II                   LeetCode #113
 *  https://leetcode.com/problems/path-sum-ii/
 *  Pattern: DFS + BACKTRACKING (one shared path list: add, recurse, remove)
 * ============================================================================
 *
 *  PROBLEM
 *    Return ALL root-to-leaf paths whose sum equals targetSum.
 *              5
 *             / \
 *            4   8
 *           /   / \          target 22 -> [[5,4,11,2], [5,8,4,5]]
 *          11  13  4
 *         /  \    / \
 *        7    2  5   1
 *
 *  ---------------------------------------------------------------------------
 *  Path Sum (E07) answered yes/no. Now we must RECORD the paths.
 *
 *  NAIVE: pass a NEW copy of the path list to every child -> O(n * h) copies.
 *
 *  BACKTRACKING: keep ONE list for the current path.
 *    path.add(node)            <- choose
 *    if leaf and remaining == 0 -> result.add(new ArrayList<>(path))   (copy only on success!)
 *    recurse left, recurse right
 *    path.remove(last)         <- un-choose (so the parent's list is as it was)
 *  Forgetting the copy is the #1 bug: you'd add the SAME list object every time,
 *  and it's empty by the end.
 *
 *  This add/recurse/remove shape is the core of all backtracking (subsets,
 *  permutations, N-Queens...).
 *
 *  COMPLEXITY   Time O(n * h) worst case (copying each found path)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M06_PathSumII {

    static List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        List<List<Integer>> result = new ArrayList<>();
        dfs(root, targetSum, new ArrayList<>(), result);
        return result;
    }

    static void dfs(TreeNode n, int remaining, List<Integer> path, List<List<Integer>> result) {
        if (n == null) return;
        path.add(n.val);                                          // choose
        remaining -= n.val;
        if (n.left == null && n.right == null && remaining == 0) {
            result.add(new ArrayList<>(path));                    // COPY the current path
        } else {
            dfs(n.left, remaining, path, result);
            dfs(n.right, remaining, path, result);
        }
        path.remove(path.size() - 1);                             // un-choose (backtrack)
    }

    // referee: every root-to-leaf path, filtered by sum (copies everywhere, simple and slow)
    static void allPaths(TreeNode n, List<Integer> soFar, List<List<Integer>> out) {
        if (n == null) return;
        List<Integer> p = new ArrayList<>(soFar);
        p.add(n.val);
        if (n.left == null && n.right == null) out.add(p);
        allPaths(n.left, p, out);
        allPaths(n.right, p, out);
    }

    public static void main(String[] args) {
        TreeNode t = build(5, 4, 8, 11, null, 13, 4, 7, 2, null, null, 5, 1);
        check("target 22", "[[5, 4, 11, 2], [5, 8, 4, 5]]", pathSum(t, 22));
        check("[1,2,3] target 5", "[]", pathSum(build(1, 2, 3), 5));
        check("[1,2] target 0", "[]", pathSum(build(1, 2), 0));
        Random rnd = new Random(6);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(15), -3, 3);
            int target = rnd.nextInt(9) - 4;
            List<List<Integer>> all = new ArrayList<>();
            allPaths(r, new ArrayList<>(), all);
            all.removeIf(p -> p.stream().mapToInt(Integer::intValue).sum() != target);
            ok &= pathSum(r, target).equals(all);
        }
        check("1,000 random trees vs all paths", true, ok);
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
