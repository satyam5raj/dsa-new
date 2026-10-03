/*
 * ============================================================================
 *  TOPIC 7.1 : OBJECT EQUALITY - the equals() / hashCode() contract
 * ============================================================================
 *
 *  Run:  java EqualsHashCodeContract.java
 *
 *  ==        : IDENTITY. Do both references point to the SAME object?
 *  equals()  : LOGICAL EQUALITY. Do they represent the same VALUE?
 *              Object's default equals() is just ==.
 *
 *  THE equals() CONTRACT (from the Object Javadoc). For non-null x, y, z:
 *    1. Reflexive  : x.equals(x) is true
 *    2. Symmetric  : x.equals(y) == y.equals(x)
 *    3. Transitive : x.equals(y) && y.equals(z)  =>  x.equals(z)
 *    4. Consistent : repeated calls give the same answer if nothing changed
 *    5. Non-null   : x.equals(null) is false (never throws)
 *
 *  THE hashCode() CONTRACT
 *    1. If x.equals(y), then x.hashCode() == y.hashCode()        <- MUST
 *    2. If !x.equals(y), the hash codes MAY still be equal (a collision),
 *       but fewer collisions = faster HashMaps
 *    3. Consistent while the fields used in equals don't change
 *
 *  => RULE: override BOTH or NEITHER. If you override only equals(), equal
 *     objects land in different HashMap buckets and lookups fail.
 *
 *  RECIPE FOR equals()
 *    1. if (this == o) return true;                       // fast path
 *    2. if (!(o instanceof MyType m)) return false;       // also handles null
 *       (or getClass() != o.getClass(), see the inheritance section)
 *    3. compare significant fields:
 *         primitives with ==  (double: Double.compare)
 *         objects with Objects.equals(a, b)  (null-safe)
 *  RECIPE FOR hashCode()
 *    Objects.hash(field1, field2, ...)   - use the SAME fields as equals()
 *    (manual version: result = 31 * result + field.hashCode(). Why 31? It's an
 *     odd prime, and 31 * i == (i << 5) - i, which is cheap to compute.)
 *
 *  EASIEST CORRECT OPTION TODAY: use a RECORD. It generates both for you.
 */

import java.util.*;

public class EqualsHashCodeContract {

    public static void main(String[] args) {
        identityVsEquality();
        onlyEqualsOverridden();
        correctImplementation();
        symmetryTrapWithInheritance();
        mutableKeyTrap();
        comparableConsistency();
    }

    // =================================================================
    // 1. == vs equals
    // =================================================================
    static void identityVsEquality() {
        System.out.println("===== 1. IDENTITY vs EQUALITY =====");
        String a = new String("pune");
        String b = new String("pune");
        System.out.println("   a == b      : " + (a == b));
        System.out.println("   a.equals(b) : " + a.equals(b));
    }

    // =================================================================
    // 2. BUG: equals() overridden, hashCode() NOT
    // =================================================================
    static class BrokenEmployee {
        final String id;
        BrokenEmployee(String id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            return o instanceof BrokenEmployee e && id.equals(e.id);
        }
        // hashCode() missing -> uses identity hash -> equal objects, different hashes
    }

    static void onlyEqualsOverridden() {
        System.out.println("\n===== 2. ONLY equals() OVERRIDDEN =====");
        BrokenEmployee e1 = new BrokenEmployee("E1");
        BrokenEmployee e2 = new BrokenEmployee("E1");
        System.out.println("   e1.equals(e2) = " + e1.equals(e2));

        Set<BrokenEmployee> set = new HashSet<>(List.of(e1));
        System.out.println("   set.contains(e2) = " + set.contains(e2) + "  <- WRONG, should be true");
        Map<BrokenEmployee, String> map = new HashMap<>();
        map.put(e1, "Engineering");
        System.out.println("   map.get(e2) = " + map.get(e2) + "  <- lookup fails");
    }

    // =================================================================
    // 3. Correct implementation (by hand) + the record equivalent
    // =================================================================
    static final class Employee {
        private final String id;
        private final String name;
        private final double salary;
        private final transient int cachedHash;   // optional: cache the hash for immutable objects

        Employee(String id, String name, double salary) {
            this.id = Objects.requireNonNull(id);
            this.name = name;
            this.salary = salary;
            this.cachedHash = Objects.hash(id, name, salary);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                          // 1. same reference
            if (!(o instanceof Employee other)) return false;    // 2. type check + null check
            return id.equals(other.id)                           // 3. field comparison
                    && Objects.equals(name, other.name)          //    null-safe for nullable fields
                    && Double.compare(salary, other.salary) == 0; //   handles NaN and -0.0 correctly
        }

        @Override
        public int hashCode() {
            return cachedHash;                                   // the same fields as equals()
        }

        @Override
        public String toString() { return "Employee[" + id + ", " + name + "]"; }
    }

    // Same semantics in one line
    record EmployeeRecord(String id, String name, double salary) {}

    static void correctImplementation() {
        System.out.println("\n===== 3. CORRECT IMPLEMENTATION =====");
        Employee a = new Employee("E1", "Ravi", 100);
        Employee b = new Employee("E1", "Ravi", 100);
        Employee c = new Employee("E1", null, 100);
        System.out.println("   equals: " + a.equals(b) + ", same hash: " + (a.hashCode() == b.hashCode()));
        System.out.println("   null-safe name compare: " + c.equals(new Employee("E1", null, 100)));
        System.out.println("   reflexive: " + a.equals(a) + ", non-null: " + a.equals(null));
        Set<Employee> set = new HashSet<>(List.of(a));
        System.out.println("   HashSet contains equal copy: " + set.contains(b));

        System.out.println("   record equals: " + new EmployeeRecord("E1", "Ravi", 100).equals(new EmployeeRecord("E1", "Ravi", 100)));
    }

    // =================================================================
    // 4. The inheritance trap: equals + subclass with extra fields
    //    Point(1,2) vs ColorPoint(1,2,RED). Using instanceof in both:
    //      point.equals(colorPoint) -> true  (Point ignores color)
    //      colorPoint.equals(point) -> false (point isn't a ColorPoint)
    //    SYMMETRY BROKEN. Fixing symmetry then breaks transitivity.
    //    Effective Java: "There is no way to extend an instantiable class and
    //    add a value component while preserving the equals contract."
    //    Options: getClass() comparison (sacrifices LSP), or (better)
    //    COMPOSITION, or make value classes FINAL / records.
    // =================================================================
    static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object o) { return o instanceof Point p && x == p.x && y == p.y; }
        @Override
        public int hashCode() { return Objects.hash(x, y); }
    }

    static class ColorPoint extends Point {
        final String color;
        ColorPoint(int x, int y, String color) { super(x, y); this.color = color; }
        @Override
        public boolean equals(Object o) { return o instanceof ColorPoint cp && super.equals(cp) && color.equals(cp.color); }
        @Override
        public int hashCode() { return Objects.hash(x, y, color); }
    }

    // Composition fix: ColorPoint HAS-A Point instead of IS-A Point
    record PointR(int x, int y) {}
    record ColorPointR(PointR point, String color) {}

    static void symmetryTrapWithInheritance() {
        System.out.println("\n===== 4. INHERITANCE BREAKS SYMMETRY =====");
        Point p = new Point(1, 2);
        ColorPoint cp = new ColorPoint(1, 2, "RED");
        System.out.println("   p.equals(cp)  = " + p.equals(cp));
        System.out.println("   cp.equals(p)  = " + cp.equals(p) + "   <- not symmetric!");
        ColorPointR fixed = new ColorPointR(new PointR(1, 2), "RED");
        System.out.println("   composition: fixed.point().equals(new PointR(1,2)) = " + fixed.point().equals(new PointR(1, 2)));
    }

    // =================================================================
    // 5. Mutating a field used in hashCode while the object is in a HashSet
    // =================================================================
    static class MutableUser {
        String email;
        MutableUser(String email) { this.email = email; }
        @Override
        public boolean equals(Object o) { return o instanceof MutableUser u && Objects.equals(email, u.email); }
        @Override
        public int hashCode() { return Objects.hashCode(email); }
    }

    static void mutableKeyTrap() {
        System.out.println("\n===== 5. MUTABLE KEY TRAP =====");
        MutableUser u = new MutableUser("old@x.com");
        Set<MutableUser> users = new HashSet<>(List.of(u));
        u.email = "new@x.com";                                   // hash changes, but it's stored in the OLD bucket
        System.out.println("   set.contains(u)        = " + users.contains(u) + "  <- the object is 'lost'");
        System.out.println("   set.size()             = " + users.size());
        System.out.println("   remove(u) succeeded?   = " + users.remove(u) + "  <- memory leak risk");
        System.out.println("   => use IMMUTABLE objects as keys (see ImmutableClassDesign.java)");
    }

    // =================================================================
    // 6. compareTo should be CONSISTENT with equals
    //    TreeSet/TreeMap use compareTo, NOT equals. If compareTo says 0 for
    //    objects that aren't equal, the TreeSet silently drops one.
    //    Famous JDK example: new BigDecimal("1.0") vs new BigDecimal("1.00")
    // =================================================================
    static void comparableConsistency() {
        System.out.println("\n===== 6. compareTo vs equals =====");
        java.math.BigDecimal one = new java.math.BigDecimal("1.0");
        java.math.BigDecimal oneOO = new java.math.BigDecimal("1.00");
        System.out.println("   equals    : " + one.equals(oneOO) + " (scale differs)");
        System.out.println("   compareTo : " + one.compareTo(oneOO));
        System.out.println("   HashSet size = " + new HashSet<>(List.of(one, oneOO)).size()
                + ", TreeSet size = " + new TreeSet<>(List.of(one, oneOO)).size() + "  <- different answers!");
    }
}
