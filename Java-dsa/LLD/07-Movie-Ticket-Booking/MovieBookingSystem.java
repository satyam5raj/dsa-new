/*
 * ============================================================================
 *  LLD 07 : MOVIE TICKET BOOKING (BookMyShow)  - the template for ALL booking systems
 * ============================================================================
 *  Run:  java MovieBookingSystem.java          (design notes: README.md, incl. hotel / airline)
 *
 *     [catalog]   Movie, Cinema, Screen, Seat, SeatType, Show
 *     [inventory] ShowSeat: per-show seat state (AVAILABLE / HELD by user until t / BOOKED),
 *                 changed ONLY by compare-and-set
 *     [booking]   Booking + BookingStatus state machine (PENDING_PAYMENT -> CONFIRMED / EXPIRED / CANCELLED)
 *     [policy]    PricingStrategy (+ WeekendSurcharge decorator), RefundPolicy
 *     [payment]   PaymentGateway interface (idempotency key = booking id)
 *     [service]   BookingService: search, seat map, hold -> pay -> confirm, cancel
 *     [demo]      happy path, hold expiry, payment failure, refunds, and a 200-user race
 *
 *  THE CORE PROBLEM: two people pick seat F7 at the same moment.
 *    Standard answer = TWO PHASES:
 *      1. HOLD   the seats for a few minutes (soft lock with expiry) - while the user pays
 *      2. CONFIRM after payment, only if the hold is still THEIRS and NOT expired
 *    Holding many seats must be ALL-OR-NOTHING: either you get all 4 seats for your family
 *    or none (you don't want 3 of them).
 *
 *  HOW: each ShowSeat holds an immutable SeatState in an AtomicReference.
 *    hold = for seats in SORTED order: CAS(available-or-expired -> HELD(me, until))
 *           if any CAS fails -> roll back the ones I already took -> fail.
 *    No locks, no deadlocks (sorted order + rollback), and expired holds are reclaimed
 *    lazily by the next CAS - no cleanup thread is required for correctness.
 * ============================================================================
 */

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class MovieBookingSystem {

    // =====================================================================
    // [catalog] mostly immutable reference data
    // =====================================================================

    record Movie(String id, String title, int durationMin, String language) {}
    enum SeatType { REGULAR, PREMIUM, RECLINER }
    record Seat(String id, int row, int number, SeatType type) {}                 // physical seat in a screen
    record Screen(String id, String name, List<Seat> seats) {}
    record Cinema(String id, String name, String city, List<Screen> screens) {}

    /** One screening. Owns the per-show inventory (an AGGREGATE root for its seats). */
    static class Show {
        final String id;
        final Movie movie;
        final Cinema cinema;
        final Screen screen;
        final LocalDateTime start;
        final Map<String, ShowSeat> seats = new LinkedHashMap<>();                  // seatId -> per-show state

        Show(String id, Movie movie, Cinema cinema, Screen screen, LocalDateTime start) {
            this.id = id; this.movie = movie; this.cinema = cinema; this.screen = screen; this.start = start;
            for (Seat s : screen.seats()) seats.put(s.id(), new ShowSeat(s));
        }
    }

    // =====================================================================
    // [inventory]
    // =====================================================================

    enum SeatStatus { AVAILABLE, HELD, BOOKED }

    /** Immutable snapshot of a seat's state. A new object for every change -> safe for CAS. */
    record SeatState(SeatStatus status, String userId, long holdUntil, String bookingId) {
        static final SeatState FREE = new SeatState(SeatStatus.AVAILABLE, null, 0, null);
        boolean isFreeAt(long now) { return status == SeatStatus.AVAILABLE || (status == SeatStatus.HELD && now >= holdUntil); }
    }

    static class ShowSeat {
        final Seat seat;
        final AtomicReference<SeatState> state = new AtomicReference<>(SeatState.FREE);
        ShowSeat(Seat seat) { this.seat = seat; }

        /** AVAILABLE (or an EXPIRED hold) -> HELD by user. Fails if someone else holds/booked it. */
        boolean tryHold(String userId, long now, long until) {
            SeatState cur = state.get();
            return cur.isFreeAt(now) && state.compareAndSet(cur, new SeatState(SeatStatus.HELD, userId, until, null));
        }

        /** Undo only OUR hold (never someone else's). */
        void release(String userId) {
            SeatState cur = state.get();
            if (cur.status() == SeatStatus.HELD && userId.equals(cur.userId())) state.compareAndSet(cur, SeatState.FREE);
        }

        /** HELD by user and not expired -> BOOKED. */
        boolean tryBook(String userId, long now, String bookingId) {
            SeatState cur = state.get();
            return cur.status() == SeatStatus.HELD && userId.equals(cur.userId()) && now < cur.holdUntil()
                    && state.compareAndSet(cur, new SeatState(SeatStatus.BOOKED, userId, 0, bookingId));
        }

        void cancelBooking(String bookingId) {
            SeatState cur = state.get();
            if (cur.status() == SeatStatus.BOOKED && bookingId.equals(cur.bookingId())) state.compareAndSet(cur, SeatState.FREE);
        }
    }

    // =====================================================================
    // [booking] state machine
    // =====================================================================

    enum BookingStatus {
        PENDING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED, PAYMENT_FAILED;
        /** Legal transitions in ONE place. Anything else is a bug and throws. */
        boolean canGoTo(BookingStatus next) {
            return switch (this) {
                case PENDING_PAYMENT -> next == CONFIRMED || next == EXPIRED || next == PAYMENT_FAILED;
                case CONFIRMED -> next == CANCELLED;
                default -> false;                                                   // terminal states
            };
        }
    }

    static class Booking {
        final String id, userId;
        final Show show;
        final List<String> seatIds;
        final long amountPaise;
        final long holdUntil;
        private BookingStatus status = BookingStatus.PENDING_PAYMENT;
        String paymentRef;

        Booking(String id, String userId, Show show, List<String> seatIds, long amountPaise, long holdUntil) {
            this.id = id; this.userId = userId; this.show = show; this.seatIds = List.copyOf(seatIds); this.amountPaise = amountPaise; this.holdUntil = holdUntil;
        }

        synchronized void transitionTo(BookingStatus next) {
            if (!status.canGoTo(next)) throw new IllegalStateException("Booking " + id + ": " + status + " -> " + next + " not allowed");
            status = next;
        }
        synchronized BookingStatus status() { return status; }
    }

    // =====================================================================
    // [policy] pricing + refunds
    // =====================================================================

    interface PricingStrategy { long pricePaise(Show show, Seat seat); }

    static class SeatTypePricing implements PricingStrategy {
        private final Map<SeatType, Long> base;
        SeatTypePricing(Map<SeatType, Long> base) { this.base = new EnumMap<>(base); }
        public long pricePaise(Show show, Seat seat) { return base.get(seat.type()); }
    }

    /** DECORATOR: adds a weekend surcharge on top of ANY pricing strategy. Stackable. */
    static class WeekendSurcharge implements PricingStrategy {
        private final PricingStrategy inner; private final int percent;
        WeekendSurcharge(PricingStrategy inner, int percent) { this.inner = inner; this.percent = percent; }
        public long pricePaise(Show show, Seat seat) {
            long p = inner.pricePaise(show, seat);
            DayOfWeek d = show.start.getDayOfWeek();
            return (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) ? p + p * percent / 100 : p;
        }
    }

    interface RefundPolicy { long refundPaise(Booking b, LocalDateTime cancelAt); }

    /** >24h before: 100%, 2-24h: 50%, <2h: nothing. */
    static class TieredRefund implements RefundPolicy {
        public long refundPaise(Booking b, LocalDateTime at) {
            long hours = Duration.between(at, b.show.start).toHours();
            if (hours >= 24) return b.amountPaise;
            if (hours >= 2) return b.amountPaise / 2;
            return 0;
        }
    }

    // =====================================================================
    // [payment]
    // =====================================================================

    interface PaymentGateway {
        /** Must be idempotent on `idempotencyKey`: a retry never charges twice. */
        Optional<String> charge(String idempotencyKey, String userId, long amountPaise);
        void refund(String paymentRef, long amountPaise);
    }

    static class FakeGateway implements PaymentGateway {
        final Map<String, String> charges = new ConcurrentHashMap<>();
        final List<String> refunds = new CopyOnWriteArrayList<>();
        final Set<String> failingUsers = ConcurrentHashMap.newKeySet();
        public Optional<String> charge(String key, String user, long amount) {
            if (failingUsers.contains(user)) return Optional.empty();
            return Optional.of(charges.computeIfAbsent(key, k -> "pay-" + k));     // idempotent
        }
        public void refund(String ref, long amount) { refunds.add(ref + ":" + amount); }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    static class BookingException extends RuntimeException { BookingException(String m) { super(m); } }

    static class BookingService {
        static final long HOLD_MS = 5 * 60_000;
        static final int MAX_SEATS = 10;

        private final Map<String, Show> shows = new ConcurrentHashMap<>();
        private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
        private final PricingStrategy pricing;
        private final RefundPolicy refunds;
        private final PaymentGateway gateway;
        private final LongSupplier clock;                          // epoch ms
        private final Function<Long, LocalDateTime> toDateTime;
        private final List<Consumer<Booking>> onConfirmed = new CopyOnWriteArrayList<>();
        private final AtomicLong seq = new AtomicLong();

        BookingService(PricingStrategy pricing, RefundPolicy refunds, PaymentGateway gateway, LongSupplier clock, Function<Long, LocalDateTime> toDateTime) {
            this.pricing = pricing; this.refunds = refunds; this.gateway = gateway; this.clock = clock; this.toDateTime = toDateTime;
        }

        void addShow(Show s) { shows.put(s.id, s); }
        void onConfirmed(Consumer<Booking> l) { onConfirmed.add(l); }

        List<Show> search(String city, String movieTitle) {
            return shows.values().stream().filter(s -> s.cinema.city().equalsIgnoreCase(city) && s.movie.title().equalsIgnoreCase(movieTitle))
                    .sorted(Comparator.comparing(s -> s.start)).toList();
        }

        /** What the UI renders: seat id -> AVAILABLE/HELD/BOOKED (expired holds shown as available). */
        Map<String, SeatStatus> seatMap(String showId) {
            long now = clock.getAsLong();
            Map<String, SeatStatus> m = new LinkedHashMap<>();
            show(showId).seats.forEach((id, ss) -> { SeatState st = ss.state.get(); m.put(id, st.isFreeAt(now) ? SeatStatus.AVAILABLE : st.status()); });
            return m;
        }

        /** PHASE 1: hold all seats or none, create a PENDING booking. */
        Booking holdSeats(String userId, String showId, List<String> seatIds) {
            if (seatIds.isEmpty() || seatIds.size() > MAX_SEATS) throw new BookingException("Choose 1-" + MAX_SEATS + " seats");
            Show show = show(showId);
            if (toDateTime.apply(clock.getAsLong()).isAfter(show.start)) throw new BookingException("Show already started");
            List<String> sorted = seatIds.stream().distinct().sorted().toList();     // a consistent order
            long now = clock.getAsLong(), until = now + HOLD_MS;
            List<ShowSeat> taken = new ArrayList<>();
            for (String id : sorted) {
                ShowSeat ss = show.seats.get(id);
                if (ss == null) { rollback(taken, userId); throw new BookingException("No seat " + id); }
                if (!ss.tryHold(userId, now, until)) { rollback(taken, userId); throw new BookingException("Seat " + id + " is not available"); }
                taken.add(ss);
            }
            long amount = taken.stream().mapToLong(ss -> pricing.pricePaise(show, ss.seat)).sum();
            Booking b = new Booking("B" + seq.incrementAndGet(), userId, show, sorted, amount, until);
            bookings.put(b.id, b);
            return b;
        }

        private static void rollback(List<ShowSeat> taken, String userId) { taken.forEach(s -> s.release(userId)); }

        /** PHASE 2: pay, then turn holds into bookings - only if the holds are still ours. */
        Booking pay(String bookingId) {
            Booking b = booking(bookingId);
            if (b.status() != BookingStatus.PENDING_PAYMENT) throw new BookingException("Booking is " + b.status());
            long now = clock.getAsLong();
            if (now >= b.holdUntil) { expire(b); throw new BookingException("Hold expired, please select seats again"); }

            Optional<String> ref = gateway.charge(b.id, b.userId, b.amountPaise);      // idempotency key = booking id
            if (ref.isEmpty()) {
                b.transitionTo(BookingStatus.PAYMENT_FAILED);
                b.seatIds.forEach(id -> b.show.seats.get(id).release(b.userId));
                throw new BookingException("Payment declined");
            }
            b.paymentRef = ref.get();
            // Convert every hold to BOOKED. If the hold expired DURING payment (slow bank),
            // someone else may have the seat now -> refund and fail cleanly.
            List<ShowSeat> booked = new ArrayList<>();
            for (String id : b.seatIds) {
                ShowSeat ss = b.show.seats.get(id);
                if (!ss.tryBook(b.userId, clock.getAsLong(), b.id)) {
                    booked.forEach(s -> s.cancelBooking(b.id));
                    b.seatIds.forEach(x -> b.show.seats.get(x).release(b.userId));
                    gateway.refund(b.paymentRef, b.amountPaise);
                    b.transitionTo(BookingStatus.EXPIRED);
                    throw new BookingException("Hold expired during payment; refunded");
                }
                booked.add(ss);
            }
            b.transitionTo(BookingStatus.CONFIRMED);
            onConfirmed.forEach(l -> l.accept(b));                                  // ticket email, SMS, analytics
            return b;
        }

        long cancel(String bookingId, String userId) {
            Booking b = booking(bookingId);
            if (!b.userId.equals(userId)) throw new BookingException("Not your booking");
            b.transitionTo(BookingStatus.CANCELLED);                               // throws unless CONFIRMED
            long refund = refunds.refundPaise(b, toDateTime.apply(clock.getAsLong()));
            if (refund > 0) gateway.refund(b.paymentRef, refund);
            b.seatIds.forEach(id -> b.show.seats.get(id).cancelBooking(b.id));     // seats go back on sale
            return refund;
        }

        private void expire(Booking b) {
            if (b.status() == BookingStatus.PENDING_PAYMENT) {
                b.transitionTo(BookingStatus.EXPIRED);
                b.seatIds.forEach(id -> b.show.seats.get(id).release(b.userId));
            }
        }

        private Show show(String id) { Show s = shows.get(id); if (s == null) throw new BookingException("No show " + id); return s; }
        private Booking booking(String id) { Booking b = bookings.get(id); if (b == null) throw new BookingException("No booking " + id); return b; }
        Collection<Booking> allBookings() { return bookings.values(); }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static Screen screen(String id, int rows, int perRow) {
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int n = 1; n <= perRow; n++) {
                SeatType t = r < 2 ? SeatType.REGULAR : r < rows - 1 ? SeatType.PREMIUM : SeatType.RECLINER;
                seats.add(new Seat("" + (char) ('A' + r) + n, r, n, t));
            }
        return new Screen(id, "Audi " + id, seats);
    }

    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong(0);
        LocalDateTime epoch = LocalDateTime.of(2026, 10, 9, 10, 0);                // a Friday 10:00
        Function<Long, LocalDateTime> toTime = ms -> epoch.plusNanos(ms * 1_000_000);
        FakeGateway gw = new FakeGateway();
        PricingStrategy pricing = new WeekendSurcharge(new SeatTypePricing(Map.of(SeatType.REGULAR, 20_000L, SeatType.PREMIUM, 30_000L, SeatType.RECLINER, 50_000L)), 20);
        BookingService svc = new BookingService(pricing, new TieredRefund(), gw, clock::get, toTime);
        List<String> tickets = new CopyOnWriteArrayList<>();
        svc.onConfirmed(b -> tickets.add(b.userId + " " + b.seatIds));

        Movie movie = new Movie("m1", "Interstellar", 169, "EN");
        Cinema pvr = new Cinema("c1", "PVR Forum", "Bangalore", List.of(screen("1", 4, 5)));
        Show friday = new Show("s1", movie, pvr, pvr.screens().get(0), epoch.plusHours(36));   // Saturday 22:00
        svc.addShow(friday);

        System.out.println("1. Search, hold, pay (happy path)");
        check("search", 1, svc.search("bangalore", "interstellar").size());
        Booking b1 = svc.holdSeats("alice", "s1", List.of("C2", "C1"));
        check("seats sorted, held", "[C1, C2]", b1.seatIds);
        check("price: 2 premium x Rs 300 + 20% weekend", 72_000L, b1.amountPaise);
        check("seat map shows HELD", SeatStatus.HELD, svc.seatMap("s1").get("C1"));
        expect("bob can't take a held seat", () -> svc.holdSeats("bob", "s1", List.of("C2", "C3")));
        check("all-or-nothing: bob didn't keep C3", SeatStatus.AVAILABLE, svc.seatMap("s1").get("C3"));
        svc.pay(b1.id);
        check("confirmed", BookingStatus.CONFIRMED, b1.status());
        check("seat BOOKED", SeatStatus.BOOKED, svc.seatMap("s1").get("C1"));
        check("ticket sent (observer)", "[alice [C1, C2]]", tickets);
        expect("can't pay twice", () -> svc.pay(b1.id));

        System.out.println("2. Hold expiry: abandoned checkout frees the seat");
        Booking b2 = svc.holdSeats("carol", "s1", List.of("D5"));
        clock.addAndGet(BookingService.HOLD_MS);                                   // 5 minutes pass
        check("expired hold shows AVAILABLE", SeatStatus.AVAILABLE, svc.seatMap("s1").get("D5"));
        Booking b3 = svc.holdSeats("dave", "s1", List.of("D5"));                   // reclaimed by CAS, no cleanup job
        expect("carol's late payment is rejected", () -> svc.pay(b2.id));
        check("carol's booking EXPIRED", BookingStatus.EXPIRED, b2.status());
        svc.pay(b3.id);
        check("dave got D5", BookingStatus.CONFIRMED, b3.status());

        System.out.println("3. Payment failure releases the seats");
        gw.failingUsers.add("erin");
        Booking b4 = svc.holdSeats("erin", "s1", List.of("A1", "A2"));
        expect("declined", () -> svc.pay(b4.id));
        check("status PAYMENT_FAILED", BookingStatus.PAYMENT_FAILED, b4.status());
        check("A1 free again", SeatStatus.AVAILABLE, svc.seatMap("s1").get("A1"));

        System.out.println("4. Cancellation with a tiered refund");
        long refund = svc.cancel(b1.id, "alice");                                  // ~36h before the show
        check("100% refund (>24h before)", 72_000L, refund);
        check("seats back on sale", SeatStatus.AVAILABLE, svc.seatMap("s1").get("C1"));
        expect("cancel twice", () -> svc.cancel(b1.id, "alice"));
        clock.set(Duration.ofHours(33).toMillis());                                // 3h before the show
        long r3 = svc.cancel(b3.id, "dave");
        check("50% refund (2-24h before)", b3.amountPaise / 2, r3);
        expect("illegal transition CANCELLED -> CONFIRMED", () -> b3.transitionTo(BookingStatus.CONFIRMED));

        System.out.println("5. Concurrency: 200 users grab random 1-4 seat groups in a 20-seat show");
        Show hot = new Show("s2", movie, pvr, pvr.screens().get(0), epoch.plusDays(3));
        svc.addShow(hot);
        List<String> allSeats = new ArrayList<>(hot.seats.keySet());
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger confirmed = new AtomicInteger();
        for (int u = 0; u < 200; u++) {
            String user = "u" + u;
            pool.submit(() -> {
                go.await();
                ThreadLocalRandom r = ThreadLocalRandom.current();
                List<String> want = new ArrayList<>();
                for (int k = 0; k < 1 + r.nextInt(4); k++) want.add(allSeats.get(r.nextInt(allSeats.size())));
                try { Booking b = svc.holdSeats(user, "s2", want); svc.pay(b.id); confirmed.incrementAndGet(); }
                catch (BookingException ignored) { }
                return null;
            });
        }
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        Map<String, Long> seatOwners = new HashMap<>();
        for (Booking b : svc.allBookings()) if (b.show == hot && b.status() == BookingStatus.CONFIRMED) for (String s : b.seatIds) seatOwners.merge(s, 1L, Long::sum);
        check("no seat in two confirmed bookings", true, seatOwners.values().stream().allMatch(c -> c == 1));
        check("every BOOKED seat belongs to a confirmed booking", seatOwners.size(),
                (int) hot.seats.values().stream().filter(s -> s.state.get().status() == SeatStatus.BOOKED).count());
        check("no seat left HELD (all-or-nothing rollback worked)", 0L, hot.seats.values().stream().filter(s -> s.state.get().status() == SeatStatus.HELD).count());
        System.out.println("   " + confirmed.get() + " bookings confirmed, " + seatOwners.size() + "/20 seats sold, zero double-booking");
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); System.out.println("         (" + e.getMessage() + ")"); }
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
