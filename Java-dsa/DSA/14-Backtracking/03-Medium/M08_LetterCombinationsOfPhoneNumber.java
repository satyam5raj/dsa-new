/*
 * ============================================================================
 *  MEDIUM 08 : LETTER COMBINATIONS OF A PHONE NUMBER   LeetCode #17
 *  https://leetcode.com/problems/letter-combinations-of-a-phone-number/
 *  Pattern: one LEVEL per input position, branch over that digit's letters
 * ============================================================================
 *
 *  PROBLEM
 *    Digits 2-9 on a phone keypad; return every letter string they could spell.
 *      "23" -> ["ad","ae","af","bd","be","bf","cd","ce","cf"]      "" -> []
 *
 *  ---------------------------------------------------------------------------
 *  Not subsets or permutations: here the tree has exactly len(digits) levels, and
 *  level i branches over the 3-4 letters of digits[i] (a CARTESIAN PRODUCT).
 *     bt(i, sb): if i == len: record sb
 *                for ch in letters(digits[i]): sb.append(ch); bt(i+1); sb.deleteLast
 *  Using a StringBuilder (append / delete last) is the same choose / un-choose.
 *
 *  ITERATIVE ALTERNATIVE: start with [""]; for each digit, extend every string with
 *  each of its letters (BFS-style).
 *
 *  EDGE CASE: empty input -> [] (not [""]).
 *
 *  COMPLEXITY   Time O(4^n * n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M08_LetterCombinationsOfPhoneNumber {

    static final String[] KEYS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};

    static List<String> letterCombinations(String digits) {
        List<String> out = new ArrayList<>();
        if (digits.isEmpty()) return out;
        bt(digits, 0, new StringBuilder(), out);
        return out;
    }

    static void bt(String d, int i, StringBuilder sb, List<String> out) {
        if (i == d.length()) { out.add(sb.toString()); return; }
        for (char ch : KEYS[d.charAt(i) - '0'].toCharArray()) {
            sb.append(ch);
            bt(d, i + 1, sb, out);
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    static List<String> iterative(String d) {
        if (d.isEmpty()) return List.of();
        List<String> cur = new ArrayList<>(List.of(""));
        for (char digit : d.toCharArray()) {
            List<String> next = new ArrayList<>();
            for (String s : cur) for (char ch : KEYS[digit - '0'].toCharArray()) next.add(s + ch);
            cur = next;
        }
        return cur;
    }

    public static void main(String[] args) {
        check("23", "[ad, ae, af, bd, be, bf, cd, ce, cf]", letterCombinations("23"));
        check("empty", "[]", letterCombinations(""));
        check("7 (4 letters)", "[p, q, r, s]", letterCombinations("7"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rnd.nextInt(5); i++) sb.append((char) ('2' + rnd.nextInt(8)));
            ok &= letterCombinations(sb.toString()).equals(iterative(sb.toString()));
        }
        check("200 random inputs: backtracking == iterative", true, ok);
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
