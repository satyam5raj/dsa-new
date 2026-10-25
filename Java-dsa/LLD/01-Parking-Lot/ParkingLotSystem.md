# 01 · Parking Lot System (explained simply)

| | |
|---|---|
| **Code** | [ParkingLotSystem.java](ParkingLotSystem.java) |
| **Run** | `java ParkingLotSystem.java` (22 self-checks, including two concurrency races) |
| **Design notes** | [README.md](README.md) (class diagram, sequence diagram, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This is the classic first LLD question. It tests whether you can turn a real place into clean classes, keep the rules in the right place, prepare for change (new vehicles, new prices), and handle "two gates at the same moment".

---

## 1. The problem in plain words

A building has several **floors**. Each floor has **spots** of different sizes: motorcycle, compact, large, and EV (with a charger). Cars come in through **entry gates** and leave through **exit gates**.

- At the **entry gate**: find a suitable free spot for the vehicle and give the driver a **ticket**.
- At the **exit gate**: read the ticket, work out the **fee**, take the **payment**, and free the spot.
- A **display board** shows how many spots of each type are free.

And it must stay correct when **many gates work at the same moment**: no spot is given to two cars, and no customer is charged twice.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify (ask before you design)

| Question to ask | Answer we'll assume |
|---|---|
| One building or many? | One lot, many floors |
| Which vehicle types? | Motorcycle, car, EV car, truck, **and it must be easy to add more** |
| Can a small vehicle use a bigger spot? | Yes, in order of preference (a bike prefers a motorcycle spot, then compact, then large) |
| Several gates at once? | **Yes**, so spot allocation must be thread-safe |
| How is the fee computed? | Per started hour, by spot type, **and pricing rules will change** |
| Payment methods (card, UPI, cash)? | Out of scope: hidden behind one simple "did the payment work?" check |
| Reservations, monthly passes, valet? | Out of scope (good extensions to discuss at the end) |

Saying "this is out of scope" out loud is a strong signal: you're controlling the size of the problem.

### Step 2 · Requirements

**It must (functional):**
1. Park a vehicle: find a suitable free spot, issue a ticket.
2. Unpark: compute the fee, take payment, free the spot.
3. Show free-spot counts per type.
4. Reject: the lot is full for that type; the same plate parked twice; the same ticket used twice.

**It must be (non-functional):**
- **Thread-safe**: no spot assigned twice, no customer charged twice.
- **Extensible**: new vehicle types, new pricing and new allocation rules without editing the core code.
- **Testable**: time is passed in, so we can "fast-forward" an hour in a test.

### Step 3 · Entities: nouns become classes

> A **vehicle** enters a **gate**, gets a free **spot** on a **floor**, and receives a **ticket**. At exit it pays a **fee** and gets a **receipt**. A **display board** shows free spots.

| Noun | Becomes | Why |
|---|---|---|
| vehicle | `record Vehicle(plate, type)` | just data: a plate and a type |
| vehicle type | `enum VehicleType` | a fixed list, each with its allowed spot types |
| spot | `class ParkingSpot` | has an identity and changes state (free ↔ taken) |
| spot type | `enum SpotType` | a fixed list |
| floor | `class ParkingFloor` | owns its spots and knows which are free |
| ticket | `class Ticket` | the record of one stay, with a status |
| receipt | `record Receipt` | the final, unchanging result |
| fee | not a class: a `long` | it's a computed number |
| display board | `class DisplayBoard` | listens for park/leave events |
| the lot itself | `class ParkingLot` | the one class the gates talk to |

**Verbs → methods:** park → `ParkingLot.park(vehicle)`; unpark/pay → `ParkingLot.unpark(ticketId, payment)`; free a spot → `ParkingFloor.giveBack(spot)`.

### Step 4 · Relationships: who has what, who owns which rule

```
ParkingLot ──has──> many ParkingFloor ──has──> many ParkingSpot
     │                                              (rule: one vehicle per spot)
     ├──uses──> SpotAllocationStrategy   (where to park)
     ├──uses──> PricingStrategy          (how much to pay)
     ├──keeps─> active Tickets           (rule: a ticket is paid once)
     └──tells─> ParkingEventListener(s)  (e.g. the DisplayBoard)
```

**The key decision: no class hierarchy for vehicles.** It's tempting to write `class Car extends Vehicle`, `class Truck extends Vehicle`. But cars and trucks don't **behave** differently here; they only differ in **which spots fit**. So one `record Vehicle` plus an enum that lists the allowed spots is simpler, and adding a VAN is one line. Interviewers like hearing **why you didn't subclass**.

### Step 5 · The public API and the main flows

```java
Ticket  park(Vehicle v)                                   // entry gate
Receipt unpark(String ticketId, Predicate<Long> payment)  // exit gate
```

**Park:** check the plate isn't already inside → the allocation strategy finds and claims a spot → create a ticket → tell the listeners.
**Unpark:** find the ticket → **claim** it (so no other gate can process it) → compute the fee → charge → free the spot → tell the listeners.

### Step 6 · Patterns: where will things change?

| What changes | Pattern | Why |
|---|---|---|
| **where** to park (nearest first, best fit, …) | **Strategy** (`SpotAllocationStrategy`) | the business may change the rule |
| **how much** to pay (hourly, mall-style, airport) | **Strategy** (`PricingStrategy`) | pricing changes often, independently of allocation |
| who wants to **know** when a car parks or leaves | **Observer** (`ParkingEventListener`) | display boards, analytics, alerts can be added without touching the lot |
| a lot with many settings and good defaults | **Builder** | clear, validated setup |

### Step 7 · Concurrency: name the contended resource
The contended resources are **a spot** (two entry gates must never assign the same one) and **a ticket** (two exit gates must never both charge it). Section 6 explains how each is protected.

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` | `Vehicle`, `Receipt` | a small immutable data class; Java writes the constructor, getters, `equals`, `hashCode`, `toString` for you |
| compact constructor | `Vehicle { Objects.requireNonNull(plate); … }` | validation that runs inside a record's constructor |
| `enum` with fields | `VehicleType(SpotType... allowed)` | each constant carries its own data (here, a list of allowed spots) |
| `EnumMap` | free spots by type | a fast map whose keys are an enum |
| `AtomicReference` + `compareAndSet` | the spot's occupant, the ticket's status | change a value **only if** it still has the expected old value, atomically ([tool 2](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-2--optimistic-locking--version--compare-and-set-retry-on-conflict)) |
| `ConcurrentLinkedDeque` | each floor's free spots | a thread-safe double-ended queue; `pollFirst()` gives an element to exactly one caller |
| `Optional<T>` | `allocate(...)` returns `Optional<ParkingSpot>` | "maybe a spot, maybe nothing", without using `null` |
| `interface` | the two strategies, the listener | a contract that several classes can implement |
| `Predicate<Long>` | the payment check | a function from a fee to true/false ("did the payment succeed?") |
| `LongSupplier` | the clock | a function that returns the current time; tests pass a fake one |
| `CopyOnWriteArrayList` | listeners, the board's log | a thread-safe list that's cheap to read, ideal when writes are rare |
| `ConcurrentHashMap.newKeySet()` | plates currently inside | a thread-safe `Set` |
| `AtomicLong` | ticket numbers | a thread-safe counter for unique ids |

---

## 4. The code, piece by piece

### 4.1 Spot types and vehicle types

```java
enum SpotType { MOTORCYCLE, COMPACT, LARGE, EV }

enum VehicleType {
    MOTORCYCLE(SpotType.MOTORCYCLE, SpotType.COMPACT, SpotType.LARGE),   // a bike can take a bigger spot if needed
    CAR(SpotType.COMPACT, SpotType.LARGE),
    EV_CAR(SpotType.EV, SpotType.COMPACT, SpotType.LARGE),               // prefers a charging spot
    TRUCK(SpotType.LARGE);

    final List<SpotType> allowedSpots;
    VehicleType(SpotType... allowed) { this.allowedSpots = List.of(allowed); }
}
```

Each vehicle type lists the spot types it can use, **in order of preference**. This one table replaces a class hierarchy **and** a pile of `if (type == CAR) … else if …`. Adding a `VAN(SpotType.LARGE)` is one line, and the allocation code never changes (the **Open/Closed principle**: open for extension, closed for modification).

`SpotType... allowed` is a **varargs** parameter: you can pass any number of spot types.

### 4.2 The vehicle: just data

```java
record Vehicle(String plate, VehicleType type) {
    Vehicle {
        Objects.requireNonNull(plate);
        Objects.requireNonNull(type);
    }
}
```

A **value object**: immutable and compared by its values. Two `Vehicle("KA-01", CAR)` objects are equal.

### 4.3 The spot: it owns the rule "one vehicle per spot"

```java
static class ParkingSpot {
    final String id;
    final int floor;
    final SpotType type;
    private final AtomicReference<Vehicle> occupant = new AtomicReference<>();

    boolean tryOccupy(Vehicle v) { return occupant.compareAndSet(null, v); }   // atomic: free -> taken
    void release() { occupant.set(null); }
    boolean isFree() { return occupant.get() == null; }
}
```

An **entity**: it has an identity (`id`) and a lifecycle (free ↔ taken). `tryOccupy` means "if nobody is here (`null`), put this vehicle here", in one indivisible step. Even if two threads somehow reached the same spot, only one `compareAndSet` can succeed.

### 4.4 The floor: owns its spots and the free lists

```java
static class ParkingFloor {
    final int number;
    private final Map<SpotType, Deque<ParkingSpot>> free = new EnumMap<>(SpotType.class);
    private final List<ParkingSpot> all = new ArrayList<>();

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
}
```

| Line | Simple meaning |
|---|---|
| `free` | for each spot type, a queue of the spots that are currently free |
| `pollFirst()` | remove and return the first free spot; thread-safe, so two gates never get the same one |
| `tryOccupy(v)` | a second, defensive check on the spot itself |
| `giveBack` | free the spot and put it at the **front**, so near spots are reused first |

The floor is an **aggregate**: nobody changes a spot except through its floor. Spot ids look like `F0-L1`: floor 0, **L**arge, number 1.

### 4.5 The ticket: a small state machine

```java
enum TicketStatus { ACTIVE, PAYING, PAID }

static class Ticket {
    final String id;
    final Vehicle vehicle;
    final ParkingSpot spot;
    final long entryTime;
    private final AtomicReference<TicketStatus> status = new AtomicReference<>(TicketStatus.ACTIVE);

    boolean startPayment() { return status.compareAndSet(TicketStatus.ACTIVE, TicketStatus.PAYING); }
    void paymentFailed() { status.set(TicketStatus.ACTIVE); }
    void paymentSucceeded() { status.set(TicketStatus.PAID); }
}
```

```
ACTIVE ──startPayment──> PAYING ──paymentSucceeded──> PAID
   ^                        │
   └────paymentFailed───────┘
```

`startPayment()` **claims** the ticket: only one exit gate can move it from ACTIVE to PAYING. That's what prevents a double charge (section 6).

### 4.6 Strategy 1: where to park

```java
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
static class BestFitAllocation implements SpotAllocationStrategy { ... same loops, swapped ... }
```

The only difference between the two is the **order of the loops**:
- **Nearest first:** floor by floor; on each floor, try the preferred spot types. A car might take a large spot on floor 0 before a compact on floor 1.
- **Best fit:** spot type by spot type across all floors. A car takes a compact spot on any floor before touching a large spot, saving large spots for trucks.

### 4.7 Strategy 2: how much to pay

```java
interface PricingStrategy {
    long feePaise(Ticket ticket, long exitTime);
}

static class HourlyPricing implements PricingStrategy {
    private final Map<SpotType, Long> ratePerHourPaise;

    public long feePaise(Ticket t, long exitTime) {
        long minutes = Math.max(1, (exitTime - t.entryTime + 59_999) / 60_000);   // round UP to whole minutes
        long hours = (minutes + 59) / 60;                                         // every STARTED hour is charged
        return hours * ratePerHourPaise.get(t.spot.type);
    }
}
```

| Expression | Simple meaning |
|---|---|
| `(x + 59_999) / 60_000` | milliseconds → minutes, **rounded up** (adding "almost one minute" before dividing) |
| `Math.max(1, …)` | always charge at least one minute |
| `(minutes + 59) / 60` | minutes → hours, rounded up: 61 minutes = 2 hours |
| `long` paise | money as whole paise, never `double` |

`FlatThenHourlyPricing` is a typical mall rule: a flat fee for the first hour, a cheaper rate after that, and a daily cap.

### 4.8 Observer: the display board

```java
interface ParkingEventListener {
    void onParked(Ticket t);
    void onLeft(Ticket t, Receipt r);
}

static class DisplayBoard implements ParkingEventListener { ... adds a line to its log on each event ... }
```

The lot keeps a list of listeners and calls each one on every park and exit. The lot **doesn't know** what a display board is. Tomorrow you can add an analytics listener or an SMS alert without changing `ParkingLot`.

### 4.9 The lot: the facade the gates call

```java
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
```

| Line | Simple meaning |
|---|---|
| `platesInside.add(plate)` | returns false if the plate is already in the set: an atomic "is this car already inside?" |
| `allocation.allocate(...)` | ask the strategy for a spot (it also claims it) |
| `orElse(null)` | unwrap the `Optional` |
| the undo | if there's no spot, remove the plate again so the car can try later |
| `ticketSeq.incrementAndGet()` | a unique ticket number: T1, T2, … |
| `listeners.forEach(...)` | tell every listener |

```java
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
```

The order is **claim → charge → commit**. If the payment fails, the ticket goes back to ACTIVE and the car stays parked, so the driver can try again.

### 4.10 The builder

```java
ParkingLot lot = new ParkingLot.Builder()
        .floor(Map.of(SpotType.MOTORCYCLE, 1, SpotType.COMPACT, 2, SpotType.LARGE, 1, SpotType.EV, 1))
        .floor(Map.of(SpotType.COMPACT, 2, SpotType.LARGE, 1))
        .clock(clock)
        .build();
```

The builder has sensible defaults (nearest-first allocation, hourly pricing, the real clock), lets you override any of them, and validates in one place (`build()` refuses a lot with no floors). Each method returns `this`, so the calls can be chained.

---

## 5. Dry run (from the real output)

The demo lot: floor 0 has 1 motorcycle, 2 compact, 1 large and 1 EV spot; floor 1 has 2 compact and 1 large.

| Step | What happens | Result |
|---|---|---|
| bike KA-01-B1 parks | prefers MOTORCYCLE → F0-M1 is free | MOTORCYCLE spot |
| bike KA-01-B2 parks | the motorcycle spot is taken → falls back to COMPACT | F0-C1 |
| EV KA-01-E1 parks | prefers EV → F0-E1 | EV spot |
| truck KA-01-T1 parks | only LARGE → F0-L1 | `F0-L1` |
| truck KA-01-T2 parks | floor 0's large spot is taken → floor 1 | floor 1 |
| truck KA-01-T3 parks | no large spot anywhere | `ParkingFullException` |
| KA-01-T1 parks again | already inside | `IllegalStateException` |
| the clock moves 61 minutes; T1 exits | 61 minutes = 2 started hours × ₹50 | 10,000 paise (₹100) |
| T1's ticket used again | no longer active | `IllegalArgumentException` |
| the bike exits, payment fails | ticket goes back to ACTIVE | still parked |
| KA-01-T3 tries again | F0-L1 was freed | gets `F0-L1` |

The display board saw 7 events:

```
IN  KA-01-B1 -> F0-M1  free: {MOTORCYCLE=0, COMPACT=4, LARGE=2, EV=1}
IN  KA-01-B2 -> F0-C1  free: {MOTORCYCLE=0, COMPACT=3, LARGE=2, EV=1}
IN  KA-01-E1 -> F0-E1  free: {MOTORCYCLE=0, COMPACT=3, LARGE=2, EV=0}
IN  KA-01-T1 -> F0-L1  free: {MOTORCYCLE=0, COMPACT=3, LARGE=1, EV=0}
IN  KA-01-T2 -> F1-L1  free: {MOTORCYCLE=0, COMPACT=3, LARGE=0, EV=0}
OUT KA-01-T1 paid 100  free: {MOTORCYCLE=0, COMPACT=3, LARGE=1, EV=0}
IN  KA-01-T3 -> F0-L1  free: {MOTORCYCLE=0, COMPACT=3, LARGE=0, EV=0}
```

**Mall pricing** (₹40 first hour, ₹20 per hour after, ₹200 daily cap):

| Stay | Calculation | Fee |
|---|---|---|
| 30 min | the first hour | ₹40 |
| 3 h | 40 + 2 × 20 | ₹80 |
| 20 h | 40 + 19 × 20 = 420, capped | ₹200 |
| 25 h | one full day (200) + 1 hour (40) | ₹240 |

---

## 6. Concurrency, explained simply

### Race 1: two entry gates, one spot
**The danger:** gates A and B both see "spot F0-C1 is free" and both assign it.

**The protection (two layers):**
1. Free spots sit in a `ConcurrentLinkedDeque`. `pollFirst()` **removes** a spot as it hands it out, atomically, so only one gate can receive any given spot.
2. The spot itself checks again with `compareAndSet(null, vehicle)`.

There's **no global lock**, so gates working on different spots never wait for each other.

**Test:** 8 gates park 200 cars into 100 compact spots at once → exactly 100 parked, 100 distinct spots, every spot really occupied.

### Race 2: two exit gates, one ticket
**The danger:** the driver scans the same ticket at two exit gates (or the gate's software retries).

**The bug that was caught while building this:** if a gate **charged first and claimed the ticket after**, both gates would charge the card, and only then would one discover the ticket was already being processed.

**The fix:** **claim before charging.** `startPayment()` is a CAS from ACTIVE to PAYING, so only one gate wins; the other is refused before any money moves. On a failed payment, the ticket goes back to ACTIVE.

**Test:** two gates scan the same ticket at once → the customer is charged exactly once, and the ticket ends PAID.

### The "same car twice" check
`platesInside.add(plate)` is atomic, so two gates can't both let in the same plate.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** no suitable spot (`ParkingFullException`), a plate already inside, a ticket used twice, a failed payment (the car stays), a stay under a minute (charged as 1 minute → 1 hour), stays longer than a day (mall pricing caps per day).

**Common mistakes:**
1. `Car extends Vehicle`, `Truck extends Vehicle` with no difference in behaviour.
2. `if/else` chains on the vehicle type inside the lot.
3. A single `synchronized` on `park()` (correct, but every gate waits in one queue).
4. Charging before claiming the ticket (double charge).
5. `double` for money.
6. Reading `System.currentTimeMillis()` inside the pricing code (untestable).
7. One giant `ParkingLotManager` class doing everything.

---

## 8. Interview questions and answers

**Q1. Why didn't you create a class for each vehicle type?**
> Because they don't behave differently: they only differ in which spots they can use. An enum that carries the allowed spot types keeps it to one record, and adding a new type is one line. If vehicles later had different behaviour (say, EVs need a charging session), I'd model that behaviour separately rather than build a deep hierarchy.

**Q2. How do you stop two gates giving out the same spot?**
> Each floor keeps its free spots in a concurrent queue, and `pollFirst()` hands a spot to exactly one caller. The spot also checks with a CAS on its occupant. No global lock, so gates allocating different spots never block each other.

**Q3. How do you stop a customer being charged twice?**
> The ticket has a status. Before charging, the gate does a CAS from ACTIVE to PAYING. Only one gate can win, so only one gate charges. If the payment fails, the status goes back to ACTIVE.

**Q4. Why are pricing and allocation strategies?**
> They change for different reasons and at different times: marketing changes prices, operations changes the allocation policy. Each is an interface with interchangeable implementations, injected through the builder, so neither change touches `ParkingLot`.

**Q5. Why use the Observer pattern for the display board?**
> The lot shouldn't depend on screens, analytics or alerts. It publishes "parked" and "left" events, and any number of listeners react. New listeners don't change the lot.

**Q6. How is the fee testable?**
> The clock is injected as a `LongSupplier`. The test passes a fake clock and moves it forward 61 minutes instantly, then checks the fee.

**Q7. Why `long` paise instead of `double` rupees?**
> Floating-point numbers can't represent most decimal amounts exactly, so totals drift. Whole paise in a `long` are exact.

**Q8. What does the builder give you?**
> Sensible defaults, readable setup, and validation in one place (`build()` rejects a lot without floors), and the configured lot can't be changed afterwards.

### Follow-up questions

**F1. "Add reservations (book a spot for 3 pm)."**
> Add a `Reservation` with a time window and a spot type. At the reserved time, the spot is taken out of the free queue (a hold with an expiry, like [tool 5](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-5--holds-with-expiry-ttl--reservations-that-free-themselves)). If the driver doesn't arrive within a grace period, the hold expires and the spot returns to the free queue.

**F2. "Add monthly passes."**
> A pricing decorator or a new `PricingStrategy` that checks whether the plate has an active pass and returns 0 (or a discounted fee). No change to the lot.

**F3. "Add EV charging fees."**
> The pricing strategy can add a charging component when the spot type is EV (or read kWh from the charger). Again, only a new or wrapped strategy.

**F4. "The driver lost the ticket."**
> Look up the active ticket by plate (keep a plate → ticket map), and apply a "lost ticket" fee rule in the pricing strategy.

**F5. "Make it work across many servers."**
> The in-memory queues become database rows. Allocate with an atomic conditional update (`UPDATE spot SET vehicle=? WHERE id=? AND vehicle IS NULL`) or `SELECT … FOR UPDATE SKIP LOCKED`. The ticket status claim becomes `UPDATE ticket SET status='PAYING' WHERE id=? AND status='ACTIVE'`. Events go through a message broker for the display boards.

**F6. "Show free spots per floor on each floor's board."**
> Give the board a floor number and have it count only that floor, or emit events that include the floor so each board filters.

**F7. "Prefer spots near the exit, or near the lift."**
> A new allocation strategy that orders spots by distance. The free queues could become priority queues ordered by that distance.

---

## 9. Running the file

```bash
java ParkingLotSystem.java      # 22/22 tests passed (including 200 cars racing for 100 spots and 2 gates on one ticket)
```

**One-line summary:** *"Vehicles are data (an enum lists the spots each can use); floors hand out free spots through lock-free queues and spots double-check with a CAS; allocation and pricing are swappable strategies; exits claim the ticket before charging; and listeners update the display boards."*
