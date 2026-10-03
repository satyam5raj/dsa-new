/*
 * ============================================================================
 *  MEDIUM 20 : LARGEST DIVISIBLE SUBSET      LeetCode #368
 *  https://leetcode.com/problems/largest-divisible-subset/
 *  Pattern: SORT, then LIS where "increasing" becomes "divides" (+ reconstruction)
 * ============================================================================
 *
 *  PROBLEM
 *    Distinct positive integers. Return the largest subset where every pair (a, b)
 *    has a % b == 0 or b % a == 0.
 *      [1,2,3]     -> [1,2] (or [1,3])
 *      [1,2,4,8]   -> [1,2,4,8]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: check all 2^n subsets.
 *
 *  KEY INSIGHT: after SORTING, a valid subset is a CHAIN a1 | a2 | a3 | ...
 *  Divisibility is transitive: if a | b and b | c then a | c. So to extend a chain
 *  ending at b with c, you only check the LAST element: b | c.
 *  That's exactly LIS with the comparison "a[j] < a[i]" replaced by "a[i] % a[j] == 0":
 *      dp[i] = 1 + max(dp[j] : j < i, a[i] % a[j] == 0),   parent[i] = that j
 *  The problem asks for the SUBSET itself -> follow parent pointers from the best end.
 *
 *  DRY RUN [1,2,4,8,9,3]  sorted [1,2,3,4,8,9]
 *    dp: 1:1  2:2  3:2  4:3(via 2)  8:4(via 4)  9:3(via 3)  -> chain 1,2,4,8
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M20_LargestDivisibleSubset {

    static List<Integer> largestDivisibleSubset(int[] nums) {
        int[] a = nums.clone();
        Arrays.sort(a);
        int n = a.length, end = 0;
        int[] dp = new int[n], parent = new int[n];
        for (int i = 0; i < n; i++) {
            dp[i] = 1; parent[i] = -1;
            for (int j = 0; j < i; j++)
                if (a[i] % a[j] == 0 && dp[j] + 1 > dp[i]) { dp[i] = dp[j] + 1; parent[i] = j; }
            if (dp[i] > dp[end]) end = i;
        }
        LinkedList<Integer> out = new LinkedList<>();
        for (int i = end; i != -1; i = parent[i]) out.addFirst(a[i]);
        return out;
    }

    static boolean valid(List<Integer> s) {
        for (int i = 0; i < s.size(); i++) for (int j = i + 1; j < s.size(); j++)
            if (s.get(i) % s.get(j) != 0 && s.get(j) % s.get(i) != 0) return false;
        return true;
    }

    static int bruteSize(int[] a) {
        int best = 0;
        for (int mask = 1; mask < 1 << a.length; mask++) {
            List<Integer> s = new ArrayList<>();
            for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s.add(a[i]);
            if (valid(s)) best = Math.max(best, s.size());
        }
        return best;
    }

    public static void main(String[] args) {
        check("[1,2,3]", "[1, 2]", largestDivisibleSubset(new int[]{1, 2, 3}));
        check("[1,2,4,8]", "[1, 2, 4, 8]", largestDivisibleSubset(new int[]{1, 2, 4, 8}));
        check("[1,2,4,8,9,3]", "[1, 2, 4, 8]", largestDivisibleSubset(new int[]{1, 2, 4, 8, 9, 3}));
        check("[5]", "[5]", largestDivisibleSubset(new int[]{5}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            Set<Integer> set = new HashSet<>();
            int n = 1 + rnd.nextInt(12);
            while (set.size() < n) set.add(1 + rnd.nextInt(60));
            int[] a = set.stream().mapToInt(Integer::intValue).toArray();
            List<Integer> s = largestDivisibleSubset(a);
            ok &= valid(s) && s.size() == bruteSize(a);
        }
        check("500 random: valid + max size vs 2^n", true, ok);
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
