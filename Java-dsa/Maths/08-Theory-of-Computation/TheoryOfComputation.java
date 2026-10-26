/*
 * ============================================================================
 *  MATHS 08 : THEORY OF COMPUTATION  (what can be computed, and how fast)
 * ============================================================================
 *  Run:  java TheoryOfComputation.java          (explanations: README.md in this folder)
 *
 *    1. DFA              a 3-state machine that checks "binary number divisible by 3"
 *    2. NFA              guessing machines, simulated with a set of states
 *    3. regex            regular expressions = finite automata
 *    4. beyond regular   a^n b^n and balanced brackets need a stack (pushdown automaton)
 *    5. grammars         a recursive-descent parser for arithmetic (context-free grammar)
 *    6. Turing machine   a tape + head + rules that adds 1 in binary
 *    7. halting          Collatz: a 3-line program nobody can prove always stops
 *    8. P vs NP          SAT: checking is fast, finding is (as far as we know) slow
 *    9. hard problems    subset sum (pseudo-polynomial DP), TSP brute force vs Held-Karp
 * ============================================================================
 */

import java.util.*;
import java.util.regex.Pattern;

public class TheoryOfComputation {

    // =====================================================================
    // 1. DFA: binary strings whose value is divisible by 3
    // =====================================================================

    /** State = value mod 3. Reading bit b turns remainder r into (2r + b) mod 3. */
    static boolean divisibleBy3(String bits) {
        int[][] delta = {{0, 1}, {2, 0}, {1, 2}};                                // delta[state][bit]
        int state = 0;
        for (char c : bits.toCharArray()) state = delta[state][c - '0'];
        return state == 0;                                                       // accept in state 0
    }

    static void dfa() {
        System.out.println("1. DFA (deterministic finite automaton)");
        check("110 (6) accepted", true, divisibleBy3("110"));
        check("111 (7) rejected", false, divisibleBy3("111"));
        boolean all = true;
        for (int len = 1; len <= 14; len++)
            for (int v = 0; v < (1 << len); v++) {
                String s = String.format("%" + len + "s", Integer.toBinaryString(v)).replace(' ', '0');
                all &= divisibleBy3(s) == (v % 3 == 0);
            }
        check("agrees with v % 3 for all 32,766 strings up to length 14", true, all);
    }

    // =====================================================================
    // 2. NFA: strings over {a,b} ending in "abb"   ( (a|b)*abb )
    // =====================================================================

    static boolean nfaEndsInAbb(String s) {
        // states 0 -a-> 1 -b-> 2 -b-> 3 (accept); state 0 loops on a and b ("guess when the suffix starts")
        Set<Integer> current = new HashSet<>(Set.of(0));
        for (char c : s.toCharArray()) {
            Set<Integer> next = new HashSet<>();
            for (int q : current) {
                if (q == 0) { next.add(0); if (c == 'a') next.add(1); }
                if (q == 1 && c == 'b') next.add(2);
                if (q == 2 && c == 'b') next.add(3);
            }
            current = next;                                                      // all the places the NFA could be
        }
        return current.contains(3);
    }

    static void nfaAndRegex() {
        System.out.println("2. NFA  3. Regular expressions");
        check("NFA accepts 'babb'", true, nfaEndsInAbb("babb"));
        check("NFA rejects 'abba'", false, nfaEndsInAbb("abba"));
        Pattern regex = Pattern.compile("(a|b)*abb");
        boolean same = true;
        int tested = 0;
        for (int len = 0; len <= 12; len++)
            for (int v = 0; v < (1 << len); v++) {
                StringBuilder sb = new StringBuilder();
                for (int i = len - 1; i >= 0; i--) sb.append((v >> i & 1) == 1 ? 'b' : 'a');
                same &= nfaEndsInAbb(sb.toString()) == regex.matcher(sb).matches();
                tested++;
            }
        check("NFA == Java regex (a|b)*abb on all strings up to length 12", true, same);
        System.out.printf("      (%,d strings tested)%n", tested);
        check("email-ish regex", true, Pattern.matches("[a-z]+@[a-z]+\\.(com|org)", "satyam@example.com"));
    }

    // =====================================================================
    // 4. beyond regular languages: a stack
    // =====================================================================

    static boolean balanced(String s) {                                         // a pushdown automaton
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if ("([{".indexOf(c) >= 0) stack.push(c);
            else {
                if (stack.isEmpty()) return false;
                char open = stack.pop();
                if ("([{".indexOf(open) != ")]}".indexOf(c)) return false;
            }
        }
        return stack.isEmpty();
    }

    static boolean anbn(String s) {                                             // a^n b^n needs an unbounded counter
        int count = 0, i = 0;
        while (i < s.length() && s.charAt(i) == 'a') { count++; i++; }
        while (i < s.length() && s.charAt(i) == 'b') { count--; i++; }
        return i == s.length() && count == 0;
    }

    static void beyondRegular() {
        System.out.println("4. Beyond regular: a^n b^n, balanced brackets");
        check("aaabbb in a^n b^n", true, anbn("aaabbb"));
        check("aaabb not in a^n b^n", false, anbn("aaabb"));
        check("balanced: ([]{()})", true, balanced("([]{()})"));
        check("unbalanced: ([)]", false, balanced("([)]"));
        // a DFA with k states can't count beyond k: give it 5 states (count capped at 4)
        int k = 5;
        String s = "a".repeat(k) + "b".repeat(k - 1);                            // NOT in the language
        int capped = 0, i = 0;
        while (i < s.length() && s.charAt(i) == 'a') { capped = Math.min(k - 1, capped + 1); i++; }
        while (i < s.length() && s.charAt(i) == 'b') { capped--; i++; }
        check("a 5-state 'counter' wrongly accepts aaaaabbbb", true, capped == 0);
    }

    // =====================================================================
    // 5. a context-free grammar and a recursive-descent parser
    // =====================================================================

    /*  expr   -> term (('+' | '-') term)*
     *  term   -> factor (('*' | '/') factor)*
     *  factor -> NUMBER | '(' expr ')'
     *  One method per rule; precedence comes from the grammar's nesting. */
    static class Parser {
        private final String s;
        private int pos = 0;
        Parser(String s) { this.s = s.replace(" ", ""); }
        double parse() { double v = expr(); if (pos != s.length()) throw new IllegalArgumentException("junk at " + pos); return v; }
        private double expr() { double v = term(); while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) { char op = s.charAt(pos++); double r = term(); v = op == '+' ? v + r : v - r; } return v; }
        private double term() { double v = factor(); while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) { char op = s.charAt(pos++); double r = factor(); v = op == '*' ? v * r : v / r; } return v; }
        private double factor() {
            if (s.charAt(pos) == '(') { pos++; double v = expr(); pos++; return v; }   // skip ')'
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            return Double.parseDouble(s.substring(start, pos));
        }
    }

    static void grammars() {
        System.out.println("5. Context-free grammar: a parser");
        check("2 + 3 * 4 (precedence from the grammar)", 14.0, new Parser("2 + 3 * 4").parse());
        check("(2 + 3) * 4", 20.0, new Parser("(2 + 3) * 4").parse());
        check("2 + 3 * (4 - 1) / 3", 5.0, new Parser("2 + 3 * (4 - 1) / 3").parse());
        check("10 - 4 - 3 (left to right)", 3.0, new Parser("10 - 4 - 3").parse());
    }

    // =====================================================================
    // 6. a Turing machine
    // =====================================================================

    /** Binary increment: move right to the end, then carry leftwards. Returns {tape, steps}. */
    static Object[] turingIncrement(String input) {
        Map<String, String[]> rules = new HashMap<>();                         // (state, symbol) -> (write, move, next)
        rules.put("right,0", new String[]{"0", "R", "right"});
        rules.put("right,1", new String[]{"1", "R", "right"});
        rules.put("right,_", new String[]{"_", "L", "carry"});
        rules.put("carry,1", new String[]{"0", "L", "carry"});                  // 1 + carry = 0, keep carrying
        rules.put("carry,0", new String[]{"1", "L", "done"});                   // 0 + carry = 1, stop
        rules.put("carry,_", new String[]{"1", "L", "done"});                   // ran off the left: new digit
        Map<Integer, Character> tape = new HashMap<>();
        for (int i = 0; i < input.length(); i++) tape.put(i, input.charAt(i));
        int head = 0, steps = 0;
        String state = "right";
        while (!state.equals("done")) {
            char sym = tape.getOrDefault(head, '_');
            String[] act = rules.get(state + "," + sym);
            tape.put(head, act[0].charAt(0));
            head += act[1].equals("R") ? 1 : -1;
            state = act[2];
            steps++;
        }
        StringBuilder out = new StringBuilder();
        int lo = Collections.min(tape.keySet()), hi = Collections.max(tape.keySet());
        for (int i = lo; i <= hi; i++) { char c = tape.getOrDefault(i, '_'); if (c != '_') out.append(c); }
        return new Object[]{out.toString(), steps};
    }

    static void turing() {
        System.out.println("6. Turing machine");
        check("1011 (11) + 1 = 1100 (12)", "1100", turingIncrement("1011")[0]);
        check("111 (7) + 1 = 1000 (8): grows the tape", "1000", turingIncrement("111")[0]);
        boolean ok = true;
        for (int v = 0; v < 2000; v++) ok &= turingIncrement(Integer.toBinaryString(v))[0].equals(Integer.toBinaryString(v + 1));
        check("correct for every number 0..1999", true, ok);
        check("steps for 1011", 8, turingIncrement("1011")[1]);
    }

    // =====================================================================
    // 7. the halting problem, in spirit
    // =====================================================================

    static long collatzSteps(long n) { long steps = 0; while (n != 1) { n = n % 2 == 0 ? n / 2 : 3 * n + 1; steps++; } return steps; }

    static void halting() {
        System.out.println("7. Halting: the Collatz loop");
        check("Collatz steps from 27", 111L, collatzSteps(27));
        long worst = 0, worstN = 0;
        for (long n = 1; n < 1_000_000; n++) { long s = collatzSteps(n); if (s > worst) { worst = s; worstN = n; } }
        check("every n < 1,000,000 reaches 1; longest run starts at", 837_799L, worstN);
        check("...taking this many steps", 524L, worst);
    }

    // =====================================================================
    // 8. P vs NP with SAT
    // =====================================================================

    /** CNF clauses: literal k means variable k is true, -k means false (variables 1..n). */
    static boolean satisfies(int[][] cnf, boolean[] assign) {                   // VERIFY: polynomial time
        for (int[] clause : cnf) {
            boolean any = false;
            for (int lit : clause) if (assign[Math.abs(lit)] == (lit > 0)) { any = true; break; }
            if (!any) return false;
        }
        return true;
    }

    static boolean[] bruteForceSat(int[][] cnf, int n, long[] tried) {          // SEARCH: 2^n assignments
        for (long mask = 0; mask < (1L << n); mask++) {
            tried[0]++;
            boolean[] a = new boolean[n + 1];
            for (int i = 1; i <= n; i++) a[i] = (mask >> (i - 1) & 1) == 1;
            if (satisfies(cnf, a)) return a;
        }
        return null;
    }

    static void pVsNp() {
        System.out.println("8. P vs NP (satisfiability)");
        int[][] cnf = {{1, 2}, {-1, 3}, {-2, -3}, {2, 3}};                        // (x1|x2)&(!x1|x3)&(!x2|!x3)&(x2|x3)
        long[] tried = {0};
        boolean[] sol = bruteForceSat(cnf, 3, tried);
        check("a satisfying assignment exists", true, sol != null);
        check("checking the certificate is instant", true, satisfies(cnf, sol));
        int[][] unsat = {{1}, {-1}};
        tried[0] = 0;
        check("x1 AND NOT x1: unsatisfiable", true, bruteForceSat(unsat, 1, tried) == null);
        // random 3-SAT with n = 20 variables: search cost vs check cost
        Random r = new Random(8);
        int n = 20;
        int[][] hard = new int[85][];
        for (int i = 0; i < hard.length; i++) { hard[i] = new int[3]; for (int j = 0; j < 3; j++) hard[i][j] = (1 + r.nextInt(n)) * (r.nextBoolean() ? 1 : -1); }
        tried[0] = 0;
        boolean[] found = bruteForceSat(hard, n, tried);
        System.out.printf("      random 3-SAT, 20 vars, 85 clauses: tried %,d of %,d assignments; satisfiable = %b%n", tried[0], 1L << n, found != null);
        check("searching cost > 1,000x more than checking (85 clause tests)", true, tried[0] > 1000L * 85);
    }

    // =====================================================================
    // 9. hard problems: subset sum and TSP
    // =====================================================================

    static void hardProblems() {
        System.out.println("9. Subset sum, travelling salesman");
        int[] items = {3, 34, 4, 12, 5, 2};
        boolean[] reach = new boolean[10];                                      // DP over sums: O(n * target)
        reach[0] = true;
        for (int it : items) for (int s = 9; s >= it; s--) reach[s] |= reach[s - it];
        check("subset of {3,34,4,12,5,2} summing to 9? (DP)", true, reach[9]);
        int count = 0;
        for (int mask = 0; mask < 64; mask++) { int s = 0; for (int i = 0; i < 6; i++) if ((mask >> i & 1) == 1) s += items[i]; if (s == 9) count++; }
        check("brute force: how many subsets sum to 9", 2, count);
        // TSP on 9 cities: brute force (8! routes) vs Held-Karp DP (2^n n^2)
        Random r = new Random(9);
        int c = 9;
        double[][] d = new double[c][c];
        double[][] pts = new double[c][2];
        for (double[] p : pts) { p[0] = r.nextDouble() * 100; p[1] = r.nextDouble() * 100; }
        for (int i = 0; i < c; i++) for (int j = 0; j < c; j++) d[i][j] = Math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]);
        long[] routes = {0};
        double brute = tspBrute(d, routes);
        double heldKarp = tspHeldKarp(d);
        check("TSP 9 cities: brute force == Held-Karp", String.format("%.4f", brute), String.format("%.4f", heldKarp));
        check("brute force checked 8! routes", 40_320L, routes[0]);
        check("nearest-neighbour heuristic is not optimal here", true, tspGreedy(d) > brute + 1e-9);
        System.out.printf("      optimal %.2f, greedy %.2f (%.1f%% longer)%n", brute, tspGreedy(d), 100 * (tspGreedy(d) / brute - 1));
    }

    static double tspBrute(double[][] d, long[] count) {
        int n = d.length;
        int[] perm = new int[n - 1];
        for (int i = 0; i < n - 1; i++) perm[i] = i + 1;
        double best = Double.MAX_VALUE;
        do {
            count[0]++;
            double len = d[0][perm[0]] + d[perm[n - 2]][0];
            for (int i = 0; i + 1 < n - 1; i++) len += d[perm[i]][perm[i + 1]];
            best = Math.min(best, len);
        } while (nextPermutation(perm));
        return best;
    }

    static double tspHeldKarp(double[][] d) {
        int n = d.length;
        double[][] dp = new double[1 << n][n];
        for (double[] row : dp) Arrays.fill(row, Double.MAX_VALUE);
        dp[1][0] = 0;
        for (int mask = 1; mask < (1 << n); mask++)
            for (int last = 0; last < n; last++) {
                if (dp[mask][last] == Double.MAX_VALUE) continue;
                for (int nxt = 0; nxt < n; nxt++) if ((mask >> nxt & 1) == 0)
                    dp[mask | 1 << nxt][nxt] = Math.min(dp[mask | 1 << nxt][nxt], dp[mask][last] + d[last][nxt]);
            }
        double best = Double.MAX_VALUE;
        for (int last = 1; last < n; last++) best = Math.min(best, dp[(1 << n) - 1][last] + d[last][0]);
        return best;
    }

    static double tspGreedy(double[][] d) {
        int n = d.length, cur = 0;
        boolean[] seen = new boolean[n];
        seen[0] = true;
        double len = 0;
        for (int step = 1; step < n; step++) {
            int best = -1;
            for (int j = 0; j < n; j++) if (!seen[j] && (best == -1 || d[cur][j] < d[cur][best])) best = j;
            len += d[cur][best]; seen[best] = true; cur = best;
        }
        return len + d[cur][0];
    }

    static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) { t = a[l]; a[l] = a[r]; a[r] = t; }
        return true;
    }

    public static void main(String[] args) {
        dfa();
        nfaAndRegex();
        beyondRegular();
        grammars();
        turing();
        halting();
        pVsNp();
        hardProblems();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
