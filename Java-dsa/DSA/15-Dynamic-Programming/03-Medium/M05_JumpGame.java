/*
 * ============================================================================
 *  MEDIUM 05 : JUMP GAME                     LeetCode #55
 *  https://leetcode.com/problems/jump-game/
 *  Pattern: DP that COLLAPSES into greedy (track the farthest reachable index)
 * ============================================================================
 *
 *  PROBLEM
 *    nums[i] = maximum jump length from index i. Starting at 0, can you reach the last
 *    index?
 *      [2,3,1,1,4] -> true        [3,2,1,0,4] -> false (everything lands on the 0)
 *
 *  ---------------------------------------------------------------------------
 *  DP VERSION (O(n^2)):
 *      STATE       good[i] = can the last index be reached starting from i?
 *      TRANSITION  good[i] = any good[j] for j in i+1 .. i+nums[i]
 *      BASE        good[n-1] = true;    fill right to left;   ANSWER good[0]
 *
 *  DP -> GREEDY: the reachable set from the start is always a PREFIX 0..far
 *  (if you can reach j you can reach every index before it). So one number is enough:
 *      far = max over reachable i of (i + nums[i])
 *  Scan left to right; if i > far you're stuck -> false.
 *  Many DP problems have this shape: once the state collapses to "a single best
 *  value", the DP IS a greedy.
 *
 *  DRY RUN [3,2,1,0,4]:  i0 far=3, i1 far=3, i2 far=3, i3 far=3, i4 > far -> false
 *
 *  COMPLEXITY   Greedy: Time O(n)  Space O(1)      DP: O(n^2) / O(n)
 * ============================================================================
 */

import java.util.*;

public class M05_JumpGame {

    static boolean canJump(int[] nums) {
        int far = 0;
        for (int i = 0; i < nums.length; i++) {
            if (i > far) return false;                    // index i can't be reached
            far = Math.max(far, i + nums[i]);
            if (far >= nums.length - 1) return true;
        }
        return true;
    }

    static boolean canJumpDP(int[] nums) {
        int n = nums.length;
        boolean[] good = new boolean[n];
        good[n - 1] = true;
        for (int i = n - 2; i >= 0; i--)
            for (int j = i + 1; j <= Math.min(n - 1, i + nums[i]) && !good[i]; j++) good[i] = good[j];
        return good[0];
    }

    public static void main(String[] args) {
        check("[2,3,1,1,4]", true, canJump(new int[]{2, 3, 1, 1, 4}));
        check("[3,2,1,0,4]", false, canJump(new int[]{3, 2, 1, 0, 4}));
        check("[0]", true, canJump(new int[]{0}));
        check("[0,1]", false, canJump(new int[]{0, 1}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int[] a = new int[1 + rnd.nextInt(15)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(4);
            ok &= canJump(a) == canJumpDP(a);
        }
        check("2000 random: greedy == DP", true, ok);
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
