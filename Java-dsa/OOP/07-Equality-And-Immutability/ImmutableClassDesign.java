/*
 * ============================================================================
 *  TOPIC 7.2 : DESIGNING IMMUTABLE CLASSES
 * ============================================================================
 *
 *  Run:  java ImmutableClassDesign.java
 *
 *  IMMUTABLE OBJECT = its state can NEVER change after construction.
 *  JDK examples: String, Integer (all wrappers), BigDecimal, LocalDate, List.of(...), records.
 *
 *  THE 5 RULES (Effective Java, Item 17) + 1
 *    1. Don't provide setters or any methods that modify state
 *    2. Make the class FINAL (so no subclass can add mutable state or
 *       override methods to fake mutation). Or use a private constructor
 *       with a static factory.
 *    3. Make all fields PRIVATE and FINAL
 *    4. DEFENSIVE COPY mutable inputs in the constructor
 *       (the caller still holds a reference to the list/array/Date it passed)
 *    5. DEFENSIVE COPY (or return unmodifiable views of) mutable fields in getters
 *    6. "Modification" methods return a NEW object (withX(), plus(), add())
 *
 *  WHY IMMUTABILITY (great senior-level talking points)
 *    - THREAD-SAFE for free: no locks, can be shared across threads safely
 *      (final fields also get special visibility guarantees in the Java Memory Model)
 *    - Safe HashMap keys / HashSet elements (the hash never changes)
 *    - Can CACHE the hashCode and reuse instances (Integer cache, String pool)
 *    - FAILURE ATOMICITY: an object is never left half-modified after an exception
 *    - Easier reasoning: no "who changed this?" debugging
 *    - Safe to share without copying ("defensive copies" become unnecessary for callers)
 *  COST
 *    - A new object for every change. Usually negligible (young-gen GC is
 *      cheap). For heavy step-by-step building, use a mutable companion
 *      (String -> StringBuilder) or a Builder.
 */

import java.time.LocalDate;
import java.util.*;

public class ImmutableClassDesign {

    public static void main(String[] args) {
        brokenImmutability();
        properImmutability();
        withersAndSharing();
        recordsAndShallowImmutability();
        unmodifiableVsImmutable();
    }

    // =================================================================
    // 1. Looks immutable (final fields, no setters)... but isn't
    // =================================================================
    static final class LeakyPassenger {
        private final String name;
        private final List<String> bookings;       // final REFERENCE, but the LIST is mutable
        private final Date dob;                    // java.util.Date is mutable

        LeakyPassenger(String name, List<String> bookings, Date dob) {
            this.name = name;
            this.bookings = bookings;              // stores the caller's list (no copy)
            this.dob = dob;                        // stores the caller's Date
        }

        List<String> getBookings() { return bookings; }   // hands out the internal list
        Date getDob() { return dob; }
    }

    static void brokenImmutability() {
        System.out.println("===== 1. BROKEN IMMUTABILITY =====");
        List<String> list = new ArrayList<>(List.of("PNR1"));
        Date dob = new Date(0);
        LeakyPassenger p = new LeakyPassenger("Ravi", list, dob);

        list.add("PNR-INJECTED-VIA-CONSTRUCTOR-ARG");   // hole #1: the caller kept a reference
        p.getBookings().add("PNR-INJECTED-VIA-GETTER"); // hole #2: the getter leaked internal state
        p.getDob().setTime(999_999_999_999L);           // hole #3: mutable Date
        System.out.println("   bookings = " + p.getBookings());
        System.out.println("   dob changed: " + (p.getDob().getTime() != 0));
        System.out.println("   'final' only freezes the REFERENCE, not the object it points to!");
    }

    // =================================================================
    // 2. Properly immutable
    // =================================================================
    static final class Passenger {                       // rule 2: final class
        private final String name;                       // rule 3: private final
        private final List<String> bookings;
        private final LocalDate dob;                     // immutable type: no copying needed
        private final int[] seatPrefs;                   // arrays are ALWAYS mutable

        Passenger(String name, List<String> bookings, LocalDate dob, int[] seatPrefs) {
            this.name = Objects.requireNonNull(name);
            this.bookings = List.copyOf(bookings);       // rule 4: defensive copy (+ immutable)
            this.dob = dob;
            this.seatPrefs = seatPrefs.clone();          // rule 4: copy the array
        }

        String name() { return name; }
        List<String> bookings() { return bookings; }     // already unmodifiable: safe to return
        LocalDate dob() { return dob; }
        int[] seatPrefs() { return seatPrefs.clone(); }  // rule 5: copy on the way out

        // rule 6: "modifiers" return NEW instances
        Passenger addBooking(String pnr) {
            List<String> next = new ArrayList<>(bookings);
            next.add(pnr);
            return new Passenger(name, next, dob, seatPrefs);
        }

        @Override
        public String toString() { return name + " " + bookings + " born " + dob + " seats " + Arrays.toString(seatPrefs); }
    }

    static void properImmutability() {
        System.out.println("\n===== 2. PROPER IMMUTABILITY =====");
        List<String> list = new ArrayList<>(List.of("PNR1"));
        int[] seats = {12, 14};
        Passenger p = new Passenger("Asha", list, LocalDate.of(1995, 5, 20), seats);

        list.add("HACK");                                         // no effect
        seats[0] = 99;                                            // no effect
        try {
            p.bookings().add("HACK");
        } catch (UnsupportedOperationException e) {
            System.out.println("   getter list is unmodifiable");
        }
        p.seatPrefs()[0] = 99;                                    // modifies a copy only
        System.out.println("   still intact: " + p);

        Passenger p2 = p.addBooking("PNR2");
        System.out.println("   original:     " + p.bookings());
        System.out.println("   new version:  " + p2.bookings());
    }

    // =================================================================
    // 3. Value objects with "withers", plus safe sharing across threads
    // =================================================================
    static final class Money {
        private final long paise;                        // store money as integer minor units
        private final String currency;

        private Money(long paise, String currency) { this.paise = paise; this.currency = currency; }

        static Money ofRupees(double rupees) { return new Money(Math.round(rupees * 100), "INR"); }  // static factory

        Money plus(Money other) {
            if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
            return new Money(paise + other.paise, currency);
        }

        Money times(int qty) { return new Money(paise * qty, currency); }

        @Override
        public boolean equals(Object o) { return o instanceof Money m && paise == m.paise && currency.equals(m.currency); }
        @Override
        public int hashCode() { return Objects.hash(paise, currency); }
        @Override
        public String toString() { return String.format("%s %d.%02d", currency, paise / 100, paise % 100); }
    }

    static void withersAndSharing() {
        System.out.println("\n===== 3. VALUE OBJECTS =====");
        Money price = Money.ofRupees(199.99);
        Money total = price.times(3).plus(Money.ofRupees(50));
        System.out.println("   price = " + price + " (unchanged), total = " + total);

        // Shared freely between threads with no synchronization: nobody can change it
        Money shared = Money.ofRupees(10);
        Runnable r = () -> shared.plus(Money.ofRupees(1));      // creates new objects, never mutates 'shared'
        Thread t1 = new Thread(r), t2 = new Thread(r);
        t1.start(); t2.start();
        try { t1.join(); t2.join(); } catch (InterruptedException ignored) {}
        System.out.println("   shared after concurrent use = " + shared);
    }

    // =================================================================
    // 4. Records are only SHALLOWLY immutable
    // =================================================================
    record Team(String name, List<String> members) {}            // members list can still change!

    record SafeTeam(String name, List<String> members) {
        SafeTeam {                                               // compact constructor
            members = List.copyOf(members);                      // deep enough for a list of Strings
        }
    }

    static void recordsAndShallowImmutability() {
        System.out.println("\n===== 4. RECORDS: SHALLOW IMMUTABILITY =====");
        List<String> m = new ArrayList<>(List.of("a"));
        Team t = new Team("core", m);
        m.add("b");
        System.out.println("   plain record changed via outside list: " + t.members());

        List<String> m2 = new ArrayList<>(List.of("a"));
        SafeTeam st = new SafeTeam("core", m2);
        m2.add("b");
        System.out.println("   record with copy in compact ctor: " + st.members());
    }

    // =================================================================
    // 5. Unmodifiable VIEW vs truly IMMUTABLE copy
    // =================================================================
    static void unmodifiableVsImmutable() {
        System.out.println("\n===== 5. UNMODIFIABLE VIEW vs IMMUTABLE COPY =====");
        List<String> source = new ArrayList<>(List.of("x"));
        List<String> view = Collections.unmodifiableList(source);   // a read-only WINDOW onto source
        List<String> copy = List.copyOf(source);                    // an independent snapshot
        source.add("y");
        System.out.println("   view = " + view + "  (sees changes through the back door)");
        System.out.println("   copy = " + copy + "      (truly immutable)");
    }
}
