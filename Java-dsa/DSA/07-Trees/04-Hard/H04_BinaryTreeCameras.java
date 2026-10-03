/*
 * ============================================================================
 *  HARD 04 : BINARY TREE CAMERAS             LeetCode #968
 *  https://leetcode.com/problems/binary-tree-cameras/
 *  Pattern: GREEDY BOTTOM-UP with 3 STATES per node
 * ============================================================================
 *
 *  PROBLEM
 *    A camera on a node watches its parent, itself and its children.
 *    Minimum cameras to watch EVERY node?
 *          0                0
 *         /                /
 *        0*        ->     0*        1 camera: the middle node covers all three
 *       / \
 *      0   0
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT (greedy): never put a camera on a LEAF. A camera on the leaf's
 *  PARENT covers everything the leaf camera would (the leaf, the parent) plus
 *  more (the grandparent, the sibling). So work from the BOTTOM up, and place a
 *  camera only when a child would otherwise stay unwatched.
 *
 *  Every node reports ONE of 3 states to its parent (postorder):
 *     NOT_COVERED (0): nobody watches me yet - my parent MUST take a camera
 *     HAS_CAMERA  (1): I have a camera (so my parent is covered too)
 *     COVERED     (2): I'm watched (by a child), but have no camera
 *  null children count as COVERED (they need nothing and offer nothing).
 *
 *  Rules at a node, given its children's states:
 *     any child NOT_COVERED      -> put a camera here: cameras++, return HAS_CAMERA
 *     else any child HAS_CAMERA  -> return COVERED
 *     else (both COVERED)        -> return NOT_COVERED (let my parent handle me)
 *  At the end: if the ROOT is NOT_COVERED, it has no parent to help -> one more camera.
 *
 *  DRY RUN (picture)
 *    leaves: children null (COVERED) -> NOT_COVERED
 *    middle: a child NOT_COVERED -> camera (1), HAS_CAMERA
 *    root:   child HAS_CAMERA -> COVERED.  total 1
 *
 *  Why the ORDER of the first two rules matters: if one child is NOT_COVERED and
 *  the other has a camera, we still need a camera here - that unwatched child has
 *  no other way to be covered.
 *
 *  COMPLEXITY   Time O(n)   Space O(h)
 * ============================================================================
 */

import java.util.*;

public class H04_BinaryTreeCameras {

    static final int NOT_COVERED = 0, HAS_CAMERA = 1, COVERED = 2;
    static int cameras;

    static int minCameraCover(TreeNode root) {
        cameras = 0;
        if (state(root) == NOT_COVERED) cameras++;          // the root has no parent to rely on
        return cameras;
    }

    static int state(TreeNode n) {
        if (n == null) return COVERED;
        int l = state(n.left), r = state(n.right);
        if (l == NOT_COVERED || r == NOT_COVERED) {          // a child needs me
            cameras++;
            return HAS_CAMERA;
        }
        if (l == HAS_CAMERA || r == HAS_CAMERA) return COVERED;
        return NOT_COVERED;
    }

    // brute force: smallest subset of nodes that covers everything (small trees)
    static int brute(TreeNode root) {
        if (root == null) return 0;
        List<TreeNode> nodes = new ArrayList<>();
        Map<TreeNode, TreeNode> parent = new HashMap<>();
        collect(root, null, nodes, parent);
        int n = nodes.size(), best = n;
        for (int mask = 0; mask < (1 << n); mask++) {
            int count = Integer.bitCount(mask);
            if (count >= best) continue;
            Set<TreeNode> watched = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask >> i & 1) == 0) continue;
                TreeNode c = nodes.get(i);
                watched.add(c);
                if (parent.get(c) != null) watched.add(parent.get(c));
                if (c.left != null) watched.add(c.left);
                if (c.right != null) watched.add(c.right);
            }
            if (watched.size() == n) best = count;
        }
        return best;
    }

    static void collect(TreeNode n, TreeNode p, List<TreeNode> nodes, Map<TreeNode, TreeNode> parent) {
        if (n == null) return;
        nodes.add(n);
        parent.put(n, p);
        collect(n.left, n, nodes, parent);
        collect(n.right, n, nodes, parent);
    }

    public static void main(String[] args) {
        check("[0,0,null,0,0]", 1, minCameraCover(build(0, 0, null, 0, 0)));
        check("[0,0,null,0,null,0,null,null,0]", 2, minCameraCover(build(0, 0, null, 0, null, 0, null, null, 0)));
        check("[0] (single node)", 1, minCameraCover(build(0)));
        check("[0,0,0] (root covers both)", 1, minCameraCover(build(0, 0, 0)));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, 1 + rnd.nextInt(14), 0, 0);
            ok &= minCameraCover(r) == brute(r);
        }
        check("500 random trees vs all subsets", true, ok);
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
