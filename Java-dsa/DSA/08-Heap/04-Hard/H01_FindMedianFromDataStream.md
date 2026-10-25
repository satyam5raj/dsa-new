# H01 · Find Median from Data Stream

| | |
|---|---|
| **LeetCode** | [#295 Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) |
| **Difficulty** | Hard |
| **Pattern** | Two heaps: a max-heap for the lower half, a min-heap for the upper half |
| **Code** | [H01_FindMedianFromDataStream.java](H01_FindMedianFromDataStream.java) |
| **Run** | `java H01_FindMedianFromDataStream.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Support `addNum(x)` and `findMedian()` on a growing stream of numbers.

```
add 1, add 2 → median 1.5
add 3        → median 2
```

**Constraints:** up to 5 · 10⁴ calls; values in the int range.

---

## 2. How to think about it

### The options
| | addNum | findMedian |
|---|---|---|
| a sorted list (insert in place) | O(n) | O(1) |
| sort on every query | O(1) | O(n log n) |
| **two heaps** | **O(log n)** | **O(1)** |

### Split the numbers into two halves
- **low** = a **max**-heap holding the smaller half → its root is the biggest of the small numbers
- **high** = a **min**-heap holding the larger half → its root is the smallest of the big numbers

The two roots sit right in the **middle** of the sorted order.

**Invariants**
1. every element of low ≤ every element of high
2. `low.size() == high.size()` or `low.size() == high.size() + 1`

**Median:** odd count → `low.peek()`; even → `(low.peek() + high.peek()) / 2`.

### Add in 3 steps (no if/else on the values)
```
low.offer(x)                  // tentatively into the lower half
high.offer(low.poll())        // the largest of the lower half moves up → invariant 1
if high.size() > low.size():  // rebalance → invariant 2
    low.offer(high.poll())
```

### Dry run: add 5, 15, 1, 3 (from the real run)

| add | low (max-heap) | high (min-heap) | median |
|---|---|---|---|
| 5 | {5} | {} | 5 |
| 15 | {5} | {15} | 10 |
| 1 | {1, 5} | {15} | 5 |
| 3 | {1, 3} | {5, 15} | 4 |

---

## 3. Java concepts used in this problem

### 3.1 Two heaps with opposite orders

```java
PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());  // max-heap
PriorityQueue<Integer> high = new PriorityQueue<>();                          // min-heap
```

### 3.2 Returning a `double` median without overflow

```java
return ((long) low.peek() + high.peek()) / 2.0;
```

`low.peek() + high.peek()` as `int`s overflows when both are near `Integer.MAX_VALUE`. Casting one to `long` makes the addition 64-bit; dividing by `2.0` (a double literal) gives a fractional result like 1.5. Dividing by `2` would do integer division.

### 3.3 Returning an `int` where a `double` is expected

`return low.peek();` in a method returning `double`: the Integer is unboxed and widened to a double automatically.

---

## 4. Approach 1: Keep a sorted list

```java
static class MedianFinderSorted {
    private final List<Integer> sorted = new ArrayList<>();
    void addNum(int num) {
        int pos = Collections.binarySearch(sorted, num);
        if (pos < 0) pos = -pos - 1;                 // the insertion point
        sorted.add(pos, num);                        // O(n) shift
    }
    double findMedian() {
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : ((long) sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
```

`Collections.binarySearch` returns `−(insertionPoint) − 1` when the value is absent, hence `-pos - 1`. Finding the spot is O(log n), but inserting into an ArrayList shifts elements: **O(n)** per add.

---

## 5. Approach 2: Two heaps (the file's solution)

### Code ([H01_FindMedianFromDataStream.java:51-65](H01_FindMedianFromDataStream.java#L51-L65))

```java
static class MedianFinder {
    private final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());  // max-heap
    private final PriorityQueue<Integer> high = new PriorityQueue<>();                          // min-heap

    void addNum(int num) {
        low.offer(num);
        high.offer(low.poll());                         // keep every low <= every high
        if (high.size() > low.size()) low.offer(high.poll());   // low may hold one extra
    }

    double findMedian() {
        if (low.size() > high.size()) return low.peek();
        return ((long) low.peek() + high.peek()) / 2.0; // long: no overflow
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `low.offer(num)` | put it on the low side first |
| `high.offer(low.poll())` | whatever is now the largest in low moves to high, so every low value ≤ every high value |
| the rebalance | if high got bigger, move its smallest back: low has the same size or one more |
| `low.size() > high.size()` | an odd count → the median is low's root |
| else | an even count → the average of the two roots |

### Why the three steps keep invariant 1
After `low.offer(num)`, low's max is the largest value among low ∪ {num}. Moving it to high means high gains something ≥ everything left in low. In the rebalance, moving high's **min** back to low keeps the order too.

### Dry run: add 1, 2, 3

| add | after low.offer | after moving low's max | after rebalance | median |
|---|---|---|---|---|
| 1 | low {1} | low {}, high {1} | low {1}, high {} | 1 |
| 2 | low {2, 1} | low {1}, high {2} | (equal sizes) | (1 + 2)/2 = **1.5** |
| 3 | low {3, 1} | low {1}, high {2, 3} | low {2, 1}, high {3} | **2** |

### Complexity
- **addNum O(log n)** (at most 3 heap operations).
- **findMedian O(1).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Sorted list | Two heaps |
|---|---|---|
| addNum | O(n) | **O(log n)** |
| findMedian | O(1) | O(1) |

The file checks 10,000 medians against sorting, plus the MAX_VALUE overflow case.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single number, duplicates, negative numbers, values near `Integer.MAX_VALUE`.

**Mistakes:**
1. Integer overflow or integer division when averaging.
2. Letting the sizes drift apart by more than 1.
3. Inserting by comparing with only one root, and forgetting to maintain invariant 1.

---

## 8. Interview questions and answers

**Q1. Why one max-heap and one min-heap?**
> The median depends on the largest of the smaller half and the smallest of the larger half. Those are exactly the roots of a max-heap of the low half and a min-heap of the high half.

**Q2. Follow-up: all the numbers are in [0, 100].**
> Keep 101 counters and the total; find the median by walking the counters: O(100) per query, O(1) per add.

**Q3. 99% of the numbers are in [0, 100].**
> Counters for [0, 100], plus two small sorted structures (or counts) for the outliers below and above. The median almost always falls inside the counter range.

### Follow-up questions

**F1. Sliding Window Median.** → [H04](H04_SlidingWindowMedian.md)
> The same two heaps plus deletions, done lazily.

**F2. Return any percentile, not just the median.**
> Keep low at ⌈p · n⌉ elements instead of half.

**F3. Median of two sorted arrays (LC 4).**
> A different technique: binary search on the partition, O(log(min(m, n))).

---

## 9. Running the file

```bash
java H01_FindMedianFromDataStream.java      # 4/4 tests passed (including 10,000 medians vs sorting)
```

**One-line summary:** *"A max-heap for the lower half and a min-heap for the upper half: add via low → high → rebalance, and the median is low's root or the average of both roots."*
