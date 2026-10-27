# Networking 07 · Routing and network infrastructure

> Packets don't find their way by magic. Switches, routers, firewalls, NAT gateways, proxies, load
> balancers and CDNs each make a decision about every packet or request. Knowing what each one sees
> and decides is how you debug "it works from here but not from there".

Run it: `java Routing.java` → **35/35 checks**:

- longest-prefix match with a binary trie;
- a learning switch with VLANs;
- OSPF-style Dijkstra with failure reconvergence;
- BGP path selection, loop prevention and a prefix hijack;
- stateless vs stateful firewalls;
- trusting `X-Forwarded-For`;
- L4 vs L7 load balancing;
- shallow vs deep health checks;
- anycast and CDN maths;
- the "works inside the VPC" diagnosis.

---

## 1. Switches vs routers

| | **Switch** (Layer 2) | **Router** (Layer 3) |
|---|---|---|
| forwards by | **MAC address** | **IP address** |
| scope | within one network / VLAN | **between** networks |
| learns | which MAC is on which port (by watching source MACs) | routes (static or via routing protocols) |
| unknown destination | **floods** to all ports in the VLAN | drops it (ICMP unreachable), or uses the default route |
| broadcasts | forwards them (one broadcast domain) | **blocks** them (separates broadcast domains) |

**A learning switch, from the run:**

1. A → B with B unknown: **flood** to ports [2, 3] and learn "A is on port 1".
2. B replies: the switch knows A, so it goes **only to port 1**, and it learns B.
3. Now A → B goes **only to port 2**.

An **L3 switch** is a switch that can also route between VLANs in hardware, common in data centres.

### VLANs and segmentation

A **VLAN** splits one physical switch into isolated virtual LANs (802.1Q tags). In the run, port 4
is on VLAN 20, so floods from VLAN 10 **never reach it**, and traffic between VLANs needs a
**router** (or L3 switch), where you can apply firewall rules.

**Why segment?** Security (a compromised printer can't reach the database), smaller broadcast
domains, and separate policies. In the cloud the equivalents are **subnets plus security groups**,
or separate VPCs.

## 2. Routing tables and longest-prefix match

A routing table maps **prefixes → next hops**. The rule: **the most specific (longest) matching
prefix wins**. The program builds a binary trie (how real routers do it, in hardware TCAM or
compressed tries):

| Destination | Matching routes | Chosen |
|---|---|---|
| 10.0.5.9 | 0/0, 10.0.0.0/16 | **/16 → local (VPC)** |
| 10.0.130.1 | 0/0, /16, 10.0.128.0/20 | **/20 → peering to the DB VPC** |
| 10.0.128.5 | 0/0, /16, /20, 10.0.128.5/32 | **/32 → firewall appliance** |
| 8.8.8.8 | only 0.0.0.0/0 | **default route → Internet gateway** |
| no match and no default | — | **unreachable** (ICMP network unreachable) |

- **Default route (`0.0.0.0/0`):** "anything I don't know, send there". A host's "default gateway"
  is exactly this route.
- **Static routing:** routes configured by hand. Simple and predictable, but it doesn't adapt to
  failures. Fine for small and cloud networks (VPC route tables are static).
- **Dynamic routing:** routers exchange information and recompute on failure (OSPF inside an
  organisation, BGP between organisations).

## 3. OSPF (link-state), conceptually

Every router floods **link-state advertisements** (its links and their costs), so every router has
the **same map** of the network and runs **Dijkstra** to compute shortest paths.

From the run:

- the cheapest R1 → R4 path is `R1 > R3 > R2 > R4` with **cost 18**;
- when the R2–R4 link fails, the change floods and everyone recomputes: `R1 > R3 > R4`, **cost 35**.

That's **convergence**: fast (sub-second with tuning), within one organisation (an "interior gateway
protocol"). Other IGPs: IS-IS, EIGRP.

## 4. BGP (path-vector), conceptually

**BGP** connects the Internet's ~75,000 **autonomous systems** (ISPs, clouds, large companies). Each
AS announces "I can reach prefix P, via AS path [..]".

From the run:

- **Selection (simplified):** the shortest AS path wins: `[64501, 15169]` over a 3-hop path. Real
  BGP first applies **policy** (local preference: customer routes beat peers, which beat transit).
- **Loop prevention:** a route whose AS path already contains **my own AS** is rejected.
- **Hijack:** someone announces the **more specific** `203.0.113.0/25`. Longest-prefix match sends
  traffic to the hijacker everywhere. That's how real incidents happen (accidental or malicious);
  defences are **RPKI** route-origin validation, prefix filters and max-prefix limits.

BGP is also how **anycast** works (§8), and why a BGP mistake can take a company offline (Facebook's
2021 outage withdrew the routes to its own DNS servers).

> For backend roles, know OSPF and BGP **conceptually**. Go deep on routing tables, NAT, firewalls,
> proxies and load balancers.

## 5. NAT gateways

A **NAT gateway** lets instances in private subnets make **outbound** connections (package
downloads, third-party APIs) without being reachable **inbound** (PAT, chapter 03 §6).

In the cloud, the private subnet's route table says `0.0.0.0/0 → nat-gw`; the NAT gateway sits in a
public subnet whose route goes `0.0.0.0/0 → igw`.

Watch for:

- SNAT **port exhaustion** to a single destination;
- per-GB **data-processing cost** (use VPC endpoints for S3 and other cloud APIs);
- idle-connection timeouts (chapter 04 §10).

## 6. Firewalls: stateless vs stateful

**Policy:** our server may call out to `93.184.216.34:443`; nothing may connect in.

| | **Stateless** (packet filter; AWS NACLs) | **Stateful** (connection tracking; security groups, iptables `-m conntrack`) |
|---|---|---|
| decision | every packet judged alone | packets matched against tracked connections |
| reply traffic | needs an explicit inbound rule, so you must open **1024–65535** for the ephemeral ports | **allowed automatically**: it matches the outbound flow |
| unsolicited packet to a high port | **allowed** (that wide-open rule) | **dropped** |
| cost | cheap, no memory | a conntrack table (which can fill up under SYN floods or huge connection counts) |

**Next-gen firewalls and WAFs** inspect higher layers too: URL paths, SQL-injection patterns, TLS
SNI. In AWS, **NACLs are stateless** (at the subnet level) and **security groups are stateful** (at
the instance or ENI level). Forgetting outbound ephemeral ports in a NACL is a classic "inbound
works, responses vanish" bug.

## 7. Proxies

| | **Forward proxy** | **Reverse proxy** |
|---|---|---|
| acts on behalf of | **clients** | **servers** |
| clients know about it? | yes (configured) | no: it *is* "the server" to them |
| uses | corporate egress filtering, caching, anonymity, egress allowlists | TLS termination, load balancing, caching, compression, WAF, routing, rate limiting |
| examples | Squid, corporate proxies | nginx, Envoy, HAProxy, cloud load balancers, API gateways, CDNs |

### Finding the real client IP behind proxies

Each proxy appends to **`X-Forwarded-For`**, and the server's TCP peer is the last proxy. **Walk
from the right and take the first address that is not one of your own trusted proxies.**

The **left-most** entry is whatever the client claimed. In the run, a forged `1.2.3.4` sits at the
left, and the correct answer is still `203.0.113.50`. Trusting the left-most entry lets attackers
bypass IP rate limits and allowlists.

## 8. Load balancers: L4 vs L7

| | **Layer 4** (TCP/UDP) | **Layer 7** (HTTP) |
|---|---|---|
| sees | IPs, ports, protocol | URLs, headers, cookies, methods, bodies |
| balances | **connections** (5-tuple hash or least connections) | **requests** (each request can go to a different backend) |
| can do | very fast, protocol-agnostic (databases, MQTT, gRPC passthrough), preserves end-to-end TLS | **path/host routing**, header rewrites, **cookie stickiness**, retries, canary splits, auth, rate limiting, compression, TLS termination |
| costs | blind to HTTP; long-lived connections (HTTP/2, gRPC) stick to one backend, so load is uneven | parses every request (more CPU, more latency); must terminate TLS |
| examples | AWS NLB, IPVS, Maglev | AWS ALB, nginx, Envoy, HAProxy (http mode) |

In the run:

- **L4:** 300 connections hashed by 5-tuple spread **101 / 98 / 101**, and every packet of a
  connection stays on one backend.
- **L7:** `/api/orders → api-pool`, `/static/app.js → cdn-origin-pool`; the same session cookie
  always goes to the same backend.

> **gRPC behind an L4 balancer:** one HTTP/2 connection carries all requests, so a client pins to
> one backend. Use L7 (request-level) balancing or client-side balancing (chapter 09).

**Session persistence (sticky sessions):** send a user's requests to the same backend (by cookie or
source IP). It's needed for in-memory sessions or WebSockets, but it unbalances load and breaks on
failover. Prefer stateless servers with shared session storage.

### Health checks and failover

The balancer probes each backend and removes failing ones. **Detection time = check interval ×
unhealthy threshold** (e.g. 10 s × 3 = 30 s of errors before removal). Failover means traffic moves
to the remaining healthy backends, or another zone or region (via DNS or anycast).

**Why can a health check pass while the service can't serve real requests?** (a senior question)

- the check is **shallow** (`/health` returns 200 if the process is alive) while the DB is down,
  the thread or connection pool is exhausted, a dependency is timing out, or the disk is full;
- the check hits a **different path or port** than real traffic (an admin port; a cached response);
- a **partial** failure: only some endpoints or tenants are broken;
- **TLS or certificate** problems on the real listener but not on the health port;
- an **overloaded** instance answers the cheap check but times out on real work.

**But deep checks have a trap.** If every instance's check depends on a **shared** database, one DB
blip marks **all instances unhealthy at once**, and the balancer removes everyone: a total outage
instead of partial errors (the run shows `[false, false, false]` vs shallow `[true, true, true]`).

**Best practice:**

- **liveness** (should I be restarted?): shallow;
- **readiness** (should I get traffic?): checks local readiness (warmed up, not overloaded) and
  maybe critical local dependencies;
- balancers that **fail open** when all targets look unhealthy;
- alerting on **real request success rates**, not just health checks.

## 9. Anycast and CDNs

**Anycast:** the **same IP** is announced by BGP from many locations, so routing delivers each user
to the **nearest** one. From Mumbai: `mumbai-pop` at 4 ms rather than Singapore (38 ms) or
Frankfurt (120 ms).

Used by DNS (root servers, `1.1.1.1`, `8.8.8.8`), CDNs and DDoS protection (attack traffic is spread
across many sites). It works best for short or stateless exchanges, since a route change mid-flow can
move a TCP connection to another site.

**CDN fundamentals:**

- edge **PoPs** cache content near users;
- the origin is hit only on a miss;
- TLS terminates at the edge (saving RTTs);
- connections from edge to origin are kept warm and reused.

With a **92 % hit ratio**:

- average fetch latency is **18.4 ms instead of 180 ms**;
- the origin serves only 8 % of requests (**12.5×** offload).

Cache keys, `Cache-Control`/`Vary` (chapter 06), purge or invalidation, and **cache stampede** on
expiry (use request coalescing) are the operational topics.

## 10. "Works inside the VPC, inaccessible from the Internet"

Walk the path **from the outside in**. The program stops at the first broken hop:

1. **DNS:** does the public name resolve to the **public** address or load balancer (not a private
   record from split horizon)?
2. **Route table:** does the public subnet have `0.0.0.0/0 → Internet gateway`?
3. **Public IP or internet-facing load balancer:** is it internal-only by mistake?
4. **Security group:** inbound 443 from `0.0.0.0/0` (or the load balancer's SG)? ← **broken in the
   run**: it allowed only the VPC CIDR, so all internal tests passed while never exercising the
   Internet path.
5. **NACL:** inbound 443 **and** outbound ephemeral ports (it's stateless)?
6. **Load-balancer target health**, and the target group's port.
7. **The app** listens on `0.0.0.0` (not `127.0.0.1`) on the right port, and the host firewall
   (iptables) allows it.

Tools: `curl -v` from outside, VPC flow logs (REJECT entries), reachability analysers, load-balancer
access logs.

---

## Senior-level questions

**1. What is the difference between a forward proxy and a reverse proxy?** A forward proxy acts for
clients (they configure it; it reaches the Internet on their behalf: egress control, caching). A
reverse proxy acts for servers (clients think it is the server; it terminates TLS, load-balances,
caches, protects the backends).

**2. How does a Layer 4 load balancer differ from a Layer 7 load balancer?** L4 forwards
connections based on IPs and ports, without parsing the application protocol: fast, generic, TLS can
pass through, but it can't route on content and is uneven for long-lived multiplexed connections. L7
parses HTTP and routes each request by path, host, header or cookie, and can retry, rewrite, rate
limit and terminate TLS, at higher CPU and latency cost.

**3. How would you troubleshoot a service that works inside a VPC but is inaccessible from the
Internet?** Walk outside-in (§10): DNS → Internet gateway route → public IP or internet-facing load
balancer → security group → NACL (both directions) → target health → app bind address and host
firewall. Use flow logs and `curl -v` from an external host.

**4. Why might a health check pass while a service is still unable to serve real requests?** The
check is shallow or hits a different path or port; there are dependency or pool failures,
overload, partial failures, or listener/TLS issues that the check doesn't exercise. Balance this
with the risk of deep checks on shared dependencies causing correlated mass removal (§8).

---

**Next:** [08 · Network security](../08-Network-Security/README.md)
