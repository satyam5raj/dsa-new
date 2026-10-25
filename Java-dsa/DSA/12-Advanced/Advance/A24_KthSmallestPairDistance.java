/*
 * ============================================================================
 *  ADVANCE 24 : FIND K-TH SMALLEST PAIR DISTANCE  ⭐ bonus   LeetCode #719
 *  https://leetcode.com/problems/find-k-th-smallest-pair-distance/
 *  Pattern: SORT + BINARY SEARCH ON THE ANSWER + TWO POINTERS to count  (or a heap)
 * ============================================================================
 *
 *  PROBLEM
 *    The distance of a pair (i < j) is |nums[i] - nums[j]|. Return the k-th smallest
 *    distance among all n(n-1)/2 pairs.
 *      [1,3,1], k = 1  ->  0      distances: |1-3|=2, |1-1|=0, |3-1|=2 -> sorted 0,2,2
 *      [1,6,1], k = 3  ->  5
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: list all ~n^2/2 distances and sort - with n = 10^4 that's 5 * 10^7
 *  numbers. Too much.
 *
 *  BINARY SEARCH ON THE ANSWER: ask "how many pairs have distance <= D?" This count
 *  only grows with D, so the answer is the SMALLEST D whose count is >= k.
 *    D ranges over [0, max - min].
 *
 *  COUNTING IN O(n) WITH TWO POINTERS (after sorting):
 *    for each right end r, move left forward while nums[r] - nums[left] > D; then every
 *    index in [left, r) pairs with r within D -> count += r - left.
 *    As r grows, left never moves back -> O(n) per count.
 *
 *  HEAP ALTERNATIVE: after sorting, the pairs (i, i+1), (i, i+2), ... have increasing
 *  distance for each i - n sorted lists. Merge them with a min-heap and pop k times:
 *  O((n + k) log n). Great for small k, bad for k ~ n^2.
 *
 *  DRY RUN [1,6,1] -> sorted [1,1,6], k = 3      lo 0, hi 5
 *    D 2: r=1 left 0 -> +1;  r=2: 6-1 > 2 -> left 2 -> +0   count 1 < 3 -> lo = 3
 *    D 4: same -> 1 < 3 -> lo = 5                              lo == hi -> 5
 *
 *  COMPLEXITY   O(n log n + n log(max - min))   Space O(1) besides sorting
 * ============================================================================
 */

import java.util.*;

public class A24_KthSmallestPairDistance {

    // ---------------- Referee: every distance, sorted ----------------
    static int smallestDistancePairBrute(int[] nums, int k) {
        List<Integer> d = new ArrayList<>();
        for (int i = 0; i < nums.length; i++)
            for (int j = i + 1; j < nums.length; j++) d.add(Math.abs(nums[i] - nums[j]));
        Collections.sort(d);
        return d.get(k - 1);
    }

    // ---------------- Approach 1: sort + min-heap merging n sorted lists ----------------
    static int smallestDistancePairHeap(int[] nums, int k) {
        int[] a = nums.clone();
        Arrays.sort(a);
        PriorityQueue<int[]> heap = new PriorityQueue<>((x, y) -> Integer.compare(a[x[1]] - a[x[0]], a[y[1]] - a[y[0]]));   // {i, j}
        for (int i = 0; i + 1 < a.length; i++) heap.add(new int[]{i, i + 1});   // the closest partner of each i
        int[] cur = null;
        for (int popped = 0; popped < k; popped++) {
            cur = heap.poll();
            if (cur[1] + 1 < a.length) heap.add(new int[]{cur[0], cur[1] + 1});   // next partner of the same i
        }
        return a[cur[1]] - a[cur[0]];
    }

    // ---------------- Approach 2: binary search on the distance + two pointers (main solution) ----------------
    static int smallestDistancePair(int[] nums, int k) {
        int[] a = nums.clone();
        Arrays.sort(a);
        int lo = 0, hi = a[a.length - 1] - a[0];
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (pairsWithin(a, mid) >= k) hi = mid;                       // at least k pairs are <= mid
            else lo = mid + 1;
        }
        return lo;
    }

    /** Number of pairs with distance <= d in a sorted array. */
    static long pairsWithin(int[] a, int d) {
        long count = 0;
        int left = 0;
        for (int right = 0; right < a.length; right++) {
            while (a[right] - a[left] > d) left++;
            count += right - left;                                        // a[left..right-1] are all within d of a[right]
        }
        return count;
    }

    public static void main(String[] args) {
        check("example 1", 0, smallestDistancePair(new int[]{1, 3, 1}, 1));
        check("example 2", 0, smallestDistancePair(new int[]{1, 1, 1}, 2));
        check("example 3", 5, smallestDistancePair(new int[]{1, 6, 1}, 3));
        check("example 3 (heap)", 5, smallestDistancePairHeap(new int[]{1, 6, 1}, 3));
        check("the largest pair", 99, smallestDistancePair(new int[]{0, 50, 99}, 3));

        Random rnd = new Random(24);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int n = 2 + rnd.nextInt(15);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(30);
            int k = 1 + rnd.nextInt(n * (n - 1) / 2);
            int want = smallestDistancePairBrute(a, k);
            ok &= smallestDistancePair(a, k) == want && smallestDistancePairHeap(a, k) == want;
        }
        check("2,000 random cases: search == heap == brute", true, ok);
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
