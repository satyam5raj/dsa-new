/*
 * ============================================================================
 *  STACK 1.1 : WHAT A STACK IS + TWO IMPLEMENTATIONS FROM SCRATCH
 * ============================================================================
 *
 *  Run:  java StackImplementations.java
 *
 *  STACK = LIFO (Last In, First Out). Like a stack of plates: you add (PUSH) on
 *  top and remove (POP) from the top. You can only see the TOP.
 *
 *        push(3)            pop() -> 3
 *         |  3 |  <- top
 *         |  2 |              |  2 |  <- top
 *         |  1 |              |  1 |
 *         +----+              +----+
 *
 *  OPERATIONS (all O(1)): push, pop, peek (look at the top), isEmpty, size
 *
 *  WHERE STACKS SHOW UP
 *    - the CALL STACK: every method call pushes a frame; return pops it
 *      (that's why deep recursion -> StackOverflowError)
 *    - undo/redo, browser back button
 *    - matching brackets, parsing and evaluating expressions (compilers!)
 *    - DFS (depth-first search): recursion IS a stack, and you can replace it with one
 *    - MONOTONIC STACK: next greater/smaller element problems
 *
 *  TWO WAYS TO BUILD ONE
 *    1. ARRAY-BACKED : top = index of the last element. Doubles when full.
 *                      Cache-friendly, the fastest in practice.
 *    2. LINKED       : push = addFirst on a singly linked list, pop = removeFirst.
 *                      Never resizes; one node allocated per element.
 *
 *  IN REAL JAVA CODE: use  Deque<Integer> stack = new ArrayDeque<>();
 *      push / pop / peek / isEmpty
 *  NOT java.util.Stack: it extends Vector (every method synchronized = slow), and
 *  it inherits methods like add(index, x) that break LIFO (see OOP topic 03).
 */

import java.util.*;

public class StackImplementations {

    // =================================================================
    // 1. ARRAY-BACKED STACK
    // =================================================================
    static class ArrayStack {
        private int[] data = new int[4];
        private int size;                     // also "index where the next push goes"

        void push(int x) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);   // grow: amortized O(1)
            data[size++] = x;
        }

        int pop() {
            if (size == 0) throw new RuntimeException("stack underflow (pop on empty stack)");
            return data[--size];              // just move the top down; the slot gets overwritten later
        }

        int peek() {
            if (size == 0) throw new RuntimeException("empty stack");
            return data[size - 1];
        }

        boolean isEmpty() { return size == 0; }
        int size() { return size; }

        @Override
        public String toString() { return Arrays.toString(Arrays.copyOf(data, size)) + " <- top"; }
    }

    // =================================================================
    // 2. LINKED STACK (singly linked list, top = head)
    // =================================================================
    static class LinkedStack {
        private static class Node {
            int val;
            Node next;
            Node(int val, Node next) { this.val = val; this.next = next; }
        }

        private Node top;
        private int size;

        void push(int x) {
            top = new Node(x, top);           // new node points at the old top
            size++;
        }

        int pop() {
            if (top == null) throw new RuntimeException("stack underflow");
            int v = top.val;
            top = top.next;
            size--;
            return v;
        }

        int peek() {
            if (top == null) throw new RuntimeException("empty stack");
            return top.val;
        }

        boolean isEmpty() { return top == null; }
        int size() { return size; }
    }

    public static void main(String[] args) {
        System.out.println("===== ARRAY STACK =====");
        ArrayStack a = new ArrayStack();
        for (int x : new int[]{10, 20, 30, 40, 50}) a.push(x);          // triggers a resize at 5
        System.out.println("   after pushing 10..50 : " + a);
        System.out.println("   peek = " + a.peek() + ", pop = " + a.pop() + ", pop = " + a.pop());
        System.out.println("   now                  : " + a);

        System.out.println("\n===== LINKED STACK =====");
        LinkedStack l = new LinkedStack();
        l.push(1);
        l.push(2);
        l.push(3);
        System.out.print("   popping all: ");
        while (!l.isEmpty()) System.out.print(l.pop() + " ");
        System.out.println("  <- reverse order of pushing (LIFO)");
        try {
            l.pop();
        } catch (RuntimeException e) {
            System.out.println("   pop on empty -> " + e.getMessage());
        }

        System.out.println("\n===== THE JAVA WAY: ArrayDeque =====");
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        stack.push(2);
        System.out.println("   peek=" + stack.peek() + " pop=" + stack.pop() + " isEmpty=" + stack.isEmpty());
        System.out.println("   (poll()/peek() return null when empty, pop() throws. Know which one you call!)");

        // self-check: both implementations vs ArrayDeque
        Random rnd = new Random(1);
        ArrayStack as = new ArrayStack();
        LinkedStack ls = new LinkedStack();
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 10_000; i++) {
            if (ref.isEmpty() || rnd.nextInt(3) > 0) {
                int v = rnd.nextInt(1000);
                as.push(v); ls.push(v); ref.push(v);
            } else {
                int r = ref.pop();
                ok &= as.pop() == r && ls.pop() == r;
            }
            ok &= as.size() == ref.size() && ls.size() == ref.size();
        }
        System.out.println("\n===== 10,000 random ops: both stacks match ArrayDeque: " + ok + " =====");
        if (!ok) System.exit(1);
    }
}
