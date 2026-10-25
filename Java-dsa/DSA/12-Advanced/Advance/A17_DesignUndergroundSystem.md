# A17 · Design Underground System ⭐

| | |
|---|---|
| **LeetCode** | [#1396 Design Underground System](https://leetcode.com/problems/design-underground-system/) |
| **Difficulty** | Medium |
| **Mix of structures** | **HashMap** (who is travelling now) + **HashMap** (running totals per route) + **records as keys** |
| **Code** | [A17_DesignUndergroundSystem.java](A17_DesignUndergroundSystem.java) |
| **Run** | `java A17_DesignUndergroundSystem.java` |

⭐ = not in `leetcode.md`, added because it's a very common "two maps working together" design question.

---

## 1. The problem

A metro card system:
- `checkIn(id, station, t)`: passenger `id` enters at `station` at time `t`;
- `checkOut(id, station, t)`: the same passenger leaves at `station` at time `t`;
- `getAverageTime(start, end)`: the average time of all **completed** trips from `start` to `end`.

```
checkIn(45, "Leyton", 3)      checkIn(32, "Paradise", 8)     checkIn(27, "Leyton", 10)
checkOut(45, "Waterloo", 15)  checkOut(27, "Waterloo", 20)   checkOut(32, "Cambridge", 22)
getAverageTime("Paradise", "Cambridge") → 14.0     (22 − 8)
getAverageTime("Leyton", "Waterloo")    → 11.0     ((15 − 3) + (20 − 10)) / 2
checkIn(10, "Leyton", 24)
getAverageTime("Leyton", "Waterloo")    → 11.0     (passenger 10 hasn't finished)
checkOut(10, "Waterloo", 38)
getAverageTime("Leyton", "Waterloo")    → 12.0     (12 + 10 + 14) / 3
```

A passenger is only in one place at a time, and averages are only asked for routes that have at least one completed trip.

---

## 2. How to think about it

### Two different questions → two maps
| Question | Map |
|---|---|
| "Where and when did passenger 45 get on?" | `active: id → (station, time)` |
| "What do we know about the route Leyton → Waterloo?" | `stats: (from, to) → {total time, trip count}` |

`checkOut` **joins** them: look up (and remove) the check-in, compute the duration, add it to the route's totals.

### Keep totals, not trips
To answer an average you don't need the individual trips, only `sum` and `count`. Two numbers per route make `getAverageTime` O(1) instead of a scan.

### The route key: a record, not a glued string
`from + to` as a key looks fine but collides: `"ab" + "c"` and `"a" + "bc"` are both `"abc"`. A separator like `"->"` usually works, unless a station name contains it. A `record Route(String from, String to)` is the clean answer: records get correct `equals` and `hashCode` automatically. The file tests exactly this collision.

### Dry run
| Call | `active` | `stats` |
|---|---|---|
| checkIn 45 Leyton@3, 32 Paradise@8, 27 Leyton@10 | {45:(Leyton,3), 32:(Paradise,8), 27:(Leyton,10)} | {} |
| checkOut 45 Waterloo@15 → 12 | {32, 27} | {Leyton→Waterloo: 12, 1} |
| checkOut 27 Waterloo@20 → 10 | {32} | {Leyton→Waterloo: 22, 2} |
| checkOut 32 Cambridge@22 → 14 | {} | + {Paradise→Cambridge: 14, 1} |
| average Leyton→Waterloo | | 22 / 2 = **11.0** |

---

## 3. Java concepts used in this problem

### 3.1 Records as HashMap keys
```java
record Route(String from, String to) {}
record CheckIn(String station, int time) {}
```
A record automatically gets a constructor, getters (`from()`, `to()`), and `equals`/`hashCode` based on **all its fields**. So `new Route("Leyton", "Waterloo")` created in `getAverageTime` finds the entry created earlier in `checkOut`, even though they're different objects. A normal class without `equals`/`hashCode` would compare by identity and never find it.

### 3.2 `remove` returns the value
```java
CheckIn in = active.remove(id);
```
One call both fetches the check-in and deletes it, since the trip is over.

### 3.3 `computeIfAbsent` with a `long[]` counter
```java
long[] s = stats.computeIfAbsent(new Route(in.station(), station), k -> new long[2]);
s[0] += t - in.time();
s[1]++;
```
It gets the route's `{total, count}` array, creating `{0, 0}` the first time. Arrays are mutable, so changing `s[0]` changes the value stored in the map; there's no need to `put` it back.

### 3.4 Integer division vs `double`
`(double) s[0] / s[1]` casts **before** dividing. `s[0] / s[1]` would be integer division: 22 / 2 = 11 happens to be exact, but 25 / 2 would give 12, not 12.5.

---

## 4. Approach 1: Keep every trip, average by scanning (the referee)

### Code ([A17_DesignUndergroundSystem.java:44-59](A17_DesignUndergroundSystem.java#L44-L59))
```java
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
```

This stores every completed trip; `getAverageTime` scans them all: **O(trips) per query**.

---

## 5. Approach 2: Two HashMaps with running totals (main solution)

### Code ([A17_DesignUndergroundSystem.java:62-79](A17_DesignUndergroundSystem.java#L62-L79))
```java
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
```

### Line by line
| Line | What it does |
|---|---|
| `checkIn` | remember where and when this passenger got on |
| `active.remove(id)` | the check-in, which is no longer needed after this |
| `computeIfAbsent(new Route(...))` | this route's totals, created on the first trip |
| `s[0] += t − in.time()` | add the trip's duration |
| `s[1]++` | one more trip |
| `getAverageTime` | total / count as a `double` |

### Complexity
- Every call is **O(1)** on average (hash map operations).
- Space: **O(passengers currently travelling + distinct routes)**. Completed trips take no extra space.

---

## 6. Comparing the approaches

| | checkIn / checkOut | getAverageTime | Memory |
|---|---|---|---|
| List of trips | O(1) | O(trips) | grows with every trip |
| **Two maps + totals** | O(1) | **O(1)** | O(routes) |

The file runs 60,000 random operations and compares the averages with the scanning version.

---

## 7. Edge cases and common mistakes

**Edge cases:** a passenger who checks in again after checking out (a new trip), the same station pair in opposite directions (A→B is a different route from B→A), a trip that hasn't finished yet (not counted), station names that collide when glued together.

**Mistakes:**
1. A string key `from + to` (collisions).
2. Integer division in the average.
3. Not removing the check-in, so a stale entry is reused if the same id travels again (here `put` would overwrite it anyway, but removing keeps the map small).
4. Storing every trip when only the sum and count are needed.
5. `int` totals: many long trips can overflow, so use `long`.

---

## 8. Interview questions and answers

**Q1. Why two maps?**
> They answer different questions with different keys: one is keyed by passenger (an open trip), the other by route (aggregate statistics). `checkOut` connects them.

**Q2. Why not store all trips?**
> The average only needs a sum and a count. Running totals make queries O(1) and keep memory proportional to the number of routes.

**Q3. What key do you use for a route?**
> A record (or a class with proper `equals`/`hashCode`) holding both station names. Concatenated strings can collide.

### Follow-up questions

**F1. "Median trip time instead of average."**
> You need the distribution: keep two heaps per route ([Find Median from Data Stream](../../08-Heap/04-Hard/H01_FindMedianFromDataStream.md)), or a sorted multiset.

**F2. "Average over the last hour only."**
> Keep a queue of (time, duration) per route; drop old entries from the front and adjust the totals: a sliding window.

**F3. "Thread-safe, many gates at once."**
> `ConcurrentHashMap` for both maps, and update route totals atomically (`compute`, or `LongAdder`s for the sum and count).

---

## 9. Running the file

```bash
java A17_DesignUndergroundSystem.java     # 6/6 tests passed (LeetCode example, open trip, record-key collision, 60,000 random ops)
```

**One-line summary:** *"One HashMap holds each passenger's open check-in, another maps a Route record to {total time, trip count}; checkOut moves a trip from the first into the second, and the average is total / count."*
