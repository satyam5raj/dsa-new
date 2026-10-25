/*
 * ============================================================================
 *  ADVANCE 21 : LONGEST INCREASING PATH IN A MATRIX  ⭐ bonus   LeetCode #329
 *  https://leetcode.com/problems/longest-increasing-path-in-a-matrix/
 *  Pattern: grid as a DAG -> DFS + MEMO (DP)  or  TOPOLOGICAL SORT (BFS by levels)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the length of the longest strictly increasing path (moves up/down/left/
 *    right, no diagonals, no wrapping).
 *      [[9,9,4],
 *       [6,6,8],      ->  4      1 -> 2 -> 6 -> 9
 *       [2,1,1]]
 *
 *  ---------------------------------------------------------------------------
 *  SEE IT AS A GRAPH: draw an arrow from each cell to every neighbour with a BIGGER
 *  value. Values strictly increase along arrows, so there can be NO CYCLE: it's a DAG.
 *  "Longest increasing path" = "longest path in a DAG", which is easy (it's NP-hard in
 *  general graphs - the DAG is what saves us).
 *
 *  BRUTE FORCE: DFS from every cell exploring every increasing path - exponential.
 *
 *  APPROACH 1 - DFS + MEMO: best(cell) = 1 + max(best(bigger neighbour)). No cycles, so
 *    the recursion always ends, and each cell's answer never changes -> cache it.
 *    O(m n). Catch: on a 200 x 200 "snake" the recursion is 40,000 calls deep and can
 *    overflow Java's call stack.
 *
 *  APPROACH 2 - TOPOLOGICAL SORT (main): indegree(cell) = number of SMALLER neighbours.
 *    Start a BFS from all cells with indegree 0 (local minima). Peel the graph level by
 *    level like Kahn's algorithm; the NUMBER OF LEVELS is the longest path. Iterative,
 *    so no stack overflow.
 *
 *  DRY RUN (topological) the example
 *    indegree 0 (no smaller neighbour): the 4 at (0,2) and the two 1s at the bottom
 *    level 1: 4, 1, 1     level 2: 6 at (1,1), 2      level 3: 9 at (0,1), 8, 6 at (1,0)
 *    level 4: 9 at (0,0)                                -> 4 levels
 *    (8 waits until level 3: it is bigger than 4, 1 AND the 6 at (1,1), which is level 2)
 *
 *  COMPLEXITY   O(m n) both approaches   Space O(m n)
 * ============================================================================
 */

import java.util.*;

public class A21_LongestIncreasingPathInMatrix {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // ---------------- Referee: DFS without memo (exponential, tiny grids) ----------------
    static int longestIncreasingPathBrute(int[][] matrix) {
        int best = 0;
        for (int r = 0; r < matrix.length; r++)
            for (int c = 0; c < matrix[0].length; c++) best = Math.max(best, explore(matrix, r, c));
        return best;
    }

    private static int explore(int[][] mat, int r, int c) {
        int best = 1;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < mat.length && nc >= 0 && nc < mat[0].length && mat[nr][nc] > mat[r][c])
                best = Math.max(best, 1 + explore(mat, nr, nc));
        }
        return best;
    }

    // ---------------- Approach 1: DFS + memo ----------------
    static int longestIncreasingPathMemo(int[][] matrix) {
        int[][] memo = new int[matrix.length][matrix[0].length];         // 0 = not computed yet
        int best = 0;
        for (int r = 0; r < matrix.length; r++)
            for (int c = 0; c < matrix[0].length; c++) best = Math.max(best, dfs(matrix, r, c, memo));
        return best;
    }

    private static int dfs(int[][] mat, int r, int c, int[][] memo) {
        if (memo[r][c] != 0) return memo[r][c];
        int best = 1;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < mat.length && nc >= 0 && nc < mat[0].length && mat[nr][nc] > mat[r][c])
                best = Math.max(best, 1 + dfs(mat, nr, nc, memo));
        }
        return memo[r][c] = best;
    }

    // ---------------- Approach 2: topological sort, count the levels (main solution) ----------------
    static int longestIncreasingPath(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        int[][] indegree = new int[m][n];                                 // arrows point from smaller to bigger
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                for (int[] d : DIRS) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr >= 0 && nr < m && nc >= 0 && nc < n && matrix[nr][nc] < matrix[r][c]) indegree[r][c]++;
                }
        Deque<int[]> queue = new ArrayDeque<>();
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) if (indegree[r][c] == 0) queue.add(new int[]{r, c});   // local minima

        int levels = 0;
        while (!queue.isEmpty()) {
            levels++;
            for (int size = queue.size(); size > 0; size--) {
                int[] cur = queue.poll();
                for (int[] d : DIRS) {
                    int nr = cur[0] + d[0], nc = cur[1] + d[1];
                    if (nr >= 0 && nr < m && nc >= 0 && nc < n && matrix[nr][nc] > matrix[cur[0]][cur[1]]
                            && --indegree[nr][nc] == 0) queue.add(new int[]{nr, nc});
                }
            }
        }
        return levels;
    }

    public static void main(String[] args) {
        int[][] ex1 = {{9, 9, 4}, {6, 6, 8}, {2, 1, 1}};
        check("example 1", 4, longestIncreasingPath(ex1));
        check("example 1 (memo)", 4, longestIncreasingPathMemo(ex1));
        check("example 2", 4, longestIncreasingPath(new int[][]{{3, 4, 5}, {3, 2, 6}, {2, 2, 1}}));
        check("all equal", 1, longestIncreasingPath(new int[][]{{7, 7}, {7, 7}}));

        int n = 200;
        int[][] snake = new int[n][n];                                    // values increase along a zig-zag through every cell
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) snake[r][c] = r * n + (r % 2 == 0 ? c : n - 1 - c);
        check("200x200 snake: 40,000 (iterative, no overflow)", 40_000, longestIncreasingPath(snake));

        Random rnd = new Random(21);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            int m = 1 + rnd.nextInt(4), w = 1 + rnd.nextInt(4);
            int[][] g = new int[m][w];
            for (int[] row : g) for (int c = 0; c < w; c++) row[c] = rnd.nextInt(6);
            int want = longestIncreasingPathBrute(g);
            ok &= longestIncreasingPath(g) == want && longestIncreasingPathMemo(g) == want;
        }
        check("1,500 random grids: topo == memo == brute", true, ok);
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
