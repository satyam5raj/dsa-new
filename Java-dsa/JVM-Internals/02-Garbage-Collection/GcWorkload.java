/*
 * ============================================================================
 *  GC WORKLOAD - the same allocation pattern, run under different collectors
 * ============================================================================
 *
 *  Run one:   java -Xmx256m -XX:+UseG1GC GcWorkload.java
 *  Run all:   ./compare-gcs.sh
 *  With logs: java -Xmx256m -XX:+UseG1GC -Xlog:gc GcWorkload.java
 *
 *  The workload imitates a typical backend service:
 *    - LOTS of short-lived garbage (request objects, JSON strings)
 *    - a slowly churning LONG-lived cache (objects that survive and get promoted)
 *  It records the worst "hiccup": the longest gap the app thread experienced
 *  between two loop iterations. That gap is a decent proxy for STW pauses
 *  seen by the application.
 *
 *  WHAT TO EXPECT (roughly; it depends on your machine):
 *    Serial   : longest pauses (one GC thread)
 *    Parallel : best throughput, but noticeable pauses
 *    G1       : balanced
 *    ZGC      : smallest pauses, maybe slightly lower throughput
 */

import java.lang.management.*;
import java.util.*;

public class GcWorkload {

    public static void main(String[] args) {
        String gcNames = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName).reduce((a, b) -> a + ", " + b).orElse("?");

        // Long-lived cache: ~60 MB that keeps changing, so objects survive and get promoted
        int cacheSize = 60_000;
        byte[][] cache = new byte[cacheSize][];
        Random rnd = new Random(1);

        long start = System.nanoTime();
        long last = start;
        long worstGapNs = 0;
        long checksum = 0;
        int iterations = 30_000_000;
        int warmup = iterations / 10;               // ignore hiccups during JIT warm-up

        for (int i = 0; i < iterations; i++) {
            // short-lived garbage, like handling a request
            String json = "{\"id\":" + i + ",\"name\":\"user" + (i % 1000) + "\"}";
            int[] temp = new int[32];
            checksum += json.length() + temp.length;

            // every 5th iteration, replace a cache entry (~1 KB): old entry becomes old-gen garbage
            if (i % 5 == 0) cache[rnd.nextInt(cacheSize)] = new byte[1024];

            long now = System.nanoTime();
            if (i > warmup) worstGapNs = Math.max(worstGapNs, now - last);
            last = now;
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;

        long gcCount = 0, gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcTime += Math.max(0, gc.getCollectionTime());
        }
        System.out.printf("%-45s total=%5d ms | GC events=%4d | GC time=%5d ms | worst app hiccup=%6.2f ms  (chk %d)%n",
                gcNames, totalMs, gcCount, gcTime, worstGapNs / 1e6, checksum % 10);
    }
}
