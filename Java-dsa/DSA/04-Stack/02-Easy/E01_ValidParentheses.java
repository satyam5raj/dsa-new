/*
 * ============================================================================
 *  EASY 01 : VALID PARENTHESES               LeetCode #20
 *  https://leetcode.com/problems/valid-parentheses/
 *  Pattern: MATCHING WITH A STACK
 * ============================================================================
 *
 *  PROBLEM
 *    s contains only ()[]{}. Valid if every opener is closed by the SAME type, in
 *    the CORRECT order.
 *      "()[]{}" -> true     "(]" -> false     "([)]" -> false     "{[]}" -> true
 *
 *  ---------------------------------------------------------------------------
 *  WHY COUNTING ISN'T ENOUGH: "([)]" has one of each, but the ORDER is wrong.
 *  The closer must match the MOST RECENTLY OPENED, still-unclosed bracket
 *  -> that's exactly what a stack's top is.
 *
 *  ALGORITHM
 *    opener -> push the EXPECTED closer (a neat trick: then we only compare chars)
 *    closer -> the stack must be non-empty AND its top must equal this closer
 *    at the end: the stack must be EMPTY (no unclosed openers left)
 *
 *  DRY RUN "([)]"
 *    '(' push ')'          stack [)]
 *    '[' push ']'          stack [), ]]
 *    ')' top is ']' != ')' -> false
 *
 *  EDGE CASES: ")" (closer on an empty stack), "((" (leftovers), odd length (quick reject).
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E01_ValidParentheses {

    static boolean isValid(String s) {
        if (s.length() % 2 == 1) return false;             // quick reject
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(') stack.push(')');
            else if (c == '[') stack.push(']');
            else if (c == '{') stack.push('}');
            else if (stack.isEmpty() || stack.pop() != c) return false;   // wrong or missing opener
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        check("()[]{}", true, isValid("()[]{}"));
        check("(]", false, isValid("(]"));
        check("([)] (wrong order)", false, isValid("([)]"));
        check("{[]} (nested)", true, isValid("{[]}"));
        check(") (closer first)", false, isValid(")"));
        check("(( (unclosed)", false, isValid("(("));
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
