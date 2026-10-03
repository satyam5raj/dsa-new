/*
 * ============================================================================
 *  TOPIC 8 : MODERN JAVA (11 -> 21)
 *            Records, Sealed classes, Pattern matching, Switch expressions,
 *            Text blocks, var, Record patterns, Virtual threads
 * ============================================================================
 *
 *  Run:  java ModernJavaDemo.java      (needs Java 21)
 *
 *  FEATURE TIMELINE (LTS versions in brackets)
 *   Java 10   var (local type inference)
 *   [Java 11] String: isBlank, strip, lines, repeat; Files.readString; HttpClient
 *   Java 14   switch expressions (arrow + yield); helpful NullPointerException messages
 *   Java 15   text blocks """ ... """
 *   Java 16   records; pattern matching for instanceof; Stream.toList()
 *   [Java 17] sealed classes
 *   [Java 21] pattern matching for switch; record patterns; virtual threads;
 *             sequenced collections (getFirst/getLast/reversed on List etc.)
 *
 *  Senior interviews increasingly expect you to write Java 17+ style code.
 */

import java.util.*;
import java.util.concurrent.*;

public class ModernJavaDemo {

    public static void main(String[] args) throws Exception {
        records();
        sealedClasses();
        patternMatchingInstanceof();
        patternMatchingSwitch();
        recordPatterns();
        textBlocks();
        sequencedCollections();
        virtualThreads();
    }

    // =================================================================
    // 1. RECORDS (Java 16)
    //    A concise, IMMUTABLE data carrier. This one line:
    //        record Point(int x, int y) {}
    //    auto-generates:
    //      - private final fields x, y
    //      - a canonical constructor Point(int x, int y)
    //      - accessors x() and y()   (note: NOT getX())
    //      - equals(), hashCode(), toString() based on all fields
    //    Rules: implicitly final (can't be extended), can't extend another
    //    class (already extends java.lang.Record), CAN implement interfaces,
    //    can have static fields/methods and extra instance methods,
    //    can NOT declare extra instance fields.
    //    Shallowly immutable: a List field can still be mutated -> copy it.
    // =================================================================
    record Point(int x, int y) {
        // Static factory / constants are allowed
        static final Point ORIGIN = new Point(0, 0);

        // Extra methods are allowed
        double distanceTo(Point other) {
            return Math.hypot(x - other.x, y - other.y);
        }
    }

    record Range(int start, int end) {
        // COMPACT constructor: validation without repeating the parameter list.
        // Field assignment happens automatically after this block.
        Range {
            if (start > end) throw new IllegalArgumentException("start > end: " + start + " > " + end);
        }

        int length() { return end - start; }
    }

    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members);   // defensive copy -> truly immutable
        }
    }

    static void records() {
        System.out.println("===== 1. RECORDS =====");
        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        System.out.println("toString      = " + p1);
        System.out.println("accessor x()  = " + p1.x());
        System.out.println("equals        = " + p1.equals(p2) + ", same hashCode = " + (p1.hashCode() == p2.hashCode()));
        System.out.println("distance      = " + p1.distanceTo(Point.ORIGIN));

        // Records make perfect HashMap keys and DTOs
        Map<Point, String> grid = new HashMap<>();
        grid.put(new Point(1, 1), "treasure");
        System.out.println("map lookup    = " + grid.get(new Point(1, 1)));

        try {
            new Range(5, 1);
        } catch (IllegalArgumentException e) {
            System.out.println("compact ctor  = " + e.getMessage());
        }

        List<String> names = new ArrayList<>(List.of("a", "b"));
        Team team = new Team("core", names);
        names.add("c");                                     // doesn't leak into the record
        System.out.println("defensive copy= " + team.members());

        // "Wither" style: create a modified copy (records have no setters)
        Point moved = new Point(p1.x() + 1, p1.y());
        System.out.println("modified copy = " + moved);
    }

    // =================================================================
    // 2. SEALED CLASSES / INTERFACES (Java 17)
    //    Restrict WHICH classes may extend/implement a type.
    //        sealed interface Shape permits Circle, Square, Rectangle
    //    Every permitted subclass must be one of:
    //        final      - no further subclassing
    //        sealed     - further restricted
    //        non-sealed - open again to anyone
    //    Why? Models a CLOSED set of variants (like enums, but each variant
    //    can carry different data). The compiler then KNOWS all cases, so a
    //    switch over a sealed type needs no 'default' (exhaustiveness check).
    //    Records are final, so they're great permitted subtypes.
    // =================================================================
    sealed interface Shape permits Circle, Square, Rectangle {}
    record Circle(double radius) implements Shape {}
    record Square(double side) implements Shape {}
    static non-sealed class Rectangle implements Shape {     // open for extension
        final double w, h;
        Rectangle(double w, double h) { this.w = w; this.h = h; }
    }

    static double area(Shape s) {
        // Exhaustive switch: no default needed. If you add a new permitted
        // Shape, this method stops compiling until you handle it. That's the point!
        return switch (s) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square sq -> sq.side() * sq.side();
            case Rectangle r -> r.w * r.h;
        };
    }

    static void sealedClasses() {
        System.out.println("\n===== 2. SEALED CLASSES =====");
        List<Shape> shapes = List.of(new Circle(1), new Square(2), new Rectangle(2, 3));
        for (Shape s : shapes) {
            System.out.printf("%-35s area = %.2f%n", s.getClass().getSimpleName(), area(s));
        }
    }

    // =================================================================
    // 3. PATTERN MATCHING FOR instanceof (Java 16)
    //    Test + cast + bind a variable in one step.
    // =================================================================
    static void patternMatchingInstanceof() {
        System.out.println("\n===== 3. PATTERN MATCHING instanceof =====");
        Object obj = "Hello Pattern";

        // Old way
        if (obj instanceof String) {
            String s = (String) obj;
            System.out.println("old way length = " + s.length());
        }

        // New way: 's' is in scope only where the match is guaranteed
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("new way length = " + s.length());
        }

        // Typical equals() implementation becomes shorter
        System.out.println("equals via pattern = " + new Money(10, "INR").equals(new Money(10, "INR")));
    }

    static final class Money {
        final long amount;
        final String currency;
        Money(long amount, String currency) { this.amount = amount; this.currency = currency; }

        @Override
        public boolean equals(Object o) {
            return o instanceof Money m && amount == m.amount && currency.equals(m.currency);
        }

        @Override
        public int hashCode() { return Objects.hash(amount, currency); }
    }

    // =================================================================
    // 4. PATTERN MATCHING FOR switch (Java 21)
    //    switch on ANY type, with type patterns, guards ('when'), and null.
    // =================================================================
    static String describe(Object o) {
        return switch (o) {
            case null -> "it's null";                               // null handled explicitly
            case Integer i when i < 0 -> "negative int " + i;       // guard with 'when'
            case Integer i -> "int " + i;
            case String s when s.isBlank() -> "blank string";
            case String s -> "string of length " + s.length();
            case int[] arr -> "int array of size " + arr.length;
            case List<?> list -> "list with " + list.size() + " items";
            default -> "something else: " + o.getClass().getSimpleName();
        };
        // ORDER MATTERS: a more specific case must come before a general one
        // (Integer i when i<0 before Integer i), otherwise compile error "dominated".
    }

    static void patternMatchingSwitch() {
        System.out.println("\n===== 4. PATTERN MATCHING switch =====");
        Object[] inputs = {42, -7, "", "hello", new int[3], List.of(1, 2), 3.14, null};
        for (Object in : inputs) System.out.println(describe(in));
    }

    // =================================================================
    // 5. RECORD PATTERNS (Java 21) - DECONSTRUCT a record right in the pattern
    // =================================================================
    record Line(Point from, Point to) {}

    sealed interface Expr permits Num, Add, Mul {}
    record Num(int value) implements Expr {}
    record Add(Expr left, Expr right) implements Expr {}
    record Mul(Expr left, Expr right) implements Expr {}

    // A tiny expression evaluator - sealed types + record patterns shine here
    static int eval(Expr e) {
        return switch (e) {
            case Num(int v) -> v;
            case Add(Expr l, Expr r) -> eval(l) + eval(r);
            case Mul(Num(var zero), Expr ignored) when zero == 0 -> 0;   // nested pattern + guard
            case Mul(Expr l, Expr r) -> eval(l) * eval(r);
        };
    }

    static void recordPatterns() {
        System.out.println("\n===== 5. RECORD PATTERNS =====");
        Object obj = new Line(new Point(0, 0), new Point(3, 4));

        // Nested deconstruction: pull x1,y1,x2,y2 out in one go
        if (obj instanceof Line(Point(var x1, var y1), Point(var x2, var y2))) {
            System.out.println("line length = " + Math.hypot(x2 - x1, y2 - y1));
        }

        // (2 + 3) * 4
        Expr expr = new Mul(new Add(new Num(2), new Num(3)), new Num(4));
        System.out.println("eval (2 + 3) * 4 = " + eval(expr));
    }

    // =================================================================
    // 6. TEXT BLOCKS (Java 15) - multi-line strings without \n and escaping
    //    Indentation is stripped based on the closing """ position.
    // =================================================================
    static void textBlocks() {
        System.out.println("\n===== 6. TEXT BLOCKS =====");
        String json = """
                {
                  "name": "Satyam",
                  "skills": ["Java", "TypeScript"]
                }
                """;
        String sql = """
                SELECT id, name
                FROM users
                WHERE active = true\
                """;   // the trailing \ joins lines (no newline at the end)
        System.out.print(json);
        System.out.println(sql);
        System.out.println("formatted: " + """
                Hello %s, you have %d messages""".formatted("Satyam", 3));
    }

    // =================================================================
    // 7. SEQUENCED COLLECTIONS (Java 21)
    //    Uniform first/last access for ordered collections.
    // =================================================================
    static void sequencedCollections() {
        System.out.println("\n===== 7. SEQUENCED COLLECTIONS =====");
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        System.out.println("getFirst/getLast = " + list.getFirst() + " / " + list.getLast());
        list.addFirst(0);
        System.out.println("addFirst(0)      = " + list + ", reversed view = " + list.reversed());

        LinkedHashMap<String, Integer> lhm = new LinkedHashMap<>();
        lhm.put("a", 1);
        lhm.put("b", 2);
        System.out.println("firstEntry       = " + lhm.firstEntry() + ", lastEntry = " + lhm.lastEntry());
    }

    // =================================================================
    // 8. VIRTUAL THREADS (Java 21) - quick preview (full coverage in the
    //    Multithreading topic). Very lightweight threads managed by the JVM,
    //    not the OS. You can create MILLIONS. Ideal for blocking I/O work
    //    (HTTP calls, DB queries): when a virtual thread blocks, the JVM
    //    parks it and frees the underlying OS ("carrier") thread.
    // =================================================================
    static void virtualThreads() throws Exception {
        System.out.println("\n===== 8. VIRTUAL THREADS =====");
        long start = System.currentTimeMillis();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                int id = i;
                futures.add(exec.submit(() -> {
                    Thread.sleep(100);            // simulated blocking I/O
                    return id;
                }));
            }
            long sum = 0;
            for (Future<Integer> f : futures) sum += f.get();
            System.out.println("10,000 tasks each sleeping 100ms finished, sum = " + sum);
        } // ExecutorService is AutoCloseable in Java 21: close() waits for tasks
        System.out.println("took ~" + (System.currentTimeMillis() - start) + " ms (not 1000 seconds!)");
    }
}
