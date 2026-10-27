# Networking 06 · HTTP, HTTPS and TLS

> For backend engineers this is home ground. HTTP is the language of your APIs; TLS is what makes
> it safe. Senior interviews expect you to go from "what's a 304?" to "walk me through the TLS 1.3
> handshake and why forward secrecy matters".

Run it: `java HttpTls.java` → **43/43 checks**:

- a **real HTTP server and client** on localhost: methods, status codes, idempotency,
  negotiation, gzip, redirects, cookies, ETag/304, keep-alive, raw chunked HTTP/1.1, Server-Sent
  Events;
- **real cryptography**: X25519 key exchange, HKDF, AES-GCM with tamper detection, RSA vs AES
  speed, a CA → intermediate → leaf certificate chain with real ECDSA signatures, forward secrecy,
  mTLS, SNI, ALPN.

---

## Part A · HTTP

### 1. Request and response structure

HTTP/1.1 is **text** over TCP. This is exactly what the program sends and receives:

```
GET /stream HTTP/1.1             ← request line: METHOD PATH VERSION
Host: 127.0.0.1                  ← headers (Host is mandatory in HTTP/1.1: many sites share one IP)
Connection: close
                                 ← blank line = end of headers; then the optional body

HTTP/1.1 200 OK                  ← status line: VERSION CODE REASON
Transfer-Encoding: chunked       ← length unknown up front, so the body is sent in chunks
Date: ...

12\r\npart1;part2;part3;\r\n0\r\n\r\n   ← chunk: <hex length> CRLF <data> CRLF ... a 0-length chunk ends it
```

The body length is given by **`Content-Length`** or by **chunked** encoding. Getting this wrong is
the root of **request smuggling** attacks between proxies and backends.

### 2. Methods: safe and idempotent

| Method | Purpose | Safe (no side effects)? | Idempotent (N calls = 1 call)? |
|---|---|---|---|
| GET | read | ✅ | ✅ |
| HEAD | GET's headers only, no body | ✅ | ✅ |
| OPTIONS | which methods are allowed? (CORS preflight) | ✅ | ✅ |
| PUT | create or **replace** at a known URL | ❌ | ✅ |
| DELETE | remove | ❌ | ✅ |
| POST | create / process (the server picks the URL) | ❌ | ❌ |
| PATCH | partial update | ❌ | not guaranteed |

Proven in the run:

- **POST twice** created **two** orders: retrying a POST duplicates it.
- **PUT twice** returned 201 then 200 with the **same** final state.
- **DELETE twice** returned 204 both times.

> Idempotency decides what is **safe to retry** after a timeout (chapter 09). Make POSTs
> retry-safe with an **`Idempotency-Key`** header that the server deduplicates (how Stripe's API
> works).

### 3. Status codes

| Class | Meaning | Ones you must know |
|---|---|---|
| **1xx** | informational | 100 Continue, 101 Switching Protocols (the WebSocket upgrade) |
| **2xx** | success | 200 OK, **201 Created** (+ `Location`), **204 No Content**, 206 Partial Content |
| **3xx** | redirect / cache | **301** permanent, **302/307** temporary (307/308 keep the method), **304 Not Modified** |
| **4xx** | **client** error | 400 bad request, **401** unauthenticated, **403** forbidden, **404**, **405** wrong method, 409 conflict, 413 too large, **429 too many requests** |
| **5xx** | **server** error | **500** bug, **502** bad gateway (the proxy got a bad upstream response), **503** unavailable (overload or maintenance, `Retry-After`), **504** gateway timeout (the upstream was too slow) |

401 vs 403: **401** = "I don't know who you are" (authenticate). **403** = "I know who you are, and
you may not". 502 vs 504 points you at **the proxy ↔ upstream link**: a crashed or closed upstream
vs a slow one.

### 4. Headers

| Header | Purpose | In the run |
|---|---|---|
| `Content-Type` / `Content-Length` | body format and size | `application/json` |
| `Accept` | content negotiation: which formats the client wants | JSON vs `text/plain` returned different bodies |
| `Accept-Encoding` / `Content-Encoding` | compression | **gzip: 7,400 → 96 bytes** on repetitive JSON |
| `Location` | where a created resource lives, or where to redirect | 201 → `/orders/1`; 301 followed to `/product` |
| `Set-Cookie` / `Cookie` | state on top of a stateless protocol | `session=abc123; HttpOnly; Secure; SameSite=Lax` |
| `Cache-Control`, `ETag`, `Vary` | caching | below |
| `Authorization` | credentials (`Bearer <token>`) | |
| `Host` | which site (virtual hosting) | |
| `X-Forwarded-For` / `Forwarded` | the original client IP behind proxies | |

Cookie flags:

- **HttpOnly:** JavaScript can't read it (limits XSS damage).
- **Secure:** sent only over HTTPS.
- **SameSite:** not sent on cross-site requests (CSRF defence).

### 5. Statelessness

Each request carries everything needed to process it. The server keeps no conversation memory
between requests, so **any instance can serve any request** and horizontal scaling and load
balancing are easy. State lives in cookies or tokens (client side) or in a shared store (Redis, a
database), not in a server's RAM. Sticky sessions are a workaround, not a design.

### 6. HTTP caching

| Directive | Meaning |
|---|---|
| `Cache-Control: max-age=60` | fresh for 60 s; no request needed |
| `public` / `private` | shared caches (CDN) may store it / only the browser may |
| `no-cache` | store it, but **revalidate** before every use |
| `no-store` | never store it (sensitive data) |
| `ETag: "abc"` + `If-None-Match` | revalidation: "has it changed since version abc?" |
| `Last-Modified` + `If-Modified-Since` | the same, by date |
| `Vary: Accept, Accept-Encoding` | cache separately per format and encoding (otherwise a gzip body gets served to a client that can't decode it) |

The run:

- the first GET returns an `ETag`;
- revalidating with `If-None-Match` gives **304 with an empty body** (only headers cross the
  network);
- after the price changed, the same ETag gave **200 with the new body**.

**Reverse proxies and CDNs** (nginx, Varnish, CloudFront) use exactly these rules to serve cached
responses near users.

### 7. Persistent connections (keep-alive)

HTTP/1.0 opened a new TCP connection per request. **HTTP/1.1 keeps connections open by default**.
In the run, 10 sequential requests all arrived from **one client port**: one TCP connection, with no
handshake per request (chapter 04 measured that as 5.6× faster).

### 8. HTTP versions

| | HTTP/1.0 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---|---|---|---|---|
| transport | TCP | TCP | TCP (+TLS in practice) | **QUIC over UDP** (TLS 1.3 built in) |
| connections | new per request | persistent | **one connection, many streams** | one connection, many streams |
| concurrency | — | **one request at a time** per connection (browsers open ~6) | **multiplexed** binary frames | multiplexed |
| headers | text | text | **HPACK** compressed | **QPACK** |
| HOL blocking | — | at the HTTP level: a slow response blocks the queue | HTTP-level fixed; **TCP-level remains** (one lost packet stalls all streams) | **fixed**: per-stream loss recovery |
| setup | TCP + TLS | TCP + TLS | TCP + TLS (2–3 RTT) | **1 RTT** (0 on resumption) |

**The page-load model** (60 small resources, RTT 50 ms):

| | Time |
|---|---|
| HTTP/1.1 (6 connections, one request in flight each) | 600 ms |
| HTTP/2 (all multiplexed) | 150 ms |
| HTTP/3 (saves a setup RTT) | **100 ms** |

**HPACK:** repeated headers (cookies, user agent) become indexes into a shared table, so a repeat
request's headers shrink to a few bytes.

**What limitations remain with HTTP/2?**

- **TCP head-of-line blocking:** one lost packet stalls every stream (chapter 04, §12).
- One connection is one congestion window, and connection-level flow control can bottleneck.
- Middleboxes and proxies may downgrade to HTTP/1.1.
- Server push failed in practice and was removed from browsers.

### 9. Server push styles: polling, SSE, WebSockets

| | Long polling | **Server-Sent Events** | **WebSockets** |
|---|---|---|---|
| direction | server → client (simulated) | **server → client** | **both ways** |
| how | the request hangs until there's data; repeat | one HTTP response streaming `text/event-stream` | HTTP `Upgrade` (101), then a framed TCP channel |
| reconnection | manual | **built in** (`Last-Event-ID`) | manual |
| through proxies and HTTP/2 | easy | easy (plain HTTP) | needs proxy support; a separate protocol |
| data | any | text events | text or binary |
| use for | legacy fallback | **notifications, live feeds, LLM token streaming, dashboards** | **chat, collaborative editing, games, trading UIs** |

The run receives a real SSE stream: three `data: {"tick":n}` events over **one** HTTP response.

**When would you use WebSockets instead of SSE?** When the **client also sends frequent messages**
on the same channel (chat, multiplayer, collaborative cursors) or you need binary frames and low
per-message overhead. For server → client only, SSE is simpler: plain HTTP, automatic reconnection,
and it works with HTTP/2 multiplexing.

---

## Part B · HTTPS and TLS

### 10. What TLS provides

1. **Confidentiality:** nobody on the path can read the data.
2. **Integrity:** nobody can modify it undetected.
3. **Authentication:** you are really talking to `api.example.com` (via certificates), and
   optionally the client is who it claims to be (mTLS).

### 11. Why both asymmetric and symmetric cryptography?

| | Asymmetric (public/private key) | Symmetric (shared key) |
|---|---|---|
| examples | RSA, ECDSA, X25519 | AES-GCM, ChaCha20-Poly1305 |
| solves | **agreeing on a key with a stranger**; proving identity (signatures) | **bulk encryption** |
| speed (from the run, per 1 KB) | RSA-2048 signature **608 µs** | AES-128-GCM **2.21 µs** (**275×** faster, with hardware AES) |

So TLS uses **asymmetric crypto only in the handshake**, to authenticate and agree on keys, then
**symmetric crypto for all the data**.

### 12. The TLS 1.3 handshake (1 RTT)

```
Client                                                      Server
ClientHello: random, cipher suites, key_share (X25519 public key),
             SNI = api.example.com, ALPN = [h2, http/1.1]
            ─────────────────────────────────────────────▶
                                          ServerHello: random, key_share (its X25519 public key)
            both now compute the shared secret (ECDHE) → derive handshake keys (HKDF)
                         {EncryptedExtensions: ALPN = h2}
                         {Certificate: leaf + intermediates}
                         {CertificateVerify: signature over the transcript with the cert's private key}
                         {Finished: MAC over the transcript}
            ◀─────────────────────────────────────────────
validate the chain + hostname; verify the signature
{Finished}  ─────────────────────────────────────────────▶
{application data: GET /orders}   ⇄   {application data: 200 OK}
```

The program does each cryptographic step for real:

1. **ECDHE (X25519):** both sides combine their **ephemeral** private key with the other's public
   key and get the **same secret**, which never crossed the wire.
2. **HKDF** derives identical traffic keys from that secret and both randoms.
3. **AES-GCM** encrypts the record. The server decrypts `GET /orders HTTP/1.1`. **Flip one bit and
   decryption fails** (`AEADBadTagException`): integrity, not just secrecy.

| Version | RTTs before data (after TCP) | Notes |
|---|---|---|
| TLS 1.2 | 2 | allowed RSA key exchange (no forward secrecy), CBC, SHA-1 |
| **TLS 1.3** | **1** | ECDHE only (**forward secrecy mandatory**), AEAD only, encrypted certificate |
| TLS 1.3 resumption + 0-RTT | 0 | early data is **replayable**, so use it only for idempotent requests |

TLS 1.0 and 1.1 are deprecated; servers should allow only 1.2 and 1.3.

### 13. Certificates, CAs and chain validation

A **certificate** binds a **public key** to an **identity** (domain names in the SAN extension),
with a validity period, and is **signed by a certificate authority (CA)**.

```
Root CA (self-signed, pre-installed in the OS/JVM trust store)
   └─ signs → Intermediate CA (kept online; the root stays offline)
                 └─ signs → leaf: api.example.com, *.example.com (your server's cert)
```

**What the client checks** (the program's `validate()`, with real ECDSA signatures):

| Check | Result in the run |
|---|---|
| valid chain, correct host | **OK** |
| wildcard `*.example.com` vs `shop.example.com` | OK (matches **one** label) |
| `a.b.example.com` | **hostname mismatch**: wildcards don't span levels |
| `api.evil.com` | hostname mismatch |
| 61 days later | **expired** |
| server didn't send the intermediate | **unknown issuer**: the most common misconfiguration ("works in the browser, fails in curl/Java", because browsers cache intermediates) |
| attacker's own cert claiming the same issuer | **bad signature**: this is what stops a man-in-the-middle |
| root not in the trust store | untrusted |

Real clients also check revocation (OCSP stapling, CRLs), key usage, constraints and Certificate
Transparency logs.

**What happens when a certificate expires?** Clients **refuse the connection** (browser
interstitial; `SSLHandshakeException: PKIX path validation failed: certificate expired` in Java).
APIs and service-to-service calls fail hard and all at once, so it's a whole-service outage.

Prevention:

- **automate renewal** (ACME / Let's Encrypt, cert-manager, cloud-managed certificates);
- **monitor expiry** (alert at 30 / 14 / 7 days), including the intermediates and **client**
  certificates for mTLS;
- keep **clocks** synced (a host with a wrong clock sees valid certificates as expired or not yet
  valid).

### 14. Forward secrecy

Every session uses **fresh ephemeral** key-exchange keys (the program's two sessions got different
secrets). The server's long-term certificate key only **signs** the handshake. If that key is stolen
next year, previously recorded traffic **still can't be decrypted**.

With old **RSA key exchange** (TLS 1.2 and earlier), the client encrypted the session secret **to
the server's long-term key**, so one key leak decrypted every recorded session. TLS 1.3 removed it.

### 15. SNI, ALPN and mTLS

- **SNI (Server Name Indication):** the ClientHello says which hostname it wants, so one IP or load
  balancer can host many sites and pick the right certificate (`shop.example.com` → `cert-shop`).
  It's visible on the wire; Encrypted Client Hello (ECH) is fixing that.
- **ALPN:** client and server agree on the application protocol **inside the handshake** (the run
  chose `h2` from `[h2, http/1.1]`), with no extra round trip.
- **Mutual TLS (mTLS):** the server also requests a **client certificate**. The client sends its
  chain and a **CertificateVerify signature** over the transcript, proving it holds the private key
  (verified in the run). Both sides are authenticated. It's used for service-to-service traffic
  (service meshes like Istio and Linkerd automate it), B2B APIs and zero-trust networks (chapter 08).

### 16. The lifecycle of an HTTPS request

1. **DNS:** resolve the name (chapter 05).
2. **TCP** handshake (1 RTT), or QUIC.
3. **TLS** handshake (1 RTT in 1.3): ECDHE, certificate chain + hostname validation, ALPN, keys.
4. The **HTTP request**, encrypted in TLS records, possibly multiplexed (HTTP/2).
5. Through **load balancers and reverse proxies** (TLS often terminated there; re-encrypted to the
   backends, or mTLS in a mesh).
6. The app handles it; the response returns (maybe compressed, cacheable, 304).
7. The connection is **kept alive** for later requests; the session can resume with 0–1 RTT next
   time.

---

## Senior-level questions

**1. Explain the complete lifecycle of an HTTPS request.** See §16, adding chapter 05's DNS steps
and chapter 04's TCP details.

**2. What is the difference between HTTP/2 and HTTP/3?** Both multiplex streams on one connection
with header compression. HTTP/2 runs on TCP, so a lost packet stalls all streams (transport HOL).
HTTP/3 runs on QUIC (UDP) with per-stream loss recovery, TLS 1.3 integrated (a 1-RTT or 0-RTT
setup), and connection migration across networks.

**3. Why does HTTPS need both asymmetric and symmetric cryptography?** Asymmetric crypto solves key
agreement and authentication with strangers but is slow (275× slower in the run). Symmetric crypto
is fast but needs a shared key. TLS uses ECDHE and signatures to establish the key, then AES-GCM or
ChaCha20 for the data.

**4. How does TLS certificate validation work?** Build the chain from the leaf through the
intermediates to a trusted root; verify each signature with the issuer's public key; check the
validity dates, CA flags and key usage, that the hostname matches the SAN, and revocation. Then
verify the server's CertificateVerify signature, which proves it owns the leaf's private key.

**5. What happens when a certificate expires?** Handshakes fail and clients refuse to connect: a
hard outage. Automate renewal, monitor expiry (including intermediates and client certificates),
and keep clocks in sync.

**6. How does HTTP/2 multiplexing work, and what limitations remain?** Requests and responses are
split into binary frames tagged with stream IDs and interleaved on one TCP connection, with
per-stream flow control and priorities. Limitations: TCP-level HOL blocking, a single congestion
window, connection-level flow-control tuning, and proxies that downgrade.

**7. When would you use WebSockets instead of SSE?** For frequent bidirectional messaging or binary
data (chat, games, collaborative editing). Use SSE for server → client streams (notifications,
feeds, LLM token streaming): simpler, plain HTTP, built-in reconnection.

---

**Next:** [07 · Routing and network infrastructure](../07-Routing-and-Infrastructure/README.md)
