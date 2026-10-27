# Networking 10 · Troubleshooting tools

> Knowing the theory isn't enough: in an incident you need to **prove** where the problem is in
> minutes. This chapter is a field guide to each tool: what it does, the exact commands, and how to
> read the output.

Run it: `java NetTools.java` → **24/24 checks**.

- **Real demos on localhost:** port open vs refused, a read timeout against a hung server, a
  `curl -w` timing breakdown against a deliberately slow server, iperf-style throughput.
- **Output-reading drills:** traceroute, `ss`, `dig`, `tcpdump` and `openssl s_client`, using
  realistic sample output.

---

## The toolbox at a glance

| Tool | Layer | Question it answers |
|---|---|---|
| `ping` | L3 (ICMP) | is the host reachable at all? What's the RTT and loss? |
| `traceroute` / `tracert` / `mtr` | L3 | which path do packets take, and where does latency or loss start? |
| `dig` / `nslookup` | L7 (DNS) | what does this name resolve to, via which server, with what TTL? |
| `nc` / `telnet` | L4 | can I open a TCP connection to this port? |
| `curl` / `wget` | L7 | what does the HTTP endpoint return, and how long does each phase take? |
| `openssl s_client` | TLS | what certificate is served, is the chain valid, which TLS version? |
| `ss` / `netstat` | L4 (local) | which ports are listening? Which connections exist, in which state? |
| `ip addr` / `ip route` / `ip neigh` | L2–L3 (local) | my addresses, routes, ARP neighbours |
| `tcpdump` / Wireshark | all | what's **actually** on the wire? |
| `iperf3` | L4 | how much throughput can this path carry? |

---

## 1. `ping`

```bash
ping -c 5 10.0.2.4
# 64 bytes from 10.0.2.4: icmp_seq=1 ttl=63 time=0.42 ms
# 5 packets transmitted, 5 received, 0% packet loss, rtt min/avg/max/mdev = 0.39/0.45/0.52/0.04 ms
```

Read the **loss %**, the **RTT** (avg and max) and **mdev** (jitter). TTL hints at the hop count
(a start of 64 or 128 minus the routers crossed).

> **Ping failing ≠ host down.** Many firewalls and security groups block ICMP. Test the real port
> with `nc` or `curl`.

## 2. `traceroute` / `tracert` / `mtr`

It sends packets with **TTL = 1, 2, 3…**. Each router that decrements TTL to 0 replies with ICMP
**Time Exceeded**, revealing itself:

```
1  192.168.1.1     1 ms
2  100.64.0.1      8 ms
3  * * *                ← this router doesn't answer (ICMP filtered/rate-limited) ...
4  198.51.100.20  41 ms ← ... but later hops do, so hop 3 is NOT where traffic dies
5  93.184.216.34  43 ms
```

- **`* * *` in the middle with later hops answering = normal.** Only a `* * *` that continues to
  the end points at a problem.
- The **latency jump** (8 → 41 ms) marks the long-haul or undersea link.
- **`mtr`** runs traceroute continuously with per-hop loss stats. **Loss at an intermediate hop
  that does not persist to the destination** is just routers de-prioritising ICMP replies.
- `traceroute -T -p 443` (TCP SYN probes) gets through firewalls that block UDP or ICMP probes.
- Windows `tracert` uses ICMP echo; Linux uses UDP by default.

## 3. `dig` / `nslookup`

```bash
dig api.example.com            # default resolver
dig @8.8.8.8 api.example.com   # ask a specific resolver
dig +short api.example.com A
dig +trace api.example.com     # resolve from the root yourself (bypasses caches)
dig -x 93.184.216.34           # reverse (PTR)
```

How to read it:

| Field | Meaning |
|---|---|
| `status: NOERROR` | the query succeeded (even with an empty answer = no record of that type) |
| `status: NXDOMAIN` | the name doesn't exist |
| `status: SERVFAIL` | **the resolver failed**: upstream unreachable, a broken authoritative server, a DNSSEC validation failure. The sample took **5003 ms**: a timeout |
| ANSWER section | here a CNAME `api.lb.example.net.` → A `203.0.113.10` |
| TTL column | seconds **remaining** in this resolver's cache (12) |
| `Query time` / `SERVER` | how long, and which resolver answered (is it the one you expected?) |

`nslookup` is older and simpler (and on Windows). Prefer `dig`.

## 4. `nc` / `telnet`: is the TCP port reachable?

```bash
nc -vz 10.0.2.4 443            # Connection to 10.0.2.4 443 port [tcp/https] succeeded!
nc -vz -w 3 10.0.2.4 5432      # with a 3 s timeout
telnet 10.0.2.4 443            # works anywhere telnet exists
```

**Timeout vs refused** (exercise 7, demonstrated for real):

| Result | Meaning | Look at |
|---|---|---|
| **succeeded** | TCP handshake completed | move up the stack (TLS, HTTP) |
| **refused** (instant) | the host is up but **nothing listens** on that port, so the kernel sent **RST** | is the service running? `ss -ltnp` on the server; wrong port; bound to 127.0.0.1 only |
| **timed out** | packets **silently dropped** | security group, firewall or NACL DROP; routing; host down; wrong IP |
| connected but **read timed out** | the kernel accepted the connection (the backlog), but the **app never answered** (302 ms in the run) | an app that's hung, deadlocked, overloaded or GC-pausing; a full accept queue |

## 5. `curl`: HTTP plus timing

```bash
curl -v https://api.example.com/health            # request/response headers, TLS info
curl -I https://api.example.com                   # HEAD: headers only
curl -sS -o /dev/null -w 'dns %{time_namelookup}  connect %{time_connect}  tls %{time_appconnect}  ttfb %{time_starttransfer}  total %{time_total}  code %{http_code}\n' https://api.example.com/orders
curl --resolve api.example.com:443:203.0.113.10 https://api.example.com   # bypass DNS: hit a specific backend
curl -H 'Host: api.example.com' http://10.0.1.5:8080/                     # talk to one instance directly
```

The timings are **cumulative from the start**. The run's breakdown against a server that "thinks"
for 120 ms:

```
time_namelookup 2.6 | time_connect 2.8 | time_starttransfer 128.6 | time_total 163.8 (ms)
```

| Phase | Large value means |
|---|---|
| namelookup | DNS (chapter 05) |
| connect − namelookup | network RTT, SYN retransmits (≥ 1 s = loss), a full accept queue |
| appconnect − connect | TLS handshake: RTTs, OCSP, CPU |
| **starttransfer − appconnect** | **server processing time** (here 120 ms) |
| total − starttransfer | body download: size or bandwidth (here the 30 ms trickle) |

`wget` is a simpler downloader (retries, recursive fetch). Use `curl` for debugging.

## 6. `ss` / `netstat`: sockets on this host

```bash
ss -ltnp                       # listening TCP sockets + owning process
ss -tan                        # all TCP sockets with state
ss -tan state time-wait | wc -l
ss -ti dst 10.0.2.4            # per-connection internals: rtt, cwnd, retrans, send/recv buffers
netstat -anp                   # older equivalent
```

The program reads this sample:

```
State      Recv-Q Send-Q Local Address:Port   Peer Address:Port
LISTEN     129    128    127.0.0.1:5432       0.0.0.0:*        ← accept queue FULL (129 > backlog 128)
ESTAB      0      87040  10.0.1.5:44120       10.0.3.7:6379    ← 87 KB unsent: Redis isn't reading / slow network
CLOSE-WAIT 1      0      10.0.1.5:51322       10.0.2.4:443     ← ×3: our app never close()d (a leak)
TIME-WAIT  0      0      10.0.1.5:51400       10.0.2.4:443     ← we closed first (normal unless thousands)
```

| Pattern | Meaning |
|---|---|
| LISTEN on `127.0.0.1:port` | **not reachable from other hosts** (a common Docker bug) |
| LISTEN with Recv-Q > Send-Q | the **accept queue overflows**: the app accepts too slowly, so clients see connect timeouts |
| many **CLOSE-WAIT** | the application leaks connections (chapter 04 §4) |
| many **TIME-WAIT** | lots of short connections, so port exhaustion risk: reuse connections |
| large Send-Q on ESTAB | the peer or network can't keep up |
| large Recv-Q on ESTAB | **our** app isn't reading fast enough |

## 7. `ip addr`, `ip route`, `ip neigh`

```bash
ip addr        # interfaces, IPs, prefixes, state UP/DOWN, MTU
ip route       # routing table: 'default via 10.0.1.1 dev eth0' is the default gateway
ip route get 8.8.8.8      # which route/interface/source IP the kernel would use for a destination
ip neigh       # ARP/NDP cache: REACHABLE, STALE, FAILED (FAILED = L2 problem)
```

These answer "is my IP, mask and gateway right?" (chapter 03 §5) and "does the kernel pick the
interface I think?".

## 8. `tcpdump` and Wireshark: the ground truth

```bash
sudo tcpdump -ni eth0 'host 10.0.2.4 and port 443'            # live, numeric
sudo tcpdump -ni any 'tcp[tcpflags] & (tcp-syn|tcp-rst) != 0'   # only SYNs and RSTs
sudo tcpdump -ni eth0 port 53                                   # DNS queries and answers
sudo tcpdump -ni eth0 -w cap.pcap 'port 443' ; then open cap.pcap in Wireshark
```

Flags: `[S]` SYN, `[S.]` SYN-ACK, `[.]` ACK, `[P.]` data (push), `[F.]` FIN, `[R]` RST.

**Exercise 6, identify TCP retransmissions.** In the sample:

| Time | Event | Diagnosis |
|---|---|---|
| 00.000 / 01.003 / 03.007 | the **same SYN seq 1000 three times** at 0 s, +1 s, +3 s | exponential backoff: the server or a firewall dropped the SYNs. That's the classic **"connect takes 3 seconds"** |
| 03.041 → 03.250 | data segment `seq 1001:1518` **sent twice**, 210 ms apart | an RTO retransmission: **packet loss** |
| 03.290 | `Flags [R]` from the server | the connection was **reset** |

In Wireshark, *Analyze → Expert Information* and the filters `tcp.analysis.retransmission`,
`tcp.analysis.zero_window` and `tcp.flags.reset==1` find these automatically. Capture on **both
ends** when you can: comparing them shows where packets are lost.

## 9. `openssl s_client`: inspecting TLS

```bash
openssl s_client -connect api.example.com:443 -servername api.example.com -showcerts </dev/null
openssl s_client -connect host:443 -servername host </dev/null 2>/dev/null | openssl x509 -noout -subject -issuer -dates
openssl s_client -connect host:443 -tls1_2        # does it still accept TLS 1.2?
openssl s_client -connect db:5432 -starttls postgres
```

Reading the sample:

- `depth=0..2`: leaf → intermediate → root (3 certificates; is the intermediate sent?).
- `subject`/`issuer`: does the name match the host?
- `notAfter=Nov 5 2026`: **9 days left**, so alert (< 14).
- `Protocol: TLSv1.3`.
- `Verify return code: 0 (ok)`. Common failures: `21 (unable to verify the first certificate)`
  means a missing intermediate; `10 (certificate has expired)`.

**Always pass `-servername`** (SNI). Without it, a shared IP or load balancer may hand you its
default certificate and you'll debug the wrong thing.

## 10. `iperf3`: path throughput

```bash
iperf3 -s                          # on the server
iperf3 -c 10.0.2.4 -t 30 -P 4      # client: 30 s, 4 parallel streams
iperf3 -c 10.0.2.4 -u -b 100M      # UDP at 100 Mbit/s: reports loss and jitter
```

Compare against the expected link or instance bandwidth. If one stream is slow but `-P 8` is fast,
you're **window- or RTT-limited** (BDP, chapter 01 §3), not bandwidth-limited. Loopback in the run
did **36 Gbit/s**: the bottleneck in real systems is the NIC, the path or the application.

---

## The practical exercises, solved

| # | Exercise | How |
|---|---|---|
| 1 | Why is a domain not resolving? | `dig name` (status? which server?) → `dig @8.8.8.8 name` (is it only our resolver?) → `dig +trace name` (does the delegation chain work?) → check `/etc/resolv.conf` and search domains; NXDOMAIN vs SERVFAIL tells you which side is broken |
| 2 | Is a TCP port reachable? | `nc -vz host port` (succeeded / refused / timed out, §4); on the server, `ss -ltnp` to confirm it listens on the right address |
| 3 | Inspect a server's TLS certificate | `openssl s_client -connect host:443 -servername host -showcerts` → subject, SAN, issuer, dates, verify code (§9) |
| 4 | Listening ports and established connections | `ss -ltnp` and `ss -tan` (or `netstat -anp`), `lsof -i :8080` (§6) |
| 5 | Trace a route; why do some hops not respond? | `traceroute` / `mtr`; non-responding middle hops filter or rate-limit ICMP. Only loss that persists to the destination matters (§2) |
| 6 | Capture packets and find TCP retransmissions | `tcpdump -w`, then Wireshark `tcp.analysis.retransmission`: repeated sequence numbers, SYN backoff at 1 s/3 s (§8) |
| 7 | Connection timeout vs connection refused | refused = RST, nothing listening (instant); timeout = silent drop by a firewall or route (§4) |
| 8 | Inspect HTTP headers and timing with `curl` | `curl -v` / `-I` for headers; `-w` with `time_*` variables for the phase breakdown (§5) |

---

**Next:** [11 · Interview questions and study plan](../11-Interview-Questions/README.md)
