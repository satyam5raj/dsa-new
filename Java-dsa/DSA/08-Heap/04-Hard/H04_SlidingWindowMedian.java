/*
 * ============================================================================
 *  HARD 04 : SLIDING WINDOW MEDIAN           LeetCode #480
 *  https://leetcode.com/problems/sliding-window-median/
 *  Pattern: TWO HEAPS (H01) + LAZY DELETION for the element leaving the window
 * ============================================================================
 *
 *  PROBLEM
 *    Return the median of every window of size k.
 *      [1,3,-1,-3,5,3,6,7], k=3 -> [1,-1,-1,3,5,6]
 *
 *  ---------------------------------------------------------------------------
 *  H01 handles ADDING. A window also REMOVES the element that slides out, and
 *  PriorityQueue.remove(x) is O(k). With n windows that's O(n*k) (acceptable for
 *  small k, and it's a fine first answer in an interview).
 *
 *  LAZY DELETION, O(n log n):
 *    Don't remove the outgoing value now. Write it in a map `delayed` (value -> count)
 *    and adjust the half sizes (sizes count only LIVE elements). When a dead value
 *    reaches the TOP of a heap (where it could affect the answer), pop it then.
 *
 *    Keep:  lowSize, highSize = live counts;  lowSize == highSize or highSize + 1
 *    add(x):    x <= low.top ? low : high;   then rebalance
 *    remove(x): delayed[x]++;  decrement the size of the half x belongs to
 *               (x <= low.top -> it's in low); if x IS a top, prune that heap now;
 *               rebalance
 *    rebalance: move one top across if the live sizes are off, then prune that heap
 *    prune(heap): while heap.top is in delayed: pop it, delayed[top]--
 *
 *  ALTERNATIVE (simpler to get right): TWO TreeSets of indices (ordered by value,
 *  then index). TreeSet.remove is O(log k) directly. Used below as the referee.
 *
 *  OVERFLOW: values up to 2^31 - 1: average two of them in long / double.
 *  COMPLEXITY   Time O(n log n)   Space O(n) (dead entries can pile up)
 * ============================================================================
 */

import java.util.*;

public class H04_SlidingWindowMedian {

    static class DualHeap {
        final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());
        final PriorityQueue<Integer> high = new PriorityQueue<>();
        final Map<Integer, Integer> delayed = new HashMap<>();
        int lowSize, highSize;                                      // LIVE element counts

        void add(int x) {
            if (low.isEmpty() || x <= low.peek()) { low.offer(x); lowSize++; }
            else { high.offer(x); highSize++; }
            rebalance();
        }

        void remove(int x) {
            delayed.merge(x, 1, Integer::sum);
            if (x <= low.peek()) {
                lowSize--;
                if (x == low.peek()) prune(low);
            } else {
                highSize--;
                if (!high.isEmpty() && x == high.peek()) prune(high);
            }
            rebalance();
        }

        double median() {
            return lowSize > highSize ? low.peek() : ((double) low.peek() + high.peek()) / 2.0;
        }

        private void rebalance() {
            if (lowSize > highSize + 1) {                           // low too big
                high.offer(low.poll()); lowSize--; highSize++;
                prune(low);
            } else if (lowSize < highSize) {                        // high too big
                low.offer(high.poll()); highSize--; lowSize++;
                prune(high);
            }
        }

        private void prune(PriorityQueue<Integer> heap) {           // drop dead values from the top
            while (!heap.isEmpty()) {
                int top = heap.peek();
                Integer d = delayed.get(top);
                if (d == null) break;
                if (d == 1) delayed.remove(top); else delayed.put(top, d - 1);
                heap.poll();
            }
        }
    }

    static double[] medianSlidingWindow(int[] nums, int k) {
        DualHeap dh = new DualHeap();
        for (int i = 0; i < k; i++) dh.add(nums[i]);
        double[] result = new double[nums.length - k + 1];
        result[0] = dh.median();
        for (int i = k; i < nums.length; i++) {
            dh.add(nums[i]);
            dh.remove(nums[i - k]);
            result[i - k + 1] = dh.median();
        }
        return result;
    }

    // referee: two TreeSets of indices, ordered by (value, index)
    static double[] withTreeSets(int[] nums, int k) {
        Comparator<Integer> cmp = (a, b) -> nums[a] != nums[b] ? Integer.compare(nums[a], nums[b]) : Integer.compare(a, b);
        TreeSet<Integer> lo = new TreeSet<>(cmp), hi = new TreeSet<>(cmp);
        double[] out = new double[nums.length - k + 1];
        for (int i = 0; i < nums.length; i++) {
            if (i >= k) { if (!lo.remove(i - k)) hi.remove(i - k); }
            lo.add(i);
            hi.add(lo.pollLast());
            if (hi.size() > lo.size()) lo.add(hi.pollFirst());
            if (i >= k - 1) out[i - k + 1] = lo.size() > hi.size() ? nums[lo.last()] : ((double) nums[lo.last()] + nums[hi.first()]) / 2.0;
        }
        return out;
    }

    public static void main(String[] args) {
        check("[1,3,-1,-3,5,3,6,7] k=3", "[1.0, -1.0, -1.0, 3.0, 5.0, 6.0]", Arrays.toString(medianSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3)));
        check("[1,2,3,4,2,3,1,4,2] k=3", "[2.0, 3.0, 3.0, 3.0, 2.0, 3.0, 2.0]", Arrays.toString(medianSlidingWindow(new int[]{1, 2, 3, 4, 2, 3, 1, 4, 2}, 3)));
        check("MAX_VALUE pair k=2", "[2.147483647E9]", Arrays.toString(medianSlidingWindow(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}, 2)));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(40), -5, 6).toArray();        // lots of duplicates
            int k = 1 + rnd.nextInt(a.length);
            ok &= Arrays.equals(medianSlidingWindow(a, k), withTreeSets(a, k));
        }
        check("1,000 random arrays vs TreeSets", true, ok);
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
