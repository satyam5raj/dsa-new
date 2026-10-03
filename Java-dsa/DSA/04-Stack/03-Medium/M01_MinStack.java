/*
 * ============================================================================
 *  MEDIUM 01 : MIN STACK                     LeetCode #155
 *  https://leetcode.com/problems/min-stack/
 *  Pattern: AUGMENTED STACK (store the running minimum with each element)
 * ============================================================================
 *
 *  PROBLEM
 *    A stack with push, pop, top AND getMin(), ALL in O(1).
 *
 *  ---------------------------------------------------------------------------
 *  WHY ONE "min" VARIABLE ISN'T ENOUGH: push 5, push 2 (min = 2), pop -> what's
 *  the min now? We forgot that it was 5 before the 2 arrived.
 *
 *  KEY INSIGHT: a stack only changes at the top, so "the minimum of everything
 *  BELOW and INCLUDING this element" never changes while the element is in the stack.
 *  -> store it WITH each element.
 *
 *  APPROACH 1: a stack of pairs [value, minSoFar]
 *    push(x): minSoFar = min(x, current top's minSoFar)
 *    getMin(): top's minSoFar
 *
 *    push 5 -> [5,5]
 *    push 2 -> [2,2]
 *    push 7 -> [7,2]
 *    getMin = 2;  pop (7);  pop (2);  getMin = 5   <- remembered!
 *
 *  APPROACH 2: two stacks, the min stack only gets a value when it's <= the
 *  current min (saves space when the minimum rarely changes). Pop from it only
 *  when the popped value equals its top. Use <= so duplicate minimums are tracked.
 *
 *  COMPLEXITY   all operations O(1)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M01_MinStack {

    static class MinStack {
        private final Deque<int[]> stack = new ArrayDeque<>();     // [value, min of everything up to here]

        public void push(int val) {
            int min = stack.isEmpty() ? val : Math.min(val, stack.peek()[1]);
            stack.push(new int[]{val, min});
        }

        public void pop() { stack.pop(); }
        public int top() { return stack.peek()[0]; }
        public int getMin() { return stack.peek()[1]; }
    }

    static class MinStackTwoStacks {
        private final Deque<Integer> values = new ArrayDeque<>();
        private final Deque<Integer> mins = new ArrayDeque<>();

        public void push(int val) {
            values.push(val);
            if (mins.isEmpty() || val <= mins.peek()) mins.push(val);   // <= keeps duplicate minimums
        }

        public void pop() {
            if (values.pop().equals(mins.peek())) mins.pop();          // equals: Integer objects!
        }

        public int top() { return values.peek(); }
        public int getMin() { return mins.peek(); }
    }

    public static void main(String[] args) {
        MinStack s = new MinStack();
        s.push(-2);
        s.push(0);
        s.push(-3);
        check("getMin after -2,0,-3", -3, s.getMin());
        s.pop();
        check("top after pop", 0, s.top());
        check("getMin after pop (remembered -2)", -2, s.getMin());

        Random rnd = new Random(1);
        MinStack a = new MinStack();
        MinStackTwoStacks b = new MinStackTwoStacks();
        List<Integer> ref = new ArrayList<>();
        boolean ok = true;
        for (int i = 0; i < 10_000; i++) {
            if (ref.isEmpty() || rnd.nextInt(3) > 0) {
                int v = rnd.nextInt(20) - 10;
                a.push(v); b.push(v); ref.add(v);
            } else {
                a.pop(); b.pop(); ref.remove(ref.size() - 1);
            }
            if (!ref.isEmpty()) {
                int min = Collections.min(ref);
                ok &= a.getMin() == min && b.getMin() == min && a.top() == ref.get(ref.size() - 1);
            }
        }
        check("10,000 random ops (both versions)", true, ok);
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
