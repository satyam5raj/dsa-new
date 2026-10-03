/*
 * ============================================================================
 *  HARD 02 : K-TH SMALLEST IN LEXICOGRAPHICAL ORDER  ⭐ bonus   LeetCode #440
 *  https://leetcode.com/problems/k-th-smallest-in-lexicographical-order/
 *  Pattern: walk a 10-ary "prefix tree" of numbers, COUNTING whole subtrees to skip them
 * ============================================================================
 *
 *  PROBLEM
 *    Among 1..n sorted as STRINGS (lexicographically), return the k-th.
 *      n=13: 1, 10, 11, 12, 13, 2, 3, 4, 5, 6, 7, 8, 9      k=2 -> 10
 *    n and k up to 10^9.
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: generate, sort as strings -> O(n log n) memory and time. n = 10^9: no.
 *
 *  THE TREE: numbers form a 10-ary tree by prefix: root -> 1..9; node x has children
 *  10x .. 10x+9. Lexicographic order = PREORDER of this tree (like a trie, 10-Trie).
 *     1 -> 10 -> 100 ... then 11 ...
 *  We need the k-th node of the preorder without visiting k nodes.
 *
 *  COUNT A SUBTREE: how many numbers in [1, n] start with prefix p?
 *    level by level: [p, p+1), then [10p, 10(p+1)), then [100p, 100(p+1)) ...
 *    count += min(n + 1, next) - cur   for cur = p, next = p + 1, multiplying both by 10
 *  O(log n) per count.
 *
 *  WALK: start at cur = 1, k -= 1 (we're standing on the 1st number)
 *    while k > 0:
 *      steps = count(cur)                 // size of cur's whole subtree
 *      if steps <= k: SKIP the subtree -> cur = cur + 1 (next sibling), k -= steps
 *      else:          GO DOWN           -> cur = cur * 10 (first child), k -= 1
 *  Each step either skips a whole subtree or descends a level -> O(log^2 n) total.
 *
 *  DRY RUN n=13, k=2: cur 1, k 1. count(1) = {1,10..13} = 5 > 1 -> go down: cur 10, k 0 -> 10
 *
 *  COMPLEXITY   Time O(log^2 n)   Space O(1)
 * ============================================================================
 */

import java.util.*;

public class H02_KthLexicographicalNumber {

    static int findKthNumber(int n, int k) {
        long cur = 1;
        k--;                                            // standing on "1" already
        while (k > 0) {
            long steps = countPrefix(n, cur);
            if (steps <= k) {                           // the answer is NOT in cur's subtree
                k -= steps;
                cur++;                                  // next sibling
            } else {
                k--;                                    // step onto the first child
                cur *= 10;
            }
        }
        return (int) cur;
    }

    // how many numbers in [1, n] have `prefix` as a prefix
    static long countPrefix(long n, long prefix) {
        long count = 0, cur = prefix, next = prefix + 1;
        while (cur <= n) {
            count += Math.min(n + 1, next) - cur;       // this level's numbers inside [1, n]
            cur *= 10;
            next *= 10;
        }
        return count;
    }

    static int brute(int n, int k) {
        String[] s = new String[n];
        for (int i = 1; i <= n; i++) s[i - 1] = String.valueOf(i);
        Arrays.sort(s);
        return Integer.parseInt(s[k - 1]);
    }

    public static void main(String[] args) {
        check("n=13 k=2", 10, findKthNumber(13, 2));
        check("n=1 k=1", 1, findKthNumber(1, 1));
        check("n=100 k=10", 17, findKthNumber(100, 10));
        check("n=10^9 k=10^9 (instant)", 999999999, findKthNumber(1_000_000_000, 1_000_000_000));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int n = 1 + rnd.nextInt(3000), k = 1 + rnd.nextInt(n);
            ok &= findKthNumber(n, k) == brute(n, k);
        }
        check("500 random (n, k) vs sorting strings", true, ok);
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
