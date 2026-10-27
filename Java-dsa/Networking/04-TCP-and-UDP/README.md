# Networking 04 · TCP and UDP (study deeply)

> Almost every backend incident involving the network ends up here: connection timeouts, TIME_WAIT
> storms, retransmissions that inflate P99, pools that go stale behind a NAT. Understand TCP's
> mechanisms and you can explain the latency graph.

Run it: `java TcpUdp.java` → **31/31 checks**:

- **real localhost sockets:** stream vs datagram boundaries, a real RST, the real cost of new
  connections;
- **simulators** for what you can't see from Java: the state machine, sequence numbers, Go-Back-N
  vs Selective Repeat, Reno congestion control, flow control, Nagle + delayed ACK, keepalive vs
  NAT, head-of-line blocking.

---

## 1. What the transport layer adds

IP delivers packets **host to host**, best effort: they can be lost, duplicated, reordered or
corrupted. The transport layer adds:

1. **Ports:** deliver to the right **process**. A connection is identified by the **4-tuple**
   (src IP, src port, dst IP, dst port).
2. Optionally (TCP): **reliability, ordering, flow control and congestion control**.

## 2. TCP in one sentence

> TCP gives each side a **reliable, ordered, byte stream** to the other, over an unreliable
> network, while not overwhelming the receiver (**flow control**) or the network (**congestion
> control**).

### A stream, not messages

The program does three `write()`s of `"hello"`, `"world"` and `"!"`. The receiver reads
**`helloworld!`**: TCP keeps no message boundaries. Protocols on TCP add **framing**, usually a
length prefix (HTTP/2 frames, gRPC, Kafka, Postgres) or delimiters (`\r\n` in HTTP/1.1 headers).
The program's 4-byte length-prefix framing rebuilds `[hello, world, !]`.

> A classic bug: assuming one `read()` returns one message. Under load, reads return partial or
> concatenated data.

## 3. The three-way handshake

```
client                                   server (LISTEN)
  │ ── SYN  seq=x ─────────────────────▶ │   SYN_RECEIVED
  │ ◀──── SYN+ACK  seq=y, ack=x+1 ────── │
  │ ── ACK  ack=y+1 ───────────────────▶ │   ESTABLISHED
ESTABLISHED
```

**Why three messages?** Each side must **send its initial sequence number** and have it
**acknowledged**:

- The SYN carries the client's ISN.
- The SYN-ACK acknowledges it and carries the server's ISN.
- The final ACK acknowledges the server's.

Two messages couldn't confirm the server's ISN. Random ISNs prevent old duplicate packets and
spoofed connections from being accepted.

**Cost:** **1 RTT** before any data (plus 1–2 RTTs for TLS, chapter 06). That's why connection
reuse matters.

On the server the kernel completes handshakes into the **accept queue** (`listen(backlog)`). If the
app accepts too slowly, the queue overflows and SYNs are dropped, so clients see timeouts.

## 4. Closing: FIN, RST, TIME_WAIT, CLOSE_WAIT

**Graceful close (4 segments):**

```
client (closes first = "active closer")          server
FIN_WAIT_1  ── FIN ──────────────────────────▶ CLOSE_WAIT   (kernel ACKs; waits for the APP to close())
FIN_WAIT_2  ◀──────────────────────── ACK ──
            ◀──────────────────────── FIN ──   LAST_ACK      (app called close())
TIME_WAIT   ── ACK ──────────────────────────▶ CLOSED
(2×MSL, 60 s on Linux)
```

TCP is **full-duplex**, so each direction closes separately (a "half-close" is possible:
`shutdownOutput()`).

**TIME_WAIT** is entered by **whichever side closes first**. It lasts 2 × MSL (maximum segment
lifetime) for two reasons:

1. If the final ACK is lost, the peer resends its FIN, and we can re-ACK it.
2. Delayed old segments from this connection die off before a **new** connection reuses the same
   4-tuple.

**TIME_WAIT problems** (a senior question): a client opening **500 short connections per second**
to one server keeps **30,000 sockets in TIME_WAIT**. That exceeds Linux's default ephemeral port
range (32768–60999 = 28,232 ports) for that destination, so new connections fail with
`EADDRNOTAVAIL` / "cannot assign requested address". Fixes:

- **reuse connections** (keep-alive, pooling): the real fix;
- `tcp_tw_reuse` for outgoing connections;
- more source IPs or a wider port range;
- let the **server** keep the connection open so clients aren't the ones closing.

**CLOSE_WAIT** = the peer closed, and **your application hasn't called `close()`**. Many sockets in
CLOSE_WAIT mean a **descriptor leak** in your code (a missing `finally` or try-with-resources, an
unreturned pooled connection). The program simulates an app that never closes and stays in
CLOSE_WAIT.

**RST (reset)** means "abort now". It's sent when:

- a SYN arrives for a **closed port**. The program connects to a closed localhost port and gets
  **"Connection refused" in 0.29 ms**: the kernel answered with RST immediately;
- a packet arrives for a connection the host doesn't know (after a crash or reboot, or after a NAT
  or load balancer dropped its state);
- an app closes with unread data, or sets `SO_LINGER = 0`;
- a firewall or load balancer forcibly kills an idle or long connection.

**What happens on receiving an RST?** The connection is destroyed immediately, with no TIME_WAIT.
Pending data is discarded and the application sees `ECONNRESET` ("connection reset by peer") on its
next read or write. If it happens mid-request, the client doesn't know whether the server processed
it, which is why **idempotency** matters for retries (chapter 09).

## 5. Sequence numbers and acknowledgements

Every **byte** has a sequence number. The receiver ACKs **cumulatively**: "I have everything up to
byte N − 1; send N next".

In the program, segments arrive in the order seq 0, 6, 3, 3 (duplicate), 9:

| Arrival | Action | ACK sent |
|---|---|---|
| seq 0 "HEL" | deliver | 3 |
| seq 6 "WOR" | **out of order**: buffer it | **3** (a duplicate ACK) |
| seq 3 "LO " | deliver it, plus the buffered 6 | 9 |
| seq 3 again | **duplicate**: discard | 9 |
| seq 9 "LD" | deliver | 11 |

The app reads **`HELLO WORLD`**: ordered, no duplicates. Sequence numbers make **reordering,
duplicate detection and retransmission** possible.

## 6. Reliable delivery and retransmission

- **Retransmission timeout (RTO):** if no ACK arrives in time, resend. The RTO adapts to the
  measured RTT and its variance (min ~200 ms on Linux, 1 s in the RFC) and **doubles** on each
  repeated timeout (exponential backoff). A timeout is expensive: often 200 ms+ of dead time, which
  shows up directly in P99 latency.
- **Fast retransmit:** **3 duplicate ACKs** mean "a segment is missing but later ones arrive", so
  resend it **without waiting** for the RTO.
- **SACK (selective ACK):** the receiver tells the sender exactly which blocks it has, so only the
  holes are resent.

| 1000 packets, window 16, 5 % loss | Transmissions |
|---|---|
| Go-Back-N (resend everything after a loss) | **1470** |
| Selective Repeat (TCP with SACK) | **1050** (ideal 1000/0.95 ≈ 1052) |

## 7. Flow control: protect the receiver

The receiver advertises its free buffer space, the **receive window (rwnd)**, in every ACK. The
sender may not have more than rwnd unacknowledged bytes in flight.

In the program the app reads 8 KB per tick, then **pauses** (think GC pause or a blocked consumer).
The advertised window shrinks `64 → 40 → 16 → 8` and then hits **0** while the reader is paused. A
**zero window** stops the sender, which sends occasional *window probes*. Throughput is capped by
the **receiver's reading speed**, not the sender's 32 KB per tick.

**Window scaling:** the window field is 16 bits (max 64 KB). On a long, fat path (1 Gbit/s ×
80 ms needs **10 MB** in flight, chapter 01) 64 KB caps you at ~6.5 Mbit/s. The **window scale
option** (negotiated in the SYN) multiplies the window by up to 2¹⁴. That's why middleboxes that
strip TCP options in the SYN cripple throughput.

## 8. Congestion control: protect the network

The sender keeps a **congestion window (cwnd)**. **Effective window = min(rwnd, cwnd).** For
example, after a loss cwnd = 10 KB with a 50 ms RTT gives 200 KB/s, however big rwnd is.

The program's Reno trace (cwnd in segments per RTT):

```
1 2 4 8 16 32 64 | 65 66 67 68 69 70 | 35 36 ... 42 | 1 2 4 8 16
└─ slow start ──┘ └ cong. avoidance ┘ ↑ 3 dup ACKs   ↑ timeout
   (×2 per RTT)     (+1 per RTT)        halve: fast     → cwnd = 1
                                        recovery
```

| Phase | Rule | Why |
|---|---|---|
| **slow start** | cwnd doubles every RTT (+1 per ACK) until **ssthresh** | find the capacity quickly from a cautious start |
| **congestion avoidance** | +1 MSS per RTT | probe gently near capacity |
| **fast retransmit / fast recovery** | 3 dup ACKs: resend, ssthresh = cwnd/2, cwnd = ssthresh | mild loss: packets still flow, so don't restart |
| **timeout** | ssthresh = cwnd/2, **cwnd = 1**, slow start again | severe: the pipe may be badly congested |

This is **AIMD** (additive increase, multiplicative decrease), and it gives the "sawtooth".

- **CUBIC** is the Linux default; it grows faster on high-BDP paths.
- **BBR** (Google) estimates bottleneck bandwidth and RTT instead of treating loss as the signal,
  which is better on lossy wireless and long paths.
- **Why new connections are slow:** they start in slow start (an initial cwnd of ~10 segments ≈
  14 KB). A 1 MB response needs several RTTs to ramp up. Reused, warmed-up connections avoid this.

### Flow control vs congestion control

| | Flow control | Congestion control |
|---|---|---|
| protects | the **receiver's** buffer | the **network's** routers and links |
| signal | rwnd advertised by the receiver | inferred from loss, delay or ECN |
| variable | rwnd | cwnd |

## 9. Nagle's algorithm and delayed ACKs

- **Nagle:** don't send a small segment while a previous small segment is unacknowledged; coalesce
  instead. That's good for telnet-style keystrokes.
- **Delayed ACK:** the receiver waits up to ~40 ms (Linux; 200 ms elsewhere) to piggyback the ACK on
  a response.

**Together they deadlock briefly:** a client does write(header), write(body), read(response). Nagle
holds the body until the header is ACKed; the server delays the ACK hoping to respond, but it can't
respond without the body. The program models it: a request on a **1 ms RTT link takes 41.5 ms**
instead of 1.0 ms.

Fixes:

- `TCP_NODELAY` (`socket.setTcpNoDelay(true)`; most RPC, database and HTTP clients enable it);
- even better, **build the whole request and write it once**.

## 10. Keepalive and half-open connections

- **Half-open connection:** one side crashed, rebooted or lost its network, and the other side still
  thinks the connection is ESTABLISHED. It finds out only when it sends (and gets an RST or a
  timeout).
- **NAT gateways, firewalls and load balancers drop idle connections** silently, e.g. after 350 s
  on a cloud NAT. A pooled DB or HTTP connection idle for 10 minutes is dead, but the pool doesn't
  know. The next query hangs until a timeout or gets an RST.
- **TCP keepalive** sends periodic probes on idle connections. **The Linux default starts after 2
  hours**, far too late for a 350 s NAT timeout. Configure it per socket (`SO_KEEPALIVE` + intervals)
  or use **application-level** keepalives or pings and pool idle timeouts **shorter than any
  middlebox timeout**.

## 11. Connection pooling (why "too many new connections" hurts)

The program does 300 requests on **localhost**:

| | Time |
|---|---|
| new connection each time | **30.3 ms** |
| one reused connection | **5.4 ms** (5.6×) |

That's even with an RTT of ~0. Over a real network each new connection adds:

- 1 RTT for the TCP handshake, plus 1–2 RTTs for TLS (plus certificate verification CPU);
- **slow start** from a small cwnd;
- a TIME_WAIT entry, and possibly port exhaustion (§4);
- kernel and memory overhead, and server-side accept-queue pressure.

So **pool and reuse** connections: HTTP keep-alive, HTTP/2 multiplexing, DB connection pools.
But size the pools carefully (chapter 09: pool exhaustion is a top cause of latency spikes).

## 12. Head-of-line (HOL) blocking

TCP delivers bytes **strictly in order**. If one segment is lost, everything behind it waits for the
retransmission, **even data belonging to unrelated requests** multiplexed on the same connection
(HTTP/2).

The program sends 4 streams (A–D), each split into 2 segments; segment B1 is lost and retransmitted
at t = 10:

| Stream completes at | TCP (HTTP/2) | QUIC (HTTP/3) |
|---|---|---|
| A | **10** | 4 |
| B | 10 | 10 |
| C | **10** | 6 |
| D | **10** | 7 |

QUIC orders data **per stream**, so a loss on one stream doesn't stall the others.

## 13. UDP

| | |
|---|---|
| connectionless | no handshake: the first packet carries data |
| no delivery or ordering guarantee | packets can be lost, duplicated or reordered |
| **message boundaries preserved** | one `send` = one datagram = one `receive` (`[hello, world, !]` in the program) |
| checksum | over the header and data (optional in IPv4, mandatory in IPv6) |
| header | **8 bytes** (TCP: 20+) |
| no flow or congestion control | the app must pace itself, or it can flood the network |

**When is UDP preferable?** When **late data is useless** and you'd rather skip than wait:

- voice and video calls, live streaming, games: a retransmitted frame from 200 ms ago is worthless;
  conceal the loss instead of stalling;
- DNS: one small request and reply, so a handshake would double the latency; retry on timeout;
- metrics and telemetry (StatsD): losing a sample is fine;
- **QUIC / HTTP/3**: reliability and congestion control rebuilt **in user space** on top of UDP.

Reliability on UDP is the application's job:

- sequence numbers, ACKs/NACKs and retransmission (QUIC, RTP/RTCP, game netcode);
- forward error correction;
- jitter buffers.

## TCP vs UDP

| Feature | TCP | UDP |
|---|---|---|
| Connection | connection-oriented (handshake) | connectionless |
| Delivery | reliable, ordered byte stream | best effort, no ordering |
| Retransmission | built in | up to the application |
| Flow control | yes (rwnd) | no |
| Congestion control | yes (cwnd) | no (the app must behave) |
| Message boundaries | not preserved | preserved per datagram |
| Header | 20–60 B | 8 B |
| HOL blocking | yes | no |
| Typical uses | HTTPS (HTTP/1.1, HTTP/2), SSH, databases, gRPC | DNS, real-time media, games, **QUIC** |

**QUIC** is the important exception to "reliable means TCP". It runs over UDP but implements
reliable streams, congestion control and **TLS 1.3 built in** (a 1-RTT handshake, or 0-RTT on
resumption), per-stream ordering (no cross-stream HOL blocking) and **connection migration**
(connection IDs survive a Wi-Fi → 4G switch). It lives in user space, so it evolves without kernel
upgrades.

---

## Senior-level questions

**1. Explain the TCP three-way handshake.** SYN (the client's ISN) → SYN-ACK (the server's ISN, ACKs
the client's) → ACK (ACKs the server's). Both ISNs get exchanged and confirmed, which establishes
state on both sides. It costs 1 RTT before data. Random ISNs prevent confusion with old segments and
blind spoofing. SYN floods abuse the half-open state; SYN cookies defend against them (chapter 08).

**2. Why does TCP use sequence numbers?** To order bytes, detect duplicates, detect gaps (so it can
retransmit), and let ACKs say exactly what was received (cumulative ACK, SACK). They also provide
some protection against injection, since a packet needs a valid sequence number.

**3. What's the difference between flow control and congestion control?** Flow control (rwnd) stops
the sender overrunning the **receiver's** buffer; the receiver advertises it. Congestion control
(cwnd) stops senders overrunning the **network**; the sender infers it from loss, delay or ECN. The
sender uses min(rwnd, cwnd).

**4. What causes a connection to enter TIME_WAIT?** Being the side that closes first. It waits 2×MSL
so it can re-ACK a retransmitted FIN and so old segments expire before the 4-tuple is reused. Many
short-lived client connections, so lots of TIME_WAIT, can exhaust ephemeral ports. Reuse connections.

**5. What happens when a TCP connection receives an RST?** It's aborted immediately: no TIME_WAIT,
buffered data discarded, and the app gets "connection reset" (`ECONNRESET`) or "connection refused"
if it happened at connect time. In-flight requests have unknown outcomes, so retry only idempotent
operations.

**6. Why can excessive connection creation degrade application performance?** Each new connection
costs RTTs (TCP + TLS), TLS CPU, slow start, kernel memory, accept-queue pressure on the server, and
TIME_WAIT entries that can exhaust ports. Under load, latency and failures rise. Pool and reuse
connections (5.6× faster even on localhost in the run).

**7. Why might UDP be preferable for a real-time application?** Late data is useless: TCP would stall
newer data behind a retransmission (HOL blocking) and back off its rate. UDP lets the app skip or
conceal losses, keep latency low and steady, and preserve message boundaries; the app adds only the
reliability it needs (or uses QUIC/WebRTC).

---

**Next:** [05 · DNS](../05-DNS/README.md)
