# M03 · Snapshot Array ⭐

| | |
|---|---|
| **LeetCode** | [#1146 Snapshot Array](https://leetcode.com/problems/snapshot-array/) |
| **Difficulty** | Medium |
| **Pattern** | Store only the **changes** per index (version lists) + binary search |
| **Code** | [M03_SnapshotArray.java](M03_SnapshotArray.java) |
| **Run** | `java M03_SnapshotArray.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Design `SnapshotArray(length)`, an array that starts as all zeros, with:
- `set(index, val)`;
- `snap()`, which takes a snapshot and returns its id (0, 1, 2, …);
- `get(index, snap_id)`, the value at `index` when snapshot `snap_id` was taken.

```
set(0, 5); snap() = 0; set(0, 6); get(0, 0) = 5
```

---

## 2. How to think about it

### The naive way: copy everything on each snap
That's O(length) per `snap`. With 5·10⁴ snaps of a 5·10⁴-element array, it's 2.5·10⁹ copies.

### Store only what changed
For each index, keep a list of `(snapId, value)` pairs: the value the index had **as of** each snapshot in which it changed.

```
set(i, v):  if the last entry of index i has the CURRENT snapId, overwrite it
            otherwise append (snapId, v)
snap():     return snapId++                      ← O(1), copies nothing
get(i, s):  binary search index i's list for the last entry with snapId ≤ s
```

Every list starts with `(0, 0)`, so a `get` always finds an entry.

This is the same idea as [M02](M02_TimeBasedKeyValueStore.md) and as copy-on-write / persistent data structures (git, database MVCC): share everything that didn't change.

### Dry run: length 3

| op | snapId before | index 0's list after | result |
|---|---|---|---|
| (start) | 0 | [(0,0)] | |
| set(0, 5) | 0 | last id 0 == 0 → overwrite → [(0,5)] | |
| snap() | 0 → 1 | | 0 |
| set(0, 6) | 1 | last id 0 ≠ 1 → append → [(0,5), (1,6)] | |
| get(0, 0) | | search for the last id ≤ 0 → (0,5) | 5 |
| get(1, 0) | | index 1 is still [(0,0)] | 0 |

The binary search for get(0, 0): lo 0, hi 1 → mid = 1, id 1 > 0 → hi 0 → stop, return entry 0's value 5.

---

## 3. Java concepts used in this problem

### 3.1 A list of lists of `int[]`

```java
private final List<List<int[]>> history = new ArrayList<>();   // per index: {snapId, value}
```

`history.get(i)` is index i's version list, and each version is a two-element `int[]` `{snapId, value}`. Using `int[]` avoids boxing both numbers.

### 3.2 Changing an array inside a list

```java
int[] last = h.get(h.size() - 1);
last[1] = val;                         // changes the array that's stored in the list
```

`h.get(...)` returns a **reference** to the stored array, not a copy, so writing `last[1]` updates the list's contents directly.

### 3.3 Post-increment as a return value

```java
public int snap() { return snapId++; }
```

`snapId++` evaluates to the **old** value and then increments. So the first call returns 0 and leaves `snapId = 1`.

### 3.4 Binary search with an upper midpoint

```java
while (lo < hi) {
    int mid = (lo + hi + 1) >>> 1;          // round UP
    if (h.get(mid)[0] <= snap) lo = mid; else hi = mid - 1;
}
return h.get(lo)[1];
```

This finds the last position whose id is ≤ `snap`, without a separate `ans` variable. Because the `true` branch sets `lo = mid`, `mid` must round **up**: with `lo = 3, hi = 4`, a rounded-down mid would be 3, and `lo = mid` would loop forever. Compare it with the `ans` style in [M02](M02_TimeBasedKeyValueStore.md#5-approach-2-appended-lists--binary-search-the-files-main-solution).

### 3.5 Arrow-style `switch` (in the test)

```java
switch (rnd.nextInt(3)) {
    case 0 -> { ... }
    case 1 -> { ... }
    default -> { ... }
}
```

Java 14+ arrow cases don't fall through, so no `break` is needed between them. (The `break` inside `default` just leaves the switch early.)

---

## 4. Approach 1: Copy the whole array on every snap (the referee)

The test keeps `cur` (the live array) and appends `cur.clone()` to `copies` on every `snap` ([M03_SnapshotArray.java:82-93](M03_SnapshotArray.java#L82-L93)). Then `get(idx, s)` is just `copies.get(s)[idx]`.

- **set O(1), snap O(length), get O(1).**
- **Space O(length · snaps).** Correct, and too slow and too big for the real limits.

---

## 5. Approach 2: Per-index version lists (the file's main solution)

### Code ([M03_SnapshotArray.java:38-68](M03_SnapshotArray.java#L38-L68))

```java
static class SnapshotArray {
    private final List<List<int[]>> history = new ArrayList<>();   // per index: {snapId, value}
    private int snapId = 0;

    public SnapshotArray(int length) {
        for (int i = 0; i < length; i++) {
            List<int[]> h = new ArrayList<>();
            h.add(new int[]{0, 0});                                // everything starts at 0
            history.add(h);
        }
    }

    public void set(int index, int val) {
        List<int[]> h = history.get(index);
        int[] last = h.get(h.size() - 1);
        if (last[0] == snapId) last[1] = val;                      // still in the same snapshot: overwrite
        else h.add(new int[]{snapId, val});
    }

    public int snap() { return snapId++; }

    public int get(int index, int snap) {
        List<int[]> h = history.get(index);
        int lo = 0, hi = h.size() - 1;
        while (lo < hi) {                                          // last entry with id <= snap
            int mid = (lo + hi + 1) >>> 1;
            if (h.get(mid)[0] <= snap) lo = mid; else hi = mid - 1;
        }
        return h.get(lo)[1];
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `snapId` | the id the **next** `snap()` will return; changes made now belong to it |
| the constructor | every index starts with the version (0, 0) |
| `set`: same snapId | several sets before one snap: only the last value matters, so overwrite |
| `set`: new snapId | the first change since the last snap: append a new version |
| `snap` | hand out the id and move on; nothing is copied |
| `get` | the last version recorded at or before snapshot `snap` |

Each index's list has strictly increasing ids, because `set` only ever appends the current id, and `snapId` only grows.

### Complexity
- **set O(1), snap O(1), get O(log v)** for v versions of that index.
- **Space O(length + number of sets).**

---

## 6. Comparing the approaches

| | Full copy per snap | Version lists |
|---|---|---|
| snap | O(length) | **O(1)** |
| get | O(1) | O(log v) |
| Space | O(length · snaps) | **O(length + sets)** |

The file runs 50,000 random operations (across 50 arrays) against full copies and checks every `get` and every snap id.

---

## 7. Edge cases and common mistakes

**Edge cases:** `get` on an index that was never set (0), several sets between two snaps, many snaps with no sets in between, a `get` for the very latest snapshot.

**Mistakes:**
1. Appending on every `set` without the overwrite (it's still correct, but the lists grow with every set instead of every snap that had a change).
2. Recording a set with id `snapId − 1` (the snapshot that was already taken must not change).
3. A rounded-down mid with `lo = mid` (an infinite loop).
4. Lazily creating the lists without the initial `(0, 0)` and then searching an empty list.

---

## 8. Interview questions and answers

**Q1. Why is a set recorded under the current `snapId`, not the last one?**
> Snapshot `snapId − 1` is already taken and frozen. Changes made now become visible starting with the next snapshot, which will get id `snapId`.

**Q2. Why is overwriting safe?**
> If the last entry has the current `snapId`, no snapshot has seen it yet, so only the final value before the next `snap` matters.

**Q3. Where is this idea used in real systems?**
> Persistent data structures and copy-on-write: git stores only changed files per commit, databases keep row versions for snapshot reads (MVCC), and file systems like ZFS snapshot by sharing unchanged blocks.

### Follow-up questions

**F1. Use a `TreeMap<Integer, Integer>` per index instead.**
> `put(snapId, val)` and `floorEntry(snap)`. Same complexity, with more memory per entry.

**F2. Snapshots of a whole map, not an array.**
> The same per-key version lists, created lazily with `computeIfAbsent`.

**F3. Time Based Key-Value Store.** → [M02](M02_TimeBasedKeyValueStore.md)

---

## 9. Running the file

```bash
java M03_SnapshotArray.java      # 4/4 tests passed (including 50,000 random ops vs full copies)
```

**One-line summary:** *"Each index keeps (snapId, value) versions that are only appended or overwritten for the current snapId, snap just returns snapId++, and get binary-searches for the last version at or before the requested snapshot."*
