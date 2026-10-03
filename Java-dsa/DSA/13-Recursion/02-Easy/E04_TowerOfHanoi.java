/*
 * ============================================================================
 *  EASY 04 : TOWER OF HANOI                  (leetcode.md "Recursion Basics" #70)
 *  Pattern: SOLVE BY ASSUMING THE SMALLER CASE WORKS (the purest recursion exercise)
 * ============================================================================
 *
 *  PROBLEM
 *    n disks of different sizes on rod A (largest at the bottom). Move them all to
 *    rod C using rod B, one disk at a time, never putting a bigger disk on a smaller
 *    one. Print the moves.
 *
 *  ---------------------------------------------------------------------------
 *  Trying to plan the moves directly is hopeless. Think recursively instead:
 *  The BIGGEST disk must move from A to C at some point. For that, all n-1 smaller
 *  disks must be out of the way - stacked on B. So:
 *     1. move the top n-1 disks  A -> B   (using C as the spare)   <- smaller problem
 *     2. move disk n             A -> C
 *     3. move the n-1 disks      B -> C   (using A as the spare)   <- smaller problem
 *  Base case: 0 disks -> nothing to do.
 *  We never need to "see" how step 1 works - we trust it (the leap of faith).
 *
 *  NUMBER OF MOVES: M(n) = 2 M(n-1) + 1, M(0) = 0  ->  M(n) = 2^n - 1 (and that's
 *  the minimum possible). 64 disks at one move per second: ~585 billion years.
 *
 *  DRY RUN n = 2: (1: A->B) (2: A->C) (1: B->C)
 *
 *  COMPLEXITY   Time O(2^n)   Space O(n) recursion depth
 * ============================================================================
 */

import java.util.*;

public class E04_TowerOfHanoi {

    static void solve(int n, char from, char to, char spare, List<String> moves) {
        if (n == 0) return;
        solve(n - 1, from, spare, to, moves);           // clear the way
        moves.add(n + ":" + from + to);                  // move the biggest
        solve(n - 1, spare, to, from, moves);           // put the rest back on top
    }

    // simulate the moves on three real stacks and check every rule
    static boolean valid(int n, List<String> moves) {
        Map<Character, Deque<Integer>> rods = new HashMap<>();
        for (char r : new char[]{'A', 'B', 'C'}) rods.put(r, new ArrayDeque<>());
        for (int d = n; d >= 1; d--) rods.get('A').push(d);
        for (String m : moves) {
            int disk = Integer.parseInt(m.substring(0, m.indexOf(':')));
            char from = m.charAt(m.length() - 2), to = m.charAt(m.length() - 1);
            if (rods.get(from).isEmpty() || rods.get(from).peek() != disk) return false;   // must be the top disk
            if (!rods.get(to).isEmpty() && rods.get(to).peek() < disk) return false;       // no big on small
            rods.get(to).push(rods.get(from).pop());
        }
        return rods.get('C').size() == n;
    }

    public static void main(String[] args) {
        List<String> moves = new ArrayList<>();
        solve(2, 'A', 'C', 'B', moves);
        check("2 disks", "[1:AB, 2:AC, 1:BC]", moves);
        boolean ok = true;
        for (int n = 0; n <= 16; n++) {
            List<String> m = new ArrayList<>();
            solve(n, 'A', 'C', 'B', m);
            ok &= m.size() == (1 << n) - 1 && valid(n, m);
        }
        check("n = 0..16: 2^n - 1 legal moves ending on C", true, ok);
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
