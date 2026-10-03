/*
 * ============================================================================
 *  HARD 04 : LONGEST VALID PARENTHESES  ⭐ bonus   LeetCode #32
 *  https://leetcode.com/problems/longest-valid-parentheses/
 *  Pattern: STACK OF INDICES with a "last invalid position" sentinel
 * ============================================================================
 *
 *  PROBLEM
 *    Length of the longest well-formed (balanced) parentheses SUBSTRING.
 *      "(()"     -> 2    ("()")
 *      ")()())"  -> 4    ("()()")
 *      ""        -> 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: check every even-length substring with E01's validity test -> O(n^3)
 *
 *  STACK OF INDICES. The bottom of the stack always holds the index of the LAST
 *  position that can't be part of a valid substring (a "barrier"). Start with -1.
 *    '(' -> push its index
 *    ')' -> pop (it matches the most recent '(' ... or the barrier)
 *           if the stack is now EMPTY: this ')' is unmatched -> push i as the new barrier
 *           else: a valid substring ends at i, starting right after the stack top:
 *                 length = i - stack.top
 *
 *  DRY RUN ")()())"
 *    start [-1]
 *    i0 ')' pop -1 -> empty -> push 0 (barrier)        [0]
 *    i1 '(' push                                        [0,1]
 *    i2 ')' pop 1 -> top 0 -> length 2-0 = 2            [0]
 *    i3 '(' push                                        [0,3]
 *    i4 ')' pop 3 -> top 0 -> length 4-0 = 4  <- best   [0]
 *    i5 ')' pop 0 -> empty -> push 5                    [5]
 *
 *  O(1) SPACE ALTERNATIVE: count open/close left-to-right (reset when close > open,
 *  record when equal), then right-to-left (mirrored). Implemented below.
 *  DP ALTERNATIVE: dp[i] = the longest valid substring ending at i (covered in DP).
 *
 *  COMPLEXITY   Time O(n)   Space O(n) (stack) or O(1) (two scans)
 * ============================================================================
 */

import java.util.*;

public class H04_LongestValidParentheses {

    static int longestValidParentheses(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(-1);                                   // the initial barrier
        int best = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                stack.push(i);
            } else {
                stack.pop();
                if (stack.isEmpty()) stack.push(i);       // unmatched ')' -> new barrier
                else best = Math.max(best, i - stack.peek());
            }
        }
        return best;
    }

    static int longestValidParenthesesTwoScans(String s) {
        int best = 0, open = 0, close = 0;
        for (int i = 0; i < s.length(); i++) {            // left -> right
            if (s.charAt(i) == '(') open++; else close++;
            if (open == close) best = Math.max(best, 2 * close);
            else if (close > open) open = close = 0;      // too many ')': nothing can extend past here
        }
        open = close = 0;
        for (int i = s.length() - 1; i >= 0; i--) {       // right -> left (catches cases like "(()")
            if (s.charAt(i) == '(') open++; else close++;
            if (open == close) best = Math.max(best, 2 * open);
            else if (open > close) open = close = 0;
        }
        return best;
    }

    static int brute(String s) {
        int best = 0;
        for (int i = 0; i < s.length(); i++) {
            int bal = 0;
            for (int j = i; j < s.length(); j++) {
                bal += s.charAt(j) == '(' ? 1 : -1;
                if (bal < 0) break;
                if (bal == 0) best = Math.max(best, j - i + 1);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("(()", 2, longestValidParentheses("(()"));
        check(")()())", 4, longestValidParentheses(")()())"));
        check("\"\" (empty)", 0, longestValidParentheses(""));
        check("()(()) (whole string)", 6, longestValidParentheses("()(())"));
        check("two-scan version: (()", 2, longestValidParenthesesTwoScans("(()"));
        Random rnd = new Random(32);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = rnd.nextInt(16); i < n; i++) sb.append(rnd.nextBoolean() ? '(' : ')');
            String x = sb.toString();
            ok &= longestValidParentheses(x) == brute(x) && longestValidParenthesesTwoScans(x) == brute(x);
        }
        check("1,000 random strings (both) vs brute force", true, ok);
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
