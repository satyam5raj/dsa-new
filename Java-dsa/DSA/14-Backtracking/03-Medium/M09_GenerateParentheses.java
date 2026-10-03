/*
 * ============================================================================
 *  MEDIUM 09 : GENERATE PARENTHESES          LeetCode #22
 *  https://leetcode.com/problems/generate-parentheses/
 *  Pattern: build character by character, only making choices that KEEP IT VALID
 * ============================================================================
 *
 *  PROBLEM
 *    All well-formed strings with n pairs of parentheses.
 *      n=3 -> ["((()))","(()())","(())()","()(())","()()()"]
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: all 2^(2n) strings of '(' and ')', keep the valid ones. n=8: 65,536
 *  strings for 1,430 answers.
 *
 *  BACKTRACKING WITH CONSTRAINTS: track open = '(' used, close = ')' used.
 *    add '(' if open < n                (still have some to place)
 *    add ')' if close < open            (only close something that's open)
 *    length 2n -> record
 *  Every partial string we build can still be completed, so EVERY leaf is an answer:
 *  no wasted branches at all. The number of answers is the n-th CATALAN number
 *  (1, 1, 2, 5, 14, 42, 132, 429, 1430...).
 *
 *  DRY RUN n=2: "(" -> "((" -> "(()" -> "(())" ;  "(" -> "()" -> "()(" -> "()()"
 *
 *  COMPLEXITY   Time O(4^n / sqrt(n)) (Catalan growth)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M09_GenerateParentheses {

    static List<String> generateParenthesis(int n) {
        List<String> out = new ArrayList<>();
        bt(n, 0, 0, new StringBuilder(), out);
        return out;
    }

    static void bt(int n, int open, int close, StringBuilder sb, List<String> out) {
        if (sb.length() == 2 * n) { out.add(sb.toString()); return; }
        if (open < n) {
            sb.append('(');
            bt(n, open + 1, close, sb, out);
            sb.deleteCharAt(sb.length() - 1);
        }
        if (close < open) {
            sb.append(')');
            bt(n, open, close + 1, sb, out);
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    static List<String> brute(int n) {
        List<String> out = new ArrayList<>();
        for (int mask = 0; mask < (1 << 2 * n); mask++) {
            StringBuilder sb = new StringBuilder();
            int bal = 0;
            boolean ok = true;
            for (int i = 2 * n - 1; i >= 0; i--) {
                boolean openP = (mask >> i & 1) == 0;
                sb.append(openP ? '(' : ')');
                bal += openP ? 1 : -1;
                if (bal < 0) { ok = false; break; }
            }
            if (ok && bal == 0) out.add(sb.toString());
        }
        return out;
    }

    public static void main(String[] args) {
        check("n=3", "[((())), (()()), (())(), ()(()), ()()()]", generateParenthesis(3));
        check("n=1", "[()]", generateParenthesis(1));
        int[] catalan = {1, 1, 2, 5, 14, 42, 132, 429, 1430, 4862};
        boolean ok = true;
        for (int n = 1; n <= 9; n++) {
            List<String> g = generateParenthesis(n);
            ok &= g.size() == catalan[n] && g.equals(brute(n));     // both come out in the same sorted order
        }
        check("n = 1..9: Catalan counts, == filtered brute force", true, ok);
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
