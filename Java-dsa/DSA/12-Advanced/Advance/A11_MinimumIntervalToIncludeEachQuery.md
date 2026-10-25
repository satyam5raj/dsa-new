# A11 · Minimum Interval to Include Each Query ⭐

| | |
|---|---|
| **LeetCode** | [#1851 Minimum Interval to Include Each Query](https://leetcode.com/problems/minimum-interval-to-include-each-query/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Offline queries** (sorted) + **sorted intervals** + **min-heap** by size |
| **Code** | [A11_MinimumIntervalToIncludeEachQuery.java](A11_MinimumIntervalToIncludeEachQuery.java) |
| **Run** | `java A11_MinimumIntervalToIncludeEachQuery.java` |

⭐ = not in `leetcode.md`, added because it teaches **offline query processing**, a powerful trick that pairs sorting with a heap.

---

## 1. The problem

`intervals[i] = [left, right]` (inclusive; size = `right − left + 1`). For each query `q`, return the size of the **smallest** interval with `left ≤ q ≤ right`, or −1 if none contains q.

```
intervals = [[1,4],[2,4],[3,6],[4,4]], queries = [2,3,4,5]      →  [3, 3, 1, 4]
intervals = [[2,3],[2,5],[1,8],[20,25]], queries = [2,19,5,22]  →  [2, -1, 4, 6]
```

Up to 10⁵ intervals and 10⁵ queries.

---

## 2. How to think about it

### Brute force
For each query, scan every interval: **O(n · q)** = 10¹⁰. Too slow.

### "Offline": answer queries in a smart order
An **online** algorithm must answer queries in the order given. Here all queries are known up front, so we may answer them in **any order**, as long as each answer goes back into its original slot. That's called **offline** processing.

Answer them from the **smallest q to the largest**, sweeping left to right:

| As q grows… | so we… |
|---|---|
| more intervals **start** (left ≤ q) | walk a pointer through intervals sorted by `left`, pushing each into a heap **once** |
| some intervals **end** (right < q) | pop them; they can't cover this q **or any later, bigger one**, so they're gone for good |
| we want the smallest interval covering q | a **min-heap by size**: after removing ended ones, the top is the answer |

Each interval is pushed once and popped at most once, so the whole sweep is O((n + q) log n).

### Why removal is safe
An interval leaves the heap only when `right < q`. Every later query is ≥ q, so the interval can never contain it. That's the reason for sorting the queries.

### Dry run: example 1
Intervals sorted by left: [1,4] [2,4] [3,6] [4,4]. Heap entries are `(size, right)`.

| q | pushed | popped (right < q) | heap top | answer |
|---|---|---|---|---|
| 2 | (4,4) from [1,4], (3,4) from [2,4] | – | (3,4) | **3** |
| 3 | (4,6) from [3,6] | – | (3,4) | **3** |
| 4 | (1,4) from [4,4] | – | (1,4) | **1** |
| 5 | – | (1,4), (3,4), (4,4) | (4,6) | **4** |

Answer **[3, 3, 1, 4]** ✓

---

## 3. Java concepts used in this problem

### 3.1 Sorting query **indices** to remember original positions
```java
Integer[] order = new Integer[queries.length];
for (int j = 0; j < queries.length; j++) order[j] = j;
Arrays.sort(order, Comparator.comparingInt(j -> queries[j]));
...
for (int j : order) { ... ans[j] = ...; }
```
We process queries by value but write each answer to `ans[j]`, its **original** slot. As in [A03](A03_SingleThreadedCPU.md), comparators need an `Integer[]`, not an `int[]`.

### 3.2 `clone()` before sorting
```java
int[][] ivs = intervals.clone();
Arrays.sort(ivs, Comparator.comparingInt(iv -> iv[0]));
```
`clone()` copies the **outer** array (a shallow copy), so sorting `ivs` doesn't reorder the caller's array. The inner `int[]`s are shared, but we never modify them.

### 3.3 A heap with a two-field comparator
```java
(a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1])   // {size, right}
```
Size first. The tie-break on `right` isn't needed for correctness (equal sizes give equal answers); it just makes the order deterministic.

---

## 4. Approach 1: Scan all intervals per query (the referee)

### Code ([A11_MinimumIntervalToIncludeEachQuery.java:41-53](A11_MinimumIntervalToIncludeEachQuery.java#L41-L53))
```java
static int[] minIntervalBrute(int[][] intervals, int[] queries) {
    int[] ans = new int[queries.length];
    for (int j = 0; j < queries.length; j++) {
        int best = -1;
        for (int[] iv : intervals)
            if (iv[0] <= queries[j] && queries[j] <= iv[1]) {
                int size = iv[1] - iv[0] + 1;
                if (best == -1 || size < best) best = size;
            }
        ans[j] = best;
    }
    return ans;
}
```

This follows the definition directly: **O(n · q)**.

---

## 5. Approach 2: Offline sweep + min-heap (main solution)

### Code ([A11_MinimumIntervalToIncludeEachQuery.java:56-76](A11_MinimumIntervalToIncludeEachQuery.java#L56-L76))
```java
static int[] minInterval(int[][] intervals, int[] queries) {
    int[][] ivs = intervals.clone();
    Arrays.sort(ivs, Comparator.comparingInt(iv -> iv[0]));        // by left end
    Integer[] order = new Integer[queries.length];
    for (int j = 0; j < queries.length; j++) order[j] = j;
    Arrays.sort(order, Comparator.comparingInt(j -> queries[j]));   // query indices, smallest q first

    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));   // {size, right}
    int[] ans = new int[queries.length];
    int i = 0;
    for (int j : order) {
        int q = queries[j];
        while (i < ivs.length && ivs[i][0] <= q) {                   // intervals that have started
            heap.add(new int[]{ivs[i][1] - ivs[i][0] + 1, ivs[i][1]});
            i++;
        }
        while (!heap.isEmpty() && heap.peek()[1] < q) heap.poll();   // ended before q: useless forever
        ans[j] = heap.isEmpty() ? -1 : heap.peek()[0];
    }
    return ans;
}
```

### Line by line
| Line | What it does |
|---|---|
| sort `ivs` by left | so a single pointer `i` can release intervals as q grows |
| sort `order` by query value | the offline order |
| the first `while` | push every interval that has started by q; `i` never goes back |
| the second `while` | drop tops that ended before q. Only the **top** needs checking: an ended interval deeper in the heap is harmless until it reaches the top, and then it's dropped (lazy deletion, like [A02](A02_StockPriceFluctuation.md)) |
| `ans[j]` | the smallest interval covering q, stored in its original slot |

### Complexity
- Sorting: O(n log n + q log q).
- Each interval pushed and popped at most once: O(n log n).
- **Total O((n + q) log n + q log q)**, space O(n + q).

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Scan per query | O(n · q) |
| **Offline sweep + heap** | **O((n + q) log n + q log q)** |

The file checks both on 2,000 random cases, including duplicate queries.

---

## 7. Edge cases and common mistakes

**Edge cases:** a query covered by no interval (−1), duplicate queries, an interval of size 1, a query exactly at an interval's left or right end (both included).

**Mistakes:**
1. Answering in sorted order but writing to `ans` in sorted order too (answers end up in the wrong slots).
2. Popping intervals with `right < q` from the **middle** of the heap (unnecessary and slow).
3. Using `right <= q` instead of `<` (an interval ending exactly at q still covers it).
4. Restarting the interval pointer for each query (back to O(n · q)).
5. Sorting the caller's `intervals` array in place.

---

## 8. Interview questions and answers

**Q1. What does "offline" mean here, and why does it help?**
> All queries are known in advance, so we can reorder them. Processing them by increasing value means intervals only ever get added (when they start) and removed (when they end), each once, instead of being re-examined for every query.

**Q2. Why is it safe to discard an interval forever?**
> It's discarded only when `right < q`, and every later query is ≥ q, so it can never cover one of them.

**Q3. Why only check the top of the heap?**
> We only need the smallest valid interval. Expired intervals deeper in the heap don't affect the top until they reach it, and then they're removed.

### Follow-up questions

**F1. "Queries arrive online (one at a time)."**
> Then the sweep doesn't work. A segment tree over coordinates (compressed), where each interval does a range "chmin(size)", answers each point query in O(log n).

**F2. "Return which interval, not its size."**
> Store the interval's index in the heap entry.

**F3. "Other offline problems?"**
> [Edge Length Limited Paths](../../11-Union-Find/04-Hard/H03_EdgeLengthLimitedPaths.md) (sort queries and edges, union-find) and [Maximum XOR With an Element](../../10-Trie/04-Hard/H05_MaximumXorWithAnElement.md) (sort queries, trie) use the same trick.

---

## 9. Running the file

```bash
java A11_MinimumIntervalToIncludeEachQuery.java     # 4/4 tests passed (two examples, duplicate queries, 2,000 random cases)
```

**One-line summary:** *"Sort intervals by left and query indices by value; sweep queries in increasing order, pushing intervals that have started into a min-heap by size, popping those that ended before q, and recording the heap's top (or −1) in each query's original slot."*
