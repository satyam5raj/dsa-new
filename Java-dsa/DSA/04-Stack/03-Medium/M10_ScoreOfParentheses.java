/*
 * ============================================================================
 *  MEDIUM 10 : SCORE OF PARENTHESES          LeetCode #856
 *  https://leetcode.com/problems/score-of-parentheses/
 *  Pattern: STACK OF SCORES PER NESTING LEVEL  /  depth counting (O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    Balanced string of ( and ). Scoring rules:
 *      "()"   = 1
 *      AB     = A + B        (side by side: add)
 *      (A)    = 2 * A        (wrapping: double)
 *        "()" -> 1    "(())" -> 2    "()()" -> 2    "(()(()))" -> 6
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: STACK OF SCORES (one running score per open level)
 *    stack starts with [0] (the score of the outermost level)
 *    '(' -> push 0                              (start a new, deeper level)
 *    ')' -> inner = pop()                       (the finished level's score)
 *           add max(2 * inner, 1) to the new top (an empty "()" is worth 1)
 *    answer = the remaining value
 *
 *  DRY RUN "(()(()))"
 *    [0] ( [0,0] ( [0,0,0] ) inner 0 -> 1 -> [0,1]
 *    ( [0,1,0] ( [0,1,0,0] ) -> 1 -> [0,1,1] ) inner 1 -> 2 -> [0,3]
 *    ) inner 3 -> 6 -> [6]    -> 6
 *
 *  APPROACH 2: O(1) SPACE INSIGHT
 *    Only the "()" pairs create points. A "()" at depth d (inside d other pairs)
 *    gets doubled d times -> worth 2^d. Answer = sum of 2^depth over every "()".
 *    "(()(()))": the first () at depth 1 -> 2, the second at depth 2 -> 4 -> 6.
 *
 *  COMPLEXITY   Time O(n)   Space O(n) for the stack, O(1) for approach 2
 * ============================================================================
 */

import java.util.*;

public class M10_ScoreOfParentheses {

    static int scoreOfParentheses(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(0);
        for (char c : s.toCharArray()) {
            if (c == '(') {
                stack.push(0);
            } else {
                int inner = stack.pop();
                stack.push(stack.pop() + Math.max(2 * inner, 1));
            }
        }
        return stack.pop();
    }

    static int scoreConstantSpace(String s) {
        int score = 0, depth = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                depth++;
            } else {
                depth--;
                if (s.charAt(i - 1) == '(') score += 1 << depth;   // a "()" at this depth
            }
        }
        return score;
    }

    public static void main(String[] args) {
        check("()", 1, scoreOfParentheses("()"));
        check("(())", 2, scoreOfParentheses("(())"));
        check("()()", 2, scoreOfParentheses("()()"));
        check("(()(()))", 6, scoreOfParentheses("(()(()))"));
        check("O(1) version: (()(()))", 6, scoreConstantSpace("(()(()))"));
        check("O(1) version: ((()))()", 5, scoreConstantSpace("((()))()"));
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
