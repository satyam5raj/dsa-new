/*
 * ============================================================================
 *  HARD 01 : SERIALIZE AND DESERIALIZE BINARY TREE   LeetCode #297
 *  https://leetcode.com/problems/serialize-and-deserialize-binary-tree/
 *  Pattern: PREORDER WITH NULL MARKERS -> one traversal fully describes a tree
 * ============================================================================
 *
 *  PROBLEM
 *    serialize(root) -> String,  deserialize(String) -> the identical tree.
 *    Any format is fine, as long as the round trip is exact.
 *
 *  ---------------------------------------------------------------------------
 *  WHY ONE TRAVERSAL ISN'T NORMALLY ENOUGH: preorder 1 2 could be "2 is the left
 *  child" or "2 is the right child" (M07 needed preorder AND inorder).
 *  FIX: write the NULLS too. Then preorder alone is unambiguous:
 *
 *          1
 *         / \           preorder with nulls:  1,2,#,#,3,4,#,#,5,#,#
 *        2   3
 *           / \
 *          4   5
 *
 *  SERIALIZE (preorder):  null -> "#";  else  val, serialize(left), serialize(right)
 *
 *  DESERIALIZE: read the tokens IN THE SAME ORDER, with a shared pointer/queue:
 *     build():
 *        token = next()
 *        "#" -> return null
 *        node = new TreeNode(token)
 *        node.left  = build()      <- consumes exactly the left subtree's tokens
 *        node.right = build()      <- then the right subtree's
 *        return node
 *  The null markers tell the recursion exactly when each subtree ends.
 *
 *  BFS VERSION (LeetCode's own format "[1,2,3,null,null,4,5]"): serialize level
 *  by level writing nulls; deserialize with a queue of parents, assigning the
 *  next two tokens as each parent's left/right. Also shown below.
 *
 *  INTERVIEW TALKING POINTS: separators matter (multi-digit and negative values);
 *  nulls make it O(n) size (n+1 null markers for n nodes); the recursive version
 *  can overflow the stack on a 10^5-deep chain, the BFS version can't.
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H01_SerializeDeserializeBinaryTree {

    // ---------- Preorder (DFS) codec ----------
    static String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        write(root, sb);
        return sb.substring(0, sb.length() - 1);                 // drop the trailing comma
    }

    static void write(TreeNode n, StringBuilder sb) {
        if (n == null) { sb.append("#,"); return; }
        sb.append(n.val).append(',');
        write(n.left, sb);
        write(n.right, sb);
    }

    static TreeNode deserialize(String data) {
        Deque<String> tokens = new ArrayDeque<>(Arrays.asList(data.split(",")));
        return read(tokens);
    }

    static TreeNode read(Deque<String> tokens) {
        String t = tokens.poll();
        if (t.equals("#")) return null;
        TreeNode n = new TreeNode(Integer.parseInt(t));
        n.left = read(tokens);                                     // eats the whole left subtree
        n.right = read(tokens);
        return n;
    }

    // ---------- Level-order (BFS) codec, LeetCode's format ----------
    static String serializeBfs(TreeNode root) {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> q = new LinkedList<>();                    // allows nulls
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            if (n == null) { sb.append("#,"); continue; }
            sb.append(n.val).append(',');
            q.add(n.left);
            q.add(n.right);
        }
        return sb.toString();
    }

    static TreeNode deserializeBfs(String data) {
        if (data.isEmpty()) return null;
        String[] t = data.split(",");
        TreeNode root = new TreeNode(Integer.parseInt(t[0]));
        Queue<TreeNode> parents = new ArrayDeque<>();
        parents.add(root);
        for (int i = 1; i < t.length; i += 2) {
            TreeNode p = parents.poll();                           // the next two tokens are p's children
            if (!t[i].equals("#")) { p.left = new TreeNode(Integer.parseInt(t[i])); parents.add(p.left); }
            if (i + 1 < t.length && !t[i + 1].equals("#")) { p.right = new TreeNode(Integer.parseInt(t[i + 1])); parents.add(p.right); }
        }
        return root;
    }

    public static void main(String[] args) {
        TreeNode t = build(1, 2, 3, null, null, 4, 5);
        check("serialize (preorder)", "1,2,#,#,3,4,#,#,5,#,#", serialize(t));
        check("round trip", "[1, 2, 3, null, null, 4, 5]", deserialize(serialize(t)));
        check("empty tree", "#", serialize(null));
        check("empty round trip", null, deserialize(serialize(null)));
        check("negative / multi-digit", "[-10, null, 2147483647]", deserialize(serialize(build(-10, null, Integer.MAX_VALUE))));
        check("BFS format", "1,2,3,#,#,4,5,#,#,#,#,", serializeBfs(t));
        check("BFS round trip", "[1, 2, 3, null, null, 4, 5]", deserializeBfs(serializeBfs(t)));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            TreeNode r = randomTree(rnd, rnd.nextInt(50), -1000, 1000);
            String exp = String.valueOf(r);
            ok &= String.valueOf(deserialize(serialize(r))).equals(exp);
            ok &= String.valueOf(deserializeBfs(serializeBfs(r))).equals(exp);
        }
        check("1,000 random trees, both codecs", true, ok);
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
