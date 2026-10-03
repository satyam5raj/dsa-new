/*
 * ============================================================================
 *  EASY 02 : IMPLEMENT QUEUE USING STACKS    LeetCode #232
 *  https://leetcode.com/problems/implement-queue-using-stacks/
 *  Pattern: TWO STACKS (in + out), lazy transfer -> amortized O(1)
 * ============================================================================
 *
 *  PROBLEM
 *    Build a FIFO queue (push, pop, peek, empty) using only stack operations.
 *
 *  ---------------------------------------------------------------------------
 *  KEY FACT: pouring a stack into another stack REVERSES the order.
 *  Reversing a LIFO order gives FIFO order!
 *
 *  TWO STACKS
 *    in  : receives every push
 *    out : serves pop/peek. When 'out' is EMPTY, pour everything from 'in' into it.
 *          The oldest element ends up on top of 'out'.
 *
 *    push 1,2,3      in [1,2,3>     out []
 *    pop             out empty -> pour: in []   out [3,2,1>  -> pop 1
 *    push 4          in [4>         out [3,2>
 *    pop             out not empty -> pop 2   (no pouring!)
 *
 *  WHY ONLY POUR WHEN 'out' IS EMPTY? If 'out' still has elements, they're OLDER
 *  than anything in 'in'. Pouring on top of them would break the order.
 *
 *  AMORTIZED O(1): each element is pushed to 'in' once, moved to 'out' once and
 *  popped once -> 3 operations per element over its lifetime. A single pop can
 *  cost O(n) (a big pour), but those are rare: the AVERAGE is O(1).
 *  (The same idea as a dynamic array's resize, see the complexity topic.)
 * ============================================================================
 */

import java.util.*;

public class E02_ImplementQueueUsingStacks {

    static class MyQueue {
        private final Deque<Integer> in = new ArrayDeque<>();
        private final Deque<Integer> out = new ArrayDeque<>();

        public void push(int x) { in.push(x); }

        public int pop() {
            moveIfNeeded();
            return out.pop();
        }

        public int peek() {
            moveIfNeeded();
            return out.peek();
        }

        public boolean empty() { return in.isEmpty() && out.isEmpty(); }

        private void moveIfNeeded() {
            if (out.isEmpty()) {
                while (!in.isEmpty()) out.push(in.pop());   // reverse the order: oldest on top
            }
        }
    }

    public static void main(String[] args) {
        MyQueue q = new MyQueue();
        q.push(1);
        q.push(2);
        check("peek after push 1,2", 1, q.peek());
        check("pop", 1, q.pop());
        q.push(3);
        check("pop (2 was older than 3)", 2, q.pop());
        check("pop", 3, q.pop());
        check("empty", true, q.empty());

        Random rnd = new Random(1);
        MyQueue mine = new MyQueue();
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 10_000; i++) {
            if (ref.isEmpty() || rnd.nextBoolean()) { int v = rnd.nextInt(); mine.push(v); ref.offer(v); }
            else ok &= mine.pop() == ref.poll();
        }
        check("10,000 random ops vs a real queue", true, ok);
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
