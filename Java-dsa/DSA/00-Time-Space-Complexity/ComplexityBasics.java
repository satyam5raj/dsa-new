/*
 * ============================================================================
 *  TOPIC 0.1 : TIME & SPACE COMPLEXITY - THE BASICS
 * ============================================================================
 *
 *  Run:  java ComplexityBasics.java
 *
 *  WHY DO WE NEED IT?
 *  ------------------
 *  "My code takes 2 seconds" means nothing on its own: it depends on the
 *  machine, the language and the input. We need a way to compare algorithms
 *  that is INDEPENDENT of hardware:
 *
 *      How does the NUMBER OF STEPS grow as the INPUT SIZE (n) grows?
 *
 *  That growth rate is the TIME COMPLEXITY.
 *  The growth of EXTRA MEMORY used is the SPACE COMPLEXITY.
 *
 *  BIG-O NOTATION, O(f(n))
 *  -----------------------
 *  An UPPER BOUND on growth: "for large n, the steps grow AT MOST like f(n)".
 *  Rules for simplifying:
 *    1. DROP CONSTANTS        O(2n)       -> O(n)       (a constant factor doesn't change the SHAPE)
 *    2. DROP LOWER TERMS      O(n^2 + n)  -> O(n^2)     (for big n, n^2 dominates)
 *    3. DIFFERENT INPUTS GET DIFFERENT LETTERS
 *         loop over array a (size n), then over array b (size m)   -> O(n + m)
 *         nested loop a inside b                                   -> O(n * m)
 *    4. SEQUENTIAL steps ADD, NESTED steps MULTIPLY
 *
 *  THE THREE NOTATIONS (interviewers sometimes ask)
 *    Big-O     O : upper bound  ("at most")      <- what people usually mean
 *    Omega     Ω : lower bound  ("at least")
 *    Theta     Θ : tight bound  ("exactly this growth")
 *  BEST / AVERAGE / WORST case are a DIFFERENT question: WHICH input?
 *    Linear search: best O(1) (first element), worst O(n) (not found).
 *    Quick sort   : average O(n log n), worst O(n^2) (bad pivots).
 *  In interviews, quote the WORST case unless asked otherwise.
 *
 *  COMMON COMPLEXITIES (fastest -> slowest)
 *  ------------------------------------------------------------------------
 *   O(1)        constant     array[i], hashMap.get, push/pop
 *   O(log n)    logarithmic  binary search (halve the problem each step)
 *   O(n)        linear       one loop over the input
 *   O(n log n)  linearithmic good sorting (merge sort, Arrays.sort)
 *   O(n^2)      quadratic    two nested loops over the input (all pairs)
 *   O(n^3)      cubic        three nested loops (all triplets)
 *   O(2^n)      exponential  all subsets, naive recursive Fibonacci
 *   O(n!)       factorial    all permutations
 *  ------------------------------------------------------------------------
 *
 *   For n = 1,000,000:
 *     O(log n) ~ 20 steps,  O(n) ~ 10^6,  O(n log n) ~ 2*10^7,  O(n^2) ~ 10^12 (hours!)
 */

import java.util.*;

public class ComplexityBasics {

    public static void main(String[] args) {
        constantTime();
        logarithmicTime();
        linearTime();
        linearithmicTime();
        quadraticTime();
        exponentialTime();
        growthTable();
        measureRealGrowth();
    }

    // =================================================================
    // O(1) - CONSTANT: the work does NOT depend on n
    // =================================================================
    static int getFirst(int[] arr) {
        return arr[0];               // one step, whether arr has 10 or 10 million elements
    }

    static int sumFirstHundred(int[] arr) {
        int sum = 0;
        for (int i = 0; i < 100 && i < arr.length; i++) sum += arr[i];
        return sum;                  // at most 100 steps -> a CONSTANT -> still O(1)
    }

    static void constantTime() {
        System.out.println("===== O(1) CONSTANT =====");
        int[] arr = {7, 3, 9};
        System.out.println("   getFirst = " + getFirst(arr) + " (array access is O(1): address = base + i * size)");
        System.out.println("   sumFirstHundred is O(1) too: the loop is capped at 100, not at n");
    }

    // =================================================================
    // O(log n) - LOGARITHMIC: the problem is CUT IN HALF every step
    //   n -> n/2 -> n/4 -> ... -> 1   takes log2(n) steps
    //   1,000,000 elements -> only ~20 steps!
    // =================================================================
    static int binarySearch(int[] sorted, int target) {
        int lo = 0, hi = sorted.length - 1;
        int steps = 0;
        while (lo <= hi) {
            steps++;
            int mid = lo + (hi - lo) / 2;          // avoids int overflow of (lo + hi)
            if (sorted[mid] == target) {
                System.out.println("   binary search found " + target + " in " + steps + " steps");
                return mid;
            }
            if (sorted[mid] < target) lo = mid + 1;   // throw away the LEFT half
            else hi = mid - 1;                        // throw away the RIGHT half
        }
        return -1;
    }

    static int countHalvings(int n) {
        int steps = 0;
        while (n > 1) {               // i = n, n/2, n/4 ... -> log2(n) iterations
            n /= 2;
            steps++;
        }
        return steps;
    }

    static void logarithmicTime() {
        System.out.println("\n===== O(log n) LOGARITHMIC =====");
        int[] sorted = new int[1_000_000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i * 2;
        binarySearch(sorted, 1_999_998);                      // the last element
        System.out.println("   halvings for n=1,000,000: " + countHalvings(1_000_000));
        System.out.println("   halvings for n=1,000,000,000: " + countHalvings(1_000_000_000) + " (1000x more data, only 10 more steps)");
    }

    // =================================================================
    // O(n) - LINEAR: touch each element a constant number of times
    // =================================================================
    static int findMax(int[] arr) {
        int max = arr[0];
        for (int x : arr) {           // n iterations
            if (x > max) max = x;     // O(1) work each
        }
        return max;                   // total: n * O(1) = O(n)
    }

    // Two SEPARATE loops: n + n = 2n -> drop the constant -> O(n)
    static int minPlusMax(int[] arr) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x : arr) min = Math.min(min, x);
        for (int x : arr) max = Math.max(max, x);
        return min + max;
    }

    static void linearTime() {
        System.out.println("\n===== O(n) LINEAR =====");
        int[] arr = {4, 9, 1, 7};
        System.out.println("   findMax = " + findMax(arr) + ", minPlusMax = " + minPlusMax(arr) + "  (2 loops = O(2n) = O(n))");
    }

    // =================================================================
    // O(n log n): typically "sort first" or divide-and-conquer
    //   merge sort: log n LEVELS of splitting, O(n) merge work per level
    // =================================================================
    static boolean hasDuplicateBySorting(int[] arr) {
        int[] copy = arr.clone();             // O(n) space
        Arrays.sort(copy);                    // O(n log n)  <- dominates
        for (int i = 1; i < copy.length; i++) {
            if (copy[i] == copy[i - 1]) return true;   // O(n)
        }
        return false;                         // O(n log n) + O(n) = O(n log n)
    }

    static void linearithmicTime() {
        System.out.println("\n===== O(n log n) =====");
        System.out.println("   hasDuplicate([3,1,4,1]) = " + hasDuplicateBySorting(new int[]{3, 1, 4, 1}));
    }

    // =================================================================
    // O(n^2) - QUADRATIC: nested loops over the same input (all pairs)
    // =================================================================
    static int countPairsWithSum(int[] arr, int target) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {          // n times
            for (int j = i + 1; j < arr.length; j++) {  // n-1, n-2, ..., 1 times
                if (arr[i] + arr[j] == target) count++;
            }
        }
        // total iterations = (n-1) + (n-2) + ... + 1 = n(n-1)/2 -> O(n^2)
        return count;
    }

    static void quadraticTime() {
        System.out.println("\n===== O(n^2) QUADRATIC =====");
        System.out.println("   pairs summing to 5 in [1,2,3,4] = " + countPairsWithSum(new int[]{1, 2, 3, 4}, 5));
        System.out.println("   n(n-1)/2 is still O(n^2): drop the 1/2 and the lower term");
    }

    // =================================================================
    // O(2^n) - EXPONENTIAL: each call branches into 2 more calls
    // =================================================================
    static long calls = 0;

    static long fibNaive(int n) {
        calls++;
        if (n <= 1) return n;
        return fibNaive(n - 1) + fibNaive(n - 2);   // 2 branches per level, ~n levels -> ~2^n calls
    }

    static long fibMemo(int n, long[] memo) {       // remember answers -> each n computed once -> O(n)
        calls++;
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        return memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    }

    static void exponentialTime() {
        System.out.println("\n===== O(2^n) EXPONENTIAL =====");
        calls = 0;
        long a = fibNaive(30);
        System.out.printf("   fibNaive(30) = %d using %,d calls%n", a, calls);
        calls = 0;
        long b = fibMemo(30, new long[31]);
        System.out.printf("   fibMemo(30)  = %d using %,d calls  <- memoization: O(2^n) -> O(n)%n", b, calls);
    }

    // =================================================================
    // How big can n be?  (~10^8 simple operations per second is a safe rule)
    // Use the CONSTRAINTS of a problem to guess the expected complexity!
    // =================================================================
    static void growthTable() {
        System.out.println("\n===== READ THE CONSTRAINTS =====");
        System.out.println("""
                   n <= 10            -> O(n!) or O(2^n * n) is fine   (permutations, backtracking)
                   n <= 20            -> O(2^n)                        (all subsets)
                   n <= 500           -> O(n^3)
                   n <= 5,000         -> O(n^2)
                   n <= 10^5 .. 10^6  -> O(n log n) or O(n)            (MOST array problems!)
                   n >= 10^8          -> O(log n) or O(1)              (math, binary search)""");
    }

    // =================================================================
    // Seeing it for real: double n and watch the time
    //   O(n)   : time doubles
    //   O(n^2) : time QUADRUPLES
    // =================================================================
    static void measureRealGrowth() {
        System.out.println("\n===== MEASURED: DOUBLE n, WATCH THE TIME =====");
        for (int i = 0; i < 3; i++) { linearWork(20_000); quadraticWork(2_000); }   // JIT warm-up
        for (int n : new int[]{4_000, 8_000, 16_000}) {
            long t0 = System.nanoTime();
            long sink = linearWork(n * 100);
            long t1 = System.nanoTime();
            sink += quadraticWork(n);
            long t2 = System.nanoTime();
            System.out.printf("   n=%,6d  O(n) loop: %6.2f ms   O(n^2) loop: %8.2f ms  (sink %d)%n",
                    n, (t1 - t0) / 1e6, (t2 - t1) / 1e6, sink & 1);
        }
        System.out.println("   -> each time n doubles, the O(n^2) column grows ~4x");
    }

    static long linearWork(int n) {
        long s = 0;
        for (int i = 0; i < n; i++) s += i ^ (s >>> 3);
        return s;
    }

    static long quadraticWork(int n) {
        long s = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) s += (i ^ j) & 1;
        return s;
    }
}
