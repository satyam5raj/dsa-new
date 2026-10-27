# Networking 11 · Interview question bank, priorities and study plan

> Senior interviewers aren't looking for definitions. They want **mechanism → trade-off →
> production consequence**. Every answer below follows that shape and links to the chapter with
> the full explanation and runnable proof.

---

## 1. If time is short: the top 12

| # | Topic | Chapter |
|---|---|---|
| 1 | TCP handshake, termination, retransmissions | [04](../04-TCP-and-UDP/README.md) §3–6 |
| 2 | Flow control vs congestion control | [04](../04-TCP-and-UDP/README.md) §7–8 |
| 3 | DNS resolution and caching | [05](../05-DNS/README.md) |
| 4 | HTTP/1.1, HTTP/2, HTTP/3 | [06](../06-HTTP-HTTPS-TLS/README.md) §7–9 |
| 5 | TLS handshake and certificate validation | [06](../06-HTTP-HTTPS-TLS/README.md) §10–16 |
| 6 | IP addressing, CIDR, subnetting | [03](../03-IP-Addressing-and-Subnetting/README.md) |
| 7 | Routing, NAT, default gateways | [03](../03-IP-Addressing-and-Subnetting/README.md) §5–6, [07](../07-Routing-and-Infrastructure/README.md) |
| 8 | Reverse proxies and load balancers | [07](../07-Routing-and-Infrastructure/README.md) §7–8, [09](../09-Load-Balancing-and-Resilience/README.md) §1–3 |
| 9 | Connection pooling, keepalive, TIME_WAIT | [04](../04-TCP-and-UDP/README.md) §4, §10–11, [09](../09-Load-Balancing-and-Resilience/README.md) §4 |
| 10 | Timeouts, retries, partial failures | [09](../09-Load-Balancing-and-Resilience/README.md) |
| 11 | Network security and mutual TLS | [08](../08-Network-Security/README.md) |
| 12 | Troubleshooting with `curl`, `dig`, `ss`, `tcpdump` | [10](../10-Troubleshooting-Tools/README.md) |

---

## 2. Question bank (answer each out loud in 60–120 seconds)

### Fundamentals ([01](../01-Fundamentals/README.md))

1. **Bandwidth vs throughput?** Capacity vs achieved rate; throughput is limited by the bottleneck
   link, overhead, window/RTT and loss (Mathis: ∝ 1/√p).
2. **Why can a high-bandwidth network still have high latency?** Latency is mostly propagation
   (physics) plus queueing; bandwidth only shrinks transmission time. A 1 KB request over 6000 km
   takes ~60 ms at 100 Mbit/s or 10 Gbit/s.
3. **What happens when packet loss occurs?** TCP retransmits (fast retransmit or RTO), shrinks its
   congestion window, and stalls later data (head-of-line), so latency spikes and throughput drops.
4. **MTU and MTU mismatch?** The largest packet per link (1500). Tunnels shrink it; blocked ICMP
   breaks PMTUD, so small requests work and big ones hang. Fix with MSS clamping or by allowing
   ICMP.

### Models ([02](../02-OSI-and-TCP-IP/README.md))

5. **What happens at each layer for an HTTPS request?** DNS → TCP handshake → TLS → HTTP in TLS
   records → TCP segments → IP packets via the gateway → Ethernet frames rebuilt per hop →
   decapsulation in reverse.
6. **Packet vs frame vs segment?** A segment is TCP's unit; a packet is IP's (addresses end to end);
   a frame is the link's (rebuilt each hop, new MACs).
7. **How do you tell which layer is broken?** Bottom-up: link/IP (ping, route) → transport (refused
   vs timeout) → TLS (`s_client`) → HTTP (`curl -v`).

### Addressing ([03](../03-IP-Addressing-and-Subnetting/README.md))

8. **Incorrect default gateway?** The local subnet works; everything off-subnet fails.
9. **How does NAT let many hosts share one IPv4?** PAT rewrites the source IP and **port**, keeps a
   translation table, maps replies back, and drops unsolicited inbound traffic. Limits: ~64k ports
   per destination, idle timeouts.
10. **Designing VPC ranges?** Non-overlapping per VPC (central IPAM), split by tier × AZ, generous
    sizes (pods), provider reservations, avoid Docker, VPN and office ranges.
11. **Reachable over IPv4 but not IPv6?** A missing AAAA record, the app bound only to 0.0.0.0,
    missing IPv6 security-group/firewall rules or routes, ICMPv6 blocked.

### TCP/UDP ([04](../04-TCP-and-UDP/README.md))

12. **Three-way handshake?** SYN (ISN) → SYN-ACK (ISN + ack) → ACK. It exchanges and confirms both
    ISNs and costs 1 RTT.
13. **Why sequence numbers?** Ordering, duplicate detection, gap detection, cumulative and
    selective ACKs.
14. **Flow vs congestion control?** rwnd protects the receiver; cwnd protects the network; send
    min(rwnd, cwnd).
15. **Why TIME_WAIT?** The active closer waits 2×MSL to re-ACK a lost FIN and let old segments
    expire. Too many means ephemeral port exhaustion, so reuse connections.
16. **Receiving an RST?** Immediate abort, data discarded, `ECONNRESET`; in-flight requests have
    unknown outcomes, so retry only idempotent ones.
17. **Why does excessive connection creation hurt?** Handshake RTTs, TLS CPU, slow start, TIME_WAIT
    and port exhaustion, server accept queues. Pooling was 5.6× faster even on localhost.
18. **Why UDP for real-time?** Late data is useless; there's no head-of-line stall; the app chooses
    its own reliability (or uses QUIC/WebRTC).

### DNS ([05](../05-DNS/README.md))

19. **What happens when you type `https://example.com`?** HSTS/caches → stub → recursive resolver →
    root/TLD/authoritative → A/AAAA → TCP or QUIC → TLS (SNI, certificate, ALPN) → HTTP → render.
20. **Why don't DNS changes take effect immediately?** TTL caching at every layer, plus negative
    caching. Lower the TTL ahead of time.
21. **Resolves on one machine but not another?** Different resolvers or views, caches, hosts files,
    search domains, VPN split DNS, DoH.
22. **Intermittent DNS failures?** A dead resolver plus timeouts, ndots fan-out, resolver rate
    limits, conntrack races, missing caching. Measure with `dig` loops.

### HTTP/TLS ([06](../06-HTTP-HTTPS-TLS/README.md))

23. **The lifecycle of an HTTPS request:** see 19, plus load balancers and proxies, keep-alive and
    resumption.
24. **HTTP/2 vs HTTP/3?** Both multiplex; HTTP/3 on QUIC removes TCP head-of-line blocking, folds
    TLS 1.3 into a 1-RTT setup, and migrates connections.
25. **Why both asymmetric and symmetric crypto?** Asymmetric for key agreement and authentication
    (slow: 275× in the run); symmetric for the data (fast).
26. **Certificate validation?** Chain → signatures → dates → CA flags → hostname (SAN) →
    revocation, then CertificateVerify proves key possession.
27. **When a certificate expires?** Handshakes fail, so a hard outage. Automate renewal; monitor
    expiry, including intermediates and client certificates.
28. **HTTP/2 multiplexing and its limits?** Framed streams on one connection; TCP head-of-line
    blocking, one cwnd, flow-control tuning, proxy downgrades.
29. **WebSockets vs SSE?** Bidirectional or binary vs simple server → client streams with
    automatic reconnection.

### Infrastructure ([07](../07-Routing-and-Infrastructure/README.md))

30. **Forward vs reverse proxy?** Acts for clients vs for servers.
31. **L4 vs L7 load balancer?** Connections by 5-tuple (fast, protocol-agnostic) vs requests by
    content (routing, retries, TLS termination; costlier).
32. **Works in the VPC but not from the Internet?** Outside-in: DNS → IGW route → public IP or
    internet-facing LB → security group → NACL → target health → bind address.
33. **Health check passes but requests fail?** Shallow or different path; dependency or pool
    failure; overload. Watch for deep checks on shared dependencies.

### Security ([08](../08-Network-Security/README.md))

34. **Securing microservice communication?** TLS everywhere, mTLS workload identity, per-call
    authorisation, JWT propagation, segmentation, rotation.
35. **VPN vs TLS?** A network-layer tunnel for all traffic vs per-connection application security.
36. **How mTLS authenticates both sides?** Both present chains and sign the handshake transcript.
37. **Defending a public API against floods?** Edge DDoS protection, SYN cookies, WAF, cheap auth
    first, rate limits, caching, timeouts, load shedding, autoscaling.
38. **Why encryption isn't authorisation?** TLS secures the channel and the server's identity; it
    doesn't say who the client is or what they may do.

### Resilience ([09](../09-Load-Balancing-and-Resilience/README.md))

39. **P99 jumped from 100 ms to 5 s: investigate.** Correlate (deploys, scope); a round 5 s means a
    timeout. Walk DNS → connect → retransmits → LB → app queueing/GC → downstream → pool waits →
    retries → deadlines. Mitigate, then root-cause.
40. **Are retries safe?** Only for idempotent operations, with backoff + jitter + budget, at one
    layer; use idempotency keys for payments.
41. **Circuit breaker vs bulkhead vs rate limiter?** Stop calling a failing dependency; isolate
    resources per dependency; cap the request rate per client.
42. **Consistent hashing, and why?** Adding a node moves ~1/N of the keys (19 % vs 80 % for mod N);
    virtual nodes balance the load.
43. **CP vs AP during a partition?** Refuse writes vs accept and reconcile later (merge, not LWW).

### Tools ([10](../10-Troubleshooting-Tools/README.md))

44. **Connection refused vs timed out?** RST (nothing listening, instant) vs silent drop (firewall
    or route).
45. **How do you find retransmissions?** `tcpdump`/Wireshark (`tcp.analysis.retransmission`),
    `ss -ti`, `nstat`.

---

## 3. Four-week plan (the networking half)

Around 60–90 minutes a day:

| Week | Networking topics | Run |
|---|---|---|
| 1 | OSI/TCP-IP, IP addressing, subnetting, TCP/UDP | `01`–`04` |
| 2 | DNS, HTTP, TLS, HTTP versions | `05`–`06` |
| 3 | Routing, NAT, load balancing, network security | `07`–`08` |
| 4 | Connection management, troubleshooting, distributed systems | `09`–`10`, this question bank |

**Daily pattern:**

1. **30 min:** read a chapter section.
2. **20 min:** answer its questions **out loud**.
3. **20 min:** run the program, change a parameter, predict the result, re-run.
4. **10–20 min:** note your gaps.

**End of each week:** a mock interview from this page without notes.

**Make it real:** in a sandbox VPC, break things on purpose and diagnose them with chapter 10's
tools:

- remove a security-group rule;
- block ICMP and shrink the MTU on a tunnel;
- point `resolv.conf` at a dead resolver;
- exhaust a connection pool.

---

**Back to:** [Networking overview](../README.md) · Next course: [Operating systems](../../OS/README.md)
