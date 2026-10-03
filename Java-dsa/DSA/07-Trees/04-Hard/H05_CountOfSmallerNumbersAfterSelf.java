/*
 * ============================================================================
 *  HARD 05 : COUNT OF SMALLER NUMBERS AFTER SELF  ⭐ bonus   LeetCode #315
 *  https://leetcode.com/problems/count-of-smaller-numbers-after-self/
 *  Pattern: FENWICK TREE over VALUES (+ coordinate compression), scan right to left
 * ============================================================================
 *
 *  PROBLEM
 *    counts[i] = how many elements to the RIGHT of nums[i] are SMALLER than it.
 *      [5,2,6,1] -> [2,1,1,0]
 *        5: {2,1} smaller -> 2;  2: {1} -> 1;  6: {1} -> 1;  1: none -> 0
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each i, scan everything to its right -> O(n^2). n = 10^5 -> 10^10.
 *
 *  KEY IDEA: scan from RIGHT to LEFT, keeping a "bag" of the values already seen
 *  (= the elements to the right of i). For each nums[i]:
 *     counts[i] = how many values in the bag are < nums[i]      <- a PREFIX COUNT query
 *     then put nums[i] into the bag                             <- a POINT UPDATE
 *  "Prefix count + point update", both in O(log n) = a FENWICK TREE (01-Basics),
 *  indexed by VALUE: tree[v] = how many times value v is in the bag.
 *
 *  COORDINATE COMPRESSION: values go from -10^4 to 10^4 (or up to 10^9 in other
 *  problems) - too big/negative for array indices. Only their ORDER matters, so
 *  replace each value by its rank among the distinct sorted values: 1, 2, 3...
 *     [5,2,6,1] -> sorted distinct [1,2,5,6] -> ranks [3,2,4,1]
 *
 *  DRY RUN (ranks [3,2,4,1]), right to left
 *    i=3 rank 1: count(ranks < 1) = 0          add 1    bag {1}
 *    i=2 rank 4: count(< 4) = 1                add 4    bag {1,4}
 *    i=1 rank 2: count(< 2) = 1                add 2    bag {1,2,4}
 *    i=0 rank 3: count(< 3) = 2 (ranks 1, 2)   add 3
 *    -> [2,1,1,0]
 *
 *  OTHER ROUTES: merge sort counting how many from the right half jump over each
 *  left element (like counting inversions), or a BST with subtree sizes (M04's
 *  follow-up). The Fenwick tree is the shortest to write.
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H05_CountOfSmallerNumbersAfterSelf {

    static List<Integer> countSmaller(int[] nums) {
        // 1. coordinate compression: value -> rank 1..m
        int[] sorted = Arrays.stream(nums).distinct().sorted().toArray();
        int m = sorted.length;

        int[] tree = new int[m + 1];                     // Fenwick tree over ranks
        Integer[] counts = new Integer[nums.length];
        for (int i = nums.length - 1; i >= 0; i--) {     // 2. right to left
            int rank = Arrays.binarySearch(sorted, nums[i]) + 1;
            int smaller = 0;
            for (int x = rank - 1; x > 0; x -= x & -x) smaller += tree[x];   // count of ranks < rank
            counts[i] = smaller;
            for (int x = rank; x <= m; x += x & -x) tree[x]++;              // add this value to the bag
        }
        return Arrays.asList(counts);
    }

    static List<Integer> brute(int[] nums) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            int c = 0;
            for (int j = i + 1; j < nums.length; j++) if (nums[j] < nums[i]) c++;
            out.add(c);
        }
        return out;
    }

    public static void main(String[] args) {
        check("[5,2,6,1]", "[2, 1, 1, 0]", countSmaller(new int[]{5, 2, 6, 1}));
        check("[-1]", "[0]", countSmaller(new int[]{-1}));
        check("[-1,-1] (equal is not smaller)", "[0, 0]", countSmaller(new int[]{-1, -1}));
        check("[3,2,1,0] (descending)", "[3, 2, 1, 0]", countSmaller(new int[]{3, 2, 1, 0}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(60), -20, 21).toArray();
            ok &= countSmaller(a).equals(brute(a));
        }
        check("500 random arrays vs O(n^2)", true, ok);
        int[] big = rnd.ints(100_000, -10_000, 10_001).toArray();
        long t0 = System.nanoTime();
        List<Integer> r = countSmaller(big);
        System.out.printf("   n = 100,000 in %d ms (brute force would do ~5 * 10^9 comparisons)%n", (System.nanoTime() - t0) / 1_000_000);
        check("big input size", 100_000, r.size());
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
