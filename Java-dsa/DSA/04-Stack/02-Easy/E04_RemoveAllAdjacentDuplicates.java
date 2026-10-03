/*
 * ============================================================================
 *  EASY 04 : REMOVE ALL ADJACENT DUPLICATES IN STRING  ⭐ bonus   LeetCode #1047
 *  https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string/
 *  Pattern: CANCEL WITH THE TOP (chain reactions for free)
 * ============================================================================
 *
 *  PROBLEM
 *    Repeatedly remove two ADJACENT equal letters until none are left.
 *      "abbaca" -> "ca"     (bb goes -> "aaca", then aa goes -> "ca")
 *      "azxxzy" -> "ay"
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: scan for a pair, remove it, rescan from the start -> O(n^2)
 *  The removals can CHAIN: removing "bb" makes the two a's neighbours.
 *
 *  STACK: process left to right. The top of the stack is the character that
 *  is currently "to the left" of the new one (after all removals so far).
 *    if c == top -> pop (the pair cancels, and the NEW top is the next neighbour,
 *                   so chain reactions happen automatically)
 *    else        -> push c
 *  A StringBuilder is the perfect char stack here: append = push,
 *  deleteCharAt(length - 1) = pop, and the result is already in order.
 *
 *  DRY RUN "abbaca"
 *    a -> [a]   b -> [a,b]   b == top -> pop [a]   a == top -> pop []
 *    c -> [c]   a -> [c,a]   -> "ca"
 *
 *  FOLLOW-UP #1209: remove k adjacent duplicates -> store (char, count) pairs on the stack.
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E04_RemoveAllAdjacentDuplicates {

    static String removeDuplicates(String s) {
        StringBuilder stack = new StringBuilder();
        for (char c : s.toCharArray()) {
            int n = stack.length();
            if (n > 0 && stack.charAt(n - 1) == c) stack.deleteCharAt(n - 1);   // cancel the pair
            else stack.append(c);
        }
        return stack.toString();
    }

    // Follow-up #1209: remove groups of k equal adjacent letters
    static String removeDuplicatesK(String s, int k) {
        Deque<int[]> stack = new ArrayDeque<>();                 // [char, run length]
        for (char c : s.toCharArray()) {
            if (!stack.isEmpty() && stack.peek()[0] == c) {
                if (++stack.peek()[1] == k) stack.pop();         // the run reached k -> remove it
            } else {
                stack.push(new int[]{c, 1});
            }
        }
        StringBuilder sb = new StringBuilder();
        Iterator<int[]> it = stack.descendingIterator();         // bottom to top = left to right
        while (it.hasNext()) {
            int[] e = it.next();
            sb.append(String.valueOf((char) e[0]).repeat(e[1]));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        check("abbaca", "ca", removeDuplicates("abbaca"));
        check("azxxzy (chain reaction)", "ay", removeDuplicates("azxxzy"));
        check("aaaa (all cancel)", "", removeDuplicates("aaaa"));
        check("abc (nothing to remove)", "abc", removeDuplicates("abc"));
        check("#1209 deeedbbcccbdaa k=3", "aa", removeDuplicatesK("deeedbbcccbdaa", 3));
        summary();
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
