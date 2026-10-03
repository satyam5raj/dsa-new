/*
 * ============================================================================
 *  HARD 03 : VERTICAL ORDER TRAVERSAL OF A BINARY TREE   LeetCode #987
 *  https://leetcode.com/problems/vertical-order-traversal-of-a-binary-tree/
 *  Pattern: give every node COORDINATES (row, col), then SORT with a 3-key comparator
 * ============================================================================
 *
 *  PROBLEM
 *    Root at (row 0, col 0). Left child of (r, c) is (r+1, c-1), right child (r+1, c+1).
 *    Return the columns from left to right. Inside a column, top to bottom; nodes
 *    in the SAME row AND column are ordered by VALUE.
 *               1 (0,0)
 *             /         \
 *        2 (1,-1)     3 (1,1)
 *         /    \       /    \
 *    4(2,-2) 5(2,0)  6(2,0)  7(2,2)        -> [[4],[2],[1,5,6],[3],[7]]
 *                                            (5 and 6 tie on (2,0): sorted by value)
 *
 *  ---------------------------------------------------------------------------
 *  WHY IT'S "HARD": not the traversal, but the TIE rule. A plain BFS by column
 *  (#314, the easier variant) would list 5, 6 in left-to-right order; here
 *  they must be sorted by value - a pure BFS order isn't enough.
 *
 *  APPROACH
 *    1. DFS (any order) collecting triples (col, row, val)
 *    2. sort by col, then row, then val
 *    3. walk the sorted list, starting a new group whenever col changes
 *
 *  ALTERNATIVE: TreeMap<col, TreeMap<row, PriorityQueue<val>>> - the maps keep
 *  columns and rows sorted, the heap handles ties. Same O(n log n), more ceremony.
 *  (Used below as the referee.)
 *
 *  COMPLEXITY   Time O(n log n) (the sort)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H03_VerticalOrderTraversal {

    static List<List<Integer>> verticalTraversal(TreeNode root) {
        List<int[]> nodes = new ArrayList<>();                 // {col, row, val}
        collect(root, 0, 0, nodes);
        nodes.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                           : a[1] != b[1] ? Integer.compare(a[1], b[1])
                           : Integer.compare(a[2], b[2]));
        List<List<Integer>> result = new ArrayList<>();
        Integer prevCol = null;
        for (int[] n : nodes) {
            if (prevCol == null || n[0] != prevCol) {          // a new column starts
                result.add(new ArrayList<>());
                prevCol = n[0];
            }
            result.get(result.size() - 1).add(n[2]);
        }
        return result;
    }

    static void collect(TreeNode n, int row, int col, List<int[]> out) {
        if (n == null) return;
        out.add(new int[]{col, row, n.val});
        collect(n.left, row + 1, col - 1, out);
        collect(n.right, row + 1, col + 1, out);
    }

    // referee: nested sorted maps + heaps
    static List<List<Integer>> withTreeMaps(TreeNode root) {
        TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> cols = new TreeMap<>();
        fill(root, 0, 0, cols);
        List<List<Integer>> result = new ArrayList<>();
        for (TreeMap<Integer, PriorityQueue<Integer>> rows : cols.values()) {
            List<Integer> col = new ArrayList<>();
            for (PriorityQueue<Integer> pq : rows.values()) while (!pq.isEmpty()) col.add(pq.poll());
            result.add(col);
        }
        return result;
    }

    static void fill(TreeNode n, int row, int col, TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> cols) {
        if (n == null) return;
        cols.computeIfAbsent(col, k -> new TreeMap<>()).computeIfAbsent(row, k -> new PriorityQueue<>()).add(n.val);
        fill(n.left, row + 1, col - 1, cols);
        fill(n.right, row + 1, col + 1, cols);
    }

    public static void main(String[] args) {
        check("[3,9,20,null,null,15,7]", "[[9], [3, 15], [20], [7]]", verticalTraversal(build(3, 9, 20, null, null, 15, 7)));
        check("[1,2,3,4,5,6,7] (tie by value)", "[[4], [2], [1, 5, 6], [3], [7]]", verticalTraversal(build(1, 2, 3, 4, 5, 6, 7)));
        check("[1,2,3,4,6,5,7] (tie, reversed)", "[[4], [2], [1, 5, 6], [3], [7]]", verticalTraversal(build(1, 2, 3, 4, 6, 5, 7)));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, 1 + rnd.nextInt(40), 0, 20);
            ok &= verticalTraversal(r).equals(withTreeMaps(r));
        }
        check("500 random trees vs TreeMap+heap", true, ok);
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
