/*
 * ============================================================================
 *  HARD 02 : REDUNDANT CONNECTION II  ⭐ bonus   LeetCode #685
 *  https://leetcode.com/problems/redundant-connection-ii/
 *  Pattern: DIRECTED version of M01 - handle "a node with TWO parents" + cycles
 * ============================================================================
 *
 *  PROBLEM
 *    A ROOTED tree (every node has exactly one parent, except the root) plus ONE
 *    extra DIRECTED edge [u, v] (u -> v). Return the edge to remove so the rest is a
 *    rooted tree; if several work, the one appearing LAST.
 *      [[1,2],[1,3],[2,3]]               -> [2,3]   (3 has two parents)
 *      [[1,2],[2,3],[3,4],[4,1],[1,5]]   -> [4,1]   (a cycle, nobody has two parents)
 *
 *  ---------------------------------------------------------------------------
 *  The extra edge breaks the tree in one of THREE ways:
 *    CASE A: some node gets TWO parents, no cycle      -> remove the LATER parent edge
 *    CASE B: a cycle, no node with two parents
 *            (the extra edge points INTO the root)     -> remove the edge that closes the cycle
 *    CASE C: two parents AND a cycle                   -> remove the parent edge that is ON the cycle
 *
 *  ALGORITHM
 *    1. Find a node with two parent edges: cand1 (earlier) and cand2 (later).
 *    2. Union-Find over all edges EXCEPT cand2 (pretend we removed it):
 *         if an edge closes a cycle:
 *            no candidates -> CASE B: return that edge
 *            candidates    -> removing cand2 didn't help, so cand1 is on the cycle: return cand1 (CASE C)
 *    3. No cycle -> removing cand2 worked: return cand2 (CASE A)
 *
 *  (Union-Find detects cycles in the UNDIRECTED sense; that's enough here because
 *   once the two-parent issue is excluded, any cycle is a directed cycle.)
 *
 *  COMPLEXITY   Time O(n * alpha)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H02_RedundantConnectionII {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static int[] findRedundantDirectedConnection(int[][] edges) {
        int n = edges.length;
        int[] treeParent = new int[n + 1];
        int[] cand1 = null, cand2 = null;
        for (int[] e : edges) {
            if (treeParent[e[1]] != 0) {                       // e[1] already has a parent
                cand1 = new int[]{treeParent[e[1]], e[1]};
                cand2 = e;
            } else {
                treeParent[e[1]] = e[0];
            }
        }
        parent = new int[n + 1];
        for (int i = 0; i <= n; i++) parent[i] = i;
        for (int[] e : edges) {
            if (e == cand2) continue;                          // try the tree WITHOUT the later parent edge
            int a = find(e[0]), b = find(e[1]);
            if (a == b) return cand1 == null ? e : cand1;      // a cycle remains
            parent[a] = b;
        }
        return cand2;                                          // removing cand2 fixed everything
    }

    // brute force: remove edges from last to first; check the rest is a rooted tree
    static int[] brute(int[][] edges) {
        int n = edges.length;
        for (int skip = n - 1; skip >= 0; skip--) {
            int[] indeg = new int[n + 1];
            List<List<Integer>> ch = new ArrayList<>();
            for (int i = 0; i <= n; i++) ch.add(new ArrayList<>());
            for (int i = 0; i < n; i++) if (i != skip) { indeg[edges[i][1]]++; ch.get(edges[i][0]).add(edges[i][1]); }
            int root = -1, roots = 0;
            boolean bad = false;
            for (int v = 1; v <= n; v++) { if (indeg[v] == 0) { root = v; roots++; } else if (indeg[v] > 1) bad = true; }
            if (bad || roots != 1) continue;
            boolean[] seen = new boolean[n + 1];
            Deque<Integer> st = new ArrayDeque<>(List.of(root));
            seen[root] = true;
            int reached = 1;
            while (!st.isEmpty()) for (int c : ch.get(st.pop())) if (!seen[c]) { seen[c] = true; reached++; st.push(c); }
            if (reached == n) return edges[skip];
        }
        return null;
    }

    public static void main(String[] args) {
        check("two parents", "[2, 3]", findRedundantDirectedConnection(new int[][]{{1, 2}, {1, 3}, {2, 3}}));
        check("cycle through the root", "[4, 1]", findRedundantDirectedConnection(new int[][]{{1, 2}, {2, 3}, {3, 4}, {4, 1}, {1, 5}}));
        check("two parents + cycle", "[2, 1]", findRedundantDirectedConnection(new int[][]{{2, 1}, {3, 1}, {4, 2}, {1, 4}}));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 2 + rnd.nextInt(7);
            List<Integer> labels = new ArrayList<>();
            for (int i = 1; i <= n; i++) labels.add(i);
            Collections.shuffle(labels, rnd);
            List<int[]> es = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int i = 1; i < n; i++) { int p = labels.get(rnd.nextInt(i)), c = labels.get(i); es.add(new int[]{p, c}); seen.add(p + ">" + c); }   // rooted tree
            int u, v;
            do { u = 1 + rnd.nextInt(n); v = 1 + rnd.nextInt(n); } while (u == v || seen.contains(u + ">" + v));
            es.add(new int[]{u, v});
            Collections.shuffle(es, rnd);
            int[][] arr = es.toArray(new int[0][]);
            ok &= Arrays.equals(findRedundantDirectedConnection(arr), brute(arr));
        }
        check("2,000 random rooted trees + 1 edge vs brute", true, ok);
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
