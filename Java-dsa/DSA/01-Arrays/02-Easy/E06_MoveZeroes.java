/*
 * ============================================================================
 *  EASY 06 : MOVE ZEROES                     LeetCode #283
 *  https://leetcode.com/problems/move-zeroes/
 *  Pattern: TWO POINTERS (slow/fast, in-place)
 * ============================================================================
 *
 *  PROBLEM
 *    Move all 0s to the END, keeping the RELATIVE ORDER of the non-zero
 *    elements. Do it IN-PLACE (no extra array).
 *      [0,1,0,3,12] -> [1,3,12,0,0]
 *
 *  ---------------------------------------------------------------------------
 *  EASY BUT NOT ALLOWED: copy non-zeros into a new array, then pad with zeros (O(n) space).
 *
 *  TWO POINTERS:
 *    'insert' (slow) = the position where the NEXT non-zero should go
 *    'i'      (fast) = scans every element
 *    When nums[i] != 0: SWAP it into position 'insert', then insert++.
 *    Everything before 'insert' is non-zero in original order. Zeros get pushed
 *    to the right by the swaps.
 *
 *  DRY RUN [0,1,0,3,12]
 *    i=0  0    skip                      insert=0  [0,1,0,3,12]
 *    i=1  1    swap(0,1)                 insert=1  [1,0,0,3,12]
 *    i=2  0    skip                      insert=1
 *    i=3  3    swap(1,3)                 insert=2  [1,3,0,0,12]
 *    i=4  12   swap(2,4)                 insert=3  [1,3,12,0,0]  done
 *
 *  ALTERNATIVE (two passes): copy non-zeros forward, then fill the tail with 0.
 *  Same O(n), but swapping does fewer writes when there are few zeros.
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E06_MoveZeroes {

    static void moveZeroes(int[] nums) {
        int insert = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                int tmp = nums[insert];        // swap nums[i] into its final place
                nums[insert] = nums[i];
                nums[i] = tmp;
                insert++;
            }
        }
    }

    static int[] run(int... nums) {
        moveZeroes(nums);
        return nums;
    }

    public static void main(String[] args) {
        check("[0,1,0,3,12]", new int[]{1, 3, 12, 0, 0}, run(0, 1, 0, 3, 12));
        check("[0] ", new int[]{0}, run(0));
        check("[1,2,3] (no zeros)", new int[]{1, 2, 3}, run(1, 2, 3));
        check("[0,0,1] (zeros first)", new int[]{1, 0, 0}, run(0, 0, 1));
        check("[4,0,-2,0,5] (negatives)", new int[]{4, -2, 5, 0, 0}, run(4, 0, -2, 0, 5));
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
