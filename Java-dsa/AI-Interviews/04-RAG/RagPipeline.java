/*
 * ============================================================================
 *  AI 04 : RAG (retrieval-augmented generation), a complete mini pipeline
 * ============================================================================
 *  Run:  java RagPipeline.java          (explanations: README.md in this folder)
 *
 *    1. chunking          fixed-size vs with overlap: a fact split in two
 *    2. indexing          chunks + metadata (department, source)
 *    3. retrieval         BM25 top-k, with a metadata filter (access control)
 *    4. reranking         a slower, more precise scorer re-orders the top-k
 *    5. query rewriting   "what about contractors?" -> a standalone question
 *    6. generation        answer ONLY from context, cite sources, else "I don't know"
 *    7. faithfulness      every answer sentence must be supported by the context
 * ============================================================================
 */

import java.util.*;
import java.util.stream.*;

public class RagPipeline {

    record Doc(String id, String department, String text) {}
    record Chunk(String id, String docId, String department, String text) {}

    static final List<Doc> KB = List.of(
            new Doc("leave-policy", "hr", "Full-time employees get 24 days of paid leave per year. Unused leave can carry over up to 5 days. "
                    + "Contractors are not eligible for paid leave but may take unpaid leave with manager approval. "
                    + "Leave requests must be submitted in the HR portal at least two weeks in advance."),
            new Doc("expense-policy", "finance", "Travel expenses are reimbursed within 30 days of submitting receipts. "
                    + "The daily meal allowance for business travel is 50 dollars. Alcohol is never reimbursed."),
            new Doc("it-security", "it", "Passwords must be at least 14 characters and are rotated every 90 days. "
                    + "Report phishing emails to security@acme.example immediately. Never share your MFA codes with anyone."),
            new Doc("salary-bands", "hr-confidential", "Senior engineer salary band is 150000 to 190000 dollars. Staff engineer band is 190000 to 240000 dollars."),
            new Doc("remote-work", "hr", "Employees may work remotely up to three days per week. Fully remote arrangements need VP approval. "
                    + "The company pays a home office stipend of 500 dollars once per year."));

    // =====================================================================
    // 1. chunking
    // =====================================================================

    static List<Chunk> chunkFixed(Doc d, int size, int overlap) {
        String[] w = d.text().split("\\s+");
        List<Chunk> out = new ArrayList<>();
        int step = size - overlap, n = 0;
        for (int start = 0; start < w.length; start += step) {
            out.add(new Chunk(d.id() + "#" + n++, d.id(), d.department(), String.join(" ", Arrays.copyOfRange(w, start, Math.min(w.length, start + size)))));
            if (start + size >= w.length) break;
        }
        return out;
    }

    static List<Chunk> chunkBySentence(Doc d) {
        String[] s = d.text().split("(?<=\\.)\\s+");
        List<Chunk> out = new ArrayList<>();
        for (int i = 0; i < s.length; i++) out.add(new Chunk(d.id() + "#" + i, d.id(), d.department(), s[i]));
        return out;
    }

    static void chunking() {
        System.out.println("1. Chunking");
        Doc leave = KB.get(0);
        List<Chunk> noOverlap = chunkFixed(leave, 12, 0), withOverlap = chunkFixed(leave, 12, 6);
        String fact = "Contractors are not eligible for paid leave";
        check("12-word chunks, no overlap: the fact is cut in two", false, noOverlap.stream().anyMatch(c -> c.text().contains(fact)));
        check("12-word chunks, 6-word overlap: some chunk holds it whole", true, withOverlap.stream().anyMatch(c -> c.text().contains(fact)));
        check("overlap costs more chunks to store and embed", true, withOverlap.size() > noOverlap.size());
        System.out.printf("      chunks: no overlap %d, with overlap %d, by sentence %d%n", noOverlap.size(), withOverlap.size(), chunkBySentence(leave).size());
        check("sentence chunking keeps each fact intact", true, chunkBySentence(leave).stream().anyMatch(c -> c.text().startsWith(fact)));
    }

    // =====================================================================
    // 2-3. indexing and retrieval (BM25) with a metadata filter
    // =====================================================================

    static List<Chunk> INDEX = new ArrayList<>();

    static List<String> tok(String s) { return Arrays.stream(s.toLowerCase().replaceAll("[^a-z0-9@. ]", " ").split("\\s+")).filter(t -> !t.isBlank() && !STOP.contains(t)).toList(); }
    static final Set<String> STOP = Set.of("the", "a", "an", "is", "are", "of", "for", "to", "and", "in", "do", "does", "how", "what", "many", "i", "my", "can", "be", "per", "at", "with", "your", "we");

    static double bm25(List<String> q, Chunk c, double avgLen) {
        List<String> d = tok(c.text());
        double s = 0;
        for (String t : new HashSet<>(q)) {
            long df = INDEX.stream().filter(x -> tok(x.text()).contains(t)).count();
            if (df == 0) continue;
            double idf = Math.log(1 + (INDEX.size() - df + 0.5) / (df + 0.5));
            int tf = Collections.frequency(d, t);
            s += idf * tf * 2.2 / (tf + 1.2 * (0.25 + 0.75 * d.size() / avgLen));
        }
        return s;
    }

    static List<Object[]> retrieve(String query, int k, Set<String> allowedDepartments) {
        List<String> q = tok(query);
        double avg = INDEX.stream().mapToInt(c -> tok(c.text()).size()).average().orElse(1);
        return INDEX.stream()
                .filter(c -> allowedDepartments.contains(c.department()))               // PRE-filter: never even score forbidden chunks
                .map(c -> new Object[]{c, bm25(q, c, avg)})
                .filter(x -> (double) x[1] > 0)
                .sorted((a, b) -> Double.compare((double) b[1], (double) a[1]))
                .limit(k).collect(Collectors.toList());
    }

    static void retrieval() {
        System.out.println("2-3. Indexing and retrieval");
        for (Doc d : KB) INDEX.addAll(chunkBySentence(d));
        check("index size (sentence chunks)", 15, INDEX.size());
        Set<String> employee = Set.of("hr", "finance", "it");
        List<Object[]> top = retrieve("How many days of paid leave do full-time employees get?", 3, employee);
        check("top chunk for the leave question", "leave-policy#0", ((Chunk) top.get(0)[0]).id());
        List<Object[]> salary = retrieve("What is the senior engineer salary band?", 3, employee);
        check("an employee cannot retrieve confidential salary data", true, salary.stream().noneMatch(x -> ((Chunk) x[0]).department().equals("hr-confidential")));
        List<Object[]> hrAdmin = retrieve("What is the senior engineer salary band?", 3, Set.of("hr", "hr-confidential"));
        check("an HR admin can", "salary-bands#0", ((Chunk) hrAdmin.get(0)[0]).id());
    }

    // =====================================================================
    // 4. reranking
    // =====================================================================

    /**
     * A stand-in for a cross-encoder reranker. A real one reads (query, chunk) TOGETHER and notices that
     * a chunk about "full-time employees" doesn't answer a question about "contractors". We mimic that by
     * rewarding coverage of the query AND, strongly, the presence of the query's most specific (rarest) term.
     */
    static double rerankScore(String query, Chunk c) {
        List<String> q = tok(query), d = tok(c.text());
        double coverage = q.stream().filter(d::contains).count() / (double) q.size();
        String rarest = q.stream().min(Comparator.comparingLong(t -> INDEX.stream().filter(x -> tok(x.text()).contains(t)).count() == 0 ? Long.MAX_VALUE : INDEX.stream().filter(x -> tok(x.text()).contains(t)).count())).orElse("");
        return coverage + (d.contains(rarest) ? 1.0 : 0.0);
    }

    static void reranking() {
        System.out.println("4. Reranking");
        String q = "How many days of paid leave do contractors get?";
        List<Object[]> first = retrieve(q, 5, Set.of("hr", "finance", "it"));
        List<Chunk> reranked = first.stream().map(x -> (Chunk) x[0]).sorted((a, b) -> Double.compare(rerankScore(q, b), rerankScore(q, a))).toList();
        check("BM25 puts the FULL-TIME rule first (common words win)", "leave-policy#0", ((Chunk) first.get(0)[0]).id());
        check("after reranking, the contractor rule is first", "leave-policy#2", reranked.get(0).id());
        System.out.println("      BM25 order: " + first.stream().map(x -> ((Chunk) x[0]).id()).toList());
        System.out.println("      reranked:   " + reranked.stream().map(Chunk::id).toList());
    }

    // =====================================================================
    // 5. query rewriting
    // =====================================================================

    /** In production an LLM does this ("rewrite the last question so it stands alone"). Canned here so the demo is deterministic. */
    static String rewrite(List<String> history, String followUp) {
        Map<String, String> llm = Map.of("How long does it take?", "How long does it take for travel expenses to be reimbursed?");
        return llm.getOrDefault(followUp, followUp);
    }

    static void queryRewriting() {
        System.out.println("5. Query rewriting (conversational RAG)");
        List<String> history = List.of("How are travel expenses reimbursed?");
        String followUp = "How long does it take?";
        List<Object[]> raw = retrieve(followUp, 3, Set.of("hr", "finance", "it"));
        check("the raw follow-up retrieves something IRRELEVANT", true, !raw.isEmpty() && !((Chunk) raw.get(0)[0]).docId().equals("expense-policy"));
        System.out.println("      raw follow-up top chunk: " + ((Chunk) raw.get(0)[0]).id() + " = \"" + ((Chunk) raw.get(0)[0]).text() + "\"");
        String standalone = rewrite(history, followUp);
        check("rewritten standalone question", "How long does it take for travel expenses to be reimbursed?", standalone);
        check("which retrieves the 30-day rule", "expense-policy#0", ((Chunk) retrieve(standalone, 1, Set.of("hr", "finance", "it")).get(0)[0]).id());
    }

    // =====================================================================
    // 6. grounded generation, 7. faithfulness
    // =====================================================================

    /** A fake LLM that behaves the way the system prompt demands: answer only from the context, cite, or refuse. */
    static String generate(String question, List<Object[]> context, double minScore) {
        if (context.isEmpty() || (double) context.get(0)[1] < minScore) return "I don't know based on the available documents.";
        Chunk best = (Chunk) context.get(0)[0];
        return best.text() + " [" + best.id() + "]";
    }

    static boolean faithful(String answer, List<Object[]> context) {             // every sentence must appear in the context
        String body = answer.replaceAll("\\s*\\[[^]]+]", "");
        for (String sentence : body.split("(?<=\\.)\\s+"))
            if (context.stream().noneMatch(x -> ((Chunk) x[0]).text().contains(sentence.trim()))) return false;
        return true;
    }

    static void generation() {
        System.out.println("6. Grounded generation  7. Faithfulness");
        Set<String> employee = Set.of("hr", "finance", "it");
        String q = "What is the daily meal allowance for business travel?";
        List<Object[]> ctx = retrieve(q, 3, employee);
        String answer = generate(q, ctx, 1.0);
        check("answer quotes the policy and cites it", "The daily meal allowance for business travel is 50 dollars. [expense-policy#1]", answer);
        check("the answer is faithful to the context", true, faithful(answer, ctx));
        String hallucinated = "The daily meal allowance for business travel is 75 dollars. [expense-policy#1]";
        check("a made-up number is caught as unfaithful", false, faithful(hallucinated, ctx));
        String offTopic = "What is the CEO's favourite colour?";
        check("no relevant context -> refuse instead of guessing", "I don't know based on the available documents.", generate(offTopic, retrieve(offTopic, 3, employee), 1.0));
        String prompt = buildPrompt(q, ctx);
        check("the prompt tells the model to use only the context", true, prompt.contains("ONLY the context") && prompt.contains("[expense-policy#1]"));
    }

    static String buildPrompt(String question, List<Object[]> ctx) {
        StringBuilder sb = new StringBuilder("Answer using ONLY the context below. Cite chunk ids in [brackets]. If the answer is not in the context, say \"I don't know\".\n\n<context>\n");
        for (Object[] x : ctx) { Chunk c = (Chunk) x[0]; sb.append("[").append(c.id()).append("] ").append(c.text()).append('\n'); }
        return sb.append("</context>\n\nQuestion: ").append(question).toString();
    }

    public static void main(String[] args) {
        chunking();
        retrieval();
        reranking();
        queryRewriting();
        generation();
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
