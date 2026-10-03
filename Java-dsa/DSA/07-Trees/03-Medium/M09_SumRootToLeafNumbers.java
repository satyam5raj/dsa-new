/*
 * ============================================================================
 *  MEDIUM 09 : SUM ROOT TO LEAF NUMBERS      LeetCode #129
 *  https://leetcode.com/problems/sum-root-to-leaf-numbers/
 *  Pattern: TOP-DOWN, build the number digit by digit (number * 10 + digit)
 * ============================================================================
 *
 *  PROBLEM
 *    Every node holds a digit 0-9. Each root-to-leaf path spells a number.
 *    Return the sum of all those numbers.
 *              4
 *             / \
 *            9   0          paths: 495, 491, 40 -> 495 + 491 + 40 = 1026
 *           / \
 *          5   1
 *
 *  ---------------------------------------------------------------------------
 *  INSIGHT: appending a digit d to a number x gives x * 10 + d
 *  (the same trick as parsing "495" -> ((4)*10 + 9)*10 + 5, see Core-Java digit problems).
 *  Pass the number built so far DOWN the tree:
 *     sum(node, soFar):
 *        null -> 0
 *        cur = soFar * 10 + node.val
 *        leaf -> cur
 *        else -> sum(left, cur) + sum(right, cur)
 *
 *  DRY RUN
 *    4 -> cur 4;  9 -> 49;  5 (leaf) -> 495;  1 (leaf) -> 491;  0 (leaf) -> 40
 *    total 1026
 *
 *  Note the leaf check again (like Path Sum, E07): a null child must contribute 0,
 *  not the partial number, or [1,2] would count "1" as well as "12".
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class M09_SumRootToLeafNumbers {

    static int sumNumbers(TreeNode root) {
        return sum(root, 0);
    }

    static int sum(TreeNode n, int soFar) {
        if (n == null) return 0;
        int cur = soFar * 10 + n.val;                         // append this digit
        if (n.left == null && n.right == null) return cur;    // a complete number
        return sum(n.left, cur) + sum(n.right, cur);
    }

    // referee: collect the paths as strings and parse them
    static void paths(TreeNode n, String s, List<String> out) {
        if (n == null) return;
        s += n.val;
        if (n.left == null && n.right == null) out.add(s);
        paths(n.left, s, out);
        paths(n.right, s, out);
    }

    public static void main(String[] args) {
        check("[1,2,3] (12 + 13)", 25, sumNumbers(build(1, 2, 3)));
        check("[4,9,0,5,1] (495+491+40)", 1026, sumNumbers(build(4, 9, 0, 5, 1)));
        check("[1,2] (only 12)", 12, sumNumbers(build(1, 2)));
        check("[0]", 0, sumNumbers(build(0)));
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(9), 0, 9);    // at most 9 digits: fits in an int
            List<String> ps = new ArrayList<>();
            paths(r, "", ps);
            int exp = ps.stream().mapToInt(Integer::parseInt).sum();
            ok &= sumNumbers(r) == exp;
        }
        check("1,000 random trees vs string paths", true, ok);
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
