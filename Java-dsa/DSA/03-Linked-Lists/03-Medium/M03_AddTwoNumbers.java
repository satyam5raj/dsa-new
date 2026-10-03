/*
 * ============================================================================
 *  MEDIUM 03 : ADD TWO NUMBERS               LeetCode #2
 *  https://leetcode.com/problems/add-two-numbers/
 *  Pattern: DIGIT-BY-DIGIT ADDITION WITH CARRY (+ dummy head to build the result)
 * ============================================================================
 *
 *  PROBLEM
 *    Two non-negative numbers stored as lists in REVERSE order (ones digit first).
 *    Return their sum in the same format.
 *      2->4->3 (342) + 5->6->4 (465) = 7->0->8 (807)
 *      9->9->9->9->9->9->9 + 9->9->9->9 = 8->9->9->9->0->0->0->1
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT CONVERT TO long? The lists can have 100 digits -> overflow.
 *
 *  SCHOOL ADDITION: the reverse order is a GIFT. The ones digits come first,
 *  which is exactly where addition starts.
 *    carry = 0
 *    while l1 or l2 has digits, OR carry > 0:
 *       sum = (l1 digit or 0) + (l2 digit or 0) + carry
 *       append (sum % 10) to the result; carry = sum / 10
 *  The "OR carry > 0" handles the final carry: 5 + 5 = 10 -> 0->1.
 *
 *  DRY RUN 342 + 465
 *    2+5+0 = 7  -> 7, carry 0
 *    4+6+0 = 10 -> 0, carry 1
 *    3+4+1 = 8  -> 8, carry 0      -> 7->0->8
 *
 *  FOLLOW-UP #445 (digits in NORMAL order, most significant first): push both
 *  lists onto STACKS (or reverse them), then add the same way.
 *  COMPLEXITY   Time O(max(m, n))   Space O(max(m, n)) for the output
 * ============================================================================
 */

import java.util.*;

public class M03_AddTwoNumbers {

    static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0), tail = dummy;
        int carry = 0;
        while (l1 != null || l2 != null || carry > 0) {
            int sum = carry;
            if (l1 != null) { sum += l1.val; l1 = l1.next; }
            if (l2 != null) { sum += l2.val; l2 = l2.next; }
            tail.next = new ListNode(sum % 10);
            tail = tail.next;
            carry = sum / 10;
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        check("342 + 465 = 807", "[7, 0, 8]", addTwoNumbers(build(2, 4, 3), build(5, 6, 4)));
        check("0 + 0", "[0]", addTwoNumbers(build(0), build(0)));
        check("9999999 + 9999", "[8, 9, 9, 9, 0, 0, 0, 1]", addTwoNumbers(build(9, 9, 9, 9, 9, 9, 9), build(9, 9, 9, 9)));
        check("5 + 5 (final carry)", "[0, 1]", addTwoNumbers(build(5), build(5)));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            java.math.BigInteger a = new java.math.BigInteger(1 + rnd.nextInt(40), rnd), b = new java.math.BigInteger(1 + rnd.nextInt(40), rnd);
            ok &= toNumber(addTwoNumbers(fromNumber(a), fromNumber(b))).equals(a.add(b));
        }
        check("300 random big numbers vs BigInteger", true, ok);
        summary();
    }

    static ListNode fromNumber(java.math.BigInteger x) {
        String s = new StringBuilder(x.toString()).reverse().toString();
        return build(s.chars().map(c -> c - '0').toArray());
    }

    static java.math.BigInteger toNumber(ListNode l) {
        StringBuilder sb = new StringBuilder();
        for (; l != null; l = l.next) sb.append(l.val);
        return new java.math.BigInteger(sb.reverse().toString());
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
