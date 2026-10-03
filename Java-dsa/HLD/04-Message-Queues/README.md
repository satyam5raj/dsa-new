# HLD 04 · Message Queues and Event Logs

> Fundamentals, and they map directly to a RabbitMQ background. Queue vs log, pub-sub vs point-to-point, **delivery semantics measured under crashes**, ordering with partitions, consumer groups and rebalancing, the **transactional outbox**, retries + DLQ, and consumer lag.
> Code: [`MessagingPatterns.java`](MessagingPatterns.java). Run with `java MessagingPatterns.java` (16 checks; a mini Kafka in ~150 lines).

---

## 1. Why a queue?
- **Decoupling:** the producer doesn't know or wait for consumers (the order service emits `OrderPlaced`; billing, email and analytics react).
- **Load levelling:** absorb spikes; consumers work at their own pace.
- **Async work:** move slow tasks (emails, thumbnails, fan-out) out of the request path.
- **Reliability:** persist work; retry failures without user impact.
- **Fan-out:** one event, many independent consumers.

## 2. Two families

| | Message queue (RabbitMQ, SQS, ActiveMQ) | Log / stream (Kafka, Kinesis, Pulsar) |
|---|---|---|
| Model | messages delivered to consumers and **deleted** on ack | an **append-only log**; consumers track **offsets**; data retained by time/size |
| Replay | no (once acked it's gone) | yes: rewind offsets, add new consumers later |
| Ordering | per queue (weakened by competing consumers / redelivery) | per **partition** |
| Routing | rich (exchanges: direct, topic, fanout, headers) | by topic + key → partition |
| Throughput | tens of K msgs/s per node | hundreds of K to millions per broker |
| Best for | task queues, RPC-style work, complex routing, per-message TTL/priority | event streams, event sourcing, CDC, analytics, high fan-out |

## 3. Pub-sub vs point-to-point
- **Point-to-point (competing consumers):** each message is processed by **one** worker; workers share the load (a RabbitMQ queue with N consumers; one Kafka consumer group).
- **Pub-sub:** each **subscriber** gets every message (a RabbitMQ fanout exchange with one queue per subscriber; Kafka: one consumer *group* per subscriber).
- Kafka does both: *within* a group partitions are split (the demo: b1 → [0, 2], b2 → [1, 3]); *between* groups everyone gets everything (billing and email each saw all 100).

## 4. Delivery semantics (measured: 1,000 messages, 10% crashes)

| Semantics | How | Demo result | Use |
|---|---|---|---|
| **At-most-once** | commit/ack **before** processing | 116 lost, 0 duplicates | metrics, logs where loss is OK |
| **At-least-once** | process, **then** commit/ack | 0 lost, 125 duplicates | the default for business events |
| **Effectively-once** | at-least-once + **idempotent consumer** (dedupe by message id / natural key, or upsert) | 1,000 effects, exactly | payments, orders, anything with side effects |

"Exactly-once" in Kafka means transactions *within Kafka* (read-process-write across topics, idempotent producers). Side effects in external systems (email, DB, a payment API) still need idempotency. Say this explicitly.

## 5. Ordering
- Order is guaranteed **only within a partition** (or a single queue with a single consumer).
- Choose the **key** so everything that must be ordered shares it: `orderId` for an order's lifecycle, `userId` for a user's actions (the demo: each user's events stay ordered).
- Ordering breaks with: retries that re-enqueue at the tail, multiple consumers on one queue, changing the partition count (keys remap), and producer retries without idempotent producers.
- **Hot keys** make hot partitions: a single key can't be parallelised beyond one consumer.

## 6. Consumer groups, rebalancing, lag
- The number of partitions caps a group's parallelism: 6 consumers on 3 partitions → 3 idle (demo). Pick partitions for **peak** throughput up front.
- **Rebalancing** happens on join/leave/crash: partitions move and in-flight work may be redone (another reason for idempotency). Cooperative/sticky assignors reduce disruption.
- **Lag** = end offset − committed offset. It's the key health metric: autoscale consumers on it, and alert when it grows.

## 7. Transactional outbox (the dual-write problem again)
"Save the order to the DB **and** publish OrderPlaced" is two systems. A crash between them loses the event (the demo: 95 of 10,000 events silently missing), or publishes an event for an order that rolled back.

**Outbox:** in the *same DB transaction* as the business write, insert a row into `outbox`. A relay (polling or CDC with Debezium) publishes the outbox rows and marks them sent, retrying until it succeeds. It may publish twice, so consumers dedupe by event id. The demo: every saved order is published.

The inverse, for consumers: the **inbox** pattern (store processed message ids in the same transaction as the side effect).

## 8. Retries, DLQ, backpressure
- **Retry with backoff** (in-process for transient errors; delayed retry topics/queues for longer waits).
- **Dead-letter queue** after N attempts, so a poison message doesn't block a partition forever (the demo: 49 processed, 1 parked after 3 tries). Alert on the DLQ, inspect it, fix, and replay.
- **Backpressure:** bounded prefetch (RabbitMQ `basic.qos`), consumer pause/resume, and producer quotas; let lag grow rather than crash consumers.

## 9. Interview Q&A

**Q1. Kafka vs RabbitMQ?**
RabbitMQ: a smart broker for task distribution, flexible routing, per-message acks and TTLs; messages disappear after consumption. Kafka: a durable partitioned log, massive throughput, replay, multiple independent consumer groups, ordering per partition. Choose by need: task queue vs event stream.

**Q2. How do you guarantee ordering?**
Same key → same partition, one consumer per partition, and in-order processing (no parallel processing inside a partition unless keyed sub-queues are used). Idempotent producers avoid reordering on retry.

**Q3. At-least-once vs exactly-once?**
At-least-once (process, then commit) never loses but may duplicate. Exactly-once end-to-end requires idempotent processing or transactional coupling of the side effect with the offset commit. Kafka EOS only covers Kafka-to-Kafka.

**Q4. How do you make a consumer idempotent?**
Dedupe on a unique message/event id stored in the same transaction as the effect (an inbox table / unique constraint), use upserts keyed by natural ids, or design operations to be naturally idempotent ("set status=PAID" rather than "add 100").

**Q5. What is the outbox pattern and why?**
It avoids the DB+broker dual write: persist the event in an outbox table atomically with the business change, and publish asynchronously with retries. The guarantee: an event is published if and only if the transaction committed (at least once).

**Q6. A poison message blocks the queue. What do you do?**
Limit retries, move it to a DLQ with error metadata, continue, alert, and replay after fixing. Classify errors: retryable vs non-retryable.

**Q7. How many partitions?**
Enough for peak parallel consumers and throughput (target throughput / per-consumer throughput), with headroom; not thousands per topic without need (more files, rebalancing cost). Increasing later remaps keys, so plan up front.

**Q8. How do you handle a slow consumer?**
Scale the consumers (up to the partition count), optimise per-message work (batching), increase partitions for future data, or shed/skip non-critical work; monitor lag.

**Q9. Push vs pull consumers?**
Push (RabbitMQ) gives low latency but needs prefetch limits for backpressure. Pull (Kafka) lets consumers control their pace and batch naturally.

**Q10. When would you NOT use a queue?**
When the caller needs the result synchronously and quickly, or when the added complexity (eventual consistency, ops, debugging) outweighs the decoupling, e.g. a simple CRUD service with low traffic.

## 10. Common mistakes
- Assuming exactly-once side effects.
- Committing offsets before processing "for speed".
- Publishing events outside the DB transaction (dual write).
- Infinite retries on poison messages.
- Too few partitions for future scale; relying on global ordering across partitions.
