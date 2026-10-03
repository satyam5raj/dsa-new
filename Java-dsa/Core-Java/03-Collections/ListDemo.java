/*
 * ============================================================================
 *  TOPIC 3.1 : COLLECTIONS FRAMEWORK OVERVIEW + LIST (ArrayList vs LinkedList)
 * ============================================================================
 *
 *  Run:  java ListDemo.java
 *
 *  THE HIERARCHY (interfaces in CAPS-ish, implementations below)
 *  -------------------------------------------------------------
 *
 *                         Iterable
 *                            |
 *                        Collection
 *            ________________|________________
 *           |                |                |
 *          List             Set             Queue
 *       ArrayList         HashSet         PriorityQueue
 *       LinkedList     LinkedHashSet         Deque
 *       Vector(legacy)   TreeSet         ArrayDeque, LinkedList
 *
 *          Map  (NOT a Collection! separate hierarchy)
 *       HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap, Hashtable(legacy)
 *
 *  LIST = ordered collection, allows duplicates, index-based access.
 *
 *  ARRAYLIST internals
 *  -------------------
 *   - Backed by an Object[] array.
 *   - Default capacity 10 (allocated lazily on first add).
 *   - When full, grows by 1.5x: newCapacity = old + (old >> 1),
 *     then copies everything with Arrays.copyOf -> O(n) for that one add,
 *     but AMORTIZED O(1) per add overall.
 *
 *  LINKEDLIST internals
 *  --------------------
 *   - Doubly linked list of Node{item, prev, next}, with head & tail pointers.
 *   - Also implements Deque (can act as a queue/stack).
 *
 *  COMPLEXITY
 *  ------------------------------------------------------------
 *   Operation              ArrayList          LinkedList
 *  ------------------------------------------------------------
 *   get(i)                 O(1)               O(n)  (walk the nodes)
 *   add(e) at end          O(1) amortized     O(1)
 *   add(0, e) at front     O(n) (shift)       O(1)
 *   remove(i) middle       O(n) (shift)       O(n) to find + O(1) unlink
 *   contains / indexOf     O(n)               O(n)
 *   memory per element     small (just ref)   big (node + 2 pointers)
 *  ------------------------------------------------------------
 *  VERDICT: use ArrayList by default. Its contiguous memory is CPU-cache
 *  friendly, which makes it faster in practice even for many "LinkedList
 *  should win" cases. For queue/stack needs use ArrayDeque.
 */

import java.util.*;

public class ListDemo {

    public static void main(String[] args) {
        arrayListBasics();
        linkedListBasics();
        iterationAndRemoval();
        sortingLists();
        immutableLists();
        commonGotchas();
    }

    // =================================================================
    // 1. ArrayList basics
    // =================================================================
    static void arrayListBasics() {
        System.out.println("===== 1. ARRAYLIST =====");

        // Program to the INTERFACE (List) on the left, implementation on the right.
        // Lets you swap ArrayList -> LinkedList later without changing other code.
        List<String> fruits = new ArrayList<>();   // <> "diamond": type inferred

        fruits.add("apple");          // append -> [apple]
        fruits.add("banana");         // [apple, banana]
        fruits.add(0, "mango");       // insert at index 0 -> shifts others right
        fruits.add("apple");          // duplicates allowed
        System.out.println("list          = " + fruits);

        System.out.println("get(1)        = " + fruits.get(1));
        fruits.set(1, "kiwi");        // replace element at index 1
        System.out.println("after set(1)  = " + fruits);

        System.out.println("size          = " + fruits.size());
        System.out.println("contains kiwi = " + fruits.contains("kiwi"));    // uses equals()
        System.out.println("indexOf apple = " + fruits.indexOf("apple"));
        System.out.println("lastIndexOf   = " + fruits.lastIndexOf("apple"));

        fruits.remove("apple");       // removes FIRST occurrence by value
        System.out.println("remove(obj)   = " + fruits);
        fruits.remove(0);             // removes by INDEX
        System.out.println("remove(0)     = " + fruits);

        // Bulk operations
        List<String> more = List.of("grape", "pear");
        fruits.addAll(more);
        System.out.println("addAll        = " + fruits);
        fruits.removeIf(f -> f.startsWith("g"));    // Java 8: remove all matching a condition
        System.out.println("removeIf g*   = " + fruits);

        // Sub-list is a VIEW: changes reflect in the original
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> view = nums.subList(1, 4);    // [2, 3, 4]
        view.set(0, 99);
        System.out.println("subList view change -> original = " + nums);

        // Convert List <-> array
        Integer[] arr = nums.toArray(new Integer[0]);
        List<Integer> fromArr = new ArrayList<>(Arrays.asList(arr));
        System.out.println("toArray -> back to list = " + fromArr);

        // Pre-size if you know the size: avoids repeated grow-and-copy
        List<Integer> sized = new ArrayList<>(1_000);
        System.out.println("pre-sized list is empty: " + sized.isEmpty());

        // 2D dynamic list (common for adjacency lists in graphs)
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < 3; i++) graph.add(new ArrayList<>());
        graph.get(0).add(1);
        graph.get(0).add(2);
        graph.get(1).add(2);
        System.out.println("adjacency list = " + graph);
    }

    // =================================================================
    // 2. LinkedList basics (also a Deque)
    // =================================================================
    static void linkedListBasics() {
        System.out.println("\n===== 2. LINKEDLIST =====");
        LinkedList<Integer> ll = new LinkedList<>();
        ll.add(2);
        ll.addFirst(1);      // O(1) - just relink head
        ll.addLast(3);       // O(1) - just relink tail
        System.out.println("list        = " + ll);
        System.out.println("getFirst    = " + ll.getFirst() + ", getLast = " + ll.getLast());
        System.out.println("removeFirst = " + ll.removeFirst() + " -> " + ll);
        System.out.println("get(1)      = " + ll.get(1) + "  (O(n): walks from the nearer end)");
    }

    // =================================================================
    // 3. Iteration and safe removal
    //    Modifying a list structurally (add/remove) while looping with
    //    for-each throws ConcurrentModificationException ("fail-fast").
    //    How? The list keeps a 'modCount'. The iterator remembers the value
    //    when it was created and checks it on every next().
    // =================================================================
    static void iterationAndRemoval() {
        System.out.println("\n===== 3. ITERATION =====");
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));

        // (a) classic index loop
        for (int i = 0; i < nums.size(); i++) System.out.print(nums.get(i) + " ");
        System.out.println(" <- index loop");

        // (b) for-each (uses an Iterator behind the scenes)
        for (int n : nums) System.out.print(n + " ");
        System.out.println(" <- for-each");

        // (c) forEach with lambda (Java 8)
        nums.forEach(n -> System.out.print(n + " "));
        System.out.println(" <- forEach lambda");

        // WRONG: removing inside for-each
        try {
            for (Integer n : nums) {
                if (n % 2 == 0) nums.remove(n);
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("for-each + remove -> ConcurrentModificationException");
        }

        // RIGHT way 1: Iterator.remove()
        nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        Iterator<Integer> it = nums.iterator();
        while (it.hasNext()) {
            if (it.next() % 2 == 0) it.remove();   // iterator updates its own expected modCount
        }
        System.out.println("iterator.remove evens -> " + nums);

        // RIGHT way 2: removeIf (cleanest)
        nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        nums.removeIf(n -> n % 2 == 0);
        System.out.println("removeIf evens        -> " + nums);

        // ListIterator: can go BACKWARDS and set/add during iteration
        List<String> letters = new ArrayList<>(List.of("a", "b", "c"));
        ListIterator<String> li = letters.listIterator(letters.size());
        System.out.print("backwards: ");
        while (li.hasPrevious()) System.out.print(li.previous() + " ");
        System.out.println();
    }

    // =================================================================
    // 4. Sorting lists - Comparable vs Comparator
    //    Comparable : the class defines its NATURAL order (compareTo).
    //                 e.g. String, Integer implement Comparable.
    //    Comparator : an EXTERNAL ordering you pass in. Many per class.
    // =================================================================
    record Student(String name, int marks) {}   // tiny data class (records are covered in topic 8)

    static void sortingLists() {
        System.out.println("\n===== 4. SORTING =====");
        List<Integer> nums = new ArrayList<>(List.of(5, 3, 8, 1));
        Collections.sort(nums);                              // natural ascending
        System.out.println("ascending   = " + nums);
        nums.sort(Collections.reverseOrder());               // descending
        System.out.println("descending  = " + nums);

        List<Student> students = new ArrayList<>(List.of(
                new Student("Ravi", 85),
                new Student("Asha", 92),
                new Student("Kiran", 85)));

        // Comparator built with helper methods (preferred, readable, no overflow bugs)
        students.sort(Comparator.comparingInt(Student::marks)       // by marks ascending
                .reversed()                                         // ...now descending
                .thenComparing(Student::name));                     // tie-break by name
        System.out.println("by marks desc, then name = " + students);

        // Old-style lambda comparator. AVOID (a, b) -> a - b for ints: it can
        // overflow for large values. Use Integer.compare(a, b).
        students.sort((s1, s2) -> Integer.compare(s1.marks(), s2.marks()));
        System.out.println("by marks asc = " + students);

        // Sort is STABLE (equal elements keep their relative order) - TimSort, O(n log n)
    }

    // =================================================================
    // 5. Immutable / unmodifiable lists
    // =================================================================
    static void immutableLists() {
        System.out.println("\n===== 5. IMMUTABLE LISTS =====");

        List<Integer> fixed = List.of(1, 2, 3);             // Java 9: truly immutable, no nulls
        try {
            fixed.add(4);
        } catch (UnsupportedOperationException e) {
            System.out.println("List.of(...).add -> UnsupportedOperationException");
        }

        // Arrays.asList: FIXED SIZE but you CAN set(), and it's backed by the array
        String[] arr = {"x", "y"};
        List<String> asList = Arrays.asList(arr);
        asList.set(0, "CHANGED");
        System.out.println("Arrays.asList set() changes array too: " + arr[0]);
        try {
            asList.add("z");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList(...).add -> UnsupportedOperationException");
        }

        // Unmodifiable VIEW: can't change through it, but sees changes in the original
        List<Integer> original = new ArrayList<>(List.of(1, 2));
        List<Integer> readOnly = Collections.unmodifiableList(original);
        original.add(3);
        System.out.println("unmodifiable view sees original change: " + readOnly);

        // List.copyOf: an immutable SNAPSHOT
        List<Integer> snapshot = List.copyOf(original);
        original.add(4);
        System.out.println("copyOf snapshot unaffected: " + snapshot);
    }

    // =================================================================
    // 6. Gotchas
    // =================================================================
    static void commonGotchas() {
        System.out.println("\n===== 6. GOTCHAS =====");
        // remove(int index) vs remove(Object o) on List<Integer>
        List<Integer> nums = new ArrayList<>(List.of(10, 20, 30));
        nums.remove(1);                       // removes INDEX 1 -> [10, 30]
        System.out.println("remove(1)                 -> " + nums);
        nums.remove(Integer.valueOf(10));     // removes VALUE 10 -> [30]
        System.out.println("remove(Integer.valueOf(10)) -> " + nums);

        // Vector & Stack are legacy (every method synchronized). Prefer
        // ArrayList, and ArrayDeque for stacks.
    }
}
