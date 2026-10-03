/*
 * ============================================================================
 *  MEDIUM 07 : PARTITION LIST                LeetCode #86
 *  https://leetcode.com/problems/partition-list/
 *  Pattern: SPLIT INTO TWO DUMMY-HEADED LISTS, THEN JOIN (stable)
 * ============================================================================
 *
 *  PROBLEM
 *    Put all nodes with value < x before the nodes with value >= x, keeping the
 *    ORIGINAL relative order inside each group (a STABLE partition).
 *      1->4->3->2->5->2, x=3 -> 1->2->2->4->3->5
 *
 *  ---------------------------------------------------------------------------
 *  With arrays a stable partition needs extra space or O(n^2) shifting. With a
 *  linked list it's easy: just RELINK nodes into two new chains.
 *    less chain: dummy1 -> (nodes < x, in order)
 *    more chain: dummy2 -> (nodes >= x, in order)
 *    then: lessTail.next = dummy2.next;  moreTail.next = null
 *
 *  THE BUG EVERYONE HITS ONCE: forgetting  moreTail.next = null.
 *  The last node of the 'more' chain still points to its OLD successor, which
 *  may now sit EARLIER in the result -> a CYCLE (and an infinite loop when printing).
 *    1->4->3->2->5->2: the last ">= 3" node is 5, and its old next is 2, which is in
 *    the 'less' chain -> without the cut: ...->5->2->2->4->... forever.
 *
 *  DRY RUN 1,4,3,2,5,2 with x=3
 *    less: 1 -> 2 -> 2        more: 4 -> 3 -> 5
 *    join: 1->2->2->4->3->5, cut after 5
 *
 *  COMPLEXITY   Time O(n)   Space O(1) (only two dummy nodes)
 * ============================================================================
 */

import java.util.*;

public class M07_PartitionList {

    static ListNode partition(ListNode head, int x) {
        ListNode lessDummy = new ListNode(0), less = lessDummy;
        ListNode moreDummy = new ListNode(0), more = moreDummy;
        for (ListNode cur = head; cur != null; cur = cur.next) {
            if (cur.val < x) {
                less.next = cur;
                less = cur;
            } else {
                more.next = cur;
                more = cur;
            }
        }
        more.next = null;                  // cut the stale link (avoids a cycle)
        less.next = moreDummy.next;        // join
        return lessDummy.next;
    }

    public static void main(String[] args) {
        check("1,4,3,2,5,2 x=3", "[1, 2, 2, 4, 3, 5]", partition(build(1, 4, 3, 2, 5, 2), 3));
        check("2,1 x=2", "[1, 2]", partition(build(2, 1), 2));
        check("all smaller", "[1, 2]", partition(build(1, 2), 9));
        check("all bigger", "[5, 6]", partition(build(5, 6), 0));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            int[] v = rnd.ints(rnd.nextInt(15), 0, 10).toArray();
            int x = rnd.nextInt(10);
            List<Integer> exp = new ArrayList<>();
            for (int a : v) if (a < x) exp.add(a);
            for (int a : v) if (a >= x) exp.add(a);
            ListNode r = partition(build(v), x);
            ok &= (r == null ? "[]" : r.toString()).equals(exp.toString());
        }
        check("400 random lists vs stable array partition", true, ok);
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
