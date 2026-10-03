/*
 * ============================================================================
 *  MEDIUM 05 : 3SUM                          LeetCode #15
 *  https://leetcode.com/problems/3sum/
 *  Pattern: SORT + FIX ONE + TWO POINTERS  (with careful duplicate skipping)
 * ============================================================================
 *
 *  PROBLEM
 *    Return all UNIQUE triplets [a, b, c] with a + b + c == 0. No duplicate triplets.
 *      [-1,0,1,2,-1,-4] -> [[-1,-1,2],[-1,0,1]]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: three nested loops + a Set to remove duplicates -> O(n^3)
 *
 *  REDUCE TO TWO SUM II:
 *    1. SORT the array (enables two pointers AND easy duplicate skipping).
 *    2. FIX the first number nums[i]. Now we need two numbers in the rest of
 *       the array summing to -nums[i]: exactly Two Sum II (two pointers).
 *    Total: O(n) choices for i x O(n) two-pointer scan = O(n^2)  (plus O(n log n) sort)
 *
 *  AVOIDING DUPLICATE TRIPLETS (the tricky part)
 *    a) skip i if nums[i] == nums[i-1]: the same first number gives the same triplets
 *    b) after finding a triplet, move left past ALL copies of nums[left]
 *       and right past all copies of nums[right]
 *  EARLY EXIT: once nums[i] > 0, every number after it is positive too -> no zero sum possible.
 *
 *  DRY RUN sorted = [-4,-1,-1,0,1,2]
 *    i=0 (-4): need +4 from [-1,-1,0,1,2] -> max is 1+2=3 -> none
 *    i=1 (-1): need +1 from [-1,0,1,2]
 *              L=-1 R=2: sum 1 -> FOUND [-1,-1,2]; skip, L=0 R=1: sum 1 -> FOUND [-1,0,1]
 *    i=2 (-1): same as nums[1] -> SKIP (would repeat the triplets)
 *    i=3 (0) : need 0 from [1,2] -> none
 *    i=4 (1) > 0 -> stop
 *
 *  COMPLEXITY   Time O(n^2)   Space O(1) extra (ignoring the output, and the sort's O(log n))
 *  GENERALISES: 4Sum = fix two numbers + two pointers -> O(n^3). kSum = recursion.
 * ============================================================================
 */

import java.util.*;

public class M05_ThreeSum {

    static List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;

        for (int i = 0; i < n - 2; i++) {
            if (nums[i] > 0) break;                              // early exit
            if (i > 0 && nums[i] == nums[i - 1]) continue;       // (a) skip duplicate first numbers

            int left = i + 1, right = n - 1;
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                if (sum < 0) {
                    left++;
                } else if (sum > 0) {
                    right--;
                } else {
                    result.add(List.of(nums[i], nums[left], nums[right]));
                    while (left < right && nums[left] == nums[left + 1]) left++;     // (b) skip copies
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++;
                    right--;
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        check("[-1,0,1,2,-1,-4]", "[[-1, -1, 2], [-1, 0, 1]]", threeSum(new int[]{-1, 0, 1, 2, -1, -4}));
        check("[0,1,1] (none)", "[]", threeSum(new int[]{0, 1, 1}));
        check("[0,0,0,0] (many zeros, one triplet)", "[[0, 0, 0]]", threeSum(new int[]{0, 0, 0, 0}));
        check("[-2,0,1,1,2]", "[[-2, 0, 2], [-2, 1, 1]]", threeSum(new int[]{-2, 0, 1, 1, 2}));
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
