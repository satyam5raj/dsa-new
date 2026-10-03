/*
 * ============================================================================
 *  HARD 05 : SUBARRAYS WITH K DIFFERENT INTEGERS   LeetCode #992
 *  https://leetcode.com/problems/subarrays-with-k-different-integers/
 *  Pattern: "EXACTLY K" = "AT MOST K" - "AT MOST K-1"  (sliding window counting)
 * ============================================================================
 *
 *  PROBLEM
 *    Count the subarrays with EXACTLY k distinct integers.
 *      [1,2,1,2,3], k=2 -> 7
 *        [1,2],[2,1],[1,2],[2,3],[1,2,1],[2,1,2],[1,2,1,2]
 *      [1,2,1,3,4], k=3 -> 3
 *
 *  ---------------------------------------------------------------------------
 *  WHY A DIRECT SLIDING WINDOW FAILS: for "AT MOST k" the window has a clean
 *  rule (shrink while > k distinct). For "EXACTLY k" it doesn't: for a fixed
 *  right end, the valid starts form a RANGE in the middle, not a prefix.
 *  The window can't represent all of them at once.
 *
 *  THE TRICK
 *    exactly(k) = atMost(k) - atMost(k - 1)
 *    (subarrays with <= k distinct, minus those with <= k-1 distinct, leaves exactly k)
 *
 *  atMost(k) uses the M14 COUNTING TRICK + the M17 WINDOW:
 *    for each right: expand, shrink while distinct > k,
 *    then ALL (right - left + 1) subarrays ending at right have <= k distinct -> add them.
 *
 *  DRY RUN atMost(2) on [1,2,1,2,3]
 *    r=0 [1]          +1   (1)
 *    r=1 [1,2]        +2   (3)
 *    r=2 [1,2,1]      +3   (6)
 *    r=3 [1,2,1,2]    +4   (10)
 *    r=4 [..3] = 3 distinct -> shrink to [2,3]  +2  (12)
 *    atMost(1) = 5 (the five single elements; no two neighbours are equal)
 *    exactly(2) = 12 - 5 = 7  ✓
 *
 *  COMPLEXITY   Time O(n) (two linear passes)   Space O(k) for the counts
 *  THIS TRICK GENERALISES: "count subarrays with sum exactly S" on binary arrays,
 *  "number of nice subarrays" (exactly k odd numbers), ...
 * ============================================================================
 */

import java.util.*;

public class H05_SubarraysWithKDifferentIntegers {

    static int subarraysWithKDistinct(int[] nums, int k) {
        return atMost(nums, k) - atMost(nums, k - 1);
    }

    // number of subarrays with AT MOST k distinct values
    static int atMost(int[] nums, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        int left = 0, total = 0;
        for (int right = 0; right < nums.length; right++) {
            count.merge(nums[right], 1, Integer::sum);
            while (count.size() > k) {
                int v = nums[left++];
                if (count.merge(v, -1, Integer::sum) == 0) count.remove(v);
            }
            total += right - left + 1;                     // all subarrays ending at 'right'
        }
        return total;
    }

    static int exactlyBrute(int[] a, int k) {
        int c = 0;
        for (int i = 0; i < a.length; i++) {
            Set<Integer> s = new HashSet<>();
            for (int j = i; j < a.length; j++) {
                s.add(a[j]);
                if (s.size() == k) c++;
            }
        }
        return c;
    }

    public static void main(String[] args) {
        check("[1,2,1,2,3] k=2", 7, subarraysWithKDistinct(new int[]{1, 2, 1, 2, 3}, 2));
        check("[1,2,1,3,4] k=3", 3, subarraysWithKDistinct(new int[]{1, 2, 1, 3, 4}, 3));
        check("[1,1,1] k=1", 6, subarraysWithKDistinct(new int[]{1, 1, 1}, 1));
        check("[1,2] k=3 (impossible)", 0, subarraysWithKDistinct(new int[]{1, 2}, 3));
        Random rnd = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), 1, 5).toArray();
            int k = 1 + rnd.nextInt(4);
            if (subarraysWithKDistinct(a, k) != exactlyBrute(a, k)) ok = false;
        }
        check("300 random arrays vs brute force", true, ok);
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
