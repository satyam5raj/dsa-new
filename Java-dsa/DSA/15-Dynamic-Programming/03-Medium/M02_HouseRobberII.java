/*
 * ============================================================================
 *  MEDIUM 02 : HOUSE ROBBER II               LeetCode #213
 *  https://leetcode.com/problems/house-robber-ii/
 *  Pattern: break a CIRCLE into two LINES, reuse the linear DP
 * ============================================================================
 *
 *  PROBLEM
 *    Same as House Robber, but the houses form a CIRCLE: the first and last are
 *    neighbours.
 *      [2,3,2]    -> 3       [1,2,3,1]  -> 4       [1,2,3] -> 3
 *
 *  ---------------------------------------------------------------------------
 *  THE PROBLEM WITH A CIRCLE: the linear DP has no "start". But the only new rule is
 *  "not both house 0 and house n-1". So every valid plan either
 *      skips house n-1  -> it's a valid plan on the LINE 0 .. n-2
 *      skips house 0    -> it's a valid plan on the LINE 1 .. n-1
 *  Answer = max(rob(0..n-2), rob(1..n-1)). Edge case: n == 1 -> nums[0].
 *
 *  This "cut the cycle by fixing one element's choice" trick appears again and again
 *  (circular arrays, circular subarray sums).
 *
 *  DRY RUN [2,3,2]:  line [2,3] -> 3,  line [3,2] -> 3  -> 3
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M02_HouseRobberII {

    static int rob(int[] nums) {
        if (nums.length == 1) return nums[0];
        return Math.max(robLine(nums, 0, nums.length - 2), robLine(nums, 1, nums.length - 1));
    }

    static int robLine(int[] a, int from, int to) {      // M01 on a[from..to]
        int prev2 = 0, prev1 = 0;
        for (int i = from; i <= to; i++) {
            int cur = Math.max(prev1, prev2 + a[i]);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static int brute(int[] a) {
        int n = a.length, best = 0;
        for (int mask = 0; mask < 1 << n; mask++) {
            boolean ok = true;
            for (int i = 0; i < n && ok; i++)
                if ((mask >> i & 1) == 1 && (mask >> ((i + 1) % n) & 1) == 1 && n > 1) ok = false;   // circular neighbour
            if (!ok) continue;
            int s = 0;
            for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) s += a[i];
            best = Math.max(best, s);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[2,3,2]", 3, rob(new int[]{2, 3, 2}));
        check("[1,2,3,1]", 4, rob(new int[]{1, 2, 3, 1}));
        check("[1,2,3]", 3, rob(new int[]{1, 2, 3}));
        check("[7]", 7, rob(new int[]{7}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = new int[1 + rnd.nextInt(14)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(50);
            ok &= rob(a) == brute(a);
        }
        check("500 random vs every circular subset", true, ok);
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
