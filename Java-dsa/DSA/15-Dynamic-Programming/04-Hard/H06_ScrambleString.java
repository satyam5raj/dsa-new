/*
 * ============================================================================
 *  HARD 06 : SCRAMBLE STRING                 LeetCode #87
 *  https://leetcode.com/problems/scramble-string/
 *  Pattern: interval DP over (start1, start2, length) - 3-D state, memoized recursion
 * ============================================================================
 *
 *  PROBLEM
 *    Scramble s: split it into two non-empty parts x + y, optionally swap them to
 *    y + x, then scramble each part recursively. Is s2 a scramble of s1?
 *      "great", "rgeat" -> true       "abcde", "caebd" -> false      "a","a" -> true
 *
 *  ---------------------------------------------------------------------------
 *  RECURSION: s1 and s2 (same length n) are scrambles iff for some split k (1..n-1):
 *      NO SWAP:  scr(s1[0..k), s2[0..k))    && scr(s1[k..n), s2[k..n))
 *      SWAP:     scr(s1[0..k), s2[n-k..n))  && scr(s1[k..n), s2[0..n-k))
 *  Without memo this is exponential: the same pairs of substrings are compared again
 *  and again.
 *
 *  STATE (substrings are determined by start positions + length):
 *      dp[i][j][len] = s1[i..i+len) is a scramble of s2[j..j+len)
 *  Only O(n^3) states, each trying O(n) splits -> O(n^4).
 *  PRUNING: if the two substrings don't have the same letter counts, answer false
 *  immediately (cuts most branches).
 *
 *  DRY RUN great / rgeat: split k=2 no swap: "gr"/"rg" (swap -> true) and "eat"/"eat"
 *
 *  COMPLEXITY   Time O(n^4)   Space O(n^3)
 * ============================================================================
 */

import java.util.*;

public class H06_ScrambleString {

    static boolean isScramble(String s1, String s2) {
        int n = s1.length();
        if (n != s2.length()) return false;
        Boolean[][][] memo = new Boolean[n][n][n + 1];
        return scr(s1, s2, 0, 0, n, memo);
    }

    static boolean scr(String a, String b, int i, int j, int len, Boolean[][][] memo) {
        if (memo[i][j][len] != null) return memo[i][j][len];
        boolean res = false;
        if (a.regionMatches(i, b, j, len)) res = true;                    // identical: trivially a scramble
        else if (sameLetters(a, b, i, j, len)) {                           // else not even an anagram -> false
            for (int k = 1; k < len && !res; k++)
                res = (scr(a, b, i, j, k, memo) && scr(a, b, i + k, j + k, len - k, memo))            // no swap
                   || (scr(a, b, i, j + len - k, k, memo) && scr(a, b, i + k, j, len - k, memo));     // swap
        }
        return memo[i][j][len] = res;
    }

    static boolean sameLetters(String a, String b, int i, int j, int len) {
        int[] cnt = new int[26];
        for (int x = 0; x < len; x++) { cnt[a.charAt(i + x) - 'a']++; cnt[b.charAt(j + x) - 'a']--; }
        for (int c : cnt) if (c != 0) return false;
        return true;
    }

    // referee: generate EVERY scramble of s and check membership
    static Set<String> allScrambles(String s, Map<String, Set<String>> cache) {
        if (cache.containsKey(s)) return cache.get(s);
        Set<String> out = new HashSet<>();
        out.add(s);
        for (int k = 1; k < s.length(); k++)
            for (String x : allScrambles(s.substring(0, k), cache))
                for (String y : allScrambles(s.substring(k), cache)) { out.add(x + y); out.add(y + x); }
        cache.put(s, out);
        return out;
    }

    public static void main(String[] args) {
        check("great / rgeat", true, isScramble("great", "rgeat"));
        check("abcde / caebd", false, isScramble("abcde", "caebd"));
        check("a / a", true, isScramble("a", "a"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int n = 1 + rnd.nextInt(6);
            String s = rs(rnd, n);
            Set<String> all = allScrambles(s, new HashMap<>());
            List<String> perms = new ArrayList<>(all);
            String yes = perms.get(rnd.nextInt(perms.size()));
            String any = shuffle(s, rnd);                                      // may or may not be a scramble
            ok &= isScramble(s, yes) && isScramble(s, any) == all.contains(any);
        }
        check("300 random vs generating all scrambles", true, ok);
        summary();
    }

    static String rs(Random r, int n) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(4))); return sb.toString(); }
    static String shuffle(String s, Random r) { List<Character> l = new ArrayList<>(); for (char c : s.toCharArray()) l.add(c); Collections.shuffle(l, r); StringBuilder sb = new StringBuilder(); for (char c : l) sb.append(c); return sb.toString(); }

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
