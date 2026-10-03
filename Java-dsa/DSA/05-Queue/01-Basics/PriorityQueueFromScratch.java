/*
 * ============================================================================
 *  QUEUE 1.2 : PRIORITY QUEUE (via a BINARY HEAP) FROM SCRATCH
 * ============================================================================
 *
 *  Run:  java PriorityQueueFromScratch.java
 *
 *  A PRIORITY QUEUE serves the element with the highest PRIORITY first, not the
 *  oldest one. (A min-priority queue serves the SMALLEST value first.)
 *    hospital ER triage, Dijkstra's shortest path, task schedulers, "top K" problems,
 *    merging k sorted lists (Linked Lists H02), event simulation
 *
 *  HOW TO BUILD ONE?
 *    unsorted array : insert O(1), remove-min O(n) (scan)
 *    sorted array   : insert O(n) (shift), remove-min O(1)
 *    BINARY HEAP    : insert O(log n), remove-min O(log n), peek O(1)   <- best balance
 *
 *  BINARY (MIN-)HEAP
 *    a COMPLETE binary tree (every level full except possibly the last, which
 *    fills left to right) where every parent <= its children. So the root is the MINIMUM.
 *    Completeness means it fits PERFECTLY in an array, with no pointers needed:
 *
 *                 1                index:   0  1  2  3  4  5
 *              /     \            array: [ 1, 3, 2, 7, 4, 5 ]
 *             3       2
 *            / \     /             parent(i) = (i - 1) / 2
 *           7   4   5              left(i)   = 2i + 1
 *                                  right(i)  = 2i + 2
 *
 *    OFFER(x): put x at the END (keeps the tree complete), then SIFT UP: swap with
 *              the parent while it's smaller than the parent. O(log n), the tree height.
 *    POLL():   the root is the answer. Move the LAST element to the root, then SIFT
 *              DOWN: swap with the SMALLER child while it's bigger than a child. O(log n)
 *    HEAPIFY (build from n elements): sift down every non-leaf from the bottom up -> O(n)!
 *
 *  This is a preview. The HEAP topic (08) goes deeper (heap sort, top-K, two-heap median).
 *  In Java: PriorityQueue<Integer> pq = new PriorityQueue<>();          (min-heap)
 *           new PriorityQueue<>(Comparator.reverseOrder());              (max-heap)
 */

import java.util.*;

public class PriorityQueueFromScratch {

    static class MinHeap {
        private int[] heap = new int[8];
        private int size;

        void offer(int x) {
            if (size == heap.length) heap = Arrays.copyOf(heap, size * 2);
            heap[size] = x;                      // 1. add at the end
            siftUp(size++);                      // 2. bubble it up to its place
        }

        int poll() {
            if (size == 0) throw new NoSuchElementException("empty heap");
            int min = heap[0];
            heap[0] = heap[--size];              // 1. move the last element to the root
            siftDown(0);                         // 2. push it down to its place
            return min;
        }

        int peek() { return heap[0]; }
        int size() { return size; }

        private void siftUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (heap[i] >= heap[parent]) break;     // heap property holds: stop
                swap(i, parent);
                i = parent;
            }
        }

        private void siftDown(int i) {
            while (true) {
                int left = 2 * i + 1, right = left + 1, smallest = i;
                if (left < size && heap[left] < heap[smallest]) smallest = left;
                if (right < size && heap[right] < heap[smallest]) smallest = right;
                if (smallest == i) break;                // both children are bigger: done
                swap(i, smallest);
                i = smallest;
            }
        }

        // O(n) build: sift down every internal node, from the last parent up to the root
        static MinHeap heapify(int[] values) {
            MinHeap h = new MinHeap();
            h.heap = Arrays.copyOf(values, Math.max(8, values.length));
            h.size = values.length;
            for (int i = values.length / 2 - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        private void swap(int a, int b) { int t = heap[a]; heap[a] = heap[b]; heap[b] = t; }

        String array() { return Arrays.toString(Arrays.copyOf(heap, size)); }
    }

    public static void main(String[] args) {
        System.out.println("===== OFFER (sift up) =====");
        MinHeap h = new MinHeap();
        for (int x : new int[]{5, 3, 8, 1, 9, 2}) {
            h.offer(x);
            System.out.printf("   offer %d -> heap array %s  (min = %d)%n", x, h.array(), h.peek());
        }

        System.out.println("\n===== POLL (sift down) =====");
        StringBuilder order = new StringBuilder();
        while (h.size() > 0) order.append(h.poll()).append(' ');
        System.out.println("   poll order: " + order + " <- sorted! (that's heap sort)");

        System.out.println("\n===== HEAPIFY in O(n) =====");
        MinHeap built = MinHeap.heapify(new int[]{9, 4, 7, 1, 8, 2, 6});
        System.out.println("   heapify [9,4,7,1,8,2,6] -> " + built.array() + " (a valid heap, root = " + built.peek() + ")");

        System.out.println("\n===== PRIORITY QUEUE OF OBJECTS (java.util.PriorityQueue) =====");
        record Patient(String name, int severity) {}
        PriorityQueue<Patient> er = new PriorityQueue<>(Comparator.comparingInt(Patient::severity).reversed());
        er.offer(new Patient("sprained ankle", 2));
        er.offer(new Patient("heart attack", 10));
        er.offer(new Patient("broken arm", 5));
        System.out.print("   ER treats: ");
        while (!er.isEmpty()) System.out.print(er.poll().name() + " | ");
        System.out.println();

        // self-check vs java.util.PriorityQueue
        Random rnd = new Random(3);
        MinHeap mine = new MinHeap();
        PriorityQueue<Integer> ref = new PriorityQueue<>();
        boolean ok = true;
        for (int i = 0; i < 20_000; i++) {
            if (ref.isEmpty() || rnd.nextBoolean()) { int v = rnd.nextInt(100); mine.offer(v); ref.offer(v); }
            else ok &= mine.poll() == ref.poll();
        }
        int[] arr = rnd.ints(500, 0, 100).toArray();
        MinHeap hz = MinHeap.heapify(arr);
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        for (int x : sorted) ok &= hz.poll() == x;
        System.out.println("\n===== 20,000 random ops + heapify check vs java.util.PriorityQueue: " + ok + " =====");
        if (!ok) System.exit(1);
    }
}
