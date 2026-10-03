/*
 * ============================================================================
 *  MEDIUM 17 : FRUIT INTO BASKETS            LeetCode #904
 *  https://leetcode.com/problems/fruit-into-baskets/
 *  Pattern: SLIDING WINDOW + FREQUENCY MAP (at most K distinct values)
 * ============================================================================
 *
 *  PROBLEM (translated out of its story)
 *    fruits[i] = the fruit type of tree i. You have 2 baskets, each holding ONE
 *    type. Walking right from any start, pick one fruit per tree, and stop when
 *    a third type appears. Max fruits = the LONGEST SUBARRAY WITH AT MOST 2 DISTINCT VALUES.
 *      [1,2,1]       -> 3
 *      [0,1,2,2]     -> 3     ([1,2,2])
 *      [1,2,3,2,2]   -> 4     ([2,3,2,2])
 *
 *  ---------------------------------------------------------------------------
 *  Same shape as M12, but the "budget" is DISTINCT TYPES, tracked with a count map:
 *    count[type] = how many of that type are in the window
 *    window valid  <=>  count.size() <= 2
 *    expand: count[fruits[right]]++
 *    shrink while count.size() > 2: count[fruits[left]]--, remove the key at 0, left++
 *
 *  DRY RUN [1,2,3,2,2]
 *    r=0 {1:1}            len 1
 *    r=1 {1:1,2:1}        len 2
 *    r=2 {1:1,2:1,3:1} -> too many: drop 1 -> {2:1,3:1}, left=1   len 2
 *    r=3 {2:2,3:1}        len 3
 *    r=4 {2:3,3:1}        len 4   <- answer
 *
 *  IMPORTANT: REMOVE keys whose count reaches 0. Otherwise map.size() still
 *  counts a type that is no longer in the window.
 *  COMPLEXITY   Time O(n)   Space O(1) (the map holds at most 3 keys)
 *  GENERALIZES to "at most K distinct" for any K (used in Hard H05).
 * ============================================================================
 */

import java.util.*;

public class M17_FruitIntoBaskets {

    static int totalFruit(int[] fruits) {
        return longestWithAtMostKDistinct(fruits, 2);
    }

    static int longestWithAtMostKDistinct(int[] a, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        int left = 0, best = 0;
        for (int right = 0; right < a.length; right++) {
            count.merge(a[right], 1, Integer::sum);
            while (count.size() > k) {
                int type = a[left++];
                if (count.merge(type, -1, Integer::sum) == 0) count.remove(type);   // gone from the window
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[1,2,1]", 3, totalFruit(new int[]{1, 2, 1}));
        check("[0,1,2,2]", 3, totalFruit(new int[]{0, 1, 2, 2}));
        check("[1,2,3,2,2]", 4, totalFruit(new int[]{1, 2, 3, 2, 2}));
        check("[3,3,3,1,2,1,1,2,3,3,4]", 5, totalFruit(new int[]{3, 3, 3, 1, 2, 1, 1, 2, 3, 3, 4}));
        check("[5] (single tree)", 1, totalFruit(new int[]{5}));
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
