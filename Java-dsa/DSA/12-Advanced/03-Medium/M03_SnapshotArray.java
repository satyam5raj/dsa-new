/*
 * ============================================================================
 *  MEDIUM 03 : SNAPSHOT ARRAY  ⭐ bonus       LeetCode #1146
 *  https://leetcode.com/problems/snapshot-array/
 *  Pattern: store only the CHANGES per index (version lists) + binary search
 * ============================================================================
 *
 *  PROBLEM
 *    SnapshotArray(length): all zeros. set(index, val); snap() returns a snap_id
 *    (0, 1, 2...); get(index, snap_id) = the value at that index when that snapshot
 *    was taken.
 *      set(0,5); snap()=0; set(0,6); get(0,0) = 5
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: copy the whole array on every snap -> O(length) per snap. With 5*10^4
 *  snaps of a 5*10^4 array: 2.5*10^9 copies.
 *
 *  STORE ONLY CHANGES: for each index, a list of (snapId, value) pairs, recording
 *  the value as of each snapshot in which it CHANGED.
 *    set(i, v): if the last entry of index i has the CURRENT snapId, overwrite it;
 *               otherwise append (snapId, v)
 *    snap():    return snapId++               <- O(1), copies nothing!
 *    get(i, s): binary search index i's list for the last entry with snapId <= s
 *  Same idea as M02 (time-based store) and as copy-on-write / persistent data
 *  structures (git, database MVCC): share everything that didn't change.
 *
 *  DRY RUN set(0,5): idx0 = [(0,5)];  snap -> 0, now snapId 1
 *          set(0,6): idx0 = [(0,5),(1,6)];  get(0,0): last entry with id <= 0 -> 5
 *
 *  COMPLEXITY   set O(1), snap O(1), get O(log sets)   Space O(length + sets)
 * ============================================================================
 */

import java.util.*;

public class M03_SnapshotArray {

    static class SnapshotArray {
        private final List<List<int[]>> history = new ArrayList<>();   // per index: {snapId, value}
        private int snapId = 0;

        public SnapshotArray(int length) {
            for (int i = 0; i < length; i++) {
                List<int[]> h = new ArrayList<>();
                h.add(new int[]{0, 0});                                // everything starts at 0
                history.add(h);
            }
        }

        public void set(int index, int val) {
            List<int[]> h = history.get(index);
            int[] last = h.get(h.size() - 1);
            if (last[0] == snapId) last[1] = val;                      // still in the same snapshot: overwrite
            else h.add(new int[]{snapId, val});
        }

        public int snap() { return snapId++; }

        public int get(int index, int snap) {
            List<int[]> h = history.get(index);
            int lo = 0, hi = h.size() - 1;
            while (lo < hi) {                                          // last entry with id <= snap
                int mid = (lo + hi + 1) >>> 1;
                if (h.get(mid)[0] <= snap) lo = mid; else hi = mid - 1;
            }
            return h.get(lo)[1];
        }
    }

    public static void main(String[] args) {
        SnapshotArray sa = new SnapshotArray(3);
        sa.set(0, 5);
        check("snap", 0, sa.snap());
        sa.set(0, 6);
        check("get(0, 0)", 5, sa.get(0, 0));
        check("get(1, 0) untouched", 0, sa.get(1, 0));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 50; t++) {
            int len = 1 + rnd.nextInt(10);
            SnapshotArray mine = new SnapshotArray(len);
            int[] cur = new int[len];
            List<int[]> copies = new ArrayList<>();                    // naive: full copy per snap
            for (int i = 0; i < 1000; i++) {
                switch (rnd.nextInt(3)) {
                    case 0 -> { int idx = rnd.nextInt(len), v = rnd.nextInt(100); mine.set(idx, v); cur[idx] = v; }
                    case 1 -> { ok &= mine.snap() == copies.size(); copies.add(cur.clone()); }
                    default -> {
                        if (copies.isEmpty()) break;
                        int s = rnd.nextInt(copies.size()), idx = rnd.nextInt(len);
                        ok &= mine.get(idx, s) == copies.get(s)[idx];
                    }
                }
            }
        }
        check("50,000 random ops vs full copies", true, ok);
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
