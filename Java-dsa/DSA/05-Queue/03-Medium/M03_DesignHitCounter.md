# M03 · Design Hit Counter

| | |
|---|---|
| **LeetCode** | [#362 Design Hit Counter](https://leetcode.com/problems/design-hit-counter/) (Premium) |
| **Difficulty** | Medium |
| **Pattern** | Queue (a sliding log) → fixed buckets (constant memory) |
| **Code** | [M03_DesignHitCounter.java](M03_DesignHitCounter.java) |
| **Run** | `java M03_DesignHitCounter.java` |

---

## 1. The problem

- `hit(timestamp)` records a hit.
- `getHits(timestamp)` returns the hits in the past **300 seconds**, i.e. in `(timestamp − 300, timestamp]`.

The timestamps are in seconds and **never go backwards**. Several hits can share a second.

```
hit(1), hit(2), hit(3)
getHits(4)   → 3
hit(300)
getHits(300) → 4
getHits(301) → 3     (the hit at second 1 has expired: 301 − 300 = 1 is not > 1)
```

Follow-up: *what if the number of hits per second is huge?*

---

## 2. How to think about it

### Approach 1: a queue of timestamps (like [E03](../02-Easy/E03_NumberOfRecentCalls.md))
`hit`: offer t. `getHits`: drop from the front while `front ≤ t − 300`, then return the size.
**The problem:** 10 million hits in 5 minutes = 10 million queue entries in memory.

### Approach 2: 300 buckets (the follow-up answer)
A hit at second t goes into **bucket `i = t % 300`**. Each bucket stores:
- `times[i]` = the second it currently represents
- `counts[i]` = the hits in that second

```
hit(t):     if times[i] != t, the bucket holds an OLD second (≥ 300 s ago): reset it
            (times[i] = t, counts[i] = 0). Then counts[i]++.
getHits(t): the sum of counts[i] over the buckets with t − times[i] < 300 (still recent)
```

The memory is **fixed** (300 slots) no matter how many hits come in, and `getHits` costs O(300) = O(1).

### The real-world version
This is how metrics systems keep "requests per minute" cheaply, and it's a fixed-window-counter rate limiter. Thread safety would need atomics or locks (see the Multithreading topic).

---

## 3. Java concepts used in this problem

### 3.1 Parallel arrays as buckets

```java
private final int[] times = new int[300], counts = new int[300];
```

Two arrays indexed by the same bucket number act like an array of `(time, count)` records, with no object per bucket.

### 3.2 `t % 300` maps time onto a circular wheel

Seconds 5, 305, 605… all map to bucket 5. Because only the last 300 seconds matter, at most **one** of those seconds can still be relevant at any moment. Storing `times[i]` tells us whether the bucket's count belongs to the current second or is stale. This is the "**timing wheel**" idea, used in schedulers and network timers.

### 3.3 Comparing the two versions in the test

20,000 random operations (many hits per second, plus occasional time jumps) confirm that the bucket version and the queue version always agree.

---

## 4. Approach 1: Queue of timestamps (the file's first class)

### Code ([M03_DesignHitCounter.java:37-46](M03_DesignHitCounter.java#L37-L46))

```java
static class HitCounterQueue {
    private final Deque<Integer> hits = new ArrayDeque<>();

    void hit(int t) { hits.offer(t); }

    int getHits(int t) {
        while (!hits.isEmpty() && hits.peek() <= t - 300) hits.poll();
        return hits.size();
    }
}
```

| Line | What it does |
|---|---|
| `hit` | append the timestamp (the timestamps are non-decreasing, so the queue is sorted) |
| `while (... hits.peek() <= t - 300) hits.poll();` | drop the hits outside `(t − 300, t]` |
| `return hits.size();` | the remaining hits are recent |

**Amortised O(1) per call, but O(number of recent hits) memory.**

### Dry run
`hit 1, 2, 3` → [1, 2, 3]; `getHits(4)`: 1 ≤ −296? no → **3**. `hit 300` → [1, 2, 3, 300]; `getHits(300)`: 1 ≤ 0? no → **4**. `getHits(301)`: 1 ≤ 1 → poll; 2 ≤ 1? no → **3** ✓

---

## 5. Approach 2: 300 buckets (the file's main class)

### Code ([M03_DesignHitCounter.java:48-67](M03_DesignHitCounter.java#L48-L67))

```java
static class HitCounter {
    private final int[] times = new int[300], counts = new int[300];

    void hit(int t) {
        int i = t % 300;
        if (times[i] != t) {          // this bucket still holds an expired second
            times[i] = t;
            counts[i] = 0;
        }
        counts[i]++;
    }

    int getHits(int t) {
        int total = 0;
        for (int i = 0; i < 300; i++) {
            if (t - times[i] < 300) total += counts[i];
        }
        return total;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = t % 300;` | this second's bucket |
| `if (times[i] != t) { times[i] = t; counts[i] = 0; }` | the bucket was last used for a different (necessarily older, by ≥ 300 s) second → recycle it |
| `counts[i]++;` | one more hit in this second |
| `if (t - times[i] < 300) total += counts[i];` | count only the buckets whose second is inside the window |

### Dry run

| operation | bucket | times[i] → | counts[i] → | result |
|---|---|---|---|---|
| hit(1) | 1 | 1 | 1 | |
| hit(2) | 2 | 2 | 1 | |
| hit(3) | 3 | 3 | 1 | |
| getHits(4) | buckets 1, 2, 3 are recent (4 − 1 = 3 < 300) | | | **3** |
| hit(300) | 0 | 300 | 1 | |
| getHits(300) | buckets 0, 1, 2, 3 are recent | | | **4** |
| getHits(301) | bucket 1: 301 − 1 = 300, **not** < 300 → excluded | | | **3** ✓ |

(The untouched buckets have times 0 and counts 0, so they add nothing.)

### Complexity
- `hit` **O(1)**; `getHits` **O(300)** = O(1); **space O(300)**, independent of the traffic.

---

## 6. Comparing the approaches

| | Queue (sliding log) | 300 buckets |
|---|---|---|
| hit | O(1) | O(1) |
| getHits | amortised O(1) | O(300) |
| Memory | **O(hits in the window)**, unbounded | **O(300)**, fixed |
| Precision | exact per hit | per second (fine here) |

---

## 7. Edge cases and common mistakes

**Edge cases:** many hits in the same second, long silent gaps (every bucket is stale), a hit exactly 300 s ago (excluded).

**Mistakes:**
1. Not resetting a stale bucket on reuse (old counts would be added to the new second).
2. Using `≤ 300` instead of `< 300` (the window is `(t − 300, t]`).
3. In the queue version, storing one entry per hit when the hits per second are huge (that's the memory blow-up the buckets fix).

---

## 8. Interview questions and answers

**Q1. Why do buckets need a timestamp, not just a count?**
> Bucket i is shared by the seconds i, i + 300, i + 600… The timestamp says which second the count belongs to, so a stale count can be recognised and reset.

**Q2. How would you make it thread-safe?**
> Use `AtomicIntegerArray` with a compare-and-set reset per bucket, or a lock per bucket (striping), or a single lock if the contention is low.

**Q3. Bigger windows or finer granularity?**
> Use more buckets (e.g. 3600 for an hour at 1 s), or a hierarchy of buckets (seconds → minutes) to trade precision for memory.

### Follow-up questions

**F1. Rate limiting with a token bucket.**
> Each client has tokens refilled at a rate r, up to a capacity b; a request consumes a token. It needs O(1) state per client: (tokens, the last refill time).

**F2. A sliding window counter (an approximation of a sliding window with two fixed windows).**
> count = current window + previous window × (overlap fraction). It needs O(1) state and is used by many API gateways.

**F3. Logger Rate Limiter.** → [12-Advanced E01](../../12-Advanced/02-Easy/E01_LoggerRateLimiter.md)

---

## 9. Running the file

```bash
java M03_DesignHitCounter.java      # 4/4 tests passed (including 20,000 random ops: buckets == queue)
```

**One-line summary:** *"Queue: offer the hits, expire ≤ t − 300. Buckets: t % 300 with (time, count), reset stale buckets, sum the recent ones: O(300) memory."*
