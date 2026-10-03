/*
 * ============================================================================
 *  EASY 02 : MOVING AVERAGE FROM DATA STREAM   LeetCode #346 (Premium)
 *  https://leetcode.com/problems/moving-average-from-data-stream/
 *  Pattern: FIXED-SIZE QUEUE + RUNNING SUM (sliding window over a stream)
 * ============================================================================
 *
 *  PROBLEM
 *    MovingAverage(size); next(val) returns the average of the LAST 'size' values.
 *      size 3: next(1) = 1.0, next(10) = 5.5, next(3) = 4.67, next(5) = 6.0  ((10+3+5)/3)
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: keep everything, sum the last 'size' values on each call -> O(size) per call.
 *
 *  QUEUE + RUNNING SUM -> O(1) per call
 *    - the queue holds exactly the values inside the window
 *    - 'sum' = the sum of the queue (updated incrementally, never recomputed)
 *    next(val): offer val, sum += val
 *               if the queue is bigger than size: sum -= poll()   (the oldest leaves)
 *               return sum / queue.size()
 *  Exactly Arrays/E23 (fixed sliding window), but the data arrives one value at a time.
 *
 *  RING-BUFFER VERSION (no Queue object): an int[size] array plus an index
 *  i = count % size. The slot being overwritten IS the value leaving the window.
 *  This is how real systems (metrics, audio buffers) do it. Shown below.
 *
 *  COMPLEXITY   O(1) per call   Space O(size)
 *  Use a long for the sum: many large values could overflow int.
 * ============================================================================
 */

import java.util.*;

public class E02_MovingAverageFromDataStream {

    static class MovingAverage {
        private final Deque<Integer> window = new ArrayDeque<>();
        private final int size;
        private long sum;

        MovingAverage(int size) { this.size = size; }

        double next(int val) {
            window.offer(val);
            sum += val;
            if (window.size() > size) sum -= window.poll();   // the oldest value leaves
            return (double) sum / window.size();
        }
    }

    static class MovingAverageRing {
        private final int[] ring;
        private int count;
        private long sum;

        MovingAverageRing(int size) { ring = new int[size]; }

        double next(int val) {
            int i = count % ring.length;
            if (count >= ring.length) sum -= ring[i];          // overwrite = drop the oldest
            ring[i] = val;
            sum += val;
            count++;
            return (double) sum / Math.min(count, ring.length);
        }
    }

    public static void main(String[] args) {
        MovingAverage m = new MovingAverage(3);
        check("next(1)", 1.0, m.next(1));
        check("next(10)", 5.5, m.next(10));
        check("next(3) ~4.667", "4.667", String.format("%.3f", m.next(3)));
        check("next(5) window [10,3,5]", 6.0, m.next(5));

        Random rnd = new Random(2);
        MovingAverage a = new MovingAverage(5);
        MovingAverageRing b = new MovingAverageRing(5);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            int v = rnd.nextInt(1000);
            ok &= Math.abs(a.next(v) - b.next(v)) < 1e-9;
        }
        check("ring buffer agrees on 1,000 values", true, ok);
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
