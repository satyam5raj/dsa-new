/*
 * ============================================================================
 *  MEDIUM 14 : ALL NODES DISTANCE K IN BINARY TREE   LeetCode #863
 *  https://leetcode.com/problems/all-nodes-distance-k-in-binary-tree/
 *  Pattern: TREE -> GRAPH (add PARENT links), then BFS from the target
 * ============================================================================
 *
 *  PROBLEM
 *    Return the values of all nodes exactly k edges away from `target`.
 *                3
 *              /   \
 *             5     1          target 5, k 2 -> [7, 4, 1]
 *            / \   / \         (7 and 4 are 2 steps DOWN, 1 is up to 3 then down)
 *           6   2 0   8
 *              / \
 *             7   4
 *
 *  ---------------------------------------------------------------------------
 *  THE DIFFICULTY: trees only point DOWN. Distance k may need going UP first.
 *
 *  INSIGHT: a tree is just an undirected graph. Give every node a link to its
 *  PARENT (a HashMap node -> parent, built with one DFS). Now each node has up to
 *  3 neighbours: left, right, parent. Then it's plain BFS:
 *    start at target; expand level by level, k times, with a VISITED set
 *    (otherwise you'd go down to a child and straight back up to where you came from);
 *    whatever is in the queue after k levels is the answer.
 *
 *  DRY RUN target 5, k 2
 *    level 0: {5}
 *    level 1: 6, 2, 3 (parent)
 *    level 2: from 6: none; from 2: 7, 4; from 3: 1 (5 already visited) -> [7, 4, 1]
 *
 *  This "convert to a graph, then BFS" move also solves: burning tree (#2385),
 *  closest leaf (#742). Graph BFS is covered fully in 09-Graph.
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M14_AllNodesDistanceK {

    static List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        Map<TreeNode, TreeNode> parent = new HashMap<>();
        linkParents(root, null, parent);

        Queue<TreeNode> q = new ArrayDeque<>();
        Set<TreeNode> visited = new HashSet<>();
        q.add(target);
        visited.add(target);
        for (int dist = 0; dist < k && !q.isEmpty(); dist++) {       // expand k levels
            for (int i = q.size(); i > 0; i--) {
                TreeNode n = q.poll();
                for (TreeNode next : new TreeNode[]{n.left, n.right, parent.get(n)}) {
                    if (next != null && visited.add(next)) q.add(next);
                }
            }
        }
        List<Integer> result = new ArrayList<>();
        for (TreeNode n : q) result.add(n.val);
        return result;
    }

    static void linkParents(TreeNode n, TreeNode p, Map<TreeNode, TreeNode> parent) {
        if (n == null) return;
        parent.put(n, p);
        linkParents(n.left, n, parent);
        linkParents(n.right, n, parent);
    }

    // referee: distance via LCA depths: dist(a,b) = depth(a) + depth(b) - 2*depth(lca)
    static List<Integer> brute(TreeNode root, TreeNode target, int k) {
        Map<TreeNode, TreeNode> parent = new HashMap<>();
        linkParents(root, null, parent);
        List<TreeNode> targetPath = pathUp(target, parent);
        List<Integer> out = new ArrayList<>();
        for (TreeNode n : parent.keySet()) {
            List<TreeNode> p = pathUp(n, parent);
            Set<TreeNode> onP = new HashSet<>(p);
            int up = 0;
            for (TreeNode a : targetPath) { if (onP.contains(a)) { if (up + p.indexOf(a) == k) out.add(n.val); break; } up++; }
        }
        return out;
    }

    static List<TreeNode> pathUp(TreeNode n, Map<TreeNode, TreeNode> parent) {
        List<TreeNode> p = new ArrayList<>();
        for (; n != null; n = parent.get(n)) p.add(n);
        return p;
    }

    static TreeNode find(TreeNode n, int v) {
        if (n == null || n.val == v) return n;
        TreeNode l = find(n.left, v);
        return l != null ? l : find(n.right, v);
    }

    static List<Integer> sorted(List<Integer> l) { List<Integer> c = new ArrayList<>(l); Collections.sort(c); return c; }

    public static void main(String[] args) {
        TreeNode t = build(3, 5, 1, 6, 2, 0, 8, null, null, 7, 4);
        check("target 5, k 2", "[1, 4, 7]", sorted(distanceK(t, find(t, 5), 2)));
        check("target 5, k 0", "[5]", distanceK(t, find(t, 5), 0));
        check("target 7, k 3 (up 3 or up 2 + down 1)", "[3, 6]", sorted(distanceK(t, find(t, 7), 3)));
        check("k too large", "[]", distanceK(t, find(t, 5), 10));
        Random rnd = new Random(14);
        boolean ok = true;
        for (int i = 0; i < 300; i++) {
            TreeNode r = randomTree(rnd, 1 + rnd.nextInt(30), 0, 0);
            int[] next = {0};
            relabel(r, next);
            TreeNode target = find(r, rnd.nextInt(next[0]));
            int k = rnd.nextInt(6);
            ok &= sorted(distanceK(r, target, k)).equals(sorted(brute(r, target, k)));
        }
        check("300 random trees vs LCA distance", true, ok);
        summary();
    }

    static void relabel(TreeNode n, int[] next) {
        if (n == null) return;
        n.val = next[0]++;
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
