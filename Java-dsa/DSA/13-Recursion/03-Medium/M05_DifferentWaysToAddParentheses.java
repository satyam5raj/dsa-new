/*
 * ============================================================================
 *  MEDIUM 05 : DIFFERENT WAYS TO ADD PARENTHESES   LeetCode #241
 *  https://leetcode.com/problems/different-ways-to-add-parentheses/
 *  Pattern: DIVIDE AND CONQUER at every operator (+ memoization)
 * ============================================================================
 *
 *  PROBLEM
 *    An expression of numbers and + - *. Return ALL results of all possible ways to
 *    fully parenthesize it (any order, duplicates kept).
 *      "2-1-1"   -> [0, 2]          ((2-1)-1) = 0, (2-(1-1)) = 2
 *      "2*3-4*5" -> [-34, -14, -10, -10, 10]
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: in any full parenthesization, SOME operator is applied LAST. Try
 *  every operator as the last one:
 *     ways(expr):
 *       if expr is just a number -> [number]
 *       for each operator at position i:
 *          left  = ways(expr[0..i))        <- all values of the left side
 *          right = ways(expr(i..])         <- all values of the right side
 *          for each l in left, r in right: add (l op r)
 *  Divide at the operator, conquer both sides, COMBINE by trying all pairs.
 *
 *  MEMOIZATION: the same substring (e.g. "3-4") appears under many splits.
 *  Cache substring -> list of results. (The number of results is a Catalan number,
 *  so the output itself can be exponential; memoization avoids recomputing it.)
 *
 *  DRY RUN "2-1-1"
 *    last op = first '-': ways("2") = [2], ways("1-1") = [0]   -> 2 - 0 = 2
 *    last op = second '-': ways("2-1") = [1], ways("1") = [1]  -> 1 - 1 = 0
 *
 *  COMPLEXITY   Time O(Catalan(n) * n) roughly   Space O(n^2) memo entries
 * ============================================================================
 */

import java.util.*;

public class M05_DifferentWaysToAddParentheses {

    static List<Integer> diffWaysToCompute(String expression) {
        return ways(expression, new HashMap<>());
    }

    static List<Integer> ways(String expr, Map<String, List<Integer>> memo) {
        if (memo.containsKey(expr)) return memo.get(expr);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c != '+' && c != '-' && c != '*') continue;
            List<Integer> left = ways(expr.substring(0, i), memo);        // all values of the left part
            List<Integer> right = ways(expr.substring(i + 1), memo);      // all values of the right part
            for (int l : left) for (int r : right) result.add(c == '+' ? l + r : c == '-' ? l - r : l * r);
        }
        if (result.isEmpty()) result.add(Integer.parseInt(expr));          // no operator: a number
        memo.put(expr, result);
        return result;
    }

    // referee: enumerate full binary trees over the tokens, no memo, evaluate each
    static List<Integer> brute(List<Integer> nums, List<Character> ops, int lo, int hi) {
        if (lo == hi) return List.of(nums.get(lo));
        List<Integer> out = new ArrayList<>();
        for (int k = lo; k < hi; k++) {
            for (int l : brute(nums, ops, lo, k)) for (int r : brute(nums, ops, k + 1, hi)) {
                char c = ops.get(k);
                out.add(c == '+' ? l + r : c == '-' ? l - r : l * r);
            }
        }
        return out;
    }

    static List<Integer> sorted(List<Integer> l) { List<Integer> c = new ArrayList<>(l); Collections.sort(c); return c; }

    public static void main(String[] args) {
        check("2-1-1", "[0, 2]", sorted(diffWaysToCompute("2-1-1")));
        check("2*3-4*5", "[-34, -14, -10, -10, 10]", sorted(diffWaysToCompute("2*3-4*5")));
        check("11 (single number)", "[11]", diffWaysToCompute("11"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int k = 1 + rnd.nextInt(6);
            List<Integer> nums = new ArrayList<>();
            List<Character> ops = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < k; i++) {
                int v = rnd.nextInt(20);
                nums.add(v);
                sb.append(v);
                if (i < k - 1) { char o = "+-*".charAt(rnd.nextInt(3)); ops.add(o); sb.append(o); }
            }
            ok &= sorted(diffWaysToCompute(sb.toString())).equals(sorted(brute(nums, ops, 0, k - 1)));
        }
        check("300 random expressions vs tree enumeration", true, ok);
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
