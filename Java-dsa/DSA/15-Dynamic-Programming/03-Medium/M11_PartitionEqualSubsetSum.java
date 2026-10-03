/*
 * ============================================================================
 *  MEDIUM 11 : PARTITION EQUAL SUBSET SUM    LeetCode #416
 *  https://leetcode.com/problems/partition-equal-subset-sum/
 *  Pattern: 0/1 knapsack, BOOLEAN ("can a subset reach this sum?")
 * ============================================================================
 *
 *  PROBLEM
 *    Can nums be split into two subsets with equal sums?
 *      [1,5,11,5] -> true ([1,5,5] and [11])       [1,2,3,5] -> false
 *
 *  ---------------------------------------------------------------------------
 *  REDUCTION: two equal halves <=> some subset sums to total/2 (the rest is the other
 *  half). If total is odd -> false immediately.
 *
 *  SUBSET SUM (0/1 knapsack, each number used at most once):
 *      STATE       can[s] = some subset of the numbers seen so far sums to s
 *      TRANSITION  for each x: for s = target DOWN TO x:  can[s] |= can[s - x]
 *      BASE        can[0] = true (empty subset)
 *  The loop runs DOWNWARD so can[s - x] still means "without x" (0/1, not unbounded).
 *
 *  BITSET TRICK: represent can[] as bits; adding x is   bits |= bits << x.
 *  64 sums per machine word -> up to ~64x faster. (C++ has std::bitset; in Java,
 *  BigInteger's shiftLeft/or does the same job and keeps the code short.)
 *
 *  DRY RUN [1,5,11,5], target 11
 *    x=1:  {0,1}      x=5: {0,1,5,6}      x=11: {...,11}  -> true
 *
 *  COMPLEXITY   Time O(n * sum)   Space O(sum)
 * ============================================================================
 */

import java.math.BigInteger;
import java.util.*;

public class M11_PartitionEqualSubsetSum {

    static boolean canPartition(int[] nums) {
        int total = 0;
        for (int x : nums) total += x;
        if (total % 2 == 1) return false;
        int target = total / 2;
        boolean[] can = new boolean[target + 1];
        can[0] = true;
        for (int x : nums)
            for (int s = target; s >= x; s--)            // backwards: each number used once
                can[s] |= can[s - x];
        return can[target];
    }

    static boolean canPartitionBits(int[] nums) {
        int total = Arrays.stream(nums).sum();
        if (total % 2 == 1) return false;
        BigInteger bits = BigInteger.ONE;                 // bit s set = sum s reachable
        for (int x : nums) bits = bits.or(bits.shiftLeft(x));
        return bits.testBit(total / 2);
    }

    static boolean brute(int[] a) {
        int total = Arrays.stream(a).sum();
        for (int mask = 0; mask < 1 << a.length; mask++) {
            int s = 0;
            for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s += a[i];
            if (2 * s == total) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        check("[1,5,11,5]", true, canPartition(new int[]{1, 5, 11, 5}));
        check("[1,2,3,5]", false, canPartition(new int[]{1, 2, 3, 5}));
        check("[2,2]", true, canPartition(new int[]{2, 2}));
        check("[1] (odd total)", false, canPartition(new int[]{1}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = new int[1 + rnd.nextInt(12)];
            for (int i = 0; i < a.length; i++) a[i] = 1 + rnd.nextInt(20);
            boolean b = brute(a);
            ok &= canPartition(a) == b && canPartitionBits(a) == b;
        }
        check("1000 random vs all subsets (+bitset)", true, ok);
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
