/*
 * ============================================================================
 *  THE 5 TRIE PATTERNS
 * ============================================================================
 *
 *  1. PREFIX LOOKUP / AUTOCOMPLETE
 *     Walk to the prefix's node, then DFS below it. Store extra data in nodes
 *     (counts, the top-3 suggestions, a sum) to answer queries without the DFS.
 *     -> Counting Words With Prefix (E01), Search Suggestions (M04), Map Sum (M06)
 *
 *  2. WALK THE TEXT, STOP AT THE FIRST WORD END ("shortest dictionary prefix")
 *     For each word of a sentence, walk the trie char by char; the first isEnd
 *     you hit is the shortest root. -> Replace Words (M03)
 *
 *  3. WILDCARD / BRANCHING SEARCH
 *     A '.' (any letter) means "try every child": DFS with branching.
 *     -> Add and Search Words (M02)
 *
 *  4. TRIE + DFS ON A GRID (prune by prefix)
 *     Search many words in a board at once: walk the board and the trie together;
 *     the moment the current path isn't a prefix of any word, stop.
 *     -> Word Search II (H01)
 *
 *  5. BINARY (BITWISE) TRIE for XOR
 *     Insert numbers bit by bit (from the highest bit). To maximize x XOR y, walk
 *     greedily toward the OPPOSITE bit at each level.
 *     -> Maximum XOR of Two Numbers (M07), Max XOR With an Element (H05)
 *
 *  BONUS: REVERSED TRIE for suffixes - insert words backwards and walk the stream
 *     from its newest character backwards. -> Stream of Characters (H02)
 *
 *  This file shows the two less obvious ones: wildcard search and the XOR trie.
 *  Run:  java TriePatterns.java
 * ============================================================================
 */

import java.util.*;

public class TriePatterns {

    // ---------------- Pattern 3: wildcard search ----------------
    static class Node {
        Node[] next = new Node[26];
        boolean end;
    }

    static void insert(Node root, String w) {
        Node n = root;
        for (char c : w.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.end = true;
    }

    // '.' matches any single letter
    static boolean matches(Node n, String pattern, int i) {
        if (n == null) return false;
        if (i == pattern.length()) return n.end;
        char c = pattern.charAt(i);
        if (c != '.') return matches(n.next[c - 'a'], pattern, i + 1);
        for (Node child : n.next) if (matches(child, pattern, i + 1)) return true;   // branch on '.'
        return false;
    }

    // ---------------- Pattern 5: binary trie for maximum XOR ----------------
    static final int BITS = 31;                       // non-negative ints: bits 30..0

    static class BitNode {
        BitNode[] child = new BitNode[2];
    }

    static void insertBits(BitNode root, int x) {
        BitNode n = root;
        for (int b = BITS - 1; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[bit] == null) n.child[bit] = new BitNode();
            n = n.child[bit];
        }
    }

    // the largest x XOR y over all y already inserted
    static int maxXorWith(BitNode root, int x) {
        BitNode n = root;
        int result = 0;
        for (int b = BITS - 1; b >= 0; b--) {
            int bit = (x >> b) & 1, want = 1 - bit;   // the opposite bit makes this XOR bit 1
            if (n.child[want] != null) {
                result |= 1 << b;                     // a high bit of 1 beats ALL lower bits combined
                n = n.child[want];
            } else {
                n = n.child[bit];
            }
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println("Pattern 3: wildcard search");
        Node root = new Node();
        for (String w : List.of("bad", "dad", "mad")) insert(root, w);
        check("pad", false, matches(root, "pad", 0));
        check(".ad", true, matches(root, ".ad", 0));
        check("b..", true, matches(root, "b..", 0));
        check("..", false, matches(root, "..", 0));

        System.out.println("Pattern 5: binary trie, max XOR");
        BitNode br = new BitNode();
        int[] nums = {3, 10, 5, 25, 2, 8};
        for (int x : nums) insertBits(br, x);
        int best = 0;
        for (int x : nums) best = Math.max(best, maxXorWith(br, x));
        check("max XOR of [3,10,5,25,2,8] (5 ^ 25)", 28, best);
        System.out.println("   why greedy works: 1xxxx (16+) always beats 0yyyy (at most 15)");

        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), 0, Integer.MAX_VALUE).toArray();
            BitNode r = new BitNode();
            for (int x : a) insertBits(r, x);
            int got = 0, exp = 0;
            for (int x : a) got = Math.max(got, maxXorWith(r, x));
            for (int x : a) for (int y : a) exp = Math.max(exp, x ^ y);
            ok &= got == exp;
        }
        check("300 random arrays vs all pairs", true, ok);
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
