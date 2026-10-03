/*
 * ============================================================================
 *  MEDIUM 08 : SWAP NODES IN PAIRS           LeetCode #24
 *  https://leetcode.com/problems/swap-nodes-in-pairs/
 *  Pattern: DUMMY HEAD + REWIRE 3 POINTERS PER PAIR (or recursion)
 * ============================================================================
 *
 *  PROBLEM
 *    Swap every two adjacent NODES (relink, don't swap values).
 *      1->2->3->4 -> 2->1->4->3       1->2->3 -> 2->1->3
 *
 *  ---------------------------------------------------------------------------
 *  ITERATIVE: 'prev' sits before each pair (starts at a dummy).
 *    first = prev.next, second = first.next
 *      prev   -> first -> second -> rest
 *    becomes
 *      prev   -> second -> first -> rest
 *    3 assignments, in this order:
 *      first.next  = second.next     (first now points to the rest)
 *      second.next = first           (second points back to first)
 *      prev.next   = second          (connect the pair to what's before)
 *    then prev = first (it's now the second node of the pair)
 *
 *  RECURSIVE (very clean):
 *    swap(head): if fewer than 2 nodes -> return head
 *                second = head.next
 *                head.next = swap(second.next)   // the rest, already swapped
 *                second.next = head
 *                return second
 *
 *  DRY RUN 1->2->3->4
 *    D->1->2->3->4  -> D->2->1->3->4  (prev = 1)
 *    1->3->4        -> 1->4->3        -> D->2->1->4->3
 *
 *  GENERALIZATION: Reverse Nodes in k-Group (Hard H01) is this with k instead of 2.
 *  COMPLEXITY   Time O(n)   Space O(1) iterative, O(n) recursive
 * ============================================================================
 */

import java.util.*;

public class M08_SwapNodesInPairs {

    static ListNode swapPairs(ListNode head) {
        ListNode dummy = new ListNode(0, head), prev = dummy;
        while (prev.next != null && prev.next.next != null) {
            ListNode first = prev.next, second = first.next;
            first.next = second.next;
            second.next = first;
            prev.next = second;
            prev = first;
        }
        return dummy.next;
    }

    static ListNode swapPairsRecursive(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode second = head.next;
        head.next = swapPairsRecursive(second.next);
        second.next = head;
        return second;
    }

    public static void main(String[] args) {
        check("1,2,3,4", "[2, 1, 4, 3]", swapPairs(build(1, 2, 3, 4)));
        check("1,2,3 (odd length)", "[2, 1, 3]", swapPairs(build(1, 2, 3)));
        check("single", "[1]", swapPairs(build(1)));
        check("empty", "null", swapPairs(build()));
        check("recursive 1..5", "[2, 1, 4, 3, 5]", swapPairsRecursive(build(1, 2, 3, 4, 5)));
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
