/*
 * ============================================================================
 *  HARD 03 : RUSSIAN DOLL ENVELOPES          LeetCode #354
 *  https://leetcode.com/problems/russian-doll-envelopes/
 *  Pattern: 2-D LIS -> sort cleverly, then 1-D LIS in O(n log n)
 * ============================================================================
 *
 *  PROBLEM
 *    Envelope (w, h) fits into another if BOTH width and height are strictly larger.
 *    Max number of nested envelopes?
 *      [[5,4],[6,4],[6,7],[2,3]] -> 3   ([2,3] -> [5,4] -> [6,7])
 *      [[1,1],[1,1],[1,1]]       -> 1
 *
 *  ---------------------------------------------------------------------------
 *  O(n^2) DP: sort by width, then LIS where j -> i needs w[j] < w[i] AND h[j] < h[i].
 *  n is up to 10^5 on LeetCode -> too slow.
 *
 *  KEY INSIGHT: sort by width ASCENDING, and for EQUAL widths by height DESCENDING.
 *  Then run a plain strictly-increasing LIS on the heights only.
 *    - width ascending: any increasing-height subsequence also has non-decreasing width
 *    - equal widths sorted by height DESCENDING can never both be in a strictly
 *      increasing height sequence -> two envelopes with the same width are never
 *      chained. That's exactly the "strictly larger width" rule.
 *  Without the descending tie-break, [3,3],[3,4] would wrongly chain.
 *
 *  DRY RUN [[5,4],[6,4],[6,7],[2,3]]
 *    sorted: [2,3],[5,4],[6,7],[6,4]  heights 3,4,7,4
 *    tails: [3] [3,4] [3,4,7] [3,4,7] (4 replaces 4)  -> 3
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H03_RussianDollEnvelopes {

    static int maxEnvelopes(int[][] envelopes) {
        int[][] e = envelopes.clone();
        Arrays.sort(e, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
        int[] tails = new int[e.length];
        int len = 0;
        for (int[] env : e) {                             // strictly increasing LIS on heights
            int h = env[1], lo = 0, hi = len;
            while (lo < hi) { int mid = (lo + hi) >>> 1; if (tails[mid] < h) lo = mid + 1; else hi = mid; }
            tails[lo] = h;
            if (lo == len) len++;
        }
        return len;
    }

    static int quadratic(int[][] env) {                  // O(n^2) DP with both conditions
        int[][] e = env.clone();
        Arrays.sort(e, (a, b) -> a[0] - b[0]);
        int[] dp = new int[e.length];
        int best = 0;
        for (int i = 0; i < e.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) if (e[j][0] < e[i][0] && e[j][1] < e[i][1]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    static int wrongTieBreak(int[][] env) {              // heights ASCENDING for equal widths: the bug
        int[][] e = env.clone();
        Arrays.sort(e, (a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
        int[] tails = new int[e.length];
        int len = 0;
        for (int[] x : e) { int lo = 0, hi = len; while (lo < hi) { int mid = (lo + hi) >>> 1; if (tails[mid] < x[1]) lo = mid + 1; else hi = mid; } tails[lo] = x[1]; if (lo == len) len++; }
        return len;
    }

    public static void main(String[] args) {
        check("example 1", 3, maxEnvelopes(new int[][]{{5, 4}, {6, 4}, {6, 7}, {2, 3}}));
        check("all equal", 1, maxEnvelopes(new int[][]{{1, 1}, {1, 1}, {1, 1}}));
        check("[3,3],[3,4] same width", 1, maxEnvelopes(new int[][]{{3, 3}, {3, 4}}));
        check("ascending tie-break (bug)", 2, wrongTieBreak(new int[][]{{3, 3}, {3, 4}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[][] e = new int[1 + rnd.nextInt(15)][];
            for (int i = 0; i < e.length; i++) e[i] = new int[]{1 + rnd.nextInt(6), 1 + rnd.nextInt(6)};
            ok &= maxEnvelopes(e) == quadratic(e);
        }
        check("1000 random vs O(n^2) DP", true, ok);
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
