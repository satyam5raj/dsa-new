/*
 * ============================================================================
 *  MEDIUM 27 : MINIMUM COST TREE FROM LEAF VALUES   LeetCode #1130
 *  https://leetcode.com/problems/minimum-cost-tree-from-leaf-values/
 *  Pattern: INTERVAL DP (try every split)  ->  then a monotonic-stack greedy in O(n)
 * ============================================================================
 *
 *  PROBLEM
 *    arr = the leaves of a binary tree, in in-order. Every non-leaf node has exactly two
 *    children and its value = (max leaf in its left subtree) * (max leaf in its right).
 *    Minimum possible sum of the non-leaf nodes?
 *      [6,2,4] -> 32   ((6) (2 4)): 2*4=8 + 6*4=24          [4,11] -> 44
 *
 *  ---------------------------------------------------------------------------
 *  INTERVAL DP: the ROOT of leaves i..j splits them into i..k and k+1..j.
 *      STATE       dp[i][j] = min cost of a tree over leaves i..j
 *      TRANSITION  dp[i][j] = min over k in [i, j) of
 *                     dp[i][k] + dp[k+1][j] + max(i..k) * max(k+1..j)
 *      BASE        dp[i][i] = 0 (a single leaf has no internal node)
 *      ORDER       by increasing interval length
 *  Precompute mx[i][j] = max of arr[i..j] so each split costs O(1).  O(n^3)
 *
 *  O(n) GREEDY (the follow-up): each internal node "uses up" the smaller of two
 *  neighbours. The cheapest way to remove a leaf x is to pair it with its smaller
 *  neighbour (left or right) that is larger than it. A decreasing stack finds those:
 *      for x: while stack.top <= x: mid = pop; cost += mid * min(newTop, x)
 *  It's the same idea as Stack problems "next greater element".
 *
 *  DRY RUN DP [6,2,4]
 *    len 2: [6,2] = 12, [2,4] = 8
 *    len 3: k=0: 0 + 8 + 6*4 = 32      k=1: 12 + 0 + 6*4 = 36   -> 32
 *
 *  COMPLEXITY   DP: Time O(n^3) Space O(n^2)       Stack: Time O(n) Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M27_MinimumCostTreeFromLeafValues {

    static int mctFromLeafValues(int[] arr) {
        int n = arr.length;
        int[][] mx = new int[n][n], dp = new int[n][n];
        for (int i = 0; i < n; i++) {
            mx[i][i] = arr[i];
            for (int j = i + 1; j < n; j++) mx[i][j] = Math.max(mx[i][j - 1], arr[j]);
        }
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                dp[i][j] = Integer.MAX_VALUE;
                for (int k = i; k < j; k++)                             // root splits into i..k | k+1..j
                    dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k + 1][j] + mx[i][k] * mx[k + 1][j]);
            }
        return dp[0][n - 1];
    }

    static int mctStack(int[] arr) {
        Deque<Integer> st = new ArrayDeque<>();
        st.push(Integer.MAX_VALUE);                                     // sentinel
        int cost = 0;
        for (int x : arr) {
            while (st.peek() <= x) {
                int mid = st.pop();                                     // remove mid, paired with its smaller bigger-neighbour
                cost += mid * Math.min(st.peek(), x);
            }
            st.push(x);
        }
        while (st.size() > 2) cost += st.pop() * st.peek();             // remaining stack is decreasing
        return cost;
    }

    static int brute(int[] a, int i, int j) {                         // plain recursion over every root split
        if (i == j) return 0;
        int best = Integer.MAX_VALUE;
        for (int k = i; k < j; k++) {
            int l = 0, r = 0;
            for (int x = i; x <= k; x++) l = Math.max(l, a[x]);
            for (int x = k + 1; x <= j; x++) r = Math.max(r, a[x]);
            best = Math.min(best, brute(a, i, k) + brute(a, k + 1, j) + l * r);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[6,2,4]", 32, mctFromLeafValues(new int[]{6, 2, 4}));
        check("[4,11]", 44, mctFromLeafValues(new int[]{4, 11}));
        check("stack [6,2,4]", 32, mctStack(new int[]{6, 2, 4}));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            int[] a = new int[2 + rnd.nextInt(8)];
            for (int i = 0; i < a.length; i++) a[i] = 1 + rnd.nextInt(15);
            int b = brute(a, 0, a.length - 1);
            ok &= mctFromLeafValues(a) == b && mctStack(a) == b;
        }
        check("400 random: DP == stack == recursion", true, ok);
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
