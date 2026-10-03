/*
 * ============================================================================
 *  GRAPH BASICS 3: TOPOLOGICAL SORT (ordering a DAG)
 * ============================================================================
 *
 *  A TOPOLOGICAL ORDER of a directed graph lists the vertices so that every edge
 *  u -> v has u BEFORE v. "Do the prerequisites first."
 *    courses (Course Schedule), build systems (compile A before B), spreadsheet
 *    formulas, task pipelines, package installers, Alien Dictionary.
 *  It exists  <=>  the graph has NO directed cycle (it's a DAG). Usually not unique.
 *
 *      shirt -> tie -> jacket
 *      pants -> shoes, pants -> belt -> jacket
 *      one valid order: pants, shirt, belt, shoes, tie, jacket
 *
 *  ---------------------------------------------------------------------------
 *  KAHN'S ALGORITHM (BFS on in-degrees) - the one to know first
 *    in-degree = how many prerequisites a vertex still waits for
 *    1. queue every vertex with in-degree 0 (nothing to wait for)
 *    2. pop u, append it to the order; for each u -> v: indeg[v]--;
 *       if it hits 0, v is now free -> enqueue it
 *    3. if the order has fewer than V vertices, the rest are stuck in a CYCLE
 *  Bonus: process the queue LEVEL BY LEVEL to get "minimum number of semesters"
 *  (Parallel Courses). Use a PriorityQueue instead to get the smallest order.
 *
 *  DFS POSTORDER
 *    A vertex finishes (postorder) only after everything reachable from it has
 *    finished. So the REVERSE of the finishing order puts every u before its v.
 *    Detect cycles with the 3 colours (GraphTraversal).
 *
 *  Both are O(V + E).
 *  Run:  java TopologicalSort.java
 * ============================================================================
 */

import java.util.*;

public class TopologicalSort {

    static List<List<Integer>> graph(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);
        return adj;
    }

    // Kahn's algorithm. Returns an empty list if there's a cycle.
    static List<Integer> kahn(int n, int[][] edges) {
        List<List<Integer>> adj = graph(n, edges);
        int[] indeg = new int[n];
        for (int[] e : edges) indeg[e[1]]++;
        Queue<Integer> q = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (indeg[v] == 0) q.add(v);
        List<Integer> order = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.poll();
            order.add(u);
            for (int v : adj.get(u)) if (--indeg[v] == 0) q.add(v);   // v's last prerequisite is done
        }
        return order.size() == n ? order : List.of();
    }

    // Kahn with a min-heap: the lexicographically smallest topological order
    static List<Integer> smallestOrder(int n, int[][] edges) {
        List<List<Integer>> adj = graph(n, edges);
        int[] indeg = new int[n];
        for (int[] e : edges) indeg[e[1]]++;
        PriorityQueue<Integer> q = new PriorityQueue<>();
        for (int v = 0; v < n; v++) if (indeg[v] == 0) q.add(v);
        List<Integer> order = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.poll();
            order.add(u);
            for (int v : adj.get(u)) if (--indeg[v] == 0) q.add(v);
        }
        return order.size() == n ? order : List.of();
    }

    // Kahn level by level: the minimum number of "rounds" if each round does all ready tasks
    static int levels(int n, int[][] edges) {
        List<List<Integer>> adj = graph(n, edges);
        int[] indeg = new int[n];
        for (int[] e : edges) indeg[e[1]]++;
        Queue<Integer> q = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (indeg[v] == 0) q.add(v);
        int rounds = 0, done = 0;
        while (!q.isEmpty()) {
            rounds++;
            for (int i = q.size(); i > 0; i--) {
                int u = q.poll();
                done++;
                for (int v : adj.get(u)) if (--indeg[v] == 0) q.add(v);
            }
        }
        return done == n ? rounds : -1;
    }

    // DFS postorder, reversed. Returns an empty list on a cycle.
    static List<Integer> dfsOrder(int n, int[][] edges) {
        List<List<Integer>> adj = graph(n, edges);
        int[] color = new int[n];                       // 0 white, 1 gray, 2 black
        LinkedList<Integer> order = new LinkedList<>();
        for (int u = 0; u < n; u++) if (color[u] == 0 && !visit(adj, u, color, order)) return List.of();
        return order;
    }

    static boolean visit(List<List<Integer>> adj, int u, int[] color, LinkedList<Integer> order) {
        color[u] = 1;
        for (int v : adj.get(u)) {
            if (color[v] == 1) return false;            // back edge: cycle
            if (color[v] == 0 && !visit(adj, v, color, order)) return false;
        }
        color[u] = 2;
        order.addFirst(u);                              // prepend at finish = reversed postorder
        return true;
    }

    static boolean isValidOrder(int n, int[][] edges, List<Integer> order) {
        if (order.size() != n) return false;
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[order.get(i)] = i;
        for (int[] e : edges) if (pos[e[0]] > pos[e[1]]) return false;
        return true;
    }

    public static void main(String[] args) {
        // 0 pants, 1 shirt, 2 belt, 3 tie, 4 shoes, 5 jacket
        int[][] dressing = {{0, 4}, {0, 2}, {1, 3}, {2, 5}, {3, 5}, {1, 2}};
        List<Integer> k = kahn(6, dressing);
        System.out.println("   Kahn order: " + k + "   DFS order: " + dfsOrder(6, dressing));
        check("Kahn order is valid", true, isValidOrder(6, dressing, k));
        check("DFS order is valid", true, isValidOrder(6, dressing, dfsOrder(6, dressing)));
        check("smallest order", "[0, 1, 2, 3, 4, 5]", smallestOrder(6, dressing));
        check("rounds needed (parallel)", 3, levels(6, dressing));

        int[][] cyclic = {{0, 1}, {1, 2}, {2, 0}, {2, 3}};
        check("cycle -> Kahn returns []", "[]", kahn(4, cyclic));
        check("cycle -> DFS returns []", "[]", dfsOrder(4, cyclic));
        check("cycle -> rounds -1", -1, levels(4, cyclic));

        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(10);
            List<int[]> es = new ArrayList<>();
            boolean dag = rnd.nextBoolean();
            for (int i = 0; i < n * 2; i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a == b) continue;
                if (dag && a > b) { int x = a; a = b; b = x; }       // only low -> high: guaranteed DAG
                es.add(new int[]{a, b});
            }
            int[][] e = es.toArray(new int[0][]);
            List<Integer> a = kahn(n, e), b = dfsOrder(n, e);
            ok &= a.isEmpty() == b.isEmpty();                        // both agree on "has a cycle"
            if (dag) ok &= !a.isEmpty();
            if (!a.isEmpty()) ok &= isValidOrder(n, e, a) && isValidOrder(n, e, b);
        }
        check("500 random graphs: Kahn and DFS agree", true, ok);
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
