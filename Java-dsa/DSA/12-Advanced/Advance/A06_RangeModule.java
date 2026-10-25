/*
 * ============================================================================
 *  ADVANCE 06 : RANGE MODULE  ⭐ bonus   LeetCode #715
 *  https://leetcode.com/problems/range-module/
 *  Pattern: TreeMap<start, end> of disjoint half-open intervals: merge on add, split on remove
 * ============================================================================
 *
 *  PROBLEM
 *    Track ranges of real numbers, half-open [left, right).
 *      addRange(l, r)     start tracking every number in [l, r)
 *      queryRange(l, r)   true if EVERY number in [l, r) is tracked
 *      removeRange(l, r)  stop tracking every number in [l, r)
 *    addRange(10,20)  removeRange(14,16)  ->  tracked [10,14) [16,20)
 *    query(10,14) true   query(13,15) false   query(16,17) true
 *
 *  ---------------------------------------------------------------------------
 *  KEEP THE MAP CLEAN: intervals are disjoint AND never touch ([1,3) + [3,5) is stored
 *  as [1,5)). Then a query is ONE lookup: the interval starting at or before l must
 *  reach r.
 *
 *  addRange(l, r):
 *    1. if the interval starting at or before l reaches l, start the merge from it
 *    2. swallow every interval starting inside [l, r], stretching r to cover it
 *    3. put [l, r)
 *  removeRange(l, r):
 *    1. the interval starting BEFORE l and running past l: cut it at l, and if it ran
 *       past r too, keep its tail [r, end)
 *    2. every interval starting inside [l, r): delete it, keeping a tail [r, end) if any
 *
 *  DRY RUN removeRange(14,16) on {10:20}
 *    lowerKey(14) = 10, ends at 20 > 14 -> put 10:14; 20 > 16 -> put 16:20
 *    ceilingKey(14) = 16, not < 16 -> stop.          map {10:14, 16:20}
 *    query(13,15): floorKey(13) = 10, ends at 14 < 15 -> false
 *
 *  COMPLEXITY   O(k log n) per add/remove for k intervals touched (amortised O(log n))
 *               query O(log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A06_RangeModule {

    // ---------------- Approach 1: one boolean per unit cell (the referee, small coordinates only) ----------------
    static class CellRangeModule {
        private final boolean[] tracked;                     // tracked[x] = is [x, x+1) tracked?
        CellRangeModule(int maxCoordinate) { tracked = new boolean[maxCoordinate]; }
        public void addRange(int left, int right) { for (int x = left; x < right; x++) tracked[x] = true; }
        public boolean queryRange(int left, int right) { for (int x = left; x < right; x++) if (!tracked[x]) return false; return true; }
        public void removeRange(int left, int right) { for (int x = left; x < right; x++) tracked[x] = false; }
    }

    // ---------------- Approach 2: TreeMap of intervals (main solution) ----------------
    static class RangeModule {
        private final TreeMap<Integer, Integer> map = new TreeMap<>();   // start -> end (exclusive)

        public void addRange(int left, int right) {
            Integer start = map.floorKey(left);
            if (start != null && map.get(start) >= left) {               // overlaps or touches on the left
                left = start;
                right = Math.max(right, map.get(start));
            }
            Integer k;
            while ((k = map.ceilingKey(left)) != null && k <= right) {    // swallow everything starting in [left, right]
                right = Math.max(right, map.get(k));
                map.remove(k);
            }
            map.put(left, right);
        }

        public boolean queryRange(int left, int right) {
            Integer start = map.floorKey(left);
            return start != null && map.get(start) >= right;
        }

        public void removeRange(int left, int right) {
            Integer before = map.lowerKey(left);
            if (before != null && map.get(before) > left) {              // an interval sticks into [left, ...)
                int end = map.get(before);
                map.put(before, left);                                   // keep the part before left
                if (end > right) map.put(right, end);                    // and the part after right
            }
            Integer k;
            while ((k = map.ceilingKey(left)) != null && k < right) {     // intervals starting inside [left, right)
                int end = map.remove(k);
                if (end > right) map.put(right, end);                    // keep the tail past right
            }
        }
    }

    public static void main(String[] args) {
        RangeModule rm = new RangeModule();
        rm.addRange(10, 20);
        rm.removeRange(14, 16);
        check("query(10,14)", true, rm.queryRange(10, 14));
        check("query(13,15)", false, rm.queryRange(13, 15));
        check("query(16,17)", true, rm.queryRange(16, 17));
        rm.addRange(14, 16);
        check("re-adding the hole merges", "{10=20}", rm.map);
        rm.addRange(20, 25);
        check("touching ranges merge", "{10=25}", rm.map);

        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            RangeModule main = new RangeModule();
            CellRangeModule ref = new CellRangeModule(50);
            for (int i = 0; i < 100; i++) {
                int a = rnd.nextInt(49), b = a + 1 + rnd.nextInt(50 - a);    // 0 <= a < b <= 50
                int op = rnd.nextInt(3);
                if (op == 0) { main.addRange(a, b); ref.addRange(a, b); }
                else if (op == 1) { main.removeRange(a, b); ref.removeRange(a, b); }
                else ok &= main.queryRange(a, b) == ref.queryRange(a, b);
            }
            for (var e : main.map.entrySet()) {                          // the map stays clean
                Integer next = main.map.higherKey(e.getKey());
                ok &= e.getKey() < e.getValue() && (next == null || e.getValue() < next);
            }
        }
        check("50,000 random ops vs cells, map stays clean", true, ok);
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
