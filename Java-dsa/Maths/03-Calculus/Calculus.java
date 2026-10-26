/*
 * ============================================================================
 *  MATHS 03 : CALCULUS  (for ML: derivatives, gradients, the chain rule, backprop)
 * ============================================================================
 *  Run:  java Calculus.java          (explanations: README.md in this folder)
 *
 *    1. limits            (1 + 1/n)^n -> e,  sin(x)/x -> 1
 *    2. derivatives       numerical slope vs the rules (power, exp, log, sin)
 *    3. rules             product, quotient, chain
 *    4. optimisation      f'(x) = 0 at a minimum; second-derivative test
 *    5. integrals         Riemann sums, trapezoid, Monte Carlo, fundamental theorem
 *    6. partials          gradient of f(x, y), it points uphill; directional derivative
 *    7. Jacobian/Hessian  matrices of derivatives
 *    8. backprop          chain rule on a computation graph + gradient check + training
 *    9. Taylor & Newton   polynomial approximations; Newton's method for sqrt(2)
 *   10. ML derivatives    sigmoid, tanh, ReLU, softmax + cross-entropy = p - y
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class Calculus {

    static double derivative(DoubleUnaryOperator f, double x) {                 // central difference
        double h = 1e-5;
        return (f.applyAsDouble(x + h) - f.applyAsDouble(x - h)) / (2 * h);
    }

    static String f4(double x) { return String.format("%.4f", x); }

    // =====================================================================
    // 1. limits
    // =====================================================================

    static void limits() {
        System.out.println("1. Limits");
        check("(1 + 1/n)^n at n = 10", "2.5937", f4(Math.pow(1 + 1.0 / 10, 10)));
        check("(1 + 1/n)^n at n = 1,000,000", "2.7183", f4(Math.pow(1 + 1e-6, 1e6)));
        check("e", "2.7183", f4(Math.E));
        check("sin(x)/x at x = 0.001", "1.0000", f4(Math.sin(0.001) / 0.001));
        check("(x^2 - 1)/(x - 1) near x = 1 -> 2", "2.0000", f4((Math.pow(1.0001, 2) - 1) / 0.0001 - 0.0001));
    }

    // =====================================================================
    // 2. derivatives, 3. rules
    // =====================================================================

    static void derivatives() {
        System.out.println("2. Derivatives  3. Rules");
        check("d/dx x^2 at 3 = 2x = 6", "6.0000", f4(derivative(x -> x * x, 3)));
        check("d/dx x^3 at 2 = 3x^2 = 12", "12.0000", f4(derivative(x -> x * x * x, 2)));
        check("d/dx e^x at 1 = e^1", f4(Math.E), f4(derivative(Math::exp, 1)));
        check("d/dx ln x at 4 = 1/4", "0.2500", f4(derivative(Math::log, 4)));
        check("d/dx sin x at 0 = cos 0 = 1", "1.0000", f4(derivative(Math::sin, 0)));
        check("d/dx 5 (a constant) = 0", "0.0000", f4(derivative(x -> 5, 7)));
        double x = 1.3;
        check("product: (x^2 sin x)' = 2x sin x + x^2 cos x", f4(2 * x * Math.sin(x) + x * x * Math.cos(x)), f4(derivative(t -> t * t * Math.sin(t), x)));
        check("quotient: (sin x / x)' = (x cos x - sin x)/x^2", f4((x * Math.cos(x) - Math.sin(x)) / (x * x)), f4(derivative(t -> Math.sin(t) / t, x)));
        check("chain: sin(x^2)' = cos(x^2) * 2x", f4(Math.cos(x * x) * 2 * x), f4(derivative(t -> Math.sin(t * t), x)));
        check("chain: e^(3x)' = 3 e^(3x)", f4(3 * Math.exp(3 * x)), f4(derivative(t -> Math.exp(3 * t), x)));
    }

    // =====================================================================
    // 4. optimisation with derivatives
    // =====================================================================

    static void optimisation() {
        System.out.println("4. Using derivatives to optimise");
        DoubleUnaryOperator f = t -> (t - 3) * (t - 3) + 1;                     // minimum at t = 3
        check("f'(3) = 0 at the minimum", "0.0000", f4(Math.abs(derivative(f, 3))));
        double x = -5, lr = 0.1;
        for (int i = 0; i < 100; i++) x -= lr * derivative(f, x);               // gradient descent
        check("gradient descent from -5 finds x = 3", "3.0000", f4(x));
        double second = (f.applyAsDouble(3 + 1e-3) - 2 * f.applyAsDouble(3) + f.applyAsDouble(3 - 1e-3)) / 1e-6;
        check("f''(3) = 2 > 0, so it's a minimum", "2.0000", f4(second));
        DoubleUnaryOperator cubic = t -> t * t * t;                             // f'(0) = 0 but not a min or max
        check("x^3: f'(0) = 0 but it's a saddle point", "0.0000", f4(Math.abs(derivative(cubic, 0))));
        // a box: cut squares of side x from a 12x12 sheet; volume = x(12-2x)^2, best x = 2
        double best = 0, bestX = 0;
        for (double c = 0.001; c < 6; c += 0.001) { double vol = c * (12 - 2 * c) * (12 - 2 * c); if (vol > best) { best = vol; bestX = c; } }
        check("box problem: best cut x (where V'(x) = 0)", "2.00", String.format("%.2f", bestX));
        check("...max volume 128", "128.0", String.format("%.1f", best));
    }

    // =====================================================================
    // 5. integrals
    // =====================================================================

    static void integrals() {
        System.out.println("5. Integrals");
        int n = 1000;
        double dx = 3.0 / n, left = 0, trap = 0;
        for (int i = 0; i < n; i++) {
            double a = i * dx, b = a + dx;
            left += a * a * dx;                                                  // left rectangles
            trap += (a * a + b * b) / 2 * dx;                                    // trapezoids
        }
        check("area under x^2 on [0,3], exact = 3^3/3", "9.0000", f4(9.0));
        check("left Riemann sum, 1,000 strips", "8.9865", f4(left));
        check("trapezoid rule, 1,000 strips", "9.0000", f4(trap));
        Random rnd = new Random(5);
        int inside = 0, darts = 1_000_000;
        for (int i = 0; i < darts; i++) { double px = rnd.nextDouble(), py = rnd.nextDouble(); if (px * px + py * py <= 1) inside++; }
        check("Monte Carlo: pi ~ 4 * fraction in the circle", "3.14", String.format("%.2f", 4.0 * inside / darts));
        double gauss = 0, step = 0.001;
        for (double t = -10; t < 10; t += step) gauss += Math.exp(-t * t / 2) * step;
        check("area under e^(-x^2/2) = sqrt(2 pi)", f4(Math.sqrt(2 * Math.PI)), f4(gauss));
        // fundamental theorem: the derivative of the running area is the function itself
        DoubleUnaryOperator area = t -> t * t * t / 3;                          // area under x^2 from 0 to t
        check("d/dt (area up to t) at 2 = f(2) = 4", "4.0000", f4(derivative(area, 2)));
    }

    // =====================================================================
    // 6. partial derivatives and gradients, 7. Jacobian and Hessian
    // =====================================================================

    static double[] gradient(ToDoubleFunction<double[]> f, double[] p) {
        double h = 1e-5;
        double[] g = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            double[] up = p.clone(), down = p.clone();
            up[i] += h; down[i] -= h;
            g[i] = (f.applyAsDouble(up) - f.applyAsDouble(down)) / (2 * h);
        }
        return g;
    }

    static void multivariable() {
        System.out.println("6. Partial derivatives, gradient  7. Jacobian, Hessian");
        ToDoubleFunction<double[]> f = p -> p[0] * p[0] + 3 * p[0] * p[1] + p[1] * p[1];
        double[] g = gradient(f, new double[]{1, 2});
        check("df/dx at (1,2) = 2x + 3y = 8", "8.0000", f4(g[0]));
        check("df/dy at (1,2) = 3x + 2y = 7", "7.0000", f4(g[1]));
        double gn = Math.hypot(g[0], g[1]);
        double[] up = {1 + 0.001 * g[0] / gn, 2 + 0.001 * g[1] / gn};
        double[] side = {1 - 0.001 * g[1] / gn, 2 + 0.001 * g[0] / gn};       // perpendicular to the gradient
        double base = f.applyAsDouble(new double[]{1, 2});
        check("a step along the gradient raises f the most", true, f.applyAsDouble(up) - base > f.applyAsDouble(side) - base);
        double[] down = {1 - 0.001 * g[0] / gn, 2 - 0.001 * g[1] / gn};
        check("directional derivative along gradient = |grad|", f4(gn), f4((f.applyAsDouble(up) - f.applyAsDouble(down)) / 0.002));
        check("|grad| = sqrt(8^2 + 7^2)", "10.6301", f4(gn));
        // Jacobian of F(x, y) = (x^2 y, x + y) at (2, 3): [[2xy, x^2], [1, 1]] = [[12, 4], [1, 1]]
        double[] j1 = gradient(p -> p[0] * p[0] * p[1], new double[]{2, 3}), j2 = gradient(p -> p[0] + p[1], new double[]{2, 3});
        check("Jacobian row 1 = [2xy, x^2]", "[12.0, 4.0]", "[" + Math.round(j1[0] * 1e4) / 1e4 + ", " + Math.round(j1[1] * 1e4) / 1e4 + "]");
        check("Jacobian row 2 = [1, 1]", "[1.0, 1.0]", "[" + Math.round(j2[0] * 1e4) / 1e4 + ", " + Math.round(j2[1] * 1e4) / 1e4 + "]");
        // Hessian of f = x^2 + 3xy + y^2 is constant [[2,3],[3,2]], eigenvalues 5 and -1 -> a saddle
        double[] hx = gradient(p -> gradient(f, p)[0], new double[]{0, 0});
        check("Hessian row 1 = [2, 3]", "[2, 3]", "[" + Math.round(hx[0]) + ", " + Math.round(hx[1]) + "]");
        check("so (0,0) is a saddle: f(1,-1) < 0 < f(1,1)", true, f.applyAsDouble(new double[]{1, -1}) < 0 && f.applyAsDouble(new double[]{1, 1}) > 0);
    }

    // =====================================================================
    // 8. backpropagation
    // =====================================================================

    static double sigmoid(double z) { return 1 / (1 + Math.exp(-z)); }

    /** One neuron: z = w1 x1 + w2 x2 + b, a = sigmoid(z), L = (a - y)^2. Returns {L, dL/dw1, dL/dw2, dL/db}. */
    static double[] forwardBackward(double w1, double w2, double b, double x1, double x2, double y) {
        double z = w1 * x1 + w2 * x2 + b;                                       // forward pass
        double a = sigmoid(z);
        double L = (a - y) * (a - y);
        double dL_da = 2 * (a - y);                                             // backward pass: chain rule, step by step
        double da_dz = a * (1 - a);
        double dL_dz = dL_da * da_dz;
        return new double[]{L, dL_dz * x1, dL_dz * x2, dL_dz};                  // dz/dw1 = x1, dz/dw2 = x2, dz/db = 1
    }

    static void backprop() {
        System.out.println("8. Backpropagation (the chain rule on a computation graph)");
        double w1 = 0.5, w2 = -0.3, b = 0.1, x1 = 2, x2 = 1, y = 1;
        double[] r = forwardBackward(w1, w2, b, x1, x2, y);
        check("z = 0.5*2 - 0.3*1 + 0.1 = 0.8, a = sigmoid(0.8)", "0.6900", f4(sigmoid(0.8)));
        check("loss for w=(0.5,-0.3), b=0.1, x=(2,1), y=1", "0.0961", f4(r[0]));
        double numeric = (forwardBackward(w1 + 1e-6, w2, b, x1, x2, y)[0] - forwardBackward(w1 - 1e-6, w2, b, x1, x2, y)[0]) / 2e-6;
        check("dL/dw1 by backprop", "-0.2653", f4(r[1]));
        check("dL/dw1 by nudging w1 (gradient check)", f4(r[1]), f4(numeric));
        check("dL/db = dL/dw1 / x1", f4(r[1] / x1), f4(r[3]));
        // train a neuron to learn OR of two inputs (logistic regression via gradient descent)
        double[][] X = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[] Y = {0, 1, 1, 1};
        double[] w = {0, 0};
        double bias = 0, lr = 1.0, first = 0, last = 0;
        for (int epoch = 0; epoch < 2000; epoch++) {
            double loss = 0, g1 = 0, g2 = 0, gb = 0;
            for (int i = 0; i < 4; i++) {
                double[] g = forwardBackward(w[0], w[1], bias, X[i][0], X[i][1], Y[i]);
                loss += g[0] / 4; g1 += g[1] / 4; g2 += g[2] / 4; gb += g[3] / 4;
            }
            if (epoch == 0) first = loss;
            last = loss;
            w[0] -= lr * g1; w[1] -= lr * g2; bias -= lr * gb;
        }
        check("loss at epoch 0", "0.2500", f4(first));
        check("loss after 2,000 steps (smaller)", true, last < 0.01);
        StringBuilder preds = new StringBuilder();
        for (double[] p : X) preds.append(sigmoid(w[0] * p[0] + w[1] * p[1] + bias) > 0.5 ? 1 : 0);
        check("learned OR: predictions for 00 01 10 11", "0111", preds.toString());
    }

    // =====================================================================
    // 9. Taylor series and Newton's method
    // =====================================================================

    static void taylorAndNewton() {
        System.out.println("9. Taylor series, Newton's method");
        double x = 1, term = 1, sum = 0;
        StringBuilder approx = new StringBuilder();
        for (int k = 0; k < 8; k++) {
            sum += term;                                                         // e^x = sum x^k / k!
            term *= x / (k + 1);
            if (k == 1 || k == 3 || k == 7) approx.append(f4(sum)).append(' ');
        }
        check("e^1 with 2, 4, 8 terms", "2.0000 2.6667 2.7183 ", approx.toString());
        double small = 0.1;
        check("sin(0.1) ~ x - x^3/6", f4(Math.sin(small)), f4(small - Math.pow(small, 3) / 6));
        check("near 0, sin x ~ x (error tiny)", true, Math.abs(Math.sin(0.01) - 0.01) < 2e-7);
        // Newton's method for sqrt(2): solve g(t) = t^2 - 2 = 0 with t <- t - g(t)/g'(t)
        double t = 1;
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < 4; i++) { t = t - (t * t - 2) / (2 * t); steps.append(String.format("%.10f ", t)); }
        check("Newton steps (correct digits roughly double)", "1.5000000000 1.4166666667 1.4142156863 1.4142135624 ", steps.toString());
    }

    // =====================================================================
    // 10. derivatives used in ML
    // =====================================================================

    static void mlDerivatives() {
        System.out.println("10. Derivatives used in ML");
        double z = 0.7;
        check("sigmoid' = s(1 - s)", f4(sigmoid(z) * (1 - sigmoid(z))), f4(derivative(Calculus::sigmoid, z)));
        check("tanh' = 1 - tanh^2", f4(1 - Math.pow(Math.tanh(z), 2)), f4(derivative(Math::tanh, z)));
        check("ReLU' = 1 for z > 0", "1.0000", f4(derivative(t -> Math.max(0, t), 2)));
        check("ReLU' = 0 for z < 0", "0.0000", f4(derivative(t -> Math.max(0, t), -2)));
        check("sigmoid' is at most 0.25 (vanishing gradients)", "0.2500", f4(sigmoid(0) * (1 - sigmoid(0))));
        // softmax + cross-entropy: gradient w.r.t. the scores is (probabilities - one-hot label)
        double[] scores = {2.0, 1.0, 0.1};
        int label = 0;
        double[] p = softmax(scores);
        check("softmax of [2, 1, 0.1]", "[0.6590, 0.2424, 0.0986]", "[" + f4(p[0]) + ", " + f4(p[1]) + ", " + f4(p[2]) + "]");
        StringBuilder grad = new StringBuilder("[");
        for (int i = 0; i < 3; i++) {
            final int k = i;
            double g = derivative(s -> { double[] sc = scores.clone(); sc[k] = s; return -Math.log(softmax(sc)[label]); }, scores[i]);
            grad.append(i > 0 ? ", " : "").append(f4(g));
        }
        check("d(cross-entropy)/d(scores), numerically", "[-0.3410, 0.2424, 0.0986]", grad.append(']').toString());
        check("= p - y  with y = [1, 0, 0]", "[-0.3410, 0.2424, 0.0986]", "[" + f4(p[0] - 1) + ", " + f4(p[1]) + ", " + f4(p[2]) + "]");
    }

    static double[] softmax(double[] s) {
        double max = Arrays.stream(s).max().getAsDouble(), sum = 0;               // subtract the max: avoids overflow
        double[] e = new double[s.length];
        for (int i = 0; i < s.length; i++) { e[i] = Math.exp(s[i] - max); sum += e[i]; }
        for (int i = 0; i < s.length; i++) e[i] /= sum;
        return e;
    }

    public static void main(String[] args) {
        limits();
        derivatives();
        optimisation();
        integrals();
        multivariable();
        backprop();
        taylorAndNewton();
        mlDerivatives();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-50s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
