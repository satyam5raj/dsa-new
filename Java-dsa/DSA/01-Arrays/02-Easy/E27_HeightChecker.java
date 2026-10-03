/*
 * ============================================================================
 *  EASY 27 : HEIGHT CHECKER                  LeetCode #1051
 *  https://leetcode.com/problems/height-checker/
 *  Pattern: COMPARE WITH THE SORTED VERSION (counting sort for small ranges)
 * ============================================================================
 *
 *  PROBLEM
 *    Students should stand in non-decreasing order of height. Return how many
 *    positions DON'T match the expected (sorted) order.
 *      [1,1,4,2,1,3] -> 3     expected [1,1,1,2,3,4]: positions 2, 4, 5 differ
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: copy, sort, compare position by position   O(n log n)
 *
 *  APPROACH 2: COUNTING SORT (heights are only 1..100)    O(n + 100)
 *    Count each height. Then walk the original array while generating the
 *    expected sequence ON THE FLY from the counts (smallest height first).
 *    We never build the sorted array explicitly.
 *
 *  DRY RUN [1,1,4,2,1,3]  counts: 1->3, 2->1, 3->1, 4->1
 *    expected stream: 1 1 1 2 3 4
 *    actual:          1 1 4 2 1 3
 *    mismatches at positions 2 (4 vs 1), 4 (1 vs 3), 5 (3 vs 4) -> 3
 *
 *  COMPLEXITY   Time O(n + R)   Space O(R), R = 101
 * ============================================================================
 */

import java.util.*;

public class E27_HeightChecker {

    static int heightCheckerSort(int[] heights) {
        int[] expected = heights.clone();
        Arrays.sort(expected);
        int diff = 0;
        for (int i = 0; i < heights.length; i++) if (heights[i] != expected[i]) diff++;
        return diff;
    }

    static int heightChecker(int[] heights) {
        int[] count = new int[101];
        for (int h : heights) count[h]++;
        int diff = 0, current = 1;                       // current = next expected height
        for (int h : heights) {
            while (count[current] == 0) current++;        // skip heights we've used up
            if (h != current) diff++;
            count[current]--;                             // consume one of the expected height
        }
        return diff;
    }

    public static void main(String[] args) {
        check("[1,1,4,2,1,3]", 3, heightChecker(new int[]{1, 1, 4, 2, 1, 3}));
        check("[5,1,2,3,4]", 5, heightChecker(new int[]{5, 1, 2, 3, 4}));
        check("[1,2,3,4,5] (already sorted)", 0, heightChecker(new int[]{1, 2, 3, 4, 5}));
        check("sort approach agrees", 3, heightCheckerSort(new int[]{1, 1, 4, 2, 1, 3}));
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
