/*
 * ============================================================================
 *  MEDIUM 12 : ASTEROID COLLISION            LeetCode #735
 *  https://leetcode.com/problems/asteroid-collision/
 *  Pattern: STACK SIMULATION (new element fights the top, possibly repeatedly)
 * ============================================================================
 *
 *  PROBLEM
 *    |value| = size, sign = direction (+ right, - left), all at the same speed.
 *    When two meet, the smaller one explodes; equal sizes both explode.
 *    Return the asteroids that survive.
 *      [5,10,-5]   -> [5,10]     (-5 hits 10 and explodes)
 *      [8,-8]      -> []         (equal: both explode)
 *      [10,2,-5]   -> [10]       (-5 destroys 2, then dies against 10)
 *      [-2,-1,1,2] -> [-2,-1,1,2] (they move apart: no collisions)
 *
 *  ---------------------------------------------------------------------------
 *  WHEN DO TWO COLLIDE? Only when a RIGHT-mover (+) is followed by a LEFT-mover (-).
 *  A new left-mover may destroy SEVERAL right-movers in a row: the most recent
 *  one first -> stack.
 *
 *  ALGORITHM
 *    for each asteroid a:
 *      alive = true
 *      while alive and a < 0 and the stack top > 0:     // a collision is happening
 *         top = stack.top
 *         if top < -a: pop (top explodes), keep fighting
 *         if top == -a: pop, alive = false (both explode)
 *         if top > -a: alive = false (a explodes)
 *      if alive: push a
 *
 *  DRY RUN [10,2,-5]
 *    10 -> [10];  2 -> [10,2]
 *    -5: top 2 < 5 -> pop [10]; top 10 > 5 -> -5 explodes   -> [10]
 *
 *  COMPLEXITY   Time O(n) (each asteroid pushed and popped at most once)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M12_AsteroidCollision {

    static int[] asteroidCollision(int[] asteroids) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (int a : asteroids) {
            boolean alive = true;
            while (alive && a < 0 && !stack.isEmpty() && stack.peek() > 0) {
                int top = stack.peek();
                if (top < -a) {
                    stack.pop();                 // the right-mover explodes; a keeps going
                } else if (top == -a) {
                    stack.pop();                 // both explode
                    alive = false;
                } else {
                    alive = false;               // a explodes
                }
            }
            if (alive) stack.push(a);
        }
        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = stack.pop();   // the stack top is the rightmost
        return result;
    }

    public static void main(String[] args) {
        check("[5,10,-5]", new int[]{5, 10}, asteroidCollision(new int[]{5, 10, -5}));
        check("[8,-8] (equal)", new int[]{}, asteroidCollision(new int[]{8, -8}));
        check("[10,2,-5] (chain)", new int[]{10}, asteroidCollision(new int[]{10, 2, -5}));
        check("[-2,-1,1,2] (moving apart)", new int[]{-2, -1, 1, 2}, asteroidCollision(new int[]{-2, -1, 1, 2}));
        check("[1,-2,-2,-2] (left-mover survives)", new int[]{-2, -2, -2}, asteroidCollision(new int[]{1, -2, -2, -2}));
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
