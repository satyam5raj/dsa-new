/*
 * ============================================================================
 *  EASY 08 : REMOVE DUPLICATES FROM SORTED LIST  ⭐ bonus   LeetCode #83 (+ #82 variant)
 *  https://leetcode.com/problems/remove-duplicates-from-sorted-list/
 *  Pattern: COMPARE WITH THE NEXT NODE (sorted -> duplicates are adjacent)
 * ============================================================================
 *
 *  PROBLEM #83
 *    Sorted list. Delete duplicates so each value appears ONCE.
 *      1->1->2->3->3 -> 1->2->3
 *
 *  ---------------------------------------------------------------------------
 *  #83 LOGIC: walk with cur. While cur.next has the SAME value, skip it
 *  (cur.next = cur.next.next). Only advance cur when the next value differs.
 *  The head never changes (we always keep the first copy), so no dummy is needed.
 *
 *  VARIANT #82 (Medium): delete ALL nodes that have duplicates, keeping only
 *  distinct values:  1->2->3->3->4->4->5 -> 1->2->5,   1->1->1->2->3 -> 2->3
 *    Now the HEAD itself may be deleted -> use a DUMMY.
 *    prev = dummy; cur = head
 *    if cur starts a run of equal values: skip the whole run, prev.next = node after the run
 *    else: prev = cur
 *
 *  COMPLEXITY   both Time O(n), Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E08_RemoveDuplicatesFromSortedList {

    // #83: keep one copy of each value
    static ListNode deleteDuplicates(ListNode head) {
        ListNode cur = head;
        while (cur != null && cur.next != null) {
            if (cur.next.val == cur.val) cur.next = cur.next.next;   // skip the duplicate
            else cur = cur.next;
        }
        return head;
    }

    // #82: remove every value that appears more than once
    static ListNode deleteAllDuplicates(ListNode head) {
        ListNode dummy = new ListNode(0, head), prev = dummy;
        ListNode cur = head;
        while (cur != null) {
            if (cur.next != null && cur.next.val == cur.val) {
                int dup = cur.val;
                while (cur != null && cur.val == dup) cur = cur.next;   // skip the whole run
                prev.next = cur;
            } else {
                prev = cur;
                cur = cur.next;
            }
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        check("1,1,2", "[1, 2]", deleteDuplicates(build(1, 1, 2)));
        check("1,1,2,3,3", "[1, 2, 3]", deleteDuplicates(build(1, 1, 2, 3, 3)));
        check("1,1,1 (all same)", "[1]", deleteDuplicates(build(1, 1, 1)));
        check("empty", "null", deleteDuplicates(build()));
        check("#82: 1,2,3,3,4,4,5", "[1, 2, 5]", deleteAllDuplicates(build(1, 2, 3, 3, 4, 4, 5)));
        check("#82: 1,1,1,2,3 (head removed)", "[2, 3]", deleteAllDuplicates(build(1, 1, 1, 2, 3)));
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
