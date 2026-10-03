/*
 * ============================================================================
 *  HARD 03 : SUBSTRING WITH CONCATENATION OF ALL WORDS  ⭐ bonus   LeetCode #30
 *  https://leetcode.com/problems/substring-with-concatenation-of-all-words/
 *  Pattern: SLIDING WINDOW OVER WORDS (word-sized steps, one pass per offset)
 * ============================================================================
 *
 *  PROBLEM
 *    words all have the SAME length L. Find every start index in s where some
 *    concatenation of ALL the words (each used exactly once, any order) appears.
 *      s "barfoothefoobarman", words ["foo","bar"] -> [0, 9]   ("barfoo", "foobar")
 *      s "barfoofoobarthefoobarman", words ["bar","foo","the"] -> [6, 9, 12]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for every start i, cut the next k words of length L and compare
 *  the counts with 'words' -> O(n * k * L)
 *
 *  KEY INSIGHT: a valid window is made of WORD-SIZED chunks. Starting from an
 *  offset in 0..L-1 and jumping L characters at a time, we see a sequence of
 *  "tokens". The problem becomes: find windows of exactly k CONSECUTIVE TOKENS
 *  whose counts match 'words', i.e. sliding window + counts, with words instead
 *  of characters (like Find All Anagrams).
 *    for offset in 0..L-1:
 *      left = offset, seen = {}, count = 0
 *      for right = offset; right + L <= n; right += L:
 *        w = s[right .. right+L)
 *        if w not in words: reset (seen = {}, count = 0, left = right + L)
 *        else:
 *          seen[w]++, count++
 *          while seen[w] > need[w]: drop the leftmost token (left += L, count--)
 *          if count == k: record left, then drop the leftmost token to keep sliding
 *
 *  WHY L OFFSETS? A match can start at any index, and the tokens line up only
 *  when we start at the right offset mod L. Together the L passes cover every start.
 *
 *  COMPLEXITY   Time O(L * (n / L) * L) = O(n * L) (each pass touches each token
 *               O(1) times, and building a token costs L)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class H03_SubstringWithConcatenationOfAllWords {

    static List<Integer> findSubstring(String s, String[] words) {
        List<Integer> result = new ArrayList<>();
        if (words.length == 0) return result;
        int L = words[0].length(), k = words.length, n = s.length();
        Map<String, Integer> need = new HashMap<>();
        for (String w : words) need.merge(w, 1, Integer::sum);

        for (int offset = 0; offset < L; offset++) {
            Map<String, Integer> seen = new HashMap<>();
            int left = offset, count = 0;
            for (int right = offset; right + L <= n; right += L) {
                String w = s.substring(right, right + L);
                if (!need.containsKey(w)) {                 // a token that can't be part of any match
                    seen.clear();
                    count = 0;
                    left = right + L;
                    continue;
                }
                seen.merge(w, 1, Integer::sum);
                count++;
                while (seen.get(w) > need.get(w)) {         // too many copies of w: shrink
                    String drop = s.substring(left, left + L);
                    seen.merge(drop, -1, Integer::sum);
                    count--;
                    left += L;
                }
                if (count == k) {                           // exactly the multiset of words
                    result.add(left);
                    String drop = s.substring(left, left + L);
                    seen.merge(drop, -1, Integer::sum);
                    count--;
                    left += L;
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    static List<Integer> brute(String s, String[] words) {
        List<Integer> res = new ArrayList<>();
        int L = words[0].length(), k = words.length;
        Map<String, Integer> need = new HashMap<>();
        for (String w : words) need.merge(w, 1, Integer::sum);
        for (int i = 0; i + L * k <= s.length(); i++) {
            Map<String, Integer> got = new HashMap<>();
            for (int j = 0; j < k; j++) got.merge(s.substring(i + j * L, i + (j + 1) * L), 1, Integer::sum);
            if (got.equals(need)) res.add(i);
        }
        return res;
    }

    public static void main(String[] args) {
        check("barfoothefoobarman [foo,bar]", "[0, 9]", findSubstring("barfoothefoobarman", new String[]{"foo", "bar"}));
        check("wordgoodgoodgoodbestword [word,good,best,word]", "[]",
                findSubstring("wordgoodgoodgoodbestword", new String[]{"word", "good", "best", "word"}));
        check("barfoofoobarthefoobarman [bar,foo,the]", "[6, 9, 12]",
                findSubstring("barfoofoobarthefoobarman", new String[]{"bar", "foo", "the"}));
        check("aaaa [aa,aa] (repeated word)", "[0]", findSubstring("aaaa", new String[]{"aa", "aa"}));
        Random rnd = new Random(21);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            String s = random(rnd, rnd.nextInt(16));
            String[] words = new String[1 + rnd.nextInt(3)];
            int L = 1 + rnd.nextInt(2);
            for (int i = 0; i < words.length; i++) words[i] = random(rnd, L);
            if (!findSubstring(s, words).equals(brute(s, words))) ok = false;
        }
        check("400 random cases vs brute force", true, ok);
        summary();
    }

    static String random(Random r, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(2)));
        return sb.toString();
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
