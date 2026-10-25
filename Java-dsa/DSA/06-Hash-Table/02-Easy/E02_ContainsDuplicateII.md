# E02 · Contains Duplicate II

| | |
|---|---|
| **LeetCode** | [#219 Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/) |
| **Difficulty** | Easy |
| **Pattern** | A HashMap of the last index / a sliding-window HashSet of size k |
| **Code** | [E02_ContainsDuplicateII.java](E02_ContainsDuplicateII.java) |
| **Run** | `java E02_ContainsDuplicateII.java` |

---

## 1. The problem

Are there two **equal values** whose **indices** are at most k apart (`nums[i] == nums[j]` and `|i − j| ≤ k`)?

| nums | k | answer | why |
|---|---|---|---|
| `[1, 2, 3, 1]` | 3 | true | indices 0 and 3 |
| `[1, 0, 1, 1]` | 1 | true | indices 2 and 3 |
| `[1, 2, 3, 1, 2, 3]` | 2 | false | every copy is 3 apart |
| `[1, 1]` | 0 | false | the distance must be ≤ 0, impossible for i ≠ j |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ k ≤ 10⁵`.

---

## 2. How to think about it

The original Contains Duplicate ([Arrays E02](../../01-Arrays/02-Easy/E02_ContainsDuplicate.md)) asks "is any value repeated?" Now the copies must also be **close**.

### Idea 1: remember where each value was last seen
For each i, the best partner for nums[i] is its **most recent** previous copy (the closest one). So a map `value → last index` is enough: check `i − last ≤ k`, then update `last` to i.

### Idea 2: only the last k values matter
A duplicate within distance k means nums[i] appears among the **previous k elements**. Keep exactly those in a set: a **sliding window** of size k.

```
[1, 2, 3, 1],  k = 3
window before i=3: {1, 2, 3}   → 1 is in it → true
```

Memory O(k) instead of O(n).

---

## 3. Java concepts used in this problem

### 3.1 `put` returns the previous value

```java
Integer j = lastIndex.put(nums[i], i);    // the old index, or null if nums[i] is new
```

That's "look up the old value and store the new one" in **one** hash operation. The result type is `Integer` (not `int`), because it can be `null`.

### 3.2 Checking `null` before using an `Integer`

```java
if (j != null && i - j <= k) return true;
```

`&&` short-circuits, so `i - j` (which unboxes j) only runs when j isn't null. In the other order you'd get a NullPointerException for every new value.

### 3.3 `set.add` returning false = "already present"

```java
if (!window.add(nums[i])) return true;
```

As in [E01](E01_HappyNumber.md): one call both inserts and reports a duplicate.

### 3.4 Removing from a `Set<Integer>`

`window.remove(nums[i - k])`: on a **Set**, `remove(Object)` removes by value; the int is autoboxed. (On a **List**, `remove(int)` would remove by **index**, a trap covered in [E03](E03_DesignHashSet.md).)

---

## 4. Approach 1: Brute force (for reference)

```java
static boolean containsNearbyDuplicateBrute(int[] nums, int k) {
    for (int i = 0; i < nums.length; i++)
        for (int j = i + 1; j < nums.length && j - i <= k; j++)
            if (nums[i] == nums[j]) return true;
    return false;
}
```

Compare each element with the next k. **O(n · min(n, k))**: up to 10¹⁰ steps at the limits.

---

## 5. Approach 2: A map of the last index

### Code ([E02_ContainsDuplicateII.java:40-47](E02_ContainsDuplicateII.java#L40-L47))

```java
static boolean containsNearbyDuplicateMap(int[] nums, int k) {
    Map<Integer, Integer> lastIndex = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        Integer j = lastIndex.put(nums[i], i);           // put returns the previous index (or null)
        if (j != null && i - j <= k) return true;
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<Integer, Integer> lastIndex` | value → the latest index where it appeared |
| `Integer j = lastIndex.put(nums[i], i);` | store i as the latest index, and get the previous one |
| `if (j != null && i - j <= k)` | a previous copy exists and is close enough |
| `return false;` | no close pair anywhere |

**Why always overwrite with the latest index?** For any future copy, the most recent earlier index is the closest one. An older index can never give a smaller distance.

### Dry run: `[1, 0, 1, 1]`, k = 1

| i | nums[i] | `put` returns j | i − j | map after |
|---|---|---|---|---|
| 0 | 1 | null | | {1: 0} |
| 1 | 0 | null | | {1: 0, 0: 1} |
| 2 | 1 | 0 | 2 > 1 | {1: **2**, 0: 1} |
| 3 | 1 | 2 | **1 ≤ 1** → true | |

**true** ✓

### Complexity
- **Time O(n).**
- **Space O(n)** (one entry per distinct value).

---

## 6. Approach 3: A sliding-window set (the file's main solution)

### Code ([E02_ContainsDuplicateII.java:49-56](E02_ContainsDuplicateII.java#L49-L56))

```java
static boolean containsNearbyDuplicate(int[] nums, int k) {
    Set<Integer> window = new HashSet<>();
    for (int i = 0; i < nums.length; i++) {
        if (!window.add(nums[i])) return true;          // already in the last k values
        if (window.size() > k) window.remove(nums[i - k]);
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Set<Integer> window` | the values at indices i−k .. i−1 (the last k elements) |
| `if (!window.add(nums[i])) return true;` | nums[i] is already in the window → a copy within distance k |
| `if (window.size() > k) window.remove(nums[i - k]);` | the window now has k + 1 values; drop the one that's k behind, because the **next** index is k + 1 away from it |
| `return false;` | no close pair |

**Why the set never holds duplicates:** if a value were already in the window we'd have returned true, so every value in the window is unique and `size()` equals the number of elements in it.

### Dry run 1: `[1, 2, 3, 1, 2, 3]`, k = 2

| i | nums[i] | add? | size > 2? → remove nums[i−2] | window after |
|---|---|---|---|---|
| 0 | 1 | new | | {1} |
| 1 | 2 | new | | {1, 2} |
| 2 | 3 | new | 3 > 2 → remove nums[0] = 1 | {2, 3} |
| 3 | 1 | new | remove nums[1] = 2 | {3, 1} |
| 4 | 2 | new | remove nums[2] = 3 | {1, 2} |
| 5 | 3 | new | remove nums[3] = 1 | {2, 3} |

**false** ✓

### Dry run 2: `[1, 0, 1, 1]`, k = 1

| i | nums[i] | add? | remove | window after |
|---|---|---|---|---|
| 0 | 1 | new | | {1} |
| 1 | 0 | new | remove nums[0] = 1 | {0} |
| 2 | 1 | new | remove nums[1] = 0 | {1} |
| 3 | 1 | **already there** → true | | |

### Dry run 3: `[1, 1]`, k = 0
i = 0: add 1 → {1}; size 1 > 0 → remove nums[0] → {}. i = 1: add 1 → new; remove → {}. **false** ✓ (with k = 0 the window is always empty.)

### Complexity
- **Time O(n).**
- **Space O(min(n, k)).**

---

## 7. Comparing the approaches

| | Brute force | Last-index map | Window set |
|---|---|---|---|
| Time | O(n · k) | O(n) | O(n) |
| Space | O(1) | O(n) | **O(min(n, k))** |

The file's tests check that the map and the window agree on 1,000 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0 (always false), k ≥ n (just "any duplicate?"), a single element, all elements equal.

**Mistakes:**
1. Keeping the **first** index in the map instead of the latest.
2. An off-by-one in the window: removing when `size() >= k` (then the window holds only k − 1 values).
3. Unboxing a `null` `Integer` (`int j = map.get(x)`).
4. Using `Math.abs(i - j)` and still keeping the first index. Not wrong, but it misses closer copies.

---

## 9. Interview questions and answers

**Q1. Map or set: which is better?**
> Both are O(n) time. The window set uses O(k) memory instead of O(n), which matters when k is much smaller than n. The map version is a little easier to explain.

**Q2. Why is the latest index always the right one to keep?**
> Future indices are larger than every stored one, so the closest earlier copy is the most recent. Older copies can only be further away.

### Follow-up questions

**F1. Contains Duplicate III: the values may differ by up to t (|nums[i] − nums[j]| ≤ t).** → [H01](../04-Hard/H01_ContainsDuplicateIII.md)
> A sliding window of **buckets** of width t + 1 (or a `TreeSet` with `floor`/`ceiling`).

**F2. Return the pair of indices.**
> In the map version, return `{j, i}` instead of `true`.

**F3. Count the close duplicate pairs.**
> Keep, per value, a queue of indices within the last k; add its size each time the value appears.

---

## 10. Running the file

```bash
java E02_ContainsDuplicateII.java      # 5/5 tests passed (including window set == last-index map, 1,000 cases)
```

**One-line summary:** *"Keep only the last k values in a HashSet; a failed `add` means a duplicate within distance k."*
