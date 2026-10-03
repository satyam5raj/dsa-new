/*
 * ============================================================================
 *  PILLAR 4 : POLYMORPHISM ("many forms")
 * ============================================================================
 *
 *  Run:  java Polymorphism.java
 *
 *  DEFINITION
 *    One interface, many implementations. The SAME call (shape.area())
 *    behaves differently depending on the actual object.
 *
 *  TWO KINDS
 *  ---------------------------------------------------------------------
 *                     COMPILE-TIME (static)        RUNTIME (dynamic)
 *  ---------------------------------------------------------------------
 *   Mechanism         Method OVERLOADING           Method OVERRIDING
 *   Decided by        compiler, from the declared  JVM, from the actual object
 *                     types of the arguments       type at runtime
 *   Also called       early binding                late binding / dynamic dispatch
 *  ---------------------------------------------------------------------
 *
 *  HOW RUNTIME DISPATCH WORKS (under the hood)
 *    Each class has a "vtable" (virtual method table) of method pointers.
 *    A call like shape.area() looks up area() in the vtable of the object's
 *    REAL class. So the reference type decides WHICH methods you may call
 *    (compile-time check), and the object type decides WHICH VERSION runs.
 *
 *  WHY IT MATTERS
 *    - Write code once against a general type; new subtypes just work.
 *      Adding a Triangle doesn't require touching totalArea().
 *    - Replaces long if/else or switch-on-type chains ("replace conditional
 *      with polymorphism"). This is the heart of the Open/Closed Principle.
 */

import java.util.*;

public class Polymorphism {

    public static void main(String[] args) {
        runtimePolymorphism();
        compileTimePolymorphism();
        overloadResolutionTraps();
        replaceConditional();
    }

    // =================================================================
    // 1. Runtime polymorphism (overriding + dynamic dispatch)
    // =================================================================
    interface Shape {
        double area();
        default String name() { return getClass().getSimpleName(); }
    }

    record Circle(double r) implements Shape {
        public double area() { return Math.PI * r * r; }
    }

    record Rectangle(double w, double h) implements Shape {
        public double area() { return w * h; }
    }

    record Triangle(double base, double height) implements Shape {
        public double area() { return 0.5 * base * height; }
    }

    // Written against the abstraction: works for ANY current or future Shape
    static double totalArea(List<Shape> shapes) {
        double total = 0;
        for (Shape s : shapes) total += s.area();    // which area()? decided at runtime
        return total;
    }

    static void runtimePolymorphism() {
        System.out.println("===== 1. RUNTIME POLYMORPHISM =====");
        List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3), new Triangle(4, 5));
        for (Shape s : shapes) System.out.printf("   %-10s area = %.2f%n", s.name(), s.area());
        System.out.printf("   total = %.2f%n", totalArea(shapes));
    }

    // =================================================================
    // 2. Compile-time polymorphism (overloading)
    //    Same method name, different parameter lists.
    // =================================================================
    static class Printer {
        String print(int x) { return "int: " + x; }
        String print(double x) { return "double: " + x; }
        String print(String x) { return "String: " + x; }
        String print(int x, int y) { return "two ints: " + x + "," + y; }
        String print(Object x) { return "Object: " + x; }
    }

    static void compileTimePolymorphism() {
        System.out.println("\n===== 2. COMPILE-TIME POLYMORPHISM =====");
        Printer p = new Printer();
        System.out.println("   " + p.print(5));
        System.out.println("   " + p.print(5.5));
        System.out.println("   " + p.print("hi"));
        System.out.println("   " + p.print(1, 2));
        System.out.println("   " + p.print(List.of(1)));
    }

    // =================================================================
    // 3. Overload resolution traps (interview favorites)
    //    The compiler picks overloads by the DECLARED type, NOT the runtime type.
    //    Priority order: exact match -> widening -> boxing -> varargs
    // =================================================================
    static class Animal {
        String sound() { return "..."; }
    }

    static class Dog extends Animal {
        @Override
        String sound() { return "Woof"; }
    }

    static String greet(Animal a) { return "greet(Animal)"; }
    static String greet(Dog d) { return "greet(Dog)"; }

    static String pick(long x) { return "long (widening)"; }
    static String pick(Integer x) { return "Integer (boxing)"; }
    static String pick(int... x) { return "varargs"; }

    static void overloadResolutionTraps() {
        System.out.println("\n===== 3. OVERLOAD RESOLUTION TRAPS =====");
        Animal a = new Dog();                   // declared Animal, actually a Dog
        System.out.println("   a.sound()  = " + a.sound() + "   <- OVERRIDING uses the runtime type (Dog)");
        System.out.println("   greet(a)   = " + greet(a) + "   <- OVERLOADING uses the declared type (Animal)");

        int i = 5;
        System.out.println("   pick(int)  = " + pick(i) + "   <- widening beats boxing beats varargs");
    }

    // =================================================================
    // 4. Replace conditional with polymorphism
    // =================================================================

    // BEFORE: a switch on type. Every new type means editing this method
    // (and every other switch like it scattered across the codebase).
    static double shippingCostSwitch(String type, double weightKg) {
        return switch (type) {
            case "STANDARD" -> 40 + 10 * weightKg;
            case "EXPRESS" -> 100 + 20 * weightKg;
            case "SAME_DAY" -> 250 + 30 * weightKg;
            default -> throw new IllegalArgumentException(type);
        };
    }

    // AFTER: each type owns its rule. Adding a new one = adding a new class.
    interface ShippingMethod {
        double cost(double weightKg);
    }

    static class Standard implements ShippingMethod {
        public double cost(double w) { return 40 + 10 * w; }
    }

    static class Express implements ShippingMethod {
        public double cost(double w) { return 100 + 20 * w; }
    }

    static class SameDay implements ShippingMethod {
        public double cost(double w) { return 250 + 30 * w; }
    }

    static class Drone implements ShippingMethod {           // NEW type, nothing else changed
        public double cost(double w) {
            if (w > 2) throw new IllegalArgumentException("drone max 2kg");
            return 500;
        }
    }

    static void replaceConditional() {
        System.out.println("\n===== 4. REPLACE CONDITIONAL WITH POLYMORPHISM =====");
        System.out.println("   switch version EXPRESS 2kg = " + shippingCostSwitch("EXPRESS", 2));
        List<ShippingMethod> methods = List.of(new Standard(), new Express(), new SameDay(), new Drone());
        for (ShippingMethod m : methods) {
            System.out.printf("   %-8s 1.5kg = %.0f%n", m.getClass().getSimpleName(), m.cost(1.5));
        }
    }
}
