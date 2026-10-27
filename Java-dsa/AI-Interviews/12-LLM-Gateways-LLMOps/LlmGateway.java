/*
 * ============================================================================
 *  AI 12 : AN LLM GATEWAY  (one front door for every model call)
 * ============================================================================
 *  Run:  java LlmGateway.java          (explanations: README.md in this folder)
 *
 *  A simulated clock keeps everything deterministic.
 *    1. virtual keys        per-team keys and budgets; the real provider keys stay secret
 *    2. rate limiting       token bucket per key
 *    3. routing             model aliases + "easy prompts go to the cheap model"
 *    4. reliability         retries with backoff, fallback provider, circuit breaker
 *    5. caching             exact cache + semantic cache (and its false-hit risk)
 *    6. cost & observability  per-team spend, latency p50/p95, redacted logs
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;

public class LlmGateway {

    static long now = 0;                                                         // simulated milliseconds

    // =====================================================================
    // providers (fakes)
    // =====================================================================

    record Model(String name, String provider, double inPerM, double outPerM, long latencyMs) {}

    static final Map<String, Model> MODELS = new LinkedHashMap<>();
    static {
        MODELS.put("big-model", new Model("big-model", "provider-a", 3.0, 15.0, 1200));
        MODELS.put("small-model", new Model("small-model", "provider-a", 0.15, 0.6, 300));
        MODELS.put("big-model-b", new Model("big-model-b", "provider-b", 3.5, 14.0, 1400));   // a similar model at another vendor
    }

    static final Set<String> DOWN = new HashSet<>();                             // providers currently failing
    static int providerCalls = 0;

    static String callProvider(Model m, String prompt) {
        providerCalls++;
        now += m.latencyMs();
        if (DOWN.contains(m.provider())) throw new RuntimeException("503 from " + m.provider());
        return m.name() + " says: " + (prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt);
    }

    static int tokens(String s) { return Math.max(1, s.length() / 4); }

    // =====================================================================
    // the gateway
    // =====================================================================

    record VirtualKey(String key, String team, double monthlyBudget, Set<String> allowedModels) {}
    record Response(String text, String model, boolean cached, double cost, long latencyMs, String error) {}

    static class Gateway {
        final Map<String, VirtualKey> keys = new HashMap<>();
        final Map<String, Double> spend = new HashMap<>();
        final Map<String, double[]> buckets = new HashMap<>();                   // key -> {tokens, lastRefillMs}
        final Map<String, String> exactCache = new HashMap<>();
        final List<Object[]> semanticCache = new ArrayList<>();                   // {embedding, answer}
        final Map<String, Integer> consecutiveFailures = new HashMap<>();
        final Map<String, Long> breakerOpenUntil = new HashMap<>();
        final List<String> logs = new ArrayList<>();
        final List<Long> latencies = new ArrayList<>();
        double semanticThreshold = 0.95;
        final double ratePerSec = 2, burst = 5;

        Response chat(String apiKey, String modelAlias, String prompt) {
            long start = now;
            VirtualKey vk = keys.get(apiKey);
            if (vk == null) return fail("401 unknown key", start);
            if (!rateOk(apiKey)) return fail("429 rate limit exceeded", start);
            String modelName = route(modelAlias, prompt);
            if (!vk.allowedModels().contains(modelName)) return fail("403 model " + modelName + " not allowed for " + vk.team(), start);
            if (spend.getOrDefault(vk.team(), 0.0) >= vk.monthlyBudget()) return fail("402 monthly budget exhausted for " + vk.team(), start);

            String norm = prompt.trim().toLowerCase().replaceAll("\\s+", " ");
            if (exactCache.containsKey(norm)) return done(vk, new Response(exactCache.get(norm), modelName, true, 0, now - start, null), prompt);
            double[] emb = embed(norm);
            for (Object[] e : semanticCache) if (cos((double[]) e[0], emb) >= semanticThreshold) return done(vk, new Response((String) e[1], modelName, true, 0, now - start, null), prompt);

            List<String> chain = new ArrayList<>(List.of(modelName));
            if (modelName.equals("big-model")) chain.add("big-model-b");            // fallback to the other vendor
            for (String candidate : chain) {
                Model m = MODELS.get(candidate);
                if (breakerOpenUntil.getOrDefault(m.provider(), 0L) > now) continue;   // circuit open: skip this provider fast
                for (int attempt = 1; attempt <= 2; attempt++) {
                    try {
                        String text = callProvider(m, prompt);
                        consecutiveFailures.put(m.provider(), 0);
                        double cost = tokens(prompt) / 1e6 * m.inPerM() + tokens(text) / 1e6 * m.outPerM();
                        exactCache.put(norm, text);
                        semanticCache.add(new Object[]{emb, text});
                        return done(vk, new Response(text, candidate, false, cost, now - start, null), prompt);
                    } catch (RuntimeException ex) {
                        int f = consecutiveFailures.merge(m.provider(), 1, Integer::sum);
                        if (f >= 2) breakerOpenUntil.put(m.provider(), now + 30_000);    // 2 failures in a row: open the breaker for 30 s
                        now += 200L * attempt;                                          // backoff before retrying
                        if (breakerOpenUntil.getOrDefault(m.provider(), 0L) > now) break;   // breaker just opened: stop retrying this provider
                    }
                }
            }
            return fail("503 all providers failed", start);
        }

        String route(String alias, String prompt) {
            return switch (alias) {
                case "smart" -> "big-model";
                case "fast" -> "small-model";
                case "auto" -> prompt.length() < 80 && !prompt.toLowerCase().matches(".*(analy[sz]e|prove|plan|step by step|code).*") ? "small-model" : "big-model";
                default -> alias;
            };
        }

        boolean rateOk(String key) {                                              // token bucket
            double[] b = buckets.computeIfAbsent(key, k -> new double[]{burst, now});
            b[0] = Math.min(burst, b[0] + (now - b[1]) / 1000.0 * ratePerSec);
            b[1] = now;
            if (b[0] < 1) return false;
            b[0] -= 1;
            return true;
        }

        Response done(VirtualKey vk, Response r, String prompt) {
            spend.merge(vk.team(), r.cost(), Double::sum);
            latencies.add(r.latencyMs());
            logs.add(vk.team() + " " + r.model() + (r.cached() ? " CACHE" : "") + " prompt=" + prompt.replaceAll("[\\w.+-]+@[\\w-]+\\.[\\w.]+", "[EMAIL]"));
            return r;
        }

        Response fail(String error, long start) { return new Response(null, null, false, 0, now - start, error); }
    }

    static double[] embed(String s) {                                            // bag-of-words "embedding" for the semantic cache
        double[] v = new double[64];
        for (String w : s.replaceAll("[^a-z0-9 ]", "").split(" ")) if (!w.isBlank() && !Set.of("the", "a", "is", "what", "my", "i", "do", "how", "can", "you", "it").contains(w)) v[Math.floorMod(w.hashCode(), 64)] += 1;
        return v;
    }

    static double cos(double[] a, double[] b) { double d = 0, na = 0, nb = 0; for (int i = 0; i < a.length; i++) { d += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; } return na == 0 || nb == 0 ? 0 : d / Math.sqrt(na * nb); }

    static Gateway newGateway() {
        Gateway g = new Gateway();
        g.keys.put("vk-support", new VirtualKey("vk-support", "support", 50.0, Set.of("big-model", "small-model", "big-model-b")));
        g.keys.put("vk-intern", new VirtualKey("vk-intern", "interns", 0.000001, Set.of("small-model")));
        return g;
    }

    // =====================================================================
    // demos
    // =====================================================================

    static void keysAndLimits() {
        System.out.println("1. Virtual keys  2. Rate limiting");
        Gateway g = newGateway();
        check("unknown key", "401 unknown key", g.chat("sk-stolen", "fast", "hi").error());
        check("a team can only use its allowed models", "403 model big-model not allowed for interns", g.chat("vk-intern", "smart", "hi").error());
        g.chat("vk-intern", "fast", "first request");
        check("budget exhausted -> blocked", "402 monthly budget exhausted for interns", g.chat("vk-intern", "fast", "second request").error());
        Gateway r = newGateway();
        int ok = 0, limited = 0;
        for (int i = 0; i < 8; i++) { if (r.rateOk("vk-support")) ok++; else limited++; }   // 8 requests arriving at the same instant
        check("a burst of 8 requests: first ~5 pass, then 429", true, ok >= 5 && limited >= 1);
        System.out.printf("      burst: %d allowed, %d rate-limited (bucket 5, refill 2/s)%n", ok, limited);
        now += 5_000;
        check("after waiting, the bucket refills", null, r.chat("vk-support", "fast", "later request").error());
    }

    static void routing() {
        System.out.println("3. Routing");
        Gateway g = newGateway();
        check("alias 'fast' -> small model", "small-model", g.route("fast", "x"));
        check("'auto': a short simple question -> small model", "small-model", g.route("auto", "What are your opening hours?"));
        check("'auto': a hard request -> big model", "big-model", g.route("auto", "Analyze this contract step by step and list the risks"));
        double big = 1000 / 1e6 * 3 + 200 / 1e6 * 15, small = 1000 / 1e6 * 0.15 + 200 / 1e6 * 0.6;
        check("cost per request big vs small (1000 in / 200 out)", "$0.00600 vs $0.00027", String.format("$%.5f vs $%.5f", big, small));
        check("routing 70% of traffic to small saves", "67%", String.format("%.0f%%", 100 * (1 - (0.3 * big + 0.7 * small) / big)));
    }

    static void reliability() {
        System.out.println("4. Retries, fallback, circuit breaker");
        Gateway g = newGateway();
        DOWN.add("provider-a");
        Response r = g.chat("vk-support", "smart", "Summarise the incident report for the outage");
        check("provider A is down -> answered by provider B", "big-model-b", r.model());
        int callsBefore = providerCalls;
        now += 10;
        Response r2 = g.chat("vk-support", "smart", "Another hard question about architecture please");
        check("breaker opened after 2 failures: A is skipped (1 call, to B)", 1, providerCalls - callsBefore);
        check("...so the second request is much faster", true, r2.latencyMs() < r.latencyMs());
        System.out.printf("      latency with retries+fallback %d ms, with breaker open %d ms%n", r.latencyMs(), r2.latencyMs());
        DOWN.clear();
        now += 31_000;
        check("after the cool-down, provider A is used again", "big-model", g.chat("vk-support", "smart", "Explain our refund rules in detail please now").model());
    }

    static void caching() {
        System.out.println("5. Caching");
        Gateway g = newGateway();
        int before = providerCalls;
        g.chat("vk-support", "fast", "What is the refund policy?");
        Response exact = g.chat("vk-support", "fast", "  what is the REFUND policy?  ");
        check("exact cache (after normalising case/spaces): no provider call", "true / calls=1", exact.cached() + " / calls=" + (providerCalls - before));
        Response semantic = g.chat("vk-support", "fast", "refund policy what is it");
        check("semantic cache: same meaning, different words", true, semantic.cached());
        Gateway loose = newGateway();
        loose.semanticThreshold = 0.5;
        loose.chat("vk-support", "fast", "cancel my order 1234");
        Response wrong = loose.chat("vk-support", "fast", "cancel my order 9876");
        check("a LOOSE threshold returns the wrong cached answer (order 1234!)", true, wrong.cached() && wrong.text().contains("1234"));
    }

    static void costAndObservability() {
        System.out.println("6. Cost and observability");
        Gateway g = newGateway();
        for (int i = 0; i < 40; i++) { now += 2_000; g.chat("vk-support", i % 4 == 0 ? "smart" : "fast", "Question number " + i + " from user" + i + "@example.com about shipping"); }
        check("spend is tracked per team", true, g.spend.get("support") > 0);
        System.out.printf("      support team spend: $%.6f over 40 requests%n", g.spend.get("support"));
        List<Long> sorted = new ArrayList<>(g.latencies);
        Collections.sort(sorted);
        long p50 = sorted.get(sorted.size() / 2), p95 = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        check("p50 latency (mostly small model)", 300L, p50);
        check("p95 latency (the big-model requests)", 1200L, p95);
        check("logs never contain raw emails", true, g.logs.stream().noneMatch(l -> l.contains("@example.com")));
    }

    public static void main(String[] args) {
        keysAndLimits();
        routing();
        reliability();
        caching();
        costAndObservability();
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
