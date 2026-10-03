# HLD 01 · Scalability and Load Balancing

> Fundamentals. Vertical vs horizontal scaling, why services must be **stateless**, L4 vs L7 load balancers, the balancing **algorithms with measured results**, health checks, and affinity via consistent hashing.
> Code: [`LoadBalancing.java`](LoadBalancing.java). Run with `java LoadBalancing.java` (12 checks plus a 200,000-request queueing simulation).

---

## 1. Vertical vs horizontal scaling

| | Vertical (scale up) | Horizontal (scale out) |
|---|---|---|
| How | a bigger CPU/RAM/disk | more machines |
| Limit | the biggest machine you can buy | practically none |
| Failure | single point of failure | survives instance loss |
| Complexity | none (same code) | needs load balancing, statelessness, data partitioning |
| Cost | super-linear at the top end | linear, commodity hardware |
| Use | databases early on, quick wins | stateless tiers always; data tiers via sharding |

**Rule:** scale *stateless* tiers horizontally from day one; scale *stateful* tiers (DBs) vertically first, then with replicas (reads), then sharding (writes).

## 2. Stateless services
A service is stateless when any instance can serve any request, because no user state lives in its memory between requests. Move state to:
- **session store** (Redis) or **signed tokens** (JWT) for login sessions
- **object storage** for uploads, the **DB** for business data, **distributed caches** for caches

The demo shows an in-memory session vanishing when its server dies, while the external store survives. Statelessness enables auto-scaling, rolling deploys and instant failover. **Sticky sessions** are a crutch: they create uneven load and lose state on failure.

## 3. Load balancer layers

| | L4 (transport) | L7 (application) |
|---|---|---|
| Sees | IP:port, TCP/UDP | HTTP method, path, headers, cookies |
| Can do | fast forwarding, TLS passthrough | path routing (`/api` vs `/static`), header-based canaries, auth, rate limits, retries, compression |
| Cost | very cheap | more CPU (parses requests, terminates TLS) |
| Examples | AWS NLB, LVS, Maglev | AWS ALB, NGINX, Envoy, HAProxy (both modes) |

Also: **DNS load balancing / GeoDNS** for global traffic (coarse, cached by clients); **anycast** for edge networks; **client-side load balancing** (gRPC, service meshes) where clients pick from a service-discovery list.

## 4. Algorithms, measured

The simulation has 3 normal servers + 1 server at 40% speed, a load of 2.8 req/time unit against a capacity of 3.4, and 200,000 requests:

| Algorithm | mean latency | p99 | share of the slow box | Notes |
|---|---|---|---|---|
| round robin | 6,713 | 51,524 | 25% | ignores load: the slow box gets more than it can process, so its queue grows forever |
| random | 6,654 | 50,953 | 25% | same problem |
| smooth weighted RR | 4.35 | 22.6 | 12% | good **if** the weights are correct and static |
| least connections | **2.50** | **13.1** | 12% | adapts automatically; needs a global view of in-flight counts |
| power of two choices | 3.97 | 23.7 | 14% | pick 2 at random, use the less loaded: O(1), no global state, near least-conn |

Takeaways:
- **Load-aware beats load-blind** when servers or requests are heterogeneous (they always are: GC pauses, noisy neighbours, mixed instance types).
- **P2C** is what large distributed balancers use: each balancer instance only needs its own view, and "two random choices" exponentially reduces the maximum load compared with one.
- **Weighted RR** works for known capacity differences (canary at 5%, a bigger instance type).
- **Consistent hashing** (affinity) is for *cache locality* / per-key ordering, not load balance. When a backend leaves, only its ~25% of keys move (tested: 0 other keys moved), versus ~75% with `hash % N`.

## 5. Health checks
- **Active:** the LB probes `/health` every few seconds; N consecutive failures → out, M successes → back in (hysteresis prevents flapping; tested with 3/2).
- **Passive:** watch real traffic (5xx, timeouts) and eject outliers (Envoy outlier detection).
- **Readiness vs liveness:** *ready* = may receive traffic (warm caches, DB connections); *live* = the process isn't stuck (restart it). Kubernetes uses both.
- **Deep vs shallow checks:** checking dependencies (the DB) in the health check can make *every* instance fail at once when the DB blips, a cascading outage. Usually check the instance itself, and track dependencies separately.

## 6. High availability of the LB itself
Run LBs in pairs (active-passive with a floating IP / VRRP) or as a managed, multi-AZ service; use DNS with multiple LB addresses; anycast for edge LBs.

## 7. Auto-scaling
Scale on a leading metric (CPU, in-flight requests, queue depth), with cool-downs. Pre-scale for known peaks (sales, matches). Keep instances stateless and starting fast; warm caches/JIT before declaring readiness.

## 8. Interview Q&A

**Q1. Round robin vs least connections?**
Round robin is fine when servers and requests are uniform. Least connections (or P2C) adapts to slow instances and long requests; the simulation shows a 4,000× p99 difference with one slow box.

**Q2. What is "power of two choices" and why does it work?**
Sample two backends at random and pick the less loaded. The maximum load drops from O(log n / log log n) to O(log log n): a huge improvement for almost no cost, and no global coordination.

**Q3. L4 or L7?**
L7 when you need content-based routing, TLS termination, auth, retries or observability per route. L4 for raw throughput, non-HTTP protocols, or as the first tier in front of L7 proxies.

**Q4. How do you make a service stateless?**
Externalise sessions (Redis/JWT), files (S3), caches (distributed cache) and data (DB). Then any instance can handle any request, and instances can be killed and added freely.

**Q5. Sticky sessions: when are they OK?**
For in-memory caches that benefit from locality, or legacy stateful apps. The downsides are uneven load and lost state on failure. Prefer consistent hashing for locality plus external state for correctness.

**Q6. How does a load balancer detect a dead server?**
Active health checks with thresholds, plus passive outlier detection on real traffic. Connection draining lets in-flight requests finish before removal.

**Q7. How do you load balance across regions?**
GeoDNS / latency-based DNS or anycast to the nearest healthy region; within a region, L4 → L7 → service instances. Failover changes DNS weights (mind TTLs).

**Q8. How do you avoid the LB being a single point of failure?**
Redundant LB instances (active-active behind DNS or anycast, or active-passive with a floating IP), managed multi-AZ LBs, and client-side balancing for internal traffic.

**Q9. Why are retries at the LB dangerous?**
Retrying non-idempotent requests can duplicate side effects, and retries multiply load during an outage (a retry storm). Retry only idempotent requests, with budgets, backoff and jitter.

**Q10. How do you scale WebSocket connections?**
L4 balancing (long-lived TCP), connection-count-aware placement, and a registry mapping user → gateway (see Chat, case 08). Draining is slow, so deploy with gradual reconnects.

## 9. Common mistakes
- Round robin across heterogeneous instances.
- In-memory sessions with no plan for failover.
- Deep health checks that fail the whole fleet when one dependency blips.
- Unlimited LB retries.
- Forgetting the LB tier needs redundancy too.
