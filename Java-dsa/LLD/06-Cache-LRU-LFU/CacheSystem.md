# 06 · Cache with LRU / LFU / FIFO Eviction (explained simply)

| | |
|---|---|
| **Code** | [CacheSystem.java](CacheSystem.java) |
| **Run** | `java CacheSystem.java` (13 self-checks, including a stampede test and 200,000 concurrent operations) |
| **Design notes** | [README.md](README.md) (more Q&A, Caffeine/Redis comparison) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |
| **The pure data structures** | [DSA M01 LRU Cache](../../DSA/12-Advanced/03-Medium/M01_LRUCache.md), [DSA H01 LFU Cache](../../DSA/12-Advanced/04-Hard/H01_LFUCache.md) |

The DSA versions build the LRU/LFU data structures. This LLD version is the **design around them**: the cache doesn't know **how** victims are chosen (a pluggable eviction policy decides), plus expiry, statistics, removal events, protection against a "stampede", and making it fast under many threads.

---

## 1. The problem in plain words

A **cache** keeps recently or frequently used data in memory so we don't have to fetch it from a slow place (a database, another service) every time. It has a **limited size**, so when it's full and a new item arrives, it must **evict** (throw out) something. Which one?
- **LRU** (Least Recently Used): the item nobody has touched for the longest time.
- **LFU** (Least Frequently Used): the item used the fewest times.
- **FIFO** (First In, First Out): the oldest item, regardless of use.

We also want items to **expire** after a time (TTL), **statistics** (hit rate), a **notification** when items are removed, **read-through loading** ("if it's missing, load it"), and good speed with many threads.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Which eviction policy? | Configurable: LRU, LFU, FIFO, and more later |
| Do entries expire? | Optionally (TTL) |
| Read-through loading? | Yes: `getOrLoad(key, loader)` |
| Many threads? | Yes |
| Statistics? | Hits, misses, evictions, loads |
| Notify on removal? | Yes, with the cause (evicted, expired, explicit, replaced) |
| Distributed cache? | Out of scope (discussed as a follow-up) |

### Step 2 · Requirements
1. `get`, `put`, `remove`, `getOrLoad` in O(1).
2. Never more than `capacity` entries.
3. Pluggable eviction policy.
4. TTL expiry.
5. Stats and removal listeners.
6. **No stampede:** many threads missing the same key at once must trigger only **one** load.
7. Thread-safe and fast.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| the cache | `interface Cache<K, V>` |
| one bounded cache | `class BoundedCache<K, V>` (storage + policy + TTL + stats, one lock) |
| eviction policy | `interface EvictionPolicy<K>` + `LruPolicy`, `LfuPolicy`, `FifoPolicy` |
| an entry | `record Entry<V>(value, expiresAt)` |
| statistics | `record CacheStats(hits, misses, evictions, loads)` |
| why something was removed | `enum RemovalCause` |
| removal notifications | `interface RemovalListener<K, V>` |
| a faster cache for many threads | `class SegmentedCache<K, V>` |
| setup | `class CacheBuilder<K, V>` |

### Step 4 · Relationships

```
CacheBuilder ──builds──> BoundedCache  (or a SegmentedCache of N BoundedCaches)
BoundedCache
   ├── Map<K, Entry<V>> data          (the values)
   ├── EvictionPolicy<K>              (tracks KEYS only, picks the victim)
   ├── RemovalListener<K, V>          (told about removals)
   └── one ReentrantLock
```

**The key separation:** the policy only tracks **keys** and picks a victim; it never sees values. The cache stores values and asks the policy "who goes next?". So a new policy never touches the cache code.

### Step 5 · API

```java
Optional<V> get(K key)
void put(K key, V value)
void remove(K key)
V getOrLoad(K key, Function<K, V> loader)
CacheStats stats()
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how the victim is chosen | **Strategy** (`EvictionPolicy`) |
| creating the right policy from an enum | **Factory** (inside the builder) |
| many optional settings | **Builder** (`CacheBuilder`) |
| reacting to removals | **Observer / listener** (`RemovalListener`) |
| speed with many threads | **lock striping** (`SegmentedCache`) |

### Step 7 · Concurrency
Two things to protect: the cache's internal structures (data map + policy), and the **stampede** (many threads loading the same missing key). Section 7 covers both.

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| generics `<K, V>` | `Cache<K, V>` | works for any key and value types |
| `LinkedHashMap(16, 0.75f, true)` | LRU | a hash map that also keeps entries in **access order**; `get()` moves a key to the end |
| `LinkedHashSet` | FIFO, LFU buckets | a set that remembers insertion order |
| `@FunctionalInterface` | `RemovalListener` | an interface with one method, so a lambda can implement it |
| `ReentrantLock` | one per `BoundedCache` | an explicit lock with `lock()` / `unlock()` |
| `LongAdder` | the stats | a fast counter for many threads |
| `CompletableFuture` + `putIfAbsent` | single-flight loading | "the first thread loads; the others wait for its result" (the idea from [tool 4](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-4--idempotency-keys--a-retry-must-not-charge-twice)) |
| `inFlight.remove(key, mine)` | cleanup | remove only if the value is still **my** future |
| `List<Runnable>` of notifications | listeners | collect the callbacks under the lock, run them after unlocking |
| `h ^= (h >>> 16)` | choosing a segment | mix the high bits of the hash into the low bits (like `HashMap` does) |

---

## 4. The eviction policies, simply

Picture a small bookshelf with room for 3 books.

| Policy | Rule | Analogy |
|---|---|---|
| **LRU** | remove the book you haven't touched for the longest time | the one gathering dust |
| **LFU** | remove the book you've read the fewest times (ties: the oldest of those) | the one you barely use |
| **FIFO** | remove the book that's been on the shelf longest, regardless of use | first in, first out |

**When LRU is weak:** a one-time scan of many items pushes out a genuinely popular item. **When LFU is weak:** an item that was very popular last week stays forever even if nobody wants it now. Real caches (Caffeine's W-TinyLFU) combine both ideas.

---

## 5. The code, piece by piece

### 5.1 The policy interface

```java
interface EvictionPolicy<K> {
    void onInsert(K key);
    void onAccess(K key);
    void onRemove(K key);
    K victim();                                                    // which key to evict next
}
```

The cache tells the policy what happened (insert, access, remove) and asks it for the next victim. Every method is called while the cache holds its lock, so policies don't need any locking of their own.

### 5.2 LRU in a few lines

```java
static class LruPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>(16, 0.75f, true);   // true = access order
    public void onInsert(K k) { order.put(k, Boolean.TRUE); }
    public void onAccess(K k) { order.get(k); }                    // get() moves it to the MRU end
    public void onRemove(K k) { order.remove(k); }
    public K victim() { return order.keySet().iterator().next(); } // eldest = least recently used
}
```

A `LinkedHashMap` with `accessOrder = true` is exactly a hash map plus a doubly linked list ([DSA M01](../../DSA/12-Advanced/03-Medium/M01_LRUCache.md)) in one object. The first key in iteration order is the least recently used.

### 5.3 FIFO

```java
static class FifoPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashSet<K> order = new LinkedHashSet<>();
    public void onInsert(K k) { order.add(k); }
    public void onAccess(K k) { }                                  // reads don't matter
    public void onRemove(K k) { order.remove(k); }
    public K victim() { return order.iterator().next(); }
}
```

### 5.4 LFU in O(1)

```java
static class LfuPolicy<K> implements EvictionPolicy<K> {
    private final Map<K, Integer> freq = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
    private int minFreq = 0;

    public void onInsert(K k) { freq.put(k, 1); bucket(1).add(k); minFreq = 1; }
    public void onAccess(K k) {
        int f = freq.get(k);
        buckets.get(f).remove(k);
        if (f == minFreq && buckets.get(f).isEmpty()) minFreq = f + 1;
        freq.put(k, f + 1);
        bucket(f + 1).add(k);
    }
    public K victim() {
        while (buckets.getOrDefault(minFreq, new LinkedHashSet<>()).isEmpty()) minFreq++;   // after explicit removes
        return buckets.get(minFreq).iterator().next();             // least frequent, oldest among ties
    }
}
```

The same structure as [DSA H01](../../DSA/12-Advanced/04-Hard/H01_LFUCache.md): key → how many times it was used, and use-count → the keys with that count (in order). `minFreq` points at the bucket to evict from.

### 5.5 Get, with lazy expiry

```java
public Optional<V> get(K key) {
    List<Runnable> notifications = new ArrayList<>();
    Optional<V> result;
    lock.lock();
    try {
        Entry<V> e = data.get(key);
        if (e != null && isExpired(e)) {                       // lazy expiry on read
            removeLocked(key, RemovalCause.EXPIRED, notifications);
            e = null;
        }
        if (e == null) { misses.increment(); result = Optional.empty(); }
        else { hits.increment(); policy.onAccess(key); result = Optional.of(e.value()); }
    } finally { lock.unlock(); }
    notifications.forEach(Runnable::run);                      // call listeners OUTSIDE the lock
    return result;
}
```

| Idea | Why |
|---|---|
| expired on read → removed | **lazy expiry**: no background thread needed |
| `policy.onAccess(key)` | a read **changes** the policy's order (LRU moves it, LFU bumps it) |
| listeners run **after** unlocking | a slow or misbehaving listener can't block the whole cache, and can't deadlock by calling back into it |

### 5.6 Put: evict first, then insert

```java
public void put(K key, V value) {
    ...
    lock.lock();
    try {
        long expiresAt = ttlMs > 0 ? clock.getAsLong() + ttlMs : Long.MAX_VALUE;
        Entry<V> old = data.get(key);
        if (old != null) {                                     // update: counts as an access
            data.put(key, new Entry<>(value, expiresAt));
            policy.onAccess(key);
            notifications.add(() -> listener.onRemoval(key, ov, RemovalCause.REPLACED));
        } else {
            // EVICT FIRST, THEN INSERT.
            while (data.size() >= capacity) {
                K victim = policy.victim();
                evictions.increment();
                removeLocked(victim, RemovalCause.EVICTED, notifications);
            }
            data.put(key, new Entry<>(value, expiresAt));
            policy.onInsert(key);
        }
    } finally { lock.unlock(); }
    notifications.forEach(Runnable::run);
}
```

**A real bug this file had while it was being written:** inserting first and evicting after. With LFU, the brand-new key has frequency 1, the lowest, so it was always chosen as the victim immediately, and the cache could never admit anything new. **Evicting first** fixes it.

### 5.7 Read-through with single-flight (no stampede)

```java
public V getOrLoad(K key, Function<K, V> loader) {
    Optional<V> hit = get(key);
    if (hit.isPresent()) return hit.get();
    CompletableFuture<V> mine = new CompletableFuture<>();
    CompletableFuture<V> existing = inFlight.putIfAbsent(key, mine);
    if (existing != null) return existing.join();              // someone is already loading it
    try {
        Optional<V> again = get(key);                          // loaded between our miss and our putIfAbsent?
        V v = again.isPresent() ? again.get() : loader.apply(key);
        if (again.isEmpty()) { loads.increment(); put(key, v); }
        mine.complete(v);
        return v;
    } catch (RuntimeException ex) {
        mine.completeExceptionally(ex);                        // waiters see the failure too
        throw ex;
    } finally {
        inFlight.remove(key, mine);
    }
}
```

**A cache stampede, simply:** a popular key expires. 100 users request it at the same moment, all miss, and all 100 query the database at once. The database falls over. This is also called a "thundering herd".

**Single-flight:** the first thread to miss registers a future ("I'm loading this"). Everyone else who misses finds that future and **waits** for it. Only one database call happens. The double check (`again`) covers the case where someone finished loading between our first miss and our registration.

### 5.8 Segments: lock striping

```java
static class SegmentedCache<K, V> implements Cache<K, V> {
    private BoundedCache<K, V> seg(K key) {
        int h = key.hashCode();
        h ^= (h >>> 16);                                              // spread high bits like HashMap does
        return segments.get(Math.floorMod(h, segments.size()));
    }
    public Optional<V> get(K k) { return seg(k).get(k); }
    public void put(K k, V v) { seg(k).put(k, v); }
}
```

**Why not a read-write lock?** In LRU and LFU, every `get` **changes** the policy (it moves the key or bumps its count), so every read is actually a write; a read lock would be wrong. Instead the cache is split into N independent segments, each with its own lock. A key always goes to the same segment (by hash), and threads on different segments run in parallel. The trade-off: eviction is LRU **within** a segment, so it's only approximately LRU overall. Java 7's `ConcurrentHashMap` and Guava's cache worked this way.

### 5.9 The builder

```java
Cache<String, Integer> c = new CacheBuilder<String, Integer>().capacity(3).policy(Policy.LFU).ttlMs(1000).build();
```

`build()` returns a single `BoundedCache`, or a `SegmentedCache` when `segments > 1`, with the capacity split evenly. The builder's `newPolicy()` is a small factory from the enum to the policy class.

---

## 6. Dry run (from the real output)

### The same access pattern under three policies (capacity 3)
`put A, B, C` → `get A, A, B` → `put D` (one must go).

| Policy | State before D | Victim | Kept |
|---|---|---|---|
| LRU | recency: C (oldest), A, B (newest) | C | **ABD** |
| LFU | counts: A = 3, B = 2, C = 1 | C | **ABD** |
| FIFO | insertion: A, B, C | A | **BCD** |

### Where LRU and LFU differ (capacity 2)
`put hot`, `get hot` × 5, `put x`, `get x`, `put y`:
- LRU: `hot` was touched before `x` → evicts **hot** → keeps `xy`.
- LFU: `hot` used 6 times, `x` twice → evicts **x** → keeps `hot`, `y`.

### TTL and removal causes (TTL 1000 ms, capacity 2)

| Step | Event |
|---|---|
| put s1 at 0 ms; get at 999 ms | still alive |
| get at 1000 ms | **s1: EXPIRED** |
| put k1 = 1, then k1 = 2 | **k1: REPLACED** |
| put k2, put k3 (full) | the LRU key k1 goes: **k1: EVICTED** |
| remove k3 | **k3: EXPLICIT** |

### Stats
20 `getOrLoad` calls cycling through keys 0–4: 5 loads, 15 hits, and **10 misses**. Why 10 and not 5? Each first-time load misses twice: once in the initial `get`, and once in the double check inside single-flight.

### Stampede
64 threads miss the key "config" at once, and the loader sleeps 50 ms → **the loader ran once**, and all 64 got the value.

### Concurrency
16 threads, 200,000 random puts/gets on an 8-segment LRU with capacity 1,000 → the size never exceeded 1,000 and no value was ever corrupted.

---

## 7. Concurrency, explained simply

1. **Internal consistency.** The data map and the policy must change together. Each `BoundedCache` does all of that under one lock, so the policy never disagrees with the data.
2. **Speed.** One lock for the whole cache means every thread waits in one line. Segments give N lines, chosen by key hash.
3. **Stampede.** Single-flight: one loader per missing key; the others wait for its future. If loading fails, waiters see the same failure, and the future is removed so a later call can retry.
4. **Listeners.** They're called **outside** the lock, so slow listener code can't block other cache users.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** capacity ≤ 0 (rejected), null keys or values (rejected), updating an existing key (no eviction; counts as an access), an expired entry being read, an explicit remove followed by LFU eviction (`victim()` skips empty buckets), a failing loader.

**Common mistakes:**
1. **Insert then evict** (LFU evicts the new key every time).
2. A read-write lock for LRU/LFU (reads mutate the order).
3. No stampede protection on `getOrLoad`.
4. Calling listeners while holding the lock.
5. A background thread per entry to expire it.
6. The cache knowing about LRU details directly instead of asking a policy.

---

## 9. Interview questions and answers

**Q1. How do you make eviction pluggable?**
> The cache only stores values and calls an `EvictionPolicy` (onInsert, onAccess, onRemove, victim). LRU, LFU and FIFO each implement it in O(1). A new policy doesn't change the cache.

**Q2. LRU or LFU?**
> LRU adapts quickly to change but a one-off scan can flush popular items. LFU protects popular items but can keep stale ones for too long. Many real caches combine them (W-TinyLFU in Caffeine).

**Q3. How does TTL expiry work here?**
> Each entry stores an absolute `expiresAt`. On a read, an expired entry is removed and treated as a miss (lazy expiry). An optional `purgeExpired()` can be scheduled to clean up entries nobody reads.

**Q4. What is a cache stampede and how do you prevent it?**
> Many requests miss the same key at once and all hit the backend. Single-flight: the first miss registers a future and loads; the others wait on that future. Other techniques: refresh-ahead before expiry, and adding random jitter to TTLs so keys don't all expire at once.

**Q5. Why not a `ReadWriteLock`?**
> In LRU and LFU, a `get` updates the eviction order, so every read is a write. Lock striping (segments) gives parallelism instead.

**Q6. What's the downside of segments?**
> Eviction is per segment, so it's only approximately the global LRU, and capacity is split, so one hot segment can evict while another has free room.

**Q7. Why run listeners outside the lock?**
> A slow listener would block every cache user, and a listener that calls back into the cache while the lock is held could deadlock or see a half-updated state.

### Follow-up questions

**F1. "Make it a distributed cache."**
> Partition keys across nodes with consistent hashing; each node runs this cache. Add replication for availability. Or use Redis/Memcached and keep a small local cache in front.

**F2. "Cache invalidation when the database changes?"**
> Write-through (update the cache when writing the database), or delete the key on write (cache-aside), or listen to change events (CDC) and invalidate. Keep TTLs as a safety net.

**F3. "Limit by memory, not entry count."**
> Give each entry a weight (its approximate size) and evict until the total weight is under the limit.

**F4. "Refresh popular keys before they expire."**
> Refresh-ahead: when an entry is read close to its expiry, reload it in the background while still serving the old value.

**F5. "How does Caffeine avoid locking on reads?"**
> It records reads in lock-free buffers and replays them into the policy in batches, so the hot read path doesn't take a lock.

---

## 10. Running the file

```bash
java CacheSystem.java      # 13/13 tests passed (including a 64-thread stampede and 200k concurrent ops)
```

**One-line summary:** *"The cache stores values and asks a pluggable EvictionPolicy (LRU, LFU, FIFO, each O(1)) for victims, evicting before inserting; entries expire lazily; getOrLoad uses single-flight futures to prevent stampedes; listeners run outside the lock; and segments (lock striping) give parallelism because every LRU/LFU read is really a write."*
