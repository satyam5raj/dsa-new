/*
 * ============================================================================
 *  MEDIUM 11 : DELETE NODE IN A LINKED LIST  LeetCode #237
 *  https://leetcode.com/problems/delete-node-in-a-linked-list/
 *  Pattern: COPY THE NEXT NODE INTO THIS ONE, THEN SKIP THE NEXT (a trick question)
 * ============================================================================
 *
 *  PROBLEM
 *    You get ONLY the node to delete. NOT the head! The node is guaranteed not
 *    to be the tail. "Delete" it, so its value disappears from the list.
 *      list 4->5->1->9, given the node 5 -> 4->1->9
 *
 *  ---------------------------------------------------------------------------
 *  THE PUZZLE: normal deletion needs the PREVIOUS node (prev.next = node.next),
 *  and we can't reach it (no head, no prev pointer).
 *
 *  THE TRICK: don't delete THIS node. Make it a COPY of the next node, then
 *  delete the NEXT node (whose previous node we DO have: this one).
 *    node.val  = node.next.val
 *    node.next = node.next.next
 *
 *    4 -> [5] -> 1 -> 9        copy 1 into it:  4 -> [1] -> 1 -> 9
 *                              skip the next:   4 -> [1] -> 9
 *
 *  LIMITATIONS (discuss them! that's the real interview value):
 *    - impossible for the TAIL (there's no next node to copy)
 *    - the node OBJECT that disappears is actually the next one. If other code
 *      holds references to node objects (like a HashMap from key to node in an
 *      LRU cache), those references would now be wrong.
 *  COMPLEXITY   Time O(1)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M11_DeleteNodeInLinkedList {

    static void deleteNode(ListNode node) {
        node.val = node.next.val;          // become the next node...
        node.next = node.next.next;        // ...and drop the original next
    }

    public static void main(String[] args) {
        ListNode a = build(4, 5, 1, 9);
        deleteNode(a.next);                                    // node 5
        check("delete 5 from 4,5,1,9", "[4, 1, 9]", a);

        ListNode b = build(4, 5, 1, 9);
        deleteNode(b.next.next);                               // node 1
        check("delete 1 from 4,5,1,9", "[4, 5, 9]", b);

        ListNode c = build(1, 2);
        deleteNode(c);                                         // the head works too (it isn't the tail)
        check("delete the head of 1,2", "[2]", c);
        summary();
    }

    // ---------------- ListNode (LeetCode's definition) + helpers for building/printing ----------------
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }

        @Override
        public String toString() {                 // prints the whole list from this node: [1, 2, 3]
            StringBuilder sb = new StringBuilder("[");
            int guard = 0;
            for (ListNode c = this; c != null; c = c.next) {
                if (guard++ > 1000) return sb.append("...cycle?]").toString();
                if (c != this) sb.append(", ");
                sb.append(c.val);
            }
            return sb.append("]").toString();
        }
    }

    static ListNode build(int... vals) {           // build(1, 2, 3) -> 1 -> 2 -> 3
        ListNode dummy = new ListNode(0), t = dummy;
        for (int v : vals) { t.next = new ListNode(v); t = t.next; }
        return dummy.next;
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
