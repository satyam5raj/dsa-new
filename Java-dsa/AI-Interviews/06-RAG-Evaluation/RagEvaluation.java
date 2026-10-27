/*
 * ============================================================================
 *  AI 06 : RAG EVALUATION  (measure retrieval and generation SEPARATELY)
 * ============================================================================
 *  Run:  java RagEvaluation.java          (explanations: README.md in this folder)
 *
 *    1. retrieval metrics   hit rate@k, precision@k, recall@k, MRR, nDCG@k
 *    2. choosing k          recall goes up, precision goes down
 *    3. generation metrics  faithfulness, answer relevance (RAGAS-style)
 *    4. context metrics     context precision, context recall
 *    5. diagnosis           is it the retriever or the generator?
 * ============================================================================
 */

import java.util.*;

public class RagEvaluation {

    static String f3(double x) { return String.format("%.3f", x); }

    // =====================================================================
    // 1. retrieval metrics
    // =====================================================================

    record Case(String query, List<String> retrieved, Set<String> relevant) {}

    static final List<Case> CASES = List.of(
            new Case("q1", List.of("d3", "d1", "d7", "d2", "d9"), Set.of("d1", "d2")),
            new Case("q2", List.of("d5", "d4", "d8", "d6", "d0"), Set.of("d5")),
            new Case("q3", List.of("d2", "d6", "d4", "d1", "d3"), Set.of("d9")),           // a total retrieval miss
            new Case("q4", List.of("d7", "d8", "d0", "d4", "d5"), Set.of("d0", "d4", "d8")));

    static double hit(Case c, int k) { for (int i = 0; i < k; i++) if (c.relevant().contains(c.retrieved().get(i))) return 1; return 0; }
    static double precision(Case c, int k) { int h = 0; for (int i = 0; i < k; i++) if (c.relevant().contains(c.retrieved().get(i))) h++; return h / (double) k; }
    static double recall(Case c, int k) { int h = 0; for (int i = 0; i < k; i++) if (c.relevant().contains(c.retrieved().get(i))) h++; return h / (double) c.relevant().size(); }
    static double rr(Case c) { for (int i = 0; i < c.retrieved().size(); i++) if (c.relevant().contains(c.retrieved().get(i))) return 1.0 / (i + 1); return 0; }

    static double ndcg(Case c, int k) {
        double dcg = 0, idcg = 0;
        for (int i = 0; i < k; i++) if (c.relevant().contains(c.retrieved().get(i))) dcg += 1 / log2(i + 2);   // gain discounted by position
        for (int i = 0; i < Math.min(k, c.relevant().size()); i++) idcg += 1 / log2(i + 2);                  // the best possible ordering
        return idcg == 0 ? 0 : dcg / idcg;
    }

    static double log2(double x) { return Math.log(x) / Math.log(2); }

    static double mean(java.util.function.ToDoubleFunction<Case> f) { return CASES.stream().mapToDouble(f).average().orElse(0); }

    static void retrievalMetrics() {
        System.out.println("1. Retrieval metrics (4 queries, top 5 retrieved)");
        Case q1 = CASES.get(0);
        check("q1 precision@3 (1 of 3 relevant)", "0.333", f3(precision(q1, 3)));
        check("q1 recall@3 (found 1 of 2 relevant)", "0.500", f3(recall(q1, 3)));
        check("q1 reciprocal rank (first relevant at position 2)", "0.500", f3(rr(q1)));
        check("q1 nDCG@5", "0.651", f3(ndcg(q1, 5)));
        check("hit rate@5 over all queries (q3 is a miss)", "0.750", f3(mean(c -> hit(c, 5))));
        check("MRR over all queries", "0.500", f3(mean(RagEvaluation::rr)));
        check("mean nDCG@5", "0.596", f3(mean(c -> ndcg(c, 5))));
    }

    // =====================================================================
    // 2. choosing k
    // =====================================================================

    static void choosingK() {
        System.out.println("2. Choosing k");
        StringBuilder s = new StringBuilder();
        double prevRecall = -1, prevPrecision = 2;
        boolean recallUp = true, precisionDown = true;
        for (int k : new int[]{1, 3, 5}) {
            final int kk = k;
            double r = mean(c -> recall(c, kk)), p = mean(c -> precision(c, kk));
            recallUp &= r >= prevRecall; precisionDown &= p <= prevPrecision + 1e-12;
            prevRecall = r; prevPrecision = p;
            s.append(String.format("k=%d: recall %.3f precision %.3f | ", k, r, p));
        }
        System.out.println("      " + s);
        check("recall never drops as k grows", true, recallUp);
        check("precision is NOT monotonic (0.25 -> 0.33 -> 0.30)", false, precisionDown);
        check("past the useful ranks, more k only adds noise: precision@5 < precision@3", true, mean(c -> precision(c, 5)) < mean(c -> precision(c, 3)));
    }

    // =====================================================================
    // 3-4. generation and context metrics (RAGAS-style, with a simple "supports" check)
    // =====================================================================

    static Set<String> words(String s) { Set<String> w = new HashSet<>(Arrays.asList(s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+"))); w.removeAll(Set.of("the", "a", "is", "of", "for", "and", "are", "to", "in", "per", "be", "can", "it", "what", "how", "many", "do", "does")); return w; }

    /** A claim is "supported" if most of its content words appear in one context chunk (a stand-in for an NLI model or LLM judge). */
    static boolean supported(String claim, List<String> context) {
        Set<String> cw = words(claim);
        for (String chunk : context) { Set<String> k = words(chunk); long hit = cw.stream().filter(k::contains).count(); if (hit >= 0.8 * cw.size()) return true; }
        return false;
    }

    static double faithfulness(String answer, List<String> context) {
        String[] claims = answer.split("(?<=\\.)\\s+");
        int ok = 0;
        for (String c : claims) if (supported(c, context)) ok++;
        return ok / (double) claims.length;
    }

    static double answerRelevance(String question, String answer) {             // share of the question's content words addressed
        Set<String> q = words(question), a = words(answer);
        return q.stream().filter(a::contains).count() / (double) q.size();
    }

    static double contextPrecision(List<String> context, Set<Integer> usefulIdx) { // rank-weighted: useful chunks should come first
        double sum = 0;
        int hits = 0;
        for (int i = 0; i < context.size(); i++) if (usefulIdx.contains(i)) { hits++; sum += hits / (double) (i + 1); }
        return hits == 0 ? 0 : sum / hits;
    }

    static double contextRecall(String groundTruth, List<String> context) {      // can the reference answer be found in the context?
        String[] claims = groundTruth.split("(?<=\\.)\\s+");
        int ok = 0;
        for (String c : claims) if (supported(c, context)) ok++;
        return ok / (double) claims.length;
    }

    static void generationMetrics() {
        System.out.println("3. Generation metrics  4. Context metrics");
        String q = "How many days of paid leave do employees get and can it carry over?";
        List<String> ctx = List.of(
                "Full-time employees get 24 days of paid leave per year.",
                "The office is closed on public holidays.",
                "Unused leave can carry over up to 5 days.");
        String good = "Employees get 24 days of paid leave per year. Unused leave can carry over up to 5 days.";
        String hallucinated = "Employees get 24 days of paid leave per year. Leave can be cashed out at 150 percent salary.";
        String offTopic = "The office is closed on public holidays.";
        check("faithfulness: grounded answer", "1.000", f3(faithfulness(good, ctx)));
        check("faithfulness: one invented claim out of two", "0.500", f3(faithfulness(hallucinated, ctx)));
        check("answer relevance: grounded answer", true, answerRelevance(q, good) > 0.7);
        check("answer relevance: faithful but OFF-TOPIC answer is low", true, answerRelevance(q, offTopic) < 0.2);
        check("...even though its faithfulness is perfect", "1.000", f3(faithfulness(offTopic, ctx)));
        check("context precision: useful chunks at ranks 1 and 3", "0.833", f3(contextPrecision(ctx, Set.of(0, 2))));
        check("context precision if useful chunks were ranked 1 and 2", "1.000", f3(contextPrecision(List.of(ctx.get(0), ctx.get(2), ctx.get(1)), Set.of(0, 1))));
        String truth = "Employees get 24 days of paid leave. Unused leave can carry over up to 5 days.";
        check("context recall: everything needed was retrieved", "1.000", f3(contextRecall(truth, ctx)));
        check("context recall if the carry-over chunk was missed", "0.500", f3(contextRecall(truth, ctx.subList(0, 2))));
    }

    // =====================================================================
    // 5. diagnosis
    // =====================================================================

    static String diagnose(double contextRecall, double faithfulness, double answerCorrect) {
        if (contextRecall < 0.5) return "fix RETRIEVAL (chunking, hybrid search, query rewriting, k, reranker)";
        if (faithfulness < 0.8) return "fix GENERATION grounding (prompt: only use context, cite; lower temperature; verify)";
        if (answerCorrect < 0.8) return "fix GENERATION reasoning/completeness (better model, instructions, examples)";
        return "healthy";
    }

    static void diagnosis() {
        System.out.println("5. Diagnosis: retriever or generator?");
        check("context recall 0.3", "fix RETRIEVAL (chunking, hybrid search, query rewriting, k, reranker)", diagnose(0.3, 0.9, 0.4));
        check("good context, answer adds claims", "fix GENERATION grounding (prompt: only use context, cite; lower temperature; verify)", diagnose(0.9, 0.6, 0.6));
        check("good context, faithful, still wrong", "fix GENERATION reasoning/completeness (better model, instructions, examples)", diagnose(0.9, 0.95, 0.6));
        check("all good", "healthy", diagnose(0.95, 0.95, 0.9));
    }

    public static void main(String[] args) {
        retrievalMetrics();
        choosingK();
        generationMetrics();
        diagnosis();
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
