/*
 * ============================================================================
 *  MEDIUM 12 : ODD EVEN LINKED LIST          LeetCode #328
 *  https://leetcode.com/problems/odd-even-linked-list/
 *  Pattern: TWO INTERLEAVED CHAINS, THEN JOIN
 * ============================================================================
 *
 *  PROBLEM
 *    Group the nodes at ODD POSITIONS (1st, 3rd, 5th...) first, then the EVEN
 *    positions, keeping the order inside each group. Positions, not values!
 *    O(1) extra space, O(n) time.
 *      1->2->3->4->5      -> 1->3->5->2->4
 *      2->1->3->5->6->4->7 -> 2->3->6->7->1->5->4
 *
 *  ---------------------------------------------------------------------------
 *  The list alternates odd, even, odd, even... Unzip it like a zipper:
 *    odd = head, even = head.next, evenHead = even   (remember where evens start)
 *    while even != null && even.next != null:
 *        odd.next  = even.next;  odd  = odd.next     (skip over an even node)
 *        even.next = odd.next;   even = even.next    (skip over an odd node)
 *    odd.next = evenHead                             (attach the evens after the odds)
 *
 *  DRY RUN 1->2->3->4->5
 *    odd=1 even=2:  1->3, odd=3;  2->4, even=4
 *    odd=3 even=4:  3->5, odd=5;  4->null, even=null -> stop
 *    5 -> evenHead(2): 1->3->5->2->4
 *
 *  Same family as Partition List (M07), but split by POSITION instead of value,
 *  and done without dummies.
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M12_OddEvenLinkedList {

    static ListNode oddEvenList(ListNode head) {
        if (head == null) return null;
        ListNode odd = head, even = head.next, evenHead = even;
        while (even != null && even.next != null) {
            odd.next = even.next;
            odd = odd.next;
            even.next = odd.next;
            even = even.next;
        }
        odd.next = evenHead;
        return head;
    }

    public static void main(String[] args) {
        check("1..5", "[1, 3, 5, 2, 4]", oddEvenList(build(1, 2, 3, 4, 5)));
        check("2,1,3,5,6,4,7", "[2, 3, 6, 7, 1, 5, 4]", oddEvenList(build(2, 1, 3, 5, 6, 4, 7)));
        check("single", "[1]", oddEvenList(build(1)));
        check("two", "[1, 2]", oddEvenList(build(1, 2)));
        boolean ok = true;
        for (int n = 1; n <= 20; n++) {
            int[] v = new int[n];
            List<Integer> exp = new ArrayList<>();
            for (int i = 0; i < n; i++) v[i] = i + 1;
            for (int i = 0; i < n; i += 2) exp.add(v[i]);
            for (int i = 1; i < n; i += 2) exp.add(v[i]);
            ok &= String.valueOf(oddEvenList(build(v))).equals(exp.toString());
        }
        check("lengths 1..20 vs array version", true, ok);
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
