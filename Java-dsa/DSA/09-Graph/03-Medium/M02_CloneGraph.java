/*
 * ============================================================================
 *  MEDIUM 02 : CLONE GRAPH                   LeetCode #133
 *  https://leetcode.com/problems/clone-graph/
 *  Pattern: DFS/BFS + HASHMAP original -> copy (the map is also the visited set)
 * ============================================================================
 *
 *  PROBLEM
 *    Deep-copy a connected undirected graph given one node. Each Node has val and
 *    a list of neighbors.  1 - 2
 *                          |   |
 *                          4 - 3
 *
 *  ---------------------------------------------------------------------------
 *  THE DIFFICULTY: cycles. A naive recursive copy of 1 copies 2, which copies its
 *  neighbour 1 again, which copies 2... forever. And even without cycles, a node
 *  reachable two ways must be copied ONCE (shared, not duplicated).
 *
 *  FIX: a map  original -> its copy.
 *    clone(node):
 *      if node is in the map -> return the existing copy   (handles cycles + sharing)
 *      copy = new Node(node.val); map.put(node, copy)      <- BEFORE recursing!
 *      for each neighbour: copy.neighbors.add(clone(neighbour))
 *  Putting the copy in the map BEFORE visiting neighbours is what breaks the cycle.
 *
 *  Same idea as Copy List with Random Pointer (Linked Lists): "old -> new" map.
 *
 *  COMPLEXITY   Time O(V + E)   Space O(V)
 * ============================================================================
 */

import java.util.*;

public class M02_CloneGraph {

    static class Node {
        int val;
        List<Node> neighbors = new ArrayList<>();
        Node(int val) { this.val = val; }
    }

    static Node cloneGraph(Node node) {
        return clone(node, new HashMap<>());
    }

    static Node clone(Node node, Map<Node, Node> copies) {
        if (node == null) return null;
        Node existing = copies.get(node);
        if (existing != null) return existing;           // already copied (cycle or shared node)
        Node copy = new Node(node.val);
        copies.put(node, copy);                          // register BEFORE recursing
        for (Node nb : node.neighbors) copy.neighbors.add(clone(nb, copies));
        return copy;
    }

    // Build from LeetCode's adjacency list format: adj[i] = neighbours of node i+1
    static Node build(int[][] adj) {
        if (adj.length == 0) return null;
        Node[] nodes = new Node[adj.length];
        for (int i = 0; i < adj.length; i++) nodes[i] = new Node(i + 1);
        for (int i = 0; i < adj.length; i++) for (int v : adj[i]) nodes[i].neighbors.add(nodes[v - 1]);
        return nodes[0];
    }

    // Serialize back to the adjacency format, and collect all node objects
    static String serialize(Node start, Set<Node> objects) {
        if (start == null) return "[]";
        TreeMap<Integer, List<Integer>> adj = new TreeMap<>();
        Deque<Node> stack = new ArrayDeque<>(List.of(start));
        Set<Node> seen = new HashSet<>(List.of(start));
        while (!stack.isEmpty()) {
            Node n = stack.pop();
            objects.add(n);
            List<Integer> nb = new ArrayList<>();
            for (Node m : n.neighbors) { nb.add(m.val); if (seen.add(m)) stack.push(m); }
            adj.put(n.val, nb);
        }
        return adj.values().toString();
    }

    public static void main(String[] args) {
        Node original = build(new int[][]{{2, 4}, {1, 3}, {2, 4}, {1, 3}});
        Node copy = cloneGraph(original);
        Set<Node> a = new HashSet<>(), b = new HashSet<>();
        check("same structure", serialize(original, a), serialize(copy, b));
        a.retainAll(b);
        check("no shared node objects (deep copy)", 0, a.size());
        check("single node", "[[]]", serialize(cloneGraph(build(new int[][]{{}})), new HashSet<>()));
        check("empty graph", "[]", serialize(cloneGraph(null), new HashSet<>()));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(10);
            List<Set<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < n; i++) adj.add(new TreeSet<>());
            for (int i = 1; i < n; i++) { int p = rnd.nextInt(i); adj.get(i).add(p + 1); adj.get(p).add(i + 1); }   // connected
            for (int i = 0; i < n; i++) { int x = rnd.nextInt(n), y = rnd.nextInt(n); if (x != y) { adj.get(x).add(y + 1); adj.get(y).add(x + 1); } }
            int[][] arr = adj.stream().map(s -> s.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
            Node o = build(arr);
            Set<Node> s1 = new HashSet<>(), s2 = new HashSet<>();
            ok &= serialize(o, s1).equals(serialize(cloneGraph(o), s2));
            s1.retainAll(s2);
            ok &= s1.isEmpty();
        }
        check("300 random graphs cloned", true, ok);
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
