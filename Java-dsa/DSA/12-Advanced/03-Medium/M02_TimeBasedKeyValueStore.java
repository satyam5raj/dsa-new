/*
 * ============================================================================
 *  MEDIUM 02 : TIME BASED KEY-VALUE STORE  ⭐ bonus   LeetCode #981
 *  https://leetcode.com/problems/time-based-key-value-store/
 *  Pattern: HashMap<key, list of (time, value)> + BINARY SEARCH for "latest <= t"
 * ============================================================================
 *
 *  PROBLEM
 *    set(key, value, timestamp); get(key, timestamp) returns the value set at the
 *    LARGEST timestamp_prev <= timestamp, or "" if none. Timestamps for set() are
 *    strictly increasing.
 *      set(foo, bar, 1); get(foo, 1) = bar; get(foo, 3) = bar;
 *      set(foo, bar2, 4); get(foo, 4) = bar2; get(foo, 5) = bar2; get(foo, 0) = ""
 *
 *  ---------------------------------------------------------------------------
 *  This is a VERSIONED map (like MVCC in databases, or git history for one key).
 *
 *  Because set() timestamps only increase, each key's history is APPENDED in sorted
 *  order -> an ArrayList per key, already sorted. get = binary search for the
 *  rightmost time <= t ("floor"):
 *     lo = 0, hi = size - 1, ans = -1
 *     while lo <= hi: mid; if time[mid] <= t: ans = mid, lo = mid + 1 else hi = mid - 1
 *
 *  ALTERNATIVE: TreeMap<Integer, String> per key and floorEntry(t). Same O(log n),
 *  less code, more memory per entry, and works even if timestamps arrive unsorted.
 *
 *  COMPLEXITY   set O(1) amortized, get O(log n)   Space O(total sets)
 * ============================================================================
 */

import java.util.*;

public class M02_TimeBasedKeyValueStore {

    static class TimeMap {
        private final Map<String, List<Integer>> times = new HashMap<>();
        private final Map<String, List<String>> values = new HashMap<>();

        public void set(String key, String value, int timestamp) {
            times.computeIfAbsent(key, k -> new ArrayList<>()).add(timestamp);   // appended in sorted order
            values.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        public String get(String key, int timestamp) {
            List<Integer> ts = times.get(key);
            if (ts == null) return "";
            int lo = 0, hi = ts.size() - 1, ans = -1;
            while (lo <= hi) {                                   // rightmost time <= timestamp
                int mid = (lo + hi) >>> 1;
                if (ts.get(mid) <= timestamp) { ans = mid; lo = mid + 1; }
                else hi = mid - 1;
            }
            return ans == -1 ? "" : values.get(key).get(ans);
        }
    }

    static class TreeMapVersion {
        private final Map<String, TreeMap<Integer, String>> m = new HashMap<>();
        void set(String k, String v, int t) { m.computeIfAbsent(k, x -> new TreeMap<>()).put(t, v); }
        String get(String k, int t) {
            TreeMap<Integer, String> tm = m.get(k);
            if (tm == null) return "";
            Map.Entry<Integer, String> e = tm.floorEntry(t);
            return e == null ? "" : e.getValue();
        }
    }

    public static void main(String[] args) {
        TimeMap tm = new TimeMap();
        tm.set("foo", "bar", 1);
        check("get(foo,1)", "bar", tm.get("foo", 1));
        check("get(foo,3)", "bar", tm.get("foo", 3));
        tm.set("foo", "bar2", 4);
        check("get(foo,4)", "bar2", tm.get("foo", 4));
        check("get(foo,5)", "bar2", tm.get("foo", 5));
        check("get(foo,0) (too early)", "", tm.get("foo", 0));
        check("unknown key", "", tm.get("baz", 9));
        Random rnd = new Random(3);
        TimeMap a = new TimeMap();
        TreeMapVersion b = new TreeMapVersion();
        boolean ok = true;
        int now = 0;
        for (int i = 0; i < 50_000; i++) {
            String k = "k" + rnd.nextInt(20);
            if (rnd.nextBoolean()) { now += 1 + rnd.nextInt(3); a.set(k, "v" + i, now); b.set(k, "v" + i, now); }
            else { int t = rnd.nextInt(now + 5); ok &= a.get(k, t).equals(b.get(k, t)); }
        }
        check("50,000 random ops: binary search == TreeMap", true, ok);
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
