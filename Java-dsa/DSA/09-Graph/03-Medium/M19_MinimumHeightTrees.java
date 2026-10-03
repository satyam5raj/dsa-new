/*
 * ============================================================================
 *  MEDIUM 19 : MINIMUM HEIGHT TREES          LeetCode #310
 *  https://leetcode.com/problems/minimum-height-trees/
 *  Pattern: PEEL LEAVES layer by layer (topological sort on an undirected tree)
 * ============================================================================
 *
 *  PROBLEM
 *    A tree with n nodes. Rooting it at node r gives some height. Return ALL roots
 *    that give the MINIMUM height.
 *      n=4, [[1,0],[1,2],[1,3]]              -> [1]
 *      n=6, [[3,0],[3,1],[3,2],[3,4],[5,4]]  -> [3,4]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: BFS from every node, measure the height -> O(n^2). n = 2*10^4: too slow.
 *
 *  INSIGHT: the best roots are the CENTRE of the tree - the middle of its longest
 *  path (diameter). There are always 1 or 2 centres. To find them, burn the tree
 *  from the outside in: remove ALL current leaves at once (degree 1), which
 *  creates new leaves; repeat. The last 1 or 2 nodes standing are the centres.
 *  (Like Kahn's algorithm, with "degree == 1" instead of "in-degree == 0".)
 *
 *    while remaining > 2:
 *      remove every current leaf (remaining -= #leaves)
 *      for each removed leaf's neighbour: degree--; if it becomes 1 -> next layer's leaf
 *    return the leaves left
 *
 *  DRY RUN n=6: 0-3, 1-3, 2-3, 3-4, 4-5
 *    leaves {0,1,2,5} -> remove -> 3's degree 4 -> 1, 4's degree 2 -> 1
 *    remaining 2 -> answer [3,4]
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M19_MinimumHeightTrees {

    static List<Integer> findMinHeightTrees(int n, int[][] edges) {
        if (n == 1) return List.of(0);
        List<Set<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new HashSet<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        int[] degree = new int[n];
        List<Integer> leaves = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            degree[i] = adj.get(i).size();
            if (degree[i] == 1) leaves.add(i);
        }
        int remaining = n;
        while (remaining > 2) {
            remaining -= leaves.size();                      // peel the whole outer layer
            List<Integer> next = new ArrayList<>();
            for (int leaf : leaves) {
                for (int nb : adj.get(leaf)) {
                    if (--degree[nb] == 1) next.add(nb);     // nb became a leaf of the inner tree
                }
            }
            leaves = next;
        }
        Collections.sort(leaves);
        return leaves;
    }

    // brute force: BFS height from every root
    static List<Integer> brute(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        int[] h = new int[n];
        for (int r = 0; r < n; r++) {
            int[] d = new int[n];
            Arrays.fill(d, -1);
            d[r] = 0;
            Queue<Integer> q = new ArrayDeque<>(List.of(r));
            while (!q.isEmpty()) { int u = q.poll(); for (int v : adj.get(u)) if (d[v] < 0) { d[v] = d[u] + 1; q.add(v); h[r] = Math.max(h[r], d[v]); } }
        }
        int min = Arrays.stream(h).min().getAsInt();
        List<Integer> out = new ArrayList<>();
        for (int r = 0; r < n; r++) if (h[r] == min) out.add(r);
        return out;
    }

    public static void main(String[] args) {
        check("star", "[1]", findMinHeightTrees(4, new int[][]{{1, 0}, {1, 2}, {1, 3}}));
        check("two centres", "[3, 4]", findMinHeightTrees(6, new int[][]{{3, 0}, {3, 1}, {3, 2}, {3, 4}, {5, 4}}));
        check("n=1", "[0]", findMinHeightTrees(1, new int[][]{}));
        check("n=2", "[0, 1]", findMinHeightTrees(2, new int[][]{{0, 1}}));
        Random rnd = new Random(19);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(15);
            int[][] es = new int[n - 1][];
            for (int i = 1; i < n; i++) es[i - 1] = new int[]{i, rnd.nextInt(i)};   // random tree
            ok &= findMinHeightTrees(n, es).equals(brute(n, es));
        }
        check("500 random trees vs BFS from every root", true, ok);
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
