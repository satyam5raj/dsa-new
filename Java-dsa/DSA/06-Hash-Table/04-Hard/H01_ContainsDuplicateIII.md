# H01 · Contains Duplicate III

| | |
|---|---|
| **LeetCode** | [#220 Contains Duplicate III](https://leetcode.com/problems/contains-duplicate-iii/) |
| **Difficulty** | Hard |
| **Pattern** | Sliding window + value buckets of width t + 1 (hashing by **range**) |
| **Code** | [H01_ContainsDuplicateIII.java](H01_ContainsDuplicateIII.java) |
| **Run** | `java H01_ContainsDuplicateIII.java` |

---

## 1. The problem

Find indices i ≠ j with **both**

```
|i − j| ≤ indexDiff   (call it k)
|nums[i] − nums[j]| ≤ valueDiff   (call it t)
```

| nums | k | t | answer |
|---|---|---|---|
| `[1, 2, 3, 1]` | 3 | 0 | true (the two 1s) |
| `[1, 5, 9, 1, 5, 9]` | 2 | 3 | false |
| `[-1, 1]` | 1 | 1 | false (they differ by 2) |
| `[MIN_VALUE, MAX_VALUE]` | 1 | 1 | false (watch out for overflow) |

**Constraints:** `2 ≤ n ≤ 10⁵`, values in the full int range, `1 ≤ k ≤ n`, `0 ≤ t ≤ 10⁹`.

---

## 2. How to think about it

This is [E02 Contains Duplicate II](../02-Easy/E02_ContainsDuplicateII.md) (the same window of the last k elements), but now values only need to be **close**, not equal. A HashSet can answer "is x present?", but not "is anything within t of x present?"

### Option 1: a sorted window (TreeSet), O(n log k)
Keep the last k values in a `TreeSet`. For x, ask for the **smallest value ≥ x − t** (`ceiling`). If it's ≤ x + t, it's within range.

### Option 2: buckets (hashing by value range), O(n)
Split the number line into **buckets of width w = t + 1**:

```
t = 3, w = 4:
   bucket −1        bucket 0        bucket 1        bucket 2
 [−4 … −1]        [0 … 3]         [4 … 7]         [8 … 11]
                    id = floor(x / w)
```

- Two values in the **same** bucket differ by at most w − 1 = t → **an immediate match**.
- A match can also be in a **neighbour** bucket (id − 1 or id + 1), so check its value.
- Anything two or more buckets away differs by more than t, so it can't match.
- **Each bucket holds at most one value**: a second value in the same bucket would already have matched. So a map of `bucketId → value` is enough.
- Keep only the last k elements in the map: when nums[i − k] leaves the window, remove its bucket.

### Two traps
- **Negative numbers:** Java's `/` rounds **toward zero** (`-1 / 2 == 0`), which would put −1 and 1 in the same bucket. `Math.floorDiv(x, w)` rounds down (−1 → bucket −1).
- **Overflow:** `x − left` with values near ±2³¹ overflows `int`, so use `long` throughout.

---

## 3. Java concepts used in this problem

### 3.1 `Math.floorDiv` vs `/`

```java
-1 / 2              →  0     (rounds toward zero)
Math.floorDiv(-1, 2) → -1    (rounds down, toward −∞)
 7 / 2              →  3
Math.floorDiv(7, 2)  →  3    (the same for non-negative numbers)
```

The companion `Math.floorMod` was used in [M03](../03-Medium/M03_ArrayPairsDivisibleByK.md). The pair floorDiv/floorMod always satisfies `x == floorDiv(x, w) * w + floorMod(x, w)`.

### 3.2 `long` everywhere

```java
long w = (long) valueDiff + 1;      // valueDiff + 1 itself is fine, but long keeps all the math in 64 bits
long x = nums[i];                   // widening int → long is automatic
```

`Map<Long, Long>` uses boxed `Long` keys. **Careful:** `buckets.get(id - 1)` with a `long` argument boxes to `Long`, which matches. Mixing in an `Integer` key would never match a `Long` key, because `Integer.valueOf(1).equals(Long.valueOf(1))` is **false**.

### 3.3 `TreeSet` and its navigation methods

A `TreeSet` keeps its elements **sorted** (it's a red-black tree), with O(log n) add, remove and contains, plus searches by order:

| Method | Returns |
|---|---|
| `ceiling(x)` | the smallest element ≥ x, or null |
| `floor(x)` | the largest element ≤ x, or null |
| `higher(x)` / `lower(x)` | strictly greater / strictly less |
| `first()` / `last()` | the min / the max |

That's the main reason to pick a `TreeSet` over a `HashSet`: questions about **order** ("what's the nearest value?").

---

## 4. Approach 1: Brute force

### Code ([H01_ContainsDuplicateIII.java:69-74](H01_ContainsDuplicateIII.java#L69-L74))

```java
static boolean brute(int[] a, int k, int t) {
    for (int i = 0; i < a.length; i++)
        for (int j = i + 1; j < a.length && j - i <= k; j++)
            if (Math.abs((long) a[i] - a[j]) <= t) return true;
    return false;
}
```

Compare each element with the next k. The `(long)` cast prevents overflow in the subtraction. **O(n · k).**

---

## 5. Approach 2: A sliding window TreeSet

### Code ([H01_ContainsDuplicateIII.java:58-67](H01_ContainsDuplicateIII.java#L58-L67))

```java
static boolean withTreeSet(int[] nums, int k, int t) {
    TreeSet<Long> window = new TreeSet<>();
    for (int i = 0; i < nums.length; i++) {
        Long c = window.ceiling((long) nums[i] - t);
        if (c != null && c <= (long) nums[i] + t) return true;
        window.add((long) nums[i]);
        if (i >= k) window.remove((long) nums[i - k]);
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `window.ceiling(x - t)` | the smallest value in the window that is ≥ x − t |
| `c <= x + t` | it's also ≤ x + t → within t of x |
| `window.add(x)` | x joins the window |
| `if (i >= k) window.remove(nums[i - k])` | keep only the last k values (the indices i − k + 1 .. i) |

A duplicate value in the window? If x were already present, `ceiling(x − t)` would find a value ≤ x and we'd have returned true, so a set (no duplicates) is safe.

### Dry run: `[1, 5, 9, 1, 5, 9]`, k = 2, t = 3

| i | x | window before | ceiling(x − 3) | ≤ x + 3? | window after |
|---|---|---|---|---|---|
| 0 | 1 | {} | null | | {1} |
| 1 | 5 | {1} | ceiling(2) = null | | {1, 5} |
| 2 | 9 | {1, 5} | ceiling(6) = null | | {5, 9} (remove 1) |
| 3 | 1 | {5, 9} | ceiling(−2) = 5 | 5 ≤ 4? no | {9, 1} (remove 5) |
| 4 | 5 | {1, 9} | ceiling(2) = 9 | 9 ≤ 8? no | {1, 5} (remove 9) |
| 5 | 9 | {1, 5} | ceiling(6) = null | | |

**false** ✓

### Complexity
- **Time O(n log k).**
- **Space O(k).**

---

## 6. Approach 3: Buckets of width t + 1 (the file's solution)

### Code ([H01_ContainsDuplicateIII.java:42-56](H01_ContainsDuplicateIII.java#L42-L56))

```java
static boolean containsNearbyAlmostDuplicate(int[] nums, int indexDiff, int valueDiff) {
    long w = (long) valueDiff + 1;
    Map<Long, Long> buckets = new HashMap<>();             // bucket id -> the value in it
    for (int i = 0; i < nums.length; i++) {
        long x = nums[i];
        long id = Math.floorDiv(x, w);
        if (buckets.containsKey(id)) return true;           // same bucket -> within valueDiff
        Long left = buckets.get(id - 1), right = buckets.get(id + 1);
        if (left != null && x - left <= valueDiff) return true;
        if (right != null && right - x <= valueDiff) return true;
        buckets.put(id, x);
        if (i >= indexDiff) buckets.remove(Math.floorDiv((long) nums[i - indexDiff], w));   // slide the window
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `long w = valueDiff + 1` | the bucket width; values in one bucket differ by ≤ valueDiff |
| `long id = Math.floorDiv(x, w)` | x's bucket (a correct floor for negatives too) |
| `if (buckets.containsKey(id))` | another window value shares the bucket → a match |
| `left` / `right` | the single values in the neighbouring buckets (if any) |
| `x - left <= valueDiff` | the left neighbour is close enough (left < x, so no abs is needed) |
| `right - x <= valueDiff` | likewise for the right neighbour |
| `buckets.put(id, x)` | x occupies its bucket |
| `buckets.remove(…nums[i − indexDiff]…)` | the element leaving the window frees its bucket |

### Dry run 1: `[1, 5, 9, 1, 5, 9]`, k = 2, t = 3 (w = 4)

| i | x | id | same? | left / right values | buckets after (id → value) |
|---|---|---|---|---|---|
| 0 | 1 | 0 | | — / — | {0: 1} |
| 1 | 5 | 1 | | left 1: 5 − 1 = 4 > 3 | {0: 1, 1: 5} |
| 2 | 9 | 2 | | left 5: 9 − 5 = 4 > 3 | {1: 5, 2: 9} (evict nums[0] → bucket 0) |
| 3 | 1 | 0 | | right 5: 5 − 1 = 4 > 3 | {2: 9, 0: 1} (evict nums[1] → bucket 1) |
| 4 | 5 | 1 | | left 1: 4 > 3; right 9: 4 > 3 | {0: 1, 1: 5} (evict nums[2] → bucket 2) |
| 5 | 9 | 2 | | left 5: 4 > 3 | … |

**false** ✓. Every neighbour pair differs by exactly 4 = t + 1.

### Dry run 2: the same array with k = 3
At i = 3 the window still holds indices 0..2, so bucket 0 contains the first 1 → **same bucket → true**.

### Dry run 3: `[-1, 1]`, k = 1, t = 1 (w = 2): why floorDiv matters

| i | x | floorDiv id | (with `/`) | result |
|---|---|---|---|---|
| 0 | −1 | **−1** | 0 | {−1: −1} |
| 1 | 1 | 0 | 0 | left = −1: 1 − (−1) = 2 > 1 → no match |

**false** ✓. With `/`, both would land in bucket 0 and the code would wrongly return true.

### Dry run 4: `[1, 2, 3, 1]`, k = 3, t = 0 (w = 1)
With w = 1 each value is its own bucket. At i = 3, x = 1 → bucket 1 is still occupied → **true** ✓.

(These traces were produced by running the code.)

### Complexity
- **Time O(n):** a constant number of hash operations per element.
- **Space O(min(n, k)).**

---

## 7. Comparing the approaches

| | Brute force | TreeSet window | Buckets |
|---|---|---|---|
| Time | O(n · k) | O(n log k) | **O(n)** |
| Space | O(1) | O(k) | O(k) |
| Trickiness | low | low | floorDiv + overflow |

The file checks both the buckets and the TreeSet against brute force on 2,000 random cases (with negatives).

---

## 8. Edge cases and common mistakes

**Edge cases:** t = 0 (exact duplicates; bucket width 1), negative values, values at `Integer.MIN_VALUE`/`MAX_VALUE`, k ≥ n.

**Mistakes:**
1. Using `/` instead of `Math.floorDiv` for negatives.
2. `int` arithmetic: `x - left` or `t + 1` overflowing.
3. A bucket width of t instead of t + 1 (then values exactly t apart can land two buckets apart).
4. Checking only the same bucket, not the neighbours.
5. Mixing `Integer` and `Long` keys in the map.

---

## 9. Interview questions and answers

**Q1. Why can each bucket hold just one value?**
> If a second value arrived in an occupied bucket, the two would differ by at most t and we'd return true immediately. So in the map, every bucket that's still present has exactly one value.

**Q2. Why are the neighbouring buckets enough?**
> A value two or more buckets away differs from x by at least w + 1 = t + 2 > t.

**Q3. Buckets or TreeSet: which would you write?**
> The TreeSet is quicker to get right and O(n log k). Buckets are O(n) but need care with negatives and overflow. I'd mention both and implement whichever the interviewer prefers.

### Follow-up questions

**F1. Return the pair of indices.**
> Store `bucketId → index` instead of the value (and read the value from nums).

**F2. A data stream with no index window.**
> Without the window, keep every bucket (memory grows with the number of distinct buckets), or use a TreeSet over all the values.

**F3. Count the close pairs instead of detecting one.**
> Buckets no longer hold a single value. Use a sorted structure with range counts (a Fenwick tree over the compressed values) as the window slides.

---

## 10. Running the file

```bash
java H01_ContainsDuplicateIII.java      # 6/6 tests passed (including 2,000 random cases, buckets & TreeSet vs brute)
```

**One-line summary:** *"Hash each value into a bucket of width t + 1 with floorDiv; a match is in the same bucket or a neighbour, and the map only keeps the last k elements."*
