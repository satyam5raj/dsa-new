/*
 * ============================================================================
 *  MEDIUM 06 : JUMP GAME II                  LeetCode #45
 *  https://leetcode.com/problems/jump-game-ii/
 *  Pattern: min-steps DP -> implicit BFS by "levels" (greedy windows)
 * ============================================================================
 *
 *  PROBLEM
 *    Same jumps as Jump Game; the end is guaranteed reachable. Minimum number of jumps?
 *      [2,3,1,1,4] -> 2  (0 -> 1 -> 4)        [2,3,0,1,4] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  DP VERSION (O(n^2)):
 *      STATE       dp[i] = min jumps to reach i
 *      TRANSITION  dp[j] = min(dp[j], dp[i] + 1) for j in i+1 .. i+nums[i]
 *
 *  KEY INSIGHT -> BFS LEVELS: the indices reachable with exactly k jumps form a
 *  contiguous WINDOW [start, end]. The next window runs from end+1 to the farthest
 *  i + nums[i] over the current window. Count windows until one covers n-1.
 *      jumps = 0, end = 0, far = 0
 *      for i in 0 .. n-2:
 *          far = max(far, i + nums[i])
 *          if i == end: jumps++, end = far        (finished this level, jump)
 *  It's BFS on an implicit graph where each level is an interval, in O(n).
 *
 *  DRY RUN [2,3,1,1,4]
 *    i0: far=2, i==end(0) -> jumps=1, end=2
 *    i1: far=4   i2: far=4, i==end -> jumps=2, end=4   (covers index 4) -> 2
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  Related: Jump Game VI (sliding-window max) -> Queue M06; Jump Game IV (BFS) -> Graph H08.
 * ============================================================================
 */

import java.util.*;

public class M06_JumpGameII {

    static int jump(int[] nums) {
        int jumps = 0, end = 0, far = 0;
        for (int i = 0; i < nums.length - 1; i++) {
            far = Math.max(far, i + nums[i]);
            if (i == end) {                               // the current level is exhausted -> take a jump
                jumps++;
                end = far;
            }
        }
        return jumps;
    }

    static int jumpDP(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;
        for (int i = 0; i < n; i++) {
            if (dp[i] == Integer.MAX_VALUE) continue;
            for (int j = i + 1; j <= Math.min(n - 1, i + nums[i]); j++) dp[j] = Math.min(dp[j], dp[i] + 1);
        }
        return dp[n - 1];
    }

    public static void main(String[] args) {
        check("[2,3,1,1,4]", 2, jump(new int[]{2, 3, 1, 1, 4}));
        check("[2,3,0,1,4]", 2, jump(new int[]{2, 3, 0, 1, 4}));
        check("[0]", 0, jump(new int[]{0}));
        check("[1,1,1,1]", 3, jump(new int[]{1, 1, 1, 1}));
        Random rnd = new Random(1);
        boolean ok = true;
        int tested = 0;
        while (tested < 1000) {
            int[] a = new int[1 + rnd.nextInt(15)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(5);
            if (jumpDP(a) == Integer.MAX_VALUE) continue;   // the problem guarantees reachability
            ok &= jump(a) == jumpDP(a);
            tested++;
        }
        check("1000 random reachable: BFS levels == DP", true, ok);
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
