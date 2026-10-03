/*
 * ============================================================================
 *  RECURSION BASICS: base case, the call stack, and the classic warm-ups
 * ============================================================================
 *
 *  RECURSION = a method that calls ITSELF on a SMALLER version of the problem.
 *  Every recursive method needs exactly two things:
 *    1. BASE CASE      : the smallest input, answered directly (stops the recursion)
 *    2. RECURSIVE CASE : solve a smaller input, then use that answer
 *  Missing/unreachable base case -> infinite recursion -> StackOverflowError.
 *
 *  THE "LEAP OF FAITH": when writing f(n), ASSUME f(n-1) already works. Don't trace
 *  the whole thing in your head; just ask "if I had the answer for n-1, how do I
 *  get the answer for n?"
 *      factorial(n) = n * factorial(n - 1),   factorial(0) = 1
 *
 *  THE CALL STACK (what really happens): every call gets a STACK FRAME with its own
 *  parameters and local variables. factorial(3):
 *
 *      factorial(3) waits for factorial(2)            push
 *        factorial(2) waits for factorial(1)          push
 *          factorial(1) waits for factorial(0)        push
 *            factorial(0) returns 1                   pop  (base case)
 *          returns 1 * 1 = 1                          pop
 *        returns 2 * 1 = 2                            pop
 *      returns 3 * 2 = 6                              pop
 *
 *  Recursion depth = how many frames are on the stack at once -> O(depth) memory.
 *  The JVM's default stack holds roughly 10^4 .. 10^5 frames (depends on frame
 *  size and -Xss). Deeper than that -> StackOverflowError.
 *
 *  WORK BEFORE vs AFTER the recursive call
 *    print(n); recurse(n-1)   -> prints n, n-1, ..., 1   ("on the way DOWN")
 *    recurse(n-1); print(n)   -> prints 1, 2, ..., n     ("on the way back UP")
 *
 *  TAIL RECURSION: the recursive call is the LAST thing done (nothing waits for it).
 *  Some languages turn it into a loop; JAVA DOES NOT, so deep tail recursion still
 *  overflows. Convert to a loop yourself when depth can be large.
 *
 *  This file: the 10 warm-ups from leetcode.md's "Recursion Basics" list, each with
 *  its recursive version, complexity, and a check against a loop.
 *  Run:  java RecursionBasics.java
 * ============================================================================
 */

import java.util.*;

public class RecursionBasics {

    // 1. Print 1..n: recurse FIRST, then print (work on the way back up)
    static void print1ToN(int n, List<Integer> out) {
        if (n == 0) return;                       // base case
        print1ToN(n - 1, out);
        out.add(n);
    }

    // 2. Print n..1: print FIRST, then recurse (work on the way down)
    static void printNTo1(int n, List<Integer> out) {
        if (n == 0) return;
        out.add(n);
        printNTo1(n - 1, out);
    }

    // 3. Sum of 1..n
    static long sum(int n) { return n == 0 ? 0 : n + sum(n - 1); }

    // 4. Factorial (and a trace of the call stack)
    static long factorial(int n) { return n <= 1 ? 1 : n * factorial(n - 1); }

    static long factorialTraced(int n, int depth, List<String> trace) {
        String pad = "  ".repeat(depth);
        trace.add(pad + "factorial(" + n + ") called");
        long r = n <= 1 ? 1 : n * factorialTraced(n - 1, depth + 1, trace);
        trace.add(pad + "factorial(" + n + ") returns " + r);
        return r;
    }

    // 5. Fibonacci: TWO recursive calls -> a tree of calls -> exponential time
    static long calls;

    static long fib(int n) {
        calls++;
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);           // fib(n-2) is computed again inside fib(n-1)!
    }

    // ...fixed with MEMOIZATION (remember answers): O(n). This is the seed of Dynamic Programming.
    static long fibMemo(int n, long[] memo) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        return memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    }

    // 6. Power a^n in O(log n): a^n = (a^(n/2))^2, times a if n is odd
    static long power(long a, int n) {
        if (n == 0) return 1;
        long half = power(a, n / 2);              // ONE recursive call, reused twice
        return n % 2 == 0 ? half * half : half * half * a;
    }

    // 7. Reverse a number: carry the reversed-so-far value DOWN as a parameter
    static long reverse(long n, long acc) {
        if (n == 0) return acc;
        return reverse(n / 10, acc * 10 + n % 10);   // a tail call (Java still uses a frame)
    }

    // 8. Sum of digits
    static int digitSum(long n) { return n < 10 ? (int) n : (int) (n % 10) + digitSum(n / 10); }

    // 9. GCD (Euclid): gcd(a, b) = gcd(b, a % b), gcd(a, 0) = a.  O(log min(a, b))
    static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    // 10. Tower of Hanoi: move n disks from 'from' to 'to' using 'via'
    //     move n-1 out of the way, move the biggest, move n-1 back on top. 2^n - 1 moves.
    static void hanoi(int n, char from, char to, char via, List<String> moves) {
        if (n == 0) return;
        hanoi(n - 1, from, via, to, moves);
        moves.add(n + ":" + from + "->" + to);
        hanoi(n - 1, via, to, from, moves);
    }

    static int depthReached;

    static void dive(int d) { depthReached = d; dive(d + 1); }

    public static void main(String[] args) {
        System.out.println("1-2. Work before vs after the recursive call");
        List<Integer> up = new ArrayList<>(), down = new ArrayList<>();
        print1ToN(5, up);
        printNTo1(5, down);
        check("print1ToN(5) (work after the call)", "[1, 2, 3, 4, 5]", up);
        check("printNTo1(5) (work before the call)", "[5, 4, 3, 2, 1]", down);

        System.out.println("3-4. Sum and factorial, with the call stack traced");
        check("sum(100)", 5050L, sum(100));
        check("factorial(20)", 2432902008176640000L, factorial(20));
        List<String> trace = new ArrayList<>();
        factorialTraced(3, 0, trace);
        trace.forEach(line -> System.out.println("      " + line));

        System.out.println("5. Fibonacci: exponential tree vs memoization");
        for (int n : new int[]{10, 20, 30}) {
            calls = 0;
            long f = fib(n);
            System.out.printf("   fib(%d) = %,d took %,d calls%n", n, f, calls);
        }
        check("fibMemo(90)", 2880067194370816120L, fibMemo(90, new long[91]));

        System.out.println("6-9. Power, reverse, digit sum, gcd");
        check("power(3, 13)", 1594323L, power(3, 13));
        check("reverse(12345)", 54321L, reverse(12345, 0));
        check("digitSum(98765)", 35, digitSum(98765));
        check("gcd(84, 36)", 12L, gcd(84, 36));

        System.out.println("10. Tower of Hanoi");
        List<String> moves = new ArrayList<>();
        hanoi(3, 'A', 'C', 'B', moves);
        check("3 disks", "[1:A->C, 2:A->B, 1:C->B, 3:A->C, 1:B->A, 2:B->C, 1:A->C]", moves);
        moves.clear();
        hanoi(10, 'A', 'C', 'B', moves);
        check("10 disks = 2^10 - 1 moves", 1023, moves.size());

        System.out.println("11. Random checks against loops");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = rnd.nextInt(30);
            long loopSum = 0, loopFact = 1, a = 1 + rnd.nextInt(5);
            for (int i = 1; i <= n; i++) { loopSum += i; loopFact *= (i <= 20 ? i : 1); }
            long loopPow = 1;
            for (int i = 0; i < n % 25; i++) loopPow *= a;
            ok &= sum(n) == loopSum && power(a, n % 25) == loopPow;
            if (n <= 20) ok &= factorial(n) == loopFact;
            long x = rnd.nextInt(1_000_000_000), y = 1 + rnd.nextInt(1_000_000);
            ok &= gcd(x, y) == java.math.BigInteger.valueOf(x).gcd(java.math.BigInteger.valueOf(y)).longValue();
            ok &= reverse(x, 0) == Long.parseLong(new StringBuilder(Long.toString(x)).reverse().toString());
        }
        check("1,000 random inputs", true, ok);

        System.out.println("12. Too deep: StackOverflowError");
        try {
            dive(0);
        } catch (StackOverflowError e) {
            System.out.printf("   overflowed after about %,d nested calls (default stack size)%n", depthReached);
        }
        check("overflow happened deep, not immediately", true, depthReached > 1000);
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
