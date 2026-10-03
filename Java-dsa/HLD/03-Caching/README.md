# HLD 03 · Caching: Patterns, Invalidation, Failure Modes

> Fundamentals. Where to cache, cache-aside vs read-through vs write-through vs write-behind, **invalidation races**, sizing with power-law traffic, and the failure modes interviewers probe: **stampede, penetration, hot keys, avalanche**. Each one is simulated with numbers.
> Code: [`CachingPatterns.java`](CachingPatterns.java). Run with `java CachingPatterns.java` (13 checks).
> The cache *implementation* (LRU/LFU, TTL, single-flight) is LLD [06](../../LLD/06-Cache-LRU-LFU/); a Redis-like store is LLD [17](../../LLD/17-KeyValue-Store/).

---

## 1. Where caches live

| Layer | Example | Caches |
|---|---|---|
| Client | browser / app cache, HTTP `Cache-Control` | static assets, API responses |
| CDN / edge | CloudFront, Akamai | images, video, JS/CSS, cacheable API GETs |
| API gateway | response cache | public, identical responses |
| Application, local | Caffeine (in-process) | ultra-hot keys, config (**near cache**) |
| Application, distributed | Redis / Memcached | DB query results, sessions, computed objects |
| Database | buffer pool, query cache | pages, plans |

Rule: cache as close to the user as the **freshness requirement** allows.

## 2. Read/write patterns

| Pattern | Read path | Write path | + | − |
|---|---|---|---|---|
| **Cache-aside** (lazy loading) | app reads the cache; on a miss, reads the DB and populates the cache | app writes the DB, then **deletes** the cache key | simple, only caches what's read, survives cache loss | first read is slow; **race windows** (below) |
| **Read-through** | the cache library loads on a miss | — | app code is simpler | same staleness issues |
| **Write-through** | — | write the cache **and** DB synchronously | cache always fresh | write latency; caches unread data |
| **Write-behind** (write-back) | — | write the cache; flush to the DB asynchronously in batches | fast writes; coalescing (demo: 1,000 writes → 430 DB writes) | **data loss** if the cache dies before flushing (demo: 32 keys lost) |
| **Refresh-ahead** | refresh hot keys before they expire | — | no miss latency for hot keys | wasted refreshes for keys that cool down |

**Why delete instead of update the cache on writes?** Two concurrent writers updating DB and cache in different orders can leave the cache permanently wrong. Deleting is idempotent; the next read reloads the truth.

## 3. The stale-cache race (even with "update DB, then delete")
```
t1 Reader: cache miss
t2 Reader: reads DB -> 100 (old)
t3 Writer: DB = 120
t4 Writer: delete cache (nothing there)
t5 Reader: SET cache = 100      <- stale until TTL
```
The demo replays this exactly. Mitigations:
- **Versioned set:** only cache if the value's version is ≥ the current one (Redis Lua / CAS). Demo: the stale set is refused.
- **Delayed double delete:** delete again ~1 s after the write.
- **Short TTLs** as a safety net, always.
- **CDC-driven invalidation:** consume the DB binlog and delete the keys, so invalidation comes from the source of truth.
- If you need strict consistency, don't cache that path (or use write-through with versioning).

## 4. Sizing: power laws are your friend
Real traffic is Zipf-like. Demo with 100,000 items, Zipf(1.0), LRU:

| Cache size | Hit ratio |
|---|---|
| 1% of items | 51% |
| 10% | 73% |
| 20% | 79% |

A tiny cache absorbs most reads, with diminishing returns after that. In estimation, cache "the hot 20%" and expect a ~80% hit ratio.

## 5. Failure modes

| Problem | What happens | Fix | Demo |
|---|---|---|---|
| **Stampede / thundering herd / dogpile** | a hot key expires and N concurrent misses all hit the DB | single-flight (request coalescing), locks, refresh-ahead, serve-stale-while-revalidate | 64 DB calls → **1** |
| **Avalanche** | many keys set together expire together, or the cache cluster restarts cold | **TTL jitter**, warm-up, staggered restarts, rate-limit the DB | worst second: 10,000 expiries → **34** with ±10 min jitter |
| **Penetration** | requests for ids that don't exist always miss and hit the DB (bugs/attacks) | **negative caching** (cache "not found" briefly) + a **Bloom filter** of existing ids | 10,000 DB calls → 200 (negative cache) → **1** (both) |
| **Hot key** | one key gets a huge share of traffic and one cache shard melts | **near cache** in each app server (short TTL), key replication (`key#1..#N`), read replicas | hottest shard 77K → 17K req (≈ average) |
| **Big keys** | a 10 MB value blocks the cache's network/CPU | split, compress, or store in object storage + cache a pointer | – |

## 6. Eviction and expiry
LRU (recency), LFU (frequency, better for stable popularity), TTL-based; Redis's `maxmemory-policy` (allkeys-lru, volatile-ttl…). Use TTLs on everything you can, plus explicit invalidation for correctness.

## 7. Distributed cache topology
- Client-side sharding with **consistent hashing** (Memcached style) or Redis Cluster (16,384 hash slots).
- Replication for availability (a replica per shard); reads from replicas trade freshness for capacity.
- A cache is **not** a source of truth: design so that losing it costs latency, not data (except with write-behind, where that's the accepted trade-off).
- More in case study [11 · Distributed Cache](../11-Distributed-Cache/).

## 8. Interview Q&A

**Q1. Cache-aside vs write-through?**
Cache-aside caches only what's read and tolerates cache loss, but has a miss penalty and race windows. Write-through keeps the cache fresh on every write, at the cost of write latency and caching data that may never be read.

**Q2. On a write, do you update or delete the cache?**
Delete (invalidate). Updates from concurrent writers can be applied out of order and leave a permanently wrong value; a delete just forces the next reader to load the truth.

**Q3. How do you keep cache and DB consistent?**
Accept bounded staleness with TTLs plus invalidation on write; close race windows with versioned sets or delayed double deletes; for full robustness drive invalidation from CDC. If strict consistency is required, read from the DB.

**Q4. What is a cache stampede and how do you prevent it?**
Many concurrent misses on the same key overload the backend. Prevent it with single-flight/request coalescing, a distributed lock for the loader, serve-stale-while-refreshing, refresh-ahead, and jittered TTLs.

**Q5. How do you handle a hot key?**
A local near cache with a short TTL, replicating the key under several names spread across shards, or precomputing the value. Detect hot keys with sampling (Redis `--hotkeys`, proxy metrics).

**Q6. What is cache penetration?**
Lookups for keys that never exist bypass the cache and hit the DB every time. Cache negative results briefly and put a Bloom filter of valid ids in front.

**Q7. How big should the cache be?**
Size for the hot working set: estimate daily distinct hot items × object size, aiming for the knee of the hit-ratio curve (often ~20% of items for ~80% of reads), then validate with hit-ratio metrics.

**Q8. Write-behind: when is it acceptable?**
When losing the last few seconds of writes is tolerable (view counters, analytics, likes) and write volume is high. Never for money or orders unless the cache itself is durable.

**Q9. What happens when the cache cluster goes down?**
Every request hits the DB, which can cascade into a DB outage. Protect the DB with rate limits/circuit breakers, warm caches gradually, run caches replicated across AZs, and degrade gracefully (serve partial or stale content).

**Q10. CDN caching: what about dynamic content?**
Cache with short TTLs or `stale-while-revalidate`, vary on the right headers, use surrogate keys to purge related objects, and keep personalised data out of shared caches.

## 9. Common mistakes
- Updating the cache instead of deleting it on writes.
- No TTLs ("we invalidate correctly", until a bug).
- Identical TTLs for bulk-loaded keys.
- Treating the cache as the source of truth.
- Ignoring hot keys and negative lookups.
- Caching personalised responses at a shared layer.
