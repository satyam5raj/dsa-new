/*
 * ============================================================================
 *  EASY 15 : PASCAL'S TRIANGLE  (+ Pascal's Triangle II)   LeetCode #118, #119
 *  https://leetcode.com/problems/pascals-triangle/
 *  Pattern: BUILD EACH ROW FROM THE PREVIOUS ONE (simple dynamic programming)
 * ============================================================================
 *
 *  PROBLEM #118: return the first numRows rows.
 *            1
 *           1 1
 *          1 2 1
 *         1 3 3 1
 *        1 4 6 4 1
 *    Each number = the sum of the two numbers directly above it. Edges are 1.
 *
 *  ---------------------------------------------------------------------------
 *  #118 LOGIC
 *    row[0] = row[last] = 1
 *    row[j] = prev[j-1] + prev[j]      for the middle positions
 *    Time O(n^2) (that's the size of the output itself), Space O(n^2) for the output.
 *
 *  PROBLEM #119: return ONLY row k (0-indexed), using O(k) extra space.
 *    Keep ONE array and update it IN-PLACE for each new row.
 *    The trick: update from RIGHT to LEFT. row[j] += row[j-1] needs the OLD
 *    row[j-1]. Going right-to-left, row[j-1] hasn't been overwritten yet.
 *
 *    k=3:  [1]
 *          [1,1]
 *          [1,2,1]      j=1: 1+1
 *          [1,3,3,1]    j=2: 1+2=3, j=1: 2+1=3   (right to left!)
 *    Going left-to-right would compute j=1: 1+2=3, then j=2: 3+1=4 -> WRONG.
 * ============================================================================
 */

import java.util.*;

public class E15_PascalsTriangle {

    static List<List<Integer>> generate(int numRows) {
        List<List<Integer>> triangle = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                if (j == 0 || j == i) {
                    row.add(1);                                    // the edges
                } else {
                    List<Integer> prev = triangle.get(i - 1);
                    row.add(prev.get(j - 1) + prev.get(j));        // the two numbers above
                }
            }
            triangle.add(row);
        }
        return triangle;
    }

    // #119: only row k, O(k) space
    static List<Integer> getRow(int rowIndex) {
        int[] row = new int[rowIndex + 1];
        row[0] = 1;
        for (int i = 1; i <= rowIndex; i++) {
            for (int j = i; j > 0; j--) {          // RIGHT to LEFT
                row[j] += row[j - 1];
            }
        }
        List<Integer> out = new ArrayList<>();
        for (int x : row) out.add(x);
        return out;
    }

    public static void main(String[] args) {
        check("generate(5)", "[[1], [1, 1], [1, 2, 1], [1, 3, 3, 1], [1, 4, 6, 4, 1]]", generate(5));
        check("generate(1)", "[[1]]", generate(1));
        check("getRow(3)", "[1, 3, 3, 1]", getRow(3));
        check("getRow(0)", "[1]", getRow(0));
        check("getRow(5)", "[1, 5, 10, 10, 5, 1]", getRow(5));
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
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
