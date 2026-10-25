/*
 * ============================================================================
 *  ADVANCE 27 : LARGEST COLOR VALUE IN A DIRECTED GRAPH  ⭐ bonus   LeetCode #1857
 *  https://leetcode.com/problems/largest-color-value-in-a-directed-graph/
 *  Pattern: TOPOLOGICAL SORT (Kahn, queue) + DP table count[node][color] + cycle check
 * ============================================================================
 *
 *  PROBLEM
 *    A directed graph; node i has colour colors.charAt(i) ('a'..'z'). The "colour value"
 *    of a path is how many times its MOST frequent colour appears on it. Return the
 *    largest colour value over all paths, or -1 if the graph has a cycle.
 *      colors "abaca", edges [[0,1],[0,2],[2,3],[3,4]]  ->  3    path 0 -> 2 -> 3 -> 4
 *                                                            has colours a, a, c, a
 *
 *  ---------------------------------------------------------------------------
 *  WHY -1 FOR A CYCLE: you could loop forever and make a colour count as big as you like.
 *
 *  DP ON A DAG: count[v][c] = the most times colour c can appear on a path ENDING at v.
 *    count[v][c] = max over edges u -> v of count[u][c]   (+1 if v itself has colour c)
 *  To use count[u] we must have FINISHED u first: process nodes in TOPOLOGICAL ORDER.
 *
 *  KAHN'S ALGORITHM does both jobs at once:
 *    - start with every node of indegree 0; pop a node, "remove" its out-edges, push
 *      nodes whose indegree drops to 0 -> a topological order
 *    - if fewer than n nodes are ever popped, the rest sit on a CYCLE -> return -1
 *  When u is popped: add its own colour, then push its counts forward to each child.
 *
 *  A NICE DETAIL: we only check count[u][own colour of u] for the answer. The best path
 *  for any colour c ends (at its last c) on a node whose own colour IS c, and that node
 *  gets checked when it is popped.
 *
 *  DRY RUN "abaca"   0(a) -> 1(b), 0 -> 2(a) -> 3(c) -> 4(a)
 *    pop 0: a=1 (best 1); push to 1 and 2
 *    pop 1: b=1          pop 2: a=2 (best 2)       pop 3: c=1, a stays 2
 *    pop 4: a=3 (best 3)                            all 5 popped -> 3
 *
 *  COMPLEXITY   O((n + m) * 26)   Space O(n * 26)
 * ============================================================================
 */

import java.util.*;

public class A27_LargestColorValueInDirectedGraph {

    // ---------------- Referee: DFS cycle check, then walk every path (tiny graphs) ----------------
    static int largestPathValueBrute(String colors, int[][] edges) {
        int n = colors.length();
        List<List<Integer>> adj = adjacency(n, edges);
        int[] state = new int[n];                                         // 0 new, 1 on the stack, 2 done
        for (int i = 0; i < n; i++) if (hasCycle(adj, i, state)) return -1;
        int best = 0;
        for (int s = 0; s < n; s++) best = Math.max(best, walk(colors, adj, s, new int[26]));
        return best;
    }

    private static boolean hasCycle(List<List<Integer>> adj, int u, int[] state) {
        if (state[u] == 1) return true;                                   // back to a node on the current path
        if (state[u] == 2) return false;
        state[u] = 1;
        for (int v : adj.get(u)) if (hasCycle(adj, v, state)) return true;
        state[u] = 2;
        return false;
    }

    private static int walk(String colors, List<List<Integer>> adj, int u, int[] count) {
        int c = colors.charAt(u) - 'a';
        count[c]++;
        int best = Arrays.stream(count).max().getAsInt();
        for (int v : adj.get(u)) best = Math.max(best, walk(colors, adj, v, count));
        count[c]--;                                                       // undo before returning
        return best;
    }

    static List<List<Integer>> adjacency(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);
        return adj;
    }

    // ---------------- Main solution: Kahn's topological sort + count[node][colour] ----------------
    static int largestPathValue(String colors, int[][] edges) {
        int n = colors.length();
        List<List<Integer>> adj = adjacency(n, edges);
        int[] indegree = new int[n];
        for (int[] e : edges) indegree[e[1]]++;

        int[][] count = new int[n][26];                                   // best count of each colour on a path ending here
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) queue.add(i);

        int processed = 0, best = 0;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            processed++;
            int own = colors.charAt(u) - 'a';
            count[u][own]++;                                              // u itself is on every path ending at u
            best = Math.max(best, count[u][own]);
            for (int v : adj.get(u)) {
                for (int c = 0; c < 26; c++) count[v][c] = Math.max(count[v][c], count[u][c]);
                if (--indegree[v] == 0) queue.add(v);
            }
        }
        return processed == n ? best : -1;                                // unprocessed nodes are stuck on a cycle
    }

    public static void main(String[] args) {
        check("example 1", 3, largestPathValue("abaca", new int[][]{{0, 1}, {0, 2}, {2, 3}, {3, 4}}));
        check("self loop -> cycle", -1, largestPathValue("a", new int[][]{{0, 0}}));
        check("no edges", 1, largestPathValue("abc", new int[][]{}));
        check("two paths merge", 3, largestPathValue("aaba", new int[][]{{0, 2}, {1, 2}, {2, 3}, {0, 1}}));
        check("cycle elsewhere still -1", -1, largestPathValue("abcd", new int[][]{{0, 1}, {2, 3}, {3, 2}}));

        Random rnd = new Random(27);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 1 + rnd.nextInt(7);
            StringBuilder col = new StringBuilder();
            for (int i = 0; i < n; i++) col.append((char) ('a' + rnd.nextInt(3)));
            boolean acyclic = rnd.nextBoolean();                          // half the time force a DAG (edges go low -> high)
            List<int[]> list = new ArrayList<>();
            for (int i = rnd.nextInt(10); i > 0; i--) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (acyclic && a == b) continue;
                list.add(acyclic ? new int[]{Math.min(a, b), Math.max(a, b)} : new int[]{a, b});
            }
            int[][] edges = list.toArray(new int[0][]);
            ok &= largestPathValue(col.toString(), edges) == largestPathValueBrute(col.toString(), edges);
        }
        check("2,000 random graphs vs DFS of every path", true, ok);
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
