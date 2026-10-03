/*
 * ============================================================================
 *  EASY 01 : REVERSE LINKED LIST             LeetCode #206
 *  https://leetcode.com/problems/reverse-linked-list/
 *  Pattern: IN-PLACE REVERSAL (prev / cur / next)   <- the #1 linked list skill
 * ============================================================================
 *
 *  PROBLEM
 *    Reverse a singly linked list and return the new head.
 *      1->2->3->4->5 -> 5->4->3->2->1        [] -> []
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT JUST COPY INTO AN ARRAY? Works (O(n) extra space), but interviewers
 *  want the O(1) pointer solution. It shows up inside many harder problems
 *  (Palindrome List, Reorder List, Reverse in k-Group).
 *
 *  ITERATIVE (O(1) space): flip each arrow, one node at a time.
 *    prev = null, cur = head
 *    while cur != null:
 *        next = cur.next     // 1. SAVE the rest (we're about to lose it)
 *        cur.next = prev     // 2. FLIP the arrow
 *        prev = cur          // 3. move prev forward
 *        cur = next          // 4. move cur forward
 *    return prev             // cur is null; prev is the old tail = the new head
 *
 *  DRY RUN 1->2->3
 *    start      prev=null  cur=1        null   1->2->3
 *    step 1     prev=1     cur=2        null<-1   2->3
 *    step 2     prev=2     cur=3        null<-1<-2   3
 *    step 3     prev=3     cur=null     null<-1<-2<-3     -> return 3
 *
 *  RECURSIVE (O(n) stack):
 *    newHead = reverse(head.next)    // trust it: the rest is now reversed, ending at head.next
 *    head.next.next = head           // make the node after me point back to me
 *    head.next = null                // I'm the new tail
 *
 *  COMPLEXITY   iterative: Time O(n), Space O(1)    recursive: Time O(n), Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E01_ReverseLinkedList {

    static ListNode reverseList(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }
        return prev;
    }

    static ListNode reverseListRecursive(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode newHead = reverseListRecursive(head.next);
        head.next.next = head;
        head.next = null;
        return newHead;
    }

    public static void main(String[] args) {
        check("1->2->3->4->5", "[5, 4, 3, 2, 1]", reverseList(build(1, 2, 3, 4, 5)));
        check("1->2", "[2, 1]", reverseList(build(1, 2)));
        check("single node", "[7]", reverseList(build(7)));
        check("empty list", "null", reverseList(build()));
        check("recursive version", "[5, 4, 3, 2, 1]", reverseListRecursive(build(1, 2, 3, 4, 5)));
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
