# A02 · Stock Price Fluctuation ⭐

| | |
|---|---|
| **LeetCode** | [#2034 Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/) |
| **Difficulty** | Medium |
| **Mix of structures** | **HashMap** (timestamp → price) + **two heaps** with **lazy deletion** (or a TreeMap of counts) |
| **Code** | [A02_StockPriceFluctuation.java](A02_StockPriceFluctuation.java) |
| **Run** | `java A02_StockPriceFluctuation.java` |

⭐ = not in `leetcode.md`, added because it needs two data structures working together.

---

## 1. The problem

A stream of `(timestamp, price)` records arrives **out of order**. A later record for a timestamp you've already seen **corrects** it.

- `update(timestamp, price)`: record, or correct, the price at that time.
- `current()`: the price at the **latest** timestamp.
- `maximum()` / `minimum()`: the highest and lowest of the **current** (corrected) prices.

```
update(1,10)  update(2,5)
current()  → 5        (latest timestamp is 2)
maximum()  → 10
update(1,3)           (the price at time 1 was really 3)
maximum()  → 5        (10 no longer exists)
update(4,2)
minimum()  → 2
```

---

## 2. How to think about it

### What's hard
`current()` is easy: a HashMap from timestamp to price, plus the largest timestamp seen.

The hard part is the **correction**. When time 1 changes from 10 to 3, the 10 must stop counting for `maximum()`. A heap gives the maximum in O(1), but **removing an arbitrary element** (the old 10) costs O(n).

### Idea 1: a TreeMap of counts
`countOfPrice: price → how many timestamps currently have this price`. A correction decrements the old price and increments the new one, both in O(log n). `maximum()` is `lastKey()` and `minimum()` is `firstKey()`. That's clean and simple.

### Idea 2: lazy deletion (the main solution)
**Don't remove old entries at all.** Push `(price, timestamp)` into a max-heap and a min-heap on every update. When someone asks for the maximum, look at the top entry and check it against the HashMap, the **source of truth**:

```
while priceAt[top.timestamp] != top.price:    // that timestamp was corrected later
    throw the top away
return top.price
```

An entry is either still true, so it's a valid answer, or stale, so we discard it. Each pushed entry is discarded **at most once**, so over the whole run the extra work is at most the number of updates.

**The analogy:** price tags in a shop. When the price changes, you stick a new tag on top instead of peeling the old one off. When you read a tag, you check it against the till's database; if it doesn't match, you peel it off and look at the next one.

### Dry run: the max-heap only
| Step | max-heap (top first) | `priceAt` | answer |
|---|---|---|---|
| `update(1,10)`, `update(2,5)` | (10,t1) (5,t2) | {1:10, 2:5} | |
| `maximum()` | top (10,t1): `priceAt[1]` = 10 ✓ | | **10** |
| `update(1,3)` | (10,t1) (5,t2) (3,t1) | {1:3, 2:5} | |
| `maximum()` | top (10,t1): `priceAt[1]` = 3 ≠ 10 → **poll**; top (5,t2): `priceAt[2]` = 5 ✓ | | **5** |

---

## 3. Java concepts used in this problem

### 3.1 `PriorityQueue<int[]>` with a comparator
```java
new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // max-heap by a[0]
```
The heap stores small arrays `{price, timestamp}`. The comparator decides the order: `compare(b, a)` (b first) reverses it into a **max-heap**. Use `Integer.compare` rather than `b[0] - a[0]`, because subtraction can overflow for large or negative values.

### 3.2 `Map.put` returns the previous value
```java
Integer old = priceAt.put(timestamp, price);     // null if the timestamp is new
```
One call both writes the new price and tells you what it replaced. The TreeMap approach uses this to know which count to decrement.

### 3.3 `Integer` vs `int` with `!=`: a classic trap
```java
while (priceAt.get(heap.peek()[1]) != heap.peek()[0]) heap.poll();
```
`priceAt.get(...)` returns an `Integer` (an object); `heap.peek()[0]` is an `int`. When one side is an `int`, Java **unboxes** the `Integer` and compares the numbers, which is correct. But if **both** sides were `Integer`, `!=` would compare **object identity**, and it would fail for values outside −128…127 (the Integer cache). With two `Integer`s, use `.equals`.

### 3.4 `Collections.max` / `Collections.min`
They scan a collection once (O(n)) to find the largest or smallest element. The referee uses them.

---

## 4. Approach 1: Scan every price (the referee)

### Code ([A02_StockPriceFluctuation.java:44-51](A02_StockPriceFluctuation.java#L44-L51))
```java
static class ScanStockPrice {
    private final Map<Integer, Integer> priceAt = new HashMap<>();
    private int latest = 0;
    public void update(int timestamp, int price) { priceAt.put(timestamp, price); latest = Math.max(latest, timestamp); }
    public int current() { return priceAt.get(latest); }
    public int maximum() { return Collections.max(priceAt.values()); }
    public int minimum() { return Collections.min(priceAt.values()); }
}
```

`put` overwrites a corrected timestamp, so the map always holds the true current prices. `maximum` and `minimum` scan all of them: **O(n) per query**.

---

## 5. Approach 2: HashMap + TreeMap of price counts

### Code ([A02_StockPriceFluctuation.java:54-68](A02_StockPriceFluctuation.java#L54-L68))
```java
static class TreeMapStockPrice {
    private final Map<Integer, Integer> priceAt = new HashMap<>();
    private final TreeMap<Integer, Integer> countOfPrice = new TreeMap<>();
    private int latest = 0;

    public void update(int timestamp, int price) {
        Integer old = priceAt.put(timestamp, price);                 // put returns the previous value (or null)
        if (old != null && countOfPrice.merge(old, -1, Integer::sum) == 0) countOfPrice.remove(old);
        countOfPrice.merge(price, 1, Integer::sum);
        latest = Math.max(latest, timestamp);
    }
    public int current() { return priceAt.get(latest); }
    public int maximum() { return countOfPrice.lastKey(); }
    public int minimum() { return countOfPrice.firstKey(); }
}
```

| Line | What it does |
|---|---|
| `old = priceAt.put(...)` | store the new price, and get the price it replaced |
| `merge(old, -1, ...) == 0 → remove` | one fewer timestamp at the old price; delete the key at zero |
| `merge(price, 1, ...)` | one more at the new price |
| `lastKey` / `firstKey` | the largest and smallest price that still exists |

Counts (not a `TreeSet` of prices) are needed because two timestamps can share a price: if times 1 and 2 are both 10 and time 1 changes, 10 must stay.

**Cost:** O(log n) for everything.

---

## 6. Approach 3: HashMap + two heaps with lazy deletion (main solution)

### Code ([A02_StockPriceFluctuation.java:71-92](A02_StockPriceFluctuation.java#L71-L92))
```java
static class StockPrice {
    private final Map<Integer, Integer> priceAt = new HashMap<>();
    private int latest = 0;
    private final PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // {price, timestamp}
    private final PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));

    public void update(int timestamp, int price) {
        priceAt.put(timestamp, price);
        latest = Math.max(latest, timestamp);
        maxHeap.add(new int[]{price, timestamp});
        minHeap.add(new int[]{price, timestamp});
    }

    public int current() { return priceAt.get(latest); }
    public int maximum() { return validTop(maxHeap); }
    public int minimum() { return validTop(minHeap); }

    private int validTop(PriorityQueue<int[]> heap) {
        while (priceAt.get(heap.peek()[1]) != heap.peek()[0]) heap.poll();   // stale: that timestamp was corrected
        return heap.peek()[0];
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| `update`: `priceAt.put` | the HashMap is always correct |
| `update`: two `add`s | push the new record into both heaps; **old records stay** |
| `validTop`: the `while` | discard tops that no longer match the HashMap |
| `return heap.peek()[0]` | the first true entry is the answer |

### Why the loop always stops
For every timestamp, the record holding its **current** price is somewhere in the heap, so a valid entry always exists. The loop only discards stale ones and stops at the first valid one.

### Complexity
- `update`: **O(log n)** (two heap pushes).
- `maximum` / `minimum`: **amortised O(log n)**. A single call can discard many entries, but each entry is discarded at most once overall.
- Space: **O(number of updates)**, because stale entries stay until they reach the top. That's the price of laziness.

---

## 7. Comparing the approaches

| | update | max / min | Memory |
|---|---|---|---|
| Scan | O(1) | O(n) | O(timestamps) |
| TreeMap of counts | O(log n) | O(log n) | O(timestamps) |
| **Heaps + lazy deletion** | O(log n) | amortised O(log n) | O(updates) |

Both the TreeMap and the heaps are good interview answers. Lazy deletion is the more general **technique**: it's how you "delete" from any heap ([Sliding Window Median](../../08-Heap/04-Hard/H04_SlidingWindowMedian.md) and the Skyline problem use it too). The file runs 60,000 random updates, with many corrections, against the scan.

---

## 8. Edge cases and common mistakes

**Edge cases:** correcting the latest timestamp (`current()` must change), correcting a price to the same value, many timestamps sharing one price, a correction that removes the current maximum.

**Mistakes:**
1. Removing the old price with `heap.remove(...)`: correct, but O(n).
2. A `TreeSet<Integer>` of prices: two timestamps with the same price collapse into one.
3. Tracking `current` as "the last update" instead of "the price at the **largest timestamp**" (updates arrive out of order).
4. Comparing two `Integer`s with `!=`.
5. Checking staleness by price alone instead of by `(price, timestamp)`.

---

## 9. Interview questions and answers

**Q1. What is lazy deletion, and why is it valid here?**
> Instead of deleting outdated heap entries, leave them and discard them when they reach the top, by checking them against the HashMap. It's valid because the HashMap always knows the true price for each timestamp, so we can always tell whether an entry is stale.

**Q2. What's the amortised cost?**
> Each update pushes one entry per heap, and each entry is popped at most once. So all the pops together cost O(total updates · log n), which is O(log n) per operation on average.

**Q3. TreeMap or heaps?**
> The TreeMap uses less memory, since there are no stale entries, and is simpler to reason about. The heaps do slightly less work per update when there are few corrections. Either is fine; mention the trade-off.

### Follow-up questions

**F1. "Max price within a time window [t1, t2]."**
> Build a segment tree over timestamps (or over compressed timestamps): point update, range-max query, O(log n) each.

**F2. "Memory keeps growing with lazy deletion."**
> When a heap gets much bigger than the number of live timestamps (say twice as big), rebuild it from the HashMap in O(n).

**F3. "Average price too."**
> Keep a running sum of the current prices: a correction adds `new − old`, and the average is sum / number of timestamps.

---

## 10. Running the file

```bash
java A02_StockPriceFluctuation.java     # 5/5 tests passed (examples, 60,000 random updates: heaps == TreeMap == scan)
```

**One-line summary:** *"A HashMap holds the true price for each timestamp. A max-heap and a min-heap get every (price, timestamp) record, and outdated entries are thrown away only when they reach the top and no longer match the HashMap (lazy deletion)."*
