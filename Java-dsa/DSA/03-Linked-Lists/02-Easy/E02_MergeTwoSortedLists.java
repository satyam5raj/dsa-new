/*
 * ============================================================================
 *  EASY 02 : MERGE TWO SORTED LISTS          LeetCode #21
 *  https://leetcode.com/problems/merge-two-sorted-lists/
 *  Pattern: DUMMY HEAD + TAIL POINTER (splice nodes, don't copy)
 * ============================================================================
 *
 *  PROBLEM
 *    Merge two sorted lists into one sorted list by SPLICING their nodes together.
 *      1->2->4 + 1->3->4 -> 1->1->2->3->4->4
 *
 *  ---------------------------------------------------------------------------
 *  THE DUMMY HEAD TRICK: we don't know which list provides the first node. A
 *  dummy node gives us a fixed starting point, and 'tail' always points at the
 *  last node of the merged list so far.
 *    while both lists have nodes:
 *      attach the SMALLER head to tail, advance that list, advance tail
 *    attach the leftover list in ONE step (it's already sorted)
 *    return dummy.next
 *
 *  DRY RUN a = 1->2->4, b = 1->3->4
 *    1 vs 1 -> take a's 1 (<= keeps it stable)   merged: 1
 *    2 vs 1 -> take b's 1                         merged: 1->1
 *    2 vs 3 -> take 2                             merged: 1->1->2
 *    4 vs 3 -> take 3                             ...->3
 *    4 vs 4 -> take a's 4                         ...->4
 *    a empty -> attach b's remaining 4            ...->4
 *
 *  RECURSIVE version (elegant, O(n + m) stack):
 *    if a.val <= b.val: a.next = merge(a.next, b); return a
 *    else:              b.next = merge(a, b.next); return b
 *
 *  COMPLEXITY   Time O(n + m)   Space O(1) iterative
 *  BUILDING BLOCK OF: Sort List (merge sort, M14) and Merge k Sorted Lists (H02).
 * ============================================================================
 */

import java.util.*;

public class E02_MergeTwoSortedLists {

    static ListNode mergeTwoLists(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) {
                tail.next = a;
                a = a.next;
            } else {
                tail.next = b;
                b = b.next;
            }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;       // attach the rest in one step
        return dummy.next;
    }

    static ListNode mergeRecursive(ListNode a, ListNode b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.val <= b.val) {
            a.next = mergeRecursive(a.next, b);
            return a;
        }
        b.next = mergeRecursive(a, b.next);
        return b;
    }

    public static void main(String[] args) {
        check("1->2->4 + 1->3->4", "[1, 1, 2, 3, 4, 4]", mergeTwoLists(build(1, 2, 4), build(1, 3, 4)));
        check("[] + []", "null", mergeTwoLists(build(), build()));
        check("[] + [0]", "[0]", mergeTwoLists(build(), build(0)));
        check("5 + 1->2->3 (one goes first entirely)", "[1, 2, 3, 5]", mergeTwoLists(build(5), build(1, 2, 3)));
        check("recursive version", "[1, 1, 2, 3, 4, 4]", mergeRecursive(build(1, 2, 4), build(1, 3, 4)));
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
