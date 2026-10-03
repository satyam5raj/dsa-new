/*
 * ============================================================================
 *  MEDIUM 06 : PRODUCT OF ARRAY EXCEPT SELF  LeetCode #238
 *  https://leetcode.com/problems/product-of-array-except-self/
 *  Pattern: PREFIX x SUFFIX products (no division)
 * ============================================================================
 *
 *  PROBLEM
 *    answer[i] = the product of all elements EXCEPT nums[i].
 *    Rules: O(n) time and NO DIVISION. Follow-up: O(1) extra space (the output doesn't count).
 *      [1,2,3,4] -> [24,12,8,6]
 *
 *  ---------------------------------------------------------------------------
 *  WHY NOT "total product / nums[i]"? Division is forbidden, and it breaks on
 *  zeros anyway (divide by 0).
 *
 *  KEY INSIGHT: "everything except i" = (everything to the LEFT of i) x (everything to the RIGHT of i)
 *    nums   = [1,  2,  3,  4]
 *    left   = [1,  1,  2,  6]     product of elements before i (1 for the first)
 *    right  = [24, 12, 4,  1]     product of elements after  i (1 for the last)
 *    answer = [24, 12, 8,  6]     left[i] * right[i]
 *
 *  O(1) EXTRA SPACE TRICK
 *    pass 1 (left -> right): answer[i] = product of everything before i
 *    pass 2 (right -> left): keep ONE variable 'suffix' = the product of everything
 *      after i, and multiply it into answer[i]
 *
 *  DRY RUN pass 2 on answer = [1,1,2,6]
 *    i=3: answer=6*1=6,   suffix=1*4=4
 *    i=2: answer=2*4=8,   suffix=4*3=12
 *    i=1: answer=1*12=12, suffix=12*2=24
 *    i=0: answer=1*24=24             -> [24,12,8,6]
 *
 *  ZEROS work automatically: [0,1,2] -> [2,0,0]. No special case needed.
 *  COMPLEXITY   Time O(n)   Space O(1) extra
 * ============================================================================
 */

import java.util.*;

public class M06_ProductOfArrayExceptSelf {

    static int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] answer = new int[n];

        answer[0] = 1;                                // nothing to the left of index 0
        for (int i = 1; i < n; i++) {
            answer[i] = answer[i - 1] * nums[i - 1];  // prefix product
        }

        int suffix = 1;                               // nothing to the right of the last index
        for (int i = n - 1; i >= 0; i--) {
            answer[i] *= suffix;                      // left part * right part
            suffix *= nums[i];                        // include nums[i] for the next (left) position
        }
        return answer;
    }

    public static void main(String[] args) {
        check("[1,2,3,4]", new int[]{24, 12, 8, 6}, productExceptSelf(new int[]{1, 2, 3, 4}));
        check("[-1,1,0,-3,3] (one zero)", new int[]{0, 0, 9, 0, 0}, productExceptSelf(new int[]{-1, 1, 0, -3, 3}));
        check("[0,0,2] (two zeros)", new int[]{0, 0, 0}, productExceptSelf(new int[]{0, 0, 2}));
        check("[2,3] (two elements)", new int[]{3, 2}, productExceptSelf(new int[]{2, 3}));
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
