/*
 * ============================================================================
 *  LLD 01 : PARKING LOT SYSTEM
 * ============================================================================
 *  Run:  java ParkingLotSystem.java          (design notes: README.md in this folder)
 *
 *  The file is organised like packages in a real project:
 *     [model]     Vehicle, VehicleType, SpotType, ParkingSpot, ParkingFloor, Ticket, Receipt
 *     [strategy]  SpotAllocationStrategy (where to park), PricingStrategy (how much to pay)
 *     [observer]  ParkingEventListener -> DisplayBoard
 *     [service]   ParkingLot (facade) + its Builder
 *     [demo]      main(): scenarios + a concurrency test
 *
 *  KEY DESIGN DECISIONS (each explained where it appears)
 *   - Vehicle is DATA (a record + enum), not a class hierarchy: Car/Bike/Truck don't
 *     BEHAVE differently, they only differ in which spots fit. -> no Car extends Vehicle.
 *   - "Which spot?" and "how much?" change independently -> two Strategy interfaces.
 *   - The contended resource is a SPOT: two entry gates must never assign the same one.
 *     Each floor keeps a lock-free queue of free spots per type; poll() hands a spot to
 *     exactly one thread, and the spot itself double-checks with a CAS.
 *   - Time is injected (LongSupplier clock) so fees are testable.
 *   - Money is long paise, never double.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class ParkingLotSystem {

    // =====================================================================
    // [model]
    // =====================================================================

    enum SpotType { MOTORCYCLE, COMPACT, LARGE, EV }

    /**
     * Each vehicle type lists the spot types it can use, IN PREFERENCE ORDER.
     * This one table replaces a class hierarchy plus if/else chains: adding a VAN is one line.
     * (Open/Closed in practice: the allocation code never changes.)
     */
    enum VehicleType {
        MOTORCYCLE(SpotType.MOTORCYCLE, SpotType.COMPACT, SpotType.LARGE),   // a bike can take a bigger spot if needed
        CAR(SpotType.COMPACT, SpotType.LARGE),
        EV_CAR(SpotType.EV, SpotType.COMPACT, SpotType.LARGE),               // prefers a charging spot
        TRUCK(SpotType.LARGE);

        final List<SpotType> allowedSpots;
        VehicleType(SpotType... allowed) { this.allowedSpots = List.of(allowed); }
    }

    /** Value object: immutable, equal by value. A record gives equals/hashCode/toString for free. */
    record Vehicle(String plate, VehicleType type) {
        Vehicle {
            Objects.requireNonNull(plate);
            Objects.requireNonNull(type);
        }
    }

    /**
     * ENTITY with identity (id) and a lifecycle (free <-> occupied).
     * The invariant "at most one vehicle per spot" lives HERE, enforced by a CAS:
     * even if two threads somehow got the same spot, only one compareAndSet succeeds.
     */
    static class ParkingSpot {
        final String id;
        final int floor;
        final SpotType type;
        private final AtomicReference<Vehicle> occupant = new AtomicReference<>();

        ParkingSpot(String id, int floor, SpotType type) { this.id = id; this.floor = floor; this.type = type; }

        boolean tryOccupy(Vehicle v) { return occupant.compareAndSet(null, v); }   // atomic: free -> taken
        void release() { occupant.set(null); }
        boolean isFree() { return occupant.get() == null; }
        Vehicle occupant() { return occupant.get(); }
        @Override public String toString() { return id; }
    }

    /**
     * A floor owns its spots (an AGGREGATE: nobody touches a spot except through its floor).
     * Free spots per type sit in a ConcurrentLinkedDeque: poll()/offer() are lock-free and
     * thread-safe, so many gates can allocate at once without a global lock.
     */
    static class ParkingFloor {
        final int number;
        private final Map<SpotType, Deque<ParkingSpot>> free = new EnumMap<>(SpotType.class);
        private final List<ParkingSpot> all = new ArrayList<>();

        ParkingFloor(int number, Map<SpotType, Integer> counts) {
            this.number = number;
            for (SpotType t : SpotType.values()) free.put(t, new ConcurrentLinkedDeque<>());
            counts.forEach((type, n) -> {
                for (int i = 1; i <= n; i++) {
                    ParkingSpot s = new ParkingSpot("F" + number + "-" + type.name().charAt(0) + i, number, type);
                    all.add(s);
                    free.get(type).addLast(s);
                }
            });
        }

        /** Hands out a free spot of this type to exactly ONE caller, or empty. */
        Optional<ParkingSpot> takeFree(SpotType type, Vehicle v) {
            Deque<ParkingSpot> q = free.get(type);
            ParkingSpot s;
            while ((s = q.pollFirst()) != null) {                // poll is atomic: two threads never get the same spot
                if (s.tryOccupy(v)) return Optional.of(s);        // defensive second check (CAS)
            }
            return Optional.empty();
        }

        void giveBack(ParkingSpot s) {
            s.release();
            free.get(s.type).addFirst(s);                         // re-use near spots first
        }

        int freeCount(SpotType t) { return free.get(t).size(); }
        List<ParkingSpot> spots() { return Collections.unmodifiableList(all); }
    }

    /** ACTIVE -> PAYING -> PAID, and PAYING -> ACTIVE if the payment fails. */
    enum TicketStatus { ACTIVE, PAYING, PAID }

    /** Ticket = the record of one stay. Status changes are CASes, so only one exit gate can work on it. */
    static class Ticket {
        final String id;
        final Vehicle vehicle;
        final ParkingSpot spot;
        final long entryTime;
        private final AtomicReference<TicketStatus> status = new AtomicReference<>(TicketStatus.ACTIVE);

        Ticket(String id, Vehicle vehicle, ParkingSpot spot, long entryTime) {
            this.id = id; this.vehicle = vehicle; this.spot = spot; this.entryTime = entryTime;
        }

        /**
         * CLAIM the ticket BEFORE charging money. If we charged first and claimed after,
         * two exit gates scanning the same ticket could BOTH charge the customer, and only
         * then would one of them lose the race. Claim -> charge -> commit (or roll back).
         */
        boolean startPayment() { return status.compareAndSet(TicketStatus.ACTIVE, TicketStatus.PAYING); }
        void paymentFailed() { status.set(TicketStatus.ACTIVE); }
        void paymentSucceeded() { status.set(TicketStatus.PAID); }
        TicketStatus status() { return status.get(); }
    }

    record Receipt(String ticketId, String plate, long durationMinutes, long amountPaise) {}

    // =====================================================================
    // [strategy] WHERE to park
    // =====================================================================

    /** Strategy #1: the allocation policy. Swappable without touching ParkingLot. */
    interface SpotAllocationStrategy {
        Optional<ParkingSpot> allocate(List<ParkingFloor> floors, Vehicle v);
    }

    /** Lowest floor first, and within a floor the vehicle's preferred spot type first. */
    static class NearestFirstAllocation implements SpotAllocationStrategy {
        public Optional<ParkingSpot> allocate(List<ParkingFloor> floors, Vehicle v) {
            for (ParkingFloor f : floors)
                for (SpotType t : v.type().allowedSpots) {
                    Optional<ParkingSpot> s = f.takeFree(t, v);
                    if (s.isPresent()) return s;
                }
            return Optional.empty();
        }
    }

    /** Preferred spot type across ALL floors before falling back (keeps big spots for trucks). */
    static class BestFitAllocation implements SpotAllocationStrategy {
        public Optional<ParkingSpot> allocate(List<ParkingFloor> floors, Vehicle v) {
            for (SpotType t : v.type().allowedSpots)
                for (ParkingFloor f : floors) {
                    Optional<ParkingSpot> s = f.takeFree(t, v);
                    if (s.isPresent()) return s;
                }
            return Optional.empty();
        }
    }

    // =====================================================================
    // [strategy] HOW MUCH to pay
    // =====================================================================

    interface PricingStrategy {
        long feePaise(Ticket ticket, long exitTime);
    }

    /** Per-started-hour rate that depends on the spot type (a truck spot costs more). */
    static class HourlyPricing implements PricingStrategy {
        private final Map<SpotType, Long> ratePerHourPaise;
        HourlyPricing(Map<SpotType, Long> rates) { this.ratePerHourPaise = new EnumMap<>(rates); }

        public long feePaise(Ticket t, long exitTime) {
            long minutes = Math.max(1, (exitTime - t.entryTime + 59_999) / 60_000);   // round UP to whole minutes
            long hours = (minutes + 59) / 60;                                         // every STARTED hour is charged
            return hours * ratePerHourPaise.get(t.spot.type);
        }
    }

    /** First hour flat, then a cheaper hourly rate, with a daily cap: a typical mall rule. */
    static class FlatThenHourlyPricing implements PricingStrategy {
        private final long firstHour, perHour, dailyCap;
        FlatThenHourlyPricing(long firstHour, long perHour, long dailyCap) { this.firstHour = firstHour; this.perHour = perHour; this.dailyCap = dailyCap; }

        public long feePaise(Ticket t, long exitTime) {
            long hours = Math.max(1, (exitTime - t.entryTime + 3_599_999) / 3_600_000);
            long days = (hours - 1) / 24, restHours = hours - days * 24;
            long restFee = Math.min(dailyCap, firstHour + (restHours - 1) * perHour);
            return days * dailyCap + restFee;
        }
    }

    // =====================================================================
    // [observer] events for display boards, analytics, ...
    // =====================================================================

    interface ParkingEventListener {
        void onParked(Ticket t);
        void onLeft(Ticket t, Receipt r);
    }

    /** Shows free spots per type. It LISTENS; the lot doesn't know what a display is. */
    static class DisplayBoard implements ParkingEventListener {
        private final ParkingLot lot;
        final List<String> log = new CopyOnWriteArrayList<>();
        DisplayBoard(ParkingLot lot) { this.lot = lot; }
        public void onParked(Ticket t) { log.add("IN  " + t.vehicle.plate() + " -> " + t.spot + "  free: " + lot.freeSummary()); }
        public void onLeft(Ticket t, Receipt r) { log.add("OUT " + t.vehicle.plate() + " paid " + r.amountPaise() / 100 + "  free: " + lot.freeSummary()); }
    }

    // =====================================================================
    // [service] the FACADE: what entry/exit gates call
    // =====================================================================

    static class ParkingFullException extends RuntimeException {
        ParkingFullException(Vehicle v) { super("No spot for " + v); }
    }

    static class ParkingLot {
        private final List<ParkingFloor> floors;
        private final SpotAllocationStrategy allocation;
        private final PricingStrategy pricing;
        private final LongSupplier clock;
        private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
        private final Set<String> platesInside = ConcurrentHashMap.newKeySet();
        private final List<ParkingEventListener> listeners = new CopyOnWriteArrayList<>();
        private final AtomicLong ticketSeq = new AtomicLong();

        private ParkingLot(Builder b) {
            this.floors = List.copyOf(b.floors);
            this.allocation = b.allocation;
            this.pricing = b.pricing;
            this.clock = b.clock;
        }

        /** Entry gate: find a spot, issue a ticket. Thread-safe; many gates call it at once. */
        Ticket park(Vehicle v) {
            if (!platesInside.add(v.plate()))                        // atomic add = "is this car already inside?"
                throw new IllegalStateException(v.plate() + " is already parked");
            ParkingSpot spot = allocation.allocate(floors, v).orElse(null);
            if (spot == null) {
                platesInside.remove(v.plate());                       // undo the reservation of the plate
                throw new ParkingFullException(v);
            }
            Ticket t = new Ticket("T" + ticketSeq.incrementAndGet(), v, spot, clock.getAsLong());
            activeTickets.put(t.id, t);
            listeners.forEach(l -> l.onParked(t));
            return t;
        }

        /** Exit gate: compute the fee, take payment, free the spot. */
        Receipt unpark(String ticketId, Predicate<Long> paymentProcessor) {
            Ticket t = activeTickets.get(ticketId);
            if (t == null) throw new IllegalArgumentException("Unknown or already closed ticket " + ticketId);
            if (!t.startPayment())                                    // another gate is already processing it
                throw new IllegalStateException("Ticket " + ticketId + " is being processed");
            long now = clock.getAsLong();
            long fee = pricing.feePaise(t, now);
            if (!paymentProcessor.test(fee)) {
                t.paymentFailed();                                    // roll back: the car stays, can retry
                throw new IllegalStateException("Payment failed for " + ticketId);
            }
            t.paymentSucceeded();
            activeTickets.remove(ticketId);
            floors.get(t.spot.floor).giveBack(t.spot);
            platesInside.remove(t.vehicle.plate());
            Receipt r = new Receipt(t.id, t.vehicle.plate(), (now - t.entryTime) / 60_000, fee);
            listeners.forEach(l -> l.onLeft(t, r));
            return r;
        }

        void addListener(ParkingEventListener l) { listeners.add(l); }

        Map<SpotType, Integer> freeSummary() {
            Map<SpotType, Integer> m = new EnumMap<>(SpotType.class);
            for (SpotType t : SpotType.values()) m.put(t, floors.stream().mapToInt(f -> f.freeCount(t)).sum());
            return m;
        }

        int activeCount() { return activeTickets.size(); }
        List<ParkingFloor> floors() { return floors; }

        /**
         * BUILDER: a lot has a layout plus optional strategies with sensible defaults.
         * Construction is validated in one place, and the built object is effectively immutable
         * in its configuration.
         */
        static class Builder {
            private final List<ParkingFloor> floors = new ArrayList<>();
            private SpotAllocationStrategy allocation = new NearestFirstAllocation();
            private PricingStrategy pricing = new HourlyPricing(Map.of(SpotType.MOTORCYCLE, 1000L, SpotType.COMPACT, 3000L, SpotType.LARGE, 5000L, SpotType.EV, 4000L));
            private LongSupplier clock = System::currentTimeMillis;

            Builder floor(Map<SpotType, Integer> counts) { floors.add(new ParkingFloor(floors.size(), counts)); return this; }
            Builder allocation(SpotAllocationStrategy s) { this.allocation = Objects.requireNonNull(s); return this; }
            Builder pricing(PricingStrategy p) { this.pricing = Objects.requireNonNull(p); return this; }
            Builder clock(LongSupplier c) { this.clock = Objects.requireNonNull(c); return this; }
            ParkingLot build() {
                if (floors.isEmpty()) throw new IllegalStateException("A parking lot needs at least one floor");
                return new ParkingLot(this);
            }
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static class FakeClock implements LongSupplier {
        long now = 0;
        public long getAsLong() { return now; }
        void plusMinutes(long m) { now += m * 60_000; }
    }

    public static void main(String[] args) throws Exception {
        FakeClock clock = new FakeClock();
        ParkingLot lot = new ParkingLot.Builder()
                .floor(Map.of(SpotType.MOTORCYCLE, 1, SpotType.COMPACT, 2, SpotType.LARGE, 1, SpotType.EV, 1))
                .floor(Map.of(SpotType.COMPACT, 2, SpotType.LARGE, 1))
                .clock(clock)
                .build();
        DisplayBoard board = new DisplayBoard(lot);
        lot.addListener(board);

        System.out.println("1. Parking and spot preferences");
        Ticket bike = lot.park(new Vehicle("KA-01-B1", VehicleType.MOTORCYCLE));
        check("bike -> motorcycle spot", SpotType.MOTORCYCLE, bike.spot.type);
        Ticket bike2 = lot.park(new Vehicle("KA-01-B2", VehicleType.MOTORCYCLE));
        check("2nd bike falls back to compact", SpotType.COMPACT, bike2.spot.type);
        Ticket ev = lot.park(new Vehicle("KA-01-E1", VehicleType.EV_CAR));
        check("EV gets the charging spot", SpotType.EV, ev.spot.type);
        Ticket truck = lot.park(new Vehicle("KA-01-T1", VehicleType.TRUCK));
        check("truck -> large on floor 0", "F0-L1", truck.spot.id);
        Ticket truck2 = lot.park(new Vehicle("KA-01-T2", VehicleType.TRUCK));
        check("2nd truck -> floor 1", 1, truck2.spot.floor);
        expectThrows("3rd truck: lot full for trucks", ParkingFullException.class, () -> lot.park(new Vehicle("KA-01-T3", VehicleType.TRUCK)));
        expectThrows("same plate twice", IllegalStateException.class, () -> lot.park(new Vehicle("KA-01-T1", VehicleType.TRUCK)));

        System.out.println("2. Exit and pricing (hourly; every started hour is charged)");
        clock.plusMinutes(61);                                                    // 1h01m -> 2 hours
        Receipt r = lot.unpark(truck.id, amount -> true);
        check("truck 61 min = 2h x Rs 50", 10000L, r.amountPaise());
        expectThrows("same ticket can't exit twice", IllegalArgumentException.class, () -> lot.unpark(truck.id, a -> true));
        expectThrows("failed payment keeps the car in", IllegalStateException.class, () -> lot.unpark(bike.id, a -> false));
        check("bike still active after failed payment", TicketStatus.ACTIVE, bike.status());
        Ticket truck3 = lot.park(new Vehicle("KA-01-T3", VehicleType.TRUCK));
        check("freed large spot is reused", "F0-L1", truck3.spot.id);

        System.out.println("3. Swapping the pricing strategy (Open/Closed)");
        PricingStrategy mall = new FlatThenHourlyPricing(4000, 2000, 20000);     // Rs 40 first hour, Rs 20/h, Rs 200/day cap
        Ticket fake = new Ticket("X", new Vehicle("P", VehicleType.CAR), new ParkingSpot("S", 0, SpotType.COMPACT), 0);
        check("mall: 30 min", 4000L, mall.feePaise(fake, 30 * 60_000));
        check("mall: 3h", 8000L, mall.feePaise(fake, 3 * 3_600_000));
        check("mall: 20h capped", 20000L, mall.feePaise(fake, 20 * 3_600_000L));
        check("mall: 25h = 1 day + 1h", 24000L, mall.feePaise(fake, 25 * 3_600_000L));

        System.out.println("4. Display board (observer) saw every event");
        board.log.forEach(line -> System.out.println("   " + line));
        check("events logged", 7, board.log.size());

        System.out.println("5. Concurrency: 8 gates park 200 cars into 100 compact spots at once");
        ParkingLot big = new ParkingLot.Builder().floor(Map.of(SpotType.COMPACT, 50)).floor(Map.of(SpotType.COMPACT, 50)).clock(clock).build();
        ExecutorService gates = Executors.newFixedThreadPool(8);
        List<Future<Ticket>> futures = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 200; i++) {
            int n = i;
            futures.add(gates.submit(() -> {
                start.await();
                try { return big.park(new Vehicle("CAR-" + n, VehicleType.CAR)); }
                catch (ParkingFullException e) { return null; }
            }));
        }
        start.countDown();
        Set<String> spotsUsed = new HashSet<>();
        int parked = 0;
        for (Future<Ticket> f : futures) { Ticket t = f.get(); if (t != null) { parked++; spotsUsed.add(t.spot.id); } }
        gates.shutdown();
        check("exactly 100 parked", 100, parked);
        check("no spot given twice", 100, spotsUsed.size());
        check("every spot really occupied", true, big.floors().stream().flatMap(f -> f.spots().stream()).noneMatch(ParkingSpot::isFree));

        System.out.println("6. Concurrency: 2 exit gates scan the SAME ticket at once");
        Ticket one = futures.get(0).get() != null ? futures.get(0).get() : futures.get(1).get();
        AtomicInteger charges = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        Runnable exit = () -> {
            try { go.await(); big.unpark(one.id, amount -> { charges.incrementAndGet(); ConcurrencyHelp.sleep(5); return true; }); }
            catch (Exception ignored) { }
        };
        Thread g1 = new Thread(exit), g2 = new Thread(exit);
        g1.start(); g2.start(); go.countDown(); g1.join(); g2.join();
        check("customer charged exactly once", 1, charges.get());
        check("ticket closed", TicketStatus.PAID, one.status());
        summary();
    }

    static class ConcurrencyHelp { static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } } }

    static void expectThrows(String label, Class<? extends Throwable> type, Runnable r) {
        try { r.run(); check(label, type.getSimpleName(), "no exception"); }
        catch (Throwable t) { check(label, type.getSimpleName(), t.getClass().getSimpleName()); }
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
