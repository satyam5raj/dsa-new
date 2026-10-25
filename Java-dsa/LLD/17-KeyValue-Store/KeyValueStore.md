# 17 · In-Memory Key-Value Store, a mini Redis (explained simply)

| | |
|---|---|
| **Code** | [KeyValueStore.java](KeyValueStore.java) |
| **Run** | `java KeyValueStore.java` (26 self-checks, including concurrent INCR, CAS loops and ~8,000 transfer transactions) |
| **Design notes** | [README.md](README.md) (replication, cluster mode, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md), [Cache LLD](../06-Cache-LRU-LFU/CacheSystem.md) |

A key-value store is a big `Map<String, String>` with superpowers: keys that **expire**, a **memory limit** with eviction, **atomic** counters, **compare-and-set**, **transactions**, and a **log** so data survives a restart. It's the backbone of caching, sessions, rate limiters and distributed locks.

---

## 1. The problem in plain words

Build Redis-lite:
- `get / set / del`, `set` with a TTL ("forget this in 5 seconds") and NX ("only if it doesn't exist", the basis of a lock);
- `incr` that never loses an update under concurrency;
- `cas`: "change it only if nobody else changed it since I read it";
- a max number of keys, with a choice: reject writes, or evict the least recently used;
- expired keys get cleaned up even if nobody reads them;
- transactions: several operations, all or nothing, aborted if a watched key changed;
- persistence: replay a log of commands to rebuild the data.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Value types? | Strings (integers stored as strings for INCR) |
| Expiry? | Per key, in ms; `ttl` returns −2 for no key, −1 for no expiry (Redis semantics) |
| Memory limit? | Max number of keys; a policy decides what happens when full |
| Concurrency? | Many threads; operations on one key must be atomic |
| Transactions? | MULTI/EXEC with optimistic WATCH |
| Persistence? | An append-only file (AOF) of write commands |
| Distributed? | No, a single node (see the follow-ups) |

### Step 2 · Requirements
1. All single-key operations are atomic.
2. Expired keys are never returned; they're cleaned up lazily and actively.
3. A pluggable eviction policy.
4. Every write bumps a **version** (used by CAS and WATCH).
5. Transactions over several keys, without deadlocks.
6. Every write is recorded as a command; replaying them gives the same data.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| value + metadata | `record Entry(value, version, expiresAt, lastAccess)` (immutable) |
| store | `class KVStore` (stripes of HashMaps + locks) |
| eviction rule | `interface EvictionPolicy` (`NoEviction`, `SampledLru`) |
| transaction | `class Transaction` (watched versions + queued ops) |
| logged write | `sealed interface Command` → `SetCmd`, `DelCmd`, `ExpireCmd` |

### Step 4 · Relationships

```
KVStore
  ├── stripe 0: HashMap<String, Entry> + ReentrantLock
  ├── stripe 1: HashMap<String, Entry> + ReentrantLock
  ├── ...                                         key → stripe = hash(key) mod N
  ├── EvictionPolicy (Strategy)
  └── aof: List<Command>  ──replay──▶ a new KVStore with the same data
```

### Step 5 · API

```java
Optional<String> get(key);      boolean set(key, value, ttlMs, onlyIfAbsent);
boolean del(key);               long incrBy(key, delta);
boolean cas(key, expectedVersion, newValue);
boolean expire(key, ttlMs);     long ttl(key);       List<String> scan(prefix);
int activeExpireCycle();
Transaction multi().watch(k).incrBy(a, -30).incrBy(b, 30).exec();
static KVStore replay(List<Command> aof, ...);
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| what to evict when full | **Strategy** (`EvictionPolicy`) |
| write operations as data you can replay | **Command** (`SetCmd`, `DelCmd`, `ExpireCmd`) |
| a value you can share safely | **immutable value object** (`Entry`) |
| concurrent writers | **lock striping** |
| "has it changed since I read it?" | **optimistic locking** with versions |

### Step 7 · Concurrency
One global lock would make every thread wait for every other. One lock per key costs too much memory. The middle ground is **lock striping**: N locks, each guarding the keys that hash to it.

---

## 3. The key ideas, simply

### Idea 1: lock striping, like checkout lanes
A supermarket with one till is a queue out the door; a till per customer is absurd. So there are 16 lanes, and your lane is decided by your key's hash. Two keys in different lanes are served **in parallel**; two operations on the same key are in the same lane, so they're **in order**.

**Why Redis doesn't need this:** Redis runs commands on **one thread**, one at a time, so there are no races at all. Java serves many threads, so we partition.

### Idea 2: versions (like a document's "last edited" stamp)
Every write gives the entry a new, larger version. CAS says "save my change only if the version is still the one I read". If someone wrote in between, the version moved on and your write is rejected, so you re-read and retry.

### Idea 3: two kinds of expiry
- **Lazy:** when you touch a key, check whether it's expired; if so, delete it and act as if it didn't exist. That's cheap, but a key nobody reads stays in memory forever.
- **Active:** every so often, sample 20 keys with a TTL and delete the expired ones; if more than 25% were expired, there are probably many more, so sample again. That's **bounded work** each cycle and still reclaims memory.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` (immutable) | `Entry` | a write replaces the entry; nobody can modify one in place |
| `Function<Map, T>` + a helper | `locked(key, m -> ...)` | "run this code with that key's stripe locked" |
| `Math.floorMod(h ^ (h >>> 16), n)` | `stripeOf` | mixes high bits into low bits, then gives a non-negative index |
| `Optional` / `OptionalLong` | `get`, `version` | "maybe a value" without null |
| `Math.addExact` | `incrBy` | throws on overflow instead of wrapping around |
| `LongAdder` | eviction / expiry counters | a counter that's fast under heavy contention |
| `sealed interface` + record patterns in `switch` | `Command`, `replay` | a closed set of command types; the compiler checks the switch covers them all |
| `TreeSet<Integer>` | `Transaction.exec` | stripe ids sorted, so locks are always taken in ascending order |
| `Collections.synchronizedList` | `aof` | a list safe to append to from many threads |

---

## 5. The code, piece by piece

### 5.1 Stripes and the `locked` helper

```java
int stripeOf(String key) { int h = key.hashCode(); return Math.floorMod(h ^ (h >>> 16), stripes); }

private <T> T locked(String key, Function<Map<String, Entry>, T> op) {
    int s = stripeOf(key);
    locks.get(s).lock();
    try { return op.apply(maps.get(s)); } finally { locks.get(s).unlock(); }
}
```

Every public operation is `locked(key, m -> { ... })`, so the lock/unlock code is written once. `floorMod` (unlike `%`) never returns a negative index.

### 5.2 Lazy expiry

```java
private Entry live(Map<String, Entry> m, String key) {
    Entry e = m.get(key);
    if (e != null && e.expired(clock.getAsLong())) { m.remove(key); expiredRemoved.increment(); return null; }
    return e;
}
// Entry: boolean expired(long now) { return expiresAt > 0 && now >= expiresAt; }
```

`expiresAt = 0` means "never expires". `>=` means a key is gone **at** its deadline, not one millisecond later.

### 5.3 SET with TTL and NX, plus making room

```java
boolean set(String key, String value, long ttlMs, boolean onlyIfAbsent) {
    return locked(key, m -> {
        Entry cur = live(m, key);
        if (onlyIfAbsent && cur != null) return false;           // NX: someone has it
        if (cur == null) makeRoom(m);                            // only a NEW key needs space
        long exp = ttlMs > 0 ? clock.getAsLong() + ttlMs : 0;
        m.put(key, new Entry(value, versions.incrementAndGet(), exp, clock.getAsLong()));
        aof.add(new SetCmd(key, value, exp));                    // log the ABSOLUTE expiry time
        return true;
    });
}
```

`SET lock:job worker-1 NX PX 30000` is the classic **distributed lock**: only one worker can create the key, and the TTL frees it if that worker crashes.

The AOF records the **absolute** expiry time, not "30 s from now"; otherwise replaying the log after a restart would give the key a fresh 30 seconds.

### 5.4 Eviction policies

```java
private void makeRoom(Map<String, Entry> m) {
    if (m.size() < maxKeysPerStripe) return;
    String victim = eviction.chooseVictim(m, rnd);
    if (victim == null) throw new KVException("OOM: maxmemory reached and policy is noeviction");
    m.remove(victim); evictions.increment(); aof.add(new DelCmd(victim));
}
```

**SampledLru**, Redis' trick: a true LRU needs a linked list with two pointers per key (lots of memory). Instead, pick 5–10 **random** keys and evict the one with the oldest `lastAccess`. That's nearly as good, with no extra memory. `get` refreshes `lastAccess` (same version, because reading isn't a write).

### 5.5 INCR and CAS

```java
long incrBy(String key, long delta) {
    return locked(key, m -> {
        Entry e = live(m, key);
        long cur = e == null ? 0 : Long.parseLong(e.value());        // missing counts as 0; non-number → error
        long next = Math.addExact(cur, delta);
        m.put(key, new Entry(Long.toString(next), versions.incrementAndGet(), e == null ? 0 : e.expiresAt(), ...));   // keeps the TTL
        ...
    });
}

boolean cas(String key, long expectedVersion, String newValue) {
    return locked(key, m -> {
        Entry e = live(m, key);
        if (e == null || e.version() != expectedVersion) return false;   // someone wrote since you read
        m.put(key, new Entry(newValue, versions.incrementAndGet(), e.expiresAt(), ...));
        ...
    });
}
```

INCR is a read-modify-write **inside** the lock, so no update is lost. CAS lets a client do its own read-modify-write: read the value and version, compute, `cas`, retry on false.

### 5.6 Active expiry (Redis' algorithm)

```java
while (true) {
    List<String> withTtl = ...keys with expiresAt > 0...;
    if (withTtl.isEmpty()) break;
    int sample = Math.min(20, withTtl.size()), expired = 0;
    for (int i = 0; i < sample; i++) { ...pick a random key; if expired, remove it and count... }
    if (expired * 4 <= sample) break;          // ≤ 25% expired: good enough, stop
}
```

(Building `withTtl` is O(n) here for simplicity; Redis keeps a separate dictionary of keys that have a TTL so sampling is O(1).)

### 5.7 Transactions: WATCH + MULTI/EXEC

```java
Transaction watch(String key) { watched.put(key, store.version(key).orElse(-1)); keys.add(key); return this; }

boolean exec() {
    TreeSet<Integer> stripeIds = ...the stripes of every key involved...;
    for (int s : stripeIds) store.locks.get(s).lock();          // ASCENDING order: no deadlock
    try {
        for (var w : watched.entrySet())
            if (currentVersion(w.getKey()) != w.getValue()) return false;   // someone wrote: abort, nothing applied
        for (var op : ops) op.accept(byKey, log);                // apply every queued op
        store.aof.addAll(log);                                   // logged together
        return true;
    } finally { for (int s : stripeIds.descendingSet()) store.locks.get(s).unlock(); }
}
```

| Part | Simple meaning |
|---|---|
| `watch` | remember the version **now** (−1 if absent) |
| `incrBy` / `set` on the transaction | only **queue** the op, don't run it yet |
| locking in ascending stripe order | two transactions on stripes {3, 7} and {7, 3} both lock 3 first, so neither can hold 7 while waiting for 3 |
| the version check | if a watched key changed, abort with nothing applied (optimistic locking) |

### 5.8 AOF replay (Command pattern)

```java
for (Command c : log) switch (c) {
    case SetCmd sc    -> ...put(sc.key(), new Entry(sc.value(), ..., sc.expiresAt(), ...));
    case DelCmd dc    -> ...remove(dc.key());
    case ExpireCmd ec -> ...update the expiry...;
}
```

Every write was recorded as a small command object. Replaying them in order rebuilds the same data, and entries whose absolute expiry has passed are simply expired on the next read.

---

## 6. Dry run (from the real output)

The clock starts at 1,000 ms; 8 stripes, sampled LRU.

| # | Operation | Result |
|---|---|---|
| 1 | `set user:1 asha`, `get` | asha; ttl = −1 (no expiry); ttl of a missing key = −2 |
| 1 | `set session:9 tok PX 5000` | ttl = 5000 |
| 1 | clock + 5000, `get session:9` | empty: expired **exactly** at the deadline (lazy) |
| 1 | `set user:1 x NX` | false (exists) |
| 1 | `set lock:job worker-1 NX PX 30000` | true: a lock |
| 1 | `del user:1`, `scan lock:` | true, `[lock:job]` |
| 2 | `incr views` ×(1, then 10) | 1, then 11 |
| 2 | `incr name` (value "bob") | exception: not an integer |
| 2 | `cas name v bobby`, then `cas name v robert` | true; false (the version moved on) |
| 3 | max 3 keys, noeviction, 4th set | exception (OOM) |
| 3 | max 100, sampled LRU (10 samples): 100 keys, touch the newer 50, add 40 more | 40 evictions, size 100, **48 of 50** hot keys survived |
| 4 | 5,000 keys with TTL 1 s + 1,000 without; clock + 2 s; active cycles | all 5,000 removed by sampling; 1,000 left |

### The transaction step by step (step 5)
| Step | a | b | Note |
|---|---|---|---|
| start | 100 | 0 | |
| tx: watch a, a −30, b +30, exec | 70 | 30 | true |
| stale tx: watch a (version v), queue a −70, b +70 | 70 | 30 | nothing applied yet |
| someone else: `incr a 5` | 75 | 30 | a's version changes |
| stale tx `exec` | 75 | 30 | **false**: watched version changed, nothing applied |

**AOF replay (step 6):** the replayed store dumps `{acct:a=75, acct:b=30, lock:job=worker-1, name=bobby, views=11}`, the same as the original.

**Concurrency (step 7):**
| Test | Result |
|---|---|
| 16 threads × 1,000 `incr counter` | 16000 (no lost updates) |
| 8 threads × 500 CAS retry loops | 4000 |
| 16 threads × 500 random transfers of 7 between 20 balances (multi-stripe transactions) | total stays 20,000, and no deadlock |

---

## 7. Concurrency, explained simply

| Situation | How it's safe |
|---|---|
| two INCRs on one key | same stripe → same lock → one after the other |
| ops on different keys | usually different stripes → parallel |
| CAS from many clients | version check and write inside the stripe lock; losers retry |
| a transaction across stripes | lock all its stripes in **ascending** order (no deadlock), check WATCHes, apply all |
| readers holding an `Entry` while someone writes | `Entry` is immutable; a writer puts a **new** one |

**Why the transfer test conserves money:** each transfer's two increments happen while both stripes are locked, so no other thread sees (or changes) the halfway state.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** expiry exactly at the deadline, NX on an existing key, INCR on a missing key (0) and on a non-number (error), overflow (`addExact`), TTL kept on INCR/CAS, a full store under each policy, WATCH on a missing key (−1), both keys in the same stripe.

**Common mistakes:**
1. One global lock (no parallelism) or a lock object per key (memory).
2. Only lazy expiry, so memory fills with dead keys.
3. Logging relative TTLs in the AOF, so replay extends every expiry.
4. Locking multiple stripes in arbitrary order (deadlock).
5. A true LRU list for millions of keys when sampling works nearly as well.
6. Making `get` bump the version, so CAS fails for no reason.

---

## 9. Interview questions and answers

**Q1. How do you make it thread-safe and still fast?**
> Lock striping: N independent maps, each with its own lock, chosen by key hash. Operations on one key are serialized; different stripes run in parallel. (Redis avoids locks entirely with a single-threaded event loop.)

**Q2. How does TTL work?**
> Store an absolute `expiresAt`. Lazy: every access checks it and deletes expired keys. Active: a periodic cycle samples keys with a TTL and repeats while over 25% are expired, so dead keys are reclaimed with bounded work.

**Q3. What happens when memory is full?**
> A pluggable policy: noeviction rejects new keys; allkeys-lru evicts approximately the least recently used by sampling a few random keys and evicting the oldest.

**Q4. How do CAS and WATCH work?**
> Every write bumps a version. CAS succeeds only if the version still equals what the client read. WATCH records versions; EXEC checks them under the locks and aborts if any changed. Both are optimistic locking.

**Q5. How do transactions avoid deadlocks?**
> They lock every involved stripe in ascending index order (one global order) and unlock in reverse.

**Q6. How do you survive a restart?**
> An append-only log of write commands with absolute expiry times; replay it in order on startup. Production adds periodic snapshots (RDB) and AOF rewriting to compact the log.

### Follow-up questions

**F1. "How do you implement a distributed lock with this?"**
> `SET lock:x <ownerId> NX PX 30000` to acquire; release with a compare-and-delete (only if the value is still your owner id), so you never delete someone else's lock after yours expired. Add fencing tokens for safety.

**F2. "Replication."**
> The primary streams its write commands (the same as the AOF) to replicas, which replay them; reads can go to replicas (eventually consistent). On primary failure, promote a replica (Redis Sentinel).

**F3. "Scale beyond one machine."**
> Shard the keyspace: Redis Cluster uses 16,384 hash slots spread across nodes; clients route by `crc16(key) mod 16384`. Multi-key transactions only work within one slot (hash tags like `{user1}:a`).

**F4. "AOF grows forever."**
> Rewrite it: write the current dataset as a minimal list of SETs to a new file, then switch. Choose an fsync policy: every write (safest), every second (the usual), or let the OS decide.

**F5. "Support lists, hashes and sorted sets."**
> Make `Entry.value` a typed object (string, list, hash, skip-list-backed sorted set) and add type-checked commands; WRONGTYPE errors on mismatches.

**F6. "Why does the CAS retry loop terminate?"**
> Each failed CAS means someone else's write succeeded, so the system as a whole always makes progress (lock-free). A single thread could in theory keep losing; with contention you add backoff.

---

## 10. Running the file

```bash
java KeyValueStore.java      # 26/26 tests passed (16,000 INCRs, 4,000 CAS increments, ~8,000 transfers conserving 20,000)
```

**One-line summary:** *"Keys hash to lock stripes of HashMaps. Each write creates an immutable entry with a new version and an absolute expiry. Expiry is lazy on access plus an active sampling cycle. A Strategy picks eviction (noeviction or Redis-style sampled LRU). CAS and WATCH/EXEC use versions for optimistic locking, and transactions lock stripes in ascending order. Every write is a Command appended to a log that replays into the same dataset."*
