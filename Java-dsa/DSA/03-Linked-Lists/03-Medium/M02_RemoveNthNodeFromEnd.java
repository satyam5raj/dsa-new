/*
 * ============================================================================
 *  MEDIUM 02 : REMOVE NTH NODE FROM END OF LIST   LeetCode #19
 *  https://leetcode.com/problems/remove-nth-node-from-end-of-list/
 *  Pattern: TWO POINTERS WITH A FIXED GAP + DUMMY HEAD (one pass)
 * ============================================================================
 *
 *  PROBLEM
 *    Remove the n-th node from the END and return the head. Do it in one pass.
 *      1->2->3->4->5, n=2 -> 1->2->3->5
 *      1, n=1             -> []
 *
 *  ---------------------------------------------------------------------------
 *  TWO PASSES: count the length L, then remove the node at position L - n.
 *
 *  ONE PASS: keep two pointers exactly n nodes apart. When the front one reaches
 *  the end, the back one is right BEFORE the node to delete.
 *    1. dummy -> head (the head itself might be the one removed!)
 *    2. fast = slow = dummy; move fast n+1 steps ahead  (gap of n nodes between them)
 *    3. move both until fast == null
 *    4. slow.next is the target: slow.next = slow.next.next
 *
 *  DRY RUN 1->2->3->4->5, n=2
 *    dummy D. fast moves 3 steps: D -> 1 -> 2 -> 3
 *    move both: (slow, fast) = (1,4) (2,5) (3,null) -> stop
 *    slow = 3, slow.next = 4 -> unlink -> 1->2->3->5
 *
 *  WHY THE DUMMY? n = length means removing the head: slow must stand BEFORE the
 *  head, and that's exactly the dummy.
 *  COMPLEXITY   Time O(L)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M02_RemoveNthNodeFromEnd {

    static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head);
        ListNode fast = dummy, slow = dummy;
        for (int i = 0; i <= n; i++) fast = fast.next;     // n+1 steps -> gap of n nodes
        while (fast != null) {
            fast = fast.next;
            slow = slow.next;
        }
        slow.next = slow.next.next;                        // unlink the target
        return dummy.next;
    }

    public static void main(String[] args) {
        check("1..5 n=2", "[1, 2, 3, 5]", removeNthFromEnd(build(1, 2, 3, 4, 5), 2));
        check("[1] n=1 (becomes empty)", "null", removeNthFromEnd(build(1), 1));
        check("1,2 n=1 (remove tail)", "[1]", removeNthFromEnd(build(1, 2), 1));
        check("1,2 n=2 (remove head)", "[2]", removeNthFromEnd(build(1, 2), 2));
        check("1..5 n=5 (remove head)", "[2, 3, 4, 5]", removeNthFromEnd(build(1, 2, 3, 4, 5), 5));
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
