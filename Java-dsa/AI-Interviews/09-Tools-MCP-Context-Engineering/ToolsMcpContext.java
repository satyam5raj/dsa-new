/*
 * ============================================================================
 *  AI 09 : TOOLS, MCP, CONTEXT ENGINEERING, DYNAMIC TOOL-OUTPUT COMPRESSION
 * ============================================================================
 *  Run:  java ToolsMcpContext.java          (explanations: README.md in this folder)
 *
 *    1. tool schemas        the JSON schema a model sees for a function
 *    2. MCP in miniature    JSON-RPC 2.0: initialize, tools/list, tools/call, errors
 *    3. tool selection      50 tools is too many: pick the relevant few per request
 *    4. context assembly    fit system + tools + history + memory + results in a budget
 *    5. compression         6 strategies on a 5,000-line log; does the ERROR survive?
 *    6. dynamic budgets     compress harder as the context fills up
 *    7. offloading          store the full output, give the model a handle + preview
 * ============================================================================
 */

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class ToolsMcpContext {

    static int tokens(String s) { return (s.length() + 3) / 4; }                 // ~4 characters per token

    // =====================================================================
    // 1. tool schemas
    // =====================================================================

    static String schema(String name, String description, Map<String, String[]> params, List<String> required) {
        String props = params.entrySet().stream().map(e -> "\"" + e.getKey() + "\": {\"type\": \"" + e.getValue()[0] + "\", \"description\": \"" + e.getValue()[1] + "\"}").collect(Collectors.joining(", "));
        return "{\"name\": \"" + name + "\", \"description\": \"" + description + "\", \"input_schema\": {\"type\": \"object\", \"properties\": {" + props
                + "}, \"required\": " + required.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(", ", "[", "]")) + "}}";
    }

    static void toolSchemas() {
        System.out.println("1. Tool schemas");
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("order_id", new String[]{"string", "The order number, e.g. A-1234"});
        params.put("include_items", new String[]{"boolean", "Whether to list the items"});
        String s = schema("get_order_status", "Look up the shipping status of one order. Use when the user asks where their order is.", params, List.of("order_id"));
        System.out.println("      " + s);
        check("schema names the required argument", true, s.contains("\"required\": [\"order_id\"]"));
        check("the description says WHEN to use the tool", true, s.contains("Use when"));
    }

    // =====================================================================
    // 2. MCP in miniature (JSON-RPC 2.0 messages as maps)
    // =====================================================================

    static class McpServer {
        final Map<String, Function<Map<String, Object>, String>> tools = new LinkedHashMap<>();
        boolean initialized = false;
        McpServer() {
            tools.put("search_docs", a -> "3 results for '" + a.get("query") + "'");
            tools.put("get_ticket", a -> "Ticket " + a.get("id") + ": status=open, priority=high");
        }

        Map<String, Object> handle(Map<String, Object> req) {
            Object id = req.get("id");
            String method = (String) req.get("method");
            @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) req.getOrDefault("params", Map.of());
            if (!"2.0".equals(req.get("jsonrpc"))) return error(id, -32600, "Invalid Request");
            if (!method.equals("initialize") && !initialized) return error(id, -32002, "Server not initialized");
            switch (method) {
                case "initialize": initialized = true;
                    return result(id, Map.of("protocolVersion", "2025-06-18", "capabilities", Map.of("tools", Map.of()), "serverInfo", Map.of("name", "acme-support", "version", "1.0")));
                case "tools/list": return result(id, Map.of("tools", new ArrayList<>(tools.keySet())));
                case "tools/call": {
                    String name = (String) params.get("name");
                    if (!tools.containsKey(name)) return error(id, -32602, "Unknown tool: " + name);
                    @SuppressWarnings("unchecked") Map<String, Object> args = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
                    return result(id, Map.of("content", List.of(Map.of("type", "text", "text", tools.get(name).apply(args))), "isError", false));
                }
                default: return error(id, -32601, "Method not found: " + method);
            }
        }
        static Map<String, Object> result(Object id, Object r) { return Map.of("jsonrpc", "2.0", "id", id, "result", r); }
        static Map<String, Object> error(Object id, int code, String msg) { return Map.of("jsonrpc", "2.0", "id", id, "error", Map.of("code", code, "message", msg)); }
    }

    static Map<String, Object> rpc(int id, String method, Map<String, Object> params) { return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params); }

    static void mcp() {
        System.out.println("2. MCP in miniature (JSON-RPC 2.0)");
        McpServer server = new McpServer();
        check("calling a tool before initialize is rejected", -32002, ((Map<?, ?>) server.handle(rpc(1, "tools/list", Map.of())).get("error")).get("code"));
        Map<String, Object> init = server.handle(rpc(2, "initialize", Map.of("protocolVersion", "2025-06-18", "clientInfo", Map.of("name", "my-agent"))));
        check("initialize returns server info and capabilities", "acme-support", ((Map<?, ?>) ((Map<?, ?>) init.get("result")).get("serverInfo")).get("name"));
        check("tools/list -> discover the tools at runtime", "[search_docs, get_ticket]", ((Map<?, ?>) server.handle(rpc(3, "tools/list", Map.of())).get("result")).get("tools").toString());
        Map<String, Object> call = server.handle(rpc(4, "tools/call", Map.of("name", "get_ticket", "arguments", Map.of("id", "T-77"))));
        check("tools/call returns content blocks", "Ticket T-77: status=open, priority=high", ((Map<?, ?>) ((List<?>) ((Map<?, ?>) call.get("result")).get("content")).get(0)).get("text"));
        check("unknown tool -> invalid params error", -32602, ((Map<?, ?>) server.handle(rpc(5, "tools/call", Map.of("name", "drop_database"))).get("error")).get("code"));
        check("unknown method -> method not found", -32601, ((Map<?, ?>) server.handle(rpc(6, "resources/delete_all", Map.of())).get("error")).get("code"));
        check("responses echo the request id (to match async replies)", 4, call.get("id"));
    }

    // =====================================================================
    // 3. dynamic tool selection ("tool RAG")
    // =====================================================================

    static void toolSelection() {
        System.out.println("3. Selecting the relevant tools");
        List<String[]> catalogue = new ArrayList<>();                                // {name, description}
        String[] domains = {"order", "invoice", "refund", "ticket", "user", "inventory", "shipment", "coupon", "review", "report"};
        String[] verbs = {"get", "create", "update", "cancel", "list"};
        for (String d : domains) for (String v : verbs) catalogue.add(new String[]{v + "_" + d, v + " a " + d + " record in the " + d + " system"});
        String query = "where is my shipment and can I cancel the order";
        Set<String> q = new HashSet<>(Arrays.asList(query.split(" ")));
        // score: does the tool's ENTITY appear in the request (strong), then its ACTION ('get' is the default lookup)
        ToIntFunction<String[]> score = t -> { String[] vn = t[0].split("_"); return (q.contains(vn[1]) ? 10 : 0) + (q.contains(vn[0]) ? 2 : vn[0].equals("get") ? 1 : 0); };
        List<String[]> picked = catalogue.stream().sorted(Comparator.comparingInt((String[] t) -> -score.applyAsInt(t))).limit(5).toList();
        System.out.println("      selected: " + picked.stream().map(t -> t[0]).toList());
        int all = catalogue.stream().mapToInt(t -> tokens(schema(t[0], t[1], Map.of("id", new String[]{"string", "record id"}), List.of("id")))).sum();
        int few = picked.stream().mapToInt(t -> tokens(schema(t[0], t[1], Map.of("id", new String[]{"string", "record id"}), List.of("id")))).sum();
        check("catalogue size", 50, catalogue.size());
        check("picked tools include cancel_order and get_shipment", true, picked.stream().map(t -> t[0]).toList().containsAll(List.of("cancel_order", "get_shipment")));
        System.out.printf("      tool-schema tokens per request: all 50 = %,d, selected 5 = %,d%n", all, few);
        check("selecting 5 of 50 tools saves ~90% of the tool tokens", true, few < all * 0.15);
    }

    // =====================================================================
    // 4. context assembly under a budget
    // =====================================================================

    static void contextAssembly() {
        System.out.println("4. Context assembly");
        int window = 16_000, reserveForAnswer = 2_000;
        Map<String, Integer> parts = new LinkedHashMap<>();
        parts.put("system prompt", 1_200);
        parts.put("tool schemas (5 selected)", 700);
        parts.put("long-term memories (top 3)", 300);
        parts.put("current user message", 150);
        int fixed = parts.values().stream().mapToInt(Integer::intValue).sum();
        int forHistoryAndTools = window - reserveForAnswer - fixed;
        check("budget left for history + tool results", 11_650, forHistoryAndTools);
        List<Integer> turns = new ArrayList<>(List.of(1500, 2200, 1800, 2600, 900, 3100, 1200));   // oldest -> newest
        int raw = turns.stream().mapToInt(Integer::intValue).sum();
        int keepRecent = 3, summaryTokens = 400;
        int recent = turns.subList(turns.size() - keepRecent, turns.size()).stream().mapToInt(Integer::intValue).sum();
        check("full history would need", 13_300, raw);
        check("summary of old turns + last 3 verbatim", 5_600, summaryTokens + recent);
        check("...leaving room for tool results", 6_050, forHistoryAndTools - summaryTokens - recent);
    }

    // =====================================================================
    // 5. tool-output compression strategies
    // =====================================================================

    static String bigLog() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5000; i++) {
            if (i == 3127) sb.append(String.format("2026-10-27T10:%02d:%02d ERROR payment-svc: connection pool exhausted (max=20) order=A-9931%n", (i / 60) % 60, i % 60));
            else if (i % 500 == 0) sb.append(String.format("2026-10-27T10:%02d:%02d WARN  payment-svc: slow query 2300ms%n", (i / 60) % 60, i % 60));
            else sb.append(String.format("2026-10-27T10:%02d:%02d INFO  payment-svc: processed request ok%n", (i / 60) % 60, i % 60));
        }
        return sb.toString();
    }

    static String headTail(String out, int budgetTokens) {
        if (tokens(out) <= budgetTokens) return out;
        int keep = budgetTokens * 4 / 2;
        return out.substring(0, keep) + "\n... [" + (out.length() - 2 * keep) + " chars omitted] ...\n" + out.substring(out.length() - keep);
    }

    static String dedupeLines(String out) {                                        // collapse repeated lines (ignoring timestamps)
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> example = new HashMap<>();
        for (String line : out.split("\n")) { String key = line.replaceFirst("^\\S+\\s+", ""); counts.merge(key, 1, Integer::sum); example.putIfAbsent(key, line); }
        return counts.entrySet().stream().map(e -> (e.getValue() > 1 ? "[x" + e.getValue() + "] " : "") + example.get(e.getKey())).collect(Collectors.joining("\n"));
    }

    static String severityFilter(String out) {                                      // keep only WARN/ERROR + a count of the rest
        List<String> keep = new ArrayList<>();
        int dropped = 0;
        for (String l : out.split("\n")) if (l.contains("ERROR") || l.contains("WARN")) keep.add(l); else dropped++;
        keep.add("(" + dropped + " INFO lines omitted)");
        return String.join("\n", keep);
    }

    static String queryFocused(String out, String goal, int maxLines) {           // keep the lines most relevant to the agent's CURRENT goal
        Set<String> g = new HashSet<>(Arrays.asList(goal.toLowerCase().split("\\W+")));
        List<String> lines = Arrays.asList(out.split("\n"));
        return lines.stream().sorted(Comparator.comparingLong((String l) -> -Arrays.stream(l.toLowerCase().split("\\W+")).filter(g::contains).count()))
                .limit(maxLines).collect(Collectors.joining("\n"));
    }

    static String jsonProjection(List<Map<String, Object>> rows, List<String> fields, int maxRows) {   // keep only the needed fields + aggregate
        String body = rows.stream().limit(maxRows).map(r -> fields.stream().map(f -> f + "=" + r.get(f)).collect(Collectors.joining(", ", "{", "}"))).collect(Collectors.joining("\n"));
        long failed = rows.stream().filter(r -> "failed".equals(r.get("status"))).count();
        return body + "\n(" + rows.size() + " rows total, " + failed + " failed; showing " + Math.min(maxRows, rows.size()) + ")";
    }

    static void compression() {
        System.out.println("5. Tool-output compression (a 5,000-line log; the cause is ONE line in the middle)");
        String log = bigLog();
        String needle = "connection pool exhausted";
        String goal = "why are payments failing error";
        int budget = 1_000;
        check("raw tool output: > 50,000 tokens for one tool call", true, tokens(log) > 50_000);
        System.out.printf("      raw log = %,d tokens; budget for this tool result = %,d%n", tokens(log), budget);
        Map<String, String> results = new LinkedHashMap<>();
        results.put("head+tail truncation", headTail(log, budget));
        results.put("dedupe repeated lines", dedupeLines(log));
        results.put("severity filter (WARN/ERROR)", severityFilter(log));
        results.put("query-focused (goal relevance)", queryFocused(log, goal, 12));
        for (var e : results.entrySet())
            System.out.printf("      %-32s %,7d tokens   error kept: %s%n", e.getKey(), tokens(e.getValue()), e.getValue().contains(needle));
        check("naive truncation LOSES the error in the middle", false, results.get("head+tail truncation").contains(needle));
        check("dedupe keeps it and shrinks to < 200 tokens", true, results.get("dedupe repeated lines").contains(needle) && tokens(results.get("dedupe repeated lines")) < 200);
        check("query-focused extraction keeps it, first line", true, results.get("query-focused (goal relevance)").split("\n")[0].contains(needle));
        check("severity filter keeps it within budget", true, results.get("severity filter (WARN/ERROR)").contains(needle) && tokens(results.get("severity filter (WARN/ERROR)")) < budget);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 300; i++) { Map<String, Object> r = new LinkedHashMap<>(); r.put("id", "P-" + i); r.put("status", i % 37 == 0 ? "failed" : "ok"); r.put("amount", 10 + i); r.put("raw_gateway_payload", "x".repeat(400)); r.put("internal_trace", "y".repeat(200)); rows.add(r); }
        String fullJson = rows.toString(), projected = jsonProjection(rows, List.of("id", "status", "amount"), 10);
        System.out.printf("      JSON 300 rows: full %,d tokens -> projected %,d tokens%n", tokens(fullJson), tokens(projected));
        check("JSON projection keeps the aggregate the agent needs", true, projected.contains("300 rows total, 9 failed"));
        check("...at < 2% of the size", true, tokens(projected) < tokens(fullJson) * 0.02);
    }

    // =====================================================================
    // 6. dynamic budgets, 7. offloading
    // =====================================================================

    static int toolBudget(int windowTokens, int usedTokens) {                     // compress harder as the context fills
        int remaining = windowTokens - usedTokens;
        return Math.max(200, Math.min(4_000, remaining / 4));                     // at most 25% of what's left, floor 200, cap 4K
    }

    static final Map<String, String> ARTIFACTS = new HashMap<>();

    static String offload(String fullOutput, int previewLines) {
        String handle = "artifact://" + Integer.toHexString(fullOutput.hashCode());
        ARTIFACTS.put(handle, fullOutput);
        String[] lines = fullOutput.split("\n");
        return "Output stored at " + handle + " (" + lines.length + " lines). Preview:\n" + String.join("\n", Arrays.copyOf(lines, previewLines))
                + "\nCall read_artifact(handle, query) to search it.";
    }

    static String readArtifact(String handle, String query) { return queryFocused(ARTIFACTS.get(handle), query, 3); }

    static void dynamicAndOffloading() {
        System.out.println("6. Dynamic budgets  7. Offloading");
        check("early in a run (2K of 128K used): budget per tool result", 4_000, toolBudget(128_000, 2_000));
        check("context nearly full (120K used): budget shrinks", 2_000, toolBudget(128_000, 120_000));
        check("almost out of room: floor of 200", 200, toolBudget(128_000, 127_500));
        String handleMsg = offload(bigLog(), 3);
        check("the model gets a short handle + preview", true, tokens(handleMsg) < 120);
        String handle = handleMsg.substring(handleMsg.indexOf("artifact://"), handleMsg.indexOf(" (5000"));
        check("later, a targeted read finds the error in the full output", true, readArtifact(handle, "error payments failing").contains("connection pool exhausted"));
    }

    public static void main(String[] args) {
        toolSchemas();
        mcp();
        toolSelection();
        contextAssembly();
        compression();
        dynamicAndOffloading();
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
