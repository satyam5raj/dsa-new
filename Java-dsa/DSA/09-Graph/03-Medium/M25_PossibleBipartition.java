/*
 * ============================================================================
 *  MEDIUM 25 : POSSIBLE BIPARTITION          LeetCode #886
 *  https://leetcode.com/problems/possible-bipartition/
 *  Pattern: Is Graph Bipartite (M17) - build the graph from "dislikes" first
 * ============================================================================
 *
 *  PROBLEM
 *    n people (1..n), dislikes[i] = [a, b]: a and b refuse to be in the same group.
 *    Can everyone be split into TWO groups?
 *      n=4, [[1,2],[1,3],[2,4]]       -> true   ({1,4} and {2,3})
 *      n=3, [[1,2],[1,3],[2,3]]       -> false  (a triangle of dislikes)
 *
 *  ---------------------------------------------------------------------------
 *  Each "dislike" is an edge whose ends need DIFFERENT groups = different colours.
 *  So: build the undirected graph, then 2-colour it (BFS/DFS), on EVERY component.
 *  Conflict (an edge with the same colour at both ends) -> false.
 *
 *  This is a word problem around M17 - the work is spotting the bipartite check.
 *  (Union-Find alternative: for each person, union all their enemies together;
 *   if a person ends up in the same set as an enemy -> false.)
 *
 *  COMPLEXITY   Time O(n + dislikes)   Space O(n + dislikes)
 * ============================================================================
 */

import java.util.*;

public class M25_PossibleBipartition {

    static boolean possibleBipartition(int n, int[][] dislikes) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
        for (int[] d : dislikes) { adj.get(d[0]).add(d[1]); adj.get(d[1]).add(d[0]); }
        int[] group = new int[n + 1];                         // 0 = unassigned, 1 or 2
        for (int s = 1; s <= n; s++) {
            if (group[s] != 0) continue;
            group[s] = 1;
            Queue<Integer> q = new ArrayDeque<>(List.of(s));
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) {
                    if (group[v] == 0) { group[v] = 3 - group[u]; q.add(v); }   // the other group
                    else if (group[v] == group[u]) return false;
                }
            }
        }
        return true;
    }

    static boolean brute(int n, int[][] d) {
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean ok = true;
            for (int[] e : d) if ((mask >> (e[0] - 1) & 1) == (mask >> (e[1] - 1) & 1)) { ok = false; break; }
            if (ok) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        check("n=4 possible", true, possibleBipartition(4, new int[][]{{1, 2}, {1, 3}, {2, 4}}));
        check("triangle", false, possibleBipartition(3, new int[][]{{1, 2}, {1, 3}, {2, 3}}));
        check("odd cycle of 5", false, possibleBipartition(5, new int[][]{{1, 2}, {2, 3}, {3, 4}, {4, 5}, {1, 5}}));
        Random rnd = new Random(25);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            List<int[]> ds = new ArrayList<>();
            for (int i = 0; i < rnd.nextInt(n + 3); i++) { int a = 1 + rnd.nextInt(n), b = 1 + rnd.nextInt(n); if (a != b) ds.add(new int[]{a, b}); }
            int[][] d = ds.toArray(new int[0][]);
            ok &= possibleBipartition(n, d) == brute(n, d);
        }
        check("1,000 random dislike lists vs all splits", true, ok);
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
