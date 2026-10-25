# A05 · Data Stream as Disjoint Intervals ⭐

| | |
|---|---|
| **LeetCode** | [#352 Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/) |
| **Difficulty** | Hard |
| **Mix of structures** | **TreeMap** `start → end` + neighbour lookups (`floorKey` / `higherKey`) |
| **Code** | [A05_DataStreamAsDisjointIntervals.java](A05_DataStreamAsDisjointIntervals.java) |
| **Run** | `java A05_DataStreamAsDisjointIntervals.java` |

⭐ = not in `leetcode.md`, added because it combines a sorted map with interval-merging logic.

---

## 1. The problem

Non-negative integers arrive one at a time.
- `addNum(value)` records a number.
- `getIntervals()` returns everything seen so far as **sorted, disjoint intervals** `[start, end]`.

```
addNum(1)  → [1,1]
addNum(3)  → [1,1] [3,3]
addNum(7)  → [1,1] [3,3] [7,7]
addNum(2)  → [1,3] [7,7]          (2 glues 1 and 3 together)
addNum(6)  → [1,3] [6,7]
```

---

## 2. How to think about it

### The simple way
Keep every number in a sorted set and build the intervals by walking it each time `getIntervals()` is called: O(n) per call. That's fine if calls are rare, but the problem's follow-up asks: *"what if there are lots of merges and the number of intervals is small compared to the stream?"* Then we want to store **intervals**, not numbers.

### Only two neighbours matter
When `v` arrives, the only intervals it can touch are:
- **left**: the interval starting at or before `v` (`floorKey(v)`);
- **right**: the interval starting after `v` (`higherKey(v)`).

Nothing else can be affected, because the intervals are disjoint and sorted. A `TreeMap<start, end>` finds both in O(log n). Then there are five cases:

| Case | Test | Action |
|---|---|---|
| already covered | `left.end >= v` | nothing |
| touches **both** | `left.end == v − 1` and `right.start == v + 1` | merge: `left.end = right.end`, remove `right` |
| touches left only | `left.end == v − 1` | `left.end = v` |
| touches right only | `right.start == v + 1` | new interval `[v, right.end]`, remove the old `right` |
| touches neither | | new interval `[v, v]` |

### Dry run
| add | floorKey → interval | higherKey | case | map after |
|---|---|---|---|---|
| 1 | – | – | neither | {1:1} |
| 3 | 1 → [1,1] (1 ≠ 2) | – | neither | {1:1, 3:3} |
| 7 | 3 → [3,3] (3 ≠ 6) | – | neither | {1:1, 3:3, 7:7} |
| 2 | 1 → [1,1] (1 = 2−1 ✓) | 3 (= 2+1 ✓) | **both** | {1:3, 7:7} |
| 6 | 1 → [1,3] (3 ≠ 5) | 7 (= 6+1 ✓) | right only | {1:3, 6:7} |

---

## 3. Java concepts used in this problem

### 3.1 TreeMap navigation methods
| Method | Returns |
|---|---|
| `floorKey(x)` | the largest key **≤ x**, or `null` |
| `ceilingKey(x)` | the smallest key **≥ x**, or `null` |
| `lowerKey(x)` | the largest key **< x**, or `null` |
| `higherKey(x)` | the smallest key **> x**, or `null` |

All are O(log n). They're what make a TreeMap more than a sorted HashMap: you can ask for the **neighbours** of a value that isn't in the map.

### 3.2 `remove` returns the removed value
```java
intervals.put(value, intervals.remove(hi));
```
`remove(hi)` deletes the right interval **and returns its end**, which becomes the end of the new interval starting at `value`. One line, no temporary variable.

### 3.3 `Integer` keys and `==`
```java
boolean joinRight = hi != null && hi == value + 1;
```
`hi` is an `Integer`; `value + 1` is an `int`, so `==` unboxes `hi` and compares numbers. Checking `hi != null` first is essential: unboxing `null` throws.

### 3.4 Building an `int[][]` from a list
```java
return out.toArray(new int[0][]);
```
`toArray(new int[0][])` gives a typed `int[][]`. The empty array just tells Java the element type.

---

## 4. Approach 1: Keep every number, build intervals on demand (the referee)

### Code ([A05_DataStreamAsDisjointIntervals.java:40-51](A05_DataStreamAsDisjointIntervals.java#L40-L51))
```java
static class SetSummaryRanges {
    private final TreeSet<Integer> seen = new TreeSet<>();
    public void addNum(int value) { seen.add(value); }
    public int[][] getIntervals() {
        List<int[]> out = new ArrayList<>();
        for (int v : seen) {
            if (!out.isEmpty() && out.get(out.size() - 1)[1] == v - 1) out.get(out.size() - 1)[1] = v;   // extend the last run
            else out.add(new int[]{v, v});
        }
        return out.toArray(new int[0][]);
    }
}
```

| Line | What it does |
|---|---|
| `TreeSet` | the numbers, sorted, without duplicates |
| the loop | walk in order; if `v` continues the last run, extend it, otherwise start a new run |

**Cost:** `addNum` O(log n); `getIntervals` **O(n)** for n distinct numbers.

---

## 5. Approach 2: TreeMap of intervals (main solution)

### Code ([A05_DataStreamAsDisjointIntervals.java:54-75](A05_DataStreamAsDisjointIntervals.java#L54-L75))
```java
static class SummaryRanges {
    private final TreeMap<Integer, Integer> intervals = new TreeMap<>();   // start -> end

    public void addNum(int value) {
        Integer lo = intervals.floorKey(value);                       // interval starting at or before value
        if (lo != null && intervals.get(lo) >= value) return;          // already covered
        Integer hi = intervals.higherKey(value);                       // interval starting after value
        boolean joinLeft = lo != null && intervals.get(lo) == value - 1;
        boolean joinRight = hi != null && hi == value + 1;
        if (joinLeft && joinRight) { intervals.put(lo, intervals.get(hi)); intervals.remove(hi); }
        else if (joinLeft) intervals.put(lo, value);
        else if (joinRight) intervals.put(value, intervals.remove(hi));
        else intervals.put(value, value);
    }

    public int[][] getIntervals() {
        int[][] out = new int[intervals.size()][];
        int i = 0;
        for (Map.Entry<Integer, Integer> e : intervals.entrySet()) out[i++] = new int[]{e.getKey(), e.getValue()};
        return out;
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| `floorKey(value)` | the only interval that could already contain `value`, or end right before it |
| `get(lo) >= value → return` | covered, including duplicates |
| `higherKey(value)` | the next interval to the right (`value` itself can't be a key here, or `floorKey` would have found it and returned) |
| `joinLeft`, `joinRight` | does `value` touch each neighbour? |
| the four branches | the four remaining cases from the table above |
| `getIntervals` | the map is already sorted by start: copy it out |

### Complexity
- `addNum`: **O(log k)** for k intervals.
- `getIntervals`: **O(k)**.
- Space: **O(k)**. With many merges, k is far smaller than the number of values.

---

## 6. Comparing the approaches

| | addNum | getIntervals | Memory |
|---|---|---|---|
| TreeSet of numbers | O(log n) | O(n) | O(n) numbers |
| **TreeMap of intervals** | O(log k) | O(k) | O(k) intervals |

The file runs 30,000 random adds and compares the intervals after every single add.

---

## 7. Edge cases and common mistakes

**Edge cases:** duplicates, a value inside an existing interval, 0 (no left neighbour), a value that bridges two intervals, values arriving in decreasing order.

**Mistakes:**
1. Checking only the left neighbour, so `[1,1]` + 2 + `[3,3]` never merges.
2. Forgetting the "already covered" check, so a duplicate creates a bogus `[v, v]`.
3. Unboxing a `null` key (`hi == value + 1` without `hi != null`).
4. Leaving the old right interval in the map after merging.

---

## 8. Interview questions and answers

**Q1. Why a TreeMap?**
> We need intervals sorted by start, plus the neighbours of a value that isn't a key (`floorKey` / `higherKey`), both in O(log n). A HashMap has no order; a sorted list has O(n) inserts.

**Q2. Why can a new number only affect two intervals?**
> The intervals are disjoint and sorted. `v` can only extend the interval just before it or the one just after it; anything further away is separated by at least one missing number.

**Q3. Follow-up from LeetCode: many merges, few intervals?**
> That's exactly why we store intervals instead of numbers: memory and `getIntervals` cost depend on the number of intervals, not on the stream length.

### Follow-up questions

**F1. "Support removing a number."**
> Find the interval containing it with `floorKey`, and split it into up to two intervals. [A06 Range Module](A06_RangeModule.md) does this for whole ranges.

**F2. "Count how many numbers are covered."**
> Keep a running total, adding the size of each merge and new interval.

**F3. "Similar problems?"**
> Insert Interval, Merge Intervals, My Calendar I/II, and Range Module: all "intervals in a sorted map".

---

## 9. Running the file

```bash
java A05_DataStreamAsDisjointIntervals.java     # 4/4 tests passed (example stream, duplicate, full merge, 30,000 random adds)
```

**One-line summary:** *"Keep disjoint intervals in a TreeMap start → end. A new number looks at its floor and higher neighbours and either is already covered, bridges both, extends one, or becomes a new [v, v]."*
