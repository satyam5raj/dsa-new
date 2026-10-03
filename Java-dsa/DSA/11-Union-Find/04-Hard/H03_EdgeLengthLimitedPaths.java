/*
 * ============================================================================
 *  HARD 03 : CHECKING EXISTENCE OF EDGE LENGTH LIMITED PATHS  ⭐ bonus   LeetCode #1697
 *  https://leetcode.com/problems/checking-existence-of-edge-length-limited-paths/
 *  Pattern: OFFLINE - sort edges and queries by weight; the DSU only ever GROWS
 * ============================================================================
 *
 *  PROBLEM
 *    Undirected weighted graph (possibly with parallel edges). queries[j] = [p, q, limit]:
 *    is there a path from p to q using ONLY edges with weight STRICTLY less than limit?
 *      n=3, edges [[0,1,2],[1,2,4],[2,0,8],[1,0,16]], queries [[0,1,2],[0,2,5]] -> [false, true]
 *
 *  ---------------------------------------------------------------------------
 *  PER QUERY BFS: ignore edges >= limit, BFS from p. O(Q * (V + E)): 10^5 * 10^5 -> too slow.
 *
 *  OFFLINE TRICK: a query with a bigger limit can use every edge a smaller-limit
 *  query can, plus more. So answer queries in INCREASING order of limit:
 *    sort edges by weight; sort query indices by limit
 *    for each query (smallest limit first):
 *       union every edge with weight < limit that hasn't been added yet (a pointer)
 *       answer = find(p) == find(q)  -> store at the query's ORIGINAL index
 *  Every edge is unioned once; every query is one find. We never need to REMOVE
 *  an edge - which Union-Find can't do anyway.
 *  (Same idea as 10-Trie/H05: "sort the queries so the structure only grows".)
 *
 *  DRY RUN: query limit 2: no edge < 2 -> 0,1 not connected -> false
 *           query limit 5: add 2 (0-1) and 4 (1-2) -> 0 ~ 2 -> true
 *
 *  COMPLEXITY   Time O(E log E + Q log Q + (E + Q) * alpha)   Space O(V + Q)
 * ============================================================================
 */

import java.util.*;

public class H03_EdgeLengthLimitedPaths {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static boolean[] distanceLimitedPathsExist(int n, int[][] edgeList, int[][] queries) {
        parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int[][] edges = edgeList.clone();
        Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));
        Integer[] order = new Integer[queries.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> queries[i][2]));
        boolean[] ans = new boolean[queries.length];
        int next = 0;
        for (int qi : order) {
            int[] q = queries[qi];
            while (next < edges.length && edges[next][2] < q[2]) {     // add every edge allowed so far
                parent[find(edges[next][0])] = find(edges[next][1]);
                next++;
            }
            ans[qi] = find(q[0]) == find(q[1]);                        // write at the ORIGINAL position
        }
        return ans;
    }

    static boolean[] brute(int n, int[][] edges, int[][] queries) {
        boolean[] out = new boolean[queries.length];
        for (int j = 0; j < queries.length; j++) {
            int[] q = queries[j];
            boolean[] seen = new boolean[n];
            Deque<Integer> st = new ArrayDeque<>(List.of(q[0]));
            seen[q[0]] = true;
            while (!st.isEmpty()) {
                int u = st.pop();
                for (int[] e : edges) {
                    if (e[2] >= q[2]) continue;
                    int v = e[0] == u ? e[1] : e[1] == u ? e[0] : -1;
                    if (v >= 0 && !seen[v]) { seen[v] = true; st.push(v); }
                }
            }
            out[j] = seen[q[1]];
        }
        return out;
    }

    public static void main(String[] args) {
        check("example 1", "[false, true]", Arrays.toString(distanceLimitedPathsExist(3,
                new int[][]{{0, 1, 2}, {1, 2, 4}, {2, 0, 8}, {1, 0, 16}}, new int[][]{{0, 1, 2}, {0, 2, 5}})));
        check("example 2", "[true, false]", Arrays.toString(distanceLimitedPathsExist(5,
                new int[][]{{0, 1, 10}, {1, 2, 5}, {2, 3, 9}, {3, 4, 13}}, new int[][]{{0, 4, 14}, {1, 4, 13}})));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 2 + rnd.nextInt(8);
            int[][] es = new int[rnd.nextInt(15)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(20)};
            int[][] qs = new int[1 + rnd.nextInt(10)][];
            for (int i = 0; i < qs.length; i++) qs[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(22)};
            ok &= Arrays.equals(distanceLimitedPathsExist(n, es, qs), brute(n, es, qs));
        }
        check("500 random graphs vs BFS per query", true, ok);
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
