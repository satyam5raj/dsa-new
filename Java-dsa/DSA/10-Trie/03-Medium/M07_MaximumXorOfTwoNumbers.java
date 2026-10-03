/*
 * ============================================================================
 *  MEDIUM 07 : MAXIMUM XOR OF TWO NUMBERS IN AN ARRAY  ⭐ bonus   LeetCode #421
 *  https://leetcode.com/problems/maximum-xor-of-two-numbers-in-an-array/
 *  Pattern: BINARY TRIE, greedily take the OPPOSITE bit from the top
 * ============================================================================
 *
 *  PROBLEM
 *    max(nums[i] XOR nums[j]).
 *      [3,10,5,25,2,8] -> 28   (5 XOR 25 = 00101 ^ 11001 = 11100)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: all pairs, O(n^2). n = 2 * 10^5 -> 2 * 10^10. Too slow.
 *
 *  KEY FACT: in binary, a 1 in bit b is worth MORE than all lower bits together
 *  (2^b > 2^b - 1). So maximize the XOR greedily from the HIGHEST bit down:
 *  at each bit, try to make the XOR bit 1, i.e. choose a partner whose bit is the
 *  OPPOSITE of ours - if any number with the prefix chosen so far has it.
 *
 *  BINARY TRIE: each number is a 31-level path of bits (high to low), children
 *  [0] and [1]. For a number x, walk down choosing child[1 - bit] when it exists
 *  (that XOR bit becomes 1), else child[bit]. That finds x's best partner in O(31).
 *    insert every number, then query every number -> O(31 n).
 *
 *  DRY RUN x = 5 (00101) with 25 (11001) in the trie, top 5 bits shown:
 *    bit4: x=0, want 1 -> 25 has 1 -> XOR bit 1
 *    bit3: x=0, want 1 -> 1 -> XOR 1;  bit2: x=1, want 0 -> 0 -> 1
 *    bit1: x=0, want 1 -> only 0 -> XOR 0;  bit0: x=1, want 0 -> only 1 -> 0
 *    -> 11100 = 28
 *
 *  (There's also a bit-by-bit HashSet-of-prefixes solution; the trie is clearer.)
 *  COMPLEXITY   Time O(31 n)   Space O(31 n)
 * ============================================================================
 */

import java.util.*;

public class M07_MaximumXorOfTwoNumbers {

    static class Node { Node[] child = new Node[2]; }

    static int findMaximumXOR(int[] nums) {
        Node root = new Node();
        for (int x : nums) {
            Node n = root;
            for (int b = 30; b >= 0; b--) {
                int bit = (x >> b) & 1;
                if (n.child[bit] == null) n.child[bit] = new Node();
                n = n.child[bit];
            }
        }
        int best = 0;
        for (int x : nums) {
            Node n = root;
            int xor = 0;
            for (int b = 30; b >= 0; b--) {
                int bit = (x >> b) & 1;
                if (n.child[1 - bit] != null) { xor |= 1 << b; n = n.child[1 - bit]; }   // opposite bit: XOR bit = 1
                else n = n.child[bit];
            }
            best = Math.max(best, xor);
        }
        return best;
    }

    static int brute(int[] a) {
        int best = 0;
        for (int x : a) for (int y : a) best = Math.max(best, x ^ y);
        return best;
    }

    public static void main(String[] args) {
        check("[3,10,5,25,2,8]", 28, findMaximumXOR(new int[]{3, 10, 5, 25, 2, 8}));
        check("[14,70,53,83,49,91,36,80,92,51,66,70]", 127, findMaximumXOR(new int[]{14, 70, 53, 83, 49, 91, 36, 80, 92, 51, 66, 70}));
        check("[0]", 0, findMaximumXOR(new int[]{0}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(40), 0, Integer.MAX_VALUE).toArray();
            ok &= findMaximumXOR(a) == brute(a);
        }
        check("500 random arrays vs all pairs", true, ok);
        int[] big = rnd.ints(200_000, 0, Integer.MAX_VALUE).toArray();
        long t0 = System.nanoTime();
        int r = findMaximumXOR(big);
        System.out.printf("   n = 200,000 in %d ms (all pairs would be 4 * 10^10)%n", (System.nanoTime() - t0) / 1_000_000);
        check("big input answer is large", true, r > (1 << 30));
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
