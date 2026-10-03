/*
 * ============================================================================
 *  TOPIC 1.4 : METHODS & ARRAYS
 * ============================================================================
 *
 *  Run:  java MethodsAndArrays.java
 *
 *  METHOD = a named, reusable block of code.
 *
 *      accessModifier [static] returnType name(paramType param, ...) {
 *          // body
 *          return value;   // not needed if returnType is void
 *      }
 *
 *  METHOD SIGNATURE = name + parameter types (NOT the return type).
 *  OVERLOADING      = same name, different parameter list (compile-time decision).
 *
 *  ARRAY = fixed-size, contiguous block of elements of ONE type.
 *    - Index starts at 0, last index is length - 1
 *    - Access by index is O(1)
 *    - Size cannot change after creation (use ArrayList for dynamic size)
 *    - Arrays are OBJECTS (live on the heap), even int[]
 */

import java.util.Arrays;

public class MethodsAndArrays {

    public static void main(String[] args) {
        methodBasics();
        overloading();
        varargs();
        recursion();
        arrayBasics();
        arraysUtilityClass();
        twoDArrays();
    }

    // =================================================================
    // 1. Method basics
    // =================================================================
    static void methodBasics() {
        System.out.println("===== 1. METHODS =====");
        int result = add(3, 4);        // 3 and 4 are ARGUMENTS; a and b are PARAMETERS
        System.out.println("add(3, 4) = " + result);
        greet("Satyam");               // void method: does something, returns nothing
    }

    static int add(int a, int b) {
        return a + b;
    }

    static void greet(String name) {
        System.out.println("Hello, " + name);
    }

    // =================================================================
    // 2. Overloading - same name, different parameters
    //    The compiler picks the best match at COMPILE time.
    //    Changing only the return type is NOT overloading (compile error).
    // =================================================================
    static void overloading() {
        System.out.println("\n===== 2. OVERLOADING =====");
        System.out.println("area(5)       = " + area(5));         // square
        System.out.println("area(4, 6)    = " + area(4, 6));      // rectangle
        System.out.println("area(2.0)     = " + area(2.0));       // circle
    }

    static int area(int side) { return side * side; }
    static int area(int length, int width) { return length * width; }
    static double area(double radius) { return Math.PI * radius * radius; }

    // =================================================================
    // 3. Varargs - accept any number of arguments (treated as an array)
    //    Must be the LAST parameter, and only one per method.
    // =================================================================
    static void varargs() {
        System.out.println("\n===== 3. VARARGS =====");
        System.out.println("sum()          = " + sum());
        System.out.println("sum(1, 2, 3)   = " + sum(1, 2, 3));
        System.out.println("sum(array)     = " + sum(new int[]{10, 20}));
    }

    static int sum(int... numbers) {      // 'numbers' is an int[] inside
        int total = 0;
        for (int n : numbers) total += n;
        return total;
    }

    // =================================================================
    // 4. Recursion - a method that calls itself
    //    Every recursive method needs:
    //      (a) BASE CASE    -> stops the recursion
    //      (b) RECURSIVE CASE -> moves toward the base case
    //    Each call adds a frame on the STACK. Too deep -> StackOverflowError.
    // =================================================================
    static void recursion() {
        System.out.println("\n===== 4. RECURSION =====");
        System.out.println("factorial(5) = " + factorial(5));
        System.out.println("fib(10)      = " + fib(10));
        System.out.println("power(2, 10) = " + power(2, 10));
    }

    // factorial(5) = 5 * factorial(4) = 5 * 4 * factorial(3) ... = 120
    static long factorial(int n) {
        if (n <= 1) return 1;            // base case
        return n * factorial(n - 1);     // recursive case
    }

    // Naive fib is O(2^n) - recomputes the same values. (DP fixes this later.)
    static int fib(int n) {
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    // Fast power: O(log n) instead of O(n). x^n = (x^(n/2))^2
    static long power(long x, int n) {
        if (n == 0) return 1;
        long half = power(x, n / 2);
        return (n % 2 == 0) ? half * half : half * half * x;
    }

    // =================================================================
    // 5. Arrays - declaration, initialization, traversal
    // =================================================================
    static void arrayBasics() {
        System.out.println("\n===== 5. ARRAYS =====");

        int[] a = new int[5];              // all elements = 0 (default)
        int[] b = {10, 20, 30, 40, 50};    // literal initialization
        String[] names = new String[3];    // all elements = null

        a[0] = 7;                          // write
        int first = b[0];                  // read
        System.out.println("a = " + Arrays.toString(a) + ", b[0] = " + first);
        System.out.println("names = " + Arrays.toString(names));
        System.out.println("length of b = " + b.length);  // length is a FIELD, not a method

        // Out of bounds -> runtime exception
        try {
            System.out.println(b[5]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("b[5] -> " + e.getMessage());
        }

        // Find max - the most basic array algorithm. O(n)
        int max = b[0];
        for (int i = 1; i < b.length; i++) {
            if (b[i] > max) max = b[i];
        }
        System.out.println("max = " + max);

        // Assigning an array copies the REFERENCE, not the data
        int[] alias = b;
        alias[0] = 999;
        System.out.println("b[0] after changing alias = " + b[0]);   // 999
    }

    // =================================================================
    // 6. java.util.Arrays - helpers you'll use all the time
    // =================================================================
    static void arraysUtilityClass() {
        System.out.println("\n===== 6. java.util.Arrays =====");
        int[] arr = {5, 2, 9, 1, 7};

        int[] copy = Arrays.copyOf(arr, arr.length);       // real copy (new array)
        int[] part = Arrays.copyOfRange(arr, 1, 4);        // index 1..3 (end exclusive)
        System.out.println("copy = " + Arrays.toString(copy) + ", range[1,4) = " + Arrays.toString(part));

        Arrays.sort(arr);                                  // primitives: dual-pivot quicksort, O(n log n)
        System.out.println("sorted = " + Arrays.toString(arr));

        int idx = Arrays.binarySearch(arr, 7);             // array MUST be sorted
        System.out.println("binarySearch(7) = index " + idx);

        int[] filled = new int[4];
        Arrays.fill(filled, -1);                           // common for DP tables / visited arrays
        System.out.println("filled = " + Arrays.toString(filled));

        System.out.println("equals? " + Arrays.equals(arr, new int[]{1, 2, 5, 7, 9}));

        // Sort objects in descending order (needs Integer[], not int[])
        Integer[] boxed = {5, 2, 9, 1};
        Arrays.sort(boxed, (x, y) -> y - x);
        System.out.println("descending = " + Arrays.toString(boxed));
    }

    // =================================================================
    // 7. 2D arrays - an "array of arrays"
    // =================================================================
    static void twoDArrays() {
        System.out.println("\n===== 7. 2D ARRAYS =====");
        int[][] grid = {
                {1, 2, 3},
                {4, 5, 6}
        };
        int rows = grid.length;        // 2
        int cols = grid[0].length;     // 3
        System.out.println("rows=" + rows + " cols=" + cols + " grid[1][2]=" + grid[1][2]);

        // Row-major traversal
        for (int[] row : grid) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println("deepToString: " + Arrays.deepToString(grid));

        // Jagged array: rows can have different lengths
        int[][] jagged = new int[3][];
        for (int i = 0; i < 3; i++) jagged[i] = new int[i + 1];
        System.out.println("jagged: " + Arrays.deepToString(jagged));

        // Transpose (rows become columns)
        int[][] t = new int[cols][rows];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                t[c][r] = grid[r][c];
        System.out.println("transpose: " + Arrays.deepToString(t));

        // 4-direction neighbours (used in grid BFS/DFS problems)
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // up, down, left, right
        int r = 0, c = 1;
        System.out.print("neighbours of (0,1): ");
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {   // bounds check
                System.out.print(grid[nr][nc] + " ");
            }
        }
        System.out.println();
    }
}
