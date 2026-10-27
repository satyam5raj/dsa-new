# Networking for senior software engineers

> How data actually moves between your services, and how to prove where it's going wrong. From
> "what is a packet" to "why did P99 jump to 5 seconds", with **runnable Java** for every chapter.

Every chapter has:

- a **README**: intuition, diagrams, the mechanism, production consequences, the senior interview
  questions **answered**;
- a **self-checking Java program**: run it with `java File.java` (JDK 21, no dependencies). Many
  demos are **real**:
  - localhost TCP/UDP sockets, real RSTs, the real cost of new connections;
  - a real HTTP server: caching, 304s, gzip, Server-Sent Events;
  - real TLS cryptography: X25519, HKDF, AES-GCM, ECDSA certificate chains;
  - the rest are faithful simulators: congestion control, DNS resolution, routing, load balancing.

```bash
cd Java-dsa/Networking
cd 04-TCP-and-UDP && java TcpUdp.java     # one chapter
./run-all.sh                               # everything
```

## Chapters

| # | Chapter | Highlights | Checks |
|---|---|---|---|
| 01 | [Fundamentals](01-Fundamentals/README.md) | LAN/WAN, topologies, bandwidth vs throughput vs latency vs jitter, queueing, BDP, Mathis loss formula, switching, MTU/fragmentation/PMTUD, encapsulation, MAC addresses | 22 |
| 02 | [OSI and TCP/IP](02-OSI-and-TCP-IP/README.md) | build and parse a real Ethernet/IPv4/TCP frame with checksum; PDUs; an HTTPS request through every layer; layer-by-layer troubleshooting | 21 |
| 03 | [IP addressing and subnetting](03-IP-Addressing-and-Subnetting/README.md) | CIDR calculator, special ranges, VPC planning and overlaps, default gateway, NAT/PAT, DHCP, ARP, IPv6 | 40 |
| 04 | [TCP and UDP](04-TCP-and-UDP/README.md) | real stream vs datagram, framing, real RST, connection cost; state machine, TIME_WAIT/CLOSE_WAIT, retransmission, congestion control, flow control, Nagle, keepalive, head-of-line blocking, QUIC | 31 |
| 05 | [DNS](05-DNS/README.md) | iterative resolution, caching/TTL, record types, negative caching, split horizon, wire format, intermittent-failure debugging | 22 |
| 06 | [HTTP, HTTPS and TLS](06-HTTP-HTTPS-TLS/README.md) | real server: methods, idempotency, status codes, caching/ETag, keep-alive, SSE; HTTP/1.1 vs 2 vs 3; real TLS crypto, certificate chains, forward secrecy, mTLS, SNI/ALPN | 43 |
| 07 | [Routing and infrastructure](07-Routing-and-Infrastructure/README.md) | longest-prefix match, switches/VLANs, OSPF, BGP, firewalls, proxies and XFF, L4 vs L7, health checks, anycast/CDN, VPC debugging | 35 |
| 08 | [Network security](08-Network-Security/README.md) | authN vs authZ, request signing, SYN cookies, rate limiting, amplification, DNS/ARP spoofing, pinning, zero trust, segmentation | 29 |
| 09 | [Load balancing and resilience](09-Load-Balancing-and-Resilience/README.md) | balancing algorithms, consistent hashing, discovery, timeouts/deadlines, backoff + jitter, retry storms, circuit breakers, bulkheads, backpressure, idempotency, pool exhaustion, partitions, gRPC; **the P99 scenario** | 36 |
| 10 | [Troubleshooting tools](10-Troubleshooting-Tools/README.md) | ping, traceroute/mtr, dig, nc, curl timing, ss, ip, tcpdump/Wireshark, openssl s_client, iperf3; the 8 practical exercises solved | 24 |
| 11 | [Interview questions and plan](11-Interview-Questions/README.md) | top-12 priorities, 45 senior questions with answers, the 4-week plan | — |

Total: **303 self-checks** across 10 programs.

## How to study

1. Read a chapter's sections in order; each ends with its senior questions answered.
2. Run the program. For every `PASS` line, find the code that produced it.
3. Change a parameter (loss rate, TTL, pool size, retry count), **predict** the output, re-run.
4. Answer the questions out loud, then check against the README.

Pair this with [OS](../OS/README.md) (processes, memory, I/O, event loops) and [HLD](../HLD/) for
system design.
