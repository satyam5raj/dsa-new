/*
 * ============================================================================
 *  AI 13 : A CLOUD-RUN-READY AGENT SERVICE  (JDK only, no dependencies)
 * ============================================================================
 *  Run the self-test:   java AgentServer.java            (starts on a free port, calls itself, stops)
 *  Run as a server:     PORT=8080 java AgentServer.java serve
 *  Container + deploy:  see Dockerfile, deploy.sh and README.md in this folder
 *
 *  What makes it "Cloud Run ready":
 *    - listens on $PORT (Cloud Run injects it), binds 0.0.0.0
 *    - stateless: conversation state lives in a SessionStore (Firestore/Redis in prod)
 *    - /healthz for startup/liveness probes; fast startup
 *    - POST /chat returns JSON; GET /chat/stream streams tokens as Server-Sent Events
 *    - secrets come from env vars (mounted from Secret Manager), never logged
 *    - structured JSON logs to stdout (Cloud Logging parses them)
 *    - graceful shutdown on SIGTERM (Cloud Run sends it before stopping an instance)
 *    - a per-request deadline so a stuck model call can't hang the request
 * ============================================================================
 */

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class AgentServer {

    // =====================================================================
    // state lives OUTSIDE the instance (any instance can serve any request)
    // =====================================================================

    interface SessionStore { List<String> history(String sessionId); void append(String sessionId, String turn); }

    /** In-memory stand-in. On Cloud Run use Firestore, Memorystore (Redis) or Cloud SQL: instances come and go. */
    static class InMemoryStore implements SessionStore {
        final ConcurrentHashMap<String, List<String>> data = new ConcurrentHashMap<>();
        public List<String> history(String id) { return data.getOrDefault(id, List.of()); }
        public void append(String id, String turn) { data.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(turn); }
    }

    // =====================================================================
    // the "agent" (a stand-in for an LLM + tools call)
    // =====================================================================

    static String agentReply(String message, List<String> history) {
        String m = message.toLowerCase();
        if (m.contains("order") && m.matches(".*\\b[a-z]-\\d+\\b.*")) {
            Matcher id = Pattern.compile("\\b([a-zA-Z]-\\d+)\\b").matcher(message);
            id.find();
            return "Order " + id.group(1).toUpperCase() + " is out for delivery (tool: get_order_status).";
        }
        if (m.contains("what did i ask")) return history.isEmpty() ? "This is our first message." : "Earlier you said: \"" + history.get(history.size() - 2).substring(6) + "\"";
        return "I can help with orders. Try: where is order A-1001?";
    }

    // =====================================================================
    // the HTTP server
    // =====================================================================

    static HttpServer server;
    static ExecutorService REQUEST_POOL;
    static final SessionStore STORE = new InMemoryStore();
    static final ExecutorService MODEL_POOL = Executors.newCachedThreadPool();
    static volatile boolean draining = false;
    static final List<String> LOG = new CopyOnWriteArrayList<>();

    static void log(String severity, String msg, Map<String, Object> fields) {    // one JSON object per line -> Cloud Logging
        StringBuilder sb = new StringBuilder("{\"severity\":\"").append(severity).append("\",\"message\":\"").append(msg.replace("\"", "'")).append('"');
        fields.forEach((k, v) -> sb.append(",\"").append(k).append("\":\"").append(String.valueOf(v).replace("\"", "'")).append('"'));
        String line = sb.append('}').toString();
        LOG.add(line);
        if (!Boolean.getBoolean("quiet")) System.out.println(line);
    }

    static HttpServer start(int port, String apiToken) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        REQUEST_POOL = Executors.newFixedThreadPool(16);                          // Cloud Run "concurrency" = requests per instance
        s.setExecutor(REQUEST_POOL);
        s.createContext("/healthz", ex -> respond(ex, draining ? 503 : 200, draining ? "{\"status\":\"draining\"}" : "{\"status\":\"ok\"}", "application/json"));
        s.createContext("/chat", ex -> {
            if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, "{\"error\":\"use POST\"}", "application/json"); return; }
            if (!authorised(ex, apiToken)) { respond(ex, 401, "{\"error\":\"unauthorised\"}", "application/json"); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String session = field(body, "session_id"), message = field(body, "message");
            if (session == null || message == null) { respond(ex, 400, "{\"error\":\"session_id and message are required\"}", "application/json"); return; }
            long t0 = System.nanoTime();
            Future<String> call = MODEL_POOL.submit(() -> agentReply(message, STORE.history(session)));
            String reply;
            try { reply = call.get(10, TimeUnit.SECONDS); }                       // per-request deadline
            catch (TimeoutException e) { call.cancel(true); respond(ex, 504, "{\"error\":\"model timeout\"}", "application/json"); return; }
            catch (Exception e) { respond(ex, 500, "{\"error\":\"internal\"}", "application/json"); return; }
            STORE.append(session, "user: " + message);
            STORE.append(session, "assistant: " + reply);
            log("INFO", "chat", Map.of("session", session, "latency_ms", (System.nanoTime() - t0) / 1_000_000, "reply_chars", reply.length()));
            respond(ex, 200, "{\"reply\":\"" + reply.replace("\"", "\\\"") + "\"}", "application/json");
        });
        s.createContext("/chat/stream", ex -> {                                  // Server-Sent Events: tokens as they are generated
            if (!authorised(ex, apiToken)) { respond(ex, 401, "{\"error\":\"unauthorised\"}", "application/json"); return; }
            String q = URLDecoder.decode(Optional.ofNullable(ex.getRequestURI().getQuery()).orElse("").replaceFirst("^message=", ""), StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/event-stream");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, 0);                                       // 0 = chunked: send as we go
            try (OutputStream os = ex.getResponseBody()) {
                for (String token : agentReply(q, List.of()).split(" ")) { os.write(("data: " + token + "\n\n").getBytes(StandardCharsets.UTF_8)); os.flush(); }
                os.write("event: done\ndata: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            }
        });
        s.start();
        log("INFO", "listening", Map.of("port", s.getAddress().getPort(), "model", System.getenv().getOrDefault("MODEL_NAME", "fake-model")));
        return s;
    }

    static boolean authorised(HttpExchange ex, String token) {
        if (token == null) return true;                                           // e.g. relying on Cloud Run IAM (--no-allow-unauthenticated) instead
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return h != null && h.equals("Bearer " + token);
    }

    static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static void respond(HttpExchange ex, int status, String body, String type) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    static void gracefulShutdown() {                                             // SIGTERM: stop taking work, finish in-flight requests
        draining = true;
        log("INFO", "SIGTERM received, draining", Map.of());
        server.stop(5);                                                           // wait up to 5 s for active exchanges (Cloud Run gives ~10 s)
        REQUEST_POOL.shutdown();                                                  // non-daemon threads would otherwise keep the JVM alive
        MODEL_POOL.shutdown();
    }

    // =====================================================================
    // main: "serve" for real, otherwise a self-test over real HTTP
    // =====================================================================

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("serve")) {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            server = start(port, System.getenv("APP_API_TOKEN"));                 // the secret comes from env (Secret Manager mount)
            Runtime.getRuntime().addShutdownHook(new Thread(AgentServer::gracefulShutdown));
            return;
        }
        System.setProperty("quiet", "true");
        String secret = "s3cr3t-token-from-secret-manager";
        server = start(0, secret);                                                // port 0 = any free port
        int port = server.getAddress().getPort();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String base = "http://localhost:" + port;

        System.out.println("1. Health and auth");
        check("GET /healthz -> 200 ok", "200 {\"status\":\"ok\"}", call(http, HttpRequest.newBuilder(URI.create(base + "/healthz")).build()));
        check("POST /chat without a token -> 401", "401", call(http, post(base, null, "{\"session_id\":\"s1\",\"message\":\"hi\"}")).split(" ")[0]);
        check("bad request -> 400", "400", call(http, post(base, secret, "{\"message\":\"hi\"}")).split(" ")[0]);

        System.out.println("2. Chat and stateless sessions");
        check("agent answers with a tool result", "200 {\"reply\":\"Order A-1001 is out for delivery (tool: get_order_status).\"}", call(http, post(base, secret, "{\"session_id\":\"s1\",\"message\":\"Where is order a-1001?\"}")));
        check("follow-up uses history from the session store", true, call(http, post(base, secret, "{\"session_id\":\"s1\",\"message\":\"What did I ask before?\"}")).contains("Where is order a-1001?"));
        check("another session has its own history", true, call(http, post(base, secret, "{\"session_id\":\"s2\",\"message\":\"What did I ask before?\"}")).contains("first message"));

        System.out.println("3. Streaming (SSE)");
        String sse = call(http, HttpRequest.newBuilder(URI.create(base + "/chat/stream?message=" + URLEncoder.encode("where is order B-7", StandardCharsets.UTF_8))).header("Authorization", "Bearer " + secret).build());
        check("tokens arrive as 'data:' events", true, sse.startsWith("200 data: Order") && sse.contains("data: B-7"));
        check("the stream ends with a done event", true, sse.trim().endsWith("data: [DONE]"));

        System.out.println("4. Concurrency (one instance serves many requests)");
        List<CompletableFuture<HttpResponse<String>>> inflight = new ArrayList<>();
        for (int i = 0; i < 40; i++) inflight.add(http.sendAsync(post(base, secret, "{\"session_id\":\"load" + i + "\",\"message\":\"where is order c-" + i + "\"}"), HttpResponse.BodyHandlers.ofString()));
        long ok = inflight.stream().map(CompletableFuture::join).filter(r -> r.statusCode() == 200).count();
        check("40 concurrent requests all succeed", 40L, ok);

        System.out.println("5. Logs and shutdown");
        check("structured JSON logs", true, LOG.stream().allMatch(l -> l.startsWith("{\"severity\":")));
        check("the secret never appears in the logs", true, LOG.stream().noneMatch(l -> l.contains(secret)));
        gracefulShutdown();
        check("after SIGTERM the server stops accepting requests", true, refused(http, base));
        summary();
        System.exit(0);                                                           // the HttpClient's threads are non-daemon too
    }

    static HttpRequest post(String base, String token, String json) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/chat")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b.build();
    }

    static String call(HttpClient http, HttpRequest req) throws Exception { HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString()); return r.statusCode() + " " + r.body().replace("\n", " ").trim(); }

    static boolean refused(HttpClient http, String base) { try { http.send(HttpRequest.newBuilder(URI.create(base + "/healthz")).timeout(Duration.ofSeconds(2)).build(), HttpResponse.BodyHandlers.ofString()); return false; } catch (Exception e) { return true; } }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-56s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a.length() > 90 ? a.substring(0, 90) + "..." : a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
