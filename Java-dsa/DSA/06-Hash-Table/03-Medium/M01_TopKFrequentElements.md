# M01 · Top K Frequent Elements

| | |
|---|---|
| **LeetCode** | [#347 Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) |
| **Difficulty** | Medium |
| **Pattern** | Count with a HashMap, then bucket sort by frequency (or a size-k min-heap) |
| **Code** | [M01_TopKFrequentElements.java](M01_TopKFrequentElements.java) |
| **Run** | `java M01_TopKFrequentElements.java` |

---

## 1. The problem

Return the **k most frequent** elements, in any order. The answer is guaranteed to be unique. The follow-up asks you to beat O(n log n).

| nums | k | answer |
|---|---|---|
| `[1, 1, 1, 2, 2, 3]` | 2 | `[1, 2]` |
| `[1]` | 1 | `[1]` |
| `[4, 4, -1, -1, -1, 7]` | 2 | `[-1, 4]` |

**Constraints:** `1 ≤ n ≤ 10⁵`, k ≤ the number of distinct values.

---

## 2. How to think about it

### Step 1 (always): count
`HashMap<value, frequency>` in O(n).

### Step 2: pick the k largest frequencies
| Option | Idea | Cost |
|---|---|---|
| A. sort | sort the distinct values by frequency, take the first k | O(d log d) |
| B. min-heap of size k | keep the k most frequent seen so far; the heap's top is the **weakest** of them, which is evicted when a better one arrives | O(d log k) |
| C. **bucket sort** | a frequency is between 1 and n, so `buckets[f]` = the values occurring exactly f times; walk from f = n down | **O(n)** |

(d = the number of distinct values.)

```
[1, 1, 1, 2, 2, 3],  k = 2
counts:  {1: 3, 2: 2, 3: 1}
buckets: index  0   1    2    3    4   5   6
                []  [3]  [2]  [1]  []  []  []
walk f = 6 → 1:  f=3 → take 1;  f=2 → take 2;  have k = 2 → stop  →  [1, 2]
```

The same trick appears in [Strings M09 Sort Characters by Frequency](../../02-Strings/03-Medium/M09_SortCharactersByFrequency.md).

---

## 3. Java concepts used in this problem

### 3.1 `List<List<Integer>>`: a list of buckets

```java
List<List<Integer>> buckets = new ArrayList<>();
for (int i = 0; i <= n; i++) buckets.add(new ArrayList<>());   // n + 1 empty buckets: indices 0..n
buckets.get(f).add(value);
```

This avoids the generic-array problem from [E03](../02-Easy/E03_DesignHashSet.md): an `ArrayList` of lists is fine.

### 3.2 `Map.forEach` with a lambda

```java
count.forEach((value, freq) -> buckets.get(freq).add(value));
```

`forEach` calls the lambda once per entry with `(key, value)`. It's equivalent to looping over `entrySet()`.

### 3.3 `Map.Entry` and `entrySet()`

```java
for (Map.Entry<Integer, Integer> e : count.entrySet()) {
    e.getKey();     // the value from nums
    e.getValue();   // its frequency
}
```

An entry is one key–value pair of the map.

### 3.4 `PriorityQueue` (a min-heap) with a comparator: a preview of the Heap topic

```java
PriorityQueue<Map.Entry<Integer, Integer>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
heap.offer(e);      // O(log size)
heap.peek();        // the SMALLEST by the comparator, O(1)
heap.poll();        // remove the smallest, O(log size)
```

A `PriorityQueue` always hands out its smallest element first, by default or by the comparator you give it. `Map.Entry.comparingByValue()` compares entries by their value (here, the frequency). It's built in [Queue 01-Basics](../../05-Queue/01-Basics/PriorityQueueFromScratch.java), with the full topic in [08-Heap](../../08-Heap/README.md).

### 3.5 Breaking out of nested loops

```java
for (int f = n; f >= 1 && idx < k; f--) {
    for (int value : buckets.get(f)) {
        if (idx == k) break;          // leaves only the INNER loop
        ...
```

`break` exits only the innermost loop, so the outer loop's condition `idx < k` stops the outer one.

---

## 4. Approach 1: Sort the distinct values by frequency

```java
static int[] topKFrequentSort(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums) count.merge(x, 1, Integer::sum);
    List<Integer> values = new ArrayList<>(count.keySet());
    values.sort((a, b) -> count.get(b) - count.get(a));      // most frequent first
    int[] result = new int[k];
    for (int i = 0; i < k; i++) result[i] = values.get(i);
    return result;
}
```

| Line | What it does |
|---|---|
| `new ArrayList<>(count.keySet())` | copy the distinct values into a sortable list |
| `values.sort((a, b) -> count.get(b) - count.get(a))` | sort by frequency, descending (b first) |
| the last loop | take the first k |

Simple and correct: **O(n + d log d)**. It's what you'd write first; then the interviewer asks for better.

---

## 5. Approach 2: A min-heap of size k

### Code ([M01_TopKFrequentElements.java:58-69](M01_TopKFrequentElements.java#L58-L69))

```java
static int[] topKFrequentHeap(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums) count.merge(x, 1, Integer::sum);
    PriorityQueue<Map.Entry<Integer, Integer>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
    for (Map.Entry<Integer, Integer> e : count.entrySet()) {
        heap.offer(e);
        if (heap.size() > k) heap.poll();                 // drop the least frequent
    }
    int[] result = new int[k];
    for (int i = 0; i < k; i++) result[i] = heap.poll().getKey();
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| the `PriorityQueue` | a min-heap ordered by frequency: the top is the **least** frequent |
| `heap.offer(e)` | add this candidate |
| `if (heap.size() > k) heap.poll();` | over capacity: evict the least frequent, so the heap holds the best k so far |
| the final loop | the k survivors, polled from the least to the most frequent |

**Why a *min*-heap for the *top* k?** The heap is a "club of the best k"; its weakest member sits at the top so it can be evicted in O(log k) when someone better shows up.

### Dry run: `[1, 1, 1, 2, 2, 3]`, k = 2

| entry offered | heap (by frequency) | size > 2? |
|---|---|---|
| 1:3 | [1:3] | |
| 2:2 | [2:2, 1:3] | |
| 3:1 | [3:1, 2:2, 1:3] | yes → poll 3:1 → [2:2, 1:3] |

Polling gives 2, then 1 → **[2, 1]** (any order is accepted) ✓

### Complexity
- **Time O(n + d log k).**
- **Space O(d + k).**

---

## 6. Approach 3: Bucket sort by frequency (the file's solution)

### Code ([M01_TopKFrequentElements.java:39-56](M01_TopKFrequentElements.java#L39-L56))

```java
static int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums) count.merge(x, 1, Integer::sum);

    List<List<Integer>> buckets = new ArrayList<>();
    for (int i = 0; i <= nums.length; i++) buckets.add(new ArrayList<>());
    count.forEach((value, freq) -> buckets.get(freq).add(value));

    int[] result = new int[k];
    int idx = 0;
    for (int f = nums.length; f >= 1 && idx < k; f--) {
        for (int value : buckets.get(f)) {
            if (idx == k) break;
            result[idx++] = value;
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `count.merge(x, 1, Integer::sum)` | count each value |
| `i <= nums.length` | buckets 0..n: a frequency can be as large as n (all the elements equal) |
| `count.forEach(... buckets.get(freq).add(value))` | drop each value into the bucket of its frequency |
| `for (int f = nums.length; f >= 1 && idx < k; f--)` | from the highest frequency down, until k values have been collected |
| `if (idx == k) break;` | a bucket may hold more values than we still need |
| `result[idx++] = value;` | store, then advance idx |

### Dry run: `[4, 4, -1, -1, -1, 7]`, k = 2

counts = {−1: 3, 4: 2, 7: 1}, n = 6

| f | buckets[f] | taken | idx |
|---|---|---|---|
| 6, 5, 4 | [] | | 0 |
| 3 | [−1] | −1 | 1 |
| 2 | [4] | 4 | 2 → the loop ends |

**[−1, 4]** ✓

### Complexity
- **Time O(n):** counting O(n), filling the buckets O(d), walking at most n + 1 buckets.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Sort | Min-heap of size k | Bucket sort |
|---|---|---|---|
| Time | O(n + d log d) | O(n + d log k) | **O(n)** |
| Space | O(d) | O(d + k) | O(n) |
| Good when | it's simple enough | k is small, or the data is a stream | always (the frequencies are bounded by n) |

The file's random test (with a unique answer each time) checks both the bucket and the heap versions.

---

## 8. Edge cases and common mistakes

**Edge cases:** k equals the number of distinct values, all elements equal (frequency n, which needs bucket index n), negative values, a single element.

**Mistakes:**
1. Allocating only n buckets (indices 0..n−1) → an out-of-bounds error when everything is equal.
2. Using a **max**-heap of all d entries and polling k times. That works, but it's O(d + k log d), which misses the point of the size-k heap.
3. `count.get(b) - count.get(a)` overflow: not an issue here (frequencies ≤ 10⁵), but `Integer.compare` is the safe habit.

---

## 9. Interview questions and answers

**Q1. Why a min-heap rather than a max-heap?**
> A size-k min-heap keeps only k entries and evicts the weakest in O(log k), so the total is O(d log k). A max-heap would need all d entries.

**Q2. Why does bucket sort work here?**
> Frequencies are integers from 1 to n, a small bounded range, so we can index by frequency directly instead of comparing.

**Q3. Which would you pick for a stream of data?**
> The size-k heap, plus a running count map: it maintains the current top k without storing buckets for every frequency.

### Follow-up questions

**F1. Top K Frequent Words, with ties broken alphabetically (LC 692).**
> The heap comparator becomes "frequency ascending, then word **descending**" (the opposite of the output order, since the min-heap evicts from the top).

**F2. Quickselect.**
> Partition the distinct values by frequency around a pivot until the k-th position is fixed: O(d) on average, O(d²) in the worst case.

**F3. A huge distributed dataset.**
> Count on each machine, merge the counts (map-reduce), then select the top k, or use approximate sketches like Count-Min + a heap.

---

## 10. Running the file

```bash
java M01_TopKFrequentElements.java      # 5/5 tests passed (including 200 random cases, both approaches)
```

**One-line summary:** *"Count with a HashMap, drop each value into buckets[frequency], and walk the buckets from n down until you have k."*
