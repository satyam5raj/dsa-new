/*
 * ============================================================================
 *  MEDIUM 10 : DESIGN LINKED LIST            LeetCode #707
 *  https://leetcode.com/problems/design-linked-list/
 *  Pattern: IMPLEMENTATION WITH A SENTINEL (dummy) HEAD + SIZE COUNTER
 * ============================================================================
 *
 *  PROBLEM: implement MyLinkedList with 0-indexed operations:
 *    get(index)                 -> value, or -1 if the index is invalid
 *    addAtHead(val), addAtTail(val)
 *    addAtIndex(index, val)     -> insert BEFORE the index-th node;
 *                                  index == size appends; index > size does nothing
 *    deleteAtIndex(index)       -> delete if valid
 *
 *  ---------------------------------------------------------------------------
 *  DESIGN DECISIONS (say them out loud in an interview)
 *    1. a SENTINEL head: every operation becomes "find the node BEFORE position i,
 *       then splice". addAtHead = addAtIndex(0), addAtTail = addAtIndex(size).
 *    2. keep 'size': O(1) bounds checks, no walking to validate an index.
 *    3. singly vs doubly: doubly + a tail sentinel would let us walk from the
 *       closer end (halves the average work) at the cost of more pointer updates.
 *       Singly is shown here; see 01-Basics/DoublyLinkedList.java for the other.
 *
 *  Every operation reduces to ONE helper:  predecessor(i) = walk i steps from the sentinel.
 *  COMPLEXITY   get/add/delete at index i: O(i)   addAtHead: O(1)   addAtTail: O(n) (O(1) with a tail pointer)
 * ============================================================================
 */

import java.util.*;

public class M10_DesignLinkedList {

    static class MyLinkedList {
        private static class Node {
            int val;
            Node next;
            Node(int val, Node next) { this.val = val; this.next = next; }
        }

        private final Node sentinel = new Node(0, null);
        private int size;

        // node right BEFORE position index (index 0 -> the sentinel)
        private Node predecessor(int index) {
            Node p = sentinel;
            for (int i = 0; i < index; i++) p = p.next;
            return p;
        }

        public int get(int index) {
            if (index < 0 || index >= size) return -1;
            return predecessor(index).next.val;
        }

        public void addAtHead(int val) { addAtIndex(0, val); }

        public void addAtTail(int val) { addAtIndex(size, val); }

        public void addAtIndex(int index, int val) {
            if (index < 0 || index > size) return;      // index == size is allowed (append)
            Node p = predecessor(index);
            p.next = new Node(val, p.next);
            size++;
        }

        public void deleteAtIndex(int index) {
            if (index < 0 || index >= size) return;
            Node p = predecessor(index);
            p.next = p.next.next;
            size--;
        }
    }

    public static void main(String[] args) {
        // the LeetCode example sequence
        MyLinkedList l = new MyLinkedList();
        l.addAtHead(1);
        l.addAtTail(3);
        l.addAtIndex(1, 2);          // 1 -> 2 -> 3
        check("get(1) after inserts", 2, l.get(1));
        l.deleteAtIndex(1);          // 1 -> 3
        check("get(1) after delete", 3, l.get(1));
        check("get(5) invalid", -1, l.get(5));
        l.addAtIndex(5, 9);          // index > size: ignored
        check("addAtIndex beyond size ignored", -1, l.get(2));

        // random operations vs java.util.ArrayList
        Random rnd = new Random(5);
        MyLinkedList mine = new MyLinkedList();
        List<Integer> ref = new ArrayList<>();
        boolean ok = true;
        for (int t = 0; t < 5000; t++) {
            int idx = rnd.nextInt(ref.size() + 3) - 1, v = rnd.nextInt(100);
            switch (rnd.nextInt(5)) {
                case 0 -> { mine.addAtHead(v); ref.add(0, v); }
                case 1 -> { mine.addAtTail(v); ref.add(v); }
                case 2 -> { mine.addAtIndex(idx, v); if (idx >= 0 && idx <= ref.size()) ref.add(idx, v); }
                case 3 -> { mine.deleteAtIndex(idx); if (idx >= 0 && idx < ref.size()) ref.remove(idx); }
                default -> ok &= mine.get(idx) == (idx >= 0 && idx < ref.size() ? ref.get(idx) : -1);
            }
        }
        check("5,000 random ops vs ArrayList", true, ok);
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
