# E02 · Moving Average from Data Stream

| | |
|---|---|
| **LeetCode** | [#346 Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/) (Premium) |
| **Difficulty** | Easy |
| **Pattern** | A fixed-size queue + a running sum (a sliding window over a stream) |
| **Code** | [E02_MovingAverageFromDataStream.java](E02_MovingAverageFromDataStream.java) |
| **Run** | `java E02_MovingAverageFromDataStream.java` |

---

## 1. The problem

`MovingAverage(size)`; `next(val)` returns the average of the **last `size` values** seen so far (or of all of them, if fewer have arrived).

```
size = 3
next(1)  → 1.0                 [1]
next(10) → 5.5                 [1, 10]
next(3)  → 4.667               [1, 10, 3]
next(5)  → 6.0                 [10, 3, 5]   (the 1 left the window)
```

---

## 2. How to think about it

- **Naive:** keep every value and sum the last `size` of them on each call: O(size) per call.
- **Queue + running sum → O(1) per call:**
  - the queue holds exactly the values **inside** the window
  - `sum` = the sum of the queue, updated incrementally (never recomputed)
  - `next(val)`: offer val, `sum += val`; if the queue is bigger than `size`: `sum −= poll()` (the oldest leaves); return `sum / queue.size()`

It's exactly the fixed sliding window from [Arrays E23](../../01-Arrays/02-Easy/E23_MaximumAverageSubarrayI.md), but the data arrives one value at a time.

### The ring-buffer version (no Queue object)
Use an `int[size]` array plus a counter. The slot to write is `count % size`, and **the value being overwritten is exactly the one leaving the window**. Real systems (metrics, audio buffers) do it this way: fixed memory, no allocation per value.

---

## 3. Java concepts used in this problem

### 3.1 `long` for a running sum

Many large values could overflow an `int` sum. `long` is safe (and costs nothing).

### 3.2 `(double) sum / window.size()`

Cast **before** dividing to get a fractional average (see the integer-division trap in [Arrays E23](../../01-Arrays/02-Easy/E23_MaximumAverageSubarrayI.md)).

### 3.3 A ring buffer with `% length`

```java
int i = count % ring.length;      // 0, 1, 2, 0, 1, 2, …   the slot to (over)write
```

The modulo wraps the write position around the fixed array, so the array behaves like a circular queue of capacity `size`. (Built from scratch in [QueueImplementations](../01-Basics/QueueImplementations.java).)

### 3.4 `String.format("%.3f", x)` in the tests

It formats a double with 3 decimal places (`4.667`), so the test can compare an inexact value like 14/3 reliably as text.

---

## 4. Approach 1: Store all values, recompute

```java
static class MovingAverageNaive {
    private final List<Integer> all = new ArrayList<>();
    private final int size;
    MovingAverageNaive(int size) { this.size = size; }
    double next(int val) {
        all.add(val);
        int from = Math.max(0, all.size() - size);
        long s = 0;
        for (int i = from; i < all.size(); i++) s += all.get(i);
        return (double) s / (all.size() - from);
    }
}
```

**O(size) per call** and **unbounded memory** (it keeps every value ever seen).

---

## 5. Approach 2: Queue + running sum (the file's first class)

### Code ([E02_MovingAverageFromDataStream.java:36-49](E02_MovingAverageFromDataStream.java#L36-L49))

```java
static class MovingAverage {
    private final Deque<Integer> window = new ArrayDeque<>();
    private final int size;
    private long sum;

    MovingAverage(int size) { this.size = size; }

    double next(int val) {
        window.offer(val);
        sum += val;
        if (window.size() > size) sum -= window.poll();   // the oldest value leaves
        return (double) sum / window.size();
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `window.offer(val); sum += val;` | the new value enters the window |
| `if (window.size() > size) sum -= window.poll();` | the window is too big → the oldest value leaves (and is subtracted) |
| `return (double) sum / window.size();` | the average of what's inside |

### Dry run: size 3, values 1, 10, 3, 5

| val | window after | sum | average |
|---|---|---|---|
| 1 | [1] | 1 | 1.0 |
| 10 | [1, 10] | 11 | 5.5 |
| 3 | [1, 10, 3] | 14 | 4.667 |
| 5 | [1, 10, 3, 5] → poll 1 → [10, 3, 5] | 18 | **6.0** |

### Complexity
- **O(1) per call**, **space O(size).**

---

## 6. Approach 3: Ring buffer (the file's second class)

### Code ([E02_MovingAverageFromDataStream.java:51-66](E02_MovingAverageFromDataStream.java#L51-L66))

```java
static class MovingAverageRing {
    private final int[] ring;
    private int count;
    private long sum;

    MovingAverageRing(int size) { ring = new int[size]; }

    double next(int val) {
        int i = count % ring.length;
        if (count >= ring.length) sum -= ring[i];          // overwrite = drop the oldest
        ring[i] = val;
        sum += val;
        count++;
        return (double) sum / Math.min(count, ring.length);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = count % ring.length;` | the slot for the new value |
| `if (count >= ring.length) sum -= ring[i];` | once the buffer is full, slot i holds the **oldest** value: subtract it |
| `ring[i] = val; sum += val; count++;` | write the new value |
| `Math.min(count, ring.length)` | how many values are in the window (fewer than `size` at the start) |

### Dry run: size 3, values 1, 10, 3, 5

| val | count before | slot | overwritten | ring | sum | average |
|---|---|---|---|---|---|---|
| 1 | 0 | 0 | — | [1, _, _] | 1 | 1.0 |
| 10 | 1 | 1 | — | [1, 10, _] | 11 | 5.5 |
| 3 | 2 | 2 | — | [1, 10, 3] | 14 | 4.667 |
| 5 | 3 | 0 | **1** | [5, 10, 3] | 18 | 6.0 |

**O(1) per call, no allocation after construction.** The file checks that both versions agree on 1000 values.

---

## 7. Comparing the approaches

| | Naive | Queue + sum | Ring buffer |
|---|---|---|---|
| Time per call | O(size) | **O(1)** | **O(1)** |
| Memory | unbounded | O(size) | O(size), a fixed array |
| Allocation per call | yes (boxing) | yes (boxing) | **none** |

---

## 8. Edge cases and common mistakes

**Edge cases:** fewer than `size` values so far, size = 1, large values (overflow), negative values.

**Mistakes:**
1. Dividing by `size` instead of the current count during warm-up.
2. Integer division (`sum / count` with ints).
3. Recomputing the sum each time (O(size)).
4. In the ring buffer, subtracting the overwritten slot before the buffer is full (those slots hold 0, which is harmless here, but wrong in general).

---

## 9. Interview questions and answers

**Q1. How do you make each call O(1)?**
> Keep a running sum: add the new value, subtract the value that leaves the window. The queue (or ring buffer) tells you which value leaves.

**Q2. Why might a real system prefer the ring buffer?**
> Fixed memory, no per-call allocation or boxing, and cache-friendly. It matters for high-throughput metrics and audio.

### Follow-up questions

**F1. A time-based window (the average over the last 5 minutes).**
> Store (timestamp, value) pairs; on each call, poll while the front is older than now − 5 min, adjusting the sum. See [E03](E03_NumberOfRecentCalls.md).

**F2. A moving median.**
> Sums don't help; use two heaps with lazy deletion ([08-Heap H04](../../08-Heap/04-Hard/H04_SlidingWindowMedian.md)).

**F3. A moving maximum.**
> A monotonic deque ([Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)).

**F4. An exponential moving average (no window).**
> `ema = α · val + (1 − α) · ema`: O(1) memory, no queue at all.

---

## 10. Running the file

```bash
java E02_MovingAverageFromDataStream.java      # 5/5 tests passed
```

**One-line summary:** *"A queue of the window + a running sum (add new, subtract the polled oldest); or a ring buffer where the overwritten slot is the value leaving."*
