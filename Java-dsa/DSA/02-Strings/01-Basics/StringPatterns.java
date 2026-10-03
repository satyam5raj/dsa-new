/*
 * ============================================================================
 *  STRINGS 1.2 : THE STRING PATTERNS
 * ============================================================================
 *
 *  Run:  java StringPatterns.java
 *
 *  -----------------------------------------------------------------------------------
 *   Pattern                       Clue in the problem                       Problems
 *  -----------------------------------------------------------------------------------
 *   1. Frequency array / map      anagram, "same letters", ransom,           E01 E06 E09 E10
 *                                 first unique, sort by frequency            M06 M09
 *   2. Two pointers               palindrome, reverse, compare from ends     E02 E03 E05
 *   3. Sliding window + counts    "longest/shortest SUBSTRING with ...",     M01-M05 H01 H03
 *                                 anagram/permutation OF a substring
 *   4. Character mapping          isomorphic, word pattern (bijection)       E07 E08
 *   5. Canonical key / signature  group things that are "equivalent"         M06
 *   6. Expand around center       palindromic SUBSTRINGS                     M10 M11
 *   7. String matching            find a pattern in a text                   E12 H02 (KMP)
 *  -----------------------------------------------------------------------------------
 *
 *  SUBSTRING vs SUBSEQUENCE (they're different!)
 *    substring   = CONTIGUOUS chars:    "ell" is a substring of "hello"
 *    subsequence = keep the order, may SKIP chars: "hlo" is a subsequence of "hello"
 *    Substring problems -> sliding window / two pointers / hashing.
 *    Subsequence problems -> usually DYNAMIC PROGRAMMING (covered later).
 *    A string of length n has n(n+1)/2 substrings but 2^n subsequences.
 */

import java.util.*;

public class StringPatterns {

    public static void main(String[] args) {
        frequencyPattern();
        twoPointerPalindrome();
        slidingWindowWithCounts();
        characterMapping();
        canonicalKey();
        expandAroundCenter();
    }

    // =================================================================
    // 1. FREQUENCY ARRAY: count with +1 for one string, -1 for the other.
    //    All zeros at the end <=> same multiset of characters.
    // =================================================================
    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
            count[t.charAt(i) - 'a']--;
        }
        for (int c : count) if (c != 0) return false;
        return true;
    }

    static void frequencyPattern() {
        System.out.println("===== 1. FREQUENCY ARRAY =====");
        System.out.println("   isAnagram(anagram, nagaram) = " + isAnagram("anagram", "nagaram"));
        System.out.println("   isAnagram(rat, car)         = " + isAnagram("rat", "car"));
    }

    // =================================================================
    // 2. TWO POINTERS: compare from both ends, walking inward
    // =================================================================
    static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i++) != s.charAt(j--)) return false;
        }
        return true;
    }

    static void twoPointerPalindrome() {
        System.out.println("\n===== 2. TWO POINTERS =====");
        System.out.println("   isPalindrome(racecar) = " + isPalindrome("racecar") + ", isPalindrome(abca) = " + isPalindrome("abca"));
    }

    // =================================================================
    // 3. SLIDING WINDOW WITH CHARACTER COUNTS - the most important string template
    //
    //    int[] count = new int[128];  int left = 0;
    //    for (int right = 0; right < s.length(); right++) {
    //        count[s.charAt(right)]++;                    // add the new char
    //        while (window is INVALID) {                  // e.g. a repeated char
    //            count[s.charAt(left)]--; left++;         // remove from the left
    //        }
    //        best = max(best, right - left + 1);          // valid window
    //    }
    //
    //    Example: longest substring without repeating characters.
    // =================================================================
    static int longestUniqueSubstring(String s) {
        int[] count = new int[128];
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            count[c]++;
            while (count[c] > 1) {                          // c is now repeated -> shrink
                count[s.charAt(left)]--;
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static void slidingWindowWithCounts() {
        System.out.println("\n===== 3. SLIDING WINDOW + COUNTS =====");
        System.out.println("   longest unique substring of \"abcabcbb\" = " + longestUniqueSubstring("abcabcbb") + " (\"abc\")");
    }

    // =================================================================
    // 4. CHARACTER MAPPING: does a consistent ONE-TO-ONE mapping exist?
    //    Need TWO maps (s->t and t->s). One map alone would accept "ab" -> "cc"
    //    (both a and b mapping to c is NOT one-to-one).
    // =================================================================
    static boolean isIsomorphic(String s, String t) {
        char[] sToT = new char[128], tToS = new char[128];
        for (int i = 0; i < s.length(); i++) {
            char a = s.charAt(i), b = t.charAt(i);
            if (sToT[a] == 0 && tToS[b] == 0) {             // both unmapped -> create the pair
                sToT[a] = b;
                tToS[b] = a;
            } else if (sToT[a] != b || tToS[b] != a) {
                return false;                               // conflicts with an earlier pair
            }
        }
        return true;
    }

    static void characterMapping() {
        System.out.println("\n===== 4. CHARACTER MAPPING =====");
        System.out.println("   isomorphic(egg, add) = " + isIsomorphic("egg", "add") + ", (ab, cc) = " + isIsomorphic("ab", "cc"));
    }

    // =================================================================
    // 5. CANONICAL KEY: map every "equivalent" string to the SAME key, then group
    //    by that key in a HashMap. For anagrams the key can be:
    //      - the sorted string        "eat" -> "aet"         O(k log k)
    //      - the count signature      "eat" -> "1#0#0#0#1#...#1#..."  O(k)
    // =================================================================
    static void canonicalKey() {
        System.out.println("\n===== 5. CANONICAL KEY =====");
        Map<String, List<String>> groups = new TreeMap<>();
        for (String w : new String[]{"eat", "tea", "tan", "ate", "nat", "bat"}) {
            char[] c = w.toCharArray();
            Arrays.sort(c);
            groups.computeIfAbsent(new String(c), k -> new ArrayList<>()).add(w);
        }
        System.out.println("   anagram groups by sorted key = " + groups);
    }

    // =================================================================
    // 6. EXPAND AROUND CENTER: every palindrome has a CENTER.
    //    There are 2n - 1 centers: n single characters ("aba") and n-1 gaps
    //    between characters ("abba"). Expand outward while both ends match.
    //    -> O(n^2) total, O(1) space. Simpler than DP for palindromic substrings.
    // =================================================================
    static String longestPalindrome(String s) {
        int bestStart = 0, bestLen = 0;
        for (int center = 0; center < s.length(); center++) {
            for (int[] lr : new int[][]{{center, center}, {center, center + 1}}) {   // odd, even
                int l = lr[0], r = lr[1];
                while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { l--; r++; }
                int len = r - l - 1;                       // the loop overshoots by one on each side
                if (len > bestLen) { bestLen = len; bestStart = l + 1; }
            }
        }
        return s.substring(bestStart, bestStart + bestLen);
    }

    static void expandAroundCenter() {
        System.out.println("\n===== 6. EXPAND AROUND CENTER =====");
        System.out.println("   longest palindrome in \"forgeeksskeegfor\" = " + longestPalindrome("forgeeksskeegfor"));
    }
}
