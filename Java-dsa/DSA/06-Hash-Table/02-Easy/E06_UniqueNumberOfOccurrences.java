/*
 * ============================================================================
 *  EASY 06 : UNIQUE NUMBER OF OCCURRENCES  ⭐ bonus   LeetCode #1207
 *  https://leetcode.com/problems/unique-number-of-occurrences/
 *  Pattern: FREQUENCY MAP, then a SET of the frequencies (hashing twice)
 * ============================================================================
 *
 *  PROBLEM
 *    Is the number of occurrences of each value UNIQUE (no two values occur the
 *    same number of times)?
 *      [1,2,2,1,1,3]   -> true    (1 occurs 3 times, 2 twice, 3 once)
 *      [1,2]           -> false   (both occur once)
 *
 *  ---------------------------------------------------------------------------
 *  TWO LAYERS OF HASHING (a common shape)
 *    1. count:  value -> frequency          (HashMap, merge)
 *    2. check:  are the frequencies distinct?  -> put them in a HashSet;
 *               add() returns false on a duplicate frequency
 *
 *  DRY RUN [1,2,2,1,1,3]
 *    counts {1:3, 2:2, 3:1}
 *    set: add 3, add 2, add 1 -> no duplicates -> true
 *
 *  JAVA ONE-LINER (good to know, less clear in an interview):
 *    new HashSet<>(counts.values()).size() == counts.size()
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E06_UniqueNumberOfOccurrences {

    static boolean uniqueOccurrences(int[] arr) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int x : arr) counts.merge(x, 1, Integer::sum);
        Set<Integer> seenFrequencies = new HashSet<>();
        for (int f : counts.values()) {
            if (!seenFrequencies.add(f)) return false;      // two values share this frequency
        }
        return true;
    }

    public static void main(String[] args) {
        check("[1,2,2,1,1,3]", true, uniqueOccurrences(new int[]{1, 2, 2, 1, 1, 3}));
        check("[1,2]", false, uniqueOccurrences(new int[]{1, 2}));
        check("[-3,0,1,-3,1,1,1,-3,10,0]", true, uniqueOccurrences(new int[]{-3, 0, 1, -3, 1, 1, 1, -3, 10, 0}));
        check("[5] (single)", true, uniqueOccurrences(new int[]{5}));
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
