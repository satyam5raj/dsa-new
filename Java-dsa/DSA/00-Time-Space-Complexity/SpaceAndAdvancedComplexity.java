/*
 * ============================================================================
 *  TOPIC 0.2 : SPACE COMPLEXITY, RECURSION, AMORTIZED ANALYSIS
 * ============================================================================
 *
 *  Run:  java SpaceAndAdvancedComplexity.java
 *
 *  SPACE COMPLEXITY = how much EXTRA memory the algorithm needs as n grows.
 *    - "Auxiliary space" = memory you allocate BEYOND the input.
 *      Interviewers usually mean this one. The input itself isn't counted.
 *    - Includes: new arrays/lists/maps, AND the RECURSION CALL STACK
 *      (each pending call keeps a stack frame).
 *    - "In-place" = O(1) extra space (modify the input directly).
 *
 *  THE TIME/SPACE TRADE-OFF
 *    Very often you can buy speed with memory:
 *      Two Sum with nested loops : O(n^2) time, O(1) space
 *      Two Sum with a HashMap    : O(n)   time, O(n) space   <- usually the better deal
 *
 *  RECURSION COMPLEXITY
 *    time  = (number of calls) x (work per call)
 *    space = (max DEPTH of the recursion) x (space per frame)
 *
 *  AMORTIZED ANALYSIS
 *    The AVERAGE cost per operation over a long SEQUENCE of operations.
 *    ArrayList.add() is usually O(1), but sometimes O(n) when the backing array
 *    is full and must be copied. Because the capacity DOUBLES (x1.5 in Java), the
 *    expensive copies are rare: n adds cost O(n) total -> O(1) AMORTIZED per add.
 */

import java.util.*;

public class SpaceAndAdvancedComplexity {

    public static void main(String[] args) {
        spaceExamples();
        recursionSpace();
        recursionTime();
        amortizedAnalysis();
        commonDataStructureCosts();
        practiceQuiz();
    }

    // =================================================================
    // 1. Space complexity examples
    // =================================================================

    // O(1) extra space: only a few variables, no matter how big arr is
    static int sum(int[] arr) {
        int total = 0;
        for (int x : arr) total += x;
        return total;
    }

    // O(n) extra space: a new array as big as the input
    static int[] doubled(int[] arr) {
        int[] out = new int[arr.length];
        for (int i = 0; i < arr.length; i++) out[i] = arr[i] * 2;
        return out;
    }

    // O(1) extra space, IN-PLACE: we reuse the input array
    static void reverseInPlace(int[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            int t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
    }

    // O(n^2) space: an n x n table
    static int[][] multiplicationTable(int n) {
        int[][] t = new int[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) t[i][j] = (i + 1) * (j + 1);
        return t;
    }

    static void spaceExamples() {
        System.out.println("===== 1. SPACE COMPLEXITY =====");
        int[] arr = {1, 2, 3, 4};
        System.out.println("   sum            -> O(1) extra  : " + sum(arr));
        System.out.println("   doubled        -> O(n) extra  : " + Arrays.toString(doubled(arr)));
        reverseInPlace(arr);
        System.out.println("   reverseInPlace -> O(1) extra  : " + Arrays.toString(arr));
        System.out.println("   table(3)       -> O(n^2) extra: " + Arrays.deepToString(multiplicationTable(3)));
    }

    // =================================================================
    // 2. Recursion uses STACK space even with no explicit array!
    // =================================================================
    static int maxDepth = 0;

    static int sumRecursive(int[] arr, int i, int depth) {
        maxDepth = Math.max(maxDepth, depth);
        if (i == arr.length) return 0;                         // base case
        return arr[i] + sumRecursive(arr, i + 1, depth + 1);   // n nested calls are alive at once
    }

    static void recursionSpace() {
        System.out.println("\n===== 2. RECURSION USES STACK SPACE =====");
        int[] arr = new int[1000];
        Arrays.fill(arr, 1);
        maxDepth = 0;
        int s = sumRecursive(arr, 0, 1);
        System.out.println("   sumRecursive of 1000 ones = " + s + ", max stack depth = " + maxDepth);
        System.out.println("   -> O(n) SPACE even though no array was created (the loop version is O(1))");
        System.out.println("   -> too deep (~10^5 frames) can throw StackOverflowError: prefer loops for linear work");
    }

    // =================================================================
    // 3. Recursion time = calls x work per call
    // =================================================================
    static long calls;

    // T(n) = T(n/2) + O(1)  -> log n calls -> O(log n)    (binary search shape)
    static int halving(int n) {
        calls++;
        return n <= 1 ? 0 : 1 + halving(n / 2);
    }

    // T(n) = 2T(n/2) + O(n) -> log n levels, O(n) work per level -> O(n log n)   (merge sort shape)
    static void mergeSortShape(int lo, int hi) {
        calls += (hi - lo);                       // count "O(n) work" at this level
        if (hi - lo <= 1) return;
        int mid = (lo + hi) / 2;
        mergeSortShape(lo, mid);
        mergeSortShape(mid, hi);
    }

    static void recursionTime() {
        System.out.println("\n===== 3. RECURSION TIME =====");
        calls = 0; halving(1 << 20);
        System.out.println("   T(n)=T(n/2)+1   for n=2^20: " + calls + " calls -> O(log n)");
        calls = 0; mergeSortShape(0, 1 << 16);
        System.out.printf("   T(n)=2T(n/2)+n  for n=2^16: %,d work units = n * log2(n) = %,d -> O(n log n)%n", calls, (1 << 16) * 16L);
    }

    // =================================================================
    // 4. Amortized O(1): count the copies a doubling array makes
    // =================================================================
    static void amortizedAnalysis() {
        System.out.println("\n===== 4. AMORTIZED ANALYSIS (dynamic array) =====");
        int capacity = 1, size = 0;
        long copies = 0;
        int n = 1_000_000;
        for (int i = 0; i < n; i++) {
            if (size == capacity) {            // full -> allocate double and copy everything
                copies += size;
                capacity *= 2;
            }
            size++;
        }
        System.out.printf("   %,d adds caused %,d element copies in total -> %.2f copies per add%n", n, copies, (double) copies / n);
        System.out.println("   1 + 2 + 4 + ... + n < 2n  ->  total O(n)  ->  O(1) AMORTIZED per add");
        System.out.println("   (if capacity grew by +10 instead of x2, the copies would be O(n^2) in total!)");
    }

    // =================================================================
    // 5. Cheat sheet: costs of the structures you'll use in DSA
    // =================================================================
    static void commonDataStructureCosts() {
        System.out.println("\n===== 5. DATA STRUCTURE COSTS =====");
        System.out.println("""
                   Structure          access   search    insert           delete
                   Array              O(1)     O(n)      O(n) (shift)     O(n) (shift)
                   ArrayList          O(1)     O(n)      O(1)* at end     O(n)
                   LinkedList         O(n)     O(n)      O(1) at ends     O(1) at ends
                   HashMap/HashSet    -        O(1) avg  O(1) avg         O(1) avg      (O(n) worst)
                   TreeMap/TreeSet    -        O(log n)  O(log n)         O(log n)
                   Stack / Queue      top O(1) O(n)      O(1)             O(1)
                   PriorityQueue      min O(1) O(n)      O(log n)         O(log n)
                   Sorting            Arrays.sort / Collections.sort -> O(n log n)
                   (* amortized)""");
    }

    // =================================================================
    // 6. Practice: guess first, then read the answer
    // =================================================================
    static void practiceQuiz() {
        System.out.println("\n===== 6. PRACTICE QUIZ =====");
        String[][] quiz = {
                {"for i in 0..n: for j in 0..n: O(1)", "O(n^2) time"},
                {"for i in 0..n: for j in 0..10: O(1)", "O(n): the inner loop is a constant 10"},
                {"for (i = 1; i < n; i *= 2)", "O(log n): i doubles each time"},
                {"for i in 0..n: for (j = 1; j < n; j *= 2)", "O(n log n)"},
                {"for i in 0..n: for j in i..n", "O(n^2): n + (n-1) + ... + 1 = n(n+1)/2"},
                {"loop over a (n), then loop over b (m)", "O(n + m)"},
                {"sort, then binary search k times", "O(n log n + k log n)"},
                {"recursive f(n) = f(n-1) + f(n-1)", "O(2^n) time, O(n) space (depth)"},
                {"HashSet of all n elements", "O(n) time, O(n) space"},
                {"two pointers from both ends moving inward", "O(n) time, O(1) space"},
        };
        for (int i = 0; i < quiz.length; i++) {
            System.out.printf("   Q%-2d %-46s -> %s%n", i + 1, quiz[i][0], quiz[i][1]);
        }
    }
}
