/*
 * ============================================================================
 *  HARD 01 : NUMBER OF ISLANDS II  ⭐ bonus   LeetCode #305 (premium)
 *  https://leetcode.com/problems/number-of-islands-ii/
 *  Pattern: ONLINE connectivity - add land one cell at a time, count islands after each
 * ============================================================================
 *
 *  PROBLEM
 *    An m x n grid of water. positions[i] = a cell that turns into land. After EACH
 *    addition, report the number of islands.
 *      m=3, n=3, [[0,0],[0,1],[1,2],[2,1]] -> [1,1,2,3]
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT DFS? Re-counting islands (M01 of 09-Graph) after every addition is
 *  O(m*n) per step -> O(k * m * n). With 10^4 additions on a 10^4-cell grid: 10^8+.
 *
 *  UNION-FIND IS BUILT FOR THIS ("edges arrive over time"):
 *    count = 0
 *    add (r, c):
 *       if it's already land -> report count again (duplicates are allowed!)
 *       make it land, count++              (a brand-new island of one cell)
 *       for each of the 4 neighbours that is land:
 *          if union(cell, neighbour) succeeds -> count--   (two islands merged)
 *  Cell id = r * n + c. Each addition costs O(4 * alpha) ≈ O(1).
 *
 *  DRY RUN
 *    (0,0): +1 -> 1
 *    (0,1): +1, merges with (0,0) -> 1
 *    (1,2): +1, no land neighbours -> 2
 *    (2,1): +1 -> 3
 *
 *  Union-Find can't handle the reverse (land turning back into water) - that would
 *  need "un-union". Trick for that: process the operations BACKWARDS (offline).
 *  COMPLEXITY   Time O(m*n + k * alpha)   Space O(m*n)
 * ============================================================================
 */

import java.util.*;

public class H01_NumberOfIslandsII {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static List<Integer> numIslands2(int m, int n, int[][] positions) {
        parent = new int[m * n];
        boolean[] land = new boolean[m * n];
        for (int i = 0; i < m * n; i++) parent[i] = i;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<Integer> result = new ArrayList<>();
        int count = 0;
        for (int[] p : positions) {
            int id = p[0] * n + p[1];
            if (!land[id]) {
                land[id] = true;
                count++;                                          // a new one-cell island
                for (int[] d : dirs) {
                    int r = p[0] + d[0], c = p[1] + d[1];
                    if (r < 0 || r >= m || c < 0 || c >= n || !land[r * n + c]) continue;
                    int a = find(id), b = find(r * n + c);
                    if (a != b) { parent[a] = b; count--; }       // merged two islands
                }
            }
            result.add(count);
        }
        return result;
    }

    // referee: recount islands with DFS after every addition
    static List<Integer> brute(int m, int n, int[][] positions) {
        boolean[][] g = new boolean[m][n];
        List<Integer> out = new ArrayList<>();
        for (int[] p : positions) {
            g[p[0]][p[1]] = true;
            boolean[][] seen = new boolean[m][n];
            int c = 0;
            for (int r = 0; r < m; r++) for (int q = 0; q < n; q++) {
                if (!g[r][q] || seen[r][q]) continue;
                c++;
                Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, q}));
                seen[r][q] = true;
                while (!st.isEmpty()) {
                    int[] x = st.pop();
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int a = x[0] + d[0], b = x[1] + d[1];
                        if (a >= 0 && a < m && b >= 0 && b < n && g[a][b] && !seen[a][b]) { seen[a][b] = true; st.push(new int[]{a, b}); }
                    }
                }
            }
            out.add(c);
        }
        return out;
    }

    public static void main(String[] args) {
        check("example", "[1, 1, 2, 3]", numIslands2(3, 3, new int[][]{{0, 0}, {0, 1}, {1, 2}, {2, 1}}));
        check("duplicate position", "[1, 1]", numIslands2(1, 1, new int[][]{{0, 0}, {0, 0}}));
        check("bridge merges 2 islands", "[1, 2, 1]", numIslands2(1, 3, new int[][]{{0, 0}, {0, 2}, {0, 1}}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int m = 1 + rnd.nextInt(6), n = 1 + rnd.nextInt(6);
            int[][] ps = new int[1 + rnd.nextInt(m * n + 3)][];
            for (int i = 0; i < ps.length; i++) ps[i] = new int[]{rnd.nextInt(m), rnd.nextInt(n)};
            ok &= numIslands2(m, n, ps).equals(brute(m, n, ps));
        }
        check("300 random sequences vs recounting with DFS", true, ok);
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
