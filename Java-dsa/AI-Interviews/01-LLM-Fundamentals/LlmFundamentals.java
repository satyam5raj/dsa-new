/*
 * ============================================================================
 *  AI 01 : LLM FUNDAMENTALS  (how a language model works, in runnable miniature)
 * ============================================================================
 *  Run:  java LlmFundamentals.java          (explanations: README.md in this folder)
 *
 *    1. tokenisation     train a tiny BPE tokenizer: merges, vocabulary, token counts
 *    2. next-token       a bigram "language model" learned by counting
 *    3. sampling         greedy, temperature, top-k, top-p (nucleus)
 *    4. context window   token budgets and what gets cut
 *    5. KV cache         why generation is cheap per token once the prompt is cached
 *    6. memory & cost    weights, KV cache size, and $ per request
 * ============================================================================
 */

import java.util.*;

public class LlmFundamentals {

    static String f2(double x) { return String.format("%.2f", x); }

    // =====================================================================
    // 1. byte-pair encoding (BPE), the tokenizer family GPT/Llama/Claude-style models use
    // =====================================================================

    /** Learns `numMerges` merges from a corpus. Words are split into characters plus an end marker '_'. */
    static List<String[]> trainBpe(String corpus, int numMerges) {
        List<List<String>> words = new ArrayList<>();
        for (String w : corpus.split("\\s+")) { List<String> sym = new ArrayList<>(); for (char c : w.toCharArray()) sym.add(String.valueOf(c)); sym.add("_"); words.add(sym); }
        List<String[]> merges = new ArrayList<>();
        for (int m = 0; m < numMerges; m++) {
            Map<String, Integer> pairCount = new LinkedHashMap<>();
            for (List<String> w : words) for (int i = 0; i + 1 < w.size(); i++) pairCount.merge(w.get(i) + " " + w.get(i + 1), 1, Integer::sum);
            if (pairCount.isEmpty()) break;
            String best = Collections.max(pairCount.entrySet(), Map.Entry.comparingByValue()).getKey();   // most frequent adjacent pair
            String[] pair = best.split(" ");
            merges.add(pair);
            for (List<String> w : words) applyMerge(w, pair);
        }
        return merges;
    }

    static void applyMerge(List<String> w, String[] pair) {
        for (int i = 0; i + 1 < w.size(); i++)
            if (w.get(i).equals(pair[0]) && w.get(i + 1).equals(pair[1])) { w.set(i, pair[0] + pair[1]); w.remove(i + 1); }
    }

    static List<String> tokenize(String text, List<String[]> merges) {
        List<String> out = new ArrayList<>();
        for (String w : text.split("\\s+")) {
            List<String> sym = new ArrayList<>();
            for (char c : w.toCharArray()) sym.add(String.valueOf(c));
            sym.add("_");
            for (String[] m : merges) applyMerge(sym, m);                            // apply merges in the learned order
            out.addAll(sym);
        }
        return out;
    }

    static void tokenisation() {
        System.out.println("1. Tokenisation (byte-pair encoding)");
        String corpus = "low lower lowest low low newer newest new new wider widest wide";
        List<String[]> merges = trainBpe(corpus, 10);
        check("first merge = most frequent pair", "[l, o]", Arrays.toString(merges.get(0)));
        List<String> known = tokenize("lowest newest", merges);
        List<String> unknown = tokenize("slowest", merges);
        check("'lowest newest' -> sub-word pieces", "[low, est_, new, est_]", known.toString());
        check("'slowest' (never seen) still works via sub-pieces", "[s, low, est_]", unknown.toString());
        check("characters vs BPE tokens for 'lowest newest'", "14 vs 4", (("lowest newest").replace(" ", "").length() + 2) + " vs " + known.size());
        check("rule of thumb: 1,000 English words ~ 1,300 tokens", 1300, (int) Math.round(1000 * 1.3));
    }

    // =====================================================================
    // 2. next-token prediction with a bigram model
    // =====================================================================

    static Map<String, Map<String, Integer>> bigram(String text) {
        String[] t = text.split("\\s+");
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        for (int i = 0; i + 1 < t.length; i++) counts.computeIfAbsent(t[i], k -> new TreeMap<>()).merge(t[i + 1], 1, Integer::sum);
        return counts;
    }

    static void nextToken() {
        System.out.println("2. Next-token prediction");
        String text = "the cat sat on the mat . the cat ate the fish . the dog sat on the rug .";
        Map<String, Map<String, Integer>> m = bigram(text);
        Map<String, Integer> afterThe = m.get("the");
        int total = afterThe.values().stream().mapToInt(Integer::intValue).sum();
        check("P(next | 'the'): counts", "{cat=2, dog=1, fish=1, mat=1, rug=1}", afterThe.toString());
        check("P('cat' | 'the') = 2/6", "0.33", f2(afterThe.get("cat") / (double) total));
        StringBuilder greedy = new StringBuilder("the");
        String cur = "the";
        for (int i = 0; i < 5; i++) { cur = Collections.max(m.get(cur).entrySet(), Map.Entry.comparingByValue()).getKey(); greedy.append(' ').append(cur); }
        check("greedy from 'the' (ties -> first; it loops!)", "the cat ate the cat ate", greedy.toString());
    }

    // =====================================================================
    // 3. sampling: temperature, top-k, top-p
    // =====================================================================

    static double[] softmax(double[] logits, double temperature) {
        double max = Arrays.stream(logits).max().getAsDouble(), s = 0;
        double[] p = new double[logits.length];
        for (int i = 0; i < p.length; i++) { p[i] = Math.exp((logits[i] - max) / temperature); s += p[i]; }
        for (int i = 0; i < p.length; i++) p[i] /= s;
        return p;
    }

    static double[] topK(double[] p, int k) {
        Integer[] idx = new Integer[p.length];
        for (int i = 0; i < p.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(p[b], p[a]));
        double[] q = new double[p.length];
        double s = 0;
        for (int i = 0; i < k; i++) { q[idx[i]] = p[idx[i]]; s += p[idx[i]]; }
        for (int i = 0; i < q.length; i++) q[i] /= s;                                    // renormalise the survivors
        return q;
    }

    static double[] topP(double[] p, double threshold) {
        Integer[] idx = new Integer[p.length];
        for (int i = 0; i < p.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(p[b], p[a]));
        double[] q = new double[p.length];
        double cum = 0, s = 0;
        for (Integer i : idx) { if (cum >= threshold) break; q[i] = p[i]; cum += p[i]; s += p[i]; }   // smallest set reaching threshold
        for (int i = 0; i < q.length; i++) q[i] /= s;
        return q;
    }

    static String probs(double[] p) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < p.length; i++) s.append(i > 0 ? ", " : "").append(f2(p[i])); return s.append(']').toString(); }

    static void sampling() {
        System.out.println("3. Sampling");
        double[] logits = {4.0, 3.0, 2.0, 1.0, 0.0};                                  // tokens: Paris, Lyon, Nice, banana, purple
        check("T = 1.0", "[0.64, 0.23, 0.09, 0.03, 0.01]", probs(softmax(logits, 1.0)));
        check("T = 0.5 (sharper, more deterministic)", "[0.86, 0.12, 0.02, 0.00, 0.00]", probs(softmax(logits, 0.5)));
        check("T = 2.0 (flatter, more creative/random)", "[0.43, 0.26, 0.16, 0.10, 0.06]", probs(softmax(logits, 2.0)));
        check("top-k = 2 keeps only the best 2", "[0.73, 0.27, 0.00, 0.00, 0.00]", probs(topK(softmax(logits, 1.0), 2)));
        check("top-p = 0.9 keeps the smallest set with 90% mass", "[0.67, 0.24, 0.09, 0.00, 0.00]", probs(topP(softmax(logits, 1.0), 0.9)));
        Random r = new Random(1);
        double[] p = softmax(logits, 1.0);
        int[] hits = new int[5];
        for (int i = 0; i < 100_000; i++) { double u = r.nextDouble(), c = 0; for (int k = 0; k < 5; k++) { c += p[k]; if (u < c) { hits[k]++; break; } } }
        check("sampling 100,000 times picks 'banana' ~3% of the time", true, Math.abs(hits[3] / 100_000.0 - p[3]) < 0.003);
    }

    // =====================================================================
    // 4. context window budgeting
    // =====================================================================

    static void contextWindow() {
        System.out.println("4. Context window");
        int window = 8_000, system = 500, maxOutput = 1_000, question = 200;
        int forHistoryAndDocs = window - system - maxOutput - question;
        check("tokens left for history + retrieved docs", 6_300, forHistoryAndDocs);
        int[] turns = {900, 1200, 700, 1500, 800, 1100, 600, 1300};                   // oldest -> newest
        int used = 0, kept = 0;
        for (int i = turns.length - 1; i >= 0; i--) { if (used + turns[i] > forHistoryAndDocs) break; used += turns[i]; kept++; }   // keep newest first
        check("keep the newest turns that fit: how many of 8", 6, kept);
        check("...using tokens", 6_000, used);
    }

    // =====================================================================
    // 5. KV cache
    // =====================================================================

    static void kvCache() {
        System.out.println("5. KV cache");
        int prompt = 1000, generated = 200;
        long withoutCache = 0, withCache = prompt;                                      // count token positions processed
        for (int t = 0; t < generated; t++) withoutCache += prompt + t + 1;            // recompute everything each step
        withCache += generated;                                                         // each new token processed once
        check("positions processed WITHOUT a KV cache", 220_100L, withoutCache);
        check("positions processed WITH a KV cache", 1_200L, withCache);
        check("work saved", "183x", Math.round((double) withoutCache / withCache) + "x");
    }

    // =====================================================================
    // 6. memory and cost
    // =====================================================================

    static void memoryAndCost() {
        System.out.println("6. Memory and cost");
        double params = 7e9;
        check("7B weights in fp16 (2 bytes each)", "14.0 GB", f1(params * 2 / 1e9) + " GB");
        check("7B weights in int8", "7.0 GB", f1(params * 1 / 1e9) + " GB");
        check("7B weights in int4", "3.5 GB", f1(params * 0.5 / 1e9) + " GB");
        // KV cache bytes = 2 (K and V) x layers x kvHeads x headDim x tokens x bytes
        double kv = 2.0 * 32 * 8 * 128 * 8192 * 2;
        check("KV cache: 32 layers, 8 KV heads x 128 dims, 8K tokens, fp16", "1.07 GB", f2(kv / 1e9) + " GB");
        check("...per request! 20 concurrent requests need", "21.5 GB", f1(20 * kv / 1e9) + " GB");
        double inPrice = 3.0, outPrice = 15.0;                                         // example $ per 1M tokens
        double perRequest = 4000 / 1e6 * inPrice + 500 / 1e6 * outPrice;
        check("one request: 4,000 in + 500 out tokens", "$0.0195", String.format("$%.4f", perRequest));
        check("1,000,000 requests a month", "$19500", String.format("$%.0f", perRequest * 1e6));
        check("output tokens cost more and are slower (decode is sequential)", true, outPrice > inPrice);
    }

    static String f1(double x) { return String.format("%.1f", x); }

    public static void main(String[] args) {
        tokenisation();
        nextToken();
        sampling();
        contextWindow();
        kvCache();
        memoryAndCost();
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
