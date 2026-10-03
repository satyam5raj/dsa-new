/*
 * ============================================================================
 *  HARD 03 : BASIC CALCULATOR  ⭐ bonus (parentheses + unary minus)   LeetCode #224
 *  https://leetcode.com/problems/basic-calculator/
 *  Pattern: STACK OF SAVED (result, sign) PAIRS AT EACH '('
 * ============================================================================
 *
 *  PROBLEM
 *    Evaluate a string with integers, '+', '-', '(', ')' and spaces. '-' can be
 *    UNARY ("-(2+3)", "-1"). No * or /.
 *      "1 + 1"               -> 2
 *      " 2-1 + 2 "           -> 3
 *      "(1+(4+5+2)-3)+(6+8)" -> 23
 *      "-(2 + 3)"            -> -5
 *
 *  ---------------------------------------------------------------------------
 *  WITHOUT PARENTHESES it's a running sum: result += sign * number, where the
 *  sign comes from the '+' or '-' before the number.
 *
 *  PARENTHESES = A NESTED SUB-PROBLEM (same idea as Decode String, M05):
 *    on '(' : SAVE the outer state (the result so far, and the sign in front of the
 *             parenthesis) on the stack, then start fresh: result = 0, sign = +1
 *    on ')' : the inner result is done. Restore:
 *             result = savedResult + savedSign * innerResult
 *  Unary minus needs no special case: "-(" just sets sign = -1 before the '(' is saved.
 *
 *  DRY RUN "1-(4+5)"
 *    '1'  -> number 1, then '-' : result = 1, sign = -1
 *    '('  -> push (1, -1); result = 0, sign = +1
 *    '4'  '+' -> result 4;  '5' then ')' -> result = 9
 *    ')'  -> result = 1 + (-1) * 9 = -8
 *
 *  ADDING * AND / (Basic Calculator III): combine this with M06's "terms" trick,
 *  or use the shunting-yard algorithm from 01-Basics/StackPatterns.java.
 *  COMPLEXITY   Time O(n)   Space O(nesting depth)
 * ============================================================================
 */

import java.util.*;

public class H03_BasicCalculator {

    static int calculate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int result = 0, sign = 1, num = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+' || c == '-') {
                result += sign * num;                  // finish the previous number
                num = 0;
                sign = c == '+' ? 1 : -1;
            } else if (c == '(') {
                stack.push(result);                    // save the outer state
                stack.push(sign);
                result = 0;
                sign = 1;
            } else if (c == ')') {
                result += sign * num;                  // finish the last number inside
                num = 0;
                int savedSign = stack.pop(), savedResult = stack.pop();
                result = savedResult + savedSign * result;
            }
            // spaces are ignored
        }
        return result + sign * num;
    }

    public static void main(String[] args) {
        check("1 + 1", 2, calculate("1 + 1"));
        check("\" 2-1 + 2 \"", 3, calculate(" 2-1 + 2 "));
        check("(1+(4+5+2)-3)+(6+8)", 23, calculate("(1+(4+5+2)-3)+(6+8)"));
        check("-(2 + 3) (unary minus)", -5, calculate("-(2 + 3)"));
        check("1-(-2) (minus minus)", 3, calculate("1-(-2)"));
        check("2147483647 (big number)", 2147483647, calculate("2147483647"));
        check("- (3 + (4 + 5))", -12, calculate("- (3 + (4 + 5))"));
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
