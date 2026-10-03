/*
 * ============================================================================
 *  MEDIUM 13 : REVERSE LINKED LIST II  ⭐ bonus (core technique)   LeetCode #92
 *  https://leetcode.com/problems/reverse-linked-list-ii/
 *  Pattern: REVERSE A SUBLIST IN ONE PASS ("head insertion")
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse only positions left..right (1-indexed), in one pass.
 *      1->2->3->4->5, left=2, right=4 -> 1->4->3->2->5
 *
 *  ---------------------------------------------------------------------------
 *  WHY THIS BONUS: reversing a PART of a list is the engine of Reverse Nodes in
 *  k-Group (H01) and many other problems.
 *
 *  HEAD-INSERTION TECHNIQUE
 *    1. dummy -> head; walk 'prev' to the node BEFORE position left.
 *    2. 'start' = prev.next is the first node of the section. It will end up
 *       as the section's LAST node, and it stays in place while we work.
 *    3. Repeat (right - left) times: take the node right after 'start' and move
 *       it to the FRONT of the section (right after prev).
 *         then = start.next
 *         start.next = then.next     (unhook 'then')
 *         then.next = prev.next      (point it at the current section front)
 *         prev.next = then           (make it the new section front)
 *
 *  DRY RUN 1->2->3->4->5, left 2, right 4      prev = 1, start = 2
 *    move 3 to the front: 1->3->2->4->5
 *    move 4 to the front: 1->4->3->2->5        done (2 moves = right - left)
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M13_ReverseLinkedListII {

    static ListNode reverseBetween(ListNode head, int left, int right) {
        ListNode dummy = new ListNode(0, head), prev = dummy;
        for (int i = 1; i < left; i++) prev = prev.next;      // node before the section
        ListNode start = prev.next;                           // becomes the section's tail
        for (int i = 0; i < right - left; i++) {
            ListNode then = start.next;
            start.next = then.next;
            then.next = prev.next;
            prev.next = then;
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        check("1..5 left=2 right=4", "[1, 4, 3, 2, 5]", reverseBetween(build(1, 2, 3, 4, 5), 2, 4));
        check("[5] left=1 right=1", "[5]", reverseBetween(build(5), 1, 1));
        check("whole list 1..4 (1..4)", "[4, 3, 2, 1]", reverseBetween(build(1, 2, 3, 4), 1, 4));
        check("3,5 left=1 right=2", "[5, 3]", reverseBetween(build(3, 5), 1, 2));
        boolean ok = true;
        for (int n = 1; n <= 10; n++)
            for (int l = 1; l <= n; l++)
                for (int r = l; r <= n; r++) {
                    int[] v = new int[n];
                    for (int i = 0; i < n; i++) v[i] = i;
                    int[] e = v.clone();
                    for (int i = l - 1, j = r - 1; i < j; i++, j--) { int t = e[i]; e[i] = e[j]; e[j] = t; }
                    ok &= String.valueOf(reverseBetween(build(v), l, r)).equals(Arrays.toString(e));
                }
        check("every (n<=10, left, right) vs array", true, ok);
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
