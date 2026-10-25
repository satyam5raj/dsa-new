/*
 * ============================================================================
 *  ADVANCE 22 : WORD BREAK II  ⭐ bonus   LeetCode #140
 *  https://leetcode.com/problems/word-break-ii/
 *  Pattern: BACKTRACKING + MEMO (HashMap start -> sentences) + HashSet dictionary
 * ============================================================================
 *
 *  PROBLEM
 *    Return ALL ways to split s into dictionary words, as sentences (any order).
 *      s = "catsanddog", dict [cat, cats, and, sand, dog]
 *        -> ["cats and dog", "cat sand dog"]
 *      s = "catsandog" -> []
 *
 *  ---------------------------------------------------------------------------
 *  WORD BREAK I (DP M13) asks "is there a split?". Here we must LIST every split.
 *
 *  BACKTRACKING: at position start, try every dictionary word that begins there and
 *  recurse on the rest. Correct, but the same suffix is re-solved again and again:
 *  for s = "aaaa...a" with dict [a, aa, aaa] the number of calls explodes.
 *
 *  MEMO: the sentences for the suffix starting at i NEVER depend on how we got to i.
 *  So solve(i) is computed once and cached in a HashMap<Integer, List<String>>.
 *    solve(n) = [""]                         (one way to split nothing)
 *    solve(i) = for each word w = s[i..j) in the dictionary:
 *                   for each sentence rest in solve(j): w + " " + rest
 *  A suffix that can't be split caches an EMPTY list - it's never explored again.
 *  Helpers: a HashSet for O(1) word checks, and the longest word length so we never
 *  try substrings longer than any word.
 *
 *  NOTE: the answer itself can be exponentially large ("aaaa..." with [a, aa]); no
 *  algorithm beats the size of its output. Memo removes the REPEATED work.
 *
 *  DRY RUN "catsanddog"
 *    solve(10) = [""]
 *    solve(7)  "dog" -> ["dog"]
 *    solve(4)  "and" + solve(7) -> ["and dog"]          solve(3) "sand" + solve(7) -> ["sand dog"]
 *    solve(0)  "cat" + solve(3) -> "cat sand dog";  "cats" + solve(4) -> "cats and dog"
 *
 *  COMPLEXITY   O(n * L) substring checks per position + O(size of the output)
 * ============================================================================
 */

import java.util.*;

public class A22_WordBreakII {

    // ---------------- Approach 1: plain backtracking (the referee) ----------------
    static List<String> wordBreakBrute(String s, List<String> wordDict) {
        List<String> out = new ArrayList<>();
        backtrack(s, 0, new HashSet<>(wordDict), new ArrayList<>(), out);
        return out;
    }

    private static void backtrack(String s, int start, Set<String> words, List<String> path, List<String> out) {
        if (start == s.length()) { out.add(String.join(" ", path)); return; }
        for (int end = start + 1; end <= s.length(); end++) {
            String word = s.substring(start, end);
            if (!words.contains(word)) continue;
            path.add(word);
            backtrack(s, end, words, path, out);
            path.remove(path.size() - 1);                                // undo the choice
        }
    }

    // ---------------- Approach 2: backtracking + memo on the start index (main solution) ----------------
    static List<String> wordBreak(String s, List<String> wordDict) {
        Set<String> words = new HashSet<>(wordDict);
        int maxLen = 0;
        for (String w : wordDict) maxLen = Math.max(maxLen, w.length());
        return solve(s, 0, words, maxLen, new HashMap<>());
    }

    private static List<String> solve(String s, int start, Set<String> words, int maxLen, Map<Integer, List<String>> memo) {
        if (start == s.length()) return List.of("");                      // one way to split the empty suffix
        List<String> cached = memo.get(start);
        if (cached != null) return cached;
        List<String> out = new ArrayList<>();
        for (int end = start + 1; end <= Math.min(s.length(), start + maxLen); end++) {
            String word = s.substring(start, end);
            if (!words.contains(word)) continue;
            for (String rest : solve(s, end, words, maxLen, memo)) out.add(rest.isEmpty() ? word : word + " " + rest);
        }
        memo.put(start, out);                                             // an empty list means "can't be split"
        return out;
    }

    public static void main(String[] args) {
        check("catsanddog", "[cat sand dog, cats and dog]", sorted(wordBreak("catsanddog", List.of("cat", "cats", "and", "sand", "dog"))));
        check("pineapplepenapple", "[pine apple pen apple, pine applepen apple, pineapple pen apple]",
                sorted(wordBreak("pineapplepenapple", List.of("apple", "pen", "applepen", "pine", "pineapple"))));
        check("catsandog (no split)", "[]", wordBreak("catsandog", List.of("cats", "dog", "sand", "and", "cat")));
        String hard = "a".repeat(150) + "b";                              // no split exists, but brute force would never finish
        check("150 a's + b: memo answers instantly", "[]", wordBreak(hard, List.of("a", "aa", "aaa", "aaaa")));

        Random rnd = new Random(22);
        boolean ok = true;
        for (int t = 0; t < 1500; t++) {
            List<String> dict = new ArrayList<>();
            for (int i = rnd.nextInt(6); i >= 0; i--) {
                StringBuilder w = new StringBuilder();
                for (int l = 1 + rnd.nextInt(3); l > 0; l--) w.append((char) ('a' + rnd.nextInt(2)));
                if (!dict.contains(w.toString())) dict.add(w.toString());
            }
            StringBuilder s = new StringBuilder();
            for (int l = 1 + rnd.nextInt(10); l > 0; l--) s.append((char) ('a' + rnd.nextInt(2)));
            ok &= sorted(wordBreak(s.toString(), dict)).equals(sorted(wordBreakBrute(s.toString(), dict)));
        }
        check("1,500 random cases vs plain backtracking", true, ok);
        summary();
    }

    static List<String> sorted(List<String> l) { List<String> c = new ArrayList<>(l); Collections.sort(c); return c; }

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
