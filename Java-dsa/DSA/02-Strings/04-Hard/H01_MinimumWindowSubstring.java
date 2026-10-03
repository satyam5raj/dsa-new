/*
 * ============================================================================
 *  HARD 01 : MINIMUM WINDOW SUBSTRING        LeetCode #76
 *  https://leetcode.com/problems/minimum-window-substring/
 *  Pattern: SLIDING WINDOW (expand until valid, shrink while valid) + "missing" counter
 *  ("revise deeply" says leetcode.md: THE sliding-window interview problem)
 * ============================================================================
 *
 *  PROBLEM
 *    Smallest substring of s that contains EVERY character of t (with
 *    multiplicity). Return "" if none.
 *      s "ADOBECODEBANC", t "ABC" -> "BANC"
 *      s "a", t "aa"              -> ""     (need two a's)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: every substring (n^2), check whether it covers t (n) -> O(n^3)
 *
 *  SLIDING WINDOW (the "minimum" template from Arrays/M11, with character counts)
 *    need[c]  = how many more c's the window still needs (starts as t's counts)
 *    missing  = total characters still needed (starts as |t|)
 *
 *    for right in s:
 *      c = s[right]; if need[c] > 0 -> missing--;  need[c]--        // EXPAND
 *      while missing == 0:                                           // window covers t
 *        record [left..right] if it's the smallest so far
 *        d = s[left]; need[d]++; if need[d] > 0 -> missing++         // SHRINK
 *        left++
 *
 *    need[c] goes NEGATIVE for surplus characters (more than t needs, or not in t
 *    at all). When such a char leaves, need goes back toward 0 but not above it,
 *    so 'missing' doesn't change. Only losing a REQUIRED character breaks validity.
 *
 *  DRY RUN s="ADOBECODEBANC", t="ABC"   (only the key moments)
 *    right=5 ('C'): window "ADOBEC" covers A,B,C -> record length 6
 *       shrink: drop 'A' -> missing=1 -> stop shrinking
 *    right=10 ('A'): window "DOBECODEBA" valid again -> shrink: drop D,O,B,E,C...
 *       "CODEBA" (6), "ODEBA" loses C -> stop
 *    right=12 ('C'): "ODEBANC" valid -> shrink to "BANC" (4) -> record, drop B -> stop
 *    answer "BANC"
 *
 *  COMPLEXITY   Time O(|s| + |t|): each index enters and leaves once   Space O(1) (128 counters)
 * ============================================================================
 */

import java.util.*;

public class H01_MinimumWindowSubstring {

    static String minWindow(String s, String t) {
        if (t.isEmpty() || t.length() > s.length()) return "";
        int[] need = new int[128];
        for (char c : t.toCharArray()) need[c]++;
        int missing = t.length();
        int left = 0, bestStart = 0, bestLen = Integer.MAX_VALUE;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (need[c] > 0) missing--;            // a required char arrived
            need[c]--;

            while (missing == 0) {                 // valid: try to shrink
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    bestStart = left;
                }
                char d = s.charAt(left++);
                need[d]++;
                if (need[d] > 0) missing++;        // we just dropped a required char
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    // brute force for cross-checking: smallest length, then leftmost start
    static String minWindowBrute(String s, String t) {
        String best = "";
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + 1; j <= s.length(); j++) {
                if ((best.isEmpty() || j - i < best.length()) && covers(s.substring(i, j), t)) best = s.substring(i, j);
            }
        }
        return best;
    }

    static boolean covers(String w, String t) {
        int[] c = new int[128];
        for (char ch : w.toCharArray()) c[ch]++;
        for (char ch : t.toCharArray()) if (--c[ch] < 0) return false;
        return true;
    }

    public static void main(String[] args) {
        check("ADOBECODEBANC / ABC", "BANC", minWindow("ADOBECODEBANC", "ABC"));
        check("a / a", "a", minWindow("a", "a"));
        check("a / aa (impossible)", "", minWindow("a", "aa"));
        check("aaflslflsldkalskaaa / aaa", "aaa", minWindow("aaflslflsldkalskaaa", "aaa"));
        check("bba / ab", "ba", minWindow("bba", "ab"));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int k = 0; k < 400; k++) {
            String s = random(rnd, rnd.nextInt(14)), t = random(rnd, 1 + rnd.nextInt(3));
            if (!minWindow(s, t).equals(minWindowBrute(s, t))) ok = false;
        }
        check("400 random cases vs brute force", true, ok);
        summary();
    }

    static String random(Random r, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(3)));
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
