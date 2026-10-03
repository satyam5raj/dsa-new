/*
 * ============================================================================
 *  EASY 03 : NEXT GREATER ELEMENT I          LeetCode #496
 *  https://leetcode.com/problems/next-greater-element-i/
 *  Pattern: MONOTONIC STACK + HASHMAP (your first monotonic stack problem)
 * ============================================================================
 *
 *  PROBLEM
 *    nums1 is a subset of nums2 (all values distinct). For each x in nums1, find
 *    the first element to the RIGHT of x IN nums2 that is greater than x (or -1).
 *      nums1 = [4,1,2], nums2 = [1,3,4,2]  ->  [-1, 3, -1]
 *        4: nothing greater after it in nums2 -> -1
 *        1: next greater is 3
 *        2: nothing after it                -> -1
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each x, find it in nums2 and scan right -> O(m * n)
 *
 *  MONOTONIC STACK: compute the next greater element for EVERY value in nums2
 *  in one pass, store it in a map, then answer nums1 by lookup.
 *    stack holds values still waiting for their answer (decreasing from bottom to top)
 *    for x in nums2:
 *       while stack.top < x:  nextGreater[stack.pop()] = x     (x is their answer)
 *       push x
 *    anything left in the stack has no greater element -> -1
 *
 *  DRY RUN nums2 = [1,3,4,2]
 *    1: push                    [1]
 *    3: 1 < 3 -> ng[1]=3; push  [3]
 *    4: 3 < 4 -> ng[3]=4; push  [4]
 *    2: 4 > 2 -> push           [4,2]      leftovers 4, 2 -> -1
 *    map {1:3, 3:4}  -> nums1 [4,1,2] -> [-1, 3, -1]
 *
 *  WHY O(n)? Each value is pushed once and popped at most once, so the inner
 *  while loop runs at most n times IN TOTAL across the whole scan.
 *  COMPLEXITY   Time O(n + m)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E03_NextGreaterElementI {

    static int[] nextGreaterElement(int[] nums1, int[] nums2) {
        Map<Integer, Integer> nextGreater = new HashMap<>();
        Deque<Integer> stack = new ArrayDeque<>();
        for (int x : nums2) {
            while (!stack.isEmpty() && stack.peek() < x) {
                nextGreater.put(stack.pop(), x);
            }
            stack.push(x);
        }
        int[] ans = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) ans[i] = nextGreater.getOrDefault(nums1[i], -1);
        return ans;
    }

    public static void main(String[] args) {
        check("[4,1,2] in [1,3,4,2]", new int[]{-1, 3, -1}, nextGreaterElement(new int[]{4, 1, 2}, new int[]{1, 3, 4, 2}));
        check("[2,4] in [1,2,3,4]", new int[]{3, -1}, nextGreaterElement(new int[]{2, 4}, new int[]{1, 2, 3, 4}));
        check("decreasing nums2", new int[]{-1, -1}, nextGreaterElement(new int[]{3, 1}, new int[]{3, 2, 1}));
        check("[1,3,5,2,4] full", new int[]{3, 5, -1, 4, -1}, nextGreaterElement(new int[]{1, 3, 5, 2, 4}, new int[]{1, 3, 5, 2, 4}));
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
