# 02 · Elevator System (explained simply)

| | |
|---|---|
| **Code** | [ElevatorSystem.java](ElevatorSystem.java) |
| **Run** | `java ElevatorSystem.java` (12 self-checks, including a 4,000-button concurrency test) |
| **Design notes** | [README.md](README.md) (state diagram, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question tests whether you spot that it's really **two** scheduling problems, can model an elevator as a **state machine**, and can keep it correct when buttons are pressed on every floor at once.

---

## 1. The problem in plain words

A building has several elevators ("cars"). People press buttons in two places:
- **On a floor (a hall call):** "I'm on floor 7 and want to go **UP**." The system picks **which** car comes.
- **Inside a car (a car call):** "Take me to floor 3." Only **that** car can serve it.

Each car must decide the **order** of its stops, open its doors at the right floors, and never take someone the wrong way. A car can also be taken out of service for maintenance.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| How many cars and floors? | Configurable (e.g. 3 cars, floors 0–20) |
| Hall buttons: one button, or UP and DOWN? | **UP and DOWN** (so the passenger's direction is known) |
| Do we model time? | Yes, as discrete **ticks**: each tick a car moves one floor or stops |
| Capacity / weight limits? | Out of scope (an extension) |
| Maintenance mode? | Yes: a car can be taken out of service |
| Emergency, fire mode, VIP? | Out of scope (extensions) |

### Step 2 · Requirements
- Accept hall calls (floor + direction) and car calls (car + floor) from **any thread**.
- Choose a car for each hall call (**dispatch**).
- Serve each car's stops in a sensible **order** (not first-come-first-served zig-zagging).
- Don't pick up someone going DOWN while the car is passing on its way UP.
- Maintenance: stop the car and hand its pending hall calls to other cars.
- Thread-safe, and the scheduling algorithms must be swappable.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| elevator / car | `class Elevator`: position, direction, state, stops |
| direction | `enum Direction { UP, DOWN, IDLE }` |
| car state | `enum ElevatorState { IDLE, MOVING, DOORS_OPEN, MAINTENANCE }` |
| a button press | `HallCall(floor, direction)` and `CarCall(elevatorId, floor)`: **request objects** |
| the controller | `class ElevatorController`: receives presses, owns the cars |
| floor display | `class FloorDisplay`: listens for stops |

### Step 4 · Relationships

```
ElevatorController ──has──> many Elevator   (each owns its own stops, direction, state)
        ├──uses──> DispatchStrategy        (WHICH car answers a hall call)
        ├──has───> inbox of Requests       (button presses waiting to be applied)
        └──tells─> ElevatorListener(s)     (floor displays)

Elevator itself runs LOOK                  (the ORDER of its own stops)
```

### Step 5 · API and flows

```java
void pressHall(int floor, Direction dir)     // any thread
void pressInCar(int elevatorId, int floor)   // any thread
void tick()                                  // ONE thread: apply presses, move every car one step
void setMaintenance(int id)
```

### Step 6 · Patterns

| What changes or matters | Pattern |
|---|---|
| **which car** answers a hall call (nearest, round robin, zoning…) | **Strategy** (`DispatchStrategy`) |
| a button press is an object you can queue and replay | **Command** (`HallCall`, `CarCall`) |
| the car behaves differently when IDLE / MOVING / DOORS_OPEN / MAINTENANCE | **State machine** (an enum) |
| floor displays react to stops | **Observer** (`ElevatorListener`) |

### Step 7 · Concurrency
Buttons are pressed from many threads, but **only one thread changes elevator state** (the tick loop). Buttons just drop a command into a thread-safe queue. That's the **single-writer** idea (section 6).

### The big insight: two different scheduling problems
1. **Dispatch:** someone on floor 7 presses UP → **which** elevator goes? (the controller decides, via `DispatchStrategy`)
2. **Ordering:** one elevator has stops {3, 9, 5, 1} → **in what order**? (the elevator decides, via the LOOK algorithm)

Saying this out loud early is a strong signal.

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `sealed interface … permits` | `Request permits HallCall, CarCall` | an interface with a **closed** list of implementations; the compiler knows every case |
| `record` | `HallCall`, `CarCall` | small immutable request objects |
| `switch` with pattern matching | `case HallCall h -> …; case CarCall c -> …` | branch on the object's type and name it in one go; with a sealed interface, no `default` is needed |
| switch **expression** | `return switch (direction) { case UP -> …; … };` | a `switch` that produces a value |
| `TreeSet<Integer>` | the up and down stop lists | a sorted set; `higher(x)` / `lower(x)` find the next stop above/below in O(log n) |
| `ConcurrentLinkedQueue` | the controller's inbox | a thread-safe queue that any thread can add to |
| `Comparator.comparingInt(...).thenComparingInt(...)` | dispatch picks the cheapest car | sort by cost, then by id to break ties |
| `Optional` | `choose(...)` may find no car (all in maintenance) | "maybe a car" |
| `AtomicBoolean` | stops the tick thread in the test | a thread-safe flag |
| `Math.floorMod` | round robin | a remainder that's never negative |

---

## 4. The code, piece by piece

### 4.1 Requests are Command objects

```java
sealed interface Request permits HallCall, CarCall {}
record HallCall(int floor, Direction direction) implements Request {}   // pressed on a floor
record CarCall(int elevatorId, int floor) implements Request {}         // pressed inside car e
```

A button press becomes a small immutable object. Objects can be **queued**, **logged** and even **replayed**: that's the Command pattern.

### 4.2 The elevator's stop lists

```java
private int floor;
private Direction direction = Direction.IDLE;
private ElevatorState state = ElevatorState.IDLE;
private final TreeSet<Integer> upStops = new TreeSet<>();    // stops to make while going UP
private final TreeSet<Integer> downStops = new TreeSet<>();  // stops to make while going DOWN
```

Two sorted sets: the stops to make on the way up, and the stops to make on the way down. Keeping them sorted makes "is there anything above me?" a fast `higher(floor)` call.

### 4.3 Adding stops: the passenger's direction matters

```java
void addCarStop(int target) {
    validate(target);
    if (target > floor) upStops.add(target);
    else if (target < floor) downStops.add(target);
    else (direction == Direction.DOWN ? downStops : upStops).add(target);   // "here": open doors next tick
}

void addHallStop(int f, Direction wanted) {
    validate(f);
    (wanted == Direction.UP ? upStops : downStops).add(f);
}
```

- **Car call:** the direction comes from where the target is **relative to the car** right now.
- **Hall call:** the direction is what the **passenger wants**. Someone on floor 5 going DOWN must **not** be picked up while the car passes floor 5 going UP, because they'd ride the wrong way. So a "5, DOWN" hall call goes into `downStops`.

`(condition ? setA : setB).add(x)` picks a set and adds to it in one expression.

### 4.4 One tick: move one floor, or stop here

```java
void step() {
    if (state == ElevatorState.MAINTENANCE) return;
    if (state == ElevatorState.DOORS_OPEN) state = ElevatorState.MOVING;      // doors close after one tick
    if (shouldStopHere()) { serveHere(); return; }

    // LOOK: keep the direction while work remains that way, else reverse, else idle
    switch (direction) {
        case UP -> { if (!anyAbove()) direction = anyBelow() ? Direction.DOWN : Direction.IDLE; }
        case DOWN -> { if (!anyBelow()) direction = anyAbove() ? Direction.UP : Direction.IDLE; }
        case IDLE -> direction = anyAbove() ? Direction.UP : anyBelow() ? Direction.DOWN : Direction.IDLE;
    }
    if (shouldStopHere()) { serveHere(); return; }                           // a call waiting at the turning point

    if (direction == Direction.UP) { floor++; floorsTravelled++; state = ElevatorState.MOVING; }
    else if (direction == Direction.DOWN) { floor--; floorsTravelled++; state = ElevatorState.MOVING; }
    else state = ElevatorState.IDLE;
}
```

In plain words, each tick:
1. In maintenance? Do nothing.
2. Doors were open? They close now.
3. Is there a stop here for my direction? Open the doors and finish this tick.
4. Otherwise apply **LOOK**: keep going while there's work in my direction; if not, turn around; if there's no work at all, go idle.
5. After turning, check again whether to stop here (someone may be waiting at the turning point).
6. Move one floor.

### 4.5 The LOOK algorithm, simply
> "Keep moving in the current direction while there are requests further that way, stopping at requests on the way. Then reverse."

It's how real elevators behave, and why they're also called the "elevator algorithm". **SCAN** is similar but always travels to the last floor before reversing; **LOOK** turns at the last **request**, so it's never worse. **FCFS** (first come, first served) serves in arrival order and zig-zags up and down.

### 4.6 Should I stop here?

```java
private boolean shouldStopHere() {
    return switch (direction) {
        case UP -> upStops.contains(floor) || (!anyAbove() && downStops.contains(floor));     // turning point
        case DOWN -> downStops.contains(floor) || (!anyBelow() && upStops.contains(floor));
        case IDLE -> upStops.contains(floor) || downStops.contains(floor);
    };
}
```

Going UP, stop for an "up" stop here. Or, if nothing more is above (this is where the car will turn), also stop for a "down" stop here.

### 4.7 Maintenance hands back unfinished work

```java
Map<Integer, Direction> enterMaintenance() {
    Map<Integer, Direction> orphaned = new TreeMap<>();
    upStops.forEach(f -> orphaned.put(f, Direction.UP));
    downStops.forEach(f -> orphaned.put(f, Direction.DOWN));
    upStops.clear(); downStops.clear();
    state = ElevatorState.MAINTENANCE;
    direction = Direction.IDLE;
    return orphaned;
}
```

The car returns its pending stops, and the controller re-dispatches them as hall calls to other cars, so nobody is left waiting forever.

### 4.8 Dispatch: which car answers a hall call

```java
static class NearestCarDispatch implements DispatchStrategy {
    public Optional<Elevator> choose(List<Elevator> cars, HallCall call) {
        return cars.stream()
                .filter(e -> e.state() != ElevatorState.MAINTENANCE)
                .min(Comparator.comparingInt((Elevator e) -> cost(e, call)).thenComparingInt(e -> e.id));
    }

    static int cost(Elevator e, HallCall c) {
        int dist = Math.abs(e.floor() - c.floor());
        boolean onTheWay = (e.direction() == Direction.UP && c.direction() == Direction.UP && c.floor() >= e.floor())
                || (e.direction() == Direction.DOWN && c.direction() == Direction.DOWN && c.floor() <= e.floor());
        if (e.direction() == Direction.IDLE || onTheWay) return dist + e.pendingStops();
        return dist + 2 * (e.maxFloor - e.minFloor) + e.pendingStops();
    }
}
```

| Car situation | Cost |
|---|---|
| idle | the distance |
| moving **towards** the caller, in the caller's direction ("on the way") | the distance |
| anything else (it must finish its current sweep first) | the distance + a big penalty |
| always | + the number of pending stops, so one car doesn't get all the work |

Ties go to the lower id. `RoundRobinDispatch` is a naive alternative that ignores position (useful for comparison).

### 4.9 The controller: a thread-safe inbox, a single-threaded tick

```java
private final Queue<Request> inbox = new ConcurrentLinkedQueue<>();   // any thread may add

void pressHall(int floor, Direction dir) { inbox.add(new HallCall(floor, dir)); }
void pressInCar(int elevatorId, int floor) { inbox.add(new CarCall(elevatorId, floor)); }

void tick() {                                                    // called ONLY by the tick thread
    Request r;
    while ((r = inbox.poll()) != null) route(r);                 // apply queued commands
    for (Elevator e : cars) e.step();
}

private void route(Request r) {
    switch (r) {
        case HallCall h -> dispatch.choose(cars, h)
                .orElseThrow(() -> new IllegalStateException("No elevator in service"))
                .addHallStop(h.floor(), h.direction());
        case CarCall c -> {
            Elevator e = cars.get(c.elevatorId());
            if (e.state() != ElevatorState.MAINTENANCE) e.addCarStop(c.floor());
        }
    }
}
```

Buttons (on any thread) only **add** a request to the inbox. The tick thread drains the inbox, routes each request, then steps every car.

---

## 5. Dry runs (from the real output)

### Scenario 1: LOOK ordering in one car
The car is at floor 0. Car calls for 5, 3, 8 (pressed in that order) all go into `upStops = {3, 5, 8}`.

| Tick result | Why |
|---|---|
| goes UP: 1, 2, 3 → **stops at 3** | the nearest stop above |
| 4, 5 → **stops at 5** | |
| 6, 7, 8 → **stops at 8** | |

Order served: **3, 5, 8** (not 5, 3, 8 as pressed). Then a car call for 6 (→ `downStops`) and a hall call "2, UP" (→ `upStops`). At 8, nothing is above, so the car turns DOWN, **stops at 6**, keeps going down, and at 2 (the turning point) **stops for the UP call**. Final order: `[E0@3, E0@5, E0@8, E0@6, E0@2]`.

### Scenario 2: the passenger's direction matters
The car is going up to 9 and has reached floor 2. Then: "5, DOWN" and "6, UP" are pressed.

| Floor | Stop? | Why |
|---|---|---|
| 5 | no | the caller wants DOWN, and the car is going UP with work above |
| 6 | **yes** | the caller wants UP |
| 9 | **yes** | the car call |
| (turns down) 5 | **yes** | now going DOWN |

Result: `[E0@6, E0@9, E0@5]`.

### Scenario 3: dispatch
Cars are at floors 0, 10 and 18, all idle. "12, DOWN" is pressed. Costs: car 0 = 12, car 1 = 2, car 2 = 6 → **car 1** (pending stops: `[0, 1, 0]`).

### Scenario 4: maintenance
"7, DOWN" goes to car 0 (a tie, so the lower id). Car 0 enters maintenance → its stop is handed to car 1, which serves floor 7. A later car call to car 0 is ignored.

### Scenario 5: LOOK vs FCFS
30 random stops between floors 0 and 49: **LOOK travels 49 floors, FCFS 675 floors.** LOOK simply sweeps up once, while FCFS zig-zags.

### Scenario 6: concurrency
8 threads press 4,000 buttons while another thread keeps ticking. At the end every request is served and every requested floor was visited, with **no locks on `Elevator`**.

---

## 6. Concurrency, explained simply: the single-writer design

**The danger:** if button threads changed `upStops` directly while the tick thread was reading it, the `TreeSet` could be corrupted (it isn't thread-safe), or a stop could be lost.

**The design:** think of a receptionist with an in-tray. Anyone can drop a note in the tray (the thread-safe `ConcurrentLinkedQueue`), but **only the receptionist** reads the notes and updates the schedule. Because only one thread ever changes elevator state, the `Elevator` class needs **no locks at all**.

This is called the **single-writer** or **event-loop** design. Real elevator controllers, game loops, and actor systems (like Akka) work this way. It's also easy to test: the test calls `tick()` manually, step by step.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** a call for the floor the car is on, a request for a floor that doesn't exist (`IllegalArgumentException`), a car call to a car in maintenance (ignored), a hall call when every car is in maintenance (`IllegalStateException`), calls waiting at the turning point.

**Common mistakes:**
1. Treating dispatch and ordering as one problem.
2. First-come-first-served ordering (zig-zags: 675 floors instead of 49 above).
3. Ignoring the passenger's direction on hall calls.
4. Locking every elevator method from many threads, instead of a single writer.
5. Losing pending stops when a car goes into maintenance.

---

## 8. Interview questions and answers

**Q1. What scheduling problems are there in an elevator system?**
> Two: dispatch (which car answers a hall call), handled by the controller with a `DispatchStrategy`; and ordering (in what order one car serves its stops), handled inside the car with LOOK.

**Q2. Explain LOOK. Why not FCFS?**
> Keep moving in the current direction while there are requests further that way, stopping on the way, then reverse. FCFS serves in arrival order and zig-zags: in the demo, 675 floors versus 49.

**Q3. Why do hall calls need a direction?**
> So the car doesn't pick up a passenger going the opposite way. A DOWN call at floor 5 is only served when the car passes 5 going down (or turns there).

**Q4. How do you make it thread-safe without locks?**
> Single writer: buttons put immutable request objects into a concurrent queue; one tick thread applies them and moves the cars. Only that thread ever touches elevator state.

**Q5. How does your dispatch cost work?**
> The distance, plus a penalty if the car is moving away or in the wrong direction (it has to finish its sweep first), plus the pending stop count to spread load. Ties go to the lowest id.

**Q6. What happens when a car goes into maintenance?**
> It returns its pending stops; the controller re-dispatches them to other cars. Car calls to it are ignored until it's back.

**Q7. Why model requests as objects?**
> The Command pattern: they can be queued between threads, logged for debugging, and replayed in tests.

### Follow-up questions

**F1. "Add a capacity limit."**
> Track the passenger count or weight per car; when it's full, skip hall calls (don't stop to pick up) but still serve car calls. Dispatch adds a penalty for nearly-full cars.

**F2. "Add an express elevator that only serves floors 0 and 20–30."**
> Give each car a set of served floors; dispatch filters out cars that can't serve the floor; car calls to other floors are rejected.

**F3. "Add a fire / emergency mode."**
> A new state: all cars cancel their stops, go to the ground floor and open their doors; hall calls are ignored until the mode is cleared.

**F4. "Optimise for the morning rush (everyone going up from the lobby)."**
> A different dispatch strategy: keep idle cars parked at the lobby, or divide the upper floors into zones served by specific cars. That's why dispatch is a strategy.

**F5. "How would you test this?"**
> Ticks make time discrete and deterministic: press buttons, call `tick()` N times, and check the stop order. That's exactly what the demo does.

---

## 9. Running the file

```bash
java ElevatorSystem.java      # 12/12 tests passed (prints LOOK 49 floors vs FCFS 675 floors)
```

**One-line summary:** *"Split it into dispatch (a cost-based Strategy picks the car) and ordering (each car runs LOOK on two sorted stop sets, respecting the passenger's direction); button presses are Command objects in a concurrent inbox, applied by a single tick thread, so elevators need no locks."*
