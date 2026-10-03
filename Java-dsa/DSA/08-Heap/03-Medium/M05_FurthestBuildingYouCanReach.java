/*
 * ============================================================================
 *  MEDIUM 05 : FURTHEST BUILDING YOU CAN REACH   LeetCode #1642
 *  https://leetcode.com/problems/furthest-building-you-can-reach/
 *  Pattern: GREEDY with a heap - "use ladders on the k biggest climbs SO FAR"
 * ============================================================================
 *
 *  PROBLEM
 *    Walk from building 0 to the right. Going DOWN or level is free. Going UP by d
 *    costs either d bricks or one ladder (any height). Given `bricks` and `ladders`,
 *    how far (index) can you get?
 *      heights [4,2,7,6,9,14,12], bricks 5, ladders 1 -> 4
 *        4->2 free, 2->7 (+5) bricks, 7->6 free, 6->9 (+3) ladder, 9->14 (+5) nothing left
 *
 *  ---------------------------------------------------------------------------
 *  THE DILEMMA: when you meet a climb, you don't know the future - should this
 *  climb get a ladder or bricks?
 *
 *  KEY INSIGHT: decide IN HINDSIGHT. Ladders are most valuable on the BIGGEST
 *  climbs. So keep the climbs that currently "own" a ladder in a MIN-heap:
 *    for each climb d > 0:
 *      give it a ladder: heap.offer(d)
 *      if the heap now holds more climbs than ladders:
 *         take back the SMALLEST laddered climb and pay for it with bricks instead
 *         bricks -= heap.poll()
 *         if bricks < 0 -> we can't pass this building: answer = i
 *  At every moment the ladders cover the k largest climbs seen so far, and bricks
 *  pay for the rest - the cheapest possible brick usage. (Top-K again!)
 *
 *  DRY RUN [4,2,7,6,9,14,12], b=5, l=1
 *    i0->1: down
 *    i1->2: +5 -> heap {5} (1 ladder ok)
 *    i2->3: down
 *    i3->4: +3 -> heap {3,5} > 1 ladder -> bricks pay 3 -> b = 2, heap {5}
 *    i4->5: +5 -> heap {5,5} -> bricks pay 5 -> b = -3 < 0 -> stop at 4
 *
 *  COMPLEXITY   Time O(n log L)   Space O(L)   (L = ladders)
 * ============================================================================
 */

import java.util.*;

public class M05_FurthestBuildingYouCanReach {

    static int furthestBuilding(int[] heights, int bricks, int ladders) {
        PriorityQueue<Integer> laddered = new PriorityQueue<>();      // climbs covered by ladders
        for (int i = 0; i < heights.length - 1; i++) {
            int climb = heights[i + 1] - heights[i];
            if (climb <= 0) continue;                                 // free
            laddered.offer(climb);
            if (laddered.size() > ladders) {
                bricks -= laddered.poll();                            // smallest climb -> bricks
                if (bricks < 0) return i;
            }
        }
        return heights.length - 1;
    }

    // brute force: try every assignment of ladders to climbs via DFS
    static int brute(int[] h, int bricks, int ladders) {
        return dfs(h, 0, bricks, ladders);
    }

    static int dfs(int[] h, int i, int bricks, int ladders) {
        if (i == h.length - 1) return i;
        int climb = h[i + 1] - h[i];
        if (climb <= 0) return dfs(h, i + 1, bricks, ladders);
        int best = i;
        if (bricks >= climb) best = Math.max(best, dfs(h, i + 1, bricks - climb, ladders));
        if (ladders > 0) best = Math.max(best, dfs(h, i + 1, bricks, ladders - 1));
        return best;
    }

    public static void main(String[] args) {
        check("[4,2,7,6,9,14,12] b5 l1", 4, furthestBuilding(new int[]{4, 2, 7, 6, 9, 14, 12}, 5, 1));
        check("[4,12,2,7,3,18,20,3,19] b10 l2", 7, furthestBuilding(new int[]{4, 12, 2, 7, 3, 18, 20, 3, 19}, 10, 2));
        check("[14,3,19,3] b17 l0", 3, furthestBuilding(new int[]{14, 3, 19, 3}, 17, 0));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            int[] h = rnd.ints(1 + rnd.nextInt(12), 1, 20).toArray();
            int b = rnd.nextInt(25), l = rnd.nextInt(3);
            ok &= furthestBuilding(h, b, l) == brute(h, b, l);
        }
        check("2,000 random cases vs trying all choices", true, ok);
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
