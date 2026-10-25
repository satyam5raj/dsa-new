/*
 * ============================================================================
 *  ADVANCE 14 : FIND CRITICAL AND PSEUDO-CRITICAL EDGES IN MST  ⭐ bonus   LeetCode #1489
 *  https://leetcode.com/problems/find-critical-and-pseudo-critical-edges-in-minimum-spanning-tree/
 *  Pattern: SORT edges + UNION-FIND (Kruskal), re-run with one edge SKIPPED or FORCED
 * ============================================================================
 *
 *  PROBLEM
 *    A connected weighted undirected graph, edges[i] = [a, b, weight].
 *    CRITICAL edge: removing it makes every MST heavier (it is in EVERY MST).
 *    PSEUDO-CRITICAL edge: in SOME MST but not all.
 *    Return [critical indices, pseudo-critical indices].
 *      n=5, edges [[0,1,1],[1,2,1],[2,3,2],[0,3,2],[0,4,3],[3,4,3],[1,4,6]]
 *      -> [[0,1],[2,3,4,5]]
 *
 *  ---------------------------------------------------------------------------
 *  KRUSKAL REMINDER: sort edges by weight; add an edge if it joins two DIFFERENT
 *  components (union-find tells us in ~O(1)); stop at n-1 edges.
 *
 *  Let base = the MST weight. For each edge e:
 *    SKIP e and rebuild: if the result is heavier (or the graph falls apart), every MST
 *       needs e -> CRITICAL.
 *    Otherwise FORCE e (union its endpoints first, count its weight) and rebuild: if the
 *       total is still base, some MST contains e -> PSEUDO-CRITICAL.
 *    Otherwise e is in no MST.
 *  Why forcing works: Kruskal around a fixed edge finds the cheapest spanning tree that
 *  CONTAINS it; if that equals base, it is an MST with e in it.
 *
 *  DRY RUN (example) base = 1+1+2+3 = 7
 *    skip edge 0 [0,1,1]: best tree 1+2+2+3 = 8 > 7      -> critical
 *    skip edge 2 [2,3,2]: 1+1+2(0-3)+3 = 7 (not heavier); force it: 7 -> pseudo
 *    skip edge 6 [1,4,6]: still 7; force it: 6+1+1+2 = 10 -> neither
 *
 *  COMPLEXITY   O(E^2 * alpha(n)) after one O(E log E) sort   Space O(n + E)
 * ============================================================================
 */

import java.util.*;

public class A14_CriticalAndPseudoCriticalEdges {

    // ---------------- Union-find with path compression + union by size ----------------
    static class DSU {
        final int[] parent, size;
        int components;
        DSU(int n) { parent = new int[n]; size = new int[n]; components = n; for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; } }
        int find(int x) { while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; } return x; }
        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra; size[ra] += size[rb]; components--;
            return true;
        }
    }

    // ---------------- Referee: list every spanning tree (tiny graphs only) ----------------
    static List<List<Integer>> findBrute(int n, int[][] edges) {
        int m = edges.length, best = Integer.MAX_VALUE;
        List<Integer> mstMasks = new ArrayList<>();
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != n - 1) continue;
            DSU d = new DSU(n);
            int w = 0;
            boolean tree = true;
            for (int i = 0; i < m && tree; i++)
                if ((mask >> i & 1) == 1) { tree = d.union(edges[i][0], edges[i][1]); w += edges[i][2]; }
            if (!tree) continue;                                         // n-1 edges without a cycle = spanning tree
            if (w < best) { best = w; mstMasks.clear(); }
            if (w == best) mstMasks.add(mask);
        }
        List<Integer> critical = new ArrayList<>(), pseudo = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int in = 0;
            for (int mask : mstMasks) if ((mask >> i & 1) == 1) in++;
            if (in == mstMasks.size()) critical.add(i);
            else if (in > 0) pseudo.add(i);
        }
        return List.of(critical, pseudo);
    }

    // ---------------- Kruskal with one edge skipped or forced (main solution) ----------------
    static List<List<Integer>> findCriticalAndPseudoCriticalEdges(int n, int[][] edges) {
        Integer[] order = new Integer[edges.length];
        for (int i = 0; i < edges.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> edges[i][2]));   // sort ONCE, reuse for every rebuild

        int base = mst(n, edges, order, -1, -1);
        List<Integer> critical = new ArrayList<>(), pseudo = new ArrayList<>();
        for (int e = 0; e < edges.length; e++) {
            if (mst(n, edges, order, e, -1) > base) critical.add(e);
            else if (mst(n, edges, order, -1, e) == base) pseudo.add(e);
        }
        return List.of(critical, pseudo);
    }

    /** MST weight, skipping edge 'skip' and/or taking edge 'force' first; MAX_VALUE if not connected. */
    static int mst(int n, int[][] edges, Integer[] order, int skip, int force) {
        DSU d = new DSU(n);
        int weight = 0;
        if (force != -1) { d.union(edges[force][0], edges[force][1]); weight += edges[force][2]; }
        for (int i : order) {
            if (i == skip) continue;
            if (d.union(edges[i][0], edges[i][1])) weight += edges[i][2];
        }
        return d.components == 1 ? weight : Integer.MAX_VALUE;
    }

    public static void main(String[] args) {
        check("example 1", "[[0, 1], [2, 3, 4, 5]]", findCriticalAndPseudoCriticalEdges(5,
                new int[][]{{0, 1, 1}, {1, 2, 1}, {2, 3, 2}, {0, 3, 2}, {0, 4, 3}, {3, 4, 3}, {1, 4, 6}}));
        check("example 2 (a square of equal weights)", "[[], [0, 1, 2, 3]]", findCriticalAndPseudoCriticalEdges(4,
                new int[][]{{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {0, 3, 1}}));
        check("a bridge is always critical", "[[0], []]", findCriticalAndPseudoCriticalEdges(2, new int[][]{{0, 1, 5}}));

        Random rnd = new Random(14);
        boolean ok = true;
        for (int t = 0; t < 800; t++) {
            int n = 2 + rnd.nextInt(5);
            List<int[]> es = new ArrayList<>();
            for (int i = 1; i < n; i++) es.add(new int[]{i, rnd.nextInt(i), 1 + rnd.nextInt(4)});   // a tree keeps it connected
            for (int e = rnd.nextInt(6); e > 0; e--) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b) es.add(new int[]{a, b, 1 + rnd.nextInt(4)});
            }
            int[][] edges = es.toArray(new int[0][]);
            ok &= findCriticalAndPseudoCriticalEdges(n, edges).equals(findBrute(n, edges));
        }
        check("800 random graphs vs every spanning tree", true, ok);
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
