/*
 * ============================================================================
 *  HARD 02 : SMALLEST RANGE COVERING ELEMENTS FROM K LISTS   LeetCode #632
 *  https://leetcode.com/problems/smallest-range-covering-elements-from-k-lists/
 *  Pattern: K-WAY MERGE with a min-heap + track the CURRENT MAX
 * ============================================================================
 *
 *  PROBLEM
 *    k sorted lists. Find the smallest range [a, b] that contains at least one
 *    number from EACH list. (Smaller = smaller b - a; ties -> smaller a.)
 *      [[4,10,15,24,26],[0,9,12,20],[5,18,22,30]] -> [20,24]
 *        (24 from list 1, 20 from list 2, 22 from list 3)
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: keep exactly ONE "current" element from each list. The range that
 *  covers them is [min of them, max of them]. To possibly shrink it, the only
 *  useful move is to advance the list that holds the MIN (advancing any other
 *  pointer only raises the max or keeps it -> the range can't get smaller).
 *
 *  ALGORITHM
 *    heap = min-heap of {value, list, index}, one per list; curMax = the max of them
 *    loop:
 *      e = heap.poll()                          -> the current min
 *      record [e.value, curMax] if it's better
 *      if e's list is exhausted -> STOP (the min can't move any further, so no smaller range exists)
 *      push the next element of e's list; curMax = max(curMax, it)
 *  This is the k-way merge (HeapPatterns #2) with one extra variable. It's also a
 *  sliding window over the merged sequence that always holds all k lists.
 *
 *  DRY RUN (first steps)
 *    heap {0,4,5}, max 5 -> range [0,5] (5)
 *    pop 0 (list 2) -> push 9, max 9 -> heap {4,5,9}: range [4,9] (5, not smaller)
 *    pop 4 -> push 10, max 10 -> [5,10] ... eventually [20,24] (4)
 *
 *  COMPLEXITY   Time O(N log k)  (N = total elements)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class H02_SmallestRangeCoveringKLists {

    static int[] smallestRange(List<List<Integer>> nums) {
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));   // {value, list, idx}
        int curMax = Integer.MIN_VALUE;
        for (int i = 0; i < nums.size(); i++) {
            int v = nums.get(i).get(0);
            heap.offer(new int[]{v, i, 0});
            curMax = Math.max(curMax, v);
        }
        int bestLo = 0, bestHi = Integer.MAX_VALUE;
        while (true) {
            int[] e = heap.poll();                                   // the current minimum
            if ((long) curMax - e[0] < (long) bestHi - bestLo) { bestLo = e[0]; bestHi = curMax; }
            int next = e[2] + 1;
            if (next == nums.get(e[1]).size()) break;                // this list is used up
            int v = nums.get(e[1]).get(next);
            heap.offer(new int[]{v, e[1], next});
            curMax = Math.max(curMax, v);
        }
        return new int[]{bestLo, bestHi};
    }

    // brute force: every candidate low value; for each list the smallest element >= low; range = max - low
    static int[] brute(List<List<Integer>> nums) {
        int bestLo = 0, bestHi = Integer.MAX_VALUE;
        Set<Integer> candidates = new TreeSet<>();
        for (List<Integer> l : nums) candidates.addAll(l);
        for (int lo : candidates) {
            int hi = lo;
            boolean ok = true;
            for (List<Integer> l : nums) {
                Integer c = null;
                for (int x : l) if (x >= lo) { c = x; break; }
                if (c == null) { ok = false; break; }
                hi = Math.max(hi, c);
            }
            if (ok && (long) hi - lo < (long) bestHi - bestLo) { bestLo = lo; bestHi = hi; }
        }
        return new int[]{bestLo, bestHi};
    }

    public static void main(String[] args) {
        List<List<Integer>> ex = List.of(List.of(4, 10, 15, 24, 26), List.of(0, 9, 12, 20), List.of(5, 18, 22, 30));
        check("example", "[20, 24]", smallestRange(ex));
        check("[[1,2,3],[1,2,3],[1,2,3]]", "[1, 1]", smallestRange(List.of(List.of(1, 2, 3), List.of(1, 2, 3), List.of(1, 2, 3))));
        check("single list", "[5, 5]", smallestRange(List.of(List.of(5, 7))));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            List<List<Integer>> lists = new ArrayList<>();
            for (int i = 0, k = 1 + rnd.nextInt(4); i < k; i++) {
                List<Integer> l = new ArrayList<>();
                for (int j = 0, n = 1 + rnd.nextInt(6); j < n; j++) l.add(rnd.nextInt(41) - 20);
                Collections.sort(l);
                lists.add(l);
            }
            ok &= Arrays.equals(smallestRange(lists), brute(lists));
        }
        check("1,000 random cases vs brute force", true, ok);
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
