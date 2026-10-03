/*
 * ============================================================================
 *  MEDIUM 08 : BULLS AND COWS                LeetCode #299
 *  https://leetcode.com/problems/bulls-and-cows/
 *  Pattern: ONE PASS WITH A SIGNED COUNT ARRAY
 * ============================================================================
 *
 *  PROBLEM
 *    secret and guess are digit strings of equal length.
 *      bulls = digits in the RIGHT position
 *      cows  = digits that exist in secret but are in the WRONG position
 *              (each secret digit can be matched at most once)
 *    Return "xAyB" (x bulls, y cows).
 *      secret "1807", guess "7810" -> "1A3B"   (8 is a bull; 1, 0, 7 are cows)
 *      secret "1123", guess "0111" -> "1A1B"
 *
 *  ---------------------------------------------------------------------------
 *  TWO PASSES (clear): count bulls, then count unmatched digits in each string;
 *  cows = sum over digits of min(secretCount[d], guessCount[d]).
 *
 *  ONE PASS (elegant): a single count[10] array where
 *    a secret digit ADDS +1 and a guess digit ADDS -1 (only for non-bull positions).
 *    When we see secret digit s:
 *      if count[s] < 0 -> an earlier GUESS digit was waiting for it -> cows++
 *    When we see guess digit g:
 *      if count[g] > 0 -> an earlier SECRET digit was waiting for it -> cows++
 *    Then update the counts.
 *
 *  DRY RUN secret "1123", guess "0111"
 *    i=0 s1 g0: count[1]<0? no; count[0]>0? no   -> count[1]=1, count[0]=-1
 *    i=1 s1 g1: BULL
 *    i=2 s2 g1: count[2]<0? no; count[1]=1 > 0 -> COW -> count[2]=1, count[1]=0
 *    i=3 s3 g1: count[3]<0? no; count[1]=0 -> no  -> count[3]=1, count[1]=-1
 *    -> 1 bull, 1 cow -> "1A1B"
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M08_BullsAndCows {

    static String getHint(String secret, String guess) {
        int[] count = new int[10];
        int bulls = 0, cows = 0;
        for (int i = 0; i < secret.length(); i++) {
            int s = secret.charAt(i) - '0', g = guess.charAt(i) - '0';
            if (s == g) {
                bulls++;
            } else {
                if (count[s] < 0) cows++;     // a guess digit was waiting for this secret digit
                if (count[g] > 0) cows++;     // a secret digit was waiting for this guess digit
                count[s]++;
                count[g]--;
            }
        }
        return bulls + "A" + cows + "B";
    }

    public static void main(String[] args) {
        check("1807 / 7810", "1A3B", getHint("1807", "7810"));
        check("1123 / 0111", "1A1B", getHint("1123", "0111"));
        check("1234 / 1234 (all bulls)", "4A0B", getHint("1234", "1234"));
        check("1122 / 2211 (all cows)", "0A4B", getHint("1122", "2211"));
        check("1 / 0", "0A0B", getHint("1", "0"));
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
