/*
 * ============================================================================
 *  PILLAR 3 : INHERITANCE
 * ============================================================================
 *
 *  Run:  java Inheritance.java
 *
 *  DEFINITION
 *    A class (subclass / child) ACQUIRES the fields and methods of another
 *    class (superclass / parent) using 'extends'. Models an "IS-A" relationship:
 *        Dog IS-A Animal,  SavingsAccount IS-A Account.
 *
 *  TYPES IN JAVA
 *    Single       : B extends A
 *    Multilevel   : C extends B extends A
 *    Hierarchical : B extends A, C extends A
 *    Multiple     : NOT allowed with classes (class C extends A, B -> error)
 *                   because of the DIAMOND PROBLEM (if A and B both have
 *                   foo(), which one does C get?). Java allows multiple
 *                   inheritance of INTERFACES instead.
 *
 *  WHAT IS / ISN'T INHERITED
 *    Inherited     : public & protected members (and package-private ones in the same package)
 *    Not inherited : private members (they exist in the object, just not accessible),
 *                    constructors (but the child MUST call one via super(...))
 *
 *  KEYWORDS
 *    extends   - inherit from a class
 *    super     - refer to the parent: super(...) constructor, super.method()
 *    @Override - ask the compiler to verify you're really overriding
 *    final     - final class: can't be extended (String);
 *                final method: can't be overridden
 *
 *  WHY USE IT
 *    - Reuse common code
 *    - Enables POLYMORPHISM (treat a Dog as an Animal)
 *  WHY BE CAREFUL
 *    - Tight coupling: child depends on the parent's internals ("fragile base class")
 *    - Deep hierarchies get rigid. Often COMPOSITION is better
 *      (see 03-Composition-vs-Inheritance).
 *    - Use inheritance only for a TRUE is-a relationship where the child can
 *      replace the parent everywhere (Liskov Substitution, see SOLID).
 */

import java.util.*;

public class Inheritance {

    public static void main(String[] args) {
        basicInheritance();
        constructorChaining();
        overridingRules();
        superKeyword();
        castingInHierarchy();
    }

    // =================================================================
    // 1. Basic inheritance - reuse + specialization
    // =================================================================
    static class Employee {
        protected final String name;
        protected final double baseSalary;

        Employee(String name, double baseSalary) {
            this.name = name;
            this.baseSalary = baseSalary;
        }

        double monthlyPay() { return baseSalary / 12; }

        String role() { return "Employee"; }

        // Shared behaviour inherited by all subclasses as-is
        String payslip() {
            return String.format("%-8s %-10s %,10.0f / month", name, role(), monthlyPay());
        }
    }

    static class Manager extends Employee {             // Manager IS-A Employee
        private final double bonus;

        Manager(String name, double baseSalary, double bonus) {
            super(name, baseSalary);                      // build the Employee part first
            this.bonus = bonus;
        }

        @Override
        double monthlyPay() { return super.monthlyPay() + bonus / 12; }   // extend parent logic

        @Override
        String role() { return "Manager"; }
    }

    static class Intern extends Employee {
        Intern(String name, double stipend) { super(name, stipend); }

        @Override
        String role() { return "Intern"; }
        // monthlyPay() inherited unchanged
    }

    static void basicInheritance() {
        System.out.println("===== 1. BASIC INHERITANCE =====");
        List<Employee> staff = List.of(
                new Employee("Ravi", 1_200_000),
                new Manager("Asha", 2_400_000, 600_000),
                new Intern("Kabir", 240_000));
        for (Employee e : staff) System.out.println(e.payslip());   // payslip() written ONCE
    }

    // =================================================================
    // 2. Constructor chaining across the hierarchy
    //    The FIRST line of every constructor is either this(...) or super(...).
    //    If you write neither, the compiler inserts super() (no-arg).
    //    If the parent has NO no-arg constructor, you MUST call super(args).
    // =================================================================
    static class A {
        A() { System.out.println("   A()"); }
    }

    static class B extends A {
        B() { System.out.println("   B()"); }               // implicit super() runs A() first
    }

    static class C extends B {
        C(String msg) { System.out.println("   C(" + msg + ")"); }
    }

    static void constructorChaining() {
        System.out.println("\n===== 2. CONSTRUCTOR CHAINING =====");
        new C("hi");   // prints A(), B(), C(hi): top of the hierarchy is built first
    }

    // =================================================================
    // 3. Method overriding rules
    //    - Same name + same parameter list
    //    - Return type: same or a SUBTYPE (covariant return)
    //    - Access: same or WIDER (protected -> public OK, public -> private NOT OK)
    //    - Checked exceptions: same, narrower, or none (NOT broader)
    //    - Can't override: final, static (static is HIDDEN, not overridden), private
    // =================================================================
    static class Shape {
        protected Shape copy() { return new Shape(); }
        static String kind() { return "Shape (static)"; }
        void draw() throws Exception { System.out.println("   drawing shape"); }
        final String id() { return "shape-id"; }
    }

    static class Circle extends Shape {
        @Override
        public Circle copy() { return new Circle(); }      // wider access + covariant return

        static String kind() { return "Circle (static)"; } // HIDES Shape.kind(), does NOT override

        @Override
        void draw() { System.out.println("   drawing circle"); }   // throws nothing: narrower is OK

        // String id() { ... }   // COMPILE ERROR: id() is final in Shape
    }

    static void overridingRules() {
        System.out.println("\n===== 3. OVERRIDING RULES =====");
        Shape s = new Circle();
        try {
            s.draw();                      // runtime type decides -> Circle.draw()
        } catch (Exception ignored) {}
        System.out.println("   copy() returns " + s.copy().getClass().getSimpleName());
        // Static methods are chosen by the DECLARED type, at compile time:
        System.out.println("   Shape.kind()  = " + Shape.kind() + ", Circle.kind() = " + Circle.kind());
    }

    // =================================================================
    // 4. super - reach the parent's version
    //    super.field also works, but avoid fields with the same name in parent
    //    and child (FIELD HIDING): fields are NOT polymorphic.
    // =================================================================
    static class Vehicle {
        String type = "vehicle";
        String start() { return "Vehicle starting"; }
    }

    static class ElectricCar extends Vehicle {
        String type = "electric car";                       // HIDES the parent's field (bad practice)

        @Override
        String start() { return super.start() + " -> battery check -> silent start"; }

        String types() { return "this.type=" + this.type + ", super.type=" + super.type; }
    }

    static void superKeyword() {
        System.out.println("\n===== 4. super =====");
        ElectricCar car = new ElectricCar();
        System.out.println("   " + car.start());
        System.out.println("   " + car.types());
        Vehicle v = car;
        System.out.println("   via Vehicle ref: v.type = " + v.type + "  (fields use the DECLARED type!)");
        System.out.println("   via Vehicle ref: v.start() = " + v.start() + "  (methods use the RUNTIME type)");
    }

    // =================================================================
    // 5. Upcasting & downcasting
    //    Upcast   (child -> parent): automatic and always safe
    //    Downcast (parent -> child): needs an explicit cast; check with instanceof
    //                                first, or you get ClassCastException
    // =================================================================
    static void castingInHierarchy() {
        System.out.println("\n===== 5. UPCASTING & DOWNCASTING =====");
        Employee e = new Manager("Diya", 3_000_000, 500_000);   // upcast
        // e.bonus  -> not visible: the reference type is Employee

        if (e instanceof Manager m) {                            // safe downcast (pattern matching)
            System.out.println("   downcast ok, role = " + m.role());
        }

        Employee plain = new Employee("Meera", 900_000);
        try {
            Manager wrong = (Manager) plain;                     // plain is NOT a Manager
            System.out.println(wrong);
        } catch (ClassCastException ex) {
            System.out.println("   unsafe downcast -> ClassCastException");
        }
    }
}
