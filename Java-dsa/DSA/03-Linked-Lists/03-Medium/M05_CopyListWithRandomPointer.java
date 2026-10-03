/*
 * ============================================================================
 *  MEDIUM 05 : COPY LIST WITH RANDOM POINTER   LeetCode #138
 *  https://leetcode.com/problems/copy-list-with-random-pointer/
 *  Pattern: HASHMAP old -> new  /  INTERLEAVING (O(1) extra space)
 * ============================================================================
 *
 *  PROBLEM
 *    Each node has 'next' AND 'random' (pointing to ANY node in the list, or null).
 *    Make a DEEP COPY: brand-new nodes, none of the copy's pointers may point
 *    into the original list.
 *
 *  ---------------------------------------------------------------------------
 *  THE DIFFICULTY: when copying node A, A.random may point to a node we haven't
 *  copied yet. Which NEW node should copyA.random point to?
 *
 *  APPROACH 1: HASHMAP original -> copy (two passes)        O(n) time, O(n) space
 *    pass 1: create a copy of every node, map.put(orig, copy)
 *    pass 2: copy.next   = map.get(orig.next)
 *            copy.random = map.get(orig.random)
 *    The map answers "which copy corresponds to this original node?"
 *
 *  APPROACH 2: INTERLEAVE (no map)                         O(n) time, O(1) extra
 *    1. insert each copy RIGHT AFTER its original:   A -> A' -> B -> B' -> C -> C'
 *       Now the copy of X is simply X.next (the list itself acts as the map!)
 *    2. set randoms:  X'.random = X.random == null ? null : X.random.next
 *    3. separate the two lists (restore the original, extract the copies)
 *
 *  COMPLEXITY   both O(n) time; approach 2 uses O(1) extra space
 * ============================================================================
 */

import java.util.*;

public class M05_CopyListWithRandomPointer {

    static class Node {
        int val;
        Node next, random;
        Node(int val) { this.val = val; }
    }

    // Approach 1: HashMap
    static Node copyRandomList(Node head) {
        Map<Node, Node> copyOf = new HashMap<>();
        for (Node c = head; c != null; c = c.next) copyOf.put(c, new Node(c.val));
        for (Node c = head; c != null; c = c.next) {
            copyOf.get(c).next = copyOf.get(c.next);        // get(null) -> null, conveniently
            copyOf.get(c).random = copyOf.get(c.random);
        }
        return copyOf.get(head);
    }

    // Approach 2: interleaving, O(1) extra space
    static Node copyRandomListInterleave(Node head) {
        if (head == null) return null;
        for (Node c = head; c != null; c = c.next.next) {          // 1. A -> A' -> B -> B'
            Node copy = new Node(c.val);
            copy.next = c.next;
            c.next = copy;
        }
        for (Node c = head; c != null; c = c.next.next) {          // 2. randoms
            c.next.random = (c.random == null) ? null : c.random.next;
        }
        Node copyHead = head.next;
        for (Node c = head; c != null; c = c.next) {               // 3. unweave
            Node copy = c.next;
            c.next = copy.next;
            copy.next = (copy.next == null) ? null : copy.next.next;
        }
        return copyHead;
    }

    // ---------- test helpers: build from [[val, randomIndex], ...] and describe ----------
    static Node buildRandom(int[][] spec) {
        Node[] nodes = new Node[spec.length];
        for (int i = 0; i < spec.length; i++) nodes[i] = new Node(spec[i][0]);
        for (int i = 0; i < spec.length; i++) {
            if (i + 1 < spec.length) nodes[i].next = nodes[i + 1];
            nodes[i].random = spec[i][1] < 0 ? null : nodes[spec[i][1]];
        }
        return spec.length == 0 ? null : nodes[0];
    }

    static String describe(Node head) {
        List<Node> order = new ArrayList<>();
        for (Node c = head; c != null; c = c.next) order.add(c);
        StringBuilder sb = new StringBuilder("[");
        for (Node n : order) sb.append("[").append(n.val).append(",").append(n.random == null ? "null" : order.indexOf(n.random)).append("]");
        return sb.append("]").toString();
    }

    static boolean sharesNodes(Node a, Node b) {
        Set<Node> orig = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Node c = a; c != null; c = c.next) orig.add(c);
        for (Node c = b; c != null; c = c.next) {
            if (orig.contains(c) || (c.random != null && orig.contains(c.random))) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        int[][] spec = {{7, -1}, {13, 0}, {11, 4}, {10, 2}, {1, 0}};
        Node original = buildRandom(spec);
        String before = describe(original);

        Node copy1 = copyRandomList(original);
        check("hashmap: same structure", before, describe(copy1));
        check("hashmap: no shared nodes (deep copy)", false, sharesNodes(original, copy1));

        Node copy2 = copyRandomListInterleave(original);
        check("interleave: same structure", before, describe(copy2));
        check("interleave: original restored", before, describe(original));
        check("interleave: no shared nodes", false, sharesNodes(original, copy2));
        check("empty list", null, copyRandomListInterleave(null));
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
