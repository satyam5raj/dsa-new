/*
 * ============================================================================
 *  MEDIUM 06 : COMBINATION SUM               LeetCode #39
 *  https://leetcode.com/problems/combination-sum/
 *  Pattern: combinations with UNLIMITED reuse - recurse with `i` (not i+1) + break on overshoot
 * ============================================================================
 *
 *  PROBLEM
 *    Distinct positive candidates; each may be used ANY number of times. Return all
 *    unique combinations summing to target.
 *      [2,3,6,7], 7 -> [[2,2,3],[7]]
 *
 *  ---------------------------------------------------------------------------
 *  Combinations template with two tweaks:
 *    1. REUSE: after picking candidates[i], recurse with start = i (not i + 1), so it
 *       can be picked again. Still never going BACK to smaller indices prevents
 *       [3,2,2] after [2,2,3].
 *    2. BOUND PRUNING: sort candidates; once candidates[i] > remaining, BREAK - all
 *       later ones are bigger too.
 *    bt(start, remaining):
 *      remaining == 0 -> record
 *      for i in start..: if c[i] > remaining: break
 *                        add c[i]; bt(i, remaining - c[i]); remove
 *
 *  DRY RUN [2,3,6,7], 7
 *    2 -> 2 -> 2 -> (rem 1: 2 > 1 break) ; 2,2,3 -> rem 0 record
 *    2,3 -> rem 2: 3 > 2 break ... ; 3 -> 3 -> rem 1 break ; 6 -> rem 1 break ; 7 -> record
 *
 *  (Counting the combinations instead of listing them is Coin Change II - a DP
 *   problem in topic 15.)
 *
 *  COMPLEXITY   exponential: O(N^(T/min + 1)) worst case; Space O(T/min) depth
 * ============================================================================
 */

import java.util.*;

public class M06_CombinationSum {

    static List<List<Integer>> combinationSum(int[] candidates, int target) {
        int[] c = candidates.clone();
        Arrays.sort(c);
        List<List<Integer>> out = new ArrayList<>();
        bt(c, 0, target, new ArrayList<>(), out);
        return out;
    }

    static void bt(int[] c, int start, int remaining, List<Integer> path, List<List<Integer>> out) {
        if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
        for (int i = start; i < c.length; i++) {
            if (c[i] > remaining) break;                       // sorted: nothing later fits either
            path.add(c[i]);
            bt(c, i, remaining - c[i], path, out);             // i, not i + 1: reuse allowed
            path.remove(path.size() - 1);
        }
    }

    // referee: count with DP (Coin Change II); the number of lists must match
    static long countWays(int[] c, int target) {
        long[] ways = new long[target + 1];
        ways[0] = 1;
        for (int coin : c) for (int s = coin; s <= target; s++) ways[s] += ways[s - coin];
        return ways[target];
    }

    public static void main(String[] args) {
        check("[2,3,6,7] 7", "[[2, 2, 3], [7]]", combinationSum(new int[]{2, 3, 6, 7}, 7));
        check("[2,3,5] 8", "[[2, 2, 2, 2], [2, 3, 3], [3, 5]]", combinationSum(new int[]{2, 3, 5}, 8));
        check("[2] 1", "[]", combinationSum(new int[]{2}, 1));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] c = rnd.ints(1 + rnd.nextInt(5), 2, 12).distinct().toArray();
            int target = 1 + rnd.nextInt(30);
            List<List<Integer>> got = combinationSum(c, target);
            ok &= got.size() == countWays(c, target) && new HashSet<>(got).size() == got.size();
            for (List<Integer> l : got) ok &= l.stream().mapToInt(Integer::intValue).sum() == target;
        }
        check("300 random cases: count == DP count, sums correct", true, ok);
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
