# HLD 05 · CAP, PACELC and Consistency Models

> Fundamentals. What CAP *actually* says (and doesn't), PACELC, the ladder of consistency models, and the tools for living with weak consistency: quorums, vector clocks, LWW and its traps, CRDTs. Each behaviour is simulated.
> Code: [`ConsistencyModels.java`](ConsistencyModels.java). Run with `java ConsistencyModels.java` (17 checks).

---

## 1. CAP, stated correctly
In a distributed system, **when a network Partition happens**, you must choose between:
- **Consistency** (linearizability: every read sees the latest write, as if there were one copy), or
- **Availability** (every request to a non-failed node gets a non-error response).

You can't "pick 2 of 3" in normal operation: partitions are not optional in real networks, so the real question is **what happens during a partition**.

The demo uses 3 nodes partitioned as {0, 1} | {2}:

| | Majority side | Minority side | After healing |
|---|---|---|---|
| **CP** (majority quorum: etcd, ZooKeeper, Spanner, HBase, MongoDB with majority writes) | reads and writes | **refuses** reads and writes | nothing to reconcile |
| **AP** (Dynamo, Cassandra at ONE, DNS, CouchDB) | accepts | **accepts** (values diverge: v2 vs v3) | reconcile; **LWW discarded v2** |

## 2. PACELC: the everyday trade-off
**If Partition: A or C; Else: Latency or Consistency.** Even with a healthy network, stronger consistency means waiting for more replicas. The demo has N=3 with 5% slow replies:

| Acks waited for | p99 write latency |
|---|---|
| W=1 | 1.8 ms |
| W=2 (majority) | 2.0 ms |
| W=3 (all) | **95.6 ms** (hits the slow tail) |

That's why majority quorums are the sweet spot, and why "write to all replicas synchronously" is rare.

Classification: Dynamo/Cassandra = PA/EL, Spanner/etcd = PC/EC, MongoDB (default) = PA/EC-ish, PNUTS = PC/EL.

## 3. Consistency models (strongest → weakest)

| Model | Guarantee | Cost / example |
|---|---|---|
| **Linearizable** (strong) | one global order matching real time; a read sees the latest completed write | consensus (Raft/Paxos), Spanner (TrueTime), single leader reads |
| **Sequential** | one global order every node agrees on, not necessarily real time | |
| **Causal** | if A could have influenced B, everyone sees A before B; concurrent ops may differ | vector clocks, COPS; great for social apps (a reply never before its post) |
| **Read-your-writes / monotonic reads / monotonic writes** (session guarantees) | per-client sanity | sticky routing, LSN tracking (see 02) |
| **Eventual** | if writes stop, replicas converge | DNS, Cassandra ONE, caches |

**Interview move:** choose the model *per piece of data*. Account balance / inventory / unique usernames → linearizable. Feed / likes / view counts → eventual. Chat order within a conversation → causal/per-key ordering. Profile edits → read-your-writes.

## 4. Conflict resolution under AP

| Technique | How | Problem |
|---|---|---|
| **Last-write-wins (LWW)** | the highest timestamp wins | **clock skew loses real updates**: a node with a fast clock beat a later write (demo: price=120 lost) |
| **Vector clocks + siblings** | detect concurrency; keep both versions; the client merges | complexity pushed to the app (the demo: a cart merged by union, nothing lost) |
| **CRDTs** | data types whose merge is commutative, associative and idempotent | limited operations, metadata overhead (the demo: G-Counter, PN-Counter, OR-Set converge in any merge order) |
| **Single leader / consensus** | avoid conflicts entirely | unavailable on the minority side; latency |

**Vector clock reading:** `a < b` if every component of a ≤ b and one is smaller → b supersedes a. Neither ≤ the other → **concurrent** → a real conflict.

**CRDT examples:** counters (likes, inventory deltas), sets (OR-Set: add-wins), registers (LWW-register, MV-register), sequences (collaborative text: RGA, Yjs, Automerge).

## 5. Consensus in one paragraph
Raft/Paxos replicate a log across 2f+1 nodes so that any committed entry survives f failures. A leader appends, a majority acknowledges, and then the entry commits. They power **CP** metadata systems (etcd for Kubernetes, ZooKeeper for Kafka's older versions, Spanner). Use them for small, critical state (leader election, config, locks, ids), not for bulk data paths.

## 6. Interview Q&A

**Q1. Explain CAP in one sentence.**
When the network partitions, a replicated system must either refuse some requests (stay consistent) or answer them with possibly stale or divergent data (stay available).

**Q2. Is MySQL with async replicas CP or AP?**
Neither cleanly. Reads from replicas are not linearizable even without partitions, and failover can lose acknowledged writes. Describe the actual behaviour rather than forcing a label.

**Q3. What is PACELC and why does it matter more day to day?**
Partitions are rare; latency is constant. PACELC adds that, even without partitions, stronger consistency means waiting for more replicas, so higher latency (2 ms vs 96 ms p99 in the demo).

**Q4. Why is last-write-wins dangerous?**
Clocks drift; a write with a later wall-clock timestamp can actually be earlier, so real updates are silently dropped. Use logical clocks, hybrid logical clocks, or conflict-free types when losing writes matters.

**Q5. What are vector clocks for?**
To decide whether one version supersedes another (happened-before) or the versions are concurrent and must be merged. Dynamo used them for shopping carts.

**Q6. What is a CRDT? Give an example.**
A replicated data type whose merge always converges regardless of order or duplication. A G-Counter keeps one counter per replica and merges by max; the value is the sum.

**Q7. How do you get strong consistency in a distributed DB?**
A consensus-replicated log per partition (Raft/Paxos) with leader reads or read-index/lease reads; for global ordering across partitions, synchronised clocks with bounded uncertainty (Spanner) or a timestamp oracle.

**Q8. Which consistency would you choose for a like counter? A bank transfer? Chat messages?**
Likes: eventual (CRDT counter / async aggregation). Bank transfer: strong/serializable, ledger-based. Chat: per-conversation total order from a single sequencer, plus causal delivery.

**Q9. What does "eventually consistent" fail to promise?**
When convergence happens, what you read meanwhile, and monotonicity: you can see a newer value and then an older one without session guarantees.

**Q10. How do quorums relate to CAP?**
Majority quorums make a system CP: the minority side can't form a quorum, so it becomes unavailable. Sloppy quorums / hinted handoff favour availability and weaken consistency.

## 7. Common mistakes
- "We picked CA."
- Treating eventual consistency as "consistent within a few ms", always.
- LWW with wall clocks for important data.
- Using consensus for high-volume data paths.
- One consistency level for everything instead of per data type.
