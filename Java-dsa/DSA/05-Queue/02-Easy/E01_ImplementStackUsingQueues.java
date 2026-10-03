/*
 * ============================================================================
 *  EASY 01 : IMPLEMENT STACK USING QUEUES    LeetCode #225
 *  https://leetcode.com/problems/implement-stack-using-queues/
 *  Pattern: ROTATE THE QUEUE after each push (the mirror of Stack/E02)
 * ============================================================================
 *
 *  PROBLEM
 *    Build a LIFO stack (push, pop, top, empty) using only queue operations
 *    (offer at the back, poll/peek at the front, size, isEmpty).
 *
 *  ---------------------------------------------------------------------------
 *  THE CHALLENGE: a queue gives back the OLDEST element; a stack must give the NEWEST.
 *
 *  ONE QUEUE, COSTLY PUSH (the cleanest version)
 *    push(x): offer x at the back, then ROTATE: move the (size - 1) elements in
 *             front of it to the back, one by one. Now x is at the FRONT.
 *    pop/top: just poll/peek the front -> O(1)
 *
 *      queue after push 1:          [1]
 *      push 2: offer -> [1,2], rotate 1 -> [2,1]
 *      push 3: offer -> [2,1,3], rotate 2 -> [3,2,1]      front = 3 = the newest
 *
 *  ALTERNATIVE: cheap push, costly pop (move n-1 elements to a second queue to
 *  reach the last one). Pick based on which operation is more frequent.
 *
 *  Compare with Stack/E02 (Queue using Stacks): that one is AMORTIZED O(1),
 *  because reversing twice restores the order. Here every push really costs O(n):
 *  you can't get an amortized O(1) stack out of plain queues.
 *  COMPLEXITY   push O(n), pop/top/empty O(1)
 * ============================================================================
 */

import java.util.*;

public class E01_ImplementStackUsingQueues {

    static class MyStack {
        private final Queue<Integer> q = new ArrayDeque<>();

        public void push(int x) {
            q.offer(x);
            for (int i = 0; i < q.size() - 1; i++) q.offer(q.poll());   // rotate x to the front
        }

        public int pop() { return q.poll(); }
        public int top() { return q.peek(); }
        public boolean empty() { return q.isEmpty(); }
    }

    public static void main(String[] args) {
        MyStack s = new MyStack();
        s.push(1);
        s.push(2);
        check("top after push 1,2", 2, s.top());
        check("pop", 2, s.pop());
        check("empty?", false, s.empty());
        check("pop", 1, s.pop());
        check("empty now", true, s.empty());

        Random rnd = new Random(1);
        MyStack mine = new MyStack();
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 3000; i++) {
            if (ref.isEmpty() || rnd.nextBoolean()) { int v = rnd.nextInt(); mine.push(v); ref.push(v); }
            else ok &= mine.pop() == ref.pop();
        }
        check("3,000 random ops vs a real stack", true, ok);
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
