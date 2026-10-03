/*
 * ============================================================================
 *  TOPIC 4 : INTERFACES vs ABSTRACT CLASSES
 * ============================================================================
 *
 *  Run:  java InterfacesVsAbstractClasses.java
 *
 *  ------------------------------------------------------------------------------
 *                         INTERFACE                      ABSTRACT CLASS
 *  ------------------------------------------------------------------------------
 *   Keyword               interface / implements         abstract class / extends
 *   Multiple?             a class can implement MANY     a class can extend only ONE
 *   State (fields)        only public static final       any fields (instance state)
 *                         constants
 *   Constructors          no                             yes (called via super())
 *   Methods               abstract (implicitly public),  abstract + concrete,
 *                         default (Java 8),              any access modifier
 *                         static (Java 8),
 *                         private (Java 9)
 *   Access of members     public (private helpers only)  public/protected/package/private
 *   Purpose               CAPABILITY / contract:         shared IDENTITY + code:
 *                         "can do X" (Comparable,        "is a kind of X" with common
 *                         Runnable, Serializable)        state (AbstractList, HttpServlet)
 *   Relationship          CAN-DO                          IS-A
 *  ------------------------------------------------------------------------------
 *
 *  WHEN TO USE WHICH
 *   Interface:
 *    - Defining a contract that unrelated classes can fulfil
 *      (a Car and a Phone can both be Chargeable)
 *    - You want multiple "types" for one class
 *    - The default choice for APIs and dependency injection (Spring beans)
 *   Abstract class:
 *    - Closely related classes share STATE and code
 *    - You need constructors or non-public members
 *    - Template method: fix the skeleton of an algorithm, let subclasses
 *      fill in steps
 *   BEST OF BOTH (the JDK's own approach): an interface for the contract plus
 *   an abstract "skeletal" class implementing it, e.g.
 *       List (interface)  <-  AbstractList (abstract class)  <-  ArrayList
 *
 *  JAVA 8+ BLURRED THE LINE: interfaces got default methods. But they still
 *  can't hold instance STATE, which is the key remaining difference.
 */

import java.util.*;

public class InterfacesVsAbstractClasses {

    public static void main(String[] args) {
        interfacesAsCapabilities();
        abstractClassWithState();
        interfaceMembers();
        diamondProblem();
        skeletalImplementation();
    }

    // =================================================================
    // 1. Interfaces = capabilities that UNRELATED classes can share
    // =================================================================
    interface Chargeable {
        int MAX_VOLTAGE = 240;          // implicitly public static final
        void charge(int minutes);        // implicitly public abstract
    }

    interface Trackable {
        String location();
    }

    // A Phone and an ElectricScooter have nothing in common as "types"...
    static class Phone implements Chargeable, Trackable {   // MULTIPLE interfaces
        int battery = 20;
        public void charge(int minutes) { battery = Math.min(100, battery + minutes); }
        public String location() { return "in pocket"; }
    }

    static class ElectricScooter implements Chargeable, Trackable {
        int battery = 5;
        public void charge(int minutes) { battery = Math.min(100, battery + minutes / 2); }
        public String location() { return "GPS 18.52N, 73.85E"; }
    }

    static void interfacesAsCapabilities() {
        System.out.println("===== 1. INTERFACES AS CAPABILITIES =====");
        // ...but code can treat them uniformly through the capability
        List<Chargeable> devices = List.of(new Phone(), new ElectricScooter());
        for (Chargeable c : devices) {
            c.charge(60);
            String where = (c instanceof Trackable t) ? t.location() : "unknown";
            System.out.println("   charged " + c.getClass().getSimpleName() + " at max " + Chargeable.MAX_VOLTAGE + "V, location: " + where);
        }
    }

    // =================================================================
    // 2. Abstract class = shared STATE + shared code + required steps
    // =================================================================
    static abstract class Account {
        private static int nextId = 1;
        protected final int id;                 // instance state (impossible in an interface)
        protected double balance;
        private final List<String> history = new ArrayList<>();

        protected Account(double opening) {     // constructor (impossible in an interface)
            this.id = nextId++;
            this.balance = opening;
        }

        // Shared concrete logic
        public final void endOfMonth() {
            double interest = monthlyInterest();         // step supplied by subclass
            double fee = monthlyFee();                   // step supplied by subclass
            balance += interest - fee;
            history.add(String.format("interest +%.2f, fee -%.2f", interest, fee));
        }

        protected abstract double monthlyInterest();
        protected double monthlyFee() { return 0; }      // optional override (a "hook")

        List<String> history() { return List.copyOf(history); }
    }

    static class SavingsAccount extends Account {
        SavingsAccount(double opening) { super(opening); }
        protected double monthlyInterest() { return balance * 0.04 / 12; }
    }

    static class CurrentAccount extends Account {
        CurrentAccount(double opening) { super(opening); }
        protected double monthlyInterest() { return 0; }
        @Override
        protected double monthlyFee() { return 50; }
    }

    static void abstractClassWithState() {
        System.out.println("\n===== 2. ABSTRACT CLASS WITH STATE =====");
        List<Account> accounts = List.of(new SavingsAccount(100_000), new CurrentAccount(100_000));
        for (Account a : accounts) {
            a.endOfMonth();
            System.out.printf("   #%d %-15s balance=%.2f  %s%n", a.id, a.getClass().getSimpleName(), a.balance, a.history());
        }
        // new Account(5);  // COMPILE ERROR: abstract classes can't be instantiated
    }

    // =================================================================
    // 3. All kinds of interface members (Java 8 / 9+)
    // =================================================================
    interface Validator<T> {
        boolean validate(T value);                               // abstract

        default Validator<T> and(Validator<T> other) {           // default: inherited behaviour
            return v -> validate(v) && other.validate(v);
        }

        default Validator<T> withLogging(String name) {
            return v -> {
                boolean ok = validate(v);
                log(name, v, ok);
                return ok;
            };
        }

        private void log(String name, T v, boolean ok) {         // private helper (Java 9)
            System.out.println("      [" + name + "] " + v + " -> " + ok);
        }

        static Validator<String> notBlank() {                    // static factory
            return s -> s != null && !s.isBlank();
        }

        static Validator<String> maxLength(int n) {
            return s -> s.length() <= n;
        }
    }

    static void interfaceMembers() {
        System.out.println("\n===== 3. INTERFACE MEMBERS (default/static/private) =====");
        Validator<String> username = Validator.notBlank().and(Validator.maxLength(8)).withLogging("username");
        username.validate("satyam");
        username.validate("averyveryverylongname");
        username.validate("  ");
    }

    // =================================================================
    // 4. Diamond problem with default methods
    //    Rules when the same default method comes from several places:
    //      1. A CLASS method wins over any interface default.
    //      2. A more SPECIFIC interface wins (child interface over parent).
    //      3. Otherwise it's ambiguous: you MUST override, and can pick one
    //         with X.super.method().
    // =================================================================
    interface Camera { default String takePhoto() { return "Camera photo"; } }
    interface Scanner { default String takePhoto() { return "Scanner image"; } }

    static class SmartDevice implements Camera, Scanner {
        @Override
        public String takePhoto() {                       // must resolve the conflict
            return Camera.super.takePhoto() + " + " + Scanner.super.takePhoto();
        }
    }

    static void diamondProblem() {
        System.out.println("\n===== 4. DIAMOND PROBLEM =====");
        System.out.println("   " + new SmartDevice().takePhoto());
    }

    // =================================================================
    // 5. Interface + skeletal abstract class (how the JDK does it)
    //    Extend AbstractList: implement only get() and size(), and you get
    //    iterator, contains, indexOf, equals, hashCode, toString, subList...
    // =================================================================
    static class RangeList extends AbstractList<Integer> {
        private final int start, end;
        RangeList(int start, int end) { this.start = start; this.end = end; }

        @Override
        public Integer get(int index) {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
            return start + index;
        }

        @Override
        public int size() { return end - start; }
    }

    static void skeletalImplementation() {
        System.out.println("\n===== 5. SKELETAL IMPLEMENTATION =====");
        List<Integer> range = new RangeList(10, 15);   // uses no memory for elements
        System.out.println("   list       = " + range);
        System.out.println("   contains 12= " + range.contains(12));
        System.out.println("   indexOf 14 = " + range.indexOf(14));
        System.out.println("   subList    = " + range.subList(1, 3));
        System.out.println("   equals     = " + range.equals(List.of(10, 11, 12, 13, 14)));
    }
}
