/*
 * ============================================================================
 *  TOPIC 1.2 : OPERATORS
 * ============================================================================
 *
 *  Run:  java Operators.java
 *
 *  Categories:
 *    1. Arithmetic      +  -  *  /  %
 *    2. Unary           ++  --  +  -  !
 *    3. Assignment      =  +=  -=  *=  /=  %=  &=  |=  ^=  <<=  >>=
 *    4. Relational      ==  !=  >  <  >=  <=
 *    5. Logical         &&  ||  !        (short-circuit)
 *    6. Bitwise         &  |  ^  ~  <<  >>  >>>
 *    7. Ternary         condition ? a : b
 *    8. instanceof      obj instanceof Type
 *
 *  PRECEDENCE (high -> low), the ones that matter most:
 *    postfix (x++)  >  unary (++x, !, ~)  >  * / %  >  + -  >  << >> >>>
 *    >  < > <= >= instanceof  >  == !=  >  &  >  ^  >  |  >  &&  >  ||
 *    >  ?:  >  assignment (=, += ...)
 *  When in doubt, use parentheses. Readable code > clever code.
 */

public class Operators {

    public static void main(String[] args) {
        arithmetic();
        incrementDecrement();
        relationalAndLogical();
        bitwise();
        ternary();
        instanceOfOperator();
    }

    // =================================================================
    // 1. Arithmetic
    // =================================================================
    static void arithmetic() {
        System.out.println("===== 1. ARITHMETIC =====");
        int a = 17, b = 5;

        System.out.println("a + b = " + (a + b));  // 22
        System.out.println("a - b = " + (a - b));  // 12
        System.out.println("a * b = " + (a * b));  // 85
        System.out.println("a / b = " + (a / b));  // 3   (integer division)
        System.out.println("a % b = " + (a % b));  // 2   (remainder)

        // Modulo with negatives: sign follows the LEFT operand
        System.out.println("-17 % 5 = " + (-17 % 5));            // -2
        // Math.floorMod always gives a non-negative result for positive divisor
        System.out.println("floorMod(-17, 5) = " + Math.floorMod(-17, 5)); // 3

        // DSA uses of %:
        //   n % 10   -> last digit
        //   n / 10   -> remove last digit
        //   n % 2==0 -> even check
        //   (i + 1) % size -> circular index (circular queue, rotate array)
        int n = 1234;
        System.out.println("last digit of 1234 = " + (n % 10) + ", without it = " + (n / 10));

        // String concatenation with '+' is evaluated LEFT to RIGHT
        System.out.println(1 + 2 + "3");   // "33"  (1+2=3 first, then "3"+"3")
        System.out.println("1" + 2 + 3);   // "123" (string first, everything after is concat)
    }

    // =================================================================
    // 2. ++ and --  (pre vs post)
    //    ++x : increment FIRST, then use the value
    //    x++ : use the value FIRST, then increment
    // =================================================================
    static void incrementDecrement() {
        System.out.println("\n===== 2. INCREMENT / DECREMENT =====");
        int x = 5;
        int y = x++;   // y = 5, then x becomes 6
        System.out.println("after y = x++ : x=" + x + " y=" + y);

        x = 5;
        y = ++x;       // x becomes 6, then y = 6
        System.out.println("after y = ++x : x=" + x + " y=" + y);

        // Classic trick question
        int i = 5;
        i = i++;       // RHS evaluates to 5, i becomes 6, THEN assignment sets i back to 5
        System.out.println("i = i++ gives i=" + i);  // 5
    }

    // =================================================================
    // 3. Relational & Logical
    //    && and || are SHORT-CIRCUIT: the right side is skipped if the
    //    result is already known. Very useful for null checks.
    // =================================================================
    static void relationalAndLogical() {
        System.out.println("\n===== 3. RELATIONAL & LOGICAL =====");
        int a = 10, b = 20;
        System.out.println("a == b: " + (a == b) + ", a != b: " + (a != b) + ", a < b: " + (a < b));

        String s = null;
        // If s is null, s.length() is NEVER called -> no NullPointerException
        if (s != null && s.length() > 0) {
            System.out.println("non-empty");
        } else {
            System.out.println("short-circuit saved us from NPE");
        }

        // Non short-circuit versions: & and | ALWAYS evaluate both sides
        int counter = 0;
        boolean r1 = false && (++counter > 0);  // right side skipped
        boolean r2 = false & (++counter > 0);   // right side evaluated
        System.out.println("counter after && and & : " + counter + " (r1=" + r1 + ", r2=" + r2 + ")");
    }

    // =================================================================
    // 4. Bitwise operators  (used a LOT in DSA "bit manipulation")
    //    &  AND : 1 only if both bits are 1
    //    |  OR  : 1 if any bit is 1
    //    ^  XOR : 1 if bits are DIFFERENT     (x ^ x = 0, x ^ 0 = x)
    //    ~  NOT : flips all bits              (~x = -x - 1)
    //    << left shift   : x << k  == x * 2^k
    //    >> right shift  : x >> k  == x / 2^k (keeps sign bit)
    //    >>> unsigned right shift : fills with 0 (sign bit not kept)
    // =================================================================
    static void bitwise() {
        System.out.println("\n===== 4. BITWISE =====");
        int a = 12;  // 1100
        int b = 10;  // 1010

        System.out.println("a & b = " + (a & b) + "  (1000)");
        System.out.println("a | b = " + (a | b) + " (1110)");
        System.out.println("a ^ b = " + (a ^ b) + "  (0110)");
        System.out.println("~a    = " + (~a));
        System.out.println("a << 2 = " + (a << 2) + " (12 * 4)");
        System.out.println("a >> 2 = " + (a >> 2) + "  (12 / 4)");
        System.out.println("-16 >> 2  = " + (-16 >> 2));    // -4 (sign kept)
        System.out.println("-16 >>> 28 = " + (-16 >>> 28)); // 15 (zeros shifted in)

        // ---- Handy bit tricks ----
        int n = 13; // 1101
        System.out.println("is odd?           " + ((n & 1) == 1));
        System.out.println("bit 2 set?        " + (((n >> 2) & 1) == 1));
        System.out.println("set bit 1:        " + (n | (1 << 1)));    // 1111 = 15
        System.out.println("clear bit 0:      " + (n & ~1));          // 1100 = 12
        System.out.println("toggle bit 3:     " + (n ^ (1 << 3)));    // 0101 = 5
        System.out.println("is power of 2(16)? " + isPowerOfTwo(16));
        System.out.println("count set bits:   " + Integer.bitCount(n));

        // XOR swap (no temp variable)
        int x = 3, y = 7;
        x = x ^ y;
        y = x ^ y;   // (x^y)^y = x
        x = x ^ y;   // (x^y)^x = y
        System.out.println("after XOR swap: x=" + x + " y=" + y);

        // Find the single non-duplicate number (LeetCode 136): pairs cancel out with XOR
        int[] nums = {4, 1, 2, 1, 2};
        int single = 0;
        for (int num : nums) single ^= num;
        System.out.println("single number = " + single);
    }

    // A power of 2 has exactly one bit set: 1000. n-1 = 0111. AND = 0.
    static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    // =================================================================
    // 5. Ternary:  condition ? valueIfTrue : valueIfFalse
    // =================================================================
    static void ternary() {
        System.out.println("\n===== 5. TERNARY =====");
        int age = 20;
        String status = (age >= 18) ? "adult" : "minor";
        System.out.println("status = " + status);

        int a = 5, b = 9, c = 7;
        int max = (a > b) ? (a > c ? a : c) : (b > c ? b : c); // nested: works, but hard to read
        System.out.println("max = " + max + " (prefer Math.max(a, Math.max(b, c)))");
    }

    // =================================================================
    // 6. instanceof - checks the runtime type of an object
    // =================================================================
    static void instanceOfOperator() {
        System.out.println("\n===== 6. instanceof =====");
        Object obj = "hello";
        System.out.println("obj instanceof String ? " + (obj instanceof String));
        System.out.println("obj instanceof Integer ? " + (obj instanceof Integer));

        // Java 16+ pattern matching: test AND cast in one step
        if (obj instanceof String str) {
            System.out.println("length via pattern variable = " + str.length());
        }
        // null instanceof Anything is always false (never throws)
        Object nothing = null;
        System.out.println("null instanceof Object ? " + (nothing instanceof Object));
    }
}
