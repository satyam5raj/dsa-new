# M02 · Time Based Key-Value Store ⭐

| | |
|---|---|
| **LeetCode** | [#981 Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) |
| **Difficulty** | Medium |
| **Pattern** | HashMap<key, list of (time, value)> + **binary search** for "the latest time ≤ t" |
| **Code** | [M02_TimeBasedKeyValueStore.java](M02_TimeBasedKeyValueStore.java) |
| **Run** | `java M02_TimeBasedKeyValueStore.java` |

⭐ = not in `leetcode.md`, added for a key technique. Design-problem basics are explained in [E01](../02-Easy/E01_LoggerRateLimiter.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Design a `TimeMap`:
- `set(key, value, timestamp)` stores a value for the key at that time;
- `get(key, timestamp)` returns the value set at the **largest** stored timestamp **≤** `timestamp`, or `""` if there isn't one.

The timestamps passed to `set` are **strictly increasing**.

| call | result |
|---|---|
| set(foo, bar, 1) | |
| get(foo, 1) | bar |
| get(foo, 3) | bar (the latest version at or before 3 is the one from time 1) |
| set(foo, bar2, 4) | |
| get(foo, 4) | bar2 |
| get(foo, 5) | bar2 |
| get(foo, 0) | "" (nothing existed yet) |

---

## 2. How to think about it

### A versioned map
Each key has a **history** of values. `get` asks "what was the value at time t?". That's how databases keep old versions (MVCC) and how git stores a file's history.

### Sorted for free
Since `set` timestamps only increase, appending each new version to a per-key `ArrayList` keeps that list **already sorted**. So `get` is a binary search for the **rightmost** time ≤ t (the "floor"):

```
lo = 0, hi = size − 1, ans = −1
while lo ≤ hi:
    mid = (lo + hi) / 2
    if time[mid] ≤ t:  ans = mid, lo = mid + 1      (a candidate; look for a later one)
    else:              hi = mid − 1                 (too late; look left)
```

### The alternative: `TreeMap.floorEntry`
A `TreeMap<Integer, String>` per key with `floorEntry(t)` does the same in O(log n) with less code. It uses more memory per entry, but it also works if the timestamps arrive out of order.

### Dry run: the binary search on foo's history, times = [1, 4]

| get | steps | ans | result |
|---|---|---|---|
| (foo, 3) | mid 0: 1 ≤ 3 → ans 0, lo 1; mid 1: 4 > 3 → hi 0; stop | 0 | bar |
| (foo, 0) | mid 0: 1 > 0 → hi −1; stop | −1 | "" |
| (foo, 5) | mid 0: 1 ≤ 5 → ans 0, lo 1; mid 1: 4 ≤ 5 → ans 1, lo 2; stop | 1 | bar2 |

---

## 3. Java concepts used in this problem

### 3.1 `computeIfAbsent` to create a list on first use

```java
times.computeIfAbsent(key, k -> new ArrayList<>()).add(timestamp);
```

If `key` has no list yet, the lambda creates one and stores it. Either way it returns the list, so `.add(...)` can be chained. This replaces the classic "get, check for null, put" sequence.

### 3.2 Two parallel lists

```java
Map<String, List<Integer>> times;      // times.get(k).get(i)  is the i-th version's time
Map<String, List<String>>  values;     // values.get(k).get(i) is the i-th version's value
```

The same position `i` in both lists is one version. This avoids creating a small pair object for every `set`.

### 3.3 `(lo + hi) >>> 1`

`>>>` is the **unsigned** right shift. `(lo + hi) >>> 1` halves the sum correctly even if `lo + hi` overflows past `Integer.MAX_VALUE` (it's treated as an unsigned number). It's a common, safe way to write the midpoint; `lo + (hi − lo) / 2` is another.

### 3.4 `TreeMap.floorEntry`

```java
Map.Entry<Integer, String> e = tm.floorEntry(t);    // the entry with the largest key ≤ t, or null
return e == null ? "" : e.getValue();
```

A `TreeMap` keeps its keys sorted (it's a red-black tree). It also has `ceilingEntry` (≥), `lowerEntry` (<) and `higherEntry` (>), each O(log n).

### 3.5 Comparing strings with `.equals`

The test uses `a.get(k, t).equals(b.get(k, t))`. `==` on `String`s compares references; `.equals` compares the characters.

---

## 4. Approach 1: `TreeMap` per key (the referee)

### Code ([M02_TimeBasedKeyValueStore.java:57-66](M02_TimeBasedKeyValueStore.java#L57-L66))

```java
static class TreeMapVersion {
    private final Map<String, TreeMap<Integer, String>> m = new HashMap<>();
    void set(String k, String v, int t) { m.computeIfAbsent(k, x -> new TreeMap<>()).put(t, v); }
    String get(String k, int t) {
        TreeMap<Integer, String> tm = m.get(k);
        if (tm == null) return "";
        Map.Entry<Integer, String> e = tm.floorEntry(t);
        return e == null ? "" : e.getValue();
    }
}
```

| Line | What it does |
|---|---|
| `m` | key → its versions, sorted by time |
| `set` | store the version (O(log n)) |
| `get`: unknown key | `""` |
| `floorEntry(t)` | the latest version at or before t |

- **set O(log n), get O(log n).**

---

## 5. Approach 2: Appended lists + binary search (the file's main solution)

### Code ([M02_TimeBasedKeyValueStore.java:35-55](M02_TimeBasedKeyValueStore.java#L35-L55))

```java
static class TimeMap {
    private final Map<String, List<Integer>> times = new HashMap<>();
    private final Map<String, List<String>> values = new HashMap<>();

    public void set(String key, String value, int timestamp) {
        times.computeIfAbsent(key, k -> new ArrayList<>()).add(timestamp);   // appended in sorted order
        values.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public String get(String key, int timestamp) {
        List<Integer> ts = times.get(key);
        if (ts == null) return "";
        int lo = 0, hi = ts.size() - 1, ans = -1;
        while (lo <= hi) {                                   // rightmost time <= timestamp
            int mid = (lo + hi) >>> 1;
            if (ts.get(mid) <= timestamp) { ans = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        return ans == -1 ? "" : values.get(key).get(ans);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `set` | append the time and the value at the same position |
| `ts == null` | a key that was never set |
| `ans = −1` | "no version found yet" |
| `ts.get(mid) <= timestamp` | `mid` is a valid version; remember it and look right for a later one |
| `else hi = mid − 1` | `mid` is in the future; look left |
| the return | the value at the found position, or `""` |

`ts.get(mid) <= timestamp` compares an `Integer` with an `int`, so Java unboxes it and compares the values.

### Complexity
- **set O(1)** amortised (appending to an `ArrayList`).
- **get O(log n)** for n versions of that key.
- **Space O(total sets).**

---

## 6. Comparing the approaches

| | TreeMap per key | Sorted lists + binary search |
|---|---|---|
| set | O(log n) | **O(1)** amortised |
| get | O(log n) | O(log n) |
| Memory | a tree node per version | two array slots per version |
| Unsorted timestamps? | works | breaks (the lists wouldn't be sorted) |

The file runs 50,000 random operations through both and checks every `get` matches.

---

## 7. Edge cases and common mistakes

**Edge cases:** a `get` before the first version (`""`), an unknown key, a `get` exactly at a stored time, a `get` far in the future (the latest version).

**Mistakes:**
1. Binary-searching for an exact match instead of the floor.
2. Returning `values.get(key).get(lo)` after the loop without tracking `ans` (`lo` ends one past the answer).
3. Using a `TreeMap` when the problem guarantees sorted input, and not mentioning the cheaper option.
4. Comparing strings with `==`.

---

## 8. Interview questions and answers

**Q1. Why is the list already sorted?**
> The problem says `set` timestamps strictly increase, so every append lands at the end in order.

**Q2. What if the timestamps weren't increasing?**
> Use a `TreeMap` per key (or insert into the list at the binary-searched position, which costs O(n) per insert).

**Q3. What does `floorEntry` do?**
> It returns the entry with the greatest key less than or equal to the argument, or `null` if there isn't one, in O(log n).

### Follow-up questions

**F1. Delete old versions to save memory.**
> Keep only the versions any reader might still need (for example, those newer than the oldest active reader's time), which is what MVCC databases do with garbage collection.

**F2. Snapshot Array.** → [M03](M03_SnapshotArray.md)
> The same "versions + floor search" idea, per array index.

**F3. Get all values in a time range.**
> Binary search for both ends (or use `TreeMap.subMap(from, to)`) and return the slice.

---

## 9. Running the file

```bash
java M02_TimeBasedKeyValueStore.java      # 7/7 tests passed (including 50,000 random ops: binary search == TreeMap)
```

**One-line summary:** *"Append each key's (time, value) versions to lists that stay sorted, and answer get with a binary search for the rightmost time ≤ t."*
