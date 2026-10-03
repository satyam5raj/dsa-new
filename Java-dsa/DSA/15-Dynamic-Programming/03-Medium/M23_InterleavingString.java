/*
 * ============================================================================
 *  MEDIUM 23 : INTERLEAVING STRING           LeetCode #97
 *  https://leetcode.com/problems/interleaving-string/
 *  Pattern: two-sequence BOOLEAN DP (the third string's index is implied: i + j)
 * ============================================================================
 *
 *  PROBLEM
 *    Is s3 formed by interleaving s1 and s2 (keeping each one's internal order)?
 *      "aabcc", "dbbca", "aadbbcbcac" -> true
 *      "aabcc", "dbbca", "aadbbbaccc" -> false
 *      "", "", ""                     -> true
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY/TWO POINTERS FAIL: when s1[i] == s2[j] == s3[k] you don't know which to take.
 *  Branching on both = exponential, but the state is only (i, j).
 *
 *  KEY INSIGHT: after using i chars of s1 and j chars of s2, we've built exactly
 *  s3[0..i+j). So (i, j) is the full state; k = i + j is not a third dimension.
 *      STATE       dp[i][j] = s3[0..i+j) is an interleaving of s1[0..i) and s2[0..j)
 *      TRANSITION  dp[i][j] = (dp[i-1][j] && s1[i-1] == s3[i+j-1])     last char from s1
 *                          || (dp[i][j-1] && s2[j-1] == s3[i+j-1])     last char from s2
 *      BASE        dp[0][0] = true;  quick reject if |s1| + |s2| != |s3|
 *  One row of booleans suffices.
 *
 *  COMPLEXITY   Time O(n*m)   Space O(m)
 * ============================================================================
 */

import java.util.*;

public class M23_InterleavingString {

    static boolean isInterleave(String s1, String s2, String s3) {
        int n = s1.length(), m = s2.length();
        if (n + m != s3.length()) return false;
        boolean[] dp = new boolean[m + 1];
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= m; j++) {
                if (i == 0 && j == 0) { dp[0] = true; continue; }
                char c = s3.charAt(i + j - 1);
                boolean fromS1 = i > 0 && dp[j] && s1.charAt(i - 1) == c;          // dp[j] still = dp[i-1][j]
                boolean fromS2 = j > 0 && dp[j - 1] && s2.charAt(j - 1) == c;      // dp[j-1] already = dp[i][j-1]
                dp[j] = fromS1 || fromS2;
            }
        return dp[m];
    }

    static boolean brute(String a, String b, String c, int i, int j) {
        if (i + j == c.length()) return i == a.length() && j == b.length();
        char x = c.charAt(i + j);
        return (i < a.length() && a.charAt(i) == x && brute(a, b, c, i + 1, j))
            || (j < b.length() && b.charAt(j) == x && brute(a, b, c, i, j + 1));
    }

    public static void main(String[] args) {
        check("aabcc/dbbca/aadbbcbcac", true, isInterleave("aabcc", "dbbca", "aadbbcbcac"));
        check("aabcc/dbbca/aadbbbaccc", false, isInterleave("aabcc", "dbbca", "aadbbbaccc"));
        check("empty", true, isInterleave("", "", ""));
        check("length mismatch", false, isInterleave("a", "", "aa"));
        Random rnd = new Random(1);
        boolean ok = true;
        int trues = 0;
        for (int t = 0; t < 2000; t++) {
            String a = rs(rnd, 6), b = rs(rnd, 6);
            String c = rnd.nextBoolean() ? shuffleMerge(a, b, rnd) : rs(rnd, a.length() + b.length() + 1);
            boolean exp = c.length() == a.length() + b.length() && brute(a, b, c, 0, 0);
            if (exp) trues++;
            ok &= isInterleave(a, b, c) == exp;
        }
        check("2000 random vs recursion", true, ok);
        System.out.println("   (" + trues + " of them were real interleavings)");
        summary();
    }

    static String shuffleMerge(String a, String b, Random r) {
        StringBuilder sb = new StringBuilder();
        int i = 0, j = 0;
        while (i < a.length() || j < b.length())
            if (j == b.length() || (i < a.length() && r.nextBoolean())) sb.append(a.charAt(i++)); else sb.append(b.charAt(j++));
        return sb.toString();
    }

    static String rs(Random r, int maxLen) { StringBuilder sb = new StringBuilder(); for (int i = r.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + r.nextInt(2))); return sb.toString(); }

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
