/*
 * ============================================================================
 *  EASY 25 : ASSIGN COOKIES                  LeetCode #455
 *  https://leetcode.com/problems/assign-cookies/
 *  Pattern: GREEDY + SORT + TWO POINTERS
 * ============================================================================
 *
 *  PROBLEM
 *    g[i] = greed of child i (the minimum cookie size that satisfies them).
 *    s[j] = size of cookie j. Each child gets at most one cookie.
 *    Maximize the number of satisfied children.
 *      g = [1,2,3], s = [1,1]   -> 1
 *      g = [1,2],   s = [1,2,3] -> 2
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY IDEA: give each child the SMALLEST cookie that satisfies them. Big
 *  cookies are kept for greedier children, and nothing is wasted.
 *    sort both arrays
 *    child pointer i, cookie pointer j
 *      s[j] >= g[i] -> child i is satisfied: i++, j++
 *      else         -> cookie j is too small for this (and every greedier) child: j++
 *
 *  WHY IS GREEDY CORRECT? If the least greedy child can be satisfied, using the
 *  smallest sufficient cookie never hurts: any other choice uses a cookie that's
 *  at least as big, leaving the remaining children worse off. (An "exchange argument".)
 *
 *  DRY RUN g=[1,2,3], s=[1,1]
 *    child 1, cookie 1 -> fits  -> satisfied 1
 *    child 2, cookie 1 -> too small -> next cookie (none left) -> answer 1
 *
 *  COMPLEXITY   Time O(n log n + m log m) for sorting   Space O(1) extra (sort in-place)
 * ============================================================================
 */

import java.util.*;

public class E25_AssignCookies {

    static int findContentChildren(int[] g, int[] s) {
        Arrays.sort(g);
        Arrays.sort(s);
        int i = 0, j = 0;
        while (i < g.length && j < s.length) {
            if (s[j] >= g[i]) i++;       // child satisfied, move to the next child
            j++;                         // this cookie is used (or too small): move on either way
        }
        return i;                        // number of satisfied children
    }

    public static void main(String[] args) {
        check("g=[1,2,3] s=[1,1]", 1, findContentChildren(new int[]{1, 2, 3}, new int[]{1, 1}));
        check("g=[1,2] s=[1,2,3]", 2, findContentChildren(new int[]{1, 2}, new int[]{1, 2, 3}));
        check("g=[10,9,8,7] s=[5,6,7,8]", 2, findContentChildren(new int[]{10, 9, 8, 7}, new int[]{5, 6, 7, 8}));
        check("no cookies", 0, findContentChildren(new int[]{1}, new int[]{}));
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
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
