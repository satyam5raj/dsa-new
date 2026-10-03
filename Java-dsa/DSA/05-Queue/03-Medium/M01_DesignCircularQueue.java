/*
 * ============================================================================
 *  MEDIUM 01 : DESIGN CIRCULAR QUEUE         LeetCode #622
 *  https://leetcode.com/problems/design-circular-queue/
 *  Pattern: FIXED-CAPACITY RING BUFFER (head + size, indices wrap with %)
 * ============================================================================
 *
 *  PROBLEM: MyCircularQueue(k) with
 *    enQueue(v) / deQueue()  -> true/false (false if full / empty)
 *    Front() / Rear()        -> the value, or -1 if empty
 *    isEmpty() / isFull()
 *
 *  ---------------------------------------------------------------------------
 *  THE CLASSIC PITFALL: with only 'head' and 'tail' indices, an EMPTY queue and a
 *  FULL queue look the same (head == tail in both cases). Fixes:
 *    (a) also track 'size'   <- used here: clearest
 *    (b) keep one slot always empty (capacity k + 1): full when (tail + 1) % cap == head
 *
 *  WITH head + size:
 *    tail index (the last element) = (head + size - 1) % k
 *    enQueue: data[(head + size) % k] = v; size++
 *    deQueue: head = (head + 1) % k;       size--
 *    Front = data[head],  Rear = data[(head + size - 1) % k]
 *
 *  DRY RUN k=3: enQueue 1,2,3 -> [1,2,3] h=0 s=3 (full); enQueue 4 -> false
 *               deQueue -> h=1 s=2;  enQueue 4 -> slot (1+2)%3 = 0 -> [4,2,3]
 *               Rear = data[(1+3-1)%3] = data[0] = 4
 *
 *  WHY RING BUFFERS MATTER: fixed memory, no allocation per element, O(1)
 *  everything. Used in network drivers, audio, logging (keep the last N lines),
 *  and the LMAX Disruptor (a famous high-throughput Java queue).
 *  COMPLEXITY   all O(1)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class M01_DesignCircularQueue {

    static class MyCircularQueue {
        private final int[] data;
        private int head = 0, size = 0;

        MyCircularQueue(int k) { data = new int[k]; }

        public boolean enQueue(int value) {
            if (isFull()) return false;
            data[(head + size) % data.length] = value;
            size++;
            return true;
        }

        public boolean deQueue() {
            if (isEmpty()) return false;
            head = (head + 1) % data.length;
            size--;
            return true;
        }

        public int Front() { return isEmpty() ? -1 : data[head]; }
        public int Rear() { return isEmpty() ? -1 : data[(head + size - 1) % data.length]; }
        public boolean isEmpty() { return size == 0; }
        public boolean isFull() { return size == data.length; }
    }

    public static void main(String[] args) {
        MyCircularQueue q = new MyCircularQueue(3);
        List<Object> out = new ArrayList<>();
        out.add(q.enQueue(1)); out.add(q.enQueue(2)); out.add(q.enQueue(3)); out.add(q.enQueue(4));
        out.add(q.Rear()); out.add(q.isFull()); out.add(q.deQueue()); out.add(q.enQueue(4)); out.add(q.Rear());
        check("LeetCode sequence", "[true, true, true, false, 3, true, true, true, 4]", out);

        Random rnd = new Random(1);
        MyCircularQueue mine = new MyCircularQueue(5);
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            int v = rnd.nextInt(100);
            if (rnd.nextBoolean()) {
                boolean can = ref.size() < 5;
                ok &= mine.enQueue(v) == can;
                if (can) ref.offer(v);
            } else {
                ok &= mine.deQueue() == !ref.isEmpty();
                ref.poll();
            }
            ok &= mine.Front() == (ref.isEmpty() ? -1 : ref.peekFirst()) && mine.Rear() == (ref.isEmpty() ? -1 : ref.peekLast());
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
