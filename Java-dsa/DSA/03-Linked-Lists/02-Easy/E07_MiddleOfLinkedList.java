/*
 * ============================================================================
 *  EASY 07 : MIDDLE OF THE LINKED LIST       LeetCode #876
 *  https://leetcode.com/problems/middle-of-the-linked-list/
 *  Pattern: FAST & SLOW POINTERS
 * ============================================================================
 *
 *  PROBLEM
 *    Return the middle node. With two middles (even length), return the SECOND.
 *      1->2->3->4->5    -> 3->4->5
 *      1->2->3->4->5->6 -> 4->5->6
 *
 *  ---------------------------------------------------------------------------
 *  TWO PASSES: count the length n, then walk n/2 steps. O(n), fine.
 *
 *  ONE PASS (fast/slow): fast moves 2, slow moves 1. When fast can't move any
 *  further, slow has gone half as far -> the middle.
 *    while fast != null && fast.next != null: slow = slow.next; fast = fast.next.next
 *
 *  DRY RUN 1..6
 *    slow 1 2 3 4
 *    fast 1 3 5 null   -> stop, slow = 4 (the second middle)
 *
 *  WANT THE FIRST MIDDLE (3 for 1..6)? Use the condition
 *    while fast.next != null && fast.next.next != null
 *  That variant is what you need for SPLITTING a list in half (merge sort, M14).
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E07_MiddleOfLinkedList {

    static ListNode middleNode(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    static ListNode firstMiddle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    public static void main(String[] args) {
        check("1..5 -> 3", "[3, 4, 5]", middleNode(build(1, 2, 3, 4, 5)));
        check("1..6 -> 4 (second middle)", "[4, 5, 6]", middleNode(build(1, 2, 3, 4, 5, 6)));
        check("single node", "[1]", middleNode(build(1)));
        check("two nodes -> second", "[2]", middleNode(build(1, 2)));
        check("first-middle variant 1..6 -> 3", 3, firstMiddle(build(1, 2, 3, 4, 5, 6)).val);
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
