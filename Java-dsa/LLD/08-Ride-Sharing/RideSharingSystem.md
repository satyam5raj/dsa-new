# 08 · Ride-Sharing System, like Uber or Ola (explained simply)

| | |
|---|---|
| **Code** | [RideSharingSystem.java](RideSharingSystem.java) |
| **Run** | `java RideSharingSystem.java` (18 self-checks, including 100 riders racing for 30 drivers) |
| **Design notes** | [README.md](README.md) (geohash/S2/H3, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question tests: finding **nearby** drivers quickly (a spatial index), matching as a swappable strategy, the trip **state machine**, surge pricing, and the race "two riders want the same driver".

---

## 1. The problem in plain words

**Drivers** go online and their apps send their **location** every few seconds. A **rider** requests a ride from a pickup point to a drop point for a vehicle type (bike, auto, mini, sedan, SUV). The system:
1. finds **available drivers nearby** with the right vehicle;
2. **ranks** them (nearest, or best rated);
3. **offers** the ride to them one by one (a driver may decline);
4. **assigns** the first one who accepts, making sure no other rider gets that same driver;
5. tracks the trip: driver arrived → trip started → trip completed;
6. computes the **fare** (with **surge** when demand is high), and lets the rider **rate** the driver.

Riders can cancel: free before a driver is assigned, a fee after.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| How do we find nearby drivers? | A grid of roughly 1 km cells over the map |
| How often do drivers send their location? | Every few seconds: the **hottest write path** |
| Matching rule? | Nearest first; **swappable** (best rated nearby, etc.) |
| Can a driver decline? | Yes, then we try the next candidate |
| Fare? | Base + per km + per minute, by vehicle type, times a surge multiplier |
| Surge? | Demand ÷ supply in the pickup cell, between 1.0× and 3.0× |
| Cancellation? | Free before assignment, ₹50 after |
| Pooling, scheduled rides, payments? | Out of scope |

### Step 2 · Requirements
1. Drivers: go online/offline, update location.
2. Riders: request, cancel, rate.
3. Find nearby available drivers of the right type, quickly.
4. Assign atomically: one driver gets at most one trip.
5. Trip lifecycle with validated transitions.
6. Fare with surge; both swappable.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| a point on the map | `record Location(lat, lon)` with a distance method |
| the spatial index | `class GridIndex` (cell → driver ids) |
| driver | `class Driver` (status changed by CAS, rating) |
| driver status | `enum DriverStatus { OFFLINE, AVAILABLE, ON_TRIP }` |
| rider | `record Rider` |
| vehicle type | `enum VehicleType` |
| trip | `class Trip` with `enum TripStatus` (a state machine) |
| matching rule | `interface MatchingStrategy` |
| fare rule | `interface FareStrategy` + `SurgeFare` decorator |
| the service | `class RideService` |

### Step 4 · Relationships

```
RideService
   ├── GridIndex             (where available drivers are)
   ├── Driver(s), Trip(s)
   ├── MatchingStrategy       (how to rank candidates)
   ├── FareStrategy           (StandardFare wrapped by SurgeFare)
   ├── OfferResponder         (does the driver accept? the driver's app)
   └── TripListener(s)        (notifications, analytics)
Trip ── has ──> Rider, Driver, pickup, drop, surge
```

### Step 5 · API and flow

```java
void goOnline(String driverId) / goOffline(...) / updateLocation(...)
Trip requestRide(Rider r, Location pickup, Location drop, VehicleType type)
void driverArrived(String tripId); void startTrip(String tripId)
long completeTrip(String tripId)           // returns the fare
long cancel(String tripId)                 // returns the cancellation fee
void rateDriver(String tripId, int stars)
```

```
requestRide: find candidates (grid) → rank (strategy) → for each: offer → if accepted, CAS AVAILABLE→ON_TRIP
             → if the CAS wins: assigned!  (if it loses, someone else got that driver: try the next)
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| ranking candidates | **Strategy** (`MatchingStrategy`) |
| the fare formula | **Strategy** (`FareStrategy`) |
| surge on top of any fare | **Decorator** (`SurgeFare`) |
| trip lifecycle | **State machine** (`TripStatus` transition table) |
| notifications | **Observer** (`TripListener`) |
| the driver's accept/decline | an injected interface (`OfferResponder`), so tests control it |

### Step 7 · Concurrency
The contended resource is **a driver**: two riders must never get the same one. The protection is a CAS on the driver's status (`AVAILABLE → ON_TRIP`).

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| the haversine formula | `Location.distanceKm` | the distance between two points on a sphere (the Earth) |
| `record Cell(long x, long y)` | grid cells | records give `equals`/`hashCode`, so they work as map keys |
| `ConcurrentHashMap<Cell, Set<String>>` | the grid | many drivers updating different cells at once |
| `volatile` | `Driver.location`, `Trip.driver` | a write by one thread is immediately visible to others |
| `AtomicReference<DriverStatus>` | driver status | CAS for "this driver is mine" |
| `DoubleAdder`, `LongAdder` | rating sums and counts | fast counters for many threads |
| a static `Map` inside an enum | `TripStatus.NEXT` | the allowed transitions as data |
| `Comparator.comparingDouble(...).thenComparingDouble(...)` | ranking | by rating (descending, via the minus sign), then by distance |
| `try { … } finally { …decrementAndGet() }` | open-request counting | always undo the demand counter, even on errors |

---

## 4. The code, piece by piece

### 4.1 The grid index: "drivers near me" without checking every driver

```java
static class GridIndex {
    record Cell(long x, long y) {}
    private final double size;                                              // 0.01 degrees ≈ 1.1 km
    private final ConcurrentHashMap<Cell, Set<String>> cells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cell> cellOf = new ConcurrentHashMap<>();

    Cell cellFor(Location l) { return new Cell((long) Math.floor(l.lat() / size), (long) Math.floor(l.lon() / size)); }

    void update(String driverId, Location l) {
        Cell next = cellFor(l);
        Cell prev = cellOf.put(driverId, next);
        if (next.equals(prev)) return;                                      // same cell: nothing to move
        cells.computeIfAbsent(next, c -> ConcurrentHashMap.newKeySet()).add(driverId);
        if (prev != null) cells.getOrDefault(prev, Set.of()).remove(driverId);
    }

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
}
```

**The analogy:** a city map divided into squares like a chessboard. Each square has a list of the drivers currently in it. To find drivers near you, look in **your** square (ring 0), then the 8 squares around it (ring 1), then the next ring out, and stop once you have enough.

```
ring 2:  ▢ ▢ ▢ ▢ ▢
ring 1:  ▢ ■ ■ ■ ▢
ring 0:  ▢ ■ ● ■ ▢      ● = the rider's cell
         ▢ ■ ■ ■ ▢
         ▢ ▢ ▢ ▢ ▢
```

| Line | Simple meaning |
|---|---|
| `cellFor` | latitude/longitude ÷ cell size, rounded down = the cell coordinates |
| `cellOf.put(driverId, next)` | remember the driver's new cell and get the old one |
| same cell? | most updates don't change cells, so they're very cheap |
| move | add to the new cell's set, remove from the old one |
| `nearby` | scan ring by ring, only the **border** of each ring (the inside was scanned already) |

Location updates are the hottest write path (every driver every ~4 seconds), so they must be cheap. In production, the cells would be **geohash**, Google **S2** or Uber **H3** cells: the same idea, better-shaped cells, and string keys that work in Redis.

### 4.2 The driver: CAS on status

```java
static class Driver {
    volatile Location location;
    final AtomicReference<DriverStatus> status = new AtomicReference<>(DriverStatus.OFFLINE);

    /** The atomic "this driver is mine" step. Only one trip can win. */
    boolean tryAssign() { return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP); }
    void free() { status.compareAndSet(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE); }
    double rating() { long n = ratingCount.sum(); return n == 0 ? 5.0 : ratingSum.sum() / n; }   // new drivers start at 5
}
```

### 4.3 The trip state machine

```java
enum TripStatus {
    REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED, NO_DRIVER_FOUND;
    private static final Map<TripStatus, Set<TripStatus>> NEXT = Map.of(
            REQUESTED, Set.of(DRIVER_ASSIGNED, CANCELLED, NO_DRIVER_FOUND),
            DRIVER_ASSIGNED, Set.of(DRIVER_ARRIVED, CANCELLED),
            DRIVER_ARRIVED, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED));
    boolean canGoTo(TripStatus t) { return NEXT.getOrDefault(this, Set.of()).contains(t); }
}
```

```
REQUESTED ──> DRIVER_ASSIGNED ──> DRIVER_ARRIVED ──> IN_PROGRESS ──> COMPLETED
    │                 │                  │
    ├──> NO_DRIVER_FOUND                 │
    └─────────────────┴──────────────────┴──> CANCELLED
```

The transitions are **data** (a map). Once a trip is in progress it can't be cancelled, only completed.

### 4.4 Matching strategies

```java
interface MatchingStrategy { List<Driver> rank(Trip trip, List<Driver> candidates); }

static class NearestDriverMatching implements MatchingStrategy {
    public List<Driver> rank(Trip t, List<Driver> c) {
        return c.stream().sorted(Comparator.comparingDouble(d -> d.location.distanceKm(t.pickup))).toList();
    }
}

static class BestRatedNearbyMatching implements MatchingStrategy {
    public List<Driver> rank(Trip t, List<Driver> c) {
        return c.stream().filter(d -> d.location.distanceKm(t.pickup) <= maxKm)
                .sorted(Comparator.comparingDouble((Driver d) -> -d.rating()).thenComparingDouble(d -> d.location.distanceKm(t.pickup))).toList();
    }
}
```

The strategy only **ranks**. The service does the atomic assignment. That separation keeps strategies simple and makes the race protection live in one place.

### 4.5 Fares and surge

```java
static class StandardFare implements FareStrategy {
    public long farePaise(Trip t, double km, double min) {
        long[] r = rates.get(t.type);                                     // {base, perKm, perMin}
        return r[0] + Math.round(km * r[1]) + Math.round(min * r[2]);
    }
}

/** DECORATOR: multiplies any fare by the surge captured when the trip was requested. */
static class SurgeFare implements FareStrategy {
    public long farePaise(Trip t, double km, double min) { return Math.round(inner.farePaise(t, km, min) * t.surge); }
}

static double surgeFor(int openRequests, int freeDrivers) {
    double ratio = freeDrivers == 0 ? 3.0 : (double) openRequests / freeDrivers;
    return Math.round(Math.max(1.0, Math.min(3.0, ratio)) * 10) / 10.0;
}
```

- **Surge** = demand ÷ supply in the pickup area, kept between 1.0 and 3.0, rounded to one decimal.
- It's **captured when the trip is requested** (stored in `t.surge`), so the rider pays the multiplier they were shown, not whatever it is when the ride ends.
- `SurgeFare` is a **decorator**: it wraps any fare strategy.

### 4.6 Requesting a ride: find → rank → offer → assign

```java
Trip requestRide(Rider r, Location pickup, Location drop, VehicleType type) {
    Trip t = new Trip("T" + seq.incrementAndGet(), r, pickup, drop, type);
    ...
    int demand = openRequests.computeIfAbsent(cell, c -> new AtomicInteger()).incrementAndGet();
    try {
        List<Driver> candidates = index.nearby(pickup, 3, 50).stream().map(drivers::get)
                .filter(d -> d != null && d.vehicle == type && d.status.get() == DriverStatus.AVAILABLE).toList();
        t.surge = surgeFor(demand, candidates.size());                  // demand here vs supply nearby
        for (Driver d : matching.rank(t, candidates)) {
            if (!responder.accepts(d, t)) { emit(t, "DECLINED by " + d.id); continue; }   // offer timed out / declined
            if (!d.tryAssign()) continue;                                 // someone else got this driver first
            index.remove(d.id);                                          // busy drivers aren't searchable
            t.driver = d;
            t.transition(TripStatus.DRIVER_ASSIGNED);
            return t;
        }
        t.transition(TripStatus.NO_DRIVER_FOUND);
        return t;
    } finally {
        openRequests.get(cell).decrementAndGet();
    }
}
```

In plain words:
1. Count this request as "demand" in the pickup cell.
2. Ask the grid for up to 50 drivers within 3 rings; keep the available ones with the right vehicle.
3. Compute the surge from demand vs supply.
4. Rank the candidates; offer to each in turn. If one declines, try the next. If the CAS fails, someone else just took them; try the next.
5. On success: remove the driver from the index (busy drivers shouldn't show up in searches) and mark the trip DRIVER_ASSIGNED.
6. Nobody? NO_DRIVER_FOUND.

### 4.7 Completing and cancelling

```java
long completeTrip(String tripId) {
    Trip t = trip(tripId);
    t.transition(TripStatus.COMPLETED);
    ...
    double km = t.pickup.distanceKm(t.drop);                              // production: the GPS trace length
    double minutes = (t.endedAt - t.startedAt) / 60_000.0;
    t.farePaise = fares.farePaise(t, km, minutes);
    releaseDriver(t.driver, t.drop);
    return t.farePaise;
}

long cancel(String tripId) {
    Trip t = trip(tripId);
    TripStatus before = t.status();
    t.transition(TripStatus.CANCELLED);
    if (t.driver != null) releaseDriver(t.driver, t.driver.location);
    return before == TripStatus.REQUESTED ? 0 : CANCEL_FEE_PAISE;
}
```

After a trip, the driver becomes AVAILABLE again **at the drop location** and goes back into the index.

---

## 5. Dry run (from the real output)

Drivers near MG Road: `d-near` (MINI, 0.5 km north), `d-far` (MINI, 2.5 km north), `d-suv` (SUV, 0.1 km north). Rates for every type: ₹50 base + ₹12/km + ₹2/min.

| Step | What happens | Result |
|---|---|---|
| Asha requests a MINI | the SUV is closest but the wrong type | **d-near** assigned, now ON_TRIP |
| arrive → start → 25 minutes → complete (10 km) | 50 + 12 × 10 + 2 × 25 | **₹220** (surge 1.0: 1 request, 2 drivers) |
| | | d-near AVAILABLE again |
| start the completed trip | COMPLETED → IN_PROGRESS | not allowed |
| Asha rates 4 stars | | rating 4.0 |
| d-near will now decline; Asha requests 10 km north | no drivers within 3 rings there | NO_DRIVER_FOUND |
| Ravi requests at MG Road | d-near declines → next candidate | **d-far** assigned |
| Ravi cancels | a driver was assigned | ₹50 fee, d-far released |
| cancel again | CANCELLED → CANCELLED | not allowed |
| surge: 1 request / 4 drivers | | 1.0 |
| 6 / 4 | | 1.5 |
| 5 / 0 | | 3.0 (capped) |
| a ₹50 base fare at 1.5× | | ₹75 |
| grid vs brute force over 2,000 moving drivers | | every driver within 1 km found; the grid returned only 56 candidates |
| 100 riders, 30 drivers, all at once | | **exactly 30 matched**, no driver twice |

---

## 6. Concurrency, explained simply

**The danger:** riders A and B both see driver D as the nearest available driver and both assign him.

**The protection:** `tryAssign()` is a CAS from AVAILABLE to ON_TRIP. Only one rider's CAS succeeds; the other simply moves on to the next candidate in their ranked list.

**Why the index can be slightly stale and that's fine:** the grid is updated constantly and isn't perfectly in sync with driver statuses at every instant. A candidate taken from the grid might have just become busy. It doesn't matter, because the **status is re-checked** by the CAS at the moment of assignment. The index only suggests; the CAS decides.

**Demand counting** uses an `AtomicInteger` per cell, decremented in `finally` so a failed request never leaves the count too high.

**Test:** 100 riders and 30 drivers, all at once → exactly 30 matched, no driver with two trips.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** no driver nearby, the wrong vehicle type nearby, a driver declining, two riders racing for one driver, cancelling before vs after assignment, invalid transitions, ratings out of range, rating before completion, new drivers without ratings (start at 5.0).

**Common mistakes:**
1. Scanning every driver for each request (O(drivers) per request).
2. Assigning without a CAS (two riders, one driver).
3. Leaving busy drivers in the search index.
4. Computing surge at the end of the trip (the rider pays a different price than shown).
5. Scattering trip status changes without a transition table.
6. `double` for money (here fares are rounded to whole paise).

---

## 8. Interview questions and answers

**Q1. How do you find nearby drivers quickly?**
> A spatial index: divide the map into cells (here a grid of ~1 km; in production geohash, S2 or H3). Each cell has the set of available driver ids. A search looks at the rider's cell and expanding rings around it until it has enough candidates.

**Q2. How do you stop two riders getting the same driver?**
> Assignment is a compare-and-set on the driver's status from AVAILABLE to ON_TRIP. Only one succeeds; the loser tries its next candidate. In a database: `UPDATE driver SET status='ON_TRIP', trip_id=? WHERE id=? AND status='AVAILABLE'`.

**Q3. How do you handle a driver declining?**
> The ranked candidates are offered one at a time; a decline (or a timeout) moves to the next candidate. If none accept, the trip becomes NO_DRIVER_FOUND (or the search widens).

**Q4. How is surge computed?**
> The ratio of open requests to available drivers near the pickup, clamped to a range and rounded. It's captured at request time and applied by a decorator around the base fare.

**Q5. Why separate ranking from assignment?**
> Strategies stay simple (just order candidates), and the race-sensitive assignment lives in one place in the service.

**Q6. Location updates are very frequent. How do you keep them cheap?**
> Most updates stay in the same cell, so they're a single map write. Moving cells is two set operations. Only available drivers are indexed.

### Follow-up questions

**F1. "Scale to a whole country."**
> Shard by region (city or geohash prefix). Keep driver locations in Redis (it has GEO commands) or an in-memory service per region. Driver location updates flow through a stream (Kafka); matching services subscribe per region.

**F2. "Add ride pooling."**
> Match riders whose routes overlap; a trip has several riders, and each pickup/drop is a stop. Matching considers detour time for existing riders.

**F3. "Use ETA instead of straight-line distance."**
> A new matching strategy that ranks by estimated travel time from a routing service.

**F4. "Driver offers time out after 15 seconds."**
> The offer becomes asynchronous: send the offer, wait up to 15 s; on timeout or decline, offer the next candidate. The trip stays REQUESTED meanwhile.

**F5. "Prevent the surge multiplier from being gamed."**
> Smooth it over time and larger areas, cap how fast it can change, and lock it for the rider for a few minutes after they see it.

---

## 9. Running the file

```bash
java RideSharingSystem.java      # 18/18 tests passed (including 100 riders racing for 30 drivers)
```

**One-line summary:** *"A grid index (cell → available drivers) finds candidates in nearby rings; a Strategy ranks them; offers go out one by one and the assignment is a CAS on the driver's status; the trip follows a validated state machine; and the fare is a Strategy wrapped by a surge Decorator captured at request time."*
