/*
 * ============================================================================
 *  MEDIUM 10 : SPIRAL MATRIX                 LeetCode #54
 *  https://leetcode.com/problems/spiral-matrix/
 *  Pattern: SHRINKING BOUNDARIES (simulation)
 * ============================================================================
 *
 *  PROBLEM
 *    Return all elements in spiral order (clockwise, starting top-left).
 *      1  2  3  4
 *      5  6  7  8      ->  [1,2,3,4,8,12,11,10,9,5,6,7]
 *      9 10 11 12
 *
 *  ---------------------------------------------------------------------------
 *  FOUR BOUNDARIES: top, bottom, left, right. Repeat while top <= bottom && left <= right:
 *    1. go RIGHT along the top row      (left..right),    then top++
 *    2. go DOWN  along the right column (top..bottom),    then right--
 *    3. go LEFT  along the bottom row   (right..left),    then bottom--   (only if top <= bottom!)
 *    4. go UP    along the left column  (bottom..top),    then left++     (only if left <= right!)
 *
 *  WHY THE EXTRA CHECKS in steps 3 and 4? For a single remaining ROW, step 1
 *  already read it. Without the check, step 3 would read it again backwards.
 *  Same for a single remaining COLUMN in step 4. Test with a 1x4 or 4x1 matrix to see the bug.
 *
 *  DRY RUN (the 3x4 above)
 *    right: 1 2 3 4           top=1
 *    down : 8 12              right=2
 *    left : 11 10 9           bottom=1
 *    up   : 5                 left=1
 *    right: 6 7               top=2 > bottom=1 -> stop
 *
 *  COMPLEXITY   Time O(m * n)   Space O(1) extra (the output doesn't count)
 * ============================================================================
 */

import java.util.*;

public class M10_SpiralMatrix {

    static List<Integer> spiralOrder(int[][] matrix) {
        List<Integer> out = new ArrayList<>();
        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;

        while (top <= bottom && left <= right) {
            for (int j = left; j <= right; j++) out.add(matrix[top][j]);      // 1. right
            top++;
            for (int i = top; i <= bottom; i++) out.add(matrix[i][right]);    // 2. down
            right--;
            if (top <= bottom) {
                for (int j = right; j >= left; j--) out.add(matrix[bottom][j]); // 3. left
                bottom--;
            }
            if (left <= right) {
                for (int i = bottom; i >= top; i--) out.add(matrix[i][left]);   // 4. up
                left++;
            }
        }
        return out;
    }

    public static void main(String[] args) {
        check("3x3", "[1, 2, 3, 6, 9, 8, 7, 4, 5]", spiralOrder(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));
        check("3x4", "[1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]", spiralOrder(new int[][]{{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}}));
        check("1x4 (single row)", "[1, 2, 3, 4]", spiralOrder(new int[][]{{1, 2, 3, 4}}));
        check("4x1 (single column)", "[1, 2, 3, 4]", spiralOrder(new int[][]{{1}, {2}, {3}, {4}}));
        check("1x1", "[7]", spiralOrder(new int[][]{{7}}));
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
