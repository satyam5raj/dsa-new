# A06 · Range Module ⭐

| | |
|---|---|
| **LeetCode** | [#715 Range Module](https://leetcode.com/problems/range-module/) |
| **Difficulty** | Hard |
| **Mix of structures** | **TreeMap** `start → end` of disjoint half-open intervals: **merge** on add, **split** on remove |
| **Code** | [A06_RangeModule.java](A06_RangeModule.java) |
| **Run** | `java A06_RangeModule.java` |

⭐ = not in `leetcode.md`. It's the "big brother" of [A05](A05_DataStreamAsDisjointIntervals.md): whole ranges instead of single numbers, and removal too.

---

## 1. The problem

Track ranges of **real numbers**, half-open `[left, right)`.
- `addRange(left, right)`: track every number in `[left, right)`.
- `queryRange(left, right)`: `true` if **every** number in `[left, right)` is tracked.
- `removeRange(left, right)`: stop tracking every number in `[left, right)`.

```
addRange(10, 20)
removeRange(14, 16)           → tracked: [10,14) and [16,20)
queryRange(10, 14) → true
queryRange(13, 15) → false    (14 and 15 aren't tracked)
queryRange(16, 17) → true
```

**Half-open** means the left end is included and the right end isn't: `[10,14)` contains 13.999 but not 14. Two ranges like `[1,3)` and `[3,5)` **touch** and together cover `[1,5)`.

---

## 2. How to think about it

### Keep the map clean
Store the tracked set as a `TreeMap<start, end>` with one rule: **intervals never overlap and never touch**. Touching intervals are merged, so `[1,3)` + `[3,5)` is stored as `[1,5)`.

With that rule, a query is **one lookup**: `[l, r)` is fully tracked only if a single interval covers it, and that interval must be the one starting at or before `l`:

```
query(l, r):  s = floorKey(l);   return s exists and end(s) >= r
```

### addRange(l, r): merge
1. If the interval starting at or before `l` reaches `l` (overlaps or touches), start the new interval at its start and stretch `r` to cover its end.
2. Swallow every interval that starts inside `[l, r]` (≤ r, because touching counts), stretching `r` again.
3. Put `[l, r)`.

### removeRange(l, r): split
1. The interval starting **before** `l` may stick into the removed zone: cut it at `l`. If it went past `r` too, keep its tail `[r, end)`.
2. Every interval starting inside `[l, r)`: delete it, but keep its tail `[r, end)` if it goes past `r`.

**The analogy:** painting a fence. Adding paint merges with any wet paint it touches; removing paint means sanding a section away, which can cut one painted stretch into two.

### Dry run
| Operation | Steps | Map |
|---|---|---|
| `addRange(10,20)` | no floor, nothing to swallow | {10:20} |
| `removeRange(14,16)` | `lowerKey(14)` = 10, ends at 20 > 14 → `put(10,14)`; 20 > 16 → `put(16,20)`. `ceilingKey(14)` = 16, not < 16 → stop | {10:14, 16:20} |
| `query(10,14)` | `floorKey(10)` = 10, end 14 ≥ 14 | **true** |
| `query(13,15)` | `floorKey(13)` = 10, end 14 < 15 | **false** |
| `query(16,17)` | `floorKey(16)` = 16, end 20 ≥ 17 | **true** |
| `addRange(14,16)` | `floorKey(14)` = 10, end 14 ≥ 14 → l = 10, r = 16. Swallow 10 (r stays 16), swallow 16 (r = 20) | {10:20} |
| `addRange(20,25)` | `floorKey(20)` = 10, end 20 ≥ 20 (touching) → l = 10, r = 25 | {10:25} |

---

## 3. Java concepts used in this problem

### 3.1 TreeMap navigation, again
`floorKey` (≤), `lowerKey` (<) and `ceilingKey` (≥): see [A05 §3.1](A05_DataStreamAsDisjointIntervals.md#3-java-concepts-used-in-this-problem). The difference between `floorKey` and `lowerKey` matters in `removeRange`: an interval starting **exactly at** `l` is handled by the loop, not by the "sticks in from the left" step.

### 3.2 Assignment inside a `while` condition
```java
Integer k;
while ((k = map.ceilingKey(left)) != null && k <= right) { ... }
```
`(k = map.ceilingKey(left))` assigns **and** gives back the value, so the loop fetches the next key and tests it in one place. The parentheses are required: without them, `!=` would bind first.

### 3.3 Reassigning parameters
`addRange` changes `left` and `right` while merging. In Java, parameters are local copies, so this doesn't affect the caller.

---

## 4. Approach 1: One boolean per unit cell (the referee)

### Code ([A06_RangeModule.java:45-51](A06_RangeModule.java#L45-L51))
```java
static class CellRangeModule {
    private final boolean[] tracked;                     // tracked[x] = is [x, x+1) tracked?
    CellRangeModule(int maxCoordinate) { tracked = new boolean[maxCoordinate]; }
    public void addRange(int left, int right) { for (int x = left; x < right; x++) tracked[x] = true; }
    public boolean queryRange(int left, int right) { for (int x = left; x < right; x++) if (!tracked[x]) return false; return true; }
    public void removeRange(int left, int right) { for (int x = left; x < right; x++) tracked[x] = false; }
}
```

All endpoints are integers, so the real line splits into unit cells `[x, x+1)` that are either fully tracked or not at all. Each operation loops over the cells: **O(range length)**. That's only usable for small coordinates (LeetCode allows up to 10⁹), but it's obviously correct.

---

## 5. Approach 2: TreeMap of intervals (main solution)

### Code ([A06_RangeModule.java:54-89](A06_RangeModule.java#L54-L89))
```java
static class RangeModule {
    private final TreeMap<Integer, Integer> map = new TreeMap<>();   // start -> end (exclusive)

    public void addRange(int left, int right) {
        Integer start = map.floorKey(left);
        if (start != null && map.get(start) >= left) {               // overlaps or touches on the left
            left = start;
            right = Math.max(right, map.get(start));
        }
        Integer k;
        while ((k = map.ceilingKey(left)) != null && k <= right) {    // swallow everything starting in [left, right]
            right = Math.max(right, map.get(k));
            map.remove(k);
        }
        map.put(left, right);
    }

    public boolean queryRange(int left, int right) {
        Integer start = map.floorKey(left);
        return start != null && map.get(start) >= right;
    }

    public void removeRange(int left, int right) {
        Integer before = map.lowerKey(left);
        if (before != null && map.get(before) > left) {              // an interval sticks into [left, ...)
            int end = map.get(before);
            map.put(before, left);                                   // keep the part before left
            if (end > right) map.put(right, end);                    // and the part after right
        }
        Integer k;
        while ((k = map.ceilingKey(left)) != null && k < right) {     // intervals starting inside [left, right)
            int end = map.remove(k);
            if (end > right) map.put(right, end);                    // keep the tail past right
        }
    }
}
```

### Line by line: `addRange`
| Line | What it does |
|---|---|
| `floorKey(left)` + `>= left` | an interval to the left overlaps or touches (`>=`, not `>`: `[5,10)` touches `[10,..)`) |
| `left = start; right = max(...)` | the merged interval starts there and reaches at least as far as that interval did |
| the `while` | every interval starting inside `[left, right]` is swallowed (`k <= right`, so a touching one on the right merges too) |
| `put(left, right)` | the single merged interval |

### Line by line: `removeRange`
| Line | What it does |
|---|---|
| `lowerKey(left)` + `> left` | an interval that starts before `left` and still runs past it |
| `put(before, left)` | keep only its part before `left` |
| `if (end > right) put(right, end)` | it also ran past `right`: keep the tail (the removal punched a hole in the middle) |
| the `while` (`k < right`) | intervals starting inside the removed zone: delete each, keeping a tail past `right` |

### Why the loops stop
- In `addRange`, every iteration **removes** a key and adds nothing; the merged interval is put back only after the loop.
- In `removeRange`, every iteration removes a key and may add the key `right`, which fails the `k < right` test, so the loop ends.

Every interval is created once and removed at most once, so across all calls the loops do amortised O(log n) work per interval.

### Complexity
- `addRange` / `removeRange`: **O((1 + t) log n)** where t = the number of intervals touched (amortised O(log n), since each interval is created once and removed once).
- `queryRange`: **O(log n)**.
- Space: **O(n)** intervals.

---

## 6. Comparing the approaches

| | add / remove | query | Coordinates |
|---|---|---|---|
| Unit cells | O(range length) | O(range length) | small only |
| **TreeMap** | amortised O(log n) | O(log n) | any (up to 10⁹) |

The file runs 50,000 random operations against the cell version and, after each test, checks that the map is clean (every interval non-empty, no overlaps, no touching).

---

## 7. Edge cases and common mistakes

**Edge cases:** adding a range touching an existing one (`[10,20)` + `[20,25)`), removing from the middle (one interval becomes two), removing exactly an interval, removing a range covering several intervals, querying a range that spans a gap.

**Mistakes:**
1. Not merging **touching** intervals: then `query(10, 25)` fails on `[10,20) [20,25)`.
2. Using `floorKey` instead of `lowerKey` in `removeRange` and mishandling an interval starting exactly at `left`.
3. Forgetting the tail `[right, end)` when a removal cuts an interval in the middle.
4. `k < right` vs `k <= right`: adding swallows touching intervals (`<=`), but removing must not delete an interval that starts exactly at `right` (`<`).
5. Querying by scanning several intervals, which is wrong if they touch and slow anyway.

---

## 8. Interview questions and answers

**Q1. Why must the map never contain touching intervals?**
> So that any fully tracked range lies inside a single interval. Then `query` is one `floorKey` lookup instead of a walk.

**Q2. What does remove do to an interval that covers the removed range on both sides?**
> It splits into two: `[start, left)` stays, and `[right, end)` is added.

**Q3. What's the complexity?**
> Each call does O(log n) work plus O(log n) per interval it merges or deletes. Each interval is created once and deleted once, so it's amortised O(log n) per call.

### Follow-up questions

**F1. "Coordinates up to 10⁹ and many overlapping operations: another way?"**
> A dynamic (lazy) segment tree over [0, 10⁹] with "assign 1 / assign 0" lazy tags and a range "all ones?" query: O(log C) per operation.

**F2. "Return the total tracked length."**
> Keep a running total, adjusted on every merge, split and delete.

**F3. "My Calendar I (no double bookings)."**
> The same TreeMap: an event `[s, e)` conflicts if the floor interval ends after `s` or the ceiling interval starts before `e`.

---

## 9. Running the file

```bash
java A06_RangeModule.java     # 6/6 tests passed (example queries, re-merge, touching merge, 50,000 random ops vs cells)
```

**One-line summary:** *"Keep disjoint, non-touching half-open intervals in a TreeMap. Add merges with the floor interval and swallows everything starting inside; remove trims the interval sticking in from the left and deletes those starting inside, keeping any tail past right; query checks whether the floor interval reaches right."*
