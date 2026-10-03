/*
 * ============================================================================
 *  MEDIUM 10 : PALINDROME PARTITIONING       LeetCode #131
 *  https://leetcode.com/problems/palindrome-partitioning/
 *  Pattern: PARTITION backtracking - choose where the next piece ENDS (+ a palindrome table)
 * ============================================================================
 *
 *  PROBLEM
 *    Split s into pieces so that EVERY piece is a palindrome. Return all such splits.
 *      "aab" -> [["a","a","b"],["aa","b"]]
 *
 *  ---------------------------------------------------------------------------
 *  PARTITION TEMPLATE: at position `start`, the next piece is s[start..end] for some
 *  end >= start. Try every end; if the piece is valid, add it and recurse on end + 1.
 *     bt(start): if start == n: record
 *                for end in start..n-1:
 *                   if isPal(start, end): add piece; bt(end + 1); remove
 *  The same shape solves Restore IP Addresses (M11), Word Break II, splitting into
 *  increasing numbers...
 *
 *  SPEED-UP: checking each piece from scratch costs O(n). Precompute a table
 *     pal[i][j] = s[i] == s[j] && (j - i < 2 || pal[i+1][j-1])
 *  in O(n^2) (a small DP, filled from the bottom row up), then each check is O(1).
 *
 *  DRY RUN "aab": "a" -> "a" -> "b" ✓ ; "a" -> "ab" ✗ ; "aa" -> "b" ✓ ; "aab" ✗
 *
 *  COMPLEXITY   Time O(n * 2^n) worst case ("aaaa...": every split works)   Space O(n^2)
 * ============================================================================
 */

import java.util.*;

public class M10_PalindromePartitioning {

    static List<List<String>> partition(String s) {
        int n = s.length();
        boolean[][] pal = new boolean[n][n];
        for (int i = n - 1; i >= 0; i--)                              // i from the end: pal[i+1] is ready
            for (int j = i; j < n; j++)
                pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
        List<List<String>> out = new ArrayList<>();
        bt(s, 0, pal, new ArrayList<>(), out);
        return out;
    }

    static void bt(String s, int start, boolean[][] pal, List<String> path, List<List<String>> out) {
        if (start == s.length()) { out.add(new ArrayList<>(path)); return; }
        for (int end = start; end < s.length(); end++) {
            if (!pal[start][end]) continue;                          // this piece isn't a palindrome
            path.add(s.substring(start, end + 1));
            bt(s, end + 1, pal, path, out);
            path.remove(path.size() - 1);
        }
    }

    // referee: every cut pattern (bit i = cut after character i), keep all-palindrome splits
    static Set<List<String>> brute(String s) {
        Set<List<String>> out = new HashSet<>();
        int n = s.length();
        for (int mask = 0; mask < (1 << Math.max(0, n - 1)); mask++) {
            List<String> parts = new ArrayList<>();
            int start = 0;
            for (int i = 0; i < n; i++) if (i == n - 1 || (mask >> i & 1) == 1) { parts.add(s.substring(start, i + 1)); start = i + 1; }
            boolean ok = true;
            for (String p : parts) ok &= new StringBuilder(p).reverse().toString().equals(p);
            if (ok) out.add(parts);
        }
        return out;
    }

    public static void main(String[] args) {
        check("aab", "[[a, a, b], [aa, b]]", partition("aab"));
        check("a", "[[a]]", partition("a"));
        check("aaaa has 2^3 splits", 8, partition("aaaa").size());
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(10); i++) sb.append((char) ('a' + rnd.nextInt(2)));
            List<List<String>> got = partition(sb.toString());
            ok &= new HashSet<>(got).equals(brute(sb.toString())) && new HashSet<>(got).size() == got.size();
        }
        check("500 random strings vs all cut patterns", true, ok);
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
