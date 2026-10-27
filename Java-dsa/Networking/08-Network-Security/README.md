# Networking 08 · Network security

> Encryption keeps eavesdroppers out. It does **not** tell you who's calling or what they're
> allowed to do. Senior engineers are expected to design defence in depth: authenticate every
> caller, authorise every action, segment the network, and survive floods.

Run it: `java NetSecurity.java` → **29/29 checks**:

- signed tokens (authN) vs RBAC (authZ);
- HMAC request signing with replay protection;
- a SYN flood vs real SYN cookies;
- a token-bucket rate limiter;
- amplification maths and DNS-spoofing odds;
- ARP spoofing detection;
- public-key pinning vs a rogue CA;
- CIDR allowlists, zero-trust policy decisions and tier segmentation.

---

## 1. The security properties

| Property | Question | Mechanism |
|---|---|---|
| **confidentiality** | can others read it? | encryption (TLS, IPsec) |
| **integrity** | was it changed? | MACs / AEAD tags (TLS), signatures, HMAC request signing |
| **authentication (authN)** | **who** is this? | passwords + MFA, tokens (OIDC/JWT), **mTLS** client certificates, API keys, signed requests |
| **authorization (authZ)** | **may** they do this? | RBAC/ABAC policies, scopes, OPA |
| **availability** | does it stay up under attack? | rate limiting, DDoS protection, autoscaling |

### Why encrypting traffic is not authorisation

In the run, alice presents a **valid, signed token over TLS**: she's authenticated, and the channel
is encrypted. Her role is `viewer`, so `orders:read` is **allowed** but `orders:refund` is
**denied (403)**. TLS answered "nobody tampered with this conversation"; it never answered "is this
user allowed to refund?".

Also from the run:

- Editing the token to say `admin` **breaks the HMAC signature** → rejected.
- An expired token → rejected.
- Signatures are compared in **constant time** (`MessageDigest.isEqual`) to avoid timing leaks.

## 2. Encryption in transit, mTLS, and service-to-service security

**How do you secure communication between microservices?** (a senior question) In layers:

1. **Encrypt everything in transit:** TLS everywhere, including "internal" traffic.
2. **Authenticate workloads:** **mTLS** with short-lived certificates that encode a workload
   identity (e.g. SPIFFE IDs like `spiffe://prod/checkout`), issued automatically by a service mesh
   (Istio, Linkerd) or SPIRE.
3. **Authorise each call:** policies like "checkout may call `payments:charge`; reporting may only
   read" (the run's zero-trust engine), enforced in the mesh sidecar or the service.
4. **Propagate end-user identity** separately (a signed JWT) for user-level authorisation.
5. **Segment the network** as defence in depth (§9).
6. **Rotate secrets and certificates automatically**; log and audit.

**How does mutual TLS authenticate both parties?** Both send certificate chains. Each validates the
other's chain against its trusted CA, and each signs the handshake transcript with its private key
(CertificateVerify), proving it holds the key (chapter 06 verifies this for real). The server maps
the client certificate's identity to permissions.

### Request signing (HMAC)

Used by AWS SigV4, Stripe and GitHub webhooks, and partner APIs. The client signs a **canonical
string** of method, path, **timestamp**, **nonce** and body with a shared secret:

| Attack | Defence | Run result |
|---|---|---|
| tamper with the body | signature mismatch | **401 bad signature** |
| **replay** the identical request | nonce already seen | **401 replayed** |
| replay an old captured request | timestamp outside ±5 min | **401 stale timestamp** |

## 3. Firewalls, security groups and allowlists

- **Security groups** (stateful) at the instance level and **NACLs** (stateless) at the subnet
  level (chapter 07 §6).
- **IP allowlists / denylists:** e.g. allow only a partner's `203.0.113.0/24`. `203.0.113.88` is
  allowed; `203.0.114.1` is rejected.
- Allowlists only work on the **real** source IP. Behind proxies, use the trusted
  `X-Forwarded-For` hop (chapter 07 §7), never a header the client controls.
- IP-based trust is **weak** on its own: IPs are shared (NAT, cloud egress) and can change. Combine
  it with authentication.

## 4. Network segmentation and access control

Divide the network so a compromise in one place can't reach everything:

```
Internet → [LB] → [web tier] → [app tier] → [db / cache]
```

Each arrow is an explicit allow rule. In the run, **web → db is denied**; only the app tier can
reach the database.

**Network access control (NAC):** devices must authenticate (802.1X) and be compliant before getting
network access; corporate networks put unknown devices on a quarantine VLAN.

## 5. VPNs and IPsec vs TLS

| | **VPN** (IPsec, WireGuard, OpenVPN) | **TLS** |
|---|---|---|
| layer | **network** (L3): tunnels **all** IP traffic between networks or devices | **application/transport**: protects **one** connection |
| scope | "put this laptop (or office) on the private network" | "secure this HTTPS/gRPC/DB session" |
| apps aware? | no: transparent | yes: each app or library speaks TLS |
| typical use | site-to-site cloud ↔ office, remote access | APIs, websites, service-to-service |
| trust model | often "inside the tunnel = trusted" (that's the problem zero trust fixes) | per-connection identity via certificates |

**IPsec:** ESP encrypts and authenticates IP packets; IKE negotiates keys. Tunnel mode wraps whole
packets (site-to-site); transport mode protects the payload. It adds header overhead, which means
**MTU issues** (chapter 01 §7).

## 6. Zero-trust networking

> "Never trust, always verify." Network location grants **nothing**.

Every request is decided on **identity** (user and workload), **device posture**, and **context**
(time, risk, the action requested).

| Request (from the run) | Decision |
|---|---|
| from the internal network, no verified workload identity | **deny** |
| verified identity + compliant device + allowed action, **from the Internet** | **allow** |
| verified identity, action not in its policy | **deny** |

Implementations: BeyondCorp-style identity-aware proxies, service meshes with mTLS plus
authorisation policies, short-lived credentials, continuous verification.

## 7. Attacks you must be able to explain

### DDoS attacks and mitigation

- **Volumetric:** flood the bandwidth, often by **reflection/amplification**. The attacker sends
  small queries with the **victim's spoofed IP** to open DNS, NTP or memcached servers, which send
  large answers to the victim. DNS: 60 B query → 3000 B answer = **50×** amplification, so
  2 Gbit/s of spoofing becomes **100 Gbit/s** at the victim.
- **Protocol:** SYN floods (below); fragmentation tricks.
- **Application (L7):** expensive HTTP requests (search, login), slowloris (many slow connections).

**Mitigation layers:**

- anycast plus scrubbing (Cloudflare, AWS Shield, Google Cloud Armor) to absorb volume at the edge;
- CDNs;
- rate limiting (§8) and WAF rules;
- autoscaling;
- caching expensive responses;
- timeouts on slow clients;
- block or close open reflectors;
- BCP 38 ingress filtering to stop spoofing at the source.

### SYN floods and SYN cookies

The attacker sends SYNs from spoofed IPs and never completes the handshake. Each one occupies a
**half-open slot** in the backlog until timeout. In the run, legit clients got through only **1.5 %**
of the time.

**SYN cookies:** the server stores **nothing**. It encodes the connection in the **ISN** of its
SYN-ACK as an HMAC of (4-tuple, time). A real client's final ACK carries ISN + 1, so the server
re-computes the HMAC and accepts. A spoofed ACK for another tuple fails. Linux enables them
automatically when the backlog overflows.

### DNS spoofing and poisoning

An off-path attacker races the real answer with forged ones. With only a 16-bit transaction ID, 1,000
forged answers win **1.51 %** of races; repeat enough and the cache is poisoned (**Kaminsky**,
2008). **Random source ports** add ~15 bits, making it **~28,000×** harder (0.0000540 %).
**DNSSEC** makes forged answers verifiably invalid.

### ARP spoofing (LAN MITM)

ARP has **no authentication**: hosts accept unsolicited replies. In the run, the attacker claims the
gateway's IP, and the victim's cache now sends gateway traffic to `de:ad:be:ef:00:66`. Detect the
sudden MAC change (arpwatch); prevent it with **dynamic ARP inspection** on switches, static ARP for
gateways, and **TLS**, so a MITM sees only ciphertext and can't present a valid certificate.

### Man-in-the-middle attacks

The attacker sits between client and server (ARP spoofing, rogue Wi-Fi, a compromised router, DNS
poisoning). **TLS with proper certificate validation defeats it**: the attacker can't produce a
valid certificate for your hostname. MITM succeeds when clients **skip validation**
(`TrustAllCerts`, `verify=False`, `InsecureSkipVerify`): never ship that.

### Certificate pinning and its trade-offs

**Pinning** = the client accepts only specific public keys (SPKI hashes) for a host, even if another
certificate chains to a trusted CA. In the run:

- a MITM cert from a **trusted-but-compromised CA** (or a corporate TLS-inspection root) passes
  normal validation **but fails the pin**;
- the real key matches;
- a **pre-pinned backup key** allows rotation.

**Trade-offs:**

- rotating to an unpinned key **bricks every installed client** (HPKP was removed from browsers for
  exactly this reason);
- it breaks corporate inspection proxies;
- it adds operational burden.

Use it mainly in mobile apps or SDKs that talk to your own APIs, always with backup pins and a
kill-switch. Certificate Transparency monitoring is a lighter alternative.

## 8. Rate limiting

A **token bucket** per client (key: API key, user or IP): capacity = burst size, refill = sustained
rate. With a limit of **10 req/s and a burst of 20**:

| Client | Allowed |
|---|---|
| legit (5 req/s) | **50/50** |
| flooder (2000 req/s) | **119/20000** ≈ burst + 10/s × 10 s |

Rejected requests get **429 Too Many Requests** plus `Retry-After`. Other algorithms: leaky bucket
(smooth output), fixed and sliding windows. In distributed systems, enforce limits in a shared store
(Redis) or at the gateway, and limit expensive operations more strictly. Chapter 09 covers
backpressure and load shedding.

## 9. Defending a public API against traffic floods

**How would you defend a public API against traffic floods?** (a senior question) In layers:

1. **Edge:** anycast CDN or DDoS scrubbing (absorbs volumetric attacks), SYN cookies, connection
   limits.
2. **WAF:** block known-bad patterns, bots and geo/ASN rules; challenge suspicious clients.
3. **Authentication first:** reject unauthenticated traffic early and cheaply.
4. **Rate limits** per API key, user and IP, with tighter limits on expensive endpoints; quotas.
5. **Protect the backend:** caching, request timeouts, bounded queues, **load shedding** and
   circuit breakers (chapter 09), autoscaling with limits.
6. **Observe and respond:** dashboards by client and endpoint, alerts, a runbook to block clients
   quickly.

---

## Senior-level questions

**1. How do you secure communication between microservices?** TLS everywhere; mTLS workload identity
(mesh or SPIFFE); per-call authorisation policies; end-user JWT propagation; segmentation; automatic
rotation; auditing (§2).

**2. What is the difference between a VPN and TLS?** A VPN tunnels all IP traffic at the network
layer between networks or devices, transparently to apps. TLS secures individual application
connections with per-connection server (and optionally client) identity. VPNs extend a trusted
network; TLS and zero trust authenticate every connection (§5).

**3. How does mutual TLS authenticate both parties?** Both present certificate chains validated
against trusted CAs, and both sign the handshake transcript with their private keys, proving
possession. The authenticated identities then drive authorisation (§2).

**4. How would you defend a public API against traffic floods?** Edge DDoS protection and SYN cookies,
WAF, cheap early auth, per-key/user/IP rate limits, caching, timeouts, load shedding, autoscaling,
monitoring (§9).

**5. Why is encrypting traffic not sufficient to establish authorization?** Encryption protects
confidentiality and integrity of the channel and, with server certificates, proves the **server's**
identity. It says nothing about **who the client is** or **what they may do**. You need
authentication (tokens, mTLS) and an authorisation policy evaluated per request (§1).

---

**Next:** [09 · Load balancing, resilience and distributed-systems networking](../09-Load-Balancing-and-Resilience/README.md)
