/*
 * ============================================================================
 *  MATHS 09 : ADVANCED TOPICS FOR ML / AI
 * ============================================================================
 *  Run:  java AdvancedTopics.java          (explanations: README.md in this folder)
 *
 *    1. numerical methods   floating point, cancellation, Kahan sum, log-sum-exp, stable sigmoid
 *    2. spectral graphs     the Laplacian: zero eigenvalues = components; Fiedler vector = clusters
 *    3. MDPs / RL           value iteration on a grid; Q-learning finds the same policy
 *    4. Fourier             the DFT finds hidden frequencies
 *    5. convolution         blur and edge detection; convolution theorem
 *    6. high dimensions     distances concentrate, random vectors are nearly orthogonal
 *    7. kernels             the kernel trick: a dot product in a space you never build
 *    8. attention           softmax(Q K^T / sqrt(d)) V and why the sqrt(d)
 * ============================================================================
 */

import java.util.*;

public class AdvancedTopics {

    static String f4(double x) { return String.format("%.4f", x); }

    // =====================================================================
    // 1. numerical methods
    // =====================================================================

    static void numerical() {
        System.out.println("1. Numerical methods");
        check("0.1 + 0.2 == 0.3 ?", false, 0.1 + 0.2 == 0.3);
        check("0.1 + 0.2 is really", "0.30000000000000004", String.valueOf(0.1 + 0.2));
        check("compare with a tolerance instead", true, Math.abs(0.1 + 0.2 - 0.3) < 1e-9);
        check("machine epsilon for double (Math.ulp(1.0))", "2.220446049250313E-16", String.valueOf(Math.ulp(1.0)));
        check("1e16 + 1 - 1e16 (the 1 is lost)", 0.0, 1e16 + 1 - 1e16);
        float f = 16_777_216f;                                                    // 2^24
        check("float: 16,777,216 + 1 = 16,777,216", 16_777_216f, f + 1);
        // catastrophic cancellation: (1 - cos x)/x^2 should be 0.5 for tiny x
        double x = 1e-8;
        check("naive (1 - cos x)/x^2 at x = 1e-8 (should be 0.5)", 0.0, (1 - Math.cos(x)) / (x * x));
        check("rewritten as 2 sin^2(x/2)/x^2", "0.5000", f4(2 * Math.pow(Math.sin(x / 2), 2) / (x * x)));
        // summing 0.1 ten million times
        double naive = 0, kahan = 0, comp = 0;
        for (int i = 0; i < 10_000_000; i++) {
            naive += 0.1;
            double y = 0.1 - comp, t = kahan + y;                                  // Kahan: carry the lost low bits
            comp = (t - kahan) - y; kahan = t;
        }
        check("naive sum of 0.1 x 10^7 (error)", true, Math.abs(naive - 1_000_000) > 1e-6);
        check("Kahan summation (error < 1e-6)", true, Math.abs(kahan - 1_000_000) < 1e-6);
        System.out.printf("      naive = %.10f   kahan = %.10f%n", naive, kahan);
        // log-sum-exp and the stable softmax
        double[] big = {1000, 1001, 1002};
        double sum = 0;
        for (double v : big) sum += Math.exp(v);
        check("naive log(sum exp) of [1000,1001,1002] overflows", "Infinity", String.valueOf(Math.log(sum)));
        double m = 1002, s2 = 0;
        for (double v : big) s2 += Math.exp(v - m);
        check("log-sum-exp trick: m + log(sum exp(v - m))", "1002.4076", f4(m + Math.log(s2)));
        check("naive sigmoid(-1000) = 1/(1+e^1000)", "0.0", String.valueOf(1 / (1 + Math.exp(1000))));
        check("stable sigmoid(-1000) = e^z/(1+e^z) for z < 0", true, stableSigmoid(-1000) >= 0 && stableSigmoid(-40) > 0);
    }

    static double stableSigmoid(double z) { return z >= 0 ? 1 / (1 + Math.exp(-z)) : Math.exp(z) / (1 + Math.exp(z)); }

    // =====================================================================
    // 2. spectral graph theory
    // =====================================================================

    static double[][] laplacian(int n, int[][] edges) {
        double[][] L = new double[n][n];
        for (int[] e : edges) { L[e[0]][e[0]]++; L[e[1]][e[1]]++; L[e[0]][e[1]]--; L[e[1]][e[0]]--; }   // L = D - A
        return L;
    }

    static void spectral() {
        System.out.println("2. Spectral graph theory");
        int[][] twoTriangles = {{0, 1}, {1, 2}, {0, 2}, {3, 4}, {4, 5}, {3, 5}};   // two separate pieces
        double[] ev = eigenvalues(laplacian(6, twoTriangles));
        long zeros = Arrays.stream(ev).filter(v -> Math.abs(v) < 1e-9).count();
        check("Laplacian zero eigenvalues = number of components", 2L, zeros);
        // two dense clusters joined by ONE bridge edge: the Fiedler vector splits them
        int[][] bridged = {{0, 1}, {1, 2}, {0, 2}, {0, 3}, {1, 3}, {4, 5}, {5, 6}, {4, 6}, {4, 7}, {5, 7}, {3, 4}};
        Object[] eig = eigen(laplacian(8, bridged));
        double[] vals = (double[]) eig[0];
        double[][] vecs = (double[][]) eig[1];
        Integer[] order = {0, 1, 2, 3, 4, 5, 6, 7};
        Arrays.sort(order, Comparator.comparingDouble(i -> vals[i]));
        int fiedler = order[1];                                                   // 2nd smallest eigenvalue
        StringBuilder side = new StringBuilder();
        for (int v = 0; v < 8; v++) side.append(vecs[v][fiedler] > 0 ? '+' : '-');
        String s = side.toString();
        check("Fiedler vector signs split the two clusters", true, s.equals("++++----") || s.equals("----++++"));
        check("algebraic connectivity (small = easy to cut)", true, vals[fiedler] > 0 && vals[fiedler] < 1);
        System.out.printf("      signs %s, lambda2 = %.4f%n", s, vals[fiedler]);
    }

    // =====================================================================
    // 3. Markov decision processes and reinforcement learning
    // =====================================================================

    // 4x4 grid, start top-left, goal bottom-right (+10), pit at (1,2) (-10), each step -1
    static final int N = 4;
    static final int[][] MOVES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};               // up, down, left, right
    static final String ARROWS = "^v<>";

    static double reward(int r, int c) { return r == 3 && c == 3 ? 10 : r == 1 && c == 2 ? -10 : -1; }
    static boolean terminal(int r, int c) { return (r == 3 && c == 3) || (r == 1 && c == 2); }
    static int[] step(int r, int c, int a) { int nr = Math.max(0, Math.min(N - 1, r + MOVES[a][0])), nc = Math.max(0, Math.min(N - 1, c + MOVES[a][1])); return new int[]{nr, nc}; }

    static void reinforcement() {
        System.out.println("3. MDPs and reinforcement learning");
        double gamma = 0.9;
        double[][] V = new double[N][N];
        int sweeps = 0;
        while (true) {                                                           // value iteration (Bellman optimality)
            double delta = 0;
            double[][] nv = new double[N][N];
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++) {
                    if (terminal(r, c)) continue;
                    double best = Double.NEGATIVE_INFINITY;
                    for (int a = 0; a < 4; a++) { int[] s = step(r, c, a); best = Math.max(best, reward(s[0], s[1]) + (terminal(s[0], s[1]) ? 0 : gamma * V[s[0]][s[1]])); }
                    nv[r][c] = best;
                    delta = Math.max(delta, Math.abs(best - V[r][c]));
                }
            V = nv;
            sweeps++;
            if (delta < 1e-9) break;
        }
        String policy = policyString(V, gamma);
        check("value iteration converged (sweeps)", true, sweeps < 20);
        check("value of the start state = -1-0.9-0.81-0.729-0.6561+10*0.9^5", "1.8098", f4(V[0][0]));
        check("optimal policy avoids the pit (P) and reaches the goal (G)", "vv>v\nvvPv\nvvvv\n>>>G", policy);
        // Q-learning: no model, just trial and error
        Random rnd = new Random(3);
        double[][][] Q = new double[N][N][4];
        for (int ep = 0; ep < 5000; ep++) {
            int r = 0, c = 0;
            for (int t = 0; t < 100 && !terminal(r, c); t++) {
                int a = rnd.nextDouble() < 0.2 ? rnd.nextInt(4) : argmax(Q[r][c]);   // epsilon-greedy
                int[] s = step(r, c, a);
                double target = reward(s[0], s[1]) + (terminal(s[0], s[1]) ? 0 : gamma * Arrays.stream(Q[s[0]][s[1]]).max().getAsDouble());
                Q[r][c][a] += 0.1 * (target - Q[r][c][a]);                         // TD update
                r = s[0]; c = s[1];
            }
        }
        int r = 0, c = 0, steps = 0;
        while (!terminal(r, c) && steps < 20) { int[] s = step(r, c, argmax(Q[r][c])); r = s[0]; c = s[1]; steps++; }
        check("Q-learning's greedy path reaches the goal", "3,3", r + "," + c);
        check("...in the optimal 6 steps", 6, steps);
        check("Q(start, best action) ~ V(start)", true, Math.abs(Arrays.stream(Q[0][0]).max().getAsDouble() - V[0][0]) < 0.2);
    }

    static int argmax(double[] a) { int b = 0; for (int i = 1; i < a.length; i++) if (a[i] > a[b]) b = i; return b; }

    static String policyString(double[][] V, double gamma) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (r == 3 && c == 3) { sb.append('G'); continue; }
                if (r == 1 && c == 2) { sb.append('P'); continue; }
                int best = 0;
                double bv = Double.NEGATIVE_INFINITY;
                for (int a = 0; a < 4; a++) { int[] s = step(r, c, a); double q = reward(s[0], s[1]) + (terminal(s[0], s[1]) ? 0 : gamma * V[s[0]][s[1]]); if (q > bv + 1e-12) { bv = q; best = a; } }
                sb.append(ARROWS.charAt(best));
            }
            if (r < N - 1) sb.append('\n');
        }
        return sb.toString();
    }

    // =====================================================================
    // 4. Fourier transform, 5. convolution
    // =====================================================================

    static double[] dftMagnitudes(double[] x) {
        int n = x.length;
        double[] mag = new double[n / 2];
        for (int k = 0; k < n / 2; k++) {
            double re = 0, im = 0;
            for (int t = 0; t < n; t++) { re += x[t] * Math.cos(2 * Math.PI * k * t / n); im -= x[t] * Math.sin(2 * Math.PI * k * t / n); }
            mag[k] = Math.hypot(re, im) / n * 2;
        }
        return mag;
    }

    static double[] convolve(double[] a, double[] k) {
        double[] out = new double[a.length + k.length - 1];
        for (int i = 0; i < a.length; i++) for (int j = 0; j < k.length; j++) out[i + j] += a[i] * k[j];
        return out;
    }

    static void fourierAndConvolution() {
        System.out.println("4. Fourier transform  5. Convolution");
        int n = 256;
        double[] signal = new double[n];
        for (int t = 0; t < n; t++) signal[t] = 3 * Math.sin(2 * Math.PI * 5 * t / n) + 1 * Math.sin(2 * Math.PI * 20 * t / n);   // 5 Hz + 20 Hz
        double[] mag = dftMagnitudes(signal);
        List<Integer> peaks = new ArrayList<>();
        for (int k = 0; k < mag.length; k++) if (mag[k] > 0.5) peaks.add(k);
        check("DFT finds the hidden frequencies", "[5, 20]", peaks.toString());
        check("...with amplitudes 3 and 1", "3.0000, 1.0000", f4(mag[5]) + ", " + f4(mag[20]));
        double[] img = {0, 0, 0, 10, 10, 10, 0, 0, 0};                         // a 1-D "image" with a bright band
        double[] blur = convolve(img, new double[]{1 / 3.0, 1 / 3.0, 1 / 3.0});
        double[] edge = convolve(img, new double[]{1, -1});
        check("blur kernel [1/3,1/3,1/3] smooths the edges", "[0, 0, 0, 3.33, 6.67, 10, 6.67, 3.33, 0, 0, 0]", fmt(blur));
        check("edge kernel [1,-1] fires only at the edges", "[0, 0, 0, 10, 0, 0, -10, 0, 0, 0]", fmt(edge));
        // convolution = multiplying polynomials: (1 + 2x)(3 + x + x^2)
        check("convolution = polynomial multiplication", "[3, 7, 3, 2]", fmt(convolve(new double[]{1, 2}, new double[]{3, 1, 1})));
    }

    static String fmt(double[] a) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < a.length; i++) { double r = Math.round(a[i] * 100) / 100.0; s.append(i > 0 ? ", " : "").append(r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r)); } return s.append(']').toString(); }

    // =====================================================================
    // 6. high-dimensional geometry
    // =====================================================================

    static void highDimensions() {
        System.out.println("6. High-dimensional geometry");
        Random r = new Random(6);
        StringBuilder spread = new StringBuilder();
        for (int d : new int[]{2, 100, 10_000}) {
            double min = Double.MAX_VALUE, max = 0;
            double[] q = randomPoint(r, d);
            for (int i = 0; i < 200; i++) { double dist = distance(q, randomPoint(r, d)); min = Math.min(min, dist); max = Math.max(max, dist); }
            spread.append(String.format("d=%d: max/min=%.2f | ", d, max / min));
        }
        System.out.println("      " + spread);
        check("distances concentrate: max/min distance -> 1 as d grows", true, spread.toString().contains("d=10000: max/min=1.0"));
        double maxCos = 0;
        for (int i = 0; i < 100; i++) { double[] a = randomGaussian(r, 1000), b = randomGaussian(r, 1000); maxCos = Math.max(maxCos, Math.abs(cos(a, b))); }
        check("random 1000-d vectors are nearly perpendicular (|cos| < 0.15)", true, maxCos < 0.15);
        check("volume of the unit ball, d = 2 (pi)", "3.1416", f4(ballVolume(2)));
        check("volume of the unit ball, d = 20 (tiny!)", "0.0258", f4(ballVolume(20)));
        check("share of a cube's volume in its outer 1% shell, d = 1000", "1.0000", f4(1 - Math.pow(0.98, 1000)));
    }

    static double ballVolume(int d) { return Math.pow(Math.PI, d / 2.0) / gamma(d / 2.0 + 1); }
    static double gamma(double z) { return z == 1 ? 1 : z == 0.5 ? Math.sqrt(Math.PI) : (z - 1) * gamma(z - 1); }
    static double[] randomPoint(Random r, int d) { double[] p = new double[d]; for (int i = 0; i < d; i++) p[i] = r.nextDouble(); return p; }
    static double[] randomGaussian(Random r, int d) { double[] p = new double[d]; for (int i = 0; i < d; i++) p[i] = r.nextGaussian(); return p; }
    static double distance(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += (a[i] - b[i]) * (a[i] - b[i]); return Math.sqrt(s); }
    static double dot(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }
    static double cos(double[] a, double[] b) { return dot(a, b) / Math.sqrt(dot(a, a) * dot(b, b)); }

    // =====================================================================
    // 7. kernel methods
    // =====================================================================

    static void kernels() {
        System.out.println("7. Kernel methods");
        double[] x = {1, 2}, y = {3, 4};
        double[] phiX = {x[0] * x[0], Math.sqrt(2) * x[0] * x[1], x[1] * x[1]}, phiY = {y[0] * y[0], Math.sqrt(2) * y[0] * y[1], y[1] * y[1]};
        check("explicit features: phi(x) . phi(y)", "121.0000", f4(dot(phiX, phiY)));
        check("kernel trick: (x . y)^2, no features built", "121.0000", f4(Math.pow(dot(x, y), 2)));
        // XOR is not linearly separable in 2-D, but is with the feature x1*x2
        double[][] pts = {{1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        int[] label = {1, 1, -1, -1};
        boolean separable = true;
        for (int i = 0; i < 4; i++) separable &= Math.signum(pts[i][0] * pts[i][1]) == label[i];
        check("XOR becomes separable using the feature x1*x2", true, separable);
        // an RBF kernel matrix is positive semi-definite: z^T K z >= 0
        Random r = new Random(7);
        double[][] P = new double[30][2];
        for (double[] p : P) { p[0] = r.nextGaussian(); p[1] = r.nextGaussian(); }
        boolean psd = true;
        for (int t = 0; t < 1000; t++) {
            double[] z = randomGaussian(r, 30);
            double q = 0;
            for (int i = 0; i < 30; i++) for (int j = 0; j < 30; j++) q += z[i] * z[j] * Math.exp(-Math.pow(distance(P[i], P[j]), 2) / 2);
            psd &= q >= -1e-9;
        }
        check("RBF kernel matrix: z^T K z >= 0 for 1,000 random z", true, psd);
    }

    // =====================================================================
    // 8. attention
    // =====================================================================

    static void attention() {
        System.out.println("8. The maths of attention");
        Random r = new Random(8);
        for (int d : new int[]{16, 1024}) {
            double s = 0, s2 = 0;
            int trials = 20_000;
            for (int t = 0; t < trials; t++) { double v = dot(randomGaussian(r, d), randomGaussian(r, d)); s += v; s2 += v * v; }
            double sd = Math.sqrt(s2 / trials - Math.pow(s / trials, 2));
            check("std of q.k for d = " + d + " is sqrt(d)", String.format("%.0f", Math.sqrt(d)), String.format("%.0f", sd));
        }
        double[] scoresRaw = {32, 0, -32}, scaled = {1, 0, -1};                 // the same scores before and after / sqrt(1024)
        check("softmax of unscaled scores saturates (gradient ~ 0)", "1.0000", f4(softmax(scoresRaw)[0]));
        check("after dividing by sqrt(d): a healthy distribution", "0.6652", f4(softmax(scaled)[0]));
        // a tiny attention head: 3 tokens, d = 2
        double[][] Qm = {{1, 0}, {0, 1}, {1, 1}}, K = {{1, 0}, {0, 1}, {1, 1}}, Vm = {{10, 0}, {0, 10}, {5, 5}};
        double[] w = softmax(new double[]{dot(Qm[0], K[0]) / Math.sqrt(2), dot(Qm[0], K[1]) / Math.sqrt(2), dot(Qm[0], K[2]) / Math.sqrt(2)});
        double[] out = {w[0] * Vm[0][0] + w[1] * Vm[1][0] + w[2] * Vm[2][0], w[0] * Vm[0][1] + w[1] * Vm[1][1] + w[2] * Vm[2][1]};
        check("token 0 attention weights", "[0.40, 0.20, 0.40]", String.format("[%.2f, %.2f, %.2f]", w[0], w[1], w[2]));
        check("token 0 output = weighted mix of the values", "[6.02, 3.98]", String.format("[%.2f, %.2f]", out[0], out[1]));
    }

    static double[] softmax(double[] z) { double m = Arrays.stream(z).max().getAsDouble(), s = 0; double[] e = new double[z.length]; for (int i = 0; i < z.length; i++) { e[i] = Math.exp(z[i] - m); s += e[i]; } for (int i = 0; i < z.length; i++) e[i] /= s; return e; }

    // =====================================================================
    // a small symmetric eigen-solver (Jacobi), as in chapter 02
    // =====================================================================

    static double[] eigenvalues(double[][] S) { return (double[]) eigen(S)[0]; }

    static Object[] eigen(double[][] S) {
        int n = S.length;
        double[][] A = new double[n][], V = new double[n][n];
        for (int i = 0; i < n; i++) { A[i] = S[i].clone(); V[i][i] = 1; }
        for (int sweep = 0; sweep < 100; sweep++) {
            double off = 0;
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) off += A[i][j] * A[i][j];
            if (off < 1e-22) break;
            for (int p = 0; p < n; p++)
                for (int q = p + 1; q < n; q++) {
                    if (Math.abs(A[p][q]) < 1e-15) continue;
                    double theta = (A[q][q] - A[p][p]) / (2 * A[p][q]);
                    double t = theta == 0 ? 1 : Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
                    double c = 1 / Math.sqrt(t * t + 1), s = t * c;
                    for (int k = 0; k < n; k++) { double a = A[k][p], b = A[k][q]; A[k][p] = c * a - s * b; A[k][q] = s * a + c * b; }
                    for (int k = 0; k < n; k++) { double a = A[p][k], b = A[q][k]; A[p][k] = c * a - s * b; A[q][k] = s * a + c * b; }
                    for (int k = 0; k < n; k++) { double a = V[k][p], b = V[k][q]; V[k][p] = c * a - s * b; V[k][q] = s * a + c * b; }
                }
        }
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) vals[i] = A[i][i];
        return new Object[]{vals, V};
    }

    public static void main(String[] args) {
        numerical();
        spectral();
        reinforcement();
        fourierAndConvolution();
        highDimensions();
        kernels();
        attention();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a.replace("\n", " / "), e.equals(a) ? "" : "   (expected " + e.replace("\n", " / ") + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
