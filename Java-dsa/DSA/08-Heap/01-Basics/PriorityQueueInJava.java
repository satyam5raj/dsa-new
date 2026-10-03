/*
 * ============================================================================
 *  java.util.PriorityQueue: the API, comparators, and the TRAPS
 * ============================================================================
 *
 *  BASICS
 *    PriorityQueue<Integer> min = new PriorityQueue<>();                        // min-heap
 *    PriorityQueue<Integer> max = new PriorityQueue<>(Comparator.reverseOrder()); // max-heap
 *    offer/add O(log n)   poll O(log n)   peek O(1)   size O(1)
 *    remove(Object) O(n)  contains O(n)   (a linear scan: avoid in loops)
 *    new PriorityQueue<>(collection) heapifies in O(n)
 *
 *  COMPARATORS (you'll write dozens)
 *    by a field         : Comparator.comparingInt(p -> p.cost)
 *    reversed           : Comparator.comparingInt((int[] p) -> p[0]).reversed()
 *    tie-breaks         : .thenComparing(...)
 *    int[] pairs        : (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1])
 *
 *  ---------------------------------------------------------------------------
 *  TRAP 1: (a, b) -> a - b  OVERFLOWS.  Integer.MIN_VALUE - 1 wraps to MAX_VALUE,
 *          so a huge negative number sorts AFTER a positive one. Always use
 *          Integer.compare(a, b) (or Comparator.comparingInt).
 *  TRAP 2: ITERATING a PriorityQueue (for-each, toString, stream) is NOT in sorted
 *          order - it's the internal array order. Only poll() gives sorted order.
 *  TRAP 3: MUTATING an element that's already inside the heap breaks it silently:
 *          the heap doesn't know it must move. Remove it, change it, re-insert.
 *          (or insert a new copy and ignore stale ones = LAZY DELETION)
 *  TRAP 4: Ties are NOT FIFO. Equal-priority elements come out in an unspecified
 *          order. If order matters, add a sequence number as a tie-breaker.
 *  TRAP 5: poll()/peek() on an EMPTY queue return null -> an int unboxing NPE.
 *          remove()/element() throw NoSuchElementException instead.
 *
 *  Run:  java PriorityQueueInJava.java
 * ============================================================================
 */

import java.util.*;

public class PriorityQueueInJava {

    record Task(String name, int priority, long seq) {}

    static class Box { int value; Box(int v) { value = v; } public String toString() { return "" + value; } }

    static List<Integer> drain(PriorityQueue<Integer> pq) {
        List<Integer> out = new ArrayList<>();
        while (!pq.isEmpty()) out.add(pq.poll());
        return out;
    }

    public static void main(String[] args) {
        System.out.println("1. Min-heap and max-heap");
        PriorityQueue<Integer> min = new PriorityQueue<>(List.of(5, 1, 8, 3));
        PriorityQueue<Integer> max = new PriorityQueue<>(Comparator.reverseOrder());
        max.addAll(List.of(5, 1, 8, 3));
        check("min peek", 1, min.peek());
        check("max peek", 8, max.peek());

        System.out.println("2. Comparators on objects and arrays");
        PriorityQueue<int[]> byDistance = new PriorityQueue<>(Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]));
        byDistance.addAll(List.of(new int[]{3, 3}, new int[]{1, -1}, new int[]{-2, 2}));
        check("closest point to origin", "[1, -1]", byDistance.poll());
        PriorityQueue<String> byLenThenAlpha = new PriorityQueue<>(Comparator.comparingInt(String::length).thenComparing(Comparator.reverseOrder()));
        byLenThenAlpha.addAll(List.of("bb", "a", "c", "aa"));
        List<String> order = new ArrayList<>();
        while (!byLenThenAlpha.isEmpty()) order.add(byLenThenAlpha.poll());
        check("length asc, then z..a", "[c, a, bb, aa]", order);

        System.out.println("TRAP 1: a - b overflows");
        PriorityQueue<Integer> bad = new PriorityQueue<>((a, b) -> a - b);
        PriorityQueue<Integer> good = new PriorityQueue<>(Integer::compare);
        for (int x : new int[]{Integer.MIN_VALUE, 1, Integer.MAX_VALUE, 0}) { bad.add(x); good.add(x); }
        List<Integer> badOrder = drain(bad);
        System.out.println("   (a, b) -> a - b  gives " + badOrder);
        check("Integer::compare gives", "[-2147483648, 0, 1, 2147483647]", drain(good));
        check("a - b got it wrong", true, !badOrder.equals(List.of(Integer.MIN_VALUE, 0, 1, Integer.MAX_VALUE)));

        System.out.println("TRAP 2: iteration order is NOT sorted");
        PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(9, 7, 5, 3, 1, 8));
        System.out.println("   toString(): " + pq + "   <- the internal array");
        check("poll order is sorted", "[1, 3, 5, 7, 8, 9]", drain(pq));

        System.out.println("TRAP 3: mutating an element inside the heap");
        PriorityQueue<Box> boxes = new PriorityQueue<>(Comparator.comparingInt(b -> b.value));
        Box a = new Box(1), b = new Box(5), c = new Box(10);
        boxes.addAll(List.of(a, b, c));
        a.value = 100;                       // the heap is NOT told
        check("peek is now wrong (100, not 5)", 100, boxes.peek().value);
        boxes.remove(a);                     // the fix: remove, change, re-add
        boxes.add(a);
        check("after remove + re-add", 5, boxes.peek().value);

        System.out.println("TRAP 4: ties are not FIFO -> add a sequence number");
        PriorityQueue<Task> tasks = new PriorityQueue<>(Comparator.comparingInt(Task::priority).thenComparingLong(Task::seq));
        long seq = 0;
        for (String name : List.of("t1", "t2", "t3", "t4", "t5", "t6")) tasks.add(new Task(name, 1, seq++));
        List<String> names = new ArrayList<>();
        while (!tasks.isEmpty()) names.add(tasks.poll().name());
        check("equal priorities, insertion order", "[t1, t2, t3, t4, t5, t6]", names);

        System.out.println("TRAP 5: poll() on empty returns null");
        PriorityQueue<Integer> empty = new PriorityQueue<>();
        check("poll() on empty", null, empty.poll());
        String what;
        try { int x = empty.poll(); what = "no error " + x; } catch (NullPointerException e) { what = "NullPointerException"; }
        check("int x = empty.poll()", "NullPointerException", what);
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
