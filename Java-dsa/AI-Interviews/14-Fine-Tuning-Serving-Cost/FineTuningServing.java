/*
 * ============================================================================
 *  AI 14 : FINE-TUNING, SERVING AND COST
 * ============================================================================
 *  Run:  java FineTuningServing.java          (explanations: README.md in this folder)
 *
 *    1. LoRA size         how few parameters a low-rank adapter trains
 *    2. LoRA training     learn W0 + A.B by gradient descent; B starts at 0
 *    3. quantisation      int8 / int4 weights: memory vs error
 *    4. distillation      soft labels carry more information than hard labels
 *    5. batching          static vs continuous batching on a GPU
 *    6. cost              API price vs self-hosting break-even
 * ============================================================================
 */

import java.util.*;

public class FineTuningServing {

    // =====================================================================
    // 1. LoRA parameter counts
    // =====================================================================

    static void loraSize() {
        System.out.println("1. LoRA: how many parameters are trained");
        long d = 4096, r = 8;
        long full = d * d, lora = 2 * d * r;                                       // W is d x d; A is d x r, B is r x d
        check("one 4096x4096 weight matrix", 16_777_216L, full);
        check("its rank-8 LoRA adapter (A + B)", 65_536L, lora);
        check("...that's this share of the matrix", "0.39%", String.format("%.2f%%", 100.0 * lora / full));
        long adapters = 32 * 2 * lora;                                             // 32 layers, adapters on the q and v projections
        check("adapters on q,v in 32 layers of a 7B model", 4_194_304L, adapters);
        check("...as a share of 7B parameters", "0.06%", String.format("%.2f%%", 100.0 * adapters / 7e9));
        check("adapter file size in fp16", "8.4 MB", String.format("%.1f MB", adapters * 2 / 1e6));
    }

    // =====================================================================
    // 2. a tiny LoRA trained by gradient descent
    // =====================================================================

    static void loraTraining() {
        System.out.println("2. Training a tiny LoRA");
        Random rnd = new Random(2);
        int n = 8;
        double[][] w0 = new double[n][n];
        for (double[] row : w0) for (int j = 0; j < n; j++) row[j] = rnd.nextGaussian();           // the frozen "pre-trained" weights
        double[] u = new double[n], v = new double[n];
        for (int i = 0; i < n; i++) { u[i] = rnd.nextGaussian(); v[i] = rnd.nextGaussian(); }
        // the new task needs W* = W0 + u v^T  (a rank-1 change)
        double[] a = new double[n], b = new double[n];                                              // LoRA rank 1: deltaW = a b^T
        for (int i = 0; i < n; i++) a[i] = rnd.nextGaussian() * 0.1;                                 // A random, B = 0 -> starts exactly at W0
        double lr = 0.02, firstLoss = 0, lastLoss = 0;
        for (int step = 0; step < 3000; step++) {
            double[] x = new double[n];
            for (int i = 0; i < n; i++) x[i] = rnd.nextGaussian();
            double ux = dot(v, x), bx = dot(b, x);
            double[] err = new double[n];
            double loss = 0;
            for (int i = 0; i < n; i++) { double target = dot(w0[i], x) + u[i] * ux, pred = dot(w0[i], x) + a[i] * bx; err[i] = pred - target; loss += err[i] * err[i] / n; }
            if (step == 0) firstLoss = loss;
            lastLoss = loss;
            double ea = dot(err, a);
            for (int i = 0; i < n; i++) { double ga = 2.0 / n * err[i] * bx, gb = 2.0 / n * ea * x[i]; a[i] -= lr * ga; b[i] -= lr * gb; }   // only A and B learn
        }
        check("at the start (B = 0) the model IS W0, so the loss is clearly non-zero", true, firstLoss > 0.1);
        check("after training, the adapter captured the change", true, lastLoss < 1e-6);
        System.out.printf("      loss %.3f -> %.2e; trained %d numbers instead of %d%n", firstLoss, lastLoss, 2 * n, n * n);
        double maxDiff = 0;
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) maxDiff = Math.max(maxDiff, Math.abs(a[i] * b[j] - u[i] * v[j]));
        check("the learned a.b^T equals the true change u.v^T", true, maxDiff < 1e-3);
        double[][] merged = new double[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) merged[i][j] = w0[i][j] + a[i] * b[j];   // fold the adapter into the weights
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = rnd.nextGaussian();
        double gap = 0;
        for (int i = 0; i < n; i++) gap = Math.max(gap, Math.abs(dot(merged[i], x) - (dot(w0[i], x) + a[i] * dot(b, x))));
        check("merged W0 + a.b^T gives the same output (no extra latency)", true, gap < 1e-12);
    }

    static double dot(double[] x, double[] y) { double s = 0; for (int i = 0; i < x.length; i++) s += x[i] * y[i]; return s; }

    // =====================================================================
    // 3. quantisation
    // =====================================================================

    static double quantError(double[] w, int bits) {                          // symmetric per-tensor quantisation
        double max = Arrays.stream(w).map(Math::abs).max().getAsDouble();
        int levels = (1 << (bits - 1)) - 1;                                     // int8 -> 127, int4 -> 7
        double scale = max / levels, err = 0;
        for (double x : w) { double q = Math.round(x / scale) * scale; err += (q - x) * (q - x); }
        return Math.sqrt(err / w.length) / Math.sqrt(Arrays.stream(w).map(x -> x * x).sum() / w.length);   // relative RMS error
    }

    static void quantisation() {
        System.out.println("3. Quantisation");
        Random rnd = new Random(3);
        double[] w = new double[100_000];
        for (int i = 0; i < w.length; i++) w[i] = rnd.nextGaussian() * 0.02;
        double e8 = quantError(w, 8), e4 = quantError(w, 4);
        System.out.printf("      relative error: int8 %.4f, int4 %.4f%n", e8, e4);
        check("int8 error is small (< 2%)", true, e8 < 0.02);
        check("int4 error is much larger (~ 10x int8)", true, e4 > 5 * e8);
        double[] outlier = w.clone();
        outlier[0] = 1.0;                                                        // one huge weight stretches the scale for everyone
        check("one outlier weight wrecks per-tensor int8 accuracy", true, quantError(outlier, 8) > 5 * e8);
        System.out.printf("      int8 with one outlier: %.4f (why real schemes use per-channel/per-group scales)%n", quantError(outlier, 8));
    }

    // =====================================================================
    // 4. distillation
    // =====================================================================

    static double[] softmax(double[] z, double t) { double m = Arrays.stream(z).max().getAsDouble(), s = 0; double[] e = new double[z.length]; for (int i = 0; i < z.length; i++) { e[i] = Math.exp((z[i] - m) / t); s += e[i]; } for (int i = 0; i < z.length; i++) e[i] /= s; return e; }

    static void distillation() {
        System.out.println("4. Distillation");
        double[] teacherLogits = {6.0, 4.5, 0.5, 0.2};                            // cat, lynx, car, toaster
        double[] hard = {1, 0, 0, 0};
        double[] soft1 = softmax(teacherLogits, 1), soft4 = softmax(teacherLogits, 4);
        check("hard label: 'cat', nothing else", "[1.0, 0.0, 0.0, 0.0]", Arrays.toString(hard));
        check("teacher at T=1: lynx is the runner-up", "[0.81, 0.18, 0.00, 0.00]", fmt(soft1));
        check("teacher at T=4: 'dark knowledge' visible (lynx >> car)", "[0.46, 0.32, 0.12, 0.11]", fmt(soft4));
        check("soft labels carry more information (entropy in bits)", true, entropy(soft4) > entropy(soft1) && entropy(hard) == 0);
    }

    static String fmt(double[] p) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < p.length; i++) s.append(i > 0 ? ", " : "").append(String.format("%.2f", p[i])); return s.append(']').toString(); }
    static double entropy(double[] p) { double h = 0; for (double x : p) if (x > 0) h -= x * Math.log(x) / Math.log(2); return h; }

    // =====================================================================
    // 5. static vs continuous batching
    // =====================================================================

    static void batching() {
        System.out.println("5. Static vs continuous batching");
        Random rnd = new Random(5);
        int requests = 64, slots = 8;
        int[] len = new int[requests];
        for (int i = 0; i < requests; i++) len[i] = 10 + rnd.nextInt(390);          // output lengths 10..400 tokens
        int totalTokens = Arrays.stream(len).sum();
        int staticSteps = 0;
        for (int i = 0; i < requests; i += slots) { int max = 0; for (int j = i; j < i + slots; j++) max = Math.max(max, len[j]); staticSteps += max; }   // wait for the longest
        PriorityQueue<Integer> running = new PriorityQueue<>();                     // finish times
        int next = 0, t = 0, contSteps = 0;
        while (next < requests || !running.isEmpty()) {
            while (running.size() < slots && next < requests) running.add(t + len[next++]);   // refill a free slot immediately
            int finish = running.poll();
            t = finish;
            while (!running.isEmpty() && running.peek() == t) running.poll();
            contSteps = t;
        }
        double staticUtil = totalTokens / (double) (staticSteps * slots), contUtil = totalTokens / (double) (contSteps * slots);
        System.out.printf("      decode steps: static %d, continuous %d; GPU slot utilisation %.0f%% vs %.0f%%%n", staticSteps, contSteps, 100 * staticUtil, 100 * contUtil);
        check("continuous batching finishes the same work in fewer steps", true, contSteps < staticSteps);
        check("...and keeps the GPU busier", true, contUtil > staticUtil + 0.1);
    }

    // =====================================================================
    // 6. API vs self-hosting
    // =====================================================================

    static void cost() {
        System.out.println("6. API vs self-hosting");
        double gpuPerHour = 4.0, tokensPerSec = 2_500;                              // a GPU serving a mid-size open model, with batching
        double selfPerM = gpuPerHour / (tokensPerSec * 3600 / 1e6);
        check("self-hosted cost per 1M tokens at FULL utilisation", "$0.44", String.format("$%.2f", selfPerM));
        check("...at 20% utilisation (idle nights, spiky traffic)", "$2.22", String.format("$%.2f", selfPerM / 0.2));
        double apiPerM = 1.0;                                                       // an API price for a comparable model
        double monthlyGpu = gpuPerHour * 24 * 30;
        check("one GPU 24/7 per month", "$2880", String.format("$%.0f", monthlyGpu));
        check("break-even volume vs the API (tokens/month)", "2880M", String.format("%.0fM", monthlyGpu / apiPerM));
        check("...which needs this utilisation of the GPU", "44%", String.format("%.0f%%", 100 * 2880e6 / (tokensPerSec * 3600 * 24 * 30)));
    }

    public static void main(String[] args) {
        loraSize();
        loraTraining();
        quantisation();
        distillation();
        batching();
        cost();
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

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
