/*
 * ============================================================================
 *  EASY 18 : SORT ARRAY BY PARITY            LeetCode #905
 *  https://leetcode.com/problems/sort-array-by-parity/
 *  Pattern: TWO POINTERS (partitioning, opposite ends)
 * ============================================================================
 *
 *  PROBLEM
 *    Move all EVEN numbers before all ODD numbers. Any order inside each group is OK.
 *      [3,1,2,4] -> [4,2,1,3]  (or [2,4,3,1], ...)
 *
 *  ---------------------------------------------------------------------------
 *  PARTITIONING WITH TWO POINTERS (this is the same partition step quicksort uses!)
 *    left  moves right while it points at an EVEN number (already in the correct zone)
 *    right moves left  while it points at an ODD  number (already in the correct zone)
 *    now left is odd and right is even: both are misplaced -> SWAP them
 *
 *  DRY RUN [3,1,2,4]
 *    left=0 (3, odd: stop)   right=3 (4, even: stop)   swap -> [4,1,2,3]
 *    left=1 (1, odd: stop)   right=2 (2, even: stop)   swap -> [4,2,1,3]
 *    left=2 > right=1 -> done
 *
 *  IF THE ORDER MUST BE STABLE (keep the original relative order), use the
 *  slow/fast pattern from Move Zeroes, or an output array. This version doesn't preserve order.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E18_SortArrayByParity {

    static int[] sortArrayByParity(int[] nums) {
        int left = 0, right = nums.length - 1;
        while (left < right) {
            if (nums[left] % 2 == 0) {
                left++;                              // even on the left: correct
            } else if (nums[right] % 2 == 1) {
                right--;                             // odd on the right: correct
            } else {
                int t = nums[left];                  // odd on the left, even on the right: swap
                nums[left] = nums[right];
                nums[right] = t;
                left++;
                right--;
            }
        }
        return nums;
    }

    // Order of the answer may vary, so verify the PROPERTY instead of an exact array
    static boolean evensFirst(int[] a) {
        boolean seenOdd = false;
        for (int x : a) {
            if (x % 2 != 0) seenOdd = true;
            else if (seenOdd) return false;          // an even AFTER an odd -> wrong
        }
        return true;
    }

    public static void main(String[] args) {
        int[] r = sortArrayByParity(new int[]{3, 1, 2, 4});
        check("[3,1,2,4] -> " + Arrays.toString(r), true, evensFirst(r));
        check("[0]", true, evensFirst(sortArrayByParity(new int[]{0})));
        check("[1,3,5,2] ", true, evensFirst(sortArrayByParity(new int[]{1, 3, 5, 2})));
        check("[2,4,6] (all even)", true, evensFirst(sortArrayByParity(new int[]{2, 4, 6})));
        check("same elements kept", "[1, 2, 3, 4]", sorted(sortArrayByParity(new int[]{3, 1, 2, 4})));
        summary();
    }

    static String sorted(int[] a) {
        int[] c = a.clone();
        Arrays.sort(c);
        return Arrays.toString(c);
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
