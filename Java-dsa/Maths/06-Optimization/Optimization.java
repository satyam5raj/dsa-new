/*
 * ============================================================================
 *  MATHS 06 : OPTIMISATION  (how models find their best parameters)
 * ============================================================================
 *  Run:  java Optimization.java          (explanations: README.md in this folder)
 *
 *    1. convexity        convex = one valley; non-convex = the start point decides
 *    2. learning rate    too small is slow, too big diverges (threshold 2 / L)
 *    3. GD variants      batch vs mini-batch vs stochastic on linear regression
 *    4. momentum         fewer steps on a long narrow valley
 *    5. RMSProp & Adam   per-parameter step sizes
 *    6. Newton           uses curvature: one step on a quadratic
 *    7. constraints      Lagrange multipliers
 *    8. linear programs  the best answer sits on a corner
 *    9. regularisation   ridge shrinks, lasso zeroes out useless weights
 *   10. saddle points    plain GD gets stuck exactly on one; a little noise escapes
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class Optimization {

    static String f4(double x) { return String.format("%.4f", x); }

    // =====================================================================
    // 1. convexity
    // =====================================================================

    static void convexity() {
        System.out.println("1. Convex vs non-convex");
        DoubleUnaryOperator convex = x -> x * x + 2 * x + 3;
        Random r = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 10_000; t++) {
            double a = r.nextDouble() * 20 - 10, b = r.nextDouble() * 20 - 10, l = r.nextDouble();
            ok &= convex.applyAsDouble(l * a + (1 - l) * b) <= l * convex.applyAsDouble(a) + (1 - l) * convex.applyAsDouble(b) + 1e-9;
        }
        check("x^2+2x+3: chord always above the curve (convex)", true, ok);
        DoubleUnaryOperator bumpy = x -> Math.pow(x, 4) - 3 * x * x + x;            // two valleys
        DoubleUnaryOperator grad = x -> 4 * Math.pow(x, 3) - 6 * x + 1;
        double left = descend(grad, -2, 0.01, 2000), right = descend(grad, 2, 0.01, 2000);
        check("non-convex: start at -2 -> lands at", "-1.3008", f4(left));
        check("non-convex: start at +2 -> lands at", "1.1309", f4(right));
        check("the left valley is lower (global minimum)", true, bumpy.applyAsDouble(left) < bumpy.applyAsDouble(right));
        System.out.printf("      f(left) = %.4f, f(right) = %.4f%n", bumpy.applyAsDouble(left), bumpy.applyAsDouble(right));
    }

    static double descend(DoubleUnaryOperator grad, double x, double lr, int steps) { for (int i = 0; i < steps; i++) x -= lr * grad.applyAsDouble(x); return x; }

    // =====================================================================
    // 2. learning rate on f(x, y) = x^2 + 10 y^2  (curvatures 2 and 20)
    // =====================================================================

    static int stepsToConverge(double lr, double momentum) {
        double x = 10, y = 1, vx = 0, vy = 0;
        for (int i = 1; i <= 10_000; i++) {
            double gx = 2 * x, gy = 20 * y;
            vx = momentum * vx - lr * gx; vy = momentum * vy - lr * gy;
            x += vx; y += vy;
            if (Math.abs(x) > 1e6 || Math.abs(y) > 1e6) return -1;                // diverged
            if (Math.hypot(x, y) < 1e-6) return i;
        }
        return 10_000;
    }

    static void learningRate() {
        System.out.println("2. Learning rate (f = x^2 + 10 y^2)");
        check("lr 0.001: converges, but very slowly", 8051, stepsToConverge(0.001, 0));
        check("lr 0.01: 10x bigger, 10x fewer steps", 798, stepsToConverge(0.01, 0));
        check("lr 0.09: just under the limit, fast", 82, stepsToConverge(0.09, 0));
        check("lr 0.11 > 2/L = 0.1: DIVERGES", -1, stepsToConverge(0.11, 0));
    }

    // =====================================================================
    // 3. batch vs mini-batch vs stochastic gradient descent
    // =====================================================================

    static void gdVariants() {
        System.out.println("3. Batch, mini-batch, stochastic GD (linear regression y = 3x + 2)");
        Random r = new Random(3);
        int n = 1000;
        double[] x = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) { x[i] = r.nextDouble() * 2 - 1; y[i] = 3 * x[i] + 2 + 0.1 * r.nextGaussian(); }
        for (int batch : new int[]{n, 32, 1}) {
            double w = 0, b = 0;
            int updates = 0;
            for (int epoch = 0; epoch < 50; epoch++)
                for (int start = 0; start < n; start += batch) {
                    double gw = 0, gb = 0;
                    int end = Math.min(n, start + batch);
                    for (int i = start; i < end; i++) { double err = w * x[i] + b - y[i]; gw += 2 * err * x[i]; gb += 2 * err; }
                    w -= 0.05 * gw / (end - start); b -= 0.05 * gb / (end - start);
                    updates++;
                }
            String name = batch == n ? "batch (1 update per epoch)" : batch == 1 ? "stochastic (batch 1)" : "mini-batch 32";
            check(name + ": w, b after 50 epochs", batch == n ? "2.46, 1.99" : batch == 1 ? "2.98, 2.00" : "3.00, 2.01", String.format("%.2f, %.2f", w, b));
            System.out.printf("      %s made %,d updates%n", name, updates);
        }
    }

    // =====================================================================
    // 4. momentum
    // =====================================================================

    static void momentum() {
        System.out.println("4. Momentum");
        int plain = stepsToConverge(0.01, 0), withMomentum = stepsToConverge(0.01, 0.9);
        check("plain GD, lr 0.01", 798, plain);
        check("with momentum 0.9, same lr (~3x fewer)", 269, withMomentum);
    }

    // =====================================================================
    // 5. RMSProp and Adam
    // =====================================================================

    /** f(x, y) = x^2 + 1000 y^2: wildly different curvatures. Returns steps to reach |p| < 1e-3. */
    static int adaptive(String method) {
        double[] p = {5, 5}, m = new double[2], s = new double[2];
        double lr = method.equals("gd") ? 0.0009 : 0.1, b1 = 0.9, b2 = 0.999, eps = 1e-8;
        for (int t = 1; t <= 20_000; t++) {
            double[] g = {2 * p[0], 2000 * p[1]};
            for (int i = 0; i < 2; i++) {
                switch (method) {
                    case "gd" -> p[i] -= lr * g[i];
                    case "rmsprop" -> { s[i] = 0.9 * s[i] + 0.1 * g[i] * g[i]; p[i] -= lr * g[i] / (Math.sqrt(s[i]) + eps); }
                    default -> {                                                     // Adam
                        m[i] = b1 * m[i] + (1 - b1) * g[i];
                        s[i] = b2 * s[i] + (1 - b2) * g[i] * g[i];
                        double mh = m[i] / (1 - Math.pow(b1, t)), sh = s[i] / (1 - Math.pow(b2, t));   // bias correction
                        p[i] -= lr * mh / (Math.sqrt(sh) + eps);
                    }
                }
            }
            if (Math.hypot(p[0], p[1]) < 1e-3) return t;
        }
        return 20_000;
    }

    static void adaptiveMethods() {
        System.out.println("5. RMSProp and Adam (f = x^2 + 1000 y^2)");
        int gd = adaptive("gd"), rms = adaptive("rmsprop"), adam = adaptive("adam");
        System.out.printf("      steps: plain GD %d, RMSProp %d, Adam %d%n", gd, rms, adam);
        check("plain GD needs a tiny lr (< 2/2000) and many steps", true, gd > 3000);
        check("RMSProp: much fewer steps", true, rms < gd / 5);
        check("Adam: much fewer steps", true, adam < gd / 5);
    }

    // =====================================================================
    // 6. Newton's method
    // =====================================================================

    static void newton() {
        System.out.println("6. Newton's method");
        // on a quadratic f = x^2 + 10y^2, one Newton step x <- x - H^-1 grad lands on the minimum
        double x = 10, y = 1;
        double nx = x - (2 * x) / 2, ny = y - (20 * y) / 20;
        check("quadratic: one Newton step from (10, 1) reaches", "(0.0, 0.0)", "(" + nx + ", " + ny + ")");
        // non-quadratic: minimise f(x) = x - ln x  (min at x = 1); f' = 1 - 1/x, f'' = 1/x^2
        double t = 0.3;
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 5; i++) { t = t - (1 - 1 / t) / (1 / (t * t)); s.append(String.format("%.8f ", t)); }
        check("x - ln x from 0.3: errors square each step", "0.51000000 0.75990000 0.94235199 0.99667671 0.99998896 ", s.toString());
    }

    // =====================================================================
    // 7. Lagrange multipliers
    // =====================================================================

    static void lagrange() {
        System.out.println("7. Constrained optimisation (Lagrange multipliers)");
        // maximise xy subject to x + y = 10:  y = lambda, x = lambda, x + y = 10 -> x = y = 5
        double best = -1, bx = 0;
        for (double c = 0; c <= 10; c += 0.001) if (c * (10 - c) > best) { best = c * (10 - c); bx = c; }
        check("max xy with x + y = 10 (grid search)", "x = 5.00, xy = 25.00", String.format("x = %.2f, xy = %.2f", bx, best));
        // minimise x^2 + y^2 subject to x + 2y = 5: grad f = lambda grad g -> (2x, 2y) = lambda (1, 2) -> y = 2x -> x = 1, y = 2
        double bestD = Double.MAX_VALUE, px = 0;
        for (double c = -5; c <= 5; c += 0.0001) { double yy = (5 - c) / 2, d = c * c + yy * yy; if (d < bestD) { bestD = d; px = c; } }
        check("closest point to origin on x + 2y = 5", "(1.000, 2.000)", String.format("(%.3f, %.3f)", px, (5 - px) / 2));
        check("...lambda = 2x = 2, and grad f is parallel to (1, 2)", "2.000, 4.000", String.format("%.3f, %.3f", 2 * px, 2 * (5 - px) / 2));
    }

    // =====================================================================
    // 8. linear programming
    // =====================================================================

    static void linearProgramming() {
        System.out.println("8. Linear programming");
        // maximise 3x + 5y  s.t.  x <= 4,  2y <= 12,  3x + 2y <= 18,  x, y >= 0
        double[][] lines = {{1, 0, 4}, {0, 2, 12}, {3, 2, 18}, {1, 0, 0}, {0, 1, 0}};   // a x + b y = c
        double best = -1;
        String at = "";
        for (int i = 0; i < lines.length; i++)
            for (int j = i + 1; j < lines.length; j++) {
                double det = lines[i][0] * lines[j][1] - lines[i][1] * lines[j][0];
                if (Math.abs(det) < 1e-12) continue;
                double px = (lines[i][2] * lines[j][1] - lines[i][1] * lines[j][2]) / det, py = (lines[i][0] * lines[j][2] - lines[i][2] * lines[j][0]) / det;
                boolean feasible = px >= -1e-9 && py >= -1e-9 && px <= 4 + 1e-9 && 2 * py <= 12 + 1e-9 && 3 * px + 2 * py <= 18 + 1e-9;
                if (feasible && 3 * px + 5 * py > best) { best = 3 * px + 5 * py; at = String.format("(%.0f, %.0f)", px, py); }
            }
        check("best corner of the feasible region", "(2, 6)", at);
        check("maximum profit 3x + 5y", "36.0", String.format("%.1f", best));
    }

    // =====================================================================
    // 9. regularisation: ridge vs lasso
    // =====================================================================

    static void regularisation() {
        System.out.println("9. Ridge (L2) vs lasso (L1)");
        Random r = new Random(9);
        int n = 200, d = 6;
        double[][] X = new double[n][d];
        double[] y = new double[n], trueW = {4, -3, 0, 0, 0, 0};                  // only 2 features matter
        for (int i = 0; i < n; i++) { for (int j = 0; j < d; j++) X[i][j] = r.nextGaussian(); y[i] = 4 * X[i][0] - 3 * X[i][1] + 0.5 * r.nextGaussian(); }
        double[] ridge = coordinateDescent(X, y, 0, 50), lasso = coordinateDescent(X, y, 30, 0);
        check("ridge: all 6 weights non-zero (just shrunk)", 6L, Arrays.stream(ridge).filter(w -> Math.abs(w) > 1e-6).count());
        check("lasso: only the 2 real features survive", 2L, Arrays.stream(lasso).filter(w -> Math.abs(w) > 1e-6).count());
        System.out.println("      true  " + Arrays.toString(trueW));
        System.out.println("      ridge " + fmt(ridge));
        System.out.println("      lasso " + fmt(lasso));
    }

    static String fmt(double[] w) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < w.length; i++) s.append(i > 0 ? ", " : "").append(String.format("%.2f", w[i])); return s.append(']').toString(); }

    /** Minimise |y - Xw|^2 + l1 |w|_1 + l2 |w|^2 by coordinate descent (soft-thresholding for L1). */
    static double[] coordinateDescent(double[][] X, double[] y, double l1, double l2) {
        int n = X.length, d = X[0].length;
        double[] w = new double[d];
        for (int it = 0; it < 200; it++)
            for (int j = 0; j < d; j++) {
                double rho = 0, z = 0;
                for (int i = 0; i < n; i++) {
                    double pred = 0;
                    for (int k = 0; k < d; k++) if (k != j) pred += X[i][k] * w[k];
                    rho += X[i][j] * (y[i] - pred);
                    z += X[i][j] * X[i][j];
                }
                double soft = Math.signum(rho) * Math.max(0, Math.abs(rho) - l1 / 2);   // the L1 "snap to zero"
                w[j] = soft / (z + l2);
            }
        return w;
    }

    // =====================================================================
    // 10. saddle points
    // =====================================================================

    static void saddle() {
        System.out.println("10. Saddle points (f = x^2 - y^2 + y^4/4)");
        // gradient (2x, -2y + y^3); (0,0) is a saddle, minima at y = +-sqrt(2)
        double x = 1, y = 0;
        for (int i = 0; i < 1000; i++) { x -= 0.05 * 2 * x; y -= 0.05 * (-2 * y + y * y * y); }
        check("start exactly on the ridge y = 0: stuck at the saddle", "(0.0000, 0.0000)", "(" + f4(x) + ", " + f4(y) + ")");
        Random r = new Random(10);
        x = 1; y = 0;
        for (int i = 0; i < 1000; i++) { x -= 0.05 * 2 * x; y -= 0.05 * (-2 * y + y * y * y) + 1e-6 * r.nextGaussian(); }
        check("tiny noise (like SGD): escapes to y = +-sqrt(2)", "1.4142", f4(Math.abs(y)));
    }

    public static void main(String[] args) {
        convexity();
        learningRate();
        gdVariants();
        momentum();
        adaptiveMethods();
        newton();
        lagrange();
        linearProgramming();
        regularisation();
        saddle();
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

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
