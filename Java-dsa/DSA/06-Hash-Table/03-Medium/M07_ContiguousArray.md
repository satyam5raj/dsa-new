# M07 · Contiguous Array ⭐

| | |
|---|---|
| **LeetCode** | [#525 Contiguous Array](https://leetcode.com/problems/contiguous-array/) |
| **Difficulty** | Medium |
| **Pattern** | Turn 0 into −1 → "the longest subarray with sum 0" → a first-index map |
| **Code** | [M07_ContiguousArray.java](M07_ContiguousArray.java) |
| **Run** | `java M07_ContiguousArray.java` |

⭐ = a bonus classic.

---

## 1. The problem

Given a binary array, return the length of the **longest contiguous subarray** with an **equal number of 0s and 1s**.

| nums | answer | subarray |
|---|---|---|
| `[0, 1]` | 2 | the whole array |
| `[0, 1, 0]` | 2 | [0, 1] or [1, 0] |
| `[0, 0, 1, 0, 0, 0, 1, 1]` | 6 | indices 2..7 = [1, 0, 0, 0, 1, 1] (three 0s, three 1s) |
| `[1, 1, 1]` | 0 | no balanced subarray |

**Constraints:** `1 ≤ n ≤ 10⁵`.

---

## 2. How to think about it

### The reframing trick
Count each **0 as −1** and each **1 as +1**. Then

> "equal numbers of 0s and 1s"  ⇔  "the subarray sums to 0"

### The longest subarray with sum 0 (prefix sums + hashing)
With prefix sums P (and P[−1] = 0):

```
sum(i+1 .. j) = P[j] − P[i] = 0   ⇔   P[j] == P[i]
```

For each j we want the **earliest** i with the same prefix value, since that gives the longest subarray. So store the **first index** of each prefix value (never overwrite), seeded with `{0: −1}`:

```
length = j − firstIndex[P[j]]
```

### The prefix-sum + HashMap family (one idea, many problems)

| Question | What the map stores | Problem |
|---|---|---|
| **count** the subarrays with sum k | prefix → **count** | [Arrays M07](../../01-Arrays/03-Medium/M07_SubarraySumEqualsK.md) |
| any subarray of length ≥ 2 with sum % k == 0 | prefix % k → **first index** | [M05](M05_ContinuousSubarraySum.md) |
| the **longest** subarray with sum 0 (or k) | prefix → **first index** | this one |

---

## 3. Java concepts used in this problem

### 3.1 `putIfAbsent`: "store it only if it's new"

```java
Integer i = firstIndex.putIfAbsent(prefix, j);
```

- If `prefix` is **absent**: it stores `prefix → j` and returns **null**.
- If `prefix` is **present**: it changes nothing and returns the **existing** value (the first index).

One call does both the lookup and the conditional insert. That's exactly the "first index, never overwrite" rule.

### 3.2 The ternary for the ±1 mapping

```java
prefix += nums[j] == 1 ? 1 : -1;
```

`==` binds tighter than `?:`, which binds tighter than `+=`, so it reads as `prefix += ((nums[j] == 1) ? 1 : -1)`. (An arithmetic alternative: `2 * nums[j] - 1`.)

### 3.3 An array instead of a map

The prefix always lies in [−n, n], so an `int[2n + 1]` with an offset of n can replace the HashMap (filled with a "not seen" marker such as −2). It's faster, with no boxing.

---

## 4. Approach 1: Brute force

### Code ([M07_ContiguousArray.java:54-64](M07_ContiguousArray.java#L54-L64))

```java
static int brute(int[] a) {
    int best = 0;
    for (int i = 0; i < a.length; i++) {
        int bal = 0;
        for (int j = i; j < a.length; j++) {
            bal += a[j] == 1 ? 1 : -1;
            if (bal == 0) best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `int bal = 0;` | the balance (1s minus 0s) of the subarray starting at i |
| `bal += …` | extend to j |
| `if (bal == 0)` | balanced → record its length |

**O(n²).** The file's test oracle.

---

## 5. Approach 2: An array of first indices

```java
static int findMaxLengthArray(int[] nums) {
    int n = nums.length;
    int[] first = new int[2 * n + 1];
    Arrays.fill(first, -2);                 // -2 = "not seen yet"
    first[n] = -1;                          // prefix 0 (offset by n) at index -1
    int prefix = 0, best = 0;
    for (int j = 0; j < n; j++) {
        prefix += nums[j] == 1 ? 1 : -1;
        if (first[prefix + n] == -2) first[prefix + n] = j;
        else best = Math.max(best, j - first[prefix + n]);
    }
    return best;
}
```

The same algorithm. `prefix + n` shifts the range [−n, n] into [0, 2n]. −2 marks "unseen", because −1 is a real index (the seed).

---

## 6. Approach 3: A HashMap of first indices (the file's solution)

### Code ([M07_ContiguousArray.java:42-52](M07_ContiguousArray.java#L42-L52))

```java
static int findMaxLength(int[] nums) {
    Map<Integer, Integer> firstIndex = new HashMap<>();
    firstIndex.put(0, -1);
    int prefix = 0, best = 0;
    for (int j = 0; j < nums.length; j++) {
        prefix += nums[j] == 1 ? 1 : -1;                  // 0 counts as -1
        Integer i = firstIndex.putIfAbsent(prefix, j);   // stores j only if the prefix is new
        if (i != null) best = Math.max(best, j - i);     // seen before -> a balanced subarray (i, j]
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `firstIndex.put(0, -1);` | the empty prefix: balance 0 at index −1 |
| `prefix += …` | the running balance |
| `putIfAbsent(prefix, j)` | the first time this balance is seen → remember j |
| `if (i != null)` | this balance was seen first at i → indices i+1..j are balanced, length j − i |

### Dry run 1: `[0, 1, 0]` (as −1, +1, −1)

| j | nums[j] | prefix | `putIfAbsent` returns | length | best | map after |
|---|---|---|---|---|---|---|
| | | | | | 0 | {0: −1} |
| 0 | 0 | −1 | null (new) | | 0 | {0: −1, −1: 0} |
| 1 | 1 | 0 | −1 | 1 − (−1) = 2 | 2 | unchanged |
| 2 | 0 | −1 | 0 | 2 − 0 = 2 | 2 | unchanged |

**Return 2** ✓

### Dry run 2: `[0, 0, 1, 0, 0, 0, 1, 1]`

| j | nums[j] | prefix | first seen at | length | best |
|---|---|---|---|---|---|
| 0 | 0 | −1 | new (0) | | 0 |
| 1 | 0 | −2 | new (1) | | 0 |
| 2 | 1 | −1 | 0 | 2 | 2 |
| 3 | 0 | −2 | 1 | 2 | 2 |
| 4 | 0 | −3 | new (4) | | 2 |
| 5 | 0 | −4 | new (5) | | 2 |
| 6 | 1 | −3 | 4 | 2 | 2 |
| 7 | 1 | −2 | **1** | **6** | **6** |

**Return 6**: indices 2..7 ✓. Prefix −2 first appeared at index 1, and keeping that **first** index is what gives the long answer at j = 7. (These traces were produced by running the code.)

### Dry run 3: `[1, 1, 1]`
Prefixes 1, 2, 3 are all new → **0** ✓

### Complexity
- **Time O(n).**
- **Space O(n).**

---

## 7. Comparing the approaches

| | Brute force | Array of first indices | HashMap |
|---|---|---|---|
| Time | O(n²) | O(n) | O(n) average |
| Space | O(1) | O(n) | O(n) |
| Notes | | fastest, needs the offset | simplest to write |

---

## 8. Edge cases and common mistakes

**Edge cases:** all 0s or all 1s (→ 0), the whole array balanced, n = 1.

**Mistakes:**
1. Forgetting the seed `{0: −1}`, which misses balanced prefixes like `[0, 1]`.
2. Overwriting the index with `put`, which shortens the answers.
3. Counting the lengths as j − i + 1 (the subarray is i+1..j, so its length is j − i).
4. Treating 0 as 0 instead of −1, so the sum tells you nothing.

---

## 9. Interview questions and answers

**Q1. Why map 0 to −1?**
> It turns "equal counts" into "sum 0", a condition prefix sums handle directly: two equal prefix values bracket a zero-sum subarray.

**Q2. Why the first index of each prefix?**
> For a fixed j, the longest balanced subarray ending at j starts right after the **earliest** i with P[i] = P[j].

### Follow-up questions

**F1. The longest subarray with sum exactly k (with negatives).**
> The same map: look up `P[j] − k` and take `j − firstIndex[P[j] − k]`.

**F2. The longest subarray where the 1s outnumber the 0s (LC 1124, "well-performing interval").**
> With ±1 values: if P[j] > 0, the answer is j + 1; otherwise look up the first index of P[j] − 1.

**F3. Count the balanced subarrays.**
> Store counts of each prefix instead of the first index (the [Arrays M07](../../01-Arrays/03-Medium/M07_SubarraySumEqualsK.md) pattern with k = 0).

---

## 10. Running the file

```bash
java M07_ContiguousArray.java      # 5/5 tests passed (including 1,000 random arrays vs brute force)
```

**One-line summary:** *"Count 0 as −1; a balanced subarray is a zero-sum one, so store the first index of each prefix (seed 0 → −1) and take j − first[P[j]]."*
