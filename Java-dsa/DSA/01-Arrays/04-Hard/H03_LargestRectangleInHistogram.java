/*
 * ============================================================================
 *  HARD 03 : LARGEST RECTANGLE IN HISTOGRAM  LeetCode #84
 *  https://leetcode.com/problems/largest-rectangle-in-histogram/
 *  Pattern: MONOTONIC STACK (increasing) - "nearest smaller element"
 * ============================================================================
 *
 *  PROBLEM
 *    heights[i] = a bar of width 1. Return the area of the largest rectangle
 *    that fits inside the histogram.
 *      [2,1,5,6,2,3] -> 10      (bars 5 and 6, height 5, width 2)
 *
 *  ---------------------------------------------------------------------------
 *  THE KEY QUESTION: for each bar i, imagine the rectangle whose height is
 *  EXACTLY heights[i]. How far can it stretch?
 *    left  boundary = the nearest bar to the LEFT  that is SHORTER than heights[i]
 *    right boundary = the nearest bar to the RIGHT that is SHORTER than heights[i]
 *    width = right - left - 1,   area = heights[i] * width
 *  The answer is the max over all i (the best rectangle's height equals SOME bar's height).
 *
 *  BRUTE FORCE: for each i, walk left and right until a shorter bar -> O(n^2)
 *
 *  MONOTONIC STACK (indices with INCREASING heights)
 *    Scan left to right. When the current bar is SHORTER than the bar on top of
 *    the stack, that top bar has just found its RIGHT boundary (the current index).
 *    And its LEFT boundary is the element BELOW it in the stack (the stack is
 *    increasing, so that's the nearest shorter bar to the left).
 *    -> pop it and compute its area right away.
 *    At the end, append a sentinel bar of height 0 so everything gets popped.
 *
 *  DRY RUN [2,1,5,6,2,3] (+ sentinel 0 at index 6). Stack shows indices(heights).
 *    i=0 h=2  push                         [0(2)]
 *    i=1 h=1  pop 0(2): left=-1, right=1 -> width 1, area 2       [1(1)]
 *    i=2 h=5  push                         [1(1),2(5)]
 *    i=3 h=6  push                         [1(1),2(5),3(6)]
 *    i=4 h=2  pop 3(6): left=2, right=4 -> width 1, area 6
 *             pop 2(5): left=1, right=4 -> width 2, area 10  <- best
 *             push                         [1(1),4(2)]
 *    i=5 h=3  push                         [1(1),4(2),5(3)]
 *    i=6 h=0  pop 5(3): width 1, area 3
 *             pop 4(2): left=1 -> width 4, area 8
 *             pop 1(1): left=-1 -> width 6, area 6
 *    answer 10
 *
 *  COMPLEXITY   Time O(n): each index pushed and popped once   Space O(n)
 *  FOLLOW-UP: "Maximal Rectangle" (a 0/1 matrix) = run this on every row's histogram.
 *  SAME TOOL: Next Greater Element, Daily Temperatures, Stock Span, Trapping Rain Water (stack version).
 * ============================================================================
 */

import java.util.*;

public class H03_LargestRectangleInHistogram {

    static int largestRectangleArea(int[] heights) {
        int n = heights.length;
        Deque<Integer> stack = new ArrayDeque<>();           // indices, heights increasing
        int best = 0;
        for (int i = 0; i <= n; i++) {
            int h = (i == n) ? 0 : heights[i];               // sentinel 0 flushes the stack at the end
            while (!stack.isEmpty() && heights[stack.peek()] > h) {
                int height = heights[stack.pop()];           // this bar's rectangle ends here
                int left = stack.isEmpty() ? -1 : stack.peek();   // nearest shorter on the left
                int width = i - left - 1;                    // i = nearest shorter on the right
                best = Math.max(best, height * width);
            }
            stack.push(i);
        }
        return best;
    }

    static int largestRectangleBrute(int[] h) {
        int best = 0;
        for (int i = 0; i < h.length; i++) {
            int l = i, r = i;
            while (l > 0 && h[l - 1] >= h[i]) l--;
            while (r < h.length - 1 && h[r + 1] >= h[i]) r++;
            best = Math.max(best, h[i] * (r - l + 1));
        }
        return best;
    }

    public static void main(String[] args) {
        check("[2,1,5,6,2,3]", 10, largestRectangleArea(new int[]{2, 1, 5, 6, 2, 3}));
        check("[2,4]", 4, largestRectangleArea(new int[]{2, 4}));
        check("[3,3,3] (flat)", 9, largestRectangleArea(new int[]{3, 3, 3}));
        check("[1,2,3,4,5] (increasing)", 9, largestRectangleArea(new int[]{1, 2, 3, 4, 5}));
        check("[0] ", 0, largestRectangleArea(new int[]{0}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] h = rnd.ints(1 + rnd.nextInt(25), 0, 10).toArray();
            if (largestRectangleArea(h) != largestRectangleBrute(h)) ok = false;
        }
        check("300 random arrays vs brute force", true, ok);
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
