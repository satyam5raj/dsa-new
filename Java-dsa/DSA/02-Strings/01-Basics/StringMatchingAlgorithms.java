/*
 * ============================================================================
 *  STRINGS 1.3 : STRING MATCHING - NAIVE, KMP, RABIN-KARP, Z-ALGORITHM
 * ============================================================================
 *
 *  Run:  java StringMatchingAlgorithms.java
 *
 *  PROBLEM: find every position where a PATTERN (length m) occurs in a TEXT (length n).
 *     text = "abababcab", pattern = "abc"  ->  found at index 4
 *
 *  ---------------------------------------------------------------------------
 *   Algorithm     Time              Space   Idea
 *  ---------------------------------------------------------------------------
 *   Naive         O(n * m) worst    O(1)    try every start, compare char by char
 *   KMP           O(n + m)          O(m)    never re-compare text characters: on a
 *                                           mismatch, jump using the pattern's own structure
 *   Rabin-Karp    O(n + m) average  O(1)    compare HASHES of windows (rolling hash),
 *                                           verify only when the hashes match
 *   Z-algorithm   O(n + m)          O(n+m)  Z[i] = length of the longest prefix match at i
 *  ---------------------------------------------------------------------------
 *  Java's String.indexOf is the naive algorithm (fast in practice for short patterns).
 *  Interviews: know the naive method well, understand KMP's LPS array, and know
 *  rolling hashes (they also solve "longest duplicate substring", "repeated DNA sequences").
 */

import java.util.*;

public class StringMatchingAlgorithms {

    public static void main(String[] args) {
        String text = "abababcabababcab", pattern = "ababc";
        System.out.println("text    = " + text);
        System.out.println("pattern = " + pattern + "\n");

        System.out.println("===== 1. NAIVE =====");
        System.out.println("   matches at " + naive(text, pattern));

        System.out.println("\n===== 2. KMP =====");
        System.out.println("   LPS(\"" + pattern + "\") = " + Arrays.toString(buildLps(pattern)));
        System.out.println("   LPS(\"aabaaab\") = " + Arrays.toString(buildLps("aabaaab")));
        System.out.println("   matches at " + kmp(text, pattern));

        System.out.println("\n===== 3. RABIN-KARP =====");
        System.out.println("   matches at " + rabinKarp(text, pattern));

        System.out.println("\n===== 4. Z-ALGORITHM =====");
        System.out.println("   Z(\"aabxaab\") = " + Arrays.toString(zArray("aabxaab")));
        System.out.println("   matches at " + zSearch(text, pattern));

        System.out.println("\n===== 5. VERIFY ALL FOUR AGREE ON 2,000 RANDOM CASES =====");
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            String tx = randomString(rnd, rnd.nextInt(30), 3);   // small alphabet -> many overlapping matches
            String p = randomString(rnd, 1 + rnd.nextInt(4), 3);
            List<Integer> expected = naive(tx, p);
            if (!expected.equals(kmp(tx, p)) || !expected.equals(rabinKarp(tx, p)) || !expected.equals(zSearch(tx, p))) ok = false;
        }
        System.out.println("   all algorithms agree: " + ok);
        if (!ok) System.exit(1);
    }

    // =================================================================
    // 1. NAIVE: for each start i, compare pattern[0..m-1] with text[i..i+m-1]
    //    Worst case "aaaa...ab" in "aaaaaaaa...a": almost every comparison goes
    //    deep before failing -> O(n * m)
    // =================================================================
    static List<Integer> naive(String text, String pattern) {
        List<Integer> res = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == m) res.add(i);
        }
        return res;
    }

    // =================================================================
    // 2. KMP (Knuth-Morris-Pratt)
    //
    //  WASTE IN NAIVE: after matching "abab" and failing on the next char, naive
    //  restarts at i+1 and re-reads characters it already knows.
    //  KMP's insight: what we matched is a PREFIX OF THE PATTERN, so we already
    //  know those characters. Precompute, for each prefix of the pattern, the
    //  length of its longest PROPER PREFIX that is also a SUFFIX (the "LPS").
    //  On a mismatch, slide the pattern so that this prefix lines up with the
    //  suffix we just read. The text pointer NEVER moves backwards.
    //
    //  LPS EXAMPLE  pattern = "ababc"
    //    "a"     -> 0
    //    "ab"    -> 0
    //    "aba"   -> 1   ("a" is both prefix and suffix)
    //    "abab"  -> 2   ("ab")
    //    "ababc" -> 0
    //    LPS = [0,0,1,2,0]
    //  So after matching "abab" and failing, continue as if "ab" (2 chars) is
    //  already matched, instead of starting over.
    // =================================================================
    static int[] buildLps(String p) {
        int[] lps = new int[p.length()];
        int len = 0;                                   // length of the current prefix-suffix
        for (int i = 1; i < p.length(); ) {
            if (p.charAt(i) == p.charAt(len)) {
                lps[i++] = ++len;                      // extend the match
            } else if (len > 0) {
                len = lps[len - 1];                    // fall back to a shorter prefix-suffix (don't move i)
            } else {
                lps[i++] = 0;                          // no prefix-suffix at all
            }
        }
        return lps;
    }

    static List<Integer> kmp(String text, String pattern) {
        List<Integer> res = new ArrayList<>();
        int m = pattern.length();
        if (m == 0) return res;
        int[] lps = buildLps(pattern);
        int j = 0;                                     // number of pattern chars currently matched
        for (int i = 0; i < text.length(); i++) {      // i only moves forward
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = lps[j - 1];                        // reuse what we know: slide the pattern
            }
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                res.add(i - m + 1);                    // full match ends at i
                j = lps[j - 1];                        // keep going (allows overlapping matches)
            }
        }
        return res;
    }

    // =================================================================
    // 3. RABIN-KARP: ROLLING HASH
    //    Treat a string as a number in base B (e.g. 31) modulo a large prime M.
    //      hash("abc") = a*B^2 + b*B + c   (mod M)
    //    Slide the window by one in O(1):
    //      remove the leftmost char:  hash -= left * B^(m-1)
    //      shift:                     hash *= B
    //      add the new char:          hash += right
    //    If the window hash == pattern hash, VERIFY by comparing characters
    //    (different strings can share a hash: a "collision").
    //    Average O(n + m). Worst case O(n*m) with many collisions (rare with a good M).
    // =================================================================
    static List<Integer> rabinKarp(String text, String pattern) {
        List<Integer> res = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0 || m > n) return res;
        final long B = 31, M = 1_000_000_007L;
        long pHash = 0, wHash = 0, power = 1;           // power = B^(m-1) mod M
        for (int i = 0; i < m; i++) {
            pHash = (pHash * B + pattern.charAt(i)) % M;
            wHash = (wHash * B + text.charAt(i)) % M;
            if (i > 0) power = (power * B) % M;
        }
        for (int i = 0; ; i++) {
            if (pHash == wHash && text.regionMatches(i, pattern, 0, m)) res.add(i);   // verify on hash match
            if (i + m >= n) break;
            wHash = (wHash - text.charAt(i) * power % M + M) % M;    // remove the left char (+M keeps it non-negative)
            wHash = (wHash * B + text.charAt(i + m)) % M;            // shift and add the right char
        }
        return res;
    }

    // =================================================================
    // 4. Z-ALGORITHM
    //    Z[i] = length of the longest substring starting at i that matches a
    //    PREFIX of the string. Z[0] is undefined (set to 0).
    //      "aabxaab" -> [0,1,0,0,3,1,0]   (at i=4, "aab" matches the prefix "aab")
    //    To search: compute Z of  pattern + '$' + text. Positions with Z == m are matches.
    //    The [l, r) "Z-box" (the rightmost prefix match seen so far) lets each Z[i] reuse
    //    earlier results -> O(n).
    // =================================================================
    static int[] zArray(String s) {
        int n = s.length();
        int[] z = new int[n];
        int l = 0, r = 0;                              // [l, r) = the rightmost window that matches a prefix
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);              // reuse: s[i..] mirrors s[i-l..]
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;   // extend explicitly
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }

    static List<Integer> zSearch(String text, String pattern) {
        List<Integer> res = new ArrayList<>();
        int m = pattern.length();
        if (m == 0) return res;
        int[] z = zArray(pattern + "\u0000" + text);   // a separator that appears in neither string
        for (int i = m + 1; i < z.length; i++) {
            if (z[i] >= m) res.add(i - m - 1);
        }
        return res;
    }

    static String randomString(Random rnd, int len, int alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append((char) ('a' + rnd.nextInt(alphabet)));
        return sb.toString();
    }
}
