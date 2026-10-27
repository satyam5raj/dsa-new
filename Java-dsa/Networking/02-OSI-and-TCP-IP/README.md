# Networking 02 · OSI and TCP/IP models

> Layered models let each part of the network do one job and trust the layers below it. You'll
> use them every day to **locate a problem**: "is this a DNS problem, a TCP problem or an
> application problem?"

Run it: `java Layers.java` → **21/21 checks**. The program builds a real Ethernet + IPv4 + TCP
frame byte by byte (with the real IPv4 checksum), then parses it back layer by layer.

---

## 1. Why layers?

Each layer solves one problem and offers a service to the layer above:

- **Ethernet** moves frames between two machines on the same link.
- **IP** moves packets between any two machines on the Internet.
- **TCP** turns unreliable packets into a reliable byte stream between two **processes**.
- **HTTP** gives that stream meaning (requests, responses).

Layers can be swapped independently: HTTP works the same over Wi-Fi or fibre, and over IPv4 or
IPv6.

## 2. The OSI model (7 layers)

Mnemonic, from layer 1 up: **P**lease **D**o **N**ot **T**hrow **S**ausage **P**izza **A**way.

| Layer | Name | Job | Examples | PDU | Devices |
|---|---|---|---|---|---|
| 7 | Application | the meaning of the data | HTTP, DNS, SMTP, SSH, gRPC | data / message | L7 load balancer, API gateway, WAF |
| 6 | Presentation | representation: encoding, compression, encryption concepts | UTF-8, JSON/Protobuf, TLS (partly) | data | — |
| 5 | Session | session setup and teardown | TLS sessions, RPC sessions (conceptual) | data | — |
| 4 | Transport | process-to-process delivery, ports, reliability | **TCP, UDP**, QUIC | **segment** (TCP) / **datagram** (UDP) | L4 load balancer, stateful firewall |
| 3 | Network | host-to-host across networks, routing | **IP**, ICMP, IPsec | **packet** | **router**, L3 switch |
| 2 | Data Link | node-to-node on one link, MAC addressing | **Ethernet**, Wi-Fi (802.11), ARP, VLANs | **frame** | **switch**, bridge, NIC |
| 1 | Physical | bits as signals | copper, fibre, radio | bits | cables, hubs, repeaters |

> **Real protocols don't fit neatly.** TLS sits *between* the application and TCP: it does
> encryption (L6-ish) and session handling (L5-ish), but runs in user space as part of the
> application. ARP links L3 addresses to L2 addresses. QUIC implements transport (L4) features on
> top of UDP (also L4). Use OSI as a **vocabulary**, not as law.

## 3. The TCP/IP model (what the Internet actually uses)

| TCP/IP layer | OSI layers | Protocols |
|---|---|---|
| **Application** | 7, 6, 5 | HTTP, TLS, DNS, SSH, gRPC |
| **Transport** | 4 | TCP, UDP, QUIC |
| **Internet** | 3 | IPv4, IPv6, ICMP |
| **Link** | 2, 1 | Ethernet, Wi-Fi, ARP / NDP |

## 4. Encapsulation, byte by byte

The program sends `GET /health HTTP/1.1` from `10.0.0.5:51514` to `10.0.1.20:80`.

```
application data   41 bytes   "GET /health HTTP/1.1\r\nHost: api.local\r\n\r\n"
+ TCP header       20 bytes   ports 51514 -> 80, seq, ack, flags ACK+PSH, window
+ IPv4 header      20 bytes   10.0.0.5 -> 10.0.1.20, protocol 6 (TCP), TTL 64, checksum
+ Ethernet header  14 bytes   dst MAC, src MAC, EtherType 0x0800 (IPv4)
= frame            95 bytes   (+ a 4-byte FCS/CRC the NIC appends)
```

The first 34 bytes on the wire:

```
aa bb cc 00 00 01  02 42 ac 11 00 02  08 00 | 45 00 00 51 1c 46 40 00 40 06 09 49 0a 00 00 05 0a 00 01 14
└ dst MAC ──────┘  └ src MAC ──────┘  └type┘  │  │  └len┘ └ id ┘ └DF─┘ TTL │ └csum┘ └ 10.0.0.5┘ └10.0.1.20┘
                                               │  └ DSCP/ECN              └ protocol 6 = TCP
                                               └ version 4 + header length 5 (×4 = 20 bytes)
```

Each header contains a field saying **what's inside**, which is how the receiver knows how to
decapsulate:

| Header | Field | Value here |
|---|---|---|
| Ethernet | EtherType | `0x0800` → IPv4 (`0x86DD` IPv6, `0x0806` ARP) |
| IPv4 | Protocol | `6` → TCP (`17` UDP, `1` ICMP) |
| TCP | destination port | `80` → the process listening on port 80 (the HTTP server) |

### The IPv4 header checksum

It's the one's-complement sum of the header's 16-bit words, inverted. The receiver sums the header
**including** the checksum; a correct header gives 0 after inversion (checked). Change one byte
(e.g. TTL 64 → 63) and the check fails. That's why **every router recomputes the checksum** after
decrementing TTL. (IPv6 dropped the header checksum entirely: link-layer CRCs and TCP/UDP
checksums already cover errors, and routers save work.)

### TTL

Every router decrements TTL. At 0 the packet is dropped and an ICMP *Time Exceeded* goes back to
the sender. This prevents loops from circulating packets forever, and it's how **traceroute** works
(chapter 10).

## 5. Packets, frames, segments: what's the difference?

| Name | Layer | Contains |
|---|---|---|
| **frame** | 2 (Link) | Ethernet header + IP packet + FCS. **Rebuilt at every hop**: the MACs change each time |
| **packet** | 3 (Internet) | IP header + segment. The IPs stay the same end to end (except through NAT) |
| **segment** | 4 (Transport, TCP) | TCP header + a slice of the byte stream |
| **datagram** | 4 (UDP) or generic | UDP header + one whole message |
| **message / data** | 7 | what the application wrote |

## 6. An HTTPS request through every layer

What happens when a client calls `https://api.example.com/orders`:

1. **Application (DNS):** resolve `api.example.com` to an IP (chapter 05).
2. **Transport:** TCP three-way handshake to port 443 (chapter 04).
3. **Session/presentation (TLS):** the TLS handshake (certificate check, key exchange); from then
   on every HTTP byte is encrypted (chapter 06).
4. **Application (HTTP):** `GET /orders` with headers, inside TLS records.
5. **Transport:** TCP splits the TLS records into segments (≤ MSS 1460 B), numbers them, and
   retransmits if any are lost.
6. **Internet:** each segment goes into an IP packet addressed to the server's IP. The host's
   routing table says "not local, so send to the default gateway".
7. **Link:** ARP finds the gateway's MAC; an Ethernet frame goes to the gateway.
8. **Physical:** bits on copper, fibre or radio.
9. Every router on the path removes the frame, reads the IP header, decrements TTL, looks up the
   next hop, builds a **new frame**, and forwards it.
10. The server decapsulates bottom-up: frame → packet → segment (reassembled in order) → TLS
    records (decrypted) → HTTP request → your handler.

## 7. Using the layers to troubleshoot

Work **bottom-up**. The program's decision function encodes this:

| Symptom | Likely layer | Check with |
|---|---|---|
| interface down, no IP | L1/L2 | `ip link`, `ip addr`, cable, Wi-Fi, VLAN |
| can't ping the gateway | L3 | `ip route`, gateway config, ARP table (`ip neigh`) |
| IP works, the name doesn't resolve | L7 (DNS) | `dig`, `/etc/resolv.conf` |
| `connection refused` | L4: nothing listening (RST) | `ss -ltnp` on the server |
| `connection timed out` | L3/L4: firewall or security group drop, routing | `nc -vz`, security groups, `traceroute` |
| TLS handshake error | TLS: certificate expired or mismatched, SNI, protocol versions, clock skew | `openssl s_client` |
| HTTP 4xx | L7, the client side: auth, bad request | `curl -v` |
| HTTP 5xx | L7, the server or upstream | logs, traces |

> **Refused vs timed out is the single most useful distinction.**
> - **Refused:** the host is reachable but nothing is listening; it answered with a TCP RST.
> - **Timed out:** packets are being **silently dropped** (a firewall or security group, wrong
>   routing, or the host is down).
>
> Chapter 10 demonstrates both for real.

---

## Senior-level questions

**1. Explain what happens at each layer when a client makes an HTTPS request.** See §6: DNS
(application), TCP handshake (transport), TLS handshake (session/presentation), HTTP inside TLS
records, segmentation and retransmission (TCP), IP routing via the default gateway (network), ARP
and Ethernet frames rebuilt at each hop (link), and decapsulation in reverse on the server.

**2. What is the difference between a packet, a frame and a TCP segment?** A segment is TCP's unit
(TCP header + stream bytes). A packet is IP's unit (IP header + segment), and it keeps its IP
addresses end to end. A frame is the link layer's unit (Ethernet header + packet + CRC), valid for
one hop only and rebuilt by every router with new MAC addresses.

**3. How would you identify whether a network issue is at the application, transport or network
layer?** Go bottom-up:

- **Network:** can I reach the IP (ping, traceroute, route table)?
- **Transport:** can I open a TCP connection to the port? Refused means nothing is listening;
  timeout means a filter or routing problem.
- **Application:** does TLS complete (`openssl s_client`)? What does HTTP return
  (`curl -v`: status, headers, timing)?

Also: if *some* hosts or sizes work and others don't, suspect MTU, DNS caches or load-balancer
backends.

---

**Next:** [03 · IP addressing and subnetting](../03-IP-Addressing-and-Subnetting/README.md)
