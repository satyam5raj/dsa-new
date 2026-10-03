/*
 * ============================================================================
 *  MEDIUM 04 : REORDER LIST                  LeetCode #143
 *  https://leetcode.com/problems/reorder-list/
 *  Pattern: MIDDLE (fast/slow) + REVERSE second half + MERGE alternately
 * ============================================================================
 *
 *  PROBLEM
 *    L0 -> L1 -> ... -> Ln-1 -> Ln   becomes   L0 -> Ln -> L1 -> Ln-1 -> L2 -> Ln-2 ...
 *    In-place (relink the nodes, don't change values).
 *      1->2->3->4   -> 1->4->2->3
 *      1->2->3->4->5 -> 1->5->2->4->3
 *
 *  ---------------------------------------------------------------------------
 *  EASY WAY: put the nodes in an array/list, then relink with two indices (i, j). O(n) space.
 *
 *  O(1) SPACE: we need to take nodes alternately from the FRONT and the BACK.
 *  A singly list can't walk backwards, so REVERSE the back half first.
 *    1. find the middle (first middle) and CUT the list into two halves
 *          1->2->3   and   4->5
 *    2. reverse the second half
 *          1->2->3   and   5->4
 *    3. weave them together:  1->5->2->4->3
 *
 *  This is the textbook "combine three patterns" problem. Each step is one of the basics.
 *
 *  DRY RUN 1->2->3->4
 *    middle: 2 -> halves 1->2 and 3->4
 *    reverse: 4->3
 *    weave: 1->4->2->3
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M04_ReorderList {

    static void reorderList(ListNode head) {
        if (head == null || head.next == null) return;
        // 1. first middle, then cut
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode second = slow.next;
        slow.next = null;                               // cut: the first half ends here
        // 2. reverse the second half
        ListNode prev = null;
        while (second != null) {
            ListNode next = second.next;
            second.next = prev;
            prev = second;
            second = next;
        }
        second = prev;
        // 3. weave: first1 -> second1 -> first2 -> second2 ...
        ListNode first = head;
        while (second != null) {
            ListNode n1 = first.next, n2 = second.next;  // save both continuations
            first.next = second;
            second.next = n1;
            first = n1;
            second = n2;
        }
    }

    static String run(int... vals) {
        ListNode h = build(vals);
        reorderList(h);
        return String.valueOf(h);
    }

    // simple array version to cross-check
    static String expected(int... vals) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0, j = vals.length - 1; i <= j; i++, j--) {
            out.add(vals[i]);
            if (i != j) out.add(vals[j]);
        }
        return vals.length == 0 ? "null" : out.toString();
    }

    public static void main(String[] args) {
        check("1,2,3,4", "[1, 4, 2, 3]", run(1, 2, 3, 4));
        check("1,2,3,4,5", "[1, 5, 2, 4, 3]", run(1, 2, 3, 4, 5));
        check("single", "[1]", run(1));
        check("two", "[1, 2]", run(1, 2));
        boolean ok = true;
        for (int n = 0; n <= 25; n++) {
            int[] v = new int[n];
            for (int i = 0; i < n; i++) v[i] = i + 1;
            ok &= run(v).equals(expected(v));
        }
        check("lengths 0..25 vs array version", true, ok);
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
