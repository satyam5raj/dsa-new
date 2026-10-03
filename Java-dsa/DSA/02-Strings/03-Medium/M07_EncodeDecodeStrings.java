/*
 * ============================================================================
 *  MEDIUM 07 : ENCODE AND DECODE STRINGS     LeetCode #271 (Premium)
 *  https://leetcode.com/problems/encode-and-decode-strings/
 *  Pattern: LENGTH-PREFIX ENCODING (how real network protocols frame messages)
 * ============================================================================
 *
 *  PROBLEM
 *    Design encode(List<String>) -> String and decode(String) -> List<String>
 *    so that decode(encode(x)) == x for ANY list, including strings that contain
 *    any character (commas, '#', digits, empty strings...).
 *
 *  ---------------------------------------------------------------------------
 *  WHY "JOIN WITH A COMMA" FAILS: ["a,b", "c"] and ["a", "b", "c"] would encode
 *  to the same "a,b,c". ANY delimiter character can appear inside a string.
 *
 *  FIX 1: escaping (like CSV quotes). Works, but it's fiddly.
 *  FIX 2: LENGTH PREFIX (simple and robust)
 *    encode each string as   <length>#<string>
 *      ["hello", "", "a#b"]  ->  "5#hello0#3#a#b"
 *    decode: read digits up to the '#' -> that's the length L. Then take EXACTLY
 *    the next L characters as the string, no matter what they contain. Repeat.
 *    The '#' only ends the NUMBER. Inside the string it's never interpreted.
 *
 *  DRY RUN decode "5#hello0#3#a#b"
 *    i=0: read "5" until '#' -> take 5 chars "hello" -> i=7
 *    i=7: read "0" -> take 0 chars ""       -> i=9
 *    i=9: read "3" -> take "a#b"            -> done
 *
 *  REAL WORLD: HTTP Content-Length, gRPC/Protobuf length-delimited messages, Redis RESP.
 *  COMPLEXITY   Time O(total length)   Space O(total length)
 * ============================================================================
 */

import java.util.*;

public class M07_EncodeDecodeStrings {

    static String encode(List<String> strs) {
        StringBuilder sb = new StringBuilder();
        for (String s : strs) sb.append(s.length()).append('#').append(s);
        return sb.toString();
    }

    static List<String> decode(String encoded) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < encoded.length()) {
            int hash = encoded.indexOf('#', i);                // end of the length field
            int len = Integer.parseInt(encoded.substring(i, hash));
            int start = hash + 1;
            out.add(encoded.substring(start, start + len));    // take exactly len chars
            i = start + len;
        }
        return out;
    }

    public static void main(String[] args) {
        List<String> a = List.of("hello", "world");
        check("encode [hello, world]", "5#hello5#world", encode(a));
        check("round trip", a, decode(encode(a)));
        List<String> tricky = List.of("a#b", "", "12#", "#", "with,comma", "1");
        check("tricky strings survive", tricky, decode(encode(tricky)));
        check("empty list", List.of(), decode(encode(List.of())));
        check("list with one empty string", List.of(""), decode(encode(List.of(""))));
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
