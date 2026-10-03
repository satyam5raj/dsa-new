/*
 * ============================================================================
 *  MEDIUM 13 : EXCLUSIVE TIME OF FUNCTIONS   LeetCode #636
 *  https://leetcode.com/problems/exclusive-time-of-functions/
 *  Pattern: SIMULATE THE CALL STACK (a CPU profiler in miniature)
 * ============================================================================
 *
 *  PROBLEM
 *    A single-threaded CPU runs n functions. Logs look like "id:start:time" or
 *    "id:end:time". A start at time t begins AT THE START of t; an end at time t
 *    ends AT THE END of t (so "0:start:0" + "0:end:0" ran for 1 unit).
 *    EXCLUSIVE time = time spent in the function itself, NOT in functions it called.
 *      n=2, ["0:start:0","1:start:2","1:end:5","0:end:6"] -> [3,4]
 *         f0 runs 0-1 (2 units), f1 runs 2-5 (4 units), f0 runs 6 (1 unit) -> f0 = 3
 *
 *  ---------------------------------------------------------------------------
 *  The running function is always the TOP of the call stack.
 *  Keep 'prev' = the start of the current, not-yet-charged time slice.
 *
 *    "start" at t: the function on top ran from prev to t-1 -> charge it (t - prev)
 *                  push the new id; prev = t
 *    "end" at t:   the top function ran from prev to t INCLUSIVE -> charge (t - prev + 1)
 *                  pop it; prev = t + 1   (the caller resumes at the next unit)
 *
 *  DRY RUN ["0:start:0","1:start:2","1:end:5","0:end:6"]
 *    0:start:0  stack empty, push 0, prev=0
 *    1:start:2  charge f0: 2-0 = 2;  push 1, prev=2
 *    1:end:5    charge f1: 5-2+1 = 4; pop, prev=6
 *    0:end:6    charge f0: 6-6+1 = 1; pop, prev=7     -> f0 = 3, f1 = 4
 *
 *  Recursion works too (a function calling itself): the stack just holds the same id twice.
 *  COMPLEXITY   Time O(number of logs)   Space O(n + depth)
 * ============================================================================
 */

import java.util.*;

public class M13_ExclusiveTimeOfFunctions {

    static int[] exclusiveTime(int n, List<String> logs) {
        int[] result = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();
        int prev = 0;
        for (String log : logs) {
            String[] p = log.split(":");
            int id = Integer.parseInt(p[0]), t = Integer.parseInt(p[2]);
            if (p[1].equals("start")) {
                if (!stack.isEmpty()) result[stack.peek()] += t - prev;   // pause the caller
                stack.push(id);
                prev = t;
            } else {
                result[stack.pop()] += t - prev + 1;                      // end is inclusive
                prev = t + 1;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        check("two functions", new int[]{3, 4}, exclusiveTime(2, List.of("0:start:0", "1:start:2", "1:end:5", "0:end:6")));
        check("recursion (same id)", new int[]{8}, exclusiveTime(1, List.of("0:start:0", "0:start:2", "0:end:5", "0:start:6", "0:end:6", "0:end:7")));
        check("sequential calls", new int[]{7, 1}, exclusiveTime(2, List.of("0:start:0", "0:start:2", "0:end:5", "1:start:6", "1:end:6", "0:end:7")));
        check("single unit", new int[]{1}, exclusiveTime(1, List.of("0:start:0", "0:end:0")));
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
