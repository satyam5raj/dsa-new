/*
 * ============================================================================
 *  MEDIUM 16 : EVALUATE DIVISION             LeetCode #399
 *  https://leetcode.com/problems/evaluate-division/
 *  Pattern: WEIGHTED GRAPH of ratios; answer = PRODUCT of weights along a path
 * ============================================================================
 *
 *  PROBLEM
 *    equations [["a","b"],["b","c"]], values [2.0, 3.0] mean a/b = 2, b/c = 3.
 *    Answer queries like a/c, b/a, a/e, x/x (-1.0 if unknown).
 *      a/c = 6.0, b/a = 0.5, a/e = -1.0, a/a = 1.0, x/x = -1.0 (x never seen)
 *
 *  ---------------------------------------------------------------------------
 *  MODEL: each variable is a vertex. a/b = 2 gives TWO directed edges:
 *      a -> b with weight 2      and      b -> a with weight 1/2
 *  Then  a/c = (a/b) * (b/c): MULTIPLY the weights along ANY path from a to c.
 *  (Any path gives the same product if the equations are consistent.)
 *
 *  query(x, y):
 *    x or y unknown -> -1
 *    DFS/BFS from x, carrying the running product; reaching y -> return it
 *    unreachable (different components) -> -1
 *  Each query is O(V + E). With many queries, a WEIGHTED Union-Find (each node
 *  stores its ratio to the root) answers each in ~O(1) - see 11-Union-Find.
 *
 *  DRY RUN a/c: a -(2)-> b -(3)-> c -> 6.0
 *
 *  COMPLEXITY   Time O(Q * (V + E))   Space O(V + E)
 * ============================================================================
 */

import java.util.*;

public class M16_EvaluateDivision {

    static double[] calcEquation(List<List<String>> equations, double[] values, List<List<String>> queries) {
        Map<String, Map<String, Double>> g = new HashMap<>();
        for (int i = 0; i < equations.size(); i++) {
            String a = equations.get(i).get(0), b = equations.get(i).get(1);
            g.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);        // a / b
            g.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);  // b / a
        }
        double[] out = new double[queries.size()];
        for (int i = 0; i < queries.size(); i++) {
            String x = queries.get(i).get(0), y = queries.get(i).get(1);
            out[i] = (!g.containsKey(x) || !g.containsKey(y)) ? -1.0 : dfs(g, x, y, 1.0, new HashSet<>());
        }
        return out;
    }

    static double dfs(Map<String, Map<String, Double>> g, String cur, String target, double product, Set<String> seen) {
        if (cur.equals(target)) return product;
        seen.add(cur);
        for (Map.Entry<String, Double> e : g.get(cur).entrySet()) {
            if (seen.contains(e.getKey())) continue;
            double r = dfs(g, e.getKey(), target, product * e.getValue(), seen);
            if (r != -1.0) return r;
        }
        return -1.0;
    }

    static List<List<String>> pairs(String... s) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < s.length; i += 2) out.add(List.of(s[i], s[i + 1]));
        return out;
    }

    public static void main(String[] args) {
        double[] r = calcEquation(pairs("a", "b", "b", "c"), new double[]{2.0, 3.0},
                pairs("a", "c", "b", "a", "a", "e", "a", "a", "x", "x"));
        check("a/c, b/a, a/e, a/a, x/x", "[6.0, 0.5, -1.0, 1.0, -1.0]", Arrays.toString(r));
        double[] r2 = calcEquation(pairs("a", "b", "b", "c", "bc", "cd"), new double[]{1.5, 2.5, 5.0},
                pairs("a", "c", "c", "b", "bc", "cd", "cd", "bc"));
        check("multi-letter names", "[3.75, 0.4, 5.0, 0.2]", Arrays.toString(r2));
        // random consistent systems: give each variable a hidden value, equations are true ratios
        Random rnd = new Random(16);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 2 + rnd.nextInt(6);
            double[] val = new double[n];
            for (int i = 0; i < n; i++) val[i] = 1 + rnd.nextInt(9);
            List<List<String>> eq = new ArrayList<>();
            List<Double> vs = new ArrayList<>();
            int[] comp = new int[n];
            for (int i = 0; i < n; i++) comp[i] = i;
            for (int i = 0; i < n; i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a == b) continue;
                eq.add(List.of("v" + a, "v" + b));
                vs.add(val[a] / val[b]);
                int ca = comp[a], cb = comp[b];
                for (int j = 0; j < n; j++) if (comp[j] == cb) comp[j] = ca;     // track components
            }
            if (eq.isEmpty()) continue;
            Set<String> known = new HashSet<>();
            for (List<String> e : eq) known.addAll(e);
            int x = rnd.nextInt(n), y = rnd.nextInt(n);
            double got = calcEquation(eq, vs.stream().mapToDouble(Double::doubleValue).toArray(), List.of(List.of("v" + x, "v" + y)))[0];
            boolean answerable = known.contains("v" + x) && known.contains("v" + y) && comp[x] == comp[y];
            ok &= answerable ? Math.abs(got - val[x] / val[y]) < 1e-9 : got == -1.0;
        }
        check("300 random consistent systems", true, ok);
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
