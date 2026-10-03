/*
 * ============================================================================
 *  MEDIUM 06 : GROUP ANAGRAMS                LeetCode #49
 *  https://leetcode.com/problems/group-anagrams/
 *  Pattern: CANONICAL KEY + HASHMAP GROUPING
 * ============================================================================
 *
 *  PROBLEM
 *    Group the words that are anagrams of each other (any order).
 *      ["eat","tea","tan","ate","nat","bat"] -> [["eat","tea","ate"],["tan","nat"],["bat"]]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: compare every pair with isAnagram -> O(n^2 * k)
 *
 *  CANONICAL KEY: give every word a KEY that is identical for all its anagrams,
 *  then group with  Map<key, List<word>>.
 *
 *  KEY OPTION 1: the SORTED word        "eat" -> "aet"        O(k log k) per word
 *  KEY OPTION 2: the LETTER COUNTS      "eat" -> "a1e1t1"     O(k) per word
 *    (build it as a string, e.g. "#1#0#0#0#1..." or only the non-zero letters)
 *
 *  DRY RUN (sorted keys)
 *    eat->aet  tea->aet  tan->ant  ate->aet  nat->ant  bat->abt
 *    map: aet=[eat,tea,ate], ant=[tan,nat], abt=[bat]
 *
 *  COMPLEXITY  n words of length up to k
 *    sorted key: O(n * k log k)     count key: O(n * k)     Space O(n * k)
 *  computeIfAbsent(key, k -> new ArrayList<>()).add(word) is the idiomatic grouping line.
 * ============================================================================
 */

import java.util.*;

public class M06_GroupAnagrams {

    static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : strs) {
            char[] c = word.toCharArray();
            Arrays.sort(c);
            String key = new String(c);                      // canonical form
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }

    static List<List<String>> groupAnagramsByCount(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : strs) {
            int[] count = new int[26];
            for (char ch : word.toCharArray()) count[ch - 'a']++;
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                if (count[i] > 0) key.append((char) ('a' + i)).append(count[i]);   // e.g. "a1e1t1"
            }
            groups.computeIfAbsent(key.toString(), k -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }

    // The order of groups (and of words inside a group) isn't specified -> normalise for testing
    static String normalise(List<List<String>> groups) {
        List<String> parts = new ArrayList<>();
        for (List<String> g : groups) {
            List<String> copy = new ArrayList<>(g);
            Collections.sort(copy);
            parts.add(copy.toString());
        }
        Collections.sort(parts);
        return parts.toString();
    }

    public static void main(String[] args) {
        String[] words = {"eat", "tea", "tan", "ate", "nat", "bat"};
        check("sorted key", "[[ate, eat, tea], [bat], [nat, tan]]", normalise(groupAnagrams(words)));
        check("count key", "[[ate, eat, tea], [bat], [nat, tan]]", normalise(groupAnagramsByCount(words)));
        check("[\"\"] (empty string)", "[[]]", normalise(groupAnagrams(new String[]{""})));
        check("[a]", "[[a]]", normalise(groupAnagrams(new String[]{"a"})));
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
