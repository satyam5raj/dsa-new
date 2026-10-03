/*
 * ============================================================================
 *  EASY 01 : LOGGER RATE LIMITER  ⭐ bonus    LeetCode #359 (premium)
 *  https://leetcode.com/problems/logger-rate-limiter/
 *  Pattern: HashMap of "next allowed time" per key (a tiny TTL cache)
 * ============================================================================
 *
 *  PROBLEM
 *    shouldPrintMessage(timestamp, message): print a message only if the same message
 *    wasn't printed in the last 10 seconds. Timestamps arrive in non-decreasing order.
 *      (1,"foo") true, (2,"bar") true, (3,"foo") false, (8,"bar") false,
 *      (10,"foo") false, (11,"foo") true
 *
 *  ---------------------------------------------------------------------------
 *  Store message -> the earliest timestamp it may be printed AGAIN:
 *     if timestamp >= allowedAt.getOrDefault(msg, 0): allowedAt[msg] = timestamp + 10; true
 *     else false
 *  O(1) per call.
 *
 *  MEMORY PROBLEM: the map grows forever with distinct messages. Real systems
 *  expire old entries:
 *    - a queue of (time, message) to evict entries older than 10s, or
 *    - two maps "current window" / "previous window", swapped every 10 seconds
 *  This is the same TTL idea used by caches (Redis EXPIRE) and rate limiters.
 *  The bounded version below keeps only messages seen in the last 10 seconds.
 *
 *  COMPLEXITY   O(1) per call (amortized for the bounded version)
 * ============================================================================
 */

import java.util.*;

public class E01_LoggerRateLimiter {

    static class Logger {
        private final Map<String, Integer> allowedAt = new HashMap<>();

        public boolean shouldPrintMessage(int timestamp, String message) {
            if (timestamp < allowedAt.getOrDefault(message, 0)) return false;
            allowedAt.put(message, timestamp + 10);
            return true;
        }
    }

    // memory-bounded: forget messages whose 10-second window has passed
    static class BoundedLogger {
        private final Map<String, Integer> lastPrinted = new HashMap<>();
        private final Deque<Object[]> recent = new ArrayDeque<>();   // {time, message}, oldest first

        public boolean shouldPrintMessage(int timestamp, String message) {
            while (!recent.isEmpty() && (int) recent.peekFirst()[0] <= timestamp - 10) {
                Object[] old = recent.pollFirst();
                if (lastPrinted.get((String) old[1]) == (int) old[0]) lastPrinted.remove((String) old[1]);   // expired
            }
            if (lastPrinted.containsKey(message)) return false;
            lastPrinted.put(message, timestamp);
            recent.addLast(new Object[]{timestamp, message});
            return true;
        }

        int remembered() { return lastPrinted.size(); }
    }

    public static void main(String[] args) {
        Logger lg = new Logger();
        StringBuilder sb = new StringBuilder();
        int[] ts = {1, 2, 3, 8, 10, 11};
        String[] ms = {"foo", "bar", "foo", "bar", "foo", "foo"};
        for (int i = 0; i < ts.length; i++) sb.append(lg.shouldPrintMessage(ts[i], ms[i]) ? 'T' : 'F');
        check("example", "TTFFFT", sb.toString());

        Random rnd = new Random(1);
        Logger a = new Logger();
        BoundedLogger b = new BoundedLogger();
        boolean ok = true;
        int t = 0, maxRemembered = 0;
        for (int i = 0; i < 100_000; i++) {
            t += rnd.nextInt(3);
            String m = "m" + rnd.nextInt(50_000);
            ok &= a.shouldPrintMessage(t, m) == b.shouldPrintMessage(t, m);
            maxRemembered = Math.max(maxRemembered, b.remembered());
        }
        check("100,000 calls: simple == bounded", true, ok);
        System.out.println("   bounded logger never remembered more than " + maxRemembered + " messages (simple one keeps them all)");
        check("bounded memory stays small", true, maxRemembered < 100);
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
