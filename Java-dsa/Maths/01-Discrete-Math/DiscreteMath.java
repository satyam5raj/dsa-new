/*
 * ============================================================================
 *  MATHS 01 : DISCRETE MATHEMATICS
 * ============================================================================
 *  Run:  java DiscreteMath.java          (explanations: README.md in this folder)
 *
 *    1. sets               union / intersection, power set via bitmasks, inclusion-exclusion
 *    2. functions          injective / surjective / bijective, pigeonhole
 *    3. counting           factorial, permutations, combinations, Pascal, stars & bars,
 *                          binomial theorem, derangements, Catalan numbers
 *    4. recurrences        Fibonacci, merge sort T(n) = 2T(n/2) + n, Hanoi, Master theorem
 *    5. graphs             handshake lemma, trees have n-1 edges, bipartite check, Euler
 *    6. number theory      gcd, sieve, modular arithmetic, fast power, inverse, toy RSA
 *    7. asymptotics        growth-rate table
 * ============================================================================
 */

import java.math.BigInteger;
import java.util.*;

public class DiscreteMath {

    // =====================================================================
    // 1. sets
    // =====================================================================

    static void sets() {
        System.out.println("1. Sets");
        Set<Integer> a = new TreeSet<>(List.of(1, 2, 3, 4)), b = new TreeSet<>(List.of(3, 4, 5));
        Set<Integer> union = new TreeSet<>(a); union.addAll(b);
        Set<Integer> inter = new TreeSet<>(a); inter.retainAll(b);
        Set<Integer> diff = new TreeSet<>(a); diff.removeAll(b);
        check("A union B", "[1, 2, 3, 4, 5]", union);
        check("A intersect B", "[3, 4]", inter);
        check("A minus B", "[1, 2]", diff);
        check("|A u B| = |A| + |B| - |A n B|", union.size(), a.size() + b.size() - inter.size());
        // power set of {a, b, c}: every number 0..2^3-1 is a subset (bit i = "take element i")
        char[] items = {'a', 'b', 'c'};
        List<String> power = new ArrayList<>();
        for (int mask = 0; mask < (1 << items.length); mask++) {
            StringBuilder s = new StringBuilder("{");
            for (int i = 0; i < items.length; i++) if ((mask >> i & 1) == 1) s.append(items[i]);
            power.add(s.append('}').toString());
        }
        check("power set of {a,b,c}", "[{}, {a}, {b}, {ab}, {c}, {ac}, {bc}, {abc}]", power);
        int count = 0;
        for (int x = 1; x <= 100; x++) if (x % 2 == 0 || x % 3 == 0 || x % 5 == 0) count++;
        int incExc = 50 + 33 + 20 - 16 - 10 - 6 + 3;                      // |A|+|B|+|C| - pairs + triple
        check("1..100 divisible by 2, 3 or 5 (count)", 74, count);
        check("inclusion-exclusion gives the same", 74, incExc);
    }

    // =====================================================================
    // 2. functions and the pigeonhole principle
    // =====================================================================

    static void functionsAndPigeonhole() {
        System.out.println("2. Functions, pigeonhole");
        int[] f = {2, 0, 1};            // f: {0,1,2} -> {0,1,2}, f(0)=2, f(1)=0, f(2)=1
        int[] g = {1, 1, 0};
        check("f is injective (one-to-one)", true, injective(f));
        check("f is surjective (onto)", true, surjective(f, 3));
        check("g injective? (g(0) = g(1))", false, injective(g));
        check("g surjective? (2 never hit)", false, surjective(g, 3));
        // pigeonhole: hashing 1,000 keys into 999 buckets MUST collide
        Random rnd = new Random(1);
        boolean alwaysCollides = true;
        for (int t = 0; t < 100; t++) {
            Set<Integer> buckets = new HashSet<>();
            boolean collided = false;
            for (int k = 0; k < 1000; k++) if (!buckets.add(rnd.nextInt(999))) collided = true;
            alwaysCollides &= collided;
        }
        check("1,000 keys into 999 buckets always collide", true, alwaysCollides);
    }

    static boolean injective(int[] f) { Set<Integer> seen = new HashSet<>(); for (int y : f) if (!seen.add(y)) return false; return true; }
    static boolean surjective(int[] f, int m) { Set<Integer> hit = new HashSet<>(); for (int y : f) hit.add(y); return hit.size() == m; }

    // =====================================================================
    // 3. counting
    // =====================================================================

    static BigInteger factorial(int n) { BigInteger r = BigInteger.ONE; for (int i = 2; i <= n; i++) r = r.multiply(BigInteger.valueOf(i)); return r; }

    static long nPr(int n, int r) { long p = 1; for (int i = 0; i < r; i++) p *= n - i; return p; }

    static long nCr(int n, int r) {                                       // multiply then divide step by step: stays exact
        r = Math.min(r, n - r);
        long c = 1;
        for (int i = 1; i <= r; i++) c = c * (n - r + i) / i;
        return c;
    }

    static long[][] pascal(int rows) {
        long[][] p = new long[rows][];
        for (int n = 0; n < rows; n++) {
            p[n] = new long[n + 1];
            p[n][0] = p[n][n] = 1;
            for (int k = 1; k < n; k++) p[n][k] = p[n - 1][k - 1] + p[n - 1][k];   // C(n,k) = C(n-1,k-1) + C(n-1,k)
        }
        return p;
    }

    static void counting() {
        System.out.println("3. Counting (combinatorics)");
        check("5! = 120", "120", factorial(5));
        check("20! fits a long, 21! does not", "51090942171709440000", factorial(21));
        check("arrange 3 of 5 books in order: P(5,3)", 60L, nPr(5, 3));
        check("choose 2 of 5: C(5,2)", 10L, nCr(5, 2));
        check("poker hands C(52,5)", 2_598_960L, nCr(52, 5));
        long[][] p = pascal(6);
        check("Pascal row 5", "[1, 5, 10, 10, 5, 1]", Arrays.toString(p[5]));
        long rowSum = 0;
        for (long v : p[5]) rowSum += v;
        check("sum of row n = 2^n (all subsets)", 32L, rowSum);
        // brute force all permutations of 4 items: 4! = 24, and count derangements (nobody in their own seat)
        int[] perm = {0, 1, 2, 3};
        int perms = 0, derangements = 0;
        do {
            perms++;
            boolean fixed = false;
            for (int i = 0; i < 4; i++) if (perm[i] == i) fixed = true;
            if (!fixed) derangements++;
        } while (nextPermutation(perm));
        check("permutations of 4", 24, perms);
        check("derangements of 4 (hat-check problem)", 9, derangements);
        check("stars and bars: 10 sweets, 3 kids: C(12,2)", 66L, nCr(12, 2));
        int sweets = 0;
        for (int x = 0; x <= 10; x++) for (int y = 0; x + y <= 10; y++) sweets++;    // z = 10 - x - y is forced
        check("brute-force count of x+y+z=10", 66, sweets);
        // Catalan: number of BSTs with n keys = number of valid bracket strings with n pairs
        long[] cat = new long[11];
        cat[0] = 1;
        for (int n = 1; n <= 10; n++) for (int i = 0; i < n; i++) cat[n] += cat[i] * cat[n - 1 - i];
        check("Catalan(5) by recurrence", 42L, cat[5]);
        check("Catalan(5) = C(10,5)/6", 42L, nCr(10, 5) / 6);
        check("binomial: (x+y)^4 coefficients", "[1, 4, 6, 4, 1]", Arrays.toString(pascal(5)[4]));
    }

    static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) { t = a[l]; a[l] = a[r]; a[r] = t; }
        return true;
    }

    // =====================================================================
    // 4. recurrences
    // =====================================================================

    static long mergeSortWork(int n) { return n <= 1 ? 0 : 2 * mergeSortWork(n / 2) + n; }    // T(n) = 2T(n/2) + n
    static long binarySearchSteps(int n) { return n <= 1 ? 0 : 1 + binarySearchSteps(n / 2); }  // T(n) = T(n/2) + 1
    static long hanoi(int n) { return n == 0 ? 0 : 2 * hanoi(n - 1) + 1; }                      // T(n) = 2T(n-1) + 1

    static void recurrences() {
        System.out.println("4. Recurrences");
        long a = 0, b = 1;
        for (int i = 0; i < 50; i++) { long t = a + b; a = b; b = t; }
        check("Fibonacci F(50)", 12_586_269_025L, a);
        double phi = (1 + Math.sqrt(5)) / 2;
        check("Binet: round(phi^50 / sqrt 5)", 12_586_269_025L, Math.round(Math.pow(phi, 50) / Math.sqrt(5)));
        check("merge sort T(1024) = n log2 n", 10_240L, mergeSortWork(1024));
        check("binary search T(1024) = log2 n", 10L, binarySearchSteps(1024));
        check("Tower of Hanoi, 10 discs = 2^10 - 1", 1023L, hanoi(10));
        // Master theorem case 1: T(n) = 4T(n/2) + n  -> n^2;  ratio T(n)/n^2 settles
        double r1 = master(4, 2, 1, 1 << 10) / Math.pow(1 << 10, 2), r2 = master(4, 2, 1, 1 << 14) / Math.pow(1 << 14, 2);
        check("T(n)=4T(n/2)+n grows like n^2 (ratio ~2)", "2.0", String.format("%.1f", r2));
        check("...and the ratio barely moves", true, Math.abs(r1 - r2) < 0.01);
    }

    /** T(n) = a T(n/b) + n^d with T(1) = 1, n a power of b. */
    static double master(int a, int b, int d, int n) { return n <= 1 ? 1 : a * master(a, b, d, n / b) + Math.pow(n, d); }

    // =====================================================================
    // 5. graphs
    // =====================================================================

    static void graphs() {
        System.out.println("5. Graphs");
        int[][] edges = {{0, 1}, {0, 2}, {1, 2}, {2, 3}, {3, 4}};
        int[] degree = new int[5];
        for (int[] e : edges) { degree[e[0]]++; degree[e[1]]++; }
        check("handshake: sum of degrees = 2|E|", 2 * edges.length, Arrays.stream(degree).sum());
        check("complete graph K10 has n(n-1)/2 edges", 45L, nCr(10, 2));
        // a tree: connected + no cycle. Add random edges, skipping any that would close a cycle,
        // until everything is connected: we always end up with exactly n-1 edges.
        Random rnd = new Random(5);
        boolean treeEdges = true;
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(50), components = n, used = 0;
            int[] parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            while (components > 1) {
                int ra = find(parent, rnd.nextInt(n)), rb = find(parent, rnd.nextInt(n));
                if (ra == rb) continue;                                      // would make a cycle: skip
                parent[ra] = rb; components--; used++;
            }
            treeEdges &= used == n - 1;
        }
        check("random spanning trees all have n-1 edges", true, treeEdges);
        check("even cycle C6 is bipartite", true, bipartite(6, cycle(6)));
        check("odd cycle C5 is NOT bipartite", false, bipartite(5, cycle(5)));
        // Euler path exists iff 0 or 2 vertices have odd degree (connected graph)
        long odd = Arrays.stream(degree).filter(d -> d % 2 == 1).count();
        check("odd-degree vertices in the sample graph", 2L, odd);
        // Konigsberg: 4 land masses, 7 bridges, all four odd -> no walk crossing each bridge once
        int[] konigsberg = {5, 3, 3, 3};
        check("Konigsberg odd-degree vertices (so no Euler path)", 4L, Arrays.stream(konigsberg).filter(d -> d % 2 == 1).count());
    }

    static int find(int[] p, int x) { while (p[x] != x) x = p[x] = p[p[x]]; return x; }

    static int[][] cycle(int n) { int[][] e = new int[n][]; for (int i = 0; i < n; i++) e[i] = new int[]{i, (i + 1) % n}; return e; }

    static boolean bipartite(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        int[] color = new int[n];
        Arrays.fill(color, -1);
        Deque<Integer> q = new ArrayDeque<>();
        for (int s = 0; s < n; s++) {
            if (color[s] != -1) continue;
            color[s] = 0; q.add(s);
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) {
                    if (color[v] == -1) { color[v] = 1 - color[u]; q.add(v); }
                    else if (color[v] == color[u]) return false;          // same colour on both ends of an edge
                }
            }
        }
        return true;
    }

    // =====================================================================
    // 6. number theory
    // =====================================================================

    static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    static long modPow(long base, long exp, long mod) {                  // square-and-multiply: O(log exp)
        long result = 1;
        base %= mod;
        while (exp > 0) {
            if ((exp & 1) == 1) result = result * base % mod;
            base = base * base % mod;
            exp >>= 1;
        }
        return result;
    }

    /** Returns {g, x, y} with a*x + b*y = g = gcd(a, b). */
    static long[] extendedGcd(long a, long b) {
        if (b == 0) return new long[]{a, 1, 0};
        long[] r = extendedGcd(b, a % b);
        return new long[]{r[0], r[2], r[1] - (a / b) * r[2]};
    }

    static void numberTheory() {
        System.out.println("6. Number theory");
        check("gcd(48, 18)", 6L, gcd(48, 18));
        check("lcm(4, 6) = 4*6/gcd", 12L, 4 * 6 / gcd(4, 6));
        boolean[] composite = new boolean[1_000_001];
        int primes = 0, under100 = 0;
        for (int i = 2; i <= 1_000_000; i++) {
            if (composite[i]) continue;
            primes++;
            if (i < 100) under100++;
            for (long j = (long) i * i; j <= 1_000_000; j += i) composite[(int) j] = true;
        }
        check("primes below 100", 25, under100);
        check("primes up to 10^6 (sieve)", 78_498, primes);
        check("(a+b) mod m = ((a mod m)+(b mod m)) mod m", (123456 + 987653) % 97, (123456 % 97 + 987653 % 97) % 97);
        check("(a*b) mod m works the same way", (123456L * 987653) % 97, (123456L % 97) * (987653 % 97) % 97);
        check("2^100 mod 1e9+7 (fast power)", BigInteger.TWO.modPow(BigInteger.valueOf(100), BigInteger.valueOf(1_000_000_007)).longValue(), modPow(2, 100, 1_000_000_007));
        long inv = modPow(3, 1_000_000_007 - 2, 1_000_000_007);              // Fermat: a^(p-2) is a^-1 mod prime p
        check("3 * inverse(3) mod p = 1", 1L, 3 * inv % 1_000_000_007);
        long[] eg = extendedGcd(240, 46);
        check("Bezout: 240x + 46y = gcd = 2", 2L, 240 * eg[1] + 46 * eg[2]);
        // toy RSA
        long p = 61, q = 53, n = p * q, phi = (p - 1) * (q - 1), e = 17;
        long d = Math.floorMod(extendedGcd(e, phi)[1], phi);
        check("RSA: n = 61*53", 3233L, n);
        check("RSA: private key d = e^-1 mod phi(n)", 2753L, d);
        long cipher = modPow(65, e, n);
        check("encrypt 65 -> 65^17 mod 3233", 2790L, cipher);
        check("decrypt 2790 -> 2790^2753 mod 3233", 65L, modPow(cipher, d, n));
    }

    // =====================================================================
    // 7. asymptotics
    // =====================================================================

    static void asymptotics() {
        System.out.println("7. Growth rates");
        int[] ns = {10, 100, 1000};
        StringBuilder sb = new StringBuilder();
        for (int n : ns)
            sb.append(String.format("n=%d: log=%.0f nlog=%.0f n^2=%d | ", n, Math.log(n) / Math.log(2), n * Math.log(n) / Math.log(2), (long) n * n));
        check("table (rounded)", "n=10: log=3 nlog=33 n^2=100 | n=100: log=7 nlog=664 n^2=10000 | n=1000: log=10 nlog=9966 n^2=1000000 | ", sb.toString());
        check("2^30 already over a billion", 1_073_741_824L, 1L << 30);
        check("3n^2 + 100n is O(n^2): ratio at n=10^6 near 3", "3.0001", String.format("%.4f", (3.0 * 1e12 + 100 * 1e6) / 1e12));
    }

    public static void main(String[] args) {
        sets();
        functionsAndPigeonhole();
        counting();
        recurrences();
        graphs();
        numberTheory();
        asymptotics();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-46s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
