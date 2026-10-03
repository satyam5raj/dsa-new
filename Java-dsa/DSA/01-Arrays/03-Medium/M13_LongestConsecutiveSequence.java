/*
 * ============================================================================
 *  MEDIUM 13 : LONGEST CONSECUTIVE SEQUENCE  LeetCode #128
 *  https://leetcode.com/problems/longest-consecutive-sequence/
 *  Pattern: HASHSET + "ONLY START COUNTING AT THE BEGINNING OF A SEQUENCE"
 * ============================================================================
 *
 *  PROBLEM
 *    Unsorted array. Return the length of the longest run of CONSECUTIVE
 *    integers (they don't need to be adjacent in the array). Must be O(n).
 *      [100,4,200,1,3,2]       -> 4   (1,2,3,4)
 *      [0,3,7,2,5,8,4,6,0,1]   -> 9   (0..8)
 *
 *  ---------------------------------------------------------------------------
 *  SORTING: sort, then count runs (skip duplicates) -> O(n log n). Not allowed.
 *
 *  HASHSET IDEA: put everything in a set (O(1) "does x exist?").
 *  NAIVE: from EVERY x, count x+1, x+2, ... -> the same run is re-counted from
 *  each of its members -> O(n^2) in the worst case.
 *
 *  THE TRICK: only start counting from x if x is the START of a run, i.e.
 *  (x - 1) is NOT in the set. Then each run is walked exactly once.
 *    for x in set:
 *      if (x-1) not in set:                // x starts a run
 *        length = 1
 *        while (x + length) in set: length++
 *        best = max(best, length)
 *
 *  WHY O(n)? The inner while loop only runs from run STARTS, and the total steps
 *  across all runs = n. Every element is visited at most twice overall.
 *
 *  DRY RUN [100,4,200,1,3,2]   set = {100,4,200,1,3,2}
 *    100: 99 absent -> start: 101 absent -> length 1
 *    4  : 3 present -> not a start, skip
 *    200: start -> length 1
 *    1  : 0 absent -> start: 2,3,4 present, 5 absent -> length 4  <- best
 *    3, 2: not starts, skip
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 *  (Iterate over the SET, not the array: duplicates in the array would repeat work.)
 * ============================================================================
 */

import java.util.*;

public class M13_LongestConsecutiveSequence {

    static int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int x : nums) set.add(x);

        int best = 0;
        for (int x : set) {
            if (!set.contains(x - 1)) {                 // x is the first number of a run
                int length = 1;
                while (set.contains(x + length)) length++;
                best = Math.max(best, length);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("[100,4,200,1,3,2]", 4, longestConsecutive(new int[]{100, 4, 200, 1, 3, 2}));
        check("[0,3,7,2,5,8,4,6,0,1]", 9, longestConsecutive(new int[]{0, 3, 7, 2, 5, 8, 4, 6, 0, 1}));
        check("[] (empty)", 0, longestConsecutive(new int[]{}));
        check("[1,2,0,1] (duplicates)", 3, longestConsecutive(new int[]{1, 2, 0, 1}));
        check("[-2,-1,0,5] (negatives)", 3, longestConsecutive(new int[]{-2, -1, 0, 5}));
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
