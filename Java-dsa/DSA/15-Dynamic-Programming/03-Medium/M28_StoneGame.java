/*
 * ============================================================================
 *  MEDIUM 28 : STONE GAME                    LeetCode #877
 *  https://leetcode.com/problems/stone-game/
 *  Pattern: GAME DP on intervals - store the SCORE DIFFERENCE for the player to move
 * ============================================================================
 *
 *  PROBLEM
 *    Even number of piles in a row, odd total. Alice and Bob alternately take a whole
 *    pile from EITHER end; both play optimally. Does Alice (first) win?
 *      [5,3,4,5] -> true        [3,7,2,3] -> true
 *
 *  ---------------------------------------------------------------------------
 *  MINIMAX TRICK: don't track two scores. Track
 *      diff[i][j] = (my stones - opponent's stones) when piles i..j remain and it's
 *                   MY turn.
 *  If I take pile i, the opponent then faces i+1..j and gets diff[i+1][j] in THEIR
 *  favour, so for me:  a[i] - diff[i+1][j].   Same for the right end.
 *      diff[i][j] = max(a[i] - diff[i+1][j],  a[j] - diff[i][j-1])
 *      BASE diff[i][i] = a[i];      Alice wins iff diff[0][n-1] > 0
 *  This "my score minus yours" formulation solves most two-player optimal games
 *  (Predict the Winner #486 is the same code without the even/odd guarantee).
 *
 *  THE MATH ANSWER: return true. With an EVEN number of piles, Alice can take all
 *  even-indexed piles or all odd-indexed ones (whatever she picks, Bob is forced
 *  onto the other parity). One parity has the larger sum, and the total is odd, so
 *  there are no ties -> Alice always wins. The DP is still the point of this problem:
 *  it gives the exact margin and handles variants without the guarantee.
 *
 *  DRY RUN [5,3,4,5]
 *    len1: 5 3 4 5      len2: [5,3]=2 [3,4]=1 [4,5]=1
 *    len3: [5,3,4]=max(5-1, 4-2)=4   [3,4,5]=max(3-1, 5-1)=4
 *    len4: max(5-4, 5-4) = 1 > 0 -> true
 *
 *  COMPLEXITY   Time O(n^2)   Space O(n) (one row, filled by increasing i)
 * ============================================================================
 */

import java.util.*;

public class M28_StoneGame {

    static boolean stoneGame(int[] piles) { return margin(piles) > 0; }

    static int margin(int[] a) {
        int n = a.length;
        int[] diff = new int[n];                          // diff[j] for the current start i
        for (int i = n - 1; i >= 0; i--) {
            diff[i] = a[i];
            for (int j = i + 1; j < n; j++)
                diff[j] = Math.max(a[i] - diff[j], a[j] - diff[j - 1]);   // diff[j] = old row (i+1), diff[j-1] = this row
        }
        return diff[n - 1];
    }

    // referee: full minimax with two explicit scores
    static int bruteMargin(int[] a, int i, int j) {
        if (i > j) return 0;
        return Math.max(a[i] - bruteMargin(a, i + 1, j), a[j] - bruteMargin(a, i, j - 1));
    }

    public static void main(String[] args) {
        check("[5,3,4,5]", true, stoneGame(new int[]{5, 3, 4, 5}));
        check("[3,7,2,3]", true, stoneGame(new int[]{3, 7, 2, 3}));
        check("margin [5,3,4,5]", 1, margin(new int[]{5, 3, 4, 5}));
        Random rnd = new Random(1);
        boolean ok = true, alwaysWins = true;
        for (int t = 0; t < 500; t++) {
            int[] a = new int[1 + rnd.nextInt(14)];
            for (int i = 0; i < a.length; i++) a[i] = 1 + rnd.nextInt(20);
            ok &= margin(a) == bruteMargin(a, 0, a.length - 1);
            if (a.length % 2 == 0 && Arrays.stream(a).sum() % 2 == 1) alwaysWins &= stoneGame(a);
        }
        check("500 random: margin == minimax", true, ok);
        check("even piles + odd total -> Alice wins", true, alwaysWins);
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
