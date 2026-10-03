/*
 * ============================================================================
 *  LLD 02 : ELEVATOR SYSTEM
 * ============================================================================
 *  Run:  java ElevatorSystem.java          (design notes: README.md in this folder)
 *
 *     [model]      Direction, HallCall, CarCall (requests = COMMAND objects)
 *     [elevator]   Elevator: a state machine + the LOOK scheduling algorithm
 *     [dispatch]   DispatchStrategy: which car answers a hall call (Strategy)
 *     [observer]   ElevatorListener -> FloorDisplay
 *     [service]    ElevatorController: thread-safe inbox + a single-threaded tick loop
 *     [demo]       main(): LOOK order, direction-aware hall calls, dispatch, maintenance,
 *                  LOOK vs FCFS travel, and a concurrency stress test
 *
 *  TWO DIFFERENT SCHEDULING PROBLEMS (say this in the interview!)
 *    1. DISPATCH: a person on floor 7 presses UP -> WHICH elevator goes?   (DispatchStrategy)
 *    2. ORDERING: one elevator has stops {3, 9, 5, 1} -> in WHICH ORDER?  (LOOK inside Elevator)
 *
 *  LOOK ALGORITHM (the "elevator algorithm"): keep moving in the current direction while
 *  there is any request further that way; serve stops on the way; then reverse.
 *  (SCAN goes all the way to the last floor before reversing; LOOK turns at the last
 *   REQUEST, so it's never worse.)  FCFS serves in arrival order and zig-zags.
 *
 *  CONCURRENCY MODEL: requests arrive from many threads (buttons on every floor), but
 *  elevator STATE is mutated by ONE thread only (the tick loop). Buttons just drop a
 *  command into a ConcurrentLinkedQueue. This "single writer / event loop" design needs no
 *  locks on Elevator at all, and it's how real controllers and actor systems work.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ElevatorSystem {

    // =====================================================================
    // [model]
    // =====================================================================

    enum Direction { UP, DOWN, IDLE }

    /** A COMMAND: an immutable request object that can be queued, logged and replayed. */
    sealed interface Request permits HallCall, CarCall {}
    /** Pressed on a floor: "I am on floor f and want to go UP/DOWN". The system picks a car. */
    record HallCall(int floor, Direction direction) implements Request {}
    /** Pressed inside car e: "take me to floor f". Must be served by THAT car. */
    record CarCall(int elevatorId, int floor) implements Request {}

    enum ElevatorState { IDLE, MOVING, DOORS_OPEN, MAINTENANCE }

    interface ElevatorListener { void onStop(int elevatorId, int floor, Direction dir); }

    // =====================================================================
    // [elevator] state machine + LOOK
    // =====================================================================

    static class Elevator {
        final int id;
        final int minFloor, maxFloor;
        private int floor;
        private Direction direction = Direction.IDLE;
        private ElevatorState state = ElevatorState.IDLE;
        // Stops to make while travelling UP, and while travelling DOWN.
        // TreeSet gives ordered "next stop above/below" queries in O(log n).
        private final TreeSet<Integer> upStops = new TreeSet<>();
        private final TreeSet<Integer> downStops = new TreeSet<>();
        private final List<ElevatorListener> listeners;
        long floorsTravelled = 0;

        Elevator(int id, int minFloor, int maxFloor, int startFloor, List<ElevatorListener> listeners) {
            this.id = id; this.minFloor = minFloor; this.maxFloor = maxFloor; this.floor = startFloor; this.listeners = listeners;
        }

        // ---- accepting work ----

        /** Car call: direction comes from where the target is RELATIVE to the car right now. */
        void addCarStop(int target) {
            validate(target);
            if (target > floor) upStops.add(target);
            else if (target < floor) downStops.add(target);
            else (direction == Direction.DOWN ? downStops : upStops).add(target);   // "here": open doors next tick
        }

        /**
         * Hall call: the PASSENGER's direction matters. Someone on 5 going DOWN must not be
         * picked up while the car passes 5 on its way UP (they'd ride the wrong way).
         */
        void addHallStop(int f, Direction wanted) {
            validate(f);
            (wanted == Direction.UP ? upStops : downStops).add(f);
        }

        private void validate(int f) {
            if (f < minFloor || f > maxFloor) throw new IllegalArgumentException("No floor " + f);
            if (state == ElevatorState.MAINTENANCE) throw new IllegalStateException("Elevator " + id + " is in maintenance");
        }

        // ---- the clock tick: move one floor OR stop at this floor ----

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

        private boolean shouldStopHere() {
            return switch (direction) {
                case UP -> upStops.contains(floor) || (!anyAbove() && downStops.contains(floor));     // turning point
                case DOWN -> downStops.contains(floor) || (!anyBelow() && upStops.contains(floor));
                case IDLE -> upStops.contains(floor) || downStops.contains(floor);
            };
        }

        private void serveHere() {
            switch (direction) {
                case UP -> { upStops.remove(floor); if (!anyAbove()) downStops.remove(floor); }
                case DOWN -> { downStops.remove(floor); if (!anyBelow()) upStops.remove(floor); }
                case IDLE -> { upStops.remove(floor); downStops.remove(floor); }
            }
            state = ElevatorState.DOORS_OPEN;
            for (ElevatorListener l : listeners) l.onStop(id, floor, direction);
        }

        private boolean anyAbove() { return upStops.higher(floor) != null || downStops.higher(floor) != null; }
        private boolean anyBelow() { return upStops.lower(floor) != null || downStops.lower(floor) != null; }

        // ---- maintenance: a state transition that hands back unfinished work ----

        Map<Integer, Direction> enterMaintenance() {
            Map<Integer, Direction> orphaned = new TreeMap<>();
            upStops.forEach(f -> orphaned.put(f, Direction.UP));
            downStops.forEach(f -> orphaned.put(f, Direction.DOWN));
            upStops.clear(); downStops.clear();
            state = ElevatorState.MAINTENANCE;
            direction = Direction.IDLE;
            return orphaned;
        }

        void exitMaintenance() { if (state == ElevatorState.MAINTENANCE) state = ElevatorState.IDLE; }

        boolean isIdle() { return upStops.isEmpty() && downStops.isEmpty() && state != ElevatorState.DOORS_OPEN; }
        int pendingStops() { return upStops.size() + downStops.size(); }
        int floor() { return floor; }
        Direction direction() { return direction; }
        ElevatorState state() { return state; }
    }

    // =====================================================================
    // [dispatch] which elevator answers a hall call
    // =====================================================================

    interface DispatchStrategy { Optional<Elevator> choose(List<Elevator> cars, HallCall call); }

    /**
     * Cost-based dispatch ("nearest car"):
     *   idle car                                 -> distance
     *   moving TOWARDS the floor, same direction -> distance          (pick them up on the way)
     *   otherwise                                -> distance + penalty (must finish its sweep first)
     * plus a small load term so one car doesn't take everything.
     */
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

    /** Naive alternative, useful to compare: ignores position entirely. */
    static class RoundRobinDispatch implements DispatchStrategy {
        private final AtomicInteger next = new AtomicInteger();
        public Optional<Elevator> choose(List<Elevator> cars, HallCall call) {
            List<Elevator> ok = cars.stream().filter(e -> e.state() != ElevatorState.MAINTENANCE).toList();
            return ok.isEmpty() ? Optional.empty() : Optional.of(ok.get(Math.floorMod(next.getAndIncrement(), ok.size())));
        }
    }

    // =====================================================================
    // [observer]
    // =====================================================================

    static class FloorDisplay implements ElevatorListener {
        final List<String> stops = new CopyOnWriteArrayList<>();
        public void onStop(int id, int floor, Direction dir) { stops.add("E" + id + "@" + floor); }
    }

    // =====================================================================
    // [service] controller: thread-safe inbox, single-threaded state changes
    // =====================================================================

    static class ElevatorController {
        private final List<Elevator> cars = new ArrayList<>();
        private final DispatchStrategy dispatch;
        private final Queue<Request> inbox = new ConcurrentLinkedQueue<>();   // any thread may add
        private final List<ElevatorListener> listeners = new CopyOnWriteArrayList<>();

        ElevatorController(int count, int minFloor, int maxFloor, DispatchStrategy dispatch) {
            this.dispatch = dispatch;
            for (int i = 0; i < count; i++) cars.add(new Elevator(i, minFloor, maxFloor, minFloor, listeners));
        }

        // ---- called from ANY thread (buttons) ----
        void pressHall(int floor, Direction dir) { inbox.add(new HallCall(floor, dir)); }
        void pressInCar(int elevatorId, int floor) { inbox.add(new CarCall(elevatorId, floor)); }

        // ---- called ONLY by the tick thread ----
        void tick() {
            Request r;
            while ((r = inbox.poll()) != null) route(r);                  // apply queued commands
            for (Elevator e : cars) e.step();
        }

        private void route(Request r) {
            switch (r) {                                                  // pattern matching on a sealed interface
                case HallCall h -> dispatch.choose(cars, h)
                        .orElseThrow(() -> new IllegalStateException("No elevator in service"))
                        .addHallStop(h.floor(), h.direction());
                case CarCall c -> {
                    Elevator e = cars.get(c.elevatorId());
                    if (e.state() != ElevatorState.MAINTENANCE) e.addCarStop(c.floor());
                }
            }
        }

        /** Take a car out of service; its pending hall work is re-dispatched to others. */
        void setMaintenance(int id) {
            Map<Integer, Direction> orphaned = cars.get(id).enterMaintenance();
            orphaned.forEach((f, d) -> route(new HallCall(f, d)));
        }

        void addListener(ElevatorListener l) { listeners.add(l); }
        boolean allIdle() { return inbox.isEmpty() && cars.stream().allMatch(Elevator::isIdle); }
        Elevator car(int id) { return cars.get(id); }
        List<Elevator> cars() { return cars; }

        int runUntilIdle(int maxTicks) {
            int t = 0;
            while (!allIdle() && t < maxTicks) { tick(); t++; }
            return t;
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static long fcfsDistance(int start, List<Integer> stops) {
        long d = 0;
        int at = start;
        for (int s : stops) { d += Math.abs(s - at); at = s; }
        return d;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("1. LOOK ordering inside one car");
        ElevatorController one = new ElevatorController(1, 0, 10, new NearestCarDispatch());
        FloorDisplay d1 = new FloorDisplay();
        one.addListener(d1);
        one.pressInCar(0, 5); one.pressInCar(0, 3); one.pressInCar(0, 8);
        one.runUntilIdle(100);
        check("stops 5,3,8 served as 3,5,8", "[E0@3, E0@5, E0@8]", d1.stops);
        one.pressInCar(0, 6); one.pressHall(2, Direction.UP);
        one.runUntilIdle(100);
        check("from 8: down to 6, then 2", "[E0@3, E0@5, E0@8, E0@6, E0@2]", d1.stops);

        System.out.println("2. Hall-call DIRECTION matters");
        ElevatorController two = new ElevatorController(1, 0, 10, new NearestCarDispatch());
        FloorDisplay d2 = new FloorDisplay();
        two.addListener(d2);
        two.pressInCar(0, 9);
        two.tick(); two.tick();                                     // car is moving up past floor 1
        two.pressHall(5, Direction.DOWN);                           // wants DOWN: don't stop on the way up
        two.pressHall(6, Direction.UP);                             // wants UP: stop on the way up
        two.runUntilIdle(100);
        check("UP call at 6 on the way, DOWN call at 5 after turning", "[E0@6, E0@9, E0@5]", d2.stops);

        System.out.println("3. Dispatch: the nearest suitable car answers");
        ElevatorController three = new ElevatorController(3, 0, 20, new NearestCarDispatch());
        three.pressInCar(1, 10);
        three.pressInCar(2, 18);
        three.runUntilIdle(100);                                    // cars at 0, 10, 18
        three.pressHall(12, Direction.DOWN);
        three.tick();
        check("car at 10 answers floor 12", "[0, 1, 0]", three.cars().stream().map(Elevator::pendingStops).toList());

        System.out.println("4. Maintenance: pending hall calls are re-dispatched");
        ElevatorController four = new ElevatorController(2, 0, 10, new NearestCarDispatch());
        four.pressHall(7, Direction.DOWN);
        four.tick();                                               // car 0 (lowest id on a tie) takes it
        check("car 0 took the call", 1, four.car(0).pendingStops());
        four.setMaintenance(0);
        check("car 1 inherited it", 1, four.car(1).pendingStops());
        four.runUntilIdle(100);
        check("served by car 1", 7, four.car(1).floor());
        check("car 0 stayed put", ElevatorState.MAINTENANCE, four.car(0).state());
        four.pressInCar(0, 3);
        four.tick();
        check("car call to a car in maintenance is ignored", 0, four.car(0).pendingStops());

        System.out.println("5. LOOK vs FCFS: total floors travelled for 30 random stops");
        Random rnd = new Random(7);
        List<Integer> stops = new ArrayList<>();
        for (int i = 0; i < 30; i++) stops.add(rnd.nextInt(50));
        ElevatorController look = new ElevatorController(1, 0, 49, new NearestCarDispatch());
        for (int s : stops) look.pressInCar(0, s);
        look.runUntilIdle(10_000);
        long lookDist = look.car(0).floorsTravelled, fcfs = fcfsDistance(0, stops);
        System.out.printf("   LOOK %d floors vs FCFS %d floors%n", lookDist, fcfs);
        check("LOOK travels less", true, lookDist < fcfs);

        System.out.println("6. Concurrency: 8 threads press 4,000 buttons while the controller ticks");
        ElevatorController busy = new ElevatorController(4, 0, 30, new NearestCarDispatch());
        Set<Integer> requested = ConcurrentHashMap.newKeySet();
        Set<Integer> visited = ConcurrentHashMap.newKeySet();
        busy.addListener((id, floor, dir) -> visited.add(floor));
        AtomicBoolean running = new AtomicBoolean(true);
        Thread ticker = new Thread(() -> { while (running.get()) busy.tick(); });
        ticker.start();
        ExecutorService people = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) people.submit(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 500; i++) {
                int f = r.nextInt(31);
                requested.add(f);
                if (r.nextBoolean()) busy.pressHall(f, f == 30 ? Direction.DOWN : f == 0 ? Direction.UP : r.nextBoolean() ? Direction.UP : Direction.DOWN);
                else busy.pressInCar(r.nextInt(4), f);
            }
        });
        people.shutdown();
        people.awaitTermination(10, TimeUnit.SECONDS);
        running.set(false);
        ticker.join();
        int ticks = busy.runUntilIdle(100_000);
        check("every request served (all idle)", true, busy.allIdle());
        check("every requested floor was visited", true, visited.containsAll(requested));
        System.out.println("   drained the remaining work in " + ticks + " ticks; no locks on Elevator needed");
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
