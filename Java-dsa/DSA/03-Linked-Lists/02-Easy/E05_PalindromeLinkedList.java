/*
 * ============================================================================
 *  EASY 05 : PALINDROME LINKED LIST          LeetCode #234
 *  https://leetcode.com/problems/palindrome-linked-list/
 *  Pattern: FIND MIDDLE (fast/slow) + REVERSE SECOND HALF + COMPARE
 * ============================================================================
 *
 *  PROBLEM
 *    Does the list read the same forwards and backwards? Follow-up: O(1) space.
 *      1->2->2->1 -> true       1->2 -> false
 *
 *  ---------------------------------------------------------------------------
 *  EASY WAY: copy the values into an array, then use two pointers -> O(n) space.
 *  The difficulty with a singly list: we can't walk BACKWARDS from the tail.
 *
 *  O(1) SPACE: make the second half walk backwards by REVERSING it.
 *    1. find the middle with slow/fast
 *    2. reverse the second half in place
 *    3. compare the first half with the reversed second half, node by node
 *    4. (good manners) reverse the second half back to restore the input
 *
 *  DRY RUN 1->2->3->2->1
 *    middle (slow) = 3
 *    reverse from 3: 1->2->[3] and 1->2->3 (reversed tail: 1->2->3)
 *    compare 1=1, 2=2, 3=3 -> true
 *
 *  This problem is a COMBINATION of two basic patterns. That's typical for
 *  linked list "medium-feeling easies".
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E05_PalindromeLinkedList {

    static boolean isPalindrome(ListNode head) {
        if (head == null || head.next == null) return true;
        // 1. middle
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        // 2. reverse the second half (starting at slow)
        ListNode secondHalf = reverse(slow);
        // 3. compare
        boolean result = true;
        ListNode p1 = head, p2 = secondHalf;
        while (p2 != null) {                       // the second half is the shorter or equal one
            if (p1.val != p2.val) { result = false; break; }
            p1 = p1.next;
            p2 = p2.next;
        }
        // 4. restore the list
        reverse(secondHalf);
        return result;
    }

    static ListNode reverse(ListNode head) {
        ListNode prev = null;
        while (head != null) {
            ListNode next = head.next;
            head.next = prev;
            prev = head;
            head = next;
        }
        return prev;
    }

    public static void main(String[] args) {
        check("1->2->2->1", true, isPalindrome(build(1, 2, 2, 1)));
        check("1->2", false, isPalindrome(build(1, 2)));
        check("1->2->3->2->1 (odd length)", true, isPalindrome(build(1, 2, 3, 2, 1)));
        check("single node", true, isPalindrome(build(9)));
        check("1->2->3", false, isPalindrome(build(1, 2, 3)));
        ListNode l = build(1, 2, 3, 2, 1);
        isPalindrome(l);
        check("input restored afterwards", "[1, 2, 3, 2, 1]", l);
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
