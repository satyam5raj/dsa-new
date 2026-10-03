/*
 * ============================================================================
 *  MEDIUM 14 : SORT LIST  ⭐ bonus (merge sort on a linked list)   LeetCode #148
 *  https://leetcode.com/problems/sort-list/
 *  Pattern: SPLIT (fast/slow) + RECURSE + MERGE (E02)
 * ============================================================================
 *
 *  PROBLEM
 *    Sort a linked list in O(n log n) time.
 *      4->2->1->3 -> 1->2->3->4
 *
 *  ---------------------------------------------------------------------------
 *  WHICH SORT FOR A LIST?
 *    - Quick sort needs random access for good pivots and partitioning -> awkward on a list.
 *    - MERGE SORT only needs to SPLIT and MERGE, both natural on lists, and the
 *      merge needs NO extra array (we just relink nodes). The perfect fit.
 *
 *  ALGORITHM
 *    sort(head):
 *      if 0 or 1 node: return head
 *      mid = FIRST middle (fast/slow)       // 4->2 | 1->3
 *      right = mid.next; mid.next = null    // cut into two halves
 *      return merge(sort(head), sort(right))
 *
 *  WHY THE FIRST MIDDLE? With 2 nodes, the "second middle" would be the 2nd
 *  node: the left half keeps both nodes -> infinite recursion. Splitting at
 *  the first middle always shrinks both halves.
 *
 *  DRY RUN 4->2->1->3
 *    split: [4,2] [1,3] -> split: [4][2] -> merge 2,4 ; [1][3] -> merge 1,3
 *    merge [2,4] + [1,3] -> 1,2,3,4
 *
 *  COMPLEXITY   Time O(n log n)   Space O(log n) recursion stack
 *  (A bottom-up iterative version gets O(1) extra space. Worth mentioning.)
 * ============================================================================
 */

import java.util.*;

public class M14_SortList {

    static ListNode sortList(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {   // FIRST middle
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode right = slow.next;
        slow.next = null;                                        // cut
        return merge(sortList(head), sortList(right));
    }

    static ListNode merge(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), t = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { t.next = a; a = a.next; }
            else { t.next = b; b = b.next; }
            t = t.next;
        }
        t.next = (a != null) ? a : b;
        return dummy.next;
    }

    public static void main(String[] args) {
        check("4,2,1,3", "[1, 2, 3, 4]", sortList(build(4, 2, 1, 3)));
        check("-1,5,3,4,0", "[-1, 0, 3, 4, 5]", sortList(build(-1, 5, 3, 4, 0)));
        check("empty", "null", sortList(build()));
        check("duplicates 3,1,3,1", "[1, 1, 3, 3]", sortList(build(3, 1, 3, 1)));
        Random rnd = new Random(14);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] v = rnd.ints(1 + rnd.nextInt(60), -20, 20).toArray();
            int[] e = v.clone();
            Arrays.sort(e);
            ok &= String.valueOf(sortList(build(v))).equals(Arrays.toString(e));
        }
        check("300 random lists vs Arrays.sort", true, ok);
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
