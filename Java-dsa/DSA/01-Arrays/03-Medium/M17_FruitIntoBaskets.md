# M17 · Fruit Into Baskets

| | |
|---|---|
| **LeetCode** | [#904 Fruit Into Baskets](https://leetcode.com/problems/fruit-into-baskets/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window + frequency map (at most K distinct values) |
| **Code** | [M17_FruitIntoBaskets.java](M17_FruitIntoBaskets.java) |
| **Run** | `java M17_FruitIntoBaskets.java` |

---

## 1. The problem

`fruits[i]` = the fruit type of tree i. You have **2 baskets**, and each basket holds **one type** (any amount of it). Start at any tree and walk right, picking one fruit from every tree; you must stop when you meet a **third** type. Return the maximum number of fruits.

**Without the story:** find the **longest subarray with at most 2 distinct values**.

| fruits | answer | subarray |
|---|---|---|
| `[1, 2, 1]` | 3 | the whole array |
| `[0, 1, 2, 2]` | 3 | `[1, 2, 2]` |
| `[1, 2, 3, 2, 2]` | 4 | `[2, 3, 2, 2]` |
| `[3,3,3,1,2,1,1,2,3,3,4]` | 5 | `[1, 2, 1, 1, 2]` |
| `[5]` | 1 | |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ fruits[i] < n`.

---

## 2. How to think about it

The translation step is the whole trick: a wordy story → "**the longest window with at most 2 distinct values**". After that, it's the **maximum-length sliding window** from [M12](M12_MaxConsecutiveOnesIII.md), with a different "budget":

| | M12 | M17 |
|---|---|---|
| window state | the number of zeros | **count of each type** in the window (a map) |
| valid when | zeros ≤ k | **map.size() ≤ 2** |

Template:

```
for right:
    count[fruits[right]]++                         // expand
    while count.size() > 2:                        // too many types → shrink
        count[fruits[left]]--; if it hit 0, remove the key
        left++
    best = max(best, right − left + 1)
```

**Important:** **remove** a key when its count reaches 0. Otherwise `map.size()` still counts a type that's no longer in the window.

---

## 3. Java concepts used in this problem

### 3.1 `merge` with −1 to decrement, and its return value

```java
if (count.merge(type, -1, Integer::sum) == 0) count.remove(type);
```

- `merge(type, -1, Integer::sum)` adds −1 to the stored count and **returns the new value**.
- If that's 0, the type is gone from the window, so remove the key.
- `map.size()` = the number of **distinct** types currently in the window.

### 3.2 Generalising with a helper method

```java
static int totalFruit(int[] fruits) {
    return longestWithAtMostKDistinct(fruits, 2);
}
```

The file solves the **general** problem (at most K distinct) and calls it with K = 2. Writing the general version often makes the code clearer, and it's reusable: the same helper is the building block of [H05 Subarrays with K Different Integers](../04-Hard/H05_SubarraysWithKDifferentIntegers.md).

### 3.3 An array instead of a HashMap

Since `0 ≤ fruits[i] < n`, you can count with `int[] count = new int[n]` and keep a separate `distinct` counter (increment it when a count goes 0 → 1, decrement it when a count goes 1 → 0). It's faster (no hashing or boxing). Approach 3 shows it.

---

## 4. Approach 1: Brute force

```java
static int totalFruitBrute(int[] fruits) {
    int best = 0;
    for (int i = 0; i < fruits.length; i++) {
        Set<Integer> types = new HashSet<>();
        int j = i;
        while (j < fruits.length && (types.contains(fruits[j]) || types.size() < 2)) {
            types.add(fruits[j]);
            j++;
        }
        best = Math.max(best, j - i);
    }
    return best;
}
```

For each start, walk right until a third type appears. **O(n²) time.**

---

## 5. Approach 2: Sliding window + HashMap (the file's solution)

### Code ([M17_FruitIntoBaskets.java:45-57](M17_FruitIntoBaskets.java#L45-L57))

```java
static int longestWithAtMostKDistinct(int[] a, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    int left = 0, best = 0;
    for (int right = 0; right < a.length; right++) {
        count.merge(a[right], 1, Integer::sum);
        while (count.size() > k) {
            int type = a[left++];
            if (count.merge(type, -1, Integer::sum) == 0) count.remove(type);   // gone from the window
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<Integer, Integer> count` | type → how many of it are in the window |
| `count.merge(a[right], 1, Integer::sum);` | **expand**: add the new tree's fruit |
| `while (count.size() > k)` | more than k types → invalid |
| `int type = a[left++];` | the leftmost fruit leaves the window |
| `if (count.merge(type, -1, ...) == 0) count.remove(type);` | decrement it; if none of that type is left, the type is gone |
| `best = Math.max(best, right - left + 1);` | the window is valid → record its length |

### Dry run 1: `[1, 2, 3, 2, 2]`

| right | add | map after adding | shrink | left | length | best |
|---|---|---|---|---|---|---|
| 0 | 1 | {1:1} | — | 0 | 1 | 1 |
| 1 | 2 | {1:1, 2:1} | — | 0 | 2 | 2 |
| 2 | 3 | {1:1, 2:1, 3:1} → 3 types | drop 1 → {2:1, 3:1} | 1 | 2 | 2 |
| 3 | 2 | {2:2, 3:1} | — | 1 | 3 | 3 |
| 4 | 2 | {2:3, 3:1} | — | 1 | 4 | **4** |

**Return 4** ✓

### Dry run 2: `[3, 3, 3, 1, 2, 1, 1, 2, 3, 3, 4]`

| right | add | map (after any shrink) | left | length | best |
|---|---|---|---|---|---|
| 0 | 3 | {3:1} | 0 | 1 | 1 |
| 1 | 3 | {3:2} | 0 | 2 | 2 |
| 2 | 3 | {3:3} | 0 | 3 | 3 |
| 3 | 1 | {1:1, 3:3} | 0 | 4 | 4 |
| 4 | 2 | 3 types → drop 3, 3, 3 → {1:1, 2:1} | 3 | 2 | 4 |
| 5 | 1 | {1:2, 2:1} | 3 | 3 | 4 |
| 6 | 1 | {1:3, 2:1} | 3 | 4 | 4 |
| 7 | 2 | {1:3, 2:2} | 3 | **5** | **5** |
| 8 | 3 | 3 types → drop 1, 2, 1, 1 → {2:1, 3:1} | 7 | 2 | 5 |
| 9 | 3 | {2:1, 3:2} | 7 | 3 | 5 |
| 10 | 4 | 3 types → drop 2 → {3:2, 4:1} | 8 | 3 | 5 |

**Return 5** ✓ (`[1, 2, 1, 1, 2]` at indices 3..7)

### Dry run 3: `[5]`
{5:1}, length 1 → **1** ✓

### Complexity
- **Time O(n):** each index enters and leaves the window once.
- **Space O(1):** the map never holds more than k + 1 = 3 keys.

---

## 6. Approach 3: Sliding window + count array (faster constant)

```java
static int totalFruitArray(int[] fruits) {
    int n = fruits.length;
    int[] count = new int[n];          // fruit types are in 0..n-1
    int distinct = 0, left = 0, best = 0;
    for (int right = 0; right < n; right++) {
        if (count[fruits[right]]++ == 0) distinct++;        // a new type entered
        while (distinct > 2) {
            if (--count[fruits[left]] == 0) distinct--;     // a type left completely
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

- `count[x]++ == 0`: the comparison uses the **old** value (post-increment), so it's true when the type was absent before.
- `--count[x] == 0`: pre-decrement compares the **new** value, so it's true when the type just disappeared.
- It's the same O(n), with no boxing or hashing.

---

## 7. Comparing the approaches

| | Brute force | Window + HashMap | Window + array |
|---|---|---|---|
| Time | O(n²) | O(n) | **O(n)**, faster |
| Space | O(1) | O(k) | O(n) array |
| Works for any value range | yes | **yes** | only small ranges |

---

## 8. Edge cases and common mistakes

**Edge cases:** one tree, only one type, exactly two types (the whole array), alternating types, a new type at the very end.

**Mistakes:**
1. Not removing keys whose count reaches 0, so `size()` stays too high and the window over-shrinks.
2. Shrinking by only one step (`if` instead of `while`). Here a single `if` would happen to work as the non-shrinking trick from M12, but be deliberate about it.
3. Thinking you must track "the last two types" by hand, which is error-prone. The count map is cleaner.

---

## 9. Interview questions and answers

**Q1. How do you recognise the pattern in the story?**
> "Pick a contiguous range of trees, at most two types" means the longest subarray with at most 2 distinct values, which is a maximum-length sliding window with a frequency map.

**Q2. Why must the key be removed at count 0?**
> The validity check is `map.size()`, the number of distinct types. A key with count 0 would be counted as present.

**Q3. What's the complexity, and why?**
> O(n) time: both pointers only move forward. O(1) space for k = 2 (at most 3 keys at once).

### Follow-up questions

**F1. At most k baskets (LC 340, k distinct characters).** → [02-Strings M04](../../02-Strings/03-Medium/M04_LongestSubstringAtMostKDistinct.md)
> Call `longestWithAtMostKDistinct(a, k)`.

**F2. Count the subarrays with EXACTLY k distinct values (LC 992).** → [04-Hard H05](../04-Hard/H05_SubarraysWithKDifferentIntegers.md)
> `exactly(k) = atMost(k) − atMost(k−1)`, where `atMost` counts with `+= right − left + 1`.

**F3. Longest substring without repeating characters (LC 3).** → [02-Strings M01](../../02-Strings/03-Medium/M01_LongestSubstringWithoutRepeating.md)
> The window is valid when every count ≤ 1; shrink while the new character's count > 1.

**F4. Return which trees to pick.**
> Store `left` and `right` when `best` improves.

---

## 10. Running the file

```bash
java M17_FruitIntoBaskets.java      # 5/5 tests passed
```

**One-line summary:** *"The longest window with at most 2 distinct values: count map, shrink while size > 2, remove keys at 0."*
