/*
 * ============================================================================
 *  MEDIUM 07 : ROTTING ORANGES               LeetCode #994
 *  https://leetcode.com/problems/rotting-oranges/
 *  Pattern: MULTI-SOURCE BFS - all rotten oranges start in the queue at minute 0
 * ============================================================================
 *
 *  PROBLEM
 *    0 empty, 1 fresh, 2 rotten. Every minute, fresh oranges next to a rotten one
 *    (4 directions) rot. Minutes until no fresh orange remains, or -1 if impossible.
 *      [[2,1,1],[1,1,0],[0,1,1]] -> 4
 *      [[2,1,1],[0,1,1],[1,0,1]] -> -1  (the bottom-left 1 is cut off)
 *
 *  ---------------------------------------------------------------------------
 *  WRONG IDEA: BFS from each rotten orange separately and combine - messy and slow.
 *
 *  MULTI-SOURCE BFS: rot spreads from ALL rotten oranges SIMULTANEOUSLY, one ring
 *  per minute. That's exactly BFS where the queue STARTS with every source:
 *    queue = all rotten cells; fresh = count of 1s
 *    minutes = 0
 *    while queue not empty and fresh > 0:
 *       process one LEVEL (size snapshot) = one minute:
 *         rot each fresh neighbour, fresh--, enqueue it
 *       minutes++
 *    return fresh == 0 ? minutes : -1
 *  Each level of the BFS is one minute (the same "level-size snapshot" as tree
 *  level order traversal).
 *
 *  (Covered as a pattern in 05-Queue/01-Basics/QueuePatterns; this is the
 *   LeetCode problem in full.)
 *
 *  DRY RUN [[2,1,1],[1,1,0],[0,1,1]]
 *    minute 1: (0,1), (1,0)    minute 2: (0,2), (1,1)
 *    minute 3: (2,1)           minute 4: (2,2)    -> fresh 0 -> 4
 *
 *  COMPLEXITY   Time O(R * C)   Space O(R * C)
 * ============================================================================
 */

import java.util.*;

public class M07_RottingOranges {

    static int orangesRotting(int[][] grid) {
        int R = grid.length, C = grid[0].length, fresh = 0;
        Queue<int[]> q = new ArrayDeque<>();
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                if (grid[r][c] == 2) q.add(new int[]{r, c});      // ALL sources start together
                else if (grid[r][c] == 1) fresh++;
            }
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int minutes = 0;
        while (!q.isEmpty() && fresh > 0) {
            for (int i = q.size(); i > 0; i--) {                 // one BFS level = one minute
                int[] p = q.poll();
                for (int[] d : dirs) {
                    int r = p[0] + d[0], c = p[1] + d[1];
                    if (r >= 0 && r < R && c >= 0 && c < C && grid[r][c] == 1) {
                        grid[r][c] = 2;
                        fresh--;
                        q.add(new int[]{r, c});
                    }
                }
            }
            minutes++;
        }
        return fresh == 0 ? minutes : -1;
    }

    // brute force: simulate minute by minute on a copy
    static int simulate(int[][] g) {
        int R = g.length, C = g[0].length;
        for (int minute = 0; ; minute++) {
            boolean anyFresh = false;
            List<int[]> toRot = new ArrayList<>();
            for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) {
                if (g[r][c] != 1) continue;
                anyFresh = true;
                if ((r > 0 && g[r - 1][c] == 2) || (r < R - 1 && g[r + 1][c] == 2) || (c > 0 && g[r][c - 1] == 2) || (c < C - 1 && g[r][c + 1] == 2)) toRot.add(new int[]{r, c});
            }
            if (!anyFresh) return minute;
            if (toRot.isEmpty()) return -1;
            for (int[] p : toRot) g[p[0]][p[1]] = 2;
        }
    }

    public static void main(String[] args) {
        check("example 1", 4, orangesRotting(new int[][]{{2, 1, 1}, {1, 1, 0}, {0, 1, 1}}));
        check("unreachable orange", -1, orangesRotting(new int[][]{{2, 1, 1}, {0, 1, 1}, {1, 0, 1}}));
        check("[[0,2]] no fresh at all", 0, orangesRotting(new int[][]{{0, 2}}));
        check("[[1]] fresh, no rotten", -1, orangesRotting(new int[][]{{1}}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int R = 1 + rnd.nextInt(6), C = 1 + rnd.nextInt(6);
            int[][] a = new int[R][C];
            for (int[] row : a) for (int c = 0; c < C; c++) row[c] = rnd.nextInt(10) < 1 ? 2 : rnd.nextInt(10) < 7 ? 1 : 0;
            int[][] b = Arrays.stream(a).map(int[]::clone).toArray(int[][]::new);
            ok &= orangesRotting(a) == simulate(b);
        }
        check("1,000 random grids vs minute-by-minute", true, ok);
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
