/*
 * ============================================================================
 *  HARD 05 : MAXIMUM XOR WITH AN ELEMENT FROM ARRAY  ⭐ bonus   LeetCode #1707
 *  https://leetcode.com/problems/maximum-xor-with-an-element-from-array/
 *  Pattern: OFFLINE QUERIES (sort by limit) + BINARY TRIE built incrementally
 * ============================================================================
 *
 *  PROBLEM
 *    queries[i] = [x, m]: the max of (x XOR nums[j]) over nums[j] <= m, or -1 if
 *    no element is <= m.
 *      nums [0,1,2,3,4], queries [[3,1],[1,3],[5,6]] -> [3,3,7]
 *
 *  ---------------------------------------------------------------------------
 *  M07 (max XOR) uses a binary trie of ALL numbers. Here each query may only use
 *  numbers <= m - a different subset per query.
 *
 *  OFFLINE TRICK: we may answer queries in ANY order (we just write each answer
 *  into its original slot). So:
 *    sort nums ascending; sort query indices by m ascending
 *    for each query (smallest m first):
 *       insert every not-yet-inserted num <= m into the trie   (a pointer sweep)
 *       if the trie is empty -> -1, else greedy max-XOR walk (M07)
 *  Each number is inserted once; each query is O(31).
 *  "Sort the queries so the data structure only GROWS" is a powerful general
 *  idea (also used with Union-Find and Fenwick trees).
 *
 *  ONLINE ALTERNATIVE: store the MIN value in each trie node's subtree and refuse
 *  to enter subtrees whose min > m. Also O(31) per query, no sorting needed.
 *
 *  DRY RUN query [1,3] (x=1, m=3): trie {0,1,2,3}; best partner of 1 (001) is 2 (010) -> 3
 *
 *  COMPLEXITY   Time O(n log n + q log q + 31 (n + q))   Space O(31 n)
 * ============================================================================
 */

import java.util.*;

public class H05_MaximumXorWithAnElement {

    static class Node { Node[] child = new Node[2]; }

    static int[] maximizeXor(int[] nums, int[][] queries) {
        int[] sorted = nums.clone();
        Arrays.sort(sorted);
        Integer[] order = new Integer[queries.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> queries[i][1]));    // by limit m
        Node root = new Node();
        int[] ans = new int[queries.length];
        int next = 0;
        for (int qi : order) {
            int x = queries[qi][0], m = queries[qi][1];
            while (next < sorted.length && sorted[next] <= m) insert(root, sorted[next++]);   // grow the trie
            ans[qi] = next == 0 ? -1 : bestXor(root, x);
        }
        return ans;
    }

    static void insert(Node root, int x) {
        Node n = root;
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[bit] == null) n.child[bit] = new Node();
            n = n.child[bit];
        }
    }

    static int bestXor(Node root, int x) {
        Node n = root;
        int res = 0;
        for (int b = 30; b >= 0; b--) {
            int bit = (x >> b) & 1;
            if (n.child[1 - bit] != null) { res |= 1 << b; n = n.child[1 - bit]; }
            else n = n.child[bit];
        }
        return res;
    }

    static int[] brute(int[] nums, int[][] qs) {
        int[] out = new int[qs.length];
        for (int i = 0; i < qs.length; i++) {
            out[i] = -1;
            for (int v : nums) if (v <= qs[i][1]) out[i] = Math.max(out[i], v ^ qs[i][0]);
        }
        return out;
    }

    public static void main(String[] args) {
        check("example 1", "[3, 3, 7]", maximizeXor(new int[]{0, 1, 2, 3, 4}, new int[][]{{3, 1}, {1, 3}, {5, 6}}));
        check("example 2", "[15, -1, 5]", maximizeXor(new int[]{5, 2, 4, 6, 6, 3}, new int[][]{{12, 4}, {8, 1}, {6, 3}}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] nums = rnd.ints(1 + rnd.nextInt(20), 0, 1000).toArray();
            int[][] qs = new int[1 + rnd.nextInt(20)][];
            for (int i = 0; i < qs.length; i++) qs[i] = new int[]{rnd.nextInt(1000), rnd.nextInt(1000)};
            ok &= Arrays.equals(maximizeXor(nums, qs), brute(nums, qs));
        }
        check("500 random cases vs brute force", true, ok);
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
