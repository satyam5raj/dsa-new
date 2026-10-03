/*
 * ============================================================================
 *  MEDIUM 12 : TARGET SUM                    LeetCode #494
 *  https://leetcode.com/problems/target-sum/
 *  Pattern: algebra turns +/- signs into 0/1 knapsack COUNTING
 * ============================================================================
 *
 *  PROBLEM
 *    Put '+' or '-' before every number. How many sign assignments give target?
 *      [1,1,1,1,1], 3 -> 5        [1], 1 -> 1
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: 2^n sign choices.
 *  MEMO: (index, running sum) -> O(n * sum range). Works, but there's a neater way.
 *
 *  KEY INSIGHT (the algebra): let P = numbers with '+', N = numbers with '-'.
 *      sum(P) - sum(N) = target
 *      sum(P) + sum(N) = total
 *      ->  sum(P) = (total + target) / 2
 *  So: count SUBSETS with sum (total + target) / 2. If total + target is odd or
 *  |target| > total -> 0.
 *
 *  SUBSET COUNT (0/1 knapsack counting):
 *      ways[0] = 1;  for x: for s = goal DOWN TO x:  ways[s] += ways[s - x]
 *  Zeros are handled naturally: each 0 doubles the count (+0 and -0 are different
 *  assignments), and ways[s] += ways[s - 0] doubles every entry.
 *
 *  DRY RUN [1,1,1,1,1], 3:  goal = (5 + 3) / 2 = 4 -> choose 4 of 5 ones = 5
 *
 *  COMPLEXITY   Time O(n * goal)   Space O(goal)
 * ============================================================================
 */

import java.util.*;

public class M12_TargetSum {

    static int findTargetSumWays(int[] nums, int target) {
        int total = 0;
        for (int x : nums) total += x;
        if (Math.abs(target) > total || (total + target) % 2 != 0) return 0;
        int goal = (total + target) / 2;
        int[] ways = new int[goal + 1];
        ways[0] = 1;
        for (int x : nums)
            for (int s = goal; s >= x; s--) ways[s] += ways[s - x];    // backwards: 0/1
        return ways[goal];
    }

    static int brute(int[] a, int i, int sum, int target) {
        if (i == a.length) return sum == target ? 1 : 0;
        return brute(a, i + 1, sum + a[i], target) + brute(a, i + 1, sum - a[i], target);
    }

    public static void main(String[] args) {
        check("[1,1,1,1,1], 3", 5, findTargetSumWays(new int[]{1, 1, 1, 1, 1}, 3));
        check("[1], 1", 1, findTargetSumWays(new int[]{1}, 1));
        check("[0,0,1], 1 (zeros double)", 4, findTargetSumWays(new int[]{0, 0, 1}, 1));
        check("[1], 2 (impossible)", 0, findTargetSumWays(new int[]{1}, 2));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = new int[1 + rnd.nextInt(12)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(6);
            int target = rnd.nextInt(21) - 10;
            ok &= findTargetSumWays(a, target) == brute(a, 0, 0, target);
        }
        check("1000 random vs all 2^n signs", true, ok);
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
