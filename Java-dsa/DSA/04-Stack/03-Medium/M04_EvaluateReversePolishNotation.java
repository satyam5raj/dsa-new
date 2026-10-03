/*
 * ============================================================================
 *  MEDIUM 04 : EVALUATE REVERSE POLISH NOTATION   LeetCode #150
 *  https://leetcode.com/problems/evaluate-reverse-polish-notation/
 *  Pattern: OPERAND STACK (postfix evaluation)
 * ============================================================================
 *
 *  PROBLEM
 *    Evaluate an expression in Reverse Polish (postfix) notation. Operators
 *    + - * /, and division truncates toward zero.
 *      ["2","1","+","3","*"]  -> 9     ((2 + 1) * 3)
 *      ["4","13","5","/","+"] -> 6     (4 + 13 / 5)
 *
 *  ---------------------------------------------------------------------------
 *  POSTFIX = the operator comes AFTER its two operands. No parentheses and no
 *  precedence rules are needed: the order of the tokens says everything.
 *  (Old HP calculators and the JVM's bytecode both work this way: the JVM is a STACK machine.)
 *
 *  ALGORITHM
 *    number   -> push it
 *    operator -> b = pop(), a = pop(), push(a OP b)
 *                ORDER MATTERS for - and /:  b was pushed LAST, so it's the RIGHT operand
 *    the final answer is the only value left on the stack
 *
 *  DRY RUN ["4","13","5","/","+"]
 *    4 -> [4]   13 -> [4,13]   5 -> [4,13,5]
 *    "/" : b=5, a=13 -> 13/5 = 2 -> [4,2]
 *    "+" : b=2, a=4  -> 6        -> [6]
 *
 *  JAVA NOTE: integer division in Java already truncates toward zero (-7/2 = -3),
 *  exactly as required.
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M04_EvaluateReversePolishNotation {

    static int evalRPN(String[] tokens) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (String t : tokens) {
            switch (t) {
                case "+" -> stack.push(stack.pop() + stack.pop());
                case "*" -> stack.push(stack.pop() * stack.pop());
                case "-" -> { int b = stack.pop(), a = stack.pop(); stack.push(a - b); }
                case "/" -> { int b = stack.pop(), a = stack.pop(); stack.push(a / b); }
                default -> stack.push(Integer.parseInt(t));          // handles "-11" (negative numbers) too
            }
        }
        return stack.pop();
    }

    public static void main(String[] args) {
        check("2 1 + 3 *", 9, evalRPN(new String[]{"2", "1", "+", "3", "*"}));
        check("4 13 5 / +", 6, evalRPN(new String[]{"4", "13", "5", "/", "+"}));
        check("LeetCode example 3", 22, evalRPN(new String[]{"10", "6", "9", "3", "+", "-11", "*", "/", "*", "17", "+", "5", "+"}));
        check("operand order: 7 2 -", 5, evalRPN(new String[]{"7", "2", "-"}));
        check("truncation: -7 2 /", -3, evalRPN(new String[]{"-7", "2", "/"}));
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
