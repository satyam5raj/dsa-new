/*
 * ============================================================================
 *  STACK 1.2 : THE STACK PATTERNS
 * ============================================================================
 *
 *  Run:  java StackPatterns.java
 *
 *  -----------------------------------------------------------------------------------
 *   Pattern                         Clue                                      Problems
 *  -----------------------------------------------------------------------------------
 *   1. Matching / nesting           brackets, tags, "most recent unmatched"   E01 M10 H04
 *   2. Cancel / collapse            adjacent things destroy each other,       E04 E05 M12
 *                                   backspaces, "remove k digits"             M09
 *   3. MONOTONIC STACK              next/previous GREATER or SMALLER element,  E03 M02 M03
 *                                   "how many days until", spans, histograms   M07 M11 M15 H02
 *   4. Expression evaluation        calculators, postfix (RPN), precedence    M04 M06 H03
 *   5. Nested structure / undo      decode "3[a2[c]]", paths, browser history M05 M08 M13 M14
 *   6. Augmented stack              O(1) min/max alongside push/pop           M01 H01
 *   7. Recursion -> explicit stack  iterative DFS, avoid StackOverflowError   (Trees/Graphs)
 *  -----------------------------------------------------------------------------------
 *
 *  THE ONE QUESTION: "Do I need the MOST RECENT unresolved thing?" -> a stack.
 */

import java.util.*;

public class StackPatterns {

    public static void main(String[] args) {
        matching();
        cancelOut();
        monotonicStack();
        expressionEvaluation();
        nestedStructure();
        recursionToStack();
    }

    // =================================================================
    // 1. MATCHING: push openers; a closer must match the MOST RECENT opener
    // =================================================================
    static boolean balanced(String s) {
        Deque<Character> st = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') st.push(c);
            else if (st.isEmpty() || "([{".indexOf(st.pop()) != ")]}".indexOf(c)) return false;
        }
        return st.isEmpty();
    }

    static void matching() {
        System.out.println("===== 1. MATCHING =====");
        System.out.println("   balanced(\"{[()]}\") = " + balanced("{[()]}") + ", balanced(\"([)]\") = " + balanced("([)]"));
    }

    // =================================================================
    // 2. CANCEL / COLLAPSE: the new element interacts with the TOP
    //    Remove adjacent duplicates "abbaca": b,b cancel -> "aaca" -> a,a cancel -> "ca"
    //    A StringBuilder works as a stack of chars (append = push, deleteCharAt(last) = pop)
    // =================================================================
    static String removeAdjacentDuplicates(String s) {
        StringBuilder st = new StringBuilder();
        for (char c : s.toCharArray()) {
            int n = st.length();
            if (n > 0 && st.charAt(n - 1) == c) st.deleteCharAt(n - 1);   // cancel with the top
            else st.append(c);
        }
        return st.toString();
    }

    static void cancelOut() {
        System.out.println("\n===== 2. CANCEL / COLLAPSE =====");
        System.out.println("   remove adjacent duplicates \"abbaca\" -> \"" + removeAdjacentDuplicates("abbaca") + "\"");
    }

    // =================================================================
    // 3. MONOTONIC STACK - the most important stack technique
    //
    //  "For each element, find the NEXT element to its right that is GREATER."
    //  Brute force: scan right from every element -> O(n^2).
    //
    //  Keep a stack of INDICES whose answer is still unknown. Their values are
    //  DECREASING from bottom to top (a "monotonic decreasing" stack).
    //  When a new element x arrives:
    //     while the top's value < x:  x IS the answer for the top -> pop and record
    //     push x's index (it now waits for its own answer)
    //  Each index is pushed once and popped once -> O(n).
    //
    //  THE 4 VARIANTS (change the comparison and/or the direction):
    //    next greater      : scan left->right, pop while top <  x   (decreasing stack)
    //    next smaller      : scan left->right, pop while top >  x   (increasing stack)
    //    previous greater  : scan right->left, or read the stack top BEFORE pushing
    //    previous smaller  : likewise
    // =================================================================
    static int[] nextGreater(int[] a) {
        int[] ans = new int[a.length];
        Arrays.fill(ans, -1);                               // -1 = no greater element
        Deque<Integer> st = new ArrayDeque<>();             // indices, values decreasing
        for (int i = 0; i < a.length; i++) {
            while (!st.isEmpty() && a[st.peek()] < a[i]) {
                ans[st.pop()] = a[i];                       // a[i] resolves the waiting index
            }
            st.push(i);
        }
        return ans;
    }

    static int[] previousSmaller(int[] a) {
        int[] ans = new int[a.length];
        Deque<Integer> st = new ArrayDeque<>();             // values increasing
        for (int i = 0; i < a.length; i++) {
            while (!st.isEmpty() && a[st.peek()] >= a[i]) st.pop();   // can't be anyone's previous smaller anymore
            ans[i] = st.isEmpty() ? -1 : a[st.peek()];      // the top is the nearest smaller on the left
            st.push(i);
        }
        return ans;
    }

    static void monotonicStack() {
        System.out.println("\n===== 3. MONOTONIC STACK =====");
        int[] a = {2, 1, 5, 6, 2, 3};
        System.out.println("   array            " + Arrays.toString(a));
        System.out.println("   next greater   = " + Arrays.toString(nextGreater(a)));
        System.out.println("   previous smaller=" + Arrays.toString(previousSmaller(a)));
    }

    // =================================================================
    // 4. EXPRESSION EVALUATION
    //    Humans write INFIX: 3 + 4 * 2. Computers like POSTFIX (Reverse Polish
    //    Notation): 3 4 2 * +   - no parentheses or precedence rules needed.
    //    a) Evaluate postfix: numbers -> push; operator -> pop two, push the result.
    //    b) Infix -> postfix: Dijkstra's SHUNTING-YARD algorithm (an operator stack):
    //       - a number goes straight to the output
    //       - an operator: first pop operators with HIGHER or EQUAL precedence to the
    //         output (left-associative), then push it
    //       - '(' push; ')' pop to the output until the '('
    // =================================================================
    static List<String> toPostfix(String expr) {
        List<String> out = new ArrayList<>();
        Deque<Character> ops = new ArrayDeque<>();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == ' ') continue;
            if (Character.isDigit(c)) {
                int j = i;
                while (j < expr.length() && Character.isDigit(expr.charAt(j))) j++;
                out.add(expr.substring(i, j));
                i = j - 1;
            } else if (c == '(') {
                ops.push(c);
            } else if (c == ')') {
                while (ops.peek() != '(') out.add(String.valueOf(ops.pop()));
                ops.pop();                                            // discard '('
            } else {
                while (!ops.isEmpty() && ops.peek() != '(' && prec(ops.peek()) >= prec(c)) out.add(String.valueOf(ops.pop()));
                ops.push(c);
            }
        }
        while (!ops.isEmpty()) out.add(String.valueOf(ops.pop()));
        return out;
    }

    static int prec(char op) { return (op == '*' || op == '/') ? 2 : 1; }

    static int evalPostfix(List<String> tokens) {
        Deque<Integer> st = new ArrayDeque<>();
        for (String t : tokens) {
            if (t.length() == 1 && "+-*/".contains(t)) {
                int b = st.pop(), a = st.pop();                       // ORDER: b was pushed last
                st.push(switch (t) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    default -> a / b;
                });
            } else {
                st.push(Integer.parseInt(t));
            }
        }
        return st.pop();
    }

    static void expressionEvaluation() {
        System.out.println("\n===== 4. EXPRESSIONS =====");
        String e = "3 + 4 * 2 - (1 + 5) / 3";
        List<String> post = toPostfix(e);
        System.out.println("   infix   : " + e);
        System.out.println("   postfix : " + String.join(" ", post));
        System.out.println("   value   : " + evalPostfix(post) + "  (3 + 8 - 2 = 9)");
    }

    // =================================================================
    // 5. NESTED STRUCTURE: when you meet an opener, SAVE the current context on
    //    the stack and start a fresh one. On the closer, RESTORE the context and combine.
    //    Example: "3[a2[c]]" -> "accaccacc"
    // =================================================================
    static String decode(String s) {
        Deque<Integer> counts = new ArrayDeque<>();
        Deque<StringBuilder> contexts = new ArrayDeque<>();
        StringBuilder cur = new StringBuilder();
        int k = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) k = k * 10 + (c - '0');
            else if (c == '[') { counts.push(k); contexts.push(cur); cur = new StringBuilder(); k = 0; }
            else if (c == ']') { String inner = cur.toString(); cur = contexts.pop().append(inner.repeat(counts.pop())); }
            else cur.append(c);
        }
        return cur.toString();
    }

    static void nestedStructure() {
        System.out.println("\n===== 5. NESTED STRUCTURE =====");
        System.out.println("   decode(\"3[a2[c]]\") = " + decode("3[a2[c]]"));
    }

    // =================================================================
    // 6. RECURSION -> EXPLICIT STACK
    //    Every recursive algorithm uses the CALL stack. You can swap it for your own
    //    stack: no StackOverflowError on deep inputs, and you control the order.
    //    Example: sum of a deeply nested list structure, without recursion.
    // =================================================================
    static long sumDepthsRecursive(int depth, int maxDepth) {
        return depth > maxDepth ? 0 : depth + sumDepthsRecursive(depth + 1, maxDepth);
    }

    static long sumDepthsIterative(int maxDepth) {
        Deque<Integer> st = new ArrayDeque<>();
        st.push(1);
        long sum = 0;
        while (!st.isEmpty()) {
            int d = st.pop();
            if (d > maxDepth) continue;
            sum += d;
            st.push(d + 1);                                  // "the recursive call"
        }
        return sum;
    }

    static void recursionToStack() {
        System.out.println("\n===== 6. RECURSION -> EXPLICIT STACK =====");
        System.out.println("   depth 1000, recursive = " + sumDepthsRecursive(1, 1000) + ", iterative = " + sumDepthsIterative(1000));
        try {
            sumDepthsRecursive(1, 1_000_000);
        } catch (StackOverflowError e) {
            System.out.println("   depth 1,000,000 recursive -> StackOverflowError");
        }
        System.out.println("   depth 1,000,000 iterative = " + sumDepthsIterative(1_000_000) + " (no problem)");
    }
}
