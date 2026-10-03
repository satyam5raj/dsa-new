/*
 * ============================================================================
 *  HARD 02 : INSERT DELETE GETRANDOM O(1) - DUPLICATES ALLOWED  ⭐ bonus   LeetCode #381
 *  https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/
 *  Pattern: M06 with value -> SET OF INDICES
 * ============================================================================
 *
 *  PROBLEM
 *    Like M06 (RandomizedSet), but it's a MULTISET: the same value may be inserted
 *    several times. getRandom's probability is proportional to each value's count.
 *      insert(1) -> true (new), insert(1) -> false (already present), insert(2) -> true
 *      getRandom -> 1 with probability 2/3, 2 with probability 1/3
 *
 *  ---------------------------------------------------------------------------
 *  WHAT CHANGES FROM M06
 *    A value now lives at SEVERAL indices of the list:
 *      map: value -> Set<Integer> of indices   (a LinkedHashSet/HashSet: O(1) add/remove)
 *    getRandom: unchanged. A random list index is automatically weighted by the
 *    counts (a value stored 2 times owns 2 slots).
 *
 *  REMOVE(val) - swap with the last, but now with index SETS
 *    i    = ANY index of val (take one from its set)
 *    last = the value in the last list slot (index L)
 *    1. list[i] = last;  in last's index set: replace L with i
 *    2. remove i from val's set (careful when val == last: steps 1 and 2 touch the same set!)
 *    3. drop the last list slot
 *  ORDER OF SET UPDATES MATTERS when val == last. The safe order:
 *      valSet.remove(i);  lastSet.add(i);  lastSet.remove(L);   (when i == L, lastSet.add(i) re-adds what remove(L) then takes away)
 *  The random test below exercises exactly that case (many duplicates).
 *
 *  COMPLEXITY   all operations O(1) average
 * ============================================================================
 */

import java.util.*;

public class H02_RandomizedCollection {

    static class RandomizedCollection {
        private final List<Integer> values = new ArrayList<>();
        private final Map<Integer, Set<Integer>> indices = new HashMap<>();
        private final Random rnd = new Random();

        public boolean insert(int val) {
            Set<Integer> set = indices.computeIfAbsent(val, k -> new HashSet<>());
            boolean wasAbsent = set.isEmpty();
            set.add(values.size());
            values.add(val);
            return wasAbsent;
        }

        public boolean remove(int val) {
            Set<Integer> valSet = indices.get(val);
            if (valSet == null || valSet.isEmpty()) return false;
            int i = valSet.iterator().next();                 // any position holding val
            int lastIndex = values.size() - 1;
            int last = values.get(lastIndex);

            valSet.remove(i);                                 // val no longer at i
            Set<Integer> lastSet = indices.get(last);
            lastSet.add(i);                                   // 'last' moves into slot i
            lastSet.remove(lastIndex);                        // ...and leaves the last slot
            values.set(i, last);
            values.remove(lastIndex);
            // If i == lastIndex, the add then the remove cancel out, leaving the sets consistent.
            if (valSet.isEmpty()) indices.remove(val);
            return true;
        }

        public int getRandom() { return values.get(rnd.nextInt(values.size())); }

        int size() { return values.size(); }
    }

    public static void main(String[] args) {
        RandomizedCollection rc = new RandomizedCollection();
        check("insert 1 (new)", true, rc.insert(1));
        check("insert 1 (duplicate)", false, rc.insert(1));
        check("insert 2", true, rc.insert(2));
        int ones = 0;
        for (int i = 0; i < 30_000; i++) if (rc.getRandom() == 1) ones++;
        System.out.printf("   P(getRandom == 1) ~ %.3f (expected 2/3 = 0.667)%n", ones / 30_000.0);
        check("getRandom weighted by count", true, Math.abs(ones / 30_000.0 - 2.0 / 3) < 0.02);
        check("remove 1", true, rc.remove(1));
        check("remove 1 (the second copy)", true, rc.remove(1));
        check("remove 1 (none left)", false, rc.remove(1));

        Random rnd = new Random(9);
        RandomizedCollection mine = new RandomizedCollection();
        Map<Integer, Integer> ref = new HashMap<>();                 // value -> count
        boolean ok = true;
        for (int i = 0; i < 50_000; i++) {
            int v = rnd.nextInt(6);                                   // tiny range: lots of duplicates
            if (rnd.nextBoolean()) {
                ok &= mine.insert(v) == !ref.containsKey(v);
                ref.merge(v, 1, Integer::sum);
            } else {
                boolean present = ref.containsKey(v);
                ok &= mine.remove(v) == present;
                if (present && ref.merge(v, -1, Integer::sum) == 0) ref.remove(v);
            }
            int total = ref.values().stream().mapToInt(Integer::intValue).sum();
            ok &= mine.size() == total;
            if (total > 0) ok &= ref.containsKey(mine.getRandom());
        }
        check("50,000 random ops vs a count map", true, ok);
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
