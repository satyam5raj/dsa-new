/*
 * ============================================================================
 *  MEDIUM 05 : DECODE STRING                 LeetCode #394
 *  https://leetcode.com/problems/decode-string/
 *  Pattern: NESTED STRUCTURE - save the context on '[' and restore it on ']'
 * ============================================================================
 *
 *  PROBLEM
 *    k[encoded] means "repeat 'encoded' k times". Brackets can NEST.
 *      "3[a]2[bc]"    -> "aaabcbc"
 *      "3[a2[c]]"     -> "accaccacc"
 *      "2[abc]3[cd]ef" -> "abcabccdcdcdef"
 *
 *  ---------------------------------------------------------------------------
 *  WHY A STACK: when we hit an inner '[', we must PAUSE the outer string we were
 *  building, decode the inner part completely, then RESUME the outer one.
 *  Pause/resume of nested work = a stack (exactly what recursion does with the call stack).
 *
 *  STATE WHILE SCANNING
 *    cur = the string being built at the current nesting level
 *    k   = the number being read (it can have several digits: "12[a]")
 *  ON EACH CHARACTER
 *    digit  -> k = k*10 + digit
 *    '['    -> push (cur, k) onto the stacks; start fresh: cur = "", k = 0
 *    ']'    -> pop (prevString, times); cur = prevString + cur repeated 'times'
 *    letter -> cur += letter
 *
 *  DRY RUN "3[a2[c]]"
 *    '3' k=3
 *    '[' push ("",3), cur=""
 *    'a' cur="a"
 *    '2' k=2
 *    '[' push ("a",2), cur=""
 *    'c' cur="c"
 *    ']' pop ("a",2): cur = "a" + "cc" = "acc"
 *    ']' pop ("",3):  cur = ""  + "acc"*3 = "accaccacc"
 *
 *  ALTERNATIVE: recursion (the call stack does the pushing for you), implemented below.
 *  COMPLEXITY   Time O(output length)   Space O(output length)
 * ============================================================================
 */

import java.util.*;

public class M05_DecodeString {

    static String decodeString(String s) {
        Deque<Integer> counts = new ArrayDeque<>();
        Deque<StringBuilder> outer = new ArrayDeque<>();
        StringBuilder cur = new StringBuilder();
        int k = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                k = k * 10 + (c - '0');
            } else if (c == '[') {
                counts.push(k);
                outer.push(cur);                 // pause the outer level
                cur = new StringBuilder();
                k = 0;
            } else if (c == ']') {
                String inner = cur.toString();
                cur = outer.pop().append(inner.repeat(counts.pop()));   // resume + combine
            } else {
                cur.append(c);
            }
        }
        return cur.toString();
    }

    // Recursive version: decode until a ']' (or the end), return what was built
    static int pos;

    static String decodeRecursive(String s) {
        pos = 0;
        return parse(s);
    }

    static String parse(String s) {
        StringBuilder sb = new StringBuilder();
        while (pos < s.length() && s.charAt(pos) != ']') {
            char c = s.charAt(pos);
            if (Character.isDigit(c)) {
                int k = 0;
                while (Character.isDigit(s.charAt(pos))) k = k * 10 + (s.charAt(pos++) - '0');
                pos++;                                   // skip '['
                String inner = parse(s);                 // decode the nested part
                pos++;                                   // skip ']'
                sb.append(inner.repeat(k));
            } else {
                sb.append(c);
                pos++;
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        check("3[a]2[bc]", "aaabcbc", decodeString("3[a]2[bc]"));
        check("3[a2[c]]", "accaccacc", decodeString("3[a2[c]]"));
        check("2[abc]3[cd]ef", "abcabccdcdcdef", decodeString("2[abc]3[cd]ef"));
        check("10[a] (multi-digit)", "aaaaaaaaaa", decodeString("10[a]"));
        check("recursive: 3[a2[c]]", "accaccacc", decodeRecursive("3[a2[c]]"));
        check("recursive: 2[abc]3[cd]ef", "abcabccdcdcdef", decodeRecursive("2[abc]3[cd]ef"));
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
