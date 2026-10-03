/*
 * ============================================================================
 *  HLD 10 : RIDE-SHARING AT SCALE (Uber / Ola / Lyft)  - simulated
 * ============================================================================
 *  Run:  java RideSharingHld.java          (the full design answer: README.md)
 *
 *     [geohash]   a real geohash encoder: interleave lon/lat bits, base32. Nearby points share
 *                 a PREFIX -> "drivers near me" = look up my cell + its 8 neighbours.
 *                 Verified against a brute-force radius search.
 *     [ingest]    250K location updates/s: shard the location index by geohash prefix;
 *                 dense cities make hot shards -> split hot cells finer
 *     [matching]  greedy (nearest driver per request, in arrival order) vs BATCHED matching
 *                 (collect requests for ~2s, solve the assignment): lower total pickup distance
 *     [surge]     demand / supply per geohash cell -> multiplier
 *  (Class-level design - trip state machine, CAS driver claim, decorators: LLD/08-Ride-Sharing.)
 * ============================================================================
 */

import java.util.*;

public class RideSharingHld {

    // =====================================================================
    // [geohash]
    // =====================================================================

    static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /** Standard geohash: alternately bisect longitude and latitude, 5 bits per character. */
    static String geohash(double lat, double lon, int precision) {
        double[] latR = {-90, 90}, lonR = {-180, 180};
        StringBuilder sb = new StringBuilder();
        boolean even = true;
        int bit = 0, ch = 0;
        while (sb.length() < precision) {
            double[] r = even ? lonR : latR;
            double v = even ? lon : lat, mid = (r[0] + r[1]) / 2;
            if (v >= mid) { ch = (ch << 1) | 1; r[0] = mid; } else { ch <<= 1; r[1] = mid; }
            even = !even;
            if (++bit == 5) { sb.append(BASE32.charAt(ch)); bit = 0; ch = 0; }
        }
        return sb.toString();
    }

    /** Cell size at a precision (degrees): lon bits = ceil(5p/2), lat bits = floor(5p/2). */
    static double[] cellSizeDeg(int p) { int bits = 5 * p; return new double[]{180 / Math.pow(2, bits / 2), 360 / Math.pow(2, (bits + 1) / 2)}; }

    /** The 9 cells around a point (same precision): compute by offsetting by one cell size. */
    static Set<String> neighbourhood(double lat, double lon, int p) {
        double[] s = cellSizeDeg(p);
        Set<String> cells = new HashSet<>();
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) cells.add(geohash(lat + dy * s[0], lon + dx * s[1], p));
        return cells;
    }

    static double km(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * 6371 * Math.asin(Math.sqrt(a));
    }

    record Driver(String id, double lat, double lon) {}

    // =====================================================================
    // [matching]
    // =====================================================================

    /** Greedy: requests arrive one by one; each takes the nearest still-free driver. */
    static double greedyTotal(double[][] riders, double[][] drivers) {
        boolean[] used = new boolean[drivers.length];
        double total = 0;
        for (double[] r : riders) {
            int best = -1; double bd = Double.MAX_VALUE;
            for (int d = 0; d < drivers.length; d++) if (!used[d]) { double x = km(r[0], r[1], drivers[d][0], drivers[d][1]); if (x < bd) { bd = x; best = d; } }
            used[best] = true; total += bd;
        }
        return total;
    }

    /** Batched: assign all riders in the batch at once minimising TOTAL pickup distance (exact, via bitmask DP). */
    static double batchedTotal(double[][] riders, double[][] drivers) {
        int n = riders.length, m = drivers.length;
        double[] dp = new double[1 << m];
        Arrays.fill(dp, Double.MAX_VALUE);
        dp[0] = 0;
        double best = Double.MAX_VALUE;
        for (int mask = 0; mask < 1 << m; mask++) {
            if (dp[mask] == Double.MAX_VALUE) continue;
            int i = Integer.bitCount(mask);                                   // rider i is next
            if (i == n) { best = Math.min(best, dp[mask]); continue; }
            for (int d = 0; d < m; d++) if ((mask >> d & 1) == 0) {
                double v = dp[mask] + km(riders[i][0], riders[i][1], drivers[d][0], drivers[d][1]);
                if (v < dp[mask | 1 << d]) dp[mask | 1 << d] = v;
            }
        }
        return best;
    }

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        System.out.println("1. Geohash");
        check("matches the reference example (Wikipedia)", "u4pruydqqvj", geohash(57.64911, 10.40744, 11));
        System.out.println("   Bangalore MG Road at precision 6: " + geohash(12.9750, 77.6060, 6));
        double[] s6 = cellSizeDeg(6);
        System.out.printf("   precision 6 cell ~ %.2f km x %.2f km%n", s6[0] * 111, s6[1] * 111 * Math.cos(Math.toRadians(13)));
        check("nearby points share a prefix", geohash(12.9750, 77.6060, 5), geohash(12.9760, 77.6070, 5));

        Random rnd = new Random(1);
        List<Driver> drivers = new ArrayList<>();
        Map<String, List<Driver>> index = new HashMap<>();                     // geohash(6) -> drivers   (Redis GEO / sorted sets)
        for (int i = 0; i < 50_000; i++) {
            Driver d = new Driver("d" + i, 12.85 + rnd.nextDouble() * 0.3, 77.45 + rnd.nextDouble() * 0.3);
            drivers.add(d);
            index.computeIfAbsent(geohash(d.lat(), d.lon(), 6), k -> new ArrayList<>()).add(d);
        }
        boolean allFound = true;
        int candidatesChecked = 0;
        for (int q = 0; q < 200; q++) {
            double lat = 12.9 + rnd.nextDouble() * 0.2, lon = 77.5 + rnd.nextDouble() * 0.2;
            Set<String> found = new HashSet<>();
            for (String cell : neighbourhood(lat, lon, 6)) for (Driver d : index.getOrDefault(cell, List.of())) { candidatesChecked++; if (km(lat, lon, d.lat(), d.lon()) <= 0.5) found.add(d.id()); }
            for (Driver d : drivers) if (km(lat, lon, d.lat(), d.lon()) <= 0.5 && !found.contains(d.id())) allFound = false;
        }
        check("9-cell lookup finds every driver within 500 m (vs brute force)", true, allFound);
        System.out.printf("   each query examined ~%d drivers instead of 50,000%n", candidatesChecked / 200);

        System.out.println("2. Location ingest: 1M drivers x 1 update / 4 s = 250K writes/s");
        Map<String, Integer> perShard = new TreeMap<>();
        for (int i = 0; i < 100_000; i++) {
            boolean dense = rnd.nextDouble() < 0.6;                          // 60% of drivers are in one metro
            double lat = dense ? 12.9 + rnd.nextDouble() * 0.2 : -40 + rnd.nextDouble() * 100, lon = dense ? 77.5 + rnd.nextDouble() * 0.2 : -120 + rnd.nextDouble() * 240;
            perShard.merge(geohash(lat, lon, 2), 1, Integer::sum);            // shard by a 2-char prefix (~1,250 km cells)
        }
        int hottest = Collections.max(perShard.values());
        System.out.printf("   %d shards; the metro's shard takes %.0f%% of all updates%n", perShard.size(), 100.0 * hottest / 100_000);
        check("prefix sharding alone creates a hot shard", true, hottest > 50_000);
        System.out.println("   -> split hot regions on longer prefixes (city -> 4-5 chars) or shard by city id + cell");

        System.out.println("3. Greedy vs batched matching (8 requests, 10 drivers per batch, 300 batches)");
        double g = 0, b = 0;
        for (int t = 0; t < 300; t++) {
            double[][] rs = new double[8][], ds = new double[10][];
            for (int i = 0; i < 8; i++) rs[i] = new double[]{12.9 + rnd.nextDouble() * 0.05, 77.6 + rnd.nextDouble() * 0.05};
            for (int i = 0; i < 10; i++) ds[i] = new double[]{12.9 + rnd.nextDouble() * 0.05, 77.6 + rnd.nextDouble() * 0.05};
            g += greedyTotal(rs, ds); b += batchedTotal(rs, ds);
        }
        System.out.printf("   total pickup km: greedy %.0f, batched %.0f (%.0f%% less driving)%n", g, b, 100 * (g - b) / g);
        check("batched assignment is never worse", true, b <= g + 1e-9);
        check("...and meaningfully better", true, (g - b) / g > 0.05);

        System.out.println("4. Surge per cell");
        Map<String, int[]> demandSupply = new HashMap<>();                   // cell -> {requests, free drivers}
        demandSupply.put("tdr1y", new int[]{120, 40});
        demandSupply.put("tdr1v", new int[]{10, 50});
        Map<String, Double> surge = new TreeMap<>();
        demandSupply.forEach((cell, ds) -> surge.put(cell, Math.round(Math.max(1.0, Math.min(3.0, (double) ds[0] / Math.max(1, ds[1]))) * 10) / 10.0));
        check("busy cell surges, quiet cell doesn't", "{tdr1v=1.0, tdr1y=3.0}", surge);
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
