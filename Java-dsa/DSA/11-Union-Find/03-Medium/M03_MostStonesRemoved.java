/*
 * ============================================================================
 *  MEDIUM 03 : MOST STONES REMOVED WITH SAME ROW OR COLUMN   LeetCode #947
 *  https://leetcode.com/problems/most-stones-removed-with-same-row-or-column/
 *  Pattern: answer = stones - components; union ROWS with COLUMNS (not stone pairs)
 * ============================================================================
 *
 *  PROBLEM
 *    Stones on a grid. A stone can be removed if another remaining stone shares its
 *    row or column. Max number of stones you can remove?
 *      [[0,0],[0,1],[1,0],[1,2],[2,1],[2,2]] -> 5
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: connect two stones if they share a row or column. In each connected
 *  group you can remove stones one at a time until exactly ONE is left (remove
 *  leaves of a spanning tree first, so the rest stays connected). Different groups
 *  never help each other. So:
 *       answer = number of stones - number of groups
 *
 *  BUILDING THE GROUPS
 *    O(n^2): union every pair of stones sharing a row/column.
 *    O(n):   TRICK - treat each ROW and each COLUMN as a node. Stone (r, c) unions
 *            node "row r" with node "column c". Stones in the same row share the row
 *            node; same column share the column node. Offset columns so they don't
 *            collide with rows: column c -> c + 10001 (or use a HashMap).
 *            groups = number of distinct roots among the row/column nodes used.
 *
 *  DRY RUN [[0,0],[0,1],[1,0],[1,2],[2,1],[2,2]]
 *    all 6 stones end up linked through rows 0,1,2 and cols 0,1,2 -> 1 group -> 6 - 1 = 5
 *
 *  COMPLEXITY   Time O(n * alpha)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M03_MostStonesRemoved {

    static Map<Integer, Integer> parent;

    static int find(int x) {
        parent.putIfAbsent(x, x);
        int root = x;
        while (parent.get(root) != root) root = parent.get(root);
        while (parent.get(x) != root) { int nx = parent.get(x); parent.put(x, root); x = nx; }   // compress
        return root;
    }

    static int removeStones(int[][] stones) {
        parent = new HashMap<>();
        for (int[] s : stones) parent.put(find(s[0]), find(s[1] + 10_001));   // row node <-> column node
        Set<Integer> roots = new HashSet<>();
        for (int key : parent.keySet()) roots.add(find(key));
        return stones.length - roots.size();
    }

    // referee: O(n^2) union of stone pairs
    static int brute(int[][] s) {
        int n = s.length;
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        int groups = n;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
            if (s[i][0] == s[j][0] || s[i][1] == s[j][1]) {
                int a = root(p, i), b = root(p, j);
                if (a != b) { p[a] = b; groups--; }
            }
        }
        return n - groups;
    }

    static int root(int[] p, int x) { while (p[x] != x) x = p[x]; return x; }

    public static void main(String[] args) {
        check("example 1", 5, removeStones(new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 2}, {2, 1}, {2, 2}}));
        check("example 2", 3, removeStones(new int[][]{{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}}));
        check("single stone", 0, removeStones(new int[][]{{0, 0}}));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            Set<String> used = new HashSet<>();
            List<int[]> st = new ArrayList<>();
            for (int i = 0; i < 1 + rnd.nextInt(15); i++) {
                int r = rnd.nextInt(6), c = rnd.nextInt(6);
                if (used.add(r + "," + c)) st.add(new int[]{r, c});
            }
            int[][] arr = st.toArray(new int[0][]);
            ok &= removeStones(arr) == brute(arr);
        }
        check("1,000 random boards: row/col trick == pairwise", true, ok);
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
