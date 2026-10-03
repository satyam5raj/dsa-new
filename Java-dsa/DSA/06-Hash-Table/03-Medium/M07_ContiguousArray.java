/*
 * ============================================================================
 *  MEDIUM 07 : CONTIGUOUS ARRAY  ⭐ bonus   LeetCode #525
 *  https://leetcode.com/problems/contiguous-array/
 *  Pattern: TURN 0 INTO -1 -> "longest subarray with sum 0" -> first-index map
 * ============================================================================
 *
 *  PROBLEM
 *    Binary array. Length of the LONGEST contiguous subarray with an EQUAL number
 *    of 0s and 1s.
 *      [0,1]       -> 2
 *      [0,1,0]     -> 2
 *      [0,0,1,0,0,0,1,1] -> 6   (indices 2..7 = [1,0,0,0,1,1]: three 0s, three 1s)
 *
 *  ---------------------------------------------------------------------------
 *  THE REFRAMING TRICK: count each 0 as -1 and each 1 as +1.
 *  "Equal numbers of 0s and 1s"  <=>  "the subarray sums to 0".
 *
 *  LONGEST SUBARRAY WITH SUM 0 (prefix sums + hashing)
 *    sum(i+1 .. j) = P[j] - P[i] = 0  <=>  P[j] == P[i]
 *    For each j we want the EARLIEST i with the same prefix -> store the FIRST
 *    index of each prefix value (never overwrite). Seed {0: -1}.
 *    length = j - firstIndex[P[j]]
 *
 *  DRY RUN [0,1,0]  -> as -1,+1,-1        map {0:-1}
 *    j0 P=-1 -> new {-1:0}
 *    j1 P=0  -> seen at -1 -> length 1-(-1) = 2   <- best
 *    j2 P=-1 -> seen at 0  -> length 2-0 = 2
 *
 *  THE PREFIX-SUM + HASHMAP FAMILY (one idea, many problems)
 *    count subarrays with sum k      -> map: prefix -> COUNT          (Arrays/M07)
 *    any length >= 2, sum % k == 0   -> map: prefix % k -> FIRST index (M05)
 *    LONGEST subarray with sum 0/k   -> map: prefix -> FIRST index    (this one)
 *  COMPLEXITY   Time O(n)   Space O(n)  (or an int[2n+1] array instead of the map)
 * ============================================================================
 */

import java.util.*;

public class M07_ContiguousArray {

    static int findMaxLength(int[] nums) {
        Map<Integer, Integer> firstIndex = new HashMap<>();
        firstIndex.put(0, -1);
        int prefix = 0, best = 0;
        for (int j = 0; j < nums.length; j++) {
            prefix += nums[j] == 1 ? 1 : -1;                  // 0 counts as -1
            Integer i = firstIndex.putIfAbsent(prefix, j);   // stores j only if the prefix is new
            if (i != null) best = Math.max(best, j - i);     // seen before -> a balanced subarray (i, j]
        }
        return best;
    }

    static int brute(int[] a) {
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            int bal = 0;
            for (int j = i; j < a.length; j++) {
                bal += a[j] == 1 ? 1 : -1;
                if (bal == 0) best = Math.max(best, j - i + 1);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("[0,1]", 2, findMaxLength(new int[]{0, 1}));
        check("[0,1,0]", 2, findMaxLength(new int[]{0, 1, 0}));
        check("[0,0,1,0,0,0,1,1]", 6, findMaxLength(new int[]{0, 0, 1, 0, 0, 0, 1, 1}));
        check("[1,1,1] (no balance)", 0, findMaxLength(new int[]{1, 1, 1}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), 0, 2).toArray();
            ok &= findMaxLength(a) == brute(a);
        }
        check("1,000 random arrays vs brute force", true, ok);
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
