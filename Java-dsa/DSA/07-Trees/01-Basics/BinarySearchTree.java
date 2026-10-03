/*
 * ============================================================================
 *  BINARY SEARCH TREE (BST) FROM SCRATCH
 * ============================================================================
 *
 *  THE BST PROPERTY (for EVERY node, not just its children):
 *     all keys in the LEFT subtree  <  node.key  <  all keys in the RIGHT subtree
 *
 *                 8
 *               /   \
 *              3     10
 *             / \      \
 *            1   6      14
 *               / \     /
 *              4   7   13
 *
 *  Consequences:
 *    - INORDER traversal visits the keys in SORTED order: 1 3 4 6 7 8 10 13 14
 *    - search is like binary search: compare, then go left OR right -> O(h)
 *    - min = the leftmost node, max = the rightmost node
 *
 *  OPERATIONS (all O(h), where h = the height)
 *    search(k)   go left if k < node, right if k > node, stop when equal or null
 *    insert(k)   search for k; attach the new node where the search fell off (a null spot)
 *    delete(k)   3 cases (the classic interview question):
 *       1. LEAF        -> just remove it
 *       2. ONE CHILD   -> replace the node with its child
 *       3. TWO CHILDREN-> copy in the inorder SUCCESSOR (min of the right subtree),
 *                         then delete the successor from the right subtree
 *                         (the successor has no left child, so that's case 1 or 2)
 *    floor(k)    largest key <= k        ceiling(k)  smallest key >= k
 *
 *  THE CATCH: h depends on the INSERTION ORDER
 *    random order   -> h ~ 3 * log2(n) on average: about 60 for a million keys
 *    SORTED order   -> every insert goes right: a linked list, h = n - 1 -> O(n) per op!
 *  Fix: SELF-BALANCING trees keep h = O(log n) no matter what:
 *    AVL (next file), Red-Black (Java's TreeMap/TreeSet), B-trees (databases).
 *
 *  Run:  java BinarySearchTree.java
 * ============================================================================
 */

import java.util.*;

public class BinarySearchTree {

    static class BST {
        static class Node {
            int key;
            Node left, right;
            Node(int key) { this.key = key; }
        }

        Node root;
        int size;

        boolean contains(int key) {
            Node n = root;
            while (n != null) {
                if (key == n.key) return true;
                n = key < n.key ? n.left : n.right;   // discard half the tree each step
            }
            return false;
        }

        // Recursive insert: returns the (possibly new) root of this subtree.
        // "node.left = insert(node.left, key)" is THE idiom for modifying trees recursively.
        boolean insert(int key) {
            int before = size;
            root = insert(root, key);
            return size > before;
        }

        private Node insert(Node node, int key) {
            if (node == null) { size++; return new Node(key); }   // fell off the tree: attach here
            if (key < node.key) node.left = insert(node.left, key);
            else if (key > node.key) node.right = insert(node.right, key);
            // key == node.key: already present, a set ignores duplicates
            return node;
        }

        boolean delete(int key) {
            int before = size;
            root = delete(root, key);
            return size < before;
        }

        private Node delete(Node node, int key) {
            if (node == null) return null;                          // not found
            if (key < node.key) {
                node.left = delete(node.left, key);
            } else if (key > node.key) {
                node.right = delete(node.right, key);
            } else {                                                // FOUND it
                if (node.left == null) { size--; return node.right; }   // cases 1 and 2
                if (node.right == null) { size--; return node.left; }   // case 2
                Node succ = node.right;                             // case 3: two children
                while (succ.left != null) succ = succ.left;         // min of the right subtree
                node.key = succ.key;                                // copy the successor in
                node.right = delete(node.right, succ.key);          // remove the original successor
            }
            return node;
        }

        Integer min() {
            if (root == null) return null;
            Node n = root;
            while (n.left != null) n = n.left;
            return n.key;
        }

        Integer max() {
            if (root == null) return null;
            Node n = root;
            while (n.right != null) n = n.right;
            return n.key;
        }

        // Largest key <= k. Going right means "node.key is a candidate, look for a bigger one".
        Integer floor(int k) {
            Integer best = null;
            for (Node n = root; n != null; ) {
                if (n.key == k) return k;
                if (n.key < k) { best = n.key; n = n.right; }
                else n = n.left;
            }
            return best;
        }

        // Smallest key >= k: the mirror image.
        Integer ceiling(int k) {
            Integer best = null;
            for (Node n = root; n != null; ) {
                if (n.key == k) return k;
                if (n.key > k) { best = n.key; n = n.left; }
                else n = n.right;
            }
            return best;
        }

        List<Integer> inorder() {
            List<Integer> out = new ArrayList<>();
            inorder(root, out);
            return out;
        }

        private void inorder(Node n, List<Integer> out) {
            if (n == null) return;
            inorder(n.left, out);
            out.add(n.key);
            inorder(n.right, out);
        }

        // height in edges, computed iteratively with BFS so a degenerate tree can't overflow the stack
        int height() {
            if (root == null) return -1;
            Queue<Node> q = new ArrayDeque<>();
            q.add(root);
            int levels = 0;
            while (!q.isEmpty()) {
                for (int i = q.size(); i > 0; i--) {
                    Node n = q.poll();
                    if (n.left != null) q.add(n.left);
                    if (n.right != null) q.add(n.right);
                }
                levels++;
            }
            return levels - 1;
        }

        // Validate the BST property with a (low, high) RANGE per node (see M03 Validate BST).
        boolean isValid() { return isValid(root, Long.MIN_VALUE, Long.MAX_VALUE); }

        private boolean isValid(Node n, long low, long high) {
            if (n == null) return true;
            if (n.key <= low || n.key >= high) return false;
            return isValid(n.left, low, n.key) && isValid(n.right, n.key, high);
        }
    }

    public static void main(String[] args) {
        BST bst = new BST();
        for (int k : new int[]{8, 3, 10, 1, 6, 14, 4, 7, 13}) bst.insert(k);

        System.out.println("1. Build, search, inorder");
        check("inorder is SORTED", "[1, 3, 4, 6, 7, 8, 10, 13, 14]", bst.inorder());
        check("contains 6", true, bst.contains(6));
        check("contains 5", false, bst.contains(5));
        check("insert duplicate 6", false, bst.insert(6));
        check("min / max", "1 / 14", bst.min() + " / " + bst.max());
        check("floor(5) / ceiling(5)", "4 / 6", bst.floor(5) + " / " + bst.ceiling(5));
        check("floor(0) (nothing <= 0)", null, bst.floor(0));
        check("height", 3, bst.height());

        System.out.println("2. Delete: the three cases");
        bst.delete(1);
        check("delete leaf 1", "[3, 4, 6, 7, 8, 10, 13, 14]", bst.inorder());
        bst.delete(10);
        check("delete 10 (one child)", "[3, 4, 6, 7, 8, 13, 14]", bst.inorder());
        bst.delete(3);
        check("delete 3 (two children)", "[4, 6, 7, 8, 13, 14]", bst.inorder());
        bst.delete(8);
        check("delete the root 8", "[4, 6, 7, 13, 14]", bst.inorder());
        check("still a valid BST", true, bst.isValid());
        check("delete absent 99", false, bst.delete(99));

        System.out.println("3. 100,000 random ops vs java.util.TreeSet (a red-black tree)");
        Random rnd = new Random(2);
        BST mine = new BST();
        TreeSet<Integer> ref = new TreeSet<>();
        boolean ok = true;
        for (int i = 0; i < 100_000; i++) {
            int k = rnd.nextInt(2000);
            switch (rnd.nextInt(4)) {
                case 0, 1 -> ok &= mine.insert(k) == ref.add(k);
                case 2 -> ok &= mine.delete(k) == ref.remove(k);
                default -> ok &= Objects.equals(mine.floor(k), ref.floor(k)) && Objects.equals(mine.ceiling(k), ref.ceiling(k));
            }
        }
        ok &= mine.inorder().equals(new ArrayList<>(ref)) && mine.size == ref.size() && mine.isValid();
        check("BST matches TreeSet", true, ok);

        System.out.println("4. Insertion order decides the height");
        BST random = new BST(), sorted = new BST();
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < 5000; i++) keys.add(i);
        for (int k : keys) sorted.insert(k);                   // 0, 1, 2, ... every insert goes right
        Collections.shuffle(keys, new Random(3));
        for (int k : keys) random.insert(k);
        System.out.printf("   5,000 keys: random order height = %d,  sorted order height = %d  (log2(5000) ~ 12.3)%n",
                random.height(), sorted.height());
        check("sorted inserts degenerate into a list", 4999, sorted.height());
        check("random inserts stay shallow (< 40)", true, random.height() < 40);
        summary();
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
