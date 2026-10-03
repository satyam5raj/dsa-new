/*
 * ============================================================================
 *  EASY 02 : LAST STONE WEIGHT  ⭐ bonus       LeetCode #1046
 *  https://leetcode.com/problems/last-stone-weight/
 *  Pattern: SIMULATION with a MAX-heap ("repeatedly take the two largest")
 * ============================================================================
 *
 *  PROBLEM
 *    Each turn, smash the two HEAVIEST stones x <= y:
 *       x == y -> both destroyed;   x != y -> a stone of weight y - x remains.
 *    Return the last stone's weight, or 0 if none remain.
 *      [2,7,4,1,8,1] -> 1
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: sort the array every turn -> O(n^2 log n).
 *
 *  A MAX-HEAP gives "the largest" in O(log n), and lets us put the leftover back:
 *     while heap has >= 2:  y = poll, x = poll;  if y > x, offer(y - x)
 *     return heap empty ? 0 : peek
 *  Any time a problem says "repeatedly take the largest/smallest and put something
 *  back", think HEAP (also: Huffman coding, connecting ropes with minimum cost).
 *
 *  DRY RUN [2,7,4,1,8,1]
 *    8,7 -> 1   heap {4,2,1,1,1}
 *    4,2 -> 2   heap {2,1,1,1}
 *    2,1 -> 1   heap {1,1,1}
 *    1,1 -> 0   heap {1}        -> 1
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E02_LastStoneWeight {

    static int lastStoneWeight(int[] stones) {
        PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.reverseOrder());   // max-heap
        for (int s : stones) heap.offer(s);
        while (heap.size() > 1) {
            int y = heap.poll(), x = heap.poll();        // the two heaviest, y >= x
            if (y > x) heap.offer(y - x);
        }
        return heap.isEmpty() ? 0 : heap.peek();
    }

    static int brute(int[] stones) {
        List<Integer> s = new ArrayList<>();
        for (int x : stones) s.add(x);
        while (s.size() > 1) {
            Collections.sort(s);
            int y = s.remove(s.size() - 1), x = s.remove(s.size() - 1);
            if (y > x) s.add(y - x);
        }
        return s.isEmpty() ? 0 : s.get(0);
    }

    public static void main(String[] args) {
        check("[2,7,4,1,8,1]", 1, lastStoneWeight(new int[]{2, 7, 4, 1, 8, 1}));
        check("[1]", 1, lastStoneWeight(new int[]{1}));
        check("[3,3] (both destroyed)", 0, lastStoneWeight(new int[]{3, 3}));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), 1, 100).toArray();
            ok &= lastStoneWeight(a) == brute(a);
        }
        check("500 random inputs vs re-sorting", true, ok);
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
