/*
 * ============================================================================
 *  TOPIC 1 : CLASSES & OBJECTS - the building blocks of OOP
 * ============================================================================
 *
 *  Run:  java ClassesAndObjects.java
 *
 *  CLASS  = a BLUEPRINT. Describes what data (fields) and behaviour
 *           (methods) its objects will have. Written once.
 *  OBJECT = an INSTANCE of a class, created with 'new'. Lives on the heap.
 *           Many objects can come from one class, each with its own field values.
 *
 *      class Car { String color; void drive() {...} }   // blueprint
 *      Car myCar = new Car();                            // object #1
 *      Car yourCar = new Car();                          // object #2
 *
 *  An object has:
 *    STATE    -> field values      (color = "red")
 *    BEHAVIOR -> methods           (drive())
 *    IDENTITY -> its own place in memory (== compares identity)
 *
 *  WHAT 'new Car()' DOES
 *    1. Allocates memory on the heap for all instance fields
 *    2. Sets fields to defaults (0, false, null)
 *    3. Runs field initializers and instance initializer blocks
 *    4. Runs the constructor body
 *    5. Returns the reference
 *
 *  THIS FILE COVERS
 *    1. Fields, methods, constructors, 'this'
 *    2. Constructor overloading & chaining  this(...)
 *    3. static vs instance members
 *    4. Initialization order (static block, instance block, constructor)
 *    5. Access modifiers
 *    6. Nested / inner / local / anonymous classes
 *    7. Enums (classes with a fixed set of instances)
 *    8. The Object class (every class's parent)
 */

import java.util.*;

public class ClassesAndObjects {

    public static void main(String[] args) {
        basics();
        constructorsAndChaining();
        staticVsInstance();
        initializationOrder();
        accessModifiers();
        nestedClasses();
        enums();
        objectClass();
    }

    // =================================================================
    // 1. A simple class: fields + constructor + methods + 'this'
    // =================================================================
    static class Car {
        // FIELDS (instance variables) - each Car object has its own copy
        String brand;
        String color;
        int speed;

        // CONSTRUCTOR - same name as the class, no return type.
        // Runs once when the object is created; sets up a valid initial state.
        Car(String brand, String color) {
            // 'this' = the current object. Needed here because the parameter
            // names SHADOW the field names. this.brand is the field.
            this.brand = brand;
            this.color = color;
            this.speed = 0;
        }

        // METHODS - behaviour. They can read/change the object's own fields.
        void accelerate(int amount) {
            speed += amount;            // same as this.speed += amount
        }

        String describe() {
            return color + " " + brand + " going " + speed + " km/h";
        }
    }

    static void basics() {
        System.out.println("===== 1. CLASS & OBJECT BASICS =====");
        Car a = new Car("Tata", "red");
        Car b = new Car("Honda", "blue");
        a.accelerate(50);               // only 'a' changes
        System.out.println("a: " + a.describe());
        System.out.println("b: " + b.describe());

        Car c = a;                      // copies the REFERENCE, not the object
        c.accelerate(10);
        System.out.println("after c.accelerate: a is now " + a.speed + " (a and c are the same object)");
        System.out.println("a == c ? " + (a == c) + ", a == b ? " + (a == b));
    }

    // =================================================================
    // 2. Constructor overloading & chaining
    //    - If you write NO constructor, Java gives a default no-arg one.
    //    - If you write ANY constructor, the default one disappears.
    //    - this(...) calls another constructor of the same class.
    //      It must be the FIRST statement. Avoids duplicating logic.
    // =================================================================
    static class Pizza {
        private final String size;
        private final String crust;
        private final List<String> toppings;

        Pizza() {
            this("medium");                          // chain -> Pizza(String)
        }

        Pizza(String size) {
            this(size, "thin", List.of("cheese"));   // chain -> the "main" constructor
        }

        Pizza(String size, String crust, List<String> toppings) {
            // ALL validation lives in one place
            if (!List.of("small", "medium", "large").contains(size)) {
                throw new IllegalArgumentException("bad size " + size);
            }
            this.size = size;
            this.crust = crust;
            this.toppings = List.copyOf(toppings);
        }

        @Override
        public String toString() { return size + "/" + crust + "/" + toppings; }
    }

    static void constructorsAndChaining() {
        System.out.println("\n===== 2. CONSTRUCTORS & CHAINING =====");
        System.out.println(new Pizza());
        System.out.println(new Pizza("large"));
        System.out.println(new Pizza("small", "thick", List.of("paneer", "onion")));
        // Too many overloaded constructors get confusing ("telescoping constructors")
        // -> the BUILDER pattern fixes this (see 06-Design-Patterns).
    }

    // =================================================================
    // 3. static vs instance
    //    instance member: belongs to EACH object (this.count)
    //    static member  : belongs to the CLASS, ONE shared copy (Counter.total)
    //    - static methods can't use 'this' or instance fields directly
    //      (which object's fields would they use?)
    //    - instance methods CAN use static members
    // =================================================================
    static class Counter {
        static int totalCreated = 0;   // shared by all Counter objects
        final int id;                  // unique per object

        Counter() {
            totalCreated++;
            id = totalCreated;
        }

        static int getTotal() {        // called as Counter.getTotal()
            // return id;              // COMPILE ERROR: no 'this' in a static context
            return totalCreated;
        }
    }

    static void staticVsInstance() {
        System.out.println("\n===== 3. STATIC vs INSTANCE =====");
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        Counter c3 = new Counter();
        System.out.println("ids = " + c1.id + ", " + c2.id + ", " + c3.id);
        System.out.println("Counter.getTotal() = " + Counter.getTotal());
        // Typical static uses: constants (Math.PI), utility methods (Math.max),
        // factory methods (List.of), counters/caches shared by all instances.
    }

    // =================================================================
    // 4. Initialization order (classic interview question)
    //    Once, when the class is first used:
    //      static fields & static blocks (top to bottom)
    //    Every 'new':
    //      parent's instance init + constructor first, then
    //      instance fields & instance blocks (top to bottom), then constructor body
    // =================================================================
    static class Parent {
        static { System.out.println("   1. Parent static block"); }
        { System.out.println("   3. Parent instance block"); }
        Parent() { System.out.println("   4. Parent constructor"); }
    }

    static class Child extends Parent {
        static { System.out.println("   2. Child static block"); }
        { System.out.println("   5. Child instance block"); }
        Child() {
            super();   // implicit even if you don't write it - parent is built FIRST
            System.out.println("   6. Child constructor");
        }
    }

    static void initializationOrder() {
        System.out.println("\n===== 4. INITIALIZATION ORDER =====");
        System.out.println("first new Child():");
        new Child();
        System.out.println("second new Child() (static blocks don't run again):");
        new Child();
    }

    // =================================================================
    // 5. Access modifiers - who can see a class member?
    //    ----------------------------------------------------------------
    //    Modifier      same class   same package   subclass   everywhere
    //    private          yes          no             no          no
    //    (default)        yes          yes            no*         no
    //    protected        yes          yes            yes         no
    //    public           yes          yes            yes         yes
    //    ----------------------------------------------------------------
    //    *default = "package-private": subclasses in OTHER packages can't see it.
    //    Rule of thumb: make everything as PRIVATE as possible (encapsulation).
    //    Note: everything here is in one file/class, so the compiler allows
    //    access; the rules matter across classes and packages.
    // =================================================================
    static class Account {
        public final String owner;     // anyone can read
        protected String branch;       // subclasses + package
        String type = "savings";       // package only
        private double balance;        // only Account itself

        Account(String owner) { this.owner = owner; }

        public double getBalance() { return balance; }   // controlled read access
        public void deposit(double amt) {                // controlled write access
            if (amt <= 0) throw new IllegalArgumentException("amount must be positive");
            balance += amt;
        }
    }

    static void accessModifiers() {
        System.out.println("\n===== 5. ACCESS MODIFIERS =====");
        Account acc = new Account("Satyam");
        acc.deposit(500);
        System.out.println(acc.owner + " has " + acc.getBalance()
                + " (balance is private; changed only through deposit())");
    }

    // =================================================================
    // 6. Nested classes
    //    a) static nested class : no link to an outer object. Most common.
    //                             (e.g. Map.Entry, Builder classes, Node in LinkedList)
    //    b) inner class (non-static): every instance is tied to an OUTER
    //                             instance and can use its fields. Holds a hidden
    //                             reference to the outer object (watch memory leaks).
    //    c) local class         : declared inside a method.
    //    d) anonymous class     : no name; declared and instantiated at once.
    //    Prefer static nested unless you really need the outer instance.
    // =================================================================
    static class LinkedBag {
        private Node head;
        private int size;

        // (a) static nested: Node doesn't need to know about the LinkedBag
        private static class Node {
            int value;
            Node next;
            Node(int value, Node next) { this.value = value; this.next = next; }
        }

        void add(int v) {
            head = new Node(v, head);
            size++;
        }

        // (b) inner class: the iterator READS the outer bag's 'head' field
        class BagIterator implements Iterator<Integer> {
            private Node current = head;           // 'head' of the outer LinkedBag
            public boolean hasNext() { return current != null; }
            public Integer next() {
                int v = current.value;
                current = current.next;
                return v;
            }
        }

        Iterator<Integer> iterator() { return new BagIterator(); }
    }

    static void nestedClasses() {
        System.out.println("\n===== 6. NESTED CLASSES =====");
        LinkedBag bag = new LinkedBag();
        bag.add(1);
        bag.add(2);
        bag.add(3);
        Iterator<Integer> it = bag.iterator();
        System.out.print("inner-class iterator: ");
        while (it.hasNext()) System.out.print(it.next() + " ");
        System.out.println();

        // Creating an inner class instance from outside needs an outer object:
        LinkedBag.BagIterator it2 = bag.new BagIterator();
        System.out.println("outer.new Inner() works, first = " + it2.next());

        // (c) local class
        class Greeter {
            String greet(String n) { return "Hi " + n; }
        }
        System.out.println("local class: " + new Greeter().greet("Satyam"));

        // (d) anonymous class: implements Comparator on the spot
        Comparator<String> byLength = new Comparator<>() {
            @Override
            public int compare(String a, String b) { return Integer.compare(a.length(), b.length()); }
        };
        List<String> words = new ArrayList<>(List.of("banana", "kiwi", "apple"));
        words.sort(byLength);
        System.out.println("anonymous class comparator: " + words);
    }

    // =================================================================
    // 7. Enums - a class with a FIXED set of instances
    //    Each constant is a singleton object. Enums can have fields,
    //    constructors (always private), methods, and even per-constant
    //    behaviour. Type-safe alternative to int/String constants.
    // =================================================================
    enum OrderStatus {
        PLACED("Order received"),
        SHIPPED("On the way"),
        DELIVERED("Delivered") {
            @Override
            boolean isFinal() { return true; }      // constant-specific override
        };

        private final String label;

        OrderStatus(String label) { this.label = label; }   // implicitly private

        String label() { return label; }
        boolean isFinal() { return false; }

        OrderStatus next() {
            // values() returns all constants in declaration order
            return this == DELIVERED ? this : values()[ordinal() + 1];
        }
    }

    static void enums() {
        System.out.println("\n===== 7. ENUMS =====");
        OrderStatus s = OrderStatus.PLACED;
        while (true) {
            System.out.println(s + " (" + s.label() + "), ordinal=" + s.ordinal() + ", final=" + s.isFinal());
            if (s.isFinal()) break;
            s = s.next();
        }
        System.out.println("valueOf(\"SHIPPED\") = " + OrderStatus.valueOf("SHIPPED"));
        // EnumMap / EnumSet are super-fast collections keyed by enums (array-backed)
        EnumMap<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
        counts.put(OrderStatus.SHIPPED, 4);
        System.out.println("EnumMap = " + counts);
    }

    // =================================================================
    // 8. java.lang.Object - the root of every class
    //    Methods every object inherits:
    //      toString()  - text form (default: ClassName@hexHash)
    //      equals()    - default: same reference (==)
    //      hashCode()  - default: identity-based number
    //      getClass()  - runtime class
    //      clone(), finalize() (deprecated), wait()/notify()/notifyAll() (threads)
    // =================================================================
    static class Plain {}

    static void objectClass() {
        System.out.println("\n===== 8. THE OBJECT CLASS =====");
        Plain p = new Plain();
        System.out.println("default toString = " + p);
        System.out.println("getClass         = " + p.getClass().getSimpleName());
        System.out.println("default equals   = " + p.equals(new Plain()) + " (compares references)");
        // Overriding equals/hashCode properly -> 07-Equality-And-Immutability
    }
}
