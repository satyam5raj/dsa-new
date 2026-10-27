# Networking 09 · Load balancing, resilience and distributed-systems networking

> In a distributed system **the network will fail**: partially, slowly, at the worst moment. Senior
> engineers design so that one slow dependency doesn't cascade into a full outage. This chapter is
> the toolkit: balancing, discovery, timeouts, retries, circuit breakers, bulkheads, backpressure,
> idempotency, and how they interact.

Run it: `java Resilience.java` → **36/36 checks**.

---

## 1. Load-balancing algorithms

Three backends, one of them **4× slower** (a noisy neighbour, GC, older hardware). About 167 req/s
offered against a total capacity of ~225 req/s:

| Algorithm | How it picks | Mean latency |
|---|---|---|
| **round robin** | next in turn | **24,690 ms**: the slow backend gets ⅓ of the traffic but can handle only ~11 %, so its queue grows without bound |
| **weighted round robin** (nginx "smooth" variant) | in proportion to configured weights (4:4:1) | **13.8 ms**, if you **know** the capacities |
| **least connections** | the backend with the fewest in-flight requests | **13.6 ms**: adapts automatically |
| **power of two choices (P2C)** | pick 2 at random, send to the less loaded | 29.6 ms |

P2C does worse than least connections here because with only 3 backends it picks the slow one twice
1/9 of the time. Its strength is scale: it needs no global view, so it's ideal for **many**
distributed load balancers (Envoy, Finagle, gRPC).

**Others:**

- **least response time / EWMA latency:** reacts to slowness, not just connection count;
- **random**;
- **IP hash / consistent hash:** affinity, for caches.

> **Rule:** prefer **load-aware** algorithms (least requests, P2C, EWMA) over blind round robin when
> backends can differ or degrade. They're the default in modern proxies.

## 2. Consistent hashing

When a key must always go to the same node (caches, sharded data, sticky routing), `hash(key) mod N`
breaks badly when N changes. From the run, adding a 5th node:

| | Keys that move |
|---|---|
| `hash mod N` | **80.0 %**: the whole cache is effectively wiped, a thundering herd on the database |
| **consistent hashing** | **19.2 %** ≈ 1/5: only the keys the new node takes over |

**How it works:**

- servers and keys are hashed onto a **ring**;
- a key belongs to the **next server clockwise**;
- each server gets **~150 virtual nodes** to even out the load (max/min < 1.4 in the run).

> A lesson from building this: the ring needs a **well-mixing hash**. My first version used Java's
> `String.hashCode` for names like `n1#3` and `n1#4`, which cluster, and got uneven load and a
> wrong remap fraction. Real implementations (ketama) use MD5 or Murmur. Variants: rendezvous
> (highest-random-weight) hashing, jump hash, Maglev (Google's load-balancer hashing).

Used in Cassandra and DynamoDB partitioning, memcached clients, Envoy's ring-hash LB, CDN cache
routing, and Kafka-style partition assignment.

## 3. Client-side vs server-side load balancing; service discovery

| | **Server-side** (a proxy: ALB, nginx, Envoy at the edge) | **Client-side** (the library picks: gRPC, Finagle, Spring Cloud LoadBalancer) |
|---|---|---|
| hops | client → LB → backend (one extra hop) | client → backend directly |
| client complexity | none | needs a discovery client and LB logic in every language |
| central control | easy | harder (config pushed to clients) |
| failure point | the LB (made highly available) | none central |

A **service mesh** (Envoy sidecars) gives client-side balancing without library code.

**Service discovery:** instances register (address, health) in a registry (Consul, Eureka, etcd,
Kubernetes Endpoints, cloud Cloud Map) and send **heartbeats**. Entries **expire after a TTL** if
heartbeats stop. In the run, the crashed instance `10.0.3.9` disappears; client-side balancing then
cycles over the two live instances. DNS is the simplest discovery (SRV records, Kubernetes service
DNS), with the caveat of TTL and caching lag.

## 4. Connection pooling and reuse

Reuse connections: HTTP keep-alive, HTTP/2, DB pools. That avoids handshakes, slow start and
TIME_WAIT (chapter 04).

**Pool exhaustion** is one of the most common causes of latency spikes. By **Little's law**
(concurrency = throughput × latency):

| | Connections needed (400 req/s) |
|---|---|
| normal DB latency 20 ms | **8** (the pool of 20 is fine) |
| DB slows to 200 ms | **80** > pool of 20 |

With the slow DB, the pool's max throughput is 20 / 0.2 s = **100 req/s** while 400 are offered.
Requests queue **waiting for a connection** and P99 explodes, even though CPU looks idle.

The fixes:

- timeouts on **acquiring** a connection;
- right-size pools (bigger isn't always better: the DB has limits too);
- fix the slow query;
- bulkheads (§8).

## 5. Timeouts

Every network call needs a timeout. Without one, a hung dependency holds a thread or connection
forever. Set them **per call**:

| Timeout | Typical setting |
|---|---|
| connect | short, 100 ms–1 s |
| request / read | based on the dependency's P99.9 plus headroom |
| total | bounded by the caller's own deadline |

### Deadline propagation

A client times out after 1,000 ms. The call chain costs gateway 200 ms → service A 300 ms → service
B 900 ms.

- **Without deadlines:** B keeps working **400 ms after the client already gave up**: wasted work,
  plus load during an incident.
- **With a propagated deadline** (gRPC deadlines, a `deadline`/`timeout` header): each hop passes
  the **remaining budget**. B sees "only 500 ms left, I need 900" and **fails fast**.

## 6. Retries, exponential backoff and jitter

Retries fix **transient** failures (a dropped packet, a restarting instance). They're **not
automatically safe**:

1. Only retry **idempotent** operations, or make them idempotent with keys (§11).
2. Use **exponential backoff:** wait 100 → 200 → 400 → 800 ms.
3. Add **jitter**. When 1,000 clients fail together and retry on the same schedule, their retries
   arrive in synchronised waves: **1,000 requests in the same 10 ms window**. With **full jitter**
   (a random wait in [0, cap]) the peak drops to **106**, about 9× lower.
4. Cap the attempts, and **retry at one layer only**.

### Retry storms and cascading failures

If 3 layers each make 1 + 3 attempts, a failing database sees up to **4³ = 64 attempts per user
request**: 64,000 req/s from 1,000 real ones, which keeps the database down. A **retry budget**
(retries ≤ 10 % of requests) caps the extra load at **+10 %**.

**Cascading failure:**

1. One dependency slows down.
2. Callers' threads and connections pile up waiting on it (pool exhaustion).
3. Callers slow down, so *their* callers pile up.
4. Retries multiply the load.
5. The whole system falls over.

The remedies below break each link of that chain.

## 7. Circuit breakers

Stop calling a dependency that's failing, and fail fast instead:

```
CLOSED ──(5 failures)──▶ OPEN ──(30 s cool-down)──▶ HALF_OPEN ──(probe ok)──▶ CLOSED
                           ▲                            │
                           └────────(probe fails)───────┘
```

In the run:

- 5 errors open the breaker, and the next 5 calls **fail fast** instead of each waiting for a
  timeout;
- after the cool-down one probe goes through: still down, so it **re-opens**;
- later a probe succeeds, and the breaker **closes**.

This protects both the struggling dependency (no load while it recovers) and the caller (no stuck
threads). Pair it with **fallbacks**: cached data, defaults, graceful degradation.

## 8. Bulkheads

Separate resource pools per dependency, like a ship's watertight compartments.

- A dependency hanging 30 s at 10 req/s needs **300 threads** (Little's law).
- With a **shared 200-thread pool** it takes them all, so the **healthy** endpoints fail too.
- With bulkheads (20 threads for the slow dependency, 180 for everything else) the fast path keeps
  working.
- Better still, a 2 s timeout means it needs only 20 threads in the first place.

## 9. Backpressure and load shedding

When demand exceeds capacity, **something** must give.

| Queue | After 60 s of 20 % overload (120 req/s on capacity 100) |
|---|---|
| **unbounded** | 1,200 requests queued; **every** request waits 12 s, so all of them time out, while the server still does the work. That's useless work and memory growth |
| **bounded** (50) + rejection | max wait 0.5 s; **17 %** of requests get a fast 429/503 and clients back off |

**Backpressure** = signalling upstream to slow down:

- TCP's receive window (chapter 04);
- reactive streams' `request(n)`;
- Kafka consumer lag;
- 429/503 with `Retry-After`.

**Load shedding:** drop low-priority work first (health checks and payments survive; analytics
doesn't).

## 10. Rate limiting

Token buckets per client or key (chapter 08 §8) protect services from abusive or buggy callers, and
global rate limits protect shared dependencies. They also cap retry storms when combined with retry
budgets.

## 11. Idempotency

**A retry after a timeout must not double-charge.** The client sends an **`Idempotency-Key`**. The
server stores key → result and returns the **stored result** for repeats. In the run:

- `order-77-attempt` was retried after a lost response: the **same receipt came back, and the
  customer was charged once**;
- without a stable key, the retry was a **second charge**.

Store keys with a TTL, scope them per client, and handle the "in progress" case (a concurrent
duplicate gets a 409 or waits).

## 12. Network partitions, partial failures, eventual consistency

Distributed systems face **partial failure**: some nodes or links are down while others are fine,
and you **can't tell slow from dead**. During a **partition** (the CAP theorem) you must choose:

| | **CP** | **AP** |
|---|---|---|
| behaviour during the partition | reject writes that can't reach a quorum | accept writes on both sides |
| result in the run | `rejected (can't reach a majority)` | the stock diverges: **97 vs 95** |
| after healing | already consistent | **reconcile**: merging the decrements gives **92** (CRDT-style). Last-write-wins would have silently **lost** one side's sales |
| examples | etcd, ZooKeeper, Spanner, single-leader SQL | Cassandra, DynamoDB (default), DNS, caches |

**Eventual consistency:** without new writes, all replicas eventually converge. Design for it with
idempotent operations, versioning (vector clocks), commutative merges (CRDTs), and
read-your-writes where users notice.

## 13. RPC vs REST, gRPC and HTTP/2, message brokers

| | **REST/JSON over HTTP** | **gRPC** |
|---|---|---|
| contract | resources + HTTP verbs; OpenAPI optional | **strongly typed** `.proto` service definitions, code generation |
| encoding | text JSON (`{"user_id":150,…}` = 49 bytes) | **protobuf** binary (**9 bytes**; `150` = varint `0x96 0x01`) |
| transport | HTTP/1.1 or 2 | **HTTP/2** (multiplexing, streaming, flow control) |
| streaming | SSE/WebSockets bolted on | client, server and **bidirectional streaming** built in |
| best for | public APIs, browsers, caching via HTTP semantics | internal service-to-service, low latency, polyglot contracts |
| gotchas | verbose, loose contracts | needs L7 / client-side balancing (one long HTTP/2 connection pins to one backend behind an L4 LB); browsers need gRPC-Web |

**Message brokers** (Kafka, RabbitMQ, SQS) decouple services **in time**: the producer doesn't need
the consumer up. They absorb bursts (a buffer), enable fan-out, and give natural backpressure
(consumer lag). Trade-offs: eventual consistency, at-least-once delivery (so **idempotent
consumers**), ordering only per partition, and the broker becomes critical infrastructure.

---

## Senior-level scenario: P99 jumps from 100 ms to 5 seconds

**"Your API normally responds in 100 ms, but its P99 suddenly rises to 5 seconds. How do you
investigate?"**

Start with **when, where and who**:

- Did it coincide with a deploy, a config change, a traffic spike or a dependency change?
- Is it all endpoints or some? All instances, zones or regions, or some?
- **The 5 s number itself is a clue:** a suspiciously round number usually means **a timeout or
  retry** somewhere (a DNS resolver timeout, a connect timeout, a pool acquire timeout).

Then walk the request path with metrics and **distributed traces** (which span got slow?):

| Suspect | Evidence | Chapter |
|---|---|---|
| **DNS** | slow or failed lookups; ~5 s = a resolver timeout; ndots fan-out | 05 |
| **connection establishment** | new connections (TLS handshakes) rising; pools churning; SYN retransmits (1 s, 3 s timeouts) | 04, 06 |
| **TCP retransmissions / packet loss** | `ss -ti` retrans counts, `nstat`, NIC drops, a specific AZ link | 04 |
| **load balancer** | surge queue, target connection limits, unhealthy targets, uneven balancing | 07, §1 |
| **application queueing** | thread pool saturated, event-loop lag (Node.js), GC pauses, CPU throttling in containers | OS course |
| **downstream latency** | DB slow queries, lock contention, a cache miss storm, a third-party API | §5 |
| **connection pool exhaustion** | time waiting to acquire a connection ≫ query time (Little's law) | §4 |
| **retry amplification** | request count to dependencies ≫ user requests; retries at several layers | §6 |
| **timeouts / deadlines** | requests finishing at exactly the timeout value; no deadline propagation | §5 |

**Mitigate first, then root-cause:**

1. Roll back the recent change.
2. Shed load, or scale out.
3. Open the circuit breakers or serve fallbacks.
4. Reduce timeouts and retries.

Then fix the cause, and add the missing alert (P99 per dependency, pool wait time, retry rate).

---

**Next:** [10 · Troubleshooting tools](../10-Troubleshooting-Tools/README.md)
