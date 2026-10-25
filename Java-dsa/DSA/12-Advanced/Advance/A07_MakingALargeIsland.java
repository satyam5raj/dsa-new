/*
 * ============================================================================
 *  ADVANCE 07 : MAKING A LARGE ISLAND  ⭐ bonus   LeetCode #827
 *  https://leetcode.com/problems/making-a-large-island/
 *  Pattern: label islands with DFS + HashMap (label -> size) + HashSet of neighbour labels
 * ============================================================================
 *
 *  PROBLEM
 *    n x n grid of 0 (water) and 1 (land). You may change AT MOST ONE 0 into a 1.
 *    Return the size of the largest island (4-directionally connected 1s) afterwards.
 *      [[1,0],[0,1]] -> 3     [[1,1],[1,0]] -> 4     [[1,1],[1,1]] -> 4
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: try flipping every 0 and run a full flood fill each time: O(n^4).
 *
 *  THE INSIGHT: flipping a 0 joins the islands that TOUCH it. If we already know every
 *  island's size, the new size is just  1 + sum of sizes of the DISTINCT islands around it.
 *    Pass 1: flood-fill each island, writing a LABEL (2, 3, 4, ...) into its cells, and
 *            record size[label]. (Labels start at 2 so they can't be confused with 0/1.)
 *    Pass 2: for every 0, collect the labels of its up-to-4 neighbours in a SET (the same
 *            island can touch a cell from two sides - count it once!) and add up sizes.
 *  If there are no 0s at all, the answer is the whole grid.
 *
 *  DRY RUN [[1,0],[0,1]]
 *    labels [[2,0],[0,3]]   size {2:1, 3:1}
 *    cell (0,1): neighbours {2, 3} -> 1 + 1 + 1 = 3      cell (1,0): same -> 3     answer 3
 *
 *  The flood fill uses an explicit stack: recursion on a 500 x 500 island would be
 *  250,000 calls deep and overflow the call stack.
 *
 *  COMPLEXITY   O(n^2)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class A07_MakingALargeIsland {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // ---------------- Approach 1: flip each 0 and flood fill again (the referee) ----------------
    static int largestIslandBrute(int[][] grid) {
        int n = grid.length, best = largestIslandNoFlip(grid);
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (grid[r][c] == 0) {
                    grid[r][c] = 1;
                    best = Math.max(best, largestIslandNoFlip(grid));
                    grid[r][c] = 0;                                      // undo
                }
        return best;
    }

    static int largestIslandNoFlip(int[][] grid) {
        int n = grid.length, best = 0;
        boolean[][] seen = new boolean[n][n];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (grid[r][c] == 1 && !seen[r][c]) {
                    int size = 0;
                    Deque<int[]> stack = new ArrayDeque<>();
                    stack.push(new int[]{r, c});
                    seen[r][c] = true;
                    while (!stack.isEmpty()) {
                        int[] cur = stack.pop();
                        size++;
                        for (int[] d : DIRS) {
                            int nr = cur[0] + d[0], nc = cur[1] + d[1];
                            if (nr >= 0 && nr < n && nc >= 0 && nc < n && grid[nr][nc] == 1 && !seen[nr][nc]) {
                                seen[nr][nc] = true;
                                stack.push(new int[]{nr, nc});
                            }
                        }
                    }
                    best = Math.max(best, size);
                }
        return best;
    }

    // ---------------- Approach 2: label islands once, then try each 0 (main solution) ----------------
    static int largestIsland(int[][] grid) {
        int n = grid.length;
        int[][] label = new int[n][n];
        Map<Integer, Integer> size = new HashMap<>();
        int next = 2, best = 0;

        for (int r = 0; r < n; r++)                                      // pass 1: label + measure
            for (int c = 0; c < n; c++)
                if (grid[r][c] == 1 && label[r][c] == 0) {
                    int s = flood(grid, label, r, c, next);
                    size.put(next, s);
                    best = Math.max(best, s);
                    next++;
                }

        boolean sawZero = false;
        for (int r = 0; r < n; r++)                                      // pass 2: try every 0
            for (int c = 0; c < n; c++)
                if (grid[r][c] == 0) {
                    sawZero = true;
                    Set<Integer> around = new HashSet<>();
                    for (int[] d : DIRS) {
                        int nr = r + d[0], nc = c + d[1];
                        if (nr >= 0 && nr < n && nc >= 0 && nc < n && label[nr][nc] != 0) around.add(label[nr][nc]);
                    }
                    int joined = 1;
                    for (int id : around) joined += size.get(id);
                    best = Math.max(best, joined);
                }
        return sawZero ? best : n * n;
    }

    private static int flood(int[][] grid, int[][] label, int r0, int c0, int id) {
        int n = grid.length, count = 0;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{r0, c0});
        label[r0][c0] = id;
        while (!stack.isEmpty()) {
            int[] cur = stack.pop();
            count++;
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr >= 0 && nr < n && nc >= 0 && nc < n && grid[nr][nc] == 1 && label[nr][nc] == 0) {
                    label[nr][nc] = id;                                  // mark when pushed, so nothing is pushed twice
                    stack.push(new int[]{nr, nc});
                }
            }
        }
        return count;
    }

    public static void main(String[] args) {
        check("example 1", 3, largestIsland(new int[][]{{1, 0}, {0, 1}}));
        check("example 2", 4, largestIsland(new int[][]{{1, 1}, {1, 0}}));
        check("no zeros", 4, largestIsland(new int[][]{{1, 1}, {1, 1}}));
        check("all water", 1, largestIsland(new int[][]{{0, 0}, {0, 0}}));
        check("same island on two sides counted once", 5, largestIsland(new int[][]{{1, 1, 1}, {1, 0, 0}, {0, 0, 0}}));

        int[][] big = new int[400][400];
        for (int[] row : big) Arrays.fill(row, 1);
        big[200][200] = 0;
        check("400x400 one hole (no stack overflow)", 160_000, largestIsland(big));

        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            int n = 1 + rnd.nextInt(6);
            int[][] g = new int[n][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(10) < 5 ? 1 : 0;
            ok &= largestIsland(g) == largestIslandBrute(g);
        }
        check("1,500 random grids vs flip-and-refill", true, ok);
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
