/*
 * ============================================================================
 *  MEDIUM 03 : DECODE WAYS                   LeetCode #91
 *  https://leetcode.com/problems/decode-ways/
 *  Pattern: 1-D counting DP with VALIDITY conditions (Climbing Stairs with rules)
 * ============================================================================
 *
 *  PROBLEM
 *    'A'=1 ... 'Z'=26. Count the ways to decode a digit string.
 *      "12"   -> 2  ("AB", "L")
 *      "226"  -> 3  ("BZ", "VF", "BBF")
 *      "06"   -> 0  ("06" isn't "6": no leading zeros)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: recursion trying 1 or 2 digits at each position -> O(2^n)
 *
 *  KEY INSIGHT: the LAST letter used either 1 digit or 2 digits - like Climbing
 *  Stairs, except a step is only allowed if it forms a valid code:
 *      STATE       dp[i] = ways to decode the first i characters
 *      TRANSITION  dp[i] = (s[i-1] != '0'           ? dp[i-1] : 0)     one digit 1..9
 *                        + (s[i-2..i-1] in 10..26     ? dp[i-2] : 0)     two digits
 *      BASE        dp[0] = 1 (empty string: one way, decode nothing)
 *  The zeros are the whole difficulty: '0' can only be the 2nd digit of 10 or 20.
 *
 *  DRY RUN "226":  dp0=1
 *    i=1 '2'  : one-digit ok -> 1
 *    i=2 '2'  : one-digit ok (1) + "22" ok (dp0=1) -> 2
 *    i=3 '6'  : one-digit ok (2) + "26" ok (dp1=1) -> 3
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 *  Follow-up with '*' wildcards: Hard H08 Decode Ways II.
 * ============================================================================
 */

import java.util.*;

public class M03_DecodeWays {

    static int numDecodings(String s) {
        int prev2 = 1, prev1 = s.charAt(0) != '0' ? 1 : 0;      // dp[0], dp[1]
        for (int i = 2; i <= s.length(); i++) {
            int cur = 0;
            if (s.charAt(i - 1) != '0') cur += prev1;              // last letter = 1 digit
            int two = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
            if (two >= 10 && two <= 26) cur += prev2;              // last letter = 2 digits (10..26: no leading 0)
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static int brute(String s, int i) {
        if (i == s.length()) return 1;
        if (s.charAt(i) == '0') return 0;
        int ways = brute(s, i + 1);
        if (i + 1 < s.length() && Integer.parseInt(s.substring(i, i + 2)) <= 26) ways += brute(s, i + 2);
        return ways;
    }

    public static void main(String[] args) {
        check("\"12\"", 2, numDecodings("12"));
        check("\"226\"", 3, numDecodings("226"));
        check("\"06\"", 0, numDecodings("06"));
        check("\"10\"", 1, numDecodings("10"));
        check("\"100\"", 0, numDecodings("100"));
        check("\"2101\"", 1, numDecodings("2101"));
        check("\"11106\"", 2, numDecodings("11106"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(14); i++) sb.append((char) ('0' + (rnd.nextInt(3) == 0 ? rnd.nextInt(3) : rnd.nextInt(10))));
            ok &= numDecodings(sb.toString()) == brute(sb.toString(), 0);
        }
        check("1000 random (many 0/1/2) vs recursion", true, ok);
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
