/*
 * ============================================================================
 *  LINKED LISTS 1.4 : THE LINKED LIST PATTERNS
 * ============================================================================
 *
 *  Run:  java LinkedListPatterns.java
 *
 *  Problems use the bare LeetCode-style node:   class ListNode { int val; ListNode next; }
 *
 *  -----------------------------------------------------------------------------------
 *   Pattern                  When                                       Problems
 *  -----------------------------------------------------------------------------------
 *   1. Dummy (sentinel) head  the head itself might change/be removed   E02 E06 M02 M07 ...
 *   2. Fast & slow pointers   middle, cycle, "k-th from the end",       E03 E05 E07 M01 M02
 *                             palindrome
 *   3. In-place reversal      reverse all / part / every k nodes        E01 E05 M04 M13 H01
 *   4. Merge                  combine sorted lists                      E02 M14 H02
 *   5. Split + reconnect      partition, odd/even, rotate, reorder      M04 M06 M07 M12
 *   6. HashMap node -> node   copy a list with extra pointers           M05
 *  -----------------------------------------------------------------------------------
 *
 *  Most hard-looking list problems are 2-3 of these glued together.
 *  Example: Reorder List = find middle (2) + reverse second half (3) + merge alternately (4).
 */

import java.util.*;

public class LinkedListPatterns {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    public static void main(String[] args) {
        dummyHead();
        fastAndSlow();
        reversal();
        merge();
        splitAndReconnect();
    }

    // =================================================================
    // 1. DUMMY HEAD: a fake node in front of the head.
    //    Every real node now has a predecessor, so "delete the head" is the same
    //    code as "delete any node". Return dummy.next at the end.
    // =================================================================
    static ListNode removeAll(ListNode head, int val) {
        ListNode dummy = new ListNode(0, head);
        ListNode prev = dummy;
        while (prev.next != null) {
            if (prev.next.val == val) prev.next = prev.next.next;   // skip it (don't advance prev!)
            else prev = prev.next;
        }
        return dummy.next;                                          // the head may have changed
    }

    static void dummyHead() {
        System.out.println("===== 1. DUMMY HEAD =====");
        System.out.println("   remove all 7s from 7->7->1->7->2 : " + str(removeAll(build(7, 7, 1, 7, 2), 7)));
    }

    // =================================================================
    // 2. FAST & SLOW POINTERS ("tortoise and hare")
    //    slow moves 1 step, fast moves 2 steps.
    //    - When fast reaches the end, slow is at the MIDDLE.
    //    - If there's a CYCLE, fast laps slow and they MEET (Floyd's algorithm).
    //    - "k-th from the end": move fast k steps first, then move both together.
    // =================================================================
    static ListNode middle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;                    // for even length: the SECOND middle
    }

    static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;   // compare NODES (references), not values
        }
        return false;
    }

    static int kthFromEnd(ListNode head, int k) {
        ListNode fast = head, slow = head;
        for (int i = 0; i < k; i++) fast = fast.next;     // a gap of k nodes
        while (fast != null) { fast = fast.next; slow = slow.next; }
        return slow.val;
    }

    static void fastAndSlow() {
        System.out.println("\n===== 2. FAST & SLOW =====");
        System.out.println("   middle of 1..5 = " + middle(build(1, 2, 3, 4, 5)).val + ", of 1..6 = " + middle(build(1, 2, 3, 4, 5, 6)).val);
        ListNode cyc = build(1, 2, 3, 4);
        cyc.next.next.next.next = cyc.next;                // 4 -> 2: a cycle
        System.out.println("   hasCycle(1->2->3->4->2...) = " + hasCycle(cyc) + ", hasCycle(1->2->3) = " + hasCycle(build(1, 2, 3)));
        System.out.println("   2nd from the end of 1..5 = " + kthFromEnd(build(1, 2, 3, 4, 5), 2));
    }

    // =================================================================
    // 3. IN-PLACE REVERSAL (prev / cur / next). Memorize the 4 lines.
    //    Variant: reverse only the nodes between positions left..right.
    // =================================================================
    static ListNode reverse(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;   // save
            cur.next = prev;            // flip
            prev = cur;                 // advance
            cur = next;
        }
        return prev;
    }

    static void reversal() {
        System.out.println("\n===== 3. REVERSAL =====");
        System.out.println("   reverse 1->2->3->4 = " + str(reverse(build(1, 2, 3, 4))));
    }

    // =================================================================
    // 4. MERGE two sorted lists with a dummy head and a 'tail' pointer
    // =================================================================
    static ListNode mergeSorted(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { tail.next = a; a = a.next; }
            else { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;   // attach whatever is left (already sorted)
        return dummy.next;
    }

    static void merge() {
        System.out.println("\n===== 4. MERGE =====");
        System.out.println("   merge 1->3->5 & 2->4 = " + str(mergeSorted(build(1, 3, 5), build(2, 4))));
    }

    // =================================================================
    // 5. SPLIT + RECONNECT: build two (or more) sub-lists with their own
    //    dummy heads, then join them. Example: partition around x.
    // =================================================================
    static ListNode partition(ListNode head, int x) {
        ListNode lessDummy = new ListNode(0), less = lessDummy;
        ListNode moreDummy = new ListNode(0), more = moreDummy;
        for (ListNode cur = head; cur != null; cur = cur.next) {
            if (cur.val < x) { less.next = cur; less = cur; }
            else { more.next = cur; more = cur; }
        }
        more.next = null;               // IMPORTANT: cut the old link, or you may create a cycle
        less.next = moreDummy.next;     // join the two lists
        return lessDummy.next;
    }

    static void splitAndReconnect() {
        System.out.println("\n===== 5. SPLIT + RECONNECT =====");
        System.out.println("   partition 1->4->3->2->5->2 around 3 = " + str(partition(build(1, 4, 3, 2, 5, 2), 3)));
    }

    // ---------- helpers ----------
    static ListNode build(int... vals) {
        ListNode dummy = new ListNode(0), t = dummy;
        for (int v : vals) { t.next = new ListNode(v); t = t.next; }
        return dummy.next;
    }

    static String str(ListNode head) {
        StringBuilder sb = new StringBuilder();
        for (ListNode c = head; c != null; c = c.next) sb.append(c.val).append("->");
        return sb.append("null").toString();
    }
}
