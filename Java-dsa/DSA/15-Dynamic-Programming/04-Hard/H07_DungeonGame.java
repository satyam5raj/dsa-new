/*
 * ============================================================================
 *  HARD 07 : DUNGEON GAME                    LeetCode #174
 *  https://leetcode.com/problems/dungeon-game/
 *  Pattern: grid DP filled BACKWARDS (from the goal), because the constraint is a
 *           running minimum, not a sum
 * ============================================================================
 *
 *  PROBLEM
 *    The knight starts top-left, moves right/down to the princess at bottom-right.
 *    Each cell adds (or removes, if negative) health; health must stay >= 1 at ALL
 *    times. Minimum starting health?
 *      [[-2,-3,3],[-5,-10,1],[10,30,-5]] -> 7   (right, right, down, down)
 *      [[0]] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  WHY FORWARD DP FAILS: going forward you'd need TWO numbers per cell (current
 *  health AND the lowest point so far), and minimising one can hurt the other -
 *  no single "best" value per cell.
 *
 *  KEY INSIGHT: go BACKWARDS. What matters at a cell is how much health you need when
 *  you ENTER it, and that only depends on the path AHEAD.
 *      STATE       need[r][c] = minimum health when entering (r, c) to survive to the end
 *      TRANSITION  next = min(need[r+1][c], need[r][c+1])     (choose the easier way on)
 *                  need[r][c] = max(1, next - dungeon[r][c])
 *                  (a big potion can't bank "negative need": you still need >= 1)
 *      BASE        beyond the princess: need = 1   (so need[m-1][n-1] = max(1, 1 - d))
 *      ANSWER      need[0][0]
 *
 *  DRY RUN    dungeon:                need table (filled from the bottom-right):
 *       -2  -3   3                    7   5   2
 *       -5 -10   1                    6  11   5
 *       10  30  -5                    1   1   6
 *
 *  COMPLEXITY   Time O(m*n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H07_DungeonGame {

    static int calculateMinimumHP(int[][] d) {
        int m = d.length, n = d[0].length;
        int[] need = new int[n + 1];
        Arrays.fill(need, Integer.MAX_VALUE);            // off-grid: impossible
        need[n - 1] = 1;                                  // "after" the princess you need 1 (seeds the corner)
        for (int r = m - 1; r >= 0; r--)
            for (int c = n - 1; c >= 0; c--) {
                int next = Math.min(need[c], need[c + 1]);          // below (old need[c]) or right (new need[c+1])
                need[c] = Math.max(1, next - d[r][c]);
            }
        return need[0];
    }

    // referee: every path; required start = max(1, 1 - (lowest prefix sum))
    static int brute(int[][] d, int r, int c, int sum, int lowest) {
        sum += d[r][c];
        lowest = Math.min(lowest, sum);
        if (r == d.length - 1 && c == d[0].length - 1) return Math.max(1, 1 - lowest);
        int best = Integer.MAX_VALUE;
        if (r + 1 < d.length) best = Math.min(best, brute(d, r + 1, c, sum, lowest));
        if (c + 1 < d[0].length) best = Math.min(best, brute(d, r, c + 1, sum, lowest));
        return best;
    }

    public static void main(String[] args) {
        check("3x3 example", 7, calculateMinimumHP(new int[][]{{-2, -3, 3}, {-5, -10, 1}, {10, 30, -5}}));
        check("[[0]]", 1, calculateMinimumHP(new int[][]{{0}}));
        check("[[100]] (potion)", 1, calculateMinimumHP(new int[][]{{100}}));
        check("[[-5]]", 6, calculateMinimumHP(new int[][]{{-5}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[][] g = new int[1 + rnd.nextInt(6)][1 + rnd.nextInt(6)];
            for (int[] row : g) for (int c = 0; c < row.length; c++) row[c] = rnd.nextInt(31) - 18;
            ok &= calculateMinimumHP(g) == brute(g, 0, 0, 0, 0);
        }
        check("500 random dungeons vs every path", true, ok);
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
