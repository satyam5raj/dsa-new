/*
 * ============================================================================
 *  UNION-FIND (DISJOINT SET UNION, DSU) FROM SCRATCH
 * ============================================================================
 *
 *  WHAT IT DOES: keeps elements 0..n-1 split into DISJOINT groups and supports
 *    union(a, b)      merge the groups of a and b
 *    find(a)          which group is a in? (returns the group's REPRESENTATIVE / root)
 *    connected(a, b)  find(a) == find(b)
 *  ...in nearly O(1) per operation, while edges keep ARRIVING. BFS/DFS would have
 *  to re-traverse after every new edge.
 *
 *  REPRESENTATION: a forest. parent[x] = x's parent; a root has parent[x] == x.
 *  find(x) = follow parents up to the root.
 *
 *      union(0,1), union(2,3), union(1,3):      0          parent = [0, 0, 0, 2]
 *                                               |\
 *                                               1 2
 *                                                 |
 *                                                 3
 *
 *  ---------------------------------------------------------------------------
 *  THE EVOLUTION (each step measured in main)
 *  1. QUICK-UNION, naive: union(a, b) = parent[find(a)] = find(b).
 *     Trees can become long CHAINS -> find is O(n).
 *  2. UNION BY SIZE (or RANK): hang the SMALLER tree under the BIGGER one.
 *     A node's depth only grows when its tree at least DOUBLES in size -> depth
 *     <= log2(n). find is O(log n).
 *  3. PATH COMPRESSION: during find, point every visited node DIRECTLY at the root.
 *     The next find on them is O(1). ("Path halving" = point each node at its
 *     grandparent: one pass, almost as good.)
 *  4. BOTH TOGETHER: amortized O(alpha(n)) per operation, where alpha is the
 *     inverse Ackermann function: alpha(n) <= 4 for any n you'll ever see.
 *     -> "effectively constant".
 *
 *  WHEN TO REACH FOR IT
 *    - connectivity with edges arriving over time (online)
 *    - "group things that are equivalent" (accounts, equations, synonyms)
 *    - Kruskal's MST, cycle detection while adding undirected edges
 *    - counting components as you go (count-- on each successful union)
 *  WHAT IT CAN'T DO: un-union (splitting groups), shortest paths, directed reachability.
 *
 *  Run:  java UnionFindBasics.java
 * ============================================================================
 */

import java.util.*;

public class UnionFindBasics {

    // ---------------- the version you should write in interviews ----------------
    static class DSU {
        private final int[] parent, size;
        private int components;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];
            for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }   // everyone is their own group
            components = n;
        }

        int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];          // path halving: skip a level as we climb
                x = parent[x];
            }
            return x;
        }

        // returns false if a and b were ALREADY connected (useful for cycle detection)
        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }   // ra = the bigger tree
            parent[rb] = ra;                            // small under big
            size[ra] += size[rb];
            components--;
            return true;
        }

        boolean connected(int a, int b) { return find(a) == find(b); }
        int sizeOf(int x) { return size[find(x)]; }
        int components() { return components; }
    }

    // ---------------- for the experiment: naive quick-union ----------------
    static class NaiveUnion {
        final int[] parent;
        long steps;                                     // parent hops performed by find
        NaiveUnion(int n) { parent = new int[n]; for (int i = 0; i < n; i++) parent[i] = i; }
        int find(int x) { while (parent[x] != x) { x = parent[x]; steps++; } return x; }
        void union(int a, int b) { parent[find(a)] = find(b); }
    }

    static class BySizeOnly {
        final int[] parent, size;
        long steps;
        BySizeOnly(int n) { parent = new int[n]; size = new int[n]; for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; } }
        int find(int x) { while (parent[x] != x) { x = parent[x]; steps++; } return x; }
        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            size[ra] += size[rb];
        }
    }

    static class Full {                                 // size + path compression, counting steps
        final int[] parent, size;
        long steps;
        Full(int n) { parent = new int[n]; size = new int[n]; for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; } }
        int find(int x) {
            while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; steps++; }
            return x;
        }
        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            size[ra] += size[rb];
        }
    }

    public static void main(String[] args) {
        System.out.println("1. Basic operations");
        DSU d = new DSU(6);
        d.union(0, 1);
        d.union(2, 3);
        check("0 ~ 1", true, d.connected(0, 1));
        check("1 ~ 2", false, d.connected(1, 2));
        d.union(1, 3);
        check("after union(1,3): 0 ~ 2", true, d.connected(0, 2));
        check("union(0,3) again -> false (already joined)", false, d.union(0, 3));
        check("size of 0's group", 4, d.sizeOf(0));
        check("components", 3, d.components());

        System.out.println("2. Total parent hops for find(i) on EVERY element");
        int n = 1 << 14;                                // 16,384
        // build A: a chain  union(0,1), union(1,2), ...   (naive union makes a path of length n)
        NaiveUnion naive = new NaiveUnion(n);
        BySizeOnly sizeA = new BySizeOnly(n);
        Full fullA = new Full(n);
        for (int i = 1; i < n; i++) { naive.union(i - 1, i); sizeA.union(i - 1, i); fullA.union(i - 1, i); }
        naive.steps = sizeA.steps = fullA.steps = 0;
        for (int i = 0; i < n; i++) { naive.find(i); sizeA.find(i); fullA.find(i); }
        System.out.printf("   chain build:    naive %,d   by size %,d   size + compression %,d%n", naive.steps, sizeA.steps, fullA.steps);
        check("naive: ~n^2/2 hops (a long path)", true, naive.steps > (long) n * n / 4);
        check("by size: the chain becomes a star", true, sizeA.steps <= n);

        // build B: merge equal-sized trees (pairs, then quads, ...): the WORST case for union by size
        BySizeOnly sizeB = new BySizeOnly(n);
        Full fullB = new Full(n);
        for (int step = 1; step < n; step *= 2)
            for (int i = 0; i + step < n; i += 2 * step) { sizeB.union(i + step, i); fullB.union(i + step, i); }
        sizeB.steps = fullB.steps = 0;
        int maxDepth = 0;
        for (int i = 0; i < n; i++) {
            long before = sizeB.steps;
            sizeB.find(i);
            maxDepth = Math.max(maxDepth, (int) (sizeB.steps - before));
            fullB.find(i);
        }
        System.out.printf("   doubling build: by size %,d (max depth %d = log2 n)   size + compression %,d%n", sizeB.steps, maxDepth, fullB.steps);
        check("by size: depth never exceeds log2(n) = 14", true, maxDepth <= 14);
        check("compression beats size alone", true, fullB.steps < sizeB.steps);

        System.out.println("3. Random check: components == BFS components");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int m = 1 + rnd.nextInt(30);
            DSU u = new DSU(m);
            List<List<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < m; i++) adj.add(new ArrayList<>());
            for (int e = 0; e < rnd.nextInt(40); e++) {
                int a = rnd.nextInt(m), b = rnd.nextInt(m);
                u.union(a, b);
                adj.get(a).add(b);
                adj.get(b).add(a);
                // after EVERY edge, compare (this is the online advantage)
                int[] comp = new int[m];
                Arrays.fill(comp, -1);
                int c = 0;
                for (int s = 0; s < m; s++) {
                    if (comp[s] != -1) continue;
                    Deque<Integer> st = new ArrayDeque<>(List.of(s));
                    comp[s] = c;
                    while (!st.isEmpty()) for (int v : adj.get(st.pop())) if (comp[v] == -1) { comp[v] = c; st.push(v); }
                    c++;
                }
                ok &= c == u.components();
                int x = rnd.nextInt(m), y = rnd.nextInt(m);
                ok &= u.connected(x, y) == (comp[x] == comp[y]);
            }
        }
        check("300 random graphs, checked after every edge", true, ok);
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
