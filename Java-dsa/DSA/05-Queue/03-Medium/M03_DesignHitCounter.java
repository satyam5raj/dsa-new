/*
 * ============================================================================
 *  MEDIUM 03 : DESIGN HIT COUNTER            LeetCode #362 (Premium)
 *  https://leetcode.com/problems/design-hit-counter/
 *  Pattern: QUEUE (sliding log)  ->  FIXED BUCKETS (constant memory)
 * ============================================================================
 *
 *  PROBLEM
 *    hit(timestamp) records a hit; getHits(timestamp) = hits in the past 300 seconds,
 *    i.e. in (timestamp - 300, timestamp]. Timestamps are in seconds and never go
 *    backwards. Several hits can share a second.
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: QUEUE OF TIMESTAMPS (E03 Recent Calls)
 *    hit: offer t.  getHits: drop from the front while front <= t - 300, return size.
 *    PROBLEM: 10 million hits in 5 minutes = 10 million queue entries in memory.
 *
 *  APPROACH 2: 300 BUCKETS (the follow-up answer)
 *    A hit at second t goes into bucket i = t % 300. Each bucket stores
 *      times[i] = the second it currently represents,  counts[i] = hits in that second.
 *    hit(t):     if times[i] != t, the bucket holds an OLD second (>= 300 s ago):
 *                reset it (times[i] = t, counts[i] = 0). Then counts[i]++.
 *    getHits(t): sum counts[i] over buckets with t - times[i] < 300 (still recent).
 *    Memory is FIXED (300 slots) no matter how many hits come in.
 *    getHits costs O(300) = O(1).
 *
 *  REAL WORLD: this is how metrics systems keep "requests per minute" cheaply,
 *  and a "fixed window counter" rate limiter. Thread safety would need atomics or
 *  locks (see the Multithreading topic).
 * ============================================================================
 */

import java.util.*;

public class M03_DesignHitCounter {

    static class HitCounterQueue {
        private final Deque<Integer> hits = new ArrayDeque<>();

        void hit(int t) { hits.offer(t); }

        int getHits(int t) {
            while (!hits.isEmpty() && hits.peek() <= t - 300) hits.poll();
            return hits.size();
        }
    }

    static class HitCounter {
        private final int[] times = new int[300], counts = new int[300];

        void hit(int t) {
            int i = t % 300;
            if (times[i] != t) {          // this bucket still holds an expired second
                times[i] = t;
                counts[i] = 0;
            }
            counts[i]++;
        }

        int getHits(int t) {
            int total = 0;
            for (int i = 0; i < 300; i++) {
                if (t - times[i] < 300) total += counts[i];
            }
            return total;
        }
    }

    public static void main(String[] args) {
        HitCounter hc = new HitCounter();
        hc.hit(1); hc.hit(2); hc.hit(3);
        check("getHits(4)", 3, hc.getHits(4));
        hc.hit(300);
        check("getHits(300)", 4, hc.getHits(300));
        check("getHits(301) (hit at 1 expired)", 3, hc.getHits(301));

        Random rnd = new Random(3);
        HitCounter buckets = new HitCounter();
        HitCounterQueue queue = new HitCounterQueue();
        boolean ok = true;
        int t = 1;
        for (int i = 0; i < 20_000; i++) {
            t += rnd.nextInt(5) == 0 ? rnd.nextInt(200) : 0;    // many hits per second, occasional jumps
            if (rnd.nextBoolean()) { buckets.hit(t); queue.hit(t); }
            else ok &= buckets.getHits(t) == queue.getHits(t);
        }
        check("buckets == queue on 20,000 random ops", true, ok);
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
