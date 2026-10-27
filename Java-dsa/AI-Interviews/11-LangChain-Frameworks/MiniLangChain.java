/*
 * ============================================================================
 *  AI 11 : LANGCHAIN AND LANGGRAPH, rebuilt in miniature to see how they work
 * ============================================================================
 *  Run:  java MiniLangChain.java          (explanations: README.md in this folder)
 *
 *    1. Runnable + pipe     LCEL's  prompt | model | parser
 *    2. parallel/passthrough the RAG chain  {context: retriever, question: passthrough} | prompt | model
 *    3. fallbacks & batch    with_fallbacks, batch
 *    4. StateGraph           nodes, edges, conditional edges, END
 *    5. checkpoints          per-thread state you can resume
 *    6. interrupt            pause for human approval, then resume
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class MiniLangChain {

    // =====================================================================
    // 1. Runnable: the one interface everything implements
    // =====================================================================

    interface Runnable<I, O> {
        O invoke(I input);
        default <R> Runnable<I, R> pipe(Runnable<O, R> next) { return in -> next.invoke(invoke(in)); }   // the "|" operator
        default List<O> batch(List<I> inputs) { return inputs.stream().map(this::invoke).toList(); }
        default Runnable<I, O> withFallbacks(Runnable<I, O> fallback) {
            return in -> { try { return invoke(in); } catch (RuntimeException e) { return fallback.invoke(in); } };
        }
    }

    static Runnable<Map<String, String>, String> promptTemplate(String template) {
        return vars -> { String s = template; for (var e : vars.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue()); return s; };
    }

    /** A fake chat model: echoes what it was asked, in a recognisable shape. */
    static Runnable<String, String> fakeModel(String name) { return prompt -> "AIMessage(" + name + "): answer to [" + prompt + "]"; }

    static Runnable<String, String> strOutputParser() { return msg -> msg.substring(msg.indexOf(": ") + 2); }

    static void runnables() {
        System.out.println("1. Runnable and pipe (LCEL)");
        Runnable<Map<String, String>, String> chain = promptTemplate("Tell me a fact about {topic}").pipe(fakeModel("gpt")).pipe(strOutputParser());
        check("prompt | model | parser", "answer to [Tell me a fact about owls]", chain.invoke(Map.of("topic", "owls")));
        check("batch runs the same chain on many inputs", 2, chain.batch(List.of(Map.of("topic", "a"), Map.of("topic", "b"))).size());
    }

    // =====================================================================
    // 2. RunnableParallel + RunnablePassthrough: the classic RAG chain
    // =====================================================================

    static <I> Runnable<I, Map<String, String>> parallel(Map<String, Runnable<I, String>> branches) {
        return in -> { Map<String, String> out = new LinkedHashMap<>(); branches.forEach((k, r) -> out.put(k, r.invoke(in))); return out; };
    }

    static <T> Runnable<T, T> passthrough() { return in -> in; }

    static void ragChain() {
        System.out.println("2. The RAG chain shape");
        Map<String, String> docs = Map.of("refund", "Refunds take 5 business days.", "shipping", "Shipping is free over $50.");
        Runnable<String, String> retriever = q -> docs.entrySet().stream().filter(e -> q.toLowerCase().contains(e.getKey())).map(Map.Entry::getValue).findFirst().orElse("(no docs)");
        Runnable<String, String> rag = MiniLangChain.<String>parallel(new LinkedHashMap<>(Map.of("context", retriever, "question", passthrough())))
                .pipe(promptTemplate("Context: {context}\nQuestion: {question}"))
                .pipe(fakeModel("gemini")).pipe(strOutputParser());
        check("the retriever's output and the original question both reach the prompt", "answer to [Context: Refunds take 5 business days.\nQuestion: How long does a refund take?]", rag.invoke("How long does a refund take?"));
    }

    // =====================================================================
    // 3. fallbacks
    // =====================================================================

    static void fallbacks() {
        System.out.println("3. Fallbacks");
        Runnable<String, String> flaky = p -> { throw new RuntimeException("429 rate limited"); };
        Runnable<String, String> robust = flaky.withFallbacks(fakeModel("backup-model"));
        check("primary model fails -> the fallback answers", "AIMessage(backup-model): answer to [hi]", robust.invoke("hi"));
    }

    // =====================================================================
    // 4-6. a tiny LangGraph
    // =====================================================================

    static final String END = "__end__";

    static class Interrupt extends RuntimeException { final String question; Interrupt(String q) { super(q); question = q; } }

    static class StateGraph {
        final Map<String, UnaryOperator<Map<String, Object>>> nodes = new LinkedHashMap<>();
        final Map<String, Function<Map<String, Object>, String>> edges = new HashMap<>();   // node -> next node (possibly conditional)
        String entry;
        final Map<String, Map<String, Object>> checkpoints = new HashMap<>();             // thread_id -> saved state
        final Map<String, String> pausedAt = new HashMap<>();

        StateGraph addNode(String name, UnaryOperator<Map<String, Object>> fn) { nodes.put(name, fn); return this; }
        StateGraph addEdge(String from, String to) { edges.put(from, s -> to); return this; }
        StateGraph addConditionalEdges(String from, Function<Map<String, Object>, String> router) { edges.put(from, router); return this; }
        StateGraph setEntry(String node) { entry = node; return this; }

        Map<String, Object> invoke(Map<String, Object> input, String threadId) {
            Map<String, Object> state = new HashMap<>(checkpoints.getOrDefault(threadId, Map.of()));
            state.putAll(input);
            String node = pausedAt.containsKey(threadId) ? pausedAt.remove(threadId) : entry;
            @SuppressWarnings("unchecked") List<String> path = (List<String>) state.computeIfAbsent("path", k -> new ArrayList<String>());
            for (int steps = 0; !node.equals(END); steps++) {
                if (steps > 25) throw new IllegalStateException("recursion limit reached");   // LangGraph has the same guard
                try { state = nodes.get(node).apply(state); }
                catch (Interrupt i) { pausedAt.put(threadId, node); state.put("__interrupt__", i.question); checkpoints.put(threadId, state); return state; }
                path.add(node);
                state.put("path", path);
                checkpoints.put(threadId, new HashMap<>(state));                   // a checkpoint after every node
                node = edges.get(node).apply(state);
            }
            state.remove("__interrupt__");
            checkpoints.put(threadId, state);
            return state;
        }
    }

    static StateGraph supportGraph() {
        return new StateGraph()
                .addNode("classify", s -> { s.put("intent", ((String) s.get("message")).contains("refund") ? "refund" : "faq"); return s; })
                .addNode("faq", s -> { s.put("reply", "Here is our FAQ answer."); return s; })
                .addNode("refund", s -> {
                    if (!s.containsKey("approved")) throw new Interrupt("Approve a refund of $" + s.get("amount") + "?");   // pause for a human
                    s.put("reply", (Boolean) s.get("approved") ? "Refund of $" + s.get("amount") + " issued." : "Refund declined by a reviewer.");
                    return s;
                })
                .setEntry("classify")
                .addConditionalEdges("classify", s -> s.get("intent").equals("refund") ? "refund" : "faq")
                .addEdge("faq", END)
                .addEdge("refund", END);
    }

    static void langGraph() {
        System.out.println("4. StateGraph  5. Checkpoints  6. Interrupts");
        StateGraph g = supportGraph();
        Map<String, Object> faq = g.invoke(new HashMap<>(Map.of("message", "What are your opening hours?")), "thread-1");
        check("conditional edge routes an FAQ", "[classify, faq] / Here is our FAQ answer.", faq.get("path") + " / " + faq.get("reply"));
        Map<String, Object> paused = g.invoke(new HashMap<>(Map.of("message", "I want a refund", "amount", 120)), "thread-2");
        check("the refund node pauses for a human", "Approve a refund of $120?", paused.get("__interrupt__"));
        check("...and the state is checkpointed for thread-2", true, g.checkpoints.containsKey("thread-2") && g.pausedAt.get("thread-2").equals("refund"));
        Map<String, Object> resumed = g.invoke(new HashMap<>(Map.of("approved", true)), "thread-2");   // later, maybe on another server
        check("resume with the human's decision", "[classify, refund] / Refund of $120 issued.", resumed.get("path") + " / " + resumed.get("reply"));
        check("thread-1's state is untouched (separate threads)", "faq", g.checkpoints.get("thread-1").get("intent"));
        StateGraph loop = new StateGraph().addNode("a", s -> s).setEntry("a").addEdge("a", "a");
        String err;
        try { loop.invoke(new HashMap<>(), "t"); err = "none"; } catch (IllegalStateException e) { err = e.getMessage(); }
        check("a cycle without an exit hits the recursion limit", "recursion limit reached", err);
    }

    public static void main(String[] args) {
        runnables();
        ragChain();
        fallbacks();
        langGraph();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-58s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a.replace("\n", "\\n"), e.equals(a) ? "" : "   (expected " + e.replace("\n", "\\n") + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
