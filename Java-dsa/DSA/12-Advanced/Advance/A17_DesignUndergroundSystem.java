/*
 * ============================================================================
 *  ADVANCE 17 : DESIGN UNDERGROUND SYSTEM  ⭐ bonus   LeetCode #1396
 *  https://leetcode.com/problems/design-underground-system/
 *  Pattern: TWO HashMaps - who is travelling right now + running totals per route
 * ============================================================================
 *
 *  PROBLEM
 *    checkIn(id, station, t)       passenger id enters at station at time t
 *    checkOut(id, station, t)      passenger id leaves at station at time t
 *    getAverageTime(start, end)    average time of all COMPLETED trips start -> end
 *      checkIn(45,"Leyton",3)  checkIn(27,"Leyton",10)
 *      checkOut(45,"Waterloo",15)  checkOut(27,"Waterloo",20)
 *      getAverageTime("Leyton","Waterloo") = (12 + 10) / 2 = 11.0
 *
 *  ---------------------------------------------------------------------------
 *  TWO DIFFERENT QUESTIONS -> TWO MAPS
 *    active: id -> (station, time)          "where and when did this person get on?"
 *    stats:  (start, end) -> {total, trips}  "what do we know about this route?"
 *  checkOut joins them: look up (and remove) the check-in, add the duration to the
 *  route's running total. The average is total / trips - no list of trips is kept.
 *
 *  ROUTE KEY: a record Route(from, to). Records get equals/hashCode for free, so they
 *  work as map keys. A string key like from + to is a trap: "ab"+"c" == "a"+"bc".
 *
 *  DRY RUN (the example)
 *    checkIn 45 Leyton@3, 27 Leyton@10        active {45:(Leyton,3), 27:(Leyton,10)}
 *    checkOut 45 Waterloo@15 -> 12             stats {Leyton->Waterloo: 12, 1}
 *    checkOut 27 Waterloo@20 -> 10             stats {Leyton->Waterloo: 22, 2}
 *    average 22 / 2 = 11.0
 *
 *  COMPLEXITY   O(1) average per call   Space O(active passengers + routes)
 * ============================================================================
 */

import java.util.*;

public class A17_DesignUndergroundSystem {

    record Route(String from, String to) {}
    record CheckIn(String station, int time) {}

    // ---------------- Approach 1: keep every trip, average by scanning (the referee) ----------------
    static class ListUndergroundSystem {
        record Trip(String from, String to, int duration) {}
        private final Map<Integer, CheckIn> active = new HashMap<>();
        private final List<Trip> trips = new ArrayList<>();

        public void checkIn(int id, String station, int t) { active.put(id, new CheckIn(station, t)); }
        public void checkOut(int id, String station, int t) {
            CheckIn in = active.remove(id);
            trips.add(new Trip(in.station(), station, t - in.time()));
        }
        public double getAverageTime(String start, String end) {
            long total = 0, count = 0;
            for (Trip trip : trips) if (trip.from().equals(start) && trip.to().equals(end)) { total += trip.duration(); count++; }
            return (double) total / count;
        }
    }

    // ---------------- Approach 2: two HashMaps with running totals (main solution) ----------------
    static class UndergroundSystem {
        private final Map<Integer, CheckIn> active = new HashMap<>();
        private final Map<Route, long[]> stats = new HashMap<>();          // route -> {total time, trips}

        public void checkIn(int id, String station, int t) { active.put(id, new CheckIn(station, t)); }

        public void checkOut(int id, String station, int t) {
            CheckIn in = active.remove(id);                                 // the trip is over: forget the check-in
            long[] s = stats.computeIfAbsent(new Route(in.station(), station), k -> new long[2]);
            s[0] += t - in.time();
            s[1]++;
        }

        public double getAverageTime(String start, String end) {
            long[] s = stats.get(new Route(start, end));
            return (double) s[0] / s[1];
        }
    }

    public static void main(String[] args) {
        UndergroundSystem u = new UndergroundSystem();
        u.checkIn(45, "Leyton", 3); u.checkIn(32, "Paradise", 8); u.checkIn(27, "Leyton", 10);
        u.checkOut(45, "Waterloo", 15); u.checkOut(27, "Waterloo", 20); u.checkOut(32, "Cambridge", 22);
        check("Paradise -> Cambridge", 14.0, u.getAverageTime("Paradise", "Cambridge"));
        check("Leyton -> Waterloo", 11.0, u.getAverageTime("Leyton", "Waterloo"));
        u.checkIn(10, "Leyton", 24);
        check("an open trip doesn't count", 11.0, u.getAverageTime("Leyton", "Waterloo"));
        u.checkOut(10, "Waterloo", 38);
        check("after it completes", 12.0, u.getAverageTime("Leyton", "Waterloo"));
        u.checkIn(1, "ab", 0); u.checkOut(1, "c", 5); u.checkIn(2, "a", 0); u.checkOut(2, "bc", 9);
        check("record keys: ab->c is not a->bc", 5.0, u.getAverageTime("ab", "c"));

        Random rnd = new Random(17);
        String[] stations = {"A", "B", "C", "D"};
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            UndergroundSystem main = new UndergroundSystem();
            ListUndergroundSystem ref = new ListUndergroundSystem();
            Set<Integer> travelling = new HashSet<>();
            Set<String> doneRoutes = new HashSet<>();
            int time = 0;
            for (int i = 0; i < 200; i++) {
                time += 1 + rnd.nextInt(5);
                int id = rnd.nextInt(10);
                String st = stations[rnd.nextInt(4)];
                if (!travelling.contains(id)) { main.checkIn(id, st, time); ref.checkIn(id, st, time); travelling.add(id); }
                else { main.checkOut(id, st, time); ref.checkOut(id, st, time); travelling.remove(id); }
                String a = stations[rnd.nextInt(4)], b = stations[rnd.nextInt(4)];
                if (doneRoutes.contains(a + ">" + b)) ok &= Math.abs(main.getAverageTime(a, b) - ref.getAverageTime(a, b)) < 1e-9;
                doneRoutes.clear();
                for (String x : stations) for (String y : stations)
                    if (ref.trips.stream().anyMatch(tr -> tr.from().equals(x) && tr.to().equals(y))) doneRoutes.add(x + ">" + y);
            }
        }
        check("60,000 random ops vs scanning all trips", true, ok);
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
