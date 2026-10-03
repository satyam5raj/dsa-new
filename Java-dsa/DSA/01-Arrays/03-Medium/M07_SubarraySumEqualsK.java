/*
 * ============================================================================
 *  MEDIUM 07 : SUBARRAY SUM EQUALS K         LeetCode #560
 *  https://leetcode.com/problems/subarray-sum-equals-k/
 *  Pattern: PREFIX SUM + HASHMAP (count of earlier prefix sums)
 * ============================================================================
 *
 *  PROBLEM
 *    Count the CONTIGUOUS subarrays whose sum equals k. Values can be NEGATIVE.
 *      [1,1,1], k=2   -> 2      ([1,1] twice)
 *      [1,2,3], k=3   -> 2      ([1,2] and [3])
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every start, extend the end with a running sum -> O(n^2)
 *
 *  WHY NOT SLIDING WINDOW? Sliding windows rely on "adding an element makes the
 *  sum bigger". With NEGATIVE numbers that's false, so we can't know when to
 *  shrink. -> We need a different idea.
 *
 *  KEY INSIGHT (prefix sums)
 *    Let prefix(j) = sum of nums[0..j]. The sum of subarray (i..j] is
 *         prefix(j) - prefix(i)
 *    We want  prefix(j) - prefix(i) == k   <=>   prefix(i) == prefix(j) - k
 *    So, at position j: "how many EARLIER prefixes equal (current - k)?"
 *    Each one is the start of a valid subarray ending at j.
 *    -> keep a HashMap  prefixSum -> how many times we've seen it.
 *
 *  THE {0: 1} SEED: the "empty prefix" before index 0 has sum 0. Without it,
 *  subarrays starting at index 0 would be missed ([1,2] with k=3: prefix 3 - 3 = 0
 *  must be found once).
 *
 *  DRY RUN [1,2,3], k=3       map starts {0:1}
 *    x=1  prefix=1  need 1-3=-2 -> 0 found   map {0:1, 1:1}
 *    x=2  prefix=3  need 3-3=0  -> 1 found   map {0:1, 1:1, 3:1}      ([1,2])
 *    x=3  prefix=6  need 6-3=3  -> 1 found   map {..., 6:1}           ([3])
 *    total = 2
 *
 *  ORDER MATTERS: look up BEFORE inserting the current prefix (otherwise k=0
 *  would count the empty subarray).
 *  COMPLEXITY   Time O(n)   Space O(n)
 *  SAME TECHNIQUE: "continuous subarray sum divisible by k" (store prefix % k),
 *  "longest subarray with sum k" (store the FIRST index of each prefix).
 * ============================================================================
 */

import java.util.*;

public class M07_SubarraySumEqualsK {

    static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> countOfPrefix = new HashMap<>();
        countOfPrefix.put(0, 1);                       // the empty prefix
        int prefix = 0, count = 0;
        for (int x : nums) {
            prefix += x;
            count += countOfPrefix.getOrDefault(prefix - k, 0);   // subarrays ending here
            countOfPrefix.merge(prefix, 1, Integer::sum);         // record AFTER looking up
        }
        return count;
    }

    static int subarraySumBrute(int[] nums, int k) {
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            int sum = 0;
            for (int j = i; j < nums.length; j++) {
                sum += nums[j];
                if (sum == k) count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        check("[1,1,1] k=2", 2, subarraySum(new int[]{1, 1, 1}, 2));
        check("[1,2,3] k=3", 2, subarraySum(new int[]{1, 2, 3}, 3));
        check("[1,-1,0] k=0 (negatives & zero)", 3, subarraySum(new int[]{1, -1, 0}, 0));
        check("[3,4,7,2,-3,1,4,2] k=7", 4, subarraySum(new int[]{3, 4, 7, 2, -3, 1, 4, 2}, 7));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), -5, 6).toArray();
            int k = rnd.nextInt(11) - 5;
            if (subarraySum(a, k) != subarraySumBrute(a, k)) ok = false;
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
