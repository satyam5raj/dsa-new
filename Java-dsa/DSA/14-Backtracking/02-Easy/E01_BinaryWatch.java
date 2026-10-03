/*
 * ============================================================================
 *  EASY 01 : BINARY WATCH  ⭐ bonus            LeetCode #401
 *  https://leetcode.com/problems/binary-watch/
 *  Pattern: CHOOSE k of 10 LEDs (combinations) + validity check; or just loop over all times
 * ============================================================================
 *
 *  PROBLEM
 *    A watch has 4 hour LEDs (1,2,4,8) and 6 minute LEDs (1..32). Given how many
 *    LEDs are on, list every valid time "h:mm" (hour 0-11, minute 0-59).
 *      turnedOn = 1 -> ["0:01","0:02","0:04","0:08","0:16","0:32","1:00","2:00","4:00","8:00"]
 *
 *  ---------------------------------------------------------------------------
 *  BACKTRACKING: choose which `turnedOn` of the 10 LEDs are lit (combinations of
 *  positions, start index to avoid repeats). Prune as soon as hours > 11 or
 *  minutes > 59 - adding more LEDs only makes them bigger.
 *
 *  SIMPLER (and worth noticing): there are only 12 * 60 = 720 times. Loop over all
 *  of them and keep those with bitCount(h) + bitCount(m) == turnedOn.
 *  Backtracking is the right tool when the search space is big; for 720 states,
 *  enumeration wins. Both are below.
 *
 *  COMPLEXITY   O(C(10, k)) backtracking  /  O(720) enumeration
 * ============================================================================
 */

import java.util.*;

public class E01_BinaryWatch {

    static final int[] LED = {8, 4, 2, 1, 32, 16, 8, 4, 2, 1};   // first 4 = hours, last 6 = minutes

    static List<String> readBinaryWatch(int turnedOn) {
        List<String> out = new ArrayList<>();
        backtrack(turnedOn, 0, 0, 0, out);
        Collections.sort(out);
        return out;
    }

    static void backtrack(int left, int start, int h, int m, List<String> out) {
        if (h > 11 || m > 59) return;                            // prune: can only grow
        if (left == 0) { out.add(h + ":" + (m < 10 ? "0" : "") + m); return; }
        for (int i = start; i < LED.length; i++) {
            if (i < 4) backtrack(left - 1, i + 1, h + LED[i], m, out);   // turn on an hour LED
            else backtrack(left - 1, i + 1, h, m + LED[i], out);         // turn on a minute LED
        }
    }

    static List<String> enumerate(int turnedOn) {
        List<String> out = new ArrayList<>();
        for (int h = 0; h < 12; h++)
            for (int m = 0; m < 60; m++)
                if (Integer.bitCount(h) + Integer.bitCount(m) == turnedOn) out.add(h + ":" + (m < 10 ? "0" : "") + m);
        Collections.sort(out);
        return out;
    }

    public static void main(String[] args) {
        check("turnedOn=1", "[0:01, 0:02, 0:04, 0:08, 0:16, 0:32, 1:00, 2:00, 4:00, 8:00]", readBinaryWatch(1));
        check("turnedOn=9 (impossible)", "[]", readBinaryWatch(9));
        boolean ok = true;
        for (int k = 0; k <= 10; k++) ok &= readBinaryWatch(k).equals(enumerate(k));
        check("k = 0..10: backtracking == enumeration", true, ok);
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
