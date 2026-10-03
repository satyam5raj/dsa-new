/*
 * ============================================================================
 *  TOPIC 4 : GENERICS - type parameters, bounded types, wildcards, erasure
 * ============================================================================
 *
 *  Run:  java GenericsDemo.java
 *
 *  WHY GENERICS?
 *  -------------
 *  Before Java 5, collections held plain Object:
 *      List list = new ArrayList();
 *      list.add("hi");
 *      list.add(42);                     // nothing stops this
 *      String s = (String) list.get(1);  // ClassCastException at RUNTIME
 *
 *  With generics the compiler checks types for you:
 *      List<String> list = new ArrayList<>();
 *      list.add(42);                     // COMPILE ERROR - caught early
 *      String s = list.get(0);           // no cast needed
 *
 *  Benefits: (1) type safety at compile time, (2) no casts, (3) reusable code.
 *
 *  NAMING CONVENTION for type parameters:
 *      T = Type, E = Element, K = Key, V = Value, R = Return, N = Number
 *
 *  KEY CONCEPTS IN THIS FILE
 *    1. Generic class            class Box<T>
 *    2. Generic method           <T> void print(T x)
 *    3. Bounded type             <T extends Comparable<T>>
 *    4. Wildcards                ?, ? extends T, ? super T     (PECS)
 *    5. Invariance               List<Integer> is NOT a List<Number>
 *    6. Type erasure + its limitations
 */

import java.util.*;

public class GenericsDemo {

    public static void main(String[] args) {
        genericClass();
        genericMethods();
        boundedTypes();
        invariance();
        wildcards();
        typeErasure();
        genericStackDemo();
    }

    // =================================================================
    // 1. Generic class: T is a placeholder filled in when you use the class
    // =================================================================
    static class Box<T> {
        private T value;

        Box(T value) { this.value = value; }
        T get() { return value; }
        void set(T value) { this.value = value; }
    }

    // Multiple type parameters
    static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) { this.key = key; this.value = value; }
        K getKey() { return key; }
        V getValue() { return value; }

        @Override
        public String toString() { return "(" + key + ", " + value + ")"; }
    }

    static void genericClass() {
        System.out.println("===== 1. GENERIC CLASS =====");
        Box<String> stringBox = new Box<>("hello");
        Box<Integer> intBox = new Box<>(42);
        String s = stringBox.get();       // no cast
        int n = intBox.get();             // auto-unboxed
        // stringBox.set(5);              // COMPILE ERROR: 5 is not a String
        System.out.println("stringBox=" + s + " intBox=" + n);

        Pair<String, Integer> p = new Pair<>("age", 30);
        System.out.println("pair = " + p + ", key = " + p.getKey());

        // Generics only work with REFERENCE types: Box<int> is illegal -> use Box<Integer>
    }

    // =================================================================
    // 2. Generic methods: the <T> before the return type declares it
    // =================================================================
    static <T> void printArray(T[] arr) {
        for (T item : arr) System.out.print(item + " ");
        System.out.println();
    }

    static <T> T firstOrDefault(List<T> list, T defaultValue) {
        return list.isEmpty() ? defaultValue : list.get(0);
    }

    static <K, V> Map<V, K> invert(Map<K, V> map) {
        Map<V, K> result = new HashMap<>();
        for (Map.Entry<K, V> e : map.entrySet()) result.put(e.getValue(), e.getKey());
        return result;
    }

    static void genericMethods() {
        System.out.println("\n===== 2. GENERIC METHODS =====");
        printArray(new Integer[]{1, 2, 3});           // T inferred as Integer
        printArray(new String[]{"a", "b"});           // T inferred as String
        System.out.println("firstOrDefault(empty) = " + firstOrDefault(new ArrayList<String>(), "none"));
        System.out.println("invert = " + invert(Map.of("one", 1, "two", 2)));
        // Explicit type argument (rarely needed):
        System.out.println(GenericsDemo.<Integer>firstOrDefault(List.of(9), 0));
    }

    // =================================================================
    // 3. Bounded types: restrict what T can be
    //    <T extends Number>              T must be Number or a subclass
    //    <T extends Comparable<T>>       T must be comparable to itself
    //    <T extends Number & Comparable<T>>  multiple bounds (class first, then interfaces)
    //    Note: 'extends' is used for BOTH classes and interfaces here.
    // =================================================================
    static <T extends Number> double sum(List<T> nums) {
        double total = 0;
        for (T n : nums) total += n.doubleValue();   // allowed because T is a Number
        return total;
    }

    static <T extends Comparable<T>> T max(List<T> list) {
        T best = list.get(0);
        for (T item : list) {
            if (item.compareTo(best) > 0) best = item; // compareTo exists thanks to the bound
        }
        return best;
    }

    static void boundedTypes() {
        System.out.println("\n===== 3. BOUNDED TYPES =====");
        System.out.println("sum ints    = " + sum(List.of(1, 2, 3)));
        System.out.println("sum doubles = " + sum(List.of(1.5, 2.5)));
        // sum(List.of("a"));   // COMPILE ERROR: String is not a Number
        System.out.println("max strings = " + max(List.of("pear", "apple", "zebra")));
        System.out.println("max ints    = " + max(List.of(4, 9, 2)));
    }

    // =================================================================
    // 4. INVARIANCE - the surprising rule
    //    Integer IS-A Number, but List<Integer> is NOT a List<Number>.
    //    Why? If it were allowed:
    //        List<Integer> ints = new ArrayList<>();
    //        List<Number> nums = ints;    // pretend this compiles
    //        nums.add(3.14);              // a Double inside a list of Integers!
    //    So the compiler forbids it. (Arrays ARE covariant, and fail at runtime instead.)
    // =================================================================
    static void invariance() {
        System.out.println("\n===== 4. INVARIANCE =====");
        // List<Number> nums = new ArrayList<Integer>();  // COMPILE ERROR

        Object[] objects = new String[1];      // arrays are covariant: compiles
        try {
            objects[0] = 42;                   // ...but fails at runtime
        } catch (ArrayStoreException e) {
            System.out.println("arrays are covariant -> ArrayStoreException at runtime");
        }
        System.out.println("generics are invariant -> error caught at COMPILE time (safer)");
    }

    // =================================================================
    // 5. WILDCARDS - flexibility when invariance is too strict
    //
    //    ?              unknown type. Read as Object. Can't add (except null).
    //    ? extends T    "T or any SUBtype"   -> you can READ T out   (PRODUCER)
    //    ? super T      "T or any SUPERtype" -> you can WRITE T in   (CONSUMER)
    //
    //    PECS:  Producer Extends, Consumer Super.
    //    Example from the JDK:  Collections.copy(List<? super T> dest, List<? extends T> src)
    // =================================================================

    // Accepts List<Integer>, List<Double>, List<Number>...  READ-ONLY use.
    static double sumAll(List<? extends Number> nums) {
        double total = 0;
        for (Number n : nums) total += n.doubleValue();   // reading as Number is safe
        // nums.add(5);   // COMPILE ERROR: the list might be List<Double>
        return total;
    }

    // Accepts List<Integer>, List<Number>, List<Object>.  WRITE use.
    static void addNumbers(List<? super Integer> list) {
        for (int i = 1; i <= 3; i++) list.add(i);         // adding Integer is always safe
        Object o = list.get(0);                           // reading only gives Object
        System.out.println("   read back as Object: " + o);
    }

    // Unbounded: works for any list when you don't care about the element type
    static int countNonNull(List<?> list) {
        int count = 0;
        for (Object o : list) if (o != null) count++;
        return count;
    }

    // PECS in action: copy from a producer into a consumer
    static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) dest.add(item);
    }

    static void wildcards() {
        System.out.println("\n===== 5. WILDCARDS & PECS =====");
        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.5, 2.5);
        System.out.println("sumAll(ints)    = " + sumAll(ints));
        System.out.println("sumAll(doubles) = " + sumAll(doubles));

        List<Number> numbers = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        addNumbers(numbers);
        addNumbers(objects);
        System.out.println("numbers = " + numbers + ", objects = " + objects);

        System.out.println("countNonNull = " + countNonNull(Arrays.asList("a", null, "b")));

        List<Number> dest = new ArrayList<>();
        copy(ints, dest);       // T = Integer: src is List<Integer>, dest is List<? super Integer>
        System.out.println("copy ints into List<Number> = " + dest);
    }

    // =================================================================
    // 6. TYPE ERASURE
    //    Generics exist only at COMPILE time. The compiler:
    //      - checks types
    //      - replaces T with its bound (Object if unbounded)
    //      - inserts casts where needed
    //    At RUNTIME, List<String> and List<Integer> are both just "List".
    //    (Done for backward compatibility with pre-Java-5 code.)
    //
    //    CONSEQUENCES (things you CANNOT do):
    //      new T()                       -> type unknown at runtime
    //      new T[10]                     -> same reason (use Object[] + cast)
    //      obj instanceof List<String>   -> only  instanceof List<?>  works
    //      static T field                -> static is shared by all T's
    //      overload foo(List<String>) and foo(List<Integer>) -> same erasure, clash
    //      primitives as T               -> List<int> not allowed
    // =================================================================
    static void typeErasure() {
        System.out.println("\n===== 6. TYPE ERASURE =====");
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        System.out.println("same runtime class? " + (strings.getClass() == integers.getClass()));
        System.out.println("runtime class      = " + strings.getClass().getName());

        // Workaround for "new T()": pass a Class<T> or a factory (Supplier<T>)
        StringBuilder sb = create(StringBuilder::new);
        System.out.println("created via Supplier: " + sb.append("works"));

        // Raw types (no <...>) bypass checks. Don't use them; they exist for old code.
        @SuppressWarnings({"rawtypes", "unchecked"})
        List raw = strings;
        raw.add(123);                                    // heap pollution: compiles with a warning
        try {
            String s = strings.get(0);                   // compiler-inserted cast fails here
            System.out.println(s);
        } catch (ClassCastException e) {
            System.out.println("raw type misuse -> ClassCastException");
        }
    }

    static <T> T create(java.util.function.Supplier<T> factory) {
        return factory.get();
    }

    // =================================================================
    // 7. Putting it together: a generic, array-backed stack
    // =================================================================
    static class GenericStack<E> implements Iterable<E> {
        private E[] data;
        private int size;

        @SuppressWarnings("unchecked")
        GenericStack() {
            data = (E[]) new Object[4];   // can't do new E[4] -> erasure workaround
        }

        void push(E item) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);  // grow
            data[size++] = item;
        }

        E pop() {
            if (size == 0) throw new RuntimeException("stack empty");
            E item = data[--size];
            data[size] = null;            // avoid memory leak: drop the reference
            return item;
        }

        boolean isEmpty() { return size == 0; }

        // Implementing Iterable lets us use for-each on our own class
        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                int i = size - 1;         // iterate from top to bottom
                public boolean hasNext() { return i >= 0; }
                public E next() { return data[i--]; }
            };
        }
    }

    static void genericStackDemo() {
        System.out.println("\n===== 7. GENERIC STACK =====");
        GenericStack<String> st = new GenericStack<>();
        for (String s : List.of("a", "b", "c", "d", "e")) st.push(s);   // triggers a resize
        System.out.print("for-each over stack: ");
        for (String s : st) System.out.print(s + " ");
        System.out.println("\npop = " + st.pop());
    }
}
