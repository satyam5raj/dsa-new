/*
 * ============================================================================
 *  UNION-FIND VARIANTS: the tricks that show up in harder problems
 * ============================================================================
 *
 *  1. ELEMENTS THAT AREN'T 0..n-1 (strings, emails, coordinates)
 *     Use HashMaps for parent (and size), creating entries on first sight.
 *     Or map each key to an int id first (Map<String, Integer>) and use arrays.
 *     -> Accounts Merge (M02), Similar groups, Smallest Equivalent String (M07)
 *
 *  2. GRID CELLS: cell (r, c) -> id r * cols + c. Extra "virtual" nodes are handy:
 *     a node for "the border", "the top row", "the ocean"...
 *     -> Regions Cut by Slashes (M05), Number of Islands II (H01)
 *
 *  3. WEIGHTED UNION-FIND: each node also stores a value RELATIVE to its parent
 *     (a ratio, a difference, a parity). find() accumulates it along the path, and
 *     path compression must update it. -> Evaluate Division (ratios), "is the
 *     parity consistent?" (bipartite checks online)
 *     For Evaluate Division:  weight[x] = x / parent[x]
 *        find(x) returns the root and sets weight[x] = x / root
 *        union(a, b, k) meaning a / b = k:  attach root(a) under root(b) with
 *          weight[ra] = ra / rb = (a / rb) / (a / ra) = (k * w[b]) / w[a]
 *        query a / b = w[a] / w[b] if they share a root
 *
 *  4. OFFLINE + SORTING: if queries ask "connected using only edges with
 *     weight < limit?", sort edges and queries by weight; add edges as the limit
 *     grows. The DSU only ever grows - it never needs to "un-union".
 *     -> Kruskal's MST, Edge-Length-Limited Paths (H03)
 *
 *  This file implements 1 and 3, with tests.
 *  Run:  java UnionFindVariants.java
 * ============================================================================
 */

import java.util.*;

public class UnionFindVariants {

    // ---------------- 1. Generic DSU over any keys, with HashMaps ----------------
    static class MapDSU<T> {
        private final Map<T, T> parent = new HashMap<>();
        private final Map<T, Integer> size = new HashMap<>();

        T find(T x) {
            if (!parent.containsKey(x)) { parent.put(x, x); size.put(x, 1); }   // create on first sight
            T root = x;
            while (!parent.get(root).equals(root)) root = parent.get(root);
            while (!parent.get(x).equals(root)) {                               // full path compression
                T next = parent.get(x);
                parent.put(x, root);
                x = next;
            }
            return root;
        }

        boolean union(T a, T b) {
            T ra = find(a), rb = find(b);
            if (ra.equals(rb)) return false;
            if (size.get(ra) < size.get(rb)) { T t = ra; ra = rb; rb = t; }
            parent.put(rb, ra);
            size.put(ra, size.get(ra) + size.get(rb));
            return true;
        }

        Map<T, List<T>> groups() {
            Map<T, List<T>> g = new TreeMap<>();
            for (T x : parent.keySet()) g.computeIfAbsent(find(x), k -> new ArrayList<>()).add(x);
            for (List<T> l : g.values()) l.sort(null);
            return g;
        }
    }

    // ---------------- 3. Weighted DSU: weight[x] = x / parent[x] ----------------
    static class RatioDSU {
        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Double> weight = new HashMap<>();     // x / parent[x]

        boolean contains(String x) { return parent.containsKey(x); }

        String find(String x) {
            if (!parent.containsKey(x)) { parent.put(x, x); weight.put(x, 1.0); }
            String p = parent.get(x);
            if (p.equals(x)) return x;
            String root = find(p);                                      // now weight[p] = p / root
            weight.put(x, weight.get(x) * weight.get(p));               // x/root = x/p * p/root
            parent.put(x, root);                                        // compress
            return root;
        }

        // record a / b = k
        void union(String a, String b, double k) {
            String ra = find(a), rb = find(b);
            if (ra.equals(rb)) return;
            parent.put(ra, rb);
            weight.put(ra, k * weight.get(b) / weight.get(a));          // ra/rb = (a/rb) / (a/ra)
        }

        double query(String a, String b) {
            if (!contains(a) || !contains(b)) return -1.0;
            if (!find(a).equals(find(b))) return -1.0;
            return weight.get(a) / weight.get(b);                       // (a/root) / (b/root)
        }
    }

    public static void main(String[] args) {
        System.out.println("1. DSU over strings");
        MapDSU<String> synonyms = new MapDSU<>();
        synonyms.union("happy", "glad");
        synonyms.union("glad", "joyful");
        synonyms.union("sad", "unhappy");
        synonyms.find("angry");
        List<String> groups = new ArrayList<>();
        for (List<String> g : synonyms.groups().values()) groups.add(g.toString());
        Collections.sort(groups);
        check("groups", "[[angry], [glad, happy, joyful], [sad, unhappy]]", groups);
        check("happy ~ joyful", true, synonyms.find("happy").equals(synonyms.find("joyful")));
        check("happy ~ sad", false, synonyms.find("happy").equals(synonyms.find("sad")));

        System.out.println("2. Weighted DSU (ratios)");
        RatioDSU r = new RatioDSU();
        r.union("a", "b", 2.0);        // a / b = 2
        r.union("b", "c", 3.0);        // b / c = 3
        check("a / c", 6.0, r.query("a", "c"));
        check("c / a", 1.0 / 6.0, r.query("c", "a"));
        check("a / x (unknown)", -1.0, r.query("a", "x"));

        System.out.println("3. Random consistent systems: weighted DSU == hidden true values");
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 2 + rnd.nextInt(10);
            double[] val = new double[n];
            for (int i = 0; i < n; i++) val[i] = 1 + rnd.nextInt(20);
            RatioDSU w = new RatioDSU();
            MapDSU<Integer> plain = new MapDSU<>();
            for (int e = 0; e < n; e++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                w.union("v" + a, "v" + b, val[a] / val[b]);
                plain.union(a, b);
            }
            for (int q = 0; q < 10; q++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                double got = w.query("v" + a, "v" + b);
                boolean known = w.contains("v" + a) && w.contains("v" + b) && plain.find(a).equals(plain.find(b));
                ok &= known ? Math.abs(got - val[a] / val[b]) < 1e-9 : got == -1.0;
            }
        }
        check("3,000 random ratio queries", true, ok);
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
