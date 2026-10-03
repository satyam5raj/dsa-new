# HLD: High-Level Design for senior backend interviews

Two halves:
1. **Fundamentals (01–06):** the building blocks. Each claim is **proven by a runnable simulation** with numbers, not just stated.
2. **Case studies (07–12):** the classic interview problems, each answered in the same 6-step format, plus a simulation of its hardest mechanism.

Every folder has a `README.md` (concepts or the full design answer, Mermaid architecture diagrams, trade-offs, failure modes, 10 interview Q&As) and one `.java` simulation (`java File.java`). Run everything with `./run-all.sh`.

**Start with [00 · HLD Playbook](00-HLD-Playbook/)**: the 45-minute answer structure, the estimation cheat sheet, and `Estimation.java` (which caught two of my own arithmetic slips while writing it).

| # | Topic | Proven in the simulation | Status |
|---|---|---|---|
| 00 | [HLD Playbook](00-HLD-Playbook/) | QPS / storage / server / cache estimates for the case studies | ✅ |
| 01 | [Scalability & Load Balancing](01-Scalability-Load-Balancing/) | round robin p99 **51,524** vs least-conn **13** with one slow box; P2C; health checks; affinity | ✅ |
| 02 | [Databases: Sharding & Replication](02-Databases-Sharding-Replication/) | index 100,000 → 11 rows; modulo moves 80% vs ring 19%; vnodes 2.52 → 1.07; replication lag; R+W>N; dual write vs CDC | ✅ |
| 03 | [Caching](03-Caching/) | Zipf hit ratios; the stale-set race; write-behind loss; stampede 64 → 1; jitter; penetration 10,000 → 1; hot keys | ✅ |
| 04 | [Message Queues](04-Message-Queues/) | mini Kafka; at-most-once loses 116, at-least-once duplicates 125, idempotent = exact; outbox vs dual write; DLQ | ✅ |
| 05 | [CAP & Consistency](05-CAP-Consistency/) | CP refuses vs AP diverges; LWW clock skew; vector clocks; CRDTs; PACELC p99 2 ms vs 96 ms | ✅ |
| 06 | [API Design](06-API-Design/) | offset duplicates vs cursor; idempotency keys; ETag 412; 429 + Retry-After; tolerant readers | ✅ |
| 07 | [URL Shortener](07-URL-Shortener/) | KGS range leases (0 duplicates, 172 leases per 1M ids); CDN/Redis/DB tiers; 301 vs 302 | ✅ |
| 08 | [Chat System](08-Chat-System/) | gateway registry routing; offline sync; gateway crash with no loss; group fan-out; partition bucketing | ✅ |
| 09 | [News Feed](09-News-Feed/) | push vs pull vs hybrid give identical feeds; writes 7.3M / 0 / 1.1M, fan-in 1 / 64 / 5.6 | ✅ |
| 10 | [Ride-Sharing](10-Ride-Sharing/) | real geohash (verified), 9-cell search vs brute force; hot metro shard; batched matching 12% better | ✅ |
| 11 | [Distributed Cache](11-Distributed-Cache/) | replication survives node loss (79% → 100%); 1/N rebalancing; slot migration with MOVED; Count-Min hot keys | ✅ |
| 12 | [Notification System](12-Notification-System/) | OTP p99 **493 s** in FIFO vs **1 s** with priority queues; campaign throughput; caps; provider failover | ✅ |

## How the three tracks connect

| Problem | LLD (classes, concurrency) | HLD (system) |
|---|---|---|
| URL shortener | [LLD 05](../LLD/05-URL-Shortener/) | [HLD 07](07-URL-Shortener/) |
| Chat | [LLD 14](../LLD/14-Chat-Application/) | [HLD 08](08-Chat-System/) |
| Ride-sharing | [LLD 08](../LLD/08-Ride-Sharing/) | [HLD 10](10-Ride-Sharing/) |
| Cache | [LLD 06](../LLD/06-Cache-LRU-LFU/), [LLD 17](../LLD/17-KeyValue-Store/) | [HLD 03](03-Caching/), [HLD 11](11-Distributed-Cache/) |
| Notifications | [LLD 15](../LLD/15-Notification-System/) | [HLD 12](12-Notification-System/) |
| Rate limiting | [LLD 04](../LLD/04-Rate-Limiter/) | [HLD 06](06-API-Design/) |
| Payments | [LLD 18](../LLD/18-Payment-Wallet/) | idempotency in [HLD 04](04-Message-Queues/), [HLD 06](06-API-Design/) |

## The answer format (memorise it)
`Requirements → Estimation → API + data model → High-level design → Deep dives → Trade-offs & failure modes`
