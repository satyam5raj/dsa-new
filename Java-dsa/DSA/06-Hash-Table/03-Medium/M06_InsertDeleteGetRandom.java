/*
 * ============================================================================
 *  MEDIUM 06 : INSERT DELETE GETRANDOM O(1)  ⭐ bonus   LeetCode #380
 *  https://leetcode.com/problems/insert-delete-getrandom-o1/
 *  Pattern: HASHMAP (value -> index) + ARRAY LIST, with SWAP-WITH-LAST deletion
 * ============================================================================
 *
 *  PROBLEM
 *    A set with insert(val), remove(val) and getRandom() (each element equally
 *    likely), ALL in average O(1).
 *
 *  ---------------------------------------------------------------------------
 *  WHY ONE STRUCTURE ISN'T ENOUGH
 *    HashSet : insert/remove O(1), but getRandom needs random INDEX access -> no
 *    ArrayList: getRandom O(1) (list.get(random index)), but remove(val) needs a
 *               search O(n) plus a shift O(n)
 *  COMBINE THEM:
 *    list : the values (for random access)
 *    map  : value -> its index in the list (to find it in O(1))
 *
 *  THE TRICK FOR O(1) REMOVE: removing from the MIDDLE of an ArrayList shifts
 *  everything after it. Instead:
 *    1. move the LAST element into the hole (overwrite the removed value's slot)
 *    2. update the moved element's index in the map
 *    3. remove the last slot: O(1), nothing shifts
 *  Order doesn't matter for a set, so this is allowed.
 *
 *  DRY RUN list [5,8,3], map {5:0, 8:1, 3:2};  remove(5)
 *    last = 3 -> list[0] = 3 -> map[3] = 0 -> drop the last slot
 *    list [3,8], map {3:0, 8:1}
 *
 *  This "map + array, swap with the last" design also shows up in game engines
 *  (entity lists) and in O(1) sampling problems.
 *  COMPLEXITY   all operations O(1) average
 * ============================================================================
 */

import java.util.*;

public class M06_InsertDeleteGetRandom {

    static class RandomizedSet {
        private final List<Integer> values = new ArrayList<>();
        private final Map<Integer, Integer> indexOf = new HashMap<>();
        private final Random rnd = new Random();

        public boolean insert(int val) {
            if (indexOf.containsKey(val)) return false;
            indexOf.put(val, values.size());
            values.add(val);
            return true;
        }

        public boolean remove(int val) {
            Integer i = indexOf.get(val);
            if (i == null) return false;
            int last = values.get(values.size() - 1);
            values.set(i, last);                       // 1. move the last element into the hole
            indexOf.put(last, i);                      // 2. fix its index
            values.remove(values.size() - 1);          // 3. drop the last slot: O(1)
            indexOf.remove(val);                       //    (do this AFTER step 2, in case val == last)
            return true;
        }

        public int getRandom() {
            return values.get(rnd.nextInt(values.size()));
        }

        int size() { return values.size(); }
    }

    public static void main(String[] args) {
        RandomizedSet rs = new RandomizedSet();
        check("insert 1", true, rs.insert(1));
        check("remove 2 (absent)", false, rs.remove(2));
        check("insert 2", true, rs.insert(2));
        check("insert 2 again", false, rs.insert(2));
        check("remove 1", true, rs.remove(1));
        check("getRandom (only 2 left)", 2, rs.getRandom());

        Random rnd = new Random(6);
        RandomizedSet mine = new RandomizedSet();
        Set<Integer> ref = new HashSet<>();
        boolean ok = true;
        for (int i = 0; i < 50_000; i++) {
            int v = rnd.nextInt(200);
            if (rnd.nextBoolean()) ok &= mine.insert(v) == ref.add(v);
            else ok &= mine.remove(v) == ref.remove(v);
            if (!ref.isEmpty()) ok &= ref.contains(mine.getRandom());
            ok &= mine.size() == ref.size();
        }
        check("50,000 random ops vs HashSet", true, ok);

        RandomizedSet uni = new RandomizedSet();
        for (int v = 0; v < 4; v++) uni.insert(v);
        int[] hits = new int[4];
        for (int i = 0; i < 40_000; i++) hits[uni.getRandom()]++;
        boolean fair = true;
        for (int h : hits) fair &= Math.abs(h - 10_000) < 600;
        System.out.println("   getRandom over 40,000 draws: " + Arrays.toString(hits) + " (about 10,000 each)");
        check("getRandom is uniform", true, fair);
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
