/*
 * ============================================================================
 *  SOLID - L : LISKOV SUBSTITUTION PRINCIPLE (LSP)
 * ============================================================================
 *
 *  Run:  java L_LiskovSubstitution.java
 *
 *  "Objects of a superclass should be replaceable with objects of its
 *   subclasses WITHOUT breaking the program."            - Barbara Liskov
 *
 *  In other words: a subclass must honour the parent's CONTRACT, meaning its
 *  behaviour and promises, not just its method signatures. The compiler
 *  checks signatures. LSP is about BEHAVIOUR, so the compiler can't check it.
 *
 *  THE RULES (design by contract)
 *    1. Preconditions can't be STRENGTHENED in a subtype
 *       (if Parent accepts any amount > 0, Child can't demand > 100)
 *    2. Postconditions can't be WEAKENED
 *       (if Parent promises a sorted result, Child must also return sorted)
 *    3. Invariants must be preserved
 *       (if a Rectangle's width and height are independent, a subtype must keep that)
 *    4. No NEW exceptions the caller doesn't expect
 *       (an overridden method that throws UnsupportedOperationException
 *       is a classic LSP violation)
 *
 *  RED FLAGS
 *    - Overridden methods that throw "not supported" or do nothing
 *    - Callers doing  if (x instanceof SpecialSubtype) { ... }
 *    - "IS-A in real life" that isn't IS-A in BEHAVIOUR (Square vs Rectangle)
 *
 *  FIX: rethink the hierarchy. Split interfaces by capability, or use
 *  composition. LSP violations are usually a sign that inheritance was the
 *  wrong tool.
 */

import java.util.*;

public class L_LiskovSubstitution {

    public static void main(String[] args) {
        squareRectangleProblem();
        birdProblem();
        readOnlyCollectionsInJdk();
    }

    // =================================================================
    // 1. The classic: Square extends Rectangle
    //    Geometry says a square IS-A rectangle. Behaviour says NO:
    //    a Rectangle promises that setWidth() doesn't change the height.
    // =================================================================
    static class Rectangle {
        protected int width, height;
        void setWidth(int w) { width = w; }
        void setHeight(int h) { height = h; }
        int area() { return width * height; }
    }

    static class Square extends Rectangle {
        // To stay a square, changing one side must change the other
        @Override
        void setWidth(int w) { width = w; height = w; }
        @Override
        void setHeight(int h) { width = h; height = h; }
    }

    // Client code written against Rectangle's contract
    static void resizeAndCheck(Rectangle r) {
        r.setWidth(5);
        r.setHeight(4);
        int expected = 20;
        String verdict = r.area() == expected ? "OK" : "BROKEN (expected " + expected + ")";
        System.out.println("   " + r.getClass().getSimpleName() + " area = " + r.area() + " -> " + verdict);
    }

    // LSP-friendly design: no mutable shared contract; each shape is its own immutable type
    interface Shape { int area(); }
    record Rect(int width, int height) implements Shape { public int area() { return width * height; } }
    record Sq(int side) implements Shape { public int area() { return side * side; } }

    static void squareRectangleProblem() {
        System.out.println("===== 1. SQUARE / RECTANGLE =====");
        resizeAndCheck(new Rectangle());
        resizeAndCheck(new Square());           // substitution breaks the caller

        System.out.println("   fix -> separate immutable types behind a Shape interface:");
        for (Shape s : List.of(new Rect(5, 4), new Sq(4))) {
            System.out.println("     " + s + " area = " + s.area());
        }
    }

    // =================================================================
    // 2. Bird / Penguin: a subtype that can't do what the parent promises
    // =================================================================
    static class Bird {
        String fly() { return "flying high"; }
    }

    static class Penguin extends Bird {
        @Override
        String fly() { throw new UnsupportedOperationException("Penguins can't fly"); }  // LSP violation
    }

    static void migrate(List<Bird> birds) {
        for (Bird b : birds) {
            try {
                System.out.println("   " + b.getClass().getSimpleName() + ": " + b.fly());
            } catch (UnsupportedOperationException e) {
                System.out.println("   " + b.getClass().getSimpleName() + ": CRASH -> " + e.getMessage());
            }
        }
    }

    // FIX: model capabilities separately. Only things that fly are FlyingBirds.
    interface Animal { String name(); }
    interface Flyer { String fly(); }
    interface Swimmer { String swim(); }

    record Sparrow(String name) implements Animal, Flyer {
        public String fly() { return name + " flies"; }
    }

    record Emperor(String name) implements Animal, Swimmer {
        public String swim() { return name + " swims"; }
    }

    static void migrateSafely(List<Flyer> flyers) {          // can only receive things that CAN fly
        flyers.forEach(f -> System.out.println("   " + f.fly()));
    }

    static void birdProblem() {
        System.out.println("\n===== 2. BIRD / PENGUIN =====");
        migrate(List.of(new Bird(), new Penguin()));

        System.out.println("   fix -> capability interfaces:");
        migrateSafely(List.of(new Sparrow("sparrow")));
        // migrateSafely(List.of(new Emperor("penguin")));  // COMPILE ERROR: now the type system protects us
        System.out.println("   " + new Emperor("penguin").swim());
    }

    // =================================================================
    // 3. The JDK itself bends LSP: List.of() returns a List whose add() throws.
    //    Code that accepts a List and calls add() can blow up depending on
    //    which List it gets. Know this trade-off for interviews.
    // =================================================================
    static void addDefault(List<String> list) {
        try {
            list.add("default");
            System.out.println("   added to " + list.getClass().getSimpleName());
        } catch (UnsupportedOperationException e) {
            System.out.println("   " + list.getClass().getSimpleName() + " rejected add() -> substitutability broken");
        }
    }

    static void readOnlyCollectionsInJdk() {
        System.out.println("\n===== 3. JDK EXAMPLE =====");
        addDefault(new ArrayList<>());
        addDefault(List.of());
        System.out.println("   lesson: accept the narrowest abstraction you need, and document mutability");
    }
}
