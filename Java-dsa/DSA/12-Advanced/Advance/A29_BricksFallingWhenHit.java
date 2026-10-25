/*
 * ============================================================================
 *  ADVANCE 29 : BRICKS FALLING WHEN HIT  ⭐ bonus   LeetCode #803
 *  https://leetcode.com/problems/bricks-falling-when-hit/
 *  Pattern: process the hits in REVERSE (offline) + UNION-FIND with sizes and a ROOF node
 * ============================================================================
 *
 *  PROBLEM
 *    grid: 1 = brick, 0 = empty. A brick is STABLE if it's in the top row or connected
 *    (up/down/left/right) to a stable brick. hits[i] = a cell to erase, in order. After
 *    each hit, every brick that is no longer stable falls (disappears). Return how many
 *    bricks fall after each hit (the erased brick itself doesn't count).
 *      grid [[1,0,0,0],[1,1,1,0]], hits [[1,0]]  ->  [2]
 *        erasing (1,0) leaves (1,1),(1,2) hanging -> both fall
 *
 *  ---------------------------------------------------------------------------
 *  WHY FORWARD IS HARD: union-find can JOIN groups but can't SPLIT them. A hit splits.
 *  Forward simulation re-checks stability with a BFS after every hit: O(hits * m n).
 *
 *  REVERSE TIME: erasing bricks forwards = ADDING bricks backwards, and adding is what
 *  union-find is good at.
 *    1. Remove every hit brick from the grid (the final state).
 *    2. Build union-find over the remaining bricks. A virtual ROOF node is joined to
 *       every top-row brick, so "stable" = "in the roof's group".
 *    3. Go through the hits from LAST to FIRST, putting each brick back:
 *         before = size of the roof's group
 *         join the brick with its neighbouring bricks (and the roof if it's in row 0)
 *         after  = size of the roof's group
 *         bricks that fell at this hit = after - before - 1   (minus the brick itself),
 *         or 0 if negative (the restored brick didn't attach to the roof)
 *  A cell can be hit twice, or hit when empty: only the FIRST hit of a real brick erases
 *  anything. Decrement the grid once per hit; when restoring in reverse, a brick comes
 *  back only when its value climbs back to exactly 1 - which happens at its first hit.
 *  ASSUMPTION (as in LeetCode's tests): every brick is stable before the first hit.
 *
 *  DRY RUN grid [[1,0,0,0],[1,1,1,0]], hits [[1,0]]
 *    after removing hits: [[1,0,0,0],[0,1,1,0]]   roof group {roof, (0,0)}: size 2
 *    (1,1)-(1,2) form their own group (size 2, not on the roof)
 *    restore (1,0): joins (0,0) [roof group] and (1,1) [the pair] -> roof size 5
 *    fallen = 5 - 2 - 1 = 2                                     -> [2]
 *
 *  COMPLEXITY   O((m n + hits) * alpha)   Space O(m n)
 * ============================================================================
 */

import java.util.*;

public class A29_BricksFallingWhenHit {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static class DSU {
        final int[] parent, size;
        DSU(int n) { parent = new int[n]; size = new int[n]; for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; } }
        int find(int x) { while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; } return x; }
        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            size[ra] += size[rb];
        }
        int sizeOf(int x) { return size[find(x)]; }
    }

    // ---------------- Approach 1: forward simulation with a BFS after every hit (the referee) ----------------
    static int[] hitBricksBrute(int[][] grid, int[][] hits) {
        int m = grid.length;
        int[][] g = new int[m][];
        for (int r = 0; r < m; r++) g[r] = grid[r].clone();
        int[] ans = new int[hits.length];
        for (int i = 0; i < hits.length; i++) {
            if (g[hits[i][0]][hits[i][1]] == 0) continue;                 // nothing there
            g[hits[i][0]][hits[i][1]] = 0;
            ans[i] = dropUnstable(g);
        }
        return ans;
    }

    /** BFS from the top row; erase every brick it can't reach and return how many fell. */
    static int dropUnstable(int[][] g) {
        int m = g.length, n = g[0].length, fallen = 0;
        boolean[][] stable = new boolean[m][n];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int c = 0; c < n; c++) if (g[0][c] == 1) { stable[0][c] = true; queue.add(new int[]{0, c}); }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && g[nr][nc] == 1 && !stable[nr][nc]) { stable[nr][nc] = true; queue.add(new int[]{nr, nc}); }
            }
        }
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) if (g[r][c] == 1 && !stable[r][c]) { g[r][c] = 0; fallen++; }   // falls
        return fallen;
    }

    // ---------------- Approach 2: reverse time + union-find (main solution) ----------------
    static int[] hitBricks(int[][] grid, int[][] hits) {
        int m = grid.length, n = grid[0].length, roof = m * n;
        int[][] g = new int[m][];
        for (int r = 0; r < m; r++) g[r] = grid[r].clone();
        for (int[] h : hits) g[h[0]][h[1]]--;                             // a brick hit at least once is gone at the end

        DSU dsu = new DSU(m * n + 1);
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) {
                if (g[r][c] != 1) continue;
                if (r == 0) dsu.union(c, roof);
                if (r > 0 && g[r - 1][c] == 1) dsu.union(r * n + c, (r - 1) * n + c);   // up and left cover every pair once
                if (c > 0 && g[r][c - 1] == 1) dsu.union(r * n + c, r * n + c - 1);
            }

        int[] ans = new int[hits.length];
        for (int i = hits.length - 1; i >= 0; i--) {
            int r = hits[i][0], c = hits[i][1];
            if (++g[r][c] != 1) continue;                                 // this hit erased nothing
            int before = dsu.sizeOf(roof);
            if (r == 0) dsu.union(c, roof);
            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && g[nr][nc] == 1) dsu.union(r * n + c, nr * n + nc);
            }
            int after = dsu.sizeOf(roof);
            ans[i] = Math.max(0, after - before - 1);                     // the restored brick itself didn't "fall"
        }
        return ans;
    }

    public static void main(String[] args) {
        check("example 1", "[2]", hitBricks(new int[][]{{1, 0, 0, 0}, {1, 1, 1, 0}}, new int[][]{{1, 0}}));
        check("example 2", "[0, 0]", hitBricks(new int[][]{{1, 0, 0, 0}, {1, 1, 0, 0}}, new int[][]{{1, 1}, {1, 0}}));
        check("hitting empty and repeated cells", "[0, 1, 0]", hitBricks(new int[][]{{1}, {1}, {0}}, new int[][]{{2, 0}, {0, 0}, {0, 0}}));
        check("cutting a column", "[0, 3]", hitBricks(new int[][]{{1, 1}, {0, 1}, {0, 1}, {0, 1}}, new int[][]{{0, 0}, {0, 1}}));

        Random rnd = new Random(29);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int m = 1 + rnd.nextInt(5), n = 1 + rnd.nextInt(5);
            int[][] g = new int[m][n];
            for (int[] row : g) for (int c = 0; c < n; c++) row[c] = rnd.nextInt(10) < 7 ? 1 : 0;
            dropUnstable(g);                                              // like LeetCode: every brick starts stable
            int[][] hits = new int[rnd.nextInt(8)][];
            for (int i = 0; i < hits.length; i++) hits[i] = new int[]{rnd.nextInt(m), rnd.nextInt(n)};
            ok &= Arrays.equals(hitBricks(g, hits), hitBricksBrute(g, hits));
        }
        check("2,000 random grids vs forward simulation", true, ok);
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
