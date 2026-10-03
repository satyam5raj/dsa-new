/*
 * ============================================================================
 *  EASY 03 : RELATIVE RANKS  ⭐ bonus          LeetCode #506
 *  https://leetcode.com/problems/relative-ranks/
 *  Pattern: heap of INDICES ordered by value (remember WHERE each value came from)
 * ============================================================================
 *
 *  PROBLEM
 *    score has unique values. The highest gets "Gold Medal", 2nd "Silver Medal",
 *    3rd "Bronze Medal", the rest their place number as a string. Answer in the
 *    ORIGINAL order.
 *      [10,3,8,9,4] -> ["Gold Medal","5","Bronze Medal","Silver Medal","4"]
 *
 *  ---------------------------------------------------------------------------
 *  THE TWIST: sorting the scores loses their positions. Push the INDICES instead,
 *  ordered by score[index] (descending). Polling gives athletes from best to worst,
 *  and each polled index tells us where to write the rank.
 *  This "heap/sort of indices with a comparator on the values" trick is used
 *  constantly (K Closest Points, IPO, Hire K Workers...).
 *
 *  (Sorting an Integer[] of indices with the same comparator works just as well:
 *   both are O(n log n). The heap version shows the pattern.)
 *
 *  DRY RUN [10,3,8,9,4]
 *    poll order of indices: 0 (10), 3 (9), 2 (8), 4 (4), 1 (3)
 *    ranks: res[0]=Gold, res[3]=Silver, res[2]=Bronze, res[4]="4", res[1]="5"
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E03_RelativeRanks {

    static String[] findRelativeRanks(int[] score) {
        PriorityQueue<Integer> heap = new PriorityQueue<>((i, j) -> Integer.compare(score[j], score[i]));   // max by score
        for (int i = 0; i < score.length; i++) heap.offer(i);
        String[] result = new String[score.length];
        String[] medals = {"Gold Medal", "Silver Medal", "Bronze Medal"};
        for (int place = 1; !heap.isEmpty(); place++) {
            int i = heap.poll();                                 // the next best athlete's INDEX
            result[i] = place <= 3 ? medals[place - 1] : String.valueOf(place);
        }
        return result;
    }

    public static void main(String[] args) {
        check("[5,4,3,2,1]", "[Gold Medal, Silver Medal, Bronze Medal, 4, 5]", Arrays.toString(findRelativeRanks(new int[]{5, 4, 3, 2, 1})));
        check("[10,3,8,9,4]", "[Gold Medal, 5, Bronze Medal, Silver Medal, 4]", Arrays.toString(findRelativeRanks(new int[]{10, 3, 8, 9, 4})));
        check("[1]", "[Gold Medal]", Arrays.toString(findRelativeRanks(new int[]{1})));
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
