/*
 * ============================================================================
 *  BINARY TREE BASICS: vocabulary, building, ALL the traversals
 * ============================================================================
 *
 *  WHAT IS A TREE?
 *    A hierarchy of NODES. One ROOT at the top; every other node has exactly one
 *    PARENT; no cycles. In a BINARY tree each node has at most 2 children: left, right.
 *
 *                1            <- root (depth 0)
 *              /   \
 *             2     3         <- depth 1
 *            / \     \
 *           4   5     6       <- depth 2. 4, 5, 6 are LEAVES (no children)
 *
 *  VOCABULARY (interviewers use these words constantly)
 *    depth(node)  = edges from the root down to the node        depth(5) = 2
 *    height(node) = edges on the longest path down to a leaf    height(1) = 2
 *                   (some books count NODES instead: 3. LeetCode "max depth" counts nodes)
 *    subtree      = a node plus everything below it
 *    FULL tree    = every node has 0 or 2 children
 *    COMPLETE     = every level full except maybe the last, which fills from the LEFT
 *                   (that's what a heap is: 08-Heap)
 *    PERFECT      = every level completely full: 2^(h+1) - 1 nodes
 *    BALANCED     = for every node, left and right heights differ by at most 1
 *                   -> height is O(log n), which is what makes trees fast
 *    DEGENERATE   = every node has one child: really a linked list, height n - 1
 *
 *  REPRESENTATION
 *    Linked nodes (TreeNode with left/right pointers): the usual choice.
 *    An array (children of i at 2i+1, 2i+2): only for COMPLETE trees (heaps, segment trees).
 *
 *  THE 4 TRAVERSALS (visit every node once: O(n) time)
 *    DFS (go deep first) - named by WHEN the node itself is visited:
 *      PREORDER   node, left, right     1 2 4 5 3 6   copy/serialize a tree, top-down info
 *      INORDER    left, node, right     4 2 5 1 3 6   on a BST: SORTED order!
 *      POSTORDER  left, right, node     4 5 2 6 3 1   children before parent: heights,
 *                                                     sizes, deleting a tree (bottom-up)
 *    BFS (level by level, with a QUEUE):
 *      LEVEL ORDER                      1 2 3 4 5 6   shortest paths, per-level questions
 *
 *  RECURSIVE vs ITERATIVE
 *    Recursion uses the call stack: O(h) space. h = n for a degenerate tree, so a
 *    100,000-node "linked list" tree can throw StackOverflowError. The iterative
 *    versions use an explicit stack (same O(h), but on the heap, so no overflow).
 *    MORRIS traversal does inorder in O(1) extra space by temporarily rewiring pointers.
 *
 *  Run:  java BinaryTreeBasics.java
 * ============================================================================
 */

import java.util.*;

public class BinaryTreeBasics {

    // ---------------- 1. Recursive DFS: three lines each ----------------
    static void preorder(TreeNode n, List<Integer> out) {
        if (n == null) return;          // base case: empty subtree
        out.add(n.val);                 // NODE first
        preorder(n.left, out);
        preorder(n.right, out);
    }

    static void inorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        inorder(n.left, out);
        out.add(n.val);                 // NODE in the middle
        inorder(n.right, out);
    }

    static void postorder(TreeNode n, List<Integer> out) {
        if (n == null) return;
        postorder(n.left, out);
        postorder(n.right, out);
        out.add(n.val);                 // NODE last
    }

    // ---------------- 2. Iterative DFS with an explicit stack ----------------

    // Preorder: pop a node, visit it, push RIGHT then LEFT (so left is popped first).
    static List<Integer> preorderIterative(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        if (root == null) return out;
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            out.add(n.val);
            if (n.right != null) stack.push(n.right);
            if (n.left != null) stack.push(n.left);
        }
        return out;
    }

    // Inorder: go LEFT as far as possible (pushing), pop & visit, then step RIGHT once.
    //   This "push the whole left spine" loop is a pattern you'll reuse:
    //   Kth Smallest in BST (M04) and the BST iterator.
    static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {             // 1. slide down the left spine
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();                // 2. leftmost unvisited node
            out.add(cur.val);
            cur = cur.right;                  // 3. its right subtree comes next
        }
        return out;
    }

    // Postorder trick: do a "node, RIGHT, left" preorder, then REVERSE it -> "left, right, node".
    static List<Integer> postorderIterative(TreeNode root) {
        LinkedList<Integer> out = new LinkedList<>();
        if (root == null) return out;
        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode n = stack.pop();
            out.addFirst(n.val);              // adding at the FRONT = reversing as we go
            if (n.left != null) stack.push(n.left);
            if (n.right != null) stack.push(n.right);
        }
        return out;
    }

    // ---------------- 3. BFS: level order with a queue ----------------
    // The "size = queue.size()" snapshot lets you process exactly one level per loop.
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> levels = new ArrayList<>();
        if (root == null) return levels;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.offer(root);
        while (!q.isEmpty()) {
            int size = q.size();              // number of nodes on THIS level
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.offer(n.left);
                if (n.right != null) q.offer(n.right);
            }
            levels.add(level);
        }
        return levels;
    }

    // ---------------- 4. Morris inorder: O(1) extra space ----------------
    // Idea: before going left, make the rightmost node of the left subtree (the
    // inorder PREDECESSOR) point back to cur with a temporary "thread". When we
    // arrive at cur a second time through that thread, we know the left side is
    // done: remove the thread, visit cur, go right. The tree is restored at the end.
    static List<Integer> morrisInorder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        TreeNode cur = root;
        while (cur != null) {
            if (cur.left == null) {
                out.add(cur.val);
                cur = cur.right;
            } else {
                TreeNode pred = cur.left;
                while (pred.right != null && pred.right != cur) pred = pred.right;
                if (pred.right == null) {     // first visit: create the thread, go left
                    pred.right = cur;
                    cur = cur.left;
                } else {                      // second visit: left side done
                    pred.right = null;        // remove the thread (restore the tree)
                    out.add(cur.val);
                    cur = cur.right;
                }
            }
        }
        return out;
    }

    // ---------------- 5. Basic measurements: all "postorder" (children first) ----------------
    static int size(TreeNode n) { return n == null ? 0 : 1 + size(n.left) + size(n.right); }

    // height in EDGES: an empty tree is -1, a single node is 0
    static int height(TreeNode n) { return n == null ? -1 : 1 + Math.max(height(n.left), height(n.right)); }

    static int countLeaves(TreeNode n) {
        if (n == null) return 0;
        if (n.left == null && n.right == null) return 1;
        return countLeaves(n.left) + countLeaves(n.right);
    }

    static int sum(TreeNode n) { return n == null ? 0 : n.val + sum(n.left) + sum(n.right); }

    // Print the tree sideways (rotate your head 90 degrees left): right subtree on top.
    static void printSideways(TreeNode n, String indent) {
        if (n == null) return;
        printSideways(n.right, indent + "      ");
        System.out.println(indent + n.val);
        printSideways(n.left, indent + "      ");
    }

    static List<Integer> rec(TreeNode root, int which) {
        List<Integer> out = new ArrayList<>();
        if (which == 0) preorder(root, out);
        else if (which == 1) inorder(root, out);
        else postorder(root, out);
        return out;
    }

    public static void main(String[] args) {
        //        1
        //      /   \
        //     2     3
        //    / \     \
        //   4   5     6
        TreeNode root = build(1, 2, 3, 4, 5, null, 6);
        System.out.println("The tree, sideways (root on the left, right subtree on top):");
        printSideways(root, "   ");
        System.out.println();

        System.out.println("1. Recursive DFS");
        check("preorder  (node,left,right)", "[1, 2, 4, 5, 3, 6]", rec(root, 0));
        check("inorder   (left,node,right)", "[4, 2, 5, 1, 3, 6]", rec(root, 1));
        check("postorder (left,right,node)", "[4, 5, 2, 6, 3, 1]", rec(root, 2));

        System.out.println("2. Iterative DFS (explicit stack)");
        check("preorder iterative", "[1, 2, 4, 5, 3, 6]", preorderIterative(root));
        check("inorder iterative", "[4, 2, 5, 1, 3, 6]", inorderIterative(root));
        check("postorder iterative", "[4, 5, 2, 6, 3, 1]", postorderIterative(root));

        System.out.println("3. BFS level order");
        check("level order", "[[1], [2, 3], [4, 5, 6]]", levelOrder(root));

        System.out.println("4. Morris inorder (O(1) space)");
        check("morris inorder", "[4, 2, 5, 1, 3, 6]", morrisInorder(root));
        check("tree restored after Morris", "[1, 2, 3, 4, 5, null, 6]", root);

        System.out.println("5. Measurements");
        check("size", 6, size(root));
        check("height (edges)", 2, height(root));
        check("leaves", 3, countLeaves(root));
        check("sum", 21, sum(root));
        check("empty tree height", -1, height(null));

        System.out.println("6. Cross-check on 500 random trees");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(30), 0, 99);
            String before = String.valueOf(r);
            ok &= rec(r, 0).equals(preorderIterative(r));
            ok &= rec(r, 1).equals(inorderIterative(r));
            ok &= rec(r, 1).equals(morrisInorder(r));
            ok &= rec(r, 2).equals(postorderIterative(r));
            ok &= before.equals(String.valueOf(r));               // Morris left the tree intact
        }
        check("iterative + Morris == recursive", true, ok);

        System.out.println("7. Why iterative matters: a degenerate tree of 100,000 nodes");
        TreeNode chain = new TreeNode(0), tail = chain;
        for (int i = 1; i < 100_000; i++) { tail.left = new TreeNode(i); tail = tail.left; }
        String recursive;
        try {
            recursive = "size " + size(chain);
        } catch (StackOverflowError e) {
            recursive = "StackOverflowError";
        }
        System.out.println("   recursive size(): " + recursive + "  (depends on the JVM stack size; -Xss raises it)");
        check("iterative inorder handles it", 100_000, inorderIterative(chain).size());
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
