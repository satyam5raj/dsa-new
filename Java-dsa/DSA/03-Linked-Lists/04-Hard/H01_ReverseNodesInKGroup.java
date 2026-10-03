/*
 * ============================================================================
 *  HARD 01 : REVERSE NODES IN K-GROUP        LeetCode #25
 *  https://leetcode.com/problems/reverse-nodes-in-k-group/
 *  Pattern: CHECK k NODES AHEAD -> REVERSE THE GROUP -> RECONNECT -> REPEAT
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse the nodes k at a time. A final group with fewer than k nodes stays
 *    as it is. Relink nodes (don't change values). O(1) extra space.
 *      1->2->3->4->5, k=2 -> 2->1->4->3->5
 *      1->2->3->4->5, k=3 -> 3->2->1->4->5
 *
 *  ---------------------------------------------------------------------------
 *  BUILDING BLOCKS YOU ALREADY KNOW
 *    - reverse a list (E01) and a sub-list (M13)
 *    - Swap Nodes in Pairs (M08) is exactly this problem with k = 2
 *
 *  ALGORITHM (iterative, O(1) space)
 *    dummy -> head;  groupPrev = dummy   (the node BEFORE the current group)
 *    loop:
 *      1. kth = the k-th node after groupPrev. If it doesn't exist -> stop
 *         (the leftover group is shorter than k and stays as is)
 *      2. groupNext = kth.next          (the first node AFTER the group)
 *      3. reverse the group's nodes. Start prev at groupNext (not null!) so the
 *         group's new tail automatically links to the rest of the list
 *      4. reconnect the front: the old first node is now the group's tail
 *            first = groupPrev.next
 *            groupPrev.next = kth       (kth is the new first node)
 *            groupPrev = first          (move on: the tail is the next group's 'prev')
 *
 *  DRY RUN 1->2->3->4->5, k=2
 *    group [1,2]: reversed -> D->2->1->3->4->5, groupPrev = 1
 *    group [3,4]: reversed -> D->2->1->4->3->5, groupPrev = 3
 *    group [5]: fewer than k -> stop
 *
 *  COMPLEXITY   Time O(n) (each node is visited twice: counting + reversing)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class H01_ReverseNodesInKGroup {

    static ListNode reverseKGroup(ListNode head, int k) {
        ListNode dummy = new ListNode(0, head);
        ListNode groupPrev = dummy;
        while (true) {
            ListNode kth = groupPrev;                         // 1. find the k-th node
            for (int i = 0; i < k && kth != null; i++) kth = kth.next;
            if (kth == null) break;                           //    fewer than k left

            ListNode groupNext = kth.next;                    // 2.
            ListNode prev = groupNext, cur = groupPrev.next;  // 3. reverse, tail -> groupNext
            while (cur != groupNext) {
                ListNode next = cur.next;
                cur.next = prev;
                prev = cur;
                cur = next;
            }
            ListNode first = groupPrev.next;                  // 4. reconnect
            groupPrev.next = kth;
            groupPrev = first;
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        check("1..5 k=2", "[2, 1, 4, 3, 5]", reverseKGroup(build(1, 2, 3, 4, 5), 2));
        check("1..5 k=3", "[3, 2, 1, 4, 5]", reverseKGroup(build(1, 2, 3, 4, 5), 3));
        check("1..4 k=4 (exactly one group)", "[4, 3, 2, 1]", reverseKGroup(build(1, 2, 3, 4), 4));
        check("1..3 k=1 (no change)", "[1, 2, 3]", reverseKGroup(build(1, 2, 3), 1));
        check("1,2 k=3 (too short)", "[1, 2]", reverseKGroup(build(1, 2), 3));
        boolean ok = true;
        for (int n = 1; n <= 20; n++) {
            for (int k = 1; k <= n + 1; k++) {
                int[] v = new int[n];
                for (int i = 0; i < n; i++) v[i] = i;
                int[] e = v.clone();
                for (int s = 0; s + k <= n; s += k)                      // reverse each full block in the array
                    for (int i = s, j = s + k - 1; i < j; i++, j--) { int t = e[i]; e[i] = e[j]; e[j] = t; }
                ok &= String.valueOf(reverseKGroup(build(v), k)).equals(Arrays.toString(e));
            }
        }
        check("every n<=20, k<=n+1 vs array version", true, ok);
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
