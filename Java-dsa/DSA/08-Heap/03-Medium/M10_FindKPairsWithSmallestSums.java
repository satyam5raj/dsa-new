/*
 * ============================================================================
 *  MEDIUM 10 : FIND K PAIRS WITH SMALLEST SUMS   LeetCode #373
 *  https://leetcode.com/problems/find-k-pairs-with-smallest-sums/
 *  Pattern: K-WAY MERGE over an implicit sorted matrix (only expand what you need)
 * ============================================================================
 *
 *  PROBLEM
 *    Two SORTED arrays. Return the k pairs (u from nums1, v from nums2) with the
 *    smallest sums u + v.
 *      nums1 [1,7,11], nums2 [2,4,6], k=3 -> [[1,2],[1,4],[1,6]]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: all n*m pairs, sort by sum -> O(nm log(nm)); n, m up to 10^5 -> 10^10 pairs!
 *
 *  KEY INSIGHT: picture the pair sums as a matrix S[i][j] = nums1[i] + nums2[j].
 *  Every ROW i is sorted (nums2 is sorted). That's "k sorted lists" -> k-way merge:
 *     start with S[i][0] for each row i (only the first min(n, k) rows: row k and
 *     beyond can never be among the k smallest, because S[0..k-1][0] are all smaller)
 *     poll the smallest {i, j}; push its right neighbour {i, j+1}
 *     stop after k polls
 *  We never touch more than ~2k cells.
 *
 *  DRY RUN [1,7,11] x [2,4,6], k=3
 *    heap {(0,0)=3, (1,0)=9, (2,0)=13}
 *    poll (0,0)=3 -> push (0,1)=5
 *    poll (0,1)=5 -> push (0,2)=7
 *    poll (0,2)=7 -> done: [1,2],[1,4],[1,6]
 *
 *  COMPLEXITY   Time O(k log min(n, k))   Space O(min(n, k))
 * ============================================================================
 */

import java.util.*;

public class M10_FindKPairsWithSmallestSums {

    static List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> nums1[e[0]] + nums2[e[1]]));
        for (int i = 0; i < Math.min(nums1.length, k); i++) heap.offer(new int[]{i, 0});   // column 0 of each row
        List<List<Integer>> result = new ArrayList<>();
        while (result.size() < k && !heap.isEmpty()) {
            int[] e = heap.poll();
            result.add(List.of(nums1[e[0]], nums2[e[1]]));
            if (e[1] + 1 < nums2.length) heap.offer(new int[]{e[0], e[1] + 1});           // next in the row
        }
        return result;
    }

    public static void main(String[] args) {
        check("[1,7,11] [2,4,6] k=3", "[[1, 2], [1, 4], [1, 6]]", kSmallestPairs(new int[]{1, 7, 11}, new int[]{2, 4, 6}, 3));
        check("[1,1,2] [1,2,3] k=2", "[[1, 1], [1, 1]]", kSmallestPairs(new int[]{1, 1, 2}, new int[]{1, 2, 3}, 2));
        check("k larger than all pairs", 4, kSmallestPairs(new int[]{1, 2}, new int[]{3, 4}, 10).size());
        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(8), -10, 10).sorted().toArray();
            int[] b = rnd.ints(1 + rnd.nextInt(8), -10, 10).sorted().toArray();
            int k = 1 + rnd.nextInt(a.length * b.length + 3);
            List<Integer> allSums = new ArrayList<>();
            for (int x : a) for (int y : b) allSums.add(x + y);
            Collections.sort(allSums);
            List<Integer> got = new ArrayList<>();
            for (List<Integer> p : kSmallestPairs(a, b, k)) got.add(p.get(0) + p.get(1));
            // ties make the exact pairs ambiguous; the SUMS must match the k smallest sums
            ok &= got.equals(allSums.subList(0, Math.min(k, allSums.size())));
        }
        check("500 random cases: sums == k smallest", true, ok);
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
