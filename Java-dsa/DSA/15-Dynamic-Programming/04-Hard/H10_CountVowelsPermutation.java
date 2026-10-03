/*
 * ============================================================================
 *  HARD 10 : COUNT VOWELS PERMUTATION        LeetCode #1220
 *  https://leetcode.com/problems/count-vowels-permutation/
 *  Pattern: STATE MACHINE COUNTING DP - dp[last letter], one step per length
 * ============================================================================
 *
 *  PROBLEM
 *    Count strings of length n over a,e,i,o,u where
 *      a -> only e              e -> a or i            i -> anything but i
 *      o -> i or u              u -> only a           (x -> y: y may follow x)
 *    Answer mod 1e9+7.     n=1 -> 5,  n=2 -> 10,  n=5 -> 68,  n=144 -> 18208803
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: build all strings -> up to 5^n.
 *
 *  KEY INSIGHT: to extend a string you only need its LAST letter. So
 *      STATE       cnt[v] = number of valid strings of the current length ending in v
 *      TRANSITION  flip the rules to "who may come BEFORE v":
 *          a <- e, i, u        e <- a, i        i <- e, o        o <- i        u <- i, o
 *      BASE        length 1: every cnt = 1
 *      ANSWER      sum of cnt after n-1 steps
 *  It's a walk count on a 5-node graph. For huge n (10^18), raise the 5x5 transition
 *  matrix to the power n-1 in O(5^3 log n) - the same trick as Fibonacci (Recursion E01).
 *
 *  DRY RUN   (a, e, i, o, u)
 *    n=1: 1 1 1 1 1                       = 5
 *    n=2: a=e+i+u=3, e=a+i=2, i=e+o=2, o=i=1, u=i+o=2   = 10
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class H10_CountVowelsPermutation {

    static final int MOD = 1_000_000_007;

    static int countVowelPermutation(int n) {
        long a = 1, e = 1, i = 1, o = 1, u = 1;
        for (int len = 2; len <= n; len++) {
            long na = (e + i + u) % MOD;                  // a can follow e, i, u
            long ne = (a + i) % MOD;
            long ni = (e + o) % MOD;
            long no = i;
            long nu = (i + o) % MOD;
            a = na; e = ne; i = ni; o = no; u = nu;
        }
        return (int) ((a + e + i + o + u) % MOD);
    }

    // matrix power version: M[x][y] = 1 if y may follow x (row = previous letter)
    static int matrixVersion(long n) {
        long[][] m = {
            {0, 1, 0, 0, 0},     // a -> e
            {1, 0, 1, 0, 0},     // e -> a, i
            {1, 1, 0, 1, 1},     // i -> a, e, o, u
            {0, 0, 1, 0, 1},     // o -> i, u
            {1, 0, 0, 0, 0}};    // u -> a
        long[][] r = pow(m, n - 1);
        long sum = 0;
        for (long[] row : r) for (long x : row) sum += x;
        return (int) (sum % MOD);
    }

    static long[][] pow(long[][] m, long p) {
        long[][] res = new long[5][5];
        for (int k = 0; k < 5; k++) res[k][k] = 1;
        while (p > 0) {
            if ((p & 1) == 1) res = mul(res, m);
            m = mul(m, m);
            p >>= 1;
        }
        return res;
    }

    static long[][] mul(long[][] x, long[][] y) {
        long[][] z = new long[5][5];
        for (int i = 0; i < 5; i++) for (int k = 0; k < 5; k++) for (int j = 0; j < 5; j++) z[i][j] = (z[i][j] + x[i][k] * y[k][j]) % MOD;
        return z;
    }

    static final String NEXT[] = {"e", "ai", "aeou", "iu", "a"};

    static long brute(int remaining, int last) {         // DFS over all strings
        if (remaining == 0) return 1;
        long c = 0;
        for (char nx : NEXT[last].toCharArray()) c += brute(remaining - 1, "aeiou".indexOf(nx));
        return c;
    }

    public static void main(String[] args) {
        check("n=1", 5, countVowelPermutation(1));
        check("n=2", 10, countVowelPermutation(2));
        check("n=5", 68, countVowelPermutation(5));
        check("n=144", 18208803, countVowelPermutation(144));
        boolean ok = true;
        for (int n = 1; n <= 10; n++) { long b = 0; for (int v = 0; v < 5; v++) b += brute(n - 1, v); ok &= countVowelPermutation(n) == b; }
        check("n = 1..10 vs listing every string", true, ok);
        ok = true;
        for (int n = 1; n <= 2000; n += 37) ok &= countVowelPermutation(n) == matrixVersion(n);
        check("DP == matrix power", true, ok);
        System.out.println("   n = 10^18 via matrix power: " + matrixVersion(1_000_000_000_000_000_000L));
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
