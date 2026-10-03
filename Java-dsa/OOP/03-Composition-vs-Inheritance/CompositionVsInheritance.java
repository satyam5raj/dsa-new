/*
 * ============================================================================
 *  TOPIC 3 : COMPOSITION vs INHERITANCE
 * ============================================================================
 *
 *  Run:  java CompositionVsInheritance.java
 *
 *  INHERITANCE = "IS-A"   class Car extends Vehicle
 *  COMPOSITION = "HAS-A"  class Car { private final Engine engine; }
 *               The class is BUILT FROM other objects and DELEGATES work to them.
 *
 *  Related terms:
 *    Association : objects know about each other (Teacher <-> Student)
 *    Aggregation : weak HAS-A; the part can live on its own (Team has Players)
 *    Composition : strong HAS-A; the part's life depends on the whole
 *                  (House has Rooms; destroy the house and the rooms go too)
 *
 *  "FAVOR COMPOSITION OVER INHERITANCE" (Effective Java, Item 18; Gang of Four)
 *  WHY?
 *   1. FRAGILE BASE CLASS: a subclass depends on the parent's IMPLEMENTATION
 *      details. A harmless-looking change in the parent can break children.
 *      (Demo 1 below: the classic InstrumentedHashSet bug.)
 *   2. RIGID: the hierarchy is fixed at COMPILE time. Composition can swap
 *      behaviour at RUNTIME (pass a different Engine).
 *   3. CLASS EXPLOSION: combining features through inheritance multiplies
 *      classes (FlyingSwimmingDuck, FlyingQuackingDuck...). Composition
 *      combines independent pieces.
 *   4. Single inheritance only: you get ONE extends. You can hold many components.
 *   5. Inheritance exposes ALL parent methods, even ones that make no sense
 *      for the child (java.util.Stack extends Vector, so you can insert into
 *      the MIDDLE of a "stack"!).
 *
 *  WHEN INHERITANCE IS RIGHT
 *   - A true, stable IS-A relationship, where the subclass can be used
 *     anywhere the parent is expected (Liskov Substitution).
 *   - The parent was DESIGNED and DOCUMENTED for extension (abstract classes,
 *     the template method pattern).
 *   - You own both classes (the same team controls changes).
 *
 *  QUICK TEST: "Would it make sense for B to be used EVERYWHERE A is used,
 *  with all of A's methods?" If not, use composition.
 */

import java.util.*;

public class CompositionVsInheritance {

    public static void main(String[] args) {
        fragileBaseClass();
        classExplosionVsComposition();
        runtimeFlexibility();
        stackMistake();
    }

    // =================================================================
    // 1. FRAGILE BASE CLASS - the famous InstrumentedHashSet example
    //    Goal: count how many elements were ever ADDED to a set.
    // =================================================================

    // INHERITANCE version - looks right, is WRONG
    static class InstrumentedHashSet<E> extends HashSet<E> {
        int addCount = 0;

        @Override
        public boolean add(E e) {
            addCount++;
            return super.add(e);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            addCount += c.size();
            return super.addAll(c);   // HashSet.addAll internally calls add() for each element!
        }                             // -> each element counted TWICE
    }

    // COMPOSITION version - wraps ANY Set and forwards calls ("forwarding" / decorator)
    static class InstrumentedSet<E> {
        private final Set<E> inner;   // HAS-A set
        int addCount = 0;

        InstrumentedSet(Set<E> inner) { this.inner = inner; }

        boolean add(E e) {
            addCount++;
            return inner.add(e);
        }

        boolean addAll(Collection<? extends E> c) {
            addCount += c.size();
            return inner.addAll(c);   // whatever inner does internally can't affect OUR count
        }

        int size() { return inner.size(); }
    }

    static void fragileBaseClass() {
        System.out.println("===== 1. FRAGILE BASE CLASS =====");
        InstrumentedHashSet<String> bad = new InstrumentedHashSet<>();
        bad.addAll(List.of("a", "b", "c"));
        System.out.println("   inheritance addCount = " + bad.addCount + "  (expected 3!)");

        InstrumentedSet<String> good = new InstrumentedSet<>(new HashSet<>());
        good.addAll(List.of("a", "b", "c"));
        System.out.println("   composition addCount = " + good.addCount);

        // Bonus: composition works with ANY Set implementation
        InstrumentedSet<String> sorted = new InstrumentedSet<>(new TreeSet<>());
        sorted.add("x");
        System.out.println("   works with TreeSet too, addCount = " + sorted.addCount);
    }

    // =================================================================
    // 2. CLASS EXPLOSION vs COMPOSITION
    //    Ducks: some fly, some don't; some quack, some squeak, some are silent.
    //    With inheritance: MallardDuck, RubberDuck, WoodenDuck, RobotDuck...
    //    and a RubberDuck inherits fly() from Duck. Rubber ducks can't fly.
    //    With composition: Duck HAS-A FlyBehavior and HAS-A QuackBehavior.
    //    (This is the STRATEGY pattern.)
    // =================================================================
    interface FlyBehavior { String fly(); }
    interface QuackBehavior { String quack(); }

    static final FlyBehavior WINGS = () -> "flaps wings";
    static final FlyBehavior NO_FLY = () -> "can't fly";
    static final FlyBehavior ROCKET = () -> "rocket-powered flight!";
    static final QuackBehavior QUACK = () -> "Quack";
    static final QuackBehavior SQUEAK = () -> "Squeak";
    static final QuackBehavior SILENT = () -> "...";

    static class Duck {
        private final String name;
        private FlyBehavior flyBehavior;       // composed, and swappable
        private final QuackBehavior quackBehavior;

        Duck(String name, FlyBehavior fly, QuackBehavior quack) {
            this.name = name;
            this.flyBehavior = fly;
            this.quackBehavior = quack;
        }

        void setFlyBehavior(FlyBehavior fb) { this.flyBehavior = fb; }

        String perform() {
            return String.format("%-12s %-24s %s", name, flyBehavior.fly(), quackBehavior.quack());
        }
    }

    static void classExplosionVsComposition() {
        System.out.println("\n===== 2. COMBINING BEHAVIOURS =====");
        // 3 fly x 3 quack = 9 combos from 6 small pieces, no new classes needed
        List<Duck> ducks = List.of(
                new Duck("Mallard", WINGS, QUACK),
                new Duck("RubberDuck", NO_FLY, SQUEAK),
                new Duck("WoodenDecoy", NO_FLY, SILENT));
        ducks.forEach(d -> System.out.println("   " + d.perform()));
    }

    // =================================================================
    // 3. RUNTIME FLEXIBILITY - change behaviour while the program runs
    // =================================================================
    static void runtimeFlexibility() {
        System.out.println("\n===== 3. RUNTIME FLEXIBILITY =====");
        Duck d = new Duck("ModelDuck", NO_FLY, QUACK);
        System.out.println("   before: " + d.perform());
        d.setFlyBehavior(ROCKET);                  // impossible with a fixed class hierarchy
        System.out.println("   after:  " + d.perform());

        // Real-world example: a Car HAS-A Engine. Swap petrol for electric,
        // and Car's code doesn't change.
        Car car = new Car(new PetrolEngine());
        System.out.println("   " + car.drive());
        car = new Car(new ElectricEngine());
        System.out.println("   " + car.drive());
    }

    interface Engine { String start(); }
    static class PetrolEngine implements Engine { public String start() { return "vroom (petrol)"; } }
    static class ElectricEngine implements Engine { public String start() { return "hum (electric)"; } }

    static class Car {
        private final Engine engine;                    // HAS-A
        Car(Engine engine) { this.engine = engine; }
        String drive() { return "Car drives: " + engine.start(); }   // DELEGATION
    }

    // =================================================================
    // 4. A real JDK mistake: java.util.Stack extends Vector
    //    A stack should only allow push/pop/peek. Because it INHERITS from
    //    Vector, it also exposes add(index, e), remove(index), get(index)...
    //    which break the LIFO guarantee.
    // =================================================================
    static class GoodStack<E> {
        private final Deque<E> items = new ArrayDeque<>();   // composition: hide everything else

        void push(E e) { items.push(e); }
        E pop() { return items.pop(); }
        E peek() { return items.peek(); }
        boolean isEmpty() { return items.isEmpty(); }
        // No way to insert in the middle. The API IS the abstraction.
    }

    static void stackMistake() {
        System.out.println("\n===== 4. java.util.Stack MISTAKE =====");
        Stack<Integer> legacy = new Stack<>();
        legacy.push(1);
        legacy.push(2);
        legacy.add(0, 99);      // inserts at the BOTTOM of a "stack". Allowed. Bad.
        System.out.println("   java.util.Stack after add(0, 99) = " + legacy + " <- LIFO broken");

        GoodStack<Integer> good = new GoodStack<>();
        good.push(1);
        good.push(2);
        System.out.println("   GoodStack pop = " + good.pop() + " (only stack operations exist)");
    }
}
