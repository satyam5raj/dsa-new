/*
 * ============================================================================
 *  EASY 03 : NUMBER OF RECENT CALLS  ⭐ bonus   LeetCode #933
 *  https://leetcode.com/problems/number-of-recent-calls/
 *  Pattern: SLIDING TIME WINDOW (expire from the front)
 * ============================================================================
 *
 *  PROBLEM
 *    ping(t) records a request at time t (t strictly increases between calls) and
 *    returns how many requests happened in the last 3000 ms: the range [t-3000, t].
 *      ping(1) = 1, ping(100) = 2, ping(3001) = 3, ping(3002) = 3   (1 is now too old)
 *
 *  ---------------------------------------------------------------------------
 *  WHY A QUEUE: times arrive in increasing order, so the OLDEST requests are
 *  always at the FRONT. Requests fall out of the window in exactly the order
 *  they came in -> FIFO.
 *    ping(t): offer t; while front < t - 3000: poll; return size
 *
 *  DRY RUN
 *    ping(1)    [1]               -> 1
 *    ping(100)  [1,100]           -> 2
 *    ping(3001) [1,100,3001]      -> 3   (1 >= 3001-3000 = 1: still inside)
 *    ping(3002) [100,3001,3002]   -> 3   (1 < 2: expired)
 *
 *  REAL WORLD: this is the "sliding log" rate limiter: count the requests in the
 *  last window and reject when over the limit. (The Design Hit Counter problem,
 *  M03, is the memory-bounded version.)
 *  COMPLEXITY   amortized O(1) per ping (each time is added and removed once)
 *               Space O(requests in one window)
 * ============================================================================
 */

import java.util.*;

public class E03_NumberOfRecentCalls {

    static class RecentCounter {
        private final Deque<Integer> requests = new ArrayDeque<>();

        int ping(int t) {
            requests.offer(t);
            while (requests.peek() < t - 3000) requests.poll();   // drop expired requests
            return requests.size();
        }
    }

    public static void main(String[] args) {
        RecentCounter rc = new RecentCounter();
        check("ping(1)", 1, rc.ping(1));
        check("ping(100)", 2, rc.ping(100));
        check("ping(3001) (1 is still inside)", 3, rc.ping(3001));
        check("ping(3002) (1 expired)", 3, rc.ping(3002));
        check("ping(10000) (all old expired)", 1, rc.ping(10000));
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
