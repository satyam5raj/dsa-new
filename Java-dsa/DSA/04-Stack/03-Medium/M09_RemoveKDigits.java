/*
 * ============================================================================
 *  MEDIUM 09 : REMOVE K DIGITS               LeetCode #402
 *  https://leetcode.com/problems/remove-k-digits/
 *  Pattern: GREEDY + MONOTONIC (increasing) STACK
 * ============================================================================
 *
 *  PROBLEM
 *    Remove exactly k digits from the number string to make it as SMALL as possible.
 *    No leading zeros in the answer ("0" if nothing is left).
 *      "1432219", k=3 -> "1219"
 *      "10200",   k=1 -> "200"      (remove 1 -> "0200" -> strip the zero)
 *      "10",      k=2 -> "0"
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY INSIGHT: the LEFTMOST digits matter most (the most significant).
 *  Scanning left to right, if a digit is BIGGER than the digit after it,
 *  removing it makes the number smaller ("14..." -> "1..." with the 4 replaced
 *  by the smaller next digit). So we want the kept digits to be as INCREASING as possible.
 *
 *  MONOTONIC INCREASING STACK
 *    for each digit d:
 *      while k > 0 and the stack top > d:  pop (remove that bigger digit), k--
 *      push d
 *    if k is still > 0: remove from the END (the stack is increasing, so the biggest are at the end)
 *    strip leading zeros; empty -> "0"
 *
 *  DRY RUN "1432219", k=3
 *    1 -> [1]
 *    4 -> [1,4]
 *    3 -> 4>3 pop (k=2) -> [1,3]
 *    2 -> 3>2 pop (k=1) -> [1,2]
 *    2 -> [1,2,2]
 *    1 -> 2>1 pop (k=0) -> [1,2,1]
 *    9 -> [1,2,1,9]   -> "1219"
 *
 *  COMPLEXITY   Time O(n)   Space O(n) (a StringBuilder used as the stack)
 * ============================================================================
 */

import java.util.*;

public class M09_RemoveKDigits {

    static String removeKdigits(String num, int k) {
        StringBuilder stack = new StringBuilder();
        for (char d : num.toCharArray()) {
            while (k > 0 && stack.length() > 0 && stack.charAt(stack.length() - 1) > d) {
                stack.deleteCharAt(stack.length() - 1);
                k--;
            }
            stack.append(d);
        }
        stack.setLength(Math.max(0, stack.length() - k));      // still need removals: cut from the end
        int i = 0;
        while (i < stack.length() - 1 && stack.charAt(i) == '0') i++;   // leading zeros (keep at least one digit)
        String result = stack.substring(i);
        return result.isEmpty() ? "0" : result;
    }

    // brute force: try every subset of positions to remove (small inputs only)
    static String brute(String num, int k) {
        int n = num.length();
        java.math.BigInteger best = null;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) != k) continue;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) if ((mask & (1 << i)) == 0) sb.append(num.charAt(i));
            java.math.BigInteger v = sb.length() == 0 ? java.math.BigInteger.ZERO : new java.math.BigInteger(sb.toString());
            if (best == null || v.compareTo(best) < 0) best = v;
        }
        return best.toString();
    }

    public static void main(String[] args) {
        check("1432219 k=3", "1219", removeKdigits("1432219", 3));
        check("10200 k=1 (leading zeros)", "200", removeKdigits("10200", 1));
        check("10 k=2 (remove all)", "0", removeKdigits("10", 2));
        check("12345 k=2 (increasing: cut the end)", "123", removeKdigits("12345", 2));
        Random rnd = new Random(9);
        boolean ok = true;
        for (int t = 0; t < 400; t++) {
            int n = 1 + rnd.nextInt(10);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append((char) ('0' + rnd.nextInt(10)));
            int k = rnd.nextInt(n + 1);
            ok &= removeKdigits(sb.toString(), k).equals(brute(sb.toString(), k));
        }
        check("400 random cases vs exhaustive search", true, ok);
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
