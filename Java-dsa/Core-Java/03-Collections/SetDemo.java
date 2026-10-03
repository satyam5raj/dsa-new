/*
 * ============================================================================
 *  TOPIC 3.2 : SET  (HashSet, LinkedHashSet, TreeSet)
 * ============================================================================
 *
 *  Run:  java SetDemo.java
 *
 *  SET = collection with NO DUPLICATES. Duplicate detection uses
 *  hashCode() + equals() (Hash sets) or compareTo()/Comparator (TreeSet).
 *
 *  ------------------------------------------------------------------------
 *   Impl            Backed by          Order             add/contains/remove
 *  ------------------------------------------------------------------------
 *   HashSet         HashMap            none (random-ish)  O(1) average
 *   LinkedHashSet   LinkedHashMap      insertion order    O(1) average
 *   TreeSet         TreeMap (Red-Black sorted              O(log n)
 *                   tree)
 *  ------------------------------------------------------------------------
 *
 *  FUN FACT: HashSet is literally a HashMap where your element is the KEY
 *  and the value is a dummy constant object called PRESENT.
 *     public boolean add(E e) { return map.put(e, PRESENT) == null; }
 */

import java.util.*;

public class SetDemo {

    public static void main(String[] args) {
        hashSet();
        linkedHashSet();
        treeSet();
        setOperations();
        customObjectsInSet();
        dsaUsage();
    }

    // =================================================================
    // 1. HashSet
    // =================================================================
    static void hashSet() {
        System.out.println("===== 1. HASHSET =====");
        Set<String> set = new HashSet<>();
        System.out.println("add java   -> " + set.add("java"));   // true (added)
        System.out.println("add python -> " + set.add("python"));
        System.out.println("add java   -> " + set.add("java"));   // false (duplicate, ignored)
        set.add(null);                                             // ONE null allowed
        System.out.println("set = " + set + "  (order NOT guaranteed)");
        System.out.println("contains(\"python\") = " + set.contains("python"));
        set.remove("python");
        System.out.println("after remove = " + set + ", size = " + set.size());
    }

    // =================================================================
    // 2. LinkedHashSet - remembers insertion order
    // =================================================================
    static void linkedHashSet() {
        System.out.println("\n===== 2. LINKEDHASHSET =====");
        Set<Integer> set = new LinkedHashSet<>(List.of(5, 1, 9, 1, 3, 5));
        System.out.println("dedupe keeping order = " + set);   // [5, 1, 9, 3]
    }

    // =================================================================
    // 3. TreeSet - sorted, plus "navigation" methods (very useful in DSA)
    // =================================================================
    static void treeSet() {
        System.out.println("\n===== 3. TREESET =====");
        TreeSet<Integer> ts = new TreeSet<>(List.of(50, 20, 80, 10, 60));
        System.out.println("sorted       = " + ts);
        System.out.println("first/last   = " + ts.first() + " / " + ts.last());
        System.out.println("floor(55)    = " + ts.floor(55));     // greatest <= 55 -> 50
        System.out.println("ceiling(55)  = " + ts.ceiling(55));   // smallest >= 55 -> 60
        System.out.println("lower(50)    = " + ts.lower(50));     // greatest <  50 -> 20
        System.out.println("higher(50)   = " + ts.higher(50));    // smallest >  50 -> 60
        System.out.println("headSet(50)  = " + ts.headSet(50));   // < 50
        System.out.println("tailSet(50)  = " + ts.tailSet(50));   // >= 50
        System.out.println("subSet(20,70)= " + ts.subSet(20, 70)); // [20, 70)
        System.out.println("descending   = " + ts.descendingSet());
        System.out.println("pollFirst    = " + ts.pollFirst() + " -> " + ts);

        // Custom ordering via Comparator: sort strings by length, then alphabetically
        TreeSet<String> byLength = new TreeSet<>(
                Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        byLength.addAll(List.of("banana", "kiwi", "fig", "apple", "pear"));
        System.out.println("by length    = " + byLength);

        // TreeSet does NOT allow null (can't compare null) -> NullPointerException
        // WARNING: TreeSet uses compareTo for equality! If the comparator says two
        // elements are 0 (equal), the second one is dropped even if equals() is false.
    }

    // =================================================================
    // 4. Set math: union, intersection, difference
    // =================================================================
    static void setOperations() {
        System.out.println("\n===== 4. SET OPERATIONS =====");
        Set<Integer> a = new HashSet<>(List.of(1, 2, 3, 4));
        Set<Integer> b = new HashSet<>(List.of(3, 4, 5, 6));

        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);
        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Integer> difference = new HashSet<>(a);
        difference.removeAll(b);

        System.out.println("A ∪ B = " + union);
        System.out.println("A ∩ B = " + intersection);
        System.out.println("A - B = " + difference);
    }

    // =================================================================
    // 5. Custom objects in a HashSet - MUST override equals() AND hashCode()
    //
    //    How HashSet.add(x) works:
    //      1. compute x.hashCode() -> pick a bucket
    //      2. in that bucket, compare with existing items using equals()
    //    If you override only equals(), two "equal" objects get different
    //    hash codes, land in different buckets, and BOTH get stored.
    //
    //    CONTRACT: if a.equals(b) then a.hashCode() == b.hashCode().
    //              (Same hashCode does NOT imply equals - that's a collision.)
    // =================================================================
    static class BadPoint {                 // no equals/hashCode -> identity based
        int x, y;
        BadPoint(int x, int y) { this.x = x; this.y = y; }
    }

    static class GoodPoint {
        final int x, y;
        GoodPoint(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                       // same reference
            if (!(o instanceof GoodPoint)) return false;      // also handles null
            GoodPoint p = (GoodPoint) o;
            return x == p.x && y == p.y;                      // compare fields
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);                        // combines field hashes
        }

        @Override
        public String toString() { return "(" + x + "," + y + ")"; }
    }

    static void customObjectsInSet() {
        System.out.println("\n===== 5. CUSTOM OBJECTS =====");
        Set<BadPoint> bad = new HashSet<>();
        bad.add(new BadPoint(1, 2));
        bad.add(new BadPoint(1, 2));
        System.out.println("BadPoint set size  = " + bad.size() + " (duplicate stored!)");

        Set<GoodPoint> good = new HashSet<>();
        good.add(new GoodPoint(1, 2));
        good.add(new GoodPoint(1, 2));
        System.out.println("GoodPoint set size = " + good.size());
        System.out.println("contains (1,2)?    = " + good.contains(new GoodPoint(1, 2)));

        // Don't mutate fields used in hashCode while the object is inside a HashSet/HashMap;
        // it will be "lost" in the wrong bucket. That's why the fields above are final.
    }

    // =================================================================
    // 6. Typical DSA uses
    // =================================================================
    static void dsaUsage() {
        System.out.println("\n===== 6. DSA USAGE =====");
        int[] nums = {3, 1, 4, 1, 5};

        // Contains duplicate? O(n)
        Set<Integer> seen = new HashSet<>();
        boolean hasDup = false;
        for (int n : nums) {
            if (!seen.add(n)) { hasDup = true; break; }  // add returns false if already present
        }
        System.out.println("has duplicate = " + hasDup);

        // Visited set for grid/graph traversal: encode (r,c) as a string or as r*cols+c
        Set<String> visited = new HashSet<>();
        visited.add(0 + "," + 1);
        System.out.println("visited (0,1)? " + visited.contains("0,1"));
    }
}
