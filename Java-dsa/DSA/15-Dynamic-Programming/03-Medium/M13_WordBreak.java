/*
 * ============================================================================
 *  MEDIUM 13 : WORD BREAK                    LeetCode #139
 *  https://leetcode.com/problems/word-break/
 *  Pattern: 1-D DP over PREFIXES ("is the prefix of length i breakable?")
 * ============================================================================
 *
 *  PROBLEM
 *    Can s be segmented into a sequence of dictionary words (reuse allowed)?
 *      "leetcode", [leet, code]               -> true
 *      "applepenapple", [apple, pen]          -> true
 *      "catsandog", [cats, dog, sand, and, cat] -> false
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: try every first word, recurse on the rest -> exponential on
 *  "aaaa...ab" with dict [a, aa, aaa, ...] (the same suffixes are re-tried).
 *
 *  KEY INSIGHT: whether the suffix from position i can be broken doesn't depend on
 *  HOW we got to i. So the state is just the position.
 *      STATE       ok[i] = the prefix s[0..i) can be segmented
 *      TRANSITION  ok[i] = some j < i with ok[j] && s[j..i) in dict
 *      BASE        ok[0] = true
 *      ANSWER      ok[n]
 *  Speed-up: only try j with i - j <= longest word length.
 *
 *  DRY RUN "leetcode": ok0=T, ok4 ("leet")=T, ok8 (ok4 && "code")=T
 *
 *  COMPLEXITY   Time O(n * L) substring checks (L = max word length)   Space O(n)
 *  Related: Trie H04 Concatenated Words runs this for every word, with a trie.
 * ============================================================================
 */

import java.util.*;

public class M13_WordBreak {

    static boolean wordBreak(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);
        int maxLen = 0;
        for (String w : wordDict) maxLen = Math.max(maxLen, w.length());
        boolean[] ok = new boolean[s.length() + 1];
        ok[0] = true;
        for (int i = 1; i <= s.length(); i++)
            for (int j = i - 1; j >= Math.max(0, i - maxLen) && !ok[i]; j--)
                ok[i] = ok[j] && dict.contains(s.substring(j, i));   // last word = s[j..i)
        return ok[s.length()];
    }

    static long bruteCalls;

    static boolean brute(String s, int start, Set<String> dict) {
        bruteCalls++;
        if (start == s.length()) return true;
        for (int end = start + 1; end <= s.length(); end++)
            if (dict.contains(s.substring(start, end)) && brute(s, end, dict)) return true;
        return false;
    }

    public static void main(String[] args) {
        check("leetcode", true, wordBreak("leetcode", List.of("leet", "code")));
        check("applepenapple", true, wordBreak("applepenapple", List.of("apple", "pen")));
        check("catsandog", false, wordBreak("catsandog", List.of("cats", "dog", "sand", "and", "cat")));

        String evil = "a".repeat(22) + "b";
        List<String> d = List.of("a", "aa", "aaa", "aaaa", "aaaaa");
        bruteCalls = 0;
        boolean b = brute(evil, 0, new HashSet<>(d));
        System.out.printf("   \"a\"*22 + \"b\": plain recursion makes %,d calls; DP fills 24 cells%n", bruteCalls);
        check("evil case: DP == recursion", b, wordBreak(evil, d));

        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            List<String> dict = new ArrayList<>();
            for (int k = 0; k < 1 + rnd.nextInt(5); k++) dict.add(rs(rnd, 1 + rnd.nextInt(3)));
            String s = rs(rnd, rnd.nextInt(12));
            ok &= wordBreak(s, dict) == brute(s, 0, new HashSet<>(dict));
        }
        check("500 random vs recursion", true, ok);
        summary();
    }

    static String rs(Random r, int len) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < len; i++) sb.append((char) ('a' + r.nextInt(2))); return sb.toString(); }

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
