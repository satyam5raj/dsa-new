/*
 * ============================================================================
 *  MEDIUM 03 : NEXT GREATER ELEMENT II       LeetCode #503
 *  https://leetcode.com/problems/next-greater-element-ii/
 *  Pattern: MONOTONIC STACK on a CIRCULAR array (loop twice with i % n)
 * ============================================================================
 *
 *  PROBLEM
 *    The array is CIRCULAR: after the last element comes the first. For each
 *    element, find the next greater value (searching around the circle), or -1.
 *      [1,2,1]     -> [2,-1,2]      (the last 1 wraps around and finds 2)
 *      [1,2,3,4,3] -> [2,3,4,-1,4]
 *
 *  ---------------------------------------------------------------------------
 *  THE CIRCULAR TRICK: pretend the array is written TWICE: [1,2,1,1,2,1].
 *  Any element's "next greater around the circle" is within the next n elements
 *  of this doubled array. Don't copy it: loop i from 0 to 2n-1 and use nums[i % n].
 *
 *  ALGORITHM
 *    answer filled with -1
 *    for i in 0 .. 2n-1:
 *       x = nums[i % n]
 *       while stack not empty and nums[stack.top] < x:  answer[stack.pop()] = x
 *       if i < n: push i        // only the FIRST pass creates "waiting" entries;
 *                               // the second pass only RESOLVES them
 *
 *  DRY RUN [1,2,1]
 *    i0 1: push 0                 [0]
 *    i1 2: pop 0 -> ans[0]=2; push 1   [1]
 *    i2 1: push 2                 [1,2]
 *    i3 (1): nothing smaller than 1 to pop
 *    i4 (2): pop 2 -> ans[2]=2    [1]
 *    i5 (1): -
 *    index 1 (the max) stays -1  -> [2,-1,2]
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M03_NextGreaterElementII {

    static int[] nextGreaterElements(int[] nums) {
        int n = nums.length;
        int[] ans = new int[n];
        Arrays.fill(ans, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < 2 * n; i++) {
            int x = nums[i % n];
            while (!stack.isEmpty() && nums[stack.peek()] < x) ans[stack.pop()] = x;
            if (i < n) stack.push(i);
        }
        return ans;
    }

    static int[] brute(int[] a) {
        int n = a.length;
        int[] r = new int[n];
        Arrays.fill(r, -1);
        for (int i = 0; i < n; i++)
            for (int k = 1; k < n; k++)
                if (a[(i + k) % n] > a[i]) { r[i] = a[(i + k) % n]; break; }
        return r;
    }

    public static void main(String[] args) {
        check("[1,2,1]", new int[]{2, -1, 2}, nextGreaterElements(new int[]{1, 2, 1}));
        check("[1,2,3,4,3]", new int[]{2, 3, 4, -1, 4}, nextGreaterElements(new int[]{1, 2, 3, 4, 3}));
        check("[5,5,5] (all equal)", new int[]{-1, -1, -1}, nextGreaterElements(new int[]{5, 5, 5}));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), 0, 8).toArray();
            ok &= Arrays.equals(nextGreaterElements(a), brute(a));
        }
        check("500 random arrays vs brute force", true, ok);
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
