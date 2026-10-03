/*
 * ============================================================================
 *  MEDIUM 09 : FLATTEN A MULTILEVEL DOUBLY LINKED LIST   LeetCode #430
 *  https://leetcode.com/problems/flatten-a-multilevel-doubly-linked-list/
 *  Pattern: DEPTH-FIRST SPLICING (stack or recursion) - like a tree's pre-order
 * ============================================================================
 *
 *  PROBLEM
 *    Nodes have prev, next and CHILD (pointing to another doubly linked list,
 *    which may have children of its own). Flatten everything into ONE level:
 *    a child list goes right after its parent, before the parent's old next.
 *    All child pointers must end up null.
 *
 *      1 - 2 - 3 - 4 - 5 - 6
 *              |
 *              7 - 8 - 9 - 10
 *                  |
 *                  11 - 12
 *    ->  1 2 3 7 8 11 12 9 10 4 5 6
 *
 *  ---------------------------------------------------------------------------
 *  IT'S A PRE-ORDER TRAVERSAL: visit a node, then its child branch (fully),
 *  then its next. "Finish the deeper thing first, then come back" -> a STACK.
 *
 *  ITERATIVE WITH A STACK
 *    cur = head
 *    while cur:
 *      if cur.child:
 *        if cur.next: push cur.next              (come back to it later)
 *        splice: cur.next = child, child.prev = cur, cur.child = null
 *      if cur.next == null and the stack isn't empty:
 *        popped = pop(); cur.next = popped; popped.prev = cur   (resume the saved part)
 *      cur = cur.next
 *
 *  DRY RUN (above): at 3 push 4, go into 7; at 8 push 9, go into 11;
 *  12 is a dead end -> pop 9; 10 is a dead end -> pop 4; continue 4 5 6.
 *
 *  COMPLEXITY   Time O(n)   Space O(depth) for the stack
 * ============================================================================
 */

import java.util.*;

public class M09_FlattenMultilevelDoublyLinkedList {

    static class Node {
        int val;
        Node prev, next, child;
        Node(int val) { this.val = val; }
    }

    static Node flatten(Node head) {
        Deque<Node> stack = new ArrayDeque<>();
        Node cur = head;
        while (cur != null) {
            if (cur.child != null) {
                if (cur.next != null) stack.push(cur.next);   // remember where to come back
                cur.next = cur.child;
                cur.child.prev = cur;
                cur.child = null;
            }
            if (cur.next == null && !stack.isEmpty()) {
                Node resume = stack.pop();
                cur.next = resume;
                resume.prev = cur;
            }
            cur = cur.next;
        }
        return head;
    }

    // ---------- helpers ----------
    static Node chain(int... vals) {
        Node head = null, prev = null;
        for (int v : vals) {
            Node n = new Node(v);
            if (prev == null) head = n; else { prev.next = n; n.prev = prev; }
            prev = n;
        }
        return head;
    }

    static Node at(Node head, int val) {
        for (Node c = head; c != null; c = c.next) if (c.val == val) return c;
        return null;
    }

    static String forward(Node head) {
        List<Integer> l = new ArrayList<>();
        for (Node c = head; c != null; c = c.next) l.add(c.val);
        return l.toString();
    }

    static boolean consistent(Node head) {        // prev pointers mirror next pointers, no children left
        for (Node c = head; c != null; c = c.next) {
            if (c.child != null) return false;
            if (c.next != null && c.next.prev != c) return false;
        }
        return head == null || head.prev == null;
    }

    public static void main(String[] args) {
        Node top = chain(1, 2, 3, 4, 5, 6);
        Node mid = chain(7, 8, 9, 10);
        Node low = chain(11, 12);
        at(top, 3).child = mid;
        at(mid, 8).child = low;
        Node flat = flatten(top);
        check("3-level example", "[1, 2, 3, 7, 8, 11, 12, 9, 10, 4, 5, 6]", forward(flat));
        check("prev pointers consistent, no children", true, consistent(flat));

        Node a = chain(1, 2);
        a.child = chain(3);                       // child on the head, with a next
        check("child of the head", "[1, 3, 2]", forward(flatten(a)));

        Node b = chain(1);
        b.child = chain(2);
        b.child.child = chain(3);                 // nested chain with no 'next' anywhere
        check("only children (a vertical line)", "[1, 2, 3]", forward(flatten(b)));
        check("empty", "[]", forward(flatten(null)));
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
