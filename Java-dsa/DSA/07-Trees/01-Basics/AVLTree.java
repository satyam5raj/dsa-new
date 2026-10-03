/*
 * ============================================================================
 *  AVL TREE: a BST that keeps itself balanced
 * ============================================================================
 *
 *  THE PROBLEM: a plain BST can degenerate into a list (sorted inserts -> height n).
 *
 *  THE AVL RULE (Adelson-Velsky & Landis, 1962): for EVERY node
 *      balance = height(left) - height(right)  must be -1, 0 or +1
 *  This guarantees height <= ~1.44 * log2(n): every operation is O(log n), always.
 *
 *  HOW: each node stores its height. After an insert/delete, walk back UP the path
 *  (the recursion unwinding does this for free), update heights, and if a node's
 *  balance becomes +2 or -2, fix it with ROTATIONS.
 *
 *  A ROTATION re-hangs 3 nodes while keeping the BST order (inorder unchanged):
 *
 *     RIGHT ROTATION at y                 LEFT ROTATION at x
 *           y                x               x                    y
 *          / \              / \             / \                  / \
 *         x   C    --->    A   y           A   y      --->      x   C
 *        / \                  / \             / \              / \
 *       A   B                B   C           B   C            A   B
 *     (B moves from x's right to y's left; A < x < B < y < C in both pictures)
 *
 *  THE 4 IMBALANCE CASES (named by the path from the unbalanced node to the new node)
 *    LL: balance +2, left child leans left (or even)   -> rotateRight(node)
 *    RR: balance -2, right child leans right (or even) -> rotateLeft(node)
 *    LR: balance +2, left child leans RIGHT   -> rotateLeft(node.left), then rotateRight(node)
 *    RL: balance -2, right child leans LEFT   -> rotateRight(node.right), then rotateLeft(node)
 *
 *  Example: insert 1, 2, 3 (sorted!)
 *     1                    2
 *      \     RR case      / \
 *       2   -------->    1   3
 *        \  rotateLeft(1)
 *         3
 *
 *  AVL vs RED-BLACK (Java's TreeMap)
 *    AVL is more strictly balanced -> faster lookups.
 *    Red-black allows height up to 2*log2(n) but needs fewer rotations on insert/delete
 *    -> faster writes. That's why most libraries pick red-black.
 *  Interviews: know the rotations and the 4 cases; nobody asks for red-black code.
 *
 *  Run:  java AVLTree.java
 * ============================================================================
 */

import java.util.*;

public class AVLTree {

    static class AVL {
        static class Node {
            int key, height = 0;          // a leaf has height 0
            Node left, right;
            Node(int key) { this.key = key; }
        }

        Node root;
        int size, rotations;

        static int h(Node n) { return n == null ? -1 : n.height; }
        static int balance(Node n) { return h(n.left) - h(n.right); }
        static void update(Node n) { n.height = 1 + Math.max(h(n.left), h(n.right)); }

        Node rotateRight(Node y) {
            Node x = y.left;
            y.left = x.right;            // B moves over
            x.right = y;
            update(y);                   // y is now BELOW x: update it first
            update(x);
            rotations++;
            return x;                    // x is the new root of this subtree
        }

        Node rotateLeft(Node x) {
            Node y = x.right;
            x.right = y.left;
            y.left = x;
            update(x);
            update(y);
            rotations++;
            return y;
        }

        // Called on every node on the way back up after an insert or delete.
        Node rebalance(Node n) {
            update(n);
            int b = balance(n);
            if (b > 1) {                                 // left-heavy
                if (balance(n.left) < 0) n.left = rotateLeft(n.left);     // LR -> turn it into LL
                return rotateRight(n);                                     // LL
            }
            if (b < -1) {                                // right-heavy
                if (balance(n.right) > 0) n.right = rotateRight(n.right); // RL -> turn it into RR
                return rotateLeft(n);                                      // RR
            }
            return n;                                    // already balanced
        }

        boolean insert(int key) {
            int before = size;
            root = insert(root, key);
            return size > before;
        }

        private Node insert(Node n, int key) {
            if (n == null) { size++; return new Node(key); }
            if (key < n.key) n.left = insert(n.left, key);
            else if (key > n.key) n.right = insert(n.right, key);
            else return n;                               // duplicate: nothing changed
            return rebalance(n);                         // <- the ONLY difference from a plain BST
        }

        boolean delete(int key) {
            int before = size;
            root = delete(root, key);
            return size < before;
        }

        private Node delete(Node n, int key) {
            if (n == null) return null;
            if (key < n.key) n.left = delete(n.left, key);
            else if (key > n.key) n.right = delete(n.right, key);
            else {
                if (n.left == null) { size--; return n.right; }
                if (n.right == null) { size--; return n.left; }
                Node succ = n.right;
                while (succ.left != null) succ = succ.left;
                n.key = succ.key;
                n.right = delete(n.right, succ.key);
            }
            return rebalance(n);
        }

        boolean contains(int key) {
            for (Node n = root; n != null; n = key < n.key ? n.left : n.right) if (n.key == key) return true;
            return false;
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

        // Checks the BST order, the stored heights AND the AVL balance rule at every node.
        boolean isValidAVL() { return check(root, Long.MIN_VALUE, Long.MAX_VALUE) != Integer.MIN_VALUE; }

        private int check(Node n, long low, long high) {
            if (n == null) return -1;
            if (n.key <= low || n.key >= high) return Integer.MIN_VALUE;
            int l = check(n.left, low, n.key), r = check(n.right, n.key, high);
            if (l == Integer.MIN_VALUE || r == Integer.MIN_VALUE || Math.abs(l - r) > 1) return Integer.MIN_VALUE;
            int height = 1 + Math.max(l, r);
            return height == n.height ? height : Integer.MIN_VALUE;
        }

        String shape(Node n) {   // compact picture: key(left,right)
            if (n == null) return "-";
            if (n.left == null && n.right == null) return String.valueOf(n.key);
            return n.key + "(" + shape(n.left) + "," + shape(n.right) + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("1. The four rotation cases (inserting 3 keys each)");
        AVL ll = new AVL(); for (int k : new int[]{3, 2, 1}) ll.insert(k);
        AVL rr = new AVL(); for (int k : new int[]{1, 2, 3}) rr.insert(k);
        AVL lr = new AVL(); for (int k : new int[]{3, 1, 2}) lr.insert(k);
        AVL rl = new AVL(); for (int k : new int[]{1, 3, 2}) rl.insert(k);
        check("LL: 3,2,1 -> one right rotation", "2(1,3) rotations=1", ll.shape(ll.root) + " rotations=" + ll.rotations);
        check("RR: 1,2,3 -> one left rotation", "2(1,3) rotations=1", rr.shape(rr.root) + " rotations=" + rr.rotations);
        check("LR: 3,1,2 -> left then right", "2(1,3) rotations=2", lr.shape(lr.root) + " rotations=" + lr.rotations);
        check("RL: 1,3,2 -> right then left", "2(1,3) rotations=2", rl.shape(rl.root) + " rotations=" + rl.rotations);

        System.out.println("2. Sorted inserts 1..7 stay perfectly balanced");
        AVL seven = new AVL();
        for (int k = 1; k <= 7; k++) seven.insert(k);
        check("shape", "4(2(1,3),6(5,7))", seven.shape(seven.root));

        System.out.println("3. 1,000,000 SORTED inserts (a plain BST would have height 999,999)");
        AVL big = new AVL();
        for (int k = 0; k < 1_000_000; k++) big.insert(k);
        System.out.printf("   height = %d   (log2(1,000,000) ~ 19.9, AVL bound ~ 1.44 * 19.9 = 28.7)%n", big.root.height);
        check("height stays logarithmic", true, big.root.height <= 28);
        check("AVL invariants hold", true, big.isValidAVL());

        System.out.println("4. 200,000 random insert/delete vs TreeSet, invariants checked along the way");
        Random rnd = new Random(4);
        AVL mine = new AVL();
        TreeSet<Integer> ref = new TreeSet<>();
        boolean ok = true;
        for (int i = 0; i < 200_000; i++) {
            int k = rnd.nextInt(5000);
            if (rnd.nextInt(3) > 0) ok &= mine.insert(k) == ref.add(k);
            else ok &= mine.delete(k) == ref.remove(k);
            if (i % 10_000 == 0) ok &= mine.isValidAVL();
            ok &= mine.contains(k) == ref.contains(k);
        }
        ok &= mine.inorder().equals(new ArrayList<>(ref)) && mine.isValidAVL();
        check("matches TreeSet, always balanced", true, ok);
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
