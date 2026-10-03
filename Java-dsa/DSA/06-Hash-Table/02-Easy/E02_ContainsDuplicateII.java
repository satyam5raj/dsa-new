/*
 * ============================================================================
 *  EASY 02 : CONTAINS DUPLICATE II           LeetCode #219
 *  https://leetcode.com/problems/contains-duplicate-ii/
 *  Pattern: HASHMAP OF LAST INDEX  /  SLIDING-WINDOW HASHSET of size k
 * ============================================================================
 *
 *  PROBLEM
 *    Are there two EQUAL values whose indices are at most k apart?
 *      [1,2,3,1], k=3       -> true   (indices 0 and 3)
 *      [1,0,1,1], k=1       -> true   (indices 2 and 3)
 *      [1,2,3,1,2,3], k=2   -> false
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: MAP value -> the LAST index where it appeared
 *    For each i: if the map has nums[i] at index j with i - j <= k -> true.
 *    Always update the map to the LATEST index: the nearest previous copy is
 *    the best candidate for future matches.
 *
 *  APPROACH 2: SLIDING WINDOW SET (the window holds the last k values)
 *    for each i:
 *      if set contains nums[i] -> true   (a copy within the last k positions)
 *      add nums[i]
 *      if set.size() > k: remove nums[i - k]   (it's now too far behind)
 *    Memory O(k) instead of O(n).
 *
 *  DRY RUN approach 2, [1,2,3,1], k=3
 *    1 -> {1}   2 -> {1,2}   3 -> {1,2,3}   1 -> already in the set -> true
 *
 *  The follow-up (#220, Contains Duplicate III) also allows the VALUES to differ
 *  by up to t. That one is Hard (H01), solved with value buckets.
 *  COMPLEXITY   Time O(n)   Space O(min(n, k))
 * ============================================================================
 */

import java.util.*;

public class E02_ContainsDuplicateII {

    static boolean containsNearbyDuplicateMap(int[] nums, int k) {
        Map<Integer, Integer> lastIndex = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer j = lastIndex.put(nums[i], i);           // put returns the previous index (or null)
            if (j != null && i - j <= k) return true;
        }
        return false;
    }

    static boolean containsNearbyDuplicate(int[] nums, int k) {
        Set<Integer> window = new HashSet<>();
        for (int i = 0; i < nums.length; i++) {
            if (!window.add(nums[i])) return true;          // already in the last k values
            if (window.size() > k) window.remove(nums[i - k]);
        }
        return false;
    }

    public static void main(String[] args) {
        check("[1,2,3,1] k=3", true, containsNearbyDuplicate(new int[]{1, 2, 3, 1}, 3));
        check("[1,0,1,1] k=1", true, containsNearbyDuplicate(new int[]{1, 0, 1, 1}, 1));
        check("[1,2,3,1,2,3] k=2", false, containsNearbyDuplicate(new int[]{1, 2, 3, 1, 2, 3}, 2));
        check("k=0 (never)", false, containsNearbyDuplicate(new int[]{1, 1}, 0));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), 0, 8).toArray();
            int k = rnd.nextInt(6);
            ok &= containsNearbyDuplicate(a, k) == containsNearbyDuplicateMap(a, k);
        }
        check("window set == last-index map (1,000 cases)", true, ok);
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
