/*
 * ============================================================================
 *  EASY 01 : TWO SUM                         LeetCode #1
 *  https://leetcode.com/problems/two-sum/
 *  Pattern: HASHING (complement lookup)
 * ============================================================================
 *
 *  Run:  java E01_TwoSum.java
 *
 *  PROBLEM
 *    Given an array nums and an integer target, return the INDICES of the two
 *    numbers that add up to target. Exactly one solution exists, and you may not
 *    use the same element twice.
 *
 *    nums = [2, 7, 11, 15], target = 9   ->  [0, 1]   because 2 + 7 = 9
 *    nums = [3, 2, 4],      target = 6   ->  [1, 2]   (not [0,0]: can't reuse the 3)
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: BRUTE FORCE - try every pair
 *    for each i, for each j > i: if nums[i] + nums[j] == target -> answer
 *    Time O(n^2), Space O(1)
 *
 *  BOTTLENECK: for each nums[i] we SCAN the rest looking for (target - nums[i]).
 *  "Looking something up fast" = a HashMap!
 *
 *  APPROACH 2: ONE-PASS HASHMAP  (optimal)
 *    The partner we need for x is   complement = target - x.
 *    Walk the array once, remembering   value -> index   in a map.
 *    For each x: if the complement is ALREADY in the map, we found the pair.
 *    Otherwise store x and move on.
 *    Checking BEFORE inserting means we never pair an element with itself.
 *
 *  DRY RUN  nums = [2, 7, 11, 15], target = 9
 *    i=0  x=2   complement=7   map={}            7 not seen  -> store 2:0   map={2:0}
 *    i=1  x=7   complement=2   map={2:0}         2 SEEN at 0 -> return [0, 1]
 *
 *  COMPLEXITY   Time O(n): one pass, O(1) map operations
 *               Space O(n): the map may hold up to n entries
 *
 *  EDGE CASES   negative numbers, zeros ([0,4,3,0], target 0 -> [0,3]),
 *               duplicates ([3,3], target 6 -> [0,1])
 * ============================================================================
 */

import java.util.*;

public class E01_TwoSum {

    // APPROACH 1: brute force, O(n^2)
    static int[] twoSumBrute(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {   // j starts after i: no reuse, no repeats
                if (nums[i] + nums[j] == target) return new int[]{i, j};
            }
        }
        return new int[0];
    }

    // APPROACH 2: one-pass HashMap, O(n)
    static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexOf = new HashMap<>();      // value -> index where we saw it
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (indexOf.containsKey(complement)) {
                return new int[]{indexOf.get(complement), i}; // the earlier index first
            }
            indexOf.put(nums[i], i);                          // store AFTER checking
        }
        return new int[0];                                    // never happens per the problem
    }

    public static void main(String[] args) {
        check("[2,7,11,15] t=9", new int[]{0, 1}, twoSum(new int[]{2, 7, 11, 15}, 9));
        check("[3,2,4] t=6", new int[]{1, 2}, twoSum(new int[]{3, 2, 4}, 6));
        check("[3,3] t=6 (duplicates)", new int[]{0, 1}, twoSum(new int[]{3, 3}, 6));
        check("[0,4,3,0] t=0 (zeros)", new int[]{0, 3}, twoSum(new int[]{0, 4, 3, 0}, 0));
        check("[-3,4,3,90] t=0 (negatives)", new int[]{0, 2}, twoSum(new int[]{-3, 4, 3, 90}, 0));
        check("brute force agrees", new int[]{1, 2}, twoSumBrute(new int[]{3, 2, 4}, 6));
        summary();
    }

    // ---------------- tiny test helper ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-32s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
