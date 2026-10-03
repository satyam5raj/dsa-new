/*
 * ============================================================================
 *  MEDIUM 14 : MATCHSTICKS TO SQUARE         LeetCode #473
 *  https://leetcode.com/problems/matchsticks-to-square/
 *  Pattern: M12 with k = 4 - four SIDES as buckets, longest sticks first
 * ============================================================================
 *
 *  PROBLEM
 *    Use every matchstick exactly once (no breaking) to form a square.
 *      [1,1,2,2,2] -> true   (sides 2, 2, 1+1, 2)
 *      [3,3,3,3,4] -> false
 *
 *  ---------------------------------------------------------------------------
 *  A square = 4 sides of equal length = Partition to K Equal Sum Subsets with k = 4.
 *  Same checks and pruning:
 *    - total % 4 != 0, or a stick longer than total / 4 -> false
 *    - sort DESCENDING (long sticks have the fewest places to go)
 *    - try each stick on each side; skip a side whose current length equals a side
 *      already tried for this stick (symmetry)
 *
 *  COMPLEXITY   O(4^n) worst case, heavily pruned   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M14_MatchsticksToSquare {

    static boolean makesquare(int[] matchsticks) {
        int total = Arrays.stream(matchsticks).sum();
        if (total % 4 != 0) return false;
        int side = total / 4;
        int[] a = matchsticks.clone();
        Arrays.sort(a);
        for (int i = 0, j = a.length - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }   // descending
        if (a[0] > side) return false;
        return place(a, 0, new int[4], side);
    }

    static boolean place(int[] a, int i, int[] sides, int target) {
        if (i == a.length) return true;
        for (int s = 0; s < 4; s++) {
            if (sides[s] + a[i] > target) continue;
            boolean seen = false;
            for (int p = 0; p < s; p++) if (sides[p] == sides[s]) { seen = true; break; }
            if (seen) continue;                                   // symmetric to an earlier side
            sides[s] += a[i];
            if (place(a, i + 1, sides, target)) return true;
            sides[s] -= a[i];
        }
        return false;
    }

    // referee: try all 4^n assignments of sticks to sides (small n)
    static boolean brute(int[] a) {
        int n = a.length, total = 1;
        for (int i = 0; i < n; i++) total *= 4;
        for (int code = 0; code < total; code++) {
            int[] s = new int[4];
            int c = code;
            for (int i = 0; i < n; i++) { s[c % 4] += a[i]; c /= 4; }
            if (s[0] == s[1] && s[1] == s[2] && s[2] == s[3] && s[0] > 0) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        check("[1,1,2,2,2]", true, makesquare(new int[]{1, 1, 2, 2, 2}));
        check("[3,3,3,3,4]", false, makesquare(new int[]{3, 3, 3, 3, 4}));
        check("[5,5,5,5,4,4,4,4,3,3,3,3]", true, makesquare(new int[]{5, 5, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(4 + rnd.nextInt(5), 1, 7).toArray();
            ok &= makesquare(a) == brute(a);
        }
        check("1,000 random stick sets vs all 4^n assignments", true, ok);
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
