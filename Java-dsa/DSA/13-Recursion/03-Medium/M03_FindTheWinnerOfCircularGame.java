/*
 * ============================================================================
 *  MEDIUM 03 : FIND THE WINNER OF THE CIRCULAR GAME (JOSEPHUS)  ⭐ bonus   LeetCode #1823
 *  https://leetcode.com/problems/find-the-winner-of-the-circular-game/
 *  Pattern: recurse on n-1 and SHIFT the answer back (renumbering)
 * ============================================================================
 *
 *  PROBLEM
 *    n friends sit in a circle (1..n). Starting at friend 1, count k friends
 *    clockwise (including the start); that friend leaves. Counting restarts at the
 *    next friend. The last one standing wins.
 *      n=5, k=2 -> 3     (2 leaves, then 4, then 1, then 5)
 *
 *  ---------------------------------------------------------------------------
 *  SIMULATION: a queue - rotate k-1 people to the back, remove the k-th.
 *  O(n * k). Fine for small inputs, and our referee.
 *
 *  JOSEPHUS RECURRENCE (0-indexed positions):
 *    J(1) = 0                                  (one person: they win)
 *    J(n) = (J(n-1) + k) % n
 *  WHY: after the first person (position k-1) leaves, the game continues with n-1
 *  people, starting from position k. Number those survivors 0..n-2 starting at the
 *  old position k. If the winner of THAT smaller game is at new position w, its old
 *  position is (w + k) % n. Recursion solves the smaller game; we just shift.
 *  Answer = J(n) + 1 (1-indexed).
 *
 *  DRY RUN n=5, k=2: J(1)=0, J(2)=(0+2)%2=0, J(3)=(0+2)%3=2, J(4)=(2+2)%4=0,
 *          J(5)=(0+2)%5=2 -> friend 3
 *
 *  COMPLEXITY   Time O(n)   Space O(n) recursion (O(1) as a loop)
 * ============================================================================
 */

import java.util.*;

public class M03_FindTheWinnerOfCircularGame {

    static int findTheWinner(int n, int k) { return josephus(n, k) + 1; }

    static int josephus(int n, int k) {
        if (n == 1) return 0;
        return (josephus(n - 1, k) + k) % n;           // shift the smaller game's winner back
    }

    static int simulate(int n, int k) {
        Deque<Integer> circle = new ArrayDeque<>();
        for (int i = 1; i <= n; i++) circle.add(i);
        while (circle.size() > 1) {
            for (int i = 0; i < k - 1; i++) circle.add(circle.poll());   // skip k-1 friends
            circle.poll();                                                 // the k-th leaves
        }
        return circle.peek();
    }

    public static void main(String[] args) {
        check("n=5 k=2", 3, findTheWinner(5, 2));
        check("n=6 k=5", 1, findTheWinner(6, 5));
        check("n=1", 1, findTheWinner(1, 7));
        boolean ok = true;
        for (int n = 1; n <= 60; n++) for (int k = 1; k <= 60; k++) ok &= findTheWinner(n, k) == simulate(n, k);
        check("all n, k in 1..60 vs simulation", true, ok);
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
