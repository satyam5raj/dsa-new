/*
 * ============================================================================
 *  EASY 01 : HAPPY NUMBER                    LeetCode #202
 *  https://leetcode.com/problems/happy-number/
 *  Pattern: HASHSET TO DETECT A CYCLE  (or Floyd's fast/slow, O(1) space)
 * ============================================================================
 *
 *  PROBLEM
 *    Repeatedly replace n with the sum of the squares of its digits. If you reach 1,
 *    n is HAPPY. Otherwise the sequence loops forever.
 *      19 -> 1+81=82 -> 64+4=68 -> 36+64=100 -> 1      -> happy
 *      2  -> 4 -> 16 -> 37 -> 58 -> 89 -> 145 -> 42 -> 20 -> 4 (a loop!) -> not happy
 *
 *  ---------------------------------------------------------------------------
 *  WHY IT MUST END IN 1 OR LOOP: the next number is small (a 3-digit number gives
 *  at most 3*81 = 243). So the sequence stays inside a finite range and must
 *  eventually REPEAT a value, which means a cycle (or the fixed point 1).
 *
 *  APPROACH 1: HASHSET of seen numbers. Seeing a number twice -> a cycle -> not happy.
 *
 *  APPROACH 2: FLOYD (exactly Linked List Cycle, E03 there): treat n -> next(n)
 *  as a linked list. slow moves 1 step, fast 2 steps; they meet inside the cycle.
 *  Happy iff they meet at 1. O(1) memory.
 *
 *  This is a lesson in recognizing structure: "a sequence where each value
 *  determines the next" IS a linked list, so cycle detection applies.
 *  COMPLEXITY   O(log n) per step; the number of steps is small (bounded)
 * ============================================================================
 */

import java.util.*;

public class E01_HappyNumber {

    static int next(int n) {
        int sum = 0;
        while (n > 0) {
            int d = n % 10;
            sum += d * d;
            n /= 10;
        }
        return sum;
    }

    static boolean isHappy(int n) {
        Set<Integer> seen = new HashSet<>();
        while (n != 1 && seen.add(n)) n = next(n);   // add() is false when n was seen before -> cycle
        return n == 1;
    }

    static boolean isHappyFloyd(int n) {
        int slow = n, fast = next(n);
        while (fast != 1 && slow != fast) {
            slow = next(slow);
            fast = next(next(fast));
        }
        return fast == 1;
    }

    public static void main(String[] args) {
        check("19 (happy)", true, isHappy(19));
        check("2 (cycle)", false, isHappy(2));
        check("1", true, isHappy(1));
        check("7 (happy)", true, isHappy(7));
        boolean ok = true;
        for (int n = 1; n <= 10_000; n++) ok &= isHappy(n) == isHappyFloyd(n);
        check("Floyd == HashSet for 1..10,000", true, ok);
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
