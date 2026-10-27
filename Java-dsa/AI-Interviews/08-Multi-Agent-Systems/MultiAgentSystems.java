/*
 * ============================================================================
 *  AI 08 : MULTI-AGENT SYSTEMS
 * ============================================================================
 *  Run:  java MultiAgentSystems.java          (explanations: README.md in this folder)
 *
 *    1. orchestrator-worker  split a task, run workers IN PARALLEL, merge; one worker fails
 *    2. handoffs             a triage agent routes to a specialist and passes context
 *    3. reviewer loop        writer <-> critic until approved or out of rounds
 *    4. discovery            pick an agent by its "agent card" capabilities (A2A-style)
 *    5. shared blackboard    agents coordinate through shared state
 *    6. cost                 why multi-agent systems use many more tokens
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class MultiAgentSystems {

    interface AgentFn extends Function<String, String> {}

    static String slowAgent(String name, String task, long millis) {             // a stand-in for an LLM call with latency
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (task.contains("FAIL")) throw new IllegalStateException(name + " could not finish: " + task);
        return name + " findings on '" + task + "'";
    }

    // =====================================================================
    // 1. orchestrator-worker
    // =====================================================================

    static void orchestratorWorker() throws Exception {
        System.out.println("1. Orchestrator-worker (parallel)");
        String goal = "Compare three cloud providers for our ML workload";
        List<String> subtasks = List.of("pricing of GPUs", "managed ML services", "data residency rules");   // the orchestrator's plan
        long t0 = System.nanoTime();
        List<String> sequential = new ArrayList<>();
        for (String s : subtasks) sequential.add(slowAgent("researcher", s, 150));
        long seqMs = (System.nanoTime() - t0) / 1_000_000;

        ExecutorService pool = Executors.newFixedThreadPool(subtasks.size());
        t0 = System.nanoTime();
        List<Future<String>> futures = new ArrayList<>();
        for (String s : subtasks) futures.add(pool.submit(() -> slowAgent("researcher", s, 150)));
        List<String> parallel = new ArrayList<>();
        for (Future<String> f : futures) parallel.add(f.get(2, TimeUnit.SECONDS));
        long parMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("      sequential %d ms, parallel %d ms%n", seqMs, parMs);
        check("same results either way", sequential.toString(), parallel.toString());
        check("parallel workers are much faster (independent subtasks)", true, parMs * 2 < seqMs);

        // one worker fails: the orchestrator retries once, then continues with partial results and says so
        List<String> withFailure = List.of("pricing of GPUs", "FAIL managed ML services", "data residency rules");
        List<String> merged = new ArrayList<>(), gaps = new ArrayList<>();
        for (String s : withFailure) {
            Future<String> f = pool.submit(() -> slowAgent("researcher", s, 10));
            try { merged.add(f.get(1, TimeUnit.SECONDS)); }
            catch (ExecutionException e) {
                try { merged.add(pool.submit(() -> slowAgent("researcher", s, 10)).get(1, TimeUnit.SECONDS)); }   // retry once (fails again here)
                catch (ExecutionException e2) { gaps.add(s.replace("FAIL ", "")); }
            }
        }
        pool.shutdown();
        String report = "Report on: " + goal + " | " + merged.size() + " sections" + (gaps.isEmpty() ? "" : " | MISSING: " + gaps);
        check("partial result, and the gap is reported (not hidden)", "Report on: " + goal + " | 2 sections | MISSING: [managed ML services]", report);
    }

    // =====================================================================
    // 2. handoffs
    // =====================================================================

    record Handoff(String to, String context) {}

    static Handoff triage(String message) {                                       // a cheap router/classifier agent
        String m = message.toLowerCase();
        String to = m.contains("refund") || m.contains("charged") || m.contains("invoice") ? "billing"
                : m.contains("error") || m.contains("crash") || m.contains("bug") ? "tech_support" : "general";
        return new Handoff(to, "user said: " + message);
    }

    static final Map<String, AgentFn> SPECIALISTS = Map.of(
            "billing", c -> "[billing] I see you were double-charged; refund issued. (" + c + ")",
            "tech_support", c -> "[tech_support] Please update to v2.3 which fixes that crash. (" + c + ")",
            "general", c -> "[general] Happy to help! (" + c + ")");

    static void handoffs() {
        System.out.println("2. Handoffs");
        check("refund question -> billing", "billing", triage("I was charged twice, I want a refund").to());
        check("crash -> tech support", "tech_support", triage("The app crashes on start").to());
        Handoff h = triage("I was charged twice, I want a refund");
        String reply = SPECIALISTS.get(h.to()).apply(h.context());
        check("the specialist received the conversation context", true, reply.contains("user said: I was charged twice"));
    }

    // =====================================================================
    // 3. writer <-> reviewer loop
    // =====================================================================

    static void reviewerLoop() {
        System.out.println("3. Writer and reviewer");
        Function<String, List<String>> reviewer = draft -> {                       // a critic agent with a checklist
            List<String> issues = new ArrayList<>();
            if (!draft.contains("price")) issues.add("mention the price");
            if (!draft.contains("deadline")) issues.add("state the deadline");
            if (draft.length() > 120) issues.add("make it shorter");
            return issues;
        };
        BiFunction<String, List<String>, String> writer = (draft, issues) -> {      // a writer agent that addresses feedback
            String d = draft;
            if (issues.contains("mention the price")) d += " The price is $40/month.";
            if (issues.contains("state the deadline")) d += " Offer ends Friday (deadline).";
            return d;
        };
        String draft = "Announcing our new Pro plan with faster support.";
        int rounds = 0;
        List<String> issues = reviewer.apply(draft);
        List<Integer> issueCounts = new ArrayList<>(List.of(issues.size()));
        while (!issues.isEmpty() && rounds < 3) { draft = writer.apply(draft, issues); issues = reviewer.apply(draft); issueCounts.add(issues.size()); rounds++; }
        check("issues per round", "[2, 0]", issueCounts.toString());
        check("approved after 1 revision", 1, rounds);
        check("max rounds bounds a writer/critic disagreement", true, rounds <= 3);
    }

    // =====================================================================
    // 4. discovery by agent card (A2A-style)
    // =====================================================================

    record AgentCard(String name, String url, Set<String> skills, double costPerTask) {}

    static void discovery() {
        System.out.println("4. Discovering agents by capability");
        List<AgentCard> registry = List.of(
                new AgentCard("travel-booker", "https://agents.acme.example/travel", Set.of("book_flight", "book_hotel"), 0.05),
                new AgentCard("expense-bot", "https://agents.acme.example/expense", Set.of("file_expense", "check_policy"), 0.01),
                new AgentCard("cheap-travel", "https://partner.example/travel", Set.of("book_flight"), 0.02));
        Optional<AgentCard> best = registry.stream().filter(c -> c.skills().contains("book_flight")).min(Comparator.comparingDouble(AgentCard::costPerTask));
        check("cheapest agent that can book a flight", "cheap-travel", best.map(AgentCard::name).orElse("none"));
        check("nobody can 'approve_budget' -> escalate to a human", "none", registry.stream().filter(c -> c.skills().contains("approve_budget")).findFirst().map(AgentCard::name).orElse("none"));
    }

    // =====================================================================
    // 5. shared blackboard
    // =====================================================================

    static void blackboard() throws Exception {
        System.out.println("5. Shared blackboard");
        ConcurrentHashMap<String, String> board = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch facts = new CountDownLatch(2);
        pool.submit(() -> { board.put("flight", "LH400 Mon 09:00"); facts.countDown(); });           // flight agent
        pool.submit(() -> { board.put("hotel", "Hotel Adlon, 3 nights"); facts.countDown(); });     // hotel agent
        Future<String> planner = pool.submit(() -> { facts.await(); return "Itinerary: " + board.get("flight") + " + " + board.get("hotel"); });   // waits for both
        check("planner combines what the others wrote", "Itinerary: LH400 Mon 09:00 + Hotel Adlon, 3 nights", planner.get(2, TimeUnit.SECONDS));
        pool.shutdown();
    }

    // =====================================================================
    // 6. cost
    // =====================================================================

    static void cost() {
        System.out.println("6. Token cost of multi-agent systems");
        int single = 1 * (2000 + 500);                                            // one agent: context + answer
        int multi = 1 * (2000 + 300) + 3 * (2500 + 800) + 1 * (2000 + 3 * 800 + 600);   // orchestrator plan + 3 workers + synthesis
        check("tokens: single agent vs orchestrator + 3 workers", "2500 vs 17200", single + " vs " + multi);
        check("multi-agent uses ~7x the tokens here", "6.9x", String.format("%.1fx", multi / (double) single));
    }

    public static void main(String[] args) throws Exception {
        orchestratorWorker();
        handoffs();
        reviewerLoop();
        discovery();
        blackboard();
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
