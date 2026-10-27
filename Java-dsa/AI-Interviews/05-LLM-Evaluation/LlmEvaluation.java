/*
 * ============================================================================
 *  AI 05 : LLM EVALUATION
 * ============================================================================
 *  Run:  java LlmEvaluation.java          (explanations: README.md in this folder)
 *
 *    1. reference metrics  exact match, normalised EM, token F1, ROUGE-L
 *    2. LLM-as-a-judge     position & length bias, and how swapping fixes position bias
 *    3. judge agreement    Cohen's kappa against human labels
 *    4. significance       bootstrap confidence intervals: small eval sets lie
 *    5. regression tests   the average improved, one category got worse
 *    6. calibration        expected calibration error (ECE)
 *    7. pass@k             for code generation
 * ============================================================================
 */

import java.util.*;

public class LlmEvaluation {

    static String f3(double x) { return String.format("%.3f", x); }

    // =====================================================================
    // 1. reference-based metrics
    // =====================================================================

    static String normalise(String s) { return s.toLowerCase().replaceAll("\\b(a|an|the)\\b", " ").replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " "); }

    static double tokenF1(String pred, String gold) {
        List<String> p = Arrays.asList(normalise(pred).split(" ")), g = new ArrayList<>(Arrays.asList(normalise(gold).split(" ")));
        int common = 0;
        for (String t : p) if (g.remove(t)) common++;                           // multiset overlap
        if (common == 0) return 0;
        double precision = common / (double) p.size(), recall = common / (double) normalise(gold).split(" ").length;
        return 2 * precision * recall / (precision + recall);
    }

    static double rougeL(String pred, String gold) {                             // longest common subsequence of tokens
        String[] a = normalise(pred).split(" "), b = normalise(gold).split(" ");
        int[][] dp = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) for (int j = 1; j <= b.length; j++) dp[i][j] = a[i - 1].equals(b[j - 1]) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        double lcs = dp[a.length][b.length], p = lcs / a.length, r = lcs / b.length;
        return lcs == 0 ? 0 : 2 * p * r / (p + r);
    }

    static void referenceMetrics() {
        System.out.println("1. Reference-based metrics");
        String gold = "Paris";
        check("exact match 'Paris.' vs 'Paris' (too strict)", false, "Paris.".equals(gold));
        check("normalised exact match", true, normalise("The Paris.").equals(normalise(gold)));
        check("token F1: 'the capital is Paris' vs 'Paris'", "0.500", f3(tokenF1("the capital is Paris", gold)));
        String g2 = "the cat sat on the mat", p2 = "the cat was sitting on the mat";
        check("token F1 (paraphrase)", "0.667", f3(tokenF1(p2, g2)));
        check("ROUGE-L (paraphrase)", "0.667", f3(rougeL(p2, g2)));
        check("a correct answer in different words scores LOW", "0.000", f3(tokenF1("The French capital city", "Paris")));
    }

    // =====================================================================
    // 2. LLM-as-a-judge: biases and the swap trick
    // =====================================================================

    /** A simulated judge: mostly prefers the truly better answer, but leans to the FIRST answer and to LONGER answers. */
    static int judge(double qualityA, int lenA, double qualityB, int lenB, Random r) {
        double score = (qualityA - qualityB) + 0.15                               // position bias towards A (shown first)
                + 0.002 * (lenA - lenB)                                           // verbosity bias
                + r.nextGaussian() * 0.2;
        return score > 0 ? 0 : 1;                                                 // 0 = A wins, 1 = B wins
    }

    static void judgeBias() {
        System.out.println("2. LLM-as-a-judge biases");
        Random r = new Random(2);
        int n = 20_000, firstWins = 0, consistentTies = 0;
        for (int i = 0; i < n; i++) if (judge(0.5, 100, 0.5, 100, r) == 0) firstWins++;   // two equally good answers
        check("equal answers: the judge picks whichever is FIRST this often", true, firstWins / (double) n > 0.7);
        System.out.printf("      first position wins %.1f%% of identical-quality pairs%n", 100.0 * firstWins / n);
        int swapAgree = 0, swapA = 0, swapB = 0;
        for (int i = 0; i < n; i++) {
            int ab = judge(0.5, 100, 0.5, 100, r), ba = judge(0.5, 100, 0.5, 100, r);   // same pair, order swapped
            boolean aWinsBoth = ab == 0 && ba == 1, bWinsBoth = ab == 1 && ba == 0;
            if (aWinsBoth) swapA++; else if (bWinsBoth) swapB++; else consistentTies++;   // disagreement -> tie
        }
        check("with order-swapping, position bias cancels (A and B win equally)", true, Math.abs(swapA - swapB) < n * 0.02);
        check("...and inconsistent verdicts become ties", true, consistentTies > n / 2);
        int longWins = 0;
        for (int i = 0; i < n; i++) { boolean longFirst = i % 2 == 0; int v = longFirst ? judge(0.5, 300, 0.5, 100, r) : judge(0.5, 100, 0.5, 300, r); if ((longFirst && v == 0) || (!longFirst && v == 1)) longWins++; }
        check("verbosity bias: the LONGER of two equal answers wins most of the time", true, longWins / (double) n > 0.75);
        System.out.printf("      longer answer wins %.1f%% (even with positions balanced)%n", 100.0 * longWins / n);
    }

    // =====================================================================
    // 3. agreement with humans: Cohen's kappa
    // =====================================================================

    static double kappa(int[] a, int[] b) {
        int n = a.length, agree = 0, a1 = 0, b1 = 0;
        for (int i = 0; i < n; i++) { if (a[i] == b[i]) agree++; a1 += a[i]; b1 += b[i]; }
        double po = agree / (double) n, pe = (a1 / (double) n) * (b1 / (double) n) + (1 - a1 / (double) n) * (1 - b1 / (double) n);
        return (po - pe) / (1 - pe);
    }

    static void agreement() {
        System.out.println("3. Judge vs human agreement");
        int[] human = {1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1};
        int[] judge = {1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1};
        int[] alwaysPass = new int[20];
        Arrays.fill(alwaysPass, 1);
        check("raw agreement judge vs human", "0.900", f3(agreement(human, judge)));
        check("Cohen's kappa (corrects for chance)", "0.780", f3(kappa(human, judge)));
        check("a judge that says PASS to everything: raw agreement", "0.650", f3(agreement(human, alwaysPass)));
        check("...but kappa exposes it", "0.000", f3(kappa(human, alwaysPass)));
    }

    static double agreement(int[] a, int[] b) { int s = 0; for (int i = 0; i < a.length; i++) if (a[i] == b[i]) s++; return s / (double) a.length; }

    // =====================================================================
    // 4. statistical significance with the bootstrap
    // =====================================================================

    static double[] bootstrapDiffCI(boolean[] a, boolean[] b, Random r) {
        int n = a.length, B = 5000;
        double[] diffs = new double[B];
        for (int t = 0; t < B; t++) {
            int ca = 0, cb = 0;
            for (int i = 0; i < n; i++) { int j = r.nextInt(n); if (a[j]) ca++; if (b[j]) cb++; }   // resample QUESTIONS (paired)
            diffs[t] = (cb - ca) / (double) n;
        }
        Arrays.sort(diffs);
        return new double[]{diffs[(int) (0.025 * B)], diffs[(int) (0.975 * B)]};
    }

    static void significance() {
        System.out.println("4. Is model B really better? (bootstrap)");
        Random r = new Random(4);
        for (int n : new int[]{50, 2000}) {
            boolean[] a = new boolean[n], b = new boolean[n];
            for (int i = 0; i < n; i++) { double difficulty = r.nextDouble(); a[i] = difficulty < 0.70; b[i] = difficulty < 0.73 || (r.nextDouble() < 0.02); }   // B truly ~3 points better
            double[] ci = bootstrapDiffCI(a, b, r);
            System.out.printf("      n=%d: 95%% CI for (B - A) = [%+.3f, %+.3f]%n", n, ci[0], ci[1]);
            if (n == 50) check("n = 50: the interval includes 0 (can't tell)", true, ci[0] <= 0);
            else check("n = 2,000: the interval excludes 0 (B is better)", true, ci[0] > 0);
        }
        check("rule of thumb: +-1 std error of an accuracy at n = 100, p = 0.8", "0.040", f3(Math.sqrt(0.8 * 0.2 / 100)));
    }

    // =====================================================================
    // 5. regression testing by category
    // =====================================================================

    static void regression() {
        System.out.println("5. Regression testing (slice the results)");
        String[] cats = {"billing", "technical", "account", "refunds"};
        int[] n = {100, 100, 100, 100};
        int[] v1 = {80, 70, 75, 90}, v2 = {88, 82, 83, 72};                         // prompt v2 vs v1, correct answers per category
        double avg1 = Arrays.stream(v1).sum() / 400.0, avg2 = Arrays.stream(v2).sum() / 400.0;
        check("overall accuracy v1 -> v2 (looks like a win)", "0.788 -> 0.813", f3(avg1) + " -> " + f3(avg2));
        List<String> regressed = new ArrayList<>();
        for (int i = 0; i < cats.length; i++) if (v2[i] < v1[i] - 5) regressed.add(cats[i] + " " + v1[i] + "% -> " + v2[i] + "%");
        check("...but a category got much worse", "[refunds 90% -> 72%]", regressed.toString());
    }

    // =====================================================================
    // 6. calibration
    // =====================================================================

    static void calibration() {
        System.out.println("6. Calibration (does '90% sure' mean right 90% of the time?)");
        Random r = new Random(6);
        int n = 20_000;
        double eceGood = ece(n, r, 0.0), eceOver = ece(n, r, 0.25);
        check("well-calibrated model: ECE close to 0", true, eceGood < 0.02);
        check("over-confident model: ECE large", true, eceOver > 0.1);
        System.out.printf("      ECE calibrated = %.3f, over-confident = %.3f%n", eceGood, eceOver);
    }

    /** Model states confidence c; it is truly right with probability c - overconfidence. */
    static double ece(int n, Random r, double overconfidence) {
        double[] sumConf = new double[10], sumRight = new double[10];
        int[] cnt = new int[10];
        for (int i = 0; i < n; i++) {
            double c = 0.5 + 0.5 * r.nextDouble();
            boolean right = r.nextDouble() < Math.max(0, c - overconfidence);
            int bin = Math.min(9, (int) (c * 10));
            cnt[bin]++; sumConf[bin] += c; sumRight[bin] += right ? 1 : 0;
        }
        double e = 0;
        for (int b = 0; b < 10; b++) if (cnt[b] > 0) e += cnt[b] / (double) n * Math.abs(sumConf[b] / cnt[b] - sumRight[b] / cnt[b]);
        return e;
    }

    // =====================================================================
    // 7. pass@k
    // =====================================================================

    static double passAtK(int n, int c, int k) {                                 // 1 - C(n-c, k) / C(n, k), computed stably
        if (n - c < k) return 1.0;
        double prod = 1;
        for (int i = n - c + 1; i <= n; i++) prod *= 1 - k / (double) i;
        return 1 - prod;
    }

    static void passAtK() {
        System.out.println("7. pass@k (code generation)");
        check("10 samples, 3 pass the tests: pass@1", "0.300", f3(passAtK(10, 3, 1)));
        check("pass@5", "0.917", f3(passAtK(10, 3, 5)));
        check("pass@10", "1.000", f3(passAtK(10, 3, 10)));
    }

    public static void main(String[] args) {
        referenceMetrics();
        judgeBias();
        agreement();
        significance();
        regression();
        calibration();
        passAtK();
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
