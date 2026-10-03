/*
 * ============================================================================
 *  DP PATTERNS: the 8 shapes almost every DP problem takes
 * ============================================================================
 *
 *  Recognising the SHAPE gives you the state for free. Each pattern below has a tiny
 *  working example, checked against brute force.
 *
 *  1. LINEAR (1-D)          dp[i] from a few earlier dp[j]
 *       "best / count up to index i"   House Robber: dp[i] = max(dp[i-1], dp[i-2] + a[i])
 *  2. GRID                  dp[r][c] from dp[r-1][c] and dp[r][c-1]
 *       Unique Paths, Min Path Sum, Dungeon Game (filled backwards)
 *  3. TWO SEQUENCES         dp[i][j] = answer for prefixes a[0..i), b[0..j)
 *       LCS, Edit Distance, Distinct Subsequences, regex / wildcard matching
 *       match -> dp[i-1][j-1] (+1);  otherwise combine dp[i-1][j] and dp[i][j-1]
 *  4. LIS FAMILY            dp[i] = best subsequence ENDING at i: O(n^2) by scanning j < i,
 *       or O(n log n) with "tails" + binary search. Russian Doll, Largest Divisible Subset
 *  5. KNAPSACK              dp[capacity], items outer (see KnapsackPatterns.java)
 *  6. INTERVAL              dp[i][j] = answer for the range i..j, built from SHORTER
 *       ranges: try every split k. Fill by increasing LENGTH.
 *       Burst Balloons, Min Cost Tree From Leaf Values, palindromes, matrix chain
 *  7. STATE MACHINE         several dp arrays, one per "mode", e.g. holding / not holding a
 *       stock / cooling down. Draw the states and arrows, then each arrow is one term.
 *  8. BITMASK               dp[mask][i] = best over the SUBSET mask of items, ending at i.
 *       For n <= ~20: travelling salesman, assignment problems. O(2^n * n^2)
 *  (also: DP ON TREES -> Trees M13 House Robber III; DIGIT DP -> Recursion H03)
 *
 *  HOW TO SPOT DP: "count the ways", "min / max", "is it possible", over choices
 *  that build on each other, where a greedy choice can fail. If the brute force
 *  recursion's arguments repeat, memoize them.
 *
 *  Run:  java DPPatterns.java
 * ============================================================================
 */

import java.util.*;

public class DPPatterns {

    // 3. TWO SEQUENCES: LCS length
    static int lcs(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];          // dp[i][j]: LCS of a[0..i), b[0..j)
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[a.length()][b.length()];
    }

    static int lcsBrute(String a, String b) {                          // every subsequence of a, is it in b?
        int best = 0;
        for (int mask = 0; mask < 1 << a.length(); mask++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length(); i++) if ((mask >> i & 1) == 1) sb.append(a.charAt(i));
            if (isSubseq(sb.toString(), b)) best = Math.max(best, sb.length());
        }
        return best;
    }

    static boolean isSubseq(String s, String t) {
        int i = 0;
        for (int j = 0; j < t.length() && i < s.length(); j++) if (s.charAt(i) == t.charAt(j)) i++;
        return i == s.length();
    }

    // 4. LIS: O(n^2) and O(n log n)
    static int lisQuadratic(int[] a) {
        int[] dp = new int[a.length];                                   // dp[i]: longest increasing subsequence ENDING at i
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    static int lisFast(int[] a) {
        int[] tails = new int[a.length];                                // tails[k]: smallest tail of an increasing subseq of length k+1
        int len = 0;
        for (int x : a) {
            int lo = 0, hi = len;
            while (lo < hi) { int mid = (lo + hi) >>> 1; if (tails[mid] < x) lo = mid + 1; else hi = mid; }
            tails[lo] = x;
            if (lo == len) len++;
        }
        return len;
    }

    // 6. INTERVAL: matrix chain multiplication. dims[i-1] x dims[i] is matrix i.
    static int matrixChain(int[] dims) {
        int n = dims.length - 1;
        int[][] dp = new int[n][n];                                     // dp[i][j]: min cost to multiply matrices i..j
        for (int len = 2; len <= n; len++)                              // SHORTER intervals first
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;
                for (int k = i; k < j; k++)                             // last multiplication splits at k
                    dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k + 1][j] + dims[i] * dims[k + 1] * dims[j + 1]);
            }
        return dp[0][n - 1];
    }

    static int matrixChainBrute(int[] dims, int i, int j) {           // plain recursion over every split
        if (i == j) return 0;
        int best = Integer.MAX_VALUE;
        for (int k = i; k < j; k++)
            best = Math.min(best, matrixChainBrute(dims, i, k) + matrixChainBrute(dims, k + 1, j) + dims[i] * dims[k + 1] * dims[j + 1]);
        return best;
    }

    // 7. STATE MACHINE: stock with at most ONE share held, unlimited trades, fee per sale
    //    hold = best profit while holding a share; free = best profit with no share
    static int stockStates(int[] prices, int fee) {
        int hold = Integer.MIN_VALUE / 2, free = 0;
        for (int p : prices) {
            int newHold = Math.max(hold, free - p);                     // keep holding, or buy today
            int newFree = Math.max(free, hold + p - fee);               // keep waiting, or sell today
            hold = newHold; free = newFree;
        }
        return free;
    }

    static int stockBrute(int[] prices, int fee, int day, boolean holding) {
        if (day == prices.length) return 0;
        int skip = stockBrute(prices, fee, day + 1, holding);
        int act = holding ? prices[day] - fee + stockBrute(prices, fee, day + 1, false)
                          : -prices[day] + stockBrute(prices, fee, day + 1, true);
        return Math.max(skip, act);
    }

    // 8. BITMASK: travelling salesman - shortest tour from city 0 visiting all, back to 0
    static int tsp(int[][] d) {
        int n = d.length, INF = Integer.MAX_VALUE / 2;
        int[][] dp = new int[1 << n][n];                                // dp[mask][i]: shortest path from 0 visiting mask, ending at i
        for (int[] row : dp) Arrays.fill(row, INF);
        dp[1][0] = 0;
        for (int mask = 1; mask < 1 << n; mask++)
            for (int i = 0; i < n; i++) {
                if (dp[mask][i] == INF) continue;
                for (int j = 0; j < n; j++) {
                    if ((mask >> j & 1) == 1) continue;                 // j already visited
                    int next = mask | 1 << j;
                    dp[next][j] = Math.min(dp[next][j], dp[mask][i] + d[i][j]);
                }
            }
        int best = INF;
        for (int i = 1; i < n; i++) best = Math.min(best, dp[(1 << n) - 1][i] + d[i][0]);
        return n == 1 ? 0 : best;
    }

    static int tspBrute(int[][] d, int at, boolean[] seen, int count) {   // try every order: (n-1)!
        if (count == d.length) return d[at][0];
        int best = Integer.MAX_VALUE;
        for (int j = 1; j < d.length; j++)
            if (!seen[j]) { seen[j] = true; best = Math.min(best, d[at][j] + tspBrute(d, j, seen, count + 1)); seen[j] = false; }
        return best;
    }

    public static void main(String[] args) {
        Random rnd = new Random(1);

        System.out.println("3. Two sequences: LCS");
        check("abcde / ace", 3, lcs("abcde", "ace"));
        boolean ok = true;
        for (int t = 0; t < 300; t++) { String a = rs(rnd, 8), b = rs(rnd, 8); ok &= lcs(a, b) == lcsBrute(a, b); }
        check("300 random vs all subsequences", true, ok);

        System.out.println("4. LIS");
        check("[10,9,2,5,3,7,101,18]", 4, lisQuadratic(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));
        ok = true;
        for (int t = 0; t < 500; t++) { int[] a = ra(rnd, 15, 20); ok &= lisQuadratic(a) == lisFast(a); }
        check("500 random: O(n^2) == O(n log n)", true, ok);

        System.out.println("6. Interval: matrix chain");
        check("10x30, 30x5, 5x60", 4500, matrixChain(new int[]{10, 30, 5, 60}));
        ok = true;
        for (int t = 0; t < 200; t++) { int[] d = ra(rnd, 2 + rnd.nextInt(7), 30); for (int i = 0; i < d.length; i++) d[i]++; ok &= matrixChain(d) == matrixChainBrute(d, 0, d.length - 2); }
        check("200 random vs plain recursion", true, ok);

        System.out.println("7. State machine: stock with fee");
        check("[1,3,2,8,4,9], fee 2", 8, stockStates(new int[]{1, 3, 2, 8, 4, 9}, 2));
        ok = true;
        for (int t = 0; t < 300; t++) { int[] p = ra(rnd, 1 + rnd.nextInt(12), 20); int fee = rnd.nextInt(4); ok &= stockStates(p, fee) == stockBrute(p, fee, 0, false); }
        check("300 random vs buy/sell recursion", true, ok);

        System.out.println("8. Bitmask: travelling salesman");
        int[][] d4 = {{0, 10, 15, 20}, {10, 0, 35, 25}, {15, 35, 0, 30}, {20, 25, 30, 0}};
        check("classic 4-city example", 80, tsp(d4));
        ok = true;
        for (int t = 0; t < 100; t++) {
            int n = 2 + rnd.nextInt(7);
            int[][] d = new int[n][n];
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (i != j) d[i][j] = 1 + rnd.nextInt(50);
            boolean[] seen = new boolean[n];
            seen[0] = true;
            ok &= tsp(d) == tspBrute(d, 0, seen, 1);
        }
        check("100 random vs every order", true, ok);
        summary();
    }

    static String rs(Random r, int maxLen) { StringBuilder sb = new StringBuilder(); for (int i = r.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + r.nextInt(3))); return sb.toString(); }
    static int[] ra(Random r, int n, int bound) { int[] a = new int[n]; for (int i = 0; i < n; i++) a[i] = r.nextInt(bound); return a; }

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
