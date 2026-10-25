# 07 · Movie Ticket Booking (explained simply)

| | |
|---|---|
| **Code** | [MovieBookingSystem.java](MovieBookingSystem.java) |
| **Run** | `java MovieBookingSystem.java` (25 self-checks, including a 200-user race for 20 seats) |
| **Design notes** | [README.md](README.md) (how the same design fits hotels and airlines, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This is **the template for every booking system** (movies, hotels, flights, doctor appointments). The heart of it: two people pick seat F7 at the same moment, and the answer is **hold → pay → confirm**, with holds that expire and are all-or-nothing.

---

## 1. The problem in plain words

Users search for a movie in their city, pick a **show** (a screening at a time), see the **seat map**, choose a few seats, **pay**, and get a ticket. They can **cancel** later and get a refund depending on how early they cancel.

The hard parts:
- Two users choosing the same seat at the same moment must not both get it.
- A user who picks seats and then wanders off must not block those seats forever.
- A family booking 4 seats wants **all 4 or none**, never 3.
- A payment can fail, or take so long that the hold runs out.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Can users choose specific seats? | Yes, from a seat map |
| How long can seats be held while paying? | 5 minutes |
| Several seats per booking? | 1 to 10, **all or nothing** |
| Pricing? | By seat type, plus a weekend surcharge; **rules change** |
| Refunds? | Tiered by how long before the show you cancel |
| Payments? | Through a gateway interface; retries must never charge twice |
| Many users at once? | Yes, very (a blockbuster's opening night) |

### Step 2 · Requirements
1. Search shows by city and movie.
2. Show the seat map (AVAILABLE / HELD / BOOKED).
3. **Hold** seats for 5 minutes, all-or-nothing.
4. **Pay**; on success the holds become **bookings**; on failure the seats are released.
5. **Cancel** with a refund rule; the seats go back on sale.
6. Never double-book; never charge twice; abandoned holds free themselves.

### Step 3 · Entities

| Noun | Becomes | Why |
|---|---|---|
| movie | `record Movie` | reference data |
| cinema, screen, seat | `record Cinema`, `Screen`, `Seat` | the physical layout, which doesn't change |
| seat type | `enum SeatType { REGULAR, PREMIUM, RECLINER }` | |
| show | `class Show` | one screening; it **owns** the per-show seat states |
| a seat **for one show** | `class ShowSeat` | the same physical seat is free in the 6 pm show and booked in the 9 pm show |
| seat state | `record SeatState(status, userId, holdUntil, bookingId)` | an immutable snapshot, changed by CAS |
| booking | `class Booking` with `enum BookingStatus` | a state machine |
| pricing, refunds | `PricingStrategy`, `RefundPolicy` | rules that change |
| payment | `interface PaymentGateway` | an external system |
| the service | `class BookingService` | search, hold, pay, cancel |

**The key modelling idea:** a `Seat` (physical, in a screen) is different from a `ShowSeat` (that seat **in one particular show**). It's the same idea as Book vs BookCopy in [the library](../03-Library-Management/LibraryManagementSystem.md#step-3--entities-the-1-insight).

### Step 4 · Relationships

```
Cinema ── has ──> Screen ── has ──> Seat            (physical, fixed)
Show (movie + screen + time) ── owns ──> ShowSeat per Seat   (the per-show inventory)
Booking ── refers to ──> Show + list of seat ids
BookingService ── uses ──> PricingStrategy, RefundPolicy, PaymentGateway
```

### Step 5 · API and the main flow

```java
List<Show> search(String city, String movieTitle)
Map<String, SeatStatus> seatMap(String showId)
Booking holdSeats(String userId, String showId, List<String> seatIds)   // phase 1
Booking pay(String bookingId)                                           // phase 2
long cancel(String bookingId, String userId)                            // returns the refund
```

```
pick seats ──holdSeats──> seats HELD (5 min), booking PENDING_PAYMENT
     ──pay──> charge the card ──> seats BOOKED, booking CONFIRMED ──> ticket sent
         └── payment declined ──> seats released, booking PAYMENT_FAILED
         └── hold already expired ──> booking EXPIRED
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| seat prices | **Strategy** (`PricingStrategy`) |
| adding a weekend surcharge on top of any pricing | **Decorator** (`WeekendSurcharge`) |
| refund rules | **Strategy** (`RefundPolicy`) |
| booking lifecycle | **State machine** (`BookingStatus.canGoTo`) |
| ticket emails, SMS, analytics on confirmation | **Observer** (`onConfirmed` listeners) |
| the payment provider | **Adapter / interface** (`PaymentGateway`) |

### Step 7 · Concurrency
The contended resource is **one seat in one show**. The protection is a CAS on its state, taken in **sorted order** with **rollback** for all-or-nothing (section 7).

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` with a static constant | `SeatState.FREE` | a shared, immutable "free" state |
| `AtomicReference<SeatState>` + `compareAndSet` | every seat change | "change it only if it's still exactly what I saw" |
| immutable state objects | `SeatState` | every change creates a **new** object, which is what makes CAS safe |
| `enum` with a method | `BookingStatus.canGoTo(next)` | the allowed transitions live in one place |
| `synchronized` methods | `Booking.transitionTo`, `status` | one thread at a time changes a booking's status |
| `LocalDateTime`, `Duration`, `DayOfWeek` | show times, refunds, weekend pricing | |
| `Function<Long, LocalDateTime>` | converts the clock's millis to a date-time | so the fake clock also controls dates |
| `Consumer<Booking>` | confirmation listeners | "a function that takes a booking and does something" |
| `stream().distinct().sorted()` | seat ids | remove duplicates and sort, for a consistent order |
| `computeIfAbsent` in the fake gateway | idempotent charge | the same key returns the same charge |

---

## 4. The code, piece by piece

### 4.1 The per-show seat and its state

```java
enum SeatStatus { AVAILABLE, HELD, BOOKED }

record SeatState(SeatStatus status, String userId, long holdUntil, String bookingId) {
    static final SeatState FREE = new SeatState(SeatStatus.AVAILABLE, null, 0, null);
    boolean isFreeAt(long now) { return status == SeatStatus.AVAILABLE || (status == SeatStatus.HELD && now >= holdUntil); }
}

static class ShowSeat {
    final Seat seat;
    final AtomicReference<SeatState> state = new AtomicReference<>(SeatState.FREE);

    boolean tryHold(String userId, long now, long until) {
        SeatState cur = state.get();
        return cur.isFreeAt(now) && state.compareAndSet(cur, new SeatState(SeatStatus.HELD, userId, until, null));
    }

    void release(String userId) {
        SeatState cur = state.get();
        if (cur.status() == SeatStatus.HELD && userId.equals(cur.userId())) state.compareAndSet(cur, SeatState.FREE);
    }

    boolean tryBook(String userId, long now, String bookingId) {
        SeatState cur = state.get();
        return cur.status() == SeatStatus.HELD && userId.equals(cur.userId()) && now < cur.holdUntil()
                && state.compareAndSet(cur, new SeatState(SeatStatus.BOOKED, userId, 0, bookingId));
    }
}
```

| Method | Plain meaning |
|---|---|
| `isFreeAt(now)` | AVAILABLE, **or** HELD but the hold has run out |
| `tryHold` | if it's free, swap in "HELD by me until X", but only if nobody changed it since I looked |
| `release` | undo **my** hold only, never someone else's |
| `tryBook` | turn **my unexpired** hold into BOOKED |

```
AVAILABLE ──tryHold──> HELD(user, until) ──tryBook──> BOOKED(bookingId)
    ^                      │   (or expires: counts as free)    │
    └──────release─────────┘                                    │
    └──────────────────────cancelBooking────────────────────────┘
```

**Expired holds are reclaimed lazily:** there's no cleanup job. An expired hold simply counts as "free" in `isFreeAt`, so the next person's `tryHold` CAS takes it over.

### 4.2 The booking state machine

```java
enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED, PAYMENT_FAILED;
    boolean canGoTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == EXPIRED || next == PAYMENT_FAILED;
            case CONFIRMED -> next == CANCELLED;
            default -> false;                                                   // terminal states
        };
    }
}

synchronized void transitionTo(BookingStatus next) {
    if (!status.canGoTo(next)) throw new IllegalStateException("Booking " + id + ": " + status + " -> " + next + " not allowed");
    status = next;
}
```

```
PENDING_PAYMENT ──> CONFIRMED ──> CANCELLED
       ├──────────> EXPIRED
       └──────────> PAYMENT_FAILED
```

All the legal moves are written in **one** place. Anything else ("cancel a cancelled booking", "confirm a cancelled booking") throws.

### 4.3 Pricing: a Strategy wrapped by a Decorator

```java
interface PricingStrategy { long pricePaise(Show show, Seat seat); }

static class SeatTypePricing implements PricingStrategy {
    public long pricePaise(Show show, Seat seat) { return base.get(seat.type()); }
}

/** DECORATOR: adds a weekend surcharge on top of ANY pricing strategy. Stackable. */
static class WeekendSurcharge implements PricingStrategy {
    private final PricingStrategy inner; private final int percent;
    public long pricePaise(Show show, Seat seat) {
        long p = inner.pricePaise(show, seat);
        DayOfWeek d = show.start.getDayOfWeek();
        return (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) ? p + p * percent / 100 : p;
    }
}
```

**Decorator, simply:** a gift box around a gift. `WeekendSurcharge` **wraps** another pricing strategy, asks it for the price, then adds 20% on weekends. You can stack more boxes (a festival surcharge, a member discount) in any combination, without changing the inner classes.

```java
PricingStrategy pricing = new WeekendSurcharge(new SeatTypePricing(Map.of(...)), 20);
```

### 4.4 Refunds

```java
static class TieredRefund implements RefundPolicy {
    public long refundPaise(Booking b, LocalDateTime at) {
        long hours = Duration.between(at, b.show.start).toHours();
        if (hours >= 24) return b.amountPaise;
        if (hours >= 2) return b.amountPaise / 2;
        return 0;
    }
}
```

| When you cancel | Refund |
|---|---|
| 24 h or more before the show | 100% |
| 2–24 h before | 50% |
| under 2 h | nothing |

### 4.5 Phase 1: hold all seats, or none

```java
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
```

In plain words:
1. Validate (1–10 seats, the show hasn't started).
2. **Sort** the seat ids (and remove duplicates).
3. Try to hold each seat with a CAS. If **any** fails, **give back** the ones already taken, and fail.
4. Price the seats and create a PENDING_PAYMENT booking.

**Why sort?** If Alice wants {C2, C1} and Bob wants {C1, C2}, both try C1 first. Whoever wins C1 continues; the other fails immediately. Without a consistent order, Alice could take C2 while Bob takes C1, and **both** would fail (each holding half). Sorting makes "one of them wins" much more likely.

### 4.6 Phase 2: pay, then confirm

```java
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
```

| Case | What happens |
|---|---|
| the hold has already expired | booking → EXPIRED, seats released |
| payment declined | booking → PAYMENT_FAILED, seats released |
| payment OK, but the hold expired **during** payment (a slow bank) | undo any seats booked so far, **refund**, booking → EXPIRED |
| payment OK, all holds still ours | seats → BOOKED, booking → CONFIRMED, listeners notified |

**Idempotency:** the booking id is passed as the idempotency key, so if the network times out and `pay` is retried, the gateway returns the same charge instead of charging twice ([tool 4](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-4--idempotency-keys--a-retry-must-not-charge-twice)).

### 4.7 Cancel

```java
long cancel(String bookingId, String userId) {
    Booking b = booking(bookingId);
    if (!b.userId.equals(userId)) throw new BookingException("Not your booking");
    b.transitionTo(BookingStatus.CANCELLED);                               // throws unless CONFIRMED
    long refund = refunds.refundPaise(b, toDateTime.apply(clock.getAsLong()));
    if (refund > 0) gateway.refund(b.paymentRef, refund);
    b.seatIds.forEach(id -> b.show.seats.get(id).cancelBooking(b.id));     // seats go back on sale
    return refund;
}
```

---

## 5. Dry run (from the real output)

Screen: 4 rows × 5 seats. Rows A and B are regular (₹200), row C is premium (₹300), row D is recliner (₹500). The show is on Saturday at 22:00, so there's a 20% weekend surcharge. "Now" is Friday 10:00.

| Step | What happens | Result |
|---|---|---|
| search Bangalore / Interstellar | | 1 show |
| Alice holds C2, C1 | sorted to [C1, C2]; 2 × ₹300 + 20% | ₹720 (72,000 paise), HELD |
| Bob holds C2, C3 | C2 is held by Alice → fails, **C3 rolled back** | C3 still AVAILABLE |
| Alice pays | | CONFIRMED, seats BOOKED, ticket listener fired |
| Alice pays again | | "Booking is CONFIRMED" |
| Carol holds D5, then 5 minutes pass | | the seat map shows D5 AVAILABLE |
| Dave holds D5 | the expired hold is reclaimed by CAS (no cleanup job) | ✓ |
| Carol pays late | | "Hold expired", booking EXPIRED |
| Dave pays | | CONFIRMED |
| Erin's card is declined | | PAYMENT_FAILED, A1 and A2 free again |
| Alice cancels (about 36 h before) | ≥ 24 h | 100% refund (₹720), seats back on sale |
| Alice cancels again | CANCELLED → CANCELLED | not allowed |
| the clock moves to 3 h before; Dave cancels | 2–24 h | 50%: ₹300 of ₹600 |
| force CANCELLED → CONFIRMED | | not allowed |

**The race:** 200 users grab random groups of 1–4 seats in a 20-seat show at once. Result (it varies per run; in this run 12 bookings, 19 seats sold): no seat in two confirmed bookings, every BOOKED seat belongs to a confirmed booking, and **no seat left HELD** (every failed attempt rolled back).

---

## 6. Hotels and airlines use the same design

| | Movie | Hotel | Airline |
|---|---|---|---|
| the inventory item | a seat in a show | a room type on a date | a seat on a flight |
| the hold | seat for 5 minutes | room-nights for 15 minutes | seat during checkout |
| all-or-nothing | several seats | several nights | several passengers / legs |

The details differ (hotels usually sell a room **type** with a count rather than a specific room), but **hold with expiry → pay with an idempotency key → confirm** is the same.

---

## 7. Concurrency, explained simply

**The danger:** 200 people tap seats at the same instant.

**The protection, in three ideas:**
1. **CAS on an immutable state** per seat. Each seat's state is swapped only if nobody changed it in between. No locks, so no deadlocks and no waiting.
2. **Sorted order + rollback** for several seats. Take seats in a fixed order; on any failure, give back what you took. That's all-or-nothing without holding locks.
3. **Expiring holds checked lazily.** An abandoned hold just "counts as free" after its time, so the next CAS takes it.

Plus: `Booking.transitionTo` is `synchronized`, so the booking's own status can't be changed by two threads at once, and an idempotency key protects the payment.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** 0 or more than 10 seats, duplicate seat ids, a seat that doesn't exist, a show that has started, paying twice, paying after the hold expired, the hold expiring during payment, a declined card, cancelling someone else's booking, cancelling twice.

**Common mistakes:**
1. One `Seat` object for every show (you can't have the 6 pm seat free and the 9 pm seat booked).
2. "Check available, then book" in two steps (double booking).
3. Holding some seats and not rolling back when another fails.
4. Holds that never expire (a user who walks away blocks seats forever).
5. A cleanup thread being **required** for correctness (here it's optional; lazy expiry is enough).
6. Retrying payment without an idempotency key (double charge).
7. Status changes scattered around the code instead of one transition table.

---

## 9. Interview questions and answers

**Q1. How do you prevent two users booking the same seat?**
> Each show-seat has an immutable state in an `AtomicReference`. Holding and booking are compare-and-set operations, so only one of two concurrent requests can win. In a database: `UPDATE show_seat SET status='HELD', user=?, hold_until=? WHERE show_id=? AND seat_id=? AND (status='AVAILABLE' OR hold_until < now())` and check the affected row count.

**Q2. Why hold, then pay, then confirm?**
> Payment takes time. A short hold gives the user exclusive access while paying, and the expiry means abandoned checkouts free the seats automatically.

**Q3. How do you make a multi-seat hold all-or-nothing?**
> Try to hold each seat in sorted order; if any fails, release the ones already held and fail. Sorting reduces the chance that two users each hold part of the other's group.

**Q4. What if the hold expires while the payment is in progress?**
> When converting holds to bookings, `tryBook` checks the hold is still ours and unexpired. If not, undo any partial bookings, refund the payment, and mark the booking EXPIRED.

**Q5. How do you stop double charging on retries?**
> Use the booking id as the idempotency key with the payment gateway, which returns the same result for repeated requests.

**Q6. Why is the weekend surcharge a decorator?**
> It adds behaviour on top of any pricing strategy, and decorators can be stacked (surcharges, discounts) without changing the base strategy or creating a class for every combination.

**Q7. How do expired holds get cleaned up?**
> Lazily: `isFreeAt` treats an expired hold as free, so the next `tryHold` reclaims it. A background sweeper is optional (to keep the seat map tidy), not needed for correctness.

### Follow-up questions

**F1. "Make it work with a database and many servers."**
> Seat state lives in a `show_seat` table; hold and book are conditional updates (or `SELECT … FOR UPDATE` on the chosen rows, locked in sorted order). A unique constraint on (show_id, seat_id) in the bookings table is a final safety net.

**F2. "Millions of users for a blockbuster's opening."**
> Put a virtual waiting room in front (admit users in batches), cache the seat map with short TTLs, and keep the hold/book path on the database's conditional updates. Shard by show.

**F3. "Add coupons and offers."**
> More decorators around the pricing strategy (or a separate discount step), applied when the booking is created.

**F4. "Let users choose 'best available' instead of specific seats."**
> A seat-selection strategy that picks contiguous seats near the centre, then calls the same `holdSeats`.

**F5. "Send the ticket by email and SMS."**
> More `onConfirmed` listeners. In production, publish a "booking confirmed" event and let a notification service consume it ([15 · Notification System](../15-Notification-System/NotificationSystem.md)).

**F6. "Hotel booking: the same room type over 3 nights."**
> The inventory becomes "available count per room type per date". Holding means decrementing each of the 3 dates atomically (all-or-nothing), with the same expiry and rollback.

---

## 10. Running the file

```bash
java MovieBookingSystem.java      # 25/25 tests passed (the 200-user race prints how many seats were sold)
```

**One-line summary:** *"Model per-show seats (ShowSeat) with immutable states changed only by CAS; hold all chosen seats in sorted order with rollback (all-or-nothing) and an expiry that's reclaimed lazily; pay with the booking id as the idempotency key; then convert holds to bookings, refunding if a hold expired mid-payment; and keep the booking lifecycle in one transition table."*
