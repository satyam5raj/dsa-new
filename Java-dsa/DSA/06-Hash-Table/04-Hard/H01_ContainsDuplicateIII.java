/*
 * ============================================================================
 *  HARD 01 : CONTAINS DUPLICATE III          LeetCode #220
 *  https://leetcode.com/problems/contains-duplicate-iii/
 *  Pattern: SLIDING WINDOW + VALUE BUCKETS of width t+1 (hashing by RANGE)
 * ============================================================================
 *
 *  PROBLEM
 *    Find indices i != j with |i - j| <= indexDiff AND |nums[i] - nums[j]| <= valueDiff.
 *      [1,2,3,1], indexDiff 3, valueDiff 0       -> true
 *      [1,5,9,1,5,9], indexDiff 2, valueDiff 3   -> false
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: compare each element with the previous indexDiff ones -> O(n * k)
 *
 *  APPROACH 1: SLIDING WINDOW + TreeSet                O(n log k)
 *    Keep the last k values in a sorted set. For x, ask for the smallest value
 *    >= x - t (set.ceiling). If it's <= x + t -> found.
 *
 *  APPROACH 2: BUCKETS (hashing by value range)        O(n)
 *    Split the number line into buckets of width w = t + 1:
 *       bucket id = floor(x / w)      e.g. t = 3: [0..3] -> 0, [4..7] -> 1, [-4..-1] -> -1
 *    - two values in the SAME bucket differ by at most t -> immediately a match
 *    - a match can also be in a NEIGHBOUR bucket (id-1 or id+1): check its one value
 *    - nothing farther can match (the difference would be > t)
 *    Each bucket holds at most ONE value (a second one would have matched already),
 *    so a HashMap  bucketId -> value  is enough. Keep only the last k elements:
 *    remove the bucket of nums[i - k] when it leaves the window.
 *
 *  NEGATIVE NUMBERS: Java's '/' rounds toward ZERO (-1 / 4 == 0), which would put -1
 *  and 1 in the same bucket. Use Math.floorDiv(x, w) for a true floor.
 *  OVERFLOW: use long for the differences (values near ±2^31).
 *
 *  COMPLEXITY   Time O(n)   Space O(min(n, k))
 * ============================================================================
 */

import java.util.*;

public class H01_ContainsDuplicateIII {

    static boolean containsNearbyAlmostDuplicate(int[] nums, int indexDiff, int valueDiff) {
        long w = (long) valueDiff + 1;
        Map<Long, Long> buckets = new HashMap<>();             // bucket id -> the value in it
        for (int i = 0; i < nums.length; i++) {
            long x = nums[i];
            long id = Math.floorDiv(x, w);
            if (buckets.containsKey(id)) return true;           // same bucket -> within valueDiff
            Long left = buckets.get(id - 1), right = buckets.get(id + 1);
            if (left != null && x - left <= valueDiff) return true;
            if (right != null && right - x <= valueDiff) return true;
            buckets.put(id, x);
            if (i >= indexDiff) buckets.remove(Math.floorDiv((long) nums[i - indexDiff], w));   // slide the window
        }
        return false;
    }

    static boolean withTreeSet(int[] nums, int k, int t) {
        TreeSet<Long> window = new TreeSet<>();
        for (int i = 0; i < nums.length; i++) {
            Long c = window.ceiling((long) nums[i] - t);
            if (c != null && c <= (long) nums[i] + t) return true;
            window.add((long) nums[i]);
            if (i >= k) window.remove((long) nums[i - k]);
        }
        return false;
    }

    static boolean brute(int[] a, int k, int t) {
        for (int i = 0; i < a.length; i++)
            for (int j = i + 1; j < a.length && j - i <= k; j++)
                if (Math.abs((long) a[i] - a[j]) <= t) return true;
        return false;
    }

    public static void main(String[] args) {
        check("[1,2,3,1] k=3 t=0", true, containsNearbyAlmostDuplicate(new int[]{1, 2, 3, 1}, 3, 0));
        check("[1,5,9,1,5,9] k=2 t=3", false, containsNearbyAlmostDuplicate(new int[]{1, 5, 9, 1, 5, 9}, 2, 3));
        check("[-3,3] k=2 t=4 (negatives)", false, containsNearbyAlmostDuplicate(new int[]{-3, 3}, 2, 4));
        check("[-1,1] k=1 t=1 (floorDiv matters)", false, containsNearbyAlmostDuplicate(new int[]{-1, 1}, 1, 1));
        check("extreme values", false, containsNearbyAlmostDuplicate(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}, 1, 1));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int i = 0; i < 2000; i++) {
            int[] a = rnd.ints(1 + rnd.nextInt(15), -20, 21).toArray();
            int k = 1 + rnd.nextInt(5), t = rnd.nextInt(6);
            boolean b = brute(a, k, t);
            ok &= containsNearbyAlmostDuplicate(a, k, t) == b && withTreeSet(a, k, t) == b;
        }
        check("2,000 random cases (buckets & TreeSet) vs brute", true, ok);
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
