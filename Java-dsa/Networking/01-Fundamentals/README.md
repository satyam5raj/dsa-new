# Networking 01 · Fundamentals

> Before TCP, DNS or HTTP, you need the vocabulary and the **numbers**: what a network is, how
> data moves, and why "the network is slow" can mean five different things.

Run it: `java NetworkFundamentals.java` → **22/22 checks**.

---

## 1. What is a computer network?

A computer network is **devices connected so they can exchange data** by following shared rules
(**protocols**). Your laptop, a Wi-Fi router, a switch in a data centre, an undersea cable and a
server in another country are all parts of one big network of networks: the **Internet**.

Every network has three things:

1. **Nodes:** hosts (laptops, servers, phones) and intermediate devices (switches, routers).
2. **Links:** copper, fibre, radio (Wi-Fi, 4G/5G).
3. **Protocols:** agreements on message format and behaviour (Ethernet, IP, TCP, HTTP…).

### Network types by size

| Type | Span | Example |
|---|---|---|
| **PAN** (personal) | a few metres | Bluetooth headphones and your phone |
| **LAN** (local) | a building or campus | office Ethernet/Wi-Fi; a data-centre rack |
| **MAN** (metropolitan) | a city | a city-wide fibre ring connecting branch offices |
| **WAN** (wide) | countries / continents | the Internet; a company's private links between regions |

In cloud terms, a **VPC** is a virtual LAN you define in software. Traffic between regions crosses
the provider's WAN backbone.

### Architectures

| | Client–server | Peer-to-peer (P2P) |
|---|---|---|
| Roles | clients request, servers respond | every node is both client and server |
| Examples | web, APIs, databases | BitTorrent, blockchain, WebRTC calls (media flows peer to peer) |
| Pros | simple control, security, consistency | no central bottleneck; scales with users |
| Cons | the server is a bottleneck and single point of failure (mitigated with load balancers and replicas) | hard to secure, NAT traversal, discovery, trust |

### Topologies

| Topology | Shape | Note |
|---|---|---|
| bus | everyone on one cable | old Ethernet; a cable break kills it |
| star | all nodes to a central switch | the modern LAN; the switch is a single point of failure |
| ring | each node to two neighbours | old Token Ring; some metro fibre rings |
| mesh | many redundant links | the Internet backbone; resilient but costly |
| tree / **leaf–spine** | hierarchical | **data centres**: every leaf switch connects to every spine, so any two servers are ≤ 2 hops apart |

## 2. Bandwidth, throughput, latency, jitter, packet loss

| Term | Meaning | Analogy |
|---|---|---|
| **Bandwidth** | the link's **maximum** rate (bits/s) | the width of a motorway |
| **Throughput** | the rate you **actually achieve** | how many cars really get through per hour |
| **Latency** | the time for data to get from A to B (often measured as RTT, round-trip time) | the travel time from city A to city B |
| **Jitter** | the **variation** in latency between packets | some trips take 50 min, some 55 |
| **Packet loss** | the percentage of packets that never arrive | cars that never arrive |

Throughput < bandwidth because of:

- protocol overhead (§8);
- TCP windows (§3);
- packet loss and retransmissions;
- congestion;
- a slow sender or receiver;
- the bottleneck link on the path.

### Where latency comes from

```
total delay = transmission + propagation + queueing + processing
```

| Component | Formula | Example from the run |
|---|---|---|
| **transmission** | packet size / bandwidth | 1500 B on 100 Mbit/s = **0.12 ms** |
| **propagation** | distance / signal speed (~200,000 km/s in fibre, ⅔ of c) | 6000 km = **30 ms**, which **no upgrade can fix** |
| **queueing** | waiting in router/switch buffers | grows explosively with utilisation (below) |
| **processing** | header lookup, firewall rules, TLS work | usually µs, sometimes ms |

**Why can a high-bandwidth network still have high latency?** (a senior question)
Bandwidth affects only *transmission* delay. A 1 KB request over 6000 km takes **60.082 ms at
100 Mbit/s and 60.001 ms at 10 Gbit/s**: 100× the bandwidth, the same latency, because
propagation dominates. For a **1 GB download** bandwidth does dominate (86 s vs 0.92 s).

> **Rule:** small requests (APIs, page loads) are **latency-bound**; bulk transfers are
> **bandwidth-bound**. To speed up APIs, reduce **round trips** (connection reuse, HTTP/2,
> batching) and **distance** (CDNs, edge, regional replicas).

**Queueing delay explodes near full utilisation.** In the M/M/1 model with 1 ms service time, the
wait is `ρ/(1−ρ)`:

| Utilisation | 50 % | 80 % | 90 % | 99 % |
|---|---|---|---|---|
| Queueing delay | 1 ms | 4 ms | 9 ms | **99 ms** |

That's why links, CPUs and thread pools run at 60–70 % utilisation, not 99 %, and why **P99
latency** blows up before averages move.

### Packet loss and its effects

- **TCP** retransmits lost data, which adds at least one RTT (often a timeout of 200 ms+), and
  **cuts its sending rate** (congestion control, chapter 04).
- The **Mathis formula** bounds TCP throughput:
  `throughput ≤ (MSS/RTT) · 1.22/√p`. At 80 ms RTT:

| Loss | Max TCP throughput |
|---|---|
| 0.01 % | **17.8 Mbit/s** |
| 1 % | **1.78 Mbit/s**: 100× more loss gives 10× less throughput |
| 1 % at a 10 ms RTT | 8× faster than at 80 ms (shorter RTT recovers faster) |

- **UDP** applications (voice, video, games) usually don't retransmit. They conceal the loss
  (skip a frame, interpolate audio).

### Jitter

The one-way delays of 6 packets were `[50, 52, 49, 55, 50, 52] ms`:

- mean latency **51.3 ms**;
- RFC 3550 smoothed jitter (`J += (|Dᵢ − Dᵢ₋₁| − J)/16`) = **1.00 ms**;
- a **jitter buffer** must absorb max − min = **6 ms**.

Real-time media trades a little latency (the buffer) for smooth playback.

## 3. Bandwidth-delay product (BDP)

```
BDP = bandwidth × RTT        = the amount of data that must be "in flight" to keep the link busy
1 Gbit/s × 80 ms = 10 MB
```

A sender can only have one **window** of unacknowledged data in flight. With the classic 64 KB
TCP window (no window scaling), the maximum throughput is `window/RTT` = **6.55 Mbit/s**, even on
a 1 Gbit/s link. That's why **TCP window scaling** exists, and why long-distance transfers need
tuned buffers (chapter 04).

## 4. Unicast, multicast, broadcast (and anycast)

| Delivery | One sender to… | Example |
|---|---|---|
| **unicast** | exactly one receiver | almost all Internet traffic: HTTP, DB connections |
| **broadcast** | **every** host on the local network | ARP "who has 10.0.0.5?", DHCP discovery. IPv4 only; routers don't forward it |
| **multicast** | a **group** that subscribed | IPTV, market-data feeds, mDNS (`224.0.0.251`), IPv6 neighbour discovery |
| **anycast** | the **nearest** of many servers sharing one IP | DNS root servers, Cloudflare and Google's public DNS (`1.1.1.1`, `8.8.8.8`), CDNs |

## 5. Circuit switching vs packet switching

| | Circuit switching | Packet switching |
|---|---|---|
| How | reserve a dedicated path and capacity for the whole session | chop data into packets; each is forwarded independently and shares links |
| Example | the old telephone network | the Internet |
| Guarantee | constant bandwidth and delay | best effort: variable delay, possible loss |
| Efficiency | wasted when idle | **statistical multiplexing**: capacity is shared |

The run makes this concrete with a 1 Mbit/s link where each user needs 100 kbit/s while active and
is active 10 % of the time:

- circuit switching supports **10** users;
- packet switching supports **35** users with only a **0.04 %** chance that more than 10 are
  active at once.

That's the economic reason the Internet is packet-switched.

## 6. Connection-oriented vs connectionless

| | Connection-oriented | Connectionless |
|---|---|---|
| Setup | a handshake first; both sides keep state | none: just send |
| Examples | **TCP**, QUIC (over UDP) | **UDP**, IP itself |
| Gives you | ordering, reliability, flow and congestion control | low overhead, no setup RTT, message boundaries |

Chapter 04 goes deep on both.

## 7. MTU, fragmentation and Path MTU Discovery

**MTU (maximum transmission unit)** is the largest IP packet a link can carry in one frame. It's
**1500 bytes** on standard Ethernet; 9000 with "jumbo frames" inside data centres.

```
MTU 1500 = IP header (20) + TCP header (20) + data (MSS = 1460)
```

**IPv4 fragmentation:** if a packet is bigger than the next link's MTU and the "Don't Fragment"
bit is off, a router splits it. A 4000-byte packet (3980 B of data) at MTU 1500 becomes:

| Fragment | Offset (8-byte units) | Data | More-Fragments flag |
|---|---|---|---|
| 1 | 0 | 1480 | 1 |
| 2 | 185 | 1480 | 1 |
| 3 | 370 | 1020 | **0** |

The receiver reassembles them using the IP ID, the offsets and the MF flag.

**Why fragmentation is bad:**

- **Lose any fragment and the whole packet is lost:** 1 % loss per fragment means ~**3 %** packet
  loss.
- Reassembly costs memory and CPU.
- Many firewalls drop fragments.
- **IPv6 routers never fragment**: only the sender may.

**Path MTU Discovery (PMTUD):** send with **DF = 1**. A router that can't forward the packet drops
it and returns **ICMP "Fragmentation Needed" / "Packet Too Big"** with its MTU. The sender then
shrinks its packets.

**MTU mismatch problems** (a senior question):

- **Tunnels and VPNs** (IPsec, VXLAN, WireGuard) add headers, so the effective MTU drops to
  ~1400–1450.
- If a firewall **blocks ICMP**, PMTUD breaks. You get a **"black hole"**: small requests
  (handshake, `GET /health`) work, while big responses or uploads **hang**.
- The classic symptom: "SSH connects, but `ls` of a big directory freezes", or "TLS handshake
  stalls at the certificate", because the certificate chain is the first big packet.
- **Fixes:**
  - allow ICMP type 3 code 4 (IPv4) / ICMPv6 type 2;
  - **MSS clamping** on the tunnel router;
  - lower the interface MTU.

## 8. Encapsulation and decapsulation

Each layer wraps the data from the layer above with its own header (and sometimes a trailer):

```
application data:                                      [ JSON {"id":1} ]
+ TLS record (5 B header + 16 B tag + ...)     [TLS hdr][ encrypted data ][tag]
+ TCP header (20 B)                   [TCP][ ...........TLS record.......... ]
+ IP header (20 B)               [IP][TCP][ ............................... ]
+ Ethernet header + FCS (18 B) [ETH][IP][TCP][ ......................... ][FCS]
```

The receiver **decapsulates** bottom-up: each layer reads its header, removes it, and hands the rest
up.

**Overhead matters for small messages:**

| Payload | Bytes on the wire | Efficiency |
|---|---|---|
| 100 B | 187 B | **53.5 %** |
| 1400 B | 1487 B | **94.1 %** |

That's why chatty protocols (many tiny messages) waste bandwidth and CPU, and why batching helps.

## 9. Network interfaces and MAC addresses

A **network interface** (NIC) is the hardware or virtual device a host uses to talk to a network:

- `eth0` / `en0` (Ethernet);
- `wlan0` (Wi-Fi);
- `lo` (loopback, `127.0.0.1`);
- virtual ones: `docker0`, `veth…` (containers), `tun0` (VPN).

A server can have several, each with its own addresses.

A **MAC address** is a 48-bit Layer-2 address, e.g. `3c:22:fb:12:34:56`:

- The first 3 bytes are the **OUI** (the vendor ID).
- Bit 0 of the first byte: **0 = unicast, 1 = multicast**.
- Bit 1 of the first byte: **0 = globally unique (burned in), 1 = locally administered** (virtual
  NICs; Docker uses `02:42:…`; phones randomise their Wi-Fi MAC for privacy).
- `ff:ff:ff:ff:ff:ff` = broadcast.

MAC addresses only matter **within one Layer-2 segment**. At every router hop the Ethernet frame is
rebuilt with new source and destination MACs, while the IP addresses stay the same (apart from
NAT). **ARP** maps IP → MAC on IPv4 LANs (chapter 03).

---

## Senior-level questions

**1. What is the difference between bandwidth and throughput?**
Bandwidth is the link's capacity; throughput is the rate actually achieved end to end. Throughput is
limited by the slowest link, protocol overhead, TCP window/BDP, packet loss (Mathis: ∝ 1/√p),
congestion, and endpoint CPU or disk.

**2. Why can a high-bandwidth network still have high latency?**
Latency is mostly propagation (physics: distance ÷ ~200,000 km/s) plus queueing and processing.
Bandwidth only reduces transmission time, which is tiny for small messages. Our 1 KB request took
60 ms on both a 100 Mbit/s and a 10 Gbit/s link.

**3. What happens when packet loss occurs?**
With TCP: the receiver sees a gap and sends duplicate ACKs; the sender retransmits (fast retransmit
after 3 dup-ACKs, or after a timeout); congestion control **shrinks the window**, so throughput
drops; and the data behind the gap waits (**head-of-line blocking**), so latency spikes. With UDP:
the data is simply missing unless the application handles it. 1 % loss can reduce TCP throughput
roughly 10× compared with 0.01 %.

**4. What is MTU, and how can an MTU mismatch cause problems?**
The largest packet a link carries (1500 on Ethernet). When a path has a smaller MTU (tunnels, VPNs)
and ICMP "packet too big" messages are blocked, PMTUD fails: small packets pass and large ones are
silently dropped. Connections hang after the handshake, uploads stall, TLS stalls at the
certificate. Fix with MSS clamping, allowing ICMP, or a lower MTU.

---

**Next:** [02 · OSI and TCP/IP models](../02-OSI-and-TCP-IP/README.md)
