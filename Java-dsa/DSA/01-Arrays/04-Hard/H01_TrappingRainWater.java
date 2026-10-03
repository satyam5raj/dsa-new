/*
 * ============================================================================
 *  HARD 01 : TRAPPING RAIN WATER             LeetCode #42
 *  https://leetcode.com/problems/trapping-rain-water/
 *  Pattern: PREFIX MAX / SUFFIX MAX  ->  TWO POINTERS (O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    height[i] = the height of a bar of width 1. How much rain water is trapped
 *    between the bars?
 *      [0,1,0,2,1,0,1,3,2,1,2,1] -> 6
 *
 *                        #
 *            #  ~  ~  ~  #  #  ~  #
 *         #  ~  #  #  ~  #  #  #  #  #     (~ = water, 6 units)
 *      ------------------------------------
 *      0  1  0  2  1  0  1  3  2  1  2  1
 *
 *  ---------------------------------------------------------------------------
 *  THE CORE FORMULA (think about ONE column at a time)
 *    Water above column i = min(tallest bar to its LEFT, tallest bar to its RIGHT) - height[i]
 *    (if that's negative -> 0). Water rises to the level of the LOWER of the two
 *    walls surrounding it, like a bucket limited by its shorter side.
 *
 *  APPROACH 1: BRUTE FORCE - for each i, scan left and right for the maxima   O(n^2)
 *
 *  APPROACH 2: PRECOMPUTE THE MAXIMA                          O(n) time, O(n) space
 *    leftMax[i]  = max(height[0..i])     (one pass left -> right)
 *    rightMax[i] = max(height[i..n-1])   (one pass right -> left)
 *    water += min(leftMax[i], rightMax[i]) - height[i]
 *
 *  APPROACH 3: TWO POINTERS                                   O(n) time, O(1) space
 *    Keep left/right pointers and the running maxima leftMax, rightMax.
 *    KEY INSIGHT: if leftMax < rightMax, the water at 'left' is decided by leftMax ALONE.
 *      We know a wall of height >= rightMax > leftMax exists somewhere to the right,
 *      so the right side is NOT the bottleneck. We don't need the exact right max.
 *      -> water at left = leftMax - height[left]; move left++
 *    Otherwise the symmetric argument applies to the right pointer.
 *
 *  DRY RUN (approach 3) on [4,2,0,3,2,5]  (answer 9)
 *    L=0 R=5  lMax=4 rMax=5  4<5 -> left side: water 4-4=0, L=1
 *    L=1      lMax=4         4<5 -> water 4-2=2   (total 2), L=2
 *    L=2                     4<5 -> water 4-0=4   (total 6), L=3
 *    L=3                     4<5 -> water 4-3=1   (total 7), L=4
 *    L=4                     4<5 -> water 4-2=2   (total 9), L=5 = R -> stop
 *
 *  ALSO SOLVABLE with a MONOTONIC STACK (filling water layer by layer).
 *  You'll meet that technique in H03.
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class H01_TrappingRainWater {

    // Approach 2: precomputed maxima
    static int trapWithArrays(int[] height) {
        int n = height.length;
        if (n == 0) return 0;
        int[] leftMax = new int[n], rightMax = new int[n];
        leftMax[0] = height[0];
        for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i - 1], height[i]);
        rightMax[n - 1] = height[n - 1];
        for (int i = n - 2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i + 1], height[i]);
        int water = 0;
        for (int i = 0; i < n; i++) water += Math.min(leftMax[i], rightMax[i]) - height[i];
        return water;
    }

    // Approach 3: two pointers, O(1) space
    static int trap(int[] height) {
        int left = 0, right = height.length - 1;
        int leftMax = 0, rightMax = 0, water = 0;
        while (left < right) {
            leftMax = Math.max(leftMax, height[left]);
            rightMax = Math.max(rightMax, height[right]);
            if (leftMax < rightMax) {
                water += leftMax - height[left];       // the left side is the bottleneck
                left++;
            } else {
                water += rightMax - height[right];     // the right side is the bottleneck
                right--;
            }
        }
        return water;
    }

    static int trapBrute(int[] h) {
        int water = 0;
        for (int i = 0; i < h.length; i++) {
            int l = 0, r = 0;
            for (int j = 0; j <= i; j++) l = Math.max(l, h[j]);
            for (int j = i; j < h.length; j++) r = Math.max(r, h[j]);
            water += Math.min(l, r) - h[i];
        }
        return water;
    }

    public static void main(String[] args) {
        check("[0,1,0,2,1,0,1,3,2,1,2,1]", 6, trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));
        check("[4,2,0,3,2,5]", 9, trap(new int[]{4, 2, 0, 3, 2, 5}));
        check("[1,2,3] (staircase: nothing)", 0, trap(new int[]{1, 2, 3}));
        check("[] (empty)", 0, trap(new int[]{}));
        check("arrays approach", 6, trapWithArrays(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));
        Random rnd = new Random(42);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] h = rnd.ints(rnd.nextInt(25), 0, 10).toArray();
            if (trap(h) != trapBrute(h) || trapWithArrays(h) != trapBrute(h)) ok = false;
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
