/*
 * ============================================================================
 *  QUEUE 1.1 : QUEUE FROM SCRATCH - LINKED, CIRCULAR ARRAY, AND DEQUE
 * ============================================================================
 *
 *  Run:  java QueueImplementations.java
 *
 *  QUEUE = FIFO (First In, First Out). Like a line at a ticket counter:
 *  join at the BACK (enqueue / offer), leave from the FRONT (dequeue / poll).
 *
 *     front -> [ 1 ][ 2 ][ 3 ] <- back        offer(4): [1][2][3][4]      poll() -> 1
 *
 *  WHERE QUEUES SHOW UP
 *    - BFS (breadth-first search): explore level by level, shortest paths
 *    - task scheduling, message queues (RabbitMQ, Kafka, SQS), print spoolers
 *    - buffering between a producer and a consumer (BlockingQueue, see Multithreading)
 *    - rate limiting / sliding time windows (keep the recent timestamps)
 *
 *  -----------------------------------------------------------------------------
 *   Implementation          offer       poll        notes
 *  -----------------------------------------------------------------------------
 *   linked list             O(1)        O(1)        head = front, tail = back
 *   plain array, shifting   O(1)        O(n)        BAD: every poll shifts everything
 *   CIRCULAR array          O(1)        O(1)        indices wrap around with % capacity
 *  -----------------------------------------------------------------------------
 *
 *  THE CIRCULAR ARRAY (ring buffer) - the important one
 *    Keep 'head' (front index) and 'size'. The back is (head + size) % capacity.
 *    Polling just moves head forward; slots get REUSED as the indices wrap:
 *
 *      capacity 5, after offering 1..5 and polling twice:
 *        index:  0   1   2   3   4
 *               [ . | . | 3 | 4 | 5 ]      head = 2, size = 3
 *      offer(6) goes to (2 + 3) % 5 = 0:
 *               [ 6 | . | 3 | 4 | 5 ]      the queue reads 3, 4, 5, 6
 *
 *  DEQUE ("deck") = double-ended queue: add/remove at BOTH ends in O(1).
 *  It can act as a stack AND as a queue. Java's ArrayDeque is a circular array
 *  deque, the default choice for both:
 *      Queue<Integer> q = new ArrayDeque<>();   offer / poll / peek
 *      Deque<Integer> d = new ArrayDeque<>();   offerFirst / offerLast / pollFirst / pollLast
 */

import java.util.*;

public class QueueImplementations {

    // =================================================================
    // 1. LINKED QUEUE
    // =================================================================
    static class LinkedQueue {
        private static class Node {
            int val;
            Node next;
            Node(int val) { this.val = val; }
        }

        private Node head, tail;     // poll from head, offer at tail
        private int size;

        void offer(int x) {
            Node n = new Node(x);
            if (tail == null) head = tail = n;
            else { tail.next = n; tail = n; }
            size++;
        }

        int poll() {
            if (head == null) throw new NoSuchElementException("queue is empty");
            int v = head.val;
            head = head.next;
            if (head == null) tail = null;           // the queue became empty
            size--;
            return v;
        }

        int peek() { if (head == null) throw new NoSuchElementException(); return head.val; }
        boolean isEmpty() { return size == 0; }
        int size() { return size; }
    }

    // =================================================================
    // 2. CIRCULAR ARRAY QUEUE (growing ring buffer)
    // =================================================================
    static class CircularQueue {
        private int[] data = new int[4];
        private int head = 0, size = 0;

        void offer(int x) {
            if (size == data.length) grow();
            data[(head + size) % data.length] = x;     // the back, wrapping around
            size++;
        }

        int poll() {
            if (size == 0) throw new NoSuchElementException("queue is empty");
            int v = data[head];
            head = (head + 1) % data.length;           // move the front forward, wrapping
            size--;
            return v;
        }

        int peek() { if (size == 0) throw new NoSuchElementException(); return data[head]; }
        boolean isEmpty() { return size == 0; }
        int size() { return size; }

        // Can't use Arrays.copyOf: the elements may wrap past the end. Copy them in queue order.
        private void grow() {
            int[] bigger = new int[data.length * 2];
            for (int i = 0; i < size; i++) bigger[i] = data[(head + i) % data.length];
            data = bigger;
            head = 0;
        }

        String layout() { return Arrays.toString(data) + " head=" + head + " size=" + size; }
    }

    // =================================================================
    // 3. CIRCULAR ARRAY DEQUE (what ArrayDeque does inside)
    //    front index moves LEFT on offerFirst:  head = (head - 1 + capacity) % capacity
    //    (+ capacity so the result never goes negative)
    // =================================================================
    static class CircularDeque {
        private int[] data = new int[4];
        private int head = 0, size = 0;

        void offerFirst(int x) {
            if (size == data.length) grow();
            head = (head - 1 + data.length) % data.length;
            data[head] = x;
            size++;
        }

        void offerLast(int x) {
            if (size == data.length) grow();
            data[(head + size) % data.length] = x;
            size++;
        }

        int pollFirst() {
            if (size == 0) throw new NoSuchElementException();
            int v = data[head];
            head = (head + 1) % data.length;
            size--;
            return v;
        }

        int pollLast() {
            if (size == 0) throw new NoSuchElementException();
            int idx = (head + size - 1) % data.length;
            size--;
            return data[idx];
        }

        int size() { return size; }

        private void grow() {
            int[] bigger = new int[data.length * 2];
            for (int i = 0; i < size; i++) bigger[i] = data[(head + i) % data.length];
            data = bigger;
            head = 0;
        }
    }

    public static void main(String[] args) {
        System.out.println("===== CIRCULAR QUEUE: watch the indices wrap =====");
        CircularQueue cq = new CircularQueue();
        for (int i = 1; i <= 4; i++) cq.offer(i);
        System.out.println("   offer 1..4         : " + cq.layout());
        System.out.println("   poll -> " + cq.poll() + ", poll -> " + cq.poll() + " : " + cq.layout());
        cq.offer(5);
        cq.offer(6);
        System.out.println("   offer 5, 6 (wrap!) : " + cq.layout() + "   <- 5,6 reused slots 0,1");
        cq.offer(7);
        System.out.println("   offer 7 (grow)     : " + cq.layout() + "   <- unwrapped into order");

        System.out.println("\n===== LINKED QUEUE =====");
        LinkedQueue lq = new LinkedQueue();
        lq.offer(10);
        lq.offer(20);
        lq.offer(30);
        System.out.print("   polling: ");
        while (!lq.isEmpty()) System.out.print(lq.poll() + " ");
        System.out.println("  <- same order as offered (FIFO)");

        System.out.println("\n===== DEQUE: both ends =====");
        CircularDeque dq = new CircularDeque();
        dq.offerLast(2);
        dq.offerFirst(1);
        dq.offerLast(3);
        dq.offerFirst(0);
        System.out.println("   offerLast 2, offerFirst 1, offerLast 3, offerFirst 0 -> pollFirst " + dq.pollFirst() + ", pollLast " + dq.pollLast());

        // self-checks vs ArrayDeque
        Random rnd = new Random(5);
        LinkedQueue a = new LinkedQueue();
        CircularQueue b = new CircularQueue();
        CircularDeque c = new CircularDeque();
        ArrayDeque<Integer> refQ = new ArrayDeque<>(), refD = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            int v = rnd.nextInt(1000);
            if (refQ.isEmpty() || rnd.nextInt(3) > 0) { a.offer(v); b.offer(v); refQ.offer(v); }
            else { int r = refQ.poll(); ok &= a.poll() == r && b.poll() == r; }
            switch (refD.isEmpty() ? rnd.nextInt(2) : rnd.nextInt(4)) {
                case 0 -> { c.offerFirst(v); refD.offerFirst(v); }
                case 1 -> { c.offerLast(v); refD.offerLast(v); }
                case 2 -> ok &= c.pollFirst() == refD.pollFirst();
                default -> ok &= c.pollLast() == refD.pollLast();
            }
            ok &= a.size() == refQ.size() && b.size() == refQ.size() && c.size() == refD.size();
        }
        System.out.println("\n===== 20,000 random ops: all three match ArrayDeque: " + ok + " =====");
        if (!ok) System.exit(1);
    }
}
