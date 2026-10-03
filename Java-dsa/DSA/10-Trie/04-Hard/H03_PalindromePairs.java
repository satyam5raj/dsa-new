/*
 * ============================================================================
 *  HARD 03 : PALINDROME PAIRS  ⭐ bonus       LeetCode #336
 *  https://leetcode.com/problems/palindrome-pairs/
 *  Pattern: for each word, look up REVERSED pieces - split into prefix/suffix cases
 * ============================================================================
 *
 *  PROBLEM
 *    Unique words. Return all pairs (i, j), i != j, where words[i] + words[j] is a
 *    palindrome.
 *      ["abcd","dcba","lls","s","sssll"] -> [[0,1],[1,0],[3,2],[2,4]]
 *        abcd+dcba, dcba+abcd, s+lls = "slls", lls+sssll = "llssssll"
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every pair, check the concatenation -> O(n^2 * L). n = 5000 -> too slow.
 *
 *  KEY INSIGHT: when is A + B a palindrome? Split the longer word at some point k:
 *    CASE 1 (A is at least as long):  A = P + S, where
 *        S is a palindrome    and    B == reverse(P)
 *        e.g. A = "abcdd" = "abc" + "dd", B = "cba":  "abc|dd|cba" is a palindrome
 *        (the palindromic middle S sits between P and its mirror B)
 *    CASE 2 (B is longer):  B = P + S, where
 *        P is a palindrome    and    A == reverse(S)
 *        e.g. B = "sssll" = "ss" + "sll", A = "lls":  "lls|ss|sll" = "llssssll"
 *  So for each word w and every split k (0..len):
 *        prefix = w[0..k), suffix = w[k..)
 *        if suffix is a palindrome and reverse(prefix) is another word x -> (w, x)
 *        if prefix is a palindrome and reverse(suffix) is another word x -> (x, w)
 *  Look-ups in a HashMap word -> index: O(n * L^2) total.
 *  (Skip one of the two cases when the split is at an end, so pairs aren't added
 *   twice when both pieces are empty / whole.)
 *
 *  THE TRIE VERSION stores words reversed and walks each word down it, collecting
 *  "rest of this path is a palindrome" information in nodes - same complexity,
 *  much more code. The hash-map version is what most people write in interviews.
 *
 *  DRY RUN w = "lls": k=0: prefix "", suffix "lls" (not palindrome); prefix "" is a
 *    palindrome, reverse(suffix) = "sll" - not a word.  k=2: prefix "ll" is a
 *    palindrome, reverse(suffix "s") = "s" is word 3 -> pair (3, 2) "s"+"lls".
 *
 *  COMPLEXITY   Time O(n * L^2)   Space O(n * L)
 * ============================================================================
 */

import java.util.*;

public class H03_PalindromePairs {

    static List<List<Integer>> palindromePairs(String[] words) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < words.length; i++) index.put(words[i], i);
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            for (int k = 0; k <= w.length(); k++) {
                String prefix = w.substring(0, k), suffix = w.substring(k);
                if (isPalindrome(suffix)) {                                // w + x with x = reverse(prefix)
                    Integer j = index.get(new StringBuilder(prefix).reverse().toString());
                    if (j != null && j != i) result.add(List.of(i, j));
                }
                if (k > 0 && isPalindrome(prefix)) {                       // x + w with x = reverse(suffix)
                    Integer j = index.get(new StringBuilder(suffix).reverse().toString());   // k > 0 avoids a duplicate
                    if (j != null && j != i) result.add(List.of(j, i));
                }
            }
        }
        return result;
    }

    static boolean isPalindrome(String s) {
        for (int i = 0, j = s.length() - 1; i < j; i++, j--) if (s.charAt(i) != s.charAt(j)) return false;
        return true;
    }

    static Set<List<Integer>> brute(String[] w) {
        Set<List<Integer>> out = new HashSet<>();
        for (int i = 0; i < w.length; i++) for (int j = 0; j < w.length; j++) if (i != j && isPalindrome(w[i] + w[j])) out.add(List.of(i, j));
        return out;
    }

    public static void main(String[] args) {
        String[] ex = {"abcd", "dcba", "lls", "s", "sssll"};
        check("example 1", brute(ex), new HashSet<>(palindromePairs(ex)));
        check("example 1 count", 4, palindromePairs(ex).size());
        String[] ex2 = {"a", ""};
        List<List<Integer>> r2 = new ArrayList<>(palindromePairs(ex2));
        r2.sort(Comparator.comparing(Object::toString));
        check("with the empty string", "[[0, 1], [1, 0]]", r2);
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            Set<String> ws = new LinkedHashSet<>();
            for (int i = 0; i < 1 + rnd.nextInt(8); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < rnd.nextInt(4); j++) sb.append((char) ('a' + rnd.nextInt(2)));
                ws.add(sb.toString());
            }
            String[] arr = ws.toArray(new String[0]);
            List<List<Integer>> got = palindromePairs(arr);
            ok &= new HashSet<>(got).equals(brute(arr)) && new HashSet<>(got).size() == got.size();   // no duplicates
        }
        check("1,000 random word sets vs all pairs", true, ok);
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
