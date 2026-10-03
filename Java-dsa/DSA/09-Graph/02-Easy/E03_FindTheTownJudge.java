/*
 * ============================================================================
 *  EASY 03 : FIND THE TOWN JUDGE  ⭐ bonus     LeetCode #997
 *  https://leetcode.com/problems/find-the-town-judge/
 *  Pattern: IN-DEGREE and OUT-DEGREE counting (no traversal needed)
 * ============================================================================
 *
 *  PROBLEM
 *    People 1..n. trust[i] = [a, b] means a trusts b. The judge trusts NOBODY and
 *    is trusted by EVERYBODY else (n - 1 people). Return the judge or -1.
 *      n=3, [[1,3],[2,3]] -> 3          n=3, [[1,3],[2,3],[3,1]] -> -1
 *
 *  ---------------------------------------------------------------------------
 *  As a directed graph (a -> b = "a trusts b"):
 *     judge: out-degree 0  AND  in-degree n - 1
 *  One counter is enough: score = in - out. Only the judge can reach n - 1
 *  (n - 1 is the maximum possible in-degree, and any out-edge lowers it).
 *
 *  Many graph questions are just DEGREE questions: the center of a star graph
 *  (E04), sources/sinks in a DAG (Kahn's algorithm), Euler paths.
 *
 *  DRY RUN n=3, [[1,3],[2,3]]
 *    score: 1 -> -1, 2 -> -1, 3 -> +2 = n - 1 -> judge 3
 *
 *  COMPLEXITY   Time O(n + trust)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E03_FindTheTownJudge {

    static int findJudge(int n, int[][] trust) {
        int[] score = new int[n + 1];
        for (int[] t : trust) {
            score[t[0]]--;                 // out-edge: trusting someone disqualifies you
            score[t[1]]++;                 // in-edge
        }
        for (int p = 1; p <= n; p++) if (score[p] == n - 1) return p;
        return -1;
    }

    static int brute(int n, int[][] trust) {
        for (int p = 1; p <= n; p++) {
            boolean trustsNobody = true;
            Set<Integer> trustedBy = new HashSet<>();
            for (int[] t : trust) {
                if (t[0] == p) trustsNobody = false;
                if (t[1] == p) trustedBy.add(t[0]);
            }
            if (trustsNobody && trustedBy.size() == n - 1) return p;
        }
        return -1;
    }

    public static void main(String[] args) {
        check("n=2 [[1,2]]", 2, findJudge(2, new int[][]{{1, 2}}));
        check("n=3 [[1,3],[2,3]]", 3, findJudge(3, new int[][]{{1, 3}, {2, 3}}));
        check("n=3 judge trusts someone", -1, findJudge(3, new int[][]{{1, 3}, {2, 3}, {3, 1}}));
        check("n=1 no trust (alone = judge)", 1, findJudge(1, new int[][]{}));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(6);
            Set<String> pairs = new LinkedHashSet<>();
            int judge = 1 + rnd.nextInt(n);
            if (rnd.nextBoolean()) for (int p = 1; p <= n; p++) if (p != judge) pairs.add(p + "," + judge);   // plant a judge
            for (int i = 0; i < rnd.nextInt(5); i++) { int a = 1 + rnd.nextInt(n), b = 1 + rnd.nextInt(n); if (a != b) pairs.add(a + "," + b); }
            int[][] tr = pairs.stream().map(s -> Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
            ok &= findJudge(n, tr) == brute(n, tr);
        }
        check("1,000 random towns vs brute force", true, ok);
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
