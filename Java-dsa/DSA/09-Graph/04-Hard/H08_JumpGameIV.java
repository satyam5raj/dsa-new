/*
 * ============================================================================
 *  HARD 08 : JUMP GAME IV                    LeetCode #1345
 *  https://leetcode.com/problems/jump-game-iv/
 *  Pattern: BFS on indices + "use each value's teleport group ONCE" (clear it)
 * ============================================================================
 *
 *  PROBLEM
 *    From index i you can jump to i+1, i-1, or ANY j with arr[j] == arr[i].
 *    Fewest jumps from index 0 to the last index?
 *      [100,-23,-23,404,100,23,23,23,3,404] -> 3   (0 -> 4 -> 3 -> 9)
 *
 *  ---------------------------------------------------------------------------
 *  BFS (each jump costs 1). Neighbours of i: i-1, i+1, and every index with the
 *  same value (precomputed in a map value -> list of indices).
 *
 *  THE TRAP: an array of 50,000 sevens. Every index has 49,999 "same value"
 *  neighbours -> scanning them from every index is O(n^2) = 2.5 * 10^9.
 *
 *  FIX: once ANY index with value v is processed, ALL indices with value v are
 *  enqueued (or already visited). Scanning that list again from another index can
 *  never find anything new. So CLEAR the list after the first use:
 *      for j in sameValue[arr[i]]: enqueue if unvisited
 *      sameValue[arr[i]].clear()     <- each group is scanned exactly once
 *  Total work: O(n).
 *
 *  DRY RUN: 0 (100) -> neighbours 1 and 4 (other 100)      level 1
 *           4 -> 3 (404's group also has 9)               level 2: {2, 3, 5}
 *           3 (404) -> 9 = last                           level 3
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H08_JumpGameIV {

    static int minJumps(int[] arr) {
        int n = arr.length;
        if (n == 1) return 0;
        Map<Integer, List<Integer>> sameValue = new HashMap<>();
        for (int i = 0; i < n; i++) sameValue.computeIfAbsent(arr[i], k -> new ArrayList<>()).add(i);
        boolean[] seen = new boolean[n];
        seen[0] = true;
        Queue<Integer> q = new ArrayDeque<>(List.of(0));
        int jumps = 0;
        while (!q.isEmpty()) {
            jumps++;
            for (int k = q.size(); k > 0; k--) {
                int i = q.poll();
                List<Integer> group = sameValue.get(arr[i]);
                group.add(i - 1);                              // reuse the list to scan the 2 neighbours too
                group.add(i + 1);
                for (int j : group) {
                    if (j < 0 || j >= n || seen[j]) continue;
                    if (j == n - 1) return jumps;
                    seen[j] = true;
                    q.add(j);
                }
                group.clear();                                 // this value's teleports are used up
            }
        }
        return -1;
    }

    // referee: plain BFS without the clearing trick (O(n^2), fine for small n)
    static int plainBfs(int[] arr) {
        int n = arr.length;
        int[] d = new int[n];
        Arrays.fill(d, -1);
        d[0] = 0;
        Queue<Integer> q = new ArrayDeque<>(List.of(0));
        while (!q.isEmpty()) {
            int i = q.poll();
            for (int j = 0; j < n; j++) {
                boolean edge = j == i - 1 || j == i + 1 || (j != i && arr[j] == arr[i]);
                if (edge && d[j] == -1) { d[j] = d[i] + 1; q.add(j); }
            }
        }
        return d[n - 1];
    }

    public static void main(String[] args) {
        check("example 1", 3, minJumps(new int[]{100, -23, -23, 404, 100, 23, 23, 23, 3, 404}));
        check("[7]", 0, minJumps(new int[]{7}));
        check("[7,6,9,6,9,6,9,7]", 1, minJumps(new int[]{7, 6, 9, 6, 9, 6, 9, 7}));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(20), 0, 5).toArray();
            ok &= minJumps(a) == plainBfs(a);
        }
        check("1,000 random arrays vs plain BFS", true, ok);
        int[] big = new int[50_000];
        Arrays.fill(big, 7);
        big[49_999] = 8;
        long t0 = System.nanoTime();
        int r = minJumps(big);
        System.out.printf("   50,000 sevens (+ an 8 at the end): %d jumps in %d ms%n", r, (System.nanoTime() - t0) / 1_000_000);
        check("big same-value array", 2, r);
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
