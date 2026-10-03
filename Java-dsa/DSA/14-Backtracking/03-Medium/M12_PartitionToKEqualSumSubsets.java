/*
 * ============================================================================
 *  MEDIUM 12 : PARTITION TO K EQUAL SUM SUBSETS   LeetCode #698
 *  https://leetcode.com/problems/partition-to-k-equal-sum-subsets/
 *  Pattern: put each number into one of k BUCKETS, with heavy pruning
 * ============================================================================
 *
 *  PROBLEM
 *    Can nums be split into k non-empty groups with equal sums?
 *      [4,3,2,3,5,2,1], k=4 -> true   ((5), (1,4), (2,3), (2,3))
 *      [1,2,3,4], k=3       -> false
 *
 *  ---------------------------------------------------------------------------
 *  QUICK CHECKS: total % k != 0 -> false; target = total / k; any number > target -> false.
 *
 *  BUCKET BACKTRACKING: place nums[i] into some bucket whose sum + nums[i] <= target;
 *  recurse on i + 1; take it out again. All placed -> true (every bucket must then be
 *  exactly target, because the total is k * target).
 *  PRUNING (it's exponential without these):
 *    1. sort DESCENDING: big numbers fail fast, near the root of the tree
 *    2. SYMMETRY: if two buckets currently have the same sum, putting nums[i] in either
 *       gives the same subtree -> try only the first of them. In particular, once a
 *       number fails in an EMPTY bucket, it'll fail in every other empty bucket: break.
 *
 *  ALTERNATIVE: bitmask DP over "which numbers are used" (2^n states) - the referee.
 *
 *  DRY RUN [5,4,3,3,2,2,1], target 5: 5 -> b0; 4 -> b1; 3 -> b2; 3 -> b3; 2 -> b2 (5);
 *          2 -> b3 (5); 1 -> b1 (5) -> true
 *
 *  COMPLEXITY   O(k^n) worst case, far less with pruning   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M12_PartitionToKEqualSumSubsets {

    static boolean canPartitionKSubsets(int[] nums, int k) {
        int total = Arrays.stream(nums).sum();
        if (total % k != 0) return false;
        int target = total / k;
        Integer[] a = Arrays.stream(nums).boxed().toArray(Integer[]::new);
        Arrays.sort(a, Collections.reverseOrder());               // big numbers first
        if (a[0] > target) return false;
        return place(a, 0, new int[k], target);
    }

    static boolean place(Integer[] a, int i, int[] bucket, int target) {
        if (i == a.length) return true;
        Set<Integer> triedSums = new HashSet<>();
        for (int b = 0; b < bucket.length; b++) {
            if (bucket[b] + a[i] > target) continue;
            if (!triedSums.add(bucket[b])) continue;              // same sum as a bucket already tried: symmetric
            bucket[b] += a[i];
            if (place(a, i + 1, bucket, target)) return true;
            bucket[b] -= a[i];
        }
        return false;
    }

    // referee: DP over subsets. reach[mask] = sum of mask's numbers modulo target, if mask is buildable
    static boolean bitmaskDp(int[] nums, int k) {
        int total = Arrays.stream(nums).sum(), n = nums.length;
        if (total % k != 0) return false;
        int target = total / k;
        int[] rem = new int[1 << n];
        Arrays.fill(rem, -1);
        rem[0] = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (rem[mask] < 0) continue;
            for (int i = 0; i < n; i++) {
                if ((mask >> i & 1) == 1 || rem[mask] + nums[i] > target) continue;   // fill buckets one at a time
                rem[mask | 1 << i] = (rem[mask] + nums[i]) % target;
            }
        }
        return rem[(1 << n) - 1] == 0;
    }

    public static void main(String[] args) {
        check("[4,3,2,3,5,2,1] k=4", true, canPartitionKSubsets(new int[]{4, 3, 2, 3, 5, 2, 1}, 4));
        check("[1,2,3,4] k=3", false, canPartitionKSubsets(new int[]{1, 2, 3, 4}, 3));
        check("[2,2,2,2,3,4,5] k=4", false, canPartitionKSubsets(new int[]{2, 2, 2, 2, 3, 4, 5}, 4));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(12), 1, 10).toArray();
            int k = 1 + rnd.nextInt(Math.min(a.length, 5));
            ok &= canPartitionKSubsets(a, k) == bitmaskDp(a, k);
        }
        check("1,000 random cases vs bitmask DP", true, ok);
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
