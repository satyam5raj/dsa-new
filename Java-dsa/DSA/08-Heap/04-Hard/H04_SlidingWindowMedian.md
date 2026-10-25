# H04 · Sliding Window Median

| | |
|---|---|
| **LeetCode** | [#480 Sliding Window Median](https://leetcode.com/problems/sliding-window-median/) |
| **Difficulty** | Hard |
| **Pattern** | Two heaps ([H01](H01_FindMedianFromDataStream.md)) + lazy deletion for the element leaving the window |
| **Code** | [H04_SlidingWindowMedian.java](H04_SlidingWindowMedian.java) |
| **Run** | `java H04_SlidingWindowMedian.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the median of every window of size k.

```
[1, 3, −1, −3, 5, 3, 6, 7], k = 3  →  [1, −1, −1, 3, 5, 6]
  [1 3 −1] → 1,  [3 −1 −3] → −1,  [−1 −3 5] → −1,  [−3 5 3] → 3,  [5 3 6] → 5,  [3 6 7] → 6
```

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`, values in the full int range.

---

## 2. How to think about it

### H01 handles adding; a window also removes
The element sliding out must be deleted, and `PriorityQueue.remove(x)` is **O(k)** (a linear search). With n windows, that's O(n · k). It's acceptable for small k, and a fine first answer in an interview.

### Lazy deletion: O(n log n)
Don't remove the outgoing value right away. Record it in a map `delayed` (value → count) and adjust the **live** sizes of the halves. A dead value is only popped once it reaches the **top** of a heap, which is the only place it could affect the answer.

```
lowSize, highSize = the LIVE counts;   keep lowSize == highSize or highSize + 1
add(x):    x <= low.top ? low : high;   rebalance
remove(x): delayed[x]++;  decrement the size of the half x belongs to (x <= low.top → low);
           if x IS that heap's top, prune it now;  rebalance
rebalance: if the live sizes are off, move one top across, then prune the heap it came from
prune(h):  while h.top is in delayed: pop it, delayed[top]−−
```

### Alternative: two TreeSets of indices
Order by (value, index). `TreeSet.remove` is O(log k) directly, with no lazy deletion needed. The file uses it as the referee.

---

## 3. Java concepts used in this problem

### 3.1 Why `PriorityQueue.remove(Object)` is slow

A heap only knows where its **minimum** is. Removing an arbitrary element means finding it (a linear scan, O(k)), then fixing the heap (O(log k)).

### 3.2 A `HashMap<Integer, Integer>` as a multiset of dead values

```java
delayed.merge(x, 1, Integer::sum);                         // one more dead copy of x
if (d == 1) delayed.remove(top); else delayed.put(top, d - 1);   // consume one
```

Duplicates matter: if two copies of 3 have left the window, two physical 3s in the heaps must eventually be discarded.

### 3.3 Live sizes vs physical sizes

`low.size()` includes dead entries waiting to be pruned. The balance rule must use the **live** counts `lowSize` and `highSize`, which is why they're tracked separately.

### 3.4 A TreeSet of indices with a tie-breaking comparator (the referee)

```java
Comparator<Integer> cmp = (a, b) -> nums[a] != nums[b] ? Integer.compare(nums[a], nums[b]) : Integer.compare(a, b);
```

TreeSets don't keep duplicates. Storing **indices** ordered by (value, index) makes equal values distinct, so each window element is its own entry and can be removed exactly.

### 3.5 `(double) a + b` for the average

As in H01: convert before adding, so two large ints don't overflow.

---

## 4. Approach 1: Two heaps with `remove(x)` (O(n · k))

```java
static double[] medianSlidingWindowSimple(int[] nums, int k) {
    PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder()), high = new PriorityQueue<>();
    double[] out = new double[nums.length - k + 1];
    for (int i = 0; i < nums.length; i++) {
        if (i >= k) {                                        // the outgoing element
            if (!low.remove(nums[i - k])) high.remove(nums[i - k]);
        }
        low.offer(nums[i]);
        high.offer(low.poll());
        while (high.size() > low.size()) low.offer(high.poll());
        while (low.size() > high.size() + 1) high.offer(low.poll());
        if (i >= k - 1) out[i - k + 1] = low.size() > high.size() ? low.peek() : ((double) low.peek() + high.peek()) / 2.0;
    }
    return out;
}
```

H01's add, plus a direct removal. After a removal the halves can be off by one in either direction, hence the two `while` loops. It's simple and correct, but **O(n · k)**.

---

## 5. Approach 2: Two TreeSets of indices (the referee)

### Code ([H04_SlidingWindowMedian.java:105-117](H04_SlidingWindowMedian.java#L105-L117))

```java
static double[] withTreeSets(int[] nums, int k) {
    Comparator<Integer> cmp = (a, b) -> nums[a] != nums[b] ? Integer.compare(nums[a], nums[b]) : Integer.compare(a, b);
    TreeSet<Integer> lo = new TreeSet<>(cmp), hi = new TreeSet<>(cmp);
    double[] out = new double[nums.length - k + 1];
    for (int i = 0; i < nums.length; i++) {
        if (i >= k) { if (!lo.remove(i - k)) hi.remove(i - k); }
        lo.add(i);
        hi.add(lo.pollLast());
        if (hi.size() > lo.size()) lo.add(hi.pollFirst());
        if (i >= k - 1) out[i - k + 1] = lo.size() > hi.size() ? nums[lo.last()] : ((double) nums[lo.last()] + nums[hi.first()]) / 2.0;
    }
    return out;
}
```

| Line | What it does |
|---|---|
| `lo.remove(i − k)` | remove the outgoing index directly in O(log k) (`remove` returns false if it was in `hi`) |
| `lo.add(i); hi.add(lo.pollLast());` | H01's "add low, move low's max up" |
| `pollLast` / `pollFirst` / `last` / `first` | the TreeSet versions of the heap operations |

**O(n log k)**, and arguably the easiest version to get right.

---

## 6. Approach 3: Two heaps + lazy deletion (the file's main solution)

### Code ([H04_SlidingWindowMedian.java:42-102](H04_SlidingWindowMedian.java#L42-L102))

```java
static class DualHeap {
    final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder());
    final PriorityQueue<Integer> high = new PriorityQueue<>();
    final Map<Integer, Integer> delayed = new HashMap<>();
    int lowSize, highSize;                                      // LIVE element counts

    void add(int x) {
        if (low.isEmpty() || x <= low.peek()) { low.offer(x); lowSize++; }
        else { high.offer(x); highSize++; }
        rebalance();
    }

    void remove(int x) {
        delayed.merge(x, 1, Integer::sum);
        if (x <= low.peek()) {
            lowSize--;
            if (x == low.peek()) prune(low);
        } else {
            highSize--;
            if (!high.isEmpty() && x == high.peek()) prune(high);
        }
        rebalance();
    }

    double median() {
        return lowSize > highSize ? low.peek() : ((double) low.peek() + high.peek()) / 2.0;
    }

    private void rebalance() {
        if (lowSize > highSize + 1) {                           // low too big
            high.offer(low.poll()); lowSize--; highSize++;
            prune(low);
        } else if (lowSize < highSize) {                        // high too big
            low.offer(high.poll()); highSize--; lowSize++;
            prune(high);
        }
    }

    private void prune(PriorityQueue<Integer> heap) {           // drop dead values from the top
        while (!heap.isEmpty()) {
            int top = heap.peek();
            Integer d = delayed.get(top);
            if (d == null) break;
            if (d == 1) delayed.remove(top); else delayed.put(top, d - 1);
            heap.poll();
        }
    }
}

static double[] medianSlidingWindow(int[] nums, int k) {
    DualHeap dh = new DualHeap();
    for (int i = 0; i < k; i++) dh.add(nums[i]);
    double[] result = new double[nums.length - k + 1];
    result[0] = dh.median();
    for (int i = k; i < nums.length; i++) {
        dh.add(nums[i]);
        dh.remove(nums[i - k]);
        result[i - k + 1] = dh.median();
    }
    return result;
}
```

### Line by line

| Part | What it does |
|---|---|
| `add` | put x on the correct side by comparing with low's top, then fix the sizes |
| `remove` | mark x dead; decide its side by the same comparison (≤ low's top → it's in low); decrement that side's live size; if it's the top, discard it now |
| `rebalance` | move one top across when the live sizes break the rule, then prune the heap it came from (its new top might be dead) |
| `prune` | pop dead tops, consuming their `delayed` counts |
| `median` | the same as H01, but using the live sizes |

**Invariant:** after every public operation, the tops of both heaps are live. That's what makes `median()` correct.

**Why "x ≤ low.peek()" identifies x's side:** every live element of low is ≤ low's top, and every live element of high is ≥ high's top ≥ low's top. A value equal to low's top could physically be in either heap, but since equal values are interchangeable, it doesn't matter which copy is marked dead.

### Dry run: `[1, 3, −1, −3, 5, 3, 6, 7]`, k = 3 (from the real run)

| step | low (max-heap, physical) | high | live sizes | delayed | median |
|---|---|---|---|---|---|
| init 1, 3, −1 | {1, −1} | {3} | (2, 1) | | **1** |
| add −3 | {−1, −3} | {1, 3} | (2, 2) | | |
| remove 1 (it's high's top → pruned) | {−1, −3} | {3} | (2, 1) | | **−1** |
| add 5 | {−1, −3} | {3, 5} | (2, 2) | | |
| remove 3 (high's top → pruned) | {−1, −3} | {5} | (2, 1) | | **−1** |
| add 3 | {−1, −3} | {3, 5} | (2, 2) | | |
| remove −1 (low's top → pruned; rebalance moves 3 down) | {3, −3} | {5} | (2, 1) | | **3** |
| add 6 | {3, −3} | {5, 6} | (2, 2) | | |
| remove −3 (**not** a top → stays, marked dead); rebalance moves 5 down | {5, 3, **−3**} | {6} | (2, 1) | {−3: 1} | **5** |
| add 7 | {5, 3, −3} | {6, 7} | (2, 2) | {−3: 1} | |
| remove 5 (low's top → pruned); rebalance moves 6 down | {6, 3, −3} | {7} | (2, 1) | {−3: 1} | **6** |

→ `[1, −1, −1, 3, 5, 6]` ✓. Note the dead −3, still physically inside low at the end: it's harmless, because it never reaches the top while it matters.

### Complexity
- **Time O(n log n)** (each value is pushed and popped at most once or twice).
- **Space O(n)** (dead entries can pile up).

---

## 7. Comparing the approaches

| | Heaps + remove(x) | TreeSets of indices | Heaps + lazy deletion |
|---|---|---|---|
| Time | O(n · k) | O(n log k) | O(n log n) |
| Space | O(k) | O(k) | O(n) worst case |
| Bug risk | low | low | high (sizes, pruning) |

The file checks the lazy version against the TreeSets on 1,000 random arrays with many duplicates.

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 1 (each element), k = n (one window), duplicates, `Integer.MAX_VALUE` pairs.

**Mistakes:**
1. Using `heap.size()` (physical) instead of the live sizes for balancing.
2. Forgetting to prune after moving a top across.
3. Comparing a removed value with the wrong top to decide its side.
4. Integer overflow when averaging.

---

## 9. Interview questions and answers

**Q1. What is lazy deletion, and why is it valid here?**
> Marking an element as deleted and only physically removing it when it reaches the top of the heap. It's valid because only the tops are ever read; a dead element deep inside a heap can't affect any answer.

**Q2. Which version would you write in an interview?**
> I'd explain the O(n · k) version, then implement the TreeSet-of-indices version (O(n log k), little room for bugs), and describe lazy deletion as the heap-only alternative.

### Follow-up questions

**F1. The sliding window maximum.** → [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)
> A monotonic deque, O(n): much simpler, because the max doesn't need two halves.

**F2. A sliding window percentile.**
> The same two structures, with low sized ⌈p · k⌉.

**F3. Lazy deletion elsewhere.**
> Dijkstra with a PriorityQueue (skip stale entries when polled) and the Skyline problem ([H06](H06_TheSkylineProblem.md)).

---

## 10. Running the file

```bash
java H04_SlidingWindowMedian.java      # 4/4 tests passed (including 1,000 random arrays vs TreeSets)
```

**One-line summary:** *"Two heaps for the halves; when an element leaves the window, mark it in a 'delayed' map, adjust the live sizes, and only pop dead values once they reach a heap's top."*
