/*
 * ============================================================================
 *  MEDIUM 02 : LONGEST REPEATING CHARACTER REPLACEMENT   LeetCode #424
 *  https://leetcode.com/problems/longest-repeating-character-replacement/
 *  Pattern: SLIDING WINDOW + "window length - max frequency <= k"
 * ============================================================================
 *
 *  PROBLEM
 *    Uppercase string s. You may change at most k characters. Return the length
 *    of the longest substring that can be made of ONE repeated letter.
 *      "ABAB", k=2    -> 4   (change both A's, or both B's)
 *      "AABABBA", k=1 -> 4   ("AABA" -> "AAAA")
 *
 *  ---------------------------------------------------------------------------
 *  KEY QUESTION: for a given window, how many changes does it need?
 *    Keep the MOST FREQUENT letter and change everything else:
 *      changes needed = windowLength - maxFrequency
 *    The window is VALID if  windowLength - maxFrequency <= k
 *
 *  SLIDING WINDOW
 *    expand right, update count[s[right]] and maxFreq
 *    while (window - maxFreq > k): remove s[left], left++
 *    best = max(best, window)
 *
 *  THE FAMOUS SUBTLETY: we never DECREASE maxFreq when shrinking, even if it's
 *  now stale. Why is that OK? The answer only grows when we find a window with
 *  a HIGHER maxFreq than before. A stale (too big) maxFreq can only keep the
 *  window at its current size. It can't create a wrong larger answer. So we can
 *  skip recomputing it (which would cost 26 steps). Also correct (and simpler
 *  to explain): recompute max over the 26 counts each time -> O(26n).
 *
 *  DRY RUN "AABABBA", k=1
 *    window "AABA": length 4, maxFreq(A)=3 -> 4-3=1 <= 1 -> valid -> best 4
 *    add 'B' -> "AABAB": 5-3=2 > 1 -> shrink to "ABAB" ...
 *
 *  COMPLEXITY   Time O(n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class M02_LongestRepeatingCharacterReplacement {

    static int characterReplacement(String s, int k) {
        int[] count = new int[26];
        int left = 0, maxFreq = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            maxFreq = Math.max(maxFreq, ++count[s.charAt(right) - 'A']);
            while ((right - left + 1) - maxFreq > k) {           // needs more than k changes
                count[s.charAt(left) - 'A']--;
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static int bruteForce(String s, int k) {
        int best = 0;
        for (int i = 0; i < s.length(); i++) {
            int[] c = new int[26];
            int mx = 0;
            for (int j = i; j < s.length(); j++) {
                mx = Math.max(mx, ++c[s.charAt(j) - 'A']);
                if (j - i + 1 - mx <= k) best = Math.max(best, j - i + 1);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        check("ABAB k=2", 4, characterReplacement("ABAB", 2));
        check("AABABBA k=1", 4, characterReplacement("AABABBA", 1));
        check("AAAA k=0", 4, characterReplacement("AAAA", 0));
        check("ABCDE k=1", 2, characterReplacement("ABCDE", 1));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = 1 + rnd.nextInt(15); i < n; i++) sb.append((char) ('A' + rnd.nextInt(3)));
            int k = rnd.nextInt(4);
            if (characterReplacement(sb.toString(), k) != bruteForce(sb.toString(), k)) ok = false;
        }
        check("500 random strings vs brute force", true, ok);
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
