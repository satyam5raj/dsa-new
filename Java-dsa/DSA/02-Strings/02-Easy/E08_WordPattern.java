/*
 * ============================================================================
 *  EASY 08 : WORD PATTERN                    LeetCode #290
 *  https://leetcode.com/problems/word-pattern/
 *  Pattern: CHARACTER <-> WORD BIJECTION (Isomorphic Strings with words)
 * ============================================================================
 *
 *  PROBLEM
 *    Does s follow pattern? Each letter maps to exactly one word, and each word
 *    to exactly one letter.
 *      pattern "abba", s "dog cat cat dog"  -> true
 *      pattern "abba", s "dog cat cat fish" -> false
 *      pattern "aaaa", s "dog cat cat dog"  -> false
 *      pattern "abba", s "dog dog dog dog"  -> false   (a and b both -> dog)
 *
 *  ---------------------------------------------------------------------------
 *  This is E07 with WORDS instead of characters:
 *    1. split s into words; if the count != pattern length -> false
 *    2. two maps: char -> word and word -> char; any conflict -> false
 *
 *  JAVA TRAP: compare the mapped words with .equals(), not == (Strings!).
 *
 *  DRY RUN "abba" / "dog dog dog dog"
 *    a->dog, dog->a
 *    b: b unmapped, but dog is already mapped from 'a' -> conflict -> false
 *
 *  COMPLEXITY   Time O(n + total word length)   Space O(number of words)
 * ============================================================================
 */

import java.util.*;

public class E08_WordPattern {

    static boolean wordPattern(String pattern, String s) {
        String[] words = s.split(" ");
        if (words.length != pattern.length()) return false;
        Map<Character, String> charToWord = new HashMap<>();
        Map<String, Character> wordToChar = new HashMap<>();
        for (int i = 0; i < words.length; i++) {
            char c = pattern.charAt(i);
            String w = words[i];
            if (charToWord.containsKey(c) && !charToWord.get(c).equals(w)) return false;
            if (wordToChar.containsKey(w) && wordToChar.get(w) != c) return false;
            charToWord.put(c, w);
            wordToChar.put(w, c);
        }
        return true;
    }

    public static void main(String[] args) {
        check("abba / dog cat cat dog", true, wordPattern("abba", "dog cat cat dog"));
        check("abba / dog cat cat fish", false, wordPattern("abba", "dog cat cat fish"));
        check("aaaa / dog cat cat dog", false, wordPattern("aaaa", "dog cat cat dog"));
        check("abba / dog dog dog dog", false, wordPattern("abba", "dog dog dog dog"));
        check("aaa / aa aa aa aa (count differs)", false, wordPattern("aaa", "aa aa aa aa"));
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
