/*
 * ============================================================================
 *  MEDIUM 02 : DESIGN CIRCULAR DEQUE         LeetCode #641
 *  https://leetcode.com/problems/design-circular-deque/
 *  Pattern: RING BUFFER WITH BOTH ENDS ((i - 1 + k) % k to step left)
 * ============================================================================
 *
 *  PROBLEM: MyCircularDeque(k) with insertFront, insertLast, deleteFront,
 *  deleteLast (each returns true/false), getFront, getRear (-1 if empty),
 *  isEmpty, isFull.
 *
 *  ---------------------------------------------------------------------------
 *  M01 plus the ability to grow/shrink at the FRONT:
 *    insertFront: head = (head - 1 + k) % k; data[head] = v; size++
 *    deleteLast:  size--   (the last element is (head + size - 1) % k)
 *
 *  THE "+ k" DETAIL: in Java, (-1) % 5 == -1 (not 4!). The % of a negative
 *  number stays negative. Adding k before the % keeps the index in [0, k).
 *  (Math.floorMod(head - 1, k) does the same thing.)
 *
 *  DRY RUN k=3
 *    insertLast 1  -> [1,_,_] h=0 s=1
 *    insertLast 2  -> [1,2,_] h=0 s=2
 *    insertFront 3 -> head = (0-1+3)%3 = 2 -> [1,2,3] h=2 s=3: the order is 3,1,2
 *    getRear = data[(2+3-1)%3] = data[1] = 2
 *
 *  This is exactly how java.util.ArrayDeque works (it also grows when full).
 *  COMPLEXITY   all O(1)
 * ============================================================================
 */

import java.util.*;

public class M02_DesignCircularDeque {

    static class MyCircularDeque {
        private final int[] data;
        private int head = 0, size = 0;

        MyCircularDeque(int k) { data = new int[k]; }

        public boolean insertFront(int value) {
            if (isFull()) return false;
            head = (head - 1 + data.length) % data.length;     // step left, wrapping
            data[head] = value;
            size++;
            return true;
        }

        public boolean insertLast(int value) {
            if (isFull()) return false;
            data[(head + size) % data.length] = value;
            size++;
            return true;
        }

        public boolean deleteFront() {
            if (isEmpty()) return false;
            head = (head + 1) % data.length;
            size--;
            return true;
        }

        public boolean deleteLast() {
            if (isEmpty()) return false;
            size--;
            return true;
        }

        public int getFront() { return isEmpty() ? -1 : data[head]; }
        public int getRear() { return isEmpty() ? -1 : data[(head + size - 1) % data.length]; }
        public boolean isEmpty() { return size == 0; }
        public boolean isFull() { return size == data.length; }
    }

    public static void main(String[] args) {
        MyCircularDeque d = new MyCircularDeque(3);
        List<Object> out = new ArrayList<>();
        out.add(d.insertLast(1)); out.add(d.insertLast(2)); out.add(d.insertFront(3)); out.add(d.insertFront(4));
        out.add(d.getRear()); out.add(d.isFull()); out.add(d.deleteLast()); out.add(d.insertFront(4)); out.add(d.getFront());
        check("LeetCode sequence", "[true, true, true, false, 2, true, true, true, 4]", out);
        check("Java: -1 % 5", -1, -1 % 5);
        check("Java: Math.floorMod(-1, 5)", 4, Math.floorMod(-1, 5));

        Random rnd = new Random(2);
        MyCircularDeque mine = new MyCircularDeque(4);
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            int v = rnd.nextInt(100);
            switch (rnd.nextInt(4)) {
                case 0 -> { boolean can = ref.size() < 4; ok &= mine.insertFront(v) == can; if (can) ref.offerFirst(v); }
                case 1 -> { boolean can = ref.size() < 4; ok &= mine.insertLast(v) == can; if (can) ref.offerLast(v); }
                case 2 -> { ok &= mine.deleteFront() == !ref.isEmpty(); ref.pollFirst(); }
                default -> { ok &= mine.deleteLast() == !ref.isEmpty(); ref.pollLast(); }
            }
            ok &= mine.getFront() == (ref.isEmpty() ? -1 : ref.peekFirst()) && mine.getRear() == (ref.isEmpty() ? -1 : ref.peekLast());
        }
        check("20,000 random ops vs bounded ArrayDeque", true, ok);
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
