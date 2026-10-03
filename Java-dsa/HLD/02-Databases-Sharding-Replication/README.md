# HLD 02 · Databases: SQL vs NoSQL, Indexing, Sharding, Replication, Quorums, Migrations

> Fundamentals. Everything you'll be asked about the data tier, with each claim **measured** in a simulation: index speed-ups, the leftmost-prefix rule, keys moved by modulo vs consistent hashing, virtual-node balance, range-sharding hot spots, replication lag and read-your-writes, data loss on failover, quorum math, and why naive dual writes diverge during migrations.
> Code: [`DatabaseScaling.java`](DatabaseScaling.java). Run with `java DatabaseScaling.java` (18 checks).

---

## 1. SQL vs NoSQL: choose by access pattern

| | Relational (Postgres, MySQL) | Wide-column (Cassandra, ScyllaDB) | Key-value / doc (DynamoDB, MongoDB, Redis) |
|---|---|---|---|
| Model | tables + joins + constraints | partition key → sorted rows | key → item/document |
| Transactions | full ACID, multi-row | per partition, lightweight transactions | per item (DynamoDB has limited multi-item transactions) |
| Queries | ad hoc, joins, aggregates | only by partition key (+ clustering range) | by key (+ secondary indexes) |
| Scaling writes | vertical → sharding (Vitess, Citus) | linear, leaderless | linear, managed |
| Consistency | strong (single primary) | tunable (quorums) | tunable / strong per read |
| Great for | money, orders, inventory, anything relational | messages, time-series, feeds, IoT | sessions, carts, profiles, caches |

**Interview answer:** "I'd start with Postgres for the transactional core because of constraints and transactions. For the 200K writes/s message store with key + time-range access I'd use Cassandra, partitioned by conversation id." Polyglot persistence is normal.

## 2. Indexing
- A **B-tree index** turns a full scan (O(n)) into a tree descent + range scan (O(log n + k)). The demo: 100,000 rows scanned vs **11** index entries for the same 613 results.
- **Composite indexes follow the leftmost-prefix rule.** `(country, age)` serves `country=?` and `country=? AND age BETWEEN …`, but not `age=?` alone.
- **Costs:** every index slows writes (it must also be updated) and uses space; too many indexes hurt write-heavy tables.
- **Covering index:** includes all queried columns, so no table lookup is needed.
- **Other kinds:** hash (equality only), LSM-tree based stores (fast writes, compaction), inverted (full-text), geospatial (R-tree, geohash), bitmap (analytics).

## 3. Replication

| Mode | How | + | − |
|---|---|---|---|
| **Leader–follower, async** | the leader commits, ships the log later | fast writes | **lag**: stale reads, data loss on failover (RPO > 0) |
| **Leader–follower, sync / semi-sync** | wait for ≥ 1 follower ack | no loss on a single failure | higher write latency |
| **Multi-leader** | several regions accept writes | local writes per region | **write conflicts** (LWW, CRDTs, app merge) |
| **Leaderless** (Dynamo, Cassandra) | write to N, quorums W/R | no failover, highly available | conflict resolution, read repair, hinted handoff |

**Replication lag problems (and fixes):**
- **Read-your-writes:** you update your profile and then see the old one. Fix: read from the leader for a short window after writing, or track your last write's LSN and only use followers that have reached it (shown in the demo).
- **Monotonic reads:** pin a user to one replica, so time doesn't go backwards.
- **Failover data loss:** the async leader dies before shipping its last writes (demo: `order:9` lost). Use semi-sync for critical data.

## 4. Quorums (N replicas, write to W, read from R)
- **R + W > N ⇒ every read overlaps the latest write** → strong-ish consistency (the demo: 0% stale for W=2/R=2, W=1/R=3, W=3/R=1).
- R + W ≤ N ⇒ stale reads possible (W=1, R=1: **67%** stale in the demo).
- Tuning: W=N, R=1 for read-heavy workloads; W=1, R=N for write-heavy ones; W=R=⌈(N+1)/2⌉ is balanced.
- Caveats: sloppy quorums, concurrent writes and clock-based LWW can still surprise you; this isn't linearizability without extra mechanisms.

## 5. Sharding (partitioning)

| Strategy | + | − |
|---|---|---|
| **Range** (by key/time) | range scans, easy splits | **hot spots**: sequential keys all hit the last shard (demo: 100% of today's writes on one shard) |
| **Hash % N** | even spread | adding a shard moves **~80%** of keys (demo, 4 → 5) |
| **Consistent hashing** | adding a shard moves **~1/N** (demo: 19%) | balance needs **virtual nodes** (max/avg load 2.52 → 1.07 with 200 vnodes) |
| **Directory / lookup service** | any placement, easy moves | an extra lookup; the directory must be HA |

**Choosing the shard key:** high cardinality, even distribution, and *aligned with the queries* (shard messages by conversation_id so a conversation is one shard). **Avoid** monotonically increasing keys for hash-less ranges; avoid celebrity keys (split them: `key#bucket`).

**The pains of sharding:** cross-shard queries (scatter-gather), cross-shard transactions (sagas / 2PC), global secondary indexes, rebalancing with live traffic, unique ids (Snowflake: timestamp + machine + sequence).

## 6. Live migrations and dual writes (from experience)
- **Naive dual write** (the app writes the old DB, then the new one): any failure between the two writes silently diverges the stores. The demo: 1% failures leave mismatched accounts that no error ever reported.
- **Safer playbook:**
  1. Keep one **source of truth**.
  2. Stream its changes via **CDC** (binlog/WAL → Debezium → Kafka) or a **transactional outbox**.
  3. **Backfill** history.
  4. **Verify** continuously (row counts, checksums, sampled diffs).
  5. Shadow-read from the new store and compare.
  6. Flip reads, then writes, behind flags; keep a rollback path.
- The demo shows CDC (an ordered log + retries + idempotent upserts) converging with **0** differences.

## 7. Other topics to name-drop correctly
- **Isolation levels:** read committed, repeatable read (snapshot), serializable. Know the anomalies they allow: lost update, write skew.
- **Connection pooling** (PgBouncer): a DB has finite connections; thousands of app instances need a pooler.
- **Read replicas for analytics** vs a separate OLAP store (columnar: BigQuery, ClickHouse) fed by CDC.
- **Partitioning within one DB** (Postgres declarative partitioning by time) for retention and pruning.
- **Hot/cold tiering:** recent data on SSD/primary, old data in object storage.

## 8. Interview Q&A

**Q1. How do you scale a relational database?**
In order: indexes and query fixes → caching → read replicas → vertical scaling → partitioning tables → sharding (app-level or Vitess/Citus) → moving specific workloads to purpose-built stores.

**Q2. Why consistent hashing over modulo?**
Adding or removing a node moves only ~1/N of the keys instead of nearly all of them, so rebalancing and cache misses stay small. Virtual nodes fix the uneven arcs.

**Q3. How do you pick a shard key?**
High cardinality, even load, and matching the dominant query so it hits one shard. Check for hot keys and monotonic values. Consider compound keys (user_id + bucket).

**Q4. What problems does replication lag cause, and how do you mitigate them?**
Stale reads, read-your-writes violations, non-monotonic reads. Mitigate with leader reads after writes, LSN-aware routing, replica pinning, or synchronous replication for critical paths.

**Q5. Explain R + W > N.**
The read set and write set must overlap in at least one replica, so a read always contacts a replica holding the latest acknowledged write (plus version comparison to pick it).

**Q6. Leader-follower vs leaderless?**
Leader-follower: simple, strongly consistent reads from the leader, but failover is required. Leaderless: always writable with quorums, but needs conflict resolution, read repair and anti-entropy.

**Q7. How do you handle a hot partition?**
Split the key (add a random/bucket suffix and aggregate on read), cache it, rate-limit it, or move it to a dedicated shard. Detect it via per-partition metrics.

**Q8. How do you migrate databases without downtime?**
CDC/outbox replication from the source of truth, backfill, continuous verification, shadow reads, a gradual cutover with feature flags, and a rollback plan. Avoid blind dual writes.

**Q9. How do you generate unique IDs across shards?**
Snowflake-style 64-bit ids (timestamp | datacenter | machine | sequence): sortable by time and needing no coordination. UUIDv7 is the time-ordered alternative. Avoid DB auto-increment across shards.

**Q10. When is denormalisation right?**
When reads dominate and joins across shards or services are too expensive. Duplicate data in the read model (CQRS) and keep it updated via events, accepting eventual consistency.

## 9. Common mistakes
- Choosing NoSQL "for scale" without naming the access pattern.
- Sharding by timestamp or auto-increment.
- `hash % N` with plans to add nodes.
- Ignoring replication lag in read-after-write UX.
- Dual writes without reconciliation.
- Indexes on everything in write-heavy tables.
