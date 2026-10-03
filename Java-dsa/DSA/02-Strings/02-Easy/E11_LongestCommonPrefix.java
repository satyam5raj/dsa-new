/*
 * ============================================================================
 *  EASY 11 : LONGEST COMMON PREFIX  ⭐ bonus (classic, not in leetcode.md)   LeetCode #14
 *  https://leetcode.com/problems/longest-common-prefix/
 *  Pattern: VERTICAL SCAN (compare column by column)
 * ============================================================================
 *
 *  PROBLEM
 *    Longest prefix shared by ALL strings in the array.
 *      ["flower","flow","flight"] -> "fl"
 *      ["dog","racecar","car"]    -> ""
 *
 *  ---------------------------------------------------------------------------
 *  VERTICAL SCAN
 *    Line the strings up and read COLUMN by column (index i = 0, 1, 2 ...).
 *    Stop at the first column where some string has ended or has a different char.
 *       f l o w e r
 *       f l o w
 *       f l i g h t
 *       ✓ ✓ ✗          -> "fl"
 *
 *  OTHER APPROACHES (good to mention)
 *    - SORT the array; the LCP of the whole array = the LCP of the FIRST and LAST
 *      strings (the most different pair after sorting). O(S log n)
 *    - TRIE: insert all words, walk down while a node has exactly one child.
 *      (Shines when you get MANY queries against the same set.)
 *
 *  COMPLEXITY   Time O(S), S = total characters in the worst case   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class E11_LongestCommonPrefix {

    static String longestCommonPrefix(String[] strs) {
        if (strs.length == 0) return "";
        String first = strs[0];
        for (int i = 0; i < first.length(); i++) {             // column i
            char c = first.charAt(i);
            for (int k = 1; k < strs.length; k++) {
                if (i == strs[k].length() || strs[k].charAt(i) != c) {
                    return first.substring(0, i);             // this column breaks
                }
            }
        }
        return first;                                         // the first string itself is the prefix
    }

    static String longestCommonPrefixSorted(String[] strs) {
        String[] a = strs.clone();
        Arrays.sort(a);
        String lo = a[0], hi = a[a.length - 1];
        int i = 0;
        while (i < lo.length() && i < hi.length() && lo.charAt(i) == hi.charAt(i)) i++;
        return lo.substring(0, i);
    }

    public static void main(String[] args) {
        check("[flower,flow,flight]", "fl", longestCommonPrefix(new String[]{"flower", "flow", "flight"}));
        check("[dog,racecar,car]", "", longestCommonPrefix(new String[]{"dog", "racecar", "car"}));
        check("[ab,a] (short string ends first)", "a", longestCommonPrefix(new String[]{"ab", "a"}));
        check("[same,same]", "same", longestCommonPrefix(new String[]{"same", "same"}));
        check("sorting approach", "fl", longestCommonPrefixSorted(new String[]{"flower", "flow", "flight"}));
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
