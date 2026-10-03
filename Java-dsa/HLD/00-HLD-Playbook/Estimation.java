/*
 * ============================================================================
 *  HLD 00 : BACK-OF-THE-ENVELOPE ESTIMATION  (the numbers behind every case study)
 * ============================================================================
 *  Run:  java Estimation.java
 *
 *  An estimate is a chain of ASSUMPTIONS turned into NUMBERS that drive DECISIONS.
 *  This file encodes the chains for the case studies, so the arithmetic is checked
 *  and each step is explained. In an interview you do the same thing on the whiteboard
 *  with round numbers (1 day ~ 10^5 s).
 *
 *     qps(perDay)                 -> average requests per second
 *     storage(items, bytes, yrs)  -> total bytes
 *     servers(qps, perServer)     -> instances needed (with headroom)
 *     cacheFor(hotFraction, ...)  -> memory for the hot set (Pareto: 20% of items, 80% of reads)
 * ============================================================================
 */

import java.util.*;

public class Estimation {

    static final double SECONDS_PER_DAY = 86_400;
    static final long KB = 1_000, MB = 1_000_000, GB = 1_000_000_000L, TB = 1_000_000_000_000L;

    static double qps(double perDay) { return perDay / SECONDS_PER_DAY; }
    static long storageBytes(double itemsPerDay, long bytesPerItem, int years) { return (long) (itemsPerDay * 365 * years * bytesPerItem); }
    /** Instances needed at `targetUtilisation` (never plan for 100%: GC, deploys, failover need headroom). */
    static long servers(double peakQps, double qpsPerServer, double targetUtilisation) { return (long) Math.ceil(peakQps / (qpsPerServer * targetUtilisation)); }

    static String human(double bytes) {
        String[] u = {"B", "KB", "MB", "GB", "TB", "PB"};
        int i = 0;
        while (bytes >= 1000 && i < u.length - 1) { bytes /= 1000; i++; }
        return String.format("%.1f %s", bytes, u[i]);
    }

    public static void main(String[] args) {
        System.out.println("0. Rules of thumb");
        check("1M/day ~ 12 QPS", 12L, Math.round(qps(1e6)));
        check("1B/day ~ 11.6K QPS", 11_574L, Math.round(qps(1e9)));

        System.out.println("1. URL shortener (case study 07)");
        // assumptions: 100M new URLs/day, 100:1 read:write, 500 B per record, keep 10 years
        double writes = qps(100e6), reads = writes * 100;
        System.out.printf("   writes %.0f/s avg, reads %.0f/s avg (~%.0f/s at 3x peak)%n", writes, reads, reads * 3);
        long storage = storageBytes(100e6, 500, 10);
        check("10-year storage ~ 182.5 TB -> must shard", "182.5 TB", human(storage));
        long codes = 100_000_000L * 365 * 10;
        check("62^7 = 3.5T codes covers 365B URLs", true, Math.pow(62, 7) > codes);
        // cache: 20% of a day's reads are for hot URLs -> cache those records
        double cache = reads * SECONDS_PER_DAY * 0.2 * 500;
        System.out.println("   cache for 20% of daily reads: " + human(cache) + " -> fits in a small Redis cluster");
        check("redirect fleet at 1K QPS/server, 3x peak, 70% util", 497L, servers(reads * 3, 1000, 0.7));

        System.out.println("2. Chat system (case study 08)");
        // assumptions: 500M DAU, 40 messages/user/day, 100 B per message, keep 5 years
        double msgs = 500e6 * 40;
        check("messages/s average ~ 231K", 231_481L, Math.round(qps(msgs)));
        check("5-year storage: 20B msgs/day x 100 B -> petabytes", "3.7 PB", human(storageBytes(msgs, 100, 5)));
        // connections: every online user holds a WebSocket; one gateway box holds ~100K
        long concurrent = (long) (500e6 * 0.4);
        check("gateways for 200M concurrent sockets @100K each (+25% headroom)", 2500L, servers(concurrent, 100_000, 0.8));

        System.out.println("3. News feed (case study 09)");
        // assumptions: 300M DAU, 2 posts/day/10 users (0.2), 200 followers avg, 10 feed loads/day
        double posts = 300e6 * 0.2;
        double fanoutWrites = posts * 200;
        check("fan-out-on-write inserts/s ~ 139K", 138_889L, Math.round(qps(fanoutWrites)));
        double feedReads = 300e6 * 10;
        check("feed reads/s ~ 34.7K avg", 34_722L, Math.round(qps(feedReads)));
        // feed cache: 300M users x 500 post ids x 8 bytes
        check("precomputed timelines (ids only)", "1.2 TB", human(300e6 * 500 * 8));
        // celebrity problem: one post by a 50M-follower account
        System.out.println("   one celebrity post = 50,000,000 timeline writes -> fan-out on READ for celebrities");

        System.out.println("4. Bandwidth sanity check");
        // 10K photo views/s x 300 KB each
        check("egress", "3.0 GB", human(10_000 * 300 * KB) );
        System.out.println("   ...per SECOND = 24 Gbit/s -> CDN, not app servers");
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
