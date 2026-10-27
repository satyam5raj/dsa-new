/*
 * ============================================================================
 *  AI 07 : AI AGENTS  (a ReAct agent loop you can read end to end)
 * ============================================================================
 *  Run:  java AiAgents.java          (explanations: README.md in this folder)
 *
 *  The "LLM" is a deterministic stand-in that reads the goal and the trace and
 *  picks the next step, exactly the decision a real model makes in a ReAct loop.
 *
 *    1. tools            a registry with schemas and argument validation
 *    2. ReAct loop       Thought -> Action -> Observation ... -> Final Answer
 *    3. tool errors      a failing call becomes an observation; the agent corrects itself
 *    4. stopping         max steps + loop detection stop a stuck agent
 *    5. human approval   risky tools (send_email) need a yes
 *    6. memory           short-term (this run) vs long-term (across sessions)
 *    7. plan-and-execute vs ReAct: number of LLM calls and cost
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class AiAgents {

    // =====================================================================
    // 1. tools
    // =====================================================================

    record Tool(String name, String description, List<String> requiredArgs, boolean risky, Function<Map<String, String>, String> run) {}

    static final Map<String, String> WEATHER = Map.of("paris", "18C, light rain", "tokyo", "25C, sunny");
    static final List<String> SENT = new ArrayList<>();
    static final Map<String, Tool> TOOLS = new LinkedHashMap<>();
    static {
        register(new Tool("calculator", "Evaluate a + - * / expression of two numbers, e.g. '23 * 7'", List.of("expression"), false, a -> {
            String[] p = a.get("expression").trim().split("\\s+");
            if (p.length != 3) throw new IllegalArgumentException("expected 'number op number', got '" + a.get("expression") + "'");
            double x = Double.parseDouble(p[0]), y = Double.parseDouble(p[2]);
            double v = switch (p[1]) { case "+" -> x + y; case "-" -> x - y; case "*" -> x * y; case "/" -> x / y; default -> throw new IllegalArgumentException("unknown operator " + p[1]); };
            return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
        }));
        register(new Tool("get_weather", "Current weather for a city", List.of("city"), false, a -> {
            String w = WEATHER.get(a.get("city").toLowerCase());
            if (w == null) throw new IllegalArgumentException("unknown city " + a.get("city"));
            return w;
        }));
        register(new Tool("send_email", "Send an email (IRREVERSIBLE)", List.of("to", "body"), true, a -> { SENT.add(a.get("to") + ": " + a.get("body")); return "sent"; }));
    }
    static void register(Tool t) { TOOLS.put(t.name(), t); }

    static String describeTools() {                                              // what goes into the system prompt
        StringBuilder sb = new StringBuilder();
        TOOLS.values().forEach(t -> sb.append("- ").append(t.name()).append(t.requiredArgs()).append(": ").append(t.description()).append('\n'));
        return sb.toString();
    }

    // =====================================================================
    // 2. the agent loop
    // =====================================================================

    record Step(String thought, String action, Map<String, String> args, String observation) {}
    record Decision(String thought, String action, Map<String, String> args, String finalAnswer) {}

    interface Llm { Decision decide(String goal, List<Step> trace, Map<String, String> memory); }

    static class Agent {
        final Llm llm;
        final int maxSteps;
        final Predicate<String> approve;                                        // human-in-the-loop for risky tools
        final Map<String, String> longTermMemory;
        int llmCalls = 0;
        Agent(Llm llm, int maxSteps, Predicate<String> approve, Map<String, String> memory) { this.llm = llm; this.maxSteps = maxSteps; this.approve = approve; this.longTermMemory = memory; }

        Object[] run(String goal) {
            List<Step> trace = new ArrayList<>();
            for (int i = 0; i < maxSteps; i++) {
                Decision d = llm.decide(goal, trace, longTermMemory);
                llmCalls++;
                if (d.finalAnswer() != null) return new Object[]{d.finalAnswer(), trace};
                if (isLooping(trace, d)) return new Object[]{"STOPPED: repeating the same failing action", trace};
                trace.add(new Step(d.thought(), d.action(), d.args(), execute(d)));
            }
            return new Object[]{"STOPPED: step budget (" + maxSteps + ") exhausted", trace};
        }

        String execute(Decision d) {
            Tool t = TOOLS.get(d.action());
            if (t == null) return "ERROR: no tool named " + d.action() + ". Available: " + TOOLS.keySet();
            for (String req : t.requiredArgs()) if (!d.args().containsKey(req)) return "ERROR: missing argument '" + req + "'";
            if (t.risky() && !approve.test(d.action() + " " + d.args())) return "DENIED: the user did not approve " + d.action();
            try { return t.run().apply(d.args()); }
            catch (RuntimeException e) { return "ERROR: " + e.getMessage(); }    // errors are observations, not crashes
        }

        static boolean isLooping(List<Step> trace, Decision next) {             // same action + args as the last 2 failed steps
            if (trace.size() < 2) return false;
            Step a = trace.get(trace.size() - 1), b = trace.get(trace.size() - 2);
            return a.action().equals(next.action()) && a.args().equals(next.args()) && b.action().equals(next.action()) && b.args().equals(next.args()) && a.observation().startsWith("ERROR");
        }
    }

    /** A scripted policy: what a capable model would decide for these goals, given the trace so far. */
    static final Llm SCRIPTED = (goal, trace, mem) -> {
        String g = goal.toLowerCase();
        String lastObs = trace.isEmpty() ? null : trace.get(trace.size() - 1).observation();
        if (g.contains("weather") && g.contains("paris") && g.contains("fahrenheit")) {
            if (trace.isEmpty()) return new Decision("I need the current weather in Paris.", "get_weather", Map.of("city", "Paris"), null);
            if (trace.size() == 1) { String c = lastObs.split("C")[0]; return new Decision("Convert " + c + "C to F: F = C * 9/5 + 32. First C * 1.8.", "calculator", Map.of("expression", c + " * 1.8"), null); }
            if (trace.size() == 2) return new Decision("Now add 32.", "calculator", Map.of("expression", lastObs + " + 32"), null);
            return new Decision("I have everything.", null, null, "It is " + trace.get(2).observation() + "F (" + trace.get(0).observation() + ") in Paris.");
        }
        if (g.contains("23 times 7")) {
            if (trace.isEmpty()) return new Decision("Use the calculator.", "calculator", Map.of("expression", "23x7"), null);   // a malformed first try
            if (lastObs.startsWith("ERROR")) return new Decision("The tool wants 'number op number'. Fix the format.", "calculator", Map.of("expression", "23 * 7"), null);
            return new Decision("Done.", null, null, "23 times 7 is " + lastObs + ".");
        }
        if (g.contains("atlantis")) return new Decision("Try the weather tool again.", "get_weather", Map.of("city", "Atlantis"), null);   // a stubborn model
        if (g.contains("email")) {
            if (trace.isEmpty()) return new Decision("I should email the summary.", "send_email", Map.of("to", "boss@acme.example", "body", "Q3 report attached"), null);
            return new Decision("Report the outcome.", null, null, lastObs.startsWith("DENIED") ? "I did not send the email because you did not approve it." : "Email sent.");
        }
        if (g.contains("my favourite city")) {
            String fav = mem.get("favourite_city");
            if (fav == null) return new Decision("I don't know it yet.", null, null, "I don't know your favourite city yet.");
            if (trace.isEmpty()) return new Decision("Memory says " + fav + ".", "get_weather", Map.of("city", fav), null);
            return new Decision("Done.", null, null, "In your favourite city, " + fav + ", it is " + lastObs + ".");
        }
        return new Decision("Nothing to do.", null, null, "I can't help with that.");
    };

    static void tools() {
        System.out.println("1. Tools");
        check("tool list shown to the model", "- calculator[expression]: Evaluate a + - * / expression of two numbers, e.g. '23 * 7'", describeTools().split("\n")[0]);
        Agent a = new Agent(SCRIPTED, 5, x -> false, new HashMap<>());
        check("unknown tool -> a helpful error, not a crash", true, a.execute(new Decision("", "browse_web", Map.of(), null)).startsWith("ERROR: no tool named browse_web"));
        check("missing argument is caught before running", "ERROR: missing argument 'city'", a.execute(new Decision("", "get_weather", Map.of(), null)));
    }

    static void reactLoop() {
        System.out.println("2. ReAct loop (multi-step)");
        Agent a = new Agent(SCRIPTED, 8, x -> false, new HashMap<>());
        Object[] r = a.run("What is the weather in Paris in Fahrenheit?");
        @SuppressWarnings("unchecked") List<Step> trace = (List<Step>) r[1];
        for (Step s : trace) System.out.printf("      Thought: %s%n      Action: %s %s%n      Observation: %s%n", s.thought(), s.action(), s.args(), s.observation());
        check("final answer", "It is 64.4F (18C, light rain) in Paris.", r[0]);
        check("tool calls made", 3, trace.size());
        check("LLM calls (one per step + the final answer)", 4, a.llmCalls);
    }

    static void toolErrors() {
        System.out.println("3. Recovering from a tool error");
        Agent a = new Agent(SCRIPTED, 5, x -> false, new HashMap<>());
        Object[] r = a.run("What is 23 times 7?");
        @SuppressWarnings("unchecked") List<Step> trace = (List<Step>) r[1];
        check("first call failed with a clear message", true, trace.get(0).observation().startsWith("ERROR: expected 'number op number'"));
        check("the agent fixed its arguments and finished", "23 times 7 is 161.", r[0]);
    }

    static void stopping() {
        System.out.println("4. Stopping a stuck agent");
        Agent a = new Agent(SCRIPTED, 10, x -> false, new HashMap<>());
        Object[] r = a.run("What's the weather in Atlantis?");
        @SuppressWarnings("unchecked") List<Step> trace = (List<Step>) r[1];
        check("loop detection stops it", "STOPPED: repeating the same failing action", r[0]);
        check("...after only 2 wasted tool calls (not 10)", 2, trace.size());
        Agent tiny = new Agent(SCRIPTED, 2, x -> false, new HashMap<>());
        check("a step budget always bounds the run", "STOPPED: step budget (2) exhausted", tiny.run("What is the weather in Paris in Fahrenheit?")[0]);
    }

    static void approval() {
        System.out.println("5. Human approval for risky actions");
        SENT.clear();
        List<String> asked = new ArrayList<>();
        Object[] denied = new Agent(SCRIPTED, 5, req -> { asked.add(req); return false; }, new HashMap<>()).run("Email the Q3 report to my boss");
        check("the human was asked", true, asked.size() == 1 && asked.get(0).startsWith("send_email"));
        check("denied -> nothing sent, and the agent says so", "I did not send the email because you did not approve it. / sent=0", denied[0] + " / sent=" + SENT.size());
        Object[] ok = new Agent(SCRIPTED, 5, req -> true, new HashMap<>()).run("Email the Q3 report to my boss");
        check("approved -> sent once", "Email sent. / sent=1", ok[0] + " / sent=" + SENT.size());
    }

    static void memory() {
        System.out.println("6. Memory");
        Map<String, String> longTerm = new HashMap<>();
        check("session 1: nothing remembered yet", "I don't know your favourite city yet.", new Agent(SCRIPTED, 5, x -> false, longTerm).run("What's the weather in my favourite city?")[0]);
        longTerm.put("favourite_city", "Tokyo");                                   // e.g. saved after "my favourite city is Tokyo"
        check("session 2: recalled from long-term memory", "In your favourite city, Tokyo, it is 25C, sunny.", new Agent(SCRIPTED, 5, x -> false, longTerm).run("What's the weather in my favourite city?")[0]);
    }

    static void planVsReact() {
        System.out.println("7. Plan-and-execute vs ReAct");
        int subtasks = 6, reactCallsPerTask = 1, planCalls = 1, finalCall = 1;
        int react = subtasks * reactCallsPerTask + finalCall;                        // the big model decides every step
        int planExec = planCalls + finalCall;                                        // big model plans once; steps run by code or a small model
        check("big-model calls: ReAct vs plan-and-execute (6 independent steps)", "7 vs 2", react + " vs " + planExec);
        double bigPrice = 0.02, smallPrice = 0.001;
        check("cost if each step uses a small model instead", "$0.140 vs $0.046", String.format("$%.3f vs $%.3f", react * bigPrice, planExec * bigPrice + subtasks * smallPrice));
    }

    public static void main(String[] args) {
        tools();
        reactLoop();
        toolErrors();
        stopping();
        approval();
        memory();
        planVsReact();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-62s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
