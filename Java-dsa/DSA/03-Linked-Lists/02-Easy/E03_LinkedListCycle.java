/*
 * ============================================================================
 *  EASY 03 : LINKED LIST CYCLE               LeetCode #141
 *  https://leetcode.com/problems/linked-list-cycle/
 *  Pattern: FAST & SLOW POINTERS (Floyd's cycle detection)
 * ============================================================================
 *
 *  PROBLEM
 *    Does the list contain a cycle (some node's next points back to an earlier node)?
 *      3 -> 2 -> 0 -> -4
 *           ^          |
 *           +----------+         -> true
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: HASHSET OF VISITED NODES                 O(n) time, O(n) space
 *    Walk the list; if we ever see the same NODE (reference) again -> cycle.
 *
 *  APPROACH 2: FLOYD'S TORTOISE & HARE                  O(n) time, O(1) space
 *    slow moves 1 step, fast moves 2 steps.
 *      no cycle -> fast reaches null -> false
 *      cycle    -> both end up inside the loop; each step fast gains 1 node on
 *                  slow, so the gap shrinks by 1 every step -> they MUST meet.
 *    (Like two runners on a circular track: the faster one laps the slower one.)
 *
 *  DRY RUN 3 -> 2 -> 0 -> -4 -> (back to 2)
 *    slow: 3, 2, 0, -4
 *    fast: 3, 0, 2, -4      -> they meet at -4 -> true
 *
 *  COMPARE NODES, NOT VALUES: two different nodes can hold the same value.
 *  FOLLOW-UP: WHERE does the cycle start? -> Medium M01 (Linked List Cycle II)
 * ============================================================================
 */

import java.util.*;

public class E03_LinkedListCycle {

    static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {   // fast checks two steps ahead
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;
        }
        return false;
    }

    static boolean hasCycleWithSet(ListNode head) {
        Set<ListNode> seen = new HashSet<>();          // identity-based (ListNode doesn't override equals)
        for (ListNode c = head; c != null; c = c.next) {
            if (!seen.add(c)) return true;
        }
        return false;
    }

    // build a list and make the last node point to the node at index 'pos' (-1 = no cycle)
    static ListNode withCycle(int pos, int... vals) {
        ListNode head = build(vals), target = null, last = null;
        int i = 0;
        for (ListNode c = head; c != null; c = c.next, i++) {
            if (i == pos) target = c;
            last = c;
        }
        if (last != null && pos >= 0) last.next = target;
        return head;
    }

    public static void main(String[] args) {
        check("3,2,0,-4 cycle to index 1", true, hasCycle(withCycle(1, 3, 2, 0, -4)));
        check("1,2 cycle to index 0", true, hasCycle(withCycle(0, 1, 2)));
        check("1 (no cycle)", false, hasCycle(withCycle(-1, 1)));
        check("empty", false, hasCycle(null));
        check("single node pointing to itself", true, hasCycle(withCycle(0, 1)));
        check("HashSet version agrees", true, hasCycleWithSet(withCycle(1, 3, 2, 0, -4)));
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
