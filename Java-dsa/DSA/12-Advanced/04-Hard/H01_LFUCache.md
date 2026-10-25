# H01 · LFU Cache ⭐

| | |
|---|---|
| **LeetCode** | [#460 LFU Cache](https://leetcode.com/problems/lfu-cache/) |
| **Difficulty** | Hard |
| **Pattern** | count → `LinkedHashSet` of keys (LRU order inside each bucket) + a `minFreq` pointer |
| **Code** | [H01_LFUCache.java](H01_LFUCache.java) |
| **Run** | `java H01_LFUCache.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem); the LRU idea is in [M01](../03-Medium/M01_LRUCache.md).

---

## 1. The problem

Design `LFUCache(capacity)` with `get(key)` and `put(key, value)`, both **O(1)**. When the cache is full, evict the key with the **lowest use count**; if several keys tie, evict the **least recently used** of them.

- `get` and `put` on an existing key both count as a use.
- A new key starts with count 1.

```
capacity 2:
put(1,1)  put(2,2)  get(1)=1
put(3,3)   → evicts 2 (count 1, versus key 1's count 2)
get(2)=-1  get(3)=3
put(4,4)   → keys 1 and 3 both have count 2; evict 1, which was used longer ago
get(1)=-1  get(3)=3  get(4)=4
```

---

## 2. How to think about it

### Why not a heap?
A heap ordered by (count, last used) works, but every update is O(log n). We need O(1).

### Buckets by count
```
values : key → value
counts : key → its use count
buckets: count → LinkedHashSet of the keys with that count, in USE order
         (a LinkedHashSet iterates in insertion order, so the first key is the least recent)
minFreq: the smallest count that currently has keys
```

**touch(key)**, on every use of an existing key:
```
c = counts[key]
move key from buckets[c] to the END of buckets[c + 1]
if buckets[c] is now empty and c == minFreq:  minFreq++
```

**evict:** the **first** key of `buckets[minFreq]`: the lowest count, and least recent among those.

**insert a new key:** count 1, add it to `buckets[1]`, and set `minFreq = 1` (a new key is always the minimum).

### Why is `minFreq++` enough after a touch?
The touched key moved from c to c + 1. If bucket c (the minimum) just emptied, bucket c + 1 now holds at least that key, so c + 1 is the new minimum. No searching is needed.

### Dry run: the example
Buckets are shown as count: [keys, least recent first].

| op | action | buckets | minFreq | output |
|---|---|---|---|---|
| put(1,1) | new | 1: [1] | 1 | |
| put(2,2) | new | 1: [1, 2] | 1 | |
| get(1) | touch 1: count 1 → 2 | 1: [2], 2: [1] | 1 | 1 |
| put(3,3) | full → evict first of bucket 1 = **2**; insert 3 | 1: [3], 2: [1] | 1 | |
| get(2) | missing | | | −1 |
| get(3) | touch 3: bucket 1 empties and 1 == minFreq | 1: [], 2: [1, 3] | 2 | 3 |
| put(4,4) | full → evict first of bucket 2 = **1**; insert 4 | 1: [4], 2: [3] | 1 | |
| get(1) | missing | | | −1 |
| get(3) | touch 3: count 2 → 3 (2 ≠ minFreq, so no change) | 1: [4], 2: [], 3: [3] | 1 | 3 |
| get(4) | touch 4: bucket 1 empties | 2: [4], 3: [3] | 2 | 4 |

Output: **"1 -1 3 -1 3 4"** ✓

---

## 3. Java concepts used in this problem

### 3.1 `LinkedHashSet`: a set that remembers insertion order

```java
LinkedHashSet<Integer> bucket = new LinkedHashSet<>();
bucket.add(key);                          // O(1), goes to the end
bucket.remove(key);                       // O(1), from anywhere
int oldest = bucket.iterator().next();    // O(1), the first inserted
```

It's a `HashSet` plus a doubly linked list through its entries, so it has O(1) add, remove and contains, **and** a predictable order. Removing a key and adding it again moves it to the end, which is exactly "mark it most recently used". It plays the role of the hand-built list in [M01](../03-Medium/M01_LRUCache.md).

### 3.2 Getting the first element: `iterator().next()`

Sets have no `get(0)`. `iterator()` returns an `Iterator`, and its first `next()` is the first element in iteration order.

### 3.3 Several maps declared in one line

```java
private final Map<Integer, Integer> values = new HashMap<>(), counts = new HashMap<>();
```

Two separate fields of the same type. Each gets its own `new HashMap<>()`.

### 3.4 The capacity 0 guard

`if (capacity == 0) return;` in `put`. Without it, the eviction code would look up `buckets.get(minFreq)` while the cache is empty and get `null`.

---

## 4. Approach 1: Scan for the victim on eviction (the referee)

### Code ([H01_LFUCache.java:84-105](H01_LFUCache.java#L84-L105))

```java
static class Brute {
    final int cap;
    final Map<Integer, int[]> m = new HashMap<>();      // key -> {value, count, lastUsed}
    int clock = 0;
    Brute(int cap) { this.cap = cap; }
    int get(int k) { int[] e = m.get(k); if (e == null) return -1; e[1]++; e[2] = clock++; return e[0]; }
    void put(int k, int v) {
        if (cap == 0) return;
        int[] e = m.get(k);
        if (e != null) { e[0] = v; e[1]++; e[2] = clock++; return; }
        if (m.size() == cap) {
            int victim = -1;
            for (Map.Entry<Integer, int[]> x : m.entrySet()) {
                if (victim == -1) { victim = x.getKey(); continue; }
                int[] a = x.getValue(), b = m.get(victim);
                if (a[1] < b[1] || (a[1] == b[1] && a[2] < b[2])) victim = x.getKey();
            }
            m.remove(victim);
        }
        m.put(k, new int[]{v, 1, clock++});
    }
}
```

| Line | What it does |
|---|---|
| `{value, count, lastUsed}` | everything needed to rank a key |
| `get` / update | bump the count and stamp the time |
| the scan | the smallest count, then the oldest `lastUsed` |

**get O(1), put O(n)** when evicting. It follows the rules literally, so it's a reliable referee. (`victim == -1` works as "none yet" because the test's keys are never negative.)

---

## 5. Approach 2: Count buckets + minFreq (the file's main solution)

### Code ([H01_LFUCache.java:41-81](H01_LFUCache.java#L41-L81))

```java
static class LFUCache {
    private final int capacity;
    private final Map<Integer, Integer> values = new HashMap<>(), counts = new HashMap<>();
    private final Map<Integer, LinkedHashSet<Integer>> buckets = new HashMap<>();
    private int minFreq = 0;

    public LFUCache(int capacity) { this.capacity = capacity; }

    public int get(int key) {
        if (!values.containsKey(key)) return -1;
        touch(key);
        return values.get(key);
    }

    public void put(int key, int value) {
        if (capacity == 0) return;
        if (values.containsKey(key)) {
            values.put(key, value);
            touch(key);
            return;
        }
        if (values.size() == capacity) {
            int victim = buckets.get(minFreq).iterator().next();      // lowest count, least recent
            buckets.get(minFreq).remove(victim);
            values.remove(victim);
            counts.remove(victim);
        }
        values.put(key, value);
        counts.put(key, 1);
        buckets.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    private void touch(int key) {
        int c = counts.get(key);
        counts.put(key, c + 1);
        buckets.get(c).remove(key);
        if (c == minFreq && buckets.get(c).isEmpty()) minFreq++;
        buckets.computeIfAbsent(c + 1, k -> new LinkedHashSet<>()).add(key);   // most recent in its new bucket
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `get` | a missing key returns −1 without counting as a use |
| `put`, existing key | update the value and count it as a use |
| `put`, full | the victim is the first key of the minimum bucket; remove it from all three maps |
| `put`, new key | count 1, the end of bucket 1, and `minFreq = 1` |
| `touch` | count + 1 and move the key to the end of the next bucket |
| the `minFreq++` line | only when the minimum bucket just emptied |

Empty buckets stay in the `buckets` map, which is harmless: `minFreq` never points at one when an eviction happens.

### Complexity
- **O(1)** per `get` and `put` (on average, for the hash operations).
- **Space O(capacity)**, plus at most a few empty buckets.

---

## 6. Comparing the approaches

| | Scan on eviction | Buckets + minFreq |
|---|---|---|
| get | O(1) | O(1) |
| put (evicting) | O(n) | **O(1)** |
| Tie-breaking | a timestamp | the `LinkedHashSet` order |

The file runs 200,000 random operations (across 100 caches, including capacity 0) against the scanning version.

---

## 7. Edge cases and common mistakes

**Edge cases:** capacity 0, capacity 1, updating an existing key when full (no eviction), many keys with the same count.

**Mistakes:**
1. Forgetting to reset `minFreq = 1` on an insert.
2. Evicting **after** inserting the new key (then the new key, with count 1, gets evicted immediately).
3. Not counting `put` on an existing key as a use.
4. Breaking ties by insertion time instead of last use time.
5. No capacity 0 guard.

---

## 8. Interview questions and answers

**Q1. How is LFU different from LRU?**
> LRU evicts the key unused for the longest time. LFU evicts the key used the fewest times, and uses recency only to break ties.

**Q2. Why does `minFreq` never need a search?**
> It only changes in two ways: an insert sets it to 1 (the smallest possible), and a touch can empty the minimum bucket, in which case the touched key is now in bucket minFreq + 1.

**Q3. Why a `LinkedHashSet` per bucket?**
> It gives O(1) removal from the middle (a touched key) and O(1) access to the oldest key (the eviction victim), keeping LRU order inside each count.

### Follow-up questions

**F1. Implement each bucket as a hand-built doubly linked list.**
> Exactly the LRU list from [M01](../03-Medium/M01_LRUCache.md), one per count, with nodes that also store their count. That's the version without library help.

**F2. Old popular keys never get evicted (cache pollution).**
> Real systems age the counts (halve them periodically) or use policies like W-TinyLFU (Caffeine), which combine frequency sketches with an LRU window.

**F3. All O(1) Data Structure.** → [H03](H03_AllOneDataStructure.md)
> The same "buckets of keys by count" idea, kept as a sorted linked list so that both the min and the max are O(1).

---

## 9. Running the file

```bash
java H01_LFUCache.java      # 3/3 tests passed (example, capacity 0, 200,000 random ops vs scanning eviction)
```

**One-line summary:** *"Group keys into count buckets that are LinkedHashSets (least recent first), track minFreq, move a key to the next bucket on every use, and evict the first key of the minFreq bucket."*
