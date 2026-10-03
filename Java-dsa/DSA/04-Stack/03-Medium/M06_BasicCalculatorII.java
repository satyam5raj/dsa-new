/*
 * ============================================================================
 *  MEDIUM 06 : BASIC CALCULATOR II           LeetCode #227
 *  https://leetcode.com/problems/basic-calculator-ii/
 *  Pattern: STACK OF TERMS (apply * and / immediately, defer + and -)
 * ============================================================================
 *
 *  PROBLEM
 *    Evaluate a string with non-negative integers, + - * / and spaces (no
 *    parentheses). Respect precedence; division truncates toward zero.
 *      "3+2*2"     -> 7
 *      " 3/2 "     -> 1
 *      " 3+5 / 2 " -> 5
 *
 *  ---------------------------------------------------------------------------
 *  THE PRECEDENCE PROBLEM: in "3+2*2" we can't add 3+2 as soon as we see the '*'.
 *
 *  IDEA: split the expression into TERMS joined by + and -:
 *      3 + 2*2 - 8/4  =  (+3) + (+2*2) + (-8/4)
 *  Then the answer is simply the SUM of the terms.
 *    - when the operator BEFORE a number is + : push +num  (a new term)
 *    - when it's -                            : push -num  (a new negative term)
 *    - when it's *                            : pop the last term, push term * num
 *    - when it's /                            : pop the last term, push term / num
 *  * and / attach to the LAST term immediately (higher precedence), while + and -
 *  just start new terms. At the end, sum the stack.
 *
 *  IMPLEMENTATION DETAIL: we act on a number only when we reach the NEXT operator
 *  (or the end of the string), using the operator we saw BEFORE it ('lastOp').
 *
 *  DRY RUN "3+2*2"
 *    read 3, see '+' -> lastOp was '+' (initial) -> push 3       [3],  lastOp='+'
 *    read 2, see '*' -> lastOp '+' -> push 2                     [3,2], lastOp='*'
 *    read 2, end     -> lastOp '*' -> pop 2, push 2*2 = 4        [3,4]
 *    sum = 7
 *
 *  O(1) SPACE VERSION: only the LAST term is ever modified, so keep it in a variable
 *  ('last') plus a running 'result' instead of a whole stack (shown below).
 *  COMPLEXITY   Time O(n)   Space O(n) (stack) or O(1)
 * ============================================================================
 */

import java.util.*;

public class M06_BasicCalculatorII {

    static int calculate(String s) {
        Deque<Integer> terms = new ArrayDeque<>();
        int num = 0;
        char lastOp = '+';
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) num = num * 10 + (c - '0');
            if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1) {   // an operator, or the end
                switch (lastOp) {
                    case '+' -> terms.push(num);
                    case '-' -> terms.push(-num);
                    case '*' -> terms.push(terms.pop() * num);
                    case '/' -> terms.push(terms.pop() / num);
                }
                lastOp = c;
                num = 0;
            }
        }
        int sum = 0;
        for (int t : terms) sum += t;
        return sum;
    }

    static int calculateConstantSpace(String s) {
        int result = 0, last = 0, num = 0;
        char lastOp = '+';
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) num = num * 10 + (c - '0');
            if ((!Character.isDigit(c) && c != ' ') || i == s.length() - 1) {
                if (lastOp == '+' || lastOp == '-') {
                    result += last;                          // the previous term is final now
                    last = lastOp == '+' ? num : -num;
                } else if (lastOp == '*') {
                    last *= num;
                } else {
                    last /= num;
                }
                lastOp = c;
                num = 0;
            }
        }
        return result + last;
    }

    public static void main(String[] args) {
        check("3+2*2", 7, calculate("3+2*2"));
        check("\" 3/2 \"", 1, calculate(" 3/2 "));
        check("\" 3+5 / 2 \"", 5, calculate(" 3+5 / 2 "));
        check("14-3*2/4+100", 113, calculate("14-3*2/4+100"));
        check("42 (single number)", 42, calculate("42"));
        check("O(1) space version", 113, calculateConstantSpace("14-3*2/4+100"));
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
