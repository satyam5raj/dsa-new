/*
 * ============================================================================
 *  HEAP DEEP DIVE: generic heap, O(n) heapify, in-place heap sort, indexed heap
 * ============================================================================
 *
 *  (05-Queue/01-Basics/PriorityQueueFromScratch built a basic int min-heap.
 *   Re-read its header if "sift up / sift down" feels fuzzy. This file goes further.)
 *
 *  RECAP
 *    HEAP PROPERTY: every parent is <= its children (MIN-heap) or >= (MAX-heap).
 *    So the root is the min (or max). Siblings are NOT ordered: a heap is only
 *    "partially sorted", which is exactly why it's cheaper than keeping things sorted.
 *    Stored as a COMPLETE tree in an array: parent (i-1)/2, children 2i+1 and 2i+2.
 *
 *      operation        cost        how
 *      peek             O(1)        a[0]
 *      offer            O(log n)    append, sift UP
 *      poll             O(log n)    move last to root, sift DOWN
 *      heapify n items  O(n)  !!    sift down every non-leaf, bottom-up
 *      remove(x)        O(n)        must FIND x first (O(log n) with an index map: part 4)
 *
 *  ---------------------------------------------------------------------------
 *  1. ONE HEAP, ANY ORDER: take a Comparator. min-heap = natural order,
 *     max-heap = reversed. Same code (that's how java.util.PriorityQueue works).
 *
 *  2. WHY HEAPIFY IS O(n), NOT O(n log n)
 *     Half the nodes are leaves: they sift down 0 levels. A quarter sift down at
 *     most 1 level, an eighth at most 2... total work <= n/4*1 + n/8*2 + n/16*3 + ... = n.
 *     The few nodes that can travel far (near the root) are rare. Measured below.
 *     (Inserting one by one IS O(n log n): many nodes are deep and may sift up far.)
 *
 *  3. HEAP SORT, IN PLACE, O(n log n), O(1) extra space
 *     a) heapify the array into a MAX-heap
 *     b) repeat: swap the root (the max) with the last element of the heap,
 *        shrink the heap by one, sift the new root down.
 *     The sorted part grows from the back. Not stable, and slower than quicksort in
 *     practice (jumps around memory), but guaranteed O(n log n) with O(1) space.
 *     (Java's Arrays.sort uses dual-pivot quicksort for ints, TimSort for objects.)
 *
 *  4. INDEXED HEAP (decrease-key): Dijkstra and Prim need "this item's priority
 *     just got better, move it up". Keep a map item -> position in the array; every
 *     swap updates it. Then update/remove of ANY item is O(log n).
 *     (With java.util.PriorityQueue people instead push a duplicate and skip stale
 *      entries when polled: "lazy deletion", see HeapPatterns.)
 *
 *  Run:  java HeapDeepDive.java
 * ============================================================================
 */

import java.util.*;

public class HeapDeepDive {

    // ---------------- 1. Generic binary heap with a comparator ----------------
    static class BinaryHeap<T> {
        private Object[] a = new Object[16];
        private int size;
        private final Comparator<? super T> cmp;
        long swaps;                                      // for the heapify experiment

        BinaryHeap(Comparator<? super T> cmp) { this.cmp = cmp; }

        // Build from a collection in O(n): copy everything, then sift down the non-leaves.
        BinaryHeap(Collection<? extends T> items, Comparator<? super T> cmp) {
            this.cmp = cmp;
            a = items.toArray(new Object[Math.max(16, items.size())]);
            size = items.size();
            for (int i = size / 2 - 1; i >= 0; i--) siftDown(i);     // last non-leaf is size/2 - 1
        }

        @SuppressWarnings("unchecked")
        private T at(int i) { return (T) a[i]; }

        private boolean less(int i, int j) { return cmp.compare(at(i), at(j)) < 0; }

        private void swap(int i, int j) { Object t = a[i]; a[i] = a[j]; a[j] = t; swaps++; }

        void offer(T x) {
            if (size == a.length) a = Arrays.copyOf(a, size * 2);
            a[size] = x;
            siftUp(size++);
        }

        T peek() { return size == 0 ? null : at(0); }

        T poll() {
            if (size == 0) return null;
            T top = at(0);
            a[0] = a[--size];
            a[size] = null;                              // let the GC reclaim it
            if (size > 0) siftDown(0);
            return top;
        }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (!less(i, p)) break;                  // parent already comes first: done
                swap(i, p);
                i = p;
            }
        }

        private void siftDown(int i) {
            while (true) {
                int l = 2 * i + 1, r = l + 1, best = i;
                if (l < size && less(l, best)) best = l;
                if (r < size && less(r, best)) best = r;  // pick the child that should come first
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }

        int size() { return size; }

        boolean isValid() {
            for (int i = 1; i < size; i++) if (less(i, (i - 1) / 2)) return false;
            return true;
        }
    }

    // ---------------- 3. In-place heap sort (ascending, using a MAX-heap) ----------------
    static void heapSort(int[] a) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDownMax(a, i, n);   // build a max-heap
        for (int end = n - 1; end > 0; end--) {
            int t = a[0]; a[0] = a[end]; a[end] = t;                 // move the max to the back
            siftDownMax(a, 0, end);                                  // heap is now a[0..end-1]
        }
    }

    static void siftDownMax(int[] a, int i, int n) {
        while (true) {
            int l = 2 * i + 1, r = l + 1, big = i;
            if (l < n && a[l] > a[big]) big = l;
            if (r < n && a[r] > a[big]) big = r;
            if (big == i) return;
            int t = a[i]; a[i] = a[big]; a[big] = t;
            i = big;
        }
    }

    // ---------------- 4. Indexed min-heap: keys 0..n-1 with changeable priorities ----------------
    static class IndexedMinHeap {
        private final int[] heap;      // heap[pos] = key
        private final int[] pos;       // pos[key]  = position in heap, or -1 if absent
        private final long[] prio;     // prio[key]
        private int size;

        IndexedMinHeap(int maxKeys) {
            heap = new int[maxKeys];
            pos = new int[maxKeys];
            prio = new long[maxKeys];
            Arrays.fill(pos, -1);
        }

        boolean contains(int key) { return pos[key] != -1; }

        void insert(int key, long priority) {
            prio[key] = priority;
            heap[size] = key;
            pos[key] = size;
            siftUp(size++);
        }

        // Dijkstra's "relax": the priority can only get smaller -> sift up
        void decreaseKey(int key, long priority) {
            prio[key] = priority;
            siftUp(pos[key]);
        }

        // remove ANY key in O(log n): swap it with the last, then fix in whichever direction
        void remove(int key) {
            int i = pos[key];
            swap(i, --size);
            pos[key] = -1;
            if (i < size) { siftUp(i); siftDown(i); }
        }

        int pollKey() {
            int key = heap[0];
            remove(key);
            return key;
        }

        long priority(int key) { return prio[key]; }
        boolean isEmpty() { return size == 0; }

        private void swap(int i, int j) {
            int t = heap[i]; heap[i] = heap[j]; heap[j] = t;
            pos[heap[i]] = i;                 // keep the position map in sync on EVERY swap
            pos[heap[j]] = j;
        }

        private boolean less(int i, int j) {
            return prio[heap[i]] < prio[heap[j]] || (prio[heap[i]] == prio[heap[j]] && heap[i] < heap[j]);
        }

        private void siftUp(int i) {
            while (i > 0 && less(i, (i - 1) / 2)) { swap(i, (i - 1) / 2); i = (i - 1) / 2; }
        }

        private void siftDown(int i) {
            while (true) {
                int l = 2 * i + 1, r = l + 1, best = i;
                if (l < size && less(l, best)) best = l;
                if (r < size && less(r, best)) best = r;
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("1. One heap class, min or max via a Comparator");
        BinaryHeap<Integer> min = new BinaryHeap<>(Comparator.naturalOrder());
        BinaryHeap<Integer> max = new BinaryHeap<>(Comparator.reverseOrder());
        for (int x : new int[]{5, 1, 8, 3, 9, 2}) { min.offer(x); max.offer(x); }
        StringBuilder sMin = new StringBuilder(), sMax = new StringBuilder();
        while (min.size() > 0) sMin.append(min.poll()).append(' ');
        while (max.size() > 0) sMax.append(max.poll()).append(' ');
        check("min-heap polls", "1 2 3 5 8 9", sMin.toString().trim());
        check("max-heap polls", "9 8 5 3 2 1", sMax.toString().trim());
        BinaryHeap<String> byLength = new BinaryHeap<>(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        for (String s : List.of("banana", "fig", "apple", "kiwi")) byLength.offer(s);
        check("strings by length", "fig", byLength.poll());

        System.out.println("2. Heapify is O(n): count the swaps");
        Random rnd = new Random(1);
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            List<Integer> items = new ArrayList<>();
            for (int i = 0; i < n; i++) items.add(i);                   // ascending input...
            Collections.reverse(items);                                // ...reversed = worst case for a min-heap
            BinaryHeap<Integer> bulk = new BinaryHeap<>(items, Comparator.naturalOrder());
            BinaryHeap<Integer> oneByOne = new BinaryHeap<>(Comparator.naturalOrder());
            for (int x : items) oneByOne.offer(x);
            System.out.printf("   n = %,9d   heapify swaps = %,10d (%.2f per item)   one-by-one swaps = %,11d (%.1f per item)%n",
                    n, bulk.swaps, bulk.swaps / (double) n, oneByOne.swaps, oneByOne.swaps / (double) n);
            if (n == 1_000_000) {
                check("heapify < 2 swaps per item", true, bulk.swaps < 2L * n);
                check("heap property holds", true, bulk.isValid());
            }
        }

        System.out.println("3. In-place heap sort");
        int[] arr = {5, 2, 9, 1, 5, 6, -3};
        heapSort(arr);
        check("heapSort", "[-3, 1, 2, 5, 5, 6, 9]", arr);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] a = rnd.ints(rnd.nextInt(200), -50, 50).toArray(), b = a.clone();
            heapSort(a);
            Arrays.sort(b);
            ok &= Arrays.equals(a, b);
        }
        check("300 random arrays == Arrays.sort", true, ok);

        System.out.println("4. Indexed heap: decrease-key and remove-any in O(log n)");
        IndexedMinHeap ih = new IndexedMinHeap(5);
        ih.insert(0, 50); ih.insert(1, 40); ih.insert(2, 30); ih.insert(3, 20); ih.insert(4, 10);
        ih.decreaseKey(0, 5);          // key 0 jumps to the front
        ih.remove(4);                  // key 4 leaves from the middle
        StringBuilder order = new StringBuilder();
        while (!ih.isEmpty()) order.append(ih.pollKey()).append(' ');
        check("poll order after updates", "0 3 2 1", order.toString().trim());
        // random ops vs a TreeSet of (priority, key)
        IndexedMinHeap h = new IndexedMinHeap(500);
        TreeSet<long[]> ref = new TreeSet<>((x, y) -> x[0] != y[0] ? Long.compare(x[0], y[0]) : Long.compare(x[1], y[1]));
        ok = true;
        for (int i = 0; i < 100_000; i++) {
            int key = rnd.nextInt(500);
            int op = rnd.nextInt(4);
            if (!h.contains(key)) {
                long p = rnd.nextInt(10_000);
                h.insert(key, p); ref.add(new long[]{p, key});
            } else if (op == 0) {
                ref.remove(new long[]{h.priority(key), key});
                h.remove(key);
            } else if (op == 1) {
                long p = h.priority(key) - rnd.nextInt(100);
                ref.remove(new long[]{h.priority(key), key});
                h.decreaseKey(key, p); ref.add(new long[]{p, key});
            } else if (!ref.isEmpty()) {
                ok &= h.pollKey() == ref.pollFirst()[1];
            }
        }
        check("100,000 random ops vs TreeSet", true, ok);
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
