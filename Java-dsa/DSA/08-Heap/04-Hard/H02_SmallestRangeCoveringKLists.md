# H02 · Smallest Range Covering Elements from K Lists

| | |
|---|---|
| **LeetCode** | [#632 Smallest Range Covering Elements from K Lists](https://leetcode.com/problems/smallest-range-covering-elements-from-k-lists/) |
| **Difficulty** | Hard |
| **Pattern** | A k-way merge with a min-heap + track the current max |
| **Code** | [H02_SmallestRangeCoveringKLists.java](H02_SmallestRangeCoveringKLists.java) |
| **Run** | `java H02_SmallestRangeCoveringKLists.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). The k-way merge was introduced in [M06](../03-Medium/M06_KthSmallestInSortedMatrix.md).

---

## 1. The problem

Given k sorted lists, find the smallest range [a, b] that contains **at least one number from each list**. "Smaller" means a smaller b − a; on a tie, the smaller a.

```
[[4, 10, 15, 24, 26], [0, 9, 12, 20], [5, 18, 22, 30]]  →  [20, 24]
  (24 from list 0, 20 from list 1, 22 from list 2)
```

| lists | answer |
|---|---|
| the example | [20, 24] |
| `[[1,2,3],[1,2,3],[1,2,3]]` | [1, 1] |
| `[[5,7]]` | [5, 5] |

---

## 2. How to think about it

### Keep exactly one "current" element from each list
The range that covers them is **[the min of them, the max of them]**. To possibly shrink it, the only useful move is to **advance the list that holds the minimum**. Advancing any other pointer only raises the max (or keeps it the same), so the range can't get smaller.

### The algorithm
```
heap   = a min-heap of {value, list, index}, one entry per list
curMax = the max of them
loop:
    e = heap.poll()                       → the current minimum
    record [e.value, curMax] if it's better
    if e's list is exhausted → STOP       (the min can't move further, so no smaller range exists)
    push the next element of e's list;  curMax = max(curMax, it)
```

It's the k-way merge plus one extra variable. Equivalently, it's a sliding window over the merged sequence that always holds an element from all k lists.

### Dry run (from the real run)

| pop (list) | curMax | range | size | best? | push |
|---|---|---|---|---|---|
| 0 (1) | 5 | [0, 5] | 5 | ✓ | 9 |
| 4 (0) | 9 | [4, 9] | 5 | (not smaller) | 10 |
| 5 (2) | 10 | [5, 10] | 5 | | 18 |
| 9 (1) | 18 | [9, 18] | 9 | | 12 |
| 10 (0) | 18 | [10, 18] | 8 | | 15 |
| 12 (1) | 18 | [12, 18] | 6 | | 20 |
| 15 (0) | 20 | [15, 20] | 5 | | 24 |
| 18 (2) | 24 | [18, 24] | 6 | | 22 |
| 20 (1) | 24 | [20, 24] | **4** | ✓ | list 1 exhausted → stop |

**[20, 24]** ✓

---

## 3. Java concepts used in this problem

### 3.1 `List<List<Integer>>` input

`nums.get(i).get(j)` is element j of list i. The heap stores `{value, list, index}` so that it knows where the next element comes from.

### 3.2 Comparing range sizes with `long`

```java
if ((long) curMax - e[0] < (long) bestHi - bestLo)
```

`bestHi` starts at `Integer.MAX_VALUE`, and values can be negative, so `bestHi − bestLo` could overflow an int. The `long` casts avoid that. Using `<` (not `<=`) keeps the **earlier** range on ties, and since the minimum only increases, the earlier one has the smaller start, which matches the tie rule.

### 3.3 `TreeSet` for the sorted, distinct candidates (in the referee)

`new TreeSet<>()` with `addAll` collects every value once, in increasing order.

---

## 4. Approach 1: Try every value as the range start (the referee)

### Code ([H02_SmallestRangeCoveringKLists.java:65-81](H02_SmallestRangeCoveringKLists.java#L65-L81))

```java
static int[] brute(List<List<Integer>> nums) {
    int bestLo = 0, bestHi = Integer.MAX_VALUE;
    Set<Integer> candidates = new TreeSet<>();
    for (List<Integer> l : nums) candidates.addAll(l);
    for (int lo : candidates) {
        int hi = lo;
        boolean ok = true;
        for (List<Integer> l : nums) {
            Integer c = null;
            for (int x : l) if (x >= lo) { c = x; break; }
            if (c == null) { ok = false; break; }
            hi = Math.max(hi, c);
        }
        if (ok && (long) hi - lo < (long) bestHi - bestLo) { bestLo = lo; bestHi = hi; }
    }
    return new int[]{bestLo, bestHi};
}
```

| Line | What it does |
|---|---|
| `candidates` | every value is a possible range start |
| for each list | the smallest element ≥ lo (the cheapest way to cover that list) |
| `hi = max(...)` | the range must reach the farthest of those |
| no such element | this start can't cover that list |

The best range starts at some value of some list, so trying every value is enough. **O(N · N)** (N = the total number of elements).

---

## 5. Approach 2: K-way merge + the current max (the file's solution)

### Code ([H02_SmallestRangeCoveringKLists.java:43-62](H02_SmallestRangeCoveringKLists.java#L43-L62))

```java
static int[] smallestRange(List<List<Integer>> nums) {
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));   // {value, list, idx}
    int curMax = Integer.MIN_VALUE;
    for (int i = 0; i < nums.size(); i++) {
        int v = nums.get(i).get(0);
        heap.offer(new int[]{v, i, 0});
        curMax = Math.max(curMax, v);
    }
    int bestLo = 0, bestHi = Integer.MAX_VALUE;
    while (true) {
        int[] e = heap.poll();                                   // the current minimum
        if ((long) curMax - e[0] < (long) bestHi - bestLo) { bestLo = e[0]; bestHi = curMax; }
        int next = e[2] + 1;
        if (next == nums.get(e[1]).size()) break;                // this list is used up
        int v = nums.get(e[1]).get(next);
        heap.offer(new int[]{v, e[1], next});
        curMax = Math.max(curMax, v);
    }
    return new int[]{bestLo, bestHi};
}
```

### Line by line

| Line | What it does |
|---|---|
| the first loop | the first element of every list, and their max |
| `e = heap.poll()` | the minimum of the current selection |
| the range check | [min, curMax] covers every list; keep it if it's strictly smaller |
| `next == size → break` | the minimum's list has no more elements: any later selection would lack that list |
| push the next element | advance the minimum's list |
| `curMax = max(...)` | the max can only grow |

The heap holds k − 1 entries right after a poll (the polled list's next element is pushed back immediately), and the current selection is "the polled element + the heap".

### Complexity
- **Time O(N log k).**
- **Space O(k).**

---

## 6. Comparing the approaches

| | Every start (referee) | Merge + current max |
|---|---|---|
| Time | O(N²) | **O(N log k)** |
| Space | O(N) | O(k) |

The file checks the merge against the referee on 1,000 random cases (with negatives and duplicates).

---

## 7. Edge cases and common mistakes

**Edge cases:** a single list ([x, x] with its first element), identical lists, lists of length 1, negative values.

**Mistakes:**
1. Advancing a list other than the minimum's.
2. Continuing after a list is exhausted.
3. Not tracking curMax (recomputing it is O(k) per step).
4. Overflow in `bestHi − bestLo` with the MAX_VALUE sentinel.

---

## 8. Interview questions and answers

**Q1. Why advance only the minimum?**
> The range is [min, max]. Advancing any other list can't lower the max or raise the min, so it can't shrink the range. Advancing the minimum is the only move that can.

**Q2. Why stop when the minimum's list is exhausted?**
> Every future selection must still include an element of that list, and all of its elements are ≤ the current minimum. Any range that includes one of them and also elements of the other lists is at least as large as the ones already considered.

### Follow-up questions

**F1. The lists are unsorted.**
> Merge everything into one array of (value, list) sorted by value, then use a sliding window with a count of the distinct lists, like [Minimum Window Substring](../../02-Strings/04-Hard/H01_MinimumWindowSubstring.md).

**F2. Cover at least m of the k lists.**
> The sliding-window version handles this: shrink while the window covers ≥ m distinct lists.

**F3. Merge k sorted lists.** → [Linked Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)

---

## 9. Running the file

```bash
java H02_SmallestRangeCoveringKLists.java      # 4/4 tests passed (including 1,000 random cases vs brute force)
```

**One-line summary:** *"Keep one element per list in a min-heap and track their max; record [min, max], advance the minimum's list, and stop when it runs out."*
