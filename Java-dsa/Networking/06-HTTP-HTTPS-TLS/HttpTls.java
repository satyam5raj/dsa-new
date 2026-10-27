/*
 * ============================================================================
 *  NETWORKING 06 : HTTP, HTTPS AND TLS  (a real HTTP server + real cryptography)
 * ============================================================================
 *  Run:  java HttpTls.java          (explanations: README.md in this folder)
 *
 *    1. raw HTTP/1.1          the text format on the wire, chunked transfer encoding
 *    2. methods + status      GET/POST/PUT/DELETE/HEAD/OPTIONS, 2xx/3xx/4xx/5xx, safe vs idempotent
 *    3. headers               content negotiation, gzip compression, cookies, redirects
 *    4. caching               Cache-Control, ETag + If-None-Match -> 304 Not Modified
 *    5. keep-alive            the client reuses one TCP connection
 *    6. HTTP versions         HTTP/1.1 vs HTTP/2 multiplexing vs HTTP/3 (model)
 *    7. Server-Sent Events    a real event stream
 *    8. TLS crypto            X25519 key exchange, HKDF, AES-GCM (tamper detection), asym vs sym speed
 *    9. certificates          a CA -> intermediate -> leaf chain with real signatures; hostname, expiry, trust
 *   10. forward secrecy, mTLS, SNI, ALPN
 * ============================================================================
 */

import com.sun.net.httpserver.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.GZIPOutputStream;

public class HttpTls {

    static final Map<String, String> ORDERS = new ConcurrentHashMap<>();
    static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    static final Set<Integer> CLIENT_PORTS = ConcurrentHashMap.newKeySet();
    static String product = "{\"id\":7,\"name\":\"keyboard\",\"price\":49}";

    // ---------------------------------------------------------------------
    // the server
    // ---------------------------------------------------------------------
    static HttpServer startServer() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.setExecutor(Executors.newFixedThreadPool(4));

        s.createContext("/orders", ex -> {
            CLIENT_PORTS.add(ex.getRemoteAddress().getPort());
            String m = ex.getRequestMethod(), path = ex.getRequestURI().getPath();
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            switch (m) {
                case "POST" -> {                                   // not idempotent: every call creates a new order
                    String id = String.valueOf(NEXT_ID.getAndIncrement());
                    ORDERS.put(id, body);
                    ex.getResponseHeaders().add("Location", "/orders/" + id);
                    send(ex, 201, "{\"id\":" + id + "}");
                }
                case "PUT" -> {                                    // idempotent: same request -> same final state
                    String id = path.substring("/orders/".length());
                    boolean existed = ORDERS.containsKey(id);
                    ORDERS.put(id, body);
                    send(ex, existed ? 200 : 201, body);
                }
                case "DELETE" -> { ORDERS.remove(path.substring("/orders/".length())); send(ex, 204, null); }
                case "GET" -> {
                    String id = path.length() > "/orders/".length() ? path.substring("/orders/".length()) : null;
                    if (id == null) send(ex, 200, ORDERS.toString());
                    else if (ORDERS.containsKey(id)) send(ex, 200, ORDERS.get(id));
                    else send(ex, 404, "{\"error\":\"not found\"}");
                }
                case "HEAD" -> {                                   // same headers as GET (incl. length), no body
                    String id = path.length() > "/orders/".length() ? path.substring("/orders/".length()) : null;
                    String b = id == null ? ORDERS.toString() : ORDERS.get(id);
                    ex.getResponseHeaders().add("Content-Type", "application/json");
                    ex.getResponseHeaders().add("X-Content-Length", String.valueOf(b == null ? 0 : b.length()));
                    ex.sendResponseHeaders(b == null ? 404 : 200, -1);
                    ex.close();
                }
                case "OPTIONS" -> { ex.getResponseHeaders().add("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS"); send(ex, 204, null); }
                default -> send(ex, 405, null);
            }
        });

        s.createContext("/product", ex -> {                       // caching + negotiation + compression
            String etag = "\"" + Integer.toHexString(product.hashCode()) + "\"";
            ex.getResponseHeaders().add("ETag", etag);
            ex.getResponseHeaders().add("Cache-Control", "public, max-age=60");
            ex.getResponseHeaders().add("Vary", "Accept, Accept-Encoding");
            if (etag.equals(ex.getRequestHeaders().getFirst("If-None-Match"))) { send(ex, 304, null); return; }
            String accept = Optional.ofNullable(ex.getRequestHeaders().getFirst("Accept")).orElse("*/*");
            boolean json = !accept.contains("text/plain");
            String body = json ? product : "keyboard, $49";
            ex.getResponseHeaders().add("Content-Type", json ? "application/json" : "text/plain; charset=utf-8");
            if (ex.getRequestURI().getQuery() != null && ex.getRequestURI().getQuery().contains("big")) body = body.repeat(200);
            String enc = ex.getRequestHeaders().getFirst("Accept-Encoding");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (enc != null && enc.contains("gzip")) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (GZIPOutputStream gz = new GZIPOutputStream(bos)) { gz.write(bytes); }
                bytes = bos.toByteArray();
                ex.getResponseHeaders().add("Content-Encoding", "gzip");
            }
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });

        s.createContext("/old-path", ex -> { ex.getResponseHeaders().add("Location", "/product"); send(ex, 301, null); });
        s.createContext("/login", ex -> {
            ex.getResponseHeaders().add("Set-Cookie", "session=abc123; HttpOnly; Secure; SameSite=Lax; Path=/");
            send(ex, 200, "ok");
        });
        s.createContext("/boom", ex -> send(ex, 503, "{\"error\":\"upstream unavailable\"}"));
        s.createContext("/stream", ex -> {                        // chunked: length unknown in advance
            ex.sendResponseHeaders(200, 0);
            try (OutputStream o = ex.getResponseBody()) { for (int i = 1; i <= 3; i++) o.write(("part" + i + ";").getBytes()); }
        });
        s.createContext("/events", ex -> {                        // Server-Sent Events
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.getResponseHeaders().add("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream o = ex.getResponseBody()) {
                for (int i = 1; i <= 3; i++) { o.write(("id: " + i + "\nevent: price\ndata: {\"tick\":" + i + "}\n\n").getBytes()); o.flush(); }
            }
        });
        s.start();
        return s;
    }

    static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        if (body != null && !ex.getResponseHeaders().containsKey("Content-Type")) ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, code == 204 || code == 304 || b.length == 0 ? -1 : b.length);
        if (b.length > 0 && code != 204 && code != 304) ex.getResponseBody().write(b);
        ex.close();
    }

    // ---------------------------------------------------------------------
    static void rawHttp(int port) throws Exception {
        section("1. Raw HTTP/1.1 on the wire");
        try (Socket sock = new Socket("127.0.0.1", port)) {
            Writer w = new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.US_ASCII);
            w.write("GET /stream HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n");   // request line, headers, blank line
            w.flush();
            String resp = new String(sock.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            String[] lines = resp.split("\r\n");
            show("status line: " + lines[0]);
            check("status line = version, code, reason", "HTTP/1.1 200 OK", lines[0]);
            check("unknown length -> Transfer-Encoding: chunked", true, resp.toLowerCase().contains("transfer-encoding: chunked"));
            String body = resp.substring(resp.indexOf("\r\n\r\n") + 4);
            show("chunked body as sent: " + body.replace("\r\n", "\\r\\n"));
            check("each chunk is '<hex length>\\r\\n<data>\\r\\n', ending with a 0-length chunk", true, body.endsWith("0\r\n\r\n"));
        }
    }

    static void methods(HttpClient c, String base) throws Exception {
        section("2. Methods, status codes, safe and idempotent");
        HttpResponse<String> r1 = c.send(req(base + "/orders").POST(HttpRequest.BodyPublishers.ofString("{\"item\":\"pen\"}")).build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> r2 = c.send(req(base + "/orders").POST(HttpRequest.BodyPublishers.ofString("{\"item\":\"pen\"}")).build(), HttpResponse.BodyHandlers.ofString());
        check("POST creates -> 201 Created + Location header", "201 /orders/1", r1.statusCode() + " " + r1.headers().firstValue("Location").orElse(""));
        check("POST is NOT idempotent: retrying created a SECOND order", 2, ORDERS.size());
        HttpResponse<String> p1 = c.send(req(base + "/orders/42").PUT(HttpRequest.BodyPublishers.ofString("{\"item\":\"ink\"}")).build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> p2 = c.send(req(base + "/orders/42").PUT(HttpRequest.BodyPublishers.ofString("{\"item\":\"ink\"}")).build(), HttpResponse.BodyHandlers.ofString());
        check("PUT twice: 201 then 200, and the SAME final state (idempotent)", "201 200 3", p1.statusCode() + " " + p2.statusCode() + " " + ORDERS.size());
        HttpResponse<String> d1 = c.send(req(base + "/orders/42").DELETE().build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> d2 = c.send(req(base + "/orders/42").DELETE().build(), HttpResponse.BodyHandlers.ofString());
        check("DELETE is idempotent (204 both times, resource gone)", "204 204", d1.statusCode() + " " + d2.statusCode());
        check("GET a missing resource -> 404", 404, c.send(req(base + "/orders/999").GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        HttpResponse<String> head = c.send(req(base + "/orders/1").method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        check("HEAD = GET's headers without the body", "200 ''", head.statusCode() + " '" + head.body() + "'");
        HttpResponse<String> opt = c.send(req(base + "/orders").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        check("OPTIONS lists allowed methods (also used by CORS preflight)", "GET, HEAD, POST, PUT, DELETE, OPTIONS", opt.headers().firstValue("Allow").orElse(""));
        check("PATCH not supported here -> 405 Method Not Allowed", 405, c.send(req(base + "/orders/1").method("PATCH", HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        check("server-side failure -> 5xx (503: retry later)", 503, c.send(req(base + "/boom").GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    static void headers(HttpClient c, String base) throws Exception {
        section("3. Headers: negotiation, compression, redirects, cookies");
        HttpResponse<String> json = c.send(req(base + "/product").header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> text = c.send(req(base + "/product").header("Accept", "text/plain").build(), HttpResponse.BodyHandlers.ofString());
        check("content negotiation: Accept: application/json", "application/json", json.headers().firstValue("Content-Type").orElse(""));
        check("content negotiation: Accept: text/plain", "keyboard, $49", text.body());
        HttpResponse<byte[]> plain = c.send(req(base + "/product?big").build(), HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> gz = c.send(req(base + "/product?big").header("Accept-Encoding", "gzip").build(), HttpResponse.BodyHandlers.ofByteArray());
        show(String.format("repetitive JSON: %,d bytes plain, %,d bytes gzip", plain.body().length, gz.body().length));
        check("gzip shrinks repetitive JSON > 20x (Content-Encoding: gzip)", true, plain.body().length > 20 * gz.body().length && gz.headers().firstValue("Content-Encoding").orElse("").equals("gzip"));
        HttpClient follow = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> red = follow.send(req(base + "/old-path").build(), HttpResponse.BodyHandlers.ofString());
        check("301 Moved Permanently -> the client follows Location", "200 " + base + "/product", red.statusCode() + " " + red.uri());
        HttpResponse<String> login = c.send(req(base + "/login").build(), HttpResponse.BodyHandlers.ofString());
        String cookie = login.headers().firstValue("Set-Cookie").orElse("");
        check("Set-Cookie with HttpOnly (no JS access), Secure (HTTPS only), SameSite (CSRF)", true, cookie.contains("HttpOnly") && cookie.contains("Secure") && cookie.contains("SameSite"));
    }

    static void caching(HttpClient c, String base) throws Exception {
        section("4. Caching: Cache-Control, ETag, conditional requests");
        HttpResponse<String> first = c.send(req(base + "/product").build(), HttpResponse.BodyHandlers.ofString());
        String etag = first.headers().firstValue("ETag").orElse("");
        check("response says how long it may be cached", "public, max-age=60", first.headers().firstValue("Cache-Control").orElse(""));
        HttpResponse<String> again = c.send(req(base + "/product").header("If-None-Match", etag).build(), HttpResponse.BodyHandlers.ofString());
        check("revalidation with If-None-Match: unchanged -> 304, empty body", "304 0", again.statusCode() + " " + again.body().length());
        product = "{\"id\":7,\"name\":\"keyboard\",\"price\":39}";                       // price changes -> new ETag
        HttpResponse<String> changed = c.send(req(base + "/product").header("If-None-Match", etag).build(), HttpResponse.BodyHandlers.ofString());
        check("after the resource changes the old ETag no longer matches -> 200 + new body", true, changed.statusCode() == 200 && changed.body().contains("39"));
    }

    static void keepAlive(HttpClient c, String base) throws Exception {
        section("5. Keep-alive / persistent connections");
        CLIENT_PORTS.clear();
        for (int i = 0; i < 10; i++) c.send(req(base + "/orders").GET().build(), HttpResponse.BodyHandlers.ofString());
        show("10 sequential requests arrived from client ports " + CLIENT_PORTS);
        check("HTTP/1.1 keep-alive: all 10 requests reused ONE TCP connection", 1, CLIENT_PORTS.size());
    }

    // ---------------------------------------------------------------------
    // 6. HTTP versions (a page-load model)
    // ---------------------------------------------------------------------
    static void versions() {
        section("6. HTTP/1.1 vs HTTP/2 vs HTTP/3 (page-load model, 60 small resources, RTT 50 ms)");
        int resources = 60, rtt = 50;
        int h11Conns = 6;                                           // browsers open ~6 connections per origin
        int h11 = rtt * 2 + (int) Math.ceil(resources / (double) h11Conns) * rtt;   // handshakes + one request per RTT per connection
        int h2 = rtt * 2 + rtt;                                     // one connection, all requests multiplexed in ~1 RTT
        int h3 = rtt * 1 + rtt;                                     // QUIC: transport + TLS 1.3 in one RTT
        show(String.format("HTTP/1.1 (6 conns, 1 request in flight each): %d ms;  HTTP/2: %d ms;  HTTP/3: %d ms", h11, h2, h3));
        check("HTTP/2 multiplexing removes the 6-requests-at-a-time bottleneck", true, h2 < h11 / 3);
        check("HTTP/3 saves a round trip of connection setup (QUIC = transport + TLS together)", rtt, h2 - h3);
        String[] reqHeaders = {":method GET", ":path /api/a", "user-agent Mozilla/5.0 (long string...)", "cookie session=abc123; prefs=dark", "accept application/json"};
        int raw = 0;
        for (String h : reqHeaders) raw += h.length();
        int secondRequest = 2 + "/api/b".length();                 // HPACK: everything else is an index into the dynamic table
        check("HPACK: a repeated request's headers shrink to a few bytes", true, secondRequest < raw / 5);
    }

    // ---------------------------------------------------------------------
    // 7. SSE
    // ---------------------------------------------------------------------
    static void sse(HttpClient c, String base) throws Exception {
        section("7. Server-Sent Events (real stream)");
        HttpResponse<java.util.stream.Stream<String>> r = c.send(req(base + "/events").header("Accept", "text/event-stream").build(), HttpResponse.BodyHandlers.ofLines());
        List<String> data = r.body().filter(l -> l.startsWith("data:")).toList();
        show("received events: " + data);
        check("one HTTP response, many events (server -> client only)", 3, data.size());
        check("content type", "text/event-stream", r.headers().firstValue("Content-Type").orElse(""));
    }

    // ---------------------------------------------------------------------
    // 8. TLS cryptography
    // ---------------------------------------------------------------------
    static byte[] hkdf(byte[] salt, byte[] ikm, String info, int len) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);                                    // extract
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] t = mac.doFinal((info + "\u0001").getBytes(StandardCharsets.UTF_8));   // expand (one block)
        return Arrays.copyOf(t, len);
    }

    static byte[] sharedSecret(PrivateKey mine, PublicKey theirs) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(mine);
        ka.doPhase(theirs, true);
        return ka.generateSecret();
    }

    static void tlsCrypto() throws Exception {
        section("8. TLS cryptography: key exchange, key derivation, authenticated encryption");
        KeyPairGenerator x = KeyPairGenerator.getInstance("X25519");
        KeyPair client = x.generateKeyPair(), server = x.generateKeyPair();     // EPHEMERAL keys, new for every connection
        byte[] sC = sharedSecret(client.getPrivate(), server.getPublic());
        byte[] sS = sharedSecret(server.getPrivate(), client.getPublic());
        check("ECDHE (X25519): both sides compute the SAME secret; it never crossed the wire", true, Arrays.equals(sC, sS));
        byte[] nonceC = new byte[32], nonceS = new byte[32];
        new SecureRandom().nextBytes(nonceC);
        new SecureRandom().nextBytes(nonceS);
        byte[] salt = new byte[64];
        System.arraycopy(nonceC, 0, salt, 0, 32);
        System.arraycopy(nonceS, 0, salt, 32, 32);
        byte[] keyC = hkdf(salt, sC, "c ap traffic", 16), keyS = hkdf(salt, sS, "c ap traffic", 16);
        check("HKDF derives identical 128-bit traffic keys on both sides", true, Arrays.equals(keyC, keyS));

        Cipher enc = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyC, "AES"), new GCMParameterSpec(128, iv));
        byte[] record = enc.doFinal("GET /orders HTTP/1.1".getBytes(StandardCharsets.UTF_8));
        Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
        dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyS, "AES"), new GCMParameterSpec(128, iv));
        check("AES-GCM: the server decrypts the client's record", "GET /orders HTTP/1.1", new String(dec.doFinal(record), StandardCharsets.UTF_8));
        record[3] ^= 1;                                              // an attacker flips one bit in transit
        String tamper;
        try {
            dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyS, "AES"), new GCMParameterSpec(128, iv));
            dec.doFinal(record);
            tamper = "accepted?!";
        } catch (AEADBadTagException e) { tamper = "rejected: " + e.getClass().getSimpleName(); }
        check("one flipped bit -> the 16-byte auth tag fails (integrity, not just secrecy)", "rejected: AEADBadTagException", tamper);

        // why both kinds of crypto: asymmetric is slow, symmetric is fast
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048);
        KeyPair rsa = rsaGen.generateKeyPair();
        Signature sig = Signature.getInstance("SHA256withRSA");
        byte[] msg = new byte[1024];
        int n = 200;
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) { sig.initSign(rsa.getPrivate()); sig.update(msg); sig.sign(); }
        double rsaUs = (System.nanoTime() - t0) / 1e3 / n;
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        t0 = System.nanoTime();
        for (int i = 0; i < 20000; i++) { iv[0] = (byte) i; iv[1] = (byte) (i >> 8); aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyC, "AES"), new GCMParameterSpec(128, iv)); aes.doFinal(msg); }
        double aesUs = (System.nanoTime() - t0) / 1e3 / 20000;
        show(String.format("per 1 KB: RSA-2048 signature %.0f us, AES-128-GCM encryption %.2f us (%.0fx)", rsaUs, aesUs, rsaUs / aesUs));
        check("asymmetric crypto is orders of magnitude slower -> use it only in the handshake", true, rsaUs > 50 * aesUs);
    }

    // ---------------------------------------------------------------------
    // 9. certificates
    // ---------------------------------------------------------------------
    record Cert(String subject, List<String> sans, PublicKey key, String issuer, Instant notBefore, Instant notAfter, boolean isCa, byte[] signature) {
        byte[] tbs() { return (subject + "|" + sans + "|" + Base64.getEncoder().encodeToString(key.getEncoded()) + "|" + issuer + "|" + notBefore + "|" + notAfter + "|" + isCa).getBytes(StandardCharsets.UTF_8); }
    }

    static Cert issue(String subject, List<String> sans, PublicKey key, String issuer, PrivateKey issuerKey, Instant nb, Instant na, boolean ca) throws Exception {
        Cert unsigned = new Cert(subject, sans, key, issuer, nb, na, ca, null);
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(issuerKey);
        s.update(unsigned.tbs());
        return new Cert(subject, sans, key, issuer, nb, na, ca, s.sign());
    }

    /** What a TLS client does with the chain the server sends. */
    static String validate(List<Cert> chain, Map<String, PublicKey> trustStore, String hostname, Instant now) throws Exception {
        Cert leaf = chain.get(0);
        if (leaf.sans().stream().noneMatch(h -> h.equals(hostname) || (h.startsWith("*.") && hostname.endsWith(h.substring(1)) && hostname.chars().filter(c -> c == '.').count() == h.chars().filter(c -> c == '.').count())))
            return "FAIL: hostname mismatch";
        for (int i = 0; i < chain.size(); i++) {
            Cert c = chain.get(i);
            if (now.isBefore(c.notBefore()) || now.isAfter(c.notAfter())) return "FAIL: certificate expired or not yet valid (" + c.subject() + ")";
            if (i > 0 && !c.isCa()) return "FAIL: issuer is not a CA";
            PublicKey issuerKey = i + 1 < chain.size() ? chain.get(i + 1).key() : trustStore.get(c.issuer());
            if (issuerKey == null) return "FAIL: unknown issuer (untrusted root or missing intermediate)";
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(issuerKey);
            s.update(c.tbs());
            if (!s.verify(c.signature())) return "FAIL: bad signature";
            if (i + 1 == chain.size() && !trustStore.containsKey(c.issuer())) return "FAIL: untrusted root";
        }
        return "OK";
    }

    static void certificates() throws Exception {
        section("9. Certificates and chain validation (real ECDSA signatures)");
        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(256);
        KeyPair root = ec.generateKeyPair(), inter = ec.generateKeyPair(), leafKey = ec.generateKeyPair(), evil = ec.generateKeyPair();
        Instant now = Instant.parse("2026-10-27T00:00:00Z");
        Cert interCert = issue("Example Issuing CA", List.of(), inter.getPublic(), "Example Root CA", root.getPrivate(), now.minus(Duration.ofDays(400)), now.plus(Duration.ofDays(2000)), true);
        Cert leaf = issue("api.example.com", List.of("api.example.com", "*.example.com"), leafKey.getPublic(), "Example Issuing CA", inter.getPrivate(), now.minus(Duration.ofDays(30)), now.plus(Duration.ofDays(60)), false);
        Map<String, PublicKey> trust = Map.of("Example Root CA", root.getPublic());                  // the OS/JVM trust store
        List<Cert> chain = List.of(leaf, interCert);
        check("valid chain: leaf <- intermediate <- trusted root", "OK", validate(chain, trust, "api.example.com", now));
        check("wildcard *.example.com matches one label: shop.example.com", "OK", validate(chain, trust, "shop.example.com", now));
        check("...but not two levels deep", "FAIL: hostname mismatch", validate(chain, trust, "a.b.example.com", now));
        check("wrong hostname", "FAIL: hostname mismatch", validate(chain, trust, "api.evil.com", now));
        check("61 days later the leaf has expired", true, validate(chain, trust, "api.example.com", now.plus(Duration.ofDays(61))).contains("expired"));
        check("server forgot to send the intermediate -> chain can't be built", true, validate(List.of(leaf), trust, "api.example.com", now).contains("unknown issuer"));
        Cert forged = issue("api.example.com", List.of("api.example.com"), evil.getPublic(), "Example Issuing CA", evil.getPrivate(), now, now.plus(Duration.ofDays(90)), false);
        check("an attacker's self-made cert claiming the same issuer -> bad signature (MITM blocked)", "FAIL: bad signature", validate(List.of(forged, interCert), trust, "api.example.com", now));
        check("a chain to a root we don't trust", true, validate(chain, Map.of(), "api.example.com", now).startsWith("FAIL"));
    }

    // ---------------------------------------------------------------------
    // 10. forward secrecy, mTLS, SNI, ALPN
    // ---------------------------------------------------------------------
    static void tlsExtras() throws Exception {
        section("10. Forward secrecy, mutual TLS, SNI, ALPN");
        KeyPairGenerator x = KeyPairGenerator.getInstance("X25519");
        byte[] s1 = sharedSecret(x.generateKeyPair().getPrivate(), x.generateKeyPair().getPublic());
        byte[] s2 = sharedSecret(x.generateKeyPair().getPrivate(), x.generateKeyPair().getPublic());
        check("every session uses fresh ephemeral keys -> different session secrets", false, Arrays.equals(s1, s2));
        show("the server's long-term key only SIGNS the handshake; stealing it later can't decrypt recorded sessions");
        show("(with old RSA key exchange the client encrypted the secret TO the long-term key: one leak decrypts everything)");

        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(256);
        KeyPair clientId = ec.generateKeyPair();
        byte[] transcript = "ClientHello|ServerHello|...".getBytes();
        Signature cs = Signature.getInstance("SHA256withECDSA");
        cs.initSign(clientId.getPrivate());
        cs.update(transcript);
        byte[] proof = cs.sign();                                   // CertificateVerify: proves possession of the private key
        Signature sv = Signature.getInstance("SHA256withECDSA");
        sv.initVerify(clientId.getPublic());
        sv.update(transcript);
        check("mTLS: the CLIENT also proves its identity by signing the handshake transcript", true, sv.verify(proof));

        Map<String, String> certsBySni = Map.of("api.example.com", "cert-api", "shop.example.com", "cert-shop");
        check("SNI: one IP hosts many sites; the ClientHello names the host so the right cert is chosen", "cert-shop", certsBySni.get("shop.example.com"));
        List<String> clientAlpn = List.of("h2", "http/1.1"), serverAlpn = List.of("http/1.1", "h2");
        String chosen = clientAlpn.stream().filter(serverAlpn::contains).findFirst().orElse("none");
        check("ALPN: the protocol (h2 vs http/1.1) is agreed inside the TLS handshake", "h2", chosen);
        show("handshake round trips before data (after TCP): TLS 1.2 = 2, TLS 1.3 = 1, TLS 1.3 resumption with 0-RTT = 0");
    }

    static HttpRequest.Builder req(String url) { return HttpRequest.newBuilder(URI.create(url)).version(HttpClient.Version.HTTP_1_1).timeout(Duration.ofSeconds(5)); }

    public static void main(String[] args) throws Exception {
        HttpServer server = startServer();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        try {
            rawHttp(server.getAddress().getPort());
            methods(client, base);
            headers(client, base);
            caching(client, base);
            keepAlive(client, base);
            versions();
            sse(client, base);
            tlsCrypto();
            certificates();
            tlsExtras();
        } finally {
            server.stop(0);
            ((ExecutorService) server.getExecutor()).shutdownNow();
        }
        summary();
    }

    // =====================================================================
    static int passed = 0, total = 0;
    static void section(String s) { System.out.println(s); }
    static void show(String s) { System.out.println("      " + s); }

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
