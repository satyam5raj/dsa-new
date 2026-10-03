/*
 * ============================================================================
 *  EASY 04 : INTERSECTION OF TWO LINKED LISTS   LeetCode #160
 *  https://leetcode.com/problems/intersection-of-two-linked-lists/
 *  Pattern: TWO POINTERS THAT SWITCH LISTS (equalize the path lengths)
 * ============================================================================
 *
 *  PROBLEM
 *    Two lists may MERGE at some node and share their tail from there on.
 *    Return that first shared NODE, or null.
 *      A:      a1 -> a2 \
 *                        c1 -> c2 -> c3
 *      B: b1 -> b2 -> b3 /                   -> c1
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: HASHSET - store all nodes of A, walk B until a node is in the set
 *              O(n + m) time, O(n) space
 *
 *  APPROACH 2: LENGTH DIFFERENCE - compute both lengths, advance the longer
 *              list's pointer by the difference, then walk together until equal.
 *              O(n + m), O(1)
 *
 *  APPROACH 3: SWITCHING POINTERS (elegant, O(1))
 *    pA walks A then continues on B; pB walks B then continues on A.
 *    Both travel  lenA + lenB  in total, so they arrive at the intersection at
 *    the SAME moment:
 *      pA: a-part + shared + b-part   ->  meets at c1
 *      pB: b-part + shared + a-part   ->  meets at c1
 *    With no intersection, both reach null at the same time -> return null.
 *
 *  DRY RUN  A = [a1, a2, c1, c2, c3] (5), B = [b1, b2, b3, c1, c2, c3] (6)
 *    pA: a1 a2 c1 c2 c3 | b1 b2 b3 c1    (9 steps to the 2nd c1)
 *    pB: b1 b2 b3 c1 c2 c3 | a1 a2 c1    (9 steps) -> both at c1
 *
 *  COMPLEXITY   Time O(n + m)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E04_IntersectionOfTwoLinkedLists {

    static ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        ListNode pA = headA, pB = headB;
        while (pA != pB) {                                   // compares NODES
            pA = (pA == null) ? headB : pA.next;            // switch to the other list at the end
            pB = (pB == null) ? headA : pB.next;
        }
        return pA;                                           // the intersection, or null
    }

    public static void main(String[] args) {
        ListNode shared = build(8, 4, 5);
        ListNode a = build(4, 1); a.next.next = shared;
        ListNode b = build(5, 6, 1); b.next.next.next = shared;
        check("intersect at 8", true, getIntersectionNode(a, b) == shared);

        ListNode shared2 = build(2, 4);
        ListNode c = build(1, 9, 1); c.next.next.next = shared2;
        ListNode d = build(3); d.next = shared2;
        check("intersect at 2 (different lengths)", true, getIntersectionNode(c, d) == shared2);

        check("no intersection -> null", true, getIntersectionNode(build(2, 6, 4), build(1, 5)) == null);
        ListNode same = build(1, 2);
        check("same list -> its head", true, getIntersectionNode(same, same) == same);
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
