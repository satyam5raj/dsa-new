/*
 * ============================================================================
 *  MEDIUM 05 : REGIONS CUT BY SLASHES        LeetCode #959
 *  https://leetcode.com/problems/regions-cut-by-slashes/
 *  Pattern: split each cell into 4 TRIANGLES; union the pieces that touch; count sets
 * ============================================================================
 *
 *  PROBLEM
 *    n x n grid; each cell is ' ', '/' or '\'. The slashes split the square into
 *    regions. How many regions?
 *      [" /","/ "] -> 2        ["/\\","\\/"] -> 5  (a diamond in the middle)
 *
 *  ---------------------------------------------------------------------------
 *  THE MODELLING TRICK: cut every cell along both diagonals into 4 triangles:
 *         +-----+
 *         |\ 0 /|        0 = top, 1 = right, 2 = bottom, 3 = left
 *         | 3 X 1|
 *         |/ 2 \|
 *         +-----+
 *  Inside a cell:
 *    ' '  -> all 4 triangles are one region: union 0-1, 1-2, 2-3
 *    '/'  -> the line goes bottom-left to top-right: top+left together (0-3),
 *            right+bottom together (1-2)
 *    '\'  -> top+right together (0-1), bottom+left together (2-3)
 *  Between cells (always open - slashes never lie on cell borders):
 *    this cell's RIGHT (1) touches the right neighbour's LEFT (3)
 *    this cell's BOTTOM (2) touches the lower neighbour's TOP (0)
 *  Triangle id = 4 * (r * n + c) + k. Answer = number of disjoint sets.
 *
 *  (Alternative: upscale each cell to a 3x3 pixel block, draw the slashes as
 *   pixels, and count islands with DFS. Same answer, more memory.)
 *
 *  COMPLEXITY   Time O(n^2 * alpha)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M05_RegionsCutBySlashes {

    static int[] parent;
    static int count;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static void union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra != rb) { parent[ra] = rb; count--; }
    }

    static int regionsBySlashes(String[] grid) {
        int n = grid.length;
        parent = new int[4 * n * n];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        count = parent.length;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int base = 4 * (r * n + c);
                char ch = grid[r].charAt(c);
                if (ch == ' ') { union(base, base + 1); union(base + 1, base + 2); union(base + 2, base + 3); }
                else if (ch == '/') { union(base, base + 3); union(base + 1, base + 2); }
                else { union(base, base + 1); union(base + 2, base + 3); }
                if (c + 1 < n) union(base + 1, 4 * (r * n + c + 1) + 3);     // right <-> neighbour's left
                if (r + 1 < n) union(base + 2, 4 * ((r + 1) * n + c));       // bottom <-> lower neighbour's top
            }
        }
        return count;
    }

    // referee: 3x upscaling + flood fill
    static int upscale(String[] grid) {
        int n = grid.length, N = 3 * n;
        boolean[][] wall = new boolean[N][N];
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
            char ch = grid[r].charAt(c);
            for (int k = 0; k < 3; k++) {
                if (ch == '/') wall[3 * r + k][3 * c + 2 - k] = true;
                if (ch == '\\') wall[3 * r + k][3 * c + k] = true;
            }
        }
        int regions = 0;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (wall[r][c]) continue;
            regions++;
            Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, c}));
            wall[r][c] = true;
            while (!st.isEmpty()) {
                int[] p = st.pop();
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nr = p[0] + d[0], nc = p[1] + d[1];
                    if (nr >= 0 && nr < N && nc >= 0 && nc < N && !wall[nr][nc]) { wall[nr][nc] = true; st.push(new int[]{nr, nc}); }
                }
            }
        }
        return regions;
    }

    public static void main(String[] args) {
        check("[' /','/ ']", 2, regionsBySlashes(new String[]{" /", "/ "}));
        check("[' /','  ']", 1, regionsBySlashes(new String[]{" /", "  "}));
        check("diamond ['/\\\\','\\\\/']", 5, regionsBySlashes(new String[]{"/\\", "\\/"}));
        Random rnd = new Random(6);
        boolean ok = true;
        char[] opts = {' ', '/', '\\'};
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(6);
            String[] g = new String[n];
            for (int r = 0; r < n; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < n; c++) sb.append(opts[rnd.nextInt(3)]);
                g[r] = sb.toString();
            }
            ok &= regionsBySlashes(g) == upscale(g);
        }
        check("500 random grids vs 3x upscale + flood fill", true, ok);
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
