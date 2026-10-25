# E03 · Number of Recent Calls ⭐

| | |
|---|---|
| **LeetCode** | [#933 Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/) |
| **Difficulty** | Easy |
| **Pattern** | A sliding time window (expire from the front) |
| **Code** | [E03_NumberOfRecentCalls.java](E03_NumberOfRecentCalls.java) |
| **Run** | `java E03_NumberOfRecentCalls.java` |

⭐ = a bonus classic.

---

## 1. The problem

`ping(t)` records a request at time t (t **strictly increases** between calls) and returns how many requests happened in the last 3000 ms, i.e. in the range **[t − 3000, t]** (inclusive).

| call | window | answer |
|---|---|---|
| ping(1) | [−2999, 1] | 1 |
| ping(100) | [−2900, 100] | 2 |
| ping(3001) | [1, 3001] | 3 (1 is still inside) |
| ping(3002) | [2, 3002] | 3 (1 has expired) |
| ping(10000) | [7000, 10000] | 1 |

---

## 2. How to think about it

### Why a queue?
The times arrive in **increasing** order, so the **oldest** requests are always at the **front**. Requests fall out of the window in exactly the order they came in: first in, first out.

```
ping(t):
    offer t
    while front < t − 3000: poll       (expire the requests that are too old)
    return size
```

### The real-world version
This is the "sliding log" **rate limiter**: count the requests in the last window and reject when the count is over the limit. ([M03 Design Hit Counter](../03-Medium/M03_DesignHitCounter.md) is the memory-bounded version.)

---

## 3. Java concepts used in this problem

### 3.1 Expiring from the front with `peek` + `poll`

```java
while (requests.peek() < t - 3000) requests.poll();
```

`peek()` reads the front without removing it. It's safe here because we've just offered t, so the queue is never empty during the loop (t itself is never expired).

### 3.2 Amortised O(1)

A single ping may expire many old requests at once, but each request is offered once and polled at most once, so the total work over all calls is O(number of pings). That's amortised O(1) per call.

### 3.3 Monotonic input → a sorted queue for free

Because t only increases, the queue is always sorted, which is why we only need to check the front. (With out-of-order timestamps you'd need a TreeMap or a heap.)

---

## 4. Approach 1: Keep all the times, count with binary search

```java
static class RecentCounterBinary {
    private final List<Integer> times = new ArrayList<>();
    int ping(int t) {
        times.add(t);                                       // sorted, since t increases
        int lo = 0, hi = times.size() - 1, from = times.size() - 1;
        while (lo <= hi) {                                  // the first index with time >= t − 3000
            int mid = (lo + hi) >>> 1;
            if (times.get(mid) >= t - 3000) { from = mid; hi = mid - 1; } else lo = mid + 1;
        }
        return times.size() - from;
    }
}
```

**O(log n) per call**, but the memory grows forever. The queue version forgets the old requests.

---

## 5. Approach 2: A queue with expiry (the file's solution)

### Code ([E03_NumberOfRecentCalls.java:37-45](E03_NumberOfRecentCalls.java#L37-L45))

```java
static class RecentCounter {
    private final Deque<Integer> requests = new ArrayDeque<>();

    int ping(int t) {
        requests.offer(t);
        while (requests.peek() < t - 3000) requests.poll();   // drop expired requests
        return requests.size();
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `requests.offer(t);` | record the new request at the back |
| `while (requests.peek() < t - 3000) requests.poll();` | the front is the oldest; drop it while it's outside `[t − 3000, t]` |
| `return requests.size();` | everything left is inside the window |

### Dry run

| ping | queue after offer | expire (front < t − 3000?) | queue after | returns |
|---|---|---|---|---|
| 1 | [1] | 1 < −2999? no | [1] | 1 |
| 100 | [1, 100] | 1 < −2900? no | [1, 100] | 2 |
| 3001 | [1, 100, 3001] | 1 < 1? **no** (1 is inclusive) | [1, 100, 3001] | 3 |
| 3002 | [1, 100, 3001, 3002] | 1 < 2? **yes** → poll; 100 < 2? no | [100, 3001, 3002] | 3 |
| 10000 | [100, 3001, 3002, 10000] | 100, 3001, 3002 < 7000 → poll all three | [10000] | 1 |

### Complexity
- **Amortised O(1) per ping**, **space O(the number of requests in one window)** (at most 3001, since t strictly increases).

---

## 6. Comparing the approaches

| | Binary search over all times | Queue with expiry |
|---|---|---|
| Time per call | O(log n) | **O(1) amortised** |
| Memory | grows forever | **O(window)** |

---

## 7. Edge cases and common mistakes

**Edge cases:** a request exactly 3000 ms old (included), big jumps in time (many expiries at once), the first call.

**Mistakes:**
1. Using `<=` instead of `<` (it drops a request exactly at t − 3000, which should be included).
2. Expiring **before** adding t (it works too, but then the queue can be empty while you peek).
3. Using a stack (wrong order: the oldest requests must leave first).

---

## 8. Interview questions and answers

**Q1. Why is a queue the right structure?**
> The requests arrive in time order and expire in the same order. Expiring always happens at the front, and new requests join at the back: that's FIFO.

**Q2. How does this relate to rate limiting?**
> It's the "sliding log" algorithm: keep the timestamps of recent requests and allow a request only if the count in the window is below the limit. Its memory is proportional to the number of requests per window.

### Follow-up questions

**F1. Design Hit Counter (LC 362) with bounded memory.** → [M03](../03-Medium/M03_DesignHitCounter.md)
> Buckets: an array of 300 slots (one per second), storing (timestamp, count) per slot.

**F2. Out-of-order timestamps.**
> Use a TreeMap of timestamp → count, and `headMap`/`tailMap` to count and to prune.

**F3. A rate limiter with N requests per minute, across many users.**
> A map of user → queue (the sliding log), or a token bucket per user (O(1) memory per user).

**F4. Logger Rate Limiter (LC 359).** → [12-Advanced E01](../../12-Advanced/02-Easy/E01_LoggerRateLimiter.md)

---

## 9. Running the file

```bash
java E03_NumberOfRecentCalls.java      # 5/5 tests passed
```

**One-line summary:** *"Offer t, poll from the front while it's older than t − 3000, return the size."*
