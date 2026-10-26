/*
 * ============================================================================
 *  MATHS 05 : STATISTICS  (learning from data)
 * ============================================================================
 *  Run:  java Statistics.java          (explanations: README.md in this folder)
 *
 *    1. descriptive       mean, median, variance, percentiles, outliers
 *    2. estimators        why the sample variance divides by n - 1; standard error
 *    3. MLE               coin bias by maximising log-likelihood; Gaussian MLE
 *    4. Bayesian / MAP    Beta prior + coin flips -> posterior
 *    5. confidence int.   95% CIs actually cover the truth ~95% of the time
 *    6. hypothesis tests  p-value of a coin, A/B test, permutation test, multiple testing
 *    7. regression        slope = cov / var, R^2
 *    8. bias-variance     polynomial degree 1 vs 3 vs 9; cross-validation picks the middle
 *    9. classification    confusion matrix, precision, recall, F1, the accuracy trap
 * ============================================================================
 */

import java.util.*;

public class Statistics {

    static final Random RND = new Random(5);
    static String f4(double x) { return String.format("%.4f", x); }

    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static double var(double[] a, int ddof) { double m = mean(a), s = 0; for (double x : a) s += (x - m) * (x - m); return s / (a.length - ddof); }

    static double percentile(double[] a, double p) {                         // linear interpolation between ranks
        double[] s = a.clone();
        Arrays.sort(s);
        double pos = p / 100 * (s.length - 1);
        int lo = (int) Math.floor(pos), hi = (int) Math.ceil(pos);
        return s[lo] + (s[hi] - s[lo]) * (pos - lo);
    }

    /** Standard normal CDF via erf (Abramowitz-Stegun 7.1.26, error < 1.5e-7). */
    static double phi(double z) {
        double x = Math.abs(z) / Math.sqrt(2), t = 1 / (1 + 0.3275911 * x);
        double erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return z >= 0 ? 0.5 * (1 + erf) : 0.5 * (1 - erf);
    }

    // =====================================================================
    // 1. descriptive statistics
    // =====================================================================

    static void descriptive() {
        System.out.println("1. Descriptive statistics");
        double[] salaries = {30, 32, 35, 38, 40, 41, 45, 50, 52, 1000};          // in $1000s; one CEO
        check("mean salary (dragged up by one outlier)", "136.3", String.format("%.1f", mean(salaries)));
        check("median salary (robust)", "40.5", String.format("%.1f", percentile(salaries, 50)));
        double[] withoutCeo = Arrays.copyOf(salaries, 9);
        check("mean without the outlier", "40.3", String.format("%.1f", mean(withoutCeo)));
        double[] d = {2, 4, 4, 4, 5, 5, 7, 9};
        check("variance (population, / n)", "4.0000", f4(var(d, 0)));
        check("standard deviation", "2.0000", f4(Math.sqrt(var(d, 0))));
        check("sample variance (/ n - 1)", "4.5714", f4(var(d, 1)));
        double q1 = percentile(d, 25), q3 = percentile(d, 75);
        check("Q1, Q3, IQR", "4.0, 5.5, 1.5", q1 + ", " + q3 + ", " + (q3 - q1));
        check("outlier fence Q3 + 1.5 IQR: is 9 an outlier?", true, 9 > q3 + 1.5 * (q3 - q1));
        double z = (9 - mean(d)) / Math.sqrt(var(d, 0));
        check("z-score of 9 = (9 - mean)/std", "2.0000", f4(z));
    }

    // =====================================================================
    // 2. estimators
    // =====================================================================

    static void estimators() {
        System.out.println("2. Estimators: bias and standard error");
        int trials = 200_000, n = 5;
        double sumBiased = 0, sumUnbiased = 0;
        for (int t = 0; t < trials; t++) {
            double[] s = new double[n];
            for (int i = 0; i < n; i++) s[i] = RND.nextGaussian() * 2;          // true variance = 4
            sumBiased += var(s, 0); sumUnbiased += var(s, 1);
        }
        near("avg of (divide by n) estimate, true var 4", 3.2, sumBiased / trials, 0.05);
        near("avg of (divide by n-1) estimate", 4.0, sumUnbiased / trials, 0.05);
        // standard error of the mean = sigma / sqrt(n)
        double[] means = new double[50_000];
        for (int t = 0; t < means.length; t++) { double s = 0; for (int i = 0; i < 100; i++) s += 10 + RND.nextGaussian() * 15; means[t] = s / 100; }
        near("spread of the sample mean, n=100, sigma=15 (SE = 1.5)", 1.5, Math.sqrt(var(means, 1)), 0.03);
    }

    // =====================================================================
    // 3. maximum likelihood
    // =====================================================================

    static void mle() {
        System.out.println("3. Maximum likelihood estimation");
        int heads = 7, n = 10;
        double bestP = 0, bestLL = Double.NEGATIVE_INFINITY;
        for (double p = 0.001; p < 1; p += 0.001) {
            double ll = heads * Math.log(p) + (n - heads) * Math.log(1 - p);      // log-likelihood
            if (ll > bestLL) { bestLL = ll; bestP = p; }
        }
        check("coin: 7 heads in 10 -> argmax log-likelihood", "0.700", String.format("%.3f", bestP));
        check("likelihood at 0.7 vs at 0.5", "0.2668 vs 0.1172", f4(choose(10, 7) * Math.pow(0.7, 7) * Math.pow(0.3, 3)) + " vs " + f4(choose(10, 7) / 1024));
        double[] data = new double[100_000];
        for (int i = 0; i < data.length; i++) data[i] = 170 + 8 * RND.nextGaussian();
        near("Gaussian MLE of the mean = sample mean (true 170)", 170, mean(data), 0.1);
        near("Gaussian MLE of sigma (true 8)", 8, Math.sqrt(var(data, 0)), 0.1);
    }

    static double choose(int n, int k) { double c = 1; for (int i = 1; i <= k; i++) c = c * (n - k + i) / i; return c; }

    // =====================================================================
    // 4. Bayesian estimation and MAP
    // =====================================================================

    static void bayesian() {
        System.out.println("4. Bayesian estimation, MAP");
        double a = 2, b = 2;                                                    // prior Beta(2,2): "probably near 0.5"
        int heads = 7, tails = 3;
        double pa = a + heads, pb = b + tails;                                  // conjugate update
        check("posterior is Beta(9, 5)", "9.0, 5.0", pa + ", " + pb);
        check("posterior mean 9/14", "0.6429", f4(pa / (pa + pb)));
        check("MAP (posterior mode) (9-1)/(9+5-2)", "0.6667", f4((pa - 1) / (pa + pb - 2)));
        check("MLE ignores the prior: 7/10", "0.7000", f4(7 / 10.0));
        check("tiny data, 1 flip, 1 head: MLE says 1.0, posterior mean", "0.6000", f4((a + 1) / (a + b + 1)));
        check("1,000 flips with 700 heads: prior barely matters", "0.6992", f4((a + 700) / (a + b + 1000)));
    }

    // =====================================================================
    // 5. confidence intervals
    // =====================================================================

    static void confidenceIntervals() {
        System.out.println("5. Confidence intervals");
        double trueMean = 50, sigma = 10;
        int n = 40, covered = 0, experiments = 20_000;
        for (int t = 0; t < experiments; t++) {
            double[] s = new double[n];
            for (int i = 0; i < n; i++) s[i] = trueMean + sigma * RND.nextGaussian();
            double m = mean(s), se = Math.sqrt(var(s, 1) / n);
            if (m - 1.96 * se <= trueMean && trueMean <= m + 1.96 * se) covered++;
        }
        near("95% CIs that contain the true mean", 0.95, (double) covered / experiments, 0.01);
        check("z for 95% two-sided = 1.96", "0.9750", f4(phi(1.96)));
        double pHat = 0.42, size = 1000;
        double margin = 1.96 * Math.sqrt(pHat * (1 - pHat) / size);
        check("poll: 42% of 1,000 people, margin of error", "3.06%", String.format("%.2f%%", margin * 100));
    }

    // =====================================================================
    // 6. hypothesis testing
    // =====================================================================

    static void hypothesisTests() {
        System.out.println("6. Hypothesis testing");
        // is a coin with 60 heads in 100 flips fair?  exact two-sided binomial p-value
        double tail = 0;
        for (int k = 60; k <= 100; k++) tail += choose(100, k) * Math.pow(0.5, 100);
        check("exact p-value, 60/100 heads (two-sided)", "0.0569", f4(2 * tail));
        double z = (60 - 50) / Math.sqrt(100 * 0.25);
        check("z-test approximation: z = 2.0, p", "0.0455", f4(2 * (1 - phi(z))));
        // A/B test: A converts 200/2000, B converts 250/2000
        double pA = 200 / 2000.0, pB = 250 / 2000.0, pool = 450 / 4000.0;
        double zAB = (pB - pA) / Math.sqrt(pool * (1 - pool) * (2.0 / 2000));
        check("A/B test z-score", "2.5020", f4(zAB));
        check("A/B p-value (two-sided)", "0.0124", f4(2 * (1 - phi(zAB))));
        // permutation test on the same data: shuffle labels, how often is the gap this big?
        int[] outcomes = new int[4000];
        for (int i = 0; i < 450; i++) outcomes[i] = 1;
        double observed = pB - pA;
        int extreme = 0, shuffles = 20_000;
        for (int s = 0; s < shuffles; s++) {
            for (int i = outcomes.length - 1; i > 0; i--) { int j = RND.nextInt(i + 1); int t = outcomes[i]; outcomes[i] = outcomes[j]; outcomes[j] = t; }
            int a = 0, b = 0;
            for (int i = 0; i < 2000; i++) a += outcomes[i];
            for (int i = 2000; i < 4000; i++) b += outcomes[i];
            if (Math.abs(b - a) / 2000.0 >= observed - 1e-12) extreme++;
        }
        near("permutation test p-value (no formulas needed)", 0.0124, (double) extreme / shuffles, 0.004);
        check("20 tests at alpha 0.05: P(at least 1 false alarm)", "0.6415", f4(1 - Math.pow(0.95, 20)));
        check("Bonferroni: use alpha/20 per test", "0.0025", f4(0.05 / 20));
    }

    // =====================================================================
    // 7. regression
    // =====================================================================

    static void regression() {
        System.out.println("7. Simple linear regression");
        double[] x = {1, 2, 3, 4, 5}, y = {2, 4, 5, 4, 5};
        double mx = mean(x), my = mean(y), cov = 0, vx = 0;
        for (int i = 0; i < 5; i++) { cov += (x[i] - mx) * (y[i] - my); vx += (x[i] - mx) * (x[i] - mx); }
        double slope = cov / vx, intercept = my - slope * mx;
        check("slope = cov(x,y)/var(x)", "0.6000", f4(slope));
        check("intercept = mean(y) - slope mean(x)", "2.2000", f4(intercept));
        double ssRes = 0, ssTot = 0;
        for (int i = 0; i < 5; i++) { double p = intercept + slope * x[i]; ssRes += (y[i] - p) * (y[i] - p); ssTot += (y[i] - my) * (y[i] - my); }
        check("R^2 = 1 - SS_res / SS_tot", "0.6000", f4(1 - ssRes / ssTot));
        check("R^2 = corr^2 for a straight line", "0.6000", f4(Math.pow(cov / Math.sqrt(vx * ssTot), 2)));
    }

    // =====================================================================
    // 8. bias-variance trade-off and cross-validation
    // =====================================================================

    static double[] polyFit(double[] x, double[] y, int deg) {
        int k = deg + 1;
        double[][] A = new double[k][k];
        double[] b = new double[k];
        for (int i = 0; i < x.length; i++)
            for (int r = 0; r < k; r++) {
                b[r] += Math.pow(x[i], r) * y[i];
                for (int c = 0; c < k; c++) A[r][c] += Math.pow(x[i], r + c);
            }
        for (int r = 0; r < k; r++) A[r][r] += 1e-12;                            // tiny ridge: keeps the solve stable
        return solve(A, b);
    }

    static double predict(double[] w, double x) { double s = 0; for (int i = w.length - 1; i >= 0; i--) s = s * x + w[i]; return s; }

    static double mse(double[] w, double[] x, double[] y) { double s = 0; for (int i = 0; i < x.length; i++) s += Math.pow(predict(w, x[i]) - y[i], 2); return s / x.length; }

    static double[] solve(double[][] A, double[] b) {
        int n = b.length;
        double[][] M = new double[n][n + 1];
        for (int i = 0; i < n; i++) { System.arraycopy(A[i], 0, M[i], 0, n); M[i][n] = b[i]; }
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
            double[] t = M[c]; M[c] = M[p]; M[p] = t;
            for (int r = c + 1; r < n; r++) { double f = M[r][c] / M[c][c]; for (int j = c; j <= n; j++) M[r][j] -= f * M[c][j]; }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) { double s = M[i][n]; for (int j = i + 1; j < n; j++) s -= M[i][j] * x[j]; x[i] = s / M[i][i]; }
        return x;
    }

    static void biasVariance() {
        System.out.println("8. Bias-variance trade-off, cross-validation");
        Random r = new Random(8);
        int nTrain = 10;
        double[] xt = new double[nTrain], yt = new double[nTrain], xs = new double[500], ys = new double[500];
        for (int i = 0; i < nTrain; i++) { xt[i] = -1 + 2.0 * i / (nTrain - 1); yt[i] = Math.sin(Math.PI * xt[i]) + 0.2 * r.nextGaussian(); }
        for (int i = 0; i < 500; i++) { xs[i] = -1 + 2 * r.nextDouble(); ys[i] = Math.sin(Math.PI * xs[i]) + 0.2 * r.nextGaussian(); }
        double[] tr = new double[10], te = new double[10];
        StringBuilder table = new StringBuilder();
        for (int deg : new int[]{1, 3, 9}) {
            double[] w = polyFit(xt, yt, deg);
            tr[deg] = mse(w, xt, yt); te[deg] = mse(w, xs, ys);
            table.append(String.format("deg %d: train %.3f test %.3f | ", deg, tr[deg], te[deg]));
        }
        System.out.println("      " + table);
        check("training error keeps falling with degree", true, tr[1] > tr[3] && tr[3] > tr[9]);
        check("degree 9 memorises the 10 points (train ~ 0)", true, tr[9] < 1e-6);
        check("test error: degree 3 beats both 1 (underfit) and 9 (overfit)", true, te[3] < te[1] && te[3] < te[9]);
        // 5-fold cross-validation on 40 points to choose the degree
        int n = 40;
        double[] cx = new double[n], cy = new double[n];
        for (int i = 0; i < n; i++) { cx[i] = -1 + 2 * r.nextDouble(); cy[i] = Math.sin(Math.PI * cx[i]) + 0.2 * r.nextGaussian(); }
        int bestDeg = -1;
        double bestErr = Double.MAX_VALUE;
        for (int deg = 1; deg <= 12; deg++) {
            double err = 0;
            for (int fold = 0; fold < 5; fold++) {
                List<Integer> trainIdx = new ArrayList<>(), testIdx = new ArrayList<>();
                for (int i = 0; i < n; i++) (i % 5 == fold ? testIdx : trainIdx).add(i);
                double[] w = polyFit(pick(cx, trainIdx), pick(cy, trainIdx), deg);
                err += mse(w, pick(cx, testIdx), pick(cy, testIdx)) / 5;
            }
            if (err < bestErr) { bestErr = err; bestDeg = deg; }
        }
        check("5-fold CV chooses a middle degree (3..7), not 1 or 12", true, bestDeg >= 3 && bestDeg <= 7);
        System.out.println("      CV picked degree " + bestDeg);
    }

    static double[] pick(double[] a, List<Integer> idx) { double[] r = new double[idx.size()]; for (int i = 0; i < r.length; i++) r[i] = a[idx.get(i)]; return r; }

    // =====================================================================
    // 9. classification metrics
    // =====================================================================

    static void classification() {
        System.out.println("9. Classification metrics");
        int tp = 80, fp = 20, fn = 40, tn = 860;                                // a fraud model on 1,000 transactions
        double precision = (double) tp / (tp + fp), recall = (double) tp / (tp + fn);
        check("accuracy (tp+tn)/all", "0.9400", f4((tp + tn) / 1000.0));
        check("precision tp/(tp+fp): of the flagged, how many are fraud", "0.8000", f4(precision));
        check("recall tp/(tp+fn): of the fraud, how many we caught", "0.6667", f4(recall));
        check("F1 = harmonic mean of precision and recall", "0.7273", f4(2 * precision * recall / (precision + recall)));
        check("a model that always says 'not fraud' scores accuracy", "0.8800", f4((fp + tn) / 1000.0));
        check("...but its recall is", "0.0000", f4(0.0));
    }

    public static void main(String[] args) {
        descriptive();
        estimators();
        mle();
        bayesian();
        confidenceIntervals();
        hypothesisTests();
        regression();
        biasVariance();
        classification();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-58s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    /** For simulations: pass if the result is within tol of the theory, and print what we got. */
    static void near(String label, double theory, double got, double tol) {
        total++;
        boolean ok = Math.abs(got - theory) <= tol;
        if (ok) passed++;
        System.out.printf("   %s  %-58s -> %.4f%s%n", ok ? "PASS" : "FAIL", label, got, ok ? "" : "   (expected " + theory + " +- " + tol + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
