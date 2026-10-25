# E06 · Unique Number of Occurrences ⭐

| | |
|---|---|
| **LeetCode** | [#1207 Unique Number of Occurrences](https://leetcode.com/problems/unique-number-of-occurrences/) |
| **Difficulty** | Easy |
| **Pattern** | A frequency map, then a set of the frequencies (two layers of hashing) |
| **Code** | [E06_UniqueNumberOfOccurrences.java](E06_UniqueNumberOfOccurrences.java) |
| **Run** | `java E06_UniqueNumberOfOccurrences.java` |

⭐ = not in `leetcode.md`, added for a key technique.

---

## 1. The problem

Is the **number of occurrences** of each value unique, i.e. do no two values occur the same number of times?

| arr | answer | counts |
|---|---|---|
| `[1, 2, 2, 1, 1, 3]` | true | 1 → 3, 2 → 2, 3 → 1 (all different) |
| `[1, 2]` | false | 1 → 1, 2 → 1 (both once) |
| `[-3, 0, 1, -3, 1, 1, 1, -3, 10, 0]` | true | −3 → 3, 0 → 2, 1 → 4, 10 → 1 |
| `[5]` | true | |

**Constraints:** `1 ≤ n ≤ 1000`, `−1000 ≤ arr[i] ≤ 1000`.

---

## 2. How to think about it

Two questions, each answered with hashing:
1. **Count:** how often does each value occur? → `HashMap<value, frequency>`
2. **Check:** are those frequencies distinct? → put them in a `HashSet`; `add` returns false on a duplicate frequency.

```
[1, 2, 2, 1, 1, 3]
step 1: counts = {1: 3, 2: 2, 3: 1}
step 2: frequencies 3, 2, 1 → set {3, 2, 1}, no duplicate → true
```

"Count, then do something with the counts" is one of the most common shapes in this topic (anagrams, top-k, majority, …).

---

## 3. Java concepts used in this problem

### 3.1 Counting with `merge`

```java
counts.merge(x, 1, Integer::sum);
```

If x is absent, store 1. Otherwise store `Integer.sum(old, 1)`, i.e. old + 1. `Integer::sum` is a **method reference**: shorthand for the lambda `(a, b) -> a + b`. The longer equivalent:

```java
counts.put(x, counts.getOrDefault(x, 0) + 1);
```

### 3.2 `map.values()`

A **view** of all the values (here, the frequencies), usable in a for-each loop. It is not a copy: it reflects the map. Duplicate values appear as many times as they occur.

### 3.3 Building a set from a collection

```java
new HashSet<>(counts.values()).size() == counts.size()
```

The `HashSet` constructor copies a collection, dropping duplicates. If no frequency repeats, the set is as big as the map. It's a neat one-liner, but the loop version can stop early and is easier to explain.

### 3.4 Small value ranges → arrays instead of maps

Values lie in [−1000, 1000], so `int[] count = new int[2001]` with `count[x + 1000]++` also works, with no boxing. The **offset** shifts negative values into valid indices.

---

## 4. Approach 1: Sort, then count the runs

```java
static boolean uniqueOccurrencesSort(int[] arr) {
    int[] a = arr.clone();
    Arrays.sort(a);
    Set<Integer> freqs = new HashSet<>();
    int i = 0;
    while (i < a.length) {
        int j = i;
        while (j < a.length && a[j] == a[i]) j++;     // a[i..j-1] is one run of equal values
        if (!freqs.add(j - i)) return false;
        i = j;
    }
    return true;
}
```

After sorting, equal values sit together, so each run's length is that value's frequency. **O(n log n)**, with no map needed.

---

## 5. Approach 2: An array counter (small range)

```java
static boolean uniqueOccurrencesArray(int[] arr) {
    int[] count = new int[2001];
    for (int x : arr) count[x + 1000]++;              // shift -1000..1000 into 0..2000
    boolean[] seen = new boolean[arr.length + 1];     // a frequency is at most n
    for (int c : count) {
        if (c == 0) continue;
        if (seen[c]) return false;
        seen[c] = true;
    }
    return true;
}
```

Both layers become arrays, which works because both the values and the frequencies have small known ranges. O(n + range).

---

## 6. Approach 3: A HashMap then a HashSet (the file's solution)

### Code ([E06_UniqueNumberOfOccurrences.java:34-42](E06_UniqueNumberOfOccurrences.java#L34-L42))

```java
static boolean uniqueOccurrences(int[] arr) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int x : arr) counts.merge(x, 1, Integer::sum);
    Set<Integer> seenFrequencies = new HashSet<>();
    for (int f : counts.values()) {
        if (!seenFrequencies.add(f)) return false;      // two values share this frequency
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `counts.merge(x, 1, Integer::sum);` | count each value |
| `for (int f : counts.values())` | every frequency (unboxed from Integer to int) |
| `if (!seenFrequencies.add(f)) return false;` | this frequency has already been seen → two values share it |
| `return true;` | all the frequencies are distinct |

### Dry run 1: `[1, 2, 2, 1, 1, 3]`

| x | counts after |
|---|---|
| 1 | {1: 1} |
| 2 | {1: 1, 2: 1} |
| 2 | {1: 1, 2: 2} |
| 1 | {1: 2, 2: 2} |
| 1 | {1: 3, 2: 2} |
| 3 | {1: 3, 2: 2, 3: 1} |

| f | add? | set after |
|---|---|---|
| 3 | new | {3} |
| 2 | new | {3, 2} |
| 1 | new | {3, 2, 1} |

**true** ✓

### Dry run 2: `[1, 2]`
counts = {1: 1, 2: 1}. f = 1 → new; f = 1 → **already there** → **false** ✓

### Complexity
- **Time O(n)** on average.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Sort + runs | Arrays | HashMap + HashSet |
|---|---|---|---|
| Time | O(n log n) | O(n + range) | O(n) |
| Space | O(n) for the copy | O(range) | O(n) |
| Works for any values? | yes | only small ranges | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** one element, all values equal (one frequency → true), all distinct with n ≥ 2 (every frequency is 1 → false), negative values.

**Mistakes:**
1. Comparing the frequencies pairwise (O(k²)) instead of using a set.
2. A negative array index when using an array counter without an offset.
3. Confusing "the values are unique" with "the frequencies are unique".

---

## 9. Interview questions and answers

**Q1. Why two hashing steps?**
> The first turns the array into frequencies; the second answers a uniqueness question about those frequencies. Each is O(n), and the second is just Contains Duplicate on the counts.

**Q2. When would you avoid a HashMap here?**
> When the value range is small and known, an `int[]` counter is faster and avoids boxing.

### Follow-up questions

**F1. Return the values whose frequency is shared with another value.**
> Group by frequency: a map of frequency → list of values; return the lists with size > 1.

**F2. The minimum deletions to make the frequencies unique (LC 1647).**
> Sort the frequencies in descending order; lower each one to at most (the previous one − 1), never below 0, and add up the reductions. O(n + k log k).

**F3. Sort the characters by frequency (LC 451).**
> Count, then bucket by frequency, the same two-layer idea.

---

## 10. Running the file

```bash
java E06_UniqueNumberOfOccurrences.java      # 4/4 tests passed
```

**One-line summary:** *"Count with `merge`, then feed the counts into a HashSet; a failed `add` means two values share a frequency."*
