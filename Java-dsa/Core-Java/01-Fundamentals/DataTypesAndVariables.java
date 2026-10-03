/*
 * ============================================================================
 *  TOPIC 1.1 : DATA TYPES & VARIABLES
 * ============================================================================
 *
 *  Run:  java DataTypesAndVariables.java
 *
 *  WHAT IS A VARIABLE?
 *  -------------------
 *  A variable is a named box in memory that holds a value.
 *  In Java every variable has a TYPE, and the type is fixed at compile time
 *  (Java is "statically typed"). The type decides:
 *     1. How much memory the box takes
 *     2. What values it can hold
 *     3. What operations you can do with it
 *
 *  TWO FAMILIES OF TYPES
 *  ---------------------
 *  1) PRIMITIVE types  -> store the actual value directly (8 of them)
 *  2) REFERENCE types  -> store an ADDRESS (reference) pointing to an object
 *                         on the heap (String, arrays, Integer, your classes...)
 *
 *  THE 8 PRIMITIVES
 *  ----------------------------------------------------------------------
 *   type     size     range / notes                         default value
 *  ----------------------------------------------------------------------
 *   byte     1 byte   -128 to 127                            0
 *   short    2 bytes  -32,768 to 32,767                      0
 *   int      4 bytes  about -2.1 billion to 2.1 billion      0
 *   long     8 bytes  about -9.2 * 10^18 to 9.2 * 10^18      0L
 *   float    4 bytes  ~7 decimal digits of precision         0.0f
 *   double   8 bytes  ~15 decimal digits of precision        0.0d
 *   char     2 bytes  a single UTF-16 character (0..65535)   '\u0000'
 *   boolean  JVM-dep. true / false                           false
 *  ----------------------------------------------------------------------
 *  NOTE: default values apply only to FIELDS (class-level variables).
 *        LOCAL variables (inside methods) have NO default - the compiler
 *        forces you to assign them before use.
 */

public class DataTypesAndVariables {

    // ---------------------------------------------------------------
    // FIELDS (class-level variables). These DO get default values.
    // 'static' means it belongs to the class, not to an object.
    // ---------------------------------------------------------------
    static int defaultInt;          // 0
    static boolean defaultBoolean;  // false
    static String defaultString;    // null  (reference types default to null)

    // 'final' = constant. Once assigned, cannot change.
    // Convention: constants are UPPER_SNAKE_CASE.
    static final double PI = 3.14159;

    public static void main(String[] args) {

        primitives();
        overflowDemo();
        typeCasting();
        wrapperClassesAndAutoboxing();
        floatingPointGotcha();
        varKeyword();
        stackVsHeap();

        System.out.println("\nField defaults -> int: " + defaultInt
                + ", boolean: " + defaultBoolean
                + ", String: " + defaultString);
    }

    // =================================================================
    // 1. Declaring primitives
    // =================================================================
    static void primitives() {
        System.out.println("===== 1. PRIMITIVES =====");

        byte b = 100;                 // small whole numbers
        short s = 30_000;             // underscores are allowed for readability
        int i = 2_000_000_000;        // the default choice for whole numbers
        long l = 9_000_000_000L;      // 'L' suffix needed: literal is too big for int
        float f = 3.14f;              // 'f' suffix needed: decimal literals are double by default
        double d = 3.141592653589793; // the default choice for decimals
        char c = 'A';                 // single quotes for char, double quotes for String
        boolean flag = true;

        System.out.println("byte=" + b + " short=" + s + " int=" + i + " long=" + l);
        System.out.println("float=" + f + " double=" + d + " char=" + c + " boolean=" + flag);

        // char is actually a number (its Unicode code point) under the hood
        char next = (char) (c + 1);          // 'A' (65) + 1 = 66 = 'B'
        System.out.println("'A' as int = " + (int) c + ", next char = " + next);

        // Min / max values are available on wrapper classes
        System.out.println("Integer.MAX_VALUE = " + Integer.MAX_VALUE);
        System.out.println("Long.MAX_VALUE    = " + Long.MAX_VALUE);

        // Other ways to write int literals
        int hex = 0xFF;        // hexadecimal -> 255
        int binary = 0b1010;   // binary      -> 10
        int octal = 017;       // octal (leading 0!) -> 15  (common trick question)
        System.out.println("hex=" + hex + " binary=" + binary + " octal=" + octal);
    }

    // =================================================================
    // 2. Overflow - numbers silently "wrap around"
    // =================================================================
    static void overflowDemo() {
        System.out.println("\n===== 2. OVERFLOW =====");

        int max = Integer.MAX_VALUE;
        int overflowed = max + 1;   // no error! wraps to Integer.MIN_VALUE
        System.out.println("MAX + 1 = " + overflowed);

        // DSA tip: computing mid in binary search
        int low = 2_000_000_000, high = 2_100_000_000;
        int badMid = (low + high) / 2;          // low + high overflows -> negative!
        int goodMid = low + (high - low) / 2;   // safe version, ALWAYS use this
        System.out.println("bad mid = " + badMid + ", good mid = " + goodMid);

        // Want an exception instead of silent wrap? Use Math.addExact
        try {
            Math.addExact(max, 1);
        } catch (ArithmeticException e) {
            System.out.println("Math.addExact threw: " + e.getMessage());
        }
    }

    // =================================================================
    // 3. Type casting
    //    WIDENING (implicit, safe):   byte -> short -> int -> long -> float -> double
    //    NARROWING (explicit, risky): double -> int etc. Needs (type) cast
    // =================================================================
    static void typeCasting() {
        System.out.println("\n===== 3. TYPE CASTING =====");

        int i = 100;
        long l = i;        // widening: automatic, no data loss
        double d = l;      // widening: automatic
        System.out.println("widened: " + d);

        double pi = 3.99;
        int truncated = (int) pi;   // narrowing: decimal part is CUT, not rounded
        System.out.println("(int) 3.99 = " + truncated);
        System.out.println("Math.round(3.99) = " + Math.round(pi));

        int big = 300;
        byte small = (byte) big;    // 300 doesn't fit in a byte -> keeps lowest 8 bits -> 44
        System.out.println("(byte) 300 = " + small);

        // Integer division trap: int / int = int (decimal part dropped)
        System.out.println("5 / 2   = " + (5 / 2));       // 2
        System.out.println("5 / 2.0 = " + (5 / 2.0));     // 2.5 (one side is double)
        System.out.println("(double) 5 / 2 = " + ((double) 5 / 2)); // 2.5

        // Type promotion in expressions: byte + byte becomes int!
        byte x = 10, y = 20;
        // byte z = x + y;      // COMPILE ERROR: x + y is int
        byte z = (byte) (x + y);
        x += 5;                 // compound assignment does an implicit cast, so this compiles
        System.out.println("byte sum = " + z + ", x after += 5 = " + x);
    }

    // =================================================================
    // 4. Wrapper classes & Autoboxing
    //    Every primitive has an object version:
    //    int -> Integer, long -> Long, double -> Double, char -> Character,
    //    boolean -> Boolean, byte -> Byte, short -> Short, float -> Float
    //
    //    Why? Collections (List, Map...) can only hold OBJECTS, not primitives.
    //    Autoboxing   = primitive -> wrapper automatically
    //    Unboxing     = wrapper -> primitive automatically
    // =================================================================
    static void wrapperClassesAndAutoboxing() {
        System.out.println("\n===== 4. WRAPPERS & AUTOBOXING =====");

        Integer boxed = 42;          // autoboxing: compiler writes Integer.valueOf(42)
        int unboxed = boxed;         // unboxing:   compiler writes boxed.intValue()
        System.out.println("boxed=" + boxed + " unboxed=" + unboxed);

        // Useful static helpers
        int parsed = Integer.parseInt("123");
        String binary = Integer.toBinaryString(10);
        System.out.println("parseInt(\"123\") = " + parsed + ", toBinaryString(10) = " + binary);

        // ---- FAMOUS INTERVIEW TRAP: Integer cache (-128 to 127) ----
        // Integer.valueOf() caches objects for values -128..127.
        // '==' on objects compares REFERENCES (addresses), not values.
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;
        System.out.println("127 == 127 ? " + (a == b));        // true  (same cached object)
        System.out.println("128 == 128 ? " + (c == d));        // false (two different objects!)
        System.out.println("128 equals 128 ? " + c.equals(d)); // true  -> ALWAYS use equals()

        // ---- TRAP 2: unboxing null throws NullPointerException ----
        Integer nothing = null;
        try {
            int crash = nothing; // tries nothing.intValue()
            System.out.println(crash);
        } catch (NullPointerException e) {
            System.out.println("Unboxing null -> NullPointerException");
        }
    }

    // =================================================================
    // 5. Floating point is NOT exact (binary can't represent 0.1 exactly)
    // =================================================================
    static void floatingPointGotcha() {
        System.out.println("\n===== 5. FLOATING POINT =====");

        System.out.println("0.1 + 0.2 = " + (0.1 + 0.2));          // 0.30000000000000004
        System.out.println("0.1 + 0.2 == 0.3 ? " + (0.1 + 0.2 == 0.3)); // false

        // Compare doubles with a tolerance (epsilon)
        double eps = 1e-9;
        System.out.println("with epsilon ? " + (Math.abs((0.1 + 0.2) - 0.3) < eps));

        // For MONEY use BigDecimal (exact decimal arithmetic). Pass Strings, not doubles!
        java.math.BigDecimal x = new java.math.BigDecimal("0.1");
        java.math.BigDecimal y = new java.math.BigDecimal("0.2");
        System.out.println("BigDecimal 0.1 + 0.2 = " + x.add(y));

        // Special values
        System.out.println("1.0 / 0 = " + (1.0 / 0));   // Infinity (no exception for doubles)
        System.out.println("0.0 / 0 = " + (0.0 / 0));   // NaN
        // System.out.println(1 / 0);                   // int division by zero -> ArithmeticException
    }

    // =================================================================
    // 6. 'var' (Java 10+) - local variable type inference
    //    The compiler figures out the type from the right-hand side.
    //    It's STILL statically typed; you just don't write the type.
    // =================================================================
    static void varKeyword() {
        System.out.println("\n===== 6. var =====");

        var count = 10;                       // inferred as int
        var name = "Satyam";                  // inferred as String
        var list = new java.util.ArrayList<String>(); // inferred as ArrayList<String>
        list.add(name);
        // count = "hello";  // COMPILE ERROR: count is an int forever

        System.out.println("count=" + count + " list=" + list);
        // Rules: only for local variables, must be initialized, cannot be null alone.
    }

    // =================================================================
    // 7. Stack vs Heap (mental model - very important!)
    //
    //   STACK: each method call gets a "frame" holding its local variables.
    //          Primitives live here directly. Freed automatically on return.
    //   HEAP : all OBJECTS live here. Local variables only hold a reference.
    //          Cleaned up by the Garbage Collector when nothing references them.
    //
    //   Java is ALWAYS "pass by value". For objects, the VALUE passed is the
    //   reference (a copy of the address). So a method can mutate the object,
    //   but cannot make the caller's variable point to a new object.
    // =================================================================
    static void stackVsHeap() {
        System.out.println("\n===== 7. STACK vs HEAP / PASS BY VALUE =====");

        int num = 5;
        tryToChangePrimitive(num);
        System.out.println("primitive after method: " + num);   // still 5

        int[] arr = {5};
        mutateArray(arr);
        System.out.println("array after mutate: " + arr[0]);    // 99 (same object changed)

        reassignArray(arr);
        System.out.println("array after reassign: " + arr[0]);  // still 99
    }

    static void tryToChangePrimitive(int n) {
        n = 100; // changes only the local copy
    }

    static void mutateArray(int[] a) {
        a[0] = 99; // follows the reference to the SAME heap object and changes it
    }

    static void reassignArray(int[] a) {
        a = new int[]{-1}; // local copy of the reference now points elsewhere; caller unaffected
    }
}
