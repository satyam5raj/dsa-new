/*
 * ============================================================================
 *  MEDIUM 21 : NUMBER OF PROVINCES           LeetCode #547
 *  https://leetcode.com/problems/number-of-provinces/
 *  Pattern: CONNECTED COMPONENTS on an ADJACENCY MATRIX
 * ============================================================================
 *
 *  PROBLEM
 *    isConnected[i][j] = 1 if city i and city j are directly connected.
 *    A province = a group of directly or indirectly connected cities. Count them.
 *      [[1,1,0],[1,1,0],[0,0,1]] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  Same as M11 (connected components), but the graph comes as an ADJACENCY
 *  MATRIX. The neighbours of i are the j with isConnected[i][j] == 1 - a scan of
 *  row i, O(n). So DFS costs O(n^2) total, which is also the input size.
 *    for each city: if unvisited -> provinces++, DFS to mark its whole province
 *
 *  (Union-Find: union every (i, j) with a 1, count the roots - 11-Union-Find.)
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M21_NumberOfProvinces {

    static int findCircleNum(int[][] isConnected) {
        int n = isConnected.length, provinces = 0;
        boolean[] seen = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!seen[i]) {
                provinces++;
                dfs(isConnected, i, seen);
            }
        }
        return provinces;
    }

    static void dfs(int[][] m, int i, boolean[] seen) {
        seen[i] = true;
        for (int j = 0; j < m.length; j++) if (m[i][j] == 1 && !seen[j]) dfs(m, j, seen);   // row scan = neighbours
    }

    static int unionFind(int[][] m) {
        int n = m.length, count = n;
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (m[i][j] == 1) {
            int a = find(p, i), b = find(p, j);
            if (a != b) { p[a] = b; count--; }
        }
        return count;
    }

    static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }

    public static void main(String[] args) {
        check("[[1,1,0],[1,1,0],[0,0,1]]", 2, findCircleNum(new int[][]{{1, 1, 0}, {1, 1, 0}, {0, 0, 1}}));
        check("identity (all separate)", 3, findCircleNum(new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}));
        check("indirect: 0-1, 1-2", 1, findCircleNum(new int[][]{{1, 1, 0}, {1, 1, 1}, {0, 1, 1}}));
        Random rnd = new Random(21);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(12);
            int[][] m = new int[n][n];
            for (int i = 0; i < n; i++) { m[i][i] = 1; for (int j = i + 1; j < n; j++) if (rnd.nextInt(6) == 0) m[i][j] = m[j][i] = 1; }
            ok &= findCircleNum(m) == unionFind(m);
        }
        check("500 random matrices: DFS == Union-Find", true, ok);
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
