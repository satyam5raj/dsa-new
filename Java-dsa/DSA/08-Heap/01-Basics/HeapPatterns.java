/*
 * ============================================================================
 *  THE 6 HEAP PATTERNS (recognize the problem -> pick the pattern)
 * ============================================================================
 *
 *  1. TOP-K with a size-k heap (the OPPOSITE kind!)
 *     "k largest" -> keep a MIN-heap of size k. Each new element pushes in; if size
 *     exceeds k, poll the smallest. What's left = the k largest; the root = the k-th largest.
 *     O(n log k) time, O(k) space. Works on a stream (you never need all n at once).
 *     Why the opposite kind? The root must be the element easiest to throw out.
 *     -> Kth Largest (M01), K Closest Points (M02), Kth Largest in a Stream (E01)
 *
 *  2. K-WAY MERGE: k sorted lists -> put each list's head in a min-heap; poll the
 *     smallest, push its successor from the same list. O(N log k).
 *     -> Merge k Sorted Lists (Linked Lists H02), Kth Smallest in Sorted Matrix (M06),
 *        K Pairs with Smallest Sums (M10), Smallest Range (H02), Ugly Numbers (M08/M09)
 *
 *  3. TWO HEAPS: a MAX-heap for the lower half and a MIN-heap for the upper half.
 *     Their roots are the two middle elements -> the median in O(1).
 *     -> Find Median from Data Stream (H01), Sliding Window Median (H04)
 *
 *  4. SORT + HEAP (greedy over time): sort events by one key (start time, capital,
 *     deadline...), sweep, and keep the "currently available / currently chosen"
 *     items in a heap ordered by ANOTHER key.
 *     -> Meeting Rooms II (M07), IPO (H03), Course Schedule III (H08),
 *        Max Performance (H07), Hire K Workers (H05), Furthest Building (M05)
 *
 *  5. GREEDY "most frequent first": a max-heap of counts; always take the most
 *     frequent remaining item (with a cooldown queue if needed).
 *     -> Task Scheduler (M03), Reorganize String (M04)
 *
 *  6. LAZY DELETION: PriorityQueue.remove(x) is O(n). Instead, record "x is dead"
 *     (a counter map) and discard dead elements only when they reach the TOP.
 *     -> Sliding Window Median (H04), The Skyline Problem (H06)
 *
 *  This file shows each pattern on a tiny example.
 *  Run:  java HeapPatterns.java
 * ============================================================================
 */

import java.util.*;

public class HeapPatterns {

    // 1. TOP-K: the k largest values, using a MIN-heap of size k
    static List<Integer> topK(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int x : nums) {
            heap.offer(x);
            if (heap.size() > k) heap.poll();          // drop the smallest of the k+1
        }
        List<Integer> out = new ArrayList<>(heap);
        Collections.sort(out, Collections.reverseOrder());
        return out;
    }

    // 2. K-WAY MERGE of sorted arrays. Heap entries: {value, which array, index in it}
    static List<Integer> mergeK(int[][] lists) {
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
        for (int i = 0; i < lists.length; i++) if (lists[i].length > 0) heap.add(new int[]{lists[i][0], i, 0});
        List<Integer> out = new ArrayList<>();
        while (!heap.isEmpty()) {
            int[] e = heap.poll();
            out.add(e[0]);
            int next = e[2] + 1;
            if (next < lists[e[1]].length) heap.add(new int[]{lists[e[1]][next], e[1], next});   // its successor
        }
        return out;
    }

    // 3. TWO HEAPS: running median
    static List<Double> runningMedian(int[] stream) {
        PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());   // max-heap: lower half
        PriorityQueue<Integer> high = new PriorityQueue<>();                           // min-heap: upper half
        List<Double> out = new ArrayList<>();
        for (int x : stream) {
            low.offer(x);
            high.offer(low.poll());                 // move the largest of the low half up
            if (high.size() > low.size()) low.offer(high.poll());   // keep low.size() >= high.size()
            out.add(low.size() > high.size() ? low.peek() : (low.peek() + high.peek()) / 2.0);
        }
        return out;
    }

    // 4. SORT + HEAP: max number of overlapping intervals (= rooms needed)
    static int maxOverlap(int[][] intervals) {
        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(iv -> iv[0]));      // by start time
        PriorityQueue<Integer> ends = new PriorityQueue<>();            // end times of the active ones
        int best = 0;
        for (int[] iv : sorted) {
            while (!ends.isEmpty() && ends.peek() <= iv[0]) ends.poll(); // finished before this starts
            ends.offer(iv[1]);
            best = Math.max(best, ends.size());
        }
        return best;
    }

    // 5. MOST FREQUENT FIRST: rearrange so no two equal letters are adjacent
    static String noAdjacent(String s) {
        Map<Character, Integer> count = new HashMap<>();
        for (char c : s.toCharArray()) count.merge(c, 1, Integer::sum);
        // Ordered by the CURRENT count. Safe only because a letter's count changes while
        // it is OUT of the heap (polled or held) - see TRAP 3 in PriorityQueueInJava.
        PriorityQueue<Character> heap = new PriorityQueue<>((a, b) -> count.get(a).equals(count.get(b))
                ? Character.compare(a, b) : Integer.compare(count.get(b), count.get(a)));
        heap.addAll(count.keySet());
        StringBuilder sb = new StringBuilder();
        Character held = null;                        // the letter we just used: not allowed next
        while (!heap.isEmpty()) {
            char c = heap.poll();
            sb.append(c);
            count.merge(c, -1, Integer::sum);
            if (held != null && count.get(held) > 0) heap.offer(held);
            held = c;
        }
        return sb.length() == s.length() ? sb.toString() : "";
    }

    // 6. LAZY DELETION: a min-heap that supports remove(x) cheaply
    static class LazyMinHeap {
        final PriorityQueue<Integer> heap = new PriorityQueue<>();
        final Map<Integer, Integer> dead = new HashMap<>();
        int size;

        void add(int x) { heap.add(x); size++; }
        void remove(int x) { dead.merge(x, 1, Integer::sum); size--; }   // O(1): just remember it
        int min() { prune(); return heap.peek(); }

        private void prune() {                        // discard dead values that reached the top
            while (!heap.isEmpty() && dead.getOrDefault(heap.peek(), 0) > 0) {
                dead.merge(heap.poll(), -1, Integer::sum);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("1. Top-K");
        check("3 largest of [3,2,1,5,6,4]", "[6, 5, 4]", topK(new int[]{3, 2, 1, 5, 6, 4}, 3));

        System.out.println("2. K-way merge");
        check("merge 3 sorted arrays", "[1, 1, 2, 3, 4, 4, 5, 6]", mergeK(new int[][]{{1, 4, 5}, {1, 3, 4}, {2, 6}}));

        System.out.println("3. Two heaps");
        check("running median of 5,15,1,3", "[5.0, 10.0, 5.0, 4.0]", runningMedian(new int[]{5, 15, 1, 3}));

        System.out.println("4. Sort + heap");
        check("rooms for [0,30],[5,10],[15,20]", 2, maxOverlap(new int[][]{{0, 30}, {5, 10}, {15, 20}}));

        System.out.println("5. Most frequent first");
        check("aaabbc", "ababac", noAdjacent("aaabbc"));
        check("aaab (impossible)", "", noAdjacent("aaab"));

        System.out.println("6. Lazy deletion");
        LazyMinHeap lz = new LazyMinHeap();
        for (int x : new int[]{5, 1, 3, 1}) lz.add(x);
        lz.remove(1);
        check("after removing one 1, min", 1, lz.min());
        lz.remove(1);
        check("after removing both 1s, min", 3, lz.min());
        check("logical size", 2, lz.size);
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
