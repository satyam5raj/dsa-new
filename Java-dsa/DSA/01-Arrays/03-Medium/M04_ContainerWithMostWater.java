/*
 * ============================================================================
 *  MEDIUM 04 : CONTAINER WITH MOST WATER     LeetCode #11
 *  https://leetcode.com/problems/container-with-most-water/
 *  Pattern: TWO POINTERS (opposite ends, greedy move of the shorter side)
 * ============================================================================
 *
 *  PROBLEM
 *    height[i] = a vertical line at x = i. Pick two lines that, with the x-axis,
 *    hold the most water.
 *      area(i, j) = min(height[i], height[j]) * (j - i)
 *      [1,8,6,2,5,4,8,3,7] -> 49   (lines at 1 and 8: min(8,7) * 7 = 49)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every pair -> O(n^2)
 *
 *  TWO POINTERS: start with the WIDEST container (left = 0, right = n-1).
 *  Every move makes the width smaller by 1, so to have ANY chance of a bigger
 *  area the height must increase. Which side should move?
 *    -> ALWAYS MOVE THE SHORTER LINE.
 *  WHY? The area is limited by the shorter line. Keeping the shorter line and
 *  moving the taller one: the width shrinks and the height can NEVER exceed the
 *  shorter line -> the area can only get worse. So every container using the
 *  shorter line with any other partner is already beaten by the current one.
 *  We can safely discard the shorter line forever.
 *
 *  DRY RUN [1,8,6,2,5,4,8,3,7]
 *    L=0(1) R=8(7)  area=1*8=8    move L (shorter)
 *    L=1(8) R=8(7)  area=7*7=49   move R
 *    L=1(8) R=7(3)  area=3*6=18   move R
 *    L=1(8) R=6(8)  area=8*5=40   equal: move either
 *    ... nothing beats 49
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  Don't confuse with "Trapping Rain Water" (Hard): there, EVERY bar holds water.
 * ============================================================================
 */

import java.util.*;

public class M04_ContainerWithMostWater {

    static int maxArea(int[] height) {
        int left = 0, right = height.length - 1, best = 0;
        while (left < right) {
            int h = Math.min(height[left], height[right]);
            best = Math.max(best, h * (right - left));
            if (height[left] < height[right]) left++;     // move the SHORTER side
            else right--;
        }
        return best;
    }

    static int maxAreaBrute(int[] height) {
        int best = 0;
        for (int i = 0; i < height.length; i++)
            for (int j = i + 1; j < height.length; j++)
                best = Math.max(best, Math.min(height[i], height[j]) * (j - i));
        return best;
    }

    public static void main(String[] args) {
        check("[1,8,6,2,5,4,8,3,7]", 49, maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}));
        check("[1,1]", 1, maxArea(new int[]{1, 1}));
        check("[4,3,2,1,4] (ends tallest)", 16, maxArea(new int[]{4, 3, 2, 1, 4}));
        check("[1,2,1]", 2, maxArea(new int[]{1, 2, 1}));
        // randomized cross-check against brute force
        Random rnd = new Random(7);
        boolean allMatch = true;
        for (int t = 0; t < 200; t++) {
            int[] h = rnd.ints(2 + rnd.nextInt(30), 0, 50).toArray();
            if (maxArea(h) != maxAreaBrute(h)) allMatch = false;
        }
        check("200 random arrays vs brute force", true, allMatch);
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
