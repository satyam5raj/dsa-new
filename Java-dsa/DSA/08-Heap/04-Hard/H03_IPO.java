/*
 * ============================================================================
 *  HARD 03 : IPO                             LeetCode #502
 *  https://leetcode.com/problems/ipo/
 *  Pattern: SORT + HEAP - "unlock" by one key (capital), "choose" by another (profit)
 * ============================================================================
 *
 *  PROBLEM
 *    You have capital w and may finish at most k projects. Project i needs
 *    capital[i] to START (it isn't spent) and adds profits[i] to your capital.
 *    Maximize the final capital.
 *      k=2, w=0, profits [1,2,3], capital [0,1,1] -> 4
 *        w=0: only project 0 is affordable -> w=1; now 1 and 2 are -> take 3 -> w=4
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY: at each step, among the projects you can AFFORD right now, take the most
 *  PROFITABLE. Profit only increases capital, so taking the biggest profit now
 *  never closes any door: it can only unlock more projects.
 *
 *  EFFICIENT: two structures
 *    projects sorted by capital needed  (a pointer walks through them)
 *    a MAX-heap of profits of the projects unlocked so far
 *    repeat k times:
 *       move every project with capital <= w from the sorted list into the heap
 *       if the heap is empty -> nothing affordable, stop early
 *       w += heap.poll()
 *  Each project enters/leaves the heap once -> O(n log n).
 *  This shape (sort by A, sweep, heap by B) appears in many "hard" greedy problems:
 *  Course Schedule III, Max Performance, Hire K Workers, Meeting Rooms.
 *
 *  DRY RUN k=3, w=0, profits [1,2,3], capital [0,1,2]
 *    unlock cap<=0: {1} -> take 1, w=1
 *    unlock cap<=1: {2} -> take 2, w=3
 *    unlock cap<=3: {3} -> take 3, w=6
 *
 *  COMPLEXITY   Time O(n log n + k log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H03_IPO {

    static int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
        int n = profits.length;
        Integer[] byCapital = new Integer[n];
        for (int i = 0; i < n; i++) byCapital[i] = i;
        Arrays.sort(byCapital, Comparator.comparingInt(i -> capital[i]));
        PriorityQueue<Integer> affordable = new PriorityQueue<>(Comparator.reverseOrder());   // profits
        int next = 0;
        for (int step = 0; step < k; step++) {
            while (next < n && capital[byCapital[next]] <= w) affordable.offer(profits[byCapital[next++]]);
            if (affordable.isEmpty()) break;                    // can't afford anything more
            w += affordable.poll();                             // the best project we can start
        }
        return w;
    }

    // brute force: try every order of choices (small n)
    static int brute(int k, int w, int[] profits, int[] capital, boolean[] used) {
        int best = w;
        if (k == 0) return w;
        for (int i = 0; i < profits.length; i++) {
            if (!used[i] && capital[i] <= w) {
                used[i] = true;
                best = Math.max(best, brute(k - 1, w + profits[i], profits, capital, used));
                used[i] = false;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("k=2 w=0 [1,2,3] [0,1,1]", 4, findMaximizedCapital(2, 0, new int[]{1, 2, 3}, new int[]{0, 1, 1}));
        check("k=3 w=0 [1,2,3] [0,1,2]", 6, findMaximizedCapital(3, 0, new int[]{1, 2, 3}, new int[]{0, 1, 2}));
        check("nothing affordable", 0, findMaximizedCapital(1, 0, new int[]{5}, new int[]{1}));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(6), k = 1 + rnd.nextInt(n), w = rnd.nextInt(5);
            int[] p = rnd.ints(n, 0, 10).toArray(), c = rnd.ints(n, 0, 15).toArray();
            ok &= findMaximizedCapital(k, w, p, c) == brute(k, w, p, c, new boolean[n]);
        }
        check("1,000 random cases vs all orders", true, ok);
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
