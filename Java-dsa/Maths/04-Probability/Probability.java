/*
 * ============================================================================
 *  MATHS 04 : PROBABILITY  (every formula next to a simulation that agrees with it)
 * ============================================================================
 *  Run:  java Probability.java          (explanations: README.md in this folder)
 *
 *    1. basics            counting outcomes, complement rule
 *    2. conditional       P(A|B), independence
 *    3. Bayes             the medical-test paradox, a tiny spam filter
 *    4. famous puzzles    birthday paradox, Monty Hall
 *    5. random variables  expectation, variance, linearity of expectation
 *    6. distributions     Bernoulli, Binomial, Geometric, Poisson, Uniform, Normal, Exponential
 *    7. joint & covariance  marginals, covariance, correlation
 *    8. limit theorems    law of large numbers, central limit theorem
 *    9. Markov chains     weather chain steady state, PageRank
 *   10. randomised algos  quicksort comparisons, reservoir sampling
 * ============================================================================
 */

import java.util.*;

public class Probability {

    static final Random RND = new Random(2026);
    static String f4(double x) { return String.format("%.4f", x); }
    static String f2(double x) { return String.format("%.2f", x); }

    // =====================================================================
    // 1. basics
    // =====================================================================

    static void basics() {
        System.out.println("1. Basics");
        int favourable = 0;
        for (int a = 1; a <= 6; a++) for (int b = 1; b <= 6; b++) if (a + b == 7) favourable++;
        check("P(two dice sum to 7) = 6/36", f4(1 / 6.0), f4(favourable / 36.0));
        double exact = 1 - Math.pow(5.0 / 6, 4);                                 // complement: 1 - P(no six)
        int hits = 0, trials = 1_000_000;
        for (int t = 0; t < trials; t++) { boolean six = false; for (int r = 0; r < 4; r++) if (RND.nextInt(6) == 5) six = true; if (six) hits++; }
        check("P(at least one 6 in 4 rolls) = 1 - (5/6)^4", "0.5177", f4(exact));
        check("...simulated with 1,000,000 tries", "0.52", f2((double) hits / trials));
    }

    // =====================================================================
    // 2. conditional probability and independence
    // =====================================================================

    static void conditional() {
        System.out.println("2. Conditional probability, independence");
        int b = 0, ab = 0;
        for (int d1 = 1; d1 <= 6; d1++) for (int d2 = 1; d2 <= 6; d2++) {
            if (d1 == 3) { b++; if (d1 + d2 == 8) ab++; }
        }
        check("P(sum = 8 | first die = 3) = P(both)/P(B)", f4(1 / 6.0), f4((double) ab / b));
        check("P(sum = 8) without the condition = 5/36", f4(5 / 36.0), f4(5 / 36.0));
        // independence: P(A and B) = P(A) P(B)?   A = first die even, B = second die > 4
        int a = 0, bb = 0, both = 0;
        for (int d1 = 1; d1 <= 6; d1++) for (int d2 = 1; d2 <= 6; d2++) {
            boolean A = d1 % 2 == 0, B = d2 > 4;
            if (A) a++; if (B) bb++; if (A && B) both++;
        }
        check("independent dice: P(A and B) = P(A) P(B)", f4((a / 36.0) * (bb / 36.0)), f4(both / 36.0));
        // dependent: A = sum >= 10, B = first die = 6
        int s10 = 0, six = 0, s10six = 0;
        for (int d1 = 1; d1 <= 6; d1++) for (int d2 = 1; d2 <= 6; d2++) {
            if (d1 + d2 >= 10) s10++; if (d1 == 6) six++; if (d1 + d2 >= 10 && d1 == 6) s10six++;
        }
        check("dependent: P(sum>=10 | first=6) = 1/2 vs P(sum>=10) = 1/6", "0.5000 vs 0.1667", f4((double) s10six / six) + " vs " + f4(s10 / 36.0));
    }

    // =====================================================================
    // 3. Bayes' theorem
    // =====================================================================

    static void bayes() {
        System.out.println("3. Bayes' theorem");
        double prior = 0.01, sens = 0.99, falsePos = 0.05;
        double pPositive = sens * prior + falsePos * (1 - prior);
        double posterior = sens * prior / pPositive;
        check("P(disease | positive test)", "0.1667", f4(posterior));
        int sick = 0, positives = 0, sickAndPositive = 0;
        for (int i = 0; i < 1_000_000; i++) {
            boolean ill = RND.nextDouble() < prior;
            boolean pos = ill ? RND.nextDouble() < sens : RND.nextDouble() < falsePos;
            if (ill) sick++;
            if (pos) { positives++; if (ill) sickAndPositive++; }
        }
        near("...simulated on 1,000,000 people", 0.1667, (double) sickAndPositive / positives, 0.01);
        double second = sens * posterior / (sens * posterior + falsePos * (1 - posterior));   // yesterday's posterior = today's prior
        check("after a SECOND positive test", "0.7984", f4(second));
        // naive Bayes spam filter from tiny counts
        Map<String, double[]> wordProb = Map.of("free", new double[]{0.30, 0.02}, "meeting", new double[]{0.01, 0.10}, "win", new double[]{0.20, 0.01});
        double pSpam = 0.4, spam = pSpam, ham = 1 - pSpam;
        for (String w : List.of("free", "win")) { spam *= wordProb.get(w)[0]; ham *= wordProb.get(w)[1]; }
        check("naive Bayes: P(spam | 'free win')", "0.9950", f4(spam / (spam + ham)));
    }

    // =====================================================================
    // 4. famous puzzles
    // =====================================================================

    static void puzzles() {
        System.out.println("4. Birthday paradox, Monty Hall");
        double noShare = 1;
        for (int k = 0; k < 23; k++) noShare *= (365.0 - k) / 365;
        check("P(two of 23 people share a birthday)", "0.5073", f4(1 - noShare));
        int shared = 0;
        for (int t = 0; t < 100_000; t++) {
            boolean[] seen = new boolean[365];
            boolean dup = false;
            for (int p = 0; p < 23 && !dup; p++) { int d = RND.nextInt(365); dup = seen[d]; seen[d] = true; }
            if (dup) shared++;
        }
        check("...simulated (100,000 rooms)", "0.51", f2(shared / 100_000.0));
        int stayWins = 0, switchWins = 0, games = 300_000;
        for (int g = 0; g < games; g++) {
            int car = RND.nextInt(3), pick = RND.nextInt(3), open;
            do open = RND.nextInt(3); while (open == car || open == pick);          // the host opens a goat door
            int switched = 3 - pick - open;
            if (pick == car) stayWins++;
            if (switched == car) switchWins++;
        }
        check("Monty Hall: P(win | stay)", "0.33", f2((double) stayWins / games));
        check("Monty Hall: P(win | switch)", "0.67", f2((double) switchWins / games));
    }

    // =====================================================================
    // 5. random variables, expectation, variance
    // =====================================================================

    static void expectation() {
        System.out.println("5. Expectation and variance");
        double mean = 0, sq = 0;
        for (int x = 1; x <= 6; x++) { mean += x / 6.0; sq += x * x / 6.0; }
        check("E[die] = (1+...+6)/6", "3.5000", f4(mean));
        check("Var[die] = E[X^2] - E[X]^2 = 35/12", "2.9167", f4(sq - mean * mean));
        check("E[2X + 3] = 2E[X] + 3", "10.0000", f4(2 * mean + 3));
        check("Var[2X + 3] = 4 Var[X] (shift adds nothing)", "11.6667", f4(4 * (sq - mean * mean)));
        // linearity of expectation: expected number of people who get their own hat back = 1 (any n)
        double fixedTotal = 0;
        int n = 10, trials = 200_000;
        for (int t = 0; t < trials; t++) {
            int[] p = shuffled(n);
            for (int i = 0; i < n; i++) if (p[i] == i) fixedTotal++;
        }
        check("E[# own hats], n = 10 (linearity says 1)", "1.00", f2(fixedTotal / trials));
        // a game: win $10 with prob 0.1, lose $2 otherwise -> E = 1 - 1.8 = -0.8
        check("expected value of a bad bet", "-0.80", f2(0.1 * 10 + 0.9 * -2));
    }

    static int[] shuffled(int n) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) { int j = RND.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }   // Fisher-Yates
        return p;
    }

    // =====================================================================
    // 6. distributions
    // =====================================================================

    static double choose(int n, int k) { double c = 1; for (int i = 1; i <= k; i++) c = c * (n - k + i) / i; return c; }

    static void distributions() {
        System.out.println("6. Distributions");
        check("Binomial(10, 0.5): P(X = 3) = C(10,3)/2^10", "0.1172", f4(choose(10, 3) / 1024));
        double atLeast8 = 0;
        for (int k = 8; k <= 10; k++) atLeast8 += choose(10, k) * Math.pow(0.7, k) * Math.pow(0.3, 10 - k);
        check("Binomial(10, 0.7): P(X >= 8)", "0.3828", f4(atLeast8));
        double sumBin = 0;
        for (int t = 0; t < 100_000; t++) { int s = 0; for (int i = 0; i < 20; i++) if (RND.nextDouble() < 0.3) s++; sumBin += s; }
        check("Binomial mean = np = 20 * 0.3", "6.0", String.format("%.1f", sumBin / 100_000));
        double trialsSum = 0;
        for (int t = 0; t < 100_000; t++) { int k = 1; while (RND.nextInt(6) != 0) k++; trialsSum += k; }
        check("Geometric: average rolls until a 6 = 1/p", "6.0", String.format("%.1f", trialsSum / 100_000));
        check("Poisson(3): P(0 events) = e^-3", "0.0498", f4(Math.exp(-3)));
        check("Poisson(3): P(X = 2) = 3^2 e^-3 / 2!", "0.2240", f4(9 * Math.exp(-3) / 2));
        int within1 = 0, within2 = 0, within3 = 0, m = 1_000_000;
        for (int i = 0; i < m; i++) { double z = Math.abs(RND.nextGaussian()); if (z < 1) within1++; if (z < 2) within2++; if (z < 3) within3++; }
        near("Normal: share within 1 std (theory 68.27%)", 68.27, 100.0 * within1 / m, 0.2);
        near("Normal: share within 2 std (theory 95.45%)", 95.45, 100.0 * within2 / m, 0.2);
        near("Normal: share within 3 std (theory 99.73%)", 99.73, 100.0 * within3 / m, 0.2);
        double expSum = 0;
        for (int i = 0; i < 1_000_000; i++) expSum += -Math.log(1 - RND.nextDouble()) / 2;   // inverse-CDF sampling, rate 2
        check("Exponential(rate 2): mean = 1/2", "0.50", f2(expSum / 1_000_000));
        check("Uniform(0,1) variance = 1/12", "0.0833", f4(1 / 12.0));
    }

    // =====================================================================
    // 7. joint distributions, covariance, correlation
    // =====================================================================

    static void jointAndCovariance() {
        System.out.println("7. Joint distributions, covariance, correlation");
        // joint table: weather (sunny/rainy) x umbrella (yes/no)
        double[][] joint = {{0.05, 0.55}, {0.30, 0.10}};                         // rows: sunny, rainy; cols: umbrella yes, no
        check("marginal P(rainy) = sum of its row", "0.40", f2(joint[1][0] + joint[1][1]));
        check("P(umbrella | rainy) = 0.30 / 0.40", "0.75", f2(joint[1][0] / (joint[1][0] + joint[1][1])));
        int n = 100_000;
        double[] x = new double[n], y = new double[n], z = new double[n];
        for (int i = 0; i < n; i++) { x[i] = RND.nextGaussian(); y[i] = 2 * x[i] + 0.5 * RND.nextGaussian(); z[i] = RND.nextGaussian(); }
        check("corr(x, 2x + small noise) ~ 0.97", "0.97", f2(corr(x, y)));
        near("corr(x, independent z) ~ 0", 0.0, corr(x, z), 0.02);
        double[] sq = new double[n];
        for (int i = 0; i < n; i++) sq[i] = x[i] * x[i];
        near("corr(x, x^2) ~ 0 although totally dependent!", 0.0, corr(x, sq), 0.02);
    }

    static double corr(double[] a, double[] b) {
        int n = a.length;
        double ma = 0, mb = 0;
        for (int i = 0; i < n; i++) { ma += a[i] / n; mb += b[i] / n; }
        double cov = 0, va = 0, vb = 0;
        for (int i = 0; i < n; i++) { cov += (a[i] - ma) * (b[i] - mb); va += (a[i] - ma) * (a[i] - ma); vb += (b[i] - mb) * (b[i] - mb); }
        return cov / Math.sqrt(va * vb);
    }

    // =====================================================================
    // 8. law of large numbers, central limit theorem
    // =====================================================================

    static void limitTheorems() {
        System.out.println("8. Law of large numbers, central limit theorem");
        StringBuilder running = new StringBuilder();
        double sum = 0;
        for (int i = 1; i <= 1_000_000; i++) {
            sum += 1 + RND.nextInt(6);
            if (i == 10 || i == 1000 || i == 1_000_000) running.append(f2(sum / i)).append(' ');
        }
        check("average of n die rolls, n = 10, 1e3, 1e6 (-> 3.5)", true, running.toString().trim().endsWith("3.50"));
        System.out.println("      running averages: " + running);
        // CLT: the mean of 30 uniforms is ~ Normal(0.5, sqrt(1/12/30))
        int samples = 200_000, k = 30, within = 0;
        double sd = Math.sqrt(1.0 / 12 / k), sumMeans = 0, sumSq = 0;
        for (int s = 0; s < samples; s++) {
            double m = 0;
            for (int i = 0; i < k; i++) m += RND.nextDouble() / k;
            sumMeans += m; sumSq += m * m;
            if (Math.abs(m - 0.5) < sd) within++;
        }
        double meanOfMeans = sumMeans / samples, sdOfMeans = Math.sqrt(sumSq / samples - meanOfMeans * meanOfMeans);
        check("CLT: std of the mean of 30 = sigma/sqrt(30)", f4(sd), f4(sdOfMeans));
        check("...and ~68% land within 1 std (normal!)", "68%", String.format("%.0f%%", 100.0 * within / samples));
    }

    // =====================================================================
    // 9. Markov chains
    // =====================================================================

    static void markov() {
        System.out.println("9. Markov chains, PageRank");
        double[][] P = {{0.9, 0.1}, {0.5, 0.5}};                                   // sunny -> sunny 0.9, rainy -> sunny 0.5
        double[] dist = {1, 0};
        for (int day = 0; day < 100; day++) dist = new double[]{dist[0] * P[0][0] + dist[1] * P[1][0], dist[0] * P[0][1] + dist[1] * P[1][1]};
        check("weather steady state (sunny, rainy) = (5/6, 1/6)", "0.8333, 0.1667", f4(dist[0]) + ", " + f4(dist[1]));
        int sunnyDays = 0, state = 0;
        for (int day = 0; day < 1_000_000; day++) { state = RND.nextDouble() < P[state][0] ? 0 : 1; if (state == 0) sunnyDays++; }
        check("...fraction of sunny days in a long simulation", "0.83", f2(sunnyDays / 1e6));
        // PageRank on 4 pages: links 0->1,2  1->2  2->0  3->2 ; damping 0.85
        int[][] links = {{1, 2}, {2}, {0}, {2}};
        double[] rank = {0.25, 0.25, 0.25, 0.25};
        for (int it = 0; it < 100; it++) {
            double[] next = new double[4];
            Arrays.fill(next, 0.15 / 4);
            for (int p = 0; p < 4; p++) for (int q : links[p]) next[q] += 0.85 * rank[p] / links[p].length;
            rank = next;
        }
        check("PageRank: page 2 (most linked) ranks highest", "0.3725, 0.1958, 0.3941, 0.0375", f4(rank[0]) + ", " + f4(rank[1]) + ", " + f4(rank[2]) + ", " + f4(rank[3]));
    }

    // =====================================================================
    // 10. randomised algorithms
    // =====================================================================

    static long comparisons;

    static void quicksort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = a[lo + RND.nextInt(hi - lo + 1)], i = lo, j = hi;
        while (i <= j) {
            while (a[i] < p) { i++; comparisons++; }
            while (a[j] > p) { j--; comparisons++; }
            comparisons += 2;
            if (i <= j) { int t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; }
        }
        quicksort(a, lo, j);
        quicksort(a, i, hi);
    }

    static void randomised() {
        System.out.println("10. Randomised algorithms");
        int n = 100_000;
        int[] a = shuffled(n);
        quicksort(a, 0, n - 1);
        double ratio = comparisons / (n * Math.log(n));
        check("random-pivot quicksort: comparisons / (n ln n) is a small constant", true, ratio > 1 && ratio < 3);
        System.out.printf("      comparisons = %,d,  n ln n = %,.0f%n", comparisons, n * Math.log(n));
        // reservoir sampling: pick 1 item uniformly from a stream of unknown length
        int[] counts = new int[10];
        for (int t = 0; t < 200_000; t++) {
            int chosen = -1;
            for (int i = 0; i < 10; i++) if (RND.nextInt(i + 1) == 0) chosen = i;   // keep item i with prob 1/(i+1)
            counts[chosen]++;
        }
        boolean uniform = true;
        for (int c : counts) uniform &= Math.abs(c / 200_000.0 - 0.1) < 0.005;
        check("reservoir sampling: each of 10 items ~10%", true, uniform);
    }

    public static void main(String[] args) {
        basics();
        conditional();
        bayes();
        puzzles();
        expectation();
        distributions();
        jointAndCovariance();
        limitTheorems();
        markov();
        randomised();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-56s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    /** For simulations: pass if the result is within tol of the theory, and print what we got. */
    static void near(String label, double theory, double got, double tol) {
        total++;
        boolean ok = Math.abs(got - theory) <= tol;
        if (ok) passed++;
        System.out.printf("   %s  %-56s -> %.4f%s%n", ok ? "PASS" : "FAIL", label, got, ok ? "" : "   (expected " + theory + " +- " + tol + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
