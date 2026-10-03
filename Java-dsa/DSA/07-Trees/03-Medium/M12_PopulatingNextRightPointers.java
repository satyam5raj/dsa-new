/*
 * ============================================================================
 *  MEDIUM 12 : POPULATING NEXT RIGHT POINTERS IN EACH NODE   LeetCode #116
 *  https://leetcode.com/problems/populating-next-right-pointers-in-each-node/
 *  Pattern: use the level you ALREADY linked to link the next one (O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    A PERFECT binary tree (all leaves on the same level, every parent has 2 children).
 *    Set each node's `next` to the node on its right at the same level (null at the end).
 *              1 -> null
 *             / \
 *            2 -> 3 -> null
 *           / \  / \
 *          4->5->6->7 -> null
 *
 *  ---------------------------------------------------------------------------
 *  EASY WAY: BFS level by level; link each polled node to the next one in the
 *  same level. O(n) time, but O(width) space for the queue.
 *
 *  O(1) SPACE: once a level is linked, it IS a linked list. Walk it, and wire the
 *  level below. For each node `cur` on the current level:
 *     cur.left.next  = cur.right                 (siblings: same parent)
 *     cur.right.next = cur.next.left             (cousins across parents, if cur.next exists)
 *  Then go down: leftmost = leftmost.left.
 *
 *  DRY RUN
 *    level 1 (just 1): 2.next = 3; (1.next null)
 *    level 2 (2 -> 3): at 2: 4.next = 5, 5.next = 3.left = 6;  at 3: 6.next = 7
 *    level 3: leaves -> stop
 *
 *  (#117 is the same for ANY tree: there, keep a dummy head + tail for the
 *   next level while walking the current one.)
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M12_PopulatingNextRightPointers {

    static class Node {
        int val;
        Node left, right, next;
        Node(int val) { this.val = val; }
    }

    static Node connect(Node root) {
        Node leftmost = root;
        while (leftmost != null && leftmost.left != null) {     // while there is a level below
            for (Node cur = leftmost; cur != null; cur = cur.next) {   // walk the linked level
                cur.left.next = cur.right;                       // siblings
                if (cur.next != null) cur.right.next = cur.next.left;   // across parents
            }
            leftmost = leftmost.left;                            // go down one level
        }
        return root;
    }

    // a perfect tree of the given depth, values 1, 2, 3... in level order
    static Node perfect(int depth) {
        if (depth == 0) return null;
        List<Node> level = new ArrayList<>();
        int[] next = {1};
        Node root = new Node(next[0]++);
        level.add(root);
        for (int d = 1; d < depth; d++) {
            List<Node> below = new ArrayList<>();
            for (Node n : level) {
                n.left = new Node(next[0]++);
                n.right = new Node(next[0]++);
                below.add(n.left);
                below.add(n.right);
            }
            level = below;
        }
        return root;
    }

    // LeetCode's output format: each level by next pointers, '#' marks the end of a level
    static String serialize(Node root) {
        StringBuilder sb = new StringBuilder();
        for (Node leftmost = root; leftmost != null; leftmost = leftmost.left) {
            for (Node n = leftmost; n != null; n = n.next) sb.append(n.val).append(',');
            sb.append("#,");
        }
        return sb.length() == 0 ? "[]" : "[" + sb.substring(0, sb.length() - 1) + "]";
    }

    public static void main(String[] args) {
        check("perfect depth 3", "[1,#,2,3,#,4,5,6,7,#]", serialize(connect(perfect(3))));
        check("single node", "[1,#]", serialize(connect(perfect(1))));
        check("empty", "[]", serialize(connect(null)));
        boolean ok = true;
        for (int d = 1; d <= 12; d++) {
            Node r = connect(perfect(d));
            // expected: level k holds 2^k consecutive values
            StringBuilder exp = new StringBuilder("[");
            int v = 1;
            for (int k = 0; k < d; k++) {
                for (int i = 0; i < (1 << k); i++) exp.append(v++).append(',');
                exp.append("#,");
            }
            ok &= serialize(r).equals(exp.substring(0, exp.length() - 1) + "]");
        }
        check("depths 1..12 (up to 4,095 nodes)", true, ok);
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
