/*
 * ============================================================================
 *  HARD 01 : MAX STACK                       LeetCode #716 (Premium)
 *  https://leetcode.com/problems/max-stack/
 *  Pattern: DOUBLY LINKED LIST + TreeMap (value -> nodes)  -> O(log n) everything
 * ============================================================================
 *
 *  PROBLEM
 *    A stack with: push(x), pop(), top(), peekMax(), and popMax(). popMax removes
 *    the MAXIMUM element; if there are several, the one closest to the TOP.
 *
 *  ---------------------------------------------------------------------------
 *  WHY IT'S HARDER THAN MIN STACK (M01): peekMax is easy (store the running max,
 *  like M01). But popMax removes an element from the MIDDLE of the stack, which
 *  breaks the "everything below me is unchanged" assumption.
 *
 *  APPROACH 1: TWO STACKS (values + running max)          popMax O(n), the rest O(1)
 *    popMax: pop into a buffer until the top equals the max, drop it, push the
 *    buffer back (recomputing the running maxes). Simple, good to mention first.
 *
 *  APPROACH 2: DLL + TreeMap                               all O(log n)
 *    - a DOUBLY LINKED LIST holds the stack order (tail = top), so ANY node can
 *      be removed in O(1) (see 01-Basics of Linked Lists: DoublyLinkedList)
 *    - a TreeMap<value, list of nodes with that value> finds the maximum in O(log n).
 *      The LAST node in a value's list is the one closest to the top.
 *    push(x):  append a node to the DLL; add it to map[x]
 *    pop():    remove the DLL tail; remove it from map[its value]
 *    popMax(): max = map.lastKey(); node = the last of map[max]; unlink it from the DLL
 *    peekMax(): map.lastKey()
 *
 *  This is exactly the kind of "combine two structures" design senior interviews love:
 *  one structure for ORDER, one for FAST LOOKUP, kept in sync.
 * ============================================================================
 */

import java.util.*;

public class H01_MaxStack {

    // ---------------- Approach 2: DLL + TreeMap ----------------
    static class MaxStack {
        private static class Node {
            int val;
            Node prev, next;
            Node(int val) { this.val = val; }
        }

        private final Node head = new Node(0), tail = new Node(0);       // sentinels
        private final TreeMap<Integer, Deque<Node>> byValue = new TreeMap<>();

        MaxStack() { head.next = tail; tail.prev = head; }

        public void push(int x) {
            Node n = new Node(x);
            n.prev = tail.prev; n.next = tail;                           // insert before the tail sentinel
            tail.prev.next = n; tail.prev = n;
            byValue.computeIfAbsent(x, k -> new ArrayDeque<>()).addLast(n);
        }

        public int pop() {
            Node n = tail.prev;
            unlink(n);
            removeFromMap(n.val);                                        // n is the newest node of its value
            return n.val;
        }

        public int top() { return tail.prev.val; }

        public int peekMax() { return byValue.lastKey(); }

        public int popMax() {
            int max = byValue.lastKey();
            Node n = byValue.get(max).peekLast();                        // the one nearest the top
            removeFromMap(max);
            unlink(n);
            return max;
        }

        private void unlink(Node n) {
            n.prev.next = n.next;
            n.next.prev = n.prev;
        }

        private void removeFromMap(int val) {
            Deque<Node> nodes = byValue.get(val);
            nodes.pollLast();
            if (nodes.isEmpty()) byValue.remove(val);
        }
    }

    // ---------------- Approach 1: two stacks ----------------
    static class MaxStackSimple {
        private final Deque<Integer> values = new ArrayDeque<>(), maxes = new ArrayDeque<>();

        public void push(int x) {
            values.push(x);
            maxes.push(maxes.isEmpty() ? x : Math.max(x, maxes.peek()));
        }

        public int pop() { maxes.pop(); return values.pop(); }
        public int top() { return values.peek(); }
        public int peekMax() { return maxes.peek(); }

        public int popMax() {
            int max = peekMax();
            Deque<Integer> buffer = new ArrayDeque<>();
            while (top() != max) buffer.push(pop());                     // dig down to the max
            pop();                                                       // remove it
            while (!buffer.isEmpty()) push(buffer.pop());                // put the rest back
            return max;
        }
    }

    public static void main(String[] args) {
        MaxStack s = new MaxStack();
        s.push(5);
        s.push(1);
        s.push(5);
        check("top", 5, s.top());
        check("popMax (the TOP 5)", 5, s.popMax());
        check("top after popMax", 1, s.top());
        check("peekMax", 5, s.peekMax());
        check("pop", 1, s.pop());
        check("top", 5, s.top());

        Random rnd = new Random(16);
        MaxStack fast = new MaxStack();
        MaxStackSimple simple = new MaxStackSimple();
        List<Integer> ref = new ArrayList<>();                           // index 0 = bottom
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            int op = ref.isEmpty() ? 0 : rnd.nextInt(5);
            switch (op) {
                case 0 -> { int v = rnd.nextInt(10); fast.push(v); simple.push(v); ref.add(v); }
                case 1 -> { int r = ref.remove(ref.size() - 1); ok &= fast.pop() == r && simple.pop() == r; }
                case 2 -> ok &= fast.top() == ref.get(ref.size() - 1) && simple.top() == ref.get(ref.size() - 1);
                case 3 -> ok &= fast.peekMax() == Collections.max(ref) && simple.peekMax() == Collections.max(ref);
                default -> {
                    int max = Collections.max(ref);
                    ref.remove(ref.lastIndexOf(max));                    // the max nearest the top
                    ok &= fast.popMax() == max && simple.popMax() == max;
                }
            }
        }
        check("20,000 random ops: both versions vs a list", true, ok);
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
