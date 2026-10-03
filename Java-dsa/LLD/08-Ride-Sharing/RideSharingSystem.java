/*
 * ============================================================================
 *  LLD 08 : RIDE-SHARING SYSTEM (Uber / Ola)
 * ============================================================================
 *  Run:  java RideSharingSystem.java          (design notes: README.md)
 *
 *     [geo]       Location (haversine distance), GridIndex: drivers bucketed into ~1 km cells
 *                 so "drivers near me" checks a few cells instead of every driver
 *     [model]     Rider, Driver (status changed by CAS), VehicleType, Trip + TripStatus state machine
 *     [matching]  MatchingStrategy: NearestDriverMatching, BestRatedNearbyMatching
 *     [pricing]   FareStrategy: StandardFare, SurgeFare (decorator, demand/supply in the cell)
 *     [service]   RideService: request -> offer to candidates -> assign -> arrive -> start -> complete,
 *                 cancel (with fee), rate; TripListener observers
 *     [demo]      matching, grid vs brute force, surge, state machine, cancellation, a race
 *
 *  KEY IDEAS
 *   - Location updates are the HOTTEST write path (every driver every ~4s). The grid index
 *     is a ConcurrentHashMap<cell, Set<driverId>>: an update moves the id between two cells.
 *   - Matching = find candidates nearby (index) -> rank (strategy) -> OFFER one by one
 *     (the driver may decline) -> ASSIGN atomically (driver AVAILABLE -> ON_TRIP by CAS).
 *     If another rider grabbed the driver first, the CAS fails and we try the next candidate.
 *   - Trip is a state machine; every transition goes through one validated method.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class RideSharingSystem {

    // =====================================================================
    // [geo]
    // =====================================================================

    record Location(double lat, double lon) {
        /** Great-circle distance in km (haversine formula). */
        double distanceKm(Location o) {
            double R = 6371, dLat = Math.toRadians(o.lat - lat), dLon = Math.toRadians(o.lon - lon);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(o.lat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            return 2 * R * Math.asin(Math.sqrt(a));
        }
    }

    /**
     * A uniform grid over lat/lon: cell = (floor(lat / size), floor(lon / size)).
     * 0.01 degrees is ~1.1 km. A query scans rings of cells around the rider until it has
     * enough candidates or hits the radius limit.  (Production: geohash / S2 / H3 cells -
     * same idea, better-shaped cells, and they're strings that work as Redis/DB keys.)
     */
    static class GridIndex {
        record Cell(long x, long y) {}
        private final double size;
        private final ConcurrentHashMap<Cell, Set<String>> cells = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Cell> cellOf = new ConcurrentHashMap<>();

        GridIndex(double cellSizeDeg) { this.size = cellSizeDeg; }

        Cell cellFor(Location l) { return new Cell((long) Math.floor(l.lat() / size), (long) Math.floor(l.lon() / size)); }

        /** Move a driver to a new cell. Old/new cell sets are concurrent, so many drivers update at once. */
        void update(String driverId, Location l) {
            Cell next = cellFor(l);
            Cell prev = cellOf.put(driverId, next);
            if (next.equals(prev)) return;                                      // same cell: nothing to move
            cells.computeIfAbsent(next, c -> ConcurrentHashMap.newKeySet()).add(driverId);
            if (prev != null) cells.getOrDefault(prev, Set.of()).remove(driverId);
        }

        void remove(String driverId) {
            Cell prev = cellOf.remove(driverId);
            if (prev != null) cells.getOrDefault(prev, Set.of()).remove(driverId);
        }

        /** Driver ids in rings 0..maxRings around the location, stopping once `enough` are found. */
        List<String> nearby(Location l, int maxRings, int enough) {
            Cell c = cellFor(l);
            List<String> out = new ArrayList<>();
            for (int ring = 0; ring <= maxRings && out.size() < enough; ring++)
                for (long dx = -ring; dx <= ring; dx++)
                    for (long dy = -ring; dy <= ring; dy++) {
                        if (Math.max(Math.abs(dx), Math.abs(dy)) != ring) continue;   // only the ring's border
                        out.addAll(cells.getOrDefault(new Cell(c.x() + dx, c.y() + dy), Set.of()));
                    }
            return out;
        }

        int countIn(Cell c) { return cells.getOrDefault(c, Set.of()).size(); }
    }

    // =====================================================================
    // [model]
    // =====================================================================

    enum VehicleType { BIKE, AUTO, MINI, SEDAN, SUV }

    enum DriverStatus { OFFLINE, AVAILABLE, ON_TRIP }

    static class Driver {
        final String id, name;
        final VehicleType vehicle;
        volatile Location location;
        final AtomicReference<DriverStatus> status = new AtomicReference<>(DriverStatus.OFFLINE);
        private final DoubleAdder ratingSum = new DoubleAdder();
        private final LongAdder ratingCount = new LongAdder();

        Driver(String id, String name, VehicleType vehicle, Location loc) { this.id = id; this.name = name; this.vehicle = vehicle; this.location = loc; }

        /** The atomic "this driver is mine" step. Only one trip can win. */
        boolean tryAssign() { return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP); }
        void free() { status.compareAndSet(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE); }
        void rate(int stars) { ratingSum.add(stars); ratingCount.increment(); }
        double rating() { long n = ratingCount.sum(); return n == 0 ? 5.0 : ratingSum.sum() / n; }   // new drivers start at 5
    }

    record Rider(String id, String name) {}

    enum TripStatus {
        REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED, NO_DRIVER_FOUND;
        private static final Map<TripStatus, Set<TripStatus>> NEXT = Map.of(
                REQUESTED, Set.of(DRIVER_ASSIGNED, CANCELLED, NO_DRIVER_FOUND),
                DRIVER_ASSIGNED, Set.of(DRIVER_ARRIVED, CANCELLED),
                DRIVER_ARRIVED, Set.of(IN_PROGRESS, CANCELLED),
                IN_PROGRESS, Set.of(COMPLETED));
        boolean canGoTo(TripStatus t) { return NEXT.getOrDefault(this, Set.of()).contains(t); }
    }

    static class Trip {
        final String id;
        final Rider rider;
        final Location pickup, drop;
        final VehicleType type;
        volatile Driver driver;
        volatile double surge = 1.0;
        long requestedAt, startedAt, endedAt;
        long farePaise;
        private TripStatus status = TripStatus.REQUESTED;

        Trip(String id, Rider r, Location pickup, Location drop, VehicleType type) { this.id = id; this.rider = r; this.pickup = pickup; this.drop = drop; this.type = type; }

        synchronized void transition(TripStatus next) {
            if (!status.canGoTo(next)) throw new IllegalStateException("Trip " + id + ": " + status + " -> " + next);
            status = next;
        }
        synchronized TripStatus status() { return status; }
    }

    // =====================================================================
    // [matching]
    // =====================================================================

    /** Ranks candidate drivers for a trip. The service does the atomic assignment. */
    interface MatchingStrategy { List<Driver> rank(Trip trip, List<Driver> candidates); }

    static class NearestDriverMatching implements MatchingStrategy {
        public List<Driver> rank(Trip t, List<Driver> c) {
            return c.stream().sorted(Comparator.comparingDouble(d -> d.location.distanceKm(t.pickup))).toList();
        }
    }

    /** Within maxKm, prefer higher rating, then distance. A different business goal, the same interface. */
    static class BestRatedNearbyMatching implements MatchingStrategy {
        private final double maxKm;
        BestRatedNearbyMatching(double maxKm) { this.maxKm = maxKm; }
        public List<Driver> rank(Trip t, List<Driver> c) {
            return c.stream().filter(d -> d.location.distanceKm(t.pickup) <= maxKm)
                    .sorted(Comparator.comparingDouble((Driver d) -> -d.rating()).thenComparingDouble(d -> d.location.distanceKm(t.pickup))).toList();
        }
    }

    // =====================================================================
    // [pricing]
    // =====================================================================

    interface FareStrategy { long farePaise(Trip trip, double km, double minutes); }

    static class StandardFare implements FareStrategy {
        private final Map<VehicleType, long[]> rates;                        // {base, perKm, perMin} paise
        StandardFare(Map<VehicleType, long[]> rates) { this.rates = rates; }
        public long farePaise(Trip t, double km, double min) {
            long[] r = rates.get(t.type);
            return r[0] + Math.round(km * r[1]) + Math.round(min * r[2]);
        }
    }

    /** DECORATOR: multiplies any fare by the surge captured when the trip was requested. */
    static class SurgeFare implements FareStrategy {
        private final FareStrategy inner;
        SurgeFare(FareStrategy inner) { this.inner = inner; }
        public long farePaise(Trip t, double km, double min) { return Math.round(inner.farePaise(t, km, min) * t.surge); }
    }

    /** Surge = demand / supply in the pickup cell, clamped to [1.0, 3.0], rounded to 0.1. */
    static double surgeFor(int openRequests, int freeDrivers) {
        double ratio = freeDrivers == 0 ? 3.0 : (double) openRequests / freeDrivers;
        return Math.round(Math.max(1.0, Math.min(3.0, ratio)) * 10) / 10.0;
    }

    // =====================================================================
    // [service]
    // =====================================================================

    interface TripListener { void onEvent(Trip trip, String event); }

    /** The driver app's answer to an offer. Injected so tests can make drivers decline. */
    interface OfferResponder { boolean accepts(Driver d, Trip t); }

    static class RideService {
        static final long CANCEL_FEE_PAISE = 5_000;
        private final Map<String, Driver> drivers = new ConcurrentHashMap<>();
        private final Map<String, Trip> trips = new ConcurrentHashMap<>();
        private final GridIndex index = new GridIndex(0.01);
        private final MatchingStrategy matching;
        private final FareStrategy fares;
        private final OfferResponder responder;
        private final LongSupplier clock;
        private final List<TripListener> listeners = new CopyOnWriteArrayList<>();
        private final ConcurrentHashMap<GridIndex.Cell, AtomicInteger> openRequests = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong();

        RideService(MatchingStrategy m, FareStrategy f, OfferResponder r, LongSupplier clock) { matching = m; fares = f; responder = r; this.clock = clock; }

        void addListener(TripListener l) { listeners.add(l); }
        private void emit(Trip t, String e) { listeners.forEach(l -> l.onEvent(t, e)); }

        // ---- driver side ----
        void registerDriver(Driver d) { drivers.put(d.id, d); }
        void goOnline(String id) { Driver d = drivers.get(id); d.status.set(DriverStatus.AVAILABLE); index.update(id, d.location); }
        void goOffline(String id) { Driver d = drivers.get(id); if (d.status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.OFFLINE)) index.remove(id); }
        void updateLocation(String id, Location l) { Driver d = drivers.get(id); d.location = l; if (d.status.get() == DriverStatus.AVAILABLE) index.update(id, l); }

        // ---- rider side ----
        Trip requestRide(Rider r, Location pickup, Location drop, VehicleType type) {
            Trip t = new Trip("T" + seq.incrementAndGet(), r, pickup, drop, type);
            t.requestedAt = clock.getAsLong();
            trips.put(t.id, t);
            GridIndex.Cell cell = index.cellFor(pickup);
            int demand = openRequests.computeIfAbsent(cell, c -> new AtomicInteger()).incrementAndGet();
            try {
                List<Driver> candidates = index.nearby(pickup, 3, 50).stream().map(drivers::get)
                        .filter(d -> d != null && d.vehicle == type && d.status.get() == DriverStatus.AVAILABLE).toList();
                t.surge = surgeFor(demand, candidates.size());                  // demand here vs supply nearby
                emit(t, "REQUESTED surge=" + t.surge);
                for (Driver d : matching.rank(t, candidates)) {
                    if (!responder.accepts(d, t)) { emit(t, "DECLINED by " + d.id); continue; }   // offer timed out / declined
                    if (!d.tryAssign()) continue;                                 // someone else got this driver first
                    index.remove(d.id);                                          // busy drivers aren't searchable
                    t.driver = d;
                    t.transition(TripStatus.DRIVER_ASSIGNED);
                    emit(t, "ASSIGNED " + d.id);
                    return t;
                }
                t.transition(TripStatus.NO_DRIVER_FOUND);
                emit(t, "NO_DRIVER_FOUND");
                return t;
            } finally {
                openRequests.get(cell).decrementAndGet();
            }
        }

        void driverArrived(String tripId) { Trip t = trip(tripId); t.transition(TripStatus.DRIVER_ARRIVED); emit(t, "ARRIVED"); }
        void startTrip(String tripId) { Trip t = trip(tripId); t.transition(TripStatus.IN_PROGRESS); t.startedAt = clock.getAsLong(); emit(t, "STARTED"); }

        long completeTrip(String tripId) {
            Trip t = trip(tripId);
            t.transition(TripStatus.COMPLETED);
            t.endedAt = clock.getAsLong();
            double km = t.pickup.distanceKm(t.drop);                              // production: the GPS trace length
            double minutes = (t.endedAt - t.startedAt) / 60_000.0;
            t.farePaise = fares.farePaise(t, km, minutes);
            releaseDriver(t.driver, t.drop);
            emit(t, "COMPLETED fare=" + t.farePaise);
            return t.farePaise;
        }

        /** Free before assignment; a fee once a driver is on the way. */
        long cancel(String tripId) {
            Trip t = trip(tripId);
            TripStatus before = t.status();
            t.transition(TripStatus.CANCELLED);
            if (t.driver != null) releaseDriver(t.driver, t.driver.location);
            long fee = before == TripStatus.REQUESTED ? 0 : CANCEL_FEE_PAISE;
            emit(t, "CANCELLED fee=" + fee);
            return fee;
        }

        void rateDriver(String tripId, int stars) {
            Trip t = trip(tripId);
            if (t.status() != TripStatus.COMPLETED) throw new IllegalStateException("Rate after completion");
            if (stars < 1 || stars > 5) throw new IllegalArgumentException("1-5 stars");
            t.driver.rate(stars);
        }

        private void releaseDriver(Driver d, Location at) { d.location = at; d.free(); index.update(d.id, at); }
        private Trip trip(String id) { Trip t = trips.get(id); if (t == null) throw new IllegalArgumentException("No trip " + id); return t; }
        GridIndex index() { return index; }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static final Location MG_ROAD = new Location(12.9750, 77.6060);
    static Location offset(Location l, double northKm, double eastKm) {
        return new Location(l.lat() + northKm / 111.0, l.lon() + eastKm / (111.0 * Math.cos(Math.toRadians(l.lat()))));
    }

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong();
        Map<VehicleType, long[]> rates = new EnumMap<>(VehicleType.class);
        for (VehicleType v : VehicleType.values()) rates.put(v, new long[]{5000, 1200, 200});    // Rs 50 + Rs 12/km + Rs 2/min
        FareStrategy fares = new SurgeFare(new StandardFare(rates));
        Set<String> decliners = ConcurrentHashMap.newKeySet();
        RideService svc = new RideService(new NearestDriverMatching(), fares, (d, t) -> !decliners.contains(d.id), clock::get);
        List<String> events = new CopyOnWriteArrayList<>();
        svc.addListener((t, e) -> events.add(t.id + " " + e));

        Driver near = new Driver("d-near", "Near", VehicleType.MINI, offset(MG_ROAD, 0.5, 0));
        Driver far = new Driver("d-far", "Far", VehicleType.MINI, offset(MG_ROAD, 2.5, 0));
        Driver suv = new Driver("d-suv", "Suv", VehicleType.SUV, offset(MG_ROAD, 0.1, 0));
        for (Driver d : List.of(near, far, suv)) { svc.registerDriver(d); svc.goOnline(d.id); }
        Rider asha = new Rider("asha", "Asha");

        System.out.println("1. Matching: nearest driver of the right vehicle type");
        Trip t1 = svc.requestRide(asha, MG_ROAD, offset(MG_ROAD, 0, 10), VehicleType.MINI);
        check("nearest MINI (SUV is closer but wrong type)", "d-near", t1.driver.id);
        check("driver is now ON_TRIP", DriverStatus.ON_TRIP, near.status.get());

        System.out.println("2. Trip lifecycle and fare");
        svc.driverArrived(t1.id);
        svc.startTrip(t1.id);
        clock.addAndGet(25 * 60_000);                                                // 25 minutes
        long fare = svc.completeTrip(t1.id);
        check("fare = 50 + 12/km x 10km + 2/min x 25", 22_000L, Math.round(fare / 100.0) * 100);
        check("driver free again", DriverStatus.AVAILABLE, near.status.get());
        expect("illegal transition COMPLETED -> STARTED", () -> svc.startTrip(t1.id));
        svc.rateDriver(t1.id, 4);
        check("rating updated", 4.0, near.rating());

        System.out.println("3. Declines: the offer moves to the next candidate");
        svc.updateLocation("d-near", offset(MG_ROAD, 0.3, 0));                     // near drives back downtown
        decliners.add("d-near");
        Trip t2 = svc.requestRide(asha, offset(MG_ROAD, 10, 0), offset(MG_ROAD, 0, 0), VehicleType.MINI);   // 10 km north: nobody there
        Trip t3 = svc.requestRide(new Rider("ravi", "Ravi"), MG_ROAD, offset(MG_ROAD, 3, 3), VehicleType.MINI);
        check("far driver took ravi (near declined)", "d-far", t3.driver == null ? "none" : t3.driver.id);
        check("asha's 2nd request: nobody within 3 rings", TripStatus.NO_DRIVER_FOUND, t2.status());
        decliners.clear();

        System.out.println("4. Cancellation fee depends on state");
        check("cancel after assignment: Rs 50", 5_000L, svc.cancel(t3.id));
        check("driver released", DriverStatus.AVAILABLE, far.status.get());
        expect("cancel twice", () -> svc.cancel(t3.id));

        System.out.println("5. Surge pricing (decorator) from demand/supply");
        check("1 request / 4 drivers -> 1.0", 1.0, surgeFor(1, 4));
        check("6 requests / 4 drivers -> 1.5", 1.5, surgeFor(6, 4));
        check("no drivers -> capped 3.0", 3.0, surgeFor(5, 0));
        Trip fake = new Trip("x", asha, MG_ROAD, MG_ROAD, VehicleType.MINI);
        fake.surge = 1.5;
        check("surge applies to the whole fare", 7_500L, fares.farePaise(fake, 0, 0));

        System.out.println("6. Grid index finds the same drivers as a brute-force scan");
        GridIndex g = new GridIndex(0.01);
        Random rnd = new Random(1);
        Map<String, Location> locs = new HashMap<>();
        for (int i = 0; i < 2000; i++) { Location l = offset(MG_ROAD, rnd.nextDouble() * 20 - 10, rnd.nextDouble() * 20 - 10); locs.put("d" + i, l); g.update("d" + i, l); }
        for (int i = 0; i < 2000; i += 2) { Location l = offset(locs.get("d" + i), rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5); locs.put("d" + i, l); g.update("d" + i, l); }   // drivers move
        Set<String> viaGrid = new HashSet<>(g.nearby(MG_ROAD, 1, Integer.MAX_VALUE));
        boolean ok = true;
        for (var e : locs.entrySet()) if (e.getValue().distanceKm(MG_ROAD) < 1.0) ok &= viaGrid.contains(e.getKey());   // 1 ring covers >= 1 cell (~1.1 km)
        check("every driver within 1 km is found", true, ok);
        System.out.println("   grid returned " + viaGrid.size() + " candidates instead of scanning 2,000");

        System.out.println("7. Concurrency: 100 riders, 30 drivers, all requesting at once");
        RideService race = new RideService(new NearestDriverMatching(), fares, (d, t) -> true, clock::get);
        for (int i = 0; i < 30; i++) { Driver d = new Driver("r" + i, "R" + i, VehicleType.AUTO, offset(MG_ROAD, rnd.nextDouble() * 2, rnd.nextDouble() * 2)); race.registerDriver(d); race.goOnline(d.id); }
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Trip>> fs = new ArrayList<>();
        for (int i = 0; i < 100; i++) { int n = i; fs.add(pool.submit(() -> { go.await(); return race.requestRide(new Rider("x" + n, "X"), offset(MG_ROAD, 1, 1), MG_ROAD, VehicleType.AUTO); })); }
        go.countDown();
        Map<String, Integer> tripsPerDriver = new HashMap<>();
        int assigned = 0;
        for (Future<Trip> f : fs) { Trip t = f.get(); if (t.driver != null) { assigned++; tripsPerDriver.merge(t.driver.id, 1, Integer::sum); } }
        pool.shutdown();
        check("exactly 30 riders matched", 30, assigned);
        check("no driver got two trips", true, tripsPerDriver.values().stream().allMatch(c -> c == 1));
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); }
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
