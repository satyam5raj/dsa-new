/*
 * ============================================================================
 *  TOPIC 6.3 : OPTIONAL + other Java 8 features
 *              (default/static interface methods, Date-Time API)
 * ============================================================================
 *
 *  Run:  java OptionalAndOtherJava8.java
 *
 *  OPTIONAL<T>
 *  -----------
 *  A container that holds either ONE value or NOTHING.
 *  Purpose: make "this might be absent" EXPLICIT in a method's return type,
 *  instead of returning null and hoping the caller remembers to check.
 *
 *      Optional<User> findById(int id);   // caller is forced to think about "not found"
 *
 *  DO
 *   - Use as a RETURN type for "may not exist" results.
 *   - Chain map / filter / orElse / orElseThrow instead of if-null checks.
 *  DON'T
 *   - Use for fields, method parameters, or collections (return an empty list instead).
 *   - Call get() without checking -> NoSuchElementException (just a different NPE).
 *   - Write  if (opt.isPresent()) { opt.get() }  everywhere - that's just a null check.
 *
 *  orElse vs orElseGet  (interview favorite)
 *   orElse(x)        -> x is ALWAYS evaluated, even if a value is present.
 *   orElseGet(() ->) -> supplier runs ONLY when empty. Use for expensive defaults.
 */

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class OptionalAndOtherJava8 {

    record User(int id, String name, String email) {}

    static final Map<Integer, User> DB = Map.of(
            1, new User(1, "Ravi", "ravi@example.com"),
            2, new User(2, "Asha", null));            // Asha has no email

    // Return Optional instead of null
    static Optional<User> findById(int id) {
        return Optional.ofNullable(DB.get(id));
    }

    public static void main(String[] args) {
        creatingOptionals();
        consumingOptionals();
        chaining();
        orElseVsOrElseGet();
        interfaceDefaultAndStaticMethods();
        dateTimeApi();
    }

    // =================================================================
    // 1. Creating
    // =================================================================
    static void creatingOptionals() {
        System.out.println("===== 1. CREATING =====");
        Optional<String> present = Optional.of("value");       // value must be non-null
        Optional<String> empty = Optional.empty();
        Optional<String> maybe = Optional.ofNullable(null);   // null -> empty
        System.out.println(present + " " + empty + " " + maybe);
        try {
            Optional.of(null);
        } catch (NullPointerException e) {
            System.out.println("Optional.of(null) -> NullPointerException (use ofNullable)");
        }
    }

    // =================================================================
    // 2. Getting the value out
    // =================================================================
    static void consumingOptionals() {
        System.out.println("\n===== 2. CONSUMING =====");
        Optional<User> found = findById(1);
        Optional<User> missing = findById(99);

        System.out.println("isPresent / isEmpty = " + found.isPresent() + " / " + missing.isEmpty());
        found.ifPresent(u -> System.out.println("ifPresent: " + u.name()));
        missing.ifPresentOrElse(
                u -> System.out.println("found " + u.name()),
                () -> System.out.println("ifPresentOrElse: user 99 not found"));   // Java 9

        System.out.println("orElse    = " + missing.map(User::name).orElse("Guest"));
        try {
            missing.orElseThrow(() -> new IllegalArgumentException("No user with id 99"));
        } catch (IllegalArgumentException e) {
            System.out.println("orElseThrow: " + e.getMessage());
        }
        // Optional.get() / orElseThrow() with no args throw NoSuchElementException when empty
    }

    // =================================================================
    // 3. Chaining: map, flatMap, filter, or
    //    No nested null checks needed - each step is skipped if empty.
    // =================================================================
    static void chaining() {
        System.out.println("\n===== 3. CHAINING =====");

        // Old style null checks:
        User u = DB.get(2);
        String domainOld = "unknown";
        if (u != null && u.email() != null && u.email().contains("@")) {
            domainOld = u.email().split("@")[1];
        }

        // Optional style:
        for (int id : new int[]{1, 2, 99}) {
            String domain = findById(id)
                    .map(User::email)                    // Optional<String>, empty if email is null
                    .filter(e -> e.contains("@"))
                    .map(e -> e.split("@")[1])
                    .orElse("unknown");
            System.out.println("id " + id + " email domain = " + domain);
        }
        System.out.println("(old style for id 2 = " + domainOld + ")");

        // flatMap when the mapping function ITSELF returns Optional (avoids Optional<Optional<X>>)
        Optional<String> name = Optional.of(1).flatMap(OptionalAndOtherJava8::findById).map(User::name);
        System.out.println("flatMap result = " + name.orElse("-"));

        // or(): fallback to ANOTHER Optional (Java 9)
        Optional<User> fallback = findById(99).or(() -> findById(1));
        System.out.println("or() fallback = " + fallback.map(User::name).orElse("-"));

        // stream(): Optional -> 0 or 1 element stream (Java 9). Useful to drop empties.
        List<String> names = List.of(1, 99, 2).stream()
                .map(OptionalAndOtherJava8::findById)
                .flatMap(Optional::stream)
                .map(User::name)
                .toList();
        System.out.println("existing user names = " + names);
    }

    // =================================================================
    // 4. orElse vs orElseGet
    // =================================================================
    static String expensiveDefault() {
        System.out.println("   (expensiveDefault() called)");
        return "default";
    }

    static void orElseVsOrElseGet() {
        System.out.println("\n===== 4. orElse vs orElseGet =====");
        Optional<String> present = Optional.of("real value");
        System.out.println("orElse:");
        present.orElse(expensiveDefault());                 // called even though not needed!
        System.out.println("orElseGet:");
        present.orElseGet(OptionalAndOtherJava8::expensiveDefault); // NOT called
        System.out.println("   (nothing printed above = supplier skipped)");
    }

    // =================================================================
    // 5. Default & static methods in interfaces (Java 8)
    //    - default: interface methods WITH a body. Lets library authors add
    //      new methods to interfaces without breaking existing implementers.
    //      (This is how List got sort() and Collection got stream()!)
    //    - static: utility methods that belong to the interface itself.
    //    - Java 9 also allows private methods in interfaces (shared helpers).
    //
    //    DIAMOND PROBLEM: if a class implements two interfaces with the same
    //    default method, it MUST override it, and can pick one with
    //    InterfaceName.super.method().
    // =================================================================
    interface Vehicle {
        String name();                                  // abstract

        default String describe() {                     // default method
            return name() + " " + wheelsText();
        }

        private String wheelsText() {                   // private helper (Java 9)
            return "moves on wheels";
        }

        static Vehicle of(String name) {                // static factory
            return () -> name;                          // Vehicle is functional (1 abstract method)
        }
    }

    interface Flying {
        default String describe() { return "flies"; }
    }

    static class FlyingCar implements Vehicle, Flying {
        public String name() { return "FlyingCar"; }

        @Override
        public String describe() {                      // required: both parents have describe()
            return Vehicle.super.describe() + " AND " + Flying.super.describe();
        }
    }

    static void interfaceDefaultAndStaticMethods() {
        System.out.println("\n===== 5. DEFAULT / STATIC INTERFACE METHODS =====");
        System.out.println(Vehicle.of("Bike").describe());
        System.out.println(new FlyingCar().describe());
    }

    // =================================================================
    // 6. java.time (Date-Time API) - replaces the old, mutable, confusing
    //    java.util.Date and Calendar. All java.time classes are IMMUTABLE
    //    and thread-safe.
    //      LocalDate      date only (2026-10-03)
    //      LocalTime      time only (14:30)
    //      LocalDateTime  date + time, no zone
    //      ZonedDateTime  date + time + zone
    //      Instant        a point on the UTC timeline (for timestamps/logs)
    //      Duration       time-based amount (hours, seconds)
    //      Period         date-based amount (years, months, days)
    // =================================================================
    static void dateTimeApi() {
        System.out.println("\n===== 6. DATE-TIME API =====");
        LocalDate date = LocalDate.of(2026, 1, 15);
        LocalDate later = date.plusMonths(2).plusDays(10);     // returns NEW objects
        System.out.println("date = " + date + ", +2 months +10 days = " + later);
        System.out.println("day of week = " + date.getDayOfWeek() + ", leap year? " + date.isLeapYear());
        System.out.println("days between = " + ChronoUnit.DAYS.between(date, later));
        System.out.println("period       = " + Period.between(date, later));

        LocalDateTime meeting = LocalDateTime.of(2026, 3, 1, 14, 30);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        System.out.println("formatted    = " + meeting.format(fmt));
        System.out.println("parsed       = " + LocalDate.parse("2026-12-25"));

        Duration d = Duration.ofMinutes(135);
        System.out.println("duration     = " + d + " (" + d.toHours() + "h " + d.toMinutesPart() + "m)");

        ZonedDateTime india = meeting.atZone(ZoneId.of("Asia/Kolkata"));
        System.out.println("in New York  = " + india.withZoneSameInstant(ZoneId.of("America/New_York")));
        System.out.println("epoch instant= " + Instant.ofEpochSecond(0));
    }
}
