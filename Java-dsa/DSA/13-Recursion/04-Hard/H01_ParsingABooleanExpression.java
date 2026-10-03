/*
 * ============================================================================
 *  HARD 01 : PARSING A BOOLEAN EXPRESSION  ⭐ bonus   LeetCode #1106
 *  https://leetcode.com/problems/parsing-a-boolean-expression/
 *  Pattern: RECURSIVE DESCENT PARSING (the grammar is recursive, so is the parser)
 * ============================================================================
 *
 *  PROBLEM
 *    Evaluate an expression built from:
 *      't', 'f'                  true / false
 *      '!(e)'                    NOT
 *      '&(e1,e2,...)'            AND of one or more
 *      '|(e1,e2,...)'            OR of one or more
 *      "&(|(f))" -> false     "|(f,f,f,t)" -> true     "!(&(f,t))" -> true
 *
 *  ---------------------------------------------------------------------------
 *  THE GRAMMAR IS RECURSIVE: an expression contains expressions. So write ONE
 *  function parse() that reads exactly one expression starting at a shared
 *  position `pos`, and returns its value:
 *     c = s[pos++]
 *     't' -> true;  'f' -> false
 *     '!' -> skip '(', v = parse(), skip ')', return !v
 *     '&' / '|' -> skip '(', then repeatedly: v = parse(); combine; if next is ','
 *                  skip it and continue; at ')' skip it and stop
 *  Each call consumes exactly its own sub-expression, so `pos` ends up right after
 *  it - the caller continues from there. This is how compilers and JSON parsers work
 *  (recursive descent). The Stack topic (04) solved Basic Calculator with explicit
 *  stacks; recursion uses the call stack instead.
 *
 *  NOTE: keep evaluating all children even after the answer is known (to consume
 *  the input). Short-circuiting needs a "skip this sub-expression" routine.
 *
 *  COMPLEXITY   Time O(n)   Space O(nesting depth)
 * ============================================================================
 */

import java.util.*;

public class H01_ParsingABooleanExpression {

    static int pos;

    static boolean parseBoolExpr(String expression) {
        pos = 0;
        return parse(expression);
    }

    static boolean parse(String s) {
        char c = s.charAt(pos++);
        if (c == 't') return true;
        if (c == 'f') return false;
        pos++;                                         // skip '('
        if (c == '!') {
            boolean v = parse(s);
            pos++;                                     // skip ')'
            return !v;
        }
        boolean result = c == '&';                     // identity: AND starts true, OR starts false
        while (true) {
            boolean v = parse(s);                      // one child expression
            result = c == '&' ? result & v : result | v;
            if (s.charAt(pos++) == ')') return result; // consumed ',' or ')'
        }
    }

    // referee: repeatedly evaluate the innermost operator with string rewriting
    static boolean rewrite(String e) {
        while (e.length() > 1) {
            int close = e.indexOf(')'), open = e.lastIndexOf('(', close);
            char op = e.charAt(open - 1);
            String[] args = e.substring(open + 1, close).split(",");
            boolean v;
            if (op == '!') v = !args[0].equals("t");
            else if (op == '&') { v = true; for (String a : args) v &= a.equals("t"); }
            else { v = false; for (String a : args) v |= a.equals("t"); }
            e = e.substring(0, open - 1) + (v ? "t" : "f") + e.substring(close + 1);
        }
        return e.equals("t");
    }

    static String randomExpr(Random r, int depth) {
        if (depth == 0 || r.nextInt(3) == 0) return r.nextBoolean() ? "t" : "f";
        int kind = r.nextInt(3);
        if (kind == 0) return "!(" + randomExpr(r, depth - 1) + ")";
        StringBuilder sb = new StringBuilder(kind == 1 ? "&(" : "|(");
        for (int i = 0, n = 1 + r.nextInt(3); i < n; i++) sb.append(i > 0 ? "," : "").append(randomExpr(r, depth - 1));
        return sb.append(")").toString();
    }

    public static void main(String[] args) {
        check("&(|(f))", false, parseBoolExpr("&(|(f))"));
        check("|(f,f,f,t)", true, parseBoolExpr("|(f,f,f,t)"));
        check("!(&(f,t))", true, parseBoolExpr("!(&(f,t))"));
        check("t", true, parseBoolExpr("t"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            String e = randomExpr(rnd, 5);
            ok &= parseBoolExpr(e) == rewrite(e);
        }
        check("2,000 random expressions vs rewriting", true, ok);
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
