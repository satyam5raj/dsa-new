/*
 * ============================================================================
 *  MEDIUM 11 : CAR FLEET                     LeetCode #853
 *  https://leetcode.com/problems/car-fleet/
 *  Pattern: SORT BY POSITION + MONOTONIC STACK OF ARRIVAL TIMES
 * ============================================================================
 *
 *  PROBLEM
 *    Cars drive toward 'target' on a one-lane road (no overtaking). A faster car
 *    that catches a slower one ahead joins it and drives at the slower speed:
 *    together they form a FLEET. How many fleets arrive at the target?
 *      target 12, position [10,8,0,5,3], speed [2,4,1,1,3] -> 3
 *
 *  ---------------------------------------------------------------------------
 *  KEY IDEAS
 *    1. Compute each car's arrival time ALONE:  time = (target - position) / speed
 *    2. Process cars from CLOSEST to the target to FARTHEST (sort by position descending).
 *    3. A car behind catches the fleet ahead iff its solo time <= that fleet's time
 *       (it would arrive earlier or at the same time, but it can't pass, so it merges).
 *       Otherwise it's slower and forms a NEW fleet, which becomes the new "fleet ahead".
 *
 *  STACK OF FLEET TIMES (strictly increasing from the target backwards)
 *    for each car, closest first:
 *       if stack empty or time > stack.top -> push time   (a new, slower fleet)
 *       else                               -> it merges   (do nothing)
 *    answer = stack size
 *  (Only the top is ever compared, so a single variable would also do. The stack
 *   shows the monotonic structure.)
 *
 *  DRY RUN target 12
 *    sorted by position: (10,2) t=1 | (8,4) t=1 | (5,1) t=7 | (3,3) t=3 | (0,1) t=12
 *    t=1  -> push              [1]
 *    t=1  -> 1 <= 1 merges     [1]
 *    t=7  -> new fleet         [1,7]
 *    t=3  -> 3 <= 7 merges     [1,7]
 *    t=12 -> new fleet         [1,7,12]   -> 3 fleets
 *
 *  COMPLEXITY   Time O(n log n) (sorting)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M11_CarFleet {

    static int carFleet(int target, int[] position, int[] speed) {
        int n = position.length;
        double[][] cars = new double[n][2];                       // [position, arrival time]
        for (int i = 0; i < n; i++) cars[i] = new double[]{position[i], (double) (target - position[i]) / speed[i]};
        Arrays.sort(cars, (a, b) -> Double.compare(b[0], a[0]));  // closest to the target first

        Deque<Double> fleets = new ArrayDeque<>();
        for (double[] car : cars) {
            if (fleets.isEmpty() || car[1] > fleets.peek()) {
                fleets.push(car[1]);                              // can't catch the fleet ahead
            }                                                     // else: merges into it
        }
        return fleets.size();
    }

    public static void main(String[] args) {
        check("target 12 example", 3, carFleet(12, new int[]{10, 8, 0, 5, 3}, new int[]{2, 4, 1, 1, 3}));
        check("single car", 1, carFleet(10, new int[]{3}, new int[]{3}));
        check("all merge into one", 1, carFleet(100, new int[]{0, 2, 4}, new int[]{4, 2, 1}));
        check("nobody catches anyone", 3, carFleet(10, new int[]{6, 8, 0}, new int[]{3, 2, 1}));   // times: 1.33, 1, 10 sorted by pos: 8(1),6(1.33),0(10)
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
