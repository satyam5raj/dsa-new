/*
 * ============================================================================
 *  ADVANCE 05 : DATA STREAM AS DISJOINT INTERVALS  ⭐ bonus   LeetCode #352
 *  https://leetcode.com/problems/data-stream-as-disjoint-intervals/
 *  Pattern: TreeMap<start, end> + floorKey / higherKey neighbours
 * ============================================================================
 *
 *  PROBLEM
 *    Numbers arrive one at a time. getIntervals() returns the numbers seen so far as
 *    sorted, disjoint intervals [start, end].
 *      add 1        -> [1,1]
 *      add 3        -> [1,1] [3,3]
 *      add 7        -> [1,1] [3,3] [7,7]
 *      add 2        -> [1,3] [7,7]          (2 joins 1 and 3)
 *      add 6        -> [1,3] [6,7]
 *
 *  ---------------------------------------------------------------------------
 *  A new number only affects its TWO NEIGHBOUR INTERVALS: the one starting at or before
 *  it (floorKey) and the one starting after it (higherKey). A TreeMap finds both in
 *  O(log n). Then there are 5 cases:
 *    already inside the left interval            -> nothing
 *    touches both (left.end == v-1, right == v+1) -> merge left + v + right
 *    touches only left                           -> extend left's end to v
 *    touches only right                          -> new start v, remove the old right
 *    touches neither                             -> new interval [v, v]
 *
 *  DRY RUN: add 2 when the map is {1:1, 3:3, 7:7}
 *    floorKey(2) = 1 (ends at 1 = 2-1, touches)   higherKey(2) = 3 (= 2+1, touches)
 *    -> map.put(1, map.get(3)=3), remove 3  ->  {1:3, 7:7}
 *
 *  COMPLEXITY   addNum O(log n)   getIntervals O(k) for k intervals   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class A05_DataStreamAsDisjointIntervals {

    // ---------------- Approach 1: keep every number, build intervals on demand (the referee) ----------------
    static class SetSummaryRanges {
        private final TreeSet<Integer> seen = new TreeSet<>();
        public void addNum(int value) { seen.add(value); }
        public int[][] getIntervals() {
            List<int[]> out = new ArrayList<>();
            for (int v : seen) {
                if (!out.isEmpty() && out.get(out.size() - 1)[1] == v - 1) out.get(out.size() - 1)[1] = v;   // extend the last run
                else out.add(new int[]{v, v});
            }
            return out.toArray(new int[0][]);
        }
    }

    // ---------------- Approach 2: TreeMap of intervals (main solution) ----------------
    static class SummaryRanges {
        private final TreeMap<Integer, Integer> intervals = new TreeMap<>();   // start -> end

        public void addNum(int value) {
            Integer lo = intervals.floorKey(value);                       // interval starting at or before value
            if (lo != null && intervals.get(lo) >= value) return;          // already covered
            Integer hi = intervals.higherKey(value);                       // interval starting after value
            boolean joinLeft = lo != null && intervals.get(lo) == value - 1;
            boolean joinRight = hi != null && hi == value + 1;
            if (joinLeft && joinRight) { intervals.put(lo, intervals.get(hi)); intervals.remove(hi); }
            else if (joinLeft) intervals.put(lo, value);
            else if (joinRight) intervals.put(value, intervals.remove(hi));
            else intervals.put(value, value);
        }

        public int[][] getIntervals() {
            int[][] out = new int[intervals.size()][];
            int i = 0;
            for (Map.Entry<Integer, Integer> e : intervals.entrySet()) out[i++] = new int[]{e.getKey(), e.getValue()};
            return out;
        }
    }

    public static void main(String[] args) {
        SummaryRanges sr = new SummaryRanges();
        StringBuilder trace = new StringBuilder();
        for (int v : new int[]{1, 3, 7, 2, 6}) { sr.addNum(v); trace.append(Arrays.deepToString(sr.getIntervals())).append(' '); }
        check("example stream 1,3,7,2,6", "[[1, 1]] [[1, 1], [3, 3]] [[1, 1], [3, 3], [7, 7]] [[1, 3], [7, 7]] [[1, 3], [6, 7]]", trace.toString().trim());
        sr.addNum(2);
        check("duplicate changes nothing", "[[1, 3], [6, 7]]", sr.getIntervals());
        sr.addNum(5); sr.addNum(4);
        check("4 joins everything into one", "[[1, 7]]", sr.getIntervals());

        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            SummaryRanges main = new SummaryRanges();
            SetSummaryRanges ref = new SetSummaryRanges();
            for (int i = 0; i < 60; i++) {
                int v = rnd.nextInt(80);
                main.addNum(v); ref.addNum(v);
                ok &= Arrays.deepEquals(main.getIntervals(), ref.getIntervals());
            }
        }
        check("30,000 random adds vs TreeSet rebuild", true, ok);
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
