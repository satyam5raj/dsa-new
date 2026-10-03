/*
 * ============================================================================
 *  MEDIUM 22 : EDIT DISTANCE                 LeetCode #72
 *  https://leetcode.com/problems/edit-distance/
 *  Pattern: two-sequence DP with three operations (Levenshtein distance)
 * ============================================================================
 *
 *  PROBLEM
 *    Minimum number of insert / delete / replace operations turning word1 into word2.
 *      "horse" -> "ros"            = 3  (horse -> rorse -> rose -> ros)
 *      "intention" -> "execution"  = 5
 *
 *  ---------------------------------------------------------------------------
 *  KEY INSIGHT: decide what happens to the LAST characters.
 *      STATE       dp[i][j] = edits to turn a[0..i) into b[0..j)
 *      a[i-1] == b[j-1]: nothing to do      dp[i-1][j-1]
 *      otherwise 1 + min of:
 *          replace a[i-1] by b[j-1]           dp[i-1][j-1]
 *          delete a[i-1]                      dp[i-1][j]
 *          insert b[j-1] at the end           dp[i][j-1]
 *      BASE        dp[i][0] = i (delete everything),  dp[0][j] = j (insert everything)
 *  The three neighbours (diagonal, up, left) = the three operations.
 *
 *  DRY RUN "horse" -> "ros"
 *          ""  r  o  s
 *     ""    0  1  2  3
 *     h     1  1  2  3
 *     o     2  2  1  2
 *     r     3  2  2  2
 *     s     4  3  3  2
 *     e     5  4  4  3   -> 3
 *
 *  COMPLEXITY   Time O(n*m)   Space O(m) with one row + diagonal variable
 *  Used in: spell checkers, DNA alignment, `diff` (which is LCS-based).
 * ============================================================================
 */

import java.util.*;

public class M22_EditDistance {

    static int minDistance(String a, String b) {
        int m = b.length();
        int[] dp = new int[m + 1];
        for (int j = 0; j <= m; j++) dp[j] = j;          // row 0: insert j characters
        for (int i = 1; i <= a.length(); i++) {
            int diag = dp[0];                            // dp[i-1][0]
            dp[0] = i;                                   // column 0: delete i characters
            for (int j = 1; j <= m; j++) {
                int up = dp[j];                          // dp[i-1][j]
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[j] = diag;
                else dp[j] = 1 + Math.min(diag, Math.min(up, dp[j - 1]));   // replace / delete / insert
                diag = up;
            }
        }
        return dp[m];
    }

    // referee: memoized recursion straight from the definition, on suffixes
    static int memo(String a, String b, int i, int j, Integer[][] cache) {
        if (i == a.length()) return b.length() - j;
        if (j == b.length()) return a.length() - i;
        if (cache[i][j] != null) return cache[i][j];
        int r;
        if (a.charAt(i) == b.charAt(j)) r = memo(a, b, i + 1, j + 1, cache);
        else r = 1 + Math.min(memo(a, b, i + 1, j + 1, cache), Math.min(memo(a, b, i + 1, j, cache), memo(a, b, i, j + 1, cache)));
        return cache[i][j] = r;
    }

    public static void main(String[] args) {
        check("horse -> ros", 3, minDistance("horse", "ros"));
        check("intention -> execution", 5, minDistance("intention", "execution"));
        check("\"\" -> abc", 3, minDistance("", "abc"));
        check("abc -> \"\"", 3, minDistance("abc", ""));
        check("kitten -> sitting", 3, minDistance("kitten", "sitting"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            String a = rs(rnd, 9), b = rs(rnd, 9);
            ok &= minDistance(a, b) == memo(a, b, 0, 0, new Integer[a.length() + 1][b.length() + 1]);
        }
        check("1000 random: table == top-down", true, ok);
        // metric sanity: d(a,c) <= d(a,b) + d(b,c), d(a,b) == d(b,a)
        ok = true;
        for (int t = 0; t < 300; t++) {
            String a = rs(rnd, 7), b = rs(rnd, 7), c = rs(rnd, 7);
            ok &= minDistance(a, c) <= minDistance(a, b) + minDistance(b, c) && minDistance(a, b) == minDistance(b, a);
        }
        check("symmetric + triangle inequality", true, ok);
        summary();
    }

    static String rs(Random r, int maxLen) { StringBuilder sb = new StringBuilder(); for (int i = r.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + r.nextInt(3))); return sb.toString(); }

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
