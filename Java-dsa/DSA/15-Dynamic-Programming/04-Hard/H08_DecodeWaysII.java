/*
 * ============================================================================
 *  HARD 08 : DECODE WAYS II                  LeetCode #639
 *  https://leetcode.com/problems/decode-ways-ii/
 *  Pattern: M03 Decode Ways, but each step MULTIPLIES by "how many codes fit here"
 * ============================================================================
 *
 *  PROBLEM
 *    Like Decode Ways, plus '*' = any digit 1..9. Count decodings modulo 1e9+7.
 *      "*"  -> 9        "1*" -> 18        "2*" -> 15        "**" -> 96
 *
 *  ---------------------------------------------------------------------------
 *  Same recurrence as M03:
 *      dp[i] = single(s[i-1]) * dp[i-1]  +  pair(s[i-2], s[i-1]) * dp[i-2]
 *  where single / pair now COUNT how many valid letters the last 1 / 2 characters can
 *  be (instead of 0 or 1):
 *      single('*') = 9         single('0') = 0       single(digit) = 1
 *      pair('*','*') = 15      11-19 (9) + 21-26 (6)
 *      pair('*', d)  = d <= 6 ? 2 : 1                 1d and 2d, or only 1d
 *      pair('1','*') = 9       11-19
 *      pair('2','*') = 6       21-26
 *      pair(other, '*') = 0
 *      pair(d1, d2)  = 10 <= d1d2 <= 26 ? 1 : 0
 *  Everything mod 1e9+7, in long (single * dp can reach 9 * 1e9).
 *
 *  DRY RUN "1*":  dp0=1, dp1=1 ('1')
 *    i=2: single('*')=9 * dp1  +  pair('1','*')=9 * dp0  = 18
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class H08_DecodeWaysII {

    static final int MOD = 1_000_000_007;

    static int numDecodings(String s) {
        long prev2 = 1, prev1 = single(s.charAt(0));       // dp[0], dp[1]
        for (int i = 2; i <= s.length(); i++) {
            long cur = (single(s.charAt(i - 1)) * prev1 + pair(s.charAt(i - 2), s.charAt(i - 1)) * prev2) % MOD;
            prev2 = prev1;
            prev1 = cur;
        }
        return (int) prev1;
    }

    static int single(char c) { return c == '*' ? 9 : c == '0' ? 0 : 1; }

    static int pair(char a, char b) {
        if (a == '*' && b == '*') return 15;
        if (a == '*') return b <= '6' ? 2 : 1;
        if (b == '*') return a == '1' ? 9 : a == '2' ? 6 : 0;
        int v = (a - '0') * 10 + (b - '0');
        return v >= 10 && v <= 26 ? 1 : 0;
    }

    // referee: expand every '*' into 1..9 and sum the plain Decode Ways counts
    static long brute(char[] s, int i) {
        if (i == s.length) return plain(new String(s));
        if (s[i] != '*') return brute(s, i + 1);
        long total = 0;
        for (char d = '1'; d <= '9'; d++) { s[i] = d; total += brute(s, i + 1); }
        s[i] = '*';
        return total;
    }

    static long plain(String s) {
        long p2 = 1, p1 = s.charAt(0) != '0' ? 1 : 0;
        for (int i = 2; i <= s.length(); i++) {
            long c = s.charAt(i - 1) != '0' ? p1 : 0;
            int two = (s.charAt(i - 2) - '0') * 10 + s.charAt(i - 1) - '0';
            if (two >= 10 && two <= 26) c += p2;
            p2 = p1; p1 = c;
        }
        return p1;
    }

    public static void main(String[] args) {
        check("*", 9, numDecodings("*"));
        check("1*", 18, numDecodings("1*"));
        check("2*", 15, numDecodings("2*"));
        check("**", 96, numDecodings("**"));
        check("*0 (only 10, 20)", 2, numDecodings("*0"));
        check("long run of stars (mod)", true, numDecodings("*".repeat(100_000)) >= 0);
        Random rnd = new Random(1);
        boolean ok = true;
        String alphabet = "*0123456789";
        for (int t = 0; t < 1000; t++) {
            char[] s = new char[1 + rnd.nextInt(8)];
            int stars = 0;
            for (int i = 0; i < s.length; i++) {
                s[i] = stars < 3 && rnd.nextInt(3) == 0 ? '*' : alphabet.charAt(1 + rnd.nextInt(rnd.nextBoolean() ? 3 : 10));
                if (s[i] == '*') stars++;
            }
            ok &= numDecodings(new String(s)) == brute(s.clone(), 0) % MOD;
        }
        check("1000 random vs expanding every '*'", true, ok);
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
