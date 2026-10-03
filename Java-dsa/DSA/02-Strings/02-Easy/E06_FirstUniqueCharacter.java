/*
 * ============================================================================
 *  EASY 06 : FIRST UNIQUE CHARACTER IN A STRING   LeetCode #387
 *  https://leetcode.com/problems/first-unique-character-in-a-string/
 *  Pattern: FREQUENCY ARRAY + SECOND PASS
 * ============================================================================
 *
 *  PROBLEM
 *    Return the INDEX of the first character that appears exactly once, or -1.
 *      "leetcode"     -> 0   ('l')
 *      "loveleetcode" -> 2   ('v')
 *      "aabb"         -> -1
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each char, scan the whole string to count it -> O(n^2)
 *
 *  TWO PASSES                                         O(n) time, O(1) space
 *    pass 1: count every character (int[26])
 *    pass 2: walk the STRING again in order. The first char with count 1 wins.
 *  Why walk the string and not the count array? The count array is in ALPHABETICAL
 *  order, but we need the first unique character in STRING order.
 *
 *  DRY RUN "loveleetcode"
 *    counts: l2 o2 v1 e4 t1 c1 d1
 *    walk: l(2) o(2) v(1) -> index 2
 *
 *  FOLLOW-UP: a STREAM of characters (can't do a second pass)?
 *    -> keep counts + a LinkedHashMap / queue of candidates in arrival order,
 *       and pop from the front while the front's count > 1. (A classic design question.)
 * ============================================================================
 */

import java.util.*;

public class E06_FirstUniqueCharacter {

    static int firstUniqChar(String s) {
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i) - 'a']++;
        for (int i = 0; i < s.length(); i++) {
            if (count[s.charAt(i) - 'a'] == 1) return i;
        }
        return -1;
    }

    // Follow-up: first unique character after each arriving character (streaming)
    static String firstUniqueInStream(String stream) {
        int[] count = new int[26];
        Deque<Character> candidates = new ArrayDeque<>();
        StringBuilder out = new StringBuilder();
        for (char c : stream.toCharArray()) {
            if (++count[c - 'a'] == 1) candidates.addLast(c);
            while (!candidates.isEmpty() && count[candidates.peekFirst() - 'a'] > 1) candidates.pollFirst();
            out.append(candidates.isEmpty() ? '#' : candidates.peekFirst());
        }
        return out.toString();
    }

    public static void main(String[] args) {
        check("leetcode", 0, firstUniqChar("leetcode"));
        check("loveleetcode", 2, firstUniqChar("loveleetcode"));
        check("aabb (none)", -1, firstUniqChar("aabb"));
        check("z (single)", 0, firstUniqChar("z"));
        check("stream \"aabc\" -> a#bb", "a#bb", firstUniqueInStream("aabc"));
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
