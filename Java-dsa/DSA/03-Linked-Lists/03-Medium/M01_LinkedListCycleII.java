/*
 * ============================================================================
 *  MEDIUM 01 : LINKED LIST CYCLE II          LeetCode #142
 *  https://leetcode.com/problems/linked-list-cycle-ii/
 *  Pattern: FLOYD'S ALGORITHM, PHASE 2 (find the cycle's ENTRY node)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the node where the cycle BEGINS, or null if there's no cycle. O(1) space.
 *      3 -> 2 -> 0 -> -4 -> (back to 2)   -> the node with value 2
 *
 *  ---------------------------------------------------------------------------
 *  HASHSET: the first node visited twice is the entry -> O(n) space.
 *
 *  FLOYD (two phases)
 *    PHASE 1: slow (1 step) and fast (2 steps) until they meet inside the cycle (E03).
 *    PHASE 2: put one pointer back at HEAD, keep the other at the MEETING point.
 *             Move BOTH one step at a time. Where they meet is the cycle ENTRY.
 *
 *  WHY PHASE 2 WORKS (the proof interviewers ask for)
 *    Let  a = distance head -> entry,  b = entry -> meeting point,  c = cycle length.
 *    When they meet:  slow walked a + b,  fast walked a + b + k*c  (some whole laps more)
 *    fast walks twice as far:  2(a + b) = a + b + k*c   =>   a + b = k*c   =>   a = k*c - b
 *    "k*c - b" is exactly the distance from the MEETING POINT forward to the ENTRY
 *    (finish the current lap: c - b, plus whole extra laps). So a walker from the
 *    head and a walker from the meeting point, both at speed 1, reach the entry
 *    after exactly 'a' steps, at the same time.
 *
 *  DRY RUN 3 -> 2 -> 0 -> -4 -> (2)     a = 1, cycle = [2, 0, -4], c = 3
 *    phase 1 meets at -4 (b = 2)
 *    phase 2: p1 from head 3, p2 from -4 -> step: p1 = 2, p2 = 2 -> entry is 2
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  ALSO SOLVES: "Find the Duplicate Number" (#287): treat i -> nums[i] as a linked list!
 * ============================================================================
 */

import java.util.*;

public class M01_LinkedListCycleII {

    static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {                       // phase 1 done: inside the cycle
                ListNode p = head;
                while (p != slow) {                   // phase 2: both at speed 1
                    p = p.next;
                    slow = slow.next;
                }
                return p;                             // the entry
            }
        }
        return null;                                  // fast hit the end: no cycle
    }

    // Bonus: Find the Duplicate Number (#287) with the same algorithm.
    // nums has n+1 values in [1, n]; view index i -> nums[i] as "next". The duplicate is the cycle entry.
    static int findDuplicate(int[] nums) {
        int slow = nums[0], fast = nums[0];
        do {
            slow = nums[slow];
            fast = nums[nums[fast]];
        } while (slow != fast);
        int p = nums[0];
        while (p != slow) {
            p = nums[p];
            slow = nums[slow];
        }
        return p;
    }

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

    static int indexOf(ListNode head, ListNode node) {     // position of 'node' (or -1)
        int i = 0;
        for (ListNode c = head; c != null && i < 10_000; c = c.next, i++) if (c == node) return i;
        return -1;
    }

    public static void main(String[] args) {
        ListNode a = withCycle(1, 3, 2, 0, -4);
        check("3,2,0,-4 cycle at index 1", 1, indexOf(a, detectCycle(a)));
        ListNode b = withCycle(0, 1, 2);
        check("1,2 cycle at index 0", 0, indexOf(b, detectCycle(b)));
        check("no cycle -> null", null, detectCycle(withCycle(-1, 1, 2, 3)));
        boolean ok = true;
        for (int n = 1; n <= 30; n++) {
            for (int pos = 0; pos < n; pos++) {
                int[] vals = new int[n];
                ListNode h = withCycle(pos, vals);
                ok &= indexOf(h, detectCycle(h)) == pos;
            }
        }
        check("every n<=30, every entry position", true, ok);
        check("bonus #287 findDuplicate [1,3,4,2,2]", 2, findDuplicate(new int[]{1, 3, 4, 2, 2}));
        check("bonus #287 findDuplicate [3,1,3,4,2]", 3, findDuplicate(new int[]{3, 1, 3, 4, 2}));
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
