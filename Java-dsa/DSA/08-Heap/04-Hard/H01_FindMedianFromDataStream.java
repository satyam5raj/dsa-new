/*
 * ============================================================================
 *  HARD 01 : FIND MEDIAN FROM DATA STREAM    LeetCode #295
 *  https://leetcode.com/problems/find-median-from-data-stream/
 *  Pattern: TWO HEAPS - a max-heap for the lower half, a min-heap for the upper half
 * ============================================================================
 *
 *  PROBLEM
 *    addNum(x) and findMedian() on a growing stream.
 *      add 1, add 2 -> median 1.5;  add 3 -> median 2
 *
 *  ---------------------------------------------------------------------------
 *  OPTIONS
 *    keep a sorted list (insertion O(n), median O(1))
 *    sort on every query (O(n log n) per query)
 *    TWO HEAPS: add O(log n), median O(1)   <- the answer
 *
 *  IDEA: split the numbers into two halves.
 *     low  = MAX-heap holding the smaller half  -> its root is the biggest small number
 *     high = MIN-heap holding the larger half   -> its root is the smallest big number
 *  The two roots sit right in the MIDDLE of the sorted order.
 *  INVARIANTS
 *     1. every element of low <= every element of high
 *     2. low.size() == high.size()  or  low.size() == high.size() + 1
 *  Median: odd count -> low.peek();  even -> (low.peek() + high.peek()) / 2
 *
 *  ADD (3 steps that keep both invariants without any if/else on values):
 *     low.offer(x)                 // tentatively into the lower half
 *     high.offer(low.poll())       // the largest of the lower half moves up -> invariant 1
 *     if high.size() > low.size(): low.offer(high.poll())   // rebalance -> invariant 2
 *
 *  DRY RUN add 5, 15, 1, 3
 *    5 : low {5}         high {}       median 5
 *    15: low {5}         high {15}     median 10
 *    1 : low {1,5}       high {15}     median 5
 *    3 : low {1,3}       high {5,15}   median 4
 *
 *  FOLLOW-UPS (classic)
 *    all numbers in [0, 100]  -> 101 counters, walk them: O(100) per median
 *    99% in [0, 100]          -> counters + two small "outlier" lists
 *
 *  OVERFLOW: (a + b) / 2.0 with ints near MAX_VALUE: compute as a / 2.0 + b / 2.0 or use long.
 *  COMPLEXITY   addNum O(log n), findMedian O(1)
 * ============================================================================
 */

import java.util.*;

public class H01_FindMedianFromDataStream {

    static class MedianFinder {
        private final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());  // max-heap
        private final PriorityQueue<Integer> high = new PriorityQueue<>();                          // min-heap

        void addNum(int num) {
            low.offer(num);
            high.offer(low.poll());                         // keep every low <= every high
            if (high.size() > low.size()) low.offer(high.poll());   // low may hold one extra
        }

        double findMedian() {
            if (low.size() > high.size()) return low.peek();
            return ((long) low.peek() + high.peek()) / 2.0; // long: no overflow
        }
    }

    public static void main(String[] args) {
        MedianFinder mf = new MedianFinder();
        mf.addNum(1);
        mf.addNum(2);
        check("1, 2", 1.5, mf.findMedian());
        mf.addNum(3);
        check("1, 2, 3", 2.0, mf.findMedian());
        MedianFinder big = new MedianFinder();
        big.addNum(Integer.MAX_VALUE);
        big.addNum(Integer.MAX_VALUE);
        check("MAX_VALUE twice (overflow check)", 2.147483647E9, big.findMedian());

        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 50; t++) {
            MedianFinder m = new MedianFinder();
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int x = rnd.nextInt(201) - 100;
                m.addNum(x);
                all.add(x);
                List<Integer> s = new ArrayList<>(all);
                Collections.sort(s);
                int n = s.size();
                double exp = n % 2 == 1 ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2.0;
                ok &= m.findMedian() == exp;
            }
        }
        check("10,000 medians vs sorting", true, ok);
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
