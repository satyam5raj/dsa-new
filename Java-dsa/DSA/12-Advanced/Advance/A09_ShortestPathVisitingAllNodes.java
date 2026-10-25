/*
 * ============================================================================
 *  ADVANCE 09 : SHORTEST PATH VISITING ALL NODES  ⭐ bonus   LeetCode #847
 *  https://leetcode.com/problems/shortest-path-visiting-all-nodes/
 *  Pattern: multi-source BFS over (node, BITMASK of visited nodes)
 * ============================================================================
 *
 *  PROBLEM
 *    An undirected, connected graph with n <= 12 nodes (adjacency list). Return the
 *    length of the shortest walk that visits every node. You may start and end anywhere,
 *    and revisit nodes and edges.
 *      [[1,2,3],[0],[0],[0]]  (a star)  ->  4     e.g. 1 -> 0 -> 2 -> 0 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  WHY A BITMASK: "where am I" is not enough to describe progress - we also need "which
 *  nodes have I seen". With n <= 12 that set fits in an int: bit i = node i visited.
 *        state = (node, mask)        n * 2^n = 12 * 4096 = 49,152 states at most
 *  Every move costs 1, so BFS over states gives the shortest walk.
 *  MULTI-SOURCE: we may start anywhere, so put ALL (i, 1 << i) in the queue at distance 0.
 *  Done when mask == (1 << n) - 1.
 *
 *  ANOTHER WAY (Held-Karp, the TSP DP):
 *    1. all-pairs shortest distances (BFS from every node, or Floyd-Warshall)
 *    2. dp[mask][i] = shortest walk that has visited exactly 'mask' and ends at i
 *       dp[mask | 1<<j][j] = min(dp[mask][i] + dist[i][j])
 *    answer = min over i of dp[full][i].   O(2^n * n^2)
 *
 *  DRY RUN the star [[1,2,3],[0],[0],[0]]   full = 1111
 *    level 0: (0,0001) (1,0010) (2,0100) (3,1000)
 *    level 1: (1,0011) (2,0101) (3,1001) (0,0011) (0,0101) (0,1001)
 *    level 2: from (0,0011): (2,0111) (3,1011) ...
 *    level 3: e.g. (0,0111)
 *    level 4: (3,1111) -> 4
 *
 *  COMPLEXITY   BFS O(2^n * n^2) worst case (each state scans its neighbours)   Space O(2^n * n)
 * ============================================================================
 */

import java.util.*;

public class A09_ShortestPathVisitingAllNodes {

    // ---------------- Referee: try every order of visiting the nodes (n <= 7) ----------------
    static int shortestPathBrute(int[][] graph) {
        int n = graph.length;
        int[][] dist = allPairs(graph);
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
        int best = Integer.MAX_VALUE;
        do {
            int len = 0;
            for (int i = 0; i + 1 < n; i++) len += dist[perm[i]][perm[i + 1]];
            best = Math.min(best, len);
        } while (nextPermutation(perm));
        return best;
    }

    static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) { t = a[l]; a[l] = a[r]; a[r] = t; }
        return true;
    }

    static int[][] allPairs(int[][] graph) {                             // BFS from every node
        int n = graph.length;
        int[][] dist = new int[n][n];
        for (int s = 0; s < n; s++) {
            Arrays.fill(dist[s], -1);
            Deque<Integer> q = new ArrayDeque<>();
            q.add(s);
            dist[s][s] = 0;
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : graph[u]) if (dist[s][v] == -1) { dist[s][v] = dist[s][u] + 1; q.add(v); }
            }
        }
        return dist;
    }

    // ---------------- Approach 1: all-pairs distances + Held-Karp bitmask DP ----------------
    static int shortestPathDP(int[][] graph) {
        int n = graph.length, full = (1 << n) - 1;
        int[][] dist = allPairs(graph);
        int[][] dp = new int[1 << n][n];
        for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE);
        for (int i = 0; i < n; i++) dp[1 << i][i] = 0;
        for (int mask = 1; mask <= full; mask++)
            for (int i = 0; i < n; i++) {
                if (dp[mask][i] == Integer.MAX_VALUE) continue;
                for (int j = 0; j < n; j++) {
                    if ((mask & (1 << j)) != 0) continue;
                    int next = mask | (1 << j);
                    dp[next][j] = Math.min(dp[next][j], dp[mask][i] + dist[i][j]);
                }
            }
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) best = Math.min(best, dp[full][i]);
        return best;
    }

    // ---------------- Approach 2: BFS over (node, mask) (main solution) ----------------
    static int shortestPathLength(int[][] graph) {
        int n = graph.length, full = (1 << n) - 1;
        if (n == 1) return 0;
        boolean[][] seen = new boolean[n][1 << n];
        Deque<int[]> queue = new ArrayDeque<>();                         // {node, mask}
        for (int i = 0; i < n; i++) {                                    // multi-source: start anywhere
            queue.add(new int[]{i, 1 << i});
            seen[i][1 << i] = true;
        }
        for (int steps = 0; !queue.isEmpty(); steps++) {
            for (int size = queue.size(); size > 0; size--) {
                int[] cur = queue.poll();
                if (cur[1] == full) return steps;
                for (int next : graph[cur[0]]) {
                    int mask = cur[1] | (1 << next);
                    if (!seen[next][mask]) {
                        seen[next][mask] = true;
                        queue.add(new int[]{next, mask});
                    }
                }
            }
        }
        return -1;                                                       // unreachable for a connected graph
    }

    public static void main(String[] args) {
        check("star", 4, shortestPathLength(new int[][]{{1, 2, 3}, {0}, {0}, {0}}));
        check("example 2", 4, shortestPathLength(new int[][]{{1}, {0, 2, 4}, {1, 3, 4}, {2}, {1, 2}}));
        check("single node", 0, shortestPathLength(new int[][]{{}}));
        int[][] path12 = new int[12][];
        for (int i = 0; i < 12; i++) path12[i] = i == 0 ? new int[]{1} : i == 11 ? new int[]{10} : new int[]{i - 1, i + 1};
        check("12-node path: 11 steps", 11, shortestPathLength(path12));

        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            int[][] g = randomConnected(1 + rnd.nextInt(7), rnd);
            int want = shortestPathBrute(g);
            ok &= shortestPathLength(g) == want && shortestPathDP(g) == want;
        }
        check("400 random graphs: BFS == DP == permutations", true, ok);
        summary();
    }

    static int[][] randomConnected(int n, Random rnd) {
        List<Set<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new TreeSet<>());
        for (int i = 1; i < n; i++) { int p = rnd.nextInt(i); adj.get(i).add(p); adj.get(p).add(i); }   // random tree
        for (int e = rnd.nextInt(n + 1); e > 0; e--) {                                                  // a few extra edges
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            if (a != b) { adj.get(a).add(b); adj.get(b).add(a); }
        }
        int[][] g = new int[n][];
        for (int i = 0; i < n; i++) g[i] = adj.get(i).stream().mapToInt(Integer::intValue).toArray();
        return g;
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
