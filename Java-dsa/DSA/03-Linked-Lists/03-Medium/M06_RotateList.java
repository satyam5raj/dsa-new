/*
 * ============================================================================
 *  MEDIUM 06 : ROTATE LIST                   LeetCode #61
 *  https://leetcode.com/problems/rotate-list/
 *  Pattern: MAKE IT A CIRCLE, THEN CUT IN THE RIGHT PLACE
 * ============================================================================
 *
 *  PROBLEM
 *    Rotate the list to the RIGHT by k places.
 *      1->2->3->4->5, k=2 -> 4->5->1->2->3
 *      0->1->2, k=4       -> 2->0->1      (4 % 3 = 1)
 *
 *  ---------------------------------------------------------------------------
 *  OBSERVATIONS
 *    - rotating by the length n gives the same list -> k = k % n
 *    - right-rotating by k = the LAST k nodes move to the front, in order
 *
 *  CIRCLE TRICK
 *    1. find the length n and the tail (one pass)
 *    2. connect tail -> head (now it's a circular list, see CircularLinkedList.java)
 *    3. the NEW tail is at position n - k - 1 (0-indexed) from the old head.
 *       Walk there; new head = newTail.next; cut: newTail.next = null
 *
 *  DRY RUN 1->2->3->4->5, k=2
 *    n = 5, k = 2 -> circle 1->2->3->4->5->1...
 *    new tail = node at index 5-2-1 = 2 -> value 3
 *    new head = 4, cut after 3 -> 4->5->1->2->3
 *
 *  Compare with Arrays/M02 (rotate array): arrays need the triple-reverse trick
 *  because elements must physically move. A list only needs 2 pointer changes.
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M06_RotateList {

    static ListNode rotateRight(ListNode head, int k) {
        if (head == null || head.next == null) return head;
        int n = 1;
        ListNode tail = head;
        while (tail.next != null) { tail = tail.next; n++; }   // 1. length + tail
        k %= n;
        if (k == 0) return head;
        tail.next = head;                                      // 2. close the circle
        ListNode newTail = head;
        for (int i = 0; i < n - k - 1; i++) newTail = newTail.next;   // 3. find the cut point
        ListNode newHead = newTail.next;
        newTail.next = null;                                   //    cut
        return newHead;
    }

    public static void main(String[] args) {
        check("1..5 k=2", "[4, 5, 1, 2, 3]", rotateRight(build(1, 2, 3, 4, 5), 2));
        check("0,1,2 k=4 (k > n)", "[2, 0, 1]", rotateRight(build(0, 1, 2), 4));
        check("k = n (no change)", "[1, 2, 3]", rotateRight(build(1, 2, 3), 3));
        check("empty", "null", rotateRight(build(), 5));
        boolean ok = true;
        for (int n = 1; n <= 12; n++) {
            for (int k = 0; k <= 30; k++) {
                int[] v = new int[n];
                for (int i = 0; i < n; i++) v[i] = i;
                int[] exp = new int[n];
                for (int i = 0; i < n; i++) exp[(i + k) % n] = v[i];      // array rotation
                ok &= String.valueOf(rotateRight(build(v), k)).equals(Arrays.toString(exp));
            }
        }
        check("all n<=12, k<=30 vs array rotation", true, ok);
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
