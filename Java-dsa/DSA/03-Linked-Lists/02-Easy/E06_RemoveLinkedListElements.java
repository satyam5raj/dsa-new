/*
 * ============================================================================
 *  EASY 06 : REMOVE LINKED LIST ELEMENTS     LeetCode #203
 *  https://leetcode.com/problems/remove-linked-list-elements/
 *  Pattern: DUMMY HEAD (removing the head becomes a normal case)
 * ============================================================================
 *
 *  PROBLEM
 *    Remove every node whose value equals val. Return the new head.
 *      1->2->6->3->4->5->6, val 6 -> 1->2->3->4->5
 *      7->7->7->7, val 7          -> []
 *
 *  ---------------------------------------------------------------------------
 *  WITHOUT A DUMMY: first loop to skip matching nodes at the head, then a second
 *  loop for the rest. Two code paths = twice the bugs.
 *
 *  WITH A DUMMY: dummy -> head. Now EVERY real node has a predecessor 'prev'.
 *    while prev.next != null:
 *      if prev.next.val == val: prev.next = prev.next.next   (unlink it; DON'T move prev:
 *                                                              the new prev.next is unchecked)
 *      else:                    prev = prev.next
 *    return dummy.next
 *
 *  DRY RUN 7->7->1, val 7
 *    dummy->7->7->1 : remove -> dummy->7->1 : remove -> dummy->1 : keep, prev=1 -> end
 *    return 1
 *
 *  RECURSIVE ONE-LINER: head.next = remove(head.next); return head.val == val ? head.next : head;
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E06_RemoveLinkedListElements {

    static ListNode removeElements(ListNode head, int val) {
        ListNode dummy = new ListNode(0, head);
        ListNode prev = dummy;
        while (prev.next != null) {
            if (prev.next.val == val) {
                prev.next = prev.next.next;      // unlink
            } else {
                prev = prev.next;                // only advance when we keep the node
            }
        }
        return dummy.next;
    }

    static ListNode removeElementsRecursive(ListNode head, int val) {
        if (head == null) return null;
        head.next = removeElementsRecursive(head.next, val);
        return head.val == val ? head.next : head;
    }

    public static void main(String[] args) {
        check("1,2,6,3,4,5,6 remove 6", "[1, 2, 3, 4, 5]", removeElements(build(1, 2, 6, 3, 4, 5, 6), 6));
        check("7,7,7,7 remove 7 (all)", "null", removeElements(build(7, 7, 7, 7), 7));
        check("empty", "null", removeElements(build(), 1));
        check("7,7,1 remove 7 (head changes)", "[1]", removeElements(build(7, 7, 1), 7));
        check("recursive version", "[1, 2, 3, 4, 5]", removeElementsRecursive(build(1, 2, 6, 3, 4, 5, 6), 6));
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
