/*
 * ============================================================================
 *  EASY 01 : FIND IF PATH EXISTS IN GRAPH - the Union-Find way  ⭐   LeetCode #1971
 *  https://leetcode.com/problems/find-if-path-exists-in-graph/
 *  Pattern: union every edge, then ask find(source) == find(destination)
 * ============================================================================
 *
 *  PROBLEM (solved with BFS in 09-Graph/E01)
 *    Undirected graph; is there a path between source and destination?
 *
 *  ---------------------------------------------------------------------------
 *  "Is there a path?" in an UNDIRECTED graph = "are they in the same component?"
 *  That's exactly what Union-Find answers:
 *    for each edge (a, b): union(a, b)
 *    return find(source) == find(destination)
 *  No adjacency list, no queue, no visited array - just two int arrays.
 *
 *  WHEN IS UNION-FIND BETTER THAN BFS HERE?
 *    - edges arrive one at a time and you're asked after each one
 *    - many (source, destination) queries on the same graph: each query ~O(1)
 *  WHEN IS BFS BETTER? when you need the PATH itself or its LENGTH (UF only says yes/no).
 *
 *  This is the template every other file in this topic builds on.
 *  COMPLEXITY   Time O((V + E) * alpha(V)) ≈ O(V + E)   Space O(V)
 * ============================================================================
 */

import java.util.*;

public class E01_FindIfPathExistsUnionFind {

    static int[] parent, size;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static void union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
    }

    static boolean validPath(int n, int[][] edges, int source, int destination) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
        for (int[] e : edges) union(e[0], e[1]);
        return find(source) == find(destination);
    }

    static boolean bfs(int n, int[][] edges, int s, int d) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
        boolean[] seen = new boolean[n];
        Deque<Integer> q = new ArrayDeque<>(List.of(s));
        seen[s] = true;
        while (!q.isEmpty()) for (int v : adj.get(q.poll())) if (!seen[v]) { seen[v] = true; q.add(v); }
        return seen[d];
    }

    public static void main(String[] args) {
        check("triangle 0 -> 2", true, validPath(3, new int[][]{{0, 1}, {1, 2}, {2, 0}}, 0, 2));
        check("two components 0 -> 5", false, validPath(6, new int[][]{{0, 1}, {0, 2}, {3, 5}, {5, 4}, {4, 3}}, 0, 5));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(12);
            int[][] es = new int[rnd.nextInt(n + 3)][];
            for (int i = 0; i < es.length; i++) es[i] = new int[]{rnd.nextInt(n), rnd.nextInt(n)};
            int s = rnd.nextInt(n), d = rnd.nextInt(n);
            ok &= validPath(n, es, s, d) == bfs(n, es, s, d);
        }
        check("1,000 random graphs: Union-Find == BFS", true, ok);
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
