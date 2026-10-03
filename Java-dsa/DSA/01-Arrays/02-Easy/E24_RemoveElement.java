/*
 * ============================================================================
 *  EASY 24 : REMOVE ELEMENT                  LeetCode #27
 *  https://leetcode.com/problems/remove-element/
 *  Pattern: TWO POINTERS (slow/fast filter)
 * ============================================================================
 *
 *  PROBLEM
 *    Remove all occurrences of val IN-PLACE. Return k = the number of elements
 *    that are NOT val. The first k slots must hold them (any order is accepted).
 *      nums = [3,2,2,3], val = 3        -> k=2, nums = [2,2,_,_]
 *      nums = [0,1,2,2,3,0,4,2], val=2  -> k=5, nums = [0,1,3,0,4,_,_,_]
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: SLOW/FAST (keeps the order)
 *    k = next write position. For each x != val: nums[k++] = x.
 *    The same template as Move Zeroes and Remove Duplicates. Learn it once, use it everywhere.
 *
 *  APPROACH 2: SWAP WITH THE END (fewer writes when val is rare; order not kept)
 *    When nums[i] == val, overwrite it with the LAST element and shrink n.
 *    Don't advance i: the element moved in still needs checking.
 *
 *  DRY RUN approach 1, [3,2,2,3], val=3
 *    3 skip | 2 -> nums[0]=2, k=1 | 2 -> nums[1]=2, k=2 | 3 skip -> k=2
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E24_RemoveElement {

    static int removeElement(int[] nums, int val) {
        int k = 0;
        for (int x : nums) {
            if (x != val) nums[k++] = x;
        }
        return k;
    }

    static int removeElementSwap(int[] nums, int val) {
        int i = 0, n = nums.length;
        while (i < n) {
            if (nums[i] == val) {
                nums[i] = nums[--n];       // bring the last element here, shrink the array
            } else {
                i++;
            }
        }
        return n;
    }

    static String kept(int[] nums, int k) {
        int[] a = Arrays.copyOf(nums, k);
        Arrays.sort(a);                    // order may differ between approaches -> compare sorted
        return Arrays.toString(a);
    }

    public static void main(String[] args) {
        int[] a = {3, 2, 2, 3};
        check("[3,2,2,3] val=3", "[2, 2]", kept(a, removeElement(a, 3)));
        int[] b = {0, 1, 2, 2, 3, 0, 4, 2};
        check("[0,1,2,2,3,0,4,2] val=2", "[0, 0, 1, 3, 4]", kept(b, removeElement(b, 2)));
        int[] c = {1, 1, 1};
        check("[1,1,1] val=1 (remove all)", "[]", kept(c, removeElement(c, 1)));
        int[] d = {0, 1, 2, 2, 3, 0, 4, 2};
        check("swap approach", "[0, 0, 1, 3, 4]", kept(d, removeElementSwap(d, 2)));
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
