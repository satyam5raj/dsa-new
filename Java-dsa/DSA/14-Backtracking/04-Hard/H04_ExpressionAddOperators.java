/*
 * ============================================================================
 *  HARD 04 : EXPRESSION ADD OPERATORS        LeetCode #282
 *  https://leetcode.com/problems/expression-add-operators/
 *  Pattern: backtracking over operand splits + operators, evaluating ON THE FLY
 * ============================================================================
 *
 *  PROBLEM
 *    Insert '+', '-', '*' between the digits of num (or nothing, to join digits into
 *    one number) so the expression equals target. Numbers can't have leading zeros.
 *      "123", 6   -> ["1*2*3","1+2+3"]
 *      "232", 8   -> ["2*3+2","2+3*2"]
 *      "105", 5   -> ["1*0+5","10-5"]
 *      "3456237490", 9191 -> []
 *
 *  ---------------------------------------------------------------------------
 *  CHOICES: at each position pick the next operand num[pos..i] (any length), then an
 *  operator before it. Building strings and evaluating at the end is slow and tricky.
 *
 *  EVALUATE AS YOU GO - the '*' precedence trick: carry
 *      value = the expression's value so far
 *      last  = the value of the LAST TERM added (with its sign)
 *    '+' x : value + x,                     last = x
 *    '-' x : value - x,                     last = -x
 *    '*' x : value - last + last * x,       last = last * x
 *  For '*', undo the last term and add it back multiplied: e.g. 2+3 then *2 ->
 *  value 5 - 3 + 3*2 = 8 = 2+3*2. Correct precedence without a parser.
 *
 *  RULES
 *    - an operand longer than one digit can't start with '0'  -> break the loop
 *    - use long: operands up to 10 digits overflow int
 *    - the first operand has no operator in front
 *
 *  COMPLEXITY   Time O(4^n * n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H04_ExpressionAddOperators {

    static List<String> addOperators(String num, int target) {
        List<String> out = new ArrayList<>();
        bt(num, target, 0, 0, 0, new StringBuilder(), out);
        return out;
    }

    static void bt(String num, int target, int pos, long value, long last, StringBuilder expr, List<String> out) {
        if (pos == num.length()) {
            if (value == target) out.add(expr.toString());
            return;
        }
        int len = expr.length();
        for (int i = pos; i < num.length(); i++) {
            if (i > pos && num.charAt(pos) == '0') break;               // no leading zeros
            String operand = num.substring(pos, i + 1);
            long x = Long.parseLong(operand);
            if (pos == 0) {
                expr.append(operand);
                bt(num, target, i + 1, x, x, expr, out);                  // first operand: no operator
                expr.setLength(len);
                continue;
            }
            expr.append('+').append(operand);
            bt(num, target, i + 1, value + x, x, expr, out);
            expr.setLength(len);
            expr.append('-').append(operand);
            bt(num, target, i + 1, value - x, -x, expr, out);
            expr.setLength(len);
            expr.append('*').append(operand);
            bt(num, target, i + 1, value - last + last * x, last * x, expr, out);   // undo last term, multiply it
            expr.setLength(len);
        }
    }

    // referee: every way to fill each gap with "", "+", "-", "*", evaluated by a tiny parser
    static Set<String> brute(String num, int target) {
        Set<String> out = new HashSet<>();
        int gaps = num.length() - 1;
        String[] ops = {"", "+", "-", "*"};
        for (int code = 0; code < Math.pow(4, gaps); code++) {
            StringBuilder sb = new StringBuilder().append(num.charAt(0));
            int c = code;
            for (int g = 0; g < gaps; g++) { sb.append(ops[c % 4]).append(num.charAt(g + 1)); c /= 4; }
            String e = sb.toString();
            Long v = eval(e);
            if (v != null && v == target) out.add(e);
        }
        return out;
    }

    static Long eval(String e) {                       // null if an operand has a leading zero
        long total = 0, term = 1;
        char sign = '+';
        int i = 0;
        while (i < e.length()) {
            int j = i;
            while (j < e.length() && Character.isDigit(e.charAt(j))) j++;
            String operand = e.substring(i, j);
            if (operand.length() > 1 && operand.charAt(0) == '0') return null;
            term *= Long.parseLong(operand);
            if (j == e.length() || e.charAt(j) != '*') { total += sign == '+' ? term : -term; term = 1; if (j < e.length()) sign = e.charAt(j); }
            i = j + 1;
        }
        return total;
    }

    static List<String> sorted(List<String> l) { List<String> c = new ArrayList<>(l); Collections.sort(c); return c; }

    public static void main(String[] args) {
        check("123 -> 6", "[1*2*3, 1+2+3]", sorted(addOperators("123", 6)));
        check("232 -> 8", "[2*3+2, 2+3*2]", sorted(addOperators("232", 8)));
        check("105 -> 5", "[1*0+5, 10-5]", sorted(addOperators("105", 5)));
        check("3456237490 -> 9191", "[]", addOperators("3456237490", 9191));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(6); i++) sb.append(rnd.nextInt(4) == 0 ? '0' : (char) ('1' + rnd.nextInt(9)));
            int target = rnd.nextInt(41) - 20;
            ok &= new HashSet<>(addOperators(sb.toString(), target)).equals(brute(sb.toString(), target));
        }
        check("300 random cases vs all 4^gaps expressions", true, ok);
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
